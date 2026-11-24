// Application modules (PROJECT_PLAN.md section 6). `engine` names the Java component that produces results.
export const MODULES = [
  { path: '/data', label: 'Data Workspace', engine: 'CSV ingestion and validation', phase: 3 },
  { path: '/overview', label: 'Portfolio Overview', engine: 'KPI service', phase: 4 },
  { path: '/properties', label: 'Property Performance', engine: 'Property analytics service', phase: 4 },
  { path: '/cash-flow', label: 'Cash-Flow Explorer', engine: 'Segment Tree (Unit 3)', phase: 2 },
  { path: '/payments', label: 'Payment Behaviour', engine: 'Randomized Quickselect (Unit 6, CO5)', phase: 2 },
  { path: '/expenses', label: 'Operating Expenses', engine: 'Expense analytics service', phase: 4 },
  { path: '/lab', label: 'Algorithm Lab', engine: 'Benchmark harness', phase: 6 },
  { path: '/methodology', label: 'Methodology', engine: 'Formula registry', phase: 5 },
]
