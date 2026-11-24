package com.leaselens.api.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.segmenttree.RangeResult;
import com.leaselens.api.service.ApiExceptions.NotFoundException;
import com.leaselens.api.store.Adjustment;
import com.leaselens.api.store.CashFlowIndex;
import com.leaselens.api.store.DatasetStore;
import com.leaselens.api.store.Snapshot;

/**
 * Cash-Flow Explorer. Every range figure is answered by the hand-written Segment Tree in
 * {@link CashFlowIndex}; this service only maps dates to day indices and formats results.
 *
 * <p>Adjustments correct the explorer's daily cash flow only. They do not change the source CSV
 * data or the Overview KPIs, and responses say how many adjustments affected the result.
 */
@Service
public class CashFlowService {

    private static final int MAX_REASON_LENGTH = 200;

    private final DatasetStore store;

    public CashFlowService(DatasetStore store) {
        this.store = store;
    }

    public record RangeView(String scope, LocalDate from, LocalDate to, int days, long netCashFlowPaise,
                            long minDailyPaise, LocalDate minDate, long maxDailyPaise, LocalDate maxDate,
                            int adjustmentsApplied, AlgorithmRun algorithm) {
    }

    public record PeriodView(String label, LocalDate from, LocalDate to, long netCashFlowPaise, long minDailyPaise,
                             LocalDate minDate, long maxDailyPaise, LocalDate maxDate) {
    }

    public record PeriodsView(String scope, String granularity, List<PeriodView> periods, AlgorithmRun algorithm) {
    }

    public record SeriesPoint(LocalDate date, long netPaise, long receiptsPaise, long expensesPaise) {
    }

    public record AdjustmentRequest(String propertyId, LocalDate date, Long amountPaise, String reason) {
    }

    public record AdjustmentView(Adjustment adjustment, AlgorithmRun algorithm) {
    }

    public RangeView range(String scope, LocalDate from, LocalDate to) {
        Snapshot s = store.require();
        String sc = scope(s, scope);
        DateRange range = DateRange.resolve(from, to, s);
        CashFlowIndex index = s.cashFlow();
        int a = index.dayIndex(range.from());
        int b = index.dayIndex(range.to());
        OperationCounter counter = new OperationCounter();
        long start = System.nanoTime();
        RangeResult r = index.query(sc, a, b, counter);
        long elapsed = System.nanoTime() - start;
        return new RangeView(sc, range.from(), range.to(), range.days(), r.sum(), r.min(), index.dateOf(r.minIndex()),
                r.max(), index.dateOf(r.maxIndex()), index.adjustmentsIn(sc, a, b),
                AlgorithmRun.segmentTree("rangeQuery", "O(log n)", index.days(), counter.get(), elapsed));
    }

    /** One segment-tree query per calendar month or quarter within the range (clipped to the range). */
    public PeriodsView periods(String scope, LocalDate from, LocalDate to, String granularity) {
        Snapshot s = store.require();
        String sc = scope(s, scope);
        DateRange range = DateRange.resolve(from, to, s);
        String g = granularity == null ? "MONTH" : granularity.toUpperCase(Locale.ROOT);
        int step = switch (g) {
            case "MONTH" -> 1;
            case "QUARTER" -> 3;
            default -> throw new IllegalArgumentException("granularity must be MONTH or QUARTER.");
        };
        CashFlowIndex index = s.cashFlow();
        OperationCounter counter = new OperationCounter();
        List<PeriodView> periods = new ArrayList<>();
        YearMonth first = YearMonth.from(range.from());
        if (step == 3) {
            first = YearMonth.of(first.getYear(), ((first.getMonthValue() - 1) / 3) * 3 + 1);
        }
        long start = System.nanoTime();
        for (YearMonth m = first; !m.atDay(1).isAfter(range.to()); m = m.plusMonths(step)) {
            LocalDate pFrom = max(m.atDay(1), range.from());
            LocalDate pTo = min(m.plusMonths(step - 1L).atEndOfMonth(), range.to());
            RangeResult r = index.query(sc, index.dayIndex(pFrom), index.dayIndex(pTo), counter);
            String label = step == 1 ? m.toString() : m.getYear() + "-Q" + ((m.getMonthValue() - 1) / 3 + 1);
            periods.add(new PeriodView(label, pFrom, pTo, r.sum(), r.min(), index.dateOf(r.minIndex()), r.max(),
                    index.dateOf(r.maxIndex())));
        }
        long elapsed = System.nanoTime() - start;
        return new PeriodsView(sc, g, periods, AlgorithmRun.segmentTree("rangeQuery x" + periods.size(),
                "O(k log n)", index.days(), counter.get(), elapsed));
    }

