import { useMemo, useState } from 'react'
import { Alert, Button, Card, Progress } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import { resolvePreviewEntryState, resolveResumeEntryState } from './conversionSession'
import { getScreenplayConversionYaml } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import { buildPipelinePhases, formatStreamEvent } from './prototypeFlow'
import { buildYamlDownloadFileName } from './screenplayPreview'

type ScreenplayConvertPageProps = {
  session: ConversionSessionState
  onBack: () => void
  onPreview: () => void
  onResume: () => void
}

function ScreenplayConvertPage({ session, onBack, onPreview, onResume }: ScreenplayConvertPageProps) {
  const [downloadingYaml, setDownloadingYaml] = useState(false)
  const [downloadError, setDownloadError] = useState<string | null>(null)

  const finishedSceneCount = session.generatedScenes.length
  const totalSceneCount = useMemo(
    () => Object.values(session.chapterSceneCounts).reduce((sum, count) => sum + count, 0),
    [session.chapterSceneCounts],
  )
  const progressPercent = useMemo(() => {
    if (totalSceneCount <= 0) {
      return 0
    }
    return Math.min(100, Math.round((finishedSceneCount / totalSceneCount) * 100))
  }, [finishedSceneCount, totalSceneCount])
  const pipelinePhases = useMemo(
    () => buildPipelinePhases({
      completed: session.completed,
      convertError: session.convertError,
      finishedSceneCount,
      totalSceneCount,
    }),
    [finishedSceneCount, session.completed, session.convertError, totalSceneCount],
  )
  const previewEntry = resolvePreviewEntryState(session)
  const resumeEntry = resolveResumeEntryState(session)

  async function handleDownloadYaml() {
    if (!session.conversionId) {
      return
    }

    setDownloadingYaml(true)
    setDownloadError(null)

    try {
      const yamlBlob = await getScreenplayConversionYaml(session.conversionId)
      const url = URL.createObjectURL(yamlBlob)
      const link = document.createElement('a')
      link.href = url
      link.download = buildYamlDownloadFileName(session.context.title)
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
        eyebrow={`03 · ${session.completed ? '转换完成' : session.convertError ? '转换中断' : '转换中'} · SSE`}
        title={`《${session.context.title}》`}
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
            <span>本篇 {session.context.chapters.length} 章已进入转换队列</span>
            <span>
              {finishedSceneCount} / {totalSceneCount || '?'} 场
            </span>
          </div>
          <Progress className="prototype-progress" percent={progressPercent} strokeColor="#be3a2e" showInfo={false} />
          <div className="prototype-export-row">
            <Button disabled={!previewEntry.enabled} icon={<EyeOutlined />} onClick={onPreview} type="primary">
              {previewEntry.label}
            </Button>
            {resumeEntry.enabled ? (
              <Button icon={<ReloadOutlined />} onClick={onResume}>
                {resumeEntry.label}
              </Button>
            ) : null}
            <Button
              disabled={!session.completed || !session.conversionId}
              icon={<DownloadOutlined />}
              loading={downloadingYaml}
              onClick={handleDownloadYaml}
            >
              导出 YAML
            </Button>
          </div>

          {session.convertError ? (
            <Alert
              className="feedback-block"
              message="转换失败"
              description={<span style={{ whiteSpace: 'pre-line' }}>{session.convertError}</span>}
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
          title={
            <PrototypePanelTitle
              code="SSE"
              title="实时事件流"
              meta={session.connecting && !session.completed ? '● LIVE' : 'STREAM'}
            />
          }
          bordered={false}
        >
          <div className="prototype-stream">
            {session.events.length === 0 ? (
              <div className="prototype-stream-line muted">event: waiting 等待服务端返回事件...</div>
            ) : (
              session.events.map((event, index) => (
                <div className="prototype-stream-line" key={`${event.type}-${index}`}>
                  {formatStreamEvent(event)}
                </div>
              ))
            )}
            {!session.completed && !session.convertError ? <span className="prototype-caret" /> : null}
          </div>
        </Card>
      </main>
    </PrototypeFrame>
  )
}

export default ScreenplayConvertPage
