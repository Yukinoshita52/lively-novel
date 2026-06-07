# Lively Novel — 剧本 YAML Schema 设计文档

> 日期：2026-06-05
> 版本：v1.0
> 依赖：需求分析文档 v1.0、产品设计文档 v1.0、技术方案文档 v1.4
>
> **本文档是题目明确要求的核心交付物之一**：定义"小说→剧本"转换产物的 YAML Schema，并逐项说明设计原因。

---

## 0. 阅读指引

| 你想了解 | 直接看 |
|---|---|
| 为什么用 YAML 而不是 JSON / 自由文本 | §2 |
| Schema 背后的设计哲学（"设计原因"总纲） | §3 |
| 完整结构长什么样 | §4 总览 + §7 完整示例 |
| 每个字段为什么这样设计 | §5 逐字段详解 |
| 枚举值有哪些、含义是什么 | §6 |
| 怎么校验一份剧本合不合法 | §8 |
| 为什么不采用别的方案 | §9 取舍 |
| 以后怎么扩展（新剧本类型等） | §10 |

---

## 1. 文档目的

题目要求：

> 能将 3 个章节以上的小说文本自动转换为**结构化剧本（YAML 格式）**，让作者可以快速获得**可编辑、可进一步打磨**的剧本初稿。请额外写一篇文档，**定义剧本的 YAML Schema**，并**说明该 Schema 的设计原因**。

因此本 Schema 的设计目标，不是"随便定义一个能存数据的格式"，而是要让产物同时满足三个硬约束：

1. **结构化**——每一个剧本语义单元（场景标题、动作、对白、转场……）都是机器可寻址的独立字段，而非埋在散文里的纯文本。
2. **可编辑**——作者能在结构上精准定位并修改某一场、某一句对白、某一处内心戏的视觉化方式。
3. **可进一步打磨**——产物保留"从哪来、怎么转的"的溯源信息，使人机协作（手动改 / AI 重生）成为可能，而不是一次性的黑箱输出。

这三点贯穿了下文每一个字段的设计取舍。

---

## 2. 载体选型：为什么是 YAML

题目指定了 YAML。但"为什么 YAML 适合剧本"本身就是 Schema 设计的第一个理由，值得讲清楚——因为它直接影响了下面字段的组织方式。

### 2.1 YAML vs. 其他候选

| 候选 | 优点 | 对本场景的不足 |
|---|---|---|
| **YAML** ✅ | 人类可读、支持嵌套与列表、注释友好、对中文无转义负担、缩进即结构 | 对大文件解析略慢（本场景无压力） |
| JSON | 程序友好、生态广 | 大量引号/逗号噪音，中文与多行文本（动作行常很长）可读性差，无注释 |
| XML | 强 Schema 校验（XSD） | 标签冗长，剧本这种"列表套对象"结构写起来极其臃肿 |
| 纯文本 / Fountain | 贴近传统剧本书写习惯 | **非结构化**，无法稳定地程序化定位"第 7 场的第 2 句对白"，违背题目"结构化"硬要求 |
| Final Draft (.fdx) | 行业标准 | 二进制/复杂 XML，闭源工具绑定，不适合作为开放交付物 |

### 2.2 YAML 与剧本结构的天然契合

剧本本质上是**"列表的列表"**：一部剧本是场景的有序列表，每个场景是动作行与对白块的有序列表。YAML 的 `-` 列表语法 + 缩进嵌套，几乎是这种结构的最直观表达：

```yaml
scenes:                    # 场景列表（有序）
  - sceneId: s1            # 一个场景
    scriptBlocks:          # 剧本正文块列表（有序）
      - type: ACTION
        text: 雨敲打着铁皮屋檐。
      - type: ACTION
        text: 林晚盘腿坐在地板上。
      - type: DIALOGUE
        character: 林晚
        line: 又一次……
```

**有序性是关键**：剧本的场景顺序、场景内动作与对白的先后顺序，承载着叙事节奏，绝不能丢。YAML 列表天然保序，这是我们选择"列表"而非"字典"来组织场景与台词的根本原因（详见 §5.3）。

---

## 3. 设计原则（"设计原因"总纲）

整个 Schema 由以下六条原则推导而来。后续每个字段的设计，都能回溯到这里的某一条或几条。

### 原则一：语义单元独立成字段（结构化优先）

