import type { ImportFlowContext, ScreenplayConvertContext } from '../types/novel'

export type AppPageKey = 'import' | 'single-scene' | 'convert' | 'preview'

export interface AppFlowState {
  page: AppPageKey
  singleSceneContext: ImportFlowContext | null
  convertContext: ScreenplayConvertContext | null
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
