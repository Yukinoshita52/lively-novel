import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Spin, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import type { GeneratedSceneSummary, ScreenplayConversionDetail } from '../types/novel'
import { getScreenplayConversionDetail } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import type { FlowStepNavigation } from './appNavigation'
import type { FlowStepKey } from './prototypeFlow'
import {
  buildPreviewActions,
  buildPreviewTabs,
  buildScriptBlockRows,
  buildSceneOutlineItems,
  buildSceneTableRows,
  getSourcePreview,
  mapPersistedScenesToGeneratedScenes,
  resolveSelectedScene,
  type PreviewTabKey,
} from './screenplayPreview'

const { Text, Title } = Typography

type ScreenplayPreviewPageProps = {
  session: ConversionSessionState
  onBackToConvert: () => void
  onPolishScene: (sceneKey: string) => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayPreviewPage({
  session,
  onBackToConvert,
  onPolishScene,
  flowNavigation,
  onNavigateStep,
}: ScreenplayPreviewPageProps) {
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [sourceExpanded, setSourceExpanded] = useState(false)
  const [activePreviewTab, setActivePreviewTab] = useState<PreviewTabKey>('script')
  const [conversionDetail, setConversionDetail] = useState<ScreenplayConversionDetail | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)

  useEffect(() => {
    if (!session.conversionId || !session.completed) {
      return undefined
    }

    let active = true

    getScreenplayConversionDetail(session.conversionId)
      .then((detail) => {
        if (active) {
          setConversionDetail(detail)
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setDetailError(error instanceof Error ? error.message : '读取预览详情失败')
        }
      })

    return () => {
      active = false
    }
  }, [session.completed, session.conversionId])

  const generatedScenes: GeneratedSceneSummary[] = useMemo(() => {
    if (conversionDetail?.scenes.length) {
      return mapPersistedScenesToGeneratedScenes(conversionDetail.scenes)
    }

    return session.generatedScenes
  }, [conversionDetail, session.generatedScenes])
  const sceneOutlineItems = useMemo(() => buildSceneOutlineItems(generatedScenes), [generatedScenes])
  const previewTabs = useMemo(() => buildPreviewTabs(activePreviewTab), [activePreviewTab])
  const sceneTableRows = useMemo(() => buildSceneTableRows(generatedScenes), [generatedScenes])
  const selectedScene = useMemo(
    () => resolveSelectedScene(generatedScenes, selectedSceneKey),
    [generatedScenes, selectedSceneKey],
  )
  const loadingDetail = Boolean(session.completed && session.conversionId && !conversionDetail && !detailError)
  const previewActions = buildPreviewActions(Boolean(selectedScene))

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
        bordered={false}
      >
        {loadingDetail ? (
          <div className="screenplay-empty">
            <Spin />
            <Text>正在读取已落库剧本...</Text>
          </div>
        ) : null}
        {detailError ? (
          <Alert className="feedback-block" message="预览详情读取失败" description={detailError} type="warning" showIcon />
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
              <Text>等待第一场剧本生成...</Text>
            </div>
          ) : selectedScene ? (
            <section className="preview-tabs-panel">
              <div className="preview-tab-row">
                {previewTabs.map((tab) => (
                  <button
                    className={`preview-tab${tab.active ? ' active' : ''}`}
                    key={tab.key}
                    onClick={() => setActivePreviewTab(tab.key)}
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
                <div className="screenplay-preview">
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

                    {buildScriptBlockRows(selectedScene).map((block) => {
                      if (block.type === 'DIALOGUE') {
                        return (
                          <div className="sp-dialogue-block" key={block.key}>
                            <div className="sp-character">{block.character}</div>
                            {block.parenthetical ? <div className="sp-parenthetical">{block.parenthetical}</div> : null}
                            <div className="sp-line">{block.line}</div>
                          </div>
                        )
                      }

                      return (
                        <p className={block.type === 'TRANSITION' ? 'sp-transition' : 'sp-action-line'} key={block.key}>
                          {block.text}
                        </p>
                      )
                    })}
                  </div>

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
