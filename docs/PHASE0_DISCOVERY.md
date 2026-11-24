# Phase 0: Academic Audit, Domain Selection and Risk Review

Status: **Approved 2026-11-24** (LeaseLens; Segment Tree + Randomized Quickselect; Randomized Quick Sort declined).
Authoritative source: `CIA3_Advanced_Algorithm_Project_Guidelines_2026-27.pdf` (6 pages; page 6 is blank). All 5 content pages were read.

Team (group of two, which meets the PDF's 2–3 rule, so no individual-submission exception is needed):

| Member | Student ID |
|---|---|
| Raakin Majeed | 2404537 |
| Ishtiyaq Noorani | 2408912 |

> Note: the original brief said the project would be individual with faculty permission. The later instruction says it is a two-person group. These documents treat the **two-person group** as correct. If the individual-submission arrangement still applies, tell us and the documents will change.

---

## 1. Academic requirements audit

### 1.1 Project brief (PDF §1)
- CIA 3 group application project, Semester V, B.Tech CS AI & ML, academic year 2026–27, assessed out of 100.
- Build a usable web or desktop-style application that solves a genuine problem with advanced algorithms from the course.
- **The algorithm must be central to the application's behaviour.** It cannot be decoration or a library call hidden behind a UI.
- Example domains given: resource allocation, digital search, logistics, healthcare operations, fraud screening, scheduling, network capacity, inventory optimisation, large-scale data retrieval.
- Backend: Java (mandatory). Frontend: JS / React / Vue / Angular or equivalent. Database: optional.

### 1.2 Non-negotiable requirements (PDF §2)
| # | Requirement |
|---|---|
| NN1 | Group of 2–3. More than 3 is not allowed. Individual submission needs explicit faculty approval. |
| NN2 | All core algorithms **and business logic** in Java (Spring Boot REST recommended). |
| NN3 | A working frontend that talks to the Java backend. |
| NN4 | **At least two distinct, meaningful syllabus techniques, with at least one from Units 4–6.** |
| NN5 | Show CO5 meaningfully through a syllabus-covered **randomized algorithm or probabilistic data structure**. Without CO5, an alternative needs advance faculty approval. |
| NN6 | Real or realistic synthetic data. Document inputs, outputs, constraints, assumptions and failure cases. |
| NN7 | Write your own Java implementations of the assessed algorithms. Frameworks may provide infrastructure only. |

### 1.3 What is evaluated (PDF §3)
Algorithm selection, correctness, engineering quality and **measurable efficiency**. The solution must show how a computational method improves an operational decision, search, allocation or **analytical task**.

### 1.4 Accepted syllabus topics (PDF §4)
| Unit | Techniques | PDF's illustrative use |
|---|---|---|
| 1 Sorting / selection | Counting / Radix / Bucket sort; **Quickselect**; Median-of-Medians; amortised analysis | Ranked dashboards, **fast percentile selection** |
| 2 Priority / DSU | Binary / Fibonacci heaps; disjoint-set union | Event prioritisation, resource groups |
| 3 Trees / range queries | AVL, Red-Black, Splay; **segment trees**; RMQ | Dynamic indexes, **interval analytics, fast updates and queries** |
| 4 Text search | KMP; tries; radix trees; suffix trees / arrays | Autocomplete, document search |
| 5 Network / approximation | Ford–Fulkerson; Edmonds–Karp; max-flow min-cut; bipartite matching; vertex/set cover; metric TSP approx. | Matching, capacity, staffing, routing, coverage |
| 6 Randomization / complexity | Randomized Quick Sort; **randomized selection**; Bloom filters; NP / NP-hard / NP-complete analysis | Membership prechecks, **randomized ranking**, performance / tractability study |

### 1.5 CO5
The PDF does **not define** the course outcomes. It only tags sample ideas with CO2–CO5. What the PDF does say:
- CO5 = shown through a syllabus-covered randomized algorithm or probabilistic data structure (§2).
- Every sample idea using "randomized selection" or a Bloom filter is tagged CO5 (§7).
- The sample *"Inventory risk and percentile dashboard: segment trees for range inventory analytics and randomized Quickselect for stock-risk quantiles on updated data"* is tagged **CO2, CO5**.

**Action:** get the CO1–CO5 wording from the course file so the report can quote it exactly.

### 1.6 Minimum functional scope (PDF §5)
1. **Workflow:** the user can input or upload data, set meaningful parameters, run the algorithm and see an interpretable result.
2. **Algorithm visibility:** name the method used and show useful output (ranking, bottleneck, utilisation, and so on).
3. **Data validation:** handle empty datasets, invalid inputs, duplicates and domain edge cases *without silently returning incorrect results*.
4. **Evidence:** **≥5 documented correctness tests per core algorithm**, including at least one boundary or edge test, plus benchmark inputs of **≥3 increasing sizes**.
5. **Baseline comparison:** compare at least one key algorithm with a reasonable baseline, alternative or theoretical expectation, and **explain the conditions under which it helps**.
6. **Reproducibility:** the project runs from the repository using written setup steps and sample data.

### 1.7 Architecture requirements (PDF §6)
| Layer | Responsibility | Options |
|---|---|---|
| Frontend | Forms, results, charts, visualisations | React / Vue / Angular / plain JS |
| Java API | Input validation, application services, REST | Spring Boot / Javalin / Jakarta Servlet |
| Algorithm engine | Self-implemented Java classes, unit tests, benchmarks | Java Collections (support only), JUnit, JMH optional |
| Persistence (optional) | Input records, experiment logs, settings | PostgreSQL / MySQL / SQLite / local files |

**Architecture rule:** Java runs the assessed algorithms on the server. The frontend must not replace them with browser libraries or precomputed results.

### 1.8 Project selection rules (PDF §7)
- The sample ideas are starting points, **not pre-approved titles**. An existing idea earns credit only through **substantial custom implementation**.
- Shopping carts, attendance forms, generic chatbots, **static dashboards** and CRUD-only sites do not qualify unless advanced algorithms drive a core feature.
- A standalone classroom visualisation of an algorithm is not enough. The technique must sit inside an authentic workflow.
- Approximation algorithms must not claim exact optimality. Bloom-filter hits mean "possibly present" and need an authoritative check.

### 1.9 Deliverables (PDF §8)
| | Deliverable | Minimum contents |
|---|---|---|
| A | Proposal (1–2 pages) | Title; team names and IDs; real-world problem; target user; features; two core techniques and why each fits; data source; measurable success criteria |
| B | Source repository | Java backend, frontend, tests, sample data, build files, dependency manifest; **readable Git history and contribution attribution** |
| C | Technical report (5–8 pages) | Problem and constraints; system design / API diagram; pseudocode; correctness discussion; time/space complexity; benchmark method and results; limitations; **individual contributions** |
| D | Demonstration evidence | Working app (local or hosted); example inputs and outputs; **evidence of error handling**; performance charts and tables; slides optional |
| E | README and reproducibility | Prerequisites; build/run commands for backend and frontend; environment variables without secrets; API examples; representative test dataset; screenshots |

### 1.10 Timeline (PDF §9, proposed; faculty confirm dates)
Week 11 group and proposal (faculty-approved scope) → Week 12 design review (architecture, data model, API contract, pseudocode) → Week 13 working prototype (Java algorithm module plus one end-to-end feature) → Week 14 validation and benchmarking → Week 15 final review, repository, report, live demo and viva.

### 1.11 Demonstration and viva (PDF §10)
- About **8 minutes per team**: realistic use case, algorithm-driven results, failure handling, performance evidence.
- About **2 minutes per student**: own contribution, a key algorithm, correctness and complexity.
- The examiner **may change the sample input, ask for an edge case, or ask for a walkthrough of a critical Java method.**
- A fully working local app is acceptable if setup is reliable.

### 1.12 Rubric (PDF §11), 100 marks
| Criterion | Marks | Assessed on | Basis |
|---|---|---|---|
| Real-world problem formulation | 10 | Clear users, credible scenario, input constraints, objective and measurable success criteria | Group |
| Algorithm selection and original implementation | 20 | ≥2 relevant syllabus techniques; CO5 or approved alternative; correct Java | Group |
| Algorithm correctness and test coverage | 20 | Correct outputs; traceable proof or explanation; edge, invalid and stress cases; **sound probabilistic behaviour** | Group |
| Complexity and empirical performance | 15 | Accurate time/space reasoning; documented baseline; reproducible experiments on scaled inputs | Group |
| Java backend and API quality | 10 | Separation of concerns, validation, error handling, maintainability, working API | Group |
| Frontend integration and usability | 5 | Complete input → execution → result workflow, clear interpretation, UI feedback | Group |
| Documentation and reproducibility | 5 | Report and README, runnable instructions, dataset attribution, repository organisation | Group |
| Live demo and individual viva | 15 | Working demo; **student's own** understanding, contribution, explanation, teamwork | **Individual** |

**What this means for planning:** 55 marks (20 + 20 + 15) go to algorithm choice, correctness and performance evidence. The frontend is worth 5. The BI features are what make the project stand out and help a postgraduate application, but they must never take time away from the algorithm, test and benchmark work.

---

## 2. Domain comparison

Scoring (0–10 per dimension) reflects how each idea would play out for this team under this rubric. Judgement calls are explained below the table.

| Dimension | LeaseLens | RevenueScope | MarketPulse | UrbanFlow |
|---|---|---|---|---|
| 1. Syllabus compatibility | 8 | 7 | 7 | 9 |
| 2. ≥2 distinct algorithms used meaningfully | 8 | 7 | 7 | 9 |
| 3. Randomized algorithm / CO5 | 8 | 8 | 8 | 7 |
| 4. Business Analytics / DS relevance | 9 | 9 | 9 | 7 |
| 5. Distinctiveness | 8 | 4 | 7 | 5 |
| 6. Dataset availability / feasibility | 6 | 8 | 9 | 9 |
| 7. EDA and statistics scope | 7 | 8 | 9 | 8 |
| 8. Feasibility for a team of two with AI assistance | 8 | 8 | 6 | 5 |
| 9. Measurable results and defensible benchmarks | 8 | 8 | 8 | 8 |
| 10. Postgraduate portfolio strength | 8 | 7 | 8 | 7 |
| **Total / 100** | **78** | **74** | **78** | **74** |

**LeaseLens (rental portfolio financial intelligence).** Daily net cash flow over date ranges, with corrections, is a natural fit for a segment tree (range sum, min and max with point updates). Payment-delay percentiles are a natural fit for randomized selection. The domain is uncommon among student projects and maps directly to real-estate asset management and FP&A analytics. Main weakness: **no realistic public rent-ledger dataset exists**, so everything is synthetic, which limits what the EDA can genuinely discover (see §3).

**RevenueScope (subscription revenue and retention).** Good fit: segment tree over daily MRR, randomized selection for customer-value percentiles. Public datasets exist (for example the IBM Telco churn sample and UCI Online Retail). However, churn and retention analysis is one of the most common student and Kaggle portfolio topics, so it scores lowest on distinctiveness. The algorithm role also tends to be secondary to cohort SQL.

**MarketPulse (marketplace seller profitability).** The strongest real-data option: the Olist Brazilian e-commerce public dataset has roughly 100k orders, which would let the 100k benchmark use real records (check the licence before use, which we understand to be non-commercial share-alike). Weaknesses: true "revenue leakage" needs commission and fee data Olist does not have, so part of the story would be speculative. The schema has many tables to join, and the dataset is well known in DS portfolios.

**UrbanFlow (urban mobility).** The best algorithm fit: max-flow (Unit 5) for fleet or dock rebalancing gives a *second* Units 4–6 technique, plus randomized selection on trip durations. Real data is abundant (NYC TLC, bike-share feeds). However, geospatial processing, very large files and the network-modelling work make it the riskiest to finish well in the time available. It is also more operations research than business analytics.

**Fifth idea considered.** I looked for something substantially stronger and did not find one. Swapping domains would not fix the main structural risk (§4.3) unless it also changed the algorithms.

**Result:** LeaseLens and MarketPulse tie. LeaseLens wins the tie on distinctiveness, feasibility and your stated preference. Its weakness (synthetic data) has a mitigation (§3.3). MarketPulse's weakness (a speculative leakage story on well-known data) does not. **Recommendation: LeaseLens, with the changes below.** MarketPulse is the fallback if you decide real data matters more than distinctiveness.

---

## 3. Critical evaluation of LeaseLens

### 3.1 Strengths
- The business question is real and well-defined: *is each asset turning billed rent into cash efficiently, and where are the operational leaks?* The users (asset managers, analysts) and decisions (collections follow-up, cost review, re-leasing priority) are believable.
- Both algorithms answer questions the user actually asks, rather than being bolted on.
- It reads as finance and BI work (cash vs accrual, receivables ageing, expense ratios, percentiles), which suits an MSBA application.

### 3.2 Weaknesses
1. **Same algorithm pair as the PDF's own sample idea.** "Segment tree + randomized Quickselect" is exactly the PDF's *Inventory risk and percentile dashboard*, moved to another domain. The PDF says an existing idea earns credit only through substantial custom implementation. The domain is distinctive; the algorithm design is not. Credit will depend on the *depth* of the work: correctness argument, workload-aware baselines, duplicate-heavy partitioning, randomization study and real integration.
2. **At demo scale, the algorithms are not needed for speed.** 36 months is about 1,096 daily points. A linear scan over 1,096 values takes microseconds, and so does sorting about 6,000 payment delays. An examiner may ask why a segment tree is needed for 1,096 numbers. The honest answer is: (a) the tree supports *many* interactive queries and *corrections* in O(log n), which matters for query-heavy workloads and longer or finer histories; (b) the benchmarks show where it wins **and where it does not** (prefix sums beat it when there are no updates). We will say this openly rather than overclaim.
3. **Synthetic-only data limits the EDA.** Analysis of generated data can only find what the generator put there. A correlation found in synthetic data is not a business finding.
4. **Multiple percentiles.** Running k separate quickselects costs O(k·n). For many percentiles at small n, one sort can win. This must be measured and explained.
5. **Censoring bias.** Excluding unpaid obligations from the delay distribution understates true delays, because the latest payments are the ones still open. This must be reported, not hidden.

### 3.3 Changes that address these weaknesses
| Weakness | Change |
|---|---|
| Same pair as sample | Deeper implementation: three-way partitioning for duplicate-heavy delay data; nearest-rank percentile with exact integer arithmetic; Las Vegas correctness study across 1,000+ seeds; comparison-count instrumentation; adversarial comparison against deterministic pivots; segment tree storing sum, min, max **and the day each min/max occurred**, with an auditable correction ledger. |
| Scale | Benchmarks at 1k / 10k / 100k (optionally 1M) with **mixed query/update workloads** compared against both a linear scan and a prefix-sum array, so the "conditions under which it helps" (PDF §5) are measured, not assumed. |
| Synthetic EDA | Frame the generator as a **scenario engine with documented, planted ground truth** (for example a retail late-payment tail, a monsoon maintenance spike, two vacancy shocks, a few outsized repairs, and deliberately *no* expense–collection link in one segment). The notebook then tests whether the analysis **recovers what was planted and does not invent what was not**. That is a legitimate method-validation exercise and is stated honestly in the report. |
| Multiple percentiles | Benchmark one sort vs k quickselects for k = 1, 3, 5, 9, and report the crossover. |
| Censoring | Show "n completed / n still open" next to every percentile, and add a censoring note in the UI and report. |

### 3.4 Verdict
LeaseLens is **academically sound and worth building**, provided the three points above are handled explicitly. Without them, it risks reading as "the PDF's sample idea with property data".

---

## 4. Algorithm and CO5 compatibility

### 4.1 Algorithm A: Segment Tree (Unit 3, Trees / range queries)
- Listed in Unit 3 ("segment trees; RMQ"; "interval analytics, fast updates and queries").
- Use: range sum, min and max of daily net cash flow (receipts minus operating expenses) for a property or the whole portfolio. Point updates when a day is corrected.
- Counts as one of the "two distinct techniques". It does **not** count towards the Units 4–6 requirement.

### 4.2 Algorithm B: Randomized Quickselect (Unit 6, randomized selection)
- Unit 6 explicitly lists "randomized selection". The PDF's job-matching and set-cover samples use "randomized selection" and are tagged CO5.
- Use: nearest-rank percentiles (P50, P75, P90 and user-defined P) of completed-payment delays.
- Satisfies **NN4** (a Units 4–6 technique) and **NN5** (CO5 randomized algorithm).

### 4.3 The biggest structural risk
**Quickselect also appears in Unit 1.** If an examiner treats our Quickselect as the Unit 1 technique, the project would fail NN4 *and* NN5 together, because nothing else comes from Units 4–6. Mitigations, in order:
1. **Present and implement it as a Unit 6 randomized algorithm:** a seeded random pivot is part of the algorithm, the proof is an expected-time argument using indicator random variables, it is described as a Las Vegas algorithm (always correct, random running time), and the work includes an empirical distribution of comparison counts across seeds plus an adversarial comparison against a fixed pivot.
2. **State the Unit 6 mapping in the proposal and get it confirmed in writing** at the Week 11 "faculty-approved scope" checkpoint.
3. **Optional low-cost insurance (declined by the team on 2026-11-24, D-003):** add **Randomized Quick Sort** (also Unit 6) for the ranked property league tables. It reuses the same three-way partition, does real work in the Property Performance module, and adds a second Unit 6 technique. Cost: about one day including tests. I do *not* recommend a Bloom filter here: an in-memory exact `HashSet` already handles duplicate-ID checks at this scale, so a Bloom filter would be decorative, which the PDF warns against.

### 4.4 CO5 evidence plan
- Seeded, injectable random source (`SplittableRandom`) so every run can be reproduced.
- Correctness: for each of 1,000+ seeds, output equals `sorted[k]` (Las Vegas).
- Behaviour: comparison counts across seeds compared with the theoretical expectation (≈ 2n–3.4n comparisons, depending on rank, for the classic analysis), plus variance.
- Contrast: deterministic first-element pivot on sorted input (Θ(n²)) vs random pivot on the same input.
- Duplicates: two-way vs three-way partitioning on realistic delay data where many values are 0.

---

## 12. Academic risks and mitigation

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Examiner classes Quickselect as Unit 1, so no Units 4–6 technique and no CO5 | Medium | **Critical** | §4.3: Unit 6 framing, **written faculty confirmation at the proposal stage** (now the only safeguard: Randomized Quick Sort was declined) |
| R2 | Seen as the PDF's inventory sample with a new label | Medium | High | Depth items in §3.3; say openly that the pairing comes from the sample and explain what we add |
| R3 | "Why a segment tree for 1,096 points?" | High | Medium | Honest workload-based justification; prefix-sum baseline; scaled benchmarks |
| R4 | Synthetic-data EDA mistaken for real findings | Medium | Medium (portfolio) | Planted-ground-truth framing; no causal claims; label every insight "synthetic" |
| R5 | AI-assisted code that a student cannot explain in the viva | Medium | **High (15 individual marks)** | Each member owns one algorithm and reviews every line of it; viva prep sheet with a line-by-line walkthrough of the critical methods; practice examiner-modified inputs. Check the course's policy on AI-assisted development. |
| R6 | Git history that does not truthfully show who did what | Medium | Medium | Each member commits their own reviewed work under their own Git identity; `CONTRIBUTIONS.md` tracks ownership; no back-dated or fabricated commits |
| R7 | Benchmark noise or JIT artefacts | Medium | Medium | Warm-up, ≥10 measured repetitions, median and IQR, fixed seeds, machine spec recorded, operation counts reported alongside time |
| R8 | Live demo breaks on the examiner's machine or modified input | Low–Medium | High | Maven wrapper, pinned npm lockfile, Java 21 target, one-command start script, invalid-CSV fixtures ready, input validation everywhere |
| R9 | Scope creep into BI polish (5 frontend marks) | High | Medium | Phase gates; algorithms, tests and benchmarks are done before UI polish |
| R10 | Inconsistent KPI definitions (for example collection efficiency above 100%) | Medium | Medium | Formula registry; cohort-based efficiency; golden hand-computed dataset tests |
| R11 | Timeline: today is 24 Nov 2026 and we do not know which course week this is | Unknown | Possibly critical | **Tell us the proposal and final-review dates**; the roadmap will be compressed to fit |
| R12 | CO definitions not in the PDF | Low | Low | Get the CO list from the course file and quote it |
| R13 | "Individual" vs "group" ambiguity in the brief | Low | Medium | Use the two-member group; remove all individual-exception wording |
