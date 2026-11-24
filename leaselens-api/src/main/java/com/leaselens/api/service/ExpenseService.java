package com.leaselens.api.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.springframework.stereotype.Service;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.selection.Percentile;
import com.leaselens.algorithms.selection.RandomizedQuickselect;
import com.leaselens.api.service.ApiExceptions.NotFoundException;
import com.leaselens.api.store.DatasetStore;
import com.leaselens.api.store.Snapshot;
import com.leaselens.data.model.Expense;
import com.leaselens.data.model.ExpenseCategory;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.Property;
import com.leaselens.data.model.RentTransaction;

/**
 * Operating expense analysis (cash basis).
 *
 * <p>"Unusual" records use Tukey's fences within each property × category: a record is flagged if
 * it exceeds Q3 + k × IQR of that property's history for that category up to the analysis date.
 * k defaults to 3 (Tukey's "far out" fence) because money amounts are right-skewed and the classic
 * k = 1.5 flags many ordinary records; k = 1.5 is available on request.
 * Quartiles are nearest-rank P25/P75 computed with our Randomized Quickselect. A flag means
 * "review recommended"; it is never evidence of fraud.
 */
@Service
public class ExpenseService {

    /** Groups smaller than this are too small for quartiles to mean anything; they are skipped and counted. */
    static final int MIN_GROUP_SIZE = 8;
    private static final long FENCE_SEED = 20261124L;

    private final DatasetStore store;

    public ExpenseService(DatasetStore store) {
        this.store = store;
    }

    public record CategoryTotal(String category, long paise, Double sharePct) {
    }

    public record PropertyExpense(String propertyId, String name, String type, long operatingExpensesPaise,
                                  long rentCollectedPaise, Double expenseToRentPct, Double expensePerSqftRupees) {
    }

    public record MonthExpense(String month, long totalPaise, Map<String, Long> byCategoryPaise) {
    }

    public record SummaryView(LocalDate from, LocalDate to, String propertyId, long totalPaise,
                              List<CategoryTotal> byCategory, List<PropertyExpense> byProperty,
                              List<MonthExpense> monthly) {
    }

    public record UnusualRecord(String expenseId, String propertyId, String propertyName, String category,
                                LocalDate date, long amountPaise, long q1Paise, long q3Paise, long upperFencePaise,
                                Double timesTypical, int groupSize) {
    }

    public record UnusualView(LocalDate from, LocalDate to, double fenceMultiplier, int groupsAnalysed,
                              int groupsSkippedTooSmall,
                              List<UnusualRecord> records, String note, AlgorithmRun algorithm) {
    }

