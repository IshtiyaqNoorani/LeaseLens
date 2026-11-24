// Display formatting only. Amounts arrive from the API in paise (integers); percentages arrive rounded.

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
const inr2 = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2 })
const int = new Intl.NumberFormat('en-IN')

/** Full rupee amount with Indian digit grouping, e.g. 4,05,000 paise -> "₹4,050". */
export function rupees(paise, { decimals = false } = {}) {
  if (paise === null || paise === undefined) return 'n/a'
  return (decimals ? inr2 : inr).format(paise / 100)
}

/** Compact rupees: ₹1.23 Cr, ₹4.56 L, ₹12,345. */
export function rupeesCompact(paise) {
  if (paise === null || paise === undefined) return 'n/a'
  const r = paise / 100
  const sign = r < 0 ? '−' : ''
  const a = Math.abs(r)
  if (a >= 1e7) return `${sign}₹${(a / 1e7).toFixed(2)} Cr`
  if (a >= 1e5) return `${sign}₹${(a / 1e5).toFixed(2)} L`
  return `${sign}₹${int.format(Math.round(a))}`
}

/** Axis ticks: short rupee labels. */
export function rupeesAxis(paise) {
  const r = paise / 100
  const a = Math.abs(r)
  const sign = r < 0 ? '−' : ''
  if (a >= 1e7) return `${sign}${+(a / 1e7).toFixed(1)} Cr`
  if (a >= 1e5) return `${sign}${+(a / 1e5).toFixed(1)} L`
  if (a >= 1e3) return `${sign}${+(a / 1e3).toFixed(0)}K`
  return `${sign}${a}`
}

export function pct(value) {
  return value === null || value === undefined ? 'n/a' : `${value.toFixed(2)}%`
}

export function number(value) {
  return value === null || value === undefined ? 'n/a' : int.format(value)
}

export function days(value) {
  if (value === null || value === undefined) return 'n/a'
  return `${int.format(value)} ${value === 1 ? 'day' : 'days'}`
}

const dateFmt = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
export function date(iso) {
  if (!iso) return 'n/a'
  return dateFmt.format(new Date(`${iso}T00:00:00`))
}

export function micros(nanos) {
  if (nanos === null || nanos === undefined) return 'n/a'
  return nanos < 1000 ? `${nanos} ns` : nanos < 1e6 ? `${(nanos / 1000).toFixed(1)} µs` : `${(nanos / 1e6).toFixed(2)} ms`
}

export function titleCase(text) {
  return text ? text.toLowerCase().replace(/(^|_)(\w)/g, (_, s, c) => (s ? ' ' : '') + c.toUpperCase()) : ''
}

/** Adds (or subtracts) days to an ISO date string. */
export function addDays(iso, n) {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + n)
  return d.toISOString().slice(0, 10)
}

export function addMonths(iso, n) {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCMonth(d.getUTCMonth() + n)
  return d.toISOString().slice(0, 10)
}
