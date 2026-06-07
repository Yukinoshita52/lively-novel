# Lively Novel — 技术方案文档

> 日期：2026-06-05
> 版本：v1.5
> 依赖：需求分析文档 v1.0、产品设计文档 v1.0、YAML Schema 文档 v1.0
>
> **v1.1 变更**：① LLM 集成改用 Spring AI；② 引入 SQLite 持久化 + 内容哈希缓存；③ 增加完整登录认证（Spring Security + JWT）；④ 取消"无状态/不存储"设定。
> **v1.2 变更**：LLM 框架改用 **Spring AI Alibaba**（构建于 Spring AI，中文文档更丰富）；DeepSeek 经 **OpenAI 兼容适配器**接入 **DeepSeek 官方 API**。
> **v1.3 变更（§5 重审重写）**：① 全局分析改为**自适应 Map-Reduce**（短文单次／长文顺序滚动逐章 Map + Reduce，非并行以保跨章连贯）；② 修正中文 token 量级误算；③ 明确**场景切分**步骤补齐 A/B 缺环；④ prompt 改为"仅任务+规则"，Schema 交由 `.entity()` 注入；⑤ MVP **仅影视类**，广播剧/话剧规则列为扩展；⑥ 补人物名一致性、前场上下文、内心戏视觉化语义。
> **v1.4 变更（§6/7/8/9 复审）**：§6 补注册/登录滥用、并发任务、上传体积、提示注入、CORS、SQL 注入等风险；§7 标注初版、补 README/resources/common/dto/exception/test/data 等关键目录；§8 补 40401/42901/40303/50004 错误码；§9 补 WAL 开启、multipart 与 SSE 超时、全局预算、模型名核对提示。
> **v1.5 变更（§4 重写）**：结合原型逐屏，为全部 16 个接口补全请求/响应详细设计；接口列表增加「原型来源」列；新增 §4.2 通用约定（鉴权/JSON 包装/SSE 约定）；注册改为返回 token 自动登录；响应字段与 `yaml-schema.md` 全面对齐（ANIME 默认、schemaVersion、scriptBlocks 正文块）；补 ⑥⑨⑩⑪⑫⑬⑮⑯ 等此前缺失的接口示例。
> **v1.6 变更（当前实现同步）**：补充 §4.0 当前实现基线，明确比赛 MVP 当前使用 `/api/screenplay/conversions/...` 转换详情、单场保存与 YAML 导出接口；标注 JWT、AI 单场重生、可读文本导出、完整人物/线索视图仍为后续扩展；同步失败后继续转换、语言漂移降级提示、`sourceText`/`visualizedInnerThoughts` 不进入导出 YAML 等已实现约束。

---

## 1. 技术选型

### 1.1 技术栈总览

| 层级 | 选型 | 理由 |
|---|---|---|
| 前端框架 | React 18 + TypeScript | 组件生态成熟，类型安全，适合剧本渲染等复杂 UI |
| 前端构建 | Vite | 开发体验好，HMR 快 |
| 前端 UI 库 | Ant Design 5 | 企业级组件库，表单/表格/布局开箱即用 |
| 后端框架 | Spring Boot 3 (Java 17) | 比赛要求 Java 后端，Spring Boot 是最成熟的选择 |
| 构建工具 | Maven | Java 生态主流，简单直接 |
| 大模型 | DeepSeek-v4 (Pro/flash) | 用户指定，OpenAI 兼容接口 |
| **LLM 集成** | **Spring AI Alibaba (ChatClient)** | 构建于 Spring AI，中文文档/示例更丰富；经 OpenAI 兼容适配器，base-url 指向 DeepSeek 官方 API |
| **持久化** | **SQLite + Spring Data JPA** | 嵌入式、零外部依赖、克隆即可跑；驱动 `xerial sqlite-jdbc`，方言 Hibernate community `SQLiteDialect`，开启 WAL |
| **认证** | **Spring Security + JWT + BCrypt** | 完整登录认证，无状态 token，对 SSE 友好 |
| 前后端通信 | REST + SSE | 常规请求用 REST，流式生成用 SSE |

### 1.2 不引入的组件（72h 取舍）

| 组件 | 不引入理由 |
|---|---|
| MySQL/外部数据库 | 嵌入式 SQLite 已满足；外部 DB 增加评委复现摩擦。JPA 写法保证未来可平滑迁移 MySQL |
| Redis | 无分布式缓存/会话需求；JWT 无状态，缓存用 DB 内容哈希即可 |
| 消息队列 | 用 SSE 流式响应替代异步队列，降低复杂度 |
| Docker | 单机开发，暂不需要容器化（可后续补） |

---

## 2. 系统架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────┐
│                    前端 (React)                       │
│                                                       │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │
│  │ 登录/  │ │ 导入页 │ │ 预览页 │ │ 编辑页 │        │
│  │ 注册页 │ │        │ │        │ │        │        │
│  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘        │
│      │          │          │          │              │
│      └─────┬────┴──────────┴──────────┘              │
│            │ REST / SSE（请求头携带 JWT）             │
└────────────┼──────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────┐
│              后端 (Spring Boot)                       │
│                                                       │
│  ┌──────────────────────────────────────────────┐   │
│  │   Security 层：JWT 过滤器 + BCrypt + 配额限流  │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │（鉴权通过）                   │
│  ┌────────────────────▼─────────────────────────┐   │
│  │   Controller 层                                │   │
│  │   Auth / Novel / Screenplay Controller        │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                              │
│  ┌────────────────────▼─────────────────────────┐   │
│  │   Service 层                                   │   │
│  │   ┌───────────┐  ┌──────────────┐             │   │
│  │   │ AnalysisSvc│  │ GenerationSvc│  阶段A/B   │   │
│  │   └─────┬─────┘  └──────┬───────┘             │   │
│  │         │               │                      │   │
│  │   ┌─────▼───────────────▼──────┐  ┌─────────┐ │   │
│  │   │  LlmClient (Spring AI)      │  │ CacheSvc│ │   │
│  │   │  ChatClient → DeepSeek      │  │ 内容哈希│ │   │
│  │   └─────────────────────────────┘  └────┬────┘ │   │
│  └─────────────────────────────────────────┼──────┘   │
│                       │                     │          │
│  ┌────────────────────▼─────────────────────▼──────┐   │
│  │   Repository 层 (Spring Data JPA)                │   │
│  │   User / Novel / Screenplay Repository           │   │
│  └────────────────────┬─────────────────────────────┘   │
│                       ▼                                  │
│              ┌──────────────────┐                       │
│              │  SQLite (文件,WAL) │                      │
│              └──────────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

### 2.2 架构说明

- **单体架构**：Spring Boot 单应用，前后端分离部署
- **有状态持久化**：用户、小说、剧本均存入 SQLite，重启不丢，支持"继续使用同一份剧本"
- **鉴权**：所有业务接口经 JWT 过滤器鉴权；剧本归属到用户，仅本人可读写
- **成本控制**：内容哈希缓存（同一份小说+类型不重复调 LLM）+ 每用户配额/限流
- **流式响应**：转换过程通过 SSE 推送进度和中间结果

