import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined, EyeInvisibleOutlined, EyeOutlined, UndoOutlined } from '@ant-design/icons'
import type { ConversionSessionState } from '../conversionSession'
import { getScreenplayConversionYaml } from '../../services/novel'
import { PrototypeFrame, PrototypeHero, PrototypePanelTitle } from '../../components/prototype/PrototypeFrame'
import type { FlowStepNavigation } from '../appNavigation'
import type { FlowStepKey } from '../../components/prototype/prototypeFlow'
import { buildYamlDownloadFileName } from '../preview/screenplayPreview'
import {
  buildExportReadinessSummary,
  buildExportWarningStorageKey,
  buildExportYamlRows,
  parseIgnoredWarningKeys,
  resolveExportYamlDisplayText,
  stringifyIgnoredWarningKeys,
  toggleIgnoredWarningKey,
} from './screenplayExport'
import { downloadBlob } from '../../utils/download'

const { Text } = Typography

type ScreenplayExportPageProps = {
  session: ConversionSessionState
  onBackToPolish: () => void
  onPolishScene: (sceneKey: string) => void
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

function ScreenplayExportPage({
  session,
  onBackToPolish,
  onPolishScene,
  flowNavigation,
  onNavigateStep,
}: ScreenplayExportPageProps) {
  const [persistedYamlText, setPersistedYamlText] = useState('')
  const [exportError, setExportError] = useState<string | null>(null)
  const [ignoredWarningKeys, setIgnoredWarningKeys] = useState<string[]>([])

  useEffect(() => {
    if (!session.conversionId) {
      setIgnoredWarningKeys([])
      return undefined
    }

    setIgnoredWarningKeys(
      parseIgnoredWarningKeys(window.localStorage.getItem(buildExportWarningStorageKey(session.conversionId))),
    )

    return undefined
  }, [session.conversionId])

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
  const readinessSummary = useMemo(
    () => buildExportReadinessSummary(session.generatedScenes, ignoredWarningKeys),
    [ignoredWarningKeys, session.generatedScenes],
  )

  function handleDownloadYaml() {
    if (!yamlText) {
      return
    }

    const yamlBlob = new Blob([yamlText], { type: 'text/yaml;charset=utf-8' })
    downloadBlob(yamlBlob, buildYamlDownloadFileName(session.context.title))
  }

  function persistIgnoredWarningKeys(nextKeys: string[]) {
    setIgnoredWarningKeys(nextKeys)
    if (!session.conversionId) {
      return
    }

    window.localStorage.setItem(buildExportWarningStorageKey(session.conversionId), stringifyIgnoredWarningKeys(nextKeys))
  }

  function handleToggleIgnoredWarning(warningKey: string) {
    persistIgnoredWarningKeys(toggleIgnoredWarningKey(ignoredWarningKeys, warningKey))
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
        className="prototype-panel export-check-panel"
        title={<PrototypePanelTitle code="CHECK" title="导出前检查" meta={readinessSummary.statusLabel} />}
        variant="borderless"
      >
        <div className={`export-readiness-banner status-${readinessSummary.status}`}>
          <div className="export-readiness-head">
            <div className="export-readiness-copy">
              <Text className="export-readiness-title">轻量质量摘要</Text>
              <Text className="export-readiness-description">{readinessSummary.statusDescription}</Text>
            </div>
            <div className="export-readiness-metrics" aria-label="导出前检查统计">
              <Tag>{`场景 ${readinessSummary.sceneCount}`}</Tag>
              <Tag color={readinessSummary.activeCheckCount > 0 ? 'gold' : 'green'}>
                {`需检查 ${readinessSummary.activeCheckCount}`}
              </Tag>
              <Tag color={readinessSummary.activeBlockingCount > 0 ? 'red' : 'default'}>
                {`阻断 ${readinessSummary.activeBlockingCount}`}
              </Tag>
              <Tag>{`已忽略 ${readinessSummary.ignoredCount}`}</Tag>
            </div>
          </div>

          {readinessSummary.issueCount > 0 ? (
            <div className="export-readiness-list">
              {readinessSummary.issues.map((warning) => (
                <div
                  className={`export-readiness-row severity-${warning.severity}${warning.ignored ? ' ignored' : ''}`}
                  key={warning.key}
                >
                  <div className="export-readiness-row-main">
                    <Text className="export-readiness-scene">{warning.sceneNumber}</Text>
                    <div className="export-readiness-row-copy">
                      <Text className="export-readiness-title-text">{warning.title}</Text>
                      <Text className="export-readiness-message">
                        {warning.ignored ? '已忽略' : warning.message}
                      </Text>
                    </div>
                  </div>
                  <div className="export-readiness-row-actions">
                    <Button icon={<EyeOutlined />} onClick={() => onPolishScene(warning.sceneKey)} size="small">
                      查看场景
                    </Button>
                    <Button
                      icon={warning.ignored ? <UndoOutlined /> : <EyeInvisibleOutlined />}
                      onClick={() => handleToggleIgnoredWarning(warning.key)}
                      size="small"
                    >
                      {warning.ignored ? '取消忽略' : '忽略'}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </Card>

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
        </div>
      </Card>
    </PrototypeFrame>
  )
}

export default ScreenplayExportPage
