import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, ReferenceDot, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { apiDelete, apiPost, qs } from '../api/client.js'
import AlgorithmPanel from '../components/AlgorithmPanel.jsx'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { GRID, NEG, POS, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Badge, Button, Card, DataTable, Field, Hero, HowCalculated, Input, Note, PageHeader, ProblemBanner, Select, Stat } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, number, rupees, rupeesAxis, rupeesCompact } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

/** Parses a rupee amount typed by the user ("-1500.50") into integer paise without floating point. */
function toPaise(text) {
  const t = text.trim()
  if (!/^-?\d+(\.\d{1,2})?$/.test(t)) return null
  const negative = t.startsWith('-')
  const [whole, frac = ''] = t.replace('-', '').split('.')
  const paise = Number(whole) * 100 + Number(frac.padEnd(2, '0'))
  return negative ? -paise : paise
}

export default function CashFlow() {
  const { version, summary, refresh } = useDataset()
  const [params, setParams] = useSearchParams()
  const scope = params.get('scope') || 'portfolio'
  const [range, setRange] = useState({})
  const [granularity, setGranularity] = useState('MONTH')
  const [compare, setCompare] = useState({ from: '', to: '' })
  const [edits, setEdits] = useState(0)
  const key = `${version}-${edits}`

  const properties = useApi('/properties/performance?sort=propertyId', version)
  const result = useApi(`/cashflow/range${qs({ scope, ...range })}`, key)
  const series = useApi(`/cashflow/series${qs({ scope, ...range })}`, key)
  const periods = useApi(`/cashflow/periods${qs({ scope, ...range, granularity })}`, key)
  const other = useApi(compare.from && compare.to ? `/cashflow/range${qs({ scope, ...compare })}` : null, key)
  const adjustments = useApi('/cashflow/adjustments', key)

  const [form, setForm] = useState({ propertyId: '', date: '', amount: '', reason: '' })
  const [formError, setFormError] = useState(null)
  const [lastUpdate, setLastUpdate] = useState(null)
  const [before, setBefore] = useState(null)

  const r = result.data
  const afterEdit = () => {
    setEdits((e) => e + 1)
    refresh()
  }

  async function submitCorrection() {
    setFormError(null)
    const amountPaise = toPaise(form.amount)
    if (amountPaise === null || amountPaise === 0) {
      setFormError({ message: 'Enter a non-zero rupee amount with at most two decimals, e.g. 1500 or -250.50.', problem: { title: 'Invalid amount' } })
      return
    }
    try {
      const propertyId = form.propertyId || (scope !== 'portfolio' ? scope : '')
      const res = await apiPost('/cashflow/adjustments', { propertyId, date: form.date, amountPaise, reason: form.reason })
      setBefore(r)
      setLastUpdate(res)
      setForm({ propertyId: '', date: '', amount: '', reason: '' })
      afterEdit()
    } catch (e) {
      setFormError(e)
    }
  }

  async function undo(id) {
    try {
      setBefore(r)
      const res = await apiDelete(`/cashflow/adjustments/${id}`)
      setLastUpdate({ adjustment: null, algorithm: res.algorithm })
      afterEdit()
    } catch (e) {
      setFormError(e)
    }
  }

  async function resetAll() {
    try {
      setBefore(r)
      await apiDelete('/cashflow/adjustments')
      setLastUpdate(null)
      afterEdit()
    } catch (e) {
      setFormError(e)
    }
  }

  const propertyOptions = properties.data?.rows ?? []
  const points = series.data?.points ?? []

  return (
    <>
      <PageHeader
        title="Cash-flow explorer"
        subtitle="Daily net rental cash flow (rent received minus operating expenses). Every range total, worst day and best day is answered by a hand-written Segment Tree in Java."
      />
      <div className="mb-5 flex flex-wrap items-end gap-4">
        <Field label="Scope">
          <Select value={scope} onChange={(e) => setParams(e.target.value === 'portfolio' ? {} : { scope: e.target.value })}>
            <option value="portfolio">Whole portfolio</option>
            {propertyOptions.map((p) => (
              <option key={p.propertyId} value={p.propertyId}>
                {p.propertyId} · {p.name}
              </option>
            ))}
          </Select>
        </Field>
        <DateRangeFilter value={range} onChange={setRange} />
      </div>
      <ProblemBanner error={result.error} onRetry={result.reload} />

      {r && (
        <div className={`space-y-5 ${result.loading ? 'opacity-60' : ''}`}>
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,2fr)]">
            <Hero
              label={`Net cash flow · ${scope === 'portfolio' ? 'portfolio' : scope}`}
              value={rupeesCompact(r.netCashFlowPaise)}
              note={`${date(r.from)} to ${date(r.to)} (${number(r.days)} days). Cash basis.`}
            />
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <Stat label="Worst day" value={rupeesCompact(r.minDailyPaise)} note={date(r.minDate)} tone={r.minDailyPaise < 0 ? 'loss' : undefined} />
              <Stat label="Best day" value={rupeesCompact(r.maxDailyPaise)} note={date(r.maxDate)} />
              <Stat label="Corrections in range" value={number(r.adjustmentsApplied)} note={r.adjustmentsApplied ? 'Included in these figures' : 'Source data only'} />
            </div>
          </div>
          <AlgorithmPanel
            run={r.algorithm}
            note={`One range query visited ${number(r.algorithm.operations)} tree node${r.algorithm.operations === 1 ? '' : 's'} to cover ${number(r.days)} days. A linear scan would read all ${number(r.days)} daily values; prefix sums could give the total quickly but not the worst or best day, and must be rebuilt after every correction.`}
          />

          <ChartCard
            title="Daily net cash flow"
            subtitle="Marked: worst and best day in the range (earliest date wins ties)"
            loading={series.loading}
            height={280}
            table={{
              columns: [
                { key: 'date', label: 'Date', render: (p) => date(p.date) },
                { key: 'r', label: 'Received', align: 'right', render: (p) => rupees(p.receiptsPaise) },
                { key: 'e', label: 'Expenses', align: 'right', render: (p) => rupees(p.expensesPaise) },
                { key: 'n', label: 'Net (incl. corrections)', align: 'right', render: (p) => rupees(p.netPaise) },
              ],
              rows: points.filter((p) => p.netPaise !== 0 || p.receiptsPaise !== 0),
            }}
          >
            <ResponsiveContainer>
              <AreaChart data={points} margin={{ top: 12, right: 16, left: 4, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="date" {...axisProps} minTickGap={48} tickFormatter={(d) => d.slice(0, 7)} />
                <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                <ReferenceLine y={0} stroke="#c3c2b7" />
                <Tooltip content={<ChartTooltip formatLabel={(l) => date(l)} formatValue={(v) => rupeesCompact(v)} />} cursor={{ stroke: '#898781' }} />
                <Area type="linear" dataKey="netPaise" name="Net cash flow" stroke={POS} strokeWidth={1.5} fill={POS} fillOpacity={0.1} isAnimationActive={false} />
                <ReferenceDot x={r.minDate} y={r.minDailyPaise} r={5} fill={NEG} stroke="#ffffff" strokeWidth={2} label={{ value: 'Worst', position: 'bottom', fontSize: 11, fill: '#52514e' }} />
                <ReferenceDot x={r.maxDate} y={r.maxDailyPaise} r={5} fill={POS} stroke="#ffffff" strokeWidth={2} label={{ value: 'Best', position: 'top', fontSize: 11, fill: '#52514e' }} />
              </AreaChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard
            title={`Net cash flow by ${granularity === 'MONTH' ? 'month' : 'quarter'}`}
            subtitle={
              <span className="inline-flex flex-wrap gap-2">
                One segment-tree query per period
                {periods.data && <span>({periods.data.periods.length} queries, {number(periods.data.algorithm.operations)} nodes visited in total)</span>}
                <span>· show</span>
                {['MONTH', 'QUARTER'].map((g) => (
                  <button key={g} type="button" onClick={() => setGranularity(g)} className={granularity === g ? 'font-semibold text-navy-700 underline' : 'text-navy-600'}>
                    {g === 'MONTH' ? 'months' : 'quarters'}
                  </button>
                ))}
              </span>
            }
            loading={periods.loading}
            table={{
              columns: [
                { key: 'label', label: 'Period' },
                { key: 'n', label: 'Net', align: 'right', render: (p) => rupees(p.netCashFlowPaise) },
                { key: 'w', label: 'Worst day', align: 'right', render: (p) => `${rupeesCompact(p.minDailyPaise)} (${date(p.minDate)})` },
                { key: 'b', label: 'Best day', align: 'right', render: (p) => `${rupeesCompact(p.maxDailyPaise)} (${date(p.maxDate)})` },
              ],
              rows: periods.data?.periods ?? [],
            }}
          >
            <ResponsiveContainer>
              <BarChart data={periods.data?.periods ?? []} margin={{ top: 8, right: 12, left: 4, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="label" {...axisProps} minTickGap={16} />
                <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                <ReferenceLine y={0} stroke="#c3c2b7" />
                <Tooltip content={<ChartTooltip formatValue={(v) => rupeesCompact(v)} />} cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="netCashFlowPaise" name="Net cash flow" maxBarSize={24} radius={[4, 4, 0, 0]}>
                  {(periods.data?.periods ?? []).map((p) => (
                    <Cell key={p.label} fill={p.netCashFlowPaise < 0 ? NEG : POS} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>

          <Card title="Compare with another period" subtitle="A second segment-tree query over a different range of the same scope">
            <div className="flex flex-wrap items-end gap-3">
              <Field label="From">
                <Input type="date" value={compare.from} min={summary?.horizonStart} max={summary?.analysisDate} onChange={(e) => setCompare((c) => ({ ...c, from: e.target.value }))} />
              </Field>
              <Field label="To">
                <Input type="date" value={compare.to} min={summary?.horizonStart} max={summary?.analysisDate} onChange={(e) => setCompare((c) => ({ ...c, to: e.target.value }))} />
              </Field>
            </div>
            <ProblemBanner error={other.error} />
            {other.data && (
              <div className="mt-4">
                <DataTable
                  rowKey={(row) => row.label}
                  columns={[
                    { key: 'label', label: 'Range' },
                    { key: 'n', label: 'Net cash flow', align: 'right', render: (x) => rupees(x.d.netCashFlowPaise) },
                    { key: 'days', label: 'Days', align: 'right', render: (x) => number(x.d.days) },
                    { key: 'w', label: 'Worst day', align: 'right', render: (x) => `${rupeesCompact(x.d.minDailyPaise)} (${date(x.d.minDate)})` },
                    { key: 'b', label: 'Best day', align: 'right', render: (x) => `${rupeesCompact(x.d.maxDailyPaise)} (${date(x.d.maxDate)})` },
                  ]}
                  rows={[
                    { label: `A · ${date(r.from)} to ${date(r.to)}`, d: r },
                    { label: `B · ${date(other.data.from)} to ${date(other.data.to)}`, d: other.data },
                  ]}
                />
              </div>
            )}
          </Card>

          <Card
            title="Corrections (point updates)"
            subtitle="Record a correction to one property's net cash flow on one day. Java applies it to that property's tree and the portfolio tree in O(log n), and every later query reflects it. Source data and Overview KPIs are not changed."
          >
            <div className="grid gap-3 md:grid-cols-[1.3fr_1fr_1fr_2fr_auto] md:items-end">
              <Field label="Property">
                <Select value={form.propertyId || (scope !== 'portfolio' ? scope : '')} onChange={(e) => setForm((f) => ({ ...f, propertyId: e.target.value }))}>
                  <option value="">Choose…</option>
                  {propertyOptions.map((p) => (
                    <option key={p.propertyId} value={p.propertyId}>{p.propertyId} · {p.name}</option>
                  ))}
                </Select>
              </Field>
              <Field label="Date">
                <Input type="date" value={form.date} min={summary?.horizonStart} max={summary?.horizonEnd} onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))} />
              </Field>
              <Field label="Amount (₹, + or −)">
                <Input inputMode="decimal" placeholder="e.g. 40000" value={form.amount} onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))} />
              </Field>
              <Field label="Reason (required)">
                <Input maxLength={200} placeholder="e.g. Reversed duplicate tax charge" value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} />
              </Field>
              <Button onClick={submitCorrection}>Apply</Button>
            </div>
            <div className="mt-3">
              <ProblemBanner error={formError} />
            </div>
            {lastUpdate && (
              <div className="mt-3">
                <AlgorithmPanel run={lastUpdate.algorithm} note="Each correction updates two root-to-leaf paths (property tree and portfolio tree); the count above is the nodes touched." />
              </div>
            )}
            {before && r && before.scope === r.scope && (
              <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-3">
                <Stat label="Net cash flow: before → now" value={`${rupees(before.netCashFlowPaise)} → ${rupees(r.netCashFlowPaise)}`} />
                <Stat label="Worst day: before → now" value={`${rupees(before.minDailyPaise)} → ${rupees(r.minDailyPaise)}`} note={`${date(before.minDate)} → ${date(r.minDate)}`} />
                <Stat label="Best day: before → now" value={`${rupees(before.maxDailyPaise)} → ${rupees(r.maxDailyPaise)}`} note={`${date(before.maxDate)} → ${date(r.maxDate)}`} />
              </div>
            )}
            <div className="mt-5 flex items-center justify-between">
              <h3 className="text-sm font-semibold text-ink">Correction log</h3>
              {(adjustments.data?.adjustments.length ?? 0) > 0 && (
                <Button variant="danger" onClick={resetAll}>Remove all corrections</Button>
              )}
            </div>
            <DataTable
              emptyText="No corrections recorded. Figures come from the source data only."
              rowKey={(a) => a.id}
              rows={adjustments.data?.adjustments ?? []}
              columns={[
                { key: 'id', label: 'Id', render: (a) => <Badge tone="warn">{a.id}</Badge> },
                { key: 'p', label: 'Property', render: (a) => a.propertyId },
                { key: 'd', label: 'Date', render: (a) => date(a.date) },
                { key: 'a', label: 'Amount', align: 'right', render: (a) => rupees(a.amountPaise, { decimals: true }) },
                { key: 'r', label: 'Reason', render: (a) => <span className="whitespace-normal">{a.reason}</span> },
                { key: 'u', label: '', render: (a) => <Button variant="secondary" onClick={() => undo(a.id)}>Undo</Button> },
              ]}
            />
            <HowCalculated>
              <p>Each day's value = rent payments received that day − operating expenses paid that day (paise), plus any corrections for that day.</p>
              <p>Range query: O(log n). Point update: O(log n). Building all trees: O(n) each. Removing all corrections rebuilds the trees from source data.</p>
            </HowCalculated>
          </Card>
          <Note>Cash basis. Corrections are what-if adjustments for exploration and are kept in memory until removed or a new dataset is loaded.</Note>
        </div>
      )}
    </>
  )
}
