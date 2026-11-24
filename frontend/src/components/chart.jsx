import { useState } from 'react'
import { DataTable } from './ui.jsx'

/** Tooltip: value first (strong), series name second, keyed by a short line of the series colour. */
export function ChartTooltip({ active, payload, label, formatLabel, formatValue }) {
  if (!active || !payload || payload.length === 0) return null
  return (
    <div className="rounded-md border border-line bg-white px-3 py-2 text-xs shadow-sm">
      {label !== undefined && <div className="mb-1 text-muted">{formatLabel ? formatLabel(label, payload) : label}</div>}
      {payload.map((p) => (
        <div key={p.dataKey ?? p.name} className="flex items-center gap-2">
          <span className="inline-block h-0.5 w-3 rounded" style={{ background: p.color || p.payload?.fill }} />
          <span className="num font-semibold text-ink">{formatValue ? formatValue(p.value, p) : p.value}</span>
          <span className="text-muted">{p.name}</span>
        </div>
      ))}
    </div>
  )
}

/** Legend rendered as HTML: swatch shape mirrors the mark (line or bar). */
export function Legend({ items }) {
  if (!items || items.length < 2) return null
  return (
    <div className="mb-2 flex flex-wrap gap-4 text-xs text-muted">
      {items.map((i) => (
        <span key={i.label} className="inline-flex items-center gap-1.5">
          {i.shape === 'bar' ? (
            <span className="inline-block size-2.5 rounded-sm" style={{ background: i.color }} />
          ) : (
            <span className="inline-block h-0.5 w-4 rounded" style={{ background: i.color }} />
          )}
          {i.label}
        </span>
      ))}
    </div>
  )
}

/**
 * A chart with a "Table" toggle so every plotted value is reachable without hovering
 * (accessibility and the contrast relief rule).
 */
export function ChartCard({ title, subtitle, legend, table, children, height = 260, loading }) {
  const [asTable, setAsTable] = useState(false)
  return (
    <section className="rounded-lg border border-line bg-surface">
      <div className="flex items-start justify-between gap-3 border-b border-line px-5 py-3">
        <div>
          <h2 className="text-sm font-semibold text-ink">{title}</h2>
          {subtitle && <p className="mt-0.5 text-xs text-muted">{subtitle}</p>}
        </div>
        {table && (
          <div className="flex rounded-md border border-line text-xs" role="group" aria-label="View">
            <button type="button" className={`px-2 py-1 ${!asTable ? 'bg-navy-50 font-semibold text-navy-700' : 'text-muted'}`} onClick={() => setAsTable(false)}>
              Chart
            </button>
            <button type="button" className={`px-2 py-1 ${asTable ? 'bg-navy-50 font-semibold text-navy-700' : 'text-muted'}`} onClick={() => setAsTable(true)}>
              Table
            </button>
          </div>
        )}
      </div>
      <div className={`px-5 py-4 transition-opacity ${loading ? 'opacity-50' : ''}`}>
        {asTable && table ? (
          <DataTable columns={table.columns} rows={table.rows} maxHeight={height + 40} />
        ) : (
          <>
            <Legend items={legend} />
            <div style={{ height }}>{children}</div>
          </>
        )}
      </div>
    </section>
  )
}
