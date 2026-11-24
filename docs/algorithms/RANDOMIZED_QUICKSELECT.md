# Randomized Quickselect: design, pseudocode, correctness and complexity

Owner: **Ishtiyaq Noorani** · Syllabus: **Unit 6, randomized selection (CO5)** · Code: `leaselens-algorithms/src/main/java/com/leaselens/algorithms/selection/{RandomizedQuickselect, Percentile}.java` · Tests: `RandomizedQuickselectTest` (19 test cases), `PercentileTest` (38 test cases)

## 1. Business role
Payment Behaviour reports percentiles of **days late** for completed rent payments: the median (P50), P75, P90, and any percentile the user enters. Each percentile is one order statistic, the k-th smallest value, so a full sort is unnecessary.

**Nearest-rank rule:** `k = ⌈P/100 · n⌉ − 1` (zero-based), with `0 < P ≤ 100`. P is held in **hundredths of a percent** and k is computed with integer arithmetic: `k = ⌈pH · n / 10000⌉ − 1`. With doubles, P = 7 and n = 100 would give 0.07 × 100 = 7.000000000000001, round up to 8, and return the wrong rank. A test covers this case.

## 2. Pseudocode
```
SELECT(a[0..n−1], k, rng)                  ▷ precondition: n ≥ 1, 0 ≤ k < n
    lo ← 0; hi ← n − 1
    loop
        if lo = hi: return a[lo]
        pivot ← a[lo + rng.uniform(0 .. hi−lo)]
        ▷ three-way (Dutch national flag) partition of a[lo..hi]
        lt ← lo; i ← lo; gt ← hi
        while i ≤ gt
            if a[i] < pivot: swap(a[lt], a[i]); lt++; i++
            else if a[i] > pivot: swap(a[i], a[gt]); gt−−
            else: i++
        ▷ now a[lo..lt−1] < pivot, a[lt..gt] = pivot, a[gt+1..hi] > pivot
        if k < lt: hi ← lt − 1
        else if k > gt: lo ← gt + 1
        else: return pivot
```
The public `select` copies the input first, so the caller's data is never changed. `selectMany` runs several selections on one copy.

## 3. Correctness
**Partition invariant.** During the inner loop: `a[lo..lt−1] < pivot`, `a[lt..i−1] = pivot`, `a[gt+1..hi] > pivot`, and `a[i..gt]` is unexamined.
- It holds at the start, since all three regions are empty.
- Each branch keeps it: a smaller element is swapped into the end of the "<" region; a larger element is swapped into the front of the ">" region (and the element swapped in is examined next, because `i` does not advance); an equal element simply extends the "=" region.
- The loop ends when `i > gt`, so the unexamined region is empty and the three regions cover `[lo, hi]`.

**Outer-loop invariant (J).** The value `s = sorted(a)[k]` lies in `a[lo..hi]`, every element left of `lo` is ≤ every element in `a[lo..hi]`, and every element right of `hi` is ≥ every element in `a[lo..hi]`.
- (J) holds at the start (`lo = 0, hi = n − 1`).
- After a partition, elements in `[lo, lt−1]` have ranks below `lt`, elements equal to the pivot fill ranks `lt..gt`, and larger elements have ranks above `gt`. Narrowing to the side that contains rank k, or returning the pivot when `lt ≤ k ≤ gt`, therefore preserves (J) or returns `s`. When `lo = hi`, (J) gives `a[lo] = s`.

**Termination.** The "=" block always contains the pivot, so each round either returns or shrinks `[lo, hi]` by at least one element.

**Las Vegas.** The random choices affect only which pivots are picked, never whether the output is correct. Every run returns `sorted[k]`. Tested over **1,000 seeds × 6 input shapes** against `Arrays.sort`.

## 4. Complexity
Let a round on a subarray of size m cost m comparisons (one per element, three-way).

**Expected time O(n): short proof.** Call a pivot *good* if its rank lies in the middle half of the current subarray. This happens with probability ≥ 1/2, and equal keys can only make the recursive side smaller. After a good pivot, the side kept has at most 3m/4 elements. The number of rounds until a good pivot is geometric, with expectation ≤ 2. So the expected work while the size is between n(3/4)^(j+1) and n(3/4)^j is at most 2·n(3/4)^j. Summing over j:
`E[comparisons] ≤ 2n · Σ (3/4)^j = 8n = O(n)`.

