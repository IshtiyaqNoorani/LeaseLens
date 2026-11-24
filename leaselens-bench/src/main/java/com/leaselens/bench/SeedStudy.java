package com.leaselens.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.function.Consumer;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.selection.RandomizedQuickselect;

/**
 * CO5 study: how the number of comparisons made by Randomized Quickselect varies with the random seed,
 * and whether the answer ever varies (it must not: Las Vegas). Counts are machine-independent.
 *
 * <p>Reference value: for the median of n distinct values the expected comparisons of randomized
 * selection are about 2(1 + ln 2)n ≈ 3.39n.
 */
public final class SeedStudy {

    public static final double MEDIAN_EXPECTATION = 2 * (1 + Math.log(2));

    private final int seeds;
    private final long baseSeed;

    public SeedStudy(int seeds, long baseSeed) {
        this.seeds = seeds;
        this.baseSeed = baseSeed;
    }

    public record Row(int n, String shape, String rank, int seeds, boolean allCorrect, Stats comparisons,
                      double meanPerElement, double p95PerElement, double maxPerElement) {

        public static final List<String> HEADER = List.of("n", "shape", "rank", "seeds", "all_correct", "mean",
                "sd", "min", "p25", "median", "p75", "max", "mean_per_n", "p95_per_n", "max_per_n", "reference_per_n");

        public List<String> values() {
            return List.of(Integer.toString(n), shape, rank, Integer.toString(seeds), Boolean.toString(allCorrect),
                    fmt(comparisons.mean()), fmt(comparisons.sd()), Long.toString(comparisons.min()),
                    Long.toString(comparisons.p25()), Long.toString(comparisons.median()), Long.toString(comparisons.p75()),
                    Long.toString(comparisons.max()), fmt(meanPerElement), fmt(p95PerElement), fmt(maxPerElement),
                    shape.equals("distinct") && rank.equals("median") ? fmt(MEDIAN_EXPECTATION) : "");
        }
    }

    public record HistogramBin(int n, double fromPerN, double toPerN, int count) {
        public static final List<String> HEADER = List.of("n", "from_per_n", "to_per_n", "runs");

        public List<String> values() {
            return List.of(Integer.toString(n), fmt(fromPerN), fmt(toPerN), Integer.toString(count));
        }
    }

    public record Result(List<Row> rows, List<HistogramBin> histogram) {
    }

    public Result run(int[] sizes, Consumer<String> progress) {
        List<Row> rows = new ArrayList<>();
        List<HistogramBin> histogram = new ArrayList<>();
        for (int n : sizes) {
            for (String shape : new String[] {"distinct", "delays"}) {
                long[] data = Inputs.shape(shape, n, baseSeed);
                long[] sorted = data.clone();
                Arrays.sort(sorted);
                for (String rank : new String[] {"median", "P90"}) {
                    progress.accept("seed study n=" + n + " " + shape + " " + rank);
                    int k = rank.equals("median") ? (n - 1) / 2 : (int) Math.ceil(0.9 * n) - 1;
                    long[] counts = new long[seeds];
                    boolean allCorrect = true;
                    for (int s = 0; s < seeds; s++) {
                        OperationCounter c = new OperationCounter();
                        long v = RandomizedQuickselect.select(data, k, new SplittableRandom(baseSeed + s), c);
                        allCorrect &= v == sorted[k];
                        counts[s] = c.get();
                    }
                    Stats st = Stats.of(counts);
                    long[] sortedCounts = counts.clone();
                    Arrays.sort(sortedCounts);
                    rows.add(new Row(n, shape, rank, seeds, allCorrect, st, st.mean() / n,
                            (double) Stats.quantile(sortedCounts, 0.95) / n, (double) st.max() / n));
                    if (shape.equals("distinct") && rank.equals("median")) {
                        histogram.addAll(bins(n, counts));
                    }
                }
            }
        }
        return new Result(rows, histogram);
    }

    private static List<HistogramBin> bins(int n, long[] counts) {
        double width = 0.25;
        double max = Arrays.stream(counts).max().orElse(0) / (double) n;
        int binCount = (int) Math.ceil(max / width) + 1;
        int[] c = new int[binCount];
        for (long v : counts) {
            c[(int) ((v / (double) n) / width)]++;
        }
        List<HistogramBin> out = new ArrayList<>();
        for (int i = 0; i < binCount; i++) {
            if (c[i] > 0 || i * width >= 1.0) {
                out.add(new HistogramBin(n, i * width, (i + 1) * width, c[i]));
            }
        }
        return out;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
