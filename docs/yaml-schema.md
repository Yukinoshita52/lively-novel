# Lively Novel 剧本 YAML Schema

> 日期：2026-06-07
> 版本：v1.0
> 对应实现：`GET /api/screenplay/conversions/{conversionId}/yaml`

本文档定义 Lively Novel 最终导出的 YAML 结构，并说明主要设计原因。文档只描述**导出给用户的剧本 YAML**；后端内部用于继续转换、恢复任务、调试或提示词上下文的字段不属于导出 Schema。

---

## 1. Schema 结构

最终 YAML 顶层字段顺序由后端导出逻辑固定为：

```yaml
schemaVersion: "1.0"
title: "作品标题"
screenplayType: "ANIME"
plotSummary: "整部作品截至当前转换结果的剧情概要。"
characters: []
scenes: []
storylines: []
```

字段总览：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `schemaVersion` | string | 是 | Schema 版本，当前为 `"1.0"`。 |
| `title` | string | 是 | 作品标题，来自导入小说的标题。 |
| `screenplayType` | enum | 是 | 剧本类型。当前 MVP 实际支持 `ANIME`。 |
| `plotSummary` | string | 是 | 滚动全局状态生成的剧情概要；无状态时为空字符串。 |
| `characters` | list | 是 | 全局人物表；无状态时为空数组。 |
| `scenes` | list | 是 | 场景列表，按章节与章内场次顺序排列。 |
| `storylines` | list | 是 | 故事线列表；无状态时为空数组。 |

---

## 2. 完整示例

下面示例展示当前实现实际支持的字段形态。内容为示意文本，不代表固定输出。

```yaml
schemaVersion: "1.0"
title: "败犬女主太多了！"
screenplayType: "ANIME"
plotSummary: "温水在家庭餐厅目睹八奈见失恋后的狼狈瞬间，并被卷入她与袴田、华恋之间的关系余波。八奈见试图用食物和玩笑掩饰失落，温水则开始意识到她并非普通同班同学。"
characters:
- name: "温水"
  role: "PROTAGONIST"
  description: "普通男高中生，习惯保持距离观察他人，却在意外中卷入八奈见的失恋事件。"
  firstAppearance: "第1章"
  relationships:
  - target: "八奈见"
    relation: "同班同学，被她要求保密并逐渐产生交集"
- name: "八奈见"
  role: "SUPPORTING"
  description: "温水的同班同学，刚经历青梅竹马恋情落败，情绪外露但仍努力维持体面。"
  firstAppearance: "第1章"
  relationships:
  - target: "袴田"
    relation: "青梅竹马，单恋对象"
scenes:
- sceneId: "s1"
  heading:
    interior: true
    location: "家庭餐厅"
    timeOfDay: "午后"
  scriptBlocks:
  - type: "SHOT"
    text: "家庭餐厅一角。午后的光从窗边斜照进来，店内客人稀疏。"
  - type: "ACTION"
    text: "温水坐在靠墙的位置，用手帕擦去额头的汗。"
  - type: "ACTION"
    text: "他环视四周，确认附近没有同校学生。"
  - type: "INSERT"
    text: "桌面上摆着自助饮料杯、大份薯条，以及一本轻小说最新卷。"
  - type: "VO"
    character: "温水"
    parenthetical: "画外音"
    line: "可乐、薯条、轻小说。派对时间到了。"
  - type: "SFX"
    text: "隔壁桌传来女声尖叫。"
  - type: "DIALOGUE"
    character: "八奈见"
    line: "这样不行啦草介！现在可不是在这种地方浪费时间的时候！"
  - type: "ACTION"
    text: "温水被叫声打断，皱眉看向隔壁桌。"
  - type: "INSERT"
    text: "八奈见杏菜和袴田草介的侧脸特写。"
  - type: "TRANSITION"
    text: "切至：自助饮料台"
  sourceChapter: 1
storylines:
- name: "八奈见的失恋余波"
  type: "MAIN"
  events:
  - scene: "s1"
    event: "温水在家庭餐厅目睹八奈见催促袴田去见华恋，并发现她失恋后的狼狈状态。"
```

---

## 3. 字段定义

### 3.1 `schemaVersion`

```yaml
schemaVersion: "1.0"
```

- 类型：string
- 当前值：`"1.0"`
- 用途：标识导出文件所遵循的 Schema 版本，便于后续兼容升级。

### 3.2 `title`

```yaml
title: "败犬女主太多了！"
```

- 类型：string
- 来源：导入页上传 txt 后的作品标题，或历史小说中保存的标题。
- 当前行为：用户可以在导入页修改标题；导出 YAML 时读取持久化后的小说标题。

### 3.3 `screenplayType`

```yaml
screenplayType: "ANIME"
```

- 类型：enum
- 枚举值：`ANIME`、`FILM`、`SHORT_DRAMA`、`RADIO`、`THEATER`
- 当前实现：MVP 实际支持 `ANIME`。其他枚举为后续扩展预留；后端 LLM 生成逻辑遇到非 `ANIME` 会抛出暂不支持异常。

