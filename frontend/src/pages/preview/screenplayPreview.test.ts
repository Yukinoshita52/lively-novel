import type { GeneratedSceneSummary } from '../../types/novel'
import {
  buildYamlDownloadFileName,
  buildPreviewTabs,
  buildScriptBlockRows,
  buildSceneHeadingText,
  buildSceneOutlineItems,
  buildSceneTableRows,
  buildPreviewActions,
  resolveAdjacentSceneKeys,
  mapPersistedScenesToGeneratedScenes,
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
        { type: 'ACTION', text: '林秋把书包放在桌上。' },
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
assert(buildYamlDownloadFileName(' 她比烟花寂寞 ') === '她比烟花寂寞-screenplay.yaml', 'YAML 文件名应使用作品标题')
assert(buildYamlDownloadFileName('') === 'screenplay.yaml', '标题为空时应使用默认 YAML 文件名')
assert(buildPreviewTabs('script')[0].active, '预览页默认应高亮剧本 tab')
assert(buildPreviewTabs('scene-table')[1].label === '场景表', '预览页应提供场景表 tab')
const previewActions = buildPreviewActions(true)
assert(previewActions.primary.label === '打磨本场', '预览页主动作应进入单场打磨')
assert(!previewActions.secondary.some((action) => action.label.includes('导出')), '预览页不应提供导出 YAML 动作')
assert(buildSceneTableRows(scenes)[0].sceneNumber === 'S1', '场景表应沿用场景大纲排序编号')
assert(buildSceneTableRows(scenes)[0].interiorText === '内景', '场景表应展示内景/外景')
assert(buildSceneTableRows(scenes)[0].location === '教室', '场景表应展示地点')
assert(buildSceneTableRows(scenes)[0].sourceChapterText === 'CH1', '场景表应展示源章节')
const scriptRows = buildScriptBlockRows(scenes[1])
assert(scriptRows[0].type === 'ACTION', '剧本预览应按 scriptBlocks 渲染动作块')
assert(scriptRows[1].type === 'DIALOGUE', '剧本预览应保留对白块顺序')
assert(scriptRows[2].text === '她抬头看向窗外。', '剧本预览应支持动作与对白交错')
assert(scriptRows[3].type === 'TRANSITION', '剧本预览应保留转场块')

const persistedScenes = mapPersistedScenesToGeneratedScenes([
  {
    chapterIndex: 1,
    sceneIndexInChapter: 1,
    scene: scenes[1].scene,
  },
])
assert(persistedScenes[0].title === '内景 — 教室 — 午后', '持久化场景应使用 heading 生成预览标题')
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
    scene: scenes[0].scene,
  },
  { totalChapters: 3 },
)

assert(
  warningSceneUpdate?.event?.message ===
    '已生成第 4 章第 11 场：后记\n该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。',
  '带提示的 scene_completed 应展示生成场次并追加后端提示',
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
assert(failedUpdate?.event.message === '转换中断，可继续转换；系统会跳过已完成部分。', 'failed 事件应使用后端用户提示')
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
