import type { NovelListItem } from '../../types/novel'

export type HistoryConversionActionKey = 'use' | 'resume' | 'preview' | 'export'

export interface HistoryConversionAction {
  key: HistoryConversionActionKey
  label: string
  enabled: boolean
}

export interface LatestConversionStatusSnapshot {
  status?: string | null
  updatedAt?: string | null
}

export interface WorkspaceSelectionState {
  selectedNovelId: string | null
}

export function selectWorkspaceNovel(state: WorkspaceSelectionState, novelId: string): WorkspaceSelectionState {
  return {
    ...state,
    selectedNovelId: novelId,
  }
}

export function buildHistoryConversionActions(item: NovelListItem): HistoryConversionAction[] {
  const actions: HistoryConversionAction[] = [
    {
      key: 'use',
      label: '使用这本',
      enabled: true,
    },
  ]

  if (!item.latestConversionId) {
    return actions
  }

  if (item.latestConversionStatus === 'COMPLETED') {
    actions.push(
      {
        key: 'preview',
        label: '查看剧本',
        enabled: true,
      },
      {
        key: 'export',
        label: '导出 YAML',
        enabled: true,
      },
    )
    return actions
  }

  if (item.latestConversionStatus === 'FAILED' || item.latestConversionStatus === 'RUNNING') {
    actions.push({
      key: 'resume',
      label: '继续转换',
      enabled: true,
    })
  }

  return actions
}

export function buildLatestConversionStatusLabel(conversion: LatestConversionStatusSnapshot | null | undefined) {
  if (!conversion?.status) {
    return '未转换'
  }

  switch (conversion.status) {
    case 'RUNNING':
      return '转换中'
    case 'FAILED':
      return '转换失败'
    case 'COMPLETED':
      return '已完成'
    default:
      return '状态未知'
  }
}
