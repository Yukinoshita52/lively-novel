import { useMemo, useState } from 'react'
import { message as antdMessage } from 'antd'
import ImportPage from './pages/ImportPage'
import ScreenplayConvertPage from './pages/ScreenplayConvertPage'
import ScreenplayExportPage from './pages/ScreenplayExportPage'
import ScreenplayPolishPage from './pages/ScreenplayPolishPage'
import ScreenplayPreviewPage from './pages/ScreenplayPreviewPage'
import type { AppPageKey } from './pages/appNavigation'
import { useScreenplayConversionSession } from './pages/conversionSession'
import { resolveFlowStepNavigation, resumeConvertPage } from './pages/appNavigation'
import type { PolishDraft } from './pages/screenplayPolish'
import type { ScreenplayConvertContext } from './types/novel'
import type { FlowStepKey } from './pages/prototypeFlow'
import './App.css'

function App() {
  const [page, setPage] = useState<AppPageKey>('import')
  const [convertContext, setConvertContext] = useState<ScreenplayConvertContext | null>(null)
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [polishDraftsBySceneKey, setPolishDraftsBySceneKey] = useState<Record<string, PolishDraft>>({})
  const conversionSession = useScreenplayConversionSession(convertContext)
  const flowNavigation = useMemo(
    () => resolveFlowStepNavigation(
      {
        page,
        singleSceneContext: null,
        convertContext,
        selectedSceneKey,
      },
      {
        hasGeneratedScenes: Boolean(conversionSession?.generatedScenes.length),
        completed: Boolean(conversionSession?.completed),
      },
    ),
    [conversionSession?.completed, conversionSession?.generatedScenes.length, convertContext, page, selectedSceneKey],
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
      singleSceneContext: null,
      convertContext,
      selectedSceneKey,
    })
    setConvertContext(nextState.convertContext)
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
      const firstScene = conversionSession.generatedScenes[0]
      setSelectedSceneKey(`${firstScene.chapterIndex}-${firstScene.sceneIndexInChapter ?? firstScene.title}`)
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
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  if (page === 'preview' && conversionSession) {
    return (
      <ScreenplayPreviewPage
        session={conversionSession}
        onBackToConvert={() => setPage('convert')}
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
        onBackToPolish={() => setPage('polish')}
        flowNavigation={flowNavigation}
        onNavigateStep={handleNavigateStep}
      />
    )
  }

  return (
    <ImportPage
      onStartConvert={(nextContext) => {
        setConvertContext(nextContext)
        setPage('convert')
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
