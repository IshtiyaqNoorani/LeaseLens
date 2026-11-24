# Benchmarks: method, results and findings

Every number in this document is copied by script from the committed CSV files in `benchmarks/results/`. Nothing is estimated or rounded by hand. The same files feed the Algorithm Lab screen.

## 1. Reproduce
```bash
./mvnw -q -DskipTests package
java -Xms1g -Xmx1g -jar leaselens-bench/target/leaselens-bench-cli.jar --suite all --sizes 1000,10000,100000 --reps 20 --warmup 10 --seed 42 --seeds 1000
```
This takes about 30 seconds on the machine below and overwrites `benchmarks/results/*.csv`. Rebuild the API afterwards so the Lab serves the new files.

## 2. Method
| Aspect | Choice |
|---|---|
| Machine (committed run) | Mac OS X 15.7.3 aarch64, 10 cores, Java 24.0.2 HotSpot, `-Xms1g -Xmx1g` (fixed heap to reduce GC noise) |
| Sizes | n = 1,000 · 10,000 · 100,000 (PDF §5 requires at least three increasing sizes) |
| Warm-up | 10 untimed runs of each task before timing, so measurements reflect JIT-compiled code |
| Repetitions | 20 timed samples per task; **median** reported with p25–p75 (IQR), min, max and mean in the CSV |
| Timer | `System.nanoTime`. Tasks shorter than 1 ms are repeated back to back inside one sample until the sample lasts at least 1 ms; the reported time is per run |
| Dead-code protection | every task returns a checksum that is added to a `volatile` field |
| Same inputs | algorithm and baselines receive identical seeded inputs (seed 42) and identical operation sequences |
| Correctness first | before timing, every method's answers are compared; a disagreement aborts the run. All 168 measurements verified |
| Machine-independent cost | an instrumented run counts tree nodes visited, elements scanned, prefix entries rewritten, or element comparisons |
| Fair copying | quickselect `select()` copies its input; the sort baseline also copies then sorts |
| Quadratic baselines | textbook two-way quickselect is run only up to n = 10,000 on its bad inputs (sorted with first pivot; identical values); at 100,000 it would need about 5×10⁹ comparisons |
| Harness | a small custom harness, chosen for transparency and for the live Lab runs. JMH (optional in PDF §6) would add fork isolation; see threats to validity |

**Baselines (labelled, not assessed):** `LinearScan` (sum/min/max by scanning), `PrefixSums` (O(1) range sum, O(n) update, **cannot answer min or max**), `Arrays.sort` (JDK dual-pivot quicksort, highly optimised production code), `ClassicQuickselect` (textbook two-way Lomuto partition; first-element or random pivot).

**Segment-tree workload:** build a fresh structure, then run 10,000 operations. A given share are point updates (corrections, 0 / 1 / 10 / 50%); the rest are range queries over random ranges (average length about n/3) or short ranges (≤ 32 days). Build time is included because every method must build first.

## 3. Results
### T1. Segment Tree workload (10,000 operations, random ranges)

| n | Corrections | Segment Tree | Linear scan | × tree | Prefix sums (sum only) | × tree |
|---|---|---|---|---|---|---|
| 1,000 | 0% | 1.17 ms | 1.99 ms | 1.7 | 9.1 µs | 0.01 |
| 1,000 | 1% | 1.14 ms | 1.99 ms | 1.7 | 15.2 µs | 0.01 |
| 1,000 | 10% | 991.5 µs | 1.83 ms | 1.8 | 53.2 µs | 0.05 |
| 1,000 | 50% | 928.5 µs | 1.04 ms | 1.1 | 230.6 µs | 0.25 |
| 10,000 | 0% | 1.68 ms | 17.11 ms | 10.2 | 22.8 µs | 0.01 |
| 10,000 | 1% | 1.61 ms | 16.93 ms | 10.5 | 48.9 µs | 0.03 |
| 10,000 | 10% | 1.59 ms | 15.80 ms | 9.9 | 341.2 µs | 0.21 |
| 10,000 | 50% | 1.53 ms | 8.75 ms | 5.7 | 1.71 ms | 1.12 |
| 100,000 | 0% | 4.08 ms | 168.68 ms | 41.4 | 64.9 µs | 0.02 |
| 100,000 | 1% | 3.99 ms | 166.10 ms | 41.6 | 651.8 µs | 0.16 |
| 100,000 | 10% | 3.86 ms | 150.18 ms | 38.9 | 5.57 ms | 1.44 |
| 100,000 | 50% | 3.67 ms | 86.04 ms | 23.4 | 28.93 ms | 7.88 |

