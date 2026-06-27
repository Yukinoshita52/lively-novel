# Agent Phase 1.6 Trace 回读 API 设计

> 日期：2026-06-27
> 状态：实施前设计
> 基线分支：`master`
> 关联规格：`docs/agent-transformation-spec.md`

## 1. 背景

Phase 1.5 已经补齐 `AgentRun -> AgentStep -> AgentGuardrailResult -> AgentToolCall` 的持久化轨迹，并通过 SSE 暴露实时事件。当前缺口是：trace 只在运行时推送和数据库中分散保存，缺少一个面向后端调试、前端展示和后续 Planner 诊断的统一回读接口。

Phase 1.6 的目标是把已落库的 Agent trace 聚合为稳定 API。它不改变转换主链路，也不引入 planner，而是先让可观测数据可以被可靠读取。

## 2. 本期目标

- 新增 Agent trace 聚合服务，按 run 回读 step、guardrail result 和 tool call。
- 新增 `GET /api/agent/runs/{runId}/trace` 接口。
- 新增面向 API 的 trace DTO，避免直接暴露 JPA entity。
- 按 step 组织 guardrail results 和 tool calls，方便前端后续渲染树状诊断视图。
- 兼容历史或异常情况下没有 `stepId` 的 tool call，避免回读接口丢数据。
- 保持现有 `/api/agent/screenplay/convert`、`/api/screenplay/convert` 和 SSE 行为不变。

## 3. 非目标

- 不新增前端 trace 面板。
- 不引入受限 Planner、ReAct loop 或多 Agent。
- 不新增 MCP server/client。
- 不改变 trace 表结构，除非实现时发现当前 repository 查询无法满足最小聚合。
- 不把小说正文、完整 LLM prompt 或敏感配置写入 trace DTO。

## 4. API 设计

新增接口：

```text
GET /api/agent/runs/{runId}/trace
```

成功响应沿用项目 `Result` 包装，`data` 为 `AgentTraceDTO`：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "runId": "ar-1234abcd",
    "userGoal": "将小说转换为动画剧本",
    "novelId": "nv-1234abcd",
    "conversionId": "cv-1234abcd",
    "screenplayType": "ANIME",
    "status": "COMPLETED",
    "currentStepIndex": 0,
    "startedAt": "2026-06-27T14:00:00Z",
    "updatedAt": "2026-06-27T14:00:01Z",
    "completedAt": "2026-06-27T14:00:02Z",
    "finalArtifactRef": "cv-1234abcd",
    "errorMessage": null,
    "steps": [
      {
        "stepId": "step-1234abcd",
        "stepIndex": 0,
        "stepName": "start_existing_conversion",
        "agentName": "ScreenplayConversionAgent",
        "status": "COMPLETED",
        "inputSummary": "{\"novelId\":\"nv-1234abcd\"}",
        "outputSummary": "{\"conversionId\":\"cv-1234abcd\"}",
        "startedAt": "2026-06-27T14:00:00Z",
        "completedAt": "2026-06-27T14:00:02Z",
        "errorMessage": null,
        "guardrails": [],
        "toolCalls": []
      }
    ],
    "unassignedToolCalls": []
  }
}
```

找不到 run 时返回业务失败结果：

```json
{
  "code": 40401,
  "message": "Agent run 不存在",
  "data": null
}
```

## 5. DTO 设计

建议新增 DTO：

```text
AgentTraceDTO
  runId
  userGoal
  novelId
  conversionId
  screenplayType
  status
  currentStepIndex
  startedAt / updatedAt / completedAt
  finalArtifactRef
  errorMessage
  steps: List<AgentStepTraceDTO>
  unassignedToolCalls: List<AgentToolCallTraceDTO>

AgentStepTraceDTO
  stepId
  stepIndex
  stepName
  agentName
  status
  inputSummary
  outputSummary
  startedAt / completedAt
  errorMessage
  guardrails: List<AgentGuardrailTraceDTO>
  toolCalls: List<AgentToolCallTraceDTO>

AgentGuardrailTraceDTO
  guardrailResultId
  guardrailName
  status
  message
  payloadJson
  createdAt

AgentToolCallTraceDTO
  toolCallId
  stepId
  toolName
  sideEffectLevel
  inputJson
  outputJson
  status
  startedAt / completedAt
  errorMessage
```

DTO 字段以 trace 诊断为主，只返回摘要与结构化结果，不扩展保存完整小说正文。

## 6. 后端组件

### 6.1 AgentTraceService

新增 `AgentTraceService`，职责：

- 根据 `runId` 查询 `AgentRunEntity`。
- 查询并按 `stepIndex` 升序返回 `AgentStepEntity`。
- 查询并按 `createdAt` 升序返回 `AgentGuardrailResultEntity`。
- 查询并按 `startedAt` 升序返回 `AgentToolCallEntity`。
- 将 guardrail result 和 tool call 按 `stepId` 归入对应 step。
- 对 `stepId` 为空或找不到对应 step 的 tool call，放入 `unassignedToolCalls`。

建议接口：

```java
Optional<AgentTraceDTO> getTrace(String runId);
```

### 6.2 AgentController

在现有 `AgentController` 中新增：

```java
@GetMapping("/runs/{runId}/trace")
public Result<AgentTraceDTO> getTrace(@PathVariable String runId)
```

行为：

- `runId` 为空或全空白：返回 `40001`。
- run 不存在：返回 `40401`。
- run 存在：返回聚合 trace。

## 7. 排序与兼容规则

- `steps` 按 `stepIndex` 升序排列。
- 每个 step 下的 `guardrails` 按 `createdAt` 升序排列。
- 每个 step 下的 `toolCalls` 按 `startedAt` 升序排列。
- `unassignedToolCalls` 按 `startedAt` 升序排列。
- 如果某个 guardrail result 的 `stepId` 找不到对应 step，第一版不单独暴露 `unassignedGuardrails`；因为 Phase 1.5 guardrail 总是 step 级结果。若实现或测试发现存在真实兼容需求，再补充字段。

## 8. 错误处理

- API 不应因为某个 step 下没有 guardrail 或 tool call 返回 500。
- API 不解析 `inputSummary`、`outputSummary`、`payloadJson`、`inputJson`、`outputJson`，只按字符串透传，避免无谓 JSON 解析失败影响诊断。
- repository 查询异常不吞掉，交由现有全局异常处理暴露服务端错误。
- 不在 trace 回读接口里触发任何转换、重试、修复或副作用行为。

## 9. 测试边界

- `AgentTraceServiceTest`：
  - run 存在时返回完整 trace。
  - steps 按 `stepIndex` 排序。
  - guardrails 和 tool calls 正确挂载到对应 step。
  - 没有 `stepId` 或 step 不存在的 tool call 进入 `unassignedToolCalls`。
  - run 不存在时返回 `Optional.empty()`。
- `AgentControllerTest` 或 `AgentControllerWebMvcTest`：
  - `GET /api/agent/runs/{runId}/trace` 成功返回 trace。
  - run 不存在时返回业务失败结果。
  - 空白 runId 若能命中路由，应返回参数错误。
- 回归测试：
  - Agent 相关后端测试继续通过。
  - 必要时执行 `backend` 全量测试，确认现有转换链路不退化。

## 10. 验收标准

- 已完成或失败的 Agent run 可以通过 runId 回读完整 trace。
- 回读结构清楚表达 run、step、guardrail 和 tool call 的层级关系。
- 历史或异常 tool call 不因缺少 step 关联而丢失。
- 现有 Agent 转换 SSE 与传统转换接口行为不变。
- 后续前端 trace 面板和受限 Planner 可以复用该 API。
