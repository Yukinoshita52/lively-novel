# Lively Novel — 技术方案文档

> 日期：2026-06-05
> 版本：v1.0
> 依赖：需求分析文档 v1.0、产品设计文档 v1.0

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
| HTTP 客户端 | OkHttp / WebClient | 用于调用 DeepSeek API，支持流式响应 |
| 前后端通信 | REST + SSE | 常规请求用 REST，流式生成用 SSE |

### 1.2 不引入的组件（72h 取舍）

| 组件 | 不引入理由 |
|---|---|
| 数据库 | 无持久化需求，转换结果在内存/前端状态中维护 |
| Redis | 无队列/缓存需求，单体足够 |
| 消息队列 | 用 SSE 流式响应替代异步队列，降低复杂度 |
| Docker | 单机开发，暂不需要容器化 |

---

## 2. 系统架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────┐
│                    前端 (React)                   │
│                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ 导入页    │ │ 预览页    │ │ 编辑页    │         │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘         │
│       │            │            │                 │
│       └────────┬───┘────────────┘                 │
│                │ REST / SSE                       │
└────────────────┼──────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│              后端 (Spring Boot)                   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │           Controller 层                    │   │
│  │  NovelController  ScreenplayController    │   │
│  └──────────────────┬───────────────────────┘   │
│                     │                            │
│  ┌──────────────────▼───────────────────────┐   │
│  │           Service 层                      │   │
│  │                                            │   │
│  │  ┌─────────────┐  ┌──────────────────┐   │   │
│  │  │ AnalysisSvc │  │ GenerationSvc    │   │   │
│  │  │ (阶段A:分析) │  │ (阶段B:逐场生成) │   │   │
│  │  └──────┬──────┘  └────────┬─────────┘   │   │
│  │         │                  │              │   │
│  │  ┌──────▼──────────────────▼─────────┐   │   │
│  │  │         LlmClient                  │   │   │
│  │  │  (DeepSeek API 调用, 流式响应)     │   │   │
│  │  └────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │           Model 层                        │   │
│  │  Novel / Chapter / Screenplay / Scene     │   │
│  │  Character / Dialogue / Action / ...      │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### 2.2 架构说明

- **单体架构**：Spring Boot 单应用，前后端分离部署
- **无状态**：不存储用户数据，每次转换独立运行
- **流式响应**：转换过程通过 SSE 推送进度和中间结果

---

## 3. 核心数据模型

### 3.1 领域模型

```
Novel (小说)
 ├── title: String
 ├── chapters: List<Chapter>
 │    ├── chapterIndex: int
 │    ├── title: String
 │    └── content: String
 └── metadata: NovelMetadata
      ├── totalChapters: int
      └── totalWordCount: int

Screenplay (剧本)
 ├── title: String
 ├── screenplayType: ScreenplayType (FILM|SHORT_DRAMA|RADIO|THEATER)
 ├── characters: List<Character>
 │    ├── name: String
 │    ├── role: String (PROTAGONIST|SUPPORTING|MINOR)
 │    ├── description: String
 │    └── firstAppearance: String (章节.场景)
 ├── scenes: List<Scene>
 │    ├── sceneId: String
 │    ├── heading: SceneHeading
 │    │    ├── interior: boolean
 │    │    ├── location: String
 │    │    └── timeOfDay: String
 │    ├── actionLines: List<String>
 │    ├── dialogueBlocks: List<DialogueBlock>
 │    │    ├── character: String
 │    │    ├── parenthetical: String (可选)
 │    │    └── line: String
 │    ├── transitions: List<Transition>
 │    ├── sourceChapter: int
 │    └── sourceText: String (原文片段)
 └── storylines: List<Storyline>
      ├── name: String
      ├── type: String (MAIN|SUB)
      └── events: List<String>
```

### 3.2 转换管线数据流

```
原始文本 (String)
    │
    ▼ [章节分割]
List<Chapter>
    │
    ▼ [全局分析 - 阶段A]
AnalysisResult { characters, scenes, storylines, plotSummary }
    │
    ▼ [逐场生成 - 阶段B，逐场景调用 LLM]
List<Scene>  ← 每生成一个场景即通过 SSE 推送前端
    │
    ▼ [组装]
Screenplay
    │
    ├──▶ YAML 导出
    └──▶ 可读文本导出
```

---

## 4. API 设计

### 4.1 接口列表

