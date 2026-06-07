import { Card, Tag, Typography } from 'antd'
import { PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import { formatWordCount } from './importFormat'
import type { DisplayResult } from './importPageTypes'

const { Text, Title } = Typography

type ImportChapterPreviewProps = {
  result: DisplayResult
}

function ImportChapterPreview({ result }: ImportChapterPreviewProps) {
  return (
    <Card className="chapter-card prototype-inner-panel" variant="borderless">
      <div className="chapter-card-head">
        <div>
          <PrototypePanelTitle code="RESULT" title={result.title || '未命名作品'} />
        </div>
        <div className="chapter-summary">
          <span>{result.totalChapters} 章</span>
          <span>{formatWordCount(result.totalWordCount)}</span>
        </div>
      </div>

      <div className="chapter-list">
        {result.chapters.map((chapter) => (
          <div className="chapter-row" key={chapter.chapterIndex}>
            <div className="chapter-copy">
              <Text className="chapter-index">CH {chapter.chapterIndex}</Text>
              <Title level={5}>{chapter.title}</Title>
              {chapter.preview ? (
                <Text className="chapter-preview">{chapter.preview}</Text>
              ) : null}
            </div>
            <Tag variant="filled">{formatWordCount(chapter.wordCount)}</Tag>
          </div>
        ))}
      </div>
    </Card>
  )
}

export default ImportChapterPreview
