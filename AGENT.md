# AGENT.md

> 本文件是仓库根索引。开始开发、改文档、拟 commit/PR 前，先读这里。

## 1. 项目概览

- 项目：`Lively Novel（活字成剧）`
- 目标：将 3+ 章小说自动转换为结构化 YAML 剧本，可逐场打磨与导出。
- 当前长期演进目标：在保留现有小说转剧本闭环的前提下，逐步改造为可观测、可控、可扩展的 Agent 项目。
- 核心交付物：
  - 结构化 YAML 剧本
  - `docs/yaml-schema.md`
- Agent 化交付物：
  - `docs/agent-transformation-spec.md`
  - Agent 编排、工具注册、运行轨迹、护栏、后续 MCP/Skill 扩展能力
- 比赛窗口：`2026-06-05 00:00` 至 `2026-06-08 00:00`
- MVP 范围：仅实现 `ANIME`；`FILM`、`SHORT_DRAMA`、`RADIO`、`THEATER` 只保留枚举与模板位。
- 关键语义：`visualizedInnerThoughts` 是留痕/审计字段，记录“原内心描写 -> 转换手法 -> 转换结果”，不是额外正文。
- Agent 化基线判断：当前代码属于 LLM workflow；只有引入目标驱动编排、受控工具调用、运行状态、护栏和 trace 后，才按 Agent 项目描述。

## 2. 仓库索引

### 2.1 正式文档

- `AGENT.md`：根索引与开发协作规范
- `docs/requirements-analysis.md`：需求分析
- `docs/product-design.md`：产品设计
- `docs/technical-design.md`：技术设计
- `docs/yaml-schema.md`：YAML Schema 定义与约束
- `docs/agent-transformation-spec.md`：Agent 化改造规格，后续 Agent 相关开发必须引用
- `docs/prototype/`：静态原型文件
- `README.md`：项目说明与运行指引，当前仓库尚未补齐

### 2.2 代码与数据目录

- `backend/`：Spring Boot 后端
- `frontend/`：React + Vite 前端
- `data/`：SQLite 数据目录，不入库

## 3. 开发规范

### 3.1 基本原则

- `dev` 是后续开发的伪 master / 集成基线，必须始终可运行。
- `master` 视为冻结的稳定历史分支；除非用户明确要求发布或同步，否则不从 feature 直接向 `master` 合并。
- 以纵向切片推进，每个切片合入后都应端到端可演示。
- 先从用户问题与演示价值推导方案，不直接堆功能。
- 改字段前先对齐三处：`docs/yaml-schema.md`、`docs/technical-design.md`、代码 DTO/Model。
- Agent 相关改动必须先对齐 `docs/agent-transformation-spec.md`；若需求不在 spec 内，先改 spec，再动代码。
- Agent 化遵循“先可观测、再可控自治、最后扩展 MCP/Skill”的顺序，不为了简历关键词一次性堆复杂框架。

### 3.2 分支与提交节奏

- 不直接在 `master` 或 `dev` 上开发功能。
- 新 feature 分支必须基于最新 `dev` 创建，开发完成后提交 PR 合并回 `dev`。
- 开始新分支前，先执行 `git fetch origin`，再确认本地 `dev` 与远端 `dev` 对齐。
- 分支命名：`feature-MuXue-xxx-yyy`
- `xxx` 使用驼峰英文功能名，`yyy` 使用秒级时间戳，例如 `20260606T160500`
- 每完成一个可验证小切片就提交，不能攒到最后一天统一导入。
- PR 默认目标分支是 `dev`。只有明确发布或回灌稳定版时，才讨论 `dev -> master`。

### 3.3 本地运行与敏感信息

- 本地启动以未追踪的 `backend/src/main/resources/application-local.yml` 为准。
- `DEEPSEEK_API_KEY`、`JWT_SECRET` 等敏感信息只放环境变量或本地配置，不入库。
- `temp/` 下真本测试文件不可提交。
- 若用命令启动 Java 进程做验证，结束后必须关闭。

## 4. 文档维护规则

- 设计结论以 `docs/` 下正式文档为准，草稿不作为事实来源。
- 路径调整后，同步检查 `AGENT.md`、设计文档和代码注释中的引用。
- 原型文件统一放在 `docs/prototype/`，不再占用仓库根目录。
- `docs/superpowers/` 是本地草稿工作区，不作为正式文档来源，不纳入版本控制。
- 正式 spec 文档放在 `docs/` 下，并统一使用中文编写。
- Agent 相关 PR 必须在 PR 正文的 `实现思路` 中引用具体 spec 章节。
- 如果代码实现偏离 spec，应优先判断是实现错误还是 spec 需要更新；不能让代码和 spec 长期不一致。

