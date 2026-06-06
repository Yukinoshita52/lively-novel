import type { ImportFlowContext, ScreenplayConvertContext } from '../types/novel'

export type AppPageKey = 'import' | 'single-scene' | 'convert' | 'preview' | 'polish' | 'export'

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
