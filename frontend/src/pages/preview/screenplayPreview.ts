import type {
  GeneratedSceneSummary,
  ScriptBlock,
  SceneHeading,
  SceneResult,
  ScreenplayPersistedScene,
} from '../../types/novel'

export interface SceneOutlineItem extends GeneratedSceneSummary {
  key: string
  sceneNumber: string
  headingText: string
  warnings: GenerationQualityWarning[]
}

export type PreviewTabKey = 'script' | 'source' | 'scene-table'

export interface PreviewTab {
  key: PreviewTabKey
  label: string
  active: boolean
}

export interface PreviewAction {
  label: string
  enabled: boolean
}

export interface PreviewActions {
  primary: PreviewAction
  secondary: PreviewAction[]
}

export interface SceneTableRow {
  key: string
  sceneNumber: string
  interiorText: string
  location: string
  timeOfDay: string
  sourceChapterText: string
}

export interface ScriptBlockRow extends ScriptBlock {
  key: string
}

export type GenerationQualityWarningSeverity = 'blocking' | 'check'
export type GenerationQualityWarningSource = 'generation' | 'structure'

export interface GenerationQualityWarning {
  key: string
  sceneKey: string
  sceneNumber: string
  chapterIndex: number
  sceneIndexInChapter?: number
  severity: GenerationQualityWarningSeverity
  source: GenerationQualityWarningSource
  title: string
  message: string
}

const PREVIEW_TABS: Array<Omit<PreviewTab, 'active'>> = [
  { key: 'script', label: '剧本' },
  { key: 'source', label: '原文' },
  { key: 'scene-table', label: '场景表' },
]

export function buildPreviewTabs(activeTab: PreviewTabKey): PreviewTab[] {
  return PREVIEW_TABS.map((tab) => ({
    ...tab,
    active: tab.key === activeTab,
  }))
}

export function buildPreviewActions(hasSelectedScene: boolean): PreviewActions {
  return {
    primary: {
      label: '打磨本场',
      enabled: hasSelectedScene,
    },
    secondary: [
      {
        label: '返回转换',
        enabled: true,
      },
    ],
  }
}

export function buildSceneHeadingText(heading?: SceneHeading) {
  if (!heading) {
    return '场景信息待生成'
  }

  const prefix = heading.interior ? '内景' : '外景'
  return `${prefix} — ${heading.location || '未知地点'} — ${heading.timeOfDay || '未知时间'}`
}

export function getSceneKey(scene: GeneratedSceneSummary) {
  return `${scene.chapterIndex}-${scene.sceneIndexInChapter ?? scene.title}`
}

function createWarning(
  scene: GeneratedSceneSummary,
  sceneKey: string,
  sceneNumber: string,
  source: GenerationQualityWarningSource,
  severity: GenerationQualityWarningSeverity,
  title: string,
  message: string,
  index: number,
): GenerationQualityWarning {
  return {
    key: `${sceneKey}-${source}-${index}`,
    sceneKey,
    sceneNumber,
    chapterIndex: scene.chapterIndex,
    sceneIndexInChapter: scene.sceneIndexInChapter,
    severity,
    source,
    title,
    message,
  }
}

export function buildSceneQualityWarnings(
  scene: GeneratedSceneSummary,
  sceneKey = getSceneKey(scene),
  sceneNumber = '',
): GenerationQualityWarning[] {
  const warnings: GenerationQualityWarning[] = []
  const scriptBlocks = normalizeScriptBlocks(scene.scene)

  scene.scene.warnings
    ?.filter((message) => message.trim())
    .forEach((message, index) => {
      warnings.push(createWarning(scene, sceneKey, sceneNumber, 'generation', 'check', '生成结果建议检查', message, index))
    })

  if (scriptBlocks.length === 0) {
    warnings.push(createWarning(
      scene,
      sceneKey,
      sceneNumber,
      'structure',
      'blocking',
      '剧本正文为空',
      '本场没有可渲染的剧本正文块，需要补写动作、对白或转场。',
      warnings.length,
    ))
  } else {
    scriptBlocks.forEach((block, index) => {
      const hasText = typeof block.text === 'string' && block.text.trim()
      const hasLine = typeof block.line === 'string' && block.line.trim()
      if (!hasText && !hasLine) {
        warnings.push(createWarning(
          scene,
          sceneKey,
          sceneNumber,
          'structure',
          'check',
          '正文块为空',
          `第 ${index + 1} 个剧本正文块缺少可读内容，需要在打磨页补齐。`,
          warnings.length,
        ))
      }
    })
  }

  if (!scene.scene.heading?.location || scene.scene.heading.location === '未知地点') {
    warnings.push(createWarning(
      scene,
      sceneKey,
      sceneNumber,
      'structure',
      'check',
      '地点待确认',
      '本场地点为空或仍是未知地点，建议检查场景标题。',
      warnings.length,
    ))
  }

  if (!scene.scene.heading?.timeOfDay || scene.scene.heading.timeOfDay === '未知时间') {
    warnings.push(createWarning(
      scene,
      sceneKey,
      sceneNumber,
      'structure',
      'check',
      '时间待确认',
      '本场时间为空或仍是未知时间，建议检查场景标题。',
      warnings.length,
    ))
  }

  return warnings
}

