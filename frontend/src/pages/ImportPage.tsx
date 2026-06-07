import { useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Input,
  Tag,
  Typography,
} from 'antd'
import {
  LoadingOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import {
  getNovelChapters,
  getNovelList,
  updateNovelTitle,
  uploadNovel,
} from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import {
  buildImportEntryActions,
  buildImportResultFromConvertContext,
  buildScreenplayTypeCards,
  resolveEditableTitle,
  selectHistoryNovel,
  selectUploadedNovel,
} from './importPageModel'
import type { FlowStepNavigation } from './appNavigation'
import type { FlowStepKey } from './prototypeFlow'
import type {
  ChapterPreview,
  NovelChaptersResult,
  NovelListItem,
  ScreenplayConvertContext,
} from '../types/novel'

const { Text, Title } = Typography

type DisplayChapter = ChapterPreview

type DisplayResult = {
  novelId?: string
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: DisplayChapter[]
}

type ImportPageProps = {
  onStartConvert: (context: ScreenplayConvertContext) => void
  onTitleUpdated?: (context: ScreenplayConvertContext) => void
  restoreContext?: ScreenplayConvertContext | null
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function formatWordCount(wordCount: number) {
  if (wordCount >= 10000) {
    return `${(wordCount / 10000).toFixed(1).replace(/\.0$/, '')} 万字`
  }
  return `${wordCount} 字`
}

function formatTime(isoTime: string | null) {
  if (!isoTime) {
    return '时间未知'
  }

  const date = new Date(isoTime)
  if (Number.isNaN(date.getTime())) {
    return '时间未知'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
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
          bordered={false}
        >

          <div className="history-toolbar">
            <Button
              size="large"
              onClick={() => (historyVisible ? setHistoryVisible(false) : void loadHistory())}
              loading={historyLoading}
            >
              从历史中选择已导入小说
            </Button>
            {historyVisible ? (
              <Button
                type="link"
                onClick={() => void loadHistory(true)}
                loading={historyLoading}
              >
                刷新历史
              </Button>
            ) : null}
          </div>

          {historyVisible ? (
            <Card className="history-card" bordered={false}>
              {historyItems.length === 0 && !historyLoading ? (
                <Text className="history-empty">还没有已导入的小说</Text>
              ) : (
                <div className="history-list">
                  {historyItems.map((item) => {
                    const selected = selectedNovelId === item.novelId

                    return (
                    <div className={`history-row${selected ? ' selected' : ''}`} key={item.novelId}>
                      <div className="history-copy">
                        <Title level={5}>{item.title}</Title>
                        <Text>{item.totalChapters} 章 · {formatWordCount(item.totalWordCount)}</Text>
                        <Text>{formatTime(item.createdAt)}</Text>
                      </div>
                      <Button type={selected ? 'primary' : 'default'} onClick={() => void handleUseHistory(item.novelId)}>
                        {selected ? '已选择' : '使用这本'}
                      </Button>
                    </div>
                    )
                  })}
                </div>
              )}
            </Card>
          ) : null}

          {chapterResult ? (
            <div className="field-stack title-edit-stack">
              <label className="field-label" htmlFor="novel-title">作品标题</label>
              <div className="title-edit-row">
                <Input
                  id="novel-title"
                  size="large"
                  value={editableTitle}
                  onChange={(event) => setEditableTitle(event.target.value)}
                  onPressEnter={() => void handleSaveTitle()}
                  placeholder="输入作品标题"
                />
                <Button
                  size="large"
                  onClick={() => void handleSaveTitle()}
                  loading={titleSaving}
                  disabled={!chapterResult.novelId}
                >
                  保存标题
                </Button>
              </div>
            </div>
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

          {chapterResult ? (
            <Card className="chapter-card prototype-inner-panel" bordered={false}>
              <div className="chapter-card-head">
                <div>
                  <Text className="panel-kicker">RESULT</Text>
                  <Title level={4}>{chapterResult.title || '未命名作品'}</Title>
                </div>
                <div className="chapter-summary">
                  <span>{chapterResult.totalChapters} 章</span>
                  <span>{formatWordCount(chapterResult.totalWordCount)}</span>
                </div>
              </div>

              <div className="chapter-list">
                {chapterResult.chapters.map((chapter) => (
                  <div className="chapter-row" key={chapter.chapterIndex}>
                    <div className="chapter-copy">
                      <Text className="chapter-index">CH {chapter.chapterIndex}</Text>
                      <Title level={5}>{chapter.title}</Title>
                      {chapter.preview ? (
                        <Text className="chapter-preview">{chapter.preview}</Text>
                      ) : null}
                    </div>
                    <Tag bordered={false}>{formatWordCount(chapter.wordCount)}</Tag>
                  </div>
                ))}
              </div>
            </Card>
          ) : null}
        </Card>

        <div className="side-column">
          <Card
            className="prototype-panel"
            title={<PrototypePanelTitle code="TYPE" title="剧本类型" />}
            bordered={false}
          >

            <div className="type-grid">
              {screenplayTypeCards.map((type) => (
                <button
                  key={type.code}
                  className={`type-card${type.active ? ' active' : ''}${type.enabled ? '' : ' disabled'}`}
                  type="button"
                  onClick={() => type.enabled && setSelectedType(type.code)}
                  disabled={!type.enabled}
                >
                  <span className="type-card-head">
                    <span className="type-name">{type.name}</span>
                    <Tag className="type-badge" bordered={false}>
                      {type.badge}
                    </Tag>
                  </span>
                  <span className="type-desc">{type.description}</span>
                </button>
              ))}
            </div>
          </Card>
          <Button
            block
            className="convert-button"
            size="large"
            type="primary"
            disabled={!entryActions.primary.enabled}
            onClick={() => void handleStartConvert()}
          >
            {entryActions.primary.label}
          </Button>
        </div>
      </main>
    </PrototypeFrame>
  )
}

export default ImportPage
