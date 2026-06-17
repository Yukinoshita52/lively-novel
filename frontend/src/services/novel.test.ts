import {
  getLatestScreenplayConversion,
  getScreenplayConversionDetail,
  getScreenplayConversionYaml,
  updateNovelTitle,
  updateScreenplayScene,
} from './novel.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

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

async function testGetsLatestScreenplayConversionWithEncodedQuery() {
  const originalFetch = globalThis.fetch
  let requestedUrl: RequestInfo | URL | undefined

  globalThis.fetch = ((url: RequestInfo | URL) => {
    requestedUrl = url
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        code: 0,
        data: {
          conversionId: 'cv-failed',
          novelId: 'nv 1234/abcd',
          screenplayType: 'ANIME',
          status: 'FAILED',
          updatedAt: '2026-06-17T09:15:30Z',
          scenes: [],
        },
      }),
    } as Response)
  }) as typeof fetch

  try {
    const result = await getLatestScreenplayConversion('nv 1234/abcd', 'ANIME')

    assert(
      requestedUrl === '/api/screenplay/conversions/latest?novelId=nv%201234%2Fabcd&screenplayType=ANIME',
      '最近转换会话请求应编码 novelId 和 screenplayType',
    )
    assert(result.conversionId === 'cv-failed', '最近转换会话应返回后端详情')
    assert(result.status === 'FAILED', '最近转换会话应允许失败状态')
    assert(result.updatedAt === '2026-06-17T09:15:30Z', '最近转换会话应包含更新时间')
  } finally {
    globalThis.fetch = originalFetch
  }
}

async function testUpdatesNovelTitleWithEncodedUrl() {
  const originalFetch = globalThis.fetch
  let requestedUrl: RequestInfo | URL | undefined
  let requestedInit: RequestInit | undefined

  globalThis.fetch = ((url: RequestInfo | URL, init?: RequestInit) => {
    requestedUrl = url
    requestedInit = init
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        code: 0,
        data: {
          novelId: 'nv 1234/abcd',
          title: '新标题',
          totalChapters: 3,
          chapters: [],
        },
      }),
    } as Response)
  }) as typeof fetch

  try {
    const result = await updateNovelTitle('nv 1234/abcd', '新标题')

    assert(requestedUrl === '/api/novel/nv%201234%2Fabcd/title', '小说标题更新应编码 novelId')
    assert(requestedInit?.method === 'PUT', '小说标题更新应使用 PUT')
    assert(requestedInit?.body === JSON.stringify({ title: '新标题' }), '小说标题更新应提交标题 JSON')
    assert(result.title === '新标题', '小说标题更新应返回后端数据')
  } finally {
    globalThis.fetch = originalFetch
  }
}

async function testUpdatesScreenplaySceneWithEncodedUrl() {
  const originalFetch = globalThis.fetch
  let requestedUrl: RequestInfo | URL | undefined
  let requestedInit: RequestInit | undefined
  const scene = {
    sceneId: 's1',
    heading: {
      interior: true,
      location: '教室',
      timeOfDay: '午后',
    },
    scriptBlocks: [{ type: 'ACTION' as const, text: '林秋走进教室。' }],
    sourceChapter: 1,
    sourceText: '原文片段',
  }

  globalThis.fetch = ((url: RequestInfo | URL, init?: RequestInit) => {
    requestedUrl = url
    requestedInit = init
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        code: 0,
        data: scene,
      }),
    } as Response)
  }) as typeof fetch

  try {
    const result = await updateScreenplayScene('cv 1234/abcd', 1, 2, scene)

    assert(
      requestedUrl === '/api/screenplay/conversions/cv%201234%2Fabcd/chapters/1/scenes/2',
      '单场保存应编码 conversionId 并包含章节与场次',
    )
    assert(requestedInit?.method === 'PUT', '单场保存应使用 PUT')
    assert(requestedInit?.body === JSON.stringify(scene), '单场保存应提交场景 JSON')
    assert(result.sceneId === 's1', '单场保存应返回后端场景')
  } finally {
    globalThis.fetch = originalFetch
  }
}

async function testGetsScreenplayYamlWithEncodedUrl() {
  const originalFetch = globalThis.fetch
  let requestedUrl: RequestInfo | URL | undefined
  const yamlBlob = new Blob(['schemaVersion: "1.0"'], { type: 'text/yaml' })

  globalThis.fetch = ((url: RequestInfo | URL) => {
    requestedUrl = url
    return Promise.resolve({
      ok: true,
      blob: () => Promise.resolve(yamlBlob),
    } as Response)
  }) as typeof fetch

  try {
    const result = await getScreenplayConversionYaml('cv 1234/abcd')

    assert(requestedUrl === '/api/screenplay/conversions/cv%201234%2Fabcd/yaml', 'YAML 导出应编码 conversionId')
    assert(result === yamlBlob, 'YAML 导出应返回后端 Blob')
  } finally {
    globalThis.fetch = originalFetch
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

await testGetsLatestScreenplayConversionWithEncodedQuery()
await testUpdatesNovelTitleWithEncodedUrl()
await testUpdatesScreenplaySceneWithEncodedUrl()
await testGetsScreenplayYamlWithEncodedUrl()
await testDedupesInFlightConversionDetailRequests()
