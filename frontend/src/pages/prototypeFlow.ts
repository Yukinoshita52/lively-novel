import type { ConvertEventItem } from '../types/novel'

export type FlowStepKey = 'import' | 'convert' | 'preview' | 'polish' | 'export'
export type PipelinePhaseStatus = 'idle' | 'active' | 'done' | 'failed'

export interface FlowStep {
  key: FlowStepKey
  number: string
  label: string
  active: boolean
  done: boolean
}

export interface PipelinePhase {
  key: 'analysis' | 'generation' | 'persist'
  mark: string
  title: string
  description: string
  progress: number
  status: PipelinePhaseStatus
}

export interface ConvertProgressNoteContext {
  currentChapterIndex?: number
  finishedSceneCount: number
  totalSceneCount: number
}

export interface CurrentConvertChapterContext {
  generatedChapterIndexes: number[]
  plannedChapterIndexes: number[]
}

export type StreamEventPartKind = 'prefix' | 'type' | 'message' | 'warning'

export interface StreamEventPart {
  kind: StreamEventPartKind
  text: string
}

const FLOW_STEPS: Array<Omit<FlowStep, 'active' | 'done'>> = [
  { key: 'import', number: '', label: '导入' },
  { key: 'convert', number: '', label: '转换' },
  { key: 'preview', number: '', label: '预览' },
  { key: 'polish', number: '', label: '打磨' },
  { key: 'export', number: '', label: '导出' },
]

export function buildFlowSteps(current: FlowStepKey): FlowStep[] {
  const activeIndex = FLOW_STEPS.findIndex((step) => step.key === current)

  return FLOW_STEPS.map((step, index) => ({
    ...step,
    active: step.key === current,
    done: activeIndex > index,
  }))
}

function clampProgress(value: number) {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round(value)))
}

export function buildPipelinePhases(context: {
  completed: boolean
  convertError: string | null
  finishedSceneCount: number
  totalSceneCount: number
}): PipelinePhase[] {
  const generationProgress = context.totalSceneCount > 0
    ? clampProgress((context.finishedSceneCount / context.totalSceneCount) * 100)
    : 0
  const hasScenePlan = context.totalSceneCount > 0

  if (context.completed) {
    return [
      createPhase('analysis', 'A', '全局分析', '通读全文 · 抽人物/场景/线索', 100, 'done'),
      createPhase('generation', 'B', '逐场生成', '带全局+前场上下文 · 逐场流式', 100, 'done'),
      createPhase('persist', '✓', '组装落库', '生成剧本 · 持久化', 100, 'done'),
    ]
  }

  return [
    createPhase('analysis', 'A', '全局分析', '通读全文 · 抽人物/场景/线索', hasScenePlan ? 100 : 35, hasScenePlan ? 'done' : 'active'),
    createPhase(
      'generation',
      'B',
      '逐场生成',
      '带全局+前场上下文 · 逐场流式',
      generationProgress,
      context.convertError ? 'failed' : hasScenePlan ? 'active' : 'idle',
    ),
    createPhase('persist', '✓', '组装落库', '生成剧本 · 持久化', 0, 'idle'),
  ]
}

export function buildConvertProgressNote(context: ConvertProgressNoteContext) {
  const chapterText = context.currentChapterIndex ? String(context.currentChapterIndex) : '?'
  const totalSceneText = context.totalSceneCount || '?'
  return `第 ${chapterText} 章正在生成中  ${context.finishedSceneCount} / ${totalSceneText} 场`
}

export function resolveCurrentConvertChapterIndex(context: CurrentConvertChapterContext) {
  const indexes = [...context.generatedChapterIndexes, ...context.plannedChapterIndexes]
    .filter((chapterIndex) => Number.isFinite(chapterIndex) && chapterIndex > 0)
  return indexes.length > 0 ? Math.max(...indexes) : 1
}

function createPhase(
  key: PipelinePhase['key'],
  mark: string,
  title: string,
  description: string,
  progress: number,
  status: PipelinePhaseStatus,
): PipelinePhase {
  return {
    key,
    mark,
    title,
    description,
    progress,
    status,
  }
}

export function formatStreamEvent(event: ConvertEventItem) {
  return `event: ${event.type} ${event.message}`
}

export function buildStreamEventParts(event: ConvertEventItem): StreamEventPart[] {
  const lines = event.message.split('\n')
  const parts: StreamEventPart[] = [
    { kind: 'prefix', text: 'event' },
    { kind: 'type', text: event.type },
  ]

  lines.forEach((line, index) => {
    const kind: StreamEventPartKind = line.includes('非中文表达') ? 'warning' : 'message'
    parts.push({
      kind,
      text: `${index > 0 ? '\n' : ''}${line}`,
    })
  })

  return parts
}
