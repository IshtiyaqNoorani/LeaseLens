import { addDays, addMonths } from '../lib/format.js'
import { useDataset } from '../context/DatasetContext.jsx'
import { Field, Input } from './ui.jsx'

/**
 * Date range control with presets relative to the analysis date. Ranges never go past the analysis
 * date because nothing after it is "known" (the API enforces this too).
 */
export default function DateRangeFilter({ value, onChange }) {
  const { summary } = useDataset()
  if (!summary) return null
  const end = summary.analysisDate
  const start = summary.horizonStart
  const clamp = (d) => (d < start ? start : d)
  const presets = [
    { label: 'All data', from: start, to: end },
    { label: 'Last 12 months', from: clamp(addDays(addMonths(end, -12), 1)), to: end },
    { label: 'Last 90 days', from: clamp(addDays(end, -89)), to: end },
    { label: 'Last 30 days', from: clamp(addDays(end, -29)), to: end },
  ]
  const current = value.from ?? start
  const currentTo = value.to ?? end
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex rounded-md border border-line bg-white text-xs" role="group" aria-label="Date presets">
        {presets.map((p) => {
          const selected = p.from === current && p.to === currentTo
          return (
            <button
              type="button"
              key={p.label}
              onClick={() => onChange({ from: p.from, to: p.to })}
              className={`px-2.5 py-1.5 ${selected ? 'bg-navy-700 font-semibold text-white' : 'text-ink hover:bg-slate-50'}`}
            >
              {p.label}
            </button>
          )
        })}
      </div>
      <Field label="From">
        <Input type="date" value={current} min={start} max={end} onChange={(e) => onChange({ ...value, from: e.target.value || start })} />
      </Field>
      <Field label="To">
        <Input type="date" value={currentTo} min={start} max={end} onChange={(e) => onChange({ ...value, to: e.target.value || end })} />
      </Field>
    </div>
  )
}
