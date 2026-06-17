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
}

export interface PreviewEntryState {
  enabled: boolean
  label: string
}

export interface ResumeEntryState {
  enabled: boolean
  label: string
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
  const errorMessage = failed
    ? context.restoredConversionErrorMessage ?? '转换中断，可继续转换；系统会跳过已完成部分。'
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
