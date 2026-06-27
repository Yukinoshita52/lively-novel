# Agent Phase 1.5 可观测与护栏地基设计

> 日期：2026-06-27
> 状态：实施前设计
> 基线分支：`master`
> 关联规格：`docs/agent-transformation-spec.md`

## 1. 背景

Phase 1 已新增双轨 Agent 转换入口 `/api/agent/screenplay/convert`，并落地 `AgentRun`、`AgentToolCall`、`ToolRegistry` 和固定计划 `AgentOrchestrator`。当前 Agent 外壳已经能复用现有 `ScreenplayService` 启动转换，但 trace 仍停留在 run 与 tool call 两层。

正式规格中的 Phase 2 是“受限 Planner”。本设计定位为 Phase 1.5：在进入 planner 前，先补齐可解释的 step trace 和可阻断的 guardrail。这样后续 planner 生成错误计划、危险副作用或缺失参数时，系统能在工具执行前拦截，并留下可诊断轨迹。

本阶段目标是补齐 Agent 可观测与护栏地基，让后续 planner、细粒度工具拆分、critic 与 agents-as-tools 都能复用同一套 step / guardrail / tool call 轨迹。

## 2. 本期目标

- 新增 `AgentStep` 持久化记录，表达固定计划中的执行步骤。
- 新增 `AgentGuardrailResult` 持久化记录，表达护栏检查结果。
- 新增 `GuardrailService`，第一版只做确定性检查，不调用 LLM。
- 扩展 `AgentOrchestrator` 执行流：`run -> step -> guardrail -> tool call -> step/run completion`。
- 新增 Agent SSE 事件：`step_started`、`step_completed`、`guardrail_checked`。
- 在 guardrail `BLOCK` 时阻止工具执行，并将 step/run 标记为失败。
- 保持现有 `/api/screenplay/convert` 与 Phase 1 Agent endpoint 行为不退化。

## 3. 非目标

- 不引入 planner 或 ReAct loop。
- 不拆分 `runExistingScreenplayConversion` 为细粒度业务工具。
- 不引入 LLM critic、自修复或多 Agent。
- 不引入 MCP server/client。
- 不新增前端 trace 面板。
- 不改变现有 `ScreenplayService` 的转换语义。

## 4. 数据模型

### 4.1 AgentStep

`AgentStep` 是 run 内的可观测执行步骤。Phase 1.5 第一版固定计划只有一个步骤：`start_existing_conversion`。后续 planner 可复用同一张表记录动态计划步骤。

建议字段：

```text
id
runId
stepIndex
stepName
agentName
status: PENDING|RUNNING|COMPLETED|FAILED|BLOCKED
inputSummary
outputSummary
startedAt
completedAt
errorMessage
```

字段说明：

- `stepName` 第一版使用 `start_existing_conversion`。
- `agentName` 第一版使用 `ScreenplayConversionAgent`。
- `inputSummary` 记录 `novelId`、`screenplayType`、目标工具名等摘要，不保存小说正文。
- `outputSummary` 记录 `conversionId` 或 guardrail block 原因等摘要。

### 4.2 AgentGuardrailResult

`AgentGuardrailResult` 表示一次确定性护栏检查。

建议字段：

```text
id
runId
stepId
guardrailName
status: PASS|WARN|BLOCK
message
payloadJson
createdAt
```

第一版 guardrail 包括：

| guardrailName | 说明 | 阻断条件 |
|---|---|---|
| `tool_registered` | 工具必须存在于 `ToolRegistry` | 工具名不存在 |
| `tool_side_effect_allowed` | 工具副作用必须在当前步骤允许范围内 | 副作用级别不允许 |
| `agent_context_valid` | Agent context 必须包含必要参数 | `novelId` 为空或 `screenplayType` 为空 |

## 5. 后端组件

### 5.1 GuardrailService

`GuardrailService` 负责执行确定性检查，并返回可持久化的结果对象。第一版不调用 LLM，也不读写业务数据。

建议接口：

```java
List<AgentGuardrailCheckResult> checkBeforeToolCall(
        AgentTool tool,
        AgentToolContext context,
        AgentStepEntity step
);
```

