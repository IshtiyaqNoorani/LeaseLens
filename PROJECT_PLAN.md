# LeaseLens: Project Plan

Status: **Approved 2026-11-24.** See `docs/PHASE0_DISCOVERY.md` for the audit, domain scoring and risks.
Team: Raakin Majeed (2404537), Ishtiyaq Noorani (2408912).

---

## 5. Recommended title and problem statement

**Title:** *LeaseLens: Rental Portfolio Cash-Flow and Payment-Behaviour Analytics using Segment Trees and Randomized Selection*

**Problem statement.** An operator of a mixed residential and commercial rental portfolio bills rent every month, collects it late, partly or not at all, and pays operating costs every day. Billing, collections and costs are recorded in separate ledgers, so basic management questions are slow to answer:
- How much net cash did this property, or the whole portfolio, generate between two dates, and which was the worst day?
- How late do tenants typically pay, and which payments sit in the slow tail?
- Which assets turn billed rent into cash least efficiently, or spend unusually heavily?

LeaseLens validates and joins these ledgers, answers range questions over daily net cash flow with a **segment tree** (O(log n) queries and corrections), and computes payment-delay percentiles with **randomized selection** (expected O(n)). Results are presented as transparent, formula-documented KPIs for decision support. It is not a property-management or tenant-administration system, and it does not score individual tenants.

**Target users:** portfolio and asset managers, commercial property operators, real-estate investment analysts, BI analysts.

**Input constraints:** CSV ledgers as specified in §7. Amounts in INR, stored as integer paise. ISO dates. Analysis horizon up to about 10 years of daily data. A fixed, user-visible **analysis date**.

**Measurable success criteria** (each is checked by a test or benchmark; numeric performance targets are hypotheses to be *measured*, not promised):
1. Segment tree sum, min and max equal brute-force results on 100% of ≥10,000 seeded random query/update cases.
2. Quickselect equals the sorted-array reference on 100% of cases across ≥1,000 seeds and ≥6 input shapes.
3. Every KPI on a hand-computed golden dataset matches the expected value **to the paisa**.
4. The validation suite detects 100% of planted defects (one fixture per error type in §8.3) with a clear message, and never returns partial results silently.
5. Benchmarks at n = 1k, 10k and 100k, each with ≥10 measured repetitions after warm-up, saved as CSV. The report states where each algorithm beats or loses to its baseline.
6. A fresh clone builds, tests and runs with the documented commands on macOS and Windows.

---

## 6. Feature breakdown

Every page shows the **analysis date**, the **data scope**, and a "How this is calculated" panel linking to the formula registry.

### M0 Data Workspace (input workflow, PDF §5)
- Load the bundled sample dataset, or upload the CSV set (multipart).
- Validation report: per-file row counts, errors (blocking) and warnings (non-blocking), each with file, row, column, rule and explanation.
- Dataset summary: horizon, counts, analysis date (editable within the horizon).
- Invalid-data fixtures shipped in `data/invalid/` for the error-handling demo.

### M1 Portfolio Overview
KPI cards (formulas in §6.1), receivables ageing, monthly billed vs collected vs opex trend, and an **attention list** of properties that break transparent, configurable rules (each flag says which rule fired).

### M2 Property Performance
Sortable league table (rent billed and collected, collection efficiency, occupancy, rent per sq ft, expense ratio, net operating cash surplus, cash yield on acquisition value, vacancy days, outstanding balance). Comparison by property type and zone, scatter of occupancy vs collection efficiency, and a property detail drawer.

### M3 Cash-Flow Explorer (Segment Tree)
- Scope: portfolio or a single property. Date range picker.
- Results **from the Java segment tree**: range net cash flow (sum), worst day (min and its date), best day (max and its date). Daily trend chart for context.
- Period comparison: the same metrics for each month or quarter in the range, so many tree queries run per request. Also A vs B comparison of two custom ranges.
- **Corrections:** record an adjustment (date, scope, amount, reason). The backend writes it to the adjustment ledger and applies an O(log n) point update to the property tree *and* the portfolio tree. Re-running the query shows the change. An audit list shows all adjustments, with undo and reset.
- Algorithm panel: method, n, nodes visited, update path length, elapsed time.

