import { useEffect, useState } from 'react'
import { apiGet } from '../api/client.js'

/** Shows whether the Java backend is reachable, so a broken setup is obvious during the demo. */
export default function BackendStatus() {
  const [state, setState] = useState({ kind: 'checking' })

  useEffect(() => {
    const controller = new AbortController()
    apiGet('/health', { signal: controller.signal })
      .then((h) => setState({ kind: 'up', version: h.version }))
      .catch((e) => {
        if (e.name !== 'AbortError') setState({ kind: 'down', message: e.message })
      })
    return () => controller.abort()
  }, [])

  const styles = {
    checking: 'bg-slate-100 text-slate-600',
    up: 'bg-emerald-50 text-gain',
    down: 'bg-red-50 text-loss',
  }
  const label = {
    checking: 'Checking backend…',
    up: `Java API connected · v${state.version}`,
    down: 'Java API unreachable: start the backend (see README)',
  }
  return (
    <span className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium ${styles[state.kind]}`}>
      <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />
      {label[state.kind]}
    </span>
  )
}
