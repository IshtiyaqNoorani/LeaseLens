# Decision Log

Format: ID · status (Proposed / Accepted / Rejected / Superseded) · decision · reason.
Phase 0 decisions approved by the team on 2026-11-24.

| ID | Status | Decision | Reason |
|---|---|---|---|
| D-001 | Accepted | Build **LeaseLens** (MarketPulse is the fallback) | Tied top score; best distinctiveness and feasibility; synthetic-data weakness is mitigable (PHASE0 §2–3) |
| D-002 | Accepted | Algorithms: **Segment Tree (Unit 3)** + **Randomized Quickselect (Unit 6)** | Both drive core features; quickselect gives the Units 4–6 technique and CO5. The pairing matches the PDF's inventory sample, so depth is required |
| D-003 | **Rejected** (2026-11-24) | Randomized Quick Sort as insurance against risk R1 | Team decision. Consequence: R1 mitigation now depends on Unit 6 framing **and written faculty confirmation at the proposal stage** |
| D-004 | Accepted | No database; CSV is the system of record; in-memory store plus adjustment ledger | Data fits in memory; the PDF makes persistence optional; fewer setup failures in the demo |
| D-005 | Accepted | Money as `long` paise; dates as `LocalDate`; currency INR | Exact arithmetic; no floating-point drift; overflow-checked sums |
| D-006 | Accepted | Percentile P in hundredths; k = ⌈P·n/10000⌉ − 1 in integer arithmetic; P ∈ (0, 100] | Nearest-rank as specified, without floating-point rounding errors |
| D-007 | Accepted | Schema additions: `tenants.csv` (anonymous segment only), `leases.billing_day`. Obligation `amount_paid`/`settled_date`/`status` kept as *declared* fields and reconciled against transactions | Referential integrity for tenant_id; realistic due dates; reconciliation is an explicit validation feature |
| D-008 | Accepted | Custom benchmark harness (warm-up, ≥10 reps, median/IQR, op counts); JMH optional later | Transparent, supports the live Lab, satisfies the PDF; JMH is optional in the PDF |
| D-009 | Accepted | Three-way partition in quickselect | Payment delays are duplicate-heavy (many zeros); two-way partition degrades on equal keys |
| D-010 | Accepted | No ML or forecasting until the mandatory scope is complete; propose separately afterwards | Brief instruction; avoid "data science theatre" |
| D-011 | Accepted | Light enterprise visual theme (slate/white, navy accent) | Projector and print legibility for demo and report |
| D-012 | Accepted | Java 21 bytecode target (`maven.compiler.release=21`), Maven wrapper, multi-module build | LTS compatibility on examiner and teammate machines; algorithms isolated in a dependency-free module |
| D-013 | Accepted | Collection efficiency defined by cohort (primary); cash-to-billing ratio shown as secondary with a timing caveat | Avoids values above 100% that confuse readers |
| D-014 | Accepted | Data generator written in Java | Python is reserved for supplementary EDA (brief) and keeps one toolchain for reproducibility |
| D-015 | Accepted | Team of two (Raakin Majeed, Ishtiyaq Noorani); no individual-exception wording | Latest instruction; meets PDF §2 directly |
| D-016 | Accepted (Phase 1) | **Spring Boot 3.5.16**, not 4.x | 3.5 APIs (Jackson 2, MockMvc test slices) are mature and widely documented, which lowers viva and debugging risk. Boot 4.x is newer, but its gains don't matter for a local demo. Note: the 3.5 line is past its open-source support window, which is acceptable for an offline academic app. |
| D-017 | Accepted (Phase 1) | Frontend: React 19, Vite, Tailwind CSS v4 (`@tailwindcss/vite`), Recharts, React Router; Vite dev proxy `/api` → `localhost:8080` | Matches the brief; the proxy avoids CORS configuration in development |
| D-018 | Accepted (Phase 1) | API contract written as Markdown with JSON examples (`docs/api/API_CONTRACT.md`) | Readable in the report and README; an OpenAPI file can be generated later if needed |
