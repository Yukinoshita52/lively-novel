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

const FLOW_STEPS: Array<Omit<FlowStep, 'active' | 'done'>> = [
  { key: 'import', number: '②', label: '导入' },
  { key: 'convert', number: '③', label: '转换' },
  { key: 'preview', number: '④', label: '预览' },
  { key: 'polish', number: '⑤', label: '打磨' },
  { key: 'export', number: '⑥', label: '导出' },
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
