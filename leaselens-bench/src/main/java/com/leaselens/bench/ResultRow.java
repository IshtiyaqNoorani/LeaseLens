package com.leaselens.bench;

import java.util.List;
import java.util.Locale;

/**
 * One benchmark measurement. {@code role} is "assessed" for our algorithms and "baseline" for the
 * comparison methods. {@code operations} is a machine-independent count (tree nodes visited,
 * elements scanned or prefixes rewritten, or element comparisons) from one instrumented run.
 */
public record ResultRow(String suite, String experiment, String algorithm, String role, int n, String workload,
                        String parameter, int repetitions, Stats nanos, long operations, boolean verified,
                        long seed, String note) {

    public static final List<String> HEADER = List.of("suite", "experiment", "algorithm", "role", "n", "workload",
            "parameter", "repetitions", "median_ns", "p25_ns", "p75_ns", "min_ns", "max_ns", "mean_ns",
            "operations", "verified", "seed", "note");

    public List<String> values() {
        return List.of(suite, experiment, algorithm, role, Integer.toString(n), workload, parameter,
                Integer.toString(repetitions), Long.toString(nanos.median()), Long.toString(nanos.p25()),
                Long.toString(nanos.p75()), Long.toString(nanos.min()), Long.toString(nanos.max()),
                String.format(Locale.ROOT, "%.1f", nanos.mean()), Long.toString(operations), Boolean.toString(verified),
                Long.toString(seed), note == null ? "" : note);
    }
}
