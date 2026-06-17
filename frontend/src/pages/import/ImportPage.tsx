import { useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Typography,
} from 'antd'
import {
  LoadingOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import {
  getNovelChapters,
  getNovelList,
  getScreenplayConversionDetail,
  updateNovelTitle,
  uploadNovel,
} from '../../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import {
  buildImportEntryActions,
  buildImportResultFromConvertContext,
  buildScreenplayTypeCards,
  resolveEditableTitle,
  selectHistoryNovel,
  selectUploadedNovel,
} from './importPageModel'
import ImportChapterPreview from './ImportChapterPreview'
import ImportHistoryPanel from './ImportHistoryPanel'
import ImportTitleEditor from './ImportTitleEditor'
import ScreenplayTypeSelector from './ScreenplayTypeSelector'
import { formatWordCount } from './importFormat'
import type { DisplayResult } from './importPageTypes'
import type { FlowStepNavigation } from '../appNavigation'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import type {
  ChapterPreview,
  NovelChaptersResult,
  NovelListItem,
  ScreenplayConvertContext,
} from '../../types/novel'
import type { HistoryConversionActionKey } from './importPageModel'
import { mapPersistedScenesToGeneratedScenes } from '../preview/screenplayPreview'

const { Text } = Typography

type ImportPageProps = {
  onStartConvert: (context: ScreenplayConvertContext) => void
  onOpenHistoryConversion: (context: ScreenplayConvertContext, targetPage: 'convert' | 'preview' | 'export') => void
  onTitleUpdated?: (context: ScreenplayConvertContext) => void
  restoreContext?: ScreenplayConvertContext | null
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function toDisplayResult(result: NovelChaptersResult): DisplayResult {
  return {
    novelId: 'novelId' in result ? result.novelId : undefined,
    title: result.title,
    totalChapters: result.totalChapters,
    totalWordCount: result.totalWordCount,
    chapters: result.chapters.map((chapter: ChapterPreview) => ({
      chapterIndex: chapter.chapterIndex,
      title: chapter.title,
      wordCount: chapter.wordCount,
      preview: 'preview' in chapter ? chapter.preview : undefined,
    })),
  }
}

function ImportPage({
  onStartConvert,
  onOpenHistoryConversion,
  onTitleUpdated,
  restoreContext,
  flowNavigation,
  onNavigateStep,
}: ImportPageProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [selectedType, setSelectedType] = useState('ANIME')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploadLoading, setUploadLoading] = useState(false)
  const [titleSaving, setTitleSaving] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [chapterResult, setChapterResult] = useState<DisplayResult | null>(
    () => buildImportResultFromConvertContext(restoreContext ?? null),
  )
  const [editableTitle, setEditableTitle] = useState(
    () => buildImportResultFromConvertContext(restoreContext ?? null)?.title ?? '',
  )
  const [historyVisible, setHistoryVisible] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyLoaded, setHistoryLoaded] = useState(false)
  const [historyItems, setHistoryItems] = useState<NovelListItem[]>([])
  const [selectedNovelId, setSelectedNovelId] = useState<string | null>(restoreContext?.novelId ?? null)

  async function handleFileSelected(file: File) {
    setSelectedFile(file)
    setSelectedNovelId((current) => selectUploadedNovel({ selectedNovelId: current }).selectedNovelId)
    setUploadLoading(true)
    setErrorMessage(null)

    try {
      const uploadResult = await uploadNovel('', file)
      const chaptersResult = await getNovelChapters(uploadResult.novelId)
      setChapterResult(toDisplayResult(chaptersResult))
      setEditableTitle(chaptersResult.title)
    } catch (error) {
      setChapterResult(null)
      setEditableTitle('')
      setErrorMessage(error instanceof Error ? error.message : '上传失败')
    } finally {
      setUploadLoading(false)
    }
  }

  async function loadHistory(force = false) {
    if (historyLoading) {
      return
    }
    if (historyLoaded && !force) {
      setHistoryVisible(true)
      return
    }

    setHistoryLoading(true)
    setErrorMessage(null)

    try {
      const result = await getNovelList()
      setHistoryItems(result.novels)
      setHistoryLoaded(true)
      setHistoryVisible(true)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '历史加载失败')
      setHistoryVisible(true)
    } finally {
      setHistoryLoading(false)
    }
  }

  async function handleUseHistory(novelId: string) {
    setErrorMessage(null)
    setSelectedNovelId((current) => selectHistoryNovel({ selectedNovelId: current }, novelId).selectedNovelId)

    try {
      const chaptersResult = await getNovelChapters(novelId)
      setSelectedFile(null)
      setChapterResult(toDisplayResult(chaptersResult))
      setEditableTitle(chaptersResult.title)
    } catch (error) {
      setSelectedNovelId(null)
      setErrorMessage(error instanceof Error ? error.message : '加载历史小说失败')
    }
  }

  async function handleHistoryConversionAction(item: NovelListItem, action: HistoryConversionActionKey) {
    setErrorMessage(null)

    try {
      const chaptersResult = await getNovelChapters(item.novelId)
      const restoredGeneratedScenes = item.latestConversionId && item.latestConversionStatus === 'COMPLETED'
        ? mapPersistedScenesToGeneratedScenes((await getScreenplayConversionDetail(item.latestConversionId)).scenes)
        : undefined
      const context: ScreenplayConvertContext = {
        novelId: chaptersResult.novelId,
        title: chaptersResult.title,
        totalChapters: chaptersResult.totalChapters,
        chapters: chaptersResult.chapters,
        screenplayType: item.latestConversionType ?? selectedType,
        restoredConversionId: item.latestConversionId ?? undefined,
        restoredConversionStatus: item.latestConversionStatus ?? undefined,
        restoredConversionMode: action === 'resume' ? 'stream' : 'static',
        restoredGeneratedScenes,
      }

      if (action === 'resume') {
        onOpenHistoryConversion(context, 'convert')
        return
      }

      if (action === 'preview') {
        onOpenHistoryConversion(context, 'preview')
        return
      }

      if (action === 'export') {
        onOpenHistoryConversion(context, 'export')
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '加载历史小说失败')
    }
  }

  async function saveTitleIfNeeded(): Promise<DisplayResult | null> {
    if (!chapterResult?.novelId) {
      return chapterResult
    }

    const nextTitle = resolveEditableTitle(editableTitle, chapterResult.title)
    if (nextTitle === chapterResult.title) {
      setEditableTitle(nextTitle)
      return chapterResult
    }

    setEditableTitle(nextTitle)
    setTitleSaving(true)
    setErrorMessage(null)

    try {
      const result = await updateNovelTitle(chapterResult.novelId, nextTitle)
      const displayResult = toDisplayResult(result)
      setChapterResult(displayResult)
      setEditableTitle(displayResult.title)
      onTitleUpdated?.({
        novelId: displayResult.novelId ?? chapterResult.novelId,
        title: displayResult.title,
        totalChapters: displayResult.totalChapters,
        chapters: displayResult.chapters,
        screenplayType: selectedType,
      })
      setHistoryItems((items) => items.map((item) => (
        item.novelId === displayResult.novelId ? { ...item, title: displayResult.title } : item
      )))
      return displayResult
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '保存标题失败')
      return null
    } finally {
      setTitleSaving(false)
    }
  }

  async function handleSaveTitle() {
    await saveTitleIfNeeded()
  }

  function handleUploadClick() {
    fileInputRef.current?.click()
  }

  async function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) {
      return
    }

    await handleFileSelected(file)
  }

  async function handleStartConvert() {
    if (!chapterResult?.novelId) {
      return
    }

    const latestResult = await saveTitleIfNeeded()
    if (!latestResult?.novelId) {
      return
    }

    onStartConvert({
      novelId: latestResult.novelId,
      title: latestResult.title,
      totalChapters: latestResult.totalChapters,
      chapters: latestResult.chapters,
      screenplayType: selectedType,
    })
  }

  const canStartConvert = Boolean(
    chapterResult &&
    chapterResult.totalChapters >= 3 &&
    chapterResult.novelId,
  )
  const entryActions = buildImportEntryActions(canStartConvert)
  const screenplayTypeCards = buildScreenplayTypeCards(selectedType)

  return (
    <PrototypeFrame
      currentStep="import"
      maxWidth={1400}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow="01 · 导入"
        title="把小说交给它"
      />

      <main className="page-grid">
        <Card
          className="prototype-panel manuscript-panel"
          title={<PrototypePanelTitle code="TEXT" title="小说正文" meta="需 ≥ 3 章 · ≤ 20 万字" />}
          variant="borderless"
        >
          <ImportHistoryPanel
            visible={historyVisible}
            loading={historyLoading}
            items={historyItems}
            selectedNovelId={selectedNovelId}
            onToggle={() => (historyVisible ? setHistoryVisible(false) : void loadHistory())}
            onRefresh={() => void loadHistory(true)}
            onUseHistory={(novelId) => void handleUseHistory(novelId)}
            onHistoryConversionAction={(item, action) => void handleHistoryConversionAction(item, action)}
          />

          {chapterResult ? (
            <ImportTitleEditor
              title={editableTitle}
              saving={titleSaving}
              disabled={!chapterResult.novelId}
              onChange={setEditableTitle}
              onSave={() => void handleSaveTitle()}
            />
          ) : null}

          <div className="action-row">
            <div>
              <Text className="status-copy">
                {chapterResult
                  ? `已识别 ${chapterResult.totalChapters} 章 · ${formatWordCount(chapterResult.totalWordCount)}`
                  : '尚未识别章节'}
              </Text>
              {selectedFile ? <Text className="file-copy">已选择：{selectedFile.name}</Text> : null}
            </div>
            <div className="button-group">
              <input
                ref={fileInputRef}
                className="file-input"
                type="file"
                accept=".txt,text/plain"
                onChange={handleFileChange}
              />
              <Button
                size="large"
                icon={uploadLoading ? <LoadingOutlined spin /> : <UploadOutlined />}
                onClick={handleUploadClick}
                loading={uploadLoading}
              >
                上传 .txt
              </Button>
            </div>
          </div>

          {errorMessage ? (
            <Alert
              className="feedback-block"
              message="操作失败"
              description={errorMessage}
              type="error"
              showIcon
            />
          ) : null}

          {chapterResult ? <ImportChapterPreview result={chapterResult} /> : null}
        </Card>

        <ScreenplayTypeSelector
          types={screenplayTypeCards}
          actions={entryActions}
          onSelectType={setSelectedType}
          onStartConvert={() => void handleStartConvert()}
        />
      </main>
    </PrototypeFrame>
  )
}

export default ImportPage