### T2. Short ranges (≤ 32 days), 10,000 operations

| n | Corrections | Segment Tree | Linear scan | × tree |
|---|---|---|---|---|
| 1,000 | 0% | 786.1 µs | 238.1 µs | 0.30 |
| 1,000 | 10% | 766.1 µs | 265.5 µs | 0.35 |
| 10,000 | 0% | 1.19 ms | 261.7 µs | 0.22 |
| 10,000 | 10% | 1.10 ms | 229.1 µs | 0.21 |
| 100,000 | 0% | 2.75 ms | 339.4 µs | 0.12 |
| 100,000 | 10% | 2.97 ms | 306.0 µs | 0.10 |

### T3. Build cost

| n | Segment Tree | Prefix sums |
|---|---|---|
| 1,000 | 13.0 µs | 2.9 µs |
| 10,000 | 142.5 µs | 15.8 µs |
| 100,000 | 2.64 ms | 97.2 µs |

### T4. One percentile (P90): Randomized Quickselect vs full sort

| n | Input | Quickselect | Comparisons | Comparisons ÷ n | Arrays.sort | sort ÷ quickselect |
|---|---|---|---|---|---|---|
| 1,000 | uniform | 13.3 µs | 3,145 | 3.15 | 20.3 µs | 1.53 |
| 1,000 | delays | 3.0 µs | 1,895 | 1.90 | 9.7 µs | 3.24 |
| 1,000 | sorted | 2.8 µs | 2,272 | 2.27 | 1.5 µs | 0.52 |
| 1,000 | reverse | 15.1 µs | 3,562 | 3.56 | 4.4 µs | 0.29 |
| 1,000 | identical | 1.9 µs | 1,000 | 1.00 | 1.3 µs | 0.68 |
| 10,000 | uniform | 23.6 µs | 28,565 | 2.86 | 140.0 µs | 5.93 |
| 10,000 | delays | 21.7 µs | 20,141 | 2.01 | 22.2 µs | 1.02 |
| 10,000 | sorted | 18.6 µs | 26,825 | 2.68 | 3.3 µs | 0.18 |
| 10,000 | reverse | 17.3 µs | 24,935 | 2.49 | 6.4 µs | 0.37 |
| 10,000 | identical | 7.0 µs | 10,000 | 1.00 | 3.2 µs | 0.46 |
| 100,000 | uniform | 333.2 µs | 181,634 | 1.82 | 3.59 ms | 10.77 |
| 100,000 | delays | 531.2 µs | 261,587 | 2.62 | 453.9 µs | 0.85 |
| 100,000 | sorted | 343.2 µs | 404,213 | 4.04 | 144.3 µs | 0.42 |
| 100,000 | reverse | 258.2 µs | 248,236 | 2.48 | 63.1 µs | 0.24 |
| 100,000 | identical | 69.1 µs | 100,000 | 1.00 | 29.1 µs | 0.42 |

### T5. k percentiles: sort time ÷ quickselect time (above 1 = quickselect faster)

| n | Input | k=1 | k=3 | k=5 | k=9 | k=19 |
|---|---|---|---|---|---|---|
| 1,000 | uniform | 2.33 | 1.11 | 0.66 | 0.51 | 0.24 |
| 1,000 | delays | 1.46 | 0.99 | 0.58 | 0.34 | 0.15 |
| 10,000 | uniform | 6.52 | 3.18 | 1.22 | 0.82 | 0.49 |
| 10,000 | delays | 1.32 | 0.67 | 0.43 | 0.25 | 0.11 |
| 100,000 | uniform | 13.99 | 2.74 | 1.73 | 1.19 | 0.74 |
| 100,000 | delays | 1.13 | 0.69 | 0.57 | 0.37 | 0.22 |

### T6. Pivot rule and partition scheme (P90)

