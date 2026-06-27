# Lively Novel Agent 化改造规格

> 日期：2026-06-14
> 版本：v0.1
> 状态：开发前规格
> 适用分支：`master` 及基于 `master` 创建的 feature 分支

---

## 1. 背景与目标

当前 Lively Novel 已经具备完整的小说转剧本 LLM workflow：导入 `.txt`、章节识别、章节切场、逐场生成、滚动全局状态、预览、单场 YAML 打磨、最终 YAML 导出。

但当前系统不是完整 Agent 项目。现状是后端代码固定编排流程，模型只在局部节点完成生成或切分任务。Agent 化改造的目标不是推翻现有链路，而是在保留稳定主流程的前提下，把系统演进为：

- 目标驱动：输入用户目标后，由编排层拆分计划并执行。
- 工具可控：模型只能调用服务端白名单工具，工具输入输出可校验。
- 状态可恢复：Agent run、step、tool call、artifact、error 均可持久化。
- 过程可观测：前端能展示计划、工具调用、护栏结果和产物。
- 质量可治理：通过 guardrail、critic、自修复和人工确认降低 LLM 不稳定性。
- 扩展可标准化：后续可暴露 MCP server，并沉淀领域 Skill。

## 2. 术语定义

| 术语 | 本项目定义 |
|---|---|
| LLM workflow | 由代码固定步骤驱动的 LLM 调用流程，例如当前“章节切分 -> 单场生成 -> 状态更新”。 |
| Agent | 由目标、指令、工具、状态、护栏和 trace 组成的受控执行单元，可在有限范围内规划下一步。 |
| Tool | 服务端暴露给 Agent 的单一能力，例如读取章节、切分章节、生成单场、保存场景、导出 YAML。 |
| Tool Registry | 工具注册表，负责工具元数据、输入输出 schema、权限、副作用级别和执行入口。 |
| Orchestrator | Agent 编排器，负责创建 run、生成/执行计划、调用工具、记录 trace、处理失败和恢复。 |
| Guardrail | 输入、输出、工具调用和副作用的约束与校验，例如 YAML schema 校验、语言漂移检测、工具白名单。 |
| Trace | Agent 运行轨迹，包括计划、步骤、工具调用、工具结果、护栏结果、错误和最终 artifact。 |
| Handoff | 一个 Agent 将任务移交给另一个 Agent。初期不直接开放，优先使用 agents-as-tools。 |
| MCP | Model Context Protocol。后续用于把 Lively Novel 的资源、提示词和工具以标准协议暴露给外部 Agent 客户端。 |
| Skill | 可版本化的领域能力包，包含 `SKILL.md`、参考资料和可复用提示词/规则。 |

## 3. 非目标

- 不重写现有导入、转换、预览、打磨、导出主链路。
- 不把所有功能一次性改成多 Agent。
- 不让模型直接访问文件系统、命令行、任意 URL 或数据库。
- 不在第一阶段引入 MCP server 和 Skill 包。
- 不把“React 前端”误写成“ReAct Agent”。若实现 ReAct，需要有明确的 plan/action/observation 循环和 trace。

## 4. 当前基线

当前已有能力：

- Spring Boot 后端，React + TypeScript 前端。
- Spring AI `ChatClient` 调用 DeepSeek OpenAI 兼容接口。
- 局部 `@Tool`：章节切场时模型可调用 `listChapterSegments` 与 `getSegmentRange`。
- SQLite + JPA 持久化小说、转换任务、场景单元、生成场景和滚动全局状态。
- SSE 展示转换过程事件。
- YAML schema 与导出逻辑。
- 语言漂移检测、JSON 解析修复、章节切场连续性校验等基础质量治理。

缺失能力：

- 没有统一 Tool Registry。
- 没有 Agent run / step / tool call trace 表。
- 没有独立 `AgentOrchestrator`。
- 没有 planner loop。
- 没有 MCP server/client。
- 没有运行时 Skill 包。
- 没有多 Agent 或 agents-as-tools 结构。

## 5. 目标架构

```text
Frontend React
  |
  | REST / SSE
  v
AgentController
  |
  v
AgentOrchestrator
  |-- RunStateRepository
  |-- AgentTraceRepository
  |-- GuardrailService
  |-- PlannerService
  |-- ToolRegistry
        |-- NovelTools
        |-- ChapterTools
        |-- ScreenplayTools
        |-- ContinuityTools
        |-- ValidationTools
        |-- PersistenceTools
        |-- ExportTools
        |-- HumanReviewTools
  |
  v
Existing Services
  |-- NovelService
  |-- ChapterSplitter
  |-- ChapterSegmentationService
  |-- LlmService
  |-- ScreenplayService
```

