# Synthetic data generator: specification and ground truth

Code: `leaselens-generator/src/main/java/com/leaselens/generator/PortfolioGenerator.java`
Regenerate the committed sample (byte-identical):

```bash
./mvnw -q -DskipTests package && java -jar leaselens-generator/target/leaselens-generator.jar sample
```

**All data is synthetic.** Names, zones, tenants and amounts are invented. No real person, property or financial record is represented. Tenants are anonymous IDs with a segment only.

## 1. Default run
| Setting | Value |
|---|---|
| Seed | 20261124 |
| Horizon | 2023-11-01 to 2026-10-31 (36 months, 1,096 days) |
| Properties | 50 (20 residential, 14 office, 11 retail, 5 industrial) |
| Output, committed in `data/sample` | 195 tenants · 248 leases · 3,980 rent obligations · 4,194 transactions · 9,699 expenses |
| Validation | 0 errors, 0 warnings |

**Reproducibility:**
- Every property has its own random stream derived from the seed, so changing one property's logic does not reshuffle the others.
- All maths uses `StrictMath` and integer paise, and dates and numbers are formatted independently of the computer's locale. Files use UTF-8 with LF line endings.
- `PortfolioGeneratorTest.committedSampleIsReproducible` fails if the committed files ever differ from a fresh run.

## 2. Modelling assumptions
These are simplifications chosen for clarity, not market estimates.

| Aspect | Residential | Office | Retail | Industrial |
|---|---|---|---|---|
| Units per property | 2–4 | 1–4 | 1–3 | 1–2 |
| Unit area (sq ft) | 650–1,600 | 1,500–6,000 | 800–3,500 | 12,000–40,000 |
| Base rent (₹/sq ft/month) × zone factor 0.85–1.25 | 28–45 | 70–130 | 90–180 | 18–32 |
| Gross yield used to set acquisition value | 3.0–4.5% | 7.0–8.5% | 7.5–9.5% | 8.0–9.5% |
| Occupied at horizon start | 90% | 82% | 85% | 90% |
| Lease terms (months) | 11 / 23 / 35 | 36 / 48 / 60 | 36 / 60 | 36 / 60 |
| Vacancy between tenants (days) | 15–75 | 60–240 | 45–180 | 30–150 |
| Renewal by the same tenant | 35% | 50% | 50% | 60% |
| Tenant behaviour mix: punctual / occasional / chronic / defaulter | 62 / 24 / 10 / 4% | 66 / 22 / 9 / 3% | 45 / 28 / 20 / 7% | 75 / 17 / 6 / 2% |

**Rent:**
- New leases escalate rent 5% a year from the horizon start.
- Rent falls due monthly on the lease's billing day. The first instalment is on the first billing day on or after the lease start. There is no pro-rating.

**Payment behaviour (days late):**

| Behaviour | Pays early | Pays on time | Pays late |
|---|---|---|---|
| Punctual | 25% (1–4 days) | 60% | 15% (1–5 days) |
| Occasional | 10% | 40% | 50% (exponential, mean 9 days) |
| Chronic | none | 10% | 90% (4 + exponential, mean 20 days) |

- Late delays are capped at 150 days.
- Some payments are split into 2–3 instalments (punctual 3%, occasional 12%, chronic 25%).
- **Short payments:** some instalments are only 50–90% paid and the rest stays owed. Chance: punctual 0.2%, occasional 3%, chronic 12%; ×1.5 for retail.
- **Defaulters** pay like chronic tenants for 3–10 instalments, then stop. Their lease is terminated 3 months later.
- **Censoring at the horizon end:** payments after 2026-10-31 are not recorded, so the latest obligations can be open.

**Expenses (cash basis):**
- Management fee: 4% of potential rent, monthly.
- Security, and cleaning for offices and retail: per sq ft, monthly.
- Utilities: 2% of potential rent, adjusted for season (summer ×1.3, monsoon ×1.1).
- Property tax: 5% of potential annual rent, paid in June.
- Insurance: 0.1% of acquisition value, paid once a year.
- Maintenance jobs and repairs: random counts (Poisson) with log-normal amounts.

## 3. Planted ground truth
The analysis (Phase 7 notebook, and the app's Phase 4–5 views) should **recover** these patterns, and should not report patterns that were not planted.

| # | Pattern | How it was planted | Observed in the committed sample |
|---|---|---|---|
| 1 | Retail pays later | Retail behaviour mix is heavier, and retail delays are multiplied by 1.4 | Mean days late (completed payments): retail **9.42**, residential 2.97, office 4.02, industrial 2.58 |
| 2 | Monsoon maintenance spike | Maintenance jobs ×1.8 and amounts ×1.25 in June–September; repairs ×1.6 | Maintenance + repairs per month: monsoon ₹14.7 lakh vs other months ₹7.8 lakh (**×1.89**) |
| 3 | Vacancy shock | Offices **P005** and **P006** have no lease running from 2024-09-01 to at least 2025-05-01 | Next leases start between 2025-05-10 and 2025-06-19 |
| 4 | Outsized repairs | 1.2% of repairs are multiplied by 8–15 | 8 of 507 repair records exceed 8× the median repair (₹21,919) |
| 5 | Degrading collections | Office **P008**: from month 18 (May 2025), 30% of instalments are only 40–80% paid and delays grow | Cohort collection efficiency 96.46% before May 2025 → **78.57%** after |
| 6 | No direct expense–collection link (negative control) | Expenses never depend on rent collected; both scale with property size | A raw correlation between them should appear because of size. Per-sq-ft or within-property analysis should show **no** direct link. Reporting a causal link would be an error. |

**An unplanted pattern to be aware of:** office P010 has the lowest collection efficiency before May 2025 (64%). This comes from random tenant behaviour (a defaulting tenant), not from a planted rule. Not every outlier is planted, and the analysis should describe it without inventing a cause.

## 4. Portfolio-level figures (committed sample)
Measured from the data, not targets:
- Obligation status: 3,841 paid, 112 partial, 27 unpaid.
- Cohort collection efficiency over the horizon: **98.01%**. Outstanding at the horizon end: ₹1,61,66,300.
- Completed payments: 2,645 of 3,841 were 0 days late. Nearest-rank P50 = 0, P75 = 3, P90 = 14 days.

## 5. Invalid fixtures and golden dataset
- `data/golden/`: 2 properties, 3 leases, 8 obligations. Every KPI is hand-computed in `data/golden/EXPECTED.md`.
- `data/invalid/`: 23 copies of the golden dataset, each with one planted defect (see `data/invalid/README.md`). Regenerate with the `fixtures` command.

## 6. Limitations
- Behaviour is drawn per tenant and does not change with the economy. No inflation shocks, no seasonality in payment delays.
- There is no pro-rating, there are no deposits, and there are no rent-free periods.
- Rent escalation is a smooth 5% a year rather than stepped contractual escalation.
- Operating expenses are independent of occupancy, except through property size.
- Because these assumptions are the generator's own, conclusions drawn from the sample show what the methods can do. They are **not** facts about real property markets.
