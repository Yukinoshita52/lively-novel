# AGENT.md

> 本文件是仓库根索引。开始开发、改文档、拟 commit/PR 前，先读这里。

## 1. 项目概览

- 项目：`Lively Novel（活字成剧）`
- 目标：将 3+ 章小说自动转换为结构化 YAML 剧本，可逐场打磨与导出。
- 核心交付物：
  - 结构化 YAML 剧本
  - `docs/yaml-schema.md`
- 比赛窗口：`2026-06-05 00:00` 至 `2026-06-08 00:00`
- MVP 范围：仅实现 `ANIME`；`FILM`、`SHORT_DRAMA`、`RADIO`、`THEATER` 只保留枚举与模板位。
- 关键语义：`visualizedInnerThoughts` 是留痕/审计字段，记录“原内心描写 -> 转换手法 -> 转换结果”，不是额外正文。

## 2. 仓库索引

### 2.1 正式文档

- `AGENT.md`：根索引与开发协作规范
- `docs/requirements-analysis.md`：需求分析
- `docs/product-design.md`：产品设计
- `docs/technical-design.md`：技术设计
- `docs/yaml-schema.md`：YAML Schema 定义与约束
- `docs/prototype/`：静态原型文件
- `README.md`：项目说明与运行指引，当前仓库尚未补齐

### 2.2 代码与数据目录

- `backend/`：Spring Boot 后端
- `frontend/`：React + Vite 前端
- `data/`：SQLite 数据目录，不入库

## 3. 开发规范

### 3.1 基本原则

- `master` 必须始终可运行。
- 以纵向切片推进，每个切片合入后都应端到端可演示。
- 先从用户问题与演示价值推导方案，不直接堆功能。
- 改字段前先对齐三处：`docs/yaml-schema.md`、`docs/technical-design.md`、代码 DTO/Model。

### 3.2 分支与提交节奏

- 不直接在 `master` 开发。
- 分支命名：`feature-MuXue-xxx-yyy`
- `xxx` 使用驼峰英文功能名，`yyy` 使用秒级时间戳，例如 `20260606T160500`
- 每完成一个可验证小切片就提交，不能攒到最后一天统一导入。

### 3.3 本地运行与敏感信息

- 本地启动以未追踪的 `backend/src/main/resources/application-local.yml` 为准。
- `DEEPSEEK_API_KEY`、`JWT_SECRET` 等敏感信息只放环境变量或本地配置，不入库。
- `temp/` 下真本测试文件不可提交。
- 若用命令启动 Java 进程做验证，结束后必须关闭。

## 4. 文档维护规则

- 设计结论以 `docs/` 下正式文档为准，草稿不作为事实来源。
- 路径调整后，同步检查 `AGENT.md`、设计文档和代码注释中的引用。
- 原型文件统一放在 `docs/prototype/`，不再占用仓库根目录。
- `docs/superpowers/specs/` 下的 spec 文档统一使用中文编写。

## 5. Commit 规范

### 5.1 执行前说明要求

执行 `git commit` 前，必须先拟稿并等待确认。说明时逐个 commit 列出：

1. commit 标题
2. 如有需要的 message 正文
3. `git add` 的具体文件

多个 commit 必须串行执行，不要并行提交 commit，避免 `.git/index.lock` 冲突和提交边界混乱。

### 5.2 写法要求

- 使用语义化前缀：`feat:` `fix:` `docs:` `chore:` `refactor:` `test:`
- commit 标题、commit message、PR 标题、PR 正文默认使用中文；仅语义化前缀保留英文
- 标题必须直接说明动作与对象，不能只写 `feat`、`docs`、`test`
- 无关改动不要混入当前 commit
- 不加 `Co-Authored-By: ...`

### 5.3 拆分原则

- 一个 PR 只做一件事
- 一个 PR 内可拆多个 commit，但每个 commit 只承担单一职责
- 默认按“配置 / 文档 / 测试 / 功能接口”拆分；若依赖关系导致必须调整，先说明原因

## 6. PR 规范

- 执行 `git push` 和 `gh pr create` 前，先给出 PR 标题与正文草稿，等确认后再执行
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

## 7. 当前约束提醒

- 前端页面保持干净，不展示“后端在线”“MVP: ANIME”等解释性提示。
- 真实前端页面在 `frontend/`，`docs/prototype/` 只是静态原型。
- 章节分割属于纯代码逻辑，不放进 `LlmService`。
- 版权测试材料只允许放在被忽略的本地目录，不进仓库。