核心原则：

- 现有业务服务继续作为真实执行层。
- Agent 层只做编排、计划、工具选择、trace 和护栏，不绕过 service 直接操作 repository。
- 第一阶段 `AgentOrchestrator` 复刻现有固定流程，不引入自由决策。
- 模型 planner 只能输出结构化计划，且计划必须通过工具白名单、参数 schema 和最大步数校验。

## 6. 工具规划

### 6.1 NovelTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `listNovels` | 获取已导入小说列表 | 无 |
| `getNovelChapters` | 获取章节摘要 | 无 |
| `getChapterDetail` | 获取单章正文 | 无 |
| `updateNovelTitle` | 修改作品标题 | 有 |

### 6.2 ChapterTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `splitNovelIntoChapters` | 确定性章节识别 | 无 |
| `segmentChapter` | 将章节拆成编号片段 | 无 |
| `splitChapterIntoSceneUnits` | 调 LLM 生成场景单元 | 可能产生 LLM 成本 |
| `validateSceneUnits` | 校验切场连续覆盖、编号连续 | 无 |

### 6.3 ScreenplayTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `convertScene` | 将单场原文生成结构化剧本 | 可能产生 LLM 成本 |
| `regenerateScene` | 根据反馈重生单场 | 可能产生 LLM 成本 |
| `updateRollingAnalysisState` | 更新滚动全局状态 | 可能产生 LLM 成本 |
| `renderScenePreview` | 将结构化场景转预览模型 | 无 |

### 6.4 ValidationTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `validateSceneLanguage` | 检测语言漂移 | 无 |
| `validateScriptBlocks` | 校验七类剧本块字段合法性 | 无 |
| `validateYamlSchema` | 校验最终 YAML schema | 无 |
| `checkContinuity` | 检查人物名、故事线、场景引用一致性 | 可能产生 LLM 成本 |

### 6.5 PersistenceTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `createAgentRun` | 创建 Agent 运行记录 | 有 |
| `saveSceneUnit` | 保存切场单元 | 有 |
| `saveGeneratedScene` | 保存生成场景 | 有 |
| `saveAnalysisState` | 保存滚动全局状态 | 有 |
| `markRunCompleted` | 标记 run 完成 | 有 |
| `markRunFailed` | 标记 run 失败 | 有 |

### 6.6 ExportTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `buildYaml` | 生成 YAML 字符串 | 无 |
| `downloadYaml` | 前端下载 YAML | 用户动作 |

### 6.7 HumanReviewTools

| 工具 | 用途 | 副作用 |
|---|---|---|
| `requestSceneReview` | 请求用户检查某场异常 | 等待用户 |
| `requestPolishInstruction` | 请求用户给单场打磨要求 | 等待用户 |
| `confirmDestructiveAction` | 对覆盖、批量重生等操作要求确认 | 等待用户 |

## 7. Agent 划分

### 7.1 第一阶段：单 Agent 编排

只引入 `ScreenplayConversionAgent`：

- 输入：`novelId`、`screenplayType`、用户目标。
- 输出：转换详情、场景列表、最终 YAML artifact。
- 能力：调用工具完成现有主链路，记录 trace。
- 决策范围：初期没有自由 planner，只执行固定 plan。

### 7.2 第二阶段：受限 planner

引入 `PlannerService`：

- 输出 `PlanDTO`，包含步骤列表、工具名、参数摘要、预期 artifact。
- 所有工具名必须存在于 Tool Registry。
- 最大步数默认不超过 100。
- 禁止 planner 生成任意自然语言命令作为工具。
- planner 失败时回退到固定 plan。

### 7.3 第三阶段：agents-as-tools

优先使用 agents-as-tools，而不是复杂 handoff：

| 专家 Agent | 职责 |
|---|---|
| `SegmentAgent` | 章节内场景切分与切场校验修复。 |
| `ScreenwriterAgent` | 单场动画剧本生成。 |
| `ContinuityAgent` | 人物、故事线、伏笔、时间线一致性维护。 |
| `CriticAgent` | 质量检查、schema 检查、语言漂移和可读性问题定位。 |
| `PolishAgent` | 根据用户反馈进行单场打磨。 |