---

## 3. 核心数据模型

### 3.1 持久化实体

```
User (用户)
 ├── id: Long (PK)
 ├── username: String (唯一)
 ├── passwordHash: String (BCrypt)
 ├── dailyTokenQuota: int (每日配额)
 ├── usedTokenToday: int (当日已用 token)
 ├── quotaDate: LocalDate (配额计数所属日期；调用时若 != 今天则先清零 usedTokenToday)
 └── createdAt: Instant

Novel (小说)  ── 归属 userId
 ├── id: String (UUID, PK)
 ├── userId: Long (FK)
 ├── title: String
 ├── contentHash: String (用于缓存命中)
 ├── totalChapters: int
 ├── totalWordCount: int
 ├── rawContent: TEXT (原文)
 └── createdAt: Instant

Screenplay (剧本)  ── 归属 userId，关联 novelId
 ├── id: String (UUID, PK)
 ├── userId: Long (FK)
 ├── novelId: String (FK)
 ├── title: String
 ├── screenplayType: ENUM (ANIME|FILM|SHORT_DRAMA|RADIO|THEATER)
 ├── contentJson: TEXT  ← 完整剧本结构序列化为 JSON 存储（避免嵌套全 ORM 化）
 ├── cacheKey: String (novelContentHash + type，缓存命中键；有意反范式，与 Novel.contentHash 冗余以换取直接索引命中)
 └── updatedAt: Instant
```

### 3.2 剧本内容结构（存于 Screenplay.contentJson）

```
ScreenplayContent
 ├── plotSummary: String          ← 阶段A全局分析的剧情概要，持久化以支持"逐场重生(globalContext)"无需重跑阶段A
 ├── characters: List<Character>
 │    ├── name / role(PROTAGONIST|SUPPORTING|MINOR)
 │    ├── description / firstAppearance
 │    └── relationships: List<{target, relation}>
 ├── scenes: List<Scene>
 │    ├── sceneId: String
 │    ├── heading: {interior, location, timeOfDay}
 │    ├── scriptBlocks: List<{type, text?, character?, parenthetical?, line?}>
 │    ├── sourceChapter: int
 │    └── sourceText: String              ← 内部溯源/重生输入，不进入导出 YAML
 └── storylines: List<{name, type(MAIN|SUB), events[]}>
```

### 3.3 转换管线数据流

```
原始文本 (String)
    │
    ▼ [章节分割]
List<Chapter>
    │
    ▼ [计算 contentHash → 查缓存命中？]——命中──▶ 直接返回已存剧本（不调 LLM）
    │ 未命中
    ▼ [全局分析 - 阶段A，Spring AI .entity(AnalysisResult.class)]
AnalysisResult { characters, scenes, storylines, plotSummary }
    │
    ▼ [逐场生成 - 阶段B，逐场景 .entity(Scene.class)]
List<Scene>  ← 每生成一个场景即通过 SSE 推送前端
    │
    ▼ [组装 + 落库 SQLite]
Screenplay (持久化)
    │
    ├──▶ YAML 导出
    └──▶ 可读文本导出
```

---

## 4. API 设计

### 4.0 当前实现基线（2026-06-07）

本节记录当前代码已经落地的比赛 MVP 接口与页面链路。§4.1 起保留的是目标态/扩展态接口设计，用于说明后续演进方向；开发与演示时以本节为准。

| 方法 | 路径 | 当前用途 | 状态 |
|---|---|---|---|
| GET | `/api/health` | 健康检查 | 已实现 |
| POST | `/api/novel/parse` | 粘贴正文解析章节 | 已实现 |
| POST | `/api/novel/upload` | 上传 `.txt`、解析并持久化小说 | 已实现 |
| GET | `/api/novel` | 历史小说列表 | 已实现 |
| GET | `/api/novel/{id}/chapters` | 回读小说章节摘要 | 已实现 |
| GET | `/api/novel/{id}/chapters/{chapterIndex}` | 回读单章详情 | 已实现 |
| PUT | `/api/novel/{id}/title` | 修改并持久化作品标题 | 已实现 |
| POST | `/api/screenplay/convert` | 提交整本转换任务，SSE 返回进度与场景 | 已实现 |
| GET | `/api/screenplay/conversions/{conversionId}` | 回读转换详情与已持久化场景 | 已实现 |
| GET | `/api/screenplay/conversions/latest?novelId=...&screenplayType=...` | 回读某小说最近完成的转换 | 已实现 |
| PUT | `/api/screenplay/conversions/{conversionId}/chapters/{chapterIndex}/scenes/{sceneIndexInChapter}` | 保存单场打磨后的场景 JSON | 已实现 |
| GET | `/api/screenplay/conversions/{conversionId}/yaml` | 导出最终 YAML 剧本 | 已实现 |
| POST | `/api/screenplay/convert-single` | 单场转换实验入口 | 已实现，非主演示链路 |

当前实现的关键约束：

- MVP 页面链路为"导入 → 转换 → 预览 → 打磨 → 导出"。导出入口统一放在"导出"页。
- 当前主链路未启用完整 JWT 登录、用户配额与资源归属校验；相关设计仍保留在目标态安全章节，作为后续扩展。
- 转换任务会持久化章节切场结果与已生成场景；失败后可继续转换，前端会跳过已完成部分。
- 单场生成如果多次重试后仍疑似出现语言漂移，不再直接中断整本转换；后端会继续保留该场并通过 SSE 提示用户在预览/打磨时重点检查。
- 打磨页当前支持直接编辑单场 YAML、保存、取消和右侧实时预览；AI 单场重生、视觉化手法选择、风格提示重写仍是后续扩展。
- 导出 YAML 使用 `scriptBlocks` 作为剧本正文唯一主体；`sourceText` 与 `visualizedInnerThoughts` 属于内部生成/审计字段，不进入最终 YAML。
- YAML 导出禁用长字符串自动反斜杠折行，保证长 `text` 字段在同一逻辑行展示。

### 4.1 接口列表（含原型映射）

> **原型映射列**指出该接口在 `docs/prototype/` 哪一屏被调用（各页面左下角「场记板」亦有标注）。
> 注意：下表描述目标态 API 设计，部分接口尚未落地或已被当前 MVP 的 `/api/screenplay/conversions/...` 接口替代；当前开发与演示接口见 §4.0。

