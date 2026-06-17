import type { GeneratedSceneSummary } from '../../types/novel'
import {
  buildYamlDownloadFileName,
  buildPreviewTabs,
  buildScriptBlockRows,
  buildSceneHeadingText,
  buildSceneOutlineItems,
  buildSceneTableRows,
  buildPreviewActions,
  buildGenerationQualityWarnings,
  buildPreviewScrollKey,
  buildSceneQualityWarnings,
  resolveAdjacentSceneKeys,
  mapPersistedScenesToGeneratedScenes,
  getSourceDisplayText,
  getSourcePreview,
  resolveSelectedScene,
} from './screenplayPreview.ts'
import { resolveConvertEventUpdate } from './convertEventUpdate.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const scenes: GeneratedSceneSummary[] = [
  {
    chapterIndex: 1,
    sceneIndexInChapter: 2,
    title: '第二场',
    scene: {
      sceneId: 's2',
      heading: {
        interior: false,
        location: '操场',
        timeOfDay: '黄昏',
      },
      scriptBlocks: [],
      sourceChapter: 1,
      sourceText: '第二场原文',
    },
  },
  {
    chapterIndex: 1,
    sceneIndexInChapter: 1,
    title: '第一场',
    scene: {
      sceneId: 's1',
      heading: {
        interior: true,
        location: '教室',
        timeOfDay: '午后',
      },
      scriptBlocks: [
        { type: 'SHOT', text: '教室后排。午后的光落在桌面。' },
        { type: 'ACTION', text: '林秋把书包放在桌上。' },
        { type: 'INSERT', text: '桌角的旧钥匙。' },
        { type: 'SFX', text: '远处传来下课铃。' },
        { type: 'VO', character: '林秋', parenthetical: '画外音', line: '我还不能停在这里。' },
        { type: 'DIALOGUE', character: '林秋', parenthetical: '低声', line: '今天先到这里。' },
        { type: 'ACTION', text: '她抬头看向窗外。' },
        { type: 'TRANSITION', text: '切至：走廊' },
      ],
      sourceChapter: 1,
      sourceText: '第一场原文',
    },
  },
]

const duplicateSceneIdScenes: GeneratedSceneSummary[] = [
  scenes[1],
  {
    ...scenes[0],
    scene: {
      ...scenes[0].scene,
      sceneId: scenes[1].scene.sceneId,
    },
  },
]

