package com.leaselens.bench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.bench.baseline.ClassicQuickselect;
import com.leaselens.bench.baseline.LinearScan;
import com.leaselens.bench.baseline.PrefixSums;

/** The baselines must be correct too, otherwise the comparisons mean nothing. */
class BenchmarkComponentsTest {

    @Test
    @DisplayName("Linear scan and prefix sums agree with each other under updates")
    void rangeBaselines() {
        long[] v = Inputs.cashFlows(500, 1);
        LinearScan scan = new LinearScan(v);
        PrefixSums prefix = new PrefixSums(v);
        SplittableRandom rng = new SplittableRandom(3);
        for (int i = 0; i < 5_000; i++) {
            if (rng.nextInt(4) == 0) {
                int idx = rng.nextInt(v.length);
                long val = rng.nextLong(-1000, 1000);
                scan.update(idx, val);
                prefix.update(idx, val);
            } else {
                int a = rng.nextInt(v.length);
                int b = rng.nextInt(v.length);
                int l = Math.min(a, b);
                int r = Math.max(a, b);
                assertEquals(scan.query(l, r)[0], prefix.sum(l, r));
            }
        }
    }

    @Test
    @DisplayName("Segment tree, linear scan and prefix sums give identical checksums on a workload")
    void workloadAgreement() {
        long[] v = Inputs.cashFlows(2_000, 7);
        SegmentTreeSuite.Ops ops = SegmentTreeSuite.ops(2_000, 0.10, false, 7);
        long[] t = SegmentTreeSuite.runTree(v, ops, null);
        long[] l = SegmentTreeSuite.runLinear(v, ops, null);
        long[] p = SegmentTreeSuite.runPrefix(v, ops, null);
        assertArrayEquals(t, l);
        assertEquals(t[0], p[0]);
    }

    @Test
    @DisplayName("Classic quickselect is correct, and quadratic on its known bad inputs")
    void classicQuickselect() {
        long[] data = Inputs.shape("uniform", 3_000, 5);
        long[] sorted = data.clone();
        Arrays.sort(sorted);
        for (int k : new int[] {0, 1_500, 2_999}) {
            assertEquals(sorted[k], ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.FIRST, null, null));
            assertEquals(sorted[k], ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.RANDOM, new SplittableRandom(1), null));
        }
        int n = 2_000;
        OperationCounter c = new OperationCounter();
        ClassicQuickselect.select(Inputs.shape("sorted", n, 1), n - 1, ClassicQuickselect.Pivot.FIRST, null, c);
        assertEquals((long) n * (n - 1) / 2, c.get()); // (n-1) + (n-2) + ... + 1
        OperationCounter same = new OperationCounter();
        ClassicQuickselect.select(Inputs.shape("identical", n, 1), n / 2, ClassicQuickselect.Pivot.RANDOM, new SplittableRandom(2), same);
        assertTrue(same.get() > (long) n * n / 10, "two-way partition degrades on identical values");
    }

    @Test
    @DisplayName("Inputs are reproducible and have the intended shape")
    void inputs() {
        assertArrayEquals(Inputs.shape("delays", 1_000, 9), Inputs.shape("delays", 1_000, 9));
        long zeros = Arrays.stream(Inputs.shape("delays", 10_000, 9)).filter(x -> x == 0).count();
        assertTrue(zeros > 6_000 && zeros < 7_500, "about two-thirds zeros: " + zeros);
        long[] d = Inputs.shape("distinct", 1_000, 9);
        long[] s = d.clone();
        Arrays.sort(s);
        for (int i = 0; i < s.length; i++) assertEquals(i, s[i]);
    }

    @Test
    @DisplayName("Stats: nearest-rank median and quartiles")
    void stats() {
        Stats s = Stats.of(new long[] {5, 1, 4, 2, 3});
        assertEquals(3, s.median());
        assertEquals(2, s.p25());
        assertEquals(4, s.p75());
        assertEquals(1, s.min());
        assertEquals(3.0, s.mean());
    }

    @Test
    @DisplayName("Small end-to-end run: every suite verifies its answers")
    void smallRun() {
        Harness h = new Harness(1, 2);
        List<ResultRow> st = new SegmentTreeSuite(h, 1).run(new int[] {200}, m -> { });
        List<ResultRow> qs = new QuickselectSuite(h, 1).run(new int[] {200}, m -> { });
        assertTrue(st.stream().allMatch(ResultRow::verified));
        assertTrue(qs.stream().allMatch(ResultRow::verified));
        SeedStudy.Result seeds = new SeedStudy(50, 1).run(new int[] {500}, m -> { });
        assertTrue(seeds.rows().stream().allMatch(SeedStudy.Row::allCorrect));
    }
}