| # | 方法 | 路径 | 说明 | 鉴权 | 响应类型 | 原型来源 |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | 注册并自动登录 | 否 | JSON | `login.html` |
| 2 | POST | `/api/auth/login` | 登录，返回 JWT | 否 | JSON | `login.html` |
| 3 | GET | `/api/auth/me` | 当前用户信息/配额 | 是 | JSON | 所有页顶栏配额条 |
| 4 | POST | `/api/novel/parse` | 粘贴文本并解析章节（当前无状态，不落库） | 否 | JSON | `import.html` |
| 5 | POST | `/api/novel/upload` | 上传 txt 文件并解析（当前无鉴权） | 否 | JSON | `import.html` |
| 6 | GET | `/api/novel/{id}/chapters` | 获取章节列表（当前无鉴权） | 否 | JSON | `import.html`（复用回显） |
| 7 | GET | `/api/novel` | 我的小说列表（复用历史） | 是 | JSON | `import.html` |
| 8 | POST | `/api/screenplay/convert` | 提交转换任务 | 是 | SSE | `converting.html` |
| 9 | GET | `/api/screenplay` | 我的剧本列表 | 是 | JSON | `preview.html`（切换剧本） |
| 10 | GET | `/api/screenplay/{id}` | 获取完整剧本 | 是(本人) | JSON | `preview.html` |
| 11 | GET | `/api/screenplay/{id}/scenes` | 场景表/大纲（轻量投影） | 是(本人) | JSON | `preview.html` |
| 12 | GET | `/api/screenplay/{id}/characters` | 获取人物表 | 是(本人) | JSON | `preview.html` |
| 13 | PUT | `/api/screenplay/{id}/scenes/{sceneId}` | 手动编辑单场 | 是(本人) | JSON | `scene-edit.html` |
| 14 | POST | `/api/screenplay/{id}/scenes/{sceneId}/regenerate` | AI 重生单场 | 是(本人) | SSE | `scene-edit.html` |
| 15 | GET | `/api/screenplay/{id}/export/yaml` | 导出 YAML（核心交付） | 是(本人) | YAML 文件 | `export.html` |
| 16 | GET | `/api/screenplay/{id}/export/text` | 导出可读剧本 | 是(本人) | Text 文件 | `export.html` |

---

### 4.2 通用约定

#### 4.2.1 鉴权

除 `register`/`login` 外，所有接口须在请求头携带 `Authorization: Bearer <JWT>`。带 `(本人)` 的接口在鉴权之外，还校验**资源归属**（`资源.userId == 当前用户`），不符返回 `40301`。

> 当前代码基线中的 `POST /api/novel/parse`、`POST /api/novel/upload`、`GET /api/novel/{id}/chapters` 尚未接入 JWT；其中 `parse` 为无状态章节识别，`upload` 与 `/{id}/chapters` 已接入 SQLite 持久化。待认证切片完成后，再按本节统一鉴权。

#### 4.2.2 JSON 响应包装

所有 **JSON** 接口统一用 `{code, data, message}` 包装（见 §8.1）。下文各接口的"响应"仅展开 `data` 内字段，外层包装省略；成功 `code=0`。失败响应形如 `{ "code": 40003, "data": null, "message": "章节数不足..." }`。

#### 4.2.3 SSE 约定

- 流式接口（`convert`、`regenerate`）以 `Content-Type: text/event-stream` 返回，**不走** `{code,data,message}` 包装；每条消息形如 `event: <名> \n data: <JSON>`。
- `data` 内是**裸 JSON 对象**（非包装体），字段直接对应。
- **前置校验**（配额/章节/并发等）在建立 SSE 流**之前**完成；若不通过，直接返回普通 JSON 错误体（HTTP 4xx + `{code,...}`），不进入事件流。
- 流中途出错以 `event: error` 推送 `{code, message}` 后关闭。

---

### 4.3 认证接口

#### ① POST `/api/auth/register` — 注册并自动登录

> 原型：`login.html` 注册表单「注册并登录」按钮——故注册成功**直接返回 token**，前端免二次登录。

**请求：**
```json
{ "username": "xiaolin", "password": "Passw0rd!" }
```

**响应 `data`：**
```json
{
  "token": "eyJhbGciOiJIUzI1Ni
...",
  "expiresIn": 86400,
  "user": { "userId": 1, "username": "xiaolin", "dailyTokenQuota": 200000 }
}
```

**可能错误码：** `40001`（参数为空）、`40004`（用户名已存在）、`40005`（密码强度不足）、`42901`（注册 IP 限流）。

#### ② POST `/api/auth/login` — 登录

> 原型：`login.html` 登录表单。

**请求：**
```json
{ "username": "xiaolin", "password": "Passw0rd!" }
```

**响应 `data`：**
```json
{
  "token": "eyJhbGciOiJIUzI1Ni
...",
  "expiresIn": 86400,
  "user": { "userId": 1, "username": "xiaolin", "dailyTokenQuota": 200000 }
}
```

**可能错误码：** `40001`、`40103`（用户名或密码错误）、`42901`（登录失败限流/退避）。

#### ③ GET `/api/auth/me` — 当前用户与配额

> 原型：所有页顶栏 `今日额度 168k / 200k tokens` + 用户头像。`remainingToken` 供配额条显示。

**请求：** 无 body。

**响应 `data`：**
```json
{
  "userId": 1,
  "username": "xiaolin",
  "dailyTokenQuota": 200000,
  "usedTokenToday": 32000,
  "remainingToken": 168000,
  "quotaDate": "2026-06-05"
}
```

**可能错误码：** `40101`（未登录）、`40102`（token 失效）。

---

### 4.4 小说接口

#### ④ POST `/api/novel/parse` — 粘贴文本解析

> 原型：`import.html` 左侧粘贴正文，触发"自动识别章节"——返回 `已识别 3 章 · 约 1.2 万字`。

**请求：**
```json
{
  "title": "她比烟花寂寞",
  "text": "第一章 出租屋的夜\n...\n第二章 天台\n...\n第三章 转机\n..."
}
```

**响应 `data`：**
```json
{
  "title": "她比烟花寂寞",
  "totalChapters": 3,
  "totalWordCount": 12000,
  "chapters": [
    { "chapterIndex": 1, "title": "出租屋的夜", "wordCount": 4200 },
    { "chapterIndex": 2, "title": "天台",       "wordCount": 3800 },
    { "chapterIndex": 3, "title": "转机",       "wordCount": 4000 }
  ]
}
```

**说明：** 当前实现为**无状态解析**：仅返回章节元信息，**不落库、不返回 `novelId/contentHash`**。章节标题/字数由后端确定性切分得出（§5.2 步骤①）；`novelId/contentHash` 待持久层切片补入。

**可能错误码：** `40001`（文本为空）、`40002`（超 20 万字）、`40003`（不足 3 章）。

#### ⑤ POST `/api/novel/upload` — 上传 .txt 解析

> 原型：`import.html`「或 上传 .txt 文件」。与 ④ 等价，仅入参形式不同。

**请求：** `multipart/form-data`
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file(.txt) | 是 | UTF-8 文本，≤ 5MB（§6 multipart 上限） |
| `title` | text | 否 | 缺省时取文件名（去扩展名） |

**响应 `data`：** 同 ④（`novelId / title / contentHash / totalChapters / totalWordCount / chapters[]`）。

**可能错误码：** `40001`（空文件/非文本）、`40002`（超 20 万字或超 5MB）、`40003`（不足 3 章）。

#### ⑥ GET `/api/novel/{id}/chapters` — 获取章节列表