| Test | n | Input | Method | Comparisons | ÷ n | Median time |
|---|---|---|---|---|---|---|
| Pivot rule | 1,000 | sorted | RandomizedQuickselect (assessed) | 2,272 | 2.3 | 1.7 µs |
| Pivot rule | 1,000 | sorted | Quickselect3Way-FirstPivot | 29,915 | 29.9 | 20.2 µs |
| Pivot rule | 1,000 | sorted | Classic2Way-FirstPivot | 494,550 | 494.6 | 77.2 µs |
| Partition | 1,000 | delays | RandomizedQuickselect (assessed) | 1,895 | 1.9 | 2.0 µs |
| Partition | 1,000 | delays | Classic2Way-RandomPivot | 4,440 | 4.4 | 2.1 µs |
| Partition | 1,000 | identical | RandomizedQuickselect (assessed) | 1,000 | 1.0 | 1.0 µs |
| Partition | 1,000 | identical | Classic2Way-RandomPivot | 494,550 | 494.6 | 103.5 µs |
| Pivot rule | 10,000 | sorted | RandomizedQuickselect (assessed) | 26,825 | 2.7 | 18.8 µs |
| Pivot rule | 10,000 | sorted | Quickselect3Way-FirstPivot | 940,410 | 94.0 | 622.1 µs |
| Pivot rule | 10,000 | sorted | Classic2Way-FirstPivot | 49,495,500 | 4949.6 | 9.46 ms |
| Partition | 10,000 | delays | RandomizedQuickselect (assessed) | 20,141 | 2.0 | 20.4 µs |
| Partition | 10,000 | delays | Classic2Way-RandomPivot | 66,231 | 6.6 | 18.8 µs |
| Partition | 10,000 | identical | RandomizedQuickselect (assessed) | 10,000 | 1.0 | 9.0 µs |
| Partition | 10,000 | identical | Classic2Way-RandomPivot | 49,495,500 | 4949.6 | 9.46 ms |
| Pivot rule | 100,000 | sorted | RandomizedQuickselect (assessed) | 404,213 | 4.0 | 272.2 µs |
| Pivot rule | 100,000 | sorted | Quickselect3Way-FirstPivot | 29,703,987 | 297.0 | 19.42 ms |
| Partition | 100,000 | delays | RandomizedQuickselect (assessed) | 261,587 | 2.6 | 390.2 µs |
| Partition | 100,000 | identical | RandomizedQuickselect (assessed) | 100,000 | 1.0 | 88.9 µs |

### T7. Randomization study: comparisons across 1,000 seeds

| n | Input | Rank | All correct | Mean ÷ n | P95 ÷ n | Max ÷ n | Theory ÷ n |
|---|---|---|---|---|---|---|---|
| 1,000 | distinct | median | yes | 3.31 | 5.06 | 6.88 | 3.39 |
| 1,000 | distinct | P90 | yes | 2.66 | 4.23 | 6.49 | |
| 1,000 | delays | median | yes | 1.30 | 2.45 | 3.75 | |
| 1,000 | delays | P90 | yes | 1.85 | 2.56 | 4.13 | |
| 10,000 | distinct | median | yes | 3.43 | 5.23 | 7.61 | 3.39 |
| 10,000 | distinct | P90 | yes | 2.63 | 4.26 | 6.06 | |
| 10,000 | delays | median | yes | 1.31 | 2.46 | 3.66 | |
| 10,000 | delays | P90 | yes | 1.91 | 2.62 | 3.67 | |
| 100,000 | distinct | median | yes | 3.42 | 5.19 | 7.67 | 3.39 |
| 100,000 | distinct | P90 | yes | 2.63 | 4.29 | 6.68 | |
| 100,000 | delays | median | yes | 1.31 | 2.00 | 3.52 | |
| 100,000 | delays | P90 | yes | 1.85 | 2.56 | 3.86 | |

## 4. Findings and the conditions under which each algorithm helps

