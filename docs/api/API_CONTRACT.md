# LeaseLens REST API Contract (v0.1, Phase 1)

Base URL: `http://localhost:8090/api`. In development the frontend calls `/api/...` through the Vite proxy.
Status: **contract frozen for Phase 4 implementation.** Changes after this point are recorded in DECISIONS.md.
Implemented (Phase 4): every endpoint below except the Algorithm Lab, which comes in Phase 6. Each one is covered by `ApiIntegrationTest`.

## Conventions
- JSON, UTF-8. Dates are ISO-8601 (`2026-10-31`). Inclusive date ranges `from..to`.
- **Money:** integers in **paise** in fields ending `Paise` (e.g. `netCashFlowPaise: 1234500` = ₹12,345.00). The frontend formats for display. No floating-point money.
- Percentages: numbers with 2 decimals in fields ending `Pct`.
- `scope`: `portfolio` or a property id such as `P012`.
- Percentiles: `p` in percent with up to 2 decimals (`90`, `99.5`). Valid range `0 < p ≤ 100`. Internally converted to hundredths (D-006).
- Errors: RFC 7807 `application/problem+json`:
  ```json
  { "type": "about:blank", "title": "Invalid request", "status": 400,
    "detail": "from (2026-11-05) is after to (2026-10-01)", "instance": "/api/cashflow/range" }
  ```
  `400` invalid parameters · `404` unknown property or record · `409` no dataset loaded · `422` uploaded dataset failed validation · `500` unexpected (never with a partial result).
- Every algorithm-backed response contains:
  ```json
  "algorithm": { "name": "SegmentTree", "unit": 3, "operation": "rangeQuery",
                 "timeComplexity": "O(log n)", "n": 1096, "operations": 21,
                 "elapsedNanos": 18400, "seed": null }
  ```
  `operations` = nodes visited (segment tree) or element comparisons (quickselect).

## Health
`GET /health` → `200 {"status":"UP","service":"leaselens-api","version":"0.1.0-SNAPSHOT"}`

## Datasets
| Method | Path | Body / params | Response |
|---|---|---|---|
| POST | `/datasets/sample?name=sample\|golden` | none | `200 DatasetSummary`. Both datasets are bundled in the jar; `sample` is also loaded at start-up unless `leaselens.autoload-sample=false` |
| POST | `/datasets/upload` | multipart: the six CSVs, each matched by file name (`leases.csv`) or form field name (`leases`) | `200 DatasetSummary` (with warnings) or `422` problem whose `validation` property holds the `ValidationReport`. **On 422 the previous dataset stays loaded.** |
| GET | `/datasets/current` | none | `200 DatasetSummary` or `409` |
| PUT | `/datasets/current/analysis-date` | `{"analysisDate":"2026-10-31"}` | `200 DatasetSummary` (must be within horizon) |

`DatasetSummary`
```json
{ "source": "sample", "horizonStart": "2023-11-01", "horizonEnd": "2026-10-31", "days": 1096,
  "analysisDate": "2026-10-31",
  "counts": { "properties": 50, "tenants": 205, "leases": 221, "obligations": 6012, "transactions": 6890, "expenses": 7140 },
  "warnings": [ { "file": "leases.csv", "row": 17, "column": "end_date", "rule": "OBLIGATION_OUTSIDE_LEASE", "message": "..." } ] }
```
(Counts are illustrative. Real values come from the generated data.)

`ValidationReport` (422)
```json
{ "valid": false,
  "errors": [ { "file": "rent_transactions.csv", "row": 42, "column": "obligation_id",
                "rule": "UNKNOWN_REFERENCE", "value": "OB99999",
                "message": "Transaction T000042 refers to obligation OB99999, which does not exist in rent_obligations.csv." } ],
  "warnings": [], "files": [ { "file": "properties.csv", "rows": 50, "errors": 0, "warnings": 0 } ] }
```

## Portfolio and properties
`GET /portfolio/overview?from&to` → KPI block (PROJECT_PLAN §6.1), each as `{ "value": ..., "unit": "paise|pct|sqft|count", "basis": "cash|accrual|cohort|physical", "formulaId": "COLLECTION_EFFICIENCY" }`, plus `receivablesAgeing` buckets, `monthlyTrend[]` and `attention[]` (`{propertyId, rule, observed, threshold, message}`).

`GET /properties/performance?from&to&sort=collectionEfficiencyPct&order=asc` → `{ count, rows: [ { propertyId, name, type, zone, rentableAreaSqft, rentBilledPaise, rentCollectedPaise, cohortPaidPaise, collectionEfficiencyPct, cashToBillingPct, leasedAreaSqft, occupancyPct, timeWeightedOccupancyPct, vacantAreaDays, contractRentMonthlyPaise, rentPerSqftRupees, operatingExpensesPaise, netOperatingCashSurplusPaise, expenseRatioPct, cashYieldPct, outstandingPaise, maxDaysOverdue, longOverduePaise } ] }`. Unknown sort keys give 400. Null ratios (zero denominator) always sort last.

