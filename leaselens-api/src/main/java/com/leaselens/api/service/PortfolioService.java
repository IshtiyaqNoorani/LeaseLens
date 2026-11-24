package com.leaselens.api.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.leaselens.api.config.LeaseLensProperties;
import com.leaselens.api.service.ApiExceptions.NotFoundException;
import com.leaselens.api.store.DatasetStore;
import com.leaselens.api.store.Snapshot;
import com.leaselens.data.model.Expense;
import com.leaselens.data.model.Lease;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.Property;
import com.leaselens.data.model.RentObligation;
import com.leaselens.data.model.RentTransaction;

/**
 * Portfolio and property KPIs. Every figure is computed from the loaded data; formulas are listed
 * in PROJECT_PLAN.md §6.1 and on the Methodology screen.
 *
 * <p>Bases: rent billed is accrual (obligations due in the period); rent collected and operating
 * expenses are cash (dated in the period); collection efficiency is cohort-based (paid by the
 * analysis date on obligations due in the period). Net operating cash surplus is a cash-basis
 * figure, not NOI and not an audited measure.
 */
@Service
public class PortfolioService {

    private final DatasetStore store;
    private final LeaseLensProperties properties;

    public PortfolioService(DatasetStore store, LeaseLensProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ DTOs

    public record PropertyMetrics(String propertyId, String name, String type, String zone, long rentableAreaSqft,
                                  long rentBilledPaise, long rentCollectedPaise, long cohortPaidPaise,
                                  Double collectionEfficiencyPct, Double cashToBillingPct,
                                  long leasedAreaSqft, Double occupancyPct, Double timeWeightedOccupancyPct,
                                  long vacantAreaDays, long contractRentMonthlyPaise, Double rentPerSqftRupees,
                                  long operatingExpensesPaise, long netOperatingCashSurplusPaise,
                                  Double expenseRatioPct, Double cashYieldPct, long outstandingPaise,
                                  long maxDaysOverdue, long longOverduePaise) {
    }

    public record AgeingBucket(String bucket, long count, long paise) {
    }

    public record MonthPoint(String month, long rentBilledPaise, long rentCollectedPaise,
                             long operatingExpensesPaise, long netOperatingCashSurplusPaise) {
    }

    public record AttentionFlag(String propertyId, String propertyName, String rule, double observed,
                                double threshold, String message) {
    }

    public record Overview(LocalDate from, LocalDate to, LocalDate analysisDate, int propertyCount,
                           long rentableAreaSqft, long leasedAreaSqft, Double occupancyPct,
                           Double timeWeightedOccupancyPct, long rentBilledPaise, long rentCollectedPaise,
                           Double collectionEfficiencyPct, Double cashToBillingPct, long outstandingPaise,
                           List<AgeingBucket> receivablesAgeing, long operatingExpensesPaise,
                           long netOperatingCashSurplusPaise, Double expenseRatioPct, Double rentPerSqftRupees,
                           List<MonthPoint> monthlyTrend, List<AttentionFlag> attention) {
    }

    public record Segment(String key, int propertyCount, long rentableAreaSqft, long rentBilledPaise,
                          long rentCollectedPaise, Double collectionEfficiencyPct, Double timeWeightedOccupancyPct,
                          long operatingExpensesPaise, long netOperatingCashSurplusPaise, Double expenseRatioPct,
                          long outstandingPaise) {
    }

    public record LeaseView(String leaseId, String tenantId, LocalDate startDate, LocalDate endDate,
                            long monthlyRentPaise, long leasedAreaSqft, int billingDay, String stateOnAnalysisDate) {
    }

    public record PropertyDetail(PropertyMetrics metrics, long acquisitionValuePaise, int totalUnits,
                                 List<LeaseView> leases, List<MonthPoint> monthlyTrend) {
    }

    // ------------------------------------------------------------------ public API

    public Overview overview(LocalDate from, LocalDate to) {
        Snapshot s = store.require();
        DateRange range = DateRange.resolve(from, to, s);
        List<PropertyMetrics> rows = computeAll(s, range);

        long area = 0, leased = 0, leasedDays = 0, billed = 0, collected = 0, cohortPaid = 0, opex = 0,
                outstanding = 0, contractRent = 0;
        for (PropertyMetrics m : rows) {
            area += m.rentableAreaSqft();
            leased += m.leasedAreaSqft();
            leasedDays += m.rentableAreaSqft() * range.days() - m.vacantAreaDays();
            billed += m.rentBilledPaise();
            collected += m.rentCollectedPaise();
            cohortPaid += m.cohortPaidPaise();
            opex += m.operatingExpensesPaise();
            outstanding += m.outstandingPaise();
            contractRent += m.contractRentMonthlyPaise();
        }
        long surplus = collected - opex;
        return new Overview(range.from(), range.to(), s.analysisDate(), rows.size(), area, leased,
                Ratios.pct(leased, area), Ratios.pct(leasedDays, area * range.days()), billed, collected,
                Ratios.pct(cohortPaid, billed), Ratios.pct(collected, billed), outstanding, ageing(s, null),
                opex, surplus, Ratios.pct(opex, collected), Ratios.rupeesPer(contractRent, leased),
                monthlyTrend(s, range, null), attention(rows));
    }

    public List<PropertyMetrics> performance(LocalDate from, LocalDate to, String sort, String order) {
        Snapshot s = store.require();
        DateRange range = DateRange.resolve(from, to, s);
        List<PropertyMetrics> rows = new ArrayList<>(computeAll(s, range));
        rows.sort(comparator(sort, order));
        return rows;
    }

    /** Portfolio KPIs grouped by property type or location zone, recomputed from the property rows. */
    public List<Segment> segments(LocalDate from, LocalDate to, String dimension) {
        Snapshot s = store.require();
        DateRange range = DateRange.resolve(from, to, s);
        String dim = dimension == null ? "type" : dimension.strip().toLowerCase(java.util.Locale.ROOT);
        Function<PropertyMetrics, String> key = switch (dim) {
            case "type" -> PropertyMetrics::type;
            case "zone" -> PropertyMetrics::zone;
            default -> throw new IllegalArgumentException("dimension must be 'type' or 'zone'.");
        };
        Map<String, long[]> sums = new java.util.TreeMap<>();
        // count, area, billed, collected, cohortPaid, leasedAreaDays, opex, outstanding
        for (PropertyMetrics m : computeAll(s, range)) {
            long[] v = sums.computeIfAbsent(key.apply(m), k -> new long[8]);
            v[0]++;
            v[1] += m.rentableAreaSqft();
            v[2] += m.rentBilledPaise();
            v[3] += m.rentCollectedPaise();
            v[4] += m.cohortPaidPaise();
            v[5] += m.rentableAreaSqft() * range.days() - m.vacantAreaDays();
            v[6] += m.operatingExpensesPaise();
            v[7] += m.outstandingPaise();
        }
        List<Segment> result = new ArrayList<>();
        sums.forEach((k, v) -> result.add(new Segment(k, (int) v[0], v[1], v[2], v[3], Ratios.pct(v[4], v[2]),
                Ratios.pct(v[5], v[1] * range.days()), v[6], v[3] - v[6], Ratios.pct(v[6], v[3]), v[7])));
        return result;
    }

    public PropertyDetail property(String propertyId, LocalDate from, LocalDate to) {
        Snapshot s = store.require();
        Property p = s.dataset().property(propertyId);
        if (p == null) {
            throw new NotFoundException("Property '" + propertyId + "' does not exist.");
        }
        DateRange range = DateRange.resolve(from, to, s);
        PropertyMetrics metrics = compute(s, range, p);
        List<LeaseView> leases = new ArrayList<>();
        for (Lease l : s.dataset().leases()) {
            if (l.propertyId().equals(propertyId)) {
                String state = l.isActiveOn(s.analysisDate()) ? "ACTIVE"
                        : l.endDate().isBefore(s.analysisDate()) ? "ENDED" : "NOT_STARTED";
                leases.add(new LeaseView(l.id(), l.tenantId(), l.startDate(), l.endDate(), l.monthlyRentPaise(),
                        l.leasedAreaSqft(), l.billingDay(), state));
            }
        }
        leases.sort(Comparator.comparing(LeaseView::startDate).thenComparing(LeaseView::leaseId));
        return new PropertyDetail(metrics, p.acquisitionValuePaise(), p.totalUnits(), leases,
                monthlyTrend(s, range, propertyId));
    }

    // ------------------------------------------------------------------ calculations

    List<PropertyMetrics> computeAll(Snapshot s, DateRange range) {
        List<PropertyMetrics> rows = new ArrayList<>();
        for (Property p : s.dataset().properties()) {
            rows.add(compute(s, range, p));
        }
        return rows;
    }

    PropertyMetrics compute(Snapshot s, DateRange range, Property p) {
        PortfolioDataset d = s.dataset();
        LocalDate asOf = s.analysisDate();
        long billed = 0, cohortPaid = 0, collected = 0, opex = 0, outstanding = 0, maxOverdue = 0, longOverdue = 0;
        int longOverdueDays = properties.attention().overdueDays();
        long leasedNow = 0, contractRent = 0, leasedAreaDays = 0;

        for (Lease l : d.leases()) {
            if (!l.propertyId().equals(p.id())) {
                continue;
            }
            if (l.isActiveOn(asOf)) {
                leasedNow += l.leasedAreaSqft();
                contractRent += l.monthlyRentPaise();
            }
            LocalDate a = l.startDate().isAfter(range.from()) ? l.startDate() : range.from();
            LocalDate b = l.endDate().isBefore(range.to()) ? l.endDate() : range.to();
            if (!a.isAfter(b)) {
                leasedAreaDays += l.leasedAreaSqft() * (ChronoUnit.DAYS.between(a, b) + 1);
            }
        }
        for (RentObligation o : d.obligations()) {
            if (!d.lease(o.leaseId()).propertyId().equals(p.id())) {
                continue;
            }
            if (range.contains(o.dueDate())) {
                billed += o.dueAmountPaise();
                cohortPaid += s.ledger().paidAsOf(o, asOf);
            }
            if (!o.dueDate().isAfter(asOf)) {
                long balance = s.ledger().balanceAsOf(o, asOf);
                if (balance > 0) {
                    long overdue = ChronoUnit.DAYS.between(o.dueDate(), asOf);
                    outstanding += balance;
                    maxOverdue = Math.max(maxOverdue, overdue);
                    if (overdue > longOverdueDays) {
                        longOverdue += balance;
                    }
                }
            }
        }
        for (RentTransaction t : d.transactions()) {
            if (range.contains(t.transactionDate())
                    && d.lease(d.obligation(t.obligationId()).leaseId()).propertyId().equals(p.id())) {
                collected += t.amountPaise();
            }
        }
        for (Expense e : d.expenses()) {
            if (e.propertyId().equals(p.id()) && range.contains(e.expenseDate())) {
                opex += e.amountPaise();
            }
        }
        long surplus = collected - opex;
        long areaDays = p.rentableAreaSqft() * range.days();
        // Cash yield: surplus annualised over the period length (× 365 / days), divided by acquisition value.
        Double cashYield = Ratios.pct(Math.round(surplus * 365.0 / range.days()), p.acquisitionValuePaise());
        return new PropertyMetrics(p.id(), p.name(), p.type().name(), p.zone(), p.rentableAreaSqft(),
                billed, collected, cohortPaid, Ratios.pct(cohortPaid, billed), Ratios.pct(collected, billed),
                leasedNow, Ratios.pct(leasedNow, p.rentableAreaSqft()), Ratios.pct(leasedAreaDays, areaDays),
                areaDays - leasedAreaDays, contractRent, Ratios.rupeesPer(contractRent, leasedNow),
                opex, surplus, Ratios.pct(opex, collected), cashYield, outstanding, maxOverdue, longOverdue);
    }

    /** Open balances as of the analysis date, grouped by days overdue. */
    List<AgeingBucket> ageing(Snapshot s, String propertyId) {
        long[][] buckets = new long[4][2];
        LocalDate asOf = s.analysisDate();
        for (RentObligation o : s.dataset().obligations()) {
            if (o.dueDate().isAfter(asOf)
                    || (propertyId != null && !s.dataset().lease(o.leaseId()).propertyId().equals(propertyId))) {
                continue;
            }
            long balance = s.ledger().balanceAsOf(o, asOf);
            if (balance <= 0) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(o.dueDate(), asOf);
            int b = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
            buckets[b][0]++;
            buckets[b][1] += balance;
        }
        String[] labels = {"0-30", "31-60", "61-90", "90+"};
        List<AgeingBucket> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            result.add(new AgeingBucket(labels[i], buckets[i][0], buckets[i][1]));
        }
        return result;
    }

