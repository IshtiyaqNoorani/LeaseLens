import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { qs } from '../api/client.js'
import AlgorithmPanel from '../components/AlgorithmPanel.jsx'
import { ChartCard, ChartTooltip } from '../components/chart.jsx'
import { GRID, SERIES, axisProps } from '../lib/chartTokens.js'
import DateRangeFilter from '../components/DateRangeFilter.jsx'
import { Card, DataTable, Field, Hero, HowCalculated, Note, PageHeader, ProblemBanner, Select } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, number, pct, rupees, rupeesAxis, rupeesCompact, titleCase } from '../lib/format.js'
import { useApi } from '../lib/useApi.js'

export default function Expenses() {
  const { version } = useDataset()
  const [range, setRange] = useState({})
  const [propertyId, setPropertyId] = useState('')
  const [fence, setFence] = useState('3')
  const properties = useApi('/properties/performance?sort=propertyId', version)
  const summary = useApi(`/expenses/summary${qs({ ...range, propertyId })}`, version)
  const unusual = useApi(`/expenses/unusual${qs({ ...range, fence })}`, version)
  const s = summary.data
  const u = unusual.data

  return (
    <>
      <PageHeader title="Operating expenses" subtitle="Where the money goes, by category, property and month (cash basis), and which individual records are unusually large compared with each property's own history." />
      <div className="mb-5 flex flex-wrap items-end gap-4">
        <Field label="Property">
          <Select value={propertyId} onChange={(e) => setPropertyId(e.target.value)}>
            <option value="">All properties</option>
            {(properties.data?.rows ?? []).map((p) => <option key={p.propertyId} value={p.propertyId}>{p.propertyId} · {p.name}</option>)}
          </Select>
        </Field>
        <DateRangeFilter value={range} onChange={setRange} />
      </div>
      <ProblemBanner error={summary.error} onRetry={summary.reload} />

      {s && (
        <div className={`space-y-5 ${summary.loading ? 'opacity-60' : ''}`}>
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
            <Hero label="Operating expenses" value={rupeesCompact(s.totalPaise)} note={`${date(s.from)} to ${date(s.to)}${s.propertyId ? ` · ${s.propertyId}` : ' · all properties'}`} />
            <ChartCard
              title="By category"
              loading={summary.loading}
              height={220}
              table={{ columns: [{ key: 'category', label: 'Category', render: (c) => titleCase(c.category) }, { key: 'p', label: 'Amount', align: 'right', render: (c) => rupees(c.paise) }, { key: 's', label: 'Share', align: 'right', render: (c) => pct(c.sharePct) }], rows: s.byCategory }}
            >
              <ResponsiveContainer>
                <BarChart data={s.byCategory} layout="vertical" margin={{ top: 0, right: 16, left: 8, bottom: 0 }}>
                  <CartesianGrid stroke={GRID} horizontal={false} />
                  <XAxis type="number" {...axisProps} tickFormatter={rupeesAxis} />
                  <YAxis type="category" dataKey="category" {...axisProps} width={110} tickFormatter={titleCase} />
                  <Tooltip content={<ChartTooltip formatLabel={(l) => titleCase(l)} formatValue={(x) => rupeesCompact(x)} />} cursor={{ fill: '#f1f5f9' }} />
                  <Bar dataKey="paise" name="Amount" fill={SERIES[0]} maxBarSize={18} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          <ChartCard
            title="Monthly operating expenses"
            subtitle="Maintenance and repairs rise in some months; compare with the category table"
            loading={summary.loading}
            table={{ columns: [{ key: 'month', label: 'Month' }, { key: 't', label: 'Total', align: 'right', render: (m) => rupees(m.totalPaise) }], rows: s.monthly }}
          >
            <ResponsiveContainer>
              <LineChart data={s.monthly} margin={{ top: 8, right: 12, left: 4, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="month" {...axisProps} minTickGap={24} />
                <YAxis {...axisProps} tickFormatter={rupeesAxis} width={56} />
                <Tooltip content={<ChartTooltip formatValue={(x) => rupeesCompact(x)} />} cursor={{ stroke: GRID }} />
                <Line type="monotone" dataKey="totalPaise" name="Operating expenses" stroke={SERIES[0]} strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>

          <Card title="By property" subtitle="Highest spend first. Expense-to-rent = operating expenses ÷ rent collected in the period.">
            <DataTable
              maxHeight={340}
              rowKey={(r) => r.propertyId}
              rows={s.byProperty}
              columns={[
                { key: 'p', label: 'Property', render: (r) => `${r.propertyId} · ${r.name}` },
                { key: 't', label: 'Type', render: (r) => titleCase(r.type) },
                { key: 'o', label: 'Expenses', align: 'right', render: (r) => rupeesCompact(r.operatingExpensesPaise) },
                { key: 'c', label: 'Rent collected', align: 'right', render: (r) => rupeesCompact(r.rentCollectedPaise) },
                { key: 'r', label: 'Expense-to-rent', align: 'right', render: (r) => pct(r.expenseToRentPct) },
                { key: 's', label: 'Per sq ft', align: 'right', render: (r) => (r.expensePerSqftRupees === null ? 'n/a' : `₹${r.expensePerSqftRupees.toFixed(2)}`) },
              ]}
            />
          </Card>
        </div>
      )}

      <Card
        className="mt-5"
        title="Unusually large expense records"
        subtitle="Compared with the same property's history in the same category, up to the analysis date"
        actions={
          <Field label="Fence">
            <Select value={fence} onChange={(e) => setFence(e.target.value)}>
              <option value="3">Q3 + 3 × IQR (far out)</option>
              <option value="1.5">Q3 + 1.5 × IQR (classic)</option>
            </Select>
          </Field>
        }
      >
        <ProblemBanner error={unusual.error} />
        {u && (
          <div className={`space-y-4 ${unusual.loading ? 'opacity-60' : ''}`}>
            <p className="text-sm text-muted">
              {number(u.records.length)} record(s) flagged across {number(u.groupsAnalysed)} property × category groups
              ({number(u.groupsSkippedTooSmall)} groups skipped: fewer than 8 records).
            </p>
            <AlgorithmPanel run={u.algorithm} note="Quartiles (P25, P50, P75) of every group are selected with Randomized Quickselect (fixed seed, so flags are reproducible)." />
            <DataTable
              maxHeight={360}
              rowKey={(r) => r.expenseId}
              rows={u.records}
              emptyText="No record is above the fence in this period."
              columns={[
                { key: 'e', label: 'Expense', render: (r) => r.expenseId },
                { key: 'p', label: 'Property', render: (r) => `${r.propertyId} · ${r.propertyName}` },
                { key: 'c', label: 'Category', render: (r) => titleCase(r.category) },
                { key: 'd', label: 'Date', render: (r) => date(r.date) },
                { key: 'a', label: 'Amount', align: 'right', render: (r) => rupees(r.amountPaise) },
                { key: 'x', label: '× median', align: 'right', render: (r) => (r.timesTypical === null ? 'n/a' : `${r.timesTypical.toFixed(1)}×`) },
                { key: 'f', label: 'Fence', align: 'right', render: (r) => rupees(r.upperFencePaise) },
                { key: 'g', label: 'History', align: 'right', render: (r) => `${r.groupSize} records` },
              ]}
            />
            <Note tone="warn">{u.note} A large repair can be entirely legitimate; this list is a prompt for review, not evidence of fraud.</Note>
          </div>
        )}
        <HowCalculated>
          <p>For each property and category: Q1 and Q3 are the nearest-rank 25th and 75th percentiles of that group's amounts; IQR = Q3 − Q1; fence = Q3 + k × IQR.</p>
          <p>k = 3 is the default because money amounts are right-skewed; the classic k = 1.5 flags many ordinary records.</p>
        </HowCalculated>
      </Card>
    </>
  )
}
