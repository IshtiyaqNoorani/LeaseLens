import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { qs } from '../api/client.js'
import AlgorithmPanel from '../components/AlgorithmPanel.jsx'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { GRID, SERIES, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Button, Card, DataTable, Field, HowCalculated, Input, Note, PageHeader, ProblemBanner, Select, Stat } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, days, number, pct, rupees, rupeesAxis, rupeesCompact, titleCase } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

const TYPES = ['RESIDENTIAL', 'OFFICE', 'RETAIL', 'INDUSTRIAL']
const PAGE_SIZE = 25

export default function Payments() {
  const { version } = useDataset()
  const [params] = useSearchParams()
  const [filters, setFilters] = useState({ propertyId: params.get('propertyId') || '', propertyType: '' })
  const [range, setRange] = useState({})
  const [pText, setPText] = useState('50,75,90')
  const [pList, setPList] = useState('50,75,90')
  const [seed, setSeed] = useState('20261124')
  const [thresholdP, setThresholdP] = useState('90')
  const [page, setPage] = useState(0)
  const [runs, setRuns] = useState([])

  const filterQs = { ...filters, from: range.from, to: range.to }
  const properties = useApi('/properties/performance?sort=propertyId', version)
  const pct_ = useApi(`/payments/percentiles${qs({ ...filterQs, p: pList, seed })}`, version)
  const thr = useApi(`/payments/threshold${qs({ ...filterQs, p: thresholdP, seed, page, size: PAGE_SIZE })}`, version)
  const open = useApi(`/payments/outstanding${qs(filterQs)}`, version)

  const v = pct_.data

  // Keep a short history of runs to show that a new seed changes the work done, never the answer.
  useEffect(() => {
    if (!v?.algorithm || v.percentiles.length === 0) return
    const row = {
      seed: v.algorithm.seed,
      values: v.percentiles.map((x) => `P${x.p}=${x.daysLate}`).join(', '),
      comparisons: v.algorithm.operations,
      n: v.algorithm.n,
    }
    setRuns((rs) => (rs.length && rs[0].seed === row.seed && rs[0].values === row.values && rs[0].n === row.n ? rs : [row, ...rs].slice(0, 8)))
  }, [v])

  const t = thr.data
  const binStart = (label) => Number(label.split(/[-+]/)[0])

  return (
    <>
      <PageHeader
        title="Payment behaviour"
        subtitle="How late completed rent payments were, as percentiles computed by a hand-written Randomized Quickselect in Java. Descriptive analysis of payment records, not a judgement about any tenant."
      />
      <div className="mb-3 flex flex-wrap items-end gap-3">
        <Field label="Property type">
          <Select value={filters.propertyType} onChange={(e) => { setFilters((f) => ({ ...f, propertyType: e.target.value })); setPage(0) }}>
            <option value="">All types</option>
            {TYPES.map((x) => <option key={x} value={x}>{titleCase(x)}</option>)}
          </Select>
        </Field>
        <Field label="Property">
          <Select value={filters.propertyId} onChange={(e) => { setFilters((f) => ({ ...f, propertyId: e.target.value })); setPage(0) }}>
            <option value="">All properties</option>
            {(properties.data?.rows ?? []).map((p) => <option key={p.propertyId} value={p.propertyId}>{p.propertyId} · {p.name}</option>)}
          </Select>
        </Field>
      </div>
      <div className="mb-5">
        <div className="mb-1 text-xs font-medium text-muted">Due date range</div>
        <DateRangeFilter value={range} onChange={(r) => { setRange(r); setPage(0) }} />
      </div>

      <Card title="Percentiles of days late" subtitle="Nearest-rank rule: k = ⌈P/100 × n⌉ − 1 (zero-based), always an actual observed value">
        <div className="mb-4 flex flex-wrap items-end gap-3">
          <Field label="Percentiles (comma-separated, up to 2 decimals)">
            <Input value={pText} onChange={(e) => setPText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && setPList(pText)} className="w-56" />
          </Field>
          <Button variant="secondary" onClick={() => setPList(pText)}>Compute</Button>
          <Field label="Random seed">
            <Input value={seed} onChange={(e) => setSeed(e.target.value.replace(/[^0-9]/g, ''))} className="w-40" />
          </Field>
          <Button variant="secondary" onClick={() => setSeed(String(Math.floor(Math.random() * 1e12) + 1))}>
            Re-run with a new random seed
          </Button>
        </div>
        <ProblemBanner error={pct_.error} />
        {v && (
          <div className={`space-y-4 ${pct_.loading ? 'opacity-60' : ''}`}>
            {v.message && <Note tone="warn">{v.message}</Note>}
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <Stat label="Completed payments (n)" value={number(v.population.completed)} />
              <Stat label="Open, excluded" value={number(v.population.openExcluded)} note="Not settled by the analysis date" />
              <Stat label="On time or early" value={number(v.population.onTimeOrEarly)} note="0 days late" />
              <Stat label="Mean days late" value={v.meanDaysLate === null ? 'n/a' : v.meanDaysLate.toFixed(2)} note="Compare with the median below" />
            </div>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-6">
              {v.percentiles.map((x) => (
                <Stat key={x.p} label={`P${x.p}`} value={days(x.daysLate)} note={`rank k = ${number(x.k)}`} />
              ))}
            </div>
            <AlgorithmPanel
              run={v.algorithm}
              note={`${v.percentiles.length} selection(s) over ${number(v.algorithm.n)} delays in ${number(v.algorithm.operations)} comparisons, with no full sort. Las Vegas algorithm: re-running with a different seed always gives the same percentiles; only the number of comparisons changes.`}
            />
            {v.censoringNote && <Note tone="warn">{v.censoringNote}</Note>}
            {runs.length > 1 && (
              <div>
                <h3 className="mb-1 text-xs font-semibold text-muted">Runs this session (randomization demo)</h3>
                <DataTable
                  rowKey={(r) => `${r.seed}-${r.values}`}
                  rows={runs}
                  columns={[
                    { key: 'seed', label: 'Seed' },
                    { key: 'values', label: 'Percentiles (days late)' },
                    { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
                    { key: 'comparisons', label: 'Comparisons', align: 'right', render: (r) => number(r.comparisons) },
                  ]}
                />
              </div>
            )}
          </div>
        )}
        <HowCalculated>
          <p>Days late = settled date − due date, floored at 0 (early payments count as 0), for obligations paid in full on or before the analysis date.</p>
          <p>Randomized Quickselect picks a pivot uniformly at random and splits the values into smaller, equal and larger groups, keeping only the side that contains rank k. Expected O(n) time; worst case O(n²) with vanishing probability.</p>
          <p>Open obligations are excluded (right-censoring). Because they are usually the latest payers, percentiles may understate delays.</p>
        </HowCalculated>
      </Card>

      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
        <ChartCard
          title="Distribution of days late"
          subtitle={t ? `Completed payments; bins starting at or above the P${t.p} threshold (${days(t.thresholdDaysLate)}) are highlighted` : 'Completed payments'}
          loading={thr.loading}
          legend={[{ label: 'Below threshold', color: SERIES[0], shape: 'bar' }, { label: 'At or above threshold', color: SERIES[1], shape: 'bar' }]}
          table={{ columns: [{ key: 'label', label: 'Days late' }, { key: 'count', label: 'Payments', align: 'right', render: (b) => number(b.count) }], rows: t?.histogram ?? [] }}
        >
          <ResponsiveContainer>
            <BarChart data={t?.histogram ?? []} margin={{ top: 8, right: 8, left: 4, bottom: 0 }}>
              <CartesianGrid stroke={GRID} vertical={false} />
              <XAxis dataKey="label" {...axisProps} />
              <YAxis {...axisProps} width={44} allowDecimals={false} />
              <Tooltip content={<ChartTooltip formatLabel={(l) => `${l} days late`} formatValue={(x) => number(x)} />} cursor={{ fill: '#f1f5f9' }} />
              <Bar dataKey="count" name="Payments" maxBarSize={24} radius={[4, 4, 0, 0]}>
                {(t?.histogram ?? []).map((b) => (
                  <Cell key={b.label} fill={t && binStart(b.label) >= t.thresholdDaysLate && t.thresholdDaysLate > 0 ? SERIES[1] : SERIES[0]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <Card title="Where the slow payments are" subtitle={t ? `Payments at or above the P${t.p} threshold, by property` : undefined}
          actions={
            <Field label="Threshold">
              <Select value={thresholdP} onChange={(e) => { setThresholdP(e.target.value); setPage(0) }}>
                {['75', '90', '95', '99'].map((p) => <option key={p} value={p}>P{p}</option>)}
              </Select>
            </Field>
          }>
          <ProblemBanner error={thr.error} />
          {t && (
            <DataTable
              maxHeight={260}
              rowKey={(r) => r.propertyId}
              rows={t.byProperty.filter((r) => r.atOrAbove > 0)}
              columns={[
                { key: 'p', label: 'Property', render: (r) => `${r.propertyId} · ${r.propertyName}` },
                { key: 'a', label: 'At/above', align: 'right', render: (r) => number(r.atOrAbove) },
                { key: 'c', label: 'Completed', align: 'right', render: (r) => number(r.completed) },
                { key: 's', label: 'Share', align: 'right', render: (r) => pct(r.sharePct) },
              ]}
            />
          )}
        </Card>
      </div>

      {t && (
        <Card className="mt-5" title={`Payment records at or above ${days(t.thresholdDaysLate)} late`} subtitle={`${number(t.countAtOrAbove)} of ${number(t.population.completed)} completed payments (P${t.p}). Mean days late across all completed payments: ${t.meanDaysLate ?? 'n/a'}.`}>
          <DataTable
            rowKey={(r) => r.obligationId}
            rows={t.records}
            columns={[
              { key: 'o', label: 'Obligation', render: (r) => r.obligationId },
              { key: 'p', label: 'Property', render: (r) => r.propertyId },
              { key: 'l', label: 'Lease', render: (r) => r.leaseId },
              { key: 'd', label: 'Due', render: (r) => date(r.dueDate) },
              { key: 's', label: 'Settled', render: (r) => date(r.settledDate) },
              { key: 'late', label: 'Days late', align: 'right', render: (r) => number(r.daysLate) },
              { key: 'amt', label: 'Amount', align: 'right', render: (r) => rupees(r.duePaise) },
            ]}
          />
          <div className="mt-3 flex items-center justify-between text-xs text-muted">
            <span>Page {page + 1} of {Math.max(1, Math.ceil(t.totalRecords / PAGE_SIZE))}</span>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <Button variant="secondary" disabled={(page + 1) * PAGE_SIZE >= t.totalRecords} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          </div>
        </Card>
      )}

      {open.data && (
        <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)]">
          <ChartCard
            title="Outstanding rent by days overdue"
            subtitle={`Kept separate from the delay distribution. ${number(open.data.openObligations)} open obligation(s), ${rupeesCompact(open.data.totalOutstandingPaise)} owed as of ${date(open.data.analysisDate)}.`}
            loading={open.loading}
            table={{ columns: [{ key: 'bucket', label: 'Days overdue' }, { key: 'count', label: 'Obligations', align: 'right' }, { key: 'p', label: 'Balance', align: 'right', render: (b) => rupees(b.paise) }], rows: open.data.ageing }}
          >
            <ResponsiveContainer>
              <BarChart data={open.data.ageing} margin={{ top: 8, right: 8, left: 4, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="bucket" {...axisProps} />
                <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                <Tooltip content={<ChartTooltip formatLabel={(l) => `${l} days overdue`} formatValue={(x) => rupeesCompact(x)} />} cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="paise" name="Balance" fill={SERIES[0]} maxBarSize={24} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
          <Card title="Open obligations" subtitle="Balance and days overdue as of the analysis date">
            <DataTable
              maxHeight={300}
              rowKey={(r) => r.obligationId}
              rows={open.data.records}
              emptyText="Nothing is outstanding for this selection."
              columns={[
                { key: 'o', label: 'Obligation', render: (r) => r.obligationId },
                { key: 'p', label: 'Property', render: (r) => r.propertyId },
                { key: 'd', label: 'Due', render: (r) => date(r.dueDate) },
                { key: 'b', label: 'Balance', align: 'right', render: (r) => rupees(r.balancePaise) },
                { key: 'od', label: 'Days overdue', align: 'right', render: (r) => number(r.daysOverdue) },
              ]}
            />
          </Card>
        </div>
      )}
    </>
  )
}
