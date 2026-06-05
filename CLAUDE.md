# CLAUDE.md — Lively Novel 开发规范与注意事项

> 本文件供 Claude Code 与所有协作者阅读。**动手写代码 / 提 PR 前先读这里。**
> 当前为**草稿（v0.1）**，规则可在团队确认后增补。

---

## 1. 这是什么项目

- **Lively Novel（活字成剧）**：七牛云 **72h 项目开发比赛**参赛作品，选题 **题目三：AI 小说转剧本工具**。
- **一句话**：把 3+ 章小说，自动转成**结构化 YAML 剧本**（格式正确、拆好场、对白清晰、内心戏已视觉化、人物/场景跨章一致），可逐场打磨、导出。
- **核心交付物**：① YAML 格式结构化剧本；② YAML Schema 设计文档（题目硬性要求，见 `docs/yaml-schema.md`）。

### 关键文档（改动前先对齐）

| 文档 | 作用 |
|---|---|
| `docs/requirements-analysis.md` | 需求分析（用户/痛点/功能） |
| `docs/product-design.md` | 产品设计（流程/页面/MVP 范围） |
| `docs/technical-design.md` | 技术方案（架构/数据模型/API/LLM 策略） |
| `docs/yaml-schema.md` | **YAML Schema 定义 + 设计原因**（题目要求交付物） |
| `prototype/` | 静态 HTML 原型（每屏左下角「场记板」标注调用的 API） |

> **改代码前**：相关字段务必与 `docs/yaml-schema.md` 和 `docs/technical-design.md §4` 对齐，三处（Schema 文档 / API 文档 / 代码）保持一致。

---

## 2. 比赛规则 ⚠️ 重中之重

### 2.1 评分占比（决定我们把力气花在哪）

| 维度 | 占比 | 含义 |
|---|---|---|
| **产品** | **40%** | 产品完成度、体验、是否解决真实痛点 |
| **过程** | **40%** | **持续的提交记录、PR 质量、工程规范** |
| **演示** | 20% | demo 视频的清晰度与说服力 |

> **过程分和产品分一样重**。代码写得好但提交记录是最后一天一次性灌进去的 → 过程分基本归零。

### 2.2 持续交付铁律（违反 = 作废）

- **必须有稳定、连续的提交节奏**。每完成一个小切片就 commit / PR，**不要攒着**。
- ❌ **最后一天批量导入** = 视为无效。
- ❌ **比赛窗口外的提交时间戳**（早于开始 / 晚于截止）= 视为无效。
- ✅ 比赛窗口：**2026-06-05 00:00 → 2026-06-08 00:00（72h）**。所有提交须落在此区间内。
- 养成习惯：**每个可独立验证的进展 = 一次提交**，哪怕只是一份文档、一个接口、一个组件。

### 2.3 工作方式：以客户为先

- 做任何产品/技术决策前，**先从用户与痛点出发**（参考"产品经理"思维），一步步推导，不要跳过需求直接堆功能。

---

## 3. 仓库与技术栈

### 3.1 仓库结构（单仓多模块）

```
lively-novel/
├── CLAUDE.md            # 本文件
├── README.md            # 交付物：项目说明 + 依赖列表 + demo 视频链接
├── docs/                # 四份设计文档
├── prototype/           # 静态 HTML 原型
├── backend/             # 后端：Java + Spring Boot 3（Maven）
├── frontend/            # 前端：React 18 + TS + Vite
└── data/                # SQLite 数据文件（.gitignore 忽略）
```

### 3.2 技术栈要点

- **后端**：Java 17 + Spring Boot 3 + Spring Data JPA + SQLite；LLM 用 **Spring AI Alibaba**（`ChatClient`），经 OpenAI 兼容适配器接 DeepSeek 官方 API。
- **前端**：React 18 + TypeScript + Vite + Ant Design 5。
- **通信**：REST + SSE（流式生成）；鉴权 JWT。
- 详见 `docs/technical-design.md §1`。

### 3.3 敏感信息（绝不入库）

- `DEEPSEEK_API_KEY`、`JWT_SECRET` 等一律走**环境变量**，不写进代码、不提交进仓库。
- `data/*.db`、本地配置、IDE 文件等确保在 `.gitignore` 中。

