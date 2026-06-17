import { useEffect, useMemo, useRef } from 'react'
import { Button, Card, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, EditOutlined, WarningOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from '../conversionSession'
import type { GeneratedSceneSummary } from '../../types/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { FlowStepNavigation } from '../appNavigation'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import type { PolishSceneStatus } from '../polish/screenplayPolish'
import {
  buildPreviewActions,
  buildPreviewScrollKey,
  buildPreviewTabs,
  buildGenerationQualityWarnings,
  buildScriptBlockRows,
  buildSceneOutlineItems,
  buildSceneTableRows,
  getSourceDisplayText,
  resolveSelectedScene,
  type PreviewTabKey,
} from './screenplayPreview'
import RenderedScriptBlock from './RenderedScriptBlock'

const { Text, Title } = Typography

type ScreenplayPreviewPageProps = {
  session: ConversionSessionState
  selectedSceneKey?: string
  activePreviewTab: PreviewTabKey
  polishSceneStatusByKey?: Record<string, PolishSceneStatus>
  onSelectScene: (sceneKey: string) => void
  onChangePreviewTab: (tab: PreviewTabKey) => void
  readingScrollByKey?: Record<string, number>
  onRecordReadingScroll?: (key: string, scrollTop: number) => void
  onBackToConvert: () => void
  onPolishScene: (sceneKey: string) => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayPreviewPage({
  session,
  selectedSceneKey,
  activePreviewTab,
  polishSceneStatusByKey = {},
  onSelectScene,
  onChangePreviewTab,
  readingScrollByKey = {},
  onRecordReadingScroll,
  onBackToConvert,
  onPolishScene,
  flowNavigation,
  onNavigateStep,
}: ScreenplayPreviewPageProps) {
  const selectedOutlineRowRef = useRef<HTMLButtonElement | null>(null)
  const readingScrollRef = useRef<HTMLDivElement | null>(null)

  const generatedScenes: GeneratedSceneSummary[] = session.generatedScenes
  const sceneOutlineItems = useMemo(() => buildSceneOutlineItems(generatedScenes), [generatedScenes])
  const qualityWarnings = useMemo(() => buildGenerationQualityWarnings(generatedScenes), [generatedScenes])
  const previewTabs = useMemo(() => buildPreviewTabs(activePreviewTab), [activePreviewTab])
  const sceneTableRows = useMemo(() => buildSceneTableRows(generatedScenes), [generatedScenes])
  const selectedScene = useMemo(
    () => resolveSelectedScene(generatedScenes, selectedSceneKey),
    [generatedScenes, selectedSceneKey],
  )
  const previewActions = buildPreviewActions(Boolean(selectedScene))
  const selectedSceneWarnings = selectedScene?.warnings ?? []
  const selectedSceneKeyForScroll = selectedScene?.key
  const readingScrollKey = selectedSceneKeyForScroll ? buildPreviewScrollKey(selectedSceneKeyForScroll, activePreviewTab) : undefined

  useEffect(() => {
    const row = selectedOutlineRowRef.current
    if (!row) {
      return
    }

    row.scrollIntoView({ block: 'nearest' })
  }, [selectedScene?.key])

  useEffect(() => {
    const previewElement = readingScrollRef.current
    if (!previewElement || !readingScrollKey) {
      return
    }

    previewElement.scrollTop = readingScrollByKey[readingScrollKey] ?? 0
  }, [readingScrollByKey, readingScrollKey])

  function handleReadingScroll() {
    if (!readingScrollKey || !onRecordReadingScroll || !readingScrollRef.current) {
      return
    }

    onRecordReadingScroll(readingScrollKey, readingScrollRef.current.scrollTop)
  }

  function selectWarningScene(sceneKey: string) {
    onSelectScene(sceneKey)
    onChangePreviewTab('script')
  }

  return (
    <PrototypeFrame
      currentStep="preview"
      maxWidth={1280}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow={`03 · ${session.completed ? '预览' : '实时预览'} · YAML`}
        title={`《${session.context.title}》`}
        meta={session.completed ? '已生成逐场剧本' : '转换仍在后台继续'}
        action={
          <Button className="prototype-ghost-button" icon={<ArrowLeftOutlined />} onClick={onBackToConvert}>
            返回转换
          </Button>
        }
      />

      <Card
        className="prototype-panel scene-preview-panel"
        title={<PrototypePanelTitle code="PREVIEW" title="逐场预览" meta="YAML → 渲染视图" />}
        variant="borderless"
      >
        {qualityWarnings.length > 0 ? (
          <div className="quality-warning-summary">
            <div className="quality-warning-summary-head">
              <WarningOutlined />
              <Text>建议重点检查 {qualityWarnings.length} 项</Text>
            </div>
            <div className="quality-warning-list">
              {qualityWarnings.map((warning) => (
                <button
                  className={`quality-warning-chip severity-${warning.severity}`}
                  key={warning.key}
                  onClick={() => selectWarningScene(warning.sceneKey)}
                  type="button"
                >
                  <span>{warning.sceneNumber}</span>
                  {warning.title}
                </button>
              ))}
            </div>
          </div>
        ) : null}

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
                  ref={scene.key === selectedScene?.key ? selectedOutlineRowRef : undefined}
                  onClick={() => {
                    onSelectScene(scene.key)
                  }}
                  type="button"
                >
                  <span className="scene-outline-no">{scene.sceneNumber}</span>
                  <span className="scene-outline-copy">
                    <span className="scene-outline-title">{scene.title}</span>
                    <span className="scene-outline-heading">{scene.headingText}</span>
                  </span>
                  <span className="scene-outline-status">
                    {polishSceneStatusByKey[scene.key]?.changed ? (
                      <span className="scene-outline-state">已改</span>
                    ) : null}
                    {polishSceneStatusByKey[scene.key]?.unsaved ? (
                      <span className="scene-outline-state unsaved">未存</span>
                    ) : null}
                    {scene.warnings.length > 0 ? (
                      <span className="scene-outline-warning">
                        <WarningOutlined />
                        {scene.warnings.length}
                      </span>
                    ) : null}
                    <span className="scene-outline-ch">CH{scene.chapterIndex}</span>
                  </span>
                </button>
              ))
            )}
          </aside>

          {generatedScenes.length === 0 ? (
            <div className="screenplay-empty">
              <Text>等待第一场剧本生成...</Text>
            </div>
          ) : selectedScene ? (
            <section className="preview-tabs-panel">
              <div className="preview-tab-row">
                {previewTabs.map((tab) => (
                  <button
                    className={`preview-tab${tab.active ? ' active' : ''}`}
                    key={tab.key}
                    onClick={() => onChangePreviewTab(tab.key)}
                    type="button"
                  >
                    {tab.label}
                  </button>
                ))}
                <Button
                  className="preview-export-button"
                  disabled={!previewActions.primary.enabled || !selectedScene}
                  icon={<EditOutlined />}
                  onClick={() => {
                    if (selectedScene) {
                      onPolishScene(selectedScene.key)
                    }
                  }}
                >
                  {previewActions.primary.label}
                </Button>
              </div>

              {activePreviewTab === 'script' ? (
                <div className="screenplay-preview preview-reading-scroll" onScroll={handleReadingScroll} ref={readingScrollRef}>
                  <div className="screenplay-scene-meta">
                    <Tag variant="filled">{selectedScene.sceneNumber}</Tag>
                    <Text>第 {selectedScene.chapterIndex} 章</Text>
                    {selectedScene.sceneIndexInChapter ? <Text>第 {selectedScene.sceneIndexInChapter} 场</Text> : null}
                  </div>
                  {selectedSceneWarnings.length > 0 ? (
                    <div className="scene-warning-panel">
                      {selectedSceneWarnings.map((warning) => (
                        <div className={`scene-warning-row severity-${warning.severity}`} key={warning.key}>
                          <Text className="scene-warning-title">{warning.title}</Text>
                          <Text>{warning.message}</Text>
                        </div>
                      ))}
                    </div>
                  ) : null}
                  <Title level={4}>{selectedScene.title}</Title>

                  <div className="screenplay-paper">
                    <div className="sp-scene-heading">
                      <span>{selectedScene.sceneNumber}</span>
                      {selectedScene.headingText}
                    </div>

                    {buildScriptBlockRows(selectedScene).map((block) => (
                      <RenderedScriptBlock block={block} key={block.key} />
                    ))}
                  </div>

                </div>
              ) : null}

              {activePreviewTab === 'source' ? (
                <div className="screenplay-preview preview-reading-scroll" onScroll={handleReadingScroll} ref={readingScrollRef}>
                  <div className="screenplay-scene-meta">
                    <Tag variant="filled">{selectedScene.sceneNumber}</Tag>
                    <Text>第 {selectedScene.chapterIndex} 章</Text>
                    {selectedScene.sceneIndexInChapter ? <Text>第 {selectedScene.sceneIndexInChapter} 场</Text> : null}
                  </div>
                  <Title level={4}>{selectedScene.title}</Title>

                  <div className="screenplay-paper source-reading-paper">
                    <div className="sp-scene-heading">
                      <span>{selectedScene.sceneNumber}</span>
                      本场原文
                    </div>
                    <p className="source-reading-text">{getSourceDisplayText(selectedScene.scene.sourceText || '')}</p>
                  </div>
                </div>
              ) : null}

              {activePreviewTab === 'scene-table' ? (
                <div className="preview-table-wrap">
                  <table className="preview-table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>内/外</th>
                        <th>地点</th>
                        <th>时间</th>
                        <th>源章</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sceneTableRows.map((row) => (
                        <tr key={row.key}>
                          <td>{row.sceneNumber}</td>
                          <td>{row.interiorText}</td>
                          <td>{row.location}</td>
                          <td>{row.timeOfDay}</td>
                          <td>{row.sourceChapterText}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}

            </section>
          ) : null}
        </div>
      </Card>
    </PrototypeFrame>
  )
}

export default ScreenplayPreviewPage
