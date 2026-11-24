/** Placeholder shown until a module is built. States plainly that nothing is implemented yet. */
export default function PlannedModule({ module }) {
  return (
    <section className="rounded-lg border border-line bg-surface p-8">
      <h1 className="text-xl font-semibold text-ink">{module.label}</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted">
        Not built yet. This screen will display results produced by the Java backend
        ({module.engine}). The algorithm or service is scheduled for Phase {module.phase}; the screen
        itself is built in Phase 5.
      </p>
    </section>
  )
}