`DirectorAgent` 作为唯一入口和主控，其他专家 Agent 只能被主控调用，不直接接管用户会话。

## 8. Trace 与数据模型

建议新增实体：

```text
AgentRun
 ├── id
 ├── userGoal
 ├── novelId
 ├── conversionId
 ├── screenplayType
 ├── status: RUNNING|WAITING_USER|COMPLETED|FAILED|CANCELLED
 ├── currentStepIndex
 ├── startedAt / updatedAt / completedAt
 └── finalArtifactRef

AgentStep
 ├── id
 ├── runId
 ├── stepIndex
 ├── agentName
 ├── intent
 ├── status
 ├── inputSummary
 ├── outputSummary
 └── errorMessage

AgentToolCall
 ├── id
 ├── runId
 ├── stepId
 ├── toolName
 ├── sideEffectLevel: NONE|WRITE|COST|WRITE_COST|USER_CONFIRM
 ├── inputJson
 ├── outputJson
 ├── status
 ├── startedAt / completedAt
 └── errorMessage

AgentGuardrailResult
 ├── id
 ├── runId
 ├── stepId
 ├── guardrailName
 ├── status: PASS|WARN|BLOCK
 ├── message
 └── payloadJson
```

第一阶段可以只落 `AgentRun` 与 `AgentToolCall`，但接口和事件命名要预留 `step` 和 `guardrail`。

## 9. SSE 事件协议

Agent 事件与现有转换事件并存，新增事件建议如下：

| 事件名 | 说明 |
|---|---|
| `agent_started` | Agent run 创建成功。 |
| `plan_created` | 计划已生成或固定计划已载入。 |
| `step_started` | 某一步开始。 |
| `tool_call_started` | 工具调用开始。 |
| `tool_call_completed` | 工具调用完成。 |
| `guardrail_checked` | 护栏校验完成。 |
| `artifact_created` | 产生场景、状态、YAML 等 artifact。 |
| `human_input_required` | 需要用户确认或补充打磨要求。 |
| `agent_completed` | Agent run 成功结束。 |
| `agent_failed` | Agent run 失败。 |

事件 payload 必须包含：

- `runId`
- `eventName`
- `timestamp`
- 可选 `stepId`
- 可选 `toolCallId`
- 可选 `conversionId`
- 可选 `message`

## 10. Guardrail 规则

### 10.1 输入护栏

- 小说文本仍受现有字数、章节数、文件类型限制。
- 用户打磨指令只作为任务目标，不允许覆盖系统规则。
- 小说正文中的指令必须作为数据处理，不能提升为 Agent 指令。

### 10.2 工具护栏

- 所有工具必须声明副作用级别。
- `WRITE`、`COST`、`USER_CONFIRM` 工具必须记录 trace。
- `USER_CONFIRM` 工具未确认前不得执行真实副作用。
- planner 输出的工具名和参数必须通过 schema 校验。

### 10.3 输出护栏

- 场景输出必须通过 `scriptBlocks` 字段校验。
- YAML 导出必须符合 `docs/yaml-schema.md`。
- 人物名、故事线事件引用、场景 id 应尽可能保持一致。
- 语言漂移只允许作为警告进入打磨流程，不能静默吞掉。

## 11. 分阶段实施计划

### Phase 0：文档与规范

- 更新 `AGENT.md`，确立 `master` 为后续开发集成基线。
- 新增本 spec。
- 更新 `docs/technical-design.md`，记录 Agent 化演进方向。

验收标准：

- 后续 Agent PR 可引用明确 spec 章节。
- 文档清楚说明当前仍是 LLM workflow，不误称完整 Agent。

### Phase 1：Tool Registry 与固定计划 Orchestrator

- 新增 Tool Registry。
- 采用双轨过渡：保留现有 `/api/screenplay/convert`，新增 Agent 转换入口验证固定计划 Agent。
- 第一版使用薄 Agent 外壳，固定计划先通过粗粒度工具复用现有 `ScreenplayService` 能力。
- 新增 Agent run 与 tool call trace。
- 新增 Agent SSE 事件。
- `ScreenplayConversionAgent` 用固定计划跑通现有主链路。

验收标准：

- 原有 `/api/screenplay/convert` 行为不退化。
- 新增 Agent 接口可完成同等转换。
- trace 能看到工具调用顺序和失败点。