> 原型：`import.html` 复用历史小说时回显章节；亦供转换前确认。

**请求：** 路径参数 `id`。

**响应 `data`：**
```json
{
  "novelId": "nv-7f3a",
  "title": "她比烟花寂寞",
  "totalChapters": 3,
  "totalWordCount": 12000,
  "chapters": [
    { "chapterIndex": 1, "title": "出租屋的夜", "wordCount": 4200, "preview": "雨敲在铁皮屋檐上。林晚把简历又看了一遍……" },
    { "chapterIndex": 2, "title": "天台",       "wordCount": 3800, "preview": "黄昏，城市在脚下铺开……" },
    { "chapterIndex": 3, "title": "转机",       "wordCount": 4000, "preview": "手机响了。是那家公司……" }
  ]
}
```

**可能错误码：** `40401`（小说不存在）、`40301`（非本人）。

#### ⑦ GET `/api/novel` — 我的小说列表

> 原型：`import.html` 复用历史小说，避免重复粘贴。

**请求：** 可选 query `?page=1&size=20`。

**响应 `data`：**
```json
{
  "novels": [
    { "novelId": "nv-7f3a", "title": "她比烟花寂寞", "totalChapters": 3, "totalWordCount": 12000, "createdAt": "2026-06-05T09:12:00Z" }
  ],
  "total": 1
}
```

---

### 4.5 剧本转换与读取接口

#### ⑧ POST `/api/screenplay/convert` — 提交转换（SSE 流式）

> 原型：`import.html`「开始转换」跳转 `converting.html`，左侧两阶段进度条 + 右侧实时事件流。

**请求：**
```json
{
  "novelId": "nv-7f3a",
  "screenplayType": "ANIME"
}
```

**前置校验（未过则不建流，返回 JSON 错误）：** `40003`（不足 3 章）、`40302`（超每日配额）、`40303`（全局预算用尽）、`42901`（已有进行中任务，超并发上限 §6）。

**SSE 事件流（`data` 为裸 JSON）：**
```
# 命中缓存：直接给结果，跳过 LLM（§5.5）
event: cache_hit
data: {"hit": true, "screenplayId": "sp-2b9c"}

# 阶段 A：全局分析
event: analysis_start
data: {"phase": "ANALYSIS", "message": "通读全文，抽取人物/场景/线索…"}

event: analysis_progress
data: {"phase": "ANALYSIS", "progress": 0.5, "message": "提取人物关系…"}

event: analysis_complete
data: {
  "phase": "ANALYSIS",
  "plotSummary": "求职屡败的林晚在天台徘徊，终被一通录用电话挽回。",
  "characters": [
    {"name":"林晚","role":"PROTAGONIST","description":"27岁求职设计师","firstAppearance":"第1章",
     "relationships":[{"target":"母亲","relation":"母女"}]}
  ],
  "scenes": [
    {"sceneId":"s1","sourceChapter":1,"heading":{"interior":true,"location":"出租屋","timeOfDay":"夜"}}
  ],
  "storylines": [
    {"name":"求职挣扎","type":"MAIN","events":[{"scene":"s1","event":"独自审视简历"}]}
  ]
}

# 阶段 B：逐场生成，每完成一场推送一次（含完整 Scene，字段同 yaml-schema.md §5.3）
event: generation_start
data: {"phase": "GENERATION", "totalScenes": 8, "message": "开始逐场生成…"}

event: scene_generated
data: {
  "phase": "GENERATION",
  "sceneIndex": 1,
  "scene": {
    "sceneId": "s1",
    "heading": {"interior": true, "location": "出租屋", "timeOfDay": "夜"},
    "scriptBlocks": [
      {"type":"ACTION","text":"雨敲打着铁皮屋檐。林晚盘腿坐在地板上，膝头摊着简历。"},
      {"type":"DIALOGUE","character":"林晚","parenthetical":"画外音","line":"又一次……我都习惯了。"},
      {"type":"TRANSITION","text":"切至："}
    ],
    "sourceChapter": 1,
    "sourceText": "雨敲在铁皮屋檐上。林晚把简历又看了一遍……"
  }
}

# 全部完成：剧本已落库，返回 screenplayId（前端跳预览）
event: complete
data: {"phase": "COMPLETE", "screenplayId": "sp-2b9c", "sceneCount": 8}

# 任意阶段出错
event: error
data: {"code": 50001, "message": "LLM 调用失败，请重试"}
```

**流中错误码：** `50001`（LLM 调用失败）、`50002`（输出格式异常）、`50004`（LLM 超时）。

#### ⑨ GET `/api/screenplay` — 我的剧本列表

> 原型：`preview.html` 顶部"切换我的其它剧本"。

**响应 `data`：**
```json
{
  "screenplays": [
    { "screenplayId": "sp-2b9c", "novelId": "nv-7f3a", "title": "她比烟花寂寞",
      "screenplayType": "ANIME", "sceneCount": 8, "updatedAt": "2026-06-05T09:20:00Z" }
  ],
  "total": 1
}
```

#### ⑩ GET `/api/screenplay/{id}` — 获取完整剧本

> 原型：`preview.html` 进入页面拉取完整剧本（剧本渲染 + 大纲 + 人物/场景表均源于此）。返回体即 **YAML Schema 的 JSON 形态**（字段一一对应 `yaml-schema.md`）。

**响应 `data`：**
```json
{
  "screenplayId": "sp-2b9c",
  "novelId": "nv-7f3a",
  "schemaVersion": "1.0",
  "title": "她比烟花寂寞",
  "screenplayType": "ANIME",
  "plotSummary": "求职屡败的林晚在天台徘徊，终被一通录用电话挽回。",
  "characters": [
    { "name": "林晚", "role": "PROTAGONIST", "description": "27岁，求职屡屡碰壁的设计师",
      "firstAppearance": "第1章", "relationships": [ { "target": "母亲", "relation": "母女" } ] }
  ],
  "scenes": [
    { "sceneId": "s1",
      "heading": { "interior": true, "location": "出租屋", "timeOfDay": "夜" },
      "scriptBlocks": [
        { "type": "ACTION", "text": "雨敲打着铁皮屋檐。林晚盘腿坐在地板上。" },
        { "type": "DIALOGUE", "character": "林晚", "parenthetical": "画外音", "line": "又一次……我都习惯了。" },
        { "type": "TRANSITION", "text": "切至：" }
      ],
      "sourceChapter": 1,
      "sourceText": "雨敲在铁皮屋檐上。林晚把简历又看了一遍……" }
  ],
  "storylines": [
    { "name": "求职挣扎", "type": "MAIN", "events": [ { "scene": "s1", "event": "独自审视简历" } ] }
  ],
  "updatedAt": "2026-06-05T09:20:00Z"
}
```

**可能错误码：** `40401`（剧本不存在）、`40301`（非本人）。

#### ⑪ GET `/api/screenplay/{id}/scenes` — 场景表 / 大纲（轻量投影）

> 原型：`preview.html` 左侧大纲 + 「场景表」Tab（列：#、内/外、地点、时间、出场、源章）。为减负返回投影，不含完整动作/对白；详情走 ⑩。

