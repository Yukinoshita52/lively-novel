import type { ConvertEventItem } from '../../types/novel'

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
      createPhase('analysis', 'A', '章节切场', '读取章节 · 拆分场景单元', 100, 'done'),
      createPhase('generation', 'B', '剧本生成', '逐场生成 YAML · 更新全局状态', 100, 'done'),
      createPhase('persist', '✓', '结果整理', '持久化场景 · 准备预览导出', 100, 'done'),
    ]
  }

  return [
    createPhase('analysis', 'A', '章节切场', '读取章节 · 拆分场景单元', hasScenePlan ? 100 : 35, hasScenePlan ? 'done' : 'active'),
    createPhase(
      'generation',
      'B',
      '剧本生成',
      '逐场生成 YAML · 更新全局状态',
      generationProgress,
      context.convertError ? 'failed' : hasScenePlan ? 'active' : 'idle',
    ),
    createPhase('persist', '✓', '结果整理', '持久化场景 · 准备预览导出', 0, 'idle'),
  ]
}

export function buildConvertProgressNote(context: ConvertProgressNoteContext) {
  if (context.totalSceneCount <= 0) {
    return '正在读取章节并拆分场景'
  }
  const chapterText = context.currentChapterIndex ? String(context.currentChapterIndex) : '?'
  return `第 ${chapterText} 章正在生成中  ${context.finishedSceneCount} / ${context.totalSceneCount} 场`
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
