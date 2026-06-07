export function formatWordCount(wordCount: number) {
  if (wordCount >= 10000) {
    return `${(wordCount / 10000).toFixed(1).replace(/\.0$/, '')} 万字`
  }
  return `${wordCount} 字`
}

export function formatHistoryTime(isoTime: string | null) {
  if (!isoTime) {
    return '时间未知'
  }

  const date = new Date(isoTime)
  if (Number.isNaN(date.getTime())) {
    return '时间未知'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
