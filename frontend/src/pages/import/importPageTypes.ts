import type { ChapterPreview } from '../../types/novel'

export type DisplayChapter = ChapterPreview

export type DisplayResult = {
  novelId?: string
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: DisplayChapter[]
}
