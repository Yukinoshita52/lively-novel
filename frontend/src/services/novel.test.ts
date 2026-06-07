import {
  buildLatestCompletedConversionUrl,
  buildNovelTitleUpdateUrl,
  buildScreenplaySceneUpdateUrl,
  buildScreenplayYamlUrl,
  getScreenplayConversionDetail,
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
  buildNovelTitleUpdateUrl('nv-1234abcd') === '/api/novel/nv-1234abcd/title',
  '小说标题更新 URL 应指向 novelId 对应接口',
)
assert(
  buildNovelTitleUpdateUrl('nv 1234/abcd') === '/api/novel/nv%201234%2Fabcd/title',
  '小说标题更新 URL 应编码 novelId',
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

function createDeferredJsonResponse() {
  let resolveJson: (value: unknown) => void = () => undefined
  const jsonPromise = new Promise<unknown>((resolve) => {
    resolveJson = resolve
  })

  return {
    response: {
      ok: true,
      json: () => jsonPromise,
    } as Response,
    resolveJson,
  }
}

async function testDedupesInFlightConversionDetailRequests() {
  const originalFetch = globalThis.fetch
  const deferred = createDeferredJsonResponse()
  let requestCount = 0

  globalThis.fetch = ((url: RequestInfo | URL) => {
    requestCount += 1
    assert(
      url === '/api/screenplay/conversions/cv-1234abcd',
      '转换详情请求应指向 conversionId 对应接口',
    )
    return Promise.resolve(deferred.response)
  }) as typeof fetch

  try {
    const firstRequest = getScreenplayConversionDetail('cv-1234abcd')
    const secondRequest = getScreenplayConversionDetail('cv-1234abcd')

    assert(requestCount === 1, '同一个 conversionId 的并发详情请求应只发起一次 fetch')

    deferred.resolveJson({
      code: 0,
      data: {
        conversionId: 'cv-1234abcd',
        novelId: 'nv-1234abcd',
        screenplayType: 'ANIME',
        status: 'COMPLETED',
        scenes: [],
      },
    })

    const firstDetail = await firstRequest
    const secondDetail = await secondRequest

    assert(firstDetail.conversionId === 'cv-1234abcd', '首个详情请求应返回转换详情')
    assert(secondDetail.conversionId === 'cv-1234abcd', '复用的详情请求应返回转换详情')
  } finally {
    globalThis.fetch = originalFetch
  }
}

await testDedupesInFlightConversionDetailRequests()
