import { useEffect, useState } from 'react'
import { Alert, Button, Card, Typography } from 'antd'
import { ArrowLeftOutlined, CopyOutlined, DownloadOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from '../conversionSession'
import { getScreenplayConversionYaml } from '../../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { FlowStepNavigation } from '../appNavigation'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import { buildYamlDownloadFileName } from '../preview/screenplayPreview'
import { buildExportYamlRows, resolveExportYamlDisplayText } from './screenplayExport'
import { downloadBlob } from '../../utils/download'

const { Text } = Typography

type ScreenplayExportPageProps = {
  session: ConversionSessionState
  onBackToPolish: () => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayExportPage({
  session,
  onBackToPolish,
  flowNavigation,
  onNavigateStep,
}: ScreenplayExportPageProps) {
  const [persistedYamlText, setPersistedYamlText] = useState('')
  const [exportError, setExportError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!session.conversionId || !session.completed) {
      return undefined
    }

    let active = true

    getScreenplayConversionYaml(session.conversionId)
      .then((blob) => blob.text())
      .then((text) => {
        if (active) {
          setPersistedYamlText(text)
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setExportError(error instanceof Error ? error.message : '读取 YAML 失败')
        }
      })

    return () => {
      active = false
    }
  }, [session.completed, session.conversionId])
  const yamlText = persistedYamlText
  const loadingYaml = Boolean(session.completed && session.conversionId && !yamlText && !exportError)
  const yamlDisplayText = resolveExportYamlDisplayText({ loading: loadingYaml, yamlText })
  const yamlRows = buildExportYamlRows(yamlDisplayText)

  function handleDownloadYaml() {
    if (!yamlText) {
      return
    }

    const yamlBlob = new Blob([yamlText], { type: 'text/yaml;charset=utf-8' })
    downloadBlob(yamlBlob, buildYamlDownloadFileName(session.context.title))
  }

  async function handleCopyYaml() {
    if (!yamlText) {
      return
    }

    await navigator.clipboard.writeText(yamlText)
    setCopied(true)
  }

  return (
    <PrototypeFrame
      currentStep="export"
      maxWidth={1280}
      flowNavigation={flowNavigation}
      onNavigateStep={onNavigateStep}
    >
      <PrototypeHero
        eyebrow="05 · 导出"
        title="带走你的剧本初稿"
        meta="结构化 YAML"
        action={
          <Button className="prototype-ghost-button" icon={<ArrowLeftOutlined />} onClick={onBackToPolish}>
            返回打磨
          </Button>
        }
      />

      <Card
        className="prototype-panel scene-preview-panel"
        title={<PrototypePanelTitle code="YAML" title="结构化剧本" meta="核心交付" />}
        variant="borderless"
      >
        {!session.completed ? (
          <Alert
            className="feedback-block"
            message="转换仍在进行"
            description="完整 YAML 会在转换完成后生成。"
            type="info"
            showIcon
          />
        ) : null}
        {exportError ? (
          <Alert className="feedback-block" message="导出失败" description={exportError} type="error" showIcon />
        ) : null}
        <div className="yaml-preview export-yaml-viewer">
          {yamlRows.map((line) => (
            <div className="export-yaml-line-row" key={`export-yaml-${line.lineNumber}`}>
              <span className="export-yaml-line-number" aria-hidden="true">{line.lineNumber}</span>
              <span className="export-yaml-line-text">{line.text || ' '}</span>
            </div>
          ))}
        </div>

        <div className="prototype-export-row">
          <Button disabled={!yamlText} icon={<DownloadOutlined />} onClick={handleDownloadYaml} type="primary">
            下载 screenplay.yaml
          </Button>
          <Button disabled={!yamlText} icon={<CopyOutlined />} onClick={handleCopyYaml}>
            {copied ? '已复制' : '复制 YAML'}
          </Button>
          <Text>YAML 是本项目的结构化交付物，可继续编辑或导入其他工具。</Text>
        </div>
      </Card>
    </PrototypeFrame>
  )
}

export default ScreenplayExportPage
