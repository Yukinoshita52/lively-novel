import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Progress, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import type {
  ConvertEventItem,
  GeneratedSceneSummary,
  SceneHeading,
  ScreenplayConvertContext,
} from '../types/novel'

const { Text, Title } = Typography

type ScreenplayConvertPageProps = {
  context: ScreenplayConvertContext
  onBack: () => void
}

type SsePayload = Record<string, unknown>

function buildHeadingText(heading?: SceneHeading) {
  if (!heading) {
    return '场景信息待生成'
  }

  const prefix = heading.interior ? '内景' : '外景'
  return `${prefix} - ${heading.location || '未知地点'} - ${heading.timeOfDay || '未知时间'}`
}

function toJsonPayload(raw: string): SsePayload {
  try {
    return JSON.parse(raw) as SsePayload
  } catch {
    return {}
  }
}

function findEventDelimiter(buffer: string) {
  const lfIndex = buffer.indexOf('\n\n')
  const crlfIndex = buffer.indexOf('\r\n\r\n')

  if (lfIndex === -1) {
    return crlfIndex
  }

  if (crlfIndex === -1) {
    return lfIndex
  }

  return Math.min(lfIndex, crlfIndex)
}

function delimiterLengthAt(buffer: string, index: number) {
  return buffer.startsWith('\r\n\r\n', index) ? 4 : 2
}

function ScreenplayConvertPage({ context, onBack }: ScreenplayConvertPageProps) {
  const [connecting, setConnecting] = useState(true)
  const [events, setEvents] = useState<ConvertEventItem[]>([])
  const [generatedScenes, setGeneratedScenes] = useState<GeneratedSceneSummary[]>([])
  const [completed, setCompleted] = useState(false)
  const [convertError, setConvertError] = useState<string | null>(null)

  useEffect(() => {
    const abortController = new AbortController()

    function pushEvent(type: ConvertEventItem['type'], message: string) {
      setEvents((current) => [...current, { type, message }])
    }

    function handleEventBlock(block: string) {
      const lines = block
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)

      const eventName = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
      const dataLines = lines
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())

      if (!eventName || dataLines.length === 0) {
        return
      }

      const payload = toJsonPayload(dataLines.join('\n'))

      if (eventName === 'started') {
        const totalChapters = Number(payload.totalChapters ?? context.totalChapters)
        pushEvent('started', `开始转换，共 ${totalChapters} 章`)
        return
      }

      if (eventName === 'chapter_loaded') {
        pushEvent(
          'chapter_loaded',
          `已读取第 ${payload.chapterIndex ?? '?'} 章：${String(payload.title ?? '未命名章节')}`,
        )
        return
      }

      if (eventName === 'scene_generated') {
        pushEvent(
          'scene_generated',
          `已生成第 ${payload.chapterIndex ?? '?'} 章场景：${String(payload.title ?? '未命名章节')}`,
        )
        setGeneratedScenes((current) => [
          ...current,
          {
            chapterIndex: Number(payload.chapterIndex ?? 0),
            title: String(payload.title ?? '未命名章节'),
            sceneId: typeof payload.sceneId === 'string' ? payload.sceneId : undefined,
            heading: payload.heading as SceneHeading | undefined,
          },
        ])
        return
      }

      if (eventName === 'completed') {
        pushEvent('completed', `整本转换完成，共处理 ${payload.totalChapters ?? context.totalChapters} 章`)
        setCompleted(true)
      }
    }

    async function startConvert() {
      try {
        const response = await fetch('/api/screenplay/convert', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            novelId: context.novelId,
            screenplayType: context.screenplayType,
          }),
          signal: abortController.signal,
        })

        if (!response.ok || !response.body) {
          throw new Error('启动转换失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            break
          }

          buffer += decoder.decode(value, { stream: true })

          let delimiterIndex = findEventDelimiter(buffer)
          while (delimiterIndex >= 0) {
            const block = buffer.slice(0, delimiterIndex)
            buffer = buffer.slice(delimiterIndex + delimiterLengthAt(buffer, delimiterIndex))
            handleEventBlock(block)
            delimiterIndex = findEventDelimiter(buffer)
          }
        }

        if (buffer.trim()) {
          handleEventBlock(buffer)
        }
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        const message = error instanceof Error ? error.message : '整本转换失败'
        setConvertError(message)
        pushEvent('error', message)
      } finally {
        setConnecting(false)
      }
    }

    void startConvert()

    return () => {
      abortController.abort()
    }
  }, [context.novelId, context.screenplayType, context.totalChapters])

  const finishedCount = generatedScenes.length
  const progressPercent = useMemo(() => {
    if (context.totalChapters <= 0) {
      return 0
    }
    return Math.min(100, Math.round((finishedCount / context.totalChapters) * 100))
  }, [context.totalChapters, finishedCount])

  return (
    <div className="convert-shell">
      <header className="convert-topbar">
        <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
          返回导入
        </Button>
        <div>
          <Title level={1}>{context.title}</Title>
        </div>
      </header>

      <main className="convert-grid convert-progress-grid">
        <Card className="panel" bordered={false}>
          <div className="panel-header compact">
            <div>
              <Text className="panel-kicker">PROGRESS</Text>
              <Title level={3}>转换进度</Title>
            </div>
            <Tag bordered={false}>{context.screenplayType}</Tag>
          </div>

          <div className="progress-summary">
            <div className="progress-status">
              <Title level={4}>{completed ? '已完成' : connecting ? '连接中' : '转换中'}</Title>
              <Text className="progress-ratio">{finishedCount} / {context.totalChapters} 章</Text>
            </div>
            <Progress percent={progressPercent} strokeColor="#9c4b2e" />
            <Text>{context.chapters.length} 章已进入待转换列表</Text>
          </div>

          {convertError ? (
            <Alert
              className="feedback-block"
              message="转换失败"
              description={convertError}
              type="error"
              showIcon
            />
          ) : null}
        </Card>

        <Card className="panel" bordered={false}>
          <div className="panel-header compact">
            <div>
              <Text className="panel-kicker">EVENTS</Text>
              <Title level={3}>事件流</Title>
            </div>
          </div>

          <div className="event-list">
            {events.length === 0 ? (
              <div className="event-row">
                <Text>等待服务端返回事件…</Text>
              </div>
            ) : (
              events.map((event, index) => (
                <div className="event-row" key={`${event.type}-${index}`}>
                  <Text className="panel-kicker">{event.type}</Text>
                  <Text>{event.message}</Text>
                </div>
              ))
            )}
          </div>
        </Card>

        <Card className="panel" bordered={false}>
          <div className="panel-header compact">
            <div>
              <Text className="panel-kicker">SCENES</Text>
              <Title level={3}>生成摘要</Title>
            </div>
          </div>

          <div className="scene-summary-list">
            {generatedScenes.length === 0 ? (
              <div className="scene-summary-row">
                <Text>场景结果生成后会显示在这里。</Text>
              </div>
            ) : (
              generatedScenes.map((scene) => (
                <div className="scene-summary-row" key={`${scene.chapterIndex}-${scene.sceneId ?? scene.title}`}>
                  <Text className="chapter-index">CH {scene.chapterIndex}</Text>
                  <Title level={5}>{scene.title}</Title>
                  <Text className="scene-summary-heading">{buildHeadingText(scene.heading)}</Text>
                  {scene.sceneId ? <Tag bordered={false}>{scene.sceneId}</Tag> : null}
                </div>
              ))
            )}
          </div>
        </Card>
      </main>
    </div>
  )
}

export default ScreenplayConvertPage