    List<MonthPoint> monthlyTrend(Snapshot s, DateRange range, String propertyId) {
        PortfolioDataset d = s.dataset();
        Map<YearMonth, long[]> months = new LinkedHashMap<>(); // billed, collected, opex
        for (YearMonth m = YearMonth.from(range.from()); !m.isAfter(YearMonth.from(range.to())); m = m.plusMonths(1)) {
            months.put(m, new long[3]);
        }
        Function<String, Boolean> inScope = pid -> propertyId == null || propertyId.equals(pid);
        for (RentObligation o : d.obligations()) {
            if (range.contains(o.dueDate()) && inScope.apply(d.lease(o.leaseId()).propertyId())) {
                months.get(YearMonth.from(o.dueDate()))[0] += o.dueAmountPaise();
            }
        }
        for (RentTransaction t : d.transactions()) {
            if (range.contains(t.transactionDate())
                    && inScope.apply(d.lease(d.obligation(t.obligationId()).leaseId()).propertyId())) {
                months.get(YearMonth.from(t.transactionDate()))[1] += t.amountPaise();
            }
        }
        for (Expense e : d.expenses()) {
            if (range.contains(e.expenseDate()) && inScope.apply(e.propertyId())) {
                months.get(YearMonth.from(e.expenseDate()))[2] += e.amountPaise();
            }
        }
        List<MonthPoint> points = new ArrayList<>();
        months.forEach((m, v) -> points.add(new MonthPoint(m.toString(), v[0], v[1], v[2], v[1] - v[2])));
        return points;
    }