    public SummaryView summary(LocalDate from, LocalDate to, String propertyId) {
        Snapshot s = store.require();
        PortfolioDataset d = s.dataset();
        if (propertyId != null && !propertyId.isBlank() && d.property(propertyId) == null) {
            throw new NotFoundException("Property '" + propertyId + "' does not exist.");
        }
        String pid = propertyId == null || propertyId.isBlank() ? null : propertyId;
        DateRange range = DateRange.resolve(from, to, s);

        Map<ExpenseCategory, Long> byCategory = new EnumMap<>(ExpenseCategory.class);
        Map<String, long[]> byProperty = new LinkedHashMap<>(); // opex, collected
        for (Property p : d.properties()) {
            if (pid == null || pid.equals(p.id())) {
                byProperty.put(p.id(), new long[2]);
            }
        }
        Map<YearMonth, Map<String, Long>> monthly = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(range.from()); !m.isAfter(YearMonth.from(range.to())); m = m.plusMonths(1)) {
            monthly.put(m, new LinkedHashMap<>());
        }
        long total = 0;
        for (Expense e : d.expenses()) {
            if (!range.contains(e.expenseDate()) || !byProperty.containsKey(e.propertyId())) {
                continue;
            }
            total += e.amountPaise();
            byCategory.merge(e.category(), e.amountPaise(), Long::sum);
            byProperty.get(e.propertyId())[0] += e.amountPaise();
            monthly.get(YearMonth.from(e.expenseDate())).merge(e.category().name(), e.amountPaise(), Long::sum);
        }
        for (RentTransaction t : d.transactions()) {
            String p = d.lease(d.obligation(t.obligationId()).leaseId()).propertyId();
            if (range.contains(t.transactionDate()) && byProperty.containsKey(p)) {
                byProperty.get(p)[1] += t.amountPaise();
            }
        }
        List<CategoryTotal> categories = new ArrayList<>();
        for (Map.Entry<ExpenseCategory, Long> e : byCategory.entrySet()) {
            categories.add(new CategoryTotal(e.getKey().name(), e.getValue(), Ratios.pct(e.getValue(), total)));
        }
        categories.sort(Comparator.comparingLong(CategoryTotal::paise).reversed());
        List<PropertyExpense> properties = new ArrayList<>();
        byProperty.forEach((id, v) -> {
            Property p = d.property(id);
            properties.add(new PropertyExpense(id, p.name(), p.type().name(), v[0], v[1], Ratios.pct(v[0], v[1]),
                    Ratios.rupeesPer(v[0], p.rentableAreaSqft())));
        });
        properties.sort(Comparator.comparingLong(PropertyExpense::operatingExpensesPaise).reversed()
                .thenComparing(PropertyExpense::propertyId));
        List<MonthExpense> months = new ArrayList<>();
        monthly.forEach((m, cats) -> months.add(new MonthExpense(m.toString(),
                cats.values().stream().mapToLong(Long::longValue).sum(), cats)));
        return new SummaryView(range.from(), range.to(), pid, total, categories, properties, months);
    }

    /**
     * @param fence Tukey multiplier: 3 (default, "far out" values) or 1.5 (the classic inner fence,
     *              which flags many records in skewed money data)
     */
    public UnusualView unusual(LocalDate from, LocalDate to, Double fence) {
        double multiplier = fence == null ? 3.0 : fence;
        if (multiplier != 1.5 && multiplier != 3.0) {
            throw new IllegalArgumentException("fence must be 1.5 or 3.");
        }
        Snapshot s = store.require();
        PortfolioDataset d = s.dataset();
        DateRange range = DateRange.resolve(from, to, s);

        // History per property × category up to the analysis date.
        Map<String, List<Expense>> groups = new LinkedHashMap<>();
        for (Expense e : d.expenses()) {
            if (!e.expenseDate().isAfter(s.analysisDate())) {
                groups.computeIfAbsent(e.propertyId() + "|" + e.category(), k -> new ArrayList<>()).add(e);
            }
        }
        OperationCounter counter = new OperationCounter();
        SplittableRandom rng = new SplittableRandom(FENCE_SEED);
        int analysed = 0;
        int skipped = 0;
        long elements = 0;
        List<UnusualRecord> flagged = new ArrayList<>();
        long start = System.nanoTime();
        for (List<Expense> group : groups.values()) {
            if (group.size() < MIN_GROUP_SIZE) {
                skipped++;
                continue;
            }
            analysed++;
            long[] amounts = group.stream().mapToLong(Expense::amountPaise).toArray();
            elements += amounts.length;
            int k1 = Percentile.nearestRankIndex(amounts.length, 2500);
            int k2 = Percentile.nearestRankIndex(amounts.length, 5000);
            int k3 = Percentile.nearestRankIndex(amounts.length, 7500);
            long[] q = RandomizedQuickselect.selectMany(amounts, new int[] {k1, k2, k3}, rng, counter);
            long upper = Math.addExact(q[2], Math.round(multiplier * (q[2] - q[0])));
            for (Expense e : group) {
                if (range.contains(e.expenseDate()) && e.amountPaise() > upper) {
                    flagged.add(new UnusualRecord(e.id(), e.propertyId(), d.property(e.propertyId()).name(),
                            e.category().name(), e.expenseDate(), e.amountPaise(), q[0], q[2], upper,
                            Ratios.ratio(e.amountPaise(), q[1]), group.size()));
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        flagged.sort(Comparator.comparing(UnusualRecord::timesTypical, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnusualRecord::expenseId));
        return new UnusualView(range.from(), range.to(), multiplier, analysed, skipped, flagged,
                "Records above Q3 + " + multiplier + " x IQR of the same property and category. Unusual does not mean wrong: "
                        + "review before drawing conclusions.",
                AlgorithmRun.quickselect("select x3 per group (" + analysed + " groups)", (int) elements,
                        counter.get(), elapsed, FENCE_SEED));
    }
}