**响应 `data`：**
```json
{
  "scenes": [
    { "sceneId": "s1", "heading": {"interior": true,  "location": "出租屋", "timeOfDay": "夜"},
      "charactersInScene": ["林晚"], "sourceChapter": 1 },
    { "sceneId": "s2", "heading": {"interior": false, "location": "天台",   "timeOfDay": "黄昏"},
      "charactersInScene": ["林晚"], "sourceChapter": 2 },
    { "sceneId": "s3", "heading": {"interior": true,  "location": "公司",   "timeOfDay": "日"},
      "charactersInScene": ["林晚", "陈经理"], "sourceChapter": 3 }
  ]
}
```

**可能错误码：** `40401`、`40301`。

#### ⑫ GET `/api/screenplay/{id}/characters` — 人物表

> 原型：`preview.html`「人物表」Tab（角色/重要度/身份描述/首次出场）+「关系图谱」加分项数据源。

**响应 `data`：**
```json
{
  "characters": [
    { "name": "林晚", "role": "PROTAGONIST", "description": "27岁，求职屡屡碰壁的设计师，敏感坚韧",
      "firstAppearance": "第1章",
      "relationships": [ { "target": "母亲", "relation": "母女" }, { "target": "陈经理", "relation": "录用关系" } ] },
    { "name": "陈经理", "role": "SUPPORTING", "description": "录用林晚的公司主管", "firstAppearance": "第3章",
      "relationships": [] }
  ]
}
```

**可能错误码：** `40401`、`40301`。

---

### 4.6 剧本打磨接口

> 原型：`scene-edit.html`。打磨即编辑 YAML 结构——两条路径：**手动编辑（⑬ PUT）**与 **AI 重生（⑭ SSE）**。
> 当前 MVP 已落地手动编辑 YAML、保存、取消与右侧实时预览；AI 重生与视觉化手法切换仍为扩展项。

#### ⑬ PUT `/api/screenplay/{id}/scenes/{sceneId}` — 手动编辑单场

> 原型：`scene-edit.html`「保存本场修改 ✓」。提交编辑后的整场 YAML 结构落库。
> 当前实现路径为 `PUT /api/screenplay/conversions/{conversionId}/chapters/{chapterIndex}/scenes/{sceneIndexInChapter}`，按转换任务、章节序号和章内场次定位已持久化场景。

**请求**（可编辑字段；`sourceChapter`/`sourceText` 为内部溯源字段，不作为打磨主体）：
```json
{
  "heading": { "interior": false, "location": "天台", "timeOfDay": "黄昏" },
  "scriptBlocks": [
    { "type": "ACTION", "text": "城市在脚下铺开，晚风掀动林晚的发。她走到栏杆边，指节因用力而泛白。" },
    { "type": "DIALOGUE", "character": "林晚", "parenthetical": "画外音", "line": "往下看一眼，就一眼……可那串电话还没挂断。" },
    { "type": "TRANSITION", "text": "切至：" }
  ]
}
```

**响应 `data`：** 落库后的完整场景 + 更新时间。
```json
{
  "scene": {
    "sceneId": "s2",
    "heading": { "interior": false, "location": "天台", "timeOfDay": "黄昏" },
    "scriptBlocks": [
      { "type": "ACTION", "text": "城市在脚下铺开，晚风掀动林晚的发。她走到栏杆边，指节因用力而泛白。" },
      { "type": "DIALOGUE", "character": "林晚", "parenthetical": "画外音", "line": "往下看一眼，就一眼……可那串电话还没挂断。" },
      { "type": "TRANSITION", "text": "切至：" }
    ],
    "sourceChapter": 2,
    "sourceText": "黄昏，城市在脚下铺开……"
  },
  "updatedAt": "2026-06-05T10:02:00Z"
}
```

**校验：** `scriptBlocks[type=DIALOGUE or VO].character` 必须存在于人物表（引用完整性，见 `yaml-schema.md` §8.3）；`scriptBlocks[].type` 须 ∈ `SHOT/ACTION/INSERT/SFX/DIALOGUE/VO/TRANSITION`。
**可能错误码：** `40001`（字段非法/人物引用悬空）、`40401`（剧本或场景不存在）、`40301`（非本人）。

#### ⑭ POST `/api/screenplay/{id}/scenes/{sceneId}/regenerate` — AI 重生单场（SSE）

> 原型：`scene-edit.html`「↻ 重新生成本场」+ 风格提示 + 视觉化手法切换 + 「携带全局上下文」勾选。
> 当前 MVP 尚未实现该接口；现阶段通过 YAML 手动编辑完成单场打磨。

**请求：**
```json
{
  "style": "更含蓄",
  "globalContext": true,
  "visualizedMethod": "VO"
}
```
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `style` | string | 否 | 风格提示（更含蓄/更戏剧化/更冷峻/自定义）；空则保持原风格 |
| `globalContext` | bool | 否 | 是否携带 plotSummary + 人物表 + 前场摘要重生（默认 true）。复用已存 `plotSummary`，**无需重跑阶段 A**（§3.2） |
| `visualizedMethod` | enum | 否 | 强制本场内心戏改用某手法（`VO/ACTION/DIALOGUE/CLOSE_UP`）；空则由模型自选 |

**前置校验：** `40302`（配额）、`40303`（全局预算）、`40401`（剧本/场景不存在）、`42901`（并发）。

**SSE 事件流：**
```
event: regeneration_start
data: {"sceneId": "s2", "message": "正在重新生成本场…"}

event: scene_generated
data: {"sceneId": "s2", "scene": { /* 完整 Scene，字段同 ⑬ 响应 */ }}

event: complete
data: {"sceneId": "s2", "updatedAt": "2026-06-05T10:05:00Z"}

event: error
data: {"code": 50001, "message": "LLM 调用失败，请重试"}
```

> 重生结果**自动落库**（覆盖该场）；前端右侧「渲染预览」即时刷新。

---

### 4.7 剧本导出接口

> 原型：`export.html`。YAML 为**核心交付**，可读文本为衍生渲染。两者均为文件下载（非 JSON 包装）。

#### ⑮ GET `/api/screenplay/{id}/export/yaml` — 导出 YAML（核心交付）

> 原型：`export.html`「↧ 下载 screenplay.yaml」。
> 当前实现路径为 `GET /api/screenplay/conversions/{conversionId}/yaml`，导出指定转换任务已持久化的最终 YAML。

**请求：** 路径参数 `id`。（预留 query `?download=true` 控制是否带 `attachment` 头。）

**响应头：**
```
Content-Type: application/x-yaml; charset=utf-8
Content-Disposition: attachment; filename="她比烟花寂寞.yaml"
```
**响应体：** 符合 `yaml-schema.md` 的完整 YAML 文本（即 `export.html` 左侧展示的内容，顶层含 `schemaVersion/title/screenplayType/plotSummary/characters/scenes/storylines`）。导出场景使用 `scriptBlocks` 表达有序剧本正文，不输出内部持久化/重生使用的 `sourceText` 原文片段，也不输出内部改编审计字段。

