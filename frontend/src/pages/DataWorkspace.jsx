import { useState } from 'react'
import { apiPost, apiPut, apiUpload } from '../api/client.js'
import { Badge, Button, Card, DataTable, Field, Input, Note, PageHeader, ProblemBanner, Stat } from '../components/ui.jsx'
import { useDataset } from '../context/DatasetContext.jsx'
import { date, number, titleCase } from '../lib/format.js'

const FILES = ['properties.csv', 'tenants.csv', 'leases.csv', 'rent_obligations.csv', 'rent_transactions.csv', 'expenses.csv']

export default function DataWorkspace() {
  const { summary, replace } = useDataset()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [report, setReport] = useState(null)
  const [message, setMessage] = useState(null)
  const [files, setFiles] = useState([])
  const [analysisDate, setAnalysisDate] = useState('')

  async function run(action, success) {
    setBusy(true)
    setError(null)
    setReport(null)
    setMessage(null)
    try {
      const s = await action()
      replace(s)
      setMessage(success(s))
    } catch (e) {
      if (e.problem?.validation) setReport(e.problem.validation)
      setError(e)
    } finally {
      setBusy(false)
    }
  }

  const loadBundled = (name) =>
    run(() => apiPost(`/datasets/sample?name=${name}`), (s) => `Loaded the ${s.source} dataset: ${s.counts.properties} properties, ${number(s.counts.obligations)} rent obligations.`)

  const upload = () =>
    run(
      () => {
        const form = new FormData()
        files.forEach((f) => form.append('files', f, f.name))
        return apiUpload('/datasets/upload', form)
      },
      (s) => `Upload accepted: ${s.counts.properties} properties, ${number(s.counts.obligations)} obligations, ${s.warnings.length} warning(s).`,
    )

  const setDate = () =>
    run(() => apiPut('/datasets/current/analysis-date', { analysisDate }), (s) => `Analysis date set to ${date(s.analysisDate)}. All figures are now as of that date.`)

  return (
    <>
      <PageHeader
        title="Data workspace"
        subtitle="Load the bundled synthetic portfolio or upload your own six CSV files. Every file is validated in Java before any analysis runs; a rejected upload never replaces the current data."
      />

      {message && (
        <div role="status" className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-gain">
          {message}
        </div>
      )}
      <ProblemBanner error={error} />
      {report && <ValidationReport report={report} />}

      <div className="grid gap-5 lg:grid-cols-2">
        <Card title="Current dataset" subtitle="What every screen is analysing right now">
          {summary ? (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center gap-2 text-sm">
                <Badge tone="navy">{titleCase(summary.source)}</Badge>
                <span className="text-muted">
                  {date(summary.horizonStart)} to {date(summary.horizonEnd)} ({number(summary.days)} days)
                </span>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <Stat label="Properties" value={number(summary.counts.properties)} />
                <Stat label="Leases" value={number(summary.counts.leases)} />
                <Stat label="Tenants" value={number(summary.counts.tenants)} />
                <Stat label="Rent obligations" value={number(summary.counts.obligations)} />
                <Stat label="Payments" value={number(summary.counts.transactions)} />
                <Stat label="Expenses" value={number(summary.counts.expenses)} />
              </div>
              <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
                <Field label="Analysis date (balances and settlements are 'as of' this date)">
                  <Input
                    type="date"
                    value={analysisDate || summary.analysisDate}
                    min={summary.horizonStart}
                    max={summary.horizonEnd}
                    onChange={(e) => setAnalysisDate(e.target.value)}
                  />
                </Field>
                <Button variant="secondary" disabled={busy || !analysisDate || analysisDate === summary.analysisDate} onClick={setDate}>
                  Apply
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted">No dataset is loaded yet.</p>
          )}
        </Card>

        <div className="space-y-5">
          <Card title="Bundled datasets" subtitle="Packaged with the application; reproducible from a fixed seed">
            <div className="flex flex-wrap gap-3">
              <Button disabled={busy} onClick={() => loadBundled('sample')}>
                Load sample portfolio
              </Button>
              <Button variant="secondary" disabled={busy} onClick={() => loadBundled('golden')}>
                Load golden dataset
              </Button>
            </div>
            <p className="mt-3 text-xs text-muted">
              Sample: 50 properties, 36 months. Golden: 2 properties with every answer worked out by hand in
              data/golden/EXPECTED.md.
            </p>
          </Card>

          <Card title="Upload CSV files" subtitle={`Select all six: ${FILES.join(', ')}`}>
            <div className="flex flex-wrap items-center gap-3">
              <input
                type="file"
                multiple
                accept=".csv,text/csv"
                aria-label="CSV files"
                className="text-sm file:mr-3 file:rounded-md file:border file:border-line file:bg-white file:px-3 file:py-1.5 file:text-sm file:font-medium"
                onChange={(e) => setFiles(Array.from(e.target.files || []))}
              />
              <Button disabled={busy || files.length === 0} onClick={upload}>
                Validate and load
              </Button>
            </div>
            {files.length > 0 && (
              <ul className="mt-3 grid grid-cols-2 gap-1 text-xs">
                {FILES.map((f) => (
                  <li key={f} className={files.some((x) => x.name === f) ? 'text-gain' : 'text-loss'}>
                    {files.some((x) => x.name === f) ? '✓' : '✗'} {f}
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-3">
              <Note>
                To see error handling, upload the files from any folder in <code>data/invalid/</code>. Each has one
                deliberate defect (for example an overpayment or overlapping leases).
              </Note>
            </div>
          </Card>
        </div>
      </div>
    </>
  )
}

function ValidationReport({ report }) {
  return (
    <Card
      className="mb-5"
      title="Validation report"
      subtitle={`${number(report.errorCount)} error(s), ${number(report.warningCount)} warning(s)${report.truncated ? ' (list truncated)' : ''}. Nothing was loaded.`}
    >
      <div className="mb-4">
        <DataTable
          rowKey={(r) => r.file}
          columns={[
            { key: 'file', label: 'File' },
            { key: 'present', label: 'Supplied', render: (r) => (r.present ? 'Yes' : <span className="text-loss">No</span>) },
            { key: 'rows', label: 'Rows', align: 'right', render: (r) => number(r.rows) },
            { key: 'errors', label: 'Errors', align: 'right', render: (r) => <span className={r.errors ? 'font-semibold text-loss' : ''}>{r.errors}</span> },
            { key: 'warnings', label: 'Warnings', align: 'right' },
          ]}
          rows={report.files}
        />
      </div>
      <DataTable
        maxHeight={360}
        columns={[
          { key: 'severity', label: 'Severity', render: (i) => <Badge tone={i.severity === 'ERROR' ? 'loss' : 'warn'}>{titleCase(i.severity)}</Badge> },
          { key: 'rule', label: 'Rule', render: (i) => <code className="text-xs">{i.rule}</code> },
          { key: 'file', label: 'File' },
          { key: 'line', label: 'Line', align: 'right', render: (i) => i.line ?? 'n/a' },
          { key: 'column', label: 'Column', render: (i) => i.column ?? 'n/a' },
          { key: 'message', label: 'What is wrong', render: (i) => <span className="whitespace-normal">{i.message}</span> },
        ]}
        rows={report.issues}
      />
    </Card>
  )
}
