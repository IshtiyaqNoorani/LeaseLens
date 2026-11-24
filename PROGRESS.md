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

## Open questions for the team
1. (Answer received: Git identities set; see CONTRIBUTIONS.md.)
2. (Answer received: Randomized Quick Sort declined.)
3. Which course week is it now, and what are the proposal and final-review dates?
4. CO1–CO5 definitions from the course file.
5. Does the course allow AI-assisted development, and is there a disclosure requirement?
6. Confirm ownership split (Raakin: Segment Tree; Ishtiyaq: Quickselect).

## Environment observed (Ishtiyaq's machine, 2026-11-24)
JDK 24.0.2, Maven 3.9.14, Node 24.16.0, Python 3 present. Port 8080 is in use by another local Java app, so LeaseLens uses 8090.
