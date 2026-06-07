import type { RefObject } from 'react'
import { Card, Tag, Typography } from 'antd'
import { PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { SceneResult } from '../../types/novel'
import {
  buildSceneHeadingText,
  buildScriptBlockRows,
  type SceneOutlineItem,
} from '../preview/screenplayPreview'

const { Text, Title } = Typography

type PolishRenderedPreviewProps = {
  panelCode: string
  panelTitle: string
  scene: SceneOutlineItem
  draftScene: SceneResult
  scrollRef: RefObject<HTMLDivElement | null>
}

function PolishRenderedPreview({
  panelCode,
  panelTitle,
  scene,
  draftScene,
  scrollRef,
}: PolishRenderedPreviewProps) {
  return (
    <Card
      className="prototype-panel polish-work-panel polish-preview-panel"
      title={<PrototypePanelTitle code={panelCode} title={panelTitle} />}
      variant="borderless"
    >
      <div className="screenplay-preview polish-preview-scroll" ref={scrollRef}>
        <div className="screenplay-scene-meta">
          <Tag variant="filled">{scene.sceneNumber}</Tag>
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
  )
}

export default PolishRenderedPreview
