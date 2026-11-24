package com.leaselens.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.function.Consumer;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.segmenttree.RangeResult;
import com.leaselens.algorithms.segmenttree.SegmentTree;
import com.leaselens.bench.baseline.LinearScan;
import com.leaselens.bench.baseline.PrefixSums;

/**
 * Segment Tree vs linear scan vs prefix sums on identical operation sequences.
 *
 * <p>Workload: a fresh structure is built, then {@value #OPERATIONS} operations run, a given share of
 * which are point updates (corrections) and the rest range queries. Build time is included because
 * every method must build before it can answer. Prefix sums answer only the range SUM: they cannot give
 * the minimum or maximum, which the explorer needs; this is stated in each row's note.
 */
public final class SegmentTreeSuite {

    static final int OPERATIONS = 10_000;
    static final double[] UPDATE_RATIOS = {0.0, 0.01, 0.10, 0.50};
    private static final int SHORT_RANGE = 32;

    private final Harness harness;
    private final long seed;

    public SegmentTreeSuite(Harness harness, long seed) {
        this.harness = harness;
        this.seed = seed;
    }

    /** Operation sequence: kind[i] 0 = query [a[i], b[i]], 1 = update index a[i] to value v[i]. */
    record Ops(byte[] kind, int[] a, int[] b, long[] v) {
    }

    static Ops ops(int n, double updateRatio, boolean shortRanges, long seed) {
        SplittableRandom rng = new SplittableRandom(seed ^ (n * 1_000_003L) ^ Double.doubleToLongBits(updateRatio)
                ^ (shortRanges ? 0x5DEECE66DL : 0));
        byte[] kind = new byte[OPERATIONS];
        int[] a = new int[OPERATIONS];
        int[] b = new int[OPERATIONS];
        long[] v = new long[OPERATIONS];
        for (int i = 0; i < OPERATIONS; i++) {
            if (rng.nextDouble() < updateRatio) {
                kind[i] = 1;
                a[i] = rng.nextInt(n);
                v[i] = rng.nextLong(-50_000_000L, 50_000_000L);
            } else if (shortRanges) {
                int len = 1 + rng.nextInt(Math.min(SHORT_RANGE, n));
                a[i] = rng.nextInt(n - len + 1);
                b[i] = a[i] + len - 1;
            } else {
                int x = rng.nextInt(n);
                int y = rng.nextInt(n);
                a[i] = Math.min(x, y);
                b[i] = Math.max(x, y);
            }
        }
        return new Ops(kind, a, b, v);
    }

    // Each runner returns {sumChecksum, extremesChecksum}. Prefix sums cannot produce extremes (0).
    static long[] runTree(long[] values, Ops ops, OperationCounter counter) {
        SegmentTree tree = new SegmentTree(values);
        long sums = 0;
        long ext = 0;
        for (int i = 0; i < OPERATIONS; i++) {
            if (ops.kind()[i] == 1) {
                tree.update(ops.a()[i], ops.v()[i], counter);
            } else {
                RangeResult r = tree.query(ops.a()[i], ops.b()[i], counter);
                sums = sums * 31 + r.sum();
                ext = ext * 31 + r.min() * 7 + r.max();
            }
        }
        return new long[] {sums, ext};
    }

    static long[] runLinear(long[] values, Ops ops, long[] scanned) {
        LinearScan scan = new LinearScan(values);
        long sums = 0;
        long ext = 0;
        for (int i = 0; i < OPERATIONS; i++) {
            if (ops.kind()[i] == 1) {
                scan.update(ops.a()[i], ops.v()[i]);
                if (scanned != null) scanned[0] += 1;
            } else {
                long[] r = scan.query(ops.a()[i], ops.b()[i]);
                sums = sums * 31 + r[0];
                ext = ext * 31 + r[1] * 7 + r[2];
                if (scanned != null) scanned[0] += ops.b()[i] - ops.a()[i] + 1;
            }
        }
        return new long[] {sums, ext};
    }

