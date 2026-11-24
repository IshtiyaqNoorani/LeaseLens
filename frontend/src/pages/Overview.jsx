import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { qs } from '../api/client.js'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { GRID, SERIES, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Badge, Card, DataTable, Hero, HowCalculated, PageHeader, ProblemBanner, Stat } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, number, pct, rupees, rupeesAxis, rupeesCompact, titleCase } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

export default function Overview() {
  const { version } = useDataset()
  const [range, setRange] = useState({})
  const { data: o, error, loading, reload } = useApi(`/portfolio/overview${qs(range)}`, version)

  return (
    <>
      <PageHeader
        title="Portfolio overview"
        subtitle="Financial and operational KPIs for the whole portfolio. Every figure is calculated in Java from the loaded data."
      />
      <div className="mb-5">
        <DateRangeFilter value={range} onChange={setRange} />
      </div>
      <ProblemBanner error={error} onRetry={reload} />
      {o && (
        <div className={`space-y-5 transition-opacity ${loading ? 'opacity-60' : ''}`}>
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,2fr)]">
            <Hero
              label="Net operating cash surplus"
              value={rupeesCompact(o.netOperatingCashSurplusPaise)}
              note={`Rent collected minus operating expenses, ${date(o.from)} to ${date(o.to)}. Cash basis: not NOI and not an audited measure.`}
            />
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Stat label="Rent billed (accrual)" value={rupeesCompact(o.rentBilledPaise)} />
              <Stat label="Rent collected (cash)" value={rupeesCompact(o.rentCollectedPaise)} />
              <Stat label="Collection efficiency" value={pct(o.collectionEfficiencyPct)} note="Cohort basis" />
              <Stat label="Outstanding receivables" value={rupeesCompact(o.outstandingPaise)} note={`As of ${date(o.analysisDate)}`} />
              <Stat label="Operating expenses" value={rupeesCompact(o.operatingExpensesPaise)} />
              <Stat label="Expense ratio" value={pct(o.expenseRatioPct)} note="Expenses ÷ rent collected" />
              <Stat label="Occupancy" value={pct(o.occupancyPct)} note={`On ${date(o.analysisDate)}`} />
              <Stat label="Time-weighted occupancy" value={pct(o.timeWeightedOccupancyPct)} note="Over the period" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Stat label="Properties" value={number(o.propertyCount)} />
            <Stat label="Rentable area" value={`${number(o.rentableAreaSqft)} sq ft`} />
            <Stat label="Contract rent per sq ft" value={o.rentPerSqftRupees === null ? 'n/a' : `₹${o.rentPerSqftRupees.toFixed(2)}`} note="Per month, active leases" />
            <Stat label="Properties needing attention" value={number(new Set(o.attention.map((a) => a.propertyId)).size)} note="See rules below" />
          </div>

          <div className="grid gap-5 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <ChartCard
              title="Monthly rent billed, collected and operating expenses"
              subtitle="Billed is accrual (due in month); collected and expenses are cash (dated in month)"
              loading={loading}
              legend={[
                { label: 'Rent billed', color: SERIES[0] },
                { label: 'Rent collected', color: SERIES[1] },
                { label: 'Operating expenses', color: SERIES[2] },
              ]}
              table={{
                columns: [
                  { key: 'month', label: 'Month' },
                  { key: 'b', label: 'Billed', align: 'right', render: (r) => rupees(r.rentBilledPaise) },
                  { key: 'c', label: 'Collected', align: 'right', render: (r) => rupees(r.rentCollectedPaise) },
                  { key: 'e', label: 'Expenses', align: 'right', render: (r) => rupees(r.operatingExpensesPaise) },
                  { key: 's', label: 'Surplus', align: 'right', render: (r) => rupees(r.netOperatingCashSurplusPaise) },
                ],
                rows: o.monthlyTrend,
              }}
            >
              <ResponsiveContainer>
                <LineChart data={o.monthlyTrend} margin={{ top: 8, right: 12, left: 4, bottom: 0 }}>
                  <CartesianGrid stroke={GRID} vertical={false} />
                  <XAxis dataKey="month" {...axisProps} minTickGap={24} />
                  <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                  <Tooltip content={<ChartTooltip formatValue={(v) => rupeesCompact(v)} />} cursor={{ stroke: GRID }} />
                  <Line isAnimationActive={false} type="monotone" dataKey="rentBilledPaise" name="Rent billed" stroke={SERIES[0]} strokeWidth={2} dot={false} />
                  <Line isAnimationActive={false} type="monotone" dataKey="rentCollectedPaise" name="Rent collected" stroke={SERIES[1]} strokeWidth={2} dot={false} />
                  <Line isAnimationActive={false} type="monotone" dataKey="operatingExpensesPaise" name="Operating expenses" stroke={SERIES[2]} strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </ChartCard>

            <ChartCard
              title="Receivables ageing"
              subtitle={`Open balances by days overdue, as of ${date(o.analysisDate)}`}
              loading={loading}
              table={{
                columns: [
                  { key: 'bucket', label: 'Days overdue' },
                  { key: 'count', label: 'Obligations', align: 'right' },
                  { key: 'paise', label: 'Balance', align: 'right', render: (r) => rupees(r.paise) },
                ],
                rows: o.receivablesAgeing,
              }}
            >
              <ResponsiveContainer>
                <BarChart data={o.receivablesAgeing} margin={{ top: 8, right: 8, left: 4, bottom: 0 }}>
                  <CartesianGrid stroke={GRID} vertical={false} />
                  <XAxis dataKey="bucket" {...axisProps} />
                  <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                  <Tooltip content={<ChartTooltip formatValue={(v) => rupeesCompact(v)} formatLabel={(l) => `${l} days overdue`} />} cursor={{ fill: '#f1f5f9' }} />
                  <Bar isAnimationActive={false} dataKey="paise" name="Balance" fill={SERIES[0]} maxBarSize={24} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          <Card
            title="Properties requiring attention"
            subtitle="Transparent rules with configurable thresholds. A flag is a prompt to investigate, not a conclusion."
          >
            <DataTable
              emptyText="No property breaks any rule in this period."
              rowKey={(a) => `${a.propertyId}-${a.rule}`}
              columns={[
                { key: 'p', label: 'Property', render: (a) => <Link className="font-medium text-navy-700 hover:underline" to={`/properties/${a.propertyId}`}>{a.propertyId} · {a.propertyName}</Link> },
                { key: 'rule', label: 'Rule', render: (a) => <Badge tone="warn">{titleCase(a.rule)}</Badge> },
                { key: 'msg', label: 'Detail', render: (a) => <span className="whitespace-normal text-muted">{a.message}</span> },
              ]}
              rows={o.attention}
            />
            <HowCalculated>
              <p>Collection efficiency (cohort) = rent paid by the analysis date on obligations due in the period ÷ rent billed in the period × 100.</p>
              <p>Time-weighted occupancy = leased area × days leased ÷ (rentable area × days in period) × 100.</p>
              <p>Expense ratio = operating expenses ÷ rent collected × 100.</p>
              <p>Long-overdue share = balances more than 90 days overdue ÷ rent billed in the period × 100.</p>
              <p>Thresholds are set in application.properties (leaselens.attention.*).</p>
            </HowCalculated>
          </Card>
        </div>
      )}
    </>
  )
}
