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
- [ ] First commit (waiting for each member's Git name and email)

## Open questions for the team
1. Git identity (name and email) for each member's commits.
2. (Answer received: Randomized Quick Sort declined.)
3. Which course week is it now, and what are the proposal and final-review dates?
4. CO1–CO5 definitions from the course file.
5. Does the course allow AI-assisted development, and is there a disclosure requirement?
6. Confirm ownership split (Raakin: Segment Tree; Ishtiyaq: Quickselect) and each member's Git identity.

## Environment observed (Ishtiyaq's machine, 2026-11-24)
JDK 24.0.2, Maven 3.9.14, Node 24.16.0, Python 3 present. Port 8080 is in use by another local Java app, so LeaseLens uses 8090.
