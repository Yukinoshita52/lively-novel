import { buildYamlLineNumbers } from './screenplayPolish.ts'

export interface ExportYamlDisplayContext {
  loading: boolean
  yamlText: string
}

export function resolveExportYamlDisplayText(context: ExportYamlDisplayContext) {
  if (context.loading) {
    return '正在读取 YAML...'
  }

  return context.yamlText || '暂无可导出的 YAML。'
}

export function buildExportYamlLineNumbers(yamlText: string) {
  return buildYamlLineNumbers(yamlText)
}

export function buildExportYamlRows(yamlText: string) {
  return buildYamlLineNumbers(yamlText)
}
