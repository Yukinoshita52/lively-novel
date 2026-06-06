import {
  createInitialAppFlowState,
  enterConvertPage,
  enterExportPage,
  enterPolishPage,
  enterPreviewPage,
  returnToPreviewPage,
  returnToConvertPage,
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
