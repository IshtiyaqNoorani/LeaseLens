import { useMemo, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { apiPost } from '../api/client.js'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { Badge, Button, Card, DataTable, Field, Note, PageHeader, ProblemBanner, Select } from '../components/ui.jsx'
import { GRID, SERIES, axisProps } from '../lib/chartTokens.js'
import { date, micros } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

const SIZES = [1000, 10000, 100000]
const sizeLabel = (n) => (n >= 1000 ? `${n / 1000}k` : String(n))
/** Plain international grouping for algorithm sizes (100,000), matching the 100k chart labels. */
const number = (v) => (v === null || v === undefined ? 'n/a' : Number(v).toLocaleString('en-US'))
const ratio = (v) => (v === null || v === undefined ? 'n/a' : `${v.toFixed(2)}×`)

/** Picks rows matching every key in `match`. Presentation only: reshapes committed results for charts. */
const pick = (rows, match) => rows.filter((r) => Object.entries(match).every(([k, v]) => r[k] === v))

export default function Lab() {
  const { data, error, loading } = useApi('/benchmarks/results')
  const t = data?.tables
  return (
    <>
      <PageHeader
        title="Algorithm performance lab"
        subtitle="Measured comparisons of the hand-written algorithms against baselines at 1,000, 10,000 and 100,000 inputs. Every number below is read from committed benchmark CSV files; nothing is estimated."
      />
      <ProblemBanner error={error} />
      {data && !data.available && <Note tone="warn">{data.note}</Note>}
      {t && (
        <div className={`space-y-6 ${loading ? 'opacity-60' : ''}`}>
          <Environment meta={t.segment_tree?.metadata} tables={t} />
          {t.segment_tree && <SegmentTreeSection rows={t.segment_tree.rows} />}
          {t.quickselect && <QuickselectSection rows={t.quickselect.rows} />}
          {t.seed_study && <SeedSection rows={t.seed_study.rows} histogram={t.seed_histogram?.rows ?? []} seeds={t.seed_study.metadata.seeds_per_configuration} />}
          <LiveRun />
        </div>
      )}
    </>
  )
}

function Environment({ meta, tables }) {
  if (!meta) return null
  const all = [...(tables.segment_tree?.rows ?? []), ...(tables.quickselect?.rows ?? [])]
  const verified = all.filter((r) => r.verified).length
  return (
    <Card title="How these numbers were measured">
      <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm md:grid-cols-4">
        <Item label="Measured" value={date(meta.generated_at?.slice(0, 10))} />
        <Item label="Java" value={meta.java} />
        <Item label="Machine" value={`${meta.os}, ${meta.cpu_cores} cores`} />
        <Item label="JVM options" value={meta.jvm_args || 'default'} />
        <Item label="Warm-up runs (discarded)" value={meta.warmup_iterations} />
        <Item label="Timed repetitions" value={meta.timed_repetitions} />
        <Item label="Seed" value={meta.seed} />
        <Item label="Answers verified" value={`${verified} of ${all.length} measurements`} />
      </dl>
      <p className="mt-3 text-xs text-muted">
        Times are medians per run (System.nanoTime; short tasks are repeated within a sample so each sample lasts at least 1 ms).
        Before timing, every method's answers were checked against the others. Absolute times depend on the machine; ratios
        and operation counts transfer better. Regenerate with the benchmark CLI (README).
      </p>
    </Card>
  )
}

function Item({ label, value }) {
  return (
    <div>
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="font-medium text-ink">{value}</dd>
    </div>
  )
}

function SegmentTreeSection({ rows }) {
  const [ratioParam, setRatioParam] = useState('update_ratio=0.10')
  const [rangeKind, setRangeKind] = useState('random')
  const shortAvailable = ratioParam === 'update_ratio=0.00' || ratioParam === 'update_ratio=0.10'
  const kind = shortAvailable ? rangeKind : 'random'
  const wl = `10000 ops, ${kind === 'random' ? 'random' : 'short (<=32 days)'} ranges`

  const chart = SIZES.map((n) => {
    const g = (a) => pick(rows, { experiment: 'workload', algorithm: a, n, parameter: ratioParam, workload: wl })[0]
    return { n: sizeLabel(n), tree: g('SegmentTree')?.median_ns, linear: g('LinearScan')?.median_ns, prefix: g('PrefixSums')?.median_ns }
  })
  const table = SIZES.flatMap((n) => pick(rows, { experiment: 'workload', n, parameter: ratioParam, workload: wl }))
  const builds = pick(rows, { experiment: 'build' })

  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold text-ink">Segment Tree vs linear scan vs prefix sums</h2>
      <p className="max-w-4xl text-sm text-muted">
        Each run builds the structure, then performs 10,000 operations on daily values: range queries for sum, minimum and maximum,
        mixed with a share of point updates (corrections). Prefix sums can answer only the sum, so they do less work than the other
        two and cannot power the worst-day and best-day figures.
      </p>
      <div className="flex flex-wrap gap-3">
        <Field label="Share of operations that are corrections">
          <Select value={ratioParam} onChange={(e) => setRatioParam(e.target.value)}>
            <option value="update_ratio=0.00">0% (queries only)</option>
            <option value="update_ratio=0.01">1%</option>
            <option value="update_ratio=0.10">10%</option>
            <option value="update_ratio=0.50">50%</option>
          </Select>
        </Field>
        <Field label="Query ranges">
          <Select value={kind} onChange={(e) => setRangeKind(e.target.value)} disabled={!shortAvailable}>
            <option value="random">Random (average a third of the data)</option>
            <option value="short">Short (up to 32 days)</option>
          </Select>
        </Field>
      </div>
      <div className="grid gap-5 2xl:grid-cols-2">
        <ChartCard
          title="Median time per workload (log scale)"
          subtitle={wl.replace('10000', '10,000').replace('<=', '≤')}
          legend={[
            { label: 'Segment Tree (assessed)', color: SERIES[0] },
            { label: 'Linear scan', color: SERIES[1] },
            { label: 'Prefix sums (sum only)', color: SERIES[2] },
          ]}
          table={{
            columns: [
              { key: 'n', label: 'n' },
              { key: 't', label: 'Segment Tree', align: 'right', render: (r) => micros(r.tree) },
              { key: 'l', label: 'Linear scan', align: 'right', render: (r) => micros(r.linear) },
              { key: 'p', label: 'Prefix sums', align: 'right', render: (r) => micros(r.prefix) },
            ],
            rows: chart,
          }}
        >
          <ResponsiveContainer>
            <LineChart data={chart} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRID} vertical={false} />
              <XAxis dataKey="n" {...axisProps} />
              <YAxis scale="log" domain={['auto', 'auto']} allowDataOverflow {...axisProps} width={64} tickFormatter={micros} />
              <Tooltip content={<ChartTooltip formatLabel={(l) => `n = ${l}`} formatValue={micros} />} cursor={{ stroke: GRID }} />
              <Line dataKey="tree" name="Segment Tree" stroke={SERIES[0]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
              <Line dataKey="linear" name="Linear scan" stroke={SERIES[1]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
              <Line dataKey="prefix" name="Prefix sums (sum only)" stroke={SERIES[2]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
        <Card title="Measurements" subtitle="× = this method's median ÷ the Segment Tree's (above 1 means slower than the tree)">
          <DataTable
            rowKey={(r) => `${r.n}-${r.algorithm}`}
            rows={table}
            columns={[
              { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
              { key: 'a', label: 'Method', render: (r) => <span>{r.algorithm} {r.role === 'assessed' && <Badge tone="navy">assessed</Badge>}</span> },
              { key: 'm', label: 'Median (IQR)', align: 'right', render: (r) => <span>{micros(r.median_ns)} <span className="text-xs text-muted">({micros(r.p25_ns)}–{micros(r.p75_ns)})</span></span> },
              { key: 'x', label: '× tree', align: 'right', render: (r) => ratio(r.vs_assessed) },
              { key: 'o', label: 'Work done', align: 'right', render: (r) => number(r.operations) },
            ]}
          />
          <p className="mt-2 text-xs text-muted">Work done: nodes visited (tree), elements scanned (linear), prefix entries read or rewritten (prefix sums).</p>
        </Card>
      </div>
      <Card title="Build cost (before any query)">
        <DataTable
          rowKey={(r) => `${r.n}-${r.algorithm}`}
          rows={builds}
          columns={[
            { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
            { key: 'a', label: 'Structure', render: (r) => r.algorithm },
            { key: 'm', label: 'Median build time', align: 'right', render: (r) => micros(r.median_ns) },
          ]}
        />
      </Card>
    </section>
  )
}

function QuickselectSection({ rows }) {
  const [shape, setShape] = useState('uniform')
  const [kShape, setKShape] = useState('delays')
  const [kN, setKN] = useState(100000)

  const single = SIZES.map((n) => {
    const g = (a) => pick(rows, { experiment: 'single-percentile', algorithm: a, n, workload: shape })[0]
    return { n: sizeLabel(n), qs: g('RandomizedQuickselect')?.median_ns, sort: g('Arrays.sort')?.median_ns }
  })
  const singleRows = SIZES.flatMap((n) => pick(rows, { experiment: 'single-percentile', n, workload: shape }))
  const kChart = [1, 3, 5, 9, 19].map((k) => {
    const g = (a) => pick(rows, { experiment: 'k-percentiles', algorithm: a, n: kN, workload: kShape, parameter: `k=${k}` })[0]
    return { k: String(k), qs: g('RandomizedQuickselect')?.median_ns, sort: g('Arrays.sort')?.median_ns }
  })
  const adversarial = rows.filter((r) => r.experiment === 'pivot-rule' || r.experiment === 'partition')

  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold text-ink">Randomized Quickselect vs sorting</h2>
      <div className="grid gap-5 xl:grid-cols-2">
        <ChartCard
          title="One percentile (P90): median time (log scale)"
          subtitle={
            <span className="inline-flex flex-wrap items-center gap-2">
              Input
              <Select value={shape} onChange={(e) => setShape(e.target.value)}>
                {['uniform', 'delays', 'sorted', 'reverse', 'identical'].map((s) => <option key={s} value={s}>{s}</option>)}
              </Select>
            </span>
          }
          legend={[{ label: 'Randomized Quickselect (assessed)', color: SERIES[0] }, { label: 'Arrays.sort then index', color: SERIES[1] }]}
          table={{
            columns: [
              { key: 'n', label: 'n' },
              { key: 'a', label: 'Method', render: (r) => r.algorithm },
              { key: 'm', label: 'Median', align: 'right', render: (r) => micros(r.median_ns) },
              { key: 'x', label: '× quickselect', align: 'right', render: (r) => ratio(r.vs_assessed) },
              { key: 'c', label: 'Comparisons', align: 'right', render: (r) => (r.operations >= 0 ? number(r.operations) : 'not instrumented') },
            ],
            rows: singleRows,
          }}
        >
          <ResponsiveContainer>
            <LineChart data={single} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRID} vertical={false} />
              <XAxis dataKey="n" {...axisProps} />
              <YAxis scale="log" domain={['auto', 'auto']} allowDataOverflow {...axisProps} width={64} tickFormatter={micros} />
              <Tooltip content={<ChartTooltip formatLabel={(l) => `n = ${l}`} formatValue={micros} />} cursor={{ stroke: GRID }} />
              <Line dataKey="qs" name="Quickselect" stroke={SERIES[0]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
              <Line dataKey="sort" name="Sort" stroke={SERIES[1]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard
          title="Several percentiles: k selections vs one sort"
          subtitle={
            <span className="inline-flex flex-wrap items-center gap-2">
              n
              <Select value={kN} onChange={(e) => setKN(Number(e.target.value))}>
                {SIZES.map((n) => <option key={n} value={n}>{number(n)}</option>)}
              </Select>
              input
              <Select value={kShape} onChange={(e) => setKShape(e.target.value)}>
                <option value="delays">delays (many zeros)</option>
                <option value="uniform">uniform</option>
              </Select>
            </span>
          }
          legend={[{ label: 'k × Randomized Quickselect', color: SERIES[0] }, { label: 'One Arrays.sort + k lookups', color: SERIES[1] }]}
          table={{ columns: [{ key: 'k', label: 'k percentiles' }, { key: 'q', label: 'Quickselect', align: 'right', render: (r) => micros(r.qs) }, { key: 's', label: 'Sort', align: 'right', render: (r) => micros(r.sort) }], rows: kChart }}
        >
          <ResponsiveContainer>
            <LineChart data={kChart} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRID} vertical={false} />
              <XAxis dataKey="k" {...axisProps} label={{ value: 'number of percentiles (k)', position: 'insideBottom', offset: -2, fontSize: 11, fill: '#898781' }} />
              <YAxis {...axisProps} width={64} tickFormatter={micros} />
              <Tooltip content={<ChartTooltip formatLabel={(l) => `k = ${l}`} formatValue={micros} />} cursor={{ stroke: GRID }} />
              <Line dataKey="qs" name="Quickselect" stroke={SERIES[0]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
              <Line dataKey="sort" name="Sort" stroke={SERIES[1]} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <Card
        title="Why the pivot is random and the partition is three-way"
        subtitle="Comparisons are machine-independent. Textbook two-way variants are not run above 10,000 because they are quadratic on these inputs."
      >
        <DataTable
          rowKey={(r) => `${r.experiment}-${r.n}-${r.workload}-${r.algorithm}`}
          rows={adversarial}
          columns={[
            { key: 'e', label: 'Test', render: (r) => (r.experiment === 'pivot-rule' ? 'Pivot rule' : 'Partition scheme') },
            { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
            { key: 'w', label: 'Input', render: (r) => r.workload },
            { key: 'a', label: 'Method', render: (r) => <span>{r.algorithm} {r.role === 'assessed' && <Badge tone="navy">assessed</Badge>}</span> },
            { key: 'c', label: 'Comparisons', align: 'right', render: (r) => number(r.operations) },
            { key: 'cn', label: 'per element', align: 'right', render: (r) => (r.operations / r.n).toFixed(1) },
            { key: 'm', label: 'Median time', align: 'right', render: (r) => micros(r.median_ns) },
          ]}
        />
      </Card>
    </section>
  )
}

function SeedSection({ rows, histogram, seeds }) {
  const h = histogram.filter((b) => b.n === 10000).map((b) => ({ label: `${b.from_per_n.toFixed(2)}`, runs: b.runs, from: b.from_per_n, to: b.to_per_n }))
  const refBin = h.find((b) => b.from <= 3.3863 && 3.3863 < b.to)
  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold text-ink">Randomization study (CO5)</h2>
      <p className="max-w-4xl text-sm text-muted">
        The same selection was run with {number(Number(seeds))} different random seeds per configuration. A Las Vegas algorithm must
        give the same answer every time; only the work done may vary. For the median of n distinct values, theory predicts about
        2(1 + ln 2)·n ≈ 3.39n comparisons on average.
      </p>
      <div className="grid gap-5 2xl:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
        <Card title="Comparisons across seeds">
          <DataTable
            rowKey={(r) => `${r.n}-${r.shape}-${r.rank}`}
            rows={rows}
            columns={[
              { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
              { key: 's', label: 'Input', render: (r) => r.shape },
              { key: 'r', label: 'Rank', render: (r) => r.rank },
              { key: 'ok', label: 'All correct', render: (r) => (r.all_correct ? <Badge tone="gain">yes</Badge> : <Badge tone="loss">no</Badge>) },
              { key: 'm', label: 'Mean ÷ n', align: 'right', render: (r) => r.mean_per_n.toFixed(2) },
              { key: 'p', label: 'P95 ÷ n', align: 'right', render: (r) => r.p95_per_n.toFixed(2) },
              { key: 'x', label: 'Max ÷ n', align: 'right', render: (r) => r.max_per_n.toFixed(2) },
              { key: 't', label: 'Theory ÷ n', align: 'right', render: (r) => (r.reference_per_n ? r.reference_per_n.toFixed(2) : '') },
            ]}
          />
          <p className="mt-2 text-xs text-muted">Delay-shaped inputs need fewer comparisons because a pivot equal to 0 removes about two-thirds of the data at once.</p>
        </Card>
        <ChartCard
          title="Distribution of comparisons, n = 10,000 (median)"
          subtitle="Runs per bin of comparisons ÷ n; the line marks the theoretical mean of about 3.39"
          table={{ columns: [{ key: 'label', label: 'Comparisons ÷ n from' }, { key: 'runs', label: 'Runs', align: 'right' }], rows: h }}
        >
          <ResponsiveContainer>
            <BarChart data={h} margin={{ top: 24, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid stroke={GRID} vertical={false} />
              <XAxis dataKey="label" {...axisProps} interval={1} />
              <YAxis {...axisProps} width={40} allowDecimals={false} />
              <Tooltip content={<ChartTooltip formatLabel={(l) => `${l} to ${(Number(l) + 0.25).toFixed(2)} × n`} formatValue={(v) => `${v} runs`} />} cursor={{ fill: '#f1f5f9' }} />
              {refBin && <ReferenceLine x={refBin.label} stroke="#52514e" strokeDasharray="0" label={{ value: '≈3.39n', position: 'top', fontSize: 11, fill: '#52514e' }} />}
              <Bar dataKey="runs" name="Runs" fill={SERIES[0]} maxBarSize={24} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>
    </section>
  )
}

function LiveRun() {
  const [suite, setSuite] = useState('quickselect')
  const [sizes, setSizes] = useState([1000, 10000])
  const [reps, setReps] = useState(5)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const toggle = (n) => setSizes((s) => (s.includes(n) ? s.filter((x) => x !== n) : [...s, n].sort((a, b) => a - b)))

  async function run() {
    setBusy(true)
    setError(null)
    try {
      setResult(await apiPost('/benchmarks/run', { suite, sizes, repetitions: reps }))
    } catch (e) {
      setError(e)
    } finally {
      setBusy(false)
    }
  }

  const rows = useMemo(() => result?.rows ?? [], [result])
  return (
    <Card title="Run a small benchmark now" subtitle="Runs inside the Java API on this machine. Bounded (n ≤ 100,000, ≤ 10 repetitions) and indicative only, because it shares the CPU with the web server.">
      <div className="flex flex-wrap items-end gap-4">
        <Field label="Suite">
          <Select value={suite} onChange={(e) => setSuite(e.target.value)}>
            <option value="quickselect">Quickselect vs sort (P90)</option>
            <option value="segment-tree">Segment Tree vs scans (10% corrections)</option>
          </Select>
        </Field>
        <fieldset className="flex items-center gap-3 text-sm">
          <legend className="mb-1 text-xs font-medium text-muted">Sizes</legend>
          {SIZES.map((n) => (
            <label key={n} className="inline-flex items-center gap-1">
              <input type="checkbox" checked={sizes.includes(n)} onChange={() => toggle(n)} /> {number(n)}
            </label>
          ))}
        </fieldset>
        <Field label="Repetitions">
          <Select value={reps} onChange={(e) => setReps(Number(e.target.value))}>
            {[3, 5, 10].map((r) => <option key={r} value={r}>{r}</option>)}
          </Select>
        </Field>
        <Button disabled={busy || sizes.length === 0} onClick={run}>{busy ? 'Running…' : 'Run benchmark'}</Button>
      </div>
      <div className="mt-3">
        <ProblemBanner error={error} />
      </div>
      {result && (
        <div className="mt-4 space-y-2">
          <DataTable
            rowKey={(r) => `${r.n}-${r.workload}-${r.algorithm}`}
            rows={rows}
            columns={[
              { key: 'n', label: 'n', align: 'right', render: (r) => number(r.n) },
              { key: 'w', label: 'Input', render: (r) => r.workload },
              { key: 'a', label: 'Method', render: (r) => <span>{r.algorithm} {r.role === 'assessed' && <Badge tone="navy">assessed</Badge>}</span> },
              { key: 'm', label: 'Median', align: 'right', render: (r) => micros(r.median_ns) },
              { key: 'x', label: '× assessed', align: 'right', render: (r) => ratio(r.vs_assessed) },
              { key: 'o', label: 'Work done', align: 'right', render: (r) => (r.operations >= 0 ? number(r.operations) : 'n/a') },
              { key: 'v', label: 'Answers agree', render: (r) => (r.verified ? <Badge tone="gain">yes</Badge> : <Badge tone="loss">no</Badge>) },
            ]}
          />
          <p className="text-xs text-muted">{result.note}</p>
        </div>
      )}
    </Card>
  )
}