### M4 Payment Behaviour (Randomized Quickselect)
- Filters: property, property type, due-date range. Percentile selector: defaults P50, P75, P90, plus a custom P with up to 2 decimal places.
- Results **from Java randomized selection**: the value at each percentile, rank k, n, comparison count, and seed.
- Distribution histogram of days late (completed payments), the mean for comparison, and the selected threshold marked on the chart.
- Records at or above the threshold: paginated table (obligation, lease, property, due date, settled date, days late, amount), plus a breakdown of tail records by property and type.
- **Kept separate:** outstanding (unpaid or part-paid) obligations as of the analysis date, with balance and days overdue, ageing buckets, and a censoring note ("x obligations are still open and are not included in the delay distribution").
- Wording rules: descriptive, aggregated by property where possible. No tenant scores, rankings or credit judgements.

### M5 Operating Expense Analysis
Spending by category and property, monthly trend, expense-to-rent ratio, highest-cost properties, and **unusually large records** flagged with Tukey's rule (above Q3 + 1.5·IQR within property × category; the quartiles come from our Quickselect). Shown as "unusual compared with this property's history; review recommended". Never labelled as fraud.

### M6 Algorithm Performance Lab
- Shows the stored offline benchmark results (CSV) as charts: time vs n on log–log axes, speed-up ratios, comparison counts vs theoretical expectation.
- A live "run small benchmark" button (n capped, e.g. ≤100k, a few repetitions) so the examiner can watch it run.
- Panels: segment tree vs linear scan vs prefix sum (by update ratio); quickselect vs full sort; random vs fixed pivot on sorted input; 2-way vs 3-way partition on duplicate-heavy data; k-percentile crossover; seed-variance distribution.

### M7 Methodology
Formula registry, cash vs accrual notes, assumptions, data dictionary and limitations, written to be readable during the demo.

### 6.1 KPI registry (all calculated from the data; none hard-coded)
| KPI | Formula | Basis |
|---|---|---|
| Total properties | count(properties) | n/a |
| Total rentable area | Σ rentable_area_sqft | n/a |
| Occupancy rate (point in time) | Σ leased_area of leases active on the analysis date ÷ Σ rentable_area × 100 | Physical |
| Occupancy (time-weighted, period) | Σ leased-area-days ÷ Σ rentable-area-days × 100 | Physical |
| Rent billed | Σ due_amount for obligations due in period | Accrual |
| Rent collected | Σ amount_received for transactions dated in period | Cash |
| Collection efficiency (cohort, primary) | Σ amount paid by the analysis date on obligations due in period ÷ Σ due_amount of those obligations × 100 | Cohort (stays ≤ 100%) |
| Cash-to-billing ratio (secondary) | Rent collected in period ÷ rent billed in period × 100 | Mixed. Can exceed 100% because of timing; labelled as such |
| Outstanding receivables | Σ (due − paid) for obligations due on or before the analysis date with a positive balance; ageing 0–30 / 31–60 / 61–90 / 90+ days | Accrual |
| Operating expenses | Σ expense amount in period | Cash |
| Net operating cash surplus | Rent collected − operating expenses | Cash. **Not NOI and not audited**: ignores capex, debt service, tax and depreciation |
| Expense ratio | Operating expenses ÷ rent collected × 100 | Cash |
| Rent per sq ft (monthly) | Σ monthly_rent of active leases ÷ Σ leased_area_sqft | Contract |
| Cash yield on acquisition value | Annualised net operating cash surplus ÷ acquisition_value × 100 | Cash. Not a cap rate |
| Vacancy days | Days on which a property's leased area is below its rentable area, weighted by vacant area | Physical |
| Days late (completed) | settled_date − due_date, floored at 0 (early payments counted separately) | Behavioural |
| Days overdue (open) | analysis_date − due_date for open obligations past due | Behavioural |

---

## 7. Data schema and dataset generation

### 7.1 Schema (revised from the brief; reasons in DECISIONS D-007)
`properties.csv`: property_id, property_name, property_type {RESIDENTIAL, OFFICE, RETAIL, INDUSTRIAL}, location_zone, rentable_area_sqft, acquisition_value, total_units

