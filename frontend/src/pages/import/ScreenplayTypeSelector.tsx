import { Button, Card, Tag, Typography } from 'antd'
import { PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { ImportEntryActions, ScreenplayTypeCard } from './importPageModel'

const { Text } = Typography

type ScreenplayTypeSelectorProps = {
  types: ScreenplayTypeCard[]
  actions: ImportEntryActions
  onSelectType: (type: string) => void
  onStartConvert: () => void
}

function ScreenplayTypeSelector({
  types,
  actions,
  onSelectType,
  onStartConvert,
}: ScreenplayTypeSelectorProps) {
  return (
    <div className="side-column">
      <Card
        className="prototype-panel import-config-panel"
        title={<PrototypePanelTitle code="TYPE" title="剧本类型" />}
        variant="borderless"
      >
        <div className="type-grid">
          {types.map((type) => (
            <button
              key={type.code}
              className={`type-card${type.active ? ' active' : ''}${type.enabled ? '' : ' disabled'}`}
              type="button"
              onClick={() => type.enabled && onSelectType(type.code)}
              disabled={!type.enabled}
            >
              <span className="type-card-head">
                <span className="type-name">{type.name}</span>
                <Tag className="type-badge" variant="filled">
                  {type.badge}
                </Tag>
              </span>
              <Text className="type-desc">{type.description}</Text>
            </button>
          ))}
        </div>

        <div className="convert-action-panel">
          <Button
            block
            className="convert-button"
            size="large"
            type="primary"
            disabled={!actions.primary.enabled}
            onClick={onStartConvert}
          >
            {actions.primary.label}
          </Button>
        </div>
      </Card>
    </div>
  )
}

export default ScreenplayTypeSelector
