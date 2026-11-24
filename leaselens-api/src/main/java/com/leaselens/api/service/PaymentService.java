package com.leaselens.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.selection.Percentile;
import com.leaselens.algorithms.selection.RandomizedQuickselect;
import com.leaselens.api.service.ApiExceptions.NotFoundException;
import com.leaselens.api.store.DatasetStore;
import com.leaselens.api.store.Snapshot;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.PropertyType;
import com.leaselens.data.model.RentObligation;

/**
 * Payment Behaviour. Percentiles of days late are computed by the hand-written
 * {@link RandomizedQuickselect} with the nearest-rank rule from {@link Percentile}.
 *
 * <p>Only <b>completed</b> payments (settled in full on or before the analysis date) enter the
 * delay distribution. Obligations still open at the analysis date are reported separately and
 * counted as excluded; this right-censoring tends to understate delays. The analysis describes
 * payment records; it does not score tenants.
 */
@Service
public class PaymentService {

    private static final int MAX_PERCENTILES = 20;
    private static final int MAX_PAGE_SIZE = 500;
    private static final int[] HISTOGRAM_EDGES = {0, 1, 4, 8, 15, 31, 61, 91}; // bins start at these values
    private static final String[] HISTOGRAM_LABELS = {"0", "1-3", "4-7", "8-14", "15-30", "31-60", "61-90", "91+"};

    private final DatasetStore store;

    public PaymentService(DatasetStore store) {
        this.store = store;
    }

    /** Optional filters; due dates default to the horizon start and the analysis date. */
    public record Filter(String propertyId, String propertyType, LocalDate dueFrom, LocalDate dueTo) {
    }

    public record Population(int completed, int openExcluded, int onTimeOrEarly) {
    }

    public record PercentileValue(String p, int k, long daysLate) {
    }

    public record PercentilesView(Population population, Double meanDaysLate, List<PercentileValue> percentiles,
                                  String censoringNote, String message, AlgorithmRun algorithm) {
    }

    public record Bin(String label, long count) {
    }

    public record PropertyShare(String propertyId, String propertyName, int completed, int atOrAbove, Double sharePct) {
    }

    public record DelayRecord(String obligationId, String leaseId, String propertyId, LocalDate dueDate,
                              LocalDate settledDate, long daysLate, long duePaise) {
    }

    public record ThresholdView(String p, long thresholdDaysLate, int countAtOrAbove, Population population,
                                Double meanDaysLate, List<Bin> histogram, List<PropertyShare> byProperty,
                                List<DelayRecord> records, int page, int size, int totalRecords,
                                AlgorithmRun algorithm) {
    }

    public record OpenRecord(String obligationId, String leaseId, String propertyId, LocalDate dueDate,
                             long duePaise, long balancePaise, long daysOverdue) {
    }

    public record OutstandingView(LocalDate analysisDate, long totalOutstandingPaise, int openObligations,
                                  List<PortfolioService.AgeingBucket> ageing, List<OpenRecord> records) {
    }

    // ------------------------------------------------------------------ public API

    public PercentilesView percentiles(String pList, Filter filter, Long seed) {
        Snapshot s = store.require();
        List<Integer> ps = parsePercentiles(pList);
        Selection sel = select(s, filter);
        long usedSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Population population = sel.population();
        if (sel.completed().isEmpty()) {
            return new PercentilesView(population, null, List.of(), censoringNote(population),
                    "No completed payments match these filters, so no percentile can be computed.",
                    AlgorithmRun.quickselect("select x0", 0, 0, 0, usedSeed));
        }
        long[] delays = sel.delays();
        int[] ranks = ps.stream().mapToInt(p -> Percentile.nearestRankIndex(delays.length, p)).toArray();
        OperationCounter counter = new OperationCounter();
        long start = System.nanoTime();
        long[] values = RandomizedQuickselect.selectMany(delays, ranks, new SplittableRandom(usedSeed), counter);
        long elapsed = System.nanoTime() - start;
        List<PercentileValue> result = new ArrayList<>();
        for (int i = 0; i < ranks.length; i++) {
            result.add(new PercentileValue(Percentile.format(ps.get(i)), ranks[i], values[i]));
        }
        return new PercentilesView(population, mean(delays), result, censoringNote(population), null,
                AlgorithmRun.quickselect("select x" + ranks.length, delays.length, counter.get(), elapsed, usedSeed));
    }

    public ThresholdView threshold(String p, Filter filter, Long seed, int page, int size) {
        Snapshot s = store.require();
        int pH = Percentile.parseHundredths(p == null ? "90" : p);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        Selection sel = select(s, filter);
        if (sel.completed().isEmpty()) {
            throw new IllegalArgumentException("No completed payments match these filters, so no threshold can be computed.");
        }
        long usedSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        long[] delays = sel.delays();
        int k = Percentile.nearestRankIndex(delays.length, pH);
        OperationCounter counter = new OperationCounter();
        long start = System.nanoTime();
        long threshold = RandomizedQuickselect.select(delays, k, new SplittableRandom(usedSeed), counter);
        long elapsed = System.nanoTime() - start;

        long[] bins = new long[HISTOGRAM_EDGES.length];
        Map<String, int[]> perProperty = new LinkedHashMap<>(); // completed, atOrAbove
        List<DelayRecord> tail = new ArrayList<>();
        for (DelayRecord r : sel.completed()) {
            bins[bin(r.daysLate())]++;
            int[] counts = perProperty.computeIfAbsent(r.propertyId(), x -> new int[2]);
            counts[0]++;
            if (r.daysLate() >= threshold) {
                counts[1]++;
                tail.add(r);
            }
        }
        List<Bin> histogram = new ArrayList<>();
        for (int i = 0; i < bins.length; i++) {
            histogram.add(new Bin(HISTOGRAM_LABELS[i], bins[i]));
        }
        List<PropertyShare> byProperty = new ArrayList<>();
        perProperty.forEach((pid, c) -> byProperty.add(new PropertyShare(pid, s.dataset().property(pid).name(), c[0],
                c[1], Ratios.pct(c[1], c[0]))));
        byProperty.sort(Comparator.comparingInt(PropertyShare::atOrAbove).reversed()
                .thenComparing(PropertyShare::propertyId));
        tail.sort(Comparator.comparingLong(DelayRecord::daysLate).reversed()
                .thenComparing(DelayRecord::dueDate).thenComparing(DelayRecord::obligationId));
        int fromIndex = Math.min(page * size, tail.size());
        int toIndex = Math.min(fromIndex + size, tail.size());
        return new ThresholdView(Percentile.format(pH), threshold, tail.size(), sel.population(), mean(delays),
                histogram, byProperty, List.copyOf(tail.subList(fromIndex, toIndex)), page, size, tail.size(),
                AlgorithmRun.quickselect("select x1", delays.length, counter.get(), elapsed, usedSeed));
    }