剧本的每个**专业要素**——场景标题（heading）、动作行（action）、角色提示（cue）、括号提示（parenthetical）、对白（dialogue）、转场（transition）——都拆成独立字段，放入有序的 `scriptBlocks`，**绝不合并为一段自由文本**。

> **为什么**：对应用户痛点 #1「格式是黑箱」。小说作者不懂这些要素如何排版。把它们结构化后，**排版逻辑交给渲染器，作者只需关心内容**。同时结构化是题目"可编辑"的前提——只有字段独立，才能"只改这一句对白"而不破坏其余部分。

### 原则二：内心戏必须视觉化进剧本正文

把心理描写/独白转成动作、对白、画外音的过程，是本项目最核心的 AI 价值，也是改编最难、最容易出错的一步。导出的 YAML 不单独暴露审计字段，而是要求转换结果直接落入 `scriptBlocks`：内心活动要么成为可见动作，要么成为对白/画外音，要么成为转场前后的画面节奏。

> **为什么**：最终交付物是剧本，不是 AI 改编日志。作者和评委最需要看到的是"内心戏已经被改成可演、可读的剧本内容"。审计与重生所需的中间信息可以留在后端内部，但不进入导出的剧本 YAML。

### 原则三：剧本交付与内部溯源分离

导出的剧本 YAML 只保留对剧本阅读和结构定位有价值的 `sourceChapter`，不输出内部生成与重生使用的 `sourceText` 原文片段。

> **为什么**：最终交付物是"剧本的 YAML"，不是"小说原文与剧本的混合包"。`sourceText` 对生成、打磨、重生有价值，应保存在后端内部持久化数据中；但它不属于剧本正文，导出后会稀释剧本感，也会把版权原文混入交付文件。`sourceChapter` 足以支撑场景来源定位，同时保持 YAML 聚焦剧本本身。

### 原则四：跨章一致性靠全局表保证

人物不是散落在各场景里，而是抽取到顶层的 `characters` 全局列表，使用**规范化的统一命名**；各场景对白只**引用**人物名。

> **为什么**：对应用户痛点 #6「跨章一致性」。3+ 章的小说里，同一个人可能有"林晚 / 小林 / 她"等多种称呼。若每场各写各的，剧本人物会混乱。全局表 + 引用，保证了"一个人物只有一个规范名"，这是"成体系"的体现。

### 原则五：标准要素对齐行业惯例

字段命名与划分，对齐国际通行的剧本格式（Fountain / 标准好莱坞剧本格式）的概念：scene heading（内外景-地点-时间）、action、character cue、parenthetical、dialogue、transition。

> **为什么**：① 产物专业、可信，评委与真实编剧一看就懂；② 便于未来导出为 Fountain / PDF 等行业格式（见 §10）；③ 降低我们自己造概念的认知成本与出错概率。

### 原则六：渐进可扩展，但 MVP 不超载

用枚举（`screenplayType`、`role`、`scriptBlocks.type`、`storyline.type`）为未来扩展预留位置，但**当前只填必要字段**，可选字段明确标注。Schema 既能表达 MVP，又不会因为"为未来留余地"而变得臃肿。

> **为什么**：72h 比赛，必须克制。但剧本类型（动画/影视/短剧…）、视觉化手法等显然会增长，用枚举而非硬编码，能让扩展只是"加一个枚举值"，而非"改结构"。

---

## 4. Schema 总览

> 当前实现说明：后端内部仍会保存 `sourceText`、`visualizedInnerThoughts` 等生成/审计字段，用于回读、调试、继续转换与未来重生；但最终导出的 YAML 只遵循本章结构。导出时以 `scriptBlocks` 作为剧本正文唯一主体，不输出内部原文片段或改编日志。

### 4.1 顶层结构

一份导出的剧本 YAML，顶层包含**元数据**、**全局分析产物**、**场景列表**三部分：

```
screenplay (根)
├── 元数据
│   ├── schemaVersion         Schema 版本（向后兼容用）
│   ├── title                 作品标题
│   └── screenplayType        剧本类型（枚举）
│
├── 全局分析产物（阶段 A）
│   ├── plotSummary           全局剧情概要
│   ├── characters[]          全局人物表（规范命名，跨章一致）
│   └── storylines[]          故事线索（主线/支线及事件）
│
└── 逐场生成产物（阶段 B）
    └── scenes[]              场景列表（有序，剧本主体）
```

