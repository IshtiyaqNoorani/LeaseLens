package com.leaselens.algorithms.segmenttree;

import java.util.Objects;

import com.leaselens.algorithms.OperationCounter;

/**
 * Segment tree over a fixed-length array of {@code long} values supporting range sum, range
 * minimum and range maximum queries (with the index where the extreme occurs) and point updates.
 *
 * <p>In LeaseLens each array element is one day's net rental cash flow in paise
 * (rent received minus operating expenses), so a range query answers "what was the net cash flow,
 * the worst day and the best day between two dates".
 *
 * <h2>Representation</h2>
 * A recursive, array-backed binary tree. Node 1 is the root and covers {@code [0, n-1]}; a node
 * covering {@code [lo, hi]} with {@code mid = (lo + hi) / 2} has its left child {@code 2*node}
 * covering {@code [lo, mid]} and its right child {@code 2*node + 1} covering {@code [mid+1, hi]}.
 * Arrays of size {@code 4n} are always large enough for this layout.
 *
 * <h2>Invariant</h2>
 * For every node covering {@code [lo, hi]}: {@code sum[node]}, {@code min[node]} and
 * {@code max[node]} equal the sum, minimum and maximum of {@code values[lo..hi]}, and
 * {@code minIdx[node]} / {@code maxIdx[node]} hold the earliest index where they occur.
 *
 * <h2>Complexity</h2>
 * Build O(n) time; query O(log n) time; update O(log n) time; O(n) space.
 *
 * <h2>Overflow</h2>
 * Sums use {@link Math#addExact(long, long)}, so an overflow throws {@link ArithmeticException}
 * instead of silently wrapping round to a wrong answer.
 *
 * <p>Not thread-safe: callers must synchronise if queries and updates can run concurrently.
 */
public final class SegmentTree {

    private static final int ROOT = 1;

    private final int n;
    private final long[] values;
    private final long[] sum;
    private final long[] min;
    private final long[] max;
    private final int[] minIdx;
    private final int[] maxIdx;

    /**
     * Builds the tree in O(n) time. The input array is copied, so later changes to the caller's
     * array do not affect the tree.
     *
     * @param initialValues the values to index; must contain at least one element
     * @throws NullPointerException     if {@code initialValues} is null
     * @throws IllegalArgumentException if {@code initialValues} is empty
     * @throws ArithmeticException      if a range sum overflows {@code long}
     */
    public SegmentTree(long[] initialValues) {
        Objects.requireNonNull(initialValues, "values must not be null");
        if (initialValues.length == 0) {
            throw new IllegalArgumentException("A segment tree needs at least one value; the input is empty.");
        }
        this.n = initialValues.length;
        this.values = initialValues.clone();
        int capacity = 4 * n;
        this.sum = new long[capacity];
        this.min = new long[capacity];
        this.max = new long[capacity];
        this.minIdx = new int[capacity];
        this.maxIdx = new int[capacity];
        build(ROOT, 0, n - 1);
    }

    /** Number of indexed values. */
    public int size() {
        return n;
    }

    /**
     * Returns the current value at {@code index}.
     *
     * @throws IllegalArgumentException if {@code index} is outside {@code [0, size()-1]}
     */
    public long valueAt(int index) {
        checkIndex(index);
        return values[index];
    }

    /** Range query over {@code [left, right]} (both inclusive). See {@link #query(int, int, OperationCounter)}. */
    public RangeResult query(int left, int right) {
        return query(left, right, null);
    }

    /**
     * Returns the sum, minimum and maximum of {@code values[left..right]} in O(log n) time.
     *
     * @param left    first index, inclusive
     * @param right   last index, inclusive
     * @param counter optional; if not null, incremented once per tree node visited
     * @throws IllegalArgumentException if the range is empty or outside the array
     * @throws ArithmeticException      if the range sum overflows {@code long}
     */
    public RangeResult query(int left, int right, OperationCounter counter) {
        checkRange(left, right);
        Accumulator acc = new Accumulator();
        query(ROOT, 0, n - 1, left, right, acc, counter);
        return new RangeResult(left, right, acc.sum, acc.min, acc.minIndex, acc.max, acc.maxIndex);
    }

    /** Point update: sets {@code values[index] = newValue}. See {@link #update(int, long, OperationCounter)}. */
    public void update(int index, long newValue) {
        update(index, newValue, null);
    }