---

## 4. 开发规范

### 4.1 分支策略

- **`master` 始终可运行**（可 clone 即跑）。任何时刻 checkout master 都应能起前后端。
- 开发前**首先新建 feature 分支**，经 PR 合入 master，**不直接 push master**。
- **分支命名格式**：`feature-MuXue-xxx-yyy`
  - `xxx`：简单驼峰式的功能英文描述（如 `parseNovel`、`exportYaml`、`sceneEditor`）
  - `yyy`：精确到秒的时间戳（如 `20260605T143052`）
  - 示例：`feature-MuXue-parseNovel-20260605T143052`

### 4.2 交付顺序：纵向切片

按 `docs/product-design.md §5` 的纵向切片推进——**每个切片让 master 端到端可跑**，而非先堆后端再堆前端。优先级：

> 骨架空链路 → YAML Schema 先行 → 最小转换切片 → 结构化地基 → 内心戏视觉化 → 跨章一致性 → 预览渲染 → 打磨闭环 → README + demo。

### 4.3 代码风格

- 遵循各语言主流风格（Java：Google/阿里规范；TS：ESLint + Prettier 默认）。
- 命名、注释密度、风格**与周边已有代码保持一致**，不要单独标新立异。
- 字段命名三处对齐：**YAML Schema ↔ API 文档 ↔ 代码 DTO/Model**。

### 4.4 提交信息（commit message）

- 用语义化前缀：`feat:` `fix:` `docs:` `chore:` `refactor:` `test:`。
- 一句话讲清「做了什么」，必要时正文补「为什么」。
- 示例：`feat: 实现 /api/novel/parse 章节切分与字数统计`。

---

## 5. PR 提交规范 ⚠️（过程分关键）

### 5.1 三条铁律

1. **一个 PR 只做一件事**——粒度尽量小，便于 review 和回溯。
2. **master 合入后仍可运行**——不引入"半成品破坏主干"。
3. **持续提**——见 §2.2，不要攒大 PR。

### 5.2 PR 描述四段式（必填）

每个 PR 描述固定包含四部分：

```markdown
## 标题
（一句话概括本 PR 做了什么，对应 commit 语义前缀）

## 功能描述
（这个 PR 给产品/系统带来了什么变化，从使用者或调用方视角说）

## 实现思路
（关键设计选择、用到的模块/接口、为什么这么做；涉及字段务必引用 docs 对应章节）

## 测试方式
（如何验证：跑了哪些命令、点了哪些页面、看到什么预期结果；附必要截图/日志）
```

### 5.3 PR 自查清单

- [ ] 只做一件事，diff 聚焦
- [ ] master 合入后前后端仍可启动 / 跑通
- [ ] 四段式描述写全
- [ ] 不含密钥、不含 `data/*.db` 等忽略文件
- [ ] 字段/接口改动已同步到 `docs/`（Schema、API 文档）
- [ ] 提交时间在比赛窗口内

---

## 6. 提交交付物清单（结项前核对）

- [ ] **公开代码仓库**（评委可访问、可复现）
- [ ] **README**：项目说明 + **依赖列表** + **运行方式** + **demo 视频链接**
- [ ] **demo 演示视频**（有讲解旁白，清晰展示核心流程）
- [ ] **YAML Schema 设计文档**（`docs/yaml-schema.md`）
- [ ] 四份设计文档齐全且与代码一致
- [ ] 提交记录连续、分布在 72h 窗口内

---

## 7. 给 Claude 的特别提醒

- **改字段先查三处一致性**：`docs/yaml-schema.md` / `docs/technical-design.md §3、§4` / 代码。
- **MVP 聚焦动画类（ANIME）**；FILM/SHORT_DRAMA/RADIO/THEATER 仅预留枚举与模板位，不在 MVP 实现。
- **YAML 是核心交付**：打磨 = 编辑 YAML 结构；可读文本是 YAML 的渲染衍生，别本末倒置。
- **内心戏视觉化（visualizedInnerThoughts）是项目灵魂**，是"留痕/审计"语义，不要当成额外正文重复叙述。
- 涉及不可逆 / 对外动作（推送、删除、改 git 历史）前先确认。
