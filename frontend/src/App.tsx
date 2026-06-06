import { useState } from 'react'
import ImportPage from './pages/ImportPage'
import ScreenplayConvertPage from './pages/ScreenplayConvertPage'
import SingleSceneConvertPage from './pages/SingleSceneConvertPage'
import type { ImportFlowContext, ScreenplayConvertContext } from './types/novel'
import './App.css'

function App() {
  const [page, setPage] = useState<'import' | 'single-scene' | 'convert'>('import')
  const [singleSceneContext, setSingleSceneContext] = useState<ImportFlowContext | null>(null)
  const [convertContext, setConvertContext] = useState<ScreenplayConvertContext | null>(null)

  if (page === 'single-scene' && singleSceneContext) {
    return (
      <SingleSceneConvertPage
        context={singleSceneContext}
        onBack={() => setPage('import')}
      />
    )
  }

  if (page === 'convert' && convertContext) {
    return (
      <ScreenplayConvertPage
        context={convertContext}
        onBack={() => setPage('import')}
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
