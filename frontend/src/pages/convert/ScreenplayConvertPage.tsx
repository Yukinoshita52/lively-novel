import { useMemo } from 'react'
import { Button, Card, Progress, Typography } from 'antd'
import { ArrowLeftOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from '../conversionSession'
import {
  resolvePreviewEntryState,
  resolveRestoredConversionSummary,
  resolveResumeEntryState,
} from '../conversionSession'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import {
  buildConvertProgressNote,
  buildPipelinePhases,
  buildStreamEventParts,
  type FlowStepKey,
  resolveCurrentConvertChapterIndex,
} from '../../components/prototype/prototypeFlow'
import type { FlowStepNavigation } from '../appNavigation'
import { formatHistoryTime } from '../import/importFormat'

const { Text } = Typography

type ScreenplayConvertPageProps = {
  session: ConversionSessionState
  onBack: () => void
  onPreview: () => void
  onResume: () => void
  onRetry: () => void
  backLabel?: string
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayConvertPage({
  session,
  onBack,
  onPreview,
  onResume,
  onRetry,
  backLabel = '返回导入',
  flowNavigation,
  onNavigateStep,
}: ScreenplayConvertPageProps) {
  const finishedSceneCount = session.generatedScenes.length
  const totalSceneCount = useMemo(
    () => Object.values(session.chapterSceneCounts).reduce((sum, count) => sum + count, 0),
    [session.chapterSceneCounts],
  )
  const currentChapterIndex = useMemo(() => {
    const plannedChapterIndexes = Object.keys(session.chapterSceneCounts)
      .map((chapterIndex) => Number(chapterIndex))
      .filter((chapterIndex) => Number.isFinite(chapterIndex))
    return resolveCurrentConvertChapterIndex({
      generatedChapterIndexes: session.generatedScenes.map((scene) => scene.chapterIndex),
      plannedChapterIndexes,
    })
  }, [session.chapterSceneCounts, session.generatedScenes])
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
  const restoredSummary = resolveRestoredConversionSummary(session.context)
  const recentScene = session.generatedScenes.at(-1)
  const chapterProgressText = currentChapterIndex > 0 ? `第 ${currentChapterIndex} 章` : '章节待定'
  const sceneProgressText = totalSceneCount > 0
    ? `${finishedSceneCount} / ${totalSceneCount} 场`
    : `${finishedSceneCount} 场`

  return (
    <PrototypeFrame
      currentStep="convert"
      maxWidth={1280}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow="02 · 转换流程演示 · SSE"
        title={`《${session.context.title}》`}
        meta="→ 动画剧本"
        action={
          <Button className="prototype-ghost-button" icon={<ArrowLeftOutlined />} onClick={onBack}>
            {backLabel}
          </Button>
        }
      />

      <main className="convert-grid convert-progress-grid">
        <Card
          className="prototype-panel"
          title={<PrototypePanelTitle code="PIPE" title="转换流水线" />}
          variant="borderless"
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

          <div className="conversion-progress-summary">
            <div className="conversion-progress-row">
              <span className="conversion-progress-label">章节进度</span>
              <span className="conversion-progress-value">{chapterProgressText}</span>
            </div>
            <div className="conversion-progress-row">
              <span className="conversion-progress-label">场景进度</span>
              <span className="conversion-progress-value">{sceneProgressText}</span>
            </div>
            <div className="conversion-progress-row">
              <span className="conversion-progress-label">最近完成</span>
              <span className="conversion-progress-value">
                {recentScene ? `第 ${recentScene.chapterIndex} 章 · ${recentScene.title}` : '等待第一场生成'}
              </span>
            </div>
          </div>
          <div className="prototype-progress-note">
            <span>{buildConvertProgressNote({ currentChapterIndex, finishedSceneCount, totalSceneCount })}</span>
          </div>
          <Progress className="prototype-progress" percent={progressPercent} strokeColor="#be3a2e" showInfo={false} />
          {restoredSummary ? (
            <div className="conversion-summary">
              <Text className={`conversion-summary-status status-${restoredSummary.status}`}>
                {restoredSummary.statusLabel}
              </Text>
              <Text>最近更新时间：{formatHistoryTime(restoredSummary.updatedAt ?? null)}</Text>
              <Text className="conversion-summary-id">转换 ID：{restoredSummary.conversionId}</Text>
            </div>
          ) : null}
          <div className="prototype-export-row">
            <Button disabled={!previewEntry.enabled} icon={<EyeOutlined />} onClick={onPreview} type="primary">
              {previewEntry.label}
            </Button>
            {resumeEntry.enabled ? (
              <Button icon={<ReloadOutlined />} onClick={onResume}>
                {resumeEntry.label}
              </Button>
            ) : null}
            {session.convertError ? (
              <Button onClick={onRetry}>
                重新尝试
              </Button>
            ) : null}
            {session.convertError ? (
              <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
                返回历史作品
              </Button>
            ) : null}
          </div>
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
          variant="borderless"
        >
          <div className="prototype-stream">
            {session.events.length === 0 ? (
              <div className="prototype-stream-line muted">event: waiting 等待服务端返回事件...</div>
            ) : (
              session.events.map((event, index) => (
                <div className="prototype-stream-line" key={`${event.type}-${index}`}>
                  {buildStreamEventParts(event).map((part, partIndex) => (
                    <span className={`prototype-stream-${part.kind}`} key={`${part.kind}-${partIndex}`}>
                      {part.text}
                    </span>
                  ))}
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
