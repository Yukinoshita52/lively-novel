import { useEffect, useState } from 'react'
import type {
  ConvertEventItem,
  GeneratedSceneSummary,
  ScreenplayConvertContext,
  ScreenplayConversionDetail,
} from '../types/novel'
import { resolveConvertEventUpdate } from './preview/convertEventUpdate.ts'
import { mapPersistedScenesToGeneratedScenes } from './preview/screenplayPreview.ts'

export interface ConversionSessionState {
  context: ScreenplayConvertContext
  connecting: boolean
  running: boolean
  events: ConvertEventItem[]
  generatedScenes: GeneratedSceneSummary[]
  chapterSceneCounts: Record<number, number>
  completed: boolean
  conversionId?: string
  convertError: string | null
  failureDetail: ConversionFailureDetail | null
}

export interface PreviewEntryState {
  enabled: boolean
  label: string
}

export interface ResumeEntryState {
  enabled: boolean
  label: string
}

export interface ConversionFailureDetail {
  userMessage: string
  technicalMessage?: string
  chapterIndex?: number
  sceneIndexInChapter?: number
  stage: string
  locationLabel: string
}

export interface RestoredConversionSummary {
  conversionId: string
  status: string
  statusLabel: string
  updatedAt?: string
}

const CONVERSION_FAILURE_USER_MESSAGE = '转换中断，可继续转换；已完成部分不会丢失。'

function extractNumber(text: string, pattern: RegExp) {
  const matched = text.match(pattern)
  return matched?.[1] ? Number(matched[1]) : undefined
}

function resolveFailureStage(text: string) {
  if (/读取|chapter_loaded/i.test(text)) {
    return '读取章节'
  }
  if (/切场|切分|片段|sceneUnit/i.test(text)) {
    return '切分场景'
  }
  if (/全局状态|analysis/i.test(text)) {
    return '更新全局状态'
  }
  if (/生成|剧本|语言漂移|场景原文|scene/i.test(text)) {
    return '生成剧本'
  }
  return '转换流程'
}

function normalizeTechnicalMessage(message: string) {
  return message
    .replace(/^转换中断[^\n]*(\n|$)/, '')
    .replace(/^转换未完成[^\n]*(\n|$)/, '')
    .replace(/^失败原因[:：]\s*/, '')
    .trim()
}

export function createConversionFailureDetail(message: string | null | undefined): ConversionFailureDetail {
  const rawMessage = message?.trim() ?? ''
  const chapterSceneMatched = rawMessage.match(/第\s*(\d+)\s*章(?:第\s*(\d+)\s*场)?/)
  const chapterIndex = chapterSceneMatched?.[1]
    ? Number(chapterSceneMatched[1])
    : extractNumber(rawMessage, /chapterIndex\s*[=:：]\s*(\d+)/i)
  const sceneIndexInChapter = chapterSceneMatched?.[2]
    ? Number(chapterSceneMatched[2])
    : extractNumber(rawMessage, /sceneIndexInChapter\s*[=:：]\s*(\d+)/i)
  const technicalMessage = normalizeTechnicalMessage(rawMessage)
  const locationLabel = chapterIndex
    ? `第 ${chapterIndex} 章${sceneIndexInChapter ? `第 ${sceneIndexInChapter} 场` : ''}`
    : '位置待确认'

  return {
    userMessage: CONVERSION_FAILURE_USER_MESSAGE,
    technicalMessage: technicalMessage || undefined,
    chapterIndex,
    sceneIndexInChapter,
    stage: resolveFailureStage(rawMessage),
    locationLabel,
  }
}

export function resolveRestoredConversionSummary(
  context: ScreenplayConvertContext,
): RestoredConversionSummary | null {
  if (!context.restoredConversionId || !context.restoredConversionStatus) {
    return null
  }

  const statusLabelMap: Record<string, string> = {
    RUNNING: '转换中',
    FAILED: '转换失败',
    COMPLETED: '已完成',
  }

  return {
    conversionId: context.restoredConversionId,
    status: context.restoredConversionStatus,
    statusLabel: statusLabelMap[context.restoredConversionStatus] ?? '状态未知',
    updatedAt: context.restoredConversionUpdatedAt,
  }
}

