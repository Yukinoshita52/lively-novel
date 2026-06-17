import {
  POLISH_YAML_SCROLLBAR_GUTTER_PX,
  POLISH_YAML_SPELL_CHECK,
  buildPolishYamlLineNumberTransform,
  buildYamlLineNumbers,
  buildPolishWorkspaceLayout,
  createPolishDraft,
  buildPolishSceneYaml,
  resetPolishDraft,
  updatePolishSceneYaml,
} from './screenplayPolish.ts'
import type { SceneResult } from '../../types/novel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const scene: SceneResult = {
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
    { type: 'TRANSITION', text: '切至：' },
  ],
  warnings: ['该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。'],
  sourceChapter: 1,
  sourceText: '第一场原文。',
}

const draft = createPolishDraft(scene)
assert(draft.scene !== scene, '本地打磨草稿不应直接复用原始 SceneResult 引用')
assert(draft.scene.warnings?.[0]?.includes('非中文表达') === true, '本地打磨草稿应保留生成质量提示')
assert(draft.savedScene.warnings?.[0]?.includes('非中文表达') === true, '本地打磨保存基线应保留生成质量提示')
assert(
  draft.sceneYamlText.includes('scriptBlocks:'),
  '本地打磨草稿应直接暴露可编辑 YAML',
)
const polishYaml = buildPolishSceneYaml(draft.scene)
assert(polishYaml.includes('sceneId: s1'), '本场结构预览应保留场景 ID')
assert(polishYaml.includes('scriptBlocks:'), '本场结构预览应保留可编辑剧本块')
assert(polishYaml.includes('  - type: SHOT'), '本场结构 YAML 应保留镜头块')
assert(polishYaml.includes('  - type: ACTION'), '本场结构 YAML 应使用与导出页一致的列表对象格式')
assert(polishYaml.includes('  - type: INSERT'), '本场结构 YAML 应保留插入特写块')
assert(polishYaml.includes('  - type: SFX'), '本场结构 YAML 应保留音效块')
assert(polishYaml.includes('  - type: VO'), '本场结构 YAML 应保留画外音块')
assert(!polishYaml.includes('  -\n    type: ACTION'), '本场结构 YAML 不应把列表横杠和 type 拆成两行')
assert(!polishYaml.includes('visualizedInnerThoughts'), '本场结构预览不应展示内心戏审计字段')
assert(!polishYaml.includes('sourceText'), '本场结构预览不应展示原文溯源字段')
assert(!polishYaml.includes('第一场原文。'), '本场结构预览不应展示原文内容')

const scriptDraft = updatePolishSceneYaml(
  draft,
  [
    'sceneId: s1',
    'heading:',
    '  interior: true',
    '  location: 走廊',
    '  timeOfDay: 午后',
    'scriptBlocks:',
    '  - type: SHOT',
    '    text: 走廊尽头。午后的光把地面拉成长条。',
    '  - type: ACTION',
    '    text: 她抬头看向窗外。',
    '  - type: INSERT',
    '    text: 桌角的旧钥匙。',
    '  - type: SFX',
    '    text: 下课铃响起。',
    '  - type: VO',
    '    character: 旁白',
    '    parenthetical: 画外音',
    '    line: 午后的风穿过走廊。',
    '  - type: DIALOGUE',
    '    character: 旁白',
    '    line: 午后的风穿过走廊。',
    '  - type: TRANSITION',
    '    text: 淡出：',
    'sourceChapter: 2',
  ].join('\n'),
)
const editedBlocks = scriptDraft.scene.scriptBlocks ?? []
const savedBlocks = scriptDraft.savedScene.scriptBlocks ?? []
assert(scriptDraft.scene.heading.location === '走廊', 'YAML heading 修改应同步到草稿场景')
assert(scriptDraft.scene.sourceChapter === 2, 'YAML sourceChapter 修改应同步到草稿场景')
assert(editedBlocks.length === 7, 'YAML 剧本块应同步到草稿场景')
assert(editedBlocks[0].type === 'SHOT', 'YAML 镜头文本应同步为镜头块')
assert(editedBlocks[1].text === '她抬头看向窗外。', 'YAML 动作文本变更应同步到草稿场景')
assert(editedBlocks[2].type === 'INSERT', 'YAML 插入特写文本应同步为插入特写块')
assert(editedBlocks[3].type === 'SFX', 'YAML 音效文本应同步为音效块')
assert(editedBlocks[4].type === 'VO', 'YAML 画外音文本应同步为画外音块')
assert(editedBlocks[4].character === '旁白', 'YAML 画外音角色名应同步到草稿场景')
assert(editedBlocks[4].parenthetical === '画外音', 'YAML 画外音括号提示应同步到草稿场景')
assert(editedBlocks[4].line === '午后的风穿过走廊。', 'YAML 画外音台词应同步到草稿场景')
assert(editedBlocks[5].character === '旁白', 'YAML 对白角色名应同步到草稿场景')
assert(editedBlocks[5].parenthetical === undefined, 'YAML 未填写 parenthetical 时不应生成字段')
assert(editedBlocks[5].line === '午后的风穿过走廊。', 'YAML 对白台词应同步到草稿场景')
assert(editedBlocks[6].type === 'TRANSITION', 'YAML 转场文本应同步为转场块')
assert(savedBlocks[1].text === '林秋把书包放在桌上。', '编辑草稿不应改写最近保存的场景基线')

