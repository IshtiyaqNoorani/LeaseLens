import { micros, number } from '../lib/format.js'

/**
 * Shows which hand-written Java algorithm produced the result on screen and what it cost.
 * Everything comes from the API's `algorithm` block.
 */
export default function AlgorithmPanel({ run, note }) {
  if (!run) return null
  const unitLabel = run.syllabusUnit === 3 ? 'Unit 3 · trees and range queries' : 'Unit 6 · randomization (CO5)'
  const opLabel = run.name === 'SegmentTree' ? 'tree nodes visited' : 'element comparisons'
  return (
    <div className="rounded-lg border border-navy-100 bg-navy-50/60 px-4 py-3 text-xs text-navy-900">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span className="font-semibold">Computed by {run.name === 'SegmentTree' ? 'Segment Tree' : 'Randomized Quickselect'}</span>
        <span className="text-navy-600">{unitLabel}</span>
        <span className="text-navy-600">· hand-written Java</span>
      </div>
      <dl className="mt-2 grid grid-cols-2 gap-x-6 gap-y-1 sm:grid-cols-3 lg:grid-cols-6">
        <Item label="Operation" value={run.operation} />
        <Item label="Complexity" value={run.timeComplexity} />
        <Item label="n" value={number(run.n)} />
        <Item label={opLabel} value={number(run.operations)} />
        <Item label="Server time" value={micros(run.elapsedNanos)} />
        {run.seed !== null && run.seed !== undefined && <Item label="Random seed" value={String(run.seed)} />}
      </dl>
      {note && <p className="mt-2 leading-relaxed text-navy-700">{note}</p>}
    </div>
  )
}

function Item({ label, value }) {
  return (
    <div>
      <dt className="text-[11px] text-navy-600">{label}</dt>
      <dd className="num font-medium">{value}</dd>
    </div>
  )
}
