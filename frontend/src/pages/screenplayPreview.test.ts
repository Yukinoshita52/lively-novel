import type { GeneratedSceneSummary } from '../types/novel'
import {
  buildYamlDownloadFileName,
  buildSceneHeadingText,
  buildSceneOutlineItems,
  getSourcePreview,
  resolveConvertEventUpdate,
  resolveSelectedScene,
} from './screenplayPreview.ts'

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
      actionLines: [],
      dialogueBlocks: [],
      visualizedInnerThoughts: [],
      transitions: [],
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
      actionLines: [],
      dialogueBlocks: [],
      visualizedInnerThoughts: [],
      transitions: [],
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
assert(
  resolveSelectedScene([...scenes, { ...scenes[0], sceneIndexInChapter: 3, title: '第三场' }], '1-1')?.scene.sceneId ===
    's1',
  '流式追加新场景时不应覆盖用户已选场景',
)
assert(getSourcePreview('一二三四五六', false, 4) === '一二三四…', '折叠原文应截断并加省略号')
assert(getSourcePreview('一二三四五六', true, 4) === '一二三四五六', '展开原文应显示全文')
assert(buildYamlDownloadFileName(' 她比烟花寂寞 ') === '她比烟花寂寞-screenplay.yaml', 'YAML 文件名应使用作品标题')
assert(buildYamlDownloadFileName('') === 'screenplay.yaml', '标题为空时应使用默认 YAML 文件名')

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

const failedUpdate = resolveConvertEventUpdate(
  'failed',
  {
    message: '转换未完成，请调整文本后重试。',
    reason: '章节切场退化为整章单场',
  },
  { totalChapters: 3 },
)

assert(failedUpdate?.event !== undefined, 'failed 事件应生成事件流记录')
assert(failedUpdate.event.type === 'failed', 'failed 事件应进入事件流')
assert(failedUpdate?.event.message === '转换未完成，请调整文本后重试。', 'failed 事件应使用后端用户提示')
assert(failedUpdate?.convertError === '转换未完成，请调整文本后重试。', 'failed 事件应设置转换错误')
