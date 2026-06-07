import { useEffect, useMemo, useRef, useState, type CSSProperties, type UIEvent } from 'react'
import { Alert, Button, Card, Input, message as antdMessage, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownOutlined, LeftOutlined, RightOutlined, SaveOutlined, StopOutlined, UpOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import { updateScreenplayScene } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import type { FlowStepNavigation } from './appNavigation'
import type { FlowStepKey } from './prototypeFlow'
import {
  buildSceneHeadingText,
  buildSceneOutlineItems,
  buildScriptBlockRows,
  resolveAdjacentSceneKeys,
  resolveSelectedScene,
} from './screenplayPreview'
import {
  POLISH_YAML_SCROLLBAR_GUTTER_PX,
  POLISH_YAML_SPELL_CHECK,
  buildYamlLineNumbers,
  buildPolishYamlLineNumberTransform,
  buildPolishWorkspaceLayout,
  createPolishDraft,
  resetPolishDraft,
  updatePolishSceneYaml,
  type PolishDraft,
} from './screenplayPolish'

const { Text, Title } = Typography
const { TextArea } = Input

type ScreenplayPolishPageProps = {
  session: ConversionSessionState
  selectedSceneKey?: string
  draftsBySceneKey: Record<string, PolishDraft>
  onUpdateDraft: (sceneKey: string, draft: PolishDraft) => void
  onSelectScene: (sceneKey: string) => void
  onBackToPreview: () => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayPolishPage({
  session,
  selectedSceneKey,
  draftsBySceneKey,
  onUpdateDraft,
  onSelectScene,
  onBackToPreview,
  flowNavigation,
  onNavigateStep,
}: ScreenplayPolishPageProps) {
  const [scenePickerExpanded, setScenePickerExpanded] = useState(false)
  const [savingScene, setSavingScene] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const yamlLineLayerRef = useRef<HTMLDivElement | null>(null)
  const previewScrollRef = useRef<HTMLDivElement | null>(null)
  const scene = useMemo(
    () => resolveSelectedScene(session.generatedScenes, selectedSceneKey),
    [selectedSceneKey, session.generatedScenes],
  )
  const sceneOutlineItems = useMemo(() => buildSceneOutlineItems(session.generatedScenes), [session.generatedScenes])
  const draft = useMemo(() => {
    if (!scene) {
      return undefined
    }

    return draftsBySceneKey[scene.key] ?? createPolishDraft(scene.scene)
  }, [draftsBySceneKey, scene])
  const draftScene = draft?.scene
  const adjacentSceneKeys = useMemo(
    () => resolveAdjacentSceneKeys(session.generatedScenes, scene?.key),
    [scene?.key, session.generatedScenes],
  )
  const workspaceLayout = useMemo(() => buildPolishWorkspaceLayout(), [])
  const yamlLineNumbers = useMemo(
    () => buildYamlLineNumbers(draft?.sceneYamlText ?? ''),
    [draft?.sceneYamlText],
  )

  useEffect(() => {
    if (yamlLineLayerRef.current) {
      yamlLineLayerRef.current.style.transform = buildPolishYamlLineNumberTransform(0)
    }
  }, [scene?.key])

  function updateDraft(nextDraft: PolishDraft) {
    if (!scene) {
      return
    }

    onUpdateDraft(scene.key, nextDraft)
  }

  async function handleSaveDraft() {
    if (!scene || !draftScene || !session.conversionId || !scene.sceneIndexInChapter) {
      return
    }

    setSavingScene(true)
    setSaveError(null)

    try {
      const savedScene = await updateScreenplayScene(
        session.conversionId,
        scene.chapterIndex,
        scene.sceneIndexInChapter,
        draftScene,
      )
      onUpdateDraft(scene.key, createPolishDraft(savedScene))
      antdMessage.open({
        key: 'polish-save-success',
        type: 'success',
        content: '保存成功',
        duration: 1.8,
      })
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : '保存本场失败')
    } finally {
      setSavingScene(false)
    }
  }

  function handleCancelDraft() {
    if (!scene || !draft) {
      return
    }

    onUpdateDraft(scene.key, resetPolishDraft(draft))
    setSaveError(null)
  }

  function handleYamlScroll(event: UIEvent<HTMLTextAreaElement>) {
    const editorElement = event.currentTarget
    if (yamlLineLayerRef.current) {
      yamlLineLayerRef.current.style.transform = buildPolishYamlLineNumberTransform(editorElement.scrollTop)
    }

    const previewElement = previewScrollRef.current
    if (!workspaceLayout.syncScroll || !previewElement) {
      return
    }

    const editorScrollableHeight = editorElement.scrollHeight - editorElement.clientHeight
    const previewScrollableHeight = previewElement.scrollHeight - previewElement.clientHeight
    if (editorScrollableHeight <= 0 || previewScrollableHeight <= 0) {
      return
    }

    previewElement.scrollTop = (editorElement.scrollTop / editorScrollableHeight) * previewScrollableHeight
  }

  return (
    <PrototypeFrame
      currentStep="polish"
      maxWidth={1280}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow="04 · 逐场打磨"
        title={scene ? `${scene.sceneNumber} · ${buildSceneHeadingText(scene.scene.heading)}` : '选择场景后打磨'}
        meta="编辑结构 · 检查渲染"
        action={
          <Button className="prototype-ghost-button" icon={<ArrowLeftOutlined />} onClick={onBackToPreview}>
            返回预览
          </Button>
        }
      />

      {!scene ? (
        <Card className="prototype-panel" bordered={false}>
          <div className="screenplay-empty">
            <Text>请先从预览页选择一个场景。</Text>
          </div>
        </Card>
      ) : draft && draftScene ? (
        <main className="polish-grid">
          <Card
            className="prototype-panel polish-scene-picker"
            title={
              <PrototypePanelTitle
                code="SCENES"
                title="选择要打磨的场景"
                meta={`${sceneOutlineItems.length} 场`}
              />
            }
            bordered={false}
          >
            <div className="polish-switch-row">
              <Button
                disabled={!adjacentSceneKeys.previousKey}
                icon={<LeftOutlined />}
                onClick={() => {
                  if (adjacentSceneKeys.previousKey) {
                    onSelectScene(adjacentSceneKeys.previousKey)
                  }
                }}
              >
                上一场景
              </Button>
              <div className="polish-current-scene">
                <Text>{scene.sceneNumber}</Text>
                <Text>{scene.title}</Text>
              </div>
              <Button
                disabled={!adjacentSceneKeys.nextKey}
                icon={<RightOutlined />}
                onClick={() => {
                  if (adjacentSceneKeys.nextKey) {
                    onSelectScene(adjacentSceneKeys.nextKey)
                  }
                }}
              >
                下一场景
              </Button>
              <Button
                icon={scenePickerExpanded ? <UpOutlined /> : <DownOutlined />}
                onClick={() => setScenePickerExpanded((current) => !current)}
              >
                {scenePickerExpanded ? '收起场景列表' : '展开场景列表'}
              </Button>
            </div>
            {scenePickerExpanded ? (
              <div className="scene-outline polish-outline-row">
                {sceneOutlineItems.map((outlineScene) => (
                  <button
                    className={`scene-outline-row ${outlineScene.key === scene.key ? 'active' : ''}`}
                    key={outlineScene.key}
                    onClick={() => onSelectScene(outlineScene.key)}
                    type="button"
                  >
                    <span className="scene-outline-no">{outlineScene.sceneNumber}</span>
                    <span className="scene-outline-copy">
                      <span className="scene-outline-title">{outlineScene.title}</span>
                      <span className="scene-outline-heading">{outlineScene.headingText}</span>
                    </span>
                  </button>
                ))}
              </div>
            ) : null}
          </Card>

          <Card
            className="prototype-panel polish-work-panel polish-yaml-panel"
            title={
              <div className="polish-panel-heading">
                <PrototypePanelTitle
                  code={workspaceLayout.left.code}
                  title={workspaceLayout.left.title}
                />
                <div className="polish-heading-actions">
                  <Button icon={<StopOutlined />} onClick={handleCancelDraft}>
                    取消
                  </Button>
                  <Button
                    disabled={!session.conversionId || !scene.sceneIndexInChapter}
                    icon={<SaveOutlined />}
                    loading={savingScene}
                    onClick={handleSaveDraft}
                    type="primary"
                  >
                    保存
                  </Button>
                </div>
              </div>
            }
            bordered={false}
          >
            {saveError ? (
              <Alert className="feedback-block" message="保存失败" description={saveError} type="error" showIcon />
            ) : null}
            <div className="polish-editor">
              <label>
                <Text className="polish-editor-hint">直接修改本场结构；右侧预览会根据 YAML 内容即时更新。</Text>
                <div className="polish-yaml-editor-frame">
                  <div
                    className="polish-yaml-line-layer"
                    aria-hidden="true"
                    ref={yamlLineLayerRef}
                    style={{
                      '--polish-yaml-scrollbar-gutter': `${POLISH_YAML_SCROLLBAR_GUTTER_PX}px`,
                    } as CSSProperties}
                  >
                    {yamlLineNumbers.map((line) => (
                      <div className="polish-yaml-line-row" key={`line-${line.lineNumber}`}>
                        <span className="polish-yaml-line-number">{line.lineNumber}</span>
                        <span className="polish-yaml-line-mirror">{line.text || ' '}</span>
                      </div>
                    ))}
                  </div>
                  <TextArea
                    autoSize={false}
                    className="polish-yaml-editor"
                    value={draft.sceneYamlText}
                    onChange={(event) => updateDraft(updatePolishSceneYaml(draft, event.target.value))}
                    onScroll={handleYamlScroll}
                    spellCheck={POLISH_YAML_SPELL_CHECK}
                    wrap="soft"
                  />
                </div>
              </label>
            </div>
          </Card>

          <Card
            className="prototype-panel polish-work-panel polish-preview-panel"
            title={<PrototypePanelTitle code={workspaceLayout.right.code} title={workspaceLayout.right.title} />}
            bordered={false}
          >
            <div className="screenplay-preview polish-preview-scroll" ref={previewScrollRef}>
              <div className="screenplay-scene-meta">
                <Tag bordered={false}>{scene.sceneNumber}</Tag>
                <Text>第 {scene.chapterIndex} 章</Text>
                {scene.sceneIndexInChapter ? <Text>第 {scene.sceneIndexInChapter} 场</Text> : null}
              </div>
              <Title level={4}>{scene.title}</Title>

              <div className="screenplay-paper">
                <div className="sp-scene-heading">
                  <span>{scene.sceneNumber}</span>
                  {buildSceneHeadingText(draftScene.heading)}
                </div>

                {buildScriptBlockRows({ ...scene, scene: draftScene }).map((block) => {
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
            </div>
          </Card>
        </main>
      ) : null}
    </PrototypeFrame>
  )
}

export default ScreenplayPolishPage
