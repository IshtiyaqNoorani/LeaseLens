import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import BackendStatus from './components/BackendStatus.jsx'
import { Badge } from './components/ui.jsx'
import { useDataset } from './context/DatasetContext.jsx'
import { date, titleCase } from './lib/format.js'
import { MODULES } from './navigation.js'
import CashFlow from './pages/CashFlow.jsx'
import DataWorkspace from './pages/DataWorkspace.jsx'
import Expenses from './pages/Expenses.jsx'
import Lab from './pages/Lab.jsx'
import Methodology from './pages/Methodology.jsx'
import Overview from './pages/Overview.jsx'
import Payments from './pages/Payments.jsx'
import Properties from './pages/Properties.jsx'
import PropertyDetail from './pages/PropertyDetail.jsx'

const PAGES = {
  '/data': DataWorkspace,
  '/overview': Overview,
  '/properties': Properties,
  '/cash-flow': CashFlow,
  '/payments': Payments,
  '/expenses': Expenses,
  '/lab': Lab,
  '/methodology': Methodology,
}

export default function App() {
  const { summary, error } = useDataset()
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
              {m.engine && <span className="block text-[11px] text-slate-400">{m.engine}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 text-[11px] leading-relaxed text-slate-500">
          CIA 3 · Advanced Algorithm
          <br />
          Raakin Majeed · Ishtiyaq Noorani
          <br />
          Synthetic data, for analysis only
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-line bg-surface px-8 py-3">
          <div className="flex items-center gap-2 text-sm text-muted">
            {summary ? (
              <>
                <Badge tone="navy">{titleCase(summary.source)} dataset</Badge>
                <span>
                  {date(summary.horizonStart)} to {date(summary.horizonEnd)}
                </span>
                <span className="text-ink">· As of {date(summary.analysisDate)}</span>
                {summary.adjustments > 0 && <Badge tone="warn">{summary.adjustments} cash-flow correction(s)</Badge>}
              </>
            ) : error?.status === 409 ? (
              <span>No dataset loaded. Go to Data Workspace.</span>
            ) : (
              <span>Loading dataset…</span>
            )}
          </div>
          <BackendStatus />
        </header>
        <main className="flex-1 overflow-auto px-8 py-6">
          <Routes>
            <Route path="/" element={<Navigate to="/overview" replace />} />
            {Object.entries(PAGES).map(([path, Page]) => (
              <Route key={path} path={path} element={<Page />} />
            ))}
            <Route path="/properties/:id" element={<PropertyDetail />} />
            <Route path="*" element={<Navigate to="/overview" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}