**Segment Tree**
1. **Long-range queries with min/max: the tree wins, increasingly with n.** Against a linear scan it is 1.7× faster at 1,000, about 10× at 10,000 and about 40× at 100,000 (0–10% corrections). This growth matches O(log n) per query against O(n). At 100,000 the tree visits about 60 nodes per operation.
2. **Short ranges: the linear scan wins.** For ranges of 32 days or fewer, scanning is 3–10× faster (0.1–0.3× the tree's time). Recursion and result-object overhead outweigh reading a few values.
3. **Prefix sums win for sum-only queries with rare corrections**, often by more than 20×, because a query is O(1). They lose once corrections are frequent at scale: 1.44× slower than the tree at 100,000 with 10% corrections, and 7.9× slower with 50%. **They cannot produce the worst day or best day at all**, which the Cash-Flow Explorer needs.
4. **Building the tree costs more:** 4–27× slower to build than a prefix array (2.6 ms vs 0.1 ms at 100,000). LeaseLens builds each tree once, when a dataset is loaded.
5. **At LeaseLens's own scale** (n = 1,096 days per tree), any method answers in microseconds. The tree is justified by supporting sum, min and max together, by O(log n) corrections, and by scaling to longer or finer histories, not by speed on 1,096 values. The report says so.

**Randomized Quickselect**
6. **One percentile of unordered data: quickselect wins, increasingly with n.** On uniform data it is 1.5× faster than sorting at 1,000, 5.9× at 10,000 and 10.8× at 100,000, consistent with expected O(n) against O(n log n).
7. **Presorted, reverse-sorted or identical input: the JDK sort is faster** (0.18–0.68× quickselect's time). It detects ordered runs and equal keys, while quickselect still performs about 1–4 comparisons per element.
8. **Duplicate-heavy delay data: roughly a tie.** Sort ÷ quickselect is 1.02 at 10,000 and 0.85 at 100,000, because the JDK sort is fast on many equal keys.
9. **Several percentiles: there is a crossover.** On uniform data, k separate selections beat one sort up to about k = 5 at 10,000 and k = 9 at 100,000. On delay data, one sort is already faster from k = 3 at 10,000 and above. **LeaseLens computes 3 percentiles on about 3,900 delays, where a single sort would be as fast or faster.** Randomized selection is used for its expected-linear guarantee on a single percentile of large unordered data, for the threshold and quartile queries, and as the syllabus Unit 6 / CO5 technique. It is not claimed to be faster for this particular workload.
10. **A random pivot avoids the adversarial case.** With a fixed first-element pivot on sorted input, the textbook two-way version makes exactly n(n−1)/2 comparisons: 49,495,500 at 10,000, or 4,950 per element, taking 9.5 ms against 18.8 µs for the randomized version (about 500×). Our three-way partition with a fixed pivot also grows faster than linear (94 per element at 10,000, 297 at 100,000).
11. **The three-way partition handles equal values.** On all-identical input, a two-way partition with a random pivot still makes n(n−1)/2 comparisons, while the three-way version makes exactly n (one pass).
12. **Randomization study (CO5).** All 12,000 seeded runs (12 configurations × 1,000 seeds) returned the correct value. For the median of distinct values, mean comparisons were 3.31n, 3.43n and 3.42n at the three sizes, against the theoretical 2(1 + ln 2)n ≈ 3.39n. The 95th percentile was about 5.2n and the maximum at most 7.7n: no run came close to quadratic. On delay data the mean is about 1.3n, because a pivot of 0 (68% of values) often ends the search at once.

## 5. Threats to validity
- **One machine and one JVM.** Absolute times will differ elsewhere; ratios and operation counts transfer better.
- **No JMH fork isolation.** Experiments run one after another in a single JVM, so later experiments may benefit from earlier JIT work. Warm-up per task, a fixed heap and medians with IQR reduce, but do not remove, this risk.
- **Timer resolution.** It is addressed by batching sub-millisecond tasks; the smallest tasks (about 1–3 µs) are still the least precise.
- **Baseline quality differs.** `Arrays.sort` is heavily optimised library code, while our implementations aim for clarity. This makes the comparisons conservative for our algorithms.
- **Synthetic inputs.** The delay shape imitates the sample data (68% zeros, long tail) but is not real payment data.
- **Live runs** from the Lab share the CPU with the web server and use fewer warm-ups. They are indicative only: for example, a live run gave a sort ÷ quickselect ratio of 2.96 on 10,000 delays, against 1.02 in the committed run.
