import { buildYamlLineNumbers } from '../polish/screenplayPolish.ts'
import {
  buildSceneOutlineItems,
  type GenerationQualityWarning,
} from '../preview/screenplayPreview.ts'
import type { GeneratedSceneSummary } from '../../types/novel.ts'

export const EXPORT_IGNORED_WARNING_STORAGE_PREFIX = 'lively-novel:export-ignored-warnings'

export interface ExportYamlDisplayContext {
  loading: boolean
  yamlText: string
}

export type ExportReadinessStatus = 'ready' | 'check' | 'blocked'

export interface ExportReadinessIssue extends GenerationQualityWarning {
  ignored: boolean
}

export interface ExportReadinessSummary {
  sceneCount: number
  issueCount: number
  activeIssueCount: number
  activeCheckCount: number
  activeBlockingCount: number
  ignoredCount: number
  status: ExportReadinessStatus
  statusLabel: string
  statusDescription: string
  issues: ExportReadinessIssue[]
}

export function resolveExportYamlDisplayText(context: ExportYamlDisplayContext) {
  if (context.loading) {
    return '正在读取 YAML...'
  }

  return context.yamlText || '暂无可导出的 YAML。'
}

export function buildExportYamlRows(yamlText: string) {
  return buildYamlLineNumbers(yamlText)
}

export function buildExportWarningStorageKey(conversionId?: string) {
  return `${EXPORT_IGNORED_WARNING_STORAGE_PREFIX}:${conversionId || 'current'}`
}

export function parseIgnoredWarningKeys(rawValue: string | null | undefined): string[] {
  if (!rawValue) {
    return []
  }

  try {
    const parsed = JSON.parse(rawValue) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }

    return Array.from(new Set(parsed.filter((value): value is string => typeof value === 'string' && value.trim())))
  } catch {
    return []
  }
}

export function stringifyIgnoredWarningKeys(keys: string[]) {
  return JSON.stringify(Array.from(new Set(keys)).sort())
}

export function toggleIgnoredWarningKey(keys: string[], warningKey: string) {
  if (keys.includes(warningKey)) {
    return keys.filter((key) => key !== warningKey)
  }

  return [...keys, warningKey]
}

function compareExportReadinessIssues(left: ExportReadinessIssue, right: ExportReadinessIssue) {
  if (left.ignored !== right.ignored) {
    return left.ignored ? 1 : -1
  }

  if (left.severity !== right.severity) {
    return left.severity === 'blocking' ? -1 : 1
  }

  if (left.chapterIndex !== right.chapterIndex) {
    return left.chapterIndex - right.chapterIndex
  }

  return (left.sceneIndexInChapter ?? 0) - (right.sceneIndexInChapter ?? 0)
}

function resolveExportReadinessStatus(activeBlockingCount: number, activeCheckCount: number): ExportReadinessStatus {
  if (activeBlockingCount > 0) {
    return 'blocked'
  }

  if (activeCheckCount > 0) {
    return 'check'
  }

  return 'ready'
}

function resolveExportReadinessStatusCopy(
  status: ExportReadinessStatus,
  activeIssueCount: number,
  ignoredCount: number,
) {
  if (status === 'blocked') {
    return {
      statusLabel: '建议处理后再导出',
      statusDescription: `还有 ${activeIssueCount} 项需要回看，其中包含影响剧本可用性的阻断项。`,
    }
  }

  if (status === 'check') {
    return {
      statusLabel: `建议回看 ${activeIssueCount} 项`,
      statusDescription: '这些提示不一定是错误，但导出前最好确认一遍。',
    }
  }

  return {
    statusLabel: '可以导出',
    statusDescription: ignoredCount > 0
      ? `当前没有待处理提示，已忽略 ${ignoredCount} 项已确认内容。`
      : '当前没有待处理提示。',
  }
}

export function buildExportReadinessSummary(
  scenes: GeneratedSceneSummary[],
  ignoredWarningKeys: string[] = [],
): ExportReadinessSummary {
  const ignoredWarningKeySet = new Set(ignoredWarningKeys)
  const issues = buildSceneOutlineItems(scenes)
    .flatMap((scene) => scene.warnings)
    .map((warning) => ({
      ...warning,
      ignored: ignoredWarningKeySet.has(warning.key),
    }))
    .sort(compareExportReadinessIssues)
  const activeIssues = issues.filter((issue) => !issue.ignored)
  const activeBlockingCount = activeIssues.filter((issue) => issue.severity === 'blocking').length
  const activeCheckCount = activeIssues.filter((issue) => issue.severity === 'check').length
  const ignoredCount = issues.length - activeIssues.length
  const status = resolveExportReadinessStatus(activeBlockingCount, activeCheckCount)
  const statusCopy = resolveExportReadinessStatusCopy(status, activeIssues.length, ignoredCount)

  return {
    sceneCount: scenes.length,
    issueCount: issues.length,
    activeIssueCount: activeIssues.length,
    activeCheckCount,
    activeBlockingCount,
    ignoredCount,
    status,
    ...statusCopy,
    issues,
  }
}