export function buildSceneOutlineItems(scenes: GeneratedSceneSummary[]): SceneOutlineItem[] {
  return [...scenes]
    .sort((left, right) => {
      if (left.chapterIndex !== right.chapterIndex) {
        return left.chapterIndex - right.chapterIndex
      }

      return (left.sceneIndexInChapter ?? 0) - (right.sceneIndexInChapter ?? 0)
    })
    .map((scene, index) => ({
      ...scene,
      key: getSceneKey(scene),
      sceneNumber: `S${index + 1}`,
      headingText: buildSceneHeadingText(scene.scene.heading),
    }))
    .map((scene) => ({
      ...scene,
      warnings: buildSceneQualityWarnings(scene, scene.key, scene.sceneNumber),
    }))
}

export function mapPersistedScenesToGeneratedScenes(scenes: ScreenplayPersistedScene[]): GeneratedSceneSummary[] {
  return scenes.map((scene) => ({
    chapterIndex: scene.chapterIndex,
    sceneIndexInChapter: scene.sceneIndexInChapter,
    title: scene.title?.trim() || buildSceneHeadingText(scene.scene.heading),
    scene: scene.scene,
  }))
}

export function buildGenerationQualityWarnings(scenes: GeneratedSceneSummary[]): GenerationQualityWarning[] {
  return buildSceneOutlineItems(scenes).flatMap((scene) => scene.warnings)
}

export function buildSceneTableRows(scenes: GeneratedSceneSummary[]): SceneTableRow[] {
  return buildSceneOutlineItems(scenes).map((scene) => ({
    key: scene.key,
    sceneNumber: scene.sceneNumber,
    interiorText: scene.scene.heading.interior ? '内景' : '外景',
    location: scene.scene.heading.location || '未知地点',
    timeOfDay: scene.scene.heading.timeOfDay || '未知时间',
    sourceChapterText: `CH${scene.scene.sourceChapter || scene.chapterIndex}`,
  }))
}

export function normalizeScriptBlocks(scene: SceneResult): ScriptBlock[] {
  if (scene.scriptBlocks?.length) {
    return scene.scriptBlocks
  }

  return [
    ...(scene.actionLines ?? [])
      .filter((text) => text.trim())
      .map((text) => ({ type: 'ACTION' as const, text })),
    ...(scene.dialogueBlocks ?? [])
      .filter((dialogue) => dialogue.character.trim() && dialogue.line.trim())
      .map((dialogue) => ({
        type: 'DIALOGUE' as const,
        character: dialogue.character,
        parenthetical: dialogue.parenthetical,
        line: dialogue.line,
      })),
    ...(scene.transitions ?? [])
      .filter((text) => text.trim())
      .map((text) => ({ type: 'TRANSITION' as const, text })),
  ]
}

export function buildScriptBlockRows(scene: GeneratedSceneSummary): ScriptBlockRow[] {
  return normalizeScriptBlocks(scene.scene).map((block, index) => ({
    ...block,
    key: `${getSceneKey(scene)}-script-${index}`,
  }))
}

export function resolveSelectedScene(
  scenes: GeneratedSceneSummary[],
  selectedSceneKey?: string,
): SceneOutlineItem | undefined {
  const outlineItems = buildSceneOutlineItems(scenes)
  if (outlineItems.length === 0) {
    return undefined
  }

  const selected = selectedSceneKey
    ? outlineItems.find((scene) => scene.key === selectedSceneKey)
    : undefined

  return selected ?? outlineItems[outlineItems.length - 1]
}

export function resolveAdjacentSceneKeys(scenes: GeneratedSceneSummary[], selectedSceneKey?: string) {
  const outlineItems = buildSceneOutlineItems(scenes)
  const selectedIndex = selectedSceneKey
    ? outlineItems.findIndex((scene) => scene.key === selectedSceneKey)
    : outlineItems.length - 1
  const normalizedIndex = selectedIndex >= 0 ? selectedIndex : outlineItems.length - 1

  return {
    previousKey: normalizedIndex > 0 ? outlineItems[normalizedIndex - 1]?.key : undefined,
    nextKey: normalizedIndex >= 0 && normalizedIndex < outlineItems.length - 1
      ? outlineItems[normalizedIndex + 1]?.key
      : undefined,
  }
}

export function getSourcePreview(sourceText: string, expanded: boolean, maxLength = 360) {
  if (expanded || sourceText.length <= maxLength) {
    return sourceText
  }

  return `${sourceText.slice(0, maxLength)}…`
}

export function getSourceDisplayText(sourceText: string) {
  return sourceText.trim() || '暂无原文内容。'
}

export function buildYamlDownloadFileName(title: string) {
  const normalizedTitle = title.trim().replace(/[\\/:*?"<>|]+/g, '-')
  return normalizedTitle ? `${normalizedTitle}-screenplay.yaml` : 'screenplay.yaml'
}

