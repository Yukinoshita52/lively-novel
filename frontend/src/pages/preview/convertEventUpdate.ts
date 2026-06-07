import type {
  ConvertEventItem,
  GeneratedSceneSummary,
  SceneResult,
} from '../../types/novel'

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

function formatFailureReason(reason: string) {
  if (reason.includes('单场剧本生成出现语言漂移')) {
    return '生成结果混入非中文表达，系统已自动重试但仍未通过。请点击“继续转换”，系统会跳过已完成部分并再次尝试。'
  }

  return reason
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
        message: payload.replayed
          ? `${String(payload.message ?? '已载入历史切场')}：第 ${payload.chapterIndex ?? '?'} 章，共 ${payload.sceneCount ?? '?'} 场`
          : `第 ${payload.chapterIndex ?? '?'} 章已切分为 ${payload.sceneCount ?? '?'} 场：${String(payload.title ?? '未命名章节')}`,
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
    const generatedMessage = `已生成第 ${payload.chapterIndex ?? '?'} 章第 ${sceneIndexInChapter ?? '?'} 场：${String(payload.title ?? '未命名章节')}`
    const warningMessage =
      typeof payload.message === 'string' && payload.message.trim() ? payload.message.trim() : undefined

    return {
      conversionId,
      event: {
        type: 'scene_completed',
        message: payload.replayed
          ? `${String(payload.message ?? '已载入历史场景')}：第 ${payload.chapterIndex ?? '?'} 章第 ${sceneIndexInChapter ?? '?'} 场：${String(payload.title ?? '未命名章节')}`
          : warningMessage
            ? `${generatedMessage}\n${warningMessage}`
            : generatedMessage,
      },
      generatedScene: {
        chapterIndex: Number(payload.chapterIndex ?? 0),
        sceneIndexInChapter,
        title: String(payload.title ?? '未命名章节'),
        scene,
      },
    }
  }

  if (eventName === 'analysis_updated') {
    const plotSummary =
      typeof payload.plotSummary === 'string' && payload.plotSummary.trim()
        ? payload.plotSummary.trim()
        : '已根据最新场景刷新人物、线索与剧情概要'

    return {
      conversionId,
      event: {
        type: 'analysis_updated',
        message: `全局状态已更新：${plotSummary}`,
      },
    }
  }

  if (eventName === 'analysis_restored') {
    const characterCount = Number(payload.characterCount ?? 0)
    const storylineCount = Number(payload.storylineCount ?? 0)
    const activeThreadCount = Number(payload.activeThreadCount ?? 0)
    const motifCount = Number(payload.motifCount ?? 0)
    const plotSummary =
      typeof payload.plotSummary === 'string' && payload.plotSummary.trim()
        ? payload.plotSummary.trim()
        : undefined
    const summaryLine = `已载入历史全局状态：已记录 ${characterCount} 名人物、${storylineCount} 条故事线、${activeThreadCount} 条活跃事件线、${motifCount} 个重复意象。`

    return {
      conversionId,
      event: {
        type: 'analysis_restored',
        message: plotSummary ? `${summaryLine}\n当前剧情概要：${plotSummary}` : summaryLine,
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
    const message = String(payload.message ?? '转换中断，可继续转换；系统会跳过已完成部分。')
    const reason = typeof payload.reason === 'string' && payload.reason.trim() ? payload.reason.trim() : undefined
    const convertError = reason ? `${message}\n失败原因：${formatFailureReason(reason)}` : message
    return {
      conversionId,
      event: {
        type: 'failed',
        message,
      },
      convertError,
    }
  }

  return undefined
}