`tenants.csv` *(added)*: tenant_id, tenant_segment {INDIVIDUAL, SME, CORPORATE}. Anonymous IDs only, with no names or personal data. It exists so `leases.tenant_id` can be checked for referential integrity.

`leases.csv`: lease_id, property_id, tenant_id, start_date, end_date, monthly_rent, leased_area_sqft, billing_day (1–28, *added*), status {ACTIVE, EXPIRED, TERMINATED}

`rent_obligations.csv`: obligation_id, lease_id, due_date, due_amount, amount_paid, settled_date (blank if open), status {PAID, PARTIAL, UNPAID}

`rent_transactions.csv`: transaction_id, obligation_id, transaction_date, amount_received

`expenses.csv`: expense_id, property_id, expense_date, expense_category {MAINTENANCE, REPAIRS, UTILITIES, PROPERTY_TAX, INSURANCE, MANAGEMENT, SECURITY, CLEANING}, amount

`amount_paid`, `settled_date` and `status` in obligations are **declared** values. The backend recomputes them from transactions and rejects the dataset if they do not match. This is the reconciliation check the brief asks for.

Money: decimal rupees with up to 2 places in the CSV, parsed exactly into `long` paise (no `double` anywhere in money paths).

### 7.2 Generator (Java CLI, seeded)
- Default horizon 2023-11-01 to 2026-10-31 (**1,096 days**). Default analysis date 2026-10-31. Default seed 20261124.
- 50 properties: about 20 residential (2–6 units), 14 office (2–6 suites), 11 retail (1–4 shops), 5 industrial (1–2 bays). Fictional names and zones.
- Leases: about 180 units, turnover and renewals, giving about **200–240 leases**. Vacancy gaps between leases. Leased area per property never exceeds rentable area.
- Obligations: one per lease-month on the billing day, about 5–7k rows. Each tenant gets a behaviour profile (punctual, occasionally late, chronically late, or defaulting) that sets the delay and partial-payment distributions. Partial payments are 2–3 transactions. Recent defaults stay open.
- Expenses: recurring categories (tax, insurance, management) plus stochastic maintenance and repairs, about 6–8k rows.
- **Planted ground truth**, recorded in `data/GENERATOR_SPEC.md` (the EDA checks whether it recovers these):
  1. Retail delays have a heavier tail than residential.
  2. Maintenance and repairs rise in June–September (monsoon).
  3. Two offices have an 8–10-month vacancy shock.
  4. About 1% of repair records are outsized one-off items.
  5. One property's collection efficiency degrades over time.
  6. Within one segment, expenses and collections are generated **independently** (a negative control: the EDA must not report a meaningful correlation).
- Outputs: `data/sample/*.csv`, plus `data/scaled/` (optional larger scenario) and `data/invalid/` (one defect per file).
- **The same seed gives byte-identical CSVs.** This is tested.

### 7.3 Validation rules (blocking unless marked W)
Missing file or column; empty file; missing required value; unparseable or invalid date; duplicate IDs (property, tenant, lease, obligation, transaction, expense); unknown property, tenant, lease or obligation reference; lease end before start; leased area above rentable area at any date (sweep-line check); negative or zero rent; negative expense; transaction before the lease start or after the analysis date; transaction amount ≤ 0; transaction total above the obligation due amount; declared amount_paid, settled_date or status not matching the transactions; obligation due date outside the lease period (W); dates outside the horizon (W). Missing values are **never** treated as zero.

---

## 8. Architecture

