import { buildScreenplayYamlUrl } from './novel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  buildScreenplayYamlUrl('cv-1234abcd') === '/api/screenplay/conversions/cv-1234abcd/yaml',
  'YAML 导出 URL 应指向 conversionId 对应的后端接口',
)
assert(
  buildScreenplayYamlUrl('cv 1234/abcd') === '/api/screenplay/conversions/cv%201234%2Fabcd/yaml',
  'YAML 导出 URL 应编码 conversionId',
)
