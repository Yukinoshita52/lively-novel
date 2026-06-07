import {
  buildImportEntryActions,
  buildImportResultFromConvertContext,
  buildImportTextAreas,
  buildScreenplayTypeCards,
  resolveEditableTitle,
  selectHistoryNovel,
  selectUploadedNovel,
} from './importPageModel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const actionsWithoutResult = buildImportEntryActions(false)
assert(actionsWithoutResult.primary.label === '开始分析', '导入页主按钮应保留开始分析')
assert(actionsWithoutResult.primary.enabled === false, '未识别出小说前不能开始分析')
assert(
  actionsWithoutResult.secondary.every((action) => !action.label.includes('打磨')),
  '导入页不应出现单章打磨入口',
)

const actionsWithResult = buildImportEntryActions(true)
assert(actionsWithResult.primary.enabled, '识别出 3 章以上小说后应允许开始分析')
assert(
  actionsWithResult.secondary.every((action) => !action.label.includes('打磨')),
  '可开始分析后导入页仍不应出现打磨入口',
)

assert(
  buildImportTextAreas().every((item) => !item.label.includes('粘贴正文')),
  '导入页不应再暴露粘贴正文区域',
)

assert(
  selectHistoryNovel({ selectedNovelId: null }, 'nv-history').selectedNovelId === 'nv-history',
  '选择历史小说后应记录选中 novelId',
)
assert(
  selectUploadedNovel({ selectedNovelId: 'nv-history' }).selectedNovelId === null,
  '重新上传 txt 后应清空历史选中状态',
)

const screenplayTypes = buildScreenplayTypeCards('ANIME')
assert(screenplayTypes.length === 5, '剧本类型应包含五种原型卡片')
assert(screenplayTypes[0].code === 'ANIME', '动画剧本应是第一个类型')
assert(screenplayTypes[0].badge === 'ANIME', '动画剧本应显示 ANIME 徽标')
assert(screenplayTypes[0].description === 'TV单集 ~20-24min', '动画剧本应展示原型里的时长描述')
assert(screenplayTypes[0].active, '当前选择 ANIME 时动画剧本卡应高亮')
assert(screenplayTypes[1].badge === 'FILM', '影视剧本应显示 FILM 徽标')
assert(screenplayTypes[3].badge === 'SOON', '暂未开放类型应显示 SOON 徽标')

const restoredImportResult = buildImportResultFromConvertContext({
  novelId: 'nv-history',
  title: '历史小说',
  totalChapters: 3,
  screenplayType: 'ANIME',
  chapters: [
    { chapterIndex: 1, title: '第一章', wordCount: 1000 },
    { chapterIndex: 2, title: '第二章', wordCount: 1200 },
    { chapterIndex: 3, title: '第三章', wordCount: 1300 },
  ],
})
assert(restoredImportResult?.novelId === 'nv-history', '回到导入页时应能从转换上下文恢复选中小说')
assert(restoredImportResult?.totalWordCount === 3500, '恢复导入结果时应保留章节字数汇总')
assert(resolveEditableTitle('  新标题  ', '旧标题') === '新标题', '作品标题应去除首尾空白')
assert(resolveEditableTitle('   ', '旧标题') === '旧标题', '标题输入为空时应回退到当前标题')