    List<AttentionFlag> attention(List<PropertyMetrics> rows) {
        LeaseLensProperties.Attention t = properties.attention();
        List<AttentionFlag> flags = new ArrayList<>();
        for (PropertyMetrics m : rows) {
            if (m.collectionEfficiencyPct() != null && m.collectionEfficiencyPct() < t.collectionEfficiencyBelowPct()) {
                flags.add(new AttentionFlag(m.propertyId(), m.name(), "LOW_COLLECTION_EFFICIENCY",
                        m.collectionEfficiencyPct(), t.collectionEfficiencyBelowPct(),
                        "Collected " + m.collectionEfficiencyPct() + "% of rent billed in the period (rule: below "
                                + t.collectionEfficiencyBelowPct() + "%)."));
            }
            if (m.timeWeightedOccupancyPct() != null && m.timeWeightedOccupancyPct() < t.occupancyBelowPct()) {
                flags.add(new AttentionFlag(m.propertyId(), m.name(), "LOW_OCCUPANCY", m.timeWeightedOccupancyPct(),
                        t.occupancyBelowPct(), "Time-weighted occupancy " + m.timeWeightedOccupancyPct()
                                + "% over the period (rule: below " + t.occupancyBelowPct() + "%)."));
            }
            if (m.expenseRatioPct() != null && m.expenseRatioPct() > t.expenseRatioAbovePct()) {
                flags.add(new AttentionFlag(m.propertyId(), m.name(), "HIGH_EXPENSE_RATIO", m.expenseRatioPct(),
                        t.expenseRatioAbovePct(), "Operating expenses were " + m.expenseRatioPct()
                                + "% of rent collected (rule: above " + t.expenseRatioAbovePct() + "%)."));
            }
            Double overdueShare = Ratios.pct(m.longOverduePaise(), m.rentBilledPaise());
            if (m.longOverduePaise() > 0 && (overdueShare == null || overdueShare > t.overdueShareAbovePct())) {
                flags.add(new AttentionFlag(m.propertyId(), m.name(), "LONG_OVERDUE_BALANCE",
                        overdueShare == null ? 100.0 : overdueShare, t.overdueShareAbovePct(),
                        "Balances overdue for more than " + t.overdueDays() + " days equal "
                                + (overdueShare == null ? "all" : overdueShare + "%") + " of rent billed in the period "
                                + "(rule: above " + t.overdueShareAbovePct() + "%)."));
            }
        }
        return flags;
    }

