# Agent Phase 1 基础骨架设计

> 日期：2026-06-27
> 状态：实施前设计
> 基线分支：`master`
> 关联规格：`docs/agent-transformation-spec.md`

## 1. 背景

`v1.0.0-ai-workflow` tag / Release 已记录 Lively Novel 在 Agent 化改造前的 AI Workflow 版本。后续开发基线切换为 `master`，Agent 化改造从稳定主链路上逐步推进。

当前代码仍是 LLM workflow：后端固定编排导入、转换、预览、打磨、导出流程，模型只在局部节点完成切场、单场生成和滚动状态更新。Phase 1 的目标不是替换主链路，而是先建立 Agent 运行骨架，让后续 planner、多 Agent、MCP 和 Skill 有稳定承载点。

## 2. 本期目标

- 新增独立 Agent 转换入口，和现有 `/api/screenplay/convert` 双轨并存。
- 新增 `AgentRun` 与 `AgentToolCall` 持久化记录。
- 新增 `ToolRegistry`，登记工具元数据、执行入口和副作用级别。
- 新增 `AgentOrchestrator`，执行固定计划并记录 trace。
- 新增最小 Agent SSE 事件，能观察 run 创建、计划载入、工具开始/完成、成功和失败。
- 第一版通过薄 Agent 外壳复用现有 `ScreenplayService` 能力，避免一次性拆分主链路。
- 第一版 `agent_completed` 表示固定计划已完成交接并启动现有转换流程；底层整本转换仍由现有转换 SSE 与持久化状态表达。后续拆分细粒度同步工具后，再把 Agent run 生命周期推进到覆盖完整转换完成。

## 3. 非目标

- 不替换现有 `/api/screenplay/convert`。
- 不引入受限 planner。
- 不做多 Agent 或 agents-as-tools。
- 不引入 MCP server/client。
- 不做前端 trace 面板。
- 不一次性把章节读取、切场、单场生成、状态更新、保存、导出全部拆成细粒度工具。

## 4. 后端组件

### 4.1 AgentController

新增独立入口：

```text
POST /api/agent/screenplay/convert
Content-Type: application/json
Accept: text/event-stream
```

请求体第一版复用 `ScreenplayConvertRequestDTO`：

```json
{
  "novelId": "nv-xxx",
  "screenplayType": "ANIME"
}
```

Controller 只负责参数接收、创建 SSE emitter，并把执行交给 `AgentOrchestrator`。

### 4.2 AgentOrchestrator

`AgentOrchestrator` 负责：

- 创建 `AgentRun`。
- 发送 `agent_started`。
- 加载固定计划并发送 `plan_created`。
- 从 `ToolRegistry` 查找并调用工具。
- 为每次工具调用创建 `AgentToolCall`。
- 工具成功时记录输出摘要并发送 `tool_call_completed`。
- 工具失败时标记 run 失败并发送 `agent_failed`。
- 固定计划完成时标记 run 完成并发送 `agent_completed`。

第一版固定计划只验证 Agent 外壳，不开放模型自由规划。

### 4.3 ToolRegistry

第一版工具使用粗粒度边界：

| 工具名 | 用途 | 副作用级别 |
|---|---|---|
| `runExistingScreenplayConversion` | 复用现有转换服务启动整本转换 | `WRITE_COST` |
| `markRunCompleted` | 标记 Agent run 完成 | `WRITE` |
| `markRunFailed` | 标记 Agent run 失败 | `WRITE` |

后续 PR 再逐步拆出 `getNovelChapters`、`splitChapterIntoSceneUnits`、`convertScene`、`updateRollingAnalysisState`、`validateYamlSchema` 等细粒度工具。

### 4.4 AgentRun

建议字段：

```text
id
userGoal
novelId
conversionId
screenplayType
status: RUNNING|WAITING_USER|COMPLETED|FAILED|CANCELLED
currentStepIndex
startedAt
updatedAt
completedAt
errorMessage
finalArtifactRef
```

### 4.5 AgentToolCall

建议字段：

```text
id
runId
stepId
toolName
sideEffectLevel: NONE|WRITE|COST|WRITE_COST|USER_CONFIRM
inputJson
outputJson
status: RUNNING|COMPLETED|FAILED
startedAt
completedAt
errorMessage
```

`stepId` 第一版可为空，用于兼容后续 `AgentStep`。

## 5. SSE 事件

第一版只实现最小集合：

| 事件名 | 说明 |
|---|---|
| `agent_started` | Agent run 创建成功 |
| `plan_created` | 固定计划已载入 |
| `tool_call_started` | 工具调用开始 |
| `tool_call_completed` | 工具调用完成 |
| `agent_completed` | Agent run 成功结束 |
| `agent_failed` | Agent run 失败 |

事件 payload：

```json
{
  "runId": "ar-xxx",
  "eventName": "tool_call_started",
  "timestamp": "2026-06-27T19:30:00Z",
  "toolCallId": "atc-xxx",
  "conversionId": "cv-xxx",
  "message": "runExistingScreenplayConversion started"
}
```

## 6. 数据流

```text
Frontend
  -> POST /api/agent/screenplay/convert
  -> AgentController
  -> AgentOrchestrator creates AgentRun
  -> ToolRegistry invokes fixed-plan tools
  -> Existing ScreenplayService runs current conversion workflow
  -> AgentRun / AgentToolCall persisted
  -> Agent SSE events streamed back
```

## 7. 测试边界

- `ToolRegistryTest` 覆盖工具注册、重复工具名、未知工具、执行成功和执行失败。
- `AgentOrchestratorTest` 覆盖固定计划成功、工具失败时 run 进入 `FAILED`、tool call 落库。
- `AgentControllerTest` 覆盖新接口能返回 SSE，至少能看到 `agent_started` 和失败事件。
- 文档检查覆盖 `dev` 不再作为后续基准分支。

Agent 相关测试默认使用 fake service 或 mock service，不依赖真实 DeepSeek API。
