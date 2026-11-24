import { Card, DataTable, Note, PageHeader } from '../components/ui.jsx'

const KPIS = [
  ['Occupancy (point in time)', 'Σ leased area of leases active on the analysis date ÷ Σ rentable area × 100', 'Physical'],
  ['Time-weighted occupancy', 'Σ leased area × days leased in the period ÷ (Σ rentable area × days in period) × 100', 'Physical'],
  ['Rent billed', 'Σ amount due on obligations falling due in the period', 'Accrual'],
  ['Rent collected', 'Σ payments dated in the period', 'Cash'],
  ['Collection efficiency', 'Σ paid by the analysis date on obligations due in the period ÷ rent billed × 100', 'Cohort'],
  ['Cash-to-billing ratio', 'Rent collected ÷ rent billed × 100 (can exceed 100% because of timing)', 'Mixed'],
  ['Outstanding receivables', 'Σ (due − paid) on obligations due by the analysis date, as of that date; aged 0–30 / 31–60 / 61–90 / 90+ days', 'Accrual'],
  ['Operating expenses', 'Σ expenses dated in the period', 'Cash'],
  ['Net operating cash surplus', 'Rent collected − operating expenses. Not NOI and not an audited measure: ignores capex, debt, tax and depreciation', 'Cash'],
  ['Expense ratio', 'Operating expenses ÷ rent collected × 100', 'Cash'],
  ['Rent per sq ft', 'Contract monthly rent of active leases ÷ their leased area', 'Contract'],
  ['Cash yield', 'Net operating cash surplus × 365 ÷ days in period ÷ acquisition value × 100. Not a cap rate', 'Cash'],
  ['Days late', 'Settled date − due date, floored at 0, for obligations paid in full by the analysis date', 'Behavioural'],
  ['Days overdue', 'Analysis date − due date, for obligations still open', 'Behavioural'],
].map(([name, formula, basis]) => ({ name, formula, basis }))

export default function Methodology() {
  return (
    <>
      <PageHeader title="Methodology" subtitle="How every number in LeaseLens is produced, and what it does not mean." />
      <div className="space-y-5">
        <Card title="KPI formulas" subtitle="All calculated in Java (leaselens-api); a ratio with a zero denominator is shown as n/a, never 0">
          <DataTable
            rowKey={(r) => r.name}
            rows={KPIS}
            columns={[
              { key: 'name', label: 'Measure', render: (r) => <span className="font-medium">{r.name}</span> },
              { key: 'formula', label: 'Formula', render: (r) => <span className="whitespace-normal">{r.formula}</span> },
              { key: 'basis', label: 'Basis' },
            ]}
          />
        </Card>

        <div className="grid gap-5 lg:grid-cols-2">
          <Card title="Segment Tree (syllabus Unit 3)">
            <div className="space-y-2 text-sm leading-relaxed">
              <p>One tree per property and one for the portfolio, over daily net cash flow. Each node stores the sum, minimum and maximum of its interval, plus the date of the extreme (earliest on ties).</p>
              <p>Range query O(log n) · point update O(log n) · build O(n) · space O(n). Sums are overflow-checked.</p>
              <p className="text-muted">Used by: Cash-Flow Explorer (range totals, worst and best day, periods, comparisons, corrections).</p>
            </div>
          </Card>
          <Card title="Randomized Quickselect (syllabus Unit 6, CO5)">
            <div className="space-y-2 text-sm leading-relaxed">
              <p>Finds the k-th smallest value with a uniformly random pivot and a three-way partition (efficient when many payments are 0 days late). Percentiles use the nearest-rank rule k = ⌈P/100 × n⌉ − 1, computed in exact integer arithmetic.</p>
              <p>Expected O(n), worst case O(n²). Las Vegas: always correct; only the running time depends on the seed.</p>
              <p className="text-muted">Used by: Payment Behaviour (percentiles, thresholds) and expense quartiles (unusual records).</p>
            </div>
          </Card>
        </div>

        <Card title="Data, assumptions and limitations">
          <ul className="list-disc space-y-1.5 pl-5 text-sm leading-relaxed">
            <li>The bundled data is synthetic, generated from a fixed seed. Patterns were planted deliberately and are documented in data/GENERATOR_SPEC.md, so findings describe this generated portfolio, not real markets.</li>
            <li>Everything is evaluated as of the analysis date: later payments are ignored and date ranges cannot go past it.</li>
            <li>The delay distribution covers completed payments only. Open obligations are reported separately; excluding them (right-censoring) can understate delays.</li>
            <li>Rent is not pro-rated; there are no deposits or rent-free periods.</li>
            <li>Corrections in the Cash-Flow Explorer are what-if adjustments: they change only that screen, never the source data or the Overview.</li>
            <li>Attention flags and unusual-expense flags are transparent rules that prompt review. They are not predictions, credit judgements or evidence of wrongdoing.</li>
            <li>Correlation between measures (for example expenses and collections) is driven largely by property size and does not imply causation.</li>
          </ul>
          <div className="mt-4">
            <Note>Validation: every uploaded file passes 20 rules (references, dates, amounts, duplicate ids, overlapping leases, overpayments, reconciliation of declared payment fields) before any analysis runs. A dataset with any error is rejected as a whole.</Note>
          </div>
        </Card>
      </div>
    </>
  )
}
