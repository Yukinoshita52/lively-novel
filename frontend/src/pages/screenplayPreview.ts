import type { GeneratedSceneSummary, SceneHeading } from '../types/novel'

export interface SceneOutlineItem extends GeneratedSceneSummary {
  key: string
  sceneNumber: string
  headingText: string
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

export function getSourcePreview(sourceText: string, expanded: boolean, maxLength = 360) {
  if (expanded || sourceText.length <= maxLength) {
    return sourceText
  }

  return `${sourceText.slice(0, maxLength)}…`
}
