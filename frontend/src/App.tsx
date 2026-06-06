import { useState } from 'react'
import ImportPage from './pages/ImportPage'
import SingleSceneConvertPage from './pages/SingleSceneConvertPage'
import type { ImportFlowContext } from './types/novel'
import './App.css'

function App() {
  const [page, setPage] = useState<'import' | 'single-scene'>('import')
  const [context, setContext] = useState<ImportFlowContext | null>(null)

  if (page === 'single-scene' && context) {
    return (
      <SingleSceneConvertPage
        context={context}
        onBack={() => setPage('import')}
      />
    )
  }

  return (
    <ImportPage
      onStartConvert={(nextContext) => {
        setContext(nextContext)
        setPage('single-scene')
      }}
    />
  )
}

export default App
