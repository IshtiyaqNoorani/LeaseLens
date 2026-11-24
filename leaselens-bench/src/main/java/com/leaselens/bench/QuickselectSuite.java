package com.leaselens.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.selection.Percentile;
import com.leaselens.algorithms.selection.RandomizedQuickselect;
import com.leaselens.bench.baseline.ClassicQuickselect;

/**
 * Randomized Quickselect (assessed) vs full sorting and vs classic two-way quickselect.
 * Every timed task copies its input first (select() copies internally; the sort baseline copies
 * then sorts), so all methods pay the same copying cost.
 */
public final class QuickselectSuite {

    /** Classic two-way quickselect is quadratic on its bad inputs; it is not run above this size. */
    static final int QUADRATIC_CAP = 10_000;
    static final int[] PERCENTILE_COUNTS = {1, 3, 5, 9, 19};

    private final Harness harness;
    private final long seed;

    public QuickselectSuite(Harness harness, long seed) {
        this.harness = harness;
        this.seed = seed;
    }

    /** Always chooses the first element of the current range: makes our algorithm deterministic. */
    static final class FirstElement implements RandomGenerator {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    static long sortSelect(long[] data, int k) {
        long[] copy = data.clone();
        Arrays.sort(copy);
        return copy[k];
    }

    public List<ResultRow> run(int[] sizes, Consumer<String> progress) {
        List<ResultRow> rows = new ArrayList<>();
        for (int n : sizes) {
            int k90 = Percentile.nearestRankIndex(n, 9000);

            // 1. One percentile (P90): assessed vs full sort, across input shapes.
            for (String shape : Inputs.SHAPES) {
                progress.accept("quickselect n=" + n + " single P90 " + shape);
                rows.addAll(singlePercentile(n, shape));
            }
            runRest(rows, n, k90, progress);
        }
        return rows;
    }

    /** P90 of one input shape: Randomized Quickselect vs a full sort (two rows). */
    public List<ResultRow> singlePercentile(int n, String shape) {
        List<ResultRow> rows = new ArrayList<>();
        int k90 = Percentile.nearestRankIndex(n, 9000);
        long[] data = Inputs.shape(shape, n, seed);
        long expected = sortSelect(data, k90);
        OperationCounter c = new OperationCounter();
        boolean ok = RandomizedQuickselect.select(data, k90, new SplittableRandom(seed), c) == expected;
        Harness.Timing q = harness.time(() -> RandomizedQuickselect.select(data, k90, new SplittableRandom(seed)));
        Harness.Timing s = harness.time(() -> sortSelect(data, k90));
        ok &= q.checksum() == expected && s.checksum() == expected;
        rows.add(new ResultRow("quickselect", "single-percentile", "RandomizedQuickselect", "assessed", n, shape,
                "P90", harness.repetitions(), q.nanos(), c.get(), ok, seed, "ops = comparisons"));
        rows.add(new ResultRow("quickselect", "single-percentile", "Arrays.sort", "baseline", n, shape, "P90",
                harness.repetitions(), s.nanos(), -1, ok, seed, "JDK dual-pivot quicksort; not instrumented"));
        return rows;
    }

