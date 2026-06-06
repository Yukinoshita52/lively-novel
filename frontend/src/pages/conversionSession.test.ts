import {
  createInitialConversionSessionState,
  reduceConversionSessionEvent,
  resolvePreviewEntryState,
  resolveResumeEntryState,
} from './conversionSession.ts'
import type { ScreenplayConvertContext, SceneResult } from '../types/novel.ts'

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

const scene: SceneResult = {
  sceneId: 'scene-1',
  heading: { interior: true, location: '教室', timeOfDay: '午后' },
  actionLines: ['林秋把书包放在桌上。'],
  dialogueBlocks: [],
  visualizedInnerThoughts: [],
  transitions: [],
  sourceChapter: 1,
  sourceText: '第一段原文。',
}

let state = createInitialConversionSessionState(context)
assert(state.context.novelId === 'nv-1234abcd', '初始会话应保留转换上下文')
assert(state.running, '初始会话应处于后台转换中')
assert(!resolvePreviewEntryState(state).enabled, '没有 conversionId 且没有场景时不应进入预览')

state = reduceConversionSessionEvent(state, 'started', {
  conversionId: 'cv-1234abcd',
  totalChapters: 3,
})
assert(state.conversionId === 'cv-1234abcd', 'started 事件应记录 conversionId')
assert(state.running, '收到 started 后后台转换仍应继续')

state = reduceConversionSessionEvent(state, 'scene_completed', {
  conversionId: 'cv-1234abcd',
  chapterIndex: 1,
  sceneIndexInChapter: 1,
  title: '第一场',
  scene,
})
assert(state.generatedScenes.length === 1, 'scene_completed 应追加已生成场景')
assert(resolvePreviewEntryState(state).enabled, '有 conversionId 和场景后应允许进入预览')
assert(resolvePreviewEntryState(state).label === '查看已生成预览', '转换中进入预览应显示实时预览文案')
assert(state.running, '进入预览的条件成立不应停止后台转换')

state = reduceConversionSessionEvent(state, 'completed', {
  conversionId: 'cv-1234abcd',
  totalChapters: 3,
})
assert(state.completed, 'completed 事件应标记转换完成')
assert(!state.running, 'completed 后后台转换才结束')
assert(resolvePreviewEntryState(state).label === '进入预览', '转换完成后应显示正式进入预览文案')

let failedState = createInitialConversionSessionState(context)
failedState = reduceConversionSessionEvent(failedState, 'started', {
  conversionId: 'cv-resume',
})
failedState = reduceConversionSessionEvent(failedState, 'scene_completed', {
  conversionId: 'cv-resume',
  chapterIndex: 4,
  sceneIndexInChapter: 10,
  title: '结业式后的逃生楼梯',
  scene,
})
failedState = reduceConversionSessionEvent(failedState, 'failed', {
  conversionId: 'cv-resume',
  message: '转换未完成，请继续转换。',
  reason: '第 4 章第 11 场生成失败',
})

assert(resolveResumeEntryState(failedState).enabled, '失败且后台已停止时应允许继续转换')
assert(resolveResumeEntryState(failedState).label === '继续转换', '继续入口文案应明确不是重新上传')
assert(failedState.convertError?.includes('第 4 章第 11 场生成失败') === true, '失败原因应进入可见错误详情')
