import { useEffect, useState } from 'react'
import { Alert, Button, Card, Spin, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { convertSingleScene, getNovelChapterDetail } from '../services/novel'
import type {
  ImportFlowContext,
  NovelChapterDetail,
  SceneResult,
} from '../types/novel'

const { Paragraph, Text, Title } = Typography

type SingleSceneConvertPageProps = {
  context: ImportFlowContext
  onBack: () => void
}

function formatWordCount(wordCount: number) {
  if (wordCount >= 10000) {
    return `${(wordCount / 10000).toFixed(1).replace(/\.0$/, '')} 万字`
  }
  return `${wordCount} 字`
}

function buildHeadingText(scene: SceneResult) {
  const prefix = scene.heading?.interior ? '内景' : '外景'
  return `${prefix} - ${scene.heading?.location || '未知地点'} - ${scene.heading?.timeOfDay || '未知时间'}`
}

function SingleSceneConvertPage({ context, onBack }: SingleSceneConvertPageProps) {
  const [selectedChapterIndex, setSelectedChapterIndex] = useState(context.selectedChapterIndex)
  const [chapterDetail, setChapterDetail] = useState<NovelChapterDetail | null>(null)
  const [chapterLoading, setChapterLoading] = useState(false)
  const [convertLoading, setConvertLoading] = useState(false)
  const [convertError, setConvertError] = useState<string | null>(null)
  const [convertResult, setConvertResult] = useState<SceneResult | null>(null)
  const [contentExpanded, setContentExpanded] = useState(false)

  useEffect(() => {
    void loadChapter(selectedChapterIndex)
  }, [selectedChapterIndex])

  async function loadChapter(chapterIndex: number) {
    setChapterLoading(true)
    setConvertError(null)

    try {
      const detail = await getNovelChapterDetail(context.novelId, chapterIndex)
      setChapterDetail(detail)
      setConvertResult(null)
      setContentExpanded(false)
    } catch (error) {
      setChapterDetail(null)
      setConvertResult(null)
      setConvertError(error instanceof Error ? error.message : '章节加载失败')
    } finally {
      setChapterLoading(false)
    }
  }

  async function handleConvert() {
    if (!chapterDetail?.content) {
      return
    }

    setConvertLoading(true)
    setConvertError(null)

    try {
      const result = await convertSingleScene(chapterDetail.content)
      setConvertResult(result)
    } catch (error) {
      setConvertResult(null)
      setConvertError(error instanceof Error ? error.message : '转换失败')
    } finally {
      setConvertLoading(false)
    }
  }

  const chapterContent = chapterDetail?.content ?? ''
  const shouldCollapseContent = chapterContent.length > 180
  const visibleContent = shouldCollapseContent && !contentExpanded
    ? `${chapterContent.slice(0, 180)}……`
    : chapterContent

  return (
    <div className="convert-shell">
      <header className="convert-topbar">
        <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
          返回导入
        </Button>
        <div>
          <p className="topbar-eyebrow">Single Scene Convert</p>
          <Title level={1}>{context.title}</Title>
        </div>
      </header>

      <main className="convert-grid">
        <Card className="panel" bordered={false}>
          <div className="panel-header">
            <div>
              <Text className="panel-kicker">CHAPTER</Text>
              <Title level={3}>章节正文</Title>
            </div>
            {chapterDetail ? <Tag bordered={false}>{formatWordCount(chapterDetail.wordCount)}</Tag> : null}
          </div>

          <div className="chapter-select-list">
            {context.chapters.map((chapter) => (
              <button
                key={chapter.chapterIndex}
                className={`chapter-select-card${selectedChapterIndex === chapter.chapterIndex ? ' active' : ''}`}
                type="button"
                onClick={() => setSelectedChapterIndex(chapter.chapterIndex)}
              >
                <div>
                  <Text className="chapter-index">CH {chapter.chapterIndex}</Text>
                  <Title level={5}>{chapter.title}</Title>
                </div>
                <Tag bordered={false}>{formatWordCount(chapter.wordCount)}</Tag>
              </button>
            ))}
          </div>

          <div className="chapter-detail-block">
            {chapterLoading ? (
              <div className="chapter-loading">
                <Spin />
              </div>
            ) : (
              <>
                <div className="chapter-content">
                  {visibleContent || '当前章节正文不可用'}
                </div>
                {shouldCollapseContent ? (
                  <Button
                    className="content-toggle"
                    type="link"
                    onClick={() => setContentExpanded((current) => !current)}
                  >
                    {contentExpanded ? '收起' : '展开全文'}
                  </Button>
                ) : null}
              </>
            )}
          </div>

          <Button
            block
            className="convert-button"
            size="large"
            type="primary"
            loading={convertLoading}
            disabled={!chapterDetail?.content || chapterLoading}
            onClick={() => void handleConvert()}
          >
            开始单场转换
          </Button>
        </Card>

        <Card className="panel" bordered={false}>
          <div className="panel-header compact">
            <div>
              <Text className="panel-kicker">RESULT</Text>
              <Title level={3}>转换结果</Title>
            </div>
          </div>

          {convertError ? (
            <Alert
              className="feedback-block"
              message="转换失败"
              description={convertError}
              type="error"
              showIcon
            />
          ) : null}

          {convertResult ? (
            <div className="scene-result">
              <div className="scene-heading">
                <Text className="panel-kicker">SCENE</Text>
                <Title level={4}>{buildHeadingText(convertResult)}</Title>
              </div>

              <section className="scene-section">
                <Text className="scene-section-title">动作</Text>
                {convertResult.actionLines.map((line, index) => (
                  <Paragraph key={`action-${index}`}>{line}</Paragraph>
                ))}
              </section>

              <section className="scene-section">
                <Text className="scene-section-title">对白</Text>
                {convertResult.dialogueBlocks.map((dialogue, index) => (
                  <div className="dialogue-row" key={`dialogue-${index}`}>
                    <Text strong>{dialogue.character}</Text>
                    {dialogue.parenthetical ? <Text>（{dialogue.parenthetical}）</Text> : null}
                    <Paragraph>{dialogue.line}</Paragraph>
                  </div>
                ))}
              </section>

              {convertResult.visualizedInnerThoughts.length > 0 ? (
                <section className="scene-section">
                  <Text className="scene-section-title">内心戏视觉化</Text>
                  {convertResult.visualizedInnerThoughts.map((thought, index) => (
                    <div className="thought-row" key={`thought-${index}`}>
                      <Text>{thought.original}</Text>
                      <Tag bordered={false}>{thought.method}</Tag>
                      <Text>{thought.result}</Text>
                    </div>
                  ))}
                </section>
              ) : null}

              {convertResult.transitions.length > 0 ? (
                <section className="scene-section">
                  <Text className="scene-section-title">转场</Text>
                  {convertResult.transitions.map((transition, index) => (
                    <Paragraph key={`transition-${index}`}>{transition}</Paragraph>
                  ))}
                </section>
              ) : null}
            </div>
          ) : (
            <div className="result-placeholder">
              <Text>选择章节正文后开始单场转换。</Text>
            </div>
          )}
        </Card>
      </main>
    </div>
  )
}

export default SingleSceneConvertPage