### 3.4 `plotSummary`

```yaml
plotSummary: "温水在家庭餐厅目睹八奈见失恋后的狼狈瞬间，并被卷入她与袴田、华恋之间的关系余波。"
```

- 类型：string
- 来源：转换过程中的滚动全局状态。
- 当前行为：每生成一个场景后，后端会尝试更新全局状态；若状态为空或解析失败，导出时该字段为空字符串。
- 内容要求：不是逐场流水账，而是对已生成内容的概括；较早事件更概括，近期未解决事件更具体。

### 3.5 `characters`

```yaml
characters:
- name: "温水"
  role: "PROTAGONIST"
  description: "普通男高中生，习惯保持距离观察他人。"
  firstAppearance: "第1章"
  relationships:
  - target: "八奈见"
    relation: "同班同学"
```

人物字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `name` | string | 是 | 人物规范名。 |
| `role` | enum | 是 | 人物重要度：`PROTAGONIST`、`SUPPORTING`、`MINOR`。 |
| `description` | string | 是 | 人物身份、性格或当前状态简述。 |
| `firstAppearance` | string | 是 | 首次出场位置，如 `"第1章"`。 |
| `relationships` | list | 是 | 与其他人物的关系；可为空数组。 |

关系字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `target` | string | 是 | 关系另一端的人物规范名。 |
| `relation` | string | 是 | 关系说明。 |

### 3.6 `scenes`

```yaml
scenes:
- sceneId: "s1"
  heading:
    interior: true
    location: "家庭餐厅"
    timeOfDay: "午后"
  scriptBlocks: []
  sourceChapter: 1
```

场景字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `sceneId` | string | 是 | 场景 id，如 `s1`。由 LLM 单场结果给出。 |
| `heading` | object | 是 | 结构化场景标题。 |
| `scriptBlocks` | list | 是 | 场景正文块，按阅读和演出顺序排列。 |
| `sourceChapter` | int | 是 | 该场来源章节序号。 |

`heading` 字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `interior` | boolean | 是 | `true` 表示内景，`false` 表示外景。 |
| `location` | string | 是 | 场景地点。 |
| `timeOfDay` | string | 是 | 场景时间，如 `午后`、`黄昏`、`夜`。 |

### 3.7 `scriptBlocks`

`scriptBlocks` 是剧本正文的唯一主体。当前导出不再并列输出旧字段 `actionLines`、`dialogueBlocks`、`transitions`，也不输出内部字段 `sourceText`、`visualizedInnerThoughts`。

通用字段：

| 字段 | 类型 | 适用块 | 说明 |
|---|---|---|---|
| `type` | enum | 全部 | 正文块类型。 |
| `text` | string | `SHOT`、`ACTION`、`INSERT`、`SFX`、`TRANSITION` | 文本内容。 |
| `character` | string | `DIALOGUE`、`VO` | 说话人。 |
| `parenthetical` | string | `DIALOGUE`、`VO` | 可选括号提示，如 `画外音`、`低声`。 |
| `line` | string | `DIALOGUE`、`VO` | 台词或画外音正文。 |

正文块类型：

| `type` | 用途 | 推荐字段 |
|---|---|---|
| `SHOT` | 镜头、构图、场景建立、视线焦点 | `text` |
| `ACTION` | 一个可拍摄、可表演的动作节拍 | `text` |
| `INSERT` | 插入特写，如书封、钥匙、账单、杯子 | `text` |
| `SFX` | 音效或声音提示 | `text` |
| `DIALOGUE` | 角色对白 | `character`、`parenthetical`、`line` |
| `VO` | 画外音或内心独白 | `character`、`parenthetical`、`line` |
| `TRANSITION` | 转场，如切至、淡出 | `text` |

示例：

```yaml
scriptBlocks:
- type: "SHOT"
  text: "家庭餐厅一角。午后的光从窗边斜照进来。"
- type: "ACTION"
  text: "温水坐在靠墙的位置，用手帕擦去额头的汗。"
- type: "INSERT"
  text: "桌面上的轻小说封面特写。"
- type: "SFX"
  text: "隔壁桌传来女声尖叫。"
- type: "DIALOGUE"
  character: "八奈见"
  parenthetical: "压低声音"
  line: "这件事不要跟别人说。"
- type: "VO"
  character: "温水"
  parenthetical: "画外音"
  line: "这时候就装作没看见吧。"
- type: "TRANSITION"
  text: "淡出"
```

### 3.8 `storylines`

```yaml
storylines:
- name: "八奈见的失恋余波"
  type: "MAIN"
  events:
  - scene: "s1"
    event: "温水目睹八奈见催促袴田去见华恋。"
```

故事线字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `name` | string | 是 | 故事线名称。 |
| `type` | enum | 是 | `MAIN` 或 `SUB`。 |
| `events` | list | 是 | 故事线上的事件节点。 |

