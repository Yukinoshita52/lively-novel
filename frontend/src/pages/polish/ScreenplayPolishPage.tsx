import { useEffect, useMemo, useRef, useState, type UIEvent } from 'react'
import { Button, Card, message as antdMessage, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from '../conversionSession'
import { updateScreenplayScene } from '../../services/novel'
import { PrototypeFrame, PrototypeHero } from '../../components/prototype/PrototypeFrame'
import type { FlowStepNavigation } from '../appNavigation'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import {
  buildSceneHeadingText,
  buildSceneOutlineItems,
  resolveAdjacentSceneKeys,
  resolveSelectedScene,
} from '../preview/screenplayPreview'
import {
  buildYamlLineNumbers,
  buildPolishYamlLineNumberTransform,
  buildPolishWorkspaceLayout,
  createPolishDraft,
  resetPolishDraft,
  updatePolishSceneYaml,
  type PolishDraft,
} from './screenplayPolish'
import PolishRenderedPreview from './PolishRenderedPreview'
import PolishScenePicker from './PolishScenePicker'
import PolishYamlEditor from './PolishYamlEditor'

const { Text } = Typography

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
        <Card className="prototype-panel" variant="borderless">
          <div className="screenplay-empty">
            <Text>请先从预览页选择一个场景。</Text>
          </div>
        </Card>
      ) : draft && draftScene ? (
        <main className="polish-grid">
          <PolishScenePicker
            scenes={sceneOutlineItems}
            selectedScene={scene}
            expanded={scenePickerExpanded}
            previousKey={adjacentSceneKeys.previousKey}
            nextKey={adjacentSceneKeys.nextKey}
            onToggleExpanded={() => setScenePickerExpanded((current) => !current)}
            onSelectScene={onSelectScene}
          />
          {scene.warnings.length > 0 ? (
            <div className="polish-warning-panel">
              {scene.warnings.map((warning) => (
                <div className={`scene-warning-row severity-${warning.severity}`} key={warning.key}>
                  <Text className="scene-warning-title">{warning.title}</Text>
                  <Text>{warning.message}</Text>
                </div>
              ))}
            </div>
          ) : null}

          <PolishYamlEditor
            panelCode={workspaceLayout.left.code}
            panelTitle={workspaceLayout.left.title}
            yamlText={draft.sceneYamlText}
            lineNumbers={yamlLineNumbers}
            lineLayerRef={yamlLineLayerRef}
            saving={savingScene}
            saveDisabled={!session.conversionId || !scene.sceneIndexInChapter}
            saveError={saveError}
            onChange={(sceneYamlText) => updateDraft(updatePolishSceneYaml(draft, sceneYamlText))}
            onScroll={handleYamlScroll}
            onCancel={handleCancelDraft}
            onSave={() => void handleSaveDraft()}
          />

          <PolishRenderedPreview
            panelCode={workspaceLayout.right.code}
            panelTitle={workspaceLayout.right.title}
            scene={scene}
            draftScene={draftScene}
            scrollRef={previewScrollRef}
          />
        </main>
      ) : null}
    </PrototypeFrame>
  )
}

export default ScreenplayPolishPage