### 4.2 结构树（含所有字段）

```
screenplay
├── schemaVersion: string                    # 如 "1.0"
├── title: string
├── screenplayType: enum                     # ANIME|FILM|SHORT_DRAMA|RADIO|THEATER
├── plotSummary: string                      # ≤ 200 字全局概要
│
├── characters: list                         # 全局人物表
│   └── (item)
│       ├── name: string                     # 规范名（唯一标识，对白引用它）
│       ├── role: enum                        # PROTAGONIST|SUPPORTING|MINOR
│       ├── description: string               # 身份/性格简述
│       ├── firstAppearance: string          # 首次出场章节，如 "第1章"
│       └── relationships: list              # 与其他人物的关系
│           └── (item)
│               ├── target: string           # 对方人物的规范名
│               └── relation: string         # 关系描述，如 "母女"
│
├── scenes: list                             # 场景列表（剧本主体，有序）
│   └── (item)
│       ├── sceneId: string                  # 场景唯一 id，如 "s1"
│       ├── heading: object                  # 场景标题（结构化）
│       │   ├── interior: bool               # true=内景, false=外景
│       │   ├── location: string             # 地点
│       │   └── timeOfDay: string            # 时间，如 "夜"/"黄昏"
│       ├── scriptBlocks: list               # 剧本正文块（有序）
│       │   └── (item)
│       │       ├── type: enum               # ACTION|DIALOGUE|TRANSITION
│       │       ├── text: string?            # 动作/转场正文
│       │       ├── character: string?       # 对白说话人
│       │       ├── parenthetical: string?   # 可选，括号提示
│       │       └── line: string?            # 台词正文
│       └── sourceChapter: int               # 来源章节：来自第几章
│
└── storylines: list                         # 故事线索
    └── (item)
        ├── name: string                     # 线索名，如 "求职挣扎"
        ├── type: enum                        # MAIN|SUB
        └── events: list                     # 该线索上的事件节点
            └── (item)
                ├── scene: string            # 关联的 sceneId
                └── event: string            # 事件描述
```

---

## 5. 字段逐项详解（含设计原因）

> 标注约定：**必填** / *可选*；类型；约束；**设计原因**。

### 5.1 顶层元数据

#### `schemaVersion` — **必填**，string

- 约束：语义化版本号，当前固定 `"1.0"`。
- **设计原因**：剧本数据会被持久化、被导出、可能被外部工具消费。一旦 Schema 演进（新增字段、改枚举），消费方需要知道"这份数据按哪一版规则解读"。预留版本号是**廉价的前向保险**——现在加一行，未来省一次破坏性迁移。

#### `title` — **必填**，string

- 约束：非空。来自用户在导入页输入的作品标题。
- **设计原因**：剧本的身份标识，导出文件名、渲染视图标题都依赖它。

#### `screenplayType` — **必填**，enum

- 取值：`ANIME | FILM | SHORT_DRAMA | RADIO | THEATER`（见 §6.1）。
- **设计原因**：剧本类型**决定生成策略与渲染规则**（动画强调镜头语言、短剧强调快节奏冲突、广播剧无画面……见技术方案 §5.3 的模板动态选择）。把它作为顶层字段而非埋在别处，是因为它是**整份剧本的全局属性**，且缓存键（`contentHash + screenplayType`）依赖它。用枚举而非自由字符串，保证生成端与渲染端对类型的理解一致。

---

### 5.2 `characters` — 全局人物表

> 对应 **原则四（跨章一致性）**。这是 3+ 章小说"成体系"的关键。

#### 为什么人物要全局抽取，而不是写在每个场景里？

如果人物信息散落在各场景（比如每场各自写"林晚：27岁设计师"），会有三个问题：① 信息重复冗余；② 多场之间容易不一致（这一场叫"林晚"，那一场叫"小林"）；③ 无法生成"人物表"这种全局视图。

**所以：人物在顶层定义一次，场景对白只引用 `name`。** 这本质上是数据库范式里的"消除冗余、单一数据源"思想，套用在剧本上。

#### 字段详解

