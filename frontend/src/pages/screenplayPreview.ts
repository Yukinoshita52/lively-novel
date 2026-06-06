import type { ConvertEventItem, GeneratedSceneSummary, SceneHeading, SceneResult } from '../types/novel'

type SsePayload = Record<string, unknown>

export interface ConvertEventUpdate {
  event?: ConvertEventItem
  conversionId?: string
  sceneCount?: {
    chapterIndex: number
    sceneCount: number
  }
  generatedScene?: GeneratedSceneSummary
  completed?: boolean
  convertError?: string
}

function toSceneResult(value: unknown): SceneResult | undefined {
  if (!value || typeof value !== 'object') {
    return undefined
  }

  const candidate = value as SceneResult
  if (typeof candidate.sceneId !== 'string') {
    return undefined
  }

  return candidate
}

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

export function buildYamlDownloadFileName(title: string) {
  const normalizedTitle = title.trim().replace(/[\\/:*?"<>|]+/g, '-')
  return normalizedTitle ? `${normalizedTitle}-screenplay.yaml` : 'screenplay.yaml'
}

function getPayloadConversionId(payload: SsePayload) {
  return typeof payload.conversionId === 'string' && payload.conversionId.trim()
    ? payload.conversionId.trim()
    : undefined
}

export function resolveConvertEventUpdate(
  eventName: string,
  payload: SsePayload,
  context: { totalChapters: number },
): ConvertEventUpdate | undefined {
  const conversionId = getPayloadConversionId(payload)

  if (eventName === 'started') {
    const totalChapters = Number(payload.totalChapters ?? context.totalChapters)
    return {
      conversionId,
      event: {
        type: 'started',
        message: `开始转换，共 ${totalChapters} 章`,
      },
    }
  }

  if (eventName === 'chapter_loaded') {
    return {
      conversionId,
      event: {
        type: 'chapter_loaded',
        message: `已读取第 ${payload.chapterIndex ?? '?'} 章：${String(payload.title ?? '未命名章节')}`,
      },
    }
  }

  if (eventName === 'chapter_split') {
    const chapterIndex =
      typeof payload.chapterIndex === 'number'
        ? payload.chapterIndex
        : Number(payload.chapterIndex ?? 0)
    const sceneCount =
      typeof payload.sceneCount === 'number' ? payload.sceneCount : Number(payload.sceneCount ?? 0)

    return {
      conversionId,
      event: {
        type: 'chapter_split',
        message: `第 ${payload.chapterIndex ?? '?'} 章已切分为 ${payload.sceneCount ?? '?'} 场：${String(payload.title ?? '未命名章节')}`,
      },
      sceneCount:
        chapterIndex > 0 && sceneCount > 0
          ? {
              chapterIndex,
              sceneCount,
            }
          : undefined,
    }
  }

  if (eventName === 'scene_completed') {
    const sceneIndexInChapter =
      typeof payload.sceneIndexInChapter === 'number'
        ? payload.sceneIndexInChapter
        : Number(payload.sceneIndexInChapter ?? 0) || undefined
    const scene = toSceneResult(payload.scene)
    if (!scene) {
      return undefined
    }

    return {
      conversionId,
      event: {
        type: 'scene_completed',
        message: `已生成第 ${payload.chapterIndex ?? '?'} 章第 ${sceneIndexInChapter ?? '?'} 场：${String(payload.title ?? '未命名章节')}`,
      },
      generatedScene: {
        chapterIndex: Number(payload.chapterIndex ?? 0),
        sceneIndexInChapter,
        title: String(payload.title ?? '未命名章节'),
        scene,
      },
    }
  }

  if (eventName === 'completed') {
    return {
      conversionId,
      event: {
        type: 'completed',
        message: `整本转换完成，共处理 ${payload.totalChapters ?? context.totalChapters} 章`,
      },
      completed: true,
    }
  }

  if (eventName === 'failed') {
    const message = String(payload.message ?? '转换未完成，请调整文本后重试。')
    return {
      conversionId,
      event: {
        type: 'failed',
        message,
      },
      convertError: message,
    }
  }

  return undefined
}
