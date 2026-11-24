import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis, ZAxis } from 'recharts'
import { qs } from '../api/client.js'
import { ChartCard } from '../components/chart.jsx'
import { GRID, POS, NEG, SERIES, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Card, DataTable, HowCalculated, PageHeader, ProblemBanner } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { number, pct, rupees, rupeesAxis, rupeesCompact, titleCase } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

export default function Properties() {
  const { version } = useDataset()
  const navigate = useNavigate()
  const [range, setRange] = useState({})
  const [sort, setSort] = useState({ key: 'collectionEfficiencyPct', order: 'asc' })
  const [dimension, setDimension] = useState('type')
  const table = useApi(`/properties/performance${qs({ ...range, sort: sort.key, order: sort.order })}`, version)
  const segments = useApi(`/properties/segments${qs({ ...range, dimension })}`, version)

  const header = (key, label) => (
    <button
      type="button"
      className={`inline-flex items-center gap-1 ${sort.key === key ? 'text-navy-700' : ''}`}
      onClick={() => setSort((s) => ({ key, order: s.key === key && s.order === 'desc' ? 'asc' : 'desc' }))}
      title="Sort (done by the Java API)"
    >
      {label}
      {sort.key === key && <span aria-hidden="true">{sort.order === 'asc' ? '▲' : '▼'}</span>}
    </button>
  )

  const rows = table.data?.rows ?? []
  return (
    <>
      <PageHeader
        title="Property performance"
        subtitle="Compare every property on income, collections, occupancy and costs. Click a column to re-rank; click a row for detail."
      />
      <div className="mb-5">
        <DateRangeFilter value={range} onChange={setRange} />
      </div>
      <ProblemBanner error={table.error || segments.error} onRetry={table.reload} />

      <Card title={`League table · ${number(table.data?.count)} properties`} subtitle="n/a means the ratio has no denominator in this period (for example, no rent fell due)">
        <div className={table.loading ? 'opacity-60' : ''}>
          <DataTable
            maxHeight={460}
            rowKey={(r) => r.propertyId}
            onRowClick={(r) => navigate(`/properties/${r.propertyId}`)}
            rows={rows}
            columns={[
              { key: 'propertyId', label: header('propertyId', 'Property'), render: (r) => <span><span className="font-medium text-navy-700">{r.propertyId}</span> <span className="text-muted">{r.name}</span></span> },
              { key: 'type', label: 'Type', render: (r) => titleCase(r.type) },
              { key: 'rb', label: header('rentBilledPaise', 'Billed'), align: 'right', render: (r) => rupeesCompact(r.rentBilledPaise) },
              { key: 'rc', label: header('rentCollectedPaise', 'Collected'), align: 'right', render: (r) => rupeesCompact(r.rentCollectedPaise) },
              { key: 'ce', label: header('collectionEfficiencyPct', 'Collection eff.'), align: 'right', render: (r) => pct(r.collectionEfficiencyPct) },
              { key: 'oc', label: header('timeWeightedOccupancyPct', 'Occupancy (TW)'), align: 'right', render: (r) => pct(r.timeWeightedOccupancyPct) },
              { key: 'rps', label: header('rentPerSqftRupees', 'Rent/sq ft'), align: 'right', render: (r) => (r.rentPerSqftRupees === null ? 'n/a' : `₹${r.rentPerSqftRupees.toFixed(2)}`) },
              { key: 'ox', label: header('operatingExpensesPaise', 'Expenses'), align: 'right', render: (r) => rupeesCompact(r.operatingExpensesPaise) },
              { key: 'er', label: header('expenseRatioPct', 'Expense ratio'), align: 'right', render: (r) => pct(r.expenseRatioPct) },
              { key: 'ns', label: header('netOperatingCashSurplusPaise', 'Net cash surplus'), align: 'right', render: (r) => rupeesCompact(r.netOperatingCashSurplusPaise) },
              { key: 'cy', label: header('cashYieldPct', 'Cash yield'), align: 'right', render: (r) => pct(r.cashYieldPct) },
              { key: 'os', label: header('outstandingPaise', 'Outstanding'), align: 'right', render: (r) => (r.outstandingPaise ? rupeesCompact(r.outstandingPaise) : '—') },
            ]}
          />
        </div>
        <HowCalculated>
          <p>Cash yield = net operating cash surplus × 365 ÷ days in period ÷ acquisition value × 100. It is a cash measure, not a cap rate.</p>
          <p>Rent per sq ft = contract monthly rent of leases active on the analysis date ÷ their leased area.</p>
          <p>Outstanding is the balance owed as of the analysis date on all obligations due by then.</p>
        </HowCalculated>
      </Card>

      <div className="mt-5 grid gap-5 xl:grid-cols-2">
        <ChartCard
          title="Occupancy vs collection efficiency"
          subtitle="One dot per property (time-weighted occupancy; cohort collection efficiency)"
          loading={table.loading}
          height={300}
          table={{
            columns: [
              { key: 'p', label: 'Property', render: (r) => `${r.propertyId} ${r.name}` },
              { key: 'o', label: 'Occupancy (TW)', align: 'right', render: (r) => pct(r.timeWeightedOccupancyPct) },
              { key: 'c', label: 'Collection eff.', align: 'right', render: (r) => pct(r.collectionEfficiencyPct) },
            ],
            rows,
          }}
        >
          <ResponsiveContainer>
            <ScatterChart margin={{ top: 8, right: 16, left: 4, bottom: 8 }}>
              <CartesianGrid stroke={GRID} />
              <XAxis type="number" dataKey="timeWeightedOccupancyPct" name="Occupancy" unit="%" domain={['dataMin - 2', 100]} {...axisProps} tickFormatter={(v) => Math.round(v)} />
              <YAxis type="number" dataKey="collectionEfficiencyPct" name="Collection efficiency" unit="%" domain={['dataMin - 1', 100]} {...axisProps} width={44} tickFormatter={(v) => Math.round(v)} />
              <ZAxis range={[64, 64]} />
              <Tooltip
                cursor={{ stroke: GRID }}
                content={({ active, payload }) =>
                  active && payload?.length ? (
                    <div className="rounded-md border border-line bg-white px-3 py-2 text-xs shadow-sm">
                      <div className="font-semibold text-ink">{payload[0].payload.propertyId} · {payload[0].payload.name}</div>
                      <div className="text-muted">{titleCase(payload[0].payload.type)}</div>
                      <div className="num mt-1">Occupancy {pct(payload[0].payload.timeWeightedOccupancyPct)}</div>
                      <div className="num">Collection {pct(payload[0].payload.collectionEfficiencyPct)}</div>
                    </div>
                  ) : null
                }
              />
              <Scatter data={rows.filter((r) => r.collectionEfficiencyPct !== null && r.timeWeightedOccupancyPct !== null)} fill={SERIES[0]} stroke="#ffffff" strokeWidth={2} onClick={(p) => navigate(`/properties/${p.propertyId}`)} />
            </ScatterChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard
          title={`Net operating cash surplus by ${dimension === 'type' ? 'property type' : 'zone'}`}
          subtitle={
            <span className="inline-flex gap-2">
              Group by
              {['type', 'zone'].map((d) => (
                <button key={d} type="button" onClick={() => setDimension(d)} className={dimension === d ? 'font-semibold text-navy-700 underline' : 'text-navy-600'}>
                  {d}
                </button>
              ))}
            </span>
          }
          loading={segments.loading}
          height={300}
          table={{
            columns: [
              { key: 'key', label: dimension === 'type' ? 'Type' : 'Zone', render: (s) => titleCase(s.key) },
              { key: 'n', label: 'Properties', align: 'right', render: (s) => s.propertyCount },
              { key: 'ce', label: 'Collection eff.', align: 'right', render: (s) => pct(s.collectionEfficiencyPct) },
              { key: 'oc', label: 'Occupancy (TW)', align: 'right', render: (s) => pct(s.timeWeightedOccupancyPct) },
              { key: 'er', label: 'Expense ratio', align: 'right', render: (s) => pct(s.expenseRatioPct) },
              { key: 'ns', label: 'Net surplus', align: 'right', render: (s) => rupees(s.netOperatingCashSurplusPaise) },
            ],
            rows: segments.data?.segments ?? [],
          }}
        >
          <ResponsiveContainer>
            <BarChart data={segments.data?.segments ?? []} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRID} horizontal={false} />
              <XAxis type="number" {...axisProps} tickFormatter={rupeesAxis} />
              <YAxis type="category" dataKey="key" {...axisProps} width={150} tickFormatter={titleCase} />
              <Tooltip
                cursor={{ fill: '#f1f5f9' }}
                content={({ active, payload }) =>
                  active && payload?.length ? (
                    <div className="rounded-md border border-line bg-white px-3 py-2 text-xs shadow-sm">
                      <div className="num font-semibold text-ink">{rupeesCompact(payload[0].value)}</div>
                      <div className="text-muted">{titleCase(payload[0].payload.key)} · {payload[0].payload.propertyCount} properties</div>
                      <div className="num text-muted">Collection {pct(payload[0].payload.collectionEfficiencyPct)}, expense ratio {pct(payload[0].payload.expenseRatioPct)}</div>
                    </div>
                  ) : null
                }
              />
              <Bar dataKey="netOperatingCashSurplusPaise" name="Net surplus" maxBarSize={24} radius={[0, 4, 4, 0]} fill={POS}
                shape={(props) => <rect x={props.x} y={props.y} width={Math.max(props.width, 0)} height={props.height} rx={4} fill={props.value < 0 ? NEG : POS} />} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>
      <p className="mt-4 text-xs text-muted">
        Synthetic data. Comparisons describe this generated portfolio and do not imply causes. See <Link className="underline" to="/methodology">Methodology</Link>.
      </p>
    </>
  )
}
