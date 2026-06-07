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
}

export type PreviewTabKey = 'script' | 'scene-table'

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

const PREVIEW_TABS: Array<Omit<PreviewTab, 'active'>> = [
  { key: 'script', label: '剧本' },
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
}

export function mapPersistedScenesToGeneratedScenes(scenes: ScreenplayPersistedScene[]): GeneratedSceneSummary[] {
  return scenes.map((scene) => ({
    chapterIndex: scene.chapterIndex,
    sceneIndexInChapter: scene.sceneIndexInChapter,
    title: buildSceneHeadingText(scene.scene.heading),
    scene: scene.scene,
  }))
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

export function buildYamlDownloadFileName(title: string) {
  const normalizedTitle = title.trim().replace(/[\\/:*?"<>|]+/g, '-')
  return normalizedTitle ? `${normalizedTitle}-screenplay.yaml` : 'screenplay.yaml'
}

