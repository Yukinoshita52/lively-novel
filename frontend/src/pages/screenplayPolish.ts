import type { SceneResult, ScriptBlock } from '../types/novel'

export interface PolishDraft {
  scene: SceneResult
  savedScene: SceneResult
  scriptBlocksText: string
}

function cloneScene(scene: SceneResult): SceneResult {
  return {
    ...scene,
    heading: { ...scene.heading },
    scriptBlocks: normalizeScriptBlocks(scene).map((block) => ({ ...block })),
    actionLines: scene.actionLines ? [...scene.actionLines] : undefined,
    dialogueBlocks: scene.dialogueBlocks?.map((dialogue) => ({ ...dialogue })),
    visualizedInnerThoughts: scene.visualizedInnerThoughts?.map((thought) => ({ ...thought })),
    transitions: scene.transitions ? [...scene.transitions] : undefined,
  }
}

function toNonEmptyLines(text: string) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function serializeScriptBlock(block: ScriptBlock) {
  if (block.type === 'DIALOGUE') {
    return ['DIALOGUE', block.character ?? '', block.parenthetical ?? '', block.line ?? ''].join('|')
  }

  return [block.type, block.text ?? ''].join('|')
}

function normalizeScriptBlocks(scene: SceneResult): ScriptBlock[] {
  if (scene.scriptBlocks?.length) {
    return scene.scriptBlocks
  }

  return [
    ...(scene.actionLines ?? [])
      .filter((text) => text.trim())
      .map((text) => ({ type: 'ACTION' as const, text })),
    ...(scene.dialogueBlocks ?? [])
      .filter((dialogue) => dialogue.character.trim() && dialogue.line.trim())
      .map((dialogue) => ({
        type: 'DIALOGUE' as const,
        character: dialogue.character,
        parenthetical: dialogue.parenthetical,
        line: dialogue.line,
      })),
    ...(scene.transitions ?? [])
      .filter((text) => text.trim())
      .map((text) => ({ type: 'TRANSITION' as const, text })),
  ]
}

function parseScriptBlockLine(line: string): ScriptBlock | undefined {
  const [rawType = '', first = '', second = '', ...rest] = line.split('|')
  const type = rawType.trim().toUpperCase()
  if (type === 'ACTION' || type === 'TRANSITION') {
    const text = [first, second, ...rest].filter(Boolean).join('|').trim()
    return text ? { type, text } : undefined
  }
  if (type === 'DIALOGUE') {
    const character = first.trim()
    const parenthetical = second.trim()
    const dialogueLine = rest.join('|').trim()
    return character && dialogueLine
      ? { type, character, parenthetical: parenthetical || undefined, line: dialogueLine }
      : undefined
  }

  return undefined
}

export function createPolishDraft(scene: SceneResult): PolishDraft {
  const clonedScene = cloneScene(scene)

  return {
    scene: clonedScene,
    savedScene: cloneScene(scene),
    scriptBlocksText: normalizeScriptBlocks(clonedScene).map(serializeScriptBlock).join('\n'),
  }
}

export function resetPolishDraft(draft: PolishDraft): PolishDraft {
  return createPolishDraft(draft.savedScene)
}

export function updateScriptBlocksText(draft: PolishDraft, scriptBlocksText: string): PolishDraft {
  return {
    ...draft,
    scriptBlocksText,
    scene: {
      ...draft.scene,
      scriptBlocks: toNonEmptyLines(scriptBlocksText)
        .map(parseScriptBlockLine)
        .filter((block): block is ScriptBlock => Boolean(block)),
    },
  }
}

function toYamlLines(value: unknown, indent = 0): string[] {
  const prefix = ' '.repeat(indent)
  if (Array.isArray(value)) {
    return value.flatMap((item) => {
      if (typeof item === 'object' && item !== null) {
        return [`${prefix}-`, ...toYamlLines(item, indent + 2)]
      }

      return [`${prefix}- ${String(item)}`]
    })
  }

  if (typeof value === 'object' && value !== null) {
    return Object.entries(value as Record<string, unknown>).flatMap(([key, item]) => {
      if (Array.isArray(item) || (typeof item === 'object' && item !== null)) {
        return [`${prefix}${key}:`, ...toYamlLines(item, indent + 2)]
      }

      return [`${prefix}${key}: ${String(item)}`]
    })
  }

  return [`${prefix}${String(value)}`]
}

export function buildPolishSceneYaml(scene: SceneResult) {
  const editableScene = {
    sceneId: scene.sceneId,
    heading: scene.heading,
    scriptBlocks: normalizeScriptBlocks(scene),
    sourceChapter: scene.sourceChapter,
  }

  return toYamlLines(editableScene).join('\n')
}