## 5. Agent 化开发规范

### 5.1 当前边界

- 当前 `lively-novel` 是 LLM workflow，不在简历或文档中直接宣称为完整 Agent 项目。
- React 是前端框架，不等同于 ReAct 推理-行动循环；若实现 ReAct，需要在 spec 和代码中明确规划、行动、观察循环。
- 现有 Spring AI `@Tool` 只属于局部工具调用，不等同于完整 Tool Registry 或 MCP。
- 当前没有 MCP server/client，也没有运行时 Skill 包；后续按 spec 分阶段引入。

### 5.2 改造顺序

1. 建立 Agent 运行轨迹：run、step、tool call、guardrail、error、artifact 均可追踪。
2. 把现有确定性能力包装为受控工具：小说读取、章节切分、场景生成、状态更新、校验、保存、导出。
3. 增加 `AgentOrchestrator`，先复刻现有固定流程，保证行为不退化。
4. 引入受限 planner，让模型只能在白名单工具和有限步骤内决策。
5. 增加 Critic/Continuity/Polish 等专家 agent 或 agents-as-tools。
6. 稳定后再暴露 MCP resources/prompts/tools。
7. 最后沉淀领域 Skill，包括动画剧本规则、YAML 打磨规则、连续性检查规则。

### 5.3 禁止事项

- 不允许引入不受限的文件系统、网络、命令执行工具给模型直接调用。
- 不允许让模型直接决定删除、覆盖、导出或发布用户数据；有副作用的工具必须有服务端校验和必要的用户确认。
- 不允许把小说正文中的指令当作系统指令执行；小说文本只能作为数据输入。
- 不允许为了 Agent 化重写整条主链路。每个 PR 必须保持导入、转换、预览、打磨、导出主链路可用。

## 6. Commit 规范

### 6.1 执行前说明要求

执行 `git commit` 前，必须先拟稿并等待确认。说明时逐个 commit 列出：

1. commit 标题
2. 如有需要的 message 正文
3. `git add` 的具体文件

多个 commit 必须串行执行，不要并行提交 commit，避免 `.git/index.lock` 冲突和提交边界混乱。

### 6.2 写法要求

- 使用语义化前缀：`feat:` `fix:` `docs:` `chore:` `refactor:` `test:`
- commit 标题、commit message、PR 标题、PR 正文默认使用中文；仅语义化前缀保留英文
- 标题必须直接说明动作与对象，不能只写 `feat`、`docs`、`test`
- 无关改动不要混入当前 commit
- 不加 `Co-Authored-By: ...`

### 6.3 拆分原则

- 一个 PR 只做一件事
- 一个 PR 内可拆多个 commit，但每个 commit 只承担单一职责
- 默认按“配置 / 文档 / 测试 / 功能接口”拆分；若依赖关系导致必须调整，先说明原因

## 7. PR 规范

- 执行 `git push` 和 `gh pr create` 前，先给出 PR 标题与正文草稿，等确认后再执行
- PR 默认合并目标为 `dev`。
- PR 正文从 `## 功能描述` 开始，不写 `## 标题`
- 固定结构：

```md
## 功能描述

## 实现思路

## 测试方式
```

- 不加 `Generated with ...` 等署名行
- `实现思路` 涉及字段或接口时，引用对应文档章节
- `测试方式` 写清命令、手动验证步骤与预期观察点
- 自动测试要写到具体测试类/测试方法或覆盖的行为，例如 `ScreenplayServiceImplTest#persistsConversionSceneUnitsAndGeneratedScenes` 验证转换任务、切场结果和单场剧本落库
- 手动验证要写可观察现象，例如 SSE `started/chapter_split/scene_completed` 事件包含同一个 `conversionId`，或 `GET /api/screenplay/conversions/{conversionId}` 能回读已生成场景
- 不要把“结果：xx tests, 0 failures”“构建成功，仅有 warning”当作测试方式；这些只可作为本地执行备注，不替代验证方法和预期现象
- Agent 相关 PR 的测试方式必须覆盖 trace、工具白名单、失败恢复或 guardrail 中至少一项，具体按 spec 要求选择。

## 8. 当前约束提醒

- 前端页面保持干净，不展示“后端在线”“MVP: ANIME”等解释性提示。
- 真实前端页面在 `frontend/`，`docs/prototype/` 只是静态原型。
- 章节分割属于纯代码逻辑，不放进 `LlmService`。
- 版权测试材料只允许放在被忽略的本地目录，不进仓库。
- `dev` 分支的目的不是重写项目，而是在稳定主链路上逐步引入 Agent 能力。