| 字段 | 必填 | 类型 | 说明与设计原因 |
|---|---|---|---|
| `name` | 必填 | string | **人物的规范名，全剧唯一，充当人物的主键**。所有 `scriptBlocks[].character` 与 `relationships.target` 都引用它。这是跨章一致性的技术锚点——AI 在全局分析阶段统一称呼后写入这里，逐场生成时严格沿用。 |
| `role` | 必填 | enum | `PROTAGONIST/SUPPORTING/MINOR`（见 §6.2）。**设计原因**：重要度是渲染（主角加粗）、可视化（关系图谱节点大小）、以及作者快速理解人物层级的依据。用三档枚举而非数字权重，是因为"主角/配角/龙套"是创作者熟悉的离散概念，过细的量化反而无意义。 |
| `description` | 必填 | string | 身份/性格简述。**设计原因**：给作者和后续 AI 调用提供人物的"人设上下文"，逐场生成时作为 prompt 的一部分，保证人物言行一致。 |
| `firstAppearance` | 必填 | string | 首次出场章节，如 `"第1章"`。**设计原因**：帮助作者定位人物引入点；也是可视化"人物随时间登场"的数据基础。用字符串而非整数，是为了兼容"序章""番外"等非数字章节名。 |
| `relationships` | *可选* | list | 人物关系列表，每项 `{target, relation}`。**设计原因**：人物关系是剧本的骨架，也是加分项「人物关系图谱」的直接数据源（`target` 为边的另一端，`relation` 为边的标签）。设为可选，是因为龙套人物通常无需关系；MVP 阶段即便不画图谱，这份数据也已备好。 |

```yaml
characters:
  - name: 林晚
    role: PROTAGONIST
    description: 27岁，求职屡屡碰壁的设计师，敏感坚韧
    firstAppearance: 第1章
    relationships:
      - target: 母亲
        relation: 母女
      - target: 陈经理
        relation: 录用关系
```

---

### 5.3 `scenes` — 场景列表（剧本主体）

> 对应 **原则一、二、三、五**。这是整份 Schema 的核心，承载了项目几乎所有的差异化价值。

#### 为什么 scenes 是"列表"而不是"字典"？

剧本的**场景顺序即叙事顺序**，不可乱序。YAML 列表保序，字典不保证。同理，场景内的 `scriptBlocks` 也是列表——一句对白在一个动作之前还是之后，决定了演出的节奏。**保序是这里所有"列表"选型的根本原因。**

#### 5.3.1 `sceneId` — **必填**，string

- 约束：全剧唯一，如 `s1`、`s2`。由全局分析阶段切分场景时顺序赋予。
- **设计原因**：场景的稳定标识。**编辑/重生单场的 API（`PUT/POST .../scenes/{sceneId}`）依赖它定位**；`storylines.events.scene` 也引用它把事件挂到场景上。用 `s1` 这种短 id 而非数组下标，是因为下标在插入/删除场景后会变化，而 id 稳定——这对"可编辑"至关重要。

#### 5.3.2 `heading` — **必填**，object：场景标题（结构化）

```yaml
heading:
  interior: true       # 内景
  location: 出租屋
  timeOfDay: 夜
```

- **设计原因（关键决策）**：标准剧本的场景标题形如 `内景 - 出租屋 - 夜`（INT. APARTMENT - NIGHT）。我们**没有把它存成一个字符串 `"内景 - 出租屋 - 夜"`，而是拆成三个字段**。原因：
  - ① **可编辑性**——作者改地点时只动 `location`，不必解析字符串；
  - ② **可查询/统计**——能轻松回答"有多少场夜戏""出租屋出现在哪几场"，这是场景表与可视化的数据基础；
  - ③ **渲染灵活**——内/外景用 `interior` 布尔值表达，渲染时可按不同剧本类型/语言习惯（中文"内景"、英文"INT."）自由拼装，而不被存储格式锁死。
- `interior` 用布尔而非字符串枚举，是因为内/外景本质是二元的（标准格式里偶有 INT./EXT. 混合场，可在未来扩展为枚举，当前 MVP 二元足够）。

#### 5.3.3 `scriptBlocks` — **必填**，list：剧本正文块

```yaml
scriptBlocks:
  - type: ACTION
    text: 雨敲打着铁皮屋檐。
  - type: DIALOGUE
    character: 林晚
    parenthetical: (画外音)
    line: 又一次……我都习惯了。
  - type: TRANSITION
    text: 切至：
```

