# Lively Novel

## 项目简介

小说和剧本是两种不同的表达方式。小说可以直接写心理活动、回忆和连续叙述；剧本需要明确场景、动作、对白、镜头、音效和转场，服务于后续动画化、影视化或声音演出。

Lively Novel 的目标不是简单摘要小说，而是把小说转换为结构化剧本初稿：

- 自动识别章节并切分场景
- 将小说片段逐场改编为动画剧本结构
- 用 `SHOT / ACTION / INSERT / SFX / DIALOGUE / VO / TRANSITION` 表达剧本正文
- 在转换过程中维护滚动全局状态，累积剧情概要、人物表和故事线
- 支持预览、逐场 YAML 打磨、保存和最终 YAML 导出

核心交付物：

- Demo 视频：`TODO: 在此填写 Demo 视频链接`
- [YAML Schema 文档](docs/yaml-schema.md)

---

## 功能完成度

### 已实现

- `.txt` 小说上传
- 已导入小说历史选择
- 作品标题持久化修改
- 章节识别与章节数校验
- 逐章切分场景
- SSE 实时转换事件流
- 中断后继续转换，跳过已完成场景
- 已完成转换历史回读
- 滚动全局状态更新与恢复事件展示
- 预览页：剧本、原文、场景表
- 打磨页：左侧 YAML 编辑，右侧渲染预览
- 导出页：展示、复制、下载 YAML
- YAML Schema 文档

当前实现重点聚焦 `ANIME` 动画剧本。`FILM`、`SHORT_DRAMA`、`RADIO`、`THEATER` 已保留枚举，但生成逻辑暂未开放。

### 暂未实现或后续扩展

- 非 `ANIME` 类型的完整生成规则
- 可读剧本文本导出
- 人物关系图谱、剧情时间线等可视化
- 单场 AI 重生与视觉化手法选择
- EPUB、Markdown、DOCX、PDF 等更多导入格式

## 原创实现范围

本项目的原创部分包括：

- 小说章节识别与章节数校验逻辑。
- 小说上传、历史记录、作品标题持久化。
- 小说章节到场景单元的切分流程。
- 整本转换任务编排、SSE 事件流和中断续跑。
- 动画剧本生成提示词与结构化解析修复逻辑。
- 七类 `scriptBlocks` 剧本正文块设计与渲染。
- 滚动全局状态设计，用于累积剧情概要、人物表和故事线。
- 单场 YAML 编辑、保存、预览和最终 YAML 导出。
- 前端导入、转换、预览、打磨、导出五个主页面及其交互逻辑。
- [docs/yaml-schema.md](docs/yaml-schema.md) 中定义的导出 Schema 与示例。

第三方框架和库只作为工程基础设施、UI 组件、数据库访问、模型调用适配和构建工具使用。

## 第三方依赖

### 前端

| 依赖 | 用途 |
|---|---|
| React 19 | 前端组件与页面状态管理。 |
| React DOM | 浏览器 DOM 渲染。 |
| TypeScript | 前端类型约束。 |
| Vite | 前端开发服务器与生产构建。 |
| Ant Design 6 | 基础 UI 组件。 |
| `@ant-design/icons` | 图标组件。 |
| ESLint、typescript-eslint | 前端代码规范检查。 |

### 后端

| 依赖 | 用途 |
|---|---|
| Java 17 | 后端运行环境。 |
| Spring Boot 3.3.5 | 后端应用框架。 |
| Spring Web | REST API 与 SSE 事件流。 |
| Spring Data JPA | 数据访问层。 |
| SQLite JDBC | SQLite 数据库驱动。 |
| Hibernate Community Dialects | SQLite 方言支持。 |
| Spring AI OpenAI 自动配置 | 调用 DeepSeek OpenAI 兼容接口。 |
| Spring AI ChatClient 自动配置 | 统一 ChatClient 调用入口。 |
| Knife4j / OpenAPI | API 文档页面。 |
| Spring Boot Starter Test | 后端测试。 |

### 模型服务

- DeepSeek OpenAI 兼容接口
- 默认配置：`https://api.deepseek.com`
- 默认模型：`deepseek-chat`

---

## 本地运行

### 1. 准备环境

需要安装：

