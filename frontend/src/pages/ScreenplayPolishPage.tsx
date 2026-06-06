import { useMemo, useState } from 'react'
import { Alert, Button, Card, Input, message as antdMessage, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownOutlined, ExportOutlined, LeftOutlined, RightOutlined, SaveOutlined, StopOutlined, UpOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import { updateScreenplayScene } from '../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import {
  buildSceneHeadingText,
  buildSceneOutlineItems,
  buildScriptBlockRows,
  resolveAdjacentSceneKeys,
  resolveSelectedScene,
} from './screenplayPreview'
import {
  buildPolishSceneYaml,
  createPolishDraft,
  resetPolishDraft,
  updateScriptBlocksText,
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
  onExport: () => void
}

function ScreenplayPolishPage({
  session,
  selectedSceneKey,
  draftsBySceneKey,
  onUpdateDraft,
  onSelectScene,
  onBackToPreview,
  onExport,
}: ScreenplayPolishPageProps) {
  const [scenePickerExpanded, setScenePickerExpanded] = useState(false)
  const [savingScene, setSavingScene] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
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
  const sceneYaml = useMemo(() => (draftScene ? buildPolishSceneYaml(draftScene) : ''), [draftScene])
  const adjacentSceneKeys = useMemo(
    () => resolveAdjacentSceneKeys(session.generatedScenes, scene?.key),
    [scene?.key, session.generatedScenes],
  )

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

  return (
    <PrototypeFrame currentStep="polish" maxWidth={1280}>
      <PrototypeHero
        eyebrow="05 · 逐场打磨"
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
            className="prototype-panel"
            title={<PrototypePanelTitle code="EDIT" title="本地打磨草稿" meta="即时预览" />}
            bordered={false}
          >
            {saveError ? (
              <Alert className="feedback-block" message="保存失败" description={saveError} type="error" showIcon />
            ) : null}
            <div className="polish-editor">
              <label>
                <Text className="thought-label">剧本正文块</Text>
                <Text className="polish-editor-hint">每行格式：ACTION|画面描述、DIALOGUE|角色|括号提示|台词、TRANSITION|转场</Text>
                <TextArea
                  autoSize={{ minRows: 8, maxRows: 14 }}
                  value={draft.scriptBlocksText}
                  onChange={(event) => updateDraft(updateScriptBlocksText(draft, event.target.value))}
                />
              </label>
            </div>
            <div className="prototype-export-row">
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
          </Card>

          <Card
            className="prototype-panel"
            title={<PrototypePanelTitle code="YAML" title="本场结构" meta="本地草稿" />}
            bordered={false}
          >
            <pre className="yaml-preview polish-yaml">{sceneYaml}</pre>
          </Card>

          <Card
            className="prototype-panel scene-preview-panel"
            title={<PrototypePanelTitle code="PREVIEW" title="渲染预览" />}
            bordered={false}
          >
            <div className="screenplay-preview">
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

          <Card className="prototype-panel polish-action-panel" bordered={false}>
            <div className="prototype-export-row">
              <Text>完成检查后进入最终导出页。</Text>
              <Button icon={<ExportOutlined />} onClick={onExport} type="primary">
                进入导出
              </Button>
            </div>
          </Card>
        </main>
      ) : null}
    </PrototypeFrame>
  )
}

export default ScreenplayPolishPage