    /**
     * Sets {@code values[index] = newValue} and repairs every node on the root-to-leaf path in
     * O(log n) time.
     *
     * @param counter optional; if not null, incremented once per tree node visited
     * @throws IllegalArgumentException if {@code index} is outside {@code [0, size()-1]}
     * @throws ArithmeticException      if a sum on the path overflows {@code long}; the tree is
     *                                  then left unchanged
     */
    public void update(int index, long newValue, OperationCounter counter) {
        checkIndex(index);
        long previous = values[index];
        values[index] = newValue;
        try {
            update(ROOT, 0, n - 1, index, counter);
        } catch (ArithmeticException overflow) {
            // Roll back so the invariant still holds, then report the failure.
            values[index] = previous;
            update(ROOT, 0, n - 1, index, null);
            throw overflow;
        }
    }

    // ---------------------------------------------------------------- recursive helpers

    private void build(int node, int lo, int hi) {
        if (lo == hi) {
            setLeaf(node, lo);
            return;
        }
        int mid = (lo + hi) >>> 1;
        build(2 * node, lo, mid);
        build(2 * node + 1, mid + 1, hi);
        pull(node);
    }

    private void update(int node, int lo, int hi, int index, OperationCounter counter) {
        if (counter != null) {
            counter.increment();
        }
        if (lo == hi) {
            setLeaf(node, lo);
            return;
        }
        int mid = (lo + hi) >>> 1;
        if (index <= mid) {
            update(2 * node, lo, mid, index, counter);
        } else {
            update(2 * node + 1, mid + 1, hi, index, counter);
        }
        pull(node);
    }

    /**
     * Visits only nodes that overlap {@code [left, right]}. A node lying completely inside the
     * range is merged as a whole; children are explored only for partial overlaps. Nodes are
     * merged left to right, so on ties the earliest index wins.
     */
    private void query(int node, int lo, int hi, int left, int right, Accumulator acc, OperationCounter counter) {
        if (counter != null) {
            counter.increment();
        }
        if (right < lo || hi < left) {
            return; // no overlap
        }
        if (left <= lo && hi <= right) {
            acc.merge(sum[node], min[node], minIdx[node], max[node], maxIdx[node]); // full overlap
            return;
        }
        int mid = (lo + hi) >>> 1;
        query(2 * node, lo, mid, left, right, acc, counter);
        query(2 * node + 1, mid + 1, hi, left, right, acc, counter);
    }

    private void setLeaf(int node, int index) {
        sum[node] = values[index];
        min[node] = values[index];
        max[node] = values[index];
        minIdx[node] = index;
        maxIdx[node] = index;
    }

    /** Recomputes a node from its two children (re-establishes the invariant). */
    private void pull(int node) {
        int l = 2 * node;
        int r = 2 * node + 1;
        sum[node] = Math.addExact(sum[l], sum[r]);
        // "<=" keeps the left (earlier) index on ties.
        if (min[l] <= min[r]) {
            min[node] = min[l];
            minIdx[node] = minIdx[l];
        } else {
            min[node] = min[r];
            minIdx[node] = minIdx[r];
        }
        if (max[l] >= max[r]) {
            max[node] = max[l];
            maxIdx[node] = maxIdx[l];
        } else {
            max[node] = max[r];
            maxIdx[node] = maxIdx[r];
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= n) {
            throw new IllegalArgumentException(
                    "Index " + index + " is outside the valid range [0, " + (n - 1) + "].");
        }
    }

    private void checkRange(int left, int right) {
        if (left > right) {
            throw new IllegalArgumentException(
                    "Range start " + left + " is after range end " + right + ".");
        }
        if (left < 0 || right >= n) {
            throw new IllegalArgumentException(
                    "Range [" + left + ", " + right + "] is outside the valid range [0, " + (n - 1) + "].");
        }
    }

    /** Running aggregate of the canonical nodes found during one query. */
    private static final class Accumulator {
        private boolean empty = true;
        private long sum;
        private long min;
        private int minIndex;
        private long max;
        private int maxIndex;

        void merge(long nodeSum, long nodeMin, int nodeMinIdx, long nodeMax, int nodeMaxIdx) {
            if (empty) {
                sum = nodeSum;
                min = nodeMin;
                minIndex = nodeMinIdx;
                max = nodeMax;
                maxIndex = nodeMaxIdx;
                empty = false;
                return;
            }
            sum = Math.addExact(sum, nodeSum);
            // Nodes arrive left to right, so strict comparison keeps the earliest index on ties.
            if (nodeMin < min) {
                min = nodeMin;
                minIndex = nodeMinIdx;
            }
            if (nodeMax > max) {
                max = nodeMax;
                maxIndex = nodeMaxIdx;
            }
        }
    }
}
