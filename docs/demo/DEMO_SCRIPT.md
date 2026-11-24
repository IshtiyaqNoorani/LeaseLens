# Live demo script (about 8 minutes)

**Before the session:** start both servers (README "Run"), open http://localhost:5173, set the browser to full screen, and reset any corrections on the Cash-Flow Explorer. Keep a terminal open at the repository root and the folder `data/invalid/15-overpayment` ready to drag into the upload field.

| Time | Who | Screen | Say / do |
|---|---|---|---|
| 0:00–0:45 | Ishtiyaq | Overview | The problem: portfolio managers can't easily see cash flow, collections and costs together. LeaseLens is decision support, not tenant admin. Point to the headline surplus (cash basis, not NOI), collection efficiency and the attention list (transparent rules). Note that all data is synthetic and seeded. |
| 0:45–1:30 | Raakin | Data Workspace | Upload the 6 files from `data/invalid/15-overpayment`: the **OVERPAYMENT** rule is shown with its file, line and column, and nothing is loaded. Click *Load sample portfolio*. "Every dataset passes 20 rules; we never return partial results." |
| 1:30–3:30 | Raakin | Cash-Flow Explorer | Algorithm A. Set the range to 1 Apr 2025 to 31 Mar 2026. Total, worst day and best day come from the Segment Tree: point at the Algorithm panel (nodes visited vs 365 days a scan would read). Switch to quarters ("one query per period"). Add a correction on the worst day (+₹1,50,000, with a reason). The before/after tiles show the change, and the panel shows 22 nodes touched (two root-to-leaf paths). Undo. Explain O(log n) query and update and why prefix sums can't give min/max. |
| 3:30–5:30 | Ishtiyaq | Payment Behaviour | Algorithm B. P50/P75/P90 from Randomized Quickselect; the nearest-rank rule is on screen. Click *Re-run with a new random seed* twice: the percentiles stay identical and the comparison counts change (Las Vegas, CO5). Filter to Retail (P90 rises: the planted pattern). Point to the censoring note and the separate outstanding table. |
| 5:30–7:15 | Both | Algorithm Lab | Raakin: Segment Tree vs scan vs prefix sums. The tree's lead grows with n (about 39× at 100k with 10% corrections), but a scan wins on short ranges and prefix sums win with no corrections. Ishtiyaq: quickselect vs sort (10.8× at 100k for one percentile), the k-percentile crossover, the textbook first-pivot version making n(n−1)/2 comparisons, and the 1,000-seed histogram around 3.39n. |
| 7:15–8:00 | Either | Terminal | Run `./mvnw -q -pl leaselens-algorithms test` (79 algorithm tests pass in seconds). Close: 172 tests, golden dataset matched to the paisa, EDA recovered the planted patterns. |

**If the examiner changes the input:** the date range, scope, percentiles (for example `37.5,99.9`), seed, threshold and fence are all editable live. For an edge case, try `p=0` (rejected with a message), a reversed date range (400 with an explanation), or a range ending after the analysis date.

**Fallbacks:** if the UI fails, every result is also available with `curl` (README API examples). Screenshots of every screen are in `docs/screenshots/`.