### 8.1 Repository layout (multi-module Maven + Vite)
```
leaselens/
├─ pom.xml                         # parent, Java 21 target, Maven wrapper
├─ leaselens-algorithms/          # PURE Java: SegmentTree, RandomizedQuickselect, (RandomizedQuickSort)
│                                 #   zero runtime dependencies; JUnit 5 tests
├─ leaselens-generator/           # seeded synthetic data CLI
├─ leaselens-api/                 # Spring Boot 3 REST app
│   └─ com.leaselens.{domain, ingest, validation, store, analytics, api, config}
├─ leaselens-bench/               # benchmark harness → benchmarks/results/*.csv
├─ frontend/                      # React + Vite + Tailwind + Recharts
├─ analytics/                     # EDA notebook, requirements.txt
├─ data/{sample,invalid,golden,scaled}
├─ benchmarks/results/
├─ docs/{proposal, report, diagrams, screenshots, demo, viva}
└─ CLAUDE.md PROJECT_PLAN.md REQUIREMENTS_TRACEABILITY.md DECISIONS.md PROGRESS.md CONTRIBUTIONS.md README.md
```
The assessed algorithms live in a module with **no Spring and no third-party dependencies**, so an examiner can open one folder and see exactly what was written by hand.

### 8.2 Backend layers
`api` (controllers, DTOs, RFC 7807 `ProblemDetail` errors) → `analytics` services (KPI, property performance, cash flow, payment behaviour, expenses, benchmark) → `store` (immutable in-memory `PortfolioDataset` snapshot plus adjustment ledger and segment-tree index) → `ingest`/`validation` (CSV parsing, rule engine, `ValidationReport`) → `leaselens-algorithms`.
No database. The data fits in memory, CSV is the system of record, and adjustments are kept in memory and can be exported (D-004).

### 8.3 Main endpoints (initial contract, frozen in Phase 1)
| Method | Path | Purpose |
|---|---|---|
| POST | /api/datasets/sample | Load bundled sample |
| POST | /api/datasets/upload | Multipart CSV upload → ValidationReport |
| GET | /api/datasets/current | Summary, horizon, analysis date |
| GET | /api/portfolio/overview?from&to | KPI block + attention list |
| GET | /api/properties/performance?from&to&sort | League table |
| GET | /api/cashflow/range?scope&from&to | **Segment tree** sum/min/max(+dates) + algorithm meta |
| GET | /api/cashflow/periods?scope&from&to&granularity | Many tree queries (monthly/quarterly) |
| GET | /api/cashflow/series?scope&from&to | Daily values for the chart |
| POST/GET/DELETE | /api/cashflow/adjustments | Point-update ledger |
| GET | /api/payments/percentiles?p=50,75,90&propertyId&type&from&to&seed | **Quickselect** results + meta |
| GET | /api/payments/threshold?p&page | Records ≥ threshold, distribution, mean |
| GET | /api/payments/outstanding | Open obligations, ageing |
| GET | /api/expenses/summary · /api/expenses/unusual | Expense analytics |
| GET | /api/benchmarks/results · POST /api/benchmarks/run | Performance Lab |
Every algorithm-backed response includes `algorithm: {name, unit, complexity, n, operations, elapsedNanos, seed?}`, which covers the PDF §5 "algorithm visibility" requirement.

### 8.4 Frontend
React and Vite, Tailwind, Recharts, React Router, plus a small typed API client. **Visual direction: clean light enterprise theme** (white and slate surfaces, deep navy accent, one positive and one negative colour, tabular numerals). It reads well on a projector and prints cleanly in the report. The frontend only displays results and never computes assessed results.

### 8.5 Python analytics architecture
`analytics/LeaseLens_EDA.ipynb` reads the **same CSVs** (read-only). It uses pandas, NumPy, Matplotlib, and SciPy for the non-parametric tests. Sections: cleaning and validation; descriptive statistics; rent distribution; delay distribution (histogram and ECDF, with censoring discussion); occupancy and vacancy; property comparisons; expense vs collection correlation (Spearman with bootstrap confidence intervals, plus the negative control); monthly trends and seasonality; segmentation by type (Kruskal–Wallis); planted-truth recovery table; insights, each labelled as synthetic. It also **cross-checks KPIs against JSON exported from the API**. Python never performs the assessed algorithms. Forecasting is out of scope until the core is done (D-010).

---

## 9. Algorithm implementation plan

