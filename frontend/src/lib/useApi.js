import { useCallback, useEffect, useRef, useState } from 'react'
import { apiGet } from '../api/client.js'

/**
 * Fetches `path` whenever it changes. While a new request is in flight the previous data is kept
 * (so charts hold their frame instead of flashing). Pass null to skip fetching. `key` forces a
 * re-fetch when it changes (e.g. the dataset version).
 */
export function useApi(path, key) {
  const [state, setState] = useState({ data: null, error: null, loading: Boolean(path) })
  const [tick, setTick] = useState(0)
  const controller = useRef(null)

  useEffect(() => {
    if (!path) return undefined
    controller.current?.abort()
    const c = new AbortController()
    controller.current = c
    setState((s) => ({ ...s, loading: true }))
    apiGet(path, { signal: c.signal })
      .then((data) => setState({ data, error: null, loading: false }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState((s) => ({ data: s.data, error, loading: false }))
      })
    return () => c.abort()
  }, [path, tick, key])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  return { ...state, reload }
}