assert(buildSceneHeadingText(scenes[1].scene.heading) === '内景 — 教室 — 午后', '应渲染中文剧本场景标题')
assert(buildSceneOutlineItems(scenes)[0].scene.sceneId === 's1', '场景大纲应按章内场次排序')
assert(
  new Set(buildSceneOutlineItems(duplicateSceneIdScenes).map((scene) => scene.key)).size === 2,
  '场景 key 不应依赖可能重复的 LLM sceneId',
)
assert(resolveSelectedScene(scenes, undefined)?.scene.sceneId === 's2', '未选择时应默认显示最新生成场景')
assert(resolveSelectedScene(scenes, '1-1')?.scene.sceneId === 's1', '应按稳定场景 key 选中指定场景')
const adjacentFromFirst = resolveAdjacentSceneKeys(scenes, '1-1')
assert(!adjacentFromFirst.previousKey, '第一个场景不应有上一场景')
assert(adjacentFromFirst.nextKey === '1-2', '第一个场景的下一场景应指向第二个场景')
const adjacentFromSecond = resolveAdjacentSceneKeys(scenes, '1-2')
assert(adjacentFromSecond.previousKey === '1-1', '第二个场景的上一场景应指向第一个场景')
assert(!adjacentFromSecond.nextKey, '最后一个场景不应有下一场景')
assert(
  resolveSelectedScene([...scenes, { ...scenes[0], sceneIndexInChapter: 3, title: '第三场' }], '1-1')?.scene.sceneId ===
    's1',
  '流式追加新场景时不应覆盖用户已选场景',
)
assert(getSourcePreview('一二三四五六', false, 4) === '一二三四…', '折叠原文应截断并加省略号')
assert(getSourcePreview('一二三四五六', true, 4) === '一二三四五六', '展开原文应显示全文')
assert(getSourceDisplayText('第一段原文') === '第一段原文', '原文 tab 应展示完整原文')
assert(getSourceDisplayText('') === '暂无原文内容。', '原文为空时应展示空状态文本')
assert(buildYamlDownloadFileName(' 她比烟花寂寞 ') === '她比烟花寂寞-screenplay.yaml', 'YAML 文件名应使用作品标题')
assert(buildYamlDownloadFileName('') === 'screenplay.yaml', '标题为空时应使用默认 YAML 文件名')
assert(buildPreviewTabs('script')[0].active, '预览页默认应高亮剧本 tab')
assert(
  buildPreviewTabs('script')
    .map((tab) => tab.label)
    .join('/') === '剧本/原文/场景表',
  '预览页 tab 应按剧本、原文、场景表排序',
)
assert(buildPreviewTabs('source')[1].active, '预览页应支持独立原文 tab')
assert(buildPreviewTabs('scene-table')[2].label === '场景表', '预览页应提供场景表 tab')
assert(buildPreviewScrollKey('1-2', 'source') === '1-2:source', '预览滚动位置应按场景和 tab 独立缓存')
const previewActions = buildPreviewActions(true)
assert(previewActions.primary.label === '打磨本场', '预览页主动作应进入单场打磨')
assert(!previewActions.secondary.some((action) => action.label.includes('导出')), '预览页不应提供导出 YAML 动作')
assert(buildSceneTableRows(scenes)[0].sceneNumber === 'S1', '场景表应沿用场景大纲排序编号')
assert(buildSceneTableRows(scenes)[0].interiorText === '内景', '场景表应展示内景/外景')
assert(buildSceneTableRows(scenes)[0].location === '教室', '场景表应展示地点')
assert(buildSceneTableRows(scenes)[0].sourceChapterText === 'CH1', '场景表应展示源章节')
const sceneWithGenerationWarning: GeneratedSceneSummary = {
  ...scenes[1],
  scene: {
    ...scenes[1].scene,
    warnings: ['该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。'],
  },
}
const generationWarnings = buildSceneQualityWarnings(sceneWithGenerationWarning, '1-1', 'S1')
assert(generationWarnings[0].sceneKey === '1-1', '生成 warning 应绑定可定位场景 key')
assert(generationWarnings[0].sceneNumber === 'S1', '生成 warning 应保留场景展示编号')
assert(generationWarnings[0].severity === 'check', '语言漂移 warning 应表达为建议检查而不是失败')
assert(generationWarnings[0].message.includes('非中文表达'), '生成 warning 应保留后端面向用户提示')
const emptySceneWarnings = buildSceneQualityWarnings({
  ...scenes[0],
  scene: {
    ...scenes[0].scene,
    heading: { interior: true, location: '', timeOfDay: '未知时间' },
    scriptBlocks: [],
    actionLines: [],
    dialogueBlocks: [],
    transitions: [],
  },
}, '1-2', 'S2')
assert(
  emptySceneWarnings.some((warning) => warning.severity === 'blocking' && warning.title === '剧本正文为空'),
  '空剧本正文应被标记为阻断性检查项',
)
assert(
  emptySceneWarnings.some((warning) => warning.title === '地点待确认') &&
    emptySceneWarnings.some((warning) => warning.title === '时间待确认'),
  '未知地点和时间应生成轻量结构检查提示',
)
const aggregatedWarnings = buildGenerationQualityWarnings([sceneWithGenerationWarning])
assert(aggregatedWarnings.length === 1 && aggregatedWarnings[0].sceneKey === '1-1', '预览页 warning 汇总应可定位到场景')
const scriptRows = buildScriptBlockRows(scenes[1])
assert(scriptRows[0].type === 'SHOT', '剧本预览应保留镜头块')
assert(scriptRows[1].type === 'ACTION', '剧本预览应按 scriptBlocks 渲染动作块')
assert(scriptRows[2].type === 'INSERT', '剧本预览应保留插入特写块')
assert(scriptRows[3].type === 'SFX', '剧本预览应保留音效块')
assert(scriptRows[4].type === 'VO', '剧本预览应保留画外音块')
assert(scriptRows[5].type === 'DIALOGUE', '剧本预览应保留对白块顺序')
assert(scriptRows[6].text === '她抬头看向窗外。', '剧本预览应支持动作与对白交错')
assert(scriptRows[7].type === 'TRANSITION', '剧本预览应保留转场块')

const persistedScenes = mapPersistedScenesToGeneratedScenes([
  {
    chapterIndex: 1,
    sceneIndexInChapter: 1,
    title: '特典：比食欲更加重要的东西',
    scene: scenes[1].scene,
  },
])
assert(persistedScenes[0].title === '特典：比食欲更加重要的东西', '持久化场景应使用切场标题生成预览标题')
assert(persistedScenes[0].scene.sceneId === 's1', '持久化场景应保留 SceneDTO')

const startedUpdate = resolveConvertEventUpdate(
  'started',
  {
    conversionId: 'cv-1234abcd',
    totalChapters: 3,
  },
  { totalChapters: 3 },
)

assert(startedUpdate?.conversionId === 'cv-1234abcd', 'started 事件应记录 conversionId 供导出 YAML 使用')

const completedUpdate = resolveConvertEventUpdate(
  'completed',
  {
    conversionId: 'cv-1234abcd',
    totalChapters: 3,
  },
  { totalChapters: 3 },
)

assert(completedUpdate?.conversionId === 'cv-1234abcd', 'completed 事件应保留 conversionId 供完成后下载')

