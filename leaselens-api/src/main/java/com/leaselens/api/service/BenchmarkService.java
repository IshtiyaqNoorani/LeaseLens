package com.leaselens.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

import com.leaselens.bench.Harness;
import com.leaselens.bench.Inputs;
import com.leaselens.bench.QuickselectSuite;
import com.leaselens.bench.ResultRow;
import com.leaselens.bench.SegmentTreeSuite;
import com.leaselens.data.csv.CsvFormatException;
import com.leaselens.data.csv.CsvParser;
import com.leaselens.data.csv.CsvTable;

/**
 * Algorithm Lab. Serves the committed benchmark CSVs (bundled into the jar from benchmarks/results)
 * and runs small, bounded live benchmarks on request. Live runs share the CPU with the web server, so
 * they are labelled indicative; the committed results are the reference.
 */
@Service
public class BenchmarkService {

    static final int MAX_LIVE_SIZE = 100_000;
    static final int MIN_LIVE_SIZE = 100;
    static final int MAX_LIVE_SIZES = 3;
    static final int MAX_LIVE_REPS = 10;
    private static final String[] FILES = {"segment_tree", "quickselect", "seed_study", "seed_histogram"};

    private final ReentrantLock liveLock = new ReentrantLock();

    public record Table(Map<String, String> metadata, List<String> columns, List<Map<String, Object>> rows) {
    }

    public record Results(boolean available, Map<String, Table> tables, String note) {
    }

    public record LiveRequest(String suite, List<Integer> sizes, Integer repetitions, Long seed) {
    }

    public record LiveResult(String suite, int repetitions, int warmups, long seed, Map<String, String> environment,
                             List<Map<String, Object>> rows, String note) {
    }

    /** Parsed committed results; each row gains {@code vs_assessed} = its median ÷ the assessed method's median. */
    public Results results() {
        Map<String, Table> tables = new LinkedHashMap<>();
        for (String name : FILES) {
            try (InputStream in = BenchmarkService.class.getResourceAsStream("/benchmarks/" + name + ".csv")) {
                if (in != null) {
                    tables.put(name, parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (tables.isEmpty()) {
            return new Results(false, tables, "No committed benchmark results were found. Run the benchmark CLI (see README).");
        }
        addRatios(tables.get("segment_tree"));
        addRatios(tables.get("quickselect"));
        return new Results(true, tables, "Measured offline with the benchmark CLI; see metadata for machine and settings.");
    }

    public LiveResult runLive(LiveRequest request) {
        if (request == null || request.suite() == null) {
            throw new IllegalArgumentException("suite is required: segment-tree or quickselect.");
        }
        List<Integer> sizes = request.sizes() == null || request.sizes().isEmpty() ? List.of(1_000, 10_000) : request.sizes();
        if (sizes.size() > MAX_LIVE_SIZES || sizes.stream().anyMatch(n -> n == null || n < MIN_LIVE_SIZE || n > MAX_LIVE_SIZE)) {
            throw new IllegalArgumentException("Choose up to " + MAX_LIVE_SIZES + " sizes between " + MIN_LIVE_SIZE
                    + " and " + MAX_LIVE_SIZE + " for a live run.");
        }
        int reps = request.repetitions() == null ? 5 : request.repetitions();
        if (reps < 3 || reps > MAX_LIVE_REPS) {
            throw new IllegalArgumentException("repetitions must be between 3 and " + MAX_LIVE_REPS + ".");
        }
        long seed = request.seed() == null ? 42 : request.seed();
        if (!liveLock.tryLock()) {
            throw new ApiExceptions.BusyException("A live benchmark is already running. Try again in a few seconds.");
        }
        try {
            Harness harness = new Harness(3, reps);
            List<ResultRow> rows = new ArrayList<>();
            String note;
            switch (request.suite()) {
                case "segment-tree" -> {
                    SegmentTreeSuite suite = new SegmentTreeSuite(harness, seed);
                    for (int n : sizes) {
                        rows.addAll(suite.workload(n, Inputs.cashFlows(n, seed), 0.10, false));
                    }
                    note = "10,000 operations (10% corrections, random ranges) per size.";
                }
                case "quickselect" -> {
                    QuickselectSuite suite = new QuickselectSuite(harness, seed);
                    for (int n : sizes) {
                        rows.addAll(suite.singlePercentile(n, "uniform"));
                        rows.addAll(suite.singlePercentile(n, "delays"));
                    }
                    note = "P90 of uniform and delay-shaped data per size.";
                }
                default -> throw new IllegalArgumentException("suite must be segment-tree or quickselect.");
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (ResultRow r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                List<String> values = r.values();
                for (int i = 0; i < ResultRow.HEADER.size(); i++) {
                    m.put(ResultRow.HEADER.get(i), typed(values.get(i)));
                }
                out.add(m);
            }
            addRatios(out);
            Map<String, String> env = new LinkedHashMap<>();
            env.put("java", System.getProperty("java.version"));
            env.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            env.put("cpu_cores", Integer.toString(Runtime.getRuntime().availableProcessors()));
            return new LiveResult(request.suite(), reps, 3, seed, env, out,
                    note + " Live runs share the CPU with the web server: indicative only; the committed results are the reference.");
        } finally {
            liveLock.unlock();
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Table parse(String text) {
        Map<String, String> metadata = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.startsWith("# ")) {
                int colon = line.indexOf(':');
                if (colon > 2) {
                    metadata.put(line.substring(2, colon).trim(), line.substring(colon + 1).trim());
                }
            } else {
                body.append(line).append('\n');
            }
        }
        try {
            CsvTable table = CsvParser.parse("benchmark", new InputStreamReader(
                    new java.io.ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (CsvTable.Row row : table.rows()) {
                Map<String, Object> m = new LinkedHashMap<>();
                for (int i = 0; i < table.header().size(); i++) {
                    m.put(table.header().get(i), typed(i < row.fields().size() ? row.fields().get(i) : ""));
                }
                rows.add(m);
            }
            return new Table(metadata, table.header(), rows);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (CsvFormatException e) {
            throw new IllegalStateException("A committed benchmark file is not valid CSV: " + e.getMessage(), e);
        }
    }

    private static void addRatios(Table table) {
        if (table != null) {
            addRatios(table.rows());
        }
    }

    /** For rows of the same experiment, size, workload and parameter: median ÷ assessed method's median. */
    private static void addRatios(List<Map<String, Object>> rows) {
        Map<String, Long> assessed = new HashMap<>();
        for (Map<String, Object> r : rows) {
            if ("assessed".equals(r.get("role"))) {
                assessed.put(key(r), ((Number) r.get("median_ns")).longValue());
            }
        }
        for (Map<String, Object> r : rows) {
            Long base = assessed.get(key(r));
            r.put("vs_assessed", base == null || base == 0 ? null
                    : Math.round(((Number) r.get("median_ns")).doubleValue() / base * 100.0) / 100.0);
        }
    }

    private static String key(Map<String, Object> r) {
        return r.get("experiment") + "|" + r.get("n") + "|" + r.get("workload") + "|" + r.get("parameter");
    }

    private static Object typed(String v) {
        if (v.equals("true") || v.equals("false")) {
            return Boolean.parseBoolean(v);
        }
        if (v.matches("-?\\d+")) {
            try {
                return Long.parseLong(v);
            } catch (NumberFormatException e) {
                return v;
            }
        }
        if (v.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(v);
        }
        return v;
    }
}
