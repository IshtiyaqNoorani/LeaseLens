# Viva preparation: Ishtiyaq Noorani (2408912)

## Two-minute answer
1. **My contribution:** I own Randomized Quickselect and the nearest-rank percentile (`leaselens-algorithms/.../selection/`) with 57 tests; the payment-behaviour service and screen; the quickselect benchmarks and the CO5 seed study; the seeded data generator; and the Python EDA notebook.
2. **The algorithm:** to find the k-th smallest value, pick a pivot uniformly at random, split the values into less than, equal to and greater than the pivot (three-way), and keep only the side that contains k. It is iterative and works on a copy.
3. **Correctness (Las Vegas):** the rank-k value always stays inside [lo, hi], with smaller values to the left and larger to the right. Every round shrinks the range or returns. The answer never depends on the seed; only the running time does.
4. **Complexity:** expected O(n). A middle-half pivot occurs with probability ≥ ½ and keeps at most ¾ of the range, giving ≤ 8n; the exact analysis gives about 3.39n for the median. Worst case O(n²) with tiny probability. Space O(n) for the copy.

## Methods to be able to walk through
| Method | File and line | Key point |
|---|---|---|
| `selectInPlace(a, k, rng, counter)` | RandomizedQuickselect.java:100 | random pivot, Dutch-national-flag loop, narrow to the side that contains k |
| `nearestRankIndex(n, pHundredths)` | Percentile.java:34 | k = ⌈pH·n/10000⌉ − 1 in `long` arithmetic; P = 0 and n = 0 rejected |
| `PaymentService.percentiles` | leaselens-api/.../service/PaymentService.java | completed payments only, the censoring note, returns the seed |

## Likely questions
- **Why randomize?** A fixed first-element pivot on sorted data is Θ(n²). The benchmark shows exactly n(n−1)/2 = 49,495,500 comparisons at 10,000, against 26,825 for ours.
- **Why three-way?** About 68% of delays are 0. A two-way partition makes n(n−1)/2 comparisons on identical values; ours makes exactly n.
- **Is it faster than sorting?** For one percentile of unordered data, yes: 10.8× at 100,000. For our app's 3 percentiles on about 3,900 delay values, a single sort is as fast. It is used for its expected-linear guarantee and as the Unit 6 / CO5 technique, and we report this honestly.
- **Las Vegas vs Monte Carlo?** Las Vegas: always correct, with random time. Monte Carlo: bounded time, possibly wrong answer.
- **Proof of CO5 behaviour?** 12,000 seeded runs were all correct; mean comparisons 3.31n–3.43n against 3.39n in theory, with the maximum at 7.7n.
- **Why exclude open obligations?** Their final delay is unknown (right-censored). The EDA's Kaplan–Meier shows P90 = 19 instead of 16 days when they are included.
- **Floating-point trap?** For P = 7 and n = 100, double arithmetic gives 7.000000000000001 and the wrong rank. We use integer hundredths.