    /** Daily values for charting (read from the tree's leaves, so adjustments are included). */
    public List<SeriesPoint> series(String scope, LocalDate from, LocalDate to) {
        Snapshot s = store.require();
        String sc = scope(s, scope);
        DateRange range = DateRange.resolve(from, to, s);
        CashFlowIndex index = s.cashFlow();
        List<SeriesPoint> points = new ArrayList<>(range.days());
        for (int i = index.dayIndex(range.from()); i <= index.dayIndex(range.to()); i++) {
            points.add(new SeriesPoint(index.dateOf(i), index.netAt(sc, i), index.receiptsAt(sc, i),
                    index.expensesAt(sc, i)));
        }
        return points;
    }

    public AdjustmentView adjust(AdjustmentRequest request) {
        Snapshot s = store.require();
        if (request == null || request.propertyId() == null || request.propertyId().isBlank()) {
            throw new IllegalArgumentException("propertyId is required; adjustments apply to one property.");
        }
        if (s.dataset().property(request.propertyId()) == null) {
            throw new NotFoundException("Property '" + request.propertyId() + "' does not exist.");
        }
        if (request.date() == null) {
            throw new IllegalArgumentException("date is required (YYYY-MM-DD).");
        }
        if (request.date().isBefore(s.dataset().horizonStart()) || request.date().isAfter(s.dataset().horizonEnd())) {
            throw new IllegalArgumentException("date " + request.date() + " is outside the data ("
                    + s.dataset().horizonStart() + " to " + s.dataset().horizonEnd() + ").");
        }
        if (request.amountPaise() == null || request.amountPaise() == 0) {
            throw new IllegalArgumentException("amountPaise is required and must not be zero.");
        }
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (reason.isEmpty() || reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason is required (1 to " + MAX_REASON_LENGTH + " characters), "
                    + "so every correction is explained.");
        }
        OperationCounter counter = new OperationCounter();
        long start = System.nanoTime();
        Adjustment a = s.cashFlow().adjust(request.propertyId(), request.date(), request.amountPaise(), reason, counter);
        long elapsed = System.nanoTime() - start;
        return new AdjustmentView(a, AlgorithmRun.segmentTree("pointUpdate x2 (property + portfolio)", "O(log n)",
                s.cashFlow().days(), counter.get(), elapsed));
    }

    public List<Adjustment> adjustments() {
        return store.require().cashFlow().adjustments();
    }

    public AlgorithmRun undo(String adjustmentId) {
        Snapshot s = store.require();
        OperationCounter counter = new OperationCounter();
        long start = System.nanoTime();
        if (!s.cashFlow().undo(adjustmentId, counter)) {
            throw new NotFoundException("Adjustment '" + adjustmentId + "' does not exist.");
        }
        return AlgorithmRun.segmentTree("pointUpdate x2 (inverse)", "O(log n)", s.cashFlow().days(), counter.get(),
                System.nanoTime() - start);
    }

    public void reset() {
        store.require().cashFlow().reset();
    }

    private static String scope(Snapshot s, String scope) {
        String sc = scope == null || scope.isBlank() ? CashFlowIndex.PORTFOLIO : scope.strip();
        if (sc.equalsIgnoreCase(CashFlowIndex.PORTFOLIO)) {
            return CashFlowIndex.PORTFOLIO;
        }
        if (!s.cashFlow().hasScope(sc)) {
            throw new NotFoundException("Scope '" + scope + "' is neither 'portfolio' nor a property id.");
        }
        return sc;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
