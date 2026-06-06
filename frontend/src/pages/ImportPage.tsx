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
import { getNovelChapters, getNovelList, parseNovel, uploadNovel } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import type {
  ChapterPreview,
  ChapterSummary,
  ImportFlowContext,
  NovelChaptersResult,
  NovelListItem,
  NovelParseResult,
  ScreenplayConvertContext,
} from '../types/novel'

const { Text, Title } = Typography
const { TextArea } = Input

const SAMPLE_TITLE = '她比烟花寂寞'
const SAMPLE_TEXT = `==========
~第一章 出租屋的夜~
==========

雨敲在铁皮屋檐上。林晚把简历又看了一遍，心里像压了块石头。

==========
~第二章 天台~
==========

黄昏，城市在脚下铺开。她想，如果就这样跳下去，会不会比活着轻松。

==========
~第三章 转机~
==========

手机响了。是那家公司。“林晚小姐，恭喜你通过了。”`

const SCREENPLAY_TYPES = [
  { code: 'ANIME', name: '动画剧本', enabled: true },
  { code: 'FILM', name: '影视剧本', enabled: false },
  { code: 'SHORT_DRAMA', name: '短剧剧本', enabled: false },
  { code: 'RADIO', name: '广播剧', enabled: false },
  { code: 'THEATER', name: '话剧', enabled: false },
]

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
  onStartSingleScene: (context: ImportFlowContext) => void
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

function toDisplayResult(result: NovelParseResult | NovelChaptersResult): DisplayResult {
  return {
    novelId: 'novelId' in result ? result.novelId : undefined,
    title: result.title,
    totalChapters: result.totalChapters,
    totalWordCount: result.totalWordCount,
    chapters: result.chapters.map((chapter: ChapterSummary | ChapterPreview) => ({
      chapterIndex: chapter.chapterIndex,
      title: chapter.title,
      wordCount: chapter.wordCount,
      preview: 'preview' in chapter ? chapter.preview : undefined,
    })),
  }
}

function ImportPage({ onStartConvert, onStartSingleScene }: ImportPageProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [title, setTitle] = useState(SAMPLE_TITLE)
  const [text, setText] = useState(SAMPLE_TEXT)
  const [selectedType, setSelectedType] = useState('ANIME')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [parseLoading, setParseLoading] = useState(false)
  const [uploadLoading, setUploadLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [chapterResult, setChapterResult] = useState<DisplayResult | null>(null)
  const [historyVisible, setHistoryVisible] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyLoaded, setHistoryLoaded] = useState(false)
  const [historyItems, setHistoryItems] = useState<NovelListItem[]>([])

  async function handleParse() {
    setParseLoading(true)
    setErrorMessage(null)

    try {
      const result = await parseNovel(title, text)
      setChapterResult(toDisplayResult(result))
    } catch (error) {
      setChapterResult(null)
      setErrorMessage(error instanceof Error ? error.message : '章节识别失败')
    } finally {
      setParseLoading(false)
    }
  }

  async function handleFileSelected(file: File) {
    setSelectedFile(file)
    setUploadLoading(true)
    setErrorMessage(null)

    try {
      const uploadResult = await uploadNovel(title, file)
      const chaptersResult = await getNovelChapters(uploadResult.novelId)
      setChapterResult(toDisplayResult(chaptersResult))
    } catch (error) {
      setChapterResult(null)
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

  async function handleUseHistory(novelId: string, novelTitle: string) {
    setErrorMessage(null)

    try {
      const chaptersResult = await getNovelChapters(novelId)
      setTitle(novelTitle)
      setSelectedFile(null)
      setChapterResult(toDisplayResult(chaptersResult))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '加载历史小说失败')
    }
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

  function handleStartConvert() {
    if (!chapterResult?.novelId) {
      return
    }

    onStartConvert({
      novelId: chapterResult.novelId,
      title: chapterResult.title,
      totalChapters: chapterResult.totalChapters,
      chapters: chapterResult.chapters,
      screenplayType: selectedType,
    })
  }

  function handleStartSingleScene() {
    if (!chapterResult?.novelId) {
      return
    }

    onStartSingleScene({
      novelId: chapterResult.novelId,
      title: chapterResult.title,
      chapters: chapterResult.chapters,
      selectedChapterIndex: chapterResult.chapters[0]?.chapterIndex ?? 1,
    })
  }

  const canStartConvert = Boolean(
    chapterResult &&
    chapterResult.totalChapters >= 3 &&
    chapterResult.novelId,
  )

  return (
    <PrototypeFrame currentStep="import">
      <PrototypeHero
        eyebrow="02 · 导入"
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
                  {historyItems.map((item) => (
                    <div className="history-row" key={item.novelId}>
                      <div className="history-copy">
                        <Title level={5}>{item.title}</Title>
                        <Text>{item.totalChapters} 章 · {formatWordCount(item.totalWordCount)}</Text>
                        <Text>{formatTime(item.createdAt)}</Text>
                      </div>
                      <Button onClick={() => void handleUseHistory(item.novelId, item.title)}>
                        使用这本
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          ) : null}

          <div className="field-stack">
            <label className="field-label" htmlFor="novel-title">作品标题</label>
            <Input
              id="novel-title"
              size="large"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="输入作品标题"
            />
          </div>

          <div className="field-stack">
            <label className="field-label" htmlFor="novel-text">粘贴正文（自动识别章节）</label>
            <TextArea
              id="novel-text"
              className="manuscript-input"
              rows={14}
              value={text}
              onChange={(event) => setText(event.target.value)}
              placeholder="粘贴至少 3 章正文"
            />
          </div>

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
              <Button
                size="large"
                type="primary"
                onClick={handleParse}
                loading={parseLoading}
                disabled={uploadLoading}
              >
                自动识别章节
              </Button>
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
                disabled={parseLoading}
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
              {SCREENPLAY_TYPES.map((type) => (
                <button
                  key={type.code}
                  className={`type-card${selectedType === type.code ? ' active' : ''}${type.enabled ? '' : ' disabled'}`}
                  type="button"
                  onClick={() => type.enabled && setSelectedType(type.code)}
                  disabled={!type.enabled}
                >
                  <span className="type-name">{type.name}</span>
                  <Tag bordered={false} color={type.enabled ? 'blue' : 'default'}>
                    {type.code}
                  </Tag>
                </button>
              ))}
            </div>
          </Card>
          <Button
            block
            className="convert-button"
            size="large"
            type="primary"
            disabled={!canStartConvert}
            onClick={handleStartConvert}
          >
            开始转换
          </Button>
          <Button
            block
            className="secondary-button"
            size="large"
            disabled={!canStartConvert}
            onClick={handleStartSingleScene}
          >
            单章打磨
          </Button>
        </div>
      </main>
    </PrototypeFrame>
  )
}

export default ImportPage