Cash yield = net operating cash surplus × 365 / days in period ÷ acquisition value. It is not a cap rate.

`GET /properties/{id}` → property detail with leases and monthly series.

## Cash-Flow Explorer (Segment Tree)
`GET /cashflow/range?scope=portfolio&from=2025-04-01&to=2026-03-31`
```json
{ "scope": "portfolio", "from": "2025-04-01", "to": "2026-03-31", "days": 365,
  "netCashFlowPaise": 412345600,
  "minDailyPaise": -2345000, "minDate": "2025-07-14",
  "maxDailyPaise": 9876500, "maxDate": "2025-12-05",
  "adjustmentsApplied": 0,
  "algorithm": { "name": "SegmentTree", "unit": 3, "operation": "rangeQuery", "timeComplexity": "O(log n)", "n": 1096, "operations": 23, "elapsedNanos": 15200, "seed": null } }
```
`GET /cashflow/periods?scope&from&to&granularity=MONTH|QUARTER` → `{ periods: [ {label, from, to, netCashFlowPaise, minDailyPaise, minDate, maxDailyPaise, maxDate} ], algorithm: {..., "operation": "rangeQuery×k"} }`

`GET /cashflow/series?scope&from&to` → `{ points: [ {date, netPaise, receiptsPaise, expensesPaise} ] }` (chart context only, read from the leaf array).

`POST /cashflow/adjustments`
```json
{ "propertyId": "P012", "date": "2025-07-14", "amountPaise": 150000, "reason": "Duplicate utility payment reversed" }
```
→ `201 { "adjustmentId": "ADJ-0001", ..., "algorithm": { "operation": "pointUpdate", "timeComplexity": "O(log n)", "operations": 22 } }`. Updates the property tree and the portfolio tree.
`GET /cashflow/adjustments` → list · `DELETE /cashflow/adjustments/{id}` → undo (inverse point update) · `DELETE /cashflow/adjustments` → reset all (rebuilds the trees).

**Scope of adjustments:** they correct the Cash-Flow Explorer's daily values only. Source CSVs and Overview KPIs are unchanged, and every range response reports `adjustmentsApplied`.

## Payment Behaviour (Randomized Quickselect)
`GET /payments/percentiles?p=50,75,90&propertyId&propertyType&from&to&seed=20261124`
```json
{ "population": { "completed": 5620, "openExcluded": 214, "earlyOrOnTime": 3810 },
  "meanDaysLate": 4.37,
  "percentiles": [ { "p": 50, "k": 2809, "daysLate": 0 }, { "p": 75, "k": 4214, "daysLate": 4 }, { "p": 90, "k": 5057, "daysLate": 13 } ],
  "censoringNote": "214 obligations are still open at the analysis date and are not in this distribution.",
  "algorithm": { "name": "RandomizedQuickselect", "unit": 6, "operation": "select×3", "timeComplexity": "expected O(n)", "n": 5620, "operations": 39120, "elapsedNanos": 210000, "seed": 20261124 } }
```
(Values are illustrative and are *not* results.) `seed` is optional. If omitted, the server picks one and returns it, so any run can be reproduced.

`GET /payments/threshold?p=90&page=0&size=50&...filters` → `{ thresholdDaysLate, countAtOrAbove, histogram: [ {daysLate, count} ], byProperty: [ {propertyId, countAtOrAbove, share} ], records: [ {obligationId, leaseId, propertyId, dueDate, settledDate, daysLate, duePaise} ], page }`

`GET /payments/outstanding?...filters` → `{ totalOutstandingPaise, ageing: [ {bucket, count, paise} ], records: [ {obligationId, propertyId, dueDate, balancePaise, daysOverdue} ] }`

## Expenses
`GET /expenses/summary?from&to&propertyId` → by category, by property, monthly trend, expense-to-rent ratio.
`GET /expenses/unusual?from&to&fence=3|1.5` → records above Q3 + k·IQR of the same property × category history (k = 3 by default, Tukey's "far out" fence; 1.5 is available). Groups with fewer than 8 records are skipped and counted. Quartiles come from Randomized Quickselect (fixed seed). Rows: `{ expenseId, propertyId, propertyName, category, date, amountPaise, q1Paise, q3Paise, upperFencePaise, timesTypical, groupSize }`. Labelled "unusual compared with history", never "fraud".

## As-of semantics
Every date range must end on or before the analysis date. Balances, settled payments and occupancy are evaluated **as of** the analysis date, so payments recorded after it are ignored. `PUT /datasets/current/analysis-date` moves it within the horizon.

## Algorithm Lab (implemented in Phase 6)
`GET /benchmarks/results` → parsed rows from committed `benchmarks/results/*.csv`.
`POST /benchmarks/run` `{ "suite": "SEGMENT_TREE|QUICKSELECT", "sizes": [1000,10000], "repetitions": 5, "seed": 1 }` → live run (sizes ≤ 100,000, repetitions ≤ 20). Results are labelled "live, indicative" and are not mixed with the committed results.