事件字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `scene` | string | 是 | 关联的 `scenes[].sceneId`。 |
| `event` | string | 是 | 事件概述。 |

---

## 4. 设计原因

### 4.1 为什么用 YAML

题目要求输出结构化 YAML。对本项目来说，YAML 同时适合机器消费和人工打磨：列表天然保序，缩进表达层级，中文文本不需要大量转义。作者在打磨页看到的也是接近最终导出形态的 YAML，修改后可以直接保存并进入导出链路。

### 4.2 为什么正文统一放进 `scriptBlocks`

剧本正文不是素材分类表，而是按阅读、表演和镜头顺序推进的文本。把 `SHOT`、`ACTION`、`DIALOGUE`、`VO` 等块放进同一个有序列表，可以保留“先看到什么、谁接着说话、镜头如何转场”的节奏。

### 4.3 为什么拆出七种正文块

早期只使用 `ACTION`、`DIALOGUE`、`TRANSITION` 时，环境、道具、镜头和内心独白容易被塞进 `ACTION`，导致动作块过长且不像剧本。当前七类块把职责拆开：

- 镜头与场景气氛放入 `SHOT`
- 可拍摄动作放入 `ACTION`
- 道具和画面特写放入 `INSERT`
- 声音提示放入 `SFX`
- 普通台词放入 `DIALOGUE`
- 内心独白或旁白放入 `VO`
- 场景切换放入 `TRANSITION`

这样生成结果更接近动画剧本，也更便于用户逐块编辑。

### 4.4 为什么保留顶层 `characters` 和 `storylines`

题目要求处理 3 章以上文本。跨章节转换时，人物称呼、关系和事件线如果只存在于单场正文中，很难保持一致。因此后端在逐场生成后维护滚动全局状态，最终把其中的 `plotSummary`、`characters`、`storylines` 写入导出 YAML。

### 4.5 为什么不导出内部字段

后端内部仍保存或使用一些辅助信息，例如：

- `sourceText`：单场原文，用于回读、继续转换和校对。
- `visualizedInnerThoughts`：旧链路中的内心戏转译留痕。
- `contextSummary`、`activeCharacters`、`activeThreads`、`motifs`、`timeline`、`foreshadows`：滚动全局状态的内部上下文。

这些字段对生成过程有价值，但不属于用户最终拿到的剧本文件。导出 YAML 只保留剧本阅读、编辑和后续加工需要的结构，避免变成 AI 调试报告。

---

## 5. 当前实现边界

| 项目 | 当前状态 |
|---|---|
| 结构化 YAML 导出 | 已实现 |
| 3 章以上小说导入与章节识别 | 已实现，少于 3 章会拒绝 |
| txt 上传 | 已实现 |
| 历史小说选择 | 已实现 |
| 逐场转换与 SSE 进度 | 已实现 |
| 中断后继续转换 | 已实现，已完成场景会回放并跳过 |
| 打磨页编辑 YAML 并保存 | 已实现 |
| 滚动全局状态填充 `plotSummary/characters/storylines` | 已实现，失败时不阻断转换 |
| `ANIME` 剧本生成 | 已实现 |
| `FILM/SHORT_DRAMA/RADIO/THEATER` | 仅保留枚举，生成逻辑未开放 |
| 可读剧本文本导出 | 未实现，当前核心交付为 YAML |

---

## 6. 校验建议

当前代码主要依赖 DTO 结构化解析、枚举约束、导出逻辑和前端编辑流程保证基本合法性。后续如果继续演进，可补充更严格的 YAML 校验：

- `schemaVersion` 必须为 `"1.0"`。
- `screenplayType` 必须属于已知枚举。
- `scenes` 至少包含一个场景。
- 每个场景必须有 `sceneId`、`heading`、`scriptBlocks`、`sourceChapter`。
- `scriptBlocks[].type` 必须属于七种正文块之一。
- `DIALOGUE`、`VO` 必须有 `character` 和 `line`。
- `SHOT`、`ACTION`、`INSERT`、`SFX`、`TRANSITION` 必须有 `text`。
- `storylines[].events[].scene` 应能引用到真实存在的 `sceneId`。

---

## 7. 与代码的对应关系

| Schema 部分 | 代码位置 |
|---|---|
| YAML 导出入口 | `ScreenplayController#exportConversionYaml` |
| YAML 组装逻辑 | `ScreenplayServiceImpl#exportConversionYaml` |
| 顶层结构 DTO | `ScreenplayContentDTO` |
| 场景 DTO | `SceneDTO` |
| 正文块 DTO | `ScriptBlockDTO` |
| 人物 DTO | `CharacterDTO`、`CharacterRelationshipDTO` |
| 故事线 DTO | `StorylineDTO`、`StorylineEventDTO` |
| 滚动全局状态 DTO | `RollingAnalysisStateDTO` |
| 动画剧本生成提示词 | `LlmServiceImpl#buildSingleScenePrompt` |
