import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { apiGet } from '../api/client.js'

const DatasetContext = createContext(null)

/**
 * Holds the current dataset summary (source, horizon, analysis date). `version` increases whenever
 * the dataset or analysis date changes, so screens re-fetch their results.
 */
export function DatasetProvider({ children }) {
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)

  const refresh = useCallback(async () => {
    try {
      const s = await apiGet('/datasets/current')
      setSummary(s)
      setError(null)
    } catch (e) {
      setSummary(null)
      setError(e)
    }
  }, [])

  const replace = useCallback((s) => {
    setSummary(s)
    setError(null)
    setVersion((v) => v + 1)
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return (
    <DatasetContext.Provider value={{ summary, error, version, refresh, replace }}>
      {children}
    </DatasetContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useDataset() {
  return useContext(DatasetContext)
}
