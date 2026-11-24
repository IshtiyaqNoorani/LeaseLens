package com.leaselens.algorithms.segmenttree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.leaselens.algorithms.OperationCounter;

/**
 * Correctness tests for {@link SegmentTree}. Every expected value is either hand-computed or
 * produced by a brute-force linear scan ({@link #bruteForce}).
 */
class SegmentTreeTest {

    /** Daily net cash flow example (paise): mixes receipts (positive) and expense-heavy days (negative). */
    private static final long[] SAMPLE = {500, -200, 300, -200, 900, 0, 900, -50};

    @Test
    @DisplayName("Single element: every query returns that element")
    void singleElement() {
        SegmentTree tree = new SegmentTree(new long[] {42});
        assertEquals(new RangeResult(0, 0, 42, 42, 0, 42, 0), tree.query(0, 0));
        assertEquals(1, tree.size());
    }

    @Test
    @DisplayName("Complete range [0, n-1] matches hand-computed totals")
    void completeRange() {
        SegmentTree tree = new SegmentTree(SAMPLE);
        // sum = 500-200+300-200+900+0+900-50 = 2150; min -200 first at 1; max 900 first at 4
        assertEquals(new RangeResult(0, 7, 2150, -200, 1, 900, 4), tree.query(0, 7));
    }

    @Test
    @DisplayName("Partial range in the middle")
    void partialRange() {
        SegmentTree tree = new SegmentTree(SAMPLE);
        // [2..5] = 300, -200, 900, 0
        assertEquals(new RangeResult(2, 5, 1000, -200, 3, 900, 4), tree.query(2, 5));
    }

    @Test
    @DisplayName("First and last boundaries, including single-day ranges")
    void boundaries() {
        SegmentTree tree = new SegmentTree(SAMPLE);
        assertEquals(new RangeResult(0, 0, 500, 500, 0, 500, 0), tree.query(0, 0));
        assertEquals(new RangeResult(7, 7, -50, -50, 7, -50, 7), tree.query(7, 7));
        assertEquals(new RangeResult(0, 1, 300, -200, 1, 500, 0), tree.query(0, 1));
        assertEquals(new RangeResult(6, 7, 850, -50, 7, 900, 6), tree.query(6, 7));
    }

    @Test
    @DisplayName("Ties for min and max resolve to the earliest day")
    void tiesResolveToEarliestIndex() {
        SegmentTree tree = new SegmentTree(new long[] {7, 7, 7, 7, 7});
        RangeResult r = tree.query(1, 4);
        assertEquals(1, r.minIndex());
        assertEquals(1, r.maxIndex());
        assertEquals(35 - 7, r.sum());
    }

    @Test
    @DisplayName("All-negative values (a loss-making period)")
    void allNegative() {
        SegmentTree tree = new SegmentTree(new long[] {-5, -1, -9, -3});
        assertEquals(new RangeResult(0, 3, -18, -9, 2, -1, 1), tree.query(0, 3));
    }

    @Test
    @DisplayName("Point update is reflected in every later query")
    void pointUpdate() {
        SegmentTree tree = new SegmentTree(SAMPLE);
        tree.update(3, 1_000); // correct day 3 from -200 to +1000
        assertEquals(1_000, tree.valueAt(3));
        assertEquals(new RangeResult(0, 7, 3350, -200, 1, 1000, 3), tree.query(0, 7));
        assertEquals(new RangeResult(2, 4, 2200, 300, 2, 1000, 3), tree.query(2, 4));
    }

    @Test
    @DisplayName("Repeated updates to the same index keep the tree consistent")
    void repeatedUpdatesSameIndex() {
        SegmentTree tree = new SegmentTree(SAMPLE);
        tree.update(0, -999);
        tree.update(0, 0);
        tree.update(0, 500); // back to the original value
        assertEquals(new RangeResult(0, 7, 2150, -200, 1, 900, 4), tree.query(0, 7));
    }

    @Test
    @DisplayName("The tree copies its input: changing the caller's array has no effect")
    void inputIsCopied() {
        long[] values = {1, 2, 3};
        SegmentTree tree = new SegmentTree(values);
        values[0] = 1_000;
        assertEquals(6, tree.query(0, 2).sum());
    }

