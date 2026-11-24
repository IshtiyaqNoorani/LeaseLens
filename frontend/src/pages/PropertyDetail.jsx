import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { qs } from '../api/client.js'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { GRID, SERIES, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Badge, Card, DataTable, PageHeader, ProblemBanner, Stat } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, days, number, pct, rupees, rupeesAxis, rupeesCompact, titleCase } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

export default function PropertyDetail() {
  const { id } = useParams()
  const { version } = useDataset()
  const [range, setRange] = useState({})
  const { data, error, loading, reload } = useApi(`/properties/${encodeURIComponent(id)}${qs(range)}`, version)
  const m = data?.metrics

  return (
    <>
      <PageHeader title={m ? `${m.propertyId} · ${m.name}` : id} subtitle={m ? `${titleCase(m.type)} · ${m.zone} · ${number(m.rentableAreaSqft)} sq ft · ${data.totalUnits} unit(s) · acquired for ${rupeesCompact(data.acquisitionValuePaise)}` : undefined}>
        <div className="flex gap-3 text-sm">
          <Link className="text-navy-700 underline" to={`/cash-flow?scope=${id}`}>Cash flow</Link>
          <Link className="text-navy-700 underline" to={`/payments?propertyId=${id}`}>Payment behaviour</Link>
          <Link className="text-navy-700 underline" to="/properties">All properties</Link>
        </div>
      </PageHeader>
      <div className="mb-5">
        <DateRangeFilter value={range} onChange={setRange} />
      </div>
      <ProblemBanner error={error} onRetry={reload} />
      {m && (
        <div className={`space-y-5 ${loading ? 'opacity-60' : ''}`}>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-6">
            <Stat label="Net operating cash surplus" value={rupeesCompact(m.netOperatingCashSurplusPaise)} note="Cash basis" />
            <Stat label="Rent billed" value={rupeesCompact(m.rentBilledPaise)} />
            <Stat label="Rent collected" value={rupeesCompact(m.rentCollectedPaise)} />
            <Stat label="Collection efficiency" value={pct(m.collectionEfficiencyPct)} />
            <Stat label="Outstanding" value={rupeesCompact(m.outstandingPaise)} note={m.maxDaysOverdue ? `Oldest ${days(m.maxDaysOverdue)} overdue` : 'Nothing overdue'} />
            <Stat label="Cash yield" value={pct(m.cashYieldPct)} note="Annualised" />
            <Stat label="Occupancy now" value={pct(m.occupancyPct)} />
            <Stat label="Time-weighted occupancy" value={pct(m.timeWeightedOccupancyPct)} />
            <Stat label="Vacant area-days" value={number(m.vacantAreaDays)} />
            <Stat label="Operating expenses" value={rupeesCompact(m.operatingExpensesPaise)} />
            <Stat label="Expense ratio" value={pct(m.expenseRatioPct)} />
            <Stat label="Rent per sq ft" value={m.rentPerSqftRupees === null ? 'n/a' : `₹${m.rentPerSqftRupees.toFixed(2)}`} note="Monthly, active leases" />
          </div>

          <ChartCard
            title="Monthly rent billed, collected and operating expenses"
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
              ],
              rows: data.monthlyTrend,
            }}
          >
            <ResponsiveContainer>
              <LineChart data={data.monthlyTrend} margin={{ top: 8, right: 12, left: 4, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="month" {...axisProps} minTickGap={24} />
                <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                <Tooltip content={<ChartTooltip formatValue={(v) => rupeesCompact(v)} />} cursor={{ stroke: GRID }} />
                <Line type="monotone" dataKey="rentBilledPaise" name="Rent billed" stroke={SERIES[0]} strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="rentCollectedPaise" name="Rent collected" stroke={SERIES[1]} strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="operatingExpensesPaise" name="Operating expenses" stroke={SERIES[2]} strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>

          <Card title="Leases" subtitle={`State on the analysis date. Tenants are anonymous identifiers.`}>
            <DataTable
              rowKey={(l) => l.leaseId}
              rows={data.leases}
              columns={[
                { key: 'leaseId', label: 'Lease' },
                { key: 'tenantId', label: 'Tenant' },
                { key: 's', label: 'Start', render: (l) => date(l.startDate) },
                { key: 'e', label: 'End', render: (l) => date(l.endDate) },
                { key: 'a', label: 'Area', align: 'right', render: (l) => `${number(l.leasedAreaSqft)} sq ft` },
                { key: 'r', label: 'Monthly rent', align: 'right', render: (l) => rupees(l.monthlyRentPaise) },
                { key: 'd', label: 'Due day', align: 'right', render: (l) => l.billingDay },
                { key: 'st', label: 'State', render: (l) => <Badge tone={l.stateOnAnalysisDate === 'ACTIVE' ? 'gain' : 'neutral'}>{titleCase(l.stateOnAnalysisDate)}</Badge> },
              ]}
            />
          </Card>
        </div>
      )}
    </>
  )
}
