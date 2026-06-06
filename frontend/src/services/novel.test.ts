import {
  buildLatestCompletedConversionUrl,
  buildScreenplaySceneUpdateUrl,
  buildScreenplayYamlUrl,
} from './novel.ts'

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
assert(
  buildLatestCompletedConversionUrl('nv-1234abcd', 'ANIME') ===
    '/api/screenplay/conversions/latest?novelId=nv-1234abcd&screenplayType=ANIME',
  '最近完成转换 URL 应按 novelId 和 screenplayType 查询',
)
assert(
  buildLatestCompletedConversionUrl('nv 1234/abcd', 'ANIME') ===
    '/api/screenplay/conversions/latest?novelId=nv%201234%2Fabcd&screenplayType=ANIME',
  '最近完成转换 URL 应编码 novelId',
)
assert(
  buildScreenplaySceneUpdateUrl('cv-1234abcd', 1, 2) ===
    '/api/screenplay/conversions/cv-1234abcd/chapters/1/scenes/2',
  '单场保存 URL 应指向 conversionId、chapterIndex 和 sceneIndexInChapter 对应的后端接口',
)
assert(
  buildScreenplaySceneUpdateUrl('cv 1234/abcd', 1, 2) ===
    '/api/screenplay/conversions/cv%201234%2Fabcd/chapters/1/scenes/2',
  '单场保存 URL 应编码 conversionId',
)
