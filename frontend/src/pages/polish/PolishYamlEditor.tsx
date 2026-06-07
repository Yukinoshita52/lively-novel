import type { CSSProperties, RefObject, UIEvent } from 'react'
import { Alert, Button, Card, Input, Typography } from 'antd'
import { SaveOutlined, StopOutlined } from '@ant-design/icons'
import { PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import {
  POLISH_YAML_SCROLLBAR_GUTTER_PX,
  POLISH_YAML_SPELL_CHECK,
  type YamlLineNumber,
} from './screenplayPolish'

const { Text } = Typography
const { TextArea } = Input

type PolishYamlEditorProps = {
  panelCode: string
  panelTitle: string
  yamlText: string
  lineNumbers: YamlLineNumber[]
  lineLayerRef: RefObject<HTMLDivElement | null>
  saving: boolean
  saveDisabled: boolean
  saveError: string | null
  onChange: (yamlText: string) => void
  onScroll: (event: UIEvent<HTMLTextAreaElement>) => void
  onCancel: () => void
  onSave: () => void
}

function PolishYamlEditor({
  panelCode,
  panelTitle,
  yamlText,
  lineNumbers,
  lineLayerRef,
  saving,
  saveDisabled,
  saveError,
  onChange,
  onScroll,
  onCancel,
  onSave,
}: PolishYamlEditorProps) {
  return (
    <Card
      className="prototype-panel polish-work-panel polish-yaml-panel"
      title={
        <div className="polish-panel-heading">
          <PrototypePanelTitle code={panelCode} title={panelTitle} />
          <div className="polish-heading-actions">
            <Button icon={<StopOutlined />} onClick={onCancel}>
              取消
            </Button>
            <Button
              disabled={saveDisabled}
              icon={<SaveOutlined />}
              loading={saving}
              onClick={onSave}
              type="primary"
            >
              保存
            </Button>
          </div>
        </div>
      }
      variant="borderless"
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
              ref={lineLayerRef}
              style={{
                '--polish-yaml-scrollbar-gutter': `${POLISH_YAML_SCROLLBAR_GUTTER_PX}px`,
              } as CSSProperties}
            >
              {lineNumbers.map((line) => (
                <div className="polish-yaml-line-row" key={`line-${line.lineNumber}`}>
                  <span className="polish-yaml-line-number">{line.lineNumber}</span>
                  <span className="polish-yaml-line-mirror">{line.text || ' '}</span>
                </div>
              ))}
            </div>
            <TextArea
              autoSize={false}
              className="polish-yaml-editor"
              value={yamlText}
              onChange={(event) => onChange(event.target.value)}
              onScroll={onScroll}
              spellCheck={POLISH_YAML_SPELL_CHECK}
              wrap="soft"
            />
          </div>
        </label>
      </div>
    </Card>
  )
}

export default PolishYamlEditor
