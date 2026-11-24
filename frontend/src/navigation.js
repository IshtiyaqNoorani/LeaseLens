// Application modules (PROJECT_PLAN.md section 6). `engine` names what produces the results.
export const MODULES = [
  { path: '/overview', label: 'Portfolio Overview' },
  { path: '/properties', label: 'Property Performance' },
  { path: '/cash-flow', label: 'Cash-Flow Explorer', engine: 'Segment Tree' },
  { path: '/payments', label: 'Payment Behaviour', engine: 'Randomized Quickselect' },
  { path: '/expenses', label: 'Operating Expenses' },
  { path: '/lab', label: 'Algorithm Lab' },
  { path: '/data', label: 'Data Workspace' },
  { path: '/methodology', label: 'Methodology' },
]
