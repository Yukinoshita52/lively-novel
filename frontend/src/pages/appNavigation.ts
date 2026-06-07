import type { ImportFlowContext, ScreenplayConvertContext } from '../types/novel'

export type AppPageKey = 'import' | 'single-scene' | 'convert' | 'preview' | 'polish' | 'export'

export interface FlowStepNavigationState {
  hasGeneratedScenes: boolean
  completed: boolean
}

export type FlowStepNavigation = Record<AppPageKey, {
  clickable: boolean
  enabled: boolean
  target: AppPageKey
  message?: string
}>

export interface AppFlowState {
  page: AppPageKey
  singleSceneContext: ImportFlowContext | null
  convertContext: ScreenplayConvertContext | null
  selectedSceneKey?: string
}

export function createInitialAppFlowState(): AppFlowState {
  return {
    page: 'import',
    singleSceneContext: null,
    convertContext: null,
  }
}

export function enterConvertPage(state: AppFlowState, context: ScreenplayConvertContext): AppFlowState {
  return {
    ...state,
    page: 'convert',
    convertContext: context,
  }
}

export function enterPreviewPage(state: AppFlowState): AppFlowState {
  return {
    ...state,
    page: 'preview',
  }
}

export function returnToConvertPage(state: AppFlowState): AppFlowState {
  return {
    ...state,
    page: 'convert',
  }
}

export function resumeConvertPage(state: AppFlowState): AppFlowState {
  return {
    ...state,
    page: 'convert',
    convertContext: state.convertContext ? { ...state.convertContext } : state.convertContext,
  }
}

export function enterPolishPage(state: AppFlowState, selectedSceneKey: string): AppFlowState {
  return {
    ...state,
    page: 'polish',
    selectedSceneKey,
  }
}

export function selectPolishScene(state: AppFlowState, selectedSceneKey: string): AppFlowState {
  return {
    ...state,
    page: 'polish',
    selectedSceneKey,
  }
}

export function returnToPreviewPage(state: AppFlowState): AppFlowState {
  return {
    ...state,
    page: 'preview',
  }
}

export function enterExportPage(state: AppFlowState): AppFlowState {
  return {
    ...state,
    page: 'export',
  }
}

export function resolveFlowStepNavigation(
  state: AppFlowState,
  navigationState: FlowStepNavigationState,
): FlowStepNavigation {
  const hasConvertContext = Boolean(state.convertContext)
  const hasGeneratedScenes = hasConvertContext && navigationState.hasGeneratedScenes
  const canExport = hasGeneratedScenes && navigationState.completed

  return {
    import: { clickable: true, enabled: true, target: 'import' },
    'single-scene': { clickable: false, enabled: false, target: 'single-scene' },
    convert: {
      clickable: true,
      enabled: hasConvertContext,
      target: 'convert',
      message: hasConvertContext ? undefined : '请先导入小说并开始分析。',
    },
    preview: {
      clickable: true,
      enabled: hasGeneratedScenes,
      target: 'preview',
      message: hasGeneratedScenes ? undefined : '请先开始分析，生成场景后再进入预览。',
    },
    polish: {
      clickable: true,
      enabled: hasGeneratedScenes,
      target: 'polish',
      message: hasGeneratedScenes ? undefined : '请先生成场景，再进入打磨。',
    },
    export: {
      clickable: true,
      enabled: canExport,
      target: 'export',
      message: canExport ? undefined : '请先完成转换，再进入导出。',
    },
  }
}