行为规则：

- 所有检查都应落库并通过 SSE 发出 `guardrail_checked`。
- 只要任意结果为 `BLOCK`，Orchestrator 不得执行工具。
- `WARN` 不阻断工具执行，但需要进入 trace，供后续 UI 或诊断使用。

### 5.2 AgentOrchestrator

Orchestrator 从 Phase 1 的“run -> tool call”升级为：

```text
create AgentRun
emit agent_started
emit plan_created
create AgentStep
emit step_started
run GuardrailService
persist AgentGuardrailResult
emit guardrail_checked
if BLOCK:
  mark step BLOCKED
  mark run FAILED
  emit agent_failed
else:
  create AgentToolCall
  execute tool
  mark tool call COMPLETED/FAILED
  mark step COMPLETED/FAILED
  mark run COMPLETED/FAILED
  emit step_completed or agent_failed
```

### 5.3 Repository

新增：

- `AgentStepRepository`
- `AgentGuardrailResultRepository`

建议查询：

```java
List<AgentStepEntity> findByRunIdOrderByStepIndexAsc(String runId);
List<AgentGuardrailResultEntity> findByRunIdOrderByCreatedAtAsc(String runId);
List<AgentGuardrailResultEntity> findByStepIdOrderByCreatedAtAsc(String stepId);
```

## 6. SSE 事件

新增事件：

| 事件名 | 说明 |
|---|---|
| `step_started` | Agent step 开始执行。 |
| `guardrail_checked` | 某条护栏检查完成。 |
| `step_completed` | Agent step 成功完成。 |

沿用已有事件：

- `agent_started`
- `plan_created`
- `tool_call_started`
- `tool_call_completed`
- `agent_completed`
- `agent_failed`

`guardrail_checked` payload 建议包含：

```json
{
  "runId": "ar-xxx",
  "eventName": "guardrail_checked",
  "timestamp": "2026-06-27T12:55:00Z",
  "stepId": "step-xxx",
  "guardrailName": "tool_side_effect_allowed",
  "guardrailStatus": "PASS",
  "message": "Tool side effect WRITE_COST is allowed"
}
```

## 7. 错误处理

- 工具不存在：由 Orchestrator 查找工具前或 GuardrailService 校验时阻断，run 标记 `FAILED`，step 标记 `BLOCKED`。
- 参数缺失：guardrail `agent_context_valid` 返回 `BLOCK`，不创建 tool call。
- 副作用不允许：guardrail `tool_side_effect_allowed` 返回 `BLOCK`，不创建 tool call。
- 工具执行失败：保留 Phase 1 行为，tool call 标记 `FAILED`，step/run 标记 `FAILED`。
- SSE 发送失败：沿用当前 `completeWithError` 行为，不反向改写已持久化 trace。

## 8. 测试边界

- `AgentStepRepositoryTest`：验证 step 可落库并按 runId、stepIndex 查询。
- `AgentGuardrailResultRepositoryTest`：验证 guardrail result 可按 runId 和 stepId 查询。
- `GuardrailServiceTest`：覆盖合法工具通过、缺失 context 阻断、副作用不允许阻断。
- `AgentOrchestratorTest`：覆盖成功路径产生 step、guardrail、tool call；guardrail block 时不调用工具并标记 run 失败。
- `AgentSseEventFactoryTest`：覆盖 `stepId`、`guardrailName`、`guardrailStatus` 等可选字段。
- `AgentControllerWebMvcTest`：验证 Agent SSE 至少包含 `step_started` 与 `guardrail_checked`。
- 回归测试：现有 `ScreenplayControllerTest` 与 `ScreenplayControllerWebMvcTest` 继续通过。

## 9. 验收标准

- Agent run 可以回读 step、guardrail result、tool call 三层 trace。
- guardrail `BLOCK` 时不会执行工具。
- 成功路径 SSE 能看到 run、plan、step、guardrail、tool call 和 completion 事件。
- 现有 `/api/screenplay/convert` 行为不受影响。
- 后续受限 planner 可直接复用 `AgentStep` 与 `AgentGuardrailResult`。