export function createInitialConversionSessionState(context: ScreenplayConvertContext): ConversionSessionState {
  return {
    context,
    connecting: true,
    running: true,
    events: [],
    generatedScenes: [],
    chapterSceneCounts: {},
    completed: false,
    convertError: null,
    failureDetail: null,
  }
}

export function isRestoredCompletedConversionContext(context: ScreenplayConvertContext): boolean {
  return Boolean(context.restoredConversionId) && context.restoredConversionStatus === 'COMPLETED'
}

export function isRestoredConversionContext(context: ScreenplayConvertContext): boolean {
  return Boolean(context.restoredConversionId)
    && Boolean(context.restoredConversionStatus)
    && context.restoredConversionMode !== 'stream'
}

export function isStaticRestoredCompletedConversionContext(context: ScreenplayConvertContext): boolean {
  return isRestoredCompletedConversionContext(context) && context.restoredConversionMode !== 'stream'
}

export function createRestoredConversionSessionState(
  context: ScreenplayConvertContext,
): ConversionSessionState {
  const restoredScenes = context.restoredGeneratedScenes ?? []
  const completed = context.restoredConversionStatus === 'COMPLETED'
  const failed = context.restoredConversionStatus === 'FAILED'
  const failureDetail = failed ? createConversionFailureDetail(context.restoredConversionErrorMessage) : null
  const errorMessage = failed && failureDetail
    ? [
      failureDetail.userMessage,
      failureDetail.technicalMessage ? `失败原因：${failureDetail.technicalMessage}` : undefined,
    ].filter(Boolean).join('\n')
    : null

  return {
    context,
    connecting: false,
    running: false,
    events: [
      {
        type: 'completed',
        message: `已载入历史转换：共 ${restoredScenes.length} 场。`,
      },
    ],
    generatedScenes: restoredScenes,
    chapterSceneCounts: {},
    completed,
    conversionId: context.restoredConversionId,
    convertError: errorMessage,
    failureDetail,
  }
}

export const createRestoredCompletedConversionSessionState = createRestoredConversionSessionState

export function createRestoredConversionContextFromDetail(
  baseContext: ScreenplayConvertContext,
  detail: ScreenplayConversionDetail,
): ScreenplayConvertContext {
  return {
    ...baseContext,
    screenplayType: detail.screenplayType,
    restoredConversionId: detail.conversionId,
    restoredConversionStatus: detail.status,
    restoredConversionUpdatedAt: detail.updatedAt ?? undefined,
    restoredConversionErrorMessage: detail.errorMessage ?? undefined,
    restoredConversionMode: 'static',
    restoredGeneratedScenes: mapPersistedScenesToGeneratedScenes(detail.scenes),
  }
}

export function createConversionSessionStateFromContext(
  context: ScreenplayConvertContext,
): ConversionSessionState {
  if (isRestoredConversionContext(context)) {
    return createRestoredConversionSessionState(context)
  }

  return createInitialConversionSessionState(context)
}

export function reduceConversionSessionEvent(
  state: ConversionSessionState,
  eventName: string,
  payload: Record<string, unknown>,
): ConversionSessionState {
  const update = resolveConvertEventUpdate(eventName, payload, {
    totalChapters: state.context.totalChapters,
  })
  if (!update) {
    return state
  }

  return {
    ...state,
    events: update.event ? [...state.events, update.event] : state.events,
    conversionId: update.conversionId ?? state.conversionId,
    chapterSceneCounts: update.sceneCount
      ? {
          ...state.chapterSceneCounts,
          [update.sceneCount.chapterIndex]: update.sceneCount.sceneCount,
        }
      : state.chapterSceneCounts,
    generatedScenes: update.generatedScene
      ? [...state.generatedScenes, update.generatedScene]
      : state.generatedScenes,
    completed: update.completed ? true : state.completed,
    connecting: update.completed || update.convertError ? false : state.connecting,
    running: update.completed || update.convertError ? false : state.running,
    convertError: update.convertError ?? state.convertError,
    failureDetail: update.convertError ? createConversionFailureDetail(update.convertError) : state.failureDetail,
  }
}

export function resolvePreviewEntryState(state: ConversionSessionState): PreviewEntryState {
  const enabled = Boolean(state.conversionId) && (state.generatedScenes.length > 0 || state.completed)

  return {
    enabled,
    label: state.completed ? '进入预览' : '查看已生成预览',
  }
}

