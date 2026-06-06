import { useState } from 'react'
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
} from '@ant-design/icons'
import './App.css'

const { Text, Title } = Typography
const { TextArea } = Input

interface ApiResponse<T> {
  code: number
  data: T | null
  message: string
}

interface ChapterSummary {
  chapterIndex: number
  title: string
  wordCount: number
}

interface NovelParseResult {
  title: string
  totalChapters: number
  totalWordCount: number
  chapters: ChapterSummary[]
}

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
  { code: 'ANIME', name: '动画剧本', detail: 'MVP 当前唯一可落地类型', enabled: true },
  { code: 'FILM', name: '影视剧本', detail: '仅预留枚举与模板位', enabled: false },
  { code: 'SHORT_DRAMA', name: '短剧剧本', detail: '仅预留枚举与模板位', enabled: false },
  { code: 'RADIO', name: '广播剧', detail: '仅预留枚举与模板位', enabled: false },
  { code: 'THEATER', name: '话剧', detail: '仅预留枚举与模板位', enabled: false },
]

function formatWordCount(wordCount: number) {
  if (wordCount >= 10000) {
    return `${(wordCount / 10000).toFixed(1).replace(/\.0$/, '')} 万字`
  }
  return `${wordCount} 字`
}

function App() {
  const [title, setTitle] = useState(SAMPLE_TITLE)
  const [text, setText] = useState(SAMPLE_TEXT)
  const [selectedType, setSelectedType] = useState('ANIME')
  const [parseLoading, setParseLoading] = useState(false)
  const [parseError, setParseError] = useState<string | null>(null)
  const [parseResult, setParseResult] = useState<NovelParseResult | null>(null)

  async function handleParse() {
    setParseLoading(true)
    setParseError(null)

    try {
      const response = await fetch('/api/novel/parse', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title, text }),
      })
      const payload = (await response.json()) as ApiResponse<NovelParseResult>

      if (!response.ok || payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || '章节识别失败')
      }

      setParseResult(payload.data)
    } catch (error) {
      const message = error instanceof Error ? error.message : '章节识别失败'
      setParseError(message)
      setParseResult(null)
    } finally {
      setParseLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="topbar-eyebrow">Lively Novel · 活字成剧</p>
          <Title level={1}>导入小说，识别章节</Title>
        </div>
      </header>

      <main className="page-grid">
        <Card className="panel manuscript-panel" bordered={false}>
          <div className="panel-header">
            <div>
              <Text className="panel-kicker">TEXT</Text>
              <Title level={3}>小说正文</Title>
            </div>
            <Text className="panel-meta">需 ≥ 3 章 · ≤ 20 万字</Text>
          </div>

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
                {parseResult
                  ? `已识别 ${parseResult.totalChapters} 章 · ${formatWordCount(parseResult.totalWordCount)}`
                  : '尚未识别章节'}
              </Text>
            </div>
            <div className="button-group">
              <Button
                size="large"
                type="primary"
                onClick={handleParse}
                loading={parseLoading}
              >
                自动识别章节
              </Button>
              <Button size="large" disabled>
                上传 .txt
              </Button>
            </div>
          </div>

          {parseError ? (
            <Alert
              className="feedback-block"
              message="章节识别失败"
              description={parseError}
              type="error"
              showIcon
            />
          ) : null}

          {parseLoading ? (
            <Alert
              className="feedback-block"
              icon={<LoadingOutlined spin />}
              message="正在识别章节"
              type="info"
              showIcon
            />
          ) : null}

          {parseResult ? (
            <Card className="chapter-card" bordered={false}>
              <div className="chapter-card-head">
                <div>
                  <Text className="panel-kicker">RESULT</Text>
                  <Title level={4}>{parseResult.title || '未命名作品'}</Title>
                </div>
                <div className="chapter-summary">
                  <span>{parseResult.totalChapters} 章</span>
                  <span>{formatWordCount(parseResult.totalWordCount)}</span>
                </div>
              </div>

              <div className="chapter-list">
                {parseResult.chapters.map((chapter) => (
                  <div className="chapter-row" key={chapter.chapterIndex}>
                    <div>
                      <Text className="chapter-index">CH {chapter.chapterIndex}</Text>
                      <Title level={5}>{chapter.title}</Title>
                    </div>
                    <Tag bordered={false}>{formatWordCount(chapter.wordCount)}</Tag>
                  </div>
                ))}
              </div>
            </Card>
          ) : null}
        </Card>

        <div className="side-column">
          <Card className="panel" bordered={false}>
            <div className="panel-header compact">
              <div>
                <Text className="panel-kicker">TYPE</Text>
                <Title level={3}>剧本类型</Title>
              </div>
            </div>

            <div className="type-grid">
              {SCREENPLAY_TYPES.map((type) => (
                <button
                  key={type.code}
                  className={`type-card${selectedType === type.code ? ' active' : ''}${type.enabled ? '' : ' disabled'}`}
                  type="button"
                  onClick={() => type.enabled && setSelectedType(type.code)}
                  disabled={!type.enabled}
                >
                  <div>
                    <span className="type-name">{type.name}</span>
                    <span className="type-detail">{type.detail}</span>
                  </div>
                  <Tag bordered={false} color={type.enabled ? 'blue' : 'default'}>
                    {type.code}
                  </Tag>
                </button>
              ))}
            </div>
          </Card>
          <Button block className="convert-button" size="large" type="primary" disabled>
            开始转换
          </Button>
        </div>
      </main>
    </div>
  )
}

export default App