> 对应 **痛点 #2「对白埋在散文里」** 和 **痛点 #5「叙述没有画面感」**。这是把动作、对白、转场放回剧本阅读顺序中的结果。

| 子字段 | 必填 | 类型 | 说明与设计原因 |
|---|---|---|---|
| `type` | 必填 | enum | `ACTION/DIALOGUE/TRANSITION`。用于标明这一块如何渲染，而不是把整场拆成三组互不相干的数据。 |
| `text` | 条件必填 | string | `ACTION` 与 `TRANSITION` 使用。动作描述可见画面，转场描述镜头/场景切换。 |
| `character` | 条件必填 | string | `DIALOGUE` 使用。说话人应来自 `characters` 表中的规范名（引用完整性，见 §8 校验规则）。 |
| `parenthetical` | *可选* | string | `DIALOGUE` 使用。括号提示，如 `(画外音)`、`(冷笑)`、`(压低声音)`。 |
| `line` | 条件必填 | string | `DIALOGUE` 使用。台词正文。 |

- **为什么不用 `actionLines` / `dialogueBlocks` / `transitions` 三组并列字段作为导出正文**：剧本不是素材分类表，而是按阅读和演出顺序推进的文本。`scriptBlocks` 既保留了动作、对白、转场的结构化类型，又把它们放在同一个有序列表里，更接近真实剧本。
- **当前导出约束**：最终 YAML 的场景正文只输出 `scriptBlocks`。即使后端 DTO 为兼容旧链路仍可能保留 `actionLines`、`dialogueBlocks`、`transitions` 或 `visualizedInnerThoughts`，这些都不作为导出正文并列输出。
- **为什么对白仍是对象块而非 `"林晚：又一次……"` 字符串**：一个对白块捆绑了"说话人 + 提示 + 台词"三位一体，这是剧本对白的最小完整语义单元。拆开存会丢失"这句提示属于这句台词"的绑定关系。

#### 5.3.4 `sourceChapter` — **必填**，int：来源章节

- 本场来自原著第几章。
- **设计原因**：① 场景表/大纲展示"源章"列，帮作者对照；② 可视化"剧情随章节推进"的数据；③ 校验场景顺序与章节顺序是否一致。
- 注意：后端内部仍可保存 `sourceText` 作为单场重生与人工校对的输入，但它不属于最终导出的剧本 YAML。

---

### 5.4 `storylines` — 故事线索

```yaml
storylines:
  - name: 求职挣扎
    type: MAIN
    events:
      - scene: s1
        event: 独自审视简历，陷入自我怀疑
      - scene: s3
        event: 接到录用电话，迎来转机
```

| 字段 | 必填 | 类型 | 说明与设计原因 |
|---|---|---|---|
| `name` | 必填 | string | 线索名称。 |
| `type` | 必填 | enum | `MAIN`（主线）/ `SUB`（支线），见 §6.4。 |
| `events` | 必填 | list | 该线索上的事件节点，每项 `{scene, event}`。`scene` 引用 `sceneId`，把事件**挂到具体场景**上。 |

- **设计原因**：① 线索是"成体系"的体现——展示 AI 不仅拆了场，还理解了**叙事结构**（哪些场属于主线、哪些是支线）；② `events` 通过 `scene` 引用场景，是加分项「剧情时间线 / 事件节点图」的直接数据源；③ 主/支线区分帮助作者把握全局节奏。
- 设为顶层字段（与 scenes 平级）而非塞进各场景：因为一条线索**横跨多个场景**，它是全局视角的产物，归属顶层才合理。

---

## 6. 枚举值定义

> 所有枚举集中定义，便于生成端、校验端、渲染端共享同一份"真值表"。MVP 实际启用的值已标注。

### 6.1 `screenplayType` — 剧本类型

| 值 | 含义 | MVP 状态 |
|---|---|---|
| `ANIME` | 动画剧本（TV 单集 ~20-24min） | ✅ 实现 |
| `FILM` | 影视剧本（长片 ~90-120min） | 预留（TODO） |
| `SHORT_DRAMA` | 短剧剧本（~2-5min/集） | 预留（TODO） |
| `RADIO` | 广播剧（无画面） | 预留（TODO） |
| `THEATER` | 话剧（舞台） | 预留（TODO） |

