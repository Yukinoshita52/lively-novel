import type {
  ApiResponse,
  NovelChapterDetail,
  NovelChaptersResult,
  NovelListResult,
  NovelParseResult,
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

export async function parseNovel(title: string, text: string) {
  const response = await fetch('/api/novel/parse', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ title, text }),
  })

  return unwrapResponse<NovelParseResult>(response)
}

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

export async function getNovelList() {
  const response = await fetch('/api/novel')
  return unwrapResponse<NovelListResult>(response)
}

export async function getNovelChapterDetail(novelId: string, chapterIndex: number) {
  const response = await fetch(`/api/novel/${novelId}/chapters/${chapterIndex}`)
  return unwrapResponse<NovelChapterDetail>(response)
}

export async function convertSingleScene(text: string) {
  const response = await fetch('/api/screenplay/convert-single', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      text,
      screenplayType: 'ANIME',
    }),
  })

  return unwrapResponse<SceneResult>(response)
}

export async function getScreenplayConversionDetail(conversionId: string) {
  const response = await fetch(`/api/screenplay/conversions/${encodeURIComponent(conversionId)}`)
  return unwrapResponse<ScreenplayConversionDetail>(response)
}

export function buildLatestCompletedConversionUrl(novelId: string, screenplayType: string) {
  return `/api/screenplay/conversions/latest?novelId=${encodeURIComponent(novelId)}&screenplayType=${encodeURIComponent(screenplayType)}`
}

export async function getLatestCompletedConversion(novelId: string, screenplayType: string) {
  const response = await fetch(buildLatestCompletedConversionUrl(novelId, screenplayType))
  return unwrapResponse<ScreenplayConversionDetail>(response)
}

export function buildScreenplaySceneUpdateUrl(
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

export function buildScreenplayYamlUrl(conversionId: string) {
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
