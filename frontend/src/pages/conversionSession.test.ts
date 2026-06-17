import {
  createConversionSessionStateFromContext,
  createInitialConversionSessionState,
  createRestoredConversionContextFromDetail,
  isRestoredCompletedConversionContext,
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

const restoredCompletedContext: ScreenplayConvertContext = {
  ...context,
  restoredConversionId: 'cv-completed',
  restoredConversionStatus: 'COMPLETED',
  restoredGeneratedScenes: [
    {
      chapterIndex: 1,
      sceneIndexInChapter: 1,
      title: '第一场',
      scene,
    },
  ],
}
assert(isRestoredCompletedConversionContext(restoredCompletedContext), '带已完成 conversionId 的历史上下文应识别为已恢复完成')

const restoredCompletedState = createConversionSessionStateFromContext(restoredCompletedContext)
assert(restoredCompletedState.conversionId === 'cv-completed', '已完成历史上下文应恢复 conversionId')
assert(restoredCompletedState.completed, '已完成历史上下文应直接进入完成态')
assert(!restoredCompletedState.running, '已完成历史上下文不应重新启动转换')
assert(!restoredCompletedState.connecting, '已完成历史上下文不应连接 SSE')
assert(resolvePreviewEntryState(restoredCompletedState).enabled, '已完成历史上下文应允许进入预览')
assert(restoredCompletedState.generatedScenes.length === 1, '已完成历史上下文应恢复已落库场景供打磨页使用')
assert(restoredCompletedState.events[0]?.message === '已载入历史转换：共 1 场。', '已完成历史上下文应展示历史载入事件')

const streamRestoredCompletedState = createConversionSessionStateFromContext({
  ...restoredCompletedContext,
  restoredConversionMode: 'stream',
})
assert(streamRestoredCompletedState.running, '已完成历史上下文进入转换页时应启动 SSE 历史重放')
assert(streamRestoredCompletedState.connecting, '已完成历史上下文进入转换页时应连接 SSE')
assert(streamRestoredCompletedState.events.length === 0, 'SSE 历史重放应等待后端返回真实事件流')

const restoredFailedContext = createRestoredConversionContextFromDetail(context, {
  conversionId: 'cv-failed',
  novelId: 'nv-1234abcd',
  screenplayType: 'ANIME',
  status: 'FAILED',
  updatedAt: '2026-06-17T09:15:30Z',
  errorMessage: '第 4 章第 11 场生成失败',
  scenes: [
    {
      chapterIndex: 4,
      sceneIndexInChapter: 10,
      title: '结业式后的逃生楼梯',
      scene,
    },
  ],
})
const restoredFailedState = createConversionSessionStateFromContext(restoredFailedContext)
assert(restoredFailedState.conversionId === 'cv-failed', '失败历史上下文应恢复 conversionId')
assert(restoredFailedState.generatedScenes.length === 1, '失败历史上下文应恢复已落库场景供预览和打磨共享')
assert(!restoredFailedState.running, '失败历史上下文静态恢复不应立即重新启动转换')
assert(resolvePreviewEntryState(restoredFailedState).enabled, '失败历史上下文有已生成场景时应允许预览')
assert(resolveResumeEntryState(restoredFailedState).enabled, '失败历史上下文应允许继续转换')
assert(restoredFailedState.convertError?.includes('第 4 章第 11 场生成失败') === true, '失败历史上下文应恢复错误详情')