> **设计原因**：见技术方案 §5.3——类型决定 prompt 模板与渲染规则。MVP 仅实现画面类（动画），其余预留枚举位，扩展时只需新增模板，不动 Schema。

### 6.2 `character.role` — 人物重要度

| 值 | 含义 |
|---|---|
| `PROTAGONIST` | 主角 |
| `SUPPORTING` | 配角 |
| `MINOR` | 次要/龙套 |

> **设计原因**：三档离散分级贴合创作者认知；驱动渲染强调与图谱节点权重。

### 6.3 `storyline.type` — 线索类型

| 值 | 含义 |
|---|---|
| `MAIN` | 主线 |
| `SUB` | 支线 |

---

## 7. 完整示例

以下是一份合法的、最小但完整的剧本 YAML（动画类型，节选自示例作品《她比烟花寂寞》）：

```yaml
schemaVersion: "1.0"
title: 她比烟花寂寞
screenplayType: ANIME
plotSummary: 求职屡败的设计师林晚在天台徘徊于生死边缘，最终被一通迟来的录用电话拉回，重拾对生活的微光。

characters:
  - name: 林晚
    role: PROTAGONIST
    description: 27岁，求职屡屡碰壁的设计师，敏感坚韧
    firstAppearance: 第1章
    relationships:
      - target: 母亲
        relation: 母女
      - target: 陈经理
        relation: 录用关系
  - name: 陈经理
    role: SUPPORTING
    description: 录用林晚的公司主管，欣赏她的作品
    firstAppearance: 第3章
  - name: 母亲
    role: SUPPORTING
    description: 电话里关心又施压的存在
    firstAppearance: 第1章

scenes:
  - sceneId: s1
    heading:
      interior: true
      location: 出租屋
      timeOfDay: 夜
    scriptBlocks:
      - type: ACTION
        text: 雨敲打着铁皮屋檐。狭小的房间里，林晚盘腿坐在地板上，膝头摊着一份被反复折叠的简历。
      - type: ACTION
        text: 台灯昏黄。她把简历又看了一遍，指尖无意识地把纸角捻出深深的褶皱。
      - type: DIALOGUE
        character: 林晚
        parenthetical: (画外音)
        line: 又一次……我都习惯了。习惯，是最体面的认输。
      - type: TRANSITION
        text: 切至：
    sourceChapter: 1

  - sceneId: s2
    heading:
      interior: false
      location: 天台
      timeOfDay: 黄昏
    scriptBlocks:
      - type: ACTION
        text: 城市在脚下铺开，晚风掀动林晚的发。她走到栏杆边，指节因用力而泛白。
      - type: DIALOGUE
        character: 林晚
        parenthetical: (画外音)
        line: 往下看一眼，就一眼……可那串电话还没挂断。
      - type: TRANSITION
        text: 切至：
    sourceChapter: 2

  - sceneId: s3
    heading:
      interior: true
      location: 公司
      timeOfDay: 日
    scriptBlocks:
      - type: ACTION
        text: 手机骤然震动。林晚怔住，缓缓接起。
      - type: DIALOGUE
        character: 陈经理
        line: 林晚小姐，恭喜你通过了。
      - type: DIALOGUE
        character: 林晚
        parenthetical: (声音发颤)
        line: ……真的吗？谢谢，谢谢您。
      - type: TRANSITION
        text: 淡出
    sourceChapter: 3

storylines:
  - name: 求职挣扎
    type: MAIN
    events:
      - scene: s1
        event: 独自审视简历，陷入自我怀疑
      - scene: s2
        event: 天台徘徊于生死边缘
      - scene: s3
        event: 接到录用电话，迎来转机
  - name: 母女羁绊
    type: SUB
    events:
      - scene: s2
        event: 想起母亲的唠叨，成为活下去的牵绊
```

---

## 8. 校验规则

一份剧本 YAML 被认为**合法**，需同时满足以下规则。后端在 LLM 结构化输出后、落库前执行校验（见技术方案 §6「LLM 输出不可控」对策）。

### 8.1 必填与类型

- 顶层 `schemaVersion`、`title`、`screenplayType`、`scenes` 必须存在。
- `scenes` 至少含 1 个场景；空剧本非法。
- 每个场景的 `scriptBlocks` 必须存在；每个正文块的 `type` 必须存在。
- 各字段类型符合 §5 定义（如 `heading.interior` 必须是布尔）。

