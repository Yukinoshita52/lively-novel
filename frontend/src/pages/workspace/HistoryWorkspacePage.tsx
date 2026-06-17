import { useEffect, useState } from 'react'
import { Alert, Button, Card, Typography } from 'antd'
import { ReloadOutlined, UploadOutlined } from '@ant-design/icons'
import { getNovelChapters, getNovelList, getScreenplayConversionDetail } from '../../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import type { FlowStepNavigation } from '../appNavigation'
import { createRestoredConversionContextFromDetail } from '../conversionSession'
import { formatHistoryTime, formatWordCount } from '../import/importFormat'
import type { NovelListItem, ScreenplayConvertContext } from '../../types/novel'
import {
  buildHistoryConversionActions,
  buildLatestConversionStatusLabel,
  selectWorkspaceNovel,
  type HistoryConversionActionKey,
} from './historyWorkspaceModel'

const { Text, Title } = Typography

type HistoryWorkspacePageProps = {
  onImportNew: () => void
  onUseNovel: (context: ScreenplayConvertContext) => void
  onOpenHistoryConversion: (context: ScreenplayConvertContext, targetPage: 'convert' | 'preview' | 'export') => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function HistoryWorkspacePage({
  onImportNew,
  onUseNovel,
  onOpenHistoryConversion,
  flowNavigation,
  onNavigateStep,
}: HistoryWorkspacePageProps) {
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState<NovelListItem[]>([])
  const [selectedNovelId, setSelectedNovelId] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function loadHistory() {
    if (loading) {
      return
    }

    setLoading(true)
    setErrorMessage(null)
    try {
      const result = await getNovelList()
      setItems(result.novels)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '历史加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadHistory()
    // 初次进入工作台时加载一次；刷新按钮负责后续重载。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function buildBaseContext(item: NovelListItem): Promise<ScreenplayConvertContext> {
    const chaptersResult = await getNovelChapters(item.novelId)
    return {
      novelId: chaptersResult.novelId,
      title: chaptersResult.title,
      totalChapters: chaptersResult.totalChapters,
      chapters: chaptersResult.chapters,
      screenplayType: item.latestConversionType ?? 'ANIME',
    }
  }

  async function handleAction(item: NovelListItem, action: HistoryConversionActionKey) {
    setErrorMessage(null)
    setSelectedNovelId((current) => selectWorkspaceNovel({ selectedNovelId: current }, item.novelId).selectedNovelId)

    try {
      const baseContext = await buildBaseContext(item)

      if (action === 'use') {
        onUseNovel(baseContext)
        return
      }

      const detail = item.latestConversionId
        ? await getScreenplayConversionDetail(item.latestConversionId)
        : null
      const restoredConversionMode: ScreenplayConvertContext['restoredConversionMode'] =
        action === 'resume' ? 'stream' : 'static'
      const context = detail
        ? {
          ...createRestoredConversionContextFromDetail(baseContext, detail),
          restoredConversionMode,
        }
        : baseContext

      if (action === 'resume') {
        onOpenHistoryConversion(context, 'convert')
        return
      }

      if (action === 'preview') {
        onOpenHistoryConversion(context, 'preview')
        return
      }

      if (action === 'export') {
        onOpenHistoryConversion(context, 'export')
      }
    } catch (error) {
      setSelectedNovelId(null)
      setErrorMessage(error instanceof Error ? error.message : '加载历史作品失败')
    }
  }

  return (
    <PrototypeFrame
      currentStep="workspace"
      maxWidth={1280}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow="00 · 历史作品"
        title="作品工作台"
        meta="管理已导入小说和转换结果"
        action={
          <Button className="prototype-ghost-button" icon={<UploadOutlined />} onClick={onImportNew}>
            上传新小说
          </Button>
        }
      />

      <main className="workspace-grid">
        <Card
          className="prototype-panel workspace-panel"
          title={<PrototypePanelTitle code="WORKS" title="历史作品" meta={`${items.length} 本小说`} />}
          extra={(
            <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void loadHistory()}>
              刷新
            </Button>
          )}
          variant="borderless"
        >
          {errorMessage ? (
            <Alert className="feedback-block" message="历史加载失败" description={errorMessage} type="error" showIcon />
          ) : null}

          {items.length === 0 && !loading ? (
            <div className="workspace-empty">
              <Text>还没有历史作品</Text>
              <Button type="primary" icon={<UploadOutlined />} onClick={onImportNew}>
                上传 .txt
              </Button>
            </div>
          ) : (
            <div className="workspace-list">
              {items.map((item) => {
                const selected = selectedNovelId === item.novelId
                const latestConversionLabel = buildLatestConversionStatusLabel({
                  status: item.latestConversionStatus,
                  updatedAt: item.latestConversionUpdatedAt,
                })
                const historyActions = buildHistoryConversionActions(item)

                return (
                  <div className={`workspace-row${selected ? ' selected' : ''}`} key={item.novelId}>
                    <div className="workspace-copy">
                      <Title level={5}>{item.title}</Title>
                      <div className="workspace-meta">
                        <Text>{item.totalChapters} 章</Text>
                        <Text>{formatWordCount(item.totalWordCount)}</Text>
                        <Text>导入 {formatHistoryTime(item.createdAt)}</Text>
                      </div>
                      <div className="workspace-status-line">
                        <Text className={`workspace-status status-${item.latestConversionStatus ?? 'NONE'}`}>
                          {latestConversionLabel}
                        </Text>
                        {item.latestConversionType ? (
                          <Text>{item.latestConversionType}</Text>
                        ) : null}
                        {item.latestConversionUpdatedAt ? (
                          <Text>{formatHistoryTime(item.latestConversionUpdatedAt)}</Text>
                        ) : null}
                      </div>
                    </div>
                    <div className="workspace-actions">
                      {historyActions.map((action) => (
                        <Button
                          key={action.key}
                          type={action.key === 'use' && selected ? 'primary' : 'default'}
                          disabled={!action.enabled}
                          onClick={() => void handleAction(item, action.key)}
                        >
                          {action.key === 'use' && selected ? '已选择' : action.label}
                        </Button>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </Card>
      </main>
    </PrototypeFrame>
  )
}

export default HistoryWorkspacePage
