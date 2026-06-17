import {
  createInitialAppFlowState,
  enterConvertPage,
  enterConvertPageForHistoryReplay,
  enterExportPage,
  enterPolishPage,
  enterPolishPageWithFallback,
  enterPreviewPage,
  returnToPreviewPage,
  returnToConvertPage,
  resolveFlowStepNavigation,
  retryConvertPage,
  resumeConvertPage,
  selectPolishScene,
} from './appNavigation.ts'
import type { ScreenplayConvertContext } from '../types/novel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const context: ScreenplayConvertContext = {
  novelId: 'nv-1234abcd',
  title: '测试小说',
  totalChapters: 3,
  chapters: [],
  screenplayType: 'ANIME',
}

let state = createInitialAppFlowState()
state = enterConvertPage(state, context)
const sessionBeforePreview = state.convertContext
state = enterPreviewPage(state)

assert(state.page === 'preview', '进入预览页时应切换到 preview 页面')
assert(state.convertContext === sessionBeforePreview, '进入预览页不应清空或替换转换上下文')

state = returnToConvertPage(state)
assert(state.page === 'convert', '从预览返回时应回到转换页')
assert(state.convertContext === sessionBeforePreview, '返回转换页应继续使用同一个转换上下文')

state = enterPreviewPage(state)
state = enterPolishPage(state, '1-2')
assert(state.page === 'polish', '从预览选择场景后应进入打磨页')
assert(state.selectedSceneKey === '1-2', '进入打磨页应记录选中的场景 key')
assert(state.convertContext === sessionBeforePreview, '进入打磨页不应清空转换上下文')

state = selectPolishScene(state, '2-1')
assert(state.page === 'polish', '打磨页切换场景时应停留在打磨页')
assert(state.selectedSceneKey === '2-1', '打磨页切换场景时应更新选中场景 key')

state = returnToPreviewPage(state)
assert(state.page === 'preview', '从打磨页返回时应回到预览页')
assert(state.selectedSceneKey === '2-1', '返回预览页应保留最新选中的场景 key')

state = enterExportPage(state)
assert(state.page === 'export', '从打磨页后续流程应能进入导出页')
assert(state.convertContext === sessionBeforePreview, '进入导出页不应清空转换上下文')

state = {
  ...state,
  selectedSceneKey: undefined,
}
state = enterPolishPageWithFallback(state, '1-1')
assert(state.page === 'polish', '从导出页返回打磨时应进入打磨页')
assert(state.selectedSceneKey === '1-1', '从导出页返回打磨时应默认选择第一场')

state = resumeConvertPage(state)
assert(state.page === 'convert', '继续转换应回到转换页')
assert(state.convertContext !== sessionBeforePreview, '继续转换应替换 context 对象以重新发起转换请求')
assert(state.convertContext?.novelId === sessionBeforePreview?.novelId, '继续转换应保留原 novelId')

state = retryConvertPage({
  ...state,
  convertContext: {
    ...context,
    restoredConversionId: 'cv-failed',
    restoredConversionStatus: 'FAILED',
    restoredConversionMode: 'static',
  },
})
assert(state.page === 'convert', '重新尝试应回到转换页')
assert(state.convertContext?.restoredConversionId === undefined, '重新尝试应清除历史 conversionId')
assert(state.convertContext?.restoredConversionStatus === undefined, '重新尝试应清除历史转换状态')
assert(state.convertContext?.restoredGeneratedScenes === undefined, '重新尝试应清除历史生成场景')

const restoredStaticState = enterConvertPageForHistoryReplay({
  page: 'preview',
  convertContext: {
    ...context,
    restoredConversionId: 'cv-completed',
    restoredConversionStatus: 'COMPLETED',
    restoredConversionMode: 'static',
  },
})
assert(restoredStaticState.page === 'convert', '进入转换页时应切到转换页')
assert(restoredStaticState.convertContext?.restoredConversionMode === 'stream', '已完成历史转换进入转换页时应切换为 SSE 重放模式')
assert(restoredStaticState.convertContext !== context, '切换 SSE 重放模式时应替换 context 对象以触发会话重建')

const navigationWithoutContext = resolveFlowStepNavigation(createInitialAppFlowState(), {
  hasGeneratedScenes: false,
  completed: false,
})
assert(navigationWithoutContext.import.enabled, '导入步骤应始终可点击')
assert(navigationWithoutContext.convert.clickable, '没有转换上下文时转换步骤仍应可点击以提示用户')
assert(!navigationWithoutContext.convert.enabled, '没有转换上下文时不应跳转转换页')
assert(!navigationWithoutContext.preview.enabled, '没有已生成场景时不应跳转预览页')
assert(
  navigationWithoutContext.convert.message === '请先导入小说并开始分析。',
  '没有转换上下文时点击转换应提示先导入',
)

const navigationWithScenes = resolveFlowStepNavigation({
  ...createInitialAppFlowState(),
  convertContext: context,
}, {
  hasGeneratedScenes: true,
  completed: false,
})
assert(navigationWithScenes.convert.enabled, '存在转换上下文时应可跳转转换页')
assert(navigationWithScenes.preview.enabled, '已有生成场景时应可跳转预览页')
assert(navigationWithScenes.polish.enabled, '已有生成场景时应可跳转打磨页')
assert(!navigationWithScenes.export.enabled, '未完成转换时不应跳转导出页')

const navigationCompleted = resolveFlowStepNavigation({
  ...createInitialAppFlowState(),
  convertContext: context,
}, {
  hasGeneratedScenes: true,
  completed: true,
})
assert(navigationCompleted.export.enabled, '转换完成后应可跳转导出页')