### 9.1 Segment Tree, `com.leaselens.algorithms.segmenttree.SegmentTree`
- Input `long[] values` (daily net cash flow in paise). Recursive tree over arrays of size 4n: `sum[]`, `min[]`, `minIdx[]`, `max[]`, `maxIdx[]`. Ties resolve to the earliest day.
- `build` O(n), `query(l, r) → RangeResult{sum, min, minIndex, max, maxIndex}` O(log n), `update(i, newValue)` O(log n), space O(n).
- Overflow: `Math.addExact` on internal sums. A 100k-day sum at ₹10 crore per day is ≈ 10¹⁶ paise, below 9.2 × 10¹⁸. Overflow throws an exception rather than wrapping.
- Guards: empty input rejected at construction; `l > r`, out-of-range or negative indices give `IllegalArgumentException` with a clear message. The service maps dates to day indices.
- Optional `OperationCounter` records nodes visited, for the algorithm panel and benchmarks.
- Portfolio scope: a portfolio tree over the daily sums across properties. A correction updates the property tree and applies the same delta to the portfolio tree (two O(log n) updates).

Pseudocode, correctness argument (by induction on the node invariant "node stores the aggregate of its interval"; queries split into O(log n) canonical disjoint segments, at most 2 partial nodes per level, so at most 4 log n visits) and complexity proofs go in the report.

### 9.2 Randomized Quickselect, `com.leaselens.algorithms.selection.RandomizedQuickselect`
- `select(int[] data, int k, RandomGenerator rng)` works on a **copy**, so the caller's data is never mutated.
- **Three-way (Dutch national flag) partition** around a uniformly random pivot. Recurse only into the side that contains k; if k falls in the "equal" band, return immediately. This handles heavy duplicates and all-identical input in linear time. Iterative loop, so there is no stack-depth risk.
- `Percentile.nearestRankIndex(n, pHundredths)`: P is given in hundredths (e.g. 9050 = 90.50%) and k = ⌈P·n / 10000⌉ − 1 is computed in **exact integer arithmetic** (no floating-point rounding). P must be in (0, 100]. n = 0 gives an empty-input error.
- Guards: null or empty input, k < 0 or k ≥ n.
- Expected comparisons: O(n), with the indicator-variable proof in the report; worst case Θ(n²) for an unlucky pivot sequence, with probability falling exponentially. Space O(n) for the defensive copy and O(1) extra.
- Las Vegas: the output is always correct; only the running time is random.

### 9.3 Randomized Quick Sort: not included (team decision, D-003). League tables use ordinary `List.sort`, which is infrastructure, not an assessed algorithm.

### 9.4 Baselines (in `leaselens-bench`, clearly labelled as baselines)
Linear scan for range sum/min/max; prefix-sum array (O(1) query, O(n) rebuild per update); `Arrays.sort` + index for percentiles; first-element-pivot quickselect; two-way (Lomuto) partition quickselect.

---

## 10. Testing and benchmarking plan

### 10.1 Tests (JUnit 5; `./mvnw verify` runs all)
**Segment tree (≥12):** single element; full range; partial range; first and last boundaries; l = r; all-negative values; ties for min/max (earliest index); point update then query; repeated updates on the same index; invalid ranges (l > r, negative, ≥ n); empty input; overflow detection; **seeded randomized differential test** against brute force (10,000 operations × several seeds).

**Quickselect (≥14):** single element; sorted; reverse-sorted; duplicates; all-identical; k = 0 and k = n − 1; invalid k; empty or null; input array not mutated; same seed gives the same comparison count; **differential test vs sort across 1,000 seeds × 6 input shapes**; nearest-rank formula table (n = 1, 2, 10, 100 at P = 0.01, 25, 50, 75, 90, 99.99, 100; P = 0 rejected; P > 100 rejected); a statistical property test that mean comparisons over 1,000 fixed seeds stay within the theoretical bound (deterministic seeds, so it cannot flake).

**Ingestion and validation:** one fixture per rule in §7.3, plus a clean dataset with zero errors.
**Business metrics:** golden dataset (2 properties, 3 leases, about 12 obligations, hand-computed in `data/golden/EXPECTED.md`) giving exact KPI values.
**API (MockMvc):** happy path for every endpoint, 400 errors with ProblemDetail bodies, 409/422 when no dataset is loaded.
**Integration:** load sample → overview → cash-flow query → adjustment → re-query reflects the change → percentiles equal the sort reference.
**Generator:** the same seed gives identical bytes, and its output passes validation.
**Frontend:** a lightweight Vitest check of formatters and the API client. Manual demo checklist.

