import type {
  ApiResponse,
  NovelChaptersResult,
  NovelListResult,
  NovelUploadResult,
  SceneResult,
  ScreenplayConversionDetail,
} from '../types/novel'

async function unwrapResponse<T>(response: Response): Promise<T> {
  const payload = (await response.json()) as ApiResponse<T>

  if (!response.ok || payload.code !== 0 || !payload.data) {
    throw new Error(payload.message || '请求失败')
  }

  return payload.data
}

const conversionDetailRequests = new Map<string, Promise<ScreenplayConversionDetail>>()

export async function uploadNovel(title: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)

  if (title.trim()) {
    formData.append('title', title.trim())
  }

  const response = await fetch('/api/novel/upload', {
    method: 'POST',
    body: formData,
  })

  return unwrapResponse<NovelUploadResult>(response)
}

export async function getNovelChapters(novelId: string) {
  const response = await fetch(`/api/novel/${novelId}/chapters`)
  return unwrapResponse<NovelChaptersResult>(response)
}

function buildNovelTitleUpdateUrl(novelId: string) {
  return `/api/novel/${encodeURIComponent(novelId)}/title`
}

export async function updateNovelTitle(novelId: string, title: string) {
  const response = await fetch(buildNovelTitleUpdateUrl(novelId), {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ title }),
  })

  return unwrapResponse<NovelChaptersResult>(response)
}

export async function getNovelList() {
  const response = await fetch('/api/novel')
  return unwrapResponse<NovelListResult>(response)
}

export async function getLatestScreenplayConversion(novelId: string, screenplayType: string) {
  const response = await fetch(
    `/api/screenplay/conversions/latest?novelId=${encodeURIComponent(novelId)}&screenplayType=${encodeURIComponent(screenplayType)}`,
  )
  return unwrapResponse<ScreenplayConversionDetail>(response)
}

export async function getScreenplayConversionDetail(conversionId: string) {
  const requestKey = conversionId.trim()
  const inFlightRequest = conversionDetailRequests.get(requestKey)

  if (inFlightRequest) {
    return inFlightRequest
  }

  const request = fetch(`/api/screenplay/conversions/${encodeURIComponent(requestKey)}`)
    .then((response) => unwrapResponse<ScreenplayConversionDetail>(response))
    .finally(() => {
      conversionDetailRequests.delete(requestKey)
    })

  conversionDetailRequests.set(requestKey, request)
  return request
}

function buildScreenplaySceneUpdateUrl(
  conversionId: string,
  chapterIndex: number,
  sceneIndexInChapter: number,
) {
  return `/api/screenplay/conversions/${encodeURIComponent(conversionId)}/chapters/${chapterIndex}/scenes/${sceneIndexInChapter}`
}

export async function updateScreenplayScene(
  conversionId: string,
  chapterIndex: number,
  sceneIndexInChapter: number,
  scene: SceneResult,
) {
  const response = await fetch(buildScreenplaySceneUpdateUrl(conversionId, chapterIndex, sceneIndexInChapter), {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(scene),
  })

  return unwrapResponse<SceneResult>(response)
}

function buildScreenplayYamlUrl(conversionId: string) {
  return `/api/screenplay/conversions/${encodeURIComponent(conversionId)}/yaml`
}

export async function getScreenplayConversionYaml(conversionId: string) {
  const response = await fetch(buildScreenplayYamlUrl(conversionId))

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || '导出 YAML 失败')
  }

  return response.blob()
}
