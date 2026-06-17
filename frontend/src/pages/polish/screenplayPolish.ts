import type { SceneResult, ScriptBlock } from '../../types/novel'

export const POLISH_YAML_SCROLLBAR_GUTTER_PX = 18
export const POLISH_YAML_SPELL_CHECK = false

export interface PolishDraft {
  scene: SceneResult
  savedScene: SceneResult
  sceneYamlText: string
}

export interface PolishSceneStatus {
  changed: boolean
  unsaved: boolean
}

export interface PolishWorkspaceLayout {
  left: {
    code: string
    title: string
    actions: Array<'cancel' | 'save'>
  }
  right: {
    code: string
    title: string
  }
  syncScroll: boolean
  showExportEntry: boolean
}

export interface YamlLineNumber {
  lineNumber: number
  text: string
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
    warnings: scene.warnings ? [...scene.warnings] : undefined,
  }
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

export function createPolishDraft(scene: SceneResult): PolishDraft {
  const clonedScene = cloneScene(scene)

  return {
    scene: clonedScene,
    savedScene: cloneScene(scene),
    sceneYamlText: buildPolishSceneYaml(clonedScene),
  }
}

export function resetPolishDraft(draft: PolishDraft): PolishDraft {
  return createPolishDraft(draft.savedScene)
}

export function updatePolishSceneYaml(draft: PolishDraft, sceneYamlText: string): PolishDraft {
  const parsedScene = parsePolishSceneYaml(sceneYamlText, draft.scene)

  return {
    ...draft,
    sceneYamlText,
    scene: parsedScene,
  }
}

export function buildPolishSceneStatus(draft?: PolishDraft): PolishSceneStatus {
  if (!draft) {
    return {
      changed: false,
      unsaved: false,
    }
  }

  const savedYamlText = buildPolishSceneYaml(draft.savedScene)

  return {
    changed: savedYamlText !== buildPolishSceneYaml(draft.scene),
    unsaved: savedYamlText !== draft.sceneYamlText,
  }
}

export function buildPolishSceneStatusByKey(draftsBySceneKey: Record<string, PolishDraft>) {
  return Object.fromEntries(
    Object.entries(draftsBySceneKey).map(([sceneKey, draft]) => [sceneKey, buildPolishSceneStatus(draft)]),
  )
}

function toYamlLines(value: unknown, indent = 0): string[] {
  const prefix = ' '.repeat(indent)
  if (Array.isArray(value)) {
    return value.flatMap((item) => {
      if (typeof item === 'object' && item !== null) {
        const itemLines = toYamlLines(item, indent + 2)
        if (itemLines.length === 0) {
          return [`${prefix}-`]
        }

        return [`${prefix}- ${itemLines[0].trim()}`, ...itemLines.slice(1)]
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

function readScalar(line: string) {
  const value = line.slice(line.indexOf(':') + 1).trim()
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  if (/^\d+$/.test(value)) {
    return Number(value)
  }
  return value
}

function parseScriptBlockValue(block: Partial<ScriptBlock>, key: string, value: unknown): Partial<ScriptBlock> {
  const textValue = String(value)
  if (key === 'type') {
    const type = textValue.toUpperCase()
    if (
      type === 'SHOT' ||
      type === 'ACTION' ||
      type === 'INSERT' ||
      type === 'SFX' ||
      type === 'DIALOGUE' ||
      type === 'VO' ||
      type === 'TRANSITION'
    ) {
      return { ...block, type }
    }
    return block
  }

  if (key === 'text' || key === 'character' || key === 'parenthetical' || key === 'line') {
    return { ...block, [key]: textValue }
  }

  return block
}

function normalizeParsedScriptBlock(block: Partial<ScriptBlock>): ScriptBlock | undefined {
  if (block.type === 'DIALOGUE' || block.type === 'VO') {
    const character = block.character?.trim()
    const line = block.line?.trim()
    const parenthetical = block.parenthetical?.trim()
    return character && line
      ? { type: block.type, character, line, parenthetical: parenthetical || undefined }
      : undefined
  }

  if (
    block.type === 'SHOT' ||
    block.type === 'ACTION' ||
    block.type === 'INSERT' ||
    block.type === 'SFX' ||
    block.type === 'TRANSITION'
  ) {
    const text = block.text?.trim()
    return text ? { type: block.type, text } : undefined
  }

  return undefined
}

function parsePolishSceneYaml(sceneYamlText: string, fallbackScene: SceneResult): SceneResult {
  const nextScene = cloneScene(fallbackScene)
  const lines = sceneYamlText.split(/\r?\n/)
  const scriptBlocks: ScriptBlock[] = []
  let section: 'root' | 'heading' | 'scriptBlocks' = 'root'
  let currentBlock: Partial<ScriptBlock> | undefined

  function pushCurrentBlock() {
    if (!currentBlock) {
      return
    }
    const block = normalizeParsedScriptBlock(currentBlock)
    if (block) {
      scriptBlocks.push(block)
    }
    currentBlock = undefined
  }

  lines.forEach((rawLine) => {
    const line = rawLine.trim()
    if (!line) {
      return
    }
    const rootLevel = !rawLine.startsWith(' ')

    if (line === 'heading:') {
      pushCurrentBlock()
      section = 'heading'
      return
    }
    if (line === 'scriptBlocks:') {
      section = 'scriptBlocks'
      return
    }
    if (line.startsWith('-')) {
      pushCurrentBlock()
      currentBlock = {}
      const inlineValue = line.slice(1).trim()
      if (inlineValue.includes(':')) {
        const key = inlineValue.slice(0, inlineValue.indexOf(':')).trim()
        const value = readScalar(inlineValue)
        currentBlock = parseScriptBlockValue(currentBlock, key, value)
      }
      return
    }
    if (!line.includes(':')) {
      return
    }

    const key = line.slice(0, line.indexOf(':')).trim()
    const value = readScalar(line)

    if (rootLevel && key !== 'type' && key !== 'text' && key !== 'character' && key !== 'parenthetical' && key !== 'line') {
      pushCurrentBlock()
      section = 'root'
    }

    if (section === 'heading') {
      if (key === 'interior' && typeof value === 'boolean') {
        nextScene.heading.interior = value
      }
      if (key === 'location') {
        nextScene.heading.location = String(value)
      }
      if (key === 'timeOfDay') {
        nextScene.heading.timeOfDay = String(value)
      }
      return
    }

    if (section === 'scriptBlocks') {
      currentBlock = parseScriptBlockValue(currentBlock ?? {}, key, value)
      return
    }

    if (key === 'sceneId') {
      nextScene.sceneId = String(value)
    }
    if (key === 'sourceChapter' && typeof value === 'number') {
      nextScene.sourceChapter = value
    }
  })

  pushCurrentBlock()

  return {
    ...nextScene,
    scriptBlocks,
  }
}

export function buildPolishWorkspaceLayout(): PolishWorkspaceLayout {
  return {
    left: {
      code: 'YAML',
      title: 'YAML 本场结构',
      actions: ['cancel', 'save'],
    },
    right: {
      code: 'PREVIEW',
      title: '渲染预览',
    },
    syncScroll: true,
    showExportEntry: false,
  }
}

export function buildYamlLineNumbers(yamlText: string): YamlLineNumber[] {
  const lines = yamlText.split(/\r?\n/)
  return lines.map((line, index) => ({
    lineNumber: index + 1,
    text: line,
  }))
}

export function buildPolishYamlLineNumberTransform(scrollTop: number) {
  const offset = Math.max(0, scrollTop)
  return `translateY(${offset === 0 ? 0 : -offset}px)`
}
