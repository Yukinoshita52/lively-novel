import {
  createPolishDraft,
  buildPolishSceneYaml,
  resetPolishDraft,
  updateScriptBlocksText,
} from './screenplayPolish.ts'
import type { SceneResult } from '../types/novel.ts'

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
    { type: 'ACTION', text: '林秋把书包放在桌上。' },
    { type: 'DIALOGUE', character: '林秋', parenthetical: '低声', line: '今天先到这里。' },
    { type: 'TRANSITION', text: '切至：' },
  ],
  sourceChapter: 1,
  sourceText: '第一场原文。',
}

const draft = createPolishDraft(scene)
assert(draft.scene !== scene, '本地打磨草稿不应直接复用原始 SceneResult 引用')
assert(
  draft.scriptBlocksText === 'ACTION|林秋把书包放在桌上。\nDIALOGUE|林秋|低声|今天先到这里。\nTRANSITION|切至：',
  '剧本块应序列化为可编辑文本',
)
const polishYaml = buildPolishSceneYaml(draft.scene)
assert(polishYaml.includes('sceneId: s1'), '本场结构预览应保留场景 ID')
assert(polishYaml.includes('scriptBlocks:'), '本场结构预览应保留可编辑剧本块')
assert(!polishYaml.includes('visualizedInnerThoughts'), '本场结构预览不应展示内心戏审计字段')
assert(!polishYaml.includes('sourceText'), '本场结构预览不应展示原文溯源字段')
assert(!polishYaml.includes('第一场原文。'), '本场结构预览不应展示原文内容')

const scriptDraft = updateScriptBlocksText(
  draft,
  'ACTION|她抬头看向窗外。\n\nDIALOGUE|旁白||午后的风穿过走廊。\nTRANSITION|淡出：',
)
const editedBlocks = scriptDraft.scene.scriptBlocks ?? []
const savedBlocks = scriptDraft.savedScene.scriptBlocks ?? []
assert(editedBlocks.length === 3, '空行不应生成剧本块')
assert(editedBlocks[0].text === '她抬头看向窗外。', '动作文本变更应同步到草稿场景')
assert(editedBlocks[1].character === '旁白', '对白第一段应作为角色名')
assert(editedBlocks[1].parenthetical === undefined, '空 parenthetical 不应保留为空字符串')
assert(editedBlocks[1].line === '午后的风穿过走廊。', '对白第四段应作为台词')
assert(editedBlocks[2].type === 'TRANSITION', '转场文本应同步为转场块')
assert(savedBlocks[0].text === '林秋把书包放在桌上。', '编辑草稿不应改写最近保存的场景基线')

const savedScene: SceneResult = {
  ...scene,
  scriptBlocks: [{ type: 'ACTION', text: '林秋把书包轻轻推到桌角。' }],
}
const savedDraft = createPolishDraft(savedScene)
const unsavedDraft = updateScriptBlocksText(savedDraft, 'ACTION|林秋又把书包拿回身边。')
const resetDraft = resetPolishDraft(unsavedDraft)
const resetBlocks = resetDraft.scene.scriptBlocks ?? []
assert(resetBlocks[0].text === '林秋把书包轻轻推到桌角。', '取消应回到最近一次保存的草稿内容')
assert(resetDraft.scriptBlocksText === 'ACTION|林秋把书包轻轻推到桌角。', '取消不应回到页面初始场景')
