import { Button, Card, Typography } from 'antd'
import type { NovelListItem } from '../../types/novel'
import { formatHistoryTime, formatWordCount } from './importFormat'
import { buildLatestConversionStatusLabel } from './importPageModel'

const { Text, Title } = Typography

type ImportHistoryPanelProps = {
  visible: boolean
  loading: boolean
  items: NovelListItem[]
  selectedNovelId: string | null
  onToggle: () => void
  onRefresh: () => void
  onUseHistory: (novelId: string) => void
}

function ImportHistoryPanel({
  visible,
  loading,
  items,
  selectedNovelId,
  onToggle,
  onRefresh,
  onUseHistory,
}: ImportHistoryPanelProps) {
  return (
    <>
      <div className="history-toolbar">
        <Button
          size="large"
          onClick={onToggle}
          loading={loading}
        >
          从历史中选择已导入小说
        </Button>
        {visible ? (
          <Button
            type="link"
            onClick={onRefresh}
            loading={loading}
          >
            刷新历史
          </Button>
        ) : null}
      </div>

      {visible ? (
        <Card className="history-card" variant="borderless">
          {items.length === 0 && !loading ? (
            <Text className="history-empty">还没有已导入的小说</Text>
          ) : (
            <div className="history-list">
              {items.map((item) => {
                const selected = selectedNovelId === item.novelId
                const latestConversionLabel = buildLatestConversionStatusLabel({
                  status: item.latestConversionStatus,
                  updatedAt: item.latestConversionUpdatedAt,
                })

                return (
                  <div className={`history-row${selected ? ' selected' : ''}`} key={item.novelId}>
                    <div className="history-copy">
                      <Title level={5}>{item.title}</Title>
                      <Text>{item.totalChapters} 章 · {formatWordCount(item.totalWordCount)}</Text>
                      <Text>{formatHistoryTime(item.createdAt)}</Text>
                      <div className="history-status-line">
                        <Text className={`history-status status-${item.latestConversionStatus ?? 'NONE'}`}>
                          {latestConversionLabel}
                        </Text>
                        {item.latestConversionUpdatedAt ? (
                          <Text className="history-status-time">
                            {formatHistoryTime(item.latestConversionUpdatedAt)}
                          </Text>
                        ) : null}
                      </div>
                    </div>
                    <Button type={selected ? 'primary' : 'default'} onClick={() => onUseHistory(item.novelId)}>
                      {selected ? '已选择' : '使用这本'}
                    </Button>
                  </div>
                )
              })}
            </div>
          )}
        </Card>
      ) : null}
    </>
  )
}

export default ImportHistoryPanel
