# Project Proposal: CIA 3 Advanced Algorithm (draft v0.1)

**Title:** LeaseLens: Rental Portfolio Cash-Flow and Payment-Behaviour Analytics using Segment Trees and Randomized Selection

**Team (group of two)**

| Name | Student ID | Primary ownership (proposed) |
|---|---|---|
| Raakin Majeed | 2404537 | Segment Tree, Cash-Flow Explorer, segment-tree benchmarks |
| Ishtiyaq Noorani | 2408912 | Randomized Quickselect, Payment Behaviour, CO5 study, EDA |

Shared: data ingestion and validation, KPI services, frontend, technical report.

## 1. Real-world problem
Operators of mixed residential and commercial rental portfolios keep billing, collections and operating costs in separate ledgers. Basic management questions are slow and error-prone to answer: *How much net cash did a property, or the whole portfolio, generate between two dates, and what was the worst day? How late do tenants typically pay, and which payments are in the slow tail? Which assets turn billed rent into cash least efficiently?* Records are also corrected after the fact (reversed charges, late postings), so the answers must update cheaply when that happens.

## 2. Target users
Portfolio and asset managers, commercial property operators, real-estate investment analysts and BI analysts. LeaseLens supports their decisions (collections follow-up, cost review, asset comparison). It is not a tenant-administration or listing system, and it does not score individual tenants.

## 3. Proposed features
1. **Data workspace:** upload six CSV ledgers or load the sample; detailed validation report (duplicates, unknown references, invalid dates, over-payments, reconciliation mismatches).
2. **Portfolio overview:** occupancy, rent billed (accrual) vs collected (cash), collection efficiency, receivables ageing, operating expenses, net operating cash surplus (cash basis), and rule-based attention flags.
3. **Property performance:** league table and comparisons by type and zone.
4. **Cash-flow explorer (Segment Tree):** range net cash flow, worst and best day for any date range and scope; period comparisons; corrections applied as point updates.
5. **Payment behaviour (Randomized Quickselect):** median, P75, P90 and custom percentiles of days late; records at or above a threshold; distribution; open receivables kept separate.
6. **Operating expense analysis:** category trends, expense-to-rent ratio, unusually large records (Tukey fences).
7. **Algorithm performance lab:** measured comparison against baselines at 1k, 10k and 100k inputs.

## 4. Core techniques and why each fits
**(a) Segment Tree (Unit 3, trees and range queries).** Daily net cash flow forms an array indexed by day. Analysts ask many range questions (sum, min, max over arbitrary periods), and individual days are corrected. A segment tree answers each query and applies each correction in O(log n) after an O(n) build, using O(n) space. A linear scan costs O(n) per query. Prefix sums give O(1) queries but O(n) per correction and cannot answer min or max. The segment tree is the structure that handles **queries and corrections together**. Our benchmarks will measure where this matters and where it does not.

**(b) Randomized Quickselect (Unit 6, randomization: randomized selection; CO5).** Percentiles of payment delays need the k-th smallest value, not a full ordering. Randomized selection with a uniformly random pivot runs in **expected O(n)** time on every input (worst case O(n²), with vanishing probability), compared with O(n log n) for sorting. It is a **Las Vegas** algorithm: always correct, with randomness only in running time. We use a three-way partition because delay data contains many equal values (for example on-time payments recorded as 0 days late). Percentiles use the nearest-rank rule k = ⌈P·n/100⌉ − 1.

> **Request to faculty:** Quickselect appears in both Unit 1 and Unit 6 of the syllabus. We are implementing and analysing it as the **Unit 6 randomized-selection** technique (random pivot, expected-time analysis, seed-variance study) to meet the Units 4–6 and CO5 requirements. Please confirm this mapping when approving the scope.

## 5. Data source
Realistic **synthetic** data from our own seeded Java generator: about 50 properties, 200–240 leases, 36 months (1,096 days) of rent obligations, payments (including late, partial and unpaid) and operating expenses. It contains no real people, properties or financial records. Some behaviour patterns are planted deliberately and documented, so the analysis can be checked against a known ground truth. Users can also upload their own CSVs in the same format.

## 6. Measurable success criteria
1. Segment-tree results equal brute-force results on 100% of ≥10,000 seeded random query and update cases.
2. Quickselect equals the sorted-array reference on 100% of cases across ≥1,000 random seeds and ≥6 input shapes, including duplicates and all-identical values.
3. All KPIs on a hand-computed reference dataset match the expected values exactly (to the paisa).
4. Every planted data defect in the invalid-data test suite is detected with a clear message, and no partial result is returned.
5. Benchmarks at 1,000, 10,000 and 100,000 inputs (≥10 timed repetitions after warm-up) against a linear scan, prefix sums and full sorting, with the crossover conditions reported.
6. The application builds and runs from a fresh clone using the README instructions.

## 7. Technology
Java 21 + Spring Boot (REST); React + Vite + Tailwind + Recharts; JUnit 5; Python (pandas, NumPy, Matplotlib) for a supplementary exploratory analysis only. The assessed algorithms are hand-written in a Java module with no third-party dependencies.
