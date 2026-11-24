# CLAUDE.md: LeaseLens

University CIA 3 Advanced Algorithm project (ATLAS uGDX, Sem V, B.Tech CS AI&ML, 2026–27).
Team of two: Raakin Majeed (2404537), Ishtiyaq Noorani (2408912).
The authoritative requirements are in `CIA3_Advanced_Algorithm_Project_Guidelines_2026-27.pdf`.

## Phase gates
- Work only in the phase the team has approved (see PROGRESS.md). Phase 0 approved 2026-11-24 (LeaseLens; Segment Tree + Randomized Quickselect; no Randomized Quick Sort).
- After each phase: run all tests, update PROGRESS.md, REQUIREMENTS_TRACEABILITY.md and DECISIONS.md, then stop for review.
- Never push to GitHub, deploy, or share data externally without explicit permission.

## Non-negotiables
- Assessed algorithms (Segment Tree, Randomized Quickselect) are written by hand in `leaselens-algorithms`, a module with **no third-party runtime dependencies**. No library substitutes, no Python, no precomputed results.
- All business logic and KPIs live in Java. The frontend only displays API results.
- Never invent benchmark numbers. Report figures only from committed CSVs in `benchmarks/results/`.
- Never treat missing data as zero. Never claim causality or fraud. No tenant credit scoring.
- Net operating cash surplus is cash basis, **not** NOI and not audited. Say so wherever it is shown.
- Git history must truthfully show who did what. No fabricated attribution.

## Conventions
- Money: `long` paise; overflow-checked (`Math.addExact`). CSV holds rupees with ≤2 decimals.
- Dates: `java.time.LocalDate`, ISO-8601. Default horizon 2023-11-01..2026-10-31 (1,096 days), analysis date 2026-10-31, seed 20261124.
- Percentile (nearest rank): P in hundredths, k = ceil(P*n/10000) - 1, P in (0, 100].
- Randomness: always injected (`RandomGenerator`/`SplittableRandom` with a seed); tests use fixed seeds.
- API errors: RFC 7807 `ProblemDetail`. Algorithm-backed responses include an `algorithm` meta block.

## Key documents
PROJECT_PLAN.md · REQUIREMENTS_TRACEABILITY.md · DECISIONS.md · PROGRESS.md · docs/PHASE0_DISCOVERY.md

## Commands
- Backend build + all tests: `./mvnw verify`
- Run API (port 8090; 8080 is taken by another app on Ishtiyaq's Mac): `./mvnw -DskipTests package && java -jar leaselens-api/target/leaselens-api.jar`
- Frontend: `cd frontend && npm ci && npm run dev` (port 5173, proxies `/api` to 8090); `npm run build`; `npm run lint`
- If npm fails with EACCES on `~/.npm`, the user's npm cache has root-owned files. Use `npm_config_cache=<scratchpad>/npm-cache` and never run sudo.