**Sharper constant.** The classical analysis of randomized selection (Knuth) gives an expected comparison count of at most `2n(1 + ln 2) ≈ 3.39n` for the median, the hardest rank, and less for other ranks. `expectedComparisonsAreLinear` measures the mean for the median of 10,001 distinct values over 500 seeds and checks it lies between 2n and 3.6n.

**Worst case Θ(n²).** If every pivot is the extreme element of its subarray, the rounds cost n + (n−1) + … . With random pivots this needs a long run of unlikely choices: each round picks an extreme pivot with probability at most 2/m.

**Space.** O(n) for the defensive copy and O(1) extra. The loop is iterative, so there is no recursion stack.

**Duplicates.** With a two-way partition, an array of identical values gives Θ(n²) even with random pivots, because each round removes only the pivot. The three-way partition puts all equal values in the "=" block, so identical input costs **exactly n comparisons** (tested).

## 5. Why randomization matters: Phase 2 measurement
Selecting the maximum of sorted input `0..n−1`, counting comparisons with `OperationCounter` (single runs, seed 1 for the random pivot; operation counts, not timings):

| n | Fixed first-element pivot | per element | Random pivot | per element |
|---|---|---|---|---|
| 1,000 | 29,820 | 29.8 | 1,081 | 1.08 |
| 2,000 | 84,336 | 42.2 | 6,518 | 3.26 |
| 4,000 | 238,520 | 59.6 | 12,287 | 3.07 |
| 8,000 | 674,625 | 84.3 | 16,287 | 2.04 |
| 16,000 | 1,908,140 | 119.3 | 24,287 | 1.52 |

**What this shows:**
- With the fixed pivot, the cost per element rises by about √2 each time n doubles. That is super-linear growth (roughly n^1.5 in this range).
- With the random pivot, the cost stays a small constant per element, as the O(n) expectation predicts.
- The fixed pivot is **not exactly quadratic** here. Our three-way partition moves larger elements to the end with swaps, so after the first round the subarray is no longer sorted, and the first element is no longer always the extreme.
- The textbook Θ(n²) case belongs to the classic **two-way partition with a first-element pivot on sorted input**. Phase 6 will add that as an explicit baseline and measure it.

## 6. Baselines (measured in Phase 6)
| Method | Time | Notes |
|---|---|---|
| Full sort (`Arrays.sort`) then index | O(n log n) | One sort can answer many percentiles |
| Fixed-pivot quickselect | Θ(n²) worst case on sorted input | Shows why randomization is needed |
| Two-way partition quickselect | degrades on duplicates | Shows why the three-way partition is needed |
| **Randomized three-way quickselect** | **expected O(n)** | Best for one or a few percentiles |

Hypothesis (to be measured): for k percentiles, k quickselects cost about k·c·n and one sort costs about n log n, so there is a crossover k beyond which sorting once is cheaper.

## 7. Limitation that matters for the business result
Only **completed** payments enter the distribution. Obligations still open at the analysis date are excluded, and those are typically the latest payers. That **right-censoring** biases the percentiles downward. The UI and report therefore show `completed` and `open (excluded)` counts next to every percentile.

## 8. Test evidence
- **RandomizedQuickselectTest:** single element · sorted input (all ranks) · reverse-sorted input · duplicate-heavy delays · all identical (exactly n comparisons) · min and max ranks with `Long.MIN_VALUE`/`MAX_VALUE` · 5 invalid ranks · empty and null inputs · caller's array not modified · partition property and permutation check (200 trials) · same seed gives the same count · **Las Vegas test over 1,000 seeds × 6 shapes** · `selectMany` · mean comparisons ≈ 3.39n · fixed vs random pivot growth.
- **PercentileTest:** nearest-rank table of 15 hand-computed cases, including the floating-point trap and the largest n · invalid P · n = 0 · parsing · formatting · end-to-end percentiles on delay data vs `sorted[k]`.

## 9. Viva checklist
- Las Vegas vs Monte Carlo, and why the answer never depends on the seed.
- The "good pivot" argument for expected O(n), and where the 3.39n constant comes from.
- Why three-way partitioning, and what identical input does to a two-way partition.
- How `k = ⌈P·n/100⌉ − 1` is computed without floating point, and what happens for P = 0, P = 100 and n = 0.
- Why open obligations are excluded, and what bias that introduces.
