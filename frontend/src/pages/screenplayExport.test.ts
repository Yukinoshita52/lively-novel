import {
  buildExportYamlRows,
  resolveExportYamlDisplayText,
} from './screenplayExport.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const exportYaml = [
  'scriptBlocks:',
  '  - type: "ACTION"',
  '    text: "温水用手帕擦额头上的汗，环视店内。周围没有穿同校制服的学生。他从书包里拿出一本文库本，封面是轻小说《跟年上的妹妹撒娇也可以吗？》最新卷。桌上摆\\',
  '      着自助饮料杯和一大盘薯条。"',
].join('\n')

const rows = buildExportYamlRows(exportYaml)
assert(rows.length === 4, '导出页 YAML 行号应按导出文本的真实逻辑行计算')
assert(rows[2].lineNumber === 3, '长文本首行应保留真实行号')
assert(rows[2].text.includes('温水用手帕'), '导出页每行应只渲染一次真实 YAML 文本')
assert(rows[3].lineNumber === 4, 'Jackson 折行后的续行应保留真实行号')
assert(resolveExportYamlDisplayText({ loading: true, yamlText: '' }) === '正在读取 YAML...', '读取中应显示加载文案')
assert(resolveExportYamlDisplayText({ loading: false, yamlText: '' }) === '暂无可导出的 YAML。', '空内容应显示占位文案')
assert(resolveExportYamlDisplayText({ loading: false, yamlText: exportYaml }) === exportYaml, '有内容时应原样显示导出 YAML')
