package com.leaselens.bench;

import java.util.Arrays;

/** Summary statistics of repeated timings (nanoseconds) or counts. */
public record Stats(long median, long p25, long p75, long min, long max, double mean, double sd) {

    public static Stats of(long[] samples) {
        long[] s = samples.clone();
        Arrays.sort(s);
        double mean = Arrays.stream(s).average().orElse(0);
        double var = Arrays.stream(s).mapToDouble(v -> (v - mean) * (v - mean)).sum() / Math.max(1, s.length - 1);
        return new Stats(quantile(s, 0.5), quantile(s, 0.25), quantile(s, 0.75), s[0], s[s.length - 1], mean, Math.sqrt(var));
    }

    /** Nearest-rank quantile of a sorted array. */
    static long quantile(long[] sorted, double q) {
        int k = (int) Math.ceil(q * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, k))];
    }
}
