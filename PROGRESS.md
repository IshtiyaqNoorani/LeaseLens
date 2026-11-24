# Progress

## Phase 0: Academic audit and project selection (approved 2026-11-24)
- [x] Read all pages of the CIA 3 guidelines PDF
- [x] Requirements audit and rubric (`docs/PHASE0_DISCOVERY.md` §1)
- [x] Domain comparison and scoring (§2)
- [x] Critical evaluation of LeaseLens (§3)
- [x] Algorithm / CO5 compatibility and structural risk (§4)
- [x] Plan: features, schema, architecture, algorithms, tests, benchmarks, deliverables, roadmap (`PROJECT_PLAN.md`)
- [x] Traceability matrix (`REQUIREMENTS_TRACEABILITY.md`), decisions (`DECISIONS.md`)
- [x] Team approved LeaseLens + Segment Tree + Randomized Quickselect; Randomized Quick Sort declined (D-003)

## Phase 1: Architecture and project foundation (complete, awaiting review)
- [x] Git repository initialised (`main`), `.gitignore` (the course PDF is excluded from Git)
- [x] Maven multi-module build (algorithms / generator / api / bench), Maven wrapper 3.9.14, Java 21 target, enforcer (JDK ≥ 21)
- [x] Spring Boot 3.5.16 API skeleton: `GET /api/health`, RFC 7807 error handling; 2 MockMvc tests pass
- [x] Frontend: React 19 + Vite 8 + Tailwind 4 + Recharts + React Router; app shell with 8 module routes and live backend status; build and lint pass
- [x] Verified end to end: browser → Vite proxy → Java API (health) and a 404 returned as `application/problem+json`
- [x] API contract `docs/api/API_CONTRACT.md`; proposal draft `docs/proposal/PROPOSAL.md`; CONTRIBUTIONS.md; README
- [x] Committed: `4bd8daa` (Phase 0 docs), `4797217` (Phase 1 foundation)

## Phase 2: Java algorithm implementation and testing (complete, awaiting review)
- [x] `SegmentTree`: range sum / min / max with earliest-index tie rule, point update, overflow-checked sums with rollback, optional node-visit counting (owner: Raakin Majeed)
- [x] `RandomizedQuickselect`: random pivot, three-way partition, iterative, non-mutating `select`, `selectMany`, comparison counting (owner: Ishtiyaq Noorani)
- [x] `Percentile`: nearest rank k = ceil(P*n/100) - 1 in exact integer arithmetic; parsing and formatting
- [x] 79 JUnit tests pass (SegmentTree 22, Quickselect 19, Percentile 38), including randomized differential tests against brute force and sorting
- [x] Pseudocode, correctness proofs and complexity notes: `docs/algorithms/SEGMENT_TREE.md`, `docs/algorithms/RANDOMIZED_QUICKSELECT.md`
- Finding: a fixed first-element pivot with our three-way partition is super-linear on sorted input (≈ n^1.5 measured) but not exactly quadratic. The classic Θ(n²) case needs a two-way first-pivot baseline, which goes into Phase 6.

## Phase 3: Synthetic data, ingestion and validation (complete, awaiting review)
- [x] New module `leaselens-data` (plain Java): domain model, hand-written RFC 4180 CSV parser, 20 validation rules, `DatasetLoader` (file → row → cross-file checks, including a sweep-line occupancy check and payment reconciliation). Never returns a partial dataset; issue list capped at 1,000.
- [x] Golden dataset `data/golden` with hand-computed KPIs, percentiles and segment-tree answers in `EXPECTED.md`
- [x] Seeded generator (`sample` command): 50 properties, 248 leases, 3,980 obligations, 4,194 transactions, 9,699 expenses over 1,096 days; byte-identical for the same seed; 0 errors and 0 warnings on validation
- [x] Five planted patterns plus a negative control, documented with measured values in `data/GENERATOR_SPEC.md`
- [x] 23 invalid-data fixtures (`fixtures` command) in `data/invalid`, each rejected with the expected rule
- [x] 132 tests pass across the project (51 new: data 40, generator 11)
- Fixed during the phase: (1) the first sample had only 0.5% open obligations (99.3% collection efficiency), unrealistically clean, so short payments were added and it is now 3.5% open, 98.0% efficiency; (2) a first-lease term bug left 5 tenants without leases.
- Moved to Phase 4: asserting KPI values against `EXPECTED.md`, since the KPI services are built there.

