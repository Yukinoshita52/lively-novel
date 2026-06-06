import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Progress, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import type {
  ConvertEventItem,
  GeneratedSceneSummary,
  ScreenplayConvertContext,
} from '../types/novel'
import { getScreenplayConversionYaml } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import { buildPipelinePhases, formatStreamEvent } from './prototypeFlow'
import {
  buildYamlDownloadFileName,
  buildSceneOutlineItems,
  getSceneKey,
  getSourcePreview,
  resolveConvertEventUpdate,
  resolveSelectedScene,
} from './screenplayPreview'

const { Text, Title } = Typography

type ScreenplayConvertPageProps = {
  context: ScreenplayConvertContext
  onBack: () => void
}

type SsePayload = Record<string, unknown>

function toJsonPayload(raw: string): SsePayload {
  try {
    return JSON.parse(raw) as SsePayload
  } catch {
    return {}
  }
}

function findEventDelimiter(buffer: string) {
  const lfIndex = buffer.indexOf('\n\n')
  const crlfIndex = buffer.indexOf('\r\n\r\n')

  if (lfIndex === -1) {
    return crlfIndex
  }

  if (crlfIndex === -1) {
    return lfIndex
  }

  return Math.min(lfIndex, crlfIndex)
}

function delimiterLengthAt(buffer: string, index: number) {
  return buffer.startsWith('\r\n\r\n', index) ? 4 : 2
}