| 方法 | 路径 | 说明 | 响应类型 |
|---|---|---|---|
| POST | `/api/novel/upload` | 上传 txt 文件 | JSON |
| POST | `/api/novel/parse` | 粘贴文本并解析章节 | JSON |
| GET | `/api/novel/{id}/chapters` | 获取章节列表 | JSON |
| POST | `/api/screenplay/convert` | 提交转换任务 | SSE |
| GET | `/api/screenplay/{id}` | 获取完整剧本 | JSON |
| PUT | `/api/screenplay/{id}/scenes/{sceneId}` | 编辑单场 | JSON |
| POST | `/api/screenplay/{id}/scenes/{sceneId}/regenerate` | 重新生成本场 | SSE |
| GET | `/api/screenplay/{id}/export/yaml` | 导出 YAML | YAML |
| GET | `/api/screenplay/{id}/export/text` | 导出可读文本 | Text |
| GET | `/api/screenplay/{id}/characters` | 获取人物表 | JSON |
| GET | `/api/screenplay/{id}/scenes` | 获取场景表 | JSON |

### 4.2 核心接口详细设计

#### POST `/api/novel/parse` — 粘贴文本解析

**请求：**
```json
{
  "text": "第一章 风起...\n第二章 云涌...\n第三章 ...",
  "title": "我的小说"
}
```

**响应：**
```json
{
  "novelId": "uuid-xxx",
  "title": "我的小说",
  "totalChapters": 3,
  "totalWordCount": 15000,
  "chapters": [
    { "chapterIndex": 1, "title": "风起", "wordCount": 5000 },
    { "chapterIndex": 2, "title": "云涌", "wordCount": 5200 },
    { "chapterIndex": 3, "title": "...", "wordCount": 4800 }
  ]
}
```

#### POST `/api/screenplay/convert` — 提交转换（SSE 流式）

**请求：**
```json
{
  "novelId": "uuid-xxx",
  "screenplayType": "SHORT_DRAMA"
}
```

**SSE 事件流：**
```
event: analysis_start
data: {"phase": "ANALYSIS", "message": "正在全局分析..."}

event: analysis_progress
data: {"phase": "ANALYSIS", "progress": 0.5, "message": "提取人物关系..."}

event: analysis_complete
data: {"phase": "ANALYSIS", "characters": [...], "scenes": [...], "storylines": [...]}

event: generation_start
data: {"phase": "GENERATION", "message": "开始逐场生成..."}

event: scene_generated
data: {"phase": "GENERATION", "sceneIndex": 1, "scene": {"sceneId": "s1", "heading": {...}, ...}}

event: scene_generated
data: {"phase": "GENERATION", "sceneIndex": 2, "scene": {...}}

event: complete
data: {"phase": "COMPLETE", "screenplayId": "uuid-yyy"}
```

#### POST `/api/screenplay/{id}/scenes/{sceneId}/regenerate` — 重新生成单场（SSE）

**请求：**
```json
{
  "style": "更含蓄",
  "globalContext": true
}
```

**SSE 事件流：**
```
event: regeneration_start
data: {"sceneId": "s7", "message": "正在重新生成..."}

event: scene_generated
data: {"sceneId": "s7", "scene": {...}}

event: complete
data: {"sceneId": "s7"}
```

---

## 5. LLM 调用策略

### 5.1 Prompt 设计

#### 阶段 A：全局分析 Prompt

```
你是一位资深编剧和剧本分析专家。请分析以下小说文本，提取结构化信息。

## 小说文本
{chapters_text}

## 请输出以下 JSON 格式
{
  "plotSummary": "剧情概要（200字内）",
  "characters": [
    {
      "name": "角色名",
      "role": "PROTAGONIST|SUPPORTING|MINOR",
      "description": "角色描述",
      "relationships": [{"target": "另一角色", "relation": "关系描述"}],
      "firstAppearance": "第X章"
    }
  ],
  "scenes": [
    {
      "location": "地点",
      "interior": true/false,
      "timeOfDay": "时间",
      "sourceChapter": 1,
      "keyEvents": ["关键事件"]
    }
  ],
  "storylines": [
    {
      "name": "线索名",
      "type": "MAIN|SUB",
      "events": ["事件1", "事件2"]
    }
  ]
}
```

#### 阶段 B：逐场生成 Prompt