**可能错误码（以 JSON 包装返回，因无文件可下）：** `40401`、`40301`、`40101`。

#### ⑯ GET `/api/screenplay/{id}/export/text` — 导出可读剧本

> 原型：`export.html`「↧ 下载 screenplay.txt」。后端将 YAML 渲染为标准剧本排版文本（场景标题、动作行、角色名、对白、转场）。
> 当前 MVP 尚未实现该接口；导出页聚焦最终 YAML 的展示、复制与下载。

**请求：** 路径参数 `id`。（预留 query `?format=txt|md|fountain`，默认 `txt`；`md`/`fountain` 可保留排版，见 `yaml-schema.md` §10/§11。）

**响应头：**
```
Content-Type: text/plain; charset=utf-8
Content-Disposition: attachment; filename="她比烟花寂寞.txt"
```
**响应体（示例）：**
```
S1  内景 — 出租屋 — 夜

雨敲打着铁皮屋檐。林晚盘腿坐在地板上，膝头摊着一份被反复折叠的简历。

                林晚
              （画外音）
        又一次……我都习惯了。习惯，是最体面的认输。

                                              切至：
```

**可能错误码：** `40401`、`40301`、`40101`。

---

## 5. LLM 调用策略

> **MVP 范围**：仅实现**画面类剧本**（动画 / 短剧，共用影视格式：内景/外景、V.O.、镜头特写）。`screenplayType` 枚举保留 RADIO/THEATER，但**广播剧/话剧的生成规则列为扩展 TODO**——因其无画面/无镜头，内心戏需转旁白/音效/形体，规则与影视不同，留待后续分化。

### 5.1 Spring AI Alibaba 集成方式

- 框架用 **Spring AI Alibaba**（构建于 Spring AI，API 兼容 `ChatClient`，中文文档/示例更丰富）
- DeepSeek 经 **OpenAI 兼容适配器**接入：配置 `base-url` 指向 DeepSeek 官方 API（计费走 DeepSeek 官方）
- 用 `ChatClient` 统一调用；**结构化输出**用 `.entity(Class)`：由 Spring AI 的转换器自动向 prompt 注入目标 JSON Schema 并把响应反序列化为 Java 对象。
- **因此 prompt 只写"任务 + 规则"，不手写输出 JSON**（避免与转换器注入的 Schema 打架导致脏输出）；下文 prompt 仅列任务与规则，结构由 `.entity()` 负责。

### 5.2 转换管线总览

```
① 章节分割（纯代码，确定性）  rawContent → List<Chapter>
        │
        ▼
② 全局分析（LLM，自适应长度）
   ├─ 短小说（< 阈值，约 8k tokens）：全文一次分析 → AnalysisResult
   └─ 长小说（超阈值）：顺序滚动 Map + Reduce（见 5.3）
        · 按章【顺序】处理（不并行——前章影响后章理解）
        · 每章带"前文累积状态摘要(running digest)"做分析，产出该章局部结果并更新 digest
        · Reduce：合并去重人物、整合关系与线索、生成 plotSummary
   产出：AnalysisResult{ plotSummary, characters[], storylines[], scenes[] }
        其中 scenes[] 即【场景切分】结果，每个场景单元含
        { sceneId, sourceChapter, sourceText(原文片段), keyEvents }
        │
        ▼
③ 逐场生成（LLM，逐场）
   对每个场景单元【顺序】生成（以便把"前一场摘要"作为上下文）
   输入：plotSummary + 规范人物表 + 前一场摘要 + 本场 sourceText
   每生成完一场 → SSE 推送 scene_generated
   产出：List<Scene>
        │
        ▼
④ 组装 + 落库（Screenplay.contentJson）
```

> **场景切分归属②**：场景边界在全局分析阶段确定（长小说在每章 Map 时切分，Reduce 时顺序拼接并赋全局 `sceneId`）。这样③逐场生成只需消费已切好的 `sourceText` 单元，补齐了原 A/B 之间"文本如何变成场景"的缺环。

### 5.3 Prompt 模板设计（动态选择架构）

> **设计原则**：Prompt 模板按 `screenplayType` 动态选择，后端通过 `PromptTemplateRegistry` 管理。MVP 仅实现 `ANIME` 模板，其他类型预留接口便于扩展。

#### 5.3.1 模板选择逻辑

```
GenerationService.generateScene(screenplayType, context):
    template = PromptTemplateRegistry.get(screenplayType)  // 按 type 取模板
    return llmClient.entity(template.render(context), Scene.class)

PromptTemplateRegistry:
    ANIME   → AnimePromptTemplate      // MVP 实现
    FILM    → FilmPromptTemplate       // TODO
    SHORT   → ShortDramaPromptTemplate // TODO
    RADIO   → RadioPromptTemplate      // TODO
    THEATER → TheaterPromptTemplate    // TODO
```

#### 5.3.2 阶段 A Prompt（所有类型共用）

**A-1：分析 Prompt（短小说全文 / 长小说逐章 Map 共用）**

```
你是一位资深编剧与剧本分析专家。请分析下面的小说文本，提取：
剧情概要、人物（含身份、重要度、相互关系、首次出场章节）、
故事线索（主线/支线及其事件）、以及按"地点或时间变化"切分出的场景列表
（每个场景标注所属章节、原文片段、关键事件）。

要求：
- 人物名一律使用原文中的【规范写法】，同一人物不要出现多种称呼混用。
- 场景切分以"地点变化或时间跳跃"为界，宁可细分，便于后续逐场改编。

【仅长小说逐章 Map 时附带】
## 前文累积状态（用于保持跨章连贯，请在其基础上更新，勿丢失已知人物/线索）
{running_digest}

## 待分析文本（本章）
{chapter_text}
```

**A-2：Reduce 合并 Prompt（仅长小说）**

```
你是一位资深编剧。下面是同一部小说【逐章分析】得到的多份局部结果。
请合并为一份全局一致的分析：
- 跨章去重并统一人物（合并同一人物的不同称呼与信息）；
- 整合人物关系与故事线索，消解前后矛盾；
- 生成全书 plotSummary（200 字内）；
- 保持场景列表的【章节顺序】，赋予连续的全局场景序号。

## 各章局部结果
{partial_results}
```

#### 5.3.3 阶段 B Prompt（按类型分模板）

---

**【ANIME】动画剧本模板**（MVP 实现）

> 适用：TV动画单集（20-24min）、OVA、剧场版片段
> 特点：强调画面感、镜头语言、内心戏转 V.O. 或特写