### 8.2 枚举合法性

- `screenplayType` ∈ §6.1；`role` ∈ §6.2；`scriptBlocks[].type` ∈ `ACTION/DIALOGUE/TRANSITION`；`storyline.type` ∈ §6.3。
- 不在枚举内的值 → 校验失败（或降级为默认值并告警，见技术方案"字段缺失降级处理"）。

### 8.3 引用完整性（关键）

这是保证"结构自洽"的核心校验：

| 引用 | 规则 |
|---|---|
| `scriptBlocks[type=DIALOGUE].character` | 必须存在于 `characters[].name` 中（对白说话人必须是已登记人物）。 |
| `relationships[].target` | 必须存在于 `characters[].name` 中（关系对端必须是已登记人物）。 |
| `storylines[].events[].scene` | 必须存在于某个 `scenes[].sceneId`（事件必须挂在真实场景上）。 |

> **为什么校验引用完整性**：这正是 Schema "结构化"价值的兑现——人物、场景、线索通过 id/name 相互引用织成一张自洽的网。引用悬空（对白引用了不存在的人物）意味着跨章一致性被破坏，必须拦截。

### 8.4 唯一性

- `characters[].name` 全剧唯一（人物主键不可重复）。
- `scenes[].sceneId` 全剧唯一。

### 8.5 一致性（软校验，告警而非拒绝）

- `sourceChapter` 宜随 `scenes` 顺序单调不减（场景顺序通常与章节顺序一致）；逆序时告警，因为可能是闪回（合法）或切分错误（需复查）。
- `scriptBlocks` 宜至少包含一个 `ACTION` 或 `DIALOGUE` 块；只有转场的场景通常意味着生成质量不足。

---

## 9. 设计取舍与被否决的替代方案

把"为什么不那样做"讲清楚，与"为什么这样做"同等重要。

### 9.1 为什么 heading 拆成对象，而不是一个字符串？

- **被否决方案**：`heading: "内景 - 出租屋 - 夜"`。
- **否决原因**：字符串需要二次解析才能编辑/查询，且不同语言/类型的标题格式不同，会把渲染格式硬编进数据。拆成 `{interior, location, timeOfDay}` 后，数据只存语义，格式交给渲染层（见 §5.3.2）。

### 9.2 为什么不把内部改编日志放进导出 YAML？

- **被否决方案**：在导出的场景中加入原文片段、改编日志等内部字段。
- **否决原因**：最终 YAML 要像一份剧本，而不是一份 AI 调试报告。内心戏改编的结果必须体现在 `scriptBlocks` 的动作、对白或画外音中；内部日志可以用于服务端重生和调试，但不应污染交付物。
- **当前实现落点**：`sourceText` 与 `visualizedInnerThoughts` 允许存在于后端内部 JSON、转换详情或调试链路中，但导出 YAML 会剔除它们，只保留剧本可读和结构定位所需字段。

### 9.3 为什么人物全局抽取，而不是就近写在场景里？

- **被否决方案**：每个场景内嵌该场出现的人物完整信息。
- **否决原因**：冗余 + 跨场不一致 + 无法生成全局人物表（见 §5.2）。全局单一数据源是范式化的必然选择。

### 9.4 为什么用 `sceneId` 字符串而不是数组下标定位场景？

- **被否决方案**：用 `scenes[6]` 这样的下标指代第 7 场。
- **否决原因**：插入/删除场景后下标全部位移，导致 `storylines.events.scene` 引用与编辑 API 失效。稳定 id 是"可编辑"的前提（见 §5.3.1）。

### 9.5 为什么 MVP 不引入更细的镜头/分镜字段（如 shotType、cameraAngle）？

- **否决原因**：72h 内必须克制（原则六）。分镜是更专业的下游环节，MVP 聚焦"小说→剧本初稿"。Schema 已通过 `scriptBlocks` 表达动作、对白与转场，更细的镜头信息留待 §10 扩展。过早引入会让 AI 输出负担与校验复杂度陡增，且非作者核心痛点。

### 9.6 为什么对白用 block 对象，而不是 `"林晚：又一次……"` 字符串？