### 10.2 Benchmark methodology
- Sizes: **1,000 / 10,000 / 100,000** (plus 1,000,000 if run time allows). Each size uses the same seeded logical input for the algorithm and its baseline.
- Segment tree workloads: Q random ranges (Q = n) with update ratios 0%, 1%, 10% and 50%, against a linear scan and prefix sums. Results are checked to be identical before timing counts.
- Quickselect: input shapes uniform, sorted, reverse, realistic delay distribution (duplicate-heavy) and all-identical; P50/P90 single select vs `Arrays.sort`; k-percentile crossover; random vs fixed pivot (fixed pivot capped at n ≤ 20k to avoid O(n²) blow-up, and noted as such).
- Protocol: JVM warm-up (≥5 untimed iterations), ≥10 timed repetitions, `System.nanoTime`, results kept live in a sink, median, IQR and min reported, operation counts recorded, JVM, OS and CPU logged in the CSV header.
- Output: `benchmarks/results/<suite>_<date>.csv`, with charts made from these CSVs only. **No number appears in the report unless it is in a committed CSV.**
- JMH is optional (PDF §6). The custom harness is chosen for transparency and to support the live Lab (D-008).

---

## 11. Deliverable mapping

| Deliverable | Produced in | Location | Contents check |
|---|---|---|---|
| A Proposal (1–2 pp) | Phase 1 (draft now, final after faculty feedback) | `docs/proposal/` | title, names and IDs, problem, user, features, 2 techniques and why, data source, success criteria (§5) |
| B Repository | Phases 1–8 | repo root | backend, frontend, tests, sample data, build files, manifests, readable history, `CONTRIBUTIONS.md` |
| C Report (5–8 pp) | Phase 9 | `docs/report/` (Markdown → PDF) | problem and constraints, architecture and API diagram, pseudocode, correctness, complexity, benchmark method and results, limitations, individual contributions |
| D Demo evidence | Phases 8–9 | `docs/demo/`, `docs/screenshots/` | screenshots, input/output examples, error-handling captures, performance charts, 8-minute demo script, optional slides |
| E README | Phase 9 (built up from Phase 1) | `README.md` | prerequisites, build and run commands, env vars, API examples (curl), dataset, screenshots |
| Viva prep | Phase 9 | `docs/viva/` | per-member sheets: contribution, algorithm walkthrough, correctness, complexity, likely examiner edge cases |

---

## 13. Phased roadmap (exit criteria; dates fitted once the course weeks are known)

| Phase | Scope | Exit criteria |
|---|---|---|
| 0 | Audit, selection, plan | **This document approved** |
| 1 | Git init, Maven multi-module + wrapper, Vite scaffold, API contract, proposal draft, CONTRIBUTIONS | `./mvnw verify` and `npm run build` pass on an empty skeleton; contract committed |
| 2 | Segment tree + quickselect and tests | All algorithm tests green; pseudocode and proof notes drafted |
| 3 | Generator, ingestion, validation, golden dataset | Same seed gives the same bytes; every invalid fixture is caught; golden KPIs pass |
| 4 | Services + REST APIs | MockMvc and integration tests green; curl examples work |
| 5 | React app (M0–M7) | Full input → execute → result workflow in the browser for both algorithms |
| 6 | Benchmarks | CSVs committed; Lab charts render from them |
| 7 | Python EDA | Notebook runs top to bottom from a clean environment; planted-truth table |
| 8 | Integration and QA | Fresh-clone run on a second machine (ideally the teammate's); demo checklist passes |
| 9 | Report, proposal final, README, demo script, viva prep | Submission package complete and checked against the traceability matrix |

Proposed ownership (to confirm): **Raakin Majeed**: Segment Tree, cash-flow service and explorer, segment-tree benchmarks. **Ishtiyaq Noorani**: Randomized Quickselect, payment-behaviour service, CO5 study, EDA notebook. **Shared**: ingestion and validation, KPIs, frontend, report. Each member must be able to explain *both* algorithms, and walk through their own line by line.
