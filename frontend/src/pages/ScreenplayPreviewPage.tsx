import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Spin, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import type { GeneratedSceneSummary, ScreenplayConversionDetail } from '../types/novel'
import { getScreenplayConversionDetail, getScreenplayConversionYaml } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import {
  buildPreviewTabs,
  buildSceneOutlineItems,
  buildSceneTableRows,
  buildThoughtAuditRows,
  buildYamlDownloadFileName,
  getSceneKey,
  getSourcePreview,
  mapPersistedScenesToGeneratedScenes,
  resolveSelectedScene,
  type PreviewTabKey,
} from './screenplayPreview'

const { Text, Title } = Typography

type ScreenplayPreviewPageProps = {
  session: ConversionSessionState
  onBackToConvert: () => void
}

function ScreenplayPreviewPage({ session, onBackToConvert }: ScreenplayPreviewPageProps) {
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [sourceExpanded, setSourceExpanded] = useState(false)
  const [activePreviewTab, setActivePreviewTab] = useState<PreviewTabKey>('script')
  const [conversionDetail, setConversionDetail] = useState<ScreenplayConversionDetail | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [downloadingYaml, setDownloadingYaml] = useState(false)
  const [downloadError, setDownloadError] = useState<string | null>(null)

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
  const thoughtAuditRows = useMemo(() => buildThoughtAuditRows(generatedScenes), [generatedScenes])
  const selectedScene = useMemo(
    () => resolveSelectedScene(generatedScenes, selectedSceneKey),
    [generatedScenes, selectedSceneKey],
  )
  const loadingDetail = Boolean(session.completed && session.conversionId && !conversionDetail && !detailError)

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
    <PrototypeFrame currentStep="preview" maxWidth={1280}>
      <PrototypeHero
        eyebrow={`04 · ${session.completed ? '预览' : '实时预览'} · YAML`}
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
        {downloadError ? (
          <Alert className="feedback-block" message="导出失败" description={downloadError} type="error" showIcon />
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
                  disabled={!session.completed || !session.conversionId}
                  icon={<DownloadOutlined />}
                  loading={downloadingYaml}
                  onClick={handleDownloadYaml}
                >
                  导出 YAML
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

              {activePreviewTab === 'thought-audit' ? (
                <div className="thought-audit-list">
                  {thoughtAuditRows.length === 0 ? (
                    <div className="screenplay-empty">
                      <Text>当前已生成场景暂无内心戏视觉化留痕。</Text>
                    </div>
                  ) : (
                    thoughtAuditRows.map((thought) => (
                      <div className="thought-audit-row" key={thought.key}>
                        <Text className="thought-label">{thought.sceneNumber} · 原文</Text>
                        <Text>{thought.original}</Text>
                        <Text className="thought-label">手法</Text>
                        <Text>{thought.method}</Text>
                        <Text className="thought-label">结果</Text>
                        <Text>{thought.result}</Text>
                      </div>
                    ))
                  )}
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
