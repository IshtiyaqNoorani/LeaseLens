package com.leaselens.bench;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the benchmark suites and writes CSVs to {@code benchmarks/results}.
 *
 * <pre>
 * java -Xms1g -Xmx1g -jar leaselens-bench/target/leaselens-bench.jar
 *      [--suite all|segment-tree|quickselect|seeds] [--sizes 1000,10000,100000]
 *      [--reps 10] [--warmup 5] [--seed 42] [--seeds 1000] [--out benchmarks/results]
 * </pre>
 */
public final class BenchmarkCli {

    private BenchmarkCli() {
    }

    public static void main(String[] args) throws Exception {
        String suite = option(args, "--suite", "all");
        int[] sizes = Arrays.stream(option(args, "--sizes", "1000,10000,100000").split(","))
                .mapToInt(s -> Integer.parseInt(s.trim())).toArray();
        int reps = Integer.parseInt(option(args, "--reps", "10"));
        int warmup = Integer.parseInt(option(args, "--warmup", "5"));
        long seed = Long.parseLong(option(args, "--seed", "42"));
        int seeds = Integer.parseInt(option(args, "--seeds", "1000"));
        Path out = Path.of(option(args, "--out", "benchmarks/results"));

        Harness harness = new Harness(warmup, reps);
        Map<String, String> meta = metadata(sizes, reps, warmup, seed);
        long start = System.nanoTime();

        if (suite.equals("all") || suite.equals("segment-tree")) {
            List<ResultRow> rows = new SegmentTreeSuite(harness, seed).run(sizes, BenchmarkCli::log);
            CsvOut.write(out.resolve("segment_tree.csv"), meta, ResultRow.HEADER, rows.stream().map(ResultRow::values).toList());
            report(rows);
        }
        if (suite.equals("all") || suite.equals("quickselect")) {
            List<ResultRow> rows = new QuickselectSuite(harness, seed).run(sizes, BenchmarkCli::log);
            CsvOut.write(out.resolve("quickselect.csv"), meta, ResultRow.HEADER, rows.stream().map(ResultRow::values).toList());
            report(rows);
        }
        if (suite.equals("all") || suite.equals("seeds")) {
            Map<String, String> m = new LinkedHashMap<>(meta);
            m.put("seeds_per_configuration", Integer.toString(seeds));
            SeedStudy.Result r = new SeedStudy(seeds, seed).run(sizes, BenchmarkCli::log);
            CsvOut.write(out.resolve("seed_study.csv"), m, SeedStudy.Row.HEADER, r.rows().stream().map(SeedStudy.Row::values).toList());
            CsvOut.write(out.resolve("seed_histogram.csv"), m, SeedStudy.HistogramBin.HEADER,
                    r.histogram().stream().map(SeedStudy.HistogramBin::values).toList());
            boolean allCorrect = r.rows().stream().allMatch(SeedStudy.Row::allCorrect);
            System.out.println("Seed study: every run correct = " + allCorrect);
            if (!allCorrect) {
                throw new IllegalStateException("A randomized selection returned a wrong answer");
            }
        }
        System.out.printf("Finished in %.1f s. Results in %s%n", (System.nanoTime() - start) / 1e9, out);
    }

    static Map<String, String> metadata(int[] sizes, int reps, int warmup, long seed) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("generated_at", Instant.now().toString());
        m.put("java", System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")");
        m.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        m.put("cpu_cores", Integer.toString(Runtime.getRuntime().availableProcessors()));
        m.put("max_heap_mb", Long.toString(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        m.put("jvm_args", String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        m.put("sizes", Arrays.toString(sizes));
        m.put("warmup_iterations", Integer.toString(warmup));
        m.put("timed_repetitions", Integer.toString(reps));
        m.put("seed", Long.toString(seed));
        m.put("timing", "System.nanoTime per repetition; median and IQR reported; results fed to a volatile sink");
        return m;
    }

    private static void report(List<ResultRow> rows) {
        long failures = rows.stream().filter(r -> !r.verified()).count();
        System.out.println(rows.size() + " measurements, " + failures + " verification failure(s)");
        if (failures > 0) {
            throw new IllegalStateException("Benchmark answers disagree between methods; results are not trustworthy");
        }
    }

    private static void log(String message) {
        System.out.println("  running " + message);
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }
}