    /** Several percentiles, pivot rules and partition schemes for one size. */
    private void runRest(List<ResultRow> rows, int n, int k90, Consumer<String> progress) {

        // 2. Several percentiles: k quickselects vs one sort (where is the crossover?).
        for (String shape : new String[] {"uniform", "delays"}) {
            long[] data = Inputs.shape(shape, n, seed);
            long[] sorted = data.clone();
            Arrays.sort(sorted);
            for (int count : PERCENTILE_COUNTS) {
                progress.accept("quickselect n=" + n + " " + count + " percentiles " + shape);
                int[] ranks = new int[count];
                for (int i = 0; i < count; i++) {
                    int pH = count == 1 ? 9000 : 500 + i * (9000 / (count - 1)); // P5..P95 evenly
                    ranks[i] = Percentile.nearestRankIndex(n, pH);
                }
                OperationCounter c = new OperationCounter();
                long[] got = RandomizedQuickselect.selectMany(data, ranks, new SplittableRandom(seed), c);
                boolean ok = true;
                for (int i = 0; i < count; i++) ok &= got[i] == sorted[ranks[i]];
                Harness.Timing q = harness.time(() -> {
                    long[] r = RandomizedQuickselect.selectMany(data, ranks, new SplittableRandom(seed), null);
                    long x = 0;
                    for (long v : r) x = x * 31 + v;
                    return x;
                });
                Harness.Timing s = harness.time(() -> {
                    long[] copy = data.clone();
                    Arrays.sort(copy);
                    long x = 0;
                    for (int r : ranks) x = x * 31 + copy[r];
                    return x;
                });
                ok &= q.checksum() == s.checksum();
                String param = "k=" + count;
                rows.add(new ResultRow("quickselect", "k-percentiles", "RandomizedQuickselect", "assessed", n, shape,
                        param, harness.repetitions(), q.nanos(), c.get(), ok, seed, "selectMany on one copy; ops = comparisons"));
                rows.add(new ResultRow("quickselect", "k-percentiles", "Arrays.sort", "baseline", n, shape, param,
                        harness.repetitions(), s.nanos(), -1, ok, seed, "one sort, then k lookups"));
            }
        }

        // 3. Pivot rule on already-sorted input (P90).
        long[] sortedInput = Inputs.shape("sorted", n, seed);
        long expectedSorted = sortedInput[k90];
        progress.accept("quickselect n=" + n + " pivot rules on sorted input");
        addVariant(rows, "pivot-rule", "RandomizedQuickselect", "assessed", n, "sorted", sortedInput, k90, expectedSorted,
                "3-way, random pivot");
        addVariant(rows, "pivot-rule", "Quickselect3Way-FirstPivot", "baseline", n, "sorted", sortedInput, k90,
                expectedSorted, "our 3-way partition with a fixed first-element pivot");
        if (n <= QUADRATIC_CAP) {
            addVariant(rows, "pivot-rule", "Classic2Way-FirstPivot", "baseline", n, "sorted", sortedInput, k90,
                    expectedSorted, "textbook Lomuto, first-element pivot (Θ(n²) here)");
        }

        // 4. Partition scheme on duplicate-heavy and all-identical input (P90).
        for (String shape : new String[] {"delays", "identical"}) {
            long[] data = Inputs.shape(shape, n, seed);
            long expected = sortSelect(data, k90);
            progress.accept("quickselect n=" + n + " partition scheme " + shape);
            addVariant(rows, "partition", "RandomizedQuickselect", "assessed", n, shape, data, k90, expected,
                    "3-way partition, random pivot");
            if (n <= QUADRATIC_CAP) {
                addVariant(rows, "partition", "Classic2Way-RandomPivot", "baseline", n, shape, data, k90, expected,
                        "2-way partition, random pivot (degrades on equal values)");
            }
        }
    }

    private void addVariant(List<ResultRow> rows, String experiment, String algorithm, String role, int n, String shape,
                            long[] data, int k, long expected, String note) {
        OperationCounter c = new OperationCounter();
        long got = switch (algorithm) {
            case "RandomizedQuickselect" -> RandomizedQuickselect.select(data, k, new SplittableRandom(seed), c);
            case "Quickselect3Way-FirstPivot" -> RandomizedQuickselect.select(data, k, new FirstElement(), c);
            case "Classic2Way-FirstPivot" -> ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.FIRST, null, c);
            case "Classic2Way-RandomPivot" ->
                    ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.RANDOM, new SplittableRandom(seed), c);
            default -> throw new IllegalArgumentException(algorithm);
        };
        Harness.Timing t = harness.time(() -> switch (algorithm) {
            case "RandomizedQuickselect" -> RandomizedQuickselect.select(data, k, new SplittableRandom(seed));
            case "Quickselect3Way-FirstPivot" -> RandomizedQuickselect.select(data, k, new FirstElement(), null);
            case "Classic2Way-FirstPivot" -> ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.FIRST, null, null);
            default -> ClassicQuickselect.select(data, k, ClassicQuickselect.Pivot.RANDOM, new SplittableRandom(seed), null);
        });
        boolean ok = got == expected && t.checksum() == expected;
        rows.add(new ResultRow("quickselect", experiment, algorithm, role, n, shape, "P90", harness.repetitions(),
                t.nanos(), c.get(), ok, seed, String.format(Locale.ROOT, "%s; ops = comparisons", note)));
    }
}
