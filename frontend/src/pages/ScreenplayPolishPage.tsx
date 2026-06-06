import { useMemo, useState } from 'react'
import { Button, Card, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownOutlined, ExportOutlined, LeftOutlined, RightOutlined, UpOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from './conversionSession'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from './PrototypeFrame'
import {
  buildSceneHeadingText,
  buildSceneOutlineItems,
  getSceneKey,
  getSourcePreview,
  resolveAdjacentSceneKeys,
  resolveSelectedScene,
} from './screenplayPreview'

const { Text, Title } = Typography

type ScreenplayPolishPageProps = {
  session: ConversionSessionState
  selectedSceneKey?: string
  onSelectScene: (sceneKey: string) => void
  onBackToPreview: () => void
  onExport: () => void
}

function toYamlLines(value: unknown, indent = 0): string[] {
  const prefix = ' '.repeat(indent)
  if (Array.isArray(value)) {
    return value.flatMap((item) => {
      if (typeof item === 'object' && item !== null) {
        return [`${prefix}-`, ...toYamlLines(item, indent + 2)]
      }

      return [`${prefix}- ${String(item)}`]
    })
  }

  if (typeof value === 'object' && value !== null) {
    return Object.entries(value as Record<string, unknown>).flatMap(([key, item]) => {
      if (Array.isArray(item) || (typeof item === 'object' && item !== null)) {
        return [`${prefix}${key}:`, ...toYamlLines(item, indent + 2)]
      }

      return [`${prefix}${key}: ${String(item)}`]
    })
  }

  return [`${prefix}${String(value)}`]
}

function ScreenplayPolishPage({
  session,
  selectedSceneKey,
  onSelectScene,
  onBackToPreview,
  onExport,
}: ScreenplayPolishPageProps) {
  const [scenePickerExpanded, setScenePickerExpanded] = useState(false)
  const scene = useMemo(
    () => resolveSelectedScene(session.generatedScenes, selectedSceneKey),
    [selectedSceneKey, session.generatedScenes],
  )
  const sceneOutlineItems = useMemo(() => buildSceneOutlineItems(session.generatedScenes), [session.generatedScenes])
  const sceneYaml = useMemo(() => (scene ? toYamlLines(scene.scene).join('\n') : ''), [scene])
  const adjacentSceneKeys = useMemo(
    () => resolveAdjacentSceneKeys(session.generatedScenes, scene?.key),
    [scene?.key, session.generatedScenes],
  )

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
      ) : (
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
            title={<PrototypePanelTitle code="SOURCE" title="小说原文 · 本场" meta={`CH${scene.chapterIndex}`} />}
            bordered={false}
          >
            <div className="source-preview-text">
              {getSourcePreview(scene.scene.sourceText || '', true)}
            </div>
          </Card>

          <Card
            className="prototype-panel"
            title={<PrototypePanelTitle code="YAML" title="本场结构" meta="可检查字段" />}
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
                  {buildSceneHeadingText(scene.scene.heading)}
                </div>

                {scene.scene.actionLines.map((line, index) => (
                  <p className="sp-action-line" key={`${getSceneKey(scene)}-action-${index}`}>
                    {line}
                  </p>
                ))}

                {scene.scene.dialogueBlocks.map((dialogue, index) => (
                  <div className="sp-dialogue-block" key={`${getSceneKey(scene)}-dialogue-${index}`}>
                    <div className="sp-character">{dialogue.character}</div>
                    {dialogue.parenthetical ? <div className="sp-parenthetical">{dialogue.parenthetical}</div> : null}
                    <div className="sp-line">{dialogue.line}</div>
                  </div>
                ))}

                {scene.scene.transitions.map((transition, index) => (
                  <p className="sp-transition" key={`${getSceneKey(scene)}-transition-${index}`}>
                    {transition}
                  </p>
                ))}
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
      )}
    </PrototypeFrame>
  )
}

export default ScreenplayPolishPage