- **否决原因**：字符串混合了说话人、提示、台词，无法稳定拆分（人名里也可能有冒号），且丢失 `parenthetical` 的归属。block 化是"把对白从散文里干净抽出"（痛点 #2）的结构兑现。

---

## 10. 扩展性设计

Schema 在不破坏现有结构的前提下，预留了以下扩展路径：

| 扩展方向 | 扩展方式 | 是否破坏兼容 |
|---|---|---|
| 新剧本类型（短剧/广播剧/话剧） | `screenplayType` 加枚举值 + 新增渲染/生成模板 | 否 |
| 新正文块类型（蒙太奇/音效/字幕） | `scriptBlocks.type` 加枚举值 | 否 |
| 分镜细化（镜头类型、运镜） | `scenes[]` 下新增可选 `shots[]` 字段 | 否（可选字段） |
| 多语言剧本 | 顶层加可选 `language`；渲染层按语言拼装 heading | 否 |
| 配乐/音效（广播剧需要） | `scenes[]` 下新增可选 `sound[]` | 否 |
| Schema 演进 | `schemaVersion` 升版，消费方按版本分支解析 | 受控 |

> **核心扩展哲学**：新增一律走"可选字段"或"枚举追加"，**绝不修改已有字段的语义或必填性**。这保证旧数据永远在新版本下仍然合法（向后兼容）。

---

## 11. 与业界格式的对照

| 业界格式 | 本 Schema 的对应/借鉴 |
|---|---|
| **Fountain**（开源剧本标记语言） | 概念对齐：scene heading、action、character、parenthetical、dialogue、transition 一一对应。本 Schema 可视为 Fountain 的"结构化（机器友好）版本"，未来可双向转换导出 `.fountain`。 |
| **标准好莱坞剧本格式** | `heading` 的"内外景-地点-时间"三要素、对白的"角色提示 + 括号提示 + 台词"三段式，均遵循该惯例。 |
| **Final Draft (.fdx)** | 不直接采用（闭源、XML 臃肿），但本 Schema 字段可无损映射到 fdx 元素，保留未来互通可能。 |

> **设计原因（呼应原则五）**：贴合业界惯例，让产物"专业、可信、可迁移"，而不是自创一套谁都不认的私有格式。

---

## 附录 A：字段速查表

| 路径 | 类型 | 必填 | 一句话说明 |
|---|---|---|---|
| `schemaVersion` | string | ✅ | Schema 版本 |
| `title` | string | ✅ | 作品标题 |
| `screenplayType` | enum | ✅ | 剧本类型 |
| `plotSummary` | string | ✅ | 全局剧情概要 |
| `characters[].name` | string | ✅ | 人物规范名（主键） |
| `characters[].role` | enum | ✅ | 重要度 |
| `characters[].description` | string | ✅ | 人物简述 |
| `characters[].firstAppearance` | string | ✅ | 首次出场章节 |
| `characters[].relationships[].target` | string | ⬜ | 关系对端人物名 |
| `characters[].relationships[].relation` | string | ⬜ | 关系描述 |
| `scenes[].sceneId` | string | ✅ | 场景唯一 id |
| `scenes[].heading.interior` | bool | ✅ | 内/外景 |
| `scenes[].heading.location` | string | ✅ | 地点 |
| `scenes[].heading.timeOfDay` | string | ✅ | 时间 |
| `scenes[].scriptBlocks[].type` | enum | ✅ | 正文块类型：`ACTION/DIALOGUE/TRANSITION` |
| `scenes[].scriptBlocks[].text` | string | 条件必填 | 动作或转场正文 |
| `scenes[].scriptBlocks[].character` | string | 条件必填 | 对白说话人（引用人物名） |
| `scenes[].scriptBlocks[].parenthetical` | string | ⬜ | 括号提示 |
| `scenes[].scriptBlocks[].line` | string | 条件必填 | 台词 |
| `scenes[].sourceChapter` | int | ✅ | 来源章节 |
| `storylines[].name` | string | ✅ | 线索名 |
| `storylines[].type` | enum | ✅ | 主线/支线 |
| `storylines[].events[].scene` | string | ✅ | 关联场景 id |
| `storylines[].events[].event` | string | ✅ | 事件描述 |

> ✅ 必填；⬜ 可选；条件必填表示仅在对应 `scriptBlocks.type` 下必填。
