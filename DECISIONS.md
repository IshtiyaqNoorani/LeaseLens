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
| D-017 | Accepted (Phase 1) | Frontend: React 19, Vite, Tailwind CSS v4 (`@tailwindcss/vite`), Recharts, React Router; Vite dev proxy `/api` → `localhost:8090` (8080 was in use on the development machine) | Matches the brief; the proxy avoids CORS configuration in development |
| D-022 | Accepted (Phase 3) | New plain-Java module `leaselens-data` for the model, CSV parsing, validation and loading | Testable without Spring; the generator's tests can validate its output with the real rules |
| D-023 | Accepted (Phase 3) | Horizon derived from the data: first day of the earliest record's month to the last day of the latest record's month; default analysis date = horizon end | No extra manifest file is needed, and uploads stay six CSVs |
| D-024 | Accepted (Phase 3) | Generator simplifications: no pro-rating, deposits or rent-free periods; the first instalment is on the first billing day on or after the lease start | Keeps the ledger hand-checkable; documented as a limitation |
| D-025 | Accepted (Phase 3) | Generator uses per-property random streams, `StrictMath`, locale-independent formatting and LF endings | Byte-identical output on any OS or JVM; a test compares the committed sample with a fresh run |
| D-026 | Accepted (Phase 3) | Declared lease `status` is informational; analytics use dates and the analysis date | Status depends on the analysis date, so trusting a declared value could give wrong occupancy |
| D-018 | Accepted (Phase 1) | API contract written as Markdown with JSON examples (`docs/api/API_CONTRACT.md`) | Readable in the report and README; an OpenAPI file can be generated later if needed |
| D-019 | Accepted (Phase 2) | One quickselect implementation on `long[]` (days late are widened to long) | The same code serves delay percentiles and expense quartiles; avoids duplicated algorithm code |
| D-020 | Accepted (Phase 2) | Machine-independent operation counts (`OperationCounter`) alongside timings | Node visits and comparisons check complexity claims without JIT or hardware noise |
| D-021 | Accepted (Phase 2) | Phase 6 adds a classic two-way, first-element-pivot quickselect baseline | Measured: our three-way partition with a fixed pivot is ≈ n^1.5 on sorted input, not n², so the textbook adversarial case needs the classic scheme |
| D-027 | Accepted (Phase 4) | Adjustments correct only the Cash-Flow Explorer (segment trees); source data and Overview KPIs are unchanged, and responses report `adjustmentsApplied` | Keeps the audited source separate from what-if corrections; avoids silently mixing them |
| D-028 | Accepted (Phase 4) | Everything is "as of" the analysis date: ranges may not go past it, and later payments are ignored | One consistent point in time for balances, occupancy and settlement |
| D-029 | Accepted (Phase 4) | Sample and golden datasets bundled in the API jar; sample loaded at start-up (`leaselens.autoload-sample`) | The demo works from any working directory; tests switch autoload off |
| D-030 | Accepted (Phase 4) | Overdue attention rule = balances more than 90 days overdue above 1% of rent billed (was "any balance more than 90 days") | The old rule flagged 33 of 50 properties; a share-based rule is meaningful at portfolio scale |
| D-031 | Accepted (Phase 4) | Unusual-expense fence k = 3 by default (Tukey "far out"), 1.5 optional; groups smaller than 8 skipped | Money amounts are right-skewed: k = 1.5 flagged 150 records, k = 3 flags 36 and keeps the planted outliers at the top |
| D-032 | Accepted (Phase 4) | Compile with `-parameters` | Spring MVC needs parameter names to bind request parameters; without it every endpoint returned 400 |
| D-033 | Accepted (Phase 4) | A zero denominator gives `null` (shown as "n/a"), never 0 | A ratio of nothing is undefined, not zero |
