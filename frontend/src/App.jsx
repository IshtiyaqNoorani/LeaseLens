import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import BackendStatus from './components/BackendStatus.jsx'
import { MODULES } from './navigation.js'
import PlannedModule from './pages/PlannedModule.jsx'

export default function App() {
  return (
    <div className="flex h-full">
      <aside className="flex w-60 shrink-0 flex-col bg-navy-900 text-slate-200">
        <div className="px-5 py-5">
          <div className="text-lg font-semibold tracking-tight text-white">LeaseLens</div>
          <div className="text-xs text-slate-400">Rental portfolio analytics</div>
        </div>
        <nav className="flex-1 space-y-0.5 px-3" aria-label="Modules">
          {MODULES.map((m) => (
            <NavLink
              key={m.path}
              to={m.path}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm ${isActive ? 'bg-navy-700 text-white' : 'text-slate-300 hover:bg-navy-700/60'}`
              }
            >
              {m.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 text-[11px] leading-relaxed text-slate-500">
          CIA 3 · Advanced Algorithm
          <br />
          Synthetic data, for analysis only
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-line bg-surface px-8 py-3">
          <div className="text-sm text-muted">No dataset loaded</div>
          <BackendStatus />
        </header>
        <main className="flex-1 overflow-auto px-8 py-6">
          <Routes>
            <Route path="/" element={<Navigate to="/overview" replace />} />
            {MODULES.map((m) => (
              <Route key={m.path} path={m.path} element={<PlannedModule module={m} />} />
            ))}
            <Route path="*" element={<Navigate to="/overview" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}