```
你是一位资深动画编剧，正在把小说改编为【动画剧本】。请将"本场原文"转换为一个标准动画场景。

## 全局上下文
- 剧情概要：{plotSummary}
- 规范人物表（对白人物名必须严格沿用此处写法）：{characters_summary}
- 前一场摘要（用于承接，不要重复其内容）：{prev_scene_summary}

## 本场原文
{source_text}

## 转换规则
1. 场景标题：内景/外景 - 地点 - 时间（如：内景 - 教室 - 黄昏）
2. 动作行：描述【可见】的动作与视觉细节，适合动画表现。
3. 对白：角色名（可选括号情绪/动作提示）+ 台词；角色名严格沿用规范人物表。
4. **内心戏视觉化**（动画改编核心）：把心理描写/独白转为以下之一——
   - 画外音(V.O.)：适合深沉独白、回忆
   - 可见动作：用表情/肢体表现情绪
   - 镜头特写：聚焦眼神、手部等细节
   - 说出口的对白：把"心里想"转成小声嘀咕
5. "讲述"变"呈现"：如"她很绝望"→ 转为垂下眼帘、紧握双拳等可画面表现的动作。
6. 转场：在场景末尾标注切至/淡出等。

## 关于 scriptBlocks 字段
它是场景正文的唯一主体：动作、对白、转场都按阅读/演出顺序放入同一个列表。
内心戏视觉化结果必须直接体现在 `ACTION`、`DIALOGUE`、`VO`、`SHOT` 或 `INSERT` 等剧本正文块中，不输出内部改编日志。
```

---

**【FILM】影视剧本模板**（TODO 扩展）

> 适用：电影长片（90-120min）
> 特点：叙事宏大、镜头语言丰富、节奏舒缓

```
// TODO: 待实现
核心差异点（预研）：
- 场景标题：内景/外景 - 地点 - 时间（与动画类似）
- 内心戏处理：V.O. / 意象化镜头 / 氛围渲染
- 节奏控制：允许长镜头、留白、情绪铺垫
- 与动画差异：更写实、少夸张表情、更多意象化隐喻镜头
```

---

**【SHORT_DRAMA】短剧剧本模板**（TODO 扩展）

> 适用：竖屏短剧（2-5min/集）
> 特点：节奏快、冲突强、反转多、对白直白

```
// TODO: 待实现
核心差异点（预研）：
- 场景标题简化：场景号 - 地点
- 内心戏处理：直接转成对白或旁白，不玩镜头语言
- 节奏要求：每场必须有冲突/悬念/反转
- 时长控制：单场对白控制在 30s 内可演完
```

---

**【RADIO】广播剧剧本模板**（TODO 扩展）

> 适用：音频广播剧
> 特点：无画面、全靠声音、大量音效标注

```
// TODO: 待实现
核心差异点（预研）：
- 无场景标题，改用【音效转场】
- 内心戏处理：全转旁白/独白
- 动作行：改为【音效描述】（脚步声、开门声等）
- 强调：语气、停顿、背景音、BGM 切换
```

---

**【THEATER】话剧剧本模板**（TODO 扩展）

> 适用：舞台剧
> 特点：舞台指示、无镜头、形体动作

```
// TODO: 待实现
核心差异点（预研）：
- 场景标题：幕 - 场
- 内心戏处理：转对白/旁白/形体动作
- 动作行：改为【舞台指示】（走到台前、灯光聚焦等）
- 无镜头语言，强调舞台空间调度
```

### 5.4 模型选择与上下文策略

| 步骤 | 模型 | 说明 |
|---|---|---|
| 短小说全文分析 | DeepSeek-v4 Pro | 一次调用，求准 |
| 长小说逐章 Map | DeepSeek-v4 flash | 调用次数多，求省；输入=本章+running digest（有界） |
| 长小说 Reduce 合并 | DeepSeek-v4 Pro | 一次调用，输入是压缩摘要，求准 |
| 逐场生成 / 单场重生 | DeepSeek-v4 flash | 逐场顺序，带全局+前场上下文 + 风格提示 |

**上下文与 token 量级（修正此前错误估算）：**
- 中文约 **1 字 ≈ 0.6–1 token**。1 万字 ≈ 6k–10k tokens；20 万字（输入上限）≈ 120k–200k+ tokens。
- 故**全文单次分析不可作为长小说默认路径**：可能超限、且贵、且超长上下文易"中段遗漏"。改用 5.2 的自适应 Map-Reduce。
- **顺序、非并行**：Map 逐章串行，靠 running digest 携带前章信息；逐场生成串行，携带前一场摘要——以此保证跨章/跨场连贯。
- 每次 LLM 调用输入恒为"有界片段 + 压缩摘要"，与小说总长解耦，**支持任意长度小说**。

### 5.5 成本控制

- **缓存**：转换前按 `novelContentHash + screenplayType` 查 DB，命中则直接返回已存剧本，不调 LLM
- **配额**：每用户每日 token 上限（`User.dailyTokenQuota` + `quotaDate` 按天重置），超限拒绝
- **输入上限**：单次最大 20 万字
- **分块即省**：Map-Reduce 让每次调用只吃片段，避免长小说反复整本入参的浪费

---

## 6. 安全设计

| 风险 | 措施 |
|---|---|
| 未授权调用 / API 被乱刷 | **JWT 登录认证**：除注册/登录外所有接口需有效 token；每用户配额 + 限流 |
| 越权访问他人资源 | 资源归属校验：**novel 与 screenplay** 操作前均比对 `userId == 当前用户` |
| **批量注册绕过配额** | 注册按 IP 限流 + **全局每日 token 预算上限**（所有用户合计）兜底，防止"多开账号刷免费额度"烧光成本 |
| **登录暴力破解** | 登录失败按 IP/用户名限流 + 退避；连续失败临时锁定 |
| **并发任务耗尽资源** | 每用户同时仅允许 1 个进行中的转换任务；SSE 连接数与时长受限 |
| **超大文件上传** | 限制 multipart 体积（如 5MB）+ 解析后仍受 20 万字上限约束 |
| **提示注入（小说正文夹带指令）** | 小说文本仅作**数据**；依赖结构化输出约束 + 输出校验；LLM 结果不驱动任何特权操作 |
| 密码泄露 | BCrypt 加盐哈希存储，不存明文；注册校验最小密码强度 |
| API Key 泄露 | DeepSeek Key 存后端环境变量，前端不可见，不入库不入仓 |
| JWT 伪造/泄露 | 服务端密钥签名校验；设置合理过期时间；密钥经环境变量注入 |
| XSS | 前端渲染用户文本时转义 HTML；React 默认转义 |
| SQL 注入 | 统一用 Spring Data JPA 参数化查询，不手拼 SQL |
| 跨域 | 生产环境显式配置 CORS 白名单；开发环境用 Vite 代理 |
| 输入过大 | 后端限制单次输入最大 20 万字；超限返回错误 |
| LLM 输出不可控 | Spring AI Alibaba 结构化输出 + 后端校验；字段缺失降级处理而非崩溃 |

---

## 7. 目录结构

> **初版设计，仅作骨架参考**。随后续迭代必然调整（包与文件会增删），最终以实际代码为准。

