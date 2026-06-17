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
  latestConversionId?: string | null
  latestConversionType?: string | null
  latestConversionStatus?: 'RUNNING' | 'FAILED' | 'COMPLETED' | string | null
  latestConversionUpdatedAt?: string | null
  latestConversionErrorMessage?: string | null
}

export interface NovelListResult {
  novels: NovelListItem[]
  total: number
}

export interface ScreenplayConvertContext {
  novelId: string
  title: string
  totalChapters: number
  chapters: ChapterPreview[]
  screenplayType: string
  restoredConversionId?: string
  restoredConversionStatus?: 'RUNNING' | 'FAILED' | 'COMPLETED' | string
  restoredConversionUpdatedAt?: string
  restoredConversionErrorMessage?: string
  restoredConversionMode?: 'static' | 'stream'
  restoredGeneratedScenes?: GeneratedSceneSummary[]
}

export interface ConvertEventItem {
  type: 'started' | 'chapter_loaded' | 'chapter_split' | 'scene_completed' | 'analysis_restored' | 'analysis_updated' | 'completed' | 'failed' | 'error'
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
  title?: string
  scene: SceneResult
}

export interface ScreenplayConversionDetail {
  conversionId: string
  novelId: string
  screenplayType: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | string
  updatedAt?: string | null
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

export type ScriptBlockType = 'SHOT' | 'ACTION' | 'INSERT' | 'SFX' | 'DIALOGUE' | 'VO' | 'TRANSITION'

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
