import { useState } from 'react'
import ImportPage from './pages/ImportPage'
import ScreenplayConvertPage from './pages/ScreenplayConvertPage'
import ScreenplayExportPage from './pages/ScreenplayExportPage'
import ScreenplayPolishPage from './pages/ScreenplayPolishPage'
import ScreenplayPreviewPage from './pages/ScreenplayPreviewPage'
import SingleSceneConvertPage from './pages/SingleSceneConvertPage'
import type { AppPageKey } from './pages/appNavigation'
import { useScreenplayConversionSession } from './pages/conversionSession'
import type { PolishDraft } from './pages/screenplayPolish'
import type { ImportFlowContext, ScreenplayConvertContext } from './types/novel'
import './App.css'

function App() {
  const [page, setPage] = useState<AppPageKey>('import')
  const [singleSceneContext, setSingleSceneContext] = useState<ImportFlowContext | null>(null)
  const [convertContext, setConvertContext] = useState<ScreenplayConvertContext | null>(null)
  const [selectedSceneKey, setSelectedSceneKey] = useState<string>()
  const [polishDraftsBySceneKey, setPolishDraftsBySceneKey] = useState<Record<string, PolishDraft>>({})
  const conversionSession = useScreenplayConversionSession(convertContext)

  function backToImport() {
    setPage('import')
    setConvertContext(null)
    setSelectedSceneKey(undefined)
    setPolishDraftsBySceneKey({})
  }

  function updatePolishDraft(sceneKey: string, draft: PolishDraft) {
    setPolishDraftsBySceneKey((current) => ({
      ...current,
      [sceneKey]: draft,
    }))
  }

  if (page === 'single-scene' && singleSceneContext) {
    return (
      <SingleSceneConvertPage
        context={singleSceneContext}
        onBack={() => setPage('import')}
      />
    )
  }

  if (page === 'convert' && conversionSession) {
    return (
      <ScreenplayConvertPage
        session={conversionSession}
        onBack={backToImport}
        onPreview={() => setPage('preview')}
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
        onExport={() => setPage('export')}
      />
    )
  }

  if (page === 'export' && conversionSession) {
    return (
      <ScreenplayExportPage
        session={conversionSession}
        onBackToPolish={() => setPage('polish')}
      />
    )
  }

  return (
    <ImportPage
      onStartConvert={(nextContext) => {
        setConvertContext(nextContext)
        setPage('convert')
      }}
      onStartSingleScene={(nextContext) => {
        setSingleSceneContext(nextContext)
        setPage('single-scene')
      }}
    />
  )
}

export default App