```
lively-novel/
├── README.md                      # 交付物：项目说明、依赖列表、运行方式、demo 视频链接
├── docs/                          # 文档
│   ├── requirements-analysis.md   # 需求分析文档
│   ├── product-design.md          # 产品设计文档
│   ├── technical-design.md        # 技术方案文档（本文件）
│   └── yaml-schema.md             # YAML Schema 设计文档（题目要求交付物）
│
├── backend/                       # 后端 (Spring Boot)
│   ├── pom.xml
│   ├── src/main/java/com/livelynovel/
│   │   ├── LivelyNovelApplication.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── NovelController.java
│   │   │   └── ScreenplayController.java
│   │   ├── service/
│   │   │   ├── NovelService.java
│   │   │   ├── AnalysisService.java        # 全局分析（自适应 Map-Reduce）
│   │   │   ├── GenerationService.java      # 逐场生成（按 type 选择模板）
│   │   │   ├── CacheService.java
│   │   │   ├── LlmClient.java              # 封装 Spring AI Alibaba ChatClient
│   │   │   └── prompt/                     # Prompt 模板（动态选择）
│   │   │       ├── PromptTemplateRegistry.java    # 模板注册表
│   │   │       ├── PromptTemplate.java            # 模板接口
│   │   │       ├── AnimePromptTemplate.java       # 动画模板（MVP）
│   │   │       ├── FilmPromptTemplate.java        # 影视模板（TODO）
│   │   │       ├── ShortDramaPromptTemplate.java  # 短剧模板（TODO）
│   │   │       ├── RadioPromptTemplate.java       # 广播剧模板（TODO）
│   │   │       └── TheaterPromptTemplate.java     # 话剧模板（TODO）
│   │   ├── security/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   ├── JwtUtil.java
│   │   │   └── RateLimitInterceptor.java   # 配额 + 注册/登录限流
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── NovelRepository.java
│   │   │   └── ScreenplayRepository.java
│   │   ├── entity/                         # JPA 实体（落表）
│   │   │   ├── User.java
│   │   │   ├── Novel.java
│   │   │   └── Screenplay.java
│   │   ├── model/                          # 剧本内容结构（存于 contentJson，非表）
│   │   │   ├── Chapter.java
│   │   │   ├── ScreenplayContent.java
│   │   │   ├── Scene.java
│   │   │   ├── Character.java
│   │   │   ├── DialogueBlock.java
│   │   │   ├── Storyline.java
│   │   │   └── AnalysisResult.java
│   │   ├── dto/                            # 请求/响应体
│   │   ├── common/
│   │   │   └── ApiResponse.java            # 统一响应包装（§8.1）
│   │   ├── exception/
│   │   │   ├── BizException.java
│   │   │   └── GlobalExceptionHandler.java # 统一错误码映射（§8.2）
│   │   └── config/
│   │       └── AppConfig.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── prompts/                        # Spring AI prompt 模板（§5.3）
│   └── src/test/java/com/livelynovel/      # 单元/集成测试
│
├── frontend/                      # 前端 (React + Vite)
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── App.tsx
│       ├── main.tsx
│       ├── router/                # 路由配置
│       ├── pages/
│       │   ├── LoginPage.tsx
│       │   ├── RegisterPage.tsx
│       │   ├── ImportPage.tsx
│       │   ├── ConvertingPage.tsx
│       │   ├── PreviewPage.tsx
│       │   ├── SceneEditPage.tsx
│       │   └── ExportPage.tsx
│       ├── components/
│       │   ├── ScreenplayRenderer.tsx
│       │   ├── SceneOutline.tsx
│       │   ├── SceneEditor.tsx
│       │   └── CharacterTable.tsx
│       ├── services/
│       │   ├── api.ts
│       │   └── auth.ts            # token 存储 + 请求拦截器
│       └── types/
│           └── index.ts
│
├── data/                          # SQLite 数据文件（.gitignore 忽略）
└── .gitignore
```

---

## 8. 前后端通信协议

### 8.1 通用响应格式

**成功：**
```json
{
  "code": 0,
  "data": { ... },
  "message": "success"
}
```

**失败：**
```json
{
  "code": 40001,
  "data": null,
  "message": "小说文本不能为空"
}
```

### 8.2 错误码定义

| 错误码 | 含义 |
|---|---|
| 0 | 成功 |
| 40001 | 请求参数错误 |
| 40002 | 文本过长（超过 20 万字） |
| 40003 | 章节数不足（需 3 章以上） |
| 40004 | 用户名已存在 |
| 40005 | 密码不符合强度要求 |
| 40101 | 未登录 / 缺少 token |
| 40102 | token 无效或已过期 |
| 40103 | 用户名或密码错误 |
| 40301 | 无权访问该资源（越权） |
| 40302 | 超出每日配额 |
| 40303 | 系统全局额度已用尽（请稍后再试） |
| 40401 | 资源不存在（小说/剧本/场景） |
| 42901 | 请求过于频繁（限流） |
| 50001 | LLM 调用失败 |
| 50002 | LLM 输出格式异常 |
| 50003 | 内部服务错误 |
| 50004 | LLM 响应超时 |

---

## 9. 开发环境配置

### 9.1 后端

> 依赖：`spring-ai-alibaba-starter`（框架）+ OpenAI 兼容模型适配器；`base-url` 指向 DeepSeek 官方。

> ⚠️ 模型名 `deepseek-v4-*` 按需求方命名占位；**部署时以 DeepSeek 官方实际模型 id 为准**（核对后回填）。

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:sqlite:../data/livelynovel.db   # backend 模块启动时落到仓库根 data/ 目录
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 1                     # SQLite 单写者，连接池设小避免写锁竞争
      connection-init-sql: "PRAGMA journal_mode=WAL;"   # 开启 WAL，提升读写并发
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update                         # demo 期用 update；后续可换 Flyway 管理迁移
  servlet:
    multipart:
      max-file-size: 5MB                       # 上传体积上限（§6）
      max-request-size: 5MB
  mvc:
    async:
      request-timeout: 600000                  # SSE 长任务超时（10min），或在 SseEmitter 单独设
  ai:
    openai:
      base-url: https://api.deepseek.com        # DeepSeek 官方 API（OpenAI 兼容）
      api-key: ${DEEPSEEK_API_KEY}              # 环境变量读取
      chat:
        options:
          model: deepseek-v4-flash               # 默认模型，Pro 在代码中按需指定

app:
  llm:
    model-pro: deepseek-v4-pro
    model-flash: deepseek-v4-flash
    max-input-chars: 200000
    analysis-single-pass-threshold-tokens: 8000  # 低于此走全文单次，否则 Map-Reduce（§5.2）
  security:
    jwt-secret: ${JWT_SECRET}                    # 环境变量注入；务必使用足够长的随机密钥
    jwt-expire-seconds: 86400
  quota:
    daily-token-quota: 200000                    # 每用户每日 token 上限
    global-daily-token-budget: 5000000           # 全局每日预算上限，防批量注册刷额度（§6）
    max-concurrent-jobs-per-user: 1              # 每用户同时进行中的转换任务数

server:
  port: 8080
```

### 9.2 前端

```typescript
// vite.config.ts 代理配置
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```