export function resolveResumeEntryState(state: ConversionSessionState): ResumeEntryState {
  return {
    enabled: Boolean(state.convertError) && !state.running,
    label: '继续转换',
  }
}

type SsePayload = Record<string, unknown>

function toJsonPayload(raw: string): SsePayload {
  try {
    return JSON.parse(raw) as SsePayload
  } catch {
    return {}
  }
}

function findEventDelimiter(buffer: string) {
  const lfIndex = buffer.indexOf('\n\n')
  const crlfIndex = buffer.indexOf('\r\n\r\n')

  if (lfIndex === -1) {
    return crlfIndex
  }

  if (crlfIndex === -1) {
    return lfIndex
  }

  return Math.min(lfIndex, crlfIndex)
}

function delimiterLengthAt(buffer: string, index: number) {
  return buffer.startsWith('\r\n\r\n', index) ? 4 : 2
}

function parseEventBlock(block: string) {
  const lines = block
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  const eventName = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
  const dataLines = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())

  if (!eventName || dataLines.length === 0) {
    return undefined
  }

  return {
    eventName,
    payload: toJsonPayload(dataLines.join('\n')),
  }
}

function appendConversionSessionError(state: ConversionSessionState, message: string): ConversionSessionState {
  return {
    ...state,
    connecting: false,
    running: false,
    convertError: message,
    failureDetail: createConversionFailureDetail(message),
    events: [...state.events, { type: 'error', message }],
  }
}

function markConnectionClosed(state: ConversionSessionState): ConversionSessionState {
  return {
    ...state,
    connecting: false,
    running: state.completed || state.convertError ? false : state.running,
  }
}

export function useScreenplayConversionSession(context: ScreenplayConvertContext | null) {
  const [state, setState] = useState<ConversionSessionState | null>(() =>
    context ? createConversionSessionStateFromContext(context) : null,
  )

  useEffect(() => {
    if (!context) {
      return undefined
    }

    if (isRestoredConversionContext(context)) {
      return undefined
    }

    const activeContext = context
    const abortController = new AbortController()

    async function startConvert() {
      try {
        const response = await fetch('/api/screenplay/convert', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            novelId: activeContext.novelId,
            screenplayType: activeContext.screenplayType,
          }),
          signal: abortController.signal,
        })

        if (!response.ok || !response.body) {
          throw new Error('启动转换失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            break
          }

          buffer += decoder.decode(value, { stream: true })

          let delimiterIndex = findEventDelimiter(buffer)
          while (delimiterIndex >= 0) {
            const block = buffer.slice(0, delimiterIndex)
            buffer = buffer.slice(delimiterIndex + delimiterLengthAt(buffer, delimiterIndex))
            const eventBlock = parseEventBlock(block)
            if (eventBlock) {
              setState((current) =>
                reduceConversionSessionEvent(
                  current?.context === activeContext ? current : createInitialConversionSessionState(activeContext),
                  eventBlock.eventName,
                  eventBlock.payload,
                ),
              )
            }
            delimiterIndex = findEventDelimiter(buffer)
          }
        }

        if (buffer.trim()) {
          const eventBlock = parseEventBlock(buffer)
          if (eventBlock) {
            setState((current) =>
              reduceConversionSessionEvent(
                current?.context === activeContext ? current : createInitialConversionSessionState(activeContext),
                eventBlock.eventName,
                eventBlock.payload,
              ),
            )
          }
        }
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        const message = error instanceof Error ? error.message : '整本转换失败'
        setState((current) =>
          appendConversionSessionError(
            current?.context === activeContext ? current : createInitialConversionSessionState(activeContext),
            message,
          ),
        )
      } finally {
        if (!abortController.signal.aborted) {
          setState((current) =>
            markConnectionClosed(
              current?.context === activeContext ? current : createInitialConversionSessionState(activeContext),
            ),
          )
        }
      }
    }

    void startConvert()

    return () => {
      abortController.abort()
    }
  }, [context])

  if (!context) {
    return null
  }

  if (!state || state.context !== context) {
    return createConversionSessionStateFromContext(context)
  }

  return state
}
