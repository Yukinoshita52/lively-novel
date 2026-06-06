import {
  createInitialAppFlowState,
  enterConvertPage,
  enterPreviewPage,
  returnToConvertPage,
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