- JDK 17+
- Maven 3.9+
- Node.js 20+
- npm

### 2. 配置后端环境变量

后端默认从环境变量读取模型密钥：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

也可以在本地创建 `backend/src/main/resources/application-local.yml` 覆盖配置。该文件只用于本地运行，不应提交到仓库。

示例：

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
```

SQLite 数据库默认写入：

```text
data/livelynovel.db
```

### 3. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

API 文档：

```text
http://localhost:8080/doc.html
http://localhost:8080/swagger-ui.html
```

### 4. 启动前端

```powershell
cd frontend
npm ci
npm run dev
```

前端默认地址以 Vite 输出为准，通常为：

```text
http://localhost:5173
```

---

## 主要使用流程

1. 在导入页上传 `.txt` 小说。
2. 修改或确认作品标题。
3. 选择剧本类型，当前建议使用 `ANIME`。
4. 点击开始分析，进入转换页。
5. 等待章节读取、场景切分和逐场生成事件。
6. 转换完成后进入预览页，查看剧本、原文和场景表。
7. 进入打磨页，编辑单场 YAML 并保存。
8. 进入导出页，复制或下载最终 YAML。

转换耗时取决于小说长度、切分场景数和模型响应速度。Demo 演示时可以先展示实时转换过程，再切换到已经转换完成的历史记录继续讲解预览、打磨和导出。

---

## YAML 输出结构

最终导出的 YAML 顶层结构如下：

```yaml
schemaVersion: "1.0"
title: "作品标题"
screenplayType: "ANIME"
plotSummary: "剧情概要"
characters: []
scenes: []
storylines: []
```

场景正文使用 `scriptBlocks` 表达：

```yaml
scriptBlocks:
- type: "SHOT"
  text: "家庭餐厅一角。午后的光从窗边斜照进来。"
- type: "ACTION"
  text: "温水坐在靠墙的位置，用手帕擦去额头的汗。"
- type: "DIALOGUE"
  character: "八奈见"
  line: "这样不行啦草介！"
- type: "VO"
  character: "温水"
  parenthetical: "画外音"
  line: "这时候就装作没看见吧。"
```

完整 Schema 见 [docs/yaml-schema.md](docs/yaml-schema.md)。

---

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/novel/upload` | 上传 `.txt` 小说并持久化。 |
| `GET` | `/api/novel` | 获取已导入小说列表。 |
| `GET` | `/api/novel/{id}/chapters` | 获取小说章节列表。 |
| `PUT` | `/api/novel/{id}/title` | 修改作品标题。 |
| `POST` | `/api/screenplay/convert` | 启动整本转换，返回 SSE 事件流。 |
| `GET` | `/api/screenplay/conversions/{conversionId}` | 获取转换详情与已生成场景。 |
| `GET` | `/api/screenplay/conversions/latest` | 获取某小说某剧本类型最近完成的转换。 |
| `PUT` | `/api/screenplay/conversions/{conversionId}/chapters/{chapterIndex}/scenes/{sceneIndexInChapter}` | 保存打磨后的单场剧本。 |
| `GET` | `/api/screenplay/conversions/{conversionId}/yaml` | 导出最终 YAML。 |

---

## 项目结构

```text
lively-novel/
├── backend/                 # Spring Boot 后端
├── frontend/                # React + Vite 前端
├── docs/
│   ├── requirements-analysis.md
│   ├── product-design.md
│   ├── technical-design.md
│   ├── yaml-schema.md       # YAML Schema 核心交付文档
│   └── prototype/           # 静态原型
├── data/                    # 本地 SQLite 数据目录，不提交
├── AGENT.md                 # 项目索引与协作规范
└── README.md
```

---

## 文档索引

- [需求分析](docs/requirements-analysis.md)
- [产品设计](docs/product-design.md)
- [技术设计](docs/technical-design.md)
- [YAML Schema](docs/yaml-schema.md)
- [静态原型](docs/prototype/)

---

## 注意事项

- `backend/src/main/resources/application-local.yml` 仅用于本地配置，不提交。
- `data/` 为本地数据库目录，不提交。
- `temp/` 可放本地测试素材，不提交。
- 当前核心交付为 YAML；可读文本、PDF、Fountain 等导出格式属于后续扩展。
