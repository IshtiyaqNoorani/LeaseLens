// Shared presentational components. They display values; they never compute analytics.

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-xl font-semibold tracking-tight text-ink">{title}</h1>
        {subtitle && <p className="mt-1 max-w-3xl text-sm text-muted">{subtitle}</p>}
      </div>
      {children}
    </div>
  )
}

export function Card({ title, subtitle, actions, children, className = '' }) {
  return (
    <section className={`rounded-lg border border-line bg-surface ${className}`}>
      {(title || actions) && (
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-line px-5 py-3">
          <div>
            {title && <h2 className="text-sm font-semibold text-ink">{title}</h2>}
            {subtitle && <p className="mt-0.5 text-xs text-muted">{subtitle}</p>}
          </div>
          {actions}
        </div>
      )}
      <div className="p-5">{children}</div>
    </section>
  )
}

/** Stat tile: label, value, optional note. */
export function Stat({ label, value, note, tone }) {
  const toneClass = tone === 'loss' ? 'text-loss' : tone === 'gain' ? 'text-gain' : 'text-ink'
  return (
    <div className="rounded-lg border border-line bg-surface px-4 py-3">
      <div className="text-xs font-medium text-muted">{label}</div>
      <div className={`mt-1 text-xl font-semibold ${toneClass}`}>{value}</div>
      {note && <div className="mt-0.5 text-xs text-muted">{note}</div>}
    </div>
  )
}

/** The single headline number of a view. */
export function Hero({ label, value, note }) {
  return (
    <div className="rounded-lg border border-line bg-surface px-5 py-4">
      <div className="text-xs font-medium uppercase tracking-wide text-muted">{label}</div>
      <div className="mt-1 whitespace-nowrap text-4xl font-semibold tracking-tight text-ink 2xl:text-5xl">{value}</div>
      {note && <div className="mt-2 max-w-md text-xs text-muted">{note}</div>}
    </div>
  )
}

export function Badge({ children, tone = 'neutral' }) {
  const tones = {
    neutral: 'bg-slate-100 text-slate-700',
    navy: 'bg-navy-50 text-navy-700',
    warn: 'bg-amber-50 text-amber-800',
    loss: 'bg-red-50 text-loss',
    gain: 'bg-emerald-50 text-gain',
  }
  return <span className={`inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium ${tones[tone]}`}>{children}</span>
}

export function Button({ children, variant = 'primary', className = '', ...props }) {
  const variants = {
    primary: 'bg-navy-700 text-white hover:bg-navy-600 disabled:bg-slate-300',
    secondary: 'border border-line bg-white text-ink hover:bg-slate-50 disabled:text-slate-400',
    danger: 'border border-red-200 bg-white text-loss hover:bg-red-50',
  }
  return (
    <button
      type="button"
      className={`inline-flex items-center justify-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}

export function Field({ label, hint, children }) {
  return (
    <label className="flex flex-col gap-1 text-xs font-medium text-muted">
      {label}
      {children}
      {hint && <span className="font-normal text-muted">{hint}</span>}
    </label>
  )
}

const inputClass =
  'rounded-md border border-line bg-white px-2.5 py-1.5 text-sm text-ink outline-none focus:border-navy-600 focus:ring-2 focus:ring-navy-100'

export function Input({ className = '', ...props }) {
  return <input className={`${inputClass} ${className}`} {...props} />
}

export function Select({ children, className = '', ...props }) {
  return (
    <select className={`${inputClass} ${className}`} {...props}>
      {children}
    </select>
  )
}

/** Shows an API problem (RFC 7807) in plain language. */
export function ProblemBanner({ error, onRetry }) {
  if (!error) return null
  const p = error.problem || {}
  return (
    <div role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-loss">
      <div className="font-semibold">{p.title || 'Something went wrong'}</div>
      <div className="mt-0.5 text-red-900">{error.message}</div>
      {onRetry && (
        <button type="button" className="mt-2 text-xs font-semibold underline" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}

export function Note({ children, tone = 'info' }) {
  const tones = { info: 'border-navy-100 bg-navy-50 text-navy-900', warn: 'border-amber-200 bg-amber-50 text-amber-900' }
  return <div className={`rounded-md border px-3 py-2 text-xs leading-relaxed ${tones[tone]}`}>{children}</div>
}

export function Empty({ children }) {
  return <div className="py-8 text-center text-sm text-muted">{children}</div>
}

/**
 * Simple data table. `columns`: [{ key, label, render?, align? }]. Optional client-side sort on
 * display only (no analytics), via `sortable`.
 */
export function DataTable({ columns, rows, rowKey, onRowClick, maxHeight, emptyText = 'No rows.' }) {
  if (!rows || rows.length === 0) return <Empty>{emptyText}</Empty>
  return (
    <div className="overflow-auto" style={maxHeight ? { maxHeight } : undefined}>
      <table className="w-full border-collapse text-sm">
        <thead className="sticky top-0 bg-surface">
          <tr className="border-b border-line">
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={`whitespace-nowrap px-3 py-2 text-xs font-semibold text-muted ${c.align === 'right' ? 'text-right' : 'text-left'}`}
              >
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr
              key={rowKey ? rowKey(row) : i}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={`border-b border-line/70 last:border-0 ${onRowClick ? 'cursor-pointer hover:bg-navy-50/60' : ''}`}
            >
              {columns.map((c) => (
                <td key={c.key} className={`whitespace-nowrap px-3 py-2 ${c.align === 'right' ? 'num text-right' : ''}`}>
                  {c.render ? c.render(row) : row[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** Collapsible explanation of how a figure is calculated. */
export function HowCalculated({ children }) {
  return (
    <details className="mt-3 text-xs text-muted">
      <summary className="cursor-pointer select-none font-medium text-navy-600">How this is calculated</summary>
      <div className="mt-2 space-y-1 leading-relaxed">{children}</div>
    </details>
  )
}
