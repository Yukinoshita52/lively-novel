export interface ApiResponse<T> {
  code: number
  data: T | null
  message: string
}

export interface ChapterSummary {
  chapterIndex: number
  title: string
  wordCount: number
}

export interface NovelUploadResult {
  novelId: string
  title: string
  contentHash: string
  totalChapters: number
  totalWordCount: number
  chapters: ChapterSummary[]
}

export interface ChapterPreview {
  chapterIndex: number
  title: string
  wordCount: number
  preview?: string
}

export interface NovelChaptersResult {
  novelId: string
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: ChapterPreview[]
}

export interface NovelListItem {
  novelId: string
  title: string
  totalChapters: number
  totalWordCount: number
  createdAt: string | null
}

export interface NovelListResult {
  novels: NovelListItem[]
  total: number
}

export interface ImportFlowContext {
  novelId: string
  title: string
  chapters: ChapterPreview[]
  selectedChapterIndex: number
}

export interface ScreenplayConvertContext {
  novelId: string
  title: string
  totalChapters: number
  chapters: ChapterPreview[]
  screenplayType: string
}

export interface ConvertEventItem {
  type: 'started' | 'chapter_loaded' | 'chapter_split' | 'scene_completed' | 'completed' | 'failed' | 'error'
  message: string
}

export interface GeneratedSceneSummary {
  chapterIndex: number
  sceneIndexInChapter?: number
  title: string
  scene: SceneResult
}

export interface ScreenplayPersistedScene {
  chapterIndex: number
  sceneIndexInChapter: number
  scene: SceneResult
}

export interface ScreenplayConversionDetail {
  conversionId: string
  novelId: string
  screenplayType: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | string
  errorMessage?: string | null
  scenes: ScreenplayPersistedScene[]
}

export interface SceneHeading {
  interior: boolean
  location: string
  timeOfDay: string
}

export interface DialogueBlock {
  character: string
  parenthetical?: string
  line: string
}

export type ScriptBlockType = 'ACTION' | 'DIALOGUE' | 'TRANSITION'

export interface ScriptBlock {
  type: ScriptBlockType
  text?: string
  character?: string
  parenthetical?: string
  line?: string
}

export interface VisualizedInnerThought {
  original: string
  method: string
  result: string
}

export interface SceneResult {
  sceneId: string
  heading: SceneHeading
  scriptBlocks?: ScriptBlock[]
  actionLines?: string[]
  dialogueBlocks?: DialogueBlock[]
  visualizedInnerThoughts?: VisualizedInnerThought[]
  transitions?: string[]
  sourceChapter: number
  sourceText: string
}