const savedScene: SceneResult = {
  ...scene,
  scriptBlocks: [{ type: 'ACTION', text: '林秋把书包轻轻推到桌角。' }],
}
const savedDraft = createPolishDraft(savedScene)
const unsavedDraft = updatePolishSceneYaml(
  savedDraft,
  [
    'sceneId: s1',
    'heading:',
    '  interior: true',
    '  location: 教室',
    '  timeOfDay: 午后',
    'scriptBlocks:',
    '  - type: ACTION',
    '    text: 林秋又把书包拿回身边。',
    'sourceChapter: 1',
  ].join('\n'),
)
const resetDraft = resetPolishDraft(unsavedDraft)
const resetBlocks = resetDraft.scene.scriptBlocks ?? []
assert(resetBlocks[0].text === '林秋把书包轻轻推到桌角。', '取消应回到最近一次保存的草稿内容')
assert(resetDraft.sceneYamlText.includes('林秋把书包轻轻推到桌角。'), '取消不应回到页面初始场景')

const workspaceLayout = buildPolishWorkspaceLayout()
assert(workspaceLayout.left.code === 'YAML', '打磨页左栏应聚焦 YAML 本场结构')
assert(workspaceLayout.left.title === 'YAML 本场结构', '打磨页左栏标题应说明可编辑的本场结构')
assert(!('meta' in workspaceLayout.left), '打磨页左栏标题区不应再显示可编辑可保存文案')
assert(workspaceLayout.left.actions.join(',') === 'cancel,save', '取消和保存应放在 YAML 标题区右侧')
assert(workspaceLayout.right.code === 'PREVIEW', '打磨页右栏应聚焦渲染预览')
assert(workspaceLayout.right.title === '渲染预览', '打磨页右栏标题应说明预览用途')
assert(workspaceLayout.syncScroll, 'YAML 编辑区和渲染预览应支持滚动进度同步')
assert(!workspaceLayout.showExportEntry, '打磨页不应再提供进入导出的独立入口')

const longYamlLineNumbers = buildYamlLineNumbers([
  'scriptBlocks:',
  '  -',
  '    type: ACTION',
  '    text: 温水用手帕擦额头上的汗，环视店内。周围没有穿同校制服的学生。他从书包里拿出一本文库本，封面是轻小说《跟年上的妹妹撒娇也可以吗？》最新卷。桌上摆着自助饮料杯和一大盘薯条。',
  'sourceChapter: 1',
].join('\n'))
assert(longYamlLineNumbers.length === 5, '软换行不应增加 YAML 逻辑行号数量')
assert(longYamlLineNumbers[3].lineNumber === 4, '长 text 行应只对应一个逻辑行号')
assert(longYamlLineNumbers[4].lineNumber === 5, '长 text 行后的下一行号应按用户输入的换行计算')

assert(
  buildPolishYamlLineNumberTransform(128) === 'translateY(-128px)',
  '打磨页 YAML 行号层应按编辑区 scrollTop 即时位移',
)
assert(
  buildPolishYamlLineNumberTransform(-12) === 'translateY(0px)',
  '打磨页 YAML 行号层不应生成负向滚动位移',
)
assert(
  POLISH_YAML_SCROLLBAR_GUTTER_PX >= 16,
  '打磨页 YAML 行号镜像层应预留滚动条宽度，避免结尾软换行高度短于编辑区',
)

assert(
  POLISH_YAML_SPELL_CHECK === false,
  '打磨页 YAML 编辑器应关闭浏览器拼写检查，避免红色波浪线干扰编辑',
)