const warningSceneUpdate = resolveConvertEventUpdate(
  'scene_completed',
  {
    conversionId: 'cv-1234abcd',
    chapterIndex: 4,
    sceneIndexInChapter: 11,
    title: '后记',
    message: '该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。',
    warning: true,
    scene: scenes[0].scene,
  },
  { totalChapters: 3 },
)

assert(
  warningSceneUpdate?.event?.message ===
    '已生成第 4 章第 11 场：后记\n该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。',
  '带提示的 scene_completed 应展示生成场次并追加后端提示',
)
assert(
  warningSceneUpdate?.generatedScene?.scene.warnings?.[0] === '该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。',
  '带 warning 标记的 scene_completed 应把提示绑定到生成场景',
)

const analysisUpdatedUpdate = resolveConvertEventUpdate(
  'analysis_updated',
  {
    conversionId: 'cv-1234abcd',
    sceneId: 's1',
    plotSummary: '温水在家庭餐厅偶然撞见八奈见失恋。',
  },
  { totalChapters: 3 },
)

assert(analysisUpdatedUpdate?.event?.type === 'analysis_updated', 'analysis_updated 应进入事件流')
assert(
  analysisUpdatedUpdate?.event?.message === '全局状态已更新：温水在家庭餐厅偶然撞见八奈见失恋。',
  'analysis_updated 应展示滚动全局状态摘要',
)

const analysisRestoredUpdate = resolveConvertEventUpdate(
  'analysis_restored',
  {
    conversionId: 'cv-1234abcd',
    characterCount: 8,
    storylineCount: 2,
    activeThreadCount: 3,
    motifCount: 1,
    plotSummary: '温水已经被卷入八奈见失恋后的关系变化。',
  },
  { totalChapters: 3 },
)

assert(analysisRestoredUpdate?.event?.type === 'analysis_restored', 'analysis_restored 应进入事件流')
assert(
  analysisRestoredUpdate?.event?.message ===
    '已载入历史全局状态：已记录 8 名人物、2 条故事线、3 条活跃事件线、1 个重复意象。\n当前剧情概要：温水已经被卷入八奈见失恋后的关系变化。',
  'analysis_restored 应展示历史状态数量和剧情概要',
)

const replayedSplitUpdate = resolveConvertEventUpdate(
  'chapter_split',
  {
    conversionId: 'cv-1234abcd',
    chapterIndex: 1,
    sceneCount: 2,
    title: '第一章',
    replayed: true,
    message: '已载入历史切场',
  },
  { totalChapters: 3 },
)

assert(
  replayedSplitUpdate?.event?.message === '已载入历史切场：第 1 章，共 2 场',
  '历史 chapter_split 事件应显示历史载入文案',
)

const replayedSceneUpdate = resolveConvertEventUpdate(
  'scene_completed',
  {
    conversionId: 'cv-1234abcd',
    chapterIndex: 1,
    sceneIndexInChapter: 1,
    title: '第一场',
    replayed: true,
    message: '已载入历史场景',
    scene: scenes[1].scene,
  },
  { totalChapters: 3 },
)

assert(
  replayedSceneUpdate?.event?.message === '已载入历史场景：第 1 章第 1 场：第一场',
  '历史 scene_completed 事件应显示历史载入文案',
)

const failedUpdate = resolveConvertEventUpdate(
  'failed',
  {
    message: '转换中断，可继续转换；系统会跳过已完成部分。',
    reason: '章节切场退化为整章单场',
  },
  { totalChapters: 3 },
)

assert(failedUpdate?.event !== undefined, 'failed 事件应生成事件流记录')
assert(failedUpdate.event.type === 'failed', 'failed 事件应进入事件流')
assert(
  failedUpdate?.event.message === '转换中断，可继续转换；系统会跳过已完成部分。\n失败原因：章节切场退化为整章单场',
  'failed 事件日志应直接包含后端失败原因',
)
assert(failedUpdate?.convertError?.includes('转换中断，可继续转换；系统会跳过已完成部分。') === true, 'failed 事件应设置转换错误')
assert(failedUpdate?.convertError?.includes('章节切场退化为整章单场') === true, 'failed 事件应展示后端失败原因')

const languageDriftFailedUpdate = resolveConvertEventUpdate(
  'failed',
  {
    message: '转换中断，可继续转换；系统会跳过已完成部分。',
    reason:
      '单场剧本生成出现语言漂移：fieldPath=scriptBlocks[0].text, textPreview=书桌上摊开一本轻小说，台灯暖光下，作者雨森たきび的手指轻轻摩挲着书页边缘。',
  },
  { totalChapters: 3 },
)

assert(
  languageDriftFailedUpdate?.convertError?.includes('生成结果混入非中文表达') === true,
  '语言漂移失败应展示面向用户的原因',
)
assert(
  !languageDriftFailedUpdate?.convertError?.includes('fieldPath'),
  '语言漂移失败不应展示内部字段路径',
)
assert(
  !languageDriftFailedUpdate?.convertError?.includes('雨森たきび'),
  '语言漂移失败不应展示生成文本片段',
)
