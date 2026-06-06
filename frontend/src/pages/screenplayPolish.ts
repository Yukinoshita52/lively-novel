import type { DialogueBlock, SceneResult } from '../types/novel'

export interface PolishDraft {
  scene: SceneResult
  savedScene: SceneResult
  actionLinesText: string
  dialogueText: string
  transitionsText: string
}

function cloneScene(scene: SceneResult): SceneResult {
  return {
    ...scene,
    heading: { ...scene.heading },
    actionLines: [...scene.actionLines],
    dialogueBlocks: scene.dialogueBlocks.map((dialogue) => ({ ...dialogue })),
    visualizedInnerThoughts: scene.visualizedInnerThoughts.map((thought) => ({ ...thought })),
    transitions: [...scene.transitions],
  }
}

function toNonEmptyLines(text: string) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function serializeDialogue(dialogue: DialogueBlock) {
  return [dialogue.character, dialogue.parenthetical ?? '', dialogue.line].join('|')
}

function parseDialogueLine(line: string): DialogueBlock | undefined {
  const [character = '', parenthetical = '', ...lineParts] = line.split('|')
  const dialogueLine = lineParts.join('|').trim()
  const normalizedCharacter = character.trim()
  if (!normalizedCharacter || !dialogueLine) {
    return undefined
  }

  return {
    character: normalizedCharacter,
    parenthetical: parenthetical.trim() || undefined,
    line: dialogueLine,
  }
}

export function createPolishDraft(scene: SceneResult): PolishDraft {
  const clonedScene = cloneScene(scene)

  return {
    scene: clonedScene,
    savedScene: cloneScene(scene),
    actionLinesText: clonedScene.actionLines.join('\n'),
    dialogueText: clonedScene.dialogueBlocks.map(serializeDialogue).join('\n'),
    transitionsText: clonedScene.transitions.join('\n'),
  }
}

export function resetPolishDraft(draft: PolishDraft): PolishDraft {
  return createPolishDraft(draft.savedScene)
}

export function updateActionLinesText(draft: PolishDraft, actionLinesText: string): PolishDraft {
  return {
    ...draft,
    actionLinesText,
    scene: {
      ...draft.scene,
      actionLines: toNonEmptyLines(actionLinesText),
    },
  }
}

export function updateDialogueText(draft: PolishDraft, dialogueText: string): PolishDraft {
  return {
    ...draft,
    dialogueText,
    scene: {
      ...draft.scene,
      dialogueBlocks: toNonEmptyLines(dialogueText)
        .map(parseDialogueLine)
        .filter((dialogue): dialogue is DialogueBlock => Boolean(dialogue)),
    },
  }
}

export function updateTransitionsText(draft: PolishDraft, transitionsText: string): PolishDraft {
  return {
    ...draft,
    transitionsText,
    scene: {
      ...draft.scene,
      transitions: toNonEmptyLines(transitionsText),
    },
  }
}
