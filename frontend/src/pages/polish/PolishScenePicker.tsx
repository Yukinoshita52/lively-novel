import { Button, Card, Typography } from 'antd'
import { DownOutlined, LeftOutlined, RightOutlined, UpOutlined } from '@ant-design/icons'
import { PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { SceneOutlineItem } from '../preview/screenplayPreview'
import type { PolishSceneStatus } from './screenplayPolish'

const { Text } = Typography

type PolishScenePickerProps = {
  scenes: SceneOutlineItem[]
  selectedScene: SceneOutlineItem
  sceneStatusByKey?: Record<string, PolishSceneStatus>
  expanded: boolean
  previousKey?: string
  nextKey?: string
  onToggleExpanded: () => void
  onSelectScene: (sceneKey: string) => void
}

function PolishScenePicker({
  scenes,
  selectedScene,
  sceneStatusByKey = {},
  expanded,
  previousKey,
  nextKey,
  onToggleExpanded,
  onSelectScene,
}: PolishScenePickerProps) {
  return (
    <Card
      className="prototype-panel polish-scene-picker"
      title={<PrototypePanelTitle code="SCENES" title="选择要打磨的场景" meta={`${scenes.length} 场`} />}
      variant="borderless"
    >
      <div className="polish-switch-row">
        <Button
          disabled={!previousKey}
          icon={<LeftOutlined />}
          onClick={() => {
            if (previousKey) {
              onSelectScene(previousKey)
            }
          }}
        >
          上一场景
        </Button>
        <div className="polish-current-scene">
          <Text>{selectedScene.sceneNumber}</Text>
          <Text>{selectedScene.title}</Text>
        </div>
        <Button
          disabled={!nextKey}
          icon={<RightOutlined />}
          onClick={() => {
            if (nextKey) {
              onSelectScene(nextKey)
            }
          }}
        >
          下一场景
        </Button>
        <Button
          icon={expanded ? <UpOutlined /> : <DownOutlined />}
          onClick={onToggleExpanded}
        >
          {expanded ? '收起场景列表' : '展开场景列表'}
        </Button>
      </div>
      {expanded ? (
        <div className="scene-outline polish-outline-row">
          {scenes.map((outlineScene) => (
            <button
              className={`scene-outline-row ${outlineScene.key === selectedScene.key ? 'active' : ''}`}
              key={outlineScene.key}
              onClick={() => onSelectScene(outlineScene.key)}
              type="button"
            >
              <span className="scene-outline-no">{outlineScene.sceneNumber}</span>
              <span className="scene-outline-copy">
                <span className="scene-outline-title">{outlineScene.title}</span>
                <span className="scene-outline-heading">{outlineScene.headingText}</span>
              </span>
              <span className="scene-outline-status">
                {sceneStatusByKey[outlineScene.key]?.changed ? (
                  <span className="scene-outline-state">已改</span>
                ) : null}
                {sceneStatusByKey[outlineScene.key]?.unsaved ? (
                  <span className="scene-outline-state unsaved">未存</span>
                ) : null}
                {outlineScene.warnings.length > 0 ? (
                  <span className="scene-outline-warning">{outlineScene.warnings.length} 项检查</span>
                ) : null}
              </span>
            </button>
          ))}
        </div>
      ) : null}
    </Card>
  )
}

export default PolishScenePicker
