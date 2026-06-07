import type { ChapterPreview, ScreenplayConvertContext } from '../../types/novel'

export interface ImportEntryAction {
  label: string
  enabled: boolean
}

export interface ImportEntryActions {
  primary: ImportEntryAction
  secondary: ImportEntryAction[]
}

export interface ScreenplayTypeCard {
  code: string
  name: string
  badge: string
  description: string
  enabled: boolean
  active: boolean
}

export interface ImportSelectionState {
  selectedNovelId: string | null
}

export interface ImportResultSnapshot {
  novelId: string
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: ChapterPreview[]
}

const SCREENPLAY_TYPE_OPTIONS: Array<Omit<ScreenplayTypeCard, 'active'>> = [
  { code: 'ANIME', name: '动画剧本', badge: 'ANIME', description: 'TV单集 ~20-24min', enabled: true },
  { code: 'FILM', name: '影视剧本', badge: 'FILM', description: '长片 ~90-120min', enabled: false },
  { code: 'SHORT_DRAMA', name: '短剧本', badge: 'SHORT', description: '~2-5min/集', enabled: false },
  { code: 'RADIO', name: '广播剧', badge: 'SOON', description: '无画面 · 待开发', enabled: false },
  { code: 'THEATER', name: '话剧', badge: 'SOON', description: '舞台 · 待开发', enabled: false },
]

export function buildImportEntryActions(canStartConvert: boolean): ImportEntryActions {
  return {
    primary: {
      label: '开始分析',
      enabled: canStartConvert,
    },
    secondary: [],
  }
}

export function buildScreenplayTypeCards(selectedType: string): ScreenplayTypeCard[] {
  return SCREENPLAY_TYPE_OPTIONS.map((type) => ({
    ...type,
    active: type.code === selectedType,
  }))
}

export function selectHistoryNovel(state: ImportSelectionState, novelId: string): ImportSelectionState {
  return {
    ...state,
    selectedNovelId: novelId,
  }
}

export function selectUploadedNovel(state: ImportSelectionState): ImportSelectionState {
  return {
    ...state,
    selectedNovelId: null,
  }
}

export function buildImportResultFromConvertContext(
  context: ScreenplayConvertContext | null,
): ImportResultSnapshot | null {
  if (!context) {
    return null
  }

  return {
    novelId: context.novelId,
    title: context.title,
    totalChapters: context.totalChapters,
    totalWordCount: context.chapters.reduce((sum, chapter) => sum + chapter.wordCount, 0),
    chapters: context.chapters,
  }
}

export function resolveEditableTitle(inputTitle: string, fallbackTitle: string) {
  const trimmedTitle = inputTitle.trim()
  return trimmedTitle || fallbackTitle
}