    @ParameterizedTest(name = "query({0}, {1}) is rejected")
    @CsvSource({"3, 2", "-1, 2", "0, 8", "8, 8", "-5, -1"})
    @DisplayName("Invalid ranges are rejected with a clear message")
    void invalidRanges(int left, int right) {
        SegmentTree tree = new SegmentTree(SAMPLE);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tree.query(left, right));
        assertTrue(e.getMessage().contains("Range"), e.getMessage());
    }

    @ParameterizedTest(name = "update({0}) is rejected")
    @ValueSource(ints = {-1, 8, Integer.MAX_VALUE})
    @DisplayName("Invalid update indices are rejected")
    void invalidUpdateIndex(int index) {
        SegmentTree tree = new SegmentTree(SAMPLE);
        assertThrows(IllegalArgumentException.class, () -> tree.update(index, 1));
    }

    @Test
    @DisplayName("Empty and null inputs are rejected")
    void emptyOrNullInput() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentTree(new long[0]));
        assertThrows(NullPointerException.class, () -> new SegmentTree(null));
    }

    @Test
    @DisplayName("Overflow is detected instead of wrapping round, and a failed update is rolled back")
    void overflowDetected() {
        assertThrows(ArithmeticException.class, () -> new SegmentTree(new long[] {Long.MAX_VALUE, 1}));

        SegmentTree tree = new SegmentTree(new long[] {Long.MAX_VALUE, 0});
        assertThrows(ArithmeticException.class, () -> tree.update(1, 1));
        assertEquals(0, tree.valueAt(1));
        assertEquals(Long.MAX_VALUE, tree.query(0, 1).sum());
    }

    @Test
    @DisplayName("Differential test: 10,000 mixed operations per seed agree with a brute-force scan")
    void randomizedDifferentialAgainstBruteForce() {
        for (long seed = 1; seed <= 5; seed++) {
            SplittableRandom rng = new SplittableRandom(seed);
            int n = 1 + rng.nextInt(2_000);
            long[] reference = new long[n];
            for (int i = 0; i < n; i++) {
                reference[i] = rng.nextLong(-1_000_000, 1_000_000);
            }
            SegmentTree tree = new SegmentTree(reference);

            for (int op = 0; op < 10_000; op++) {
                if (rng.nextInt(10) < 3) { // 30% updates
                    int index = rng.nextInt(n);
                    long value = rng.nextLong(-1_000_000, 1_000_000);
                    reference[index] = value;
                    tree.update(index, value);
                } else {
                    int a = rng.nextInt(n);
                    int b = rng.nextInt(n);
                    int left = Math.min(a, b);
                    int right = Math.max(a, b);
                    assertEquals(bruteForce(reference, left, right), tree.query(left, right),
                            "seed " + seed + ", operation " + op);
                }
            }
        }
    }

    @Test
    @DisplayName("Complexity check: a query visits at most 4 nodes per tree level")
    void queryVisitsLogarithmicNodes() {
        int n = 100_000;
        SegmentTree tree = new SegmentTree(new long[n]);
        int levels = 32 - Integer.numberOfLeadingZeros(n - 1) + 1; // ceil(log2 n) + 1
        SplittableRandom rng = new SplittableRandom(7);
        for (int q = 0; q < 2_000; q++) {
            int a = rng.nextInt(n);
            int b = rng.nextInt(n);
            OperationCounter counter = new OperationCounter();
            tree.query(Math.min(a, b), Math.max(a, b), counter);
            assertTrue(counter.get() <= 4L * levels,
                    "visited " + counter.get() + " nodes, bound is " + (4 * levels));
        }
    }

    @Test
    @DisplayName("Complexity check: an update visits exactly one root-to-leaf path")
    void updateVisitsOnePath() {
        int n = 100_000;
        SegmentTree tree = new SegmentTree(new long[n]);
        int levels = 32 - Integer.numberOfLeadingZeros(n - 1) + 1;
        for (int index : new int[] {0, 1, n / 2, n - 1}) {
            OperationCounter counter = new OperationCounter();
            tree.update(index, 5, counter);
            assertTrue(counter.get() <= levels, "path length " + counter.get());
        }
    }

    /** Reference implementation: O(n) linear scan with the same tie rule (earliest index). */
    static RangeResult bruteForce(long[] values, int left, int right) {
        long sum = 0;
        long min = values[left];
        long max = values[left];
        int minIndex = left;
        int maxIndex = left;
        for (int i = left; i <= right; i++) {
            sum += values[i];
            if (values[i] < min) {
                min = values[i];
                minIndex = i;
            }
            if (values[i] > max) {
                max = values[i];
                maxIndex = i;
            }
        }
        return new RangeResult(left, right, sum, min, minIndex, max, maxIndex);
    }
}
