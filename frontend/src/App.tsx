import { useMemo, useState } from 'react'
import { message as antdMessage } from 'antd'
import ImportPage from './pages/import/ImportPage'
import ScreenplayConvertPage from './pages/convert/ScreenplayConvertPage'
import ScreenplayExportPage from './pages/export/ScreenplayExportPage'
import ScreenplayPolishPage from './pages/polish/ScreenplayPolishPage'
import ScreenplayPreviewPage from './pages/preview/ScreenplayPreviewPage'
import type { AppPageKey } from './pages/appNavigation'
import { useScreenplayConversionSession } from './pages/conversionSession'
import {
  enterConvertPageForHistoryReplay,
  enterPolishPageWithFallback,
  resolveFlowStepNavigation,
  retryConvertPage,
  resumeConvertPage,
} from './pages/appNavigation'
import type { PolishDraft } from './pages/polish/screenplayPolish'
import type { ScreenplayConvertContext } from './types/novel'
import type { FlowStepKey } from './components/prototype/prototypeFlow'
import { getSceneKey } from './pages/preview/screenplayPreview'
import './styles/app.css'
import './pages/import/import.css'
import './pages/import/importType.css'
import './pages/convert/convert.css'
import './pages/preview/preview.css'
import './components/prototype/prototype.css'
import './pages/polish/polish.css'
import './pages/export/export.css'
import './styles/responsive.css'

function App() {
  const [page, setPage] = useState<AppPageKey>('import')
  const [convertContext, setConvertContext] = useState<ScreenplayConvertContext | null>(null)
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [polishDraftsBySceneKey, setPolishDraftsBySceneKey] = useState<Record<string, PolishDraft>>({})
  const conversionSession = useScreenplayConversionSession(convertContext)
  const hasSceneAccess = Boolean(conversionSession?.generatedScenes.length)
  const flowNavigation = useMemo(
    () => resolveFlowStepNavigation(
      {
        page,
        convertContext,
        selectedSceneKey,
      },
      {
        hasGeneratedScenes: hasSceneAccess,
        completed: Boolean(conversionSession?.completed),
      },
    ),
    [conversionSession?.completed, convertContext, hasSceneAccess, page, selectedSceneKey],
  )

  function backToImport() {
    setPage('import')
    setSelectedSceneKey(undefined)
    setPolishDraftsBySceneKey({})
  }

  function updatePolishDraft(sceneKey: string, draft: PolishDraft) {
    setPolishDraftsBySceneKey((current) => ({
      ...current,
      [sceneKey]: draft,
    }))
  }

  function resumeConversion() {
    const nextState = resumeConvertPage({
      page,
      convertContext,
      selectedSceneKey,
    })
    setConvertContext(nextState.convertContext)
    setPage(nextState.page)
  }

  function retryConversion() {
    const nextState = retryConvertPage({
      page,
      convertContext,
      selectedSceneKey,
    })
    setConvertContext(nextState.convertContext)
    setSelectedSceneKey(nextState.selectedSceneKey)
    setPolishDraftsBySceneKey({})
    setPage(nextState.page)
  }

  function openConvertPage() {
    const nextState = enterConvertPageForHistoryReplay({
      page,
      convertContext,
      selectedSceneKey,
    })
    setConvertContext(nextState.convertContext)
    setPage(nextState.page)
  }

  function openPolishPage() {
    const firstScene = conversionSession?.generatedScenes[0]
    const nextState = enterPolishPageWithFallback(
      {
        page,
        convertContext,
        selectedSceneKey,
      },
      firstScene ? getSceneKey(firstScene) : undefined,
    )
    setSelectedSceneKey(nextState.selectedSceneKey)
    setPage(nextState.page)
  }

  function handleNavigateStep(step: FlowStepKey) {
    const navigation = flowNavigation[step]
    if (!navigation.enabled) {
      if (navigation.message) {
        void antdMessage.warning(navigation.message)
      }
      return
    }

    if (step === 'import') {
      backToImport()
      return
    }

    if (step === 'polish' && !selectedSceneKey && conversionSession?.generatedScenes[0]) {
      openPolishPage()
      return
    }

    if (step === 'convert') {
      openConvertPage()
      return
    }

    setPage(navigation.target)
  }

  if (page === 'convert' && conversionSession) {
    return (
      <ScreenplayConvertPage
        session={conversionSession}
        onBack={backToImport}
        onPreview={() => setPage('preview')}
        onResume={resumeConversion}
        onRetry={retryConversion}
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  if (page === 'preview' && conversionSession) {
    return (
      <ScreenplayPreviewPage
        session={conversionSession}
        onBackToConvert={openConvertPage}
        onPolishScene={(sceneKey) => {
          setSelectedSceneKey(sceneKey)
          setPage('polish')
        }}
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  if (page === 'polish' && conversionSession) {
    return (
      <ScreenplayPolishPage
        session={conversionSession}
        selectedSceneKey={selectedSceneKey}
        draftsBySceneKey={polishDraftsBySceneKey}
        onUpdateDraft={updatePolishDraft}
        onSelectScene={setSelectedSceneKey}
        onBackToPreview={() => setPage('preview')}
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  if (page === 'export' && conversionSession) {
    return (
      <ScreenplayExportPage
        session={conversionSession}
        onBackToPolish={openPolishPage}
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  return (
    <ImportPage
      onStartConvert={(nextContext) => {
        setConvertContext(nextContext)
        setSelectedSceneKey(undefined)
        setPage('convert')
      }}
      onOpenHistoryConversion={(nextContext, targetPage) => {
        setConvertContext(nextContext)
        setSelectedSceneKey(undefined)
        setPage(targetPage)
      }}
      onTitleUpdated={(nextContext) => {
        setConvertContext((current) => (
          current?.novelId === nextContext.novelId ? { ...current, title: nextContext.title } : current
        ))
      }}
      restoreContext={convertContext}
      flowNavigation={flowNavigation}
      onNavigateStep={handleNavigateStep}
    />
  )
}

export default App
