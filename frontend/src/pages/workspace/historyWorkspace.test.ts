import {
  buildHistoryConversionActions,
  buildLatestConversionStatusLabel,
  selectWorkspaceNovel,
} from './historyWorkspaceModel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

assert(selectWorkspaceNovel({ selectedNovelId: null }, 'nv-history').selectedNovelId === 'nv-history', '工作台选择小说后应记录选中 novelId')
assert(buildLatestConversionStatusLabel(null) === '未转换', '没有最近转换时工作台应展示未转换')
assert(
  buildLatestConversionStatusLabel({ status: 'RUNNING', updatedAt: null }) === '转换中',
  'RUNNING 状态应展示转换中',
)
assert(
  buildLatestConversionStatusLabel({ status: 'FAILED', updatedAt: '2026-06-17T02:10:00Z' }) === '转换失败',
  'FAILED 状态应展示转换失败',
)
assert(
  buildLatestConversionStatusLabel({ status: 'COMPLETED', updatedAt: '2026-06-17T02:10:00Z' }) === '已完成',
  'COMPLETED 状态应展示已完成',
)

const historyWithoutConversionActions = buildHistoryConversionActions({
  novelId: 'nv-none',
  title: '未转换小说',
  totalChapters: 3,
  totalWordCount: 3000,
  createdAt: '2026-06-17T02:10:00Z',
})
assert(historyWithoutConversionActions.map((action) => action.label).join(',') === '使用这本', '未转换小说只应展示使用入口')

const failedHistoryActions = buildHistoryConversionActions({
  novelId: 'nv-failed',
  title: '失败小说',
  totalChapters: 3,
  totalWordCount: 3000,
  createdAt: '2026-06-17T02:10:00Z',
  latestConversionId: 'cv-failed',
  latestConversionType: 'ANIME',
  latestConversionStatus: 'FAILED',
})
assert(
  failedHistoryActions.map((action) => action.label).join(',') === '使用这本,继续转换',
  '失败转换应提供继续转换入口',
)

const completedHistoryActions = buildHistoryConversionActions({
  novelId: 'nv-completed',
  title: '已完成小说',
  totalChapters: 3,
  totalWordCount: 3000,
  createdAt: '2026-06-17T02:10:00Z',
  latestConversionId: 'cv-completed',
  latestConversionType: 'ANIME',
  latestConversionStatus: 'COMPLETED',
})
assert(
  completedHistoryActions.map((action) => action.label).join(',') === '使用这本,查看剧本,导出 YAML',
  '已完成转换应提供查看剧本和导出 YAML 入口',
)
