package com.leaselens.algorithms.selection;

import java.util.Objects;
import java.util.random.RandomGenerator;

import com.leaselens.algorithms.OperationCounter;

/**
 * Randomized selection (Quickselect with a uniformly random pivot and three-way partitioning).
 *
 * <p>{@code select(data, k, rng)} returns the k-th smallest value (k is zero-based), i.e. the value
 * that would be at {@code sorted[k]}, without fully sorting. In LeaseLens it computes
 * percentiles of payment delays in days.
 *
 * <h2>Why randomized (syllabus Unit 6, CO5)</h2>
 * A fixed pivot rule (e.g. "first element") has inputs that force Θ(n²) work, such as already
 * sorted data. Choosing the pivot uniformly at random means no particular input is bad:
 * for every input the <em>expected</em> number of comparisons is O(n) (at most about
 * 2(1 + ln 2)n ≈ 3.39n). The worst case is still Θ(n²), but it needs a long run of unlucky
 * pivots and is very unlikely. This is a <b>Las Vegas</b> algorithm: the answer is always
 * correct; only the running time depends on the random choices.
 *
 * <h2>Why three-way partitioning</h2>
 * Payment delays contain many equal values (most payments are 0 days late). A two-way
 * partition does not shrink the problem when the pivot equals many elements, and on an array
 * of identical values degrades to Θ(n²) even with random pivots. The three-way
 * (Dutch national flag) partition puts every element equal to the pivot in one middle block and
 * returns immediately if k lands in it, so identical values cost a single linear pass.
 *
 * <h2>Reproducibility</h2>
 * The random source is always passed in. The same seed gives the same pivot sequence and the
 * same comparison count.
 */
public final class RandomizedQuickselect {

    private RandomizedQuickselect() {
    }

    /**
     * Returns the k-th smallest value of {@code data} (zero-based). The input array is not
     * modified: the algorithm works on a copy (O(n) extra space).
     *
     * @throws NullPointerException     if {@code data} or {@code rng} is null
     * @throws IllegalArgumentException if {@code data} is empty or {@code k} is outside {@code [0, n-1]}
     */
    public static long select(long[] data, int k, RandomGenerator rng) {
        return select(data, k, rng, null);
    }

    /**
     * As {@link #select(long[], int, RandomGenerator)}, also counting element-to-pivot comparisons.
     *
     * @param counter optional; if not null, incremented once per element compared with a pivot
     */
    public static long select(long[] data, int k, RandomGenerator rng, OperationCounter counter) {
        Objects.requireNonNull(data, "data must not be null");
        return selectInPlace(data.clone(), k, rng, counter);
    }

    /**
     * Selects several ranks from one copy of the data. Each selection rearranges the working
     * copy but keeps it a permutation of the input, so later selections remain correct.
     * Cost: k independent selections, each expected O(n).
     *
     * @param ranks zero-based ranks; each must be within {@code [0, n-1]}
     * @return the selected values, in the same order as {@code ranks}
     */
    public static long[] selectMany(long[] data, int[] ranks, RandomGenerator rng, OperationCounter counter) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(ranks, "ranks must not be null");
        long[] work = data.clone();
        long[] results = new long[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            results[i] = selectInPlace(work, ranks[i], rng, counter);
        }
        return results;
    }

    /**
     * In-place selection: rearranges {@code a} and returns the k-th smallest value.
     *
     * <p>On return, {@code a[k]} holds the result, every element before index k is ≤ the result
     * and every element after index k is ≥ the result.
     *
     * <pre>
     * lo ← 0, hi ← n − 1
     * loop
     *     if lo = hi: return a[lo]
     *     pivot ← a[uniform random index in [lo, hi]]
     *     three-way partition a[lo..hi] into  &lt; pivot | = pivot | &gt; pivot
     *         occupying [lo, lt−1], [lt, gt], [gt+1, hi]
     *     if k &lt; lt: hi ← lt − 1          (answer is among the smaller values)
     *     else if k &gt; gt: lo ← gt + 1     (answer is among the larger values)
     *     else return pivot               (k falls in the block equal to the pivot)
     * </pre>
     *
     * @throws NullPointerException     if {@code a} or {@code rng} is null
     * @throws IllegalArgumentException if {@code a} is empty or {@code k} is outside {@code [0, n-1]}
     */
    public static long selectInPlace(long[] a, int k, RandomGenerator rng, OperationCounter counter) {
        Objects.requireNonNull(a, "data must not be null");
        Objects.requireNonNull(rng, "random generator must not be null");
        if (a.length == 0) {
            throw new IllegalArgumentException("Cannot select from an empty dataset.");
        }
        if (k < 0 || k >= a.length) {
            throw new IllegalArgumentException(
                    "Rank k = " + k + " is outside the valid range [0, " + (a.length - 1) + "].");
        }

        int lo = 0;
        int hi = a.length - 1;
        while (true) {
            if (lo == hi) {
                return a[lo];
            }
            long pivot = a[lo + rng.nextInt(hi - lo + 1)];

            // Dutch national flag partition of a[lo..hi].
            // Invariant: a[lo..lt-1] < pivot, a[lt..i-1] == pivot, a[gt+1..hi] > pivot, a[i..gt] unexamined.
            int lt = lo;
            int i = lo;
            int gt = hi;
            while (i <= gt) {
                if (counter != null) {
                    counter.increment();
                }
                long x = a[i];
                if (x < pivot) {
                    swap(a, lt++, i++);
                } else if (x > pivot) {
                    swap(a, i, gt--);
                } else {
                    i++;
                }
            }

            if (k < lt) {
                hi = lt - 1;
            } else if (k > gt) {
                lo = gt + 1;
            } else {
                return pivot;
            }
        }
    }

    private static void swap(long[] a, int i, int j) {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