    public OutstandingView outstanding(Filter filter) {
        Snapshot s = store.require();
        Selection sel = select(s, filter);
        List<OpenRecord> open = new ArrayList<>(sel.open());
        open.sort(Comparator.comparingLong(OpenRecord::daysOverdue).reversed().thenComparing(OpenRecord::obligationId));
        long[][] buckets = new long[4][2];
        long total = 0;
        for (OpenRecord r : open) {
            int b = r.daysOverdue() <= 30 ? 0 : r.daysOverdue() <= 60 ? 1 : r.daysOverdue() <= 90 ? 2 : 3;
            buckets[b][0]++;
            buckets[b][1] += r.balancePaise();
            total += r.balancePaise();
        }
        String[] labels = {"0-30", "31-60", "61-90", "90+"};
        List<PortfolioService.AgeingBucket> ageing = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ageing.add(new PortfolioService.AgeingBucket(labels[i], buckets[i][0], buckets[i][1]));
        }
        return new OutstandingView(s.analysisDate(), total, open.size(), ageing, open);
    }

    // ------------------------------------------------------------------ selection of records

    private record Selection(List<DelayRecord> completed, List<OpenRecord> open, int onTimeOrEarly) {
        Population population() {
            return new Population(completed.size(), open.size(), onTimeOrEarly);
        }

        long[] delays() {
            return completed.stream().mapToLong(DelayRecord::daysLate).toArray();
        }
    }

    private Selection select(Snapshot s, Filter filter) {
        PortfolioDataset d = s.dataset();
        Filter f = filter == null ? new Filter(null, null, null, null) : filter;
        if (f.propertyId() != null && !f.propertyId().isBlank() && d.property(f.propertyId()) == null) {
            throw new NotFoundException("Property '" + f.propertyId() + "' does not exist.");
        }
        PropertyType type = null;
        if (f.propertyType() != null && !f.propertyType().isBlank()) {
            try {
                type = PropertyType.valueOf(f.propertyType().strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("propertyType must be one of RESIDENTIAL, OFFICE, RETAIL, INDUSTRIAL.");
            }
        }
        DateRange due = DateRange.resolve(f.dueFrom(), f.dueTo(), s);
        LocalDate asOf = s.analysisDate();
        List<DelayRecord> completed = new ArrayList<>();
        List<OpenRecord> open = new ArrayList<>();
        int onTime = 0;
        for (RentObligation o : d.obligations()) {
            String propertyId = d.lease(o.leaseId()).propertyId();
            if (!due.contains(o.dueDate())
                    || (f.propertyId() != null && !f.propertyId().isBlank() && !f.propertyId().equals(propertyId))
                    || (type != null && d.property(propertyId).type() != type)) {
                continue;
            }
            LocalDate settled = s.ledger().settledAsOf(o, asOf);
            if (settled != null) {
                long late = Math.max(0, ChronoUnit.DAYS.between(o.dueDate(), settled));
                if (late == 0) {
                    onTime++;
                }
                completed.add(new DelayRecord(o.id(), o.leaseId(), propertyId, o.dueDate(), settled, late,
                        o.dueAmountPaise()));
            } else {
                open.add(new OpenRecord(o.id(), o.leaseId(), propertyId, o.dueDate(), o.dueAmountPaise(),
                        s.ledger().balanceAsOf(o, asOf), ChronoUnit.DAYS.between(o.dueDate(), asOf)));
            }
        }
        return new Selection(completed, open, onTime);
    }

    private static List<Integer> parsePercentiles(String pList) {
        String text = pList == null || pList.isBlank() ? "50,75,90" : pList;
        List<Integer> ps = new ArrayList<>();
        for (String part : text.split(",")) {
            ps.add(Percentile.parseHundredths(part));
        }
        if (ps.size() > MAX_PERCENTILES) {
            throw new IllegalArgumentException("At most " + MAX_PERCENTILES + " percentiles per request.");
        }
        return ps;
    }

    private static int bin(long daysLate) {
        for (int i = HISTOGRAM_EDGES.length - 1; i >= 0; i--) {
            if (daysLate >= HISTOGRAM_EDGES[i]) {
                return i;
            }
        }
        return 0;
    }

    private static Double mean(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return Ratios.ratio(sum, values.length);
    }

    private static String censoringNote(Population p) {
        return p.openExcluded() == 0 ? null
                : p.openExcluded() + " obligation(s) were still open at the analysis date and are not in this "
                        + "distribution; because these are usually the latest payers, the percentiles may understate delays.";
    }
}