function ScreenplayConvertPage({ context, onBack }: ScreenplayConvertPageProps) {
  const [connecting, setConnecting] = useState(true)
  const [events, setEvents] = useState<ConvertEventItem[]>([])
  const [generatedScenes, setGeneratedScenes] = useState<GeneratedSceneSummary[]>([])
  const [chapterSceneCounts, setChapterSceneCounts] = useState<Record<number, number>>({})
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [sourceExpanded, setSourceExpanded] = useState(false)
  const [completed, setCompleted] = useState(false)
  const [conversionId, setConversionId] = useState<string>()
  const [downloadingYaml, setDownloadingYaml] = useState(false)
  const [convertError, setConvertError] = useState<string | null>(null)
  const [downloadError, setDownloadError] = useState<string | null>(null)

  useEffect(() => {
    const abortController = new AbortController()

    function pushEvent(type: ConvertEventItem['type'], message: string) {
      setEvents((current) => [...current, { type, message }])
    }

    function handleEventBlock(block: string) {
      const lines = block
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)

      const eventName = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
      const dataLines = lines
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())

      if (!eventName || dataLines.length === 0) {
        return
      }

      const payload = toJsonPayload(dataLines.join('\n'))
      const update = resolveConvertEventUpdate(eventName, payload, {
        totalChapters: context.totalChapters,
      })
      if (!update) {
        return
      }

      if (update.event) {
        pushEvent(update.event.type, update.event.message)
      }
      if (update.conversionId) {
        setConversionId(update.conversionId)
      }
      if (update.sceneCount) {
        setChapterSceneCounts((current) => ({
          ...current,
          [update.sceneCount!.chapterIndex]: update.sceneCount!.sceneCount,
        }))
      }
      if (update.generatedScene) {
        setGeneratedScenes((current) => [...current, update.generatedScene!])
      }
      if (update.completed) {
        setCompleted(true)
      }
      if (update.convertError) {
        setConvertError(update.convertError)
        setConnecting(false)
      }
    }

    async function startConvert() {
      try {
        const response = await fetch('/api/screenplay/convert', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            novelId: context.novelId,
            screenplayType: context.screenplayType,
          }),
          signal: abortController.signal,
        })

        if (!response.ok || !response.body) {
          throw new Error('启动转换失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            break
          }

          buffer += decoder.decode(value, { stream: true })

          let delimiterIndex = findEventDelimiter(buffer)
          while (delimiterIndex >= 0) {
            const block = buffer.slice(0, delimiterIndex)
            buffer = buffer.slice(delimiterIndex + delimiterLengthAt(buffer, delimiterIndex))
            handleEventBlock(block)
            delimiterIndex = findEventDelimiter(buffer)
          }
        }

        if (buffer.trim()) {
          handleEventBlock(buffer)
        }
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        const message = error instanceof Error ? error.message : '整本转换失败'
        setConvertError(message)
        pushEvent('error', message)
      } finally {
        setConnecting(false)
      }
    }

    void startConvert()

    return () => {
      abortController.abort()
    }
  }, [context.novelId, context.screenplayType, context.totalChapters])

  const finishedSceneCount = generatedScenes.length
  const sceneOutlineItems = useMemo(() => buildSceneOutlineItems(generatedScenes), [generatedScenes])
  const selectedScene = useMemo(
    () => resolveSelectedScene(generatedScenes, selectedSceneKey),
    [generatedScenes, selectedSceneKey],
  )
  const totalSceneCount = useMemo(
    () => Object.values(chapterSceneCounts).reduce((sum, count) => sum + count, 0),
    [chapterSceneCounts],
  )
  const progressPercent = useMemo(() => {
    if (totalSceneCount <= 0) {
      return 0
    }
    return Math.min(100, Math.round((finishedSceneCount / totalSceneCount) * 100))
  }, [finishedSceneCount, totalSceneCount])
  const pipelinePhases = useMemo(
    () => buildPipelinePhases({
      completed,
      convertError,
      finishedSceneCount,
      totalSceneCount,
    }),
    [completed, convertError, finishedSceneCount, totalSceneCount],
  )

  async function handleDownloadYaml() {
    if (!conversionId) {
      return
    }

    setDownloadingYaml(true)
    setDownloadError(null)

    try {
      const yamlBlob = await getScreenplayConversionYaml(conversionId)
      const url = URL.createObjectURL(yamlBlob)
      const link = document.createElement('a')
      link.href = url
      link.download = buildYamlDownloadFileName(context.title)
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (error) {
      setDownloadError(error instanceof Error ? error.message : '导出 YAML 失败')
    } finally {
      setDownloadingYaml(false)
    }
  }

  return (
    <PrototypeFrame currentStep="convert" maxWidth={1280}>
      <PrototypeHero
        eyebrow={`03 · ${completed ? '转换完成' : convertError ? '转换中断' : '转换中'} · SSE`}
        title={`《${context.title}》`}
        meta="→ 动画剧本"
        action={
          <Button className="prototype-ghost-button" icon={<ArrowLeftOutlined />} onClick={onBack}>
            返回导入
          </Button>
        }
      />

      <main className="convert-grid convert-progress-grid">
        <Card
          className="prototype-panel"
          title={<PrototypePanelTitle code="PIPE" title="两阶段转换" />}
          bordered={false}
        >
          <div className="prototype-phase-list">
            {pipelinePhases.map((phase) => (
              <div className={`prototype-phase ${phase.status}`} key={phase.key}>
                <div className="prototype-phase-row">
                  <span className="prototype-phase-dot">{phase.mark}</span>
                  <div>
                    <div className="prototype-phase-name">{phase.title}</div>
                    <div className="prototype-phase-desc">{phase.description}</div>
                  </div>
                </div>
                <div className="prototype-phase-bar">
                  <i style={{ width: `${phase.progress}%` }} />
                </div>
              </div>
            ))}
          </div>

          <div className="prototype-progress-note">
            <span>本篇 {context.chapters.length} 章已进入转换队列</span>
            <span>{finishedSceneCount} / {totalSceneCount || '?'} 场</span>
          </div>
          <Progress className="prototype-progress" percent={progressPercent} strokeColor="#be3a2e" showInfo={false} />
          <div className="prototype-export-row">
            <Button
              disabled={!completed || !conversionId}
              icon={<DownloadOutlined />}
              loading={downloadingYaml}
              onClick={handleDownloadYaml}
              type="primary"
            >
              导出 YAML
            </Button>
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
          {downloadError ? (
            <Alert
              className="feedback-block"
              message="导出失败"
              description={downloadError}
              type="error"
              showIcon
            />
          ) : null}
        </Card>

        <Card
          className="prototype-panel event-panel"
          title={<PrototypePanelTitle code="SSE" title="实时事件流" meta={connecting && !completed ? '● LIVE' : 'STREAM'} />}
          bordered={false}
        >
          <div className="prototype-stream">
            {events.length === 0 ? (
              <div className="prototype-stream-line muted">event: waiting 等待服务端返回事件…</div>
            ) : (
              events.map((event, index) => (
                <div className="prototype-stream-line" key={`${event.type}-${index}`}>
                  {formatStreamEvent(event)}
                </div>
              ))
            )}
            {!completed && !convertError ? <span className="prototype-caret" /> : null}
          </div>
        </Card>

        <Card
          className="prototype-panel scene-preview-panel"
          title={<PrototypePanelTitle code="PREVIEW" title="逐场预览" meta="YAML → 渲染视图" />}
          bordered={false}
        >

          <div className="scene-preview-grid">
            <aside className="scene-outline">
              {sceneOutlineItems.length === 0 ? (
                <div className="scene-outline-empty">
                  <Text>场景生成后会显示在这里。</Text>
                </div>
              ) : (
                sceneOutlineItems.map((scene) => (
                  <button
                    className={`scene-outline-row ${scene.key === selectedScene?.key ? 'active' : ''}`}
                    key={scene.key}
                    onClick={() => {
                      setSelectedSceneKey(scene.key)
                      setSourceExpanded(false)
                    }}
                    type="button"
                  >
                    <span className="scene-outline-no">{scene.sceneNumber}</span>
                    <span className="scene-outline-copy">
                      <span className="scene-outline-title">{scene.title}</span>
                      <span className="scene-outline-heading">{scene.headingText}</span>
                    </span>
                    <span className="scene-outline-ch">CH{scene.chapterIndex}</span>
                  </button>
                ))
              )}
            </aside>

            {generatedScenes.length === 0 ? (
              <div className="screenplay-empty">
                <Text>等待第一场剧本生成…</Text>
              </div>
            ) : selectedScene ? (
              <section className="screenplay-preview">
                <div className="screenplay-scene-meta">
                  <Tag bordered={false}>{selectedScene.sceneNumber}</Tag>
                  <Text>第 {selectedScene.chapterIndex} 章</Text>
                  {selectedScene.sceneIndexInChapter ? <Text>第 {selectedScene.sceneIndexInChapter} 场</Text> : null}
                </div>
                <Title level={4}>{selectedScene.title}</Title>

                <div className="screenplay-paper">
                  <div className="sp-scene-heading">
                    <span>{selectedScene.sceneNumber}</span>
                    {selectedScene.headingText}
                  </div>

                  {selectedScene.scene.actionLines.map((line, index) => (
                    <p className="sp-action-line" key={`${getSceneKey(selectedScene)}-action-${index}`}>
                      {line}
                    </p>
                  ))}

                  {selectedScene.scene.dialogueBlocks.map((dialogue, index) => (
                    <div className="sp-dialogue-block" key={`${getSceneKey(selectedScene)}-dialogue-${index}`}>
                      <div className="sp-character">{dialogue.character}</div>
                      {dialogue.parenthetical ? <div className="sp-parenthetical">{dialogue.parenthetical}</div> : null}
                      <div className="sp-line">{dialogue.line}</div>
                    </div>
                  ))}

                  {selectedScene.scene.transitions.map((transition, index) => (
                    <p className="sp-transition" key={`${getSceneKey(selectedScene)}-transition-${index}`}>
                      {transition}
                    </p>
                  ))}
                </div>

                {selectedScene.scene.visualizedInnerThoughts.length > 0 ? (
                  <div className="thought-audit">
                    <Text className="scene-section-title">内心戏视觉化留痕</Text>
                    {selectedScene.scene.visualizedInnerThoughts.map((thought, index) => (
                      <div className="thought-audit-row" key={`${getSceneKey(selectedScene)}-thought-${index}`}>
                        <Text className="thought-label">原文</Text>
                        <Text>{thought.original}</Text>
                        <Text className="thought-label">手法</Text>
                        <Text>{thought.method}</Text>
                        <Text className="thought-label">结果</Text>
                        <Text>{thought.result}</Text>
                      </div>
                    ))}
                  </div>
                ) : null}

                <div className="source-preview">
                  <div className="source-preview-head">
                    <Text className="scene-section-title">本场原文</Text>
                    <Button
                      className="content-toggle"
                      onClick={() => setSourceExpanded((current) => !current)}
                      type="link"
                    >
                      {sourceExpanded ? '收起' : '展开'}
                    </Button>
                  </div>
                  <div className="source-preview-text">
                    {getSourcePreview(selectedScene.scene.sourceText || '', sourceExpanded)}
                  </div>
                </div>
              </section>
            ) : null}
          </div>
        </Card>
      </main>
    </PrototypeFrame>
  )
}

export default ScreenplayConvertPage
