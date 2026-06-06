import {
  createPolishDraft,
  resetPolishDraft,
  updateActionLinesText,
  updateDialogueText,
  updateTransitionsText,
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
  actionLines: ['林秋把书包放在桌上。'],
  dialogueBlocks: [
    {
      character: '林秋',
      parenthetical: '(低声)',
      line: '今天先到这里。',
    },
  ],
  visualizedInnerThoughts: [
    {
      original: '她不想承认自己害怕',
      method: 'ACTION',
      result: '她把手藏进袖口。',
    },
  ],
  transitions: ['切至：'],
  sourceChapter: 1,
  sourceText: '第一场原文。',
}

const draft = createPolishDraft(scene)
assert(draft.scene !== scene, '本地打磨草稿不应直接复用原始 SceneResult 引用')
assert(draft.actionLinesText === '林秋把书包放在桌上。', '动作行应序列化为可编辑文本')
assert(draft.dialogueText === '林秋|(低声)|今天先到这里。', '对白应序列化为角色、提示和台词三段')
assert(draft.transitionsText === '切至：', '转场应序列化为可编辑文本')

const actionDraft = updateActionLinesText(draft, '她抬头看向窗外。\n\n风吹动窗帘。')
assert(actionDraft.scene.actionLines.length === 2, '空行不应生成动作行')
assert(actionDraft.scene.actionLines[1] === '风吹动窗帘。', '动作文本变更应同步到草稿场景')
assert(actionDraft.savedScene.actionLines[0] === '林秋把书包放在桌上。', '编辑草稿不应改写最近保存的场景基线')

const dialogueDraft = updateDialogueText(draft, '林秋|(低声)|今天先到这里。\n旁白||午后的风穿过走廊。')
assert(dialogueDraft.scene.dialogueBlocks.length === 2, '对白文本应按行生成多个对白块')
assert(dialogueDraft.scene.dialogueBlocks[1].character === '旁白', '对白第一段应作为角色名')
assert(dialogueDraft.scene.dialogueBlocks[1].parenthetical === undefined, '空 parenthetical 不应保留为空字符串')
assert(dialogueDraft.scene.dialogueBlocks[1].line === '午后的风穿过走廊。', '对白第三段应作为台词')

const transitionDraft = updateTransitionsText(draft, '淡出：\n切至：走廊')
assert(transitionDraft.scene.transitions[0] === '淡出：', '转场文本变更应同步到草稿场景')
assert(transitionDraft.scene.transitions.length === 2, '多行转场文本应生成多个转场')

const savedScene: SceneResult = {
  ...scene,
  actionLines: ['林秋把书包轻轻推到桌角。'],
  transitions: ['淡入：'],
}
const savedDraft = createPolishDraft(savedScene)
const unsavedDraft = updateActionLinesText(savedDraft, '林秋又把书包拿回身边。')
const resetDraft = resetPolishDraft(unsavedDraft)
assert(resetDraft.scene.actionLines[0] === '林秋把书包轻轻推到桌角。', '取消应回到最近一次保存的草稿内容')
assert(resetDraft.transitionsText === '淡入：', '取消不应回到页面初始场景')