```
你是一位资深编剧。请根据以下信息，将小说片段转换为标准剧本格式。

## 全局上下文
- 剧本类型：{screenplayType}
- 剧情概要：{plotSummary}
- 出场人物：{characters_summary}

## 当前场景的小说原文
{source_text}

## 转换规则
1. 场景标题格式：内景/外景 - 地点 - 时间
2. 动作行描述可见的动作和视觉细节
3. 对白格式：角色名（括号提示可选）→ 台词
4. **内心戏必须视觉化**：将心理描写转为以下形式之一：
   - 画外音 (V.O.)
   - 可见动作（如：她攥紧了拳头）
   - 对白（将内心想法说出）
   - 细节特写（如：镜头推近她泛红的眼眶）
5. "讲述"变"呈现"：将"她很绝望"转为可拍摄的动作

## 输出格式（JSON）
{
  "heading": {"interior": true, "location": "地点", "timeOfDay": "时间"},
  "actionLines": ["动作行1", "动作行2"],
  "dialogueBlocks": [
    {"character": "角色", "parenthetical": "提示", "line": "台词"}
  ],
  "transitions": ["CUT TO:"],
  "visualizedInnerThoughts": [
    {"original": "原文内心描写", "method": "VO|ACTION|DIALOGUE|CLOSE_UP", "result": "转换结果"}
  ]
}
```

### 5.2 调用策略

| 场景 | 模型 | 策略 |
|---|---|---|
| 全局分析 | DeepSeek-v4 Pro | 全文一次性传入，JSON 输出 |
| 逐场生成 | DeepSeek-v4 flash | 按场景分段调用，流式返回，更经济 |
| 单场重生 | DeepSeek-v4 flash | 带全局上下文 + 风格提示，流式返回 |

### 5.3 上下文窗口管理

- DeepSeek-v4 支持长上下文（128K tokens），3-5 章小说（1-3 万字 ≈ 2-4K tokens 中文）不会超限
- 阶段 B 每次调用传入：全局分析摘要 + 当前场景原文 + 前后场景摘要（保持连贯）

---

## 6. 安全设计

| 风险 | 措施 |
|---|---|
| API Key 泄露 | Key 存于后端环境变量，前端不可见；不提交到代码仓库 |
| XSS | 前端渲染用户文本时转义 HTML；React 默认转义 |
| 输入过大 | 后端限制单次输入最大 10 万字；超限返回错误提示 |
| LLM 输出不可控 | 后端校验 JSON 结构；字段缺失时降级处理而非崩溃 |

---

## 7. 目录结构

```
lively-novel/
├── docs/                          # 文档
│   ├── requirements-analysis.md   # 需求分析文档
│   ├── product-design.md          # 产品设计文档
│   ├── technical-design.md        # 技术方案文档（本文件）
│   └── yaml-schema.md             # YAML Schema 设计文档（待创建）
│
├── backend/                       # 后端 (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/com/livelynovel/
│       ├── LivelyNovelApplication.java
│       ├── controller/
│       │   ├── NovelController.java
│       │   └── ScreenplayController.java
│       ├── service/
│       │   ├── NovelService.java
│       │   ├── AnalysisService.java
│       │   ├── GenerationService.java
│       │   └── LlmClient.java
│       ├── model/
│       │   ├── Novel.java
│       │   ├── Chapter.java
│       │   ├── Screenplay.java
│       │   ├── Scene.java
│       │   ├── Character.java
│       │   ├── DialogueBlock.java
│       │   └── AnalysisResult.java
│       └── config/
│           └── AppConfig.java
│
├── frontend/                      # 前端 (React + Vite)
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── App.tsx
│       ├── main.tsx
│       ├── pages/
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
│       │   └── api.ts
│       └── types/
│           └── index.ts
│
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
| 40002 | 文本过长（超过 10 万字） |
| 40003 | 章节数不足（需 3 章以上） |
| 50001 | LLM 调用失败 |
| 50002 | LLM 输出格式异常 |
| 50003 | 内部服务错误 |

---

## 9. 开发环境配置

### 9.1 后端

```yaml
# application.yml
llm:
  base-url: https://api.deepseek.com    # 可替换为七牛云 AI 网关
  api-key: ${DEEPSEEK_API_KEY}           # 环境变量读取
  model-pro: deepseek-v4-pro
  model-flash: deepseek-v4-flash
  max-input-chars: 100000

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