    static long[] runPrefix(long[] values, Ops ops, long[] touched) {
        PrefixSums prefix = new PrefixSums(values);
        long sums = 0;
        for (int i = 0; i < OPERATIONS; i++) {
            if (ops.kind()[i] == 1) {
                prefix.update(ops.a()[i], ops.v()[i]);
                if (touched != null) touched[0] += values.length - ops.a()[i];
            } else {
                sums = sums * 31 + prefix.sum(ops.a()[i], ops.b()[i]);
                if (touched != null) touched[0] += 2;
            }
        }
        return new long[] {sums, 0};
    }

    public List<ResultRow> run(int[] sizes, Consumer<String> progress) {
        List<ResultRow> rows = new ArrayList<>();
        for (int n : sizes) {
            long[] values = Inputs.cashFlows(n, seed);

            // Build cost alone.
            Harness.Timing tb = harness.time(() -> {
                SegmentTree t = new SegmentTree(values);
                return t.query(0, n - 1).sum();
            });
            Harness.Timing pb = harness.time(() -> new PrefixSums(values).sum(0, n - 1));
            boolean buildOk = tb.checksum() == pb.checksum();
            rows.add(new ResultRow("segment-tree", "build", "SegmentTree", "assessed", n, "build only", "-",
                    harness.repetitions(), tb.nanos(), -1, buildOk, seed, "build O(n)"));
            rows.add(new ResultRow("segment-tree", "build", "PrefixSums", "baseline", n, "build only", "-",
                    harness.repetitions(), pb.nanos(), -1, buildOk, seed, "build O(n)"));

            for (double ratio : UPDATE_RATIOS) {
                for (boolean shortRanges : new boolean[] {false, true}) {
                    if (shortRanges && ratio != 0.0 && ratio != 0.10) {
                        continue;
                    }
                    progress.accept("segment-tree n=" + n + " update_ratio=" + ratio + " " + (shortRanges ? "short" : "random"));
                    rows.addAll(workload(n, values, ratio, shortRanges));
                }
            }
        }
        return rows;
    }

    /** One workload configuration: three rows (segment tree, linear scan, prefix sums). */
    public List<ResultRow> workload(int n, long[] values, double ratio, boolean shortRanges) {
        Ops ops = ops(n, ratio, shortRanges, seed);
        String workload = String.format(Locale.ROOT, "%d ops, %s ranges", OPERATIONS,
                shortRanges ? "short (<=32 days)" : "random");
        String param = String.format(Locale.ROOT, "update_ratio=%.2f", ratio);

        // Verification pass (outside timing): identical answers from every method.
        OperationCounter nodes = new OperationCounter();
        long[] scanned = new long[1];
        long[] touched = new long[1];
        long[] t = runTree(values, ops, nodes);
        long[] l = runLinear(values, ops, scanned);
        long[] p = runPrefix(values, ops, touched);
        boolean ok = t[0] == l[0] && t[1] == l[1] && t[0] == p[0];

        Harness.Timing tt = harness.time(() -> { long[] r = runTree(values, ops, null); return r[0] ^ r[1]; });
        Harness.Timing lt = harness.time(() -> { long[] r = runLinear(values, ops, null); return r[0] ^ r[1]; });
        Harness.Timing pt = harness.time(() -> runPrefix(values, ops, null)[0]);
        return List.of(
                new ResultRow("segment-tree", "workload", "SegmentTree", "assessed", n, workload, param,
                        harness.repetitions(), tt.nanos(), nodes.get(), ok, seed, "sum+min+max; ops = nodes visited"),
                new ResultRow("segment-tree", "workload", "LinearScan", "baseline", n, workload, param,
                        harness.repetitions(), lt.nanos(), scanned[0], ok, seed, "sum+min+max; ops = elements scanned"),
                new ResultRow("segment-tree", "workload", "PrefixSums", "baseline", n, workload, param,
                        harness.repetitions(), pt.nanos(), touched[0], ok, seed,
                        "SUM ONLY (cannot answer min/max); ops = prefix entries read or rewritten"));
    }
}
