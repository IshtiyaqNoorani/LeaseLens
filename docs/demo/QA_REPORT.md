# Phase 8: integration and QA report

Date: 25 Nov 2026 · Machine: macOS 15.7.3 (Apple silicon, 10 cores), JDK 24.0.2, Node 24.16, Python 3.13.

## 1. Fresh-clone reproducibility (as an examiner would run it)
| Step | Command | Result |
|---|---|---|
| Clone | `git clone <repo>` | ✅ |
| Build and all tests | `./mvnw verify` | ✅ **172 tests, 0 failures** |
| Frontend install and build | `cd frontend && npm ci && npm run build` | ✅ |
| Regenerate sample data and fixtures | generator `sample` and `fixtures` commands | ✅ **byte-identical**: `git status` clean afterwards |
| Run API and frontend | README "Run" commands | ✅ sample auto-loads; UI connects through the proxy |

## 2. API checks (running jar from the clone)
| Check | Expected | Result |
|---|---|---|
| Every README `curl` example | 200 / 201 / 204 | ✅ |
| Bad date `from=2026-13-01` | 400 problem+json | ✅ |
| Unknown scope `P999` | 404 | ✅ |
| Percentile `p=0` | 400 | ✅ |
| Unknown bundled dataset | 404 | ✅ |
| Unknown sort key | 400 | ✅ |
| **Upload each of the 23 invalid fixtures** | 422, report contains the expected rule | ✅ **23 / 23** |
| Rejected upload keeps the previous dataset | still `golden` or `sample` | ✅ (also covered by an API test) |
| Response times on the sample | interactive | ✅ 1–20 ms per endpoint |

## 3. Cross-checks between components
- Segment Tree full-range net cash flow = Overview net operating cash surplus = pandas recomputation (₹62,95,81,982).
- Quickselect percentiles = pandas percentiles by full sort (P50 / 75 / 90 / 95 = 0 / 3 / 16 / 29 days); retail P50 / 75 / 90 = 0 / 9 / 26.
- Golden dataset: every hand-computed value in `data/golden/EXPECTED.md` matches (`GoldenKpiTest`).
- Benchmarks: 168 measurements, with every method's answers compared before timing; 12,000 seeded selections all correct.

## 4. Browser checks (Phases 5–6, repeated for screenshots)
Every screen renders against the live API (`docs/screenshots/01–09`). Interactive flows verified by hand:
- A correction, then the change shown before and after, then undo.
- New-seed runs: same percentiles, different comparison counts.
- An invalid upload showing its validation report.
- A live benchmark run.

Chart animations are disabled so charts hold their frame on refresh and render in screenshots.

## 5. Known limitations and open items
| Item | Status |
|---|---|
| Run on a **second machine** (ideally Raakin's, and any Windows PC) | ⚠️ Not yet done: recommended before the demo. Needs JDK 21+ and Node 20.19+ |
| Port 8080 was taken on the development Mac | Mitigated: the API uses 8090 (`PORT` overrides) |
| npm cache owned by root on the development Mac | Mitigated: a workaround is documented in CLAUDE.md; fix with `sudo chown -R 501:20 ~/.npm` |
| 3 lint warnings (state updates in data-fetching effects) | Accepted: intentional fetch-on-change pattern |
| Benchmarks measured on one machine without JMH fork isolation | Documented in `benchmarks/BENCHMARKS.md` §5 |
| Corrections live in memory only | By design (D-027): cleared on reload |