Phase 1 第一版暂不做：

- 不引入受限 planner。
- 不做多 Agent 或 agents-as-tools。
- 不引入 MCP server/client。
- 不做前端 trace 面板。
- 不替换现有 `/api/screenplay/convert`。
- 不一次性拆分所有现有业务步骤为细粒度工具。

### Phase 2：受限 Planner

- Planner 生成结构化 plan。
- Plan 只允许白名单工具。
- planner 失败回退固定计划。

验收标准：

- 同一转换目标可生成可解释 plan。
- 错误工具名、越界参数、过多步骤会被拒绝。

### Phase 3：Critic 与自修复

- 引入 `CriticAgent` 或 Critic tool。
- 针对 JSON 解析失败、YAML schema 失败、语言漂移、人物名不一致尝试自动修复。
- 无法修复时进入人工打磨提示。

验收标准：

- 至少覆盖一种失败自动修复路径。
- 修复前后 trace 可读。

### Phase 4：多 Agent / agents-as-tools

- 引入 `SegmentAgent`、`ScreenwriterAgent`、`ContinuityAgent`、`PolishAgent`。
- `DirectorAgent` 作为唯一主控。

验收标准：

- 每个专家 Agent 的输入输出边界清晰。
- 主链路仍可端到端完成。

### Phase 5：MCP Server

- 暴露 MCP resources：小说章节、转换状态、YAML schema、分析状态。
- 暴露 MCP prompts：动画剧本生成、连续性更新、YAML 打磨。
- 暴露 MCP tools：读取章节、生成单场、校验 YAML、导出 YAML。

验收标准：

- MCP 能被外部客户端列出 tools/resources/prompts。
- 外部 Agent 可在受控权限下读取资源和调用工具。

### Phase 6：Skill 包

- 新增 `skills/anime-screenplay/SKILL.md`。
- 新增 `skills/continuity-editor/SKILL.md`。
- 新增 `skills/yaml-polisher/SKILL.md`。

验收标准：

- Skill 中沉淀领域规则，不再散落在超长 prompt 中。
- Prompt 构造可以引用 Skill 规则版本。

## 12. PR 切片建议

| PR | 内容 |
|---|---|
| PR-0 | 文档规范：`AGENT.md`、本 spec、技术设计索引。 |
| PR-1 | 新增 Agent trace 实体与 repository，无业务接入。 |
| PR-2 | 新增 Tool Registry 和只读工具。 |
| PR-3 | 包装写入工具与副作用声明。 |
| PR-4 | 新增固定计划 `ScreenplayConversionAgent`。 |
| PR-5 | 新增 Agent SSE 事件与前端 trace 面板。 |
| PR-6 | 引入受限 Planner。 |
| PR-7 | 引入 Critic 自修复。 |
| PR-8 | 引入 MCP server。 |
| PR-9 | 引入 Skill 包。 |

## 13. 测试要求

Agent 相关 PR 至少覆盖以下类型中的相关项：

- Tool Registry：工具注册、重复名称、未知工具、参数校验。
- Tool 执行：成功、失败、副作用级别记录。
- Orchestrator：固定计划成功、工具失败中断、失败状态落库。
- Planner：非法工具名、非法参数、最大步数限制、回退固定计划。
- Guardrail：语言漂移、YAML schema、scriptBlocks 字段缺失。
- SSE：事件顺序、runId 一致性、失败事件可见。
- 前端：trace 面板渲染、失败提示、继续转换入口。

测试中默认使用 fake LLM 或 mock service，不依赖真实 DeepSeek API。

## 14. 文档同步要求

涉及以下变更时必须同步文档：

- 新增/删除工具：更新本 spec 的工具规划。
- 新增 Agent：更新 Agent 划分。
- 新增 SSE 事件：更新事件协议。
- 修改 YAML schema：同步 `docs/yaml-schema.md`、`docs/technical-design.md`、DTO/Model。
- 修改分支或 PR 规则：同步 `AGENT.md`。

## 15. 参考资料

- OpenAI Agents 文档：`https://developers.openai.com/api/docs/guides/agents`
- OpenAI Agent orchestration 文档：`https://developers.openai.com/api/docs/guides/agents/orchestration`
- MCP 规范：`https://modelcontextprotocol.io/specification/2025-06-18`
- Anthropic《Building effective agents》：`https://www.anthropic.com/research/building-effective-agents`