## Phase 4: Java backend and REST APIs (complete, awaiting review)
- [x] Store: immutable `Snapshot` (dataset, analysis date, payment `Ledger`) swapped atomically; `CashFlowIndex` = 51 Segment Trees (one per property and one for the portfolio) with an auditable adjustment ledger (add, undo, reset)
- [x] Services: KPI overview and attention rules, property league table and detail, Cash-Flow Explorer (Segment Tree), Payment Behaviour (Randomized Quickselect), expenses with Tukey fences (quartiles via Quickselect), dataset load, upload and analysis date
- [x] 20 REST endpoints, RFC 7807 errors (400, 404, 409, 422); a rejected upload keeps the previous dataset; sample and golden bundled in the jar; sample loaded at start-up
- [x] Every algorithm-backed response includes an `algorithm` block (name, syllabus unit, operation, complexity, n, operation count, elapsed ns, seed)
- [x] `GoldenKpiTest`: every value in `data/golden/EXPECTED.md` matches exactly (8 tests, about 70 assertions); `ApiIntegrationTest` (21), `NoDatasetTest`; **163 tests pass in total**
- [x] Smoke test on the sample: a range query visits 37 nodes in about 0.15 ms; 4 percentiles over 3,918 delays take about 1 ms
- Fixes and recalibration during the phase:
  - Missing `-parameters` compiler flag made every endpoint return 400; fixed in the parent POM.
  - The attention list flagged 33 of 50 properties because short payments were never recovered. The generator now recovers 75% of them, and the overdue rule uses the share of billing. Now 10 properties are flagged, including planted P005 and P008 and unplanted P010.
  - Tukey 1.5×IQR flagged 150 skewed expense records; the default is now k = 3 (36 flagged), with 1.5 on request.
  - Planted pattern 5 was strengthened so it survives random variation; the sample was regenerated and the spec updated.

## Phase 5: React analytical application (complete, awaiting review)
- [x] Screens: Data Workspace (load sample or golden, upload with a full validation report, analysis date), Portfolio Overview (hero surplus, 12 KPIs, monthly trend, receivables ageing, attention rules), Property Performance (server-sorted league table, occupancy vs collection scatter, type/zone segments), Property detail, Cash-Flow Explorer (range, worst/best day, daily chart, month/quarter periods, A vs B comparison, corrections with before/after, log, undo, reset), Payment Behaviour (percentiles, new-seed randomization demo, histogram vs threshold, slow-payment records, outstanding ageing), Operating Expenses (category, monthly, by property, unusual records with fence choice), Methodology, Lab placeholder
- [x] Algorithm panel on every algorithm-backed result (name, syllabus unit, complexity, n, nodes or comparisons, server time, seed)
- [x] Charts follow the dataviz method: validated palette (blue/orange/aqua; aqua below 3:1, so every chart has a table view), one y-axis, legends for 2 or more series, thin marks, value-first tooltips
- [x] New backend endpoint `/api/properties/segments` so type/zone aggregation stays in Java (+1 API test)
- [x] Browser-verified: Overview, Properties, Cash-Flow correction then undo (22 nodes touched; header badge updates), Payments new-seed runs (same percentiles; 14,278 / 23,606 / 18,310 comparisons), Expenses, invalid upload (OCCUPANCY_EXCEEDS_RENTABLE shown with line and column; previous data kept). The only console error is the expected 422.
- Fixes during the phase: `Input`/`Select` dropped their base styles when given a `className`; before/after tiles now show full rupee amounts (compact figures hid the change); removed two client-side derived numbers so the frontend computes nothing analytical.

## Phase 6: Benchmarking and performance evaluation (complete, awaiting review)
- [x] `leaselens-bench`: harness (10 warm-ups, 20 timed reps, medians and IQR, sub-millisecond batching, volatile sink), seeded inputs, labelled baselines (linear scan, prefix sums, `Arrays.sort`, textbook two-way quickselect), answers verified before timing (6 tests)
- [x] Committed results `benchmarks/results/{segment_tree,quickselect,seed_study,seed_histogram}.csv` at n = 1,000 / 10,000 / 100,000: 168 timed measurements all verified, plus 12,000 seeded selections all correct. Runtime about 30 s
- [x] `benchmarks/BENCHMARKS.md`: method, 7 result tables generated from the CSVs, 12 findings with the conditions where each method wins or loses, and threats to validity
- [x] API `GET /api/benchmarks/results` (bundled CSVs plus ratios to the assessed method) and bounded `POST /api/benchmarks/run` (one at a time, n ≤ 100k, ≤ 10 reps); +2 API tests
- [x] Algorithm Lab screen: environment, Segment Tree charts and tables by correction share and range length, build cost, quickselect vs sort by input shape, k-percentile crossover, pivot and partition table, seed-study table and histogram, live-run panel. Browser-verified
- Honest findings for the report: a scan beats the tree on short ranges; prefix sums beat it for sum-only queries with rare corrections; the JDK sort beats quickselect on presorted data and, for LeaseLens's 3 percentiles on delay data, a single sort would be as fast or faster

## Open questions for the team
1. (Answer received: Git identities set; see CONTRIBUTIONS.md.)
2. (Answer received: Randomized Quick Sort declined.)
3. Which course week is it now, and what are the proposal and final-review dates?
4. CO1–CO5 definitions from the course file.
5. Does the course allow AI-assisted development, and is there a disclosure requirement?
6. Confirm ownership split (Raakin: Segment Tree; Ishtiyaq: Quickselect).

## Environment observed (Ishtiyaq's machine, 2026-11-24)
JDK 24.0.2, Maven 3.9.14, Node 24.16.0, Python 3 present. Port 8080 is in use by another local Java app, so LeaseLens uses 8090.
