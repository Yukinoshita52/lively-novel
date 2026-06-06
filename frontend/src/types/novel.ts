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

export interface NovelParseResult {
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: ChapterSummary[]
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
