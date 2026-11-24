# Requirements Traceability Matrix

Source: `CIA3_Advanced_Algorithm_Project_Guidelines_2026-27.pdf` (§ = PDF section). Status: Planned / In progress / Done (verified) / At risk.

| ID | PDF § | Requirement | How LeaseLens satisfies it | Evidence / artefact | Verification | Status |
|---|---|---|---|---|---|---|
| R01 | §1, §2 | Group of 2–3 | Raakin Majeed (2404537), Ishtiyaq Noorani (2408912) | Proposal, report, CONTRIBUTIONS.md | Names and IDs on every deliverable | Planned |
| R02 | §1 | Usable web application solving a genuine problem | Rental portfolio cash-flow and payment-behaviour analytics | App, proposal §problem | Demo | Planned |
| R03 | §1, §7 | Algorithm central to behaviour, not decorative | Cash-Flow Explorer results come *only* from the segment tree; Payment Behaviour percentiles come *only* from quickselect | Service code paths; algorithm meta in API responses | Integration test asserts the service calls the algorithm; code walkthrough | Planned |
| R04 | §2 | Core algorithms and business logic in Java | All KPIs, validation and algorithms in `leaselens-api` / `leaselens-algorithms` | Source | Review: frontend has no computation logic | Planned |
| R05 | §2 | Frontend interacts with Java backend | React calls the REST API via the Vite proxy | `frontend/src/api` | Health check shown in UI (verified Phase 1) | In progress |
| R06 | §2, §4 | ≥2 distinct syllabus techniques | Segment Tree (Unit 3); Randomized Selection (Unit 6) | `leaselens-algorithms` | Implemented and unit-tested (Phase 2); integration into app workflows pending (Phase 4) | In progress |
| R07 | §2 | ≥1 technique from Units 4–6 | Randomized selection (Unit 6) | Proposal, report | **Written faculty confirmation of the Unit 6 mapping** | At risk (R1) |
| R08 | §2 | CO5 via randomized algorithm | Seeded random-pivot quickselect; Las Vegas analysis; seed-variance study | Tests, Lab, report | Differential tests over 1,000 seeds; comparison-count statistics (Phase 2 tests pass); Lab pending | In progress |
| R09 | §2 | Real or realistic synthetic data; document inputs, outputs, constraints, assumptions, failures | Seeded generator plus spec; data dictionary; validation rules | `data/GENERATOR_SPEC.md`, README, report | Generator determinism test | Planned |
| R10 | §2, §6 | Own Java implementations; frameworks provide infrastructure only | Dependency-free algorithms module | `leaselens-algorithms/pom.xml` | Dependency check: no third-party runtime deps (module created Phase 1 with JUnit test scope only) | In progress |
| R11 | §3 | Show how the method improves an analytical task | Interactive range analytics plus corrections; fast percentiles; measured against baselines | Lab, report | Benchmarks | Planned |
| R12 | §5 | Upload data, configure parameters, execute, interpretable result | M0 upload; scope, date, percentile and seed controls; result panels with explanations | UI | Demo script steps | Planned |
| R13 | §5 | Algorithm visibility | `algorithm` meta block shown in an algorithm panel on M3, M4 and M6 | API, UI | API tests check the meta fields | Planned |
| R14 | §5 | Validation: empty, invalid, duplicates, domain edge cases; never silently wrong | Rule engine (PROJECT_PLAN §7.3); algorithm guards | `data/invalid/`, ValidationReport | One test per fixture | Planned |
| R15 | §5 | ≥5 correctness tests per core algorithm incl. an edge case | 22 segment tree, 19 quickselect, 38 percentile; edge, invalid and stress cases included | JUnit | `./mvnw verify`: 79/79 pass (Phase 2) | Done (verified) |
| R16 | §5 | Benchmarks at ≥3 increasing sizes | 1k / 10k / 100k (+1M optional) | `benchmarks/results/*.csv` | Committed CSVs | Planned |
| R17 | §5 | Baseline comparison and when it helps | Linear scan and prefix sums; full sort; fixed pivot; 2-way partition | Lab, report §benchmarks | Crossover analysis written up | Planned |
| R18 | §5 | Runnable from repo with written steps and sample data | Maven wrapper, npm lockfile, `data/sample` | README | Fresh-clone run on a second machine | Planned |
| R19 | §6 | Java executes algorithms server-side; no browser or precomputed substitutes | Frontend renders API results only | Code review | Search frontend for algorithm logic | Planned |
| R20 | §7 | Not a static dashboard or CRUD | Parameterised algorithm-driven analytics with corrections | Demo | Demo | Planned |
| R21 | §7 | Sample idea needs substantial custom work | Depth items (PHASE0 §3.3) | Report "Contribution beyond the sample idea" | Faculty review | Planned |
| R22 | §8A | Proposal 1–2 pp with required fields | `docs/proposal/PROPOSAL.md` (draft v0.1, all fields present) | PDF | Field checklist | In progress |
| R23 | §8B | Repo: backend, frontend, tests, sample data, build files, manifests, readable history, attribution | Repo layout | Git log, CONTRIBUTIONS.md | Checklist | Planned |
| R24 | §8C | Report 5–8 pp with required sections | `docs/report/` | PDF | Section checklist; page count | Planned |
| R25 | §8D | Demo evidence: working app, I/O examples, error handling, perf charts | `docs/demo/`, screenshots | Files | Checklist | Planned |
| R26 | §8E | README: prereqs, commands, env vars, API examples, dataset, screenshots | `README.md` | File | Fresh-clone test | Planned |
| R27 | §10 | 8-minute demo: use case, algorithm results, failure handling, performance | Demo script | `docs/demo/DEMO_SCRIPT.md` | Timed rehearsal | Planned |
| R28 | §10 | 2-minute individual viva: contribution, algorithm, correctness, complexity; modified inputs; method walkthrough | Per-member viva sheets; editable inputs in UI | `docs/viva/` | Mock viva | Planned |
| R29 | §11 | Measurable success criteria | PROJECT_PLAN §5 criteria 1–6 | Proposal, report | Each criterion linked to a test or benchmark | Planned |
| R30 | §11 | Sound probabilistic behaviour | Seed studies, Las Vegas proof, fixed seeds in tests | Tests, `docs/algorithms/RANDOMIZED_QUICKSELECT.md` | Mean-comparison test, 1,000-seed differential test (pass) | In progress |
| R31 | §11 | Backend quality: separation, validation, errors, maintainability | Layered modules, ProblemDetail, tests | Source | API tests | Planned |
| R32 | Brief | Python EDA (supplementary only) | `analytics/LeaseLens_EDA.ipynb` | Notebook | Runs top to bottom in a clean venv | Planned |
