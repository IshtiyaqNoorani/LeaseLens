# Golden dataset: hand-computed expected values

A deliberately tiny dataset whose every figure can be checked with a calculator. Automated tests compare the Java results against these numbers **exactly** (amounts in rupees here; the code works in paise).

- Horizon: 2026-01-01 to 2026-03-31 (90 days: 31 + 28 + 31). The horizon is derived from the records.
- Analysis date: 2026-03-31. Period for KPIs: the whole horizon.
- Properties: P001 Golden Residency (residential, 2,000 sq ft, acquired for ₹1,00,00,000), P002 Golden Plaza (retail, 3,000 sq ft, acquired for ₹3,00,00,000).
- Leases: L001 (P001, 1,000 sq ft, ₹25,000/month, due on the 1st), L002 (P002, 2,000 sq ft, ₹1,20,000/month, due on the 5th), L003 (P001, 1,000 sq ft, ₹30,000/month from 2026-02-01, due on the 1st).

## 1. Obligations and payments
| Obligation | Lease | Due | Amount | Payments | Status | Days late |
|---|---|---|---|---|---|---|
| O001 | L001 | 01-01 | 25,000 | 25,000 on 01-01 | PAID | 0 (on time) |
| O002 | L001 | 02-01 | 25,000 | 25,000 on 02-11 | PAID | 10 |
| O003 | L001 | 03-01 | 25,000 | 10,000 on 03-10 + 15,000 on 03-20 | PAID (settled 03-20) | 19 |
| O004 | L002 | 01-05 | 1,20,000 | 1,20,000 on 01-04 | PAID | 0 (1 day early) |
| O005 | L002 | 02-05 | 1,20,000 | 1,20,000 on 02-20 | PAID | 15 |
| O006 | L002 | 03-05 | 1,20,000 | 60,000 on 03-15 | PARTIAL | open, 26 days overdue |
| O007 | L003 | 02-01 | 30,000 | 30,000 on 02-03 | PAID | 2 |
| O008 | L003 | 03-01 | 30,000 | none | UNPAID | open, 30 days overdue |

## 2. Portfolio KPIs
| KPI | Calculation | Expected |
|---|---|---|
| Total properties | | 2 |
| Total rentable area | 2,000 + 3,000 | 5,000 sq ft |
| Occupancy on 2026-03-31 | (1,000 + 1,000 + 2,000) / 5,000 | **80.00%** |
| Time-weighted occupancy | (1,000×90 + 1,000×59 + 2,000×90) / (5,000×90) = 329,000 / 450,000 | **73.11%** |
| Rent billed (accrual) | 3×25,000 + 3×1,20,000 + 2×30,000 | **₹4,95,000** |
| Rent collected (cash) | sum of TX001–TX008 | **₹4,05,000** |
| Collection efficiency (cohort) | 4,05,000 / 4,95,000 | **81.82%** |
| Cash-to-billing ratio | 4,05,000 / 4,95,000 (all payments relate to in-period obligations) | 81.82% |
| Outstanding receivables | O006 60,000 + O008 30,000 | **₹90,000** (ageing 0–30 days: 2 obligations, ₹90,000) |
| Operating expenses | 5,000 + 3,000 + 12,000 + 40,000 + 8,000 + 2,000 | **₹70,000** |
| Net operating cash surplus | 4,05,000 − 70,000 | **₹3,35,000** |
| Expense ratio | 70,000 / 4,05,000 | **17.28%** |
| Rent per sq ft (active leases on 2026-03-31) | (25,000 + 30,000 + 1,20,000) / 4,000 | ₹43.75 per sq ft per month |

## 3. Property KPIs
| | P001 | P002 |
|---|---|---|
| Rent billed | 75,000 + 60,000 = ₹1,35,000 | ₹3,60,000 |
| Rent collected | 25,000 + 25,000 + 10,000 + 15,000 + 30,000 = ₹1,05,000 | 1,20,000 + 1,20,000 + 60,000 = ₹3,00,000 |
| Collection efficiency | 77.78% | 83.33% |
| Operating expenses | ₹20,000 | ₹50,000 |
| Net operating cash surplus | ₹85,000 | ₹2,50,000 |
| Expense ratio | 19.05% | 16.67% |
| Outstanding | ₹30,000 (O008) | ₹60,000 (O006) |
| Occupancy on 2026-03-31 | 100.00% | 66.67% |
| Time-weighted occupancy | 149,000 / 180,000 = 82.78% | 180,000 / 270,000 = 66.67% |
| Vacant area-days | 31,000 | 90,000 |
| Rent per sq ft | 55,000 / 2,000 = ₹27.50 | 1,20,000 / 2,000 = ₹60.00 |

Cash yield is excluded until Phase 4 fixes the annualisation convention.

## 4. Payment delays (Randomized Quickselect)
- Completed obligations: 6 (O001, O002, O003, O004, O005, O007). Open obligations excluded: 2 (O006, O008). On time or early: 2.
- Days late, sorted: `[0, 0, 2, 10, 15, 19]`, n = 6. Mean = 46 / 6 = **7.67** days.

| Percentile | k = ⌈P·n/100⌉ − 1 | Value |
|---|---|---|
| P10 | ⌈0.6⌉ − 1 = 0 | 0 |
| P50 | ⌈3⌉ − 1 = 2 | **2** |
| P75 | ⌈4.5⌉ − 1 = 4 | **15** |
| P90 | ⌈5.4⌉ − 1 = 5 | **19** |
| P100 | 6 − 1 = 5 | 19 |

At or above the P75 threshold (15 days): O005 (15) and O003 (19), so **2 records**.

## 5. Daily net cash flow (Segment Tree)
Day index 0 = 2026-01-01. Days without records have a net flow of 0.

| Date | Index | Portfolio net | Source |
|---|---|---|---|
| 01-01 | 0 | +25,000 | TX001 |
| 01-04 | 3 | +1,20,000 | TX005 |
| 01-10 | 9 | −5,000 | E001 |
| 01-20 | 19 | −40,000 | E004 |
| 02-03 | 33 | +30,000 | TX008 |
| 02-11 | 41 | +25,000 | TX002 |
| 02-15 | 45 | −3,000 | E002 |
| 02-20 | 50 | +1,20,000 | TX006 |
| 03-05 | 63 | −8,000 | E005 |
| 03-10 | 68 | +10,000 | TX003 |
| 03-12 | 70 | −12,000 | E003 |
| 03-15 | 73 | +60,000 | TX007 |
| 03-20 | 78 | +15,000 | TX004 |
| 03-25 | 83 | −2,000 | E006 |

| Query | Sum | Min (date) | Max (date) |
|---|---|---|---|
| Portfolio, whole horizon [0, 89] | **₹3,35,000** | −40,000 (01-20) | 1,20,000 (**01-04**; ties with 02-20, earliest wins) |
| Portfolio, January [0, 30] | ₹1,00,000 | −40,000 (01-20) | 1,20,000 (01-04) |
| Portfolio, February [31, 58] | ₹1,72,000 | −3,000 (02-15) | 1,20,000 (02-20) |
| Portfolio, March [59, 89] | ₹63,000 | −12,000 (03-12) | 60,000 (03-15) |
| P001, January [0, 30] | ₹20,000 | −5,000 (01-10) | 25,000 (01-01) |

**Correction scenario:** add +₹40,000 to P002 on 2026-01-20 (for example, a reversed tax charge). Then:
- P002's 01-20 value becomes 0.
- The portfolio's January minimum becomes −5,000 on 01-10.
- The whole-horizon sum becomes ₹3,75,000.
