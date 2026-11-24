import { Card, Note, PageHeader } from '../components/ui.jsx'

/** Placeholder until Phase 6 adds the benchmark harness and its committed results. */
export default function Lab() {
  return (
    <>
      <PageHeader
        title="Algorithm performance lab"
        subtitle="Measured comparisons of the hand-written algorithms against baselines at 1,000, 10,000 and 100,000 inputs."
      />
      <Card title="Not built yet (Phase 6)">
        <div className="space-y-3 text-sm text-ink">
          <p>This screen will show only measured results, read from committed benchmark CSV files:</p>
          <ul className="list-disc space-y-1 pl-5 text-muted">
            <li>Segment Tree vs linear scan vs prefix sums, for query-only and mixed query/correction workloads.</li>
            <li>Randomized Quickselect vs full sorting, one percentile vs many (the crossover point).</li>
            <li>Random pivot vs fixed pivot on sorted input, and three-way vs two-way partitioning on duplicate-heavy delays.</li>
            <li>Distribution of comparison counts across random seeds (CO5).</li>
          </ul>
          <Note>No benchmark numbers are shown until they have been measured. Correctness evidence already exists: run <code>./mvnw verify</code>.</Note>
        </div>
      </Card>
    </>
  )
}