    /** Sort keys allowed for the league table. Nulls ("not applicable") always sort last. */
    private static final Map<String, Function<PropertyMetrics, Comparable<?>>> SORT_KEYS = new LinkedHashMap<>();

    static {
        SORT_KEYS.put("propertyId", PropertyMetrics::propertyId);
        SORT_KEYS.put("name", PropertyMetrics::name);
        SORT_KEYS.put("rentBilledPaise", PropertyMetrics::rentBilledPaise);
        SORT_KEYS.put("rentCollectedPaise", PropertyMetrics::rentCollectedPaise);
        SORT_KEYS.put("collectionEfficiencyPct", PropertyMetrics::collectionEfficiencyPct);
        SORT_KEYS.put("occupancyPct", PropertyMetrics::occupancyPct);
        SORT_KEYS.put("timeWeightedOccupancyPct", PropertyMetrics::timeWeightedOccupancyPct);
        SORT_KEYS.put("rentPerSqftRupees", PropertyMetrics::rentPerSqftRupees);
        SORT_KEYS.put("operatingExpensesPaise", PropertyMetrics::operatingExpensesPaise);
        SORT_KEYS.put("netOperatingCashSurplusPaise", PropertyMetrics::netOperatingCashSurplusPaise);
        SORT_KEYS.put("expenseRatioPct", PropertyMetrics::expenseRatioPct);
        SORT_KEYS.put("cashYieldPct", PropertyMetrics::cashYieldPct);
        SORT_KEYS.put("vacantAreaDays", PropertyMetrics::vacantAreaDays);
        SORT_KEYS.put("outstandingPaise", PropertyMetrics::outstandingPaise);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<PropertyMetrics> comparator(String sort, String order) {
        String key = sort == null || sort.isBlank() ? "propertyId" : sort;
        Function<PropertyMetrics, Comparable<?>> f = SORT_KEYS.get(key);
        if (f == null) {
            throw new IllegalArgumentException("Cannot sort by '" + sort + "'. Allowed: "
                    + String.join(", ", SORT_KEYS.keySet()) + ".");
        }
        if (order != null && !order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("order must be 'asc' or 'desc'.");
        }
        Comparator<Comparable> direction = "desc".equalsIgnoreCase(order)
                ? Comparator.reverseOrder() : Comparator.naturalOrder();
        Comparator<PropertyMetrics> c = Comparator.comparing(m -> (Comparable) f.apply(m), Comparator.nullsLast(direction));
        return c.thenComparing(PropertyMetrics::propertyId);
    }
}
