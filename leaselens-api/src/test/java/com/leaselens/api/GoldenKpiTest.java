package com.leaselens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.leaselens.api.config.LeaseLensProperties;
import com.leaselens.api.service.CashFlowService;
import com.leaselens.api.service.CashFlowService.AdjustmentRequest;
import com.leaselens.api.service.CashFlowService.RangeView;
import com.leaselens.api.service.DatasetService;
import com.leaselens.api.service.PaymentService;
import com.leaselens.api.service.PaymentService.PercentilesView;
import com.leaselens.api.service.PortfolioService;
import com.leaselens.api.service.PortfolioService.Overview;
import com.leaselens.api.service.PortfolioService.PropertyMetrics;
import com.leaselens.api.store.DatasetStore;

/**
 * Every value asserted here is hand-computed in data/golden/EXPECTED.md. Amounts are in paise
 * (₹1 = 100 paise). If one of these tests fails, either the code or EXPECTED.md is wrong, and
 * the two must be reconciled by hand.
 */
class GoldenKpiTest {

    private static final LocalDate JAN1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate MAR31 = LocalDate.of(2026, 3, 31);

    private PortfolioService portfolio;
    private CashFlowService cashFlow;
    private PaymentService payments;

    @BeforeEach
    void loadGolden() {
        DatasetStore store = new DatasetStore();
        LeaseLensProperties props = new LeaseLensProperties(false, null);
        new DatasetService(store).loadBundled("golden");
        portfolio = new PortfolioService(store, props);
        cashFlow = new CashFlowService(store);
        payments = new PaymentService(store);
    }

    private static long rupees(long r) {
        return r * 100;
    }

    @Test
    @DisplayName("§2 Portfolio KPIs")
    void portfolioKpis() {
        Overview o = portfolio.overview(null, null); // defaults: horizon start to analysis date
        assertEquals(JAN1, o.from());
        assertEquals(MAR31, o.to());
        assertEquals(2, o.propertyCount());
        assertEquals(5_000, o.rentableAreaSqft());
        assertEquals(80.00, o.occupancyPct());
        assertEquals(73.11, o.timeWeightedOccupancyPct());
        assertEquals(rupees(495_000), o.rentBilledPaise());
        assertEquals(rupees(405_000), o.rentCollectedPaise());
        assertEquals(81.82, o.collectionEfficiencyPct());
        assertEquals(81.82, o.cashToBillingPct());
        assertEquals(rupees(90_000), o.outstandingPaise());
        assertEquals(rupees(70_000), o.operatingExpensesPaise());
        assertEquals(rupees(335_000), o.netOperatingCashSurplusPaise());
        assertEquals(17.28, o.expenseRatioPct());
        assertEquals(43.75, o.rentPerSqftRupees());
        assertEquals(2, o.receivablesAgeing().get(0).count());          // 0-30 days
        assertEquals(rupees(90_000), o.receivablesAgeing().get(0).paise());
        assertEquals(0, o.receivablesAgeing().get(1).count());
    }

    @Test
    @DisplayName("§2 Monthly trend reconciles to the period totals")
    void monthlyTrend() {
        Overview o = portfolio.overview(null, null);
        assertEquals(List.of("2026-01", "2026-02", "2026-03"), o.monthlyTrend().stream().map(m -> m.month()).toList());
        assertEquals(o.netOperatingCashSurplusPaise(),
                o.monthlyTrend().stream().mapToLong(m -> m.netOperatingCashSurplusPaise()).sum());
        assertEquals(rupees(100_000), o.monthlyTrend().get(0).netOperatingCashSurplusPaise());
        assertEquals(rupees(172_000), o.monthlyTrend().get(1).netOperatingCashSurplusPaise());
        assertEquals(rupees(63_000), o.monthlyTrend().get(2).netOperatingCashSurplusPaise());
    }

    @Test
    @DisplayName("§3 Property KPIs")
    void propertyKpis() {
        Map<String, PropertyMetrics> m = portfolio.performance(null, null, null, null).stream()
                .collect(Collectors.toMap(PropertyMetrics::propertyId, Function.identity()));
        PropertyMetrics p1 = m.get("P001");
        assertEquals(rupees(135_000), p1.rentBilledPaise());
        assertEquals(rupees(105_000), p1.rentCollectedPaise());
        assertEquals(77.78, p1.collectionEfficiencyPct());
        assertEquals(rupees(20_000), p1.operatingExpensesPaise());
        assertEquals(rupees(85_000), p1.netOperatingCashSurplusPaise());
        assertEquals(19.05, p1.expenseRatioPct());
        assertEquals(rupees(30_000), p1.outstandingPaise());
        assertEquals(100.00, p1.occupancyPct());
        assertEquals(82.78, p1.timeWeightedOccupancyPct());
        assertEquals(31_000, p1.vacantAreaDays());
        assertEquals(27.50, p1.rentPerSqftRupees());
        assertEquals(3.45, p1.cashYieldPct());

        PropertyMetrics p2 = m.get("P002");
        assertEquals(rupees(360_000), p2.rentBilledPaise());
        assertEquals(rupees(300_000), p2.rentCollectedPaise());
        assertEquals(83.33, p2.collectionEfficiencyPct());
        assertEquals(rupees(250_000), p2.netOperatingCashSurplusPaise());
        assertEquals(16.67, p2.expenseRatioPct());
        assertEquals(66.67, p2.occupancyPct());
        assertEquals(66.67, p2.timeWeightedOccupancyPct());
        assertEquals(90_000, p2.vacantAreaDays());
        assertEquals(60.00, p2.rentPerSqftRupees());
        assertEquals(3.38, p2.cashYieldPct());
    }

    @Test
    @DisplayName("§4 Payment-delay percentiles via Randomized Quickselect, for several seeds")
    void percentiles() {
        for (long seed = 1; seed <= 25; seed++) {
            PercentilesView v = payments.percentiles("10,50,75,90,100", null, seed);
            assertEquals(6, v.population().completed());
            assertEquals(2, v.population().openExcluded());
            assertEquals(2, v.population().onTimeOrEarly());
            assertEquals(7.67, v.meanDaysLate());
            assertEquals(List.of(0L, 2L, 15L, 19L, 19L), v.percentiles().stream().map(x -> x.daysLate()).toList());
            assertEquals(List.of(0, 2, 4, 5, 5), v.percentiles().stream().map(x -> x.k()).toList());
            assertEquals("RandomizedQuickselect", v.algorithm().name());
            assertEquals(seed, v.algorithm().seed());
        }
    }

    @Test
    @DisplayName("§4 Threshold records at or above P75 and open obligations")
    void thresholdAndOutstanding() {
        var t = payments.threshold("75", null, 1L, 0, 50);
        assertEquals(15, t.thresholdDaysLate());
        assertEquals(2, t.countAtOrAbove());
        assertEquals(List.of("O003", "O005"), t.records().stream().map(r -> r.obligationId()).toList());

        var open = payments.outstanding(null);
        assertEquals(rupees(90_000), open.totalOutstandingPaise());
        assertEquals(List.of("O008", "O006"), open.records().stream().map(r -> r.obligationId()).toList());
        assertEquals(30, open.records().get(0).daysOverdue());
        assertEquals(26, open.records().get(1).daysOverdue());
    }

    @Test
    @DisplayName("§5 Segment-tree range queries, including the earliest-date tie rule")
    void cashFlowRanges() {
        RangeView all = cashFlow.range("portfolio", null, null);
        assertEquals(rupees(335_000), all.netCashFlowPaise());
        assertEquals(rupees(-40_000), all.minDailyPaise());
        assertEquals(LocalDate.of(2026, 1, 20), all.minDate());
        assertEquals(rupees(120_000), all.maxDailyPaise());
        assertEquals(LocalDate.of(2026, 1, 4), all.maxDate()); // ties with 02-20; earliest wins
        assertEquals("SegmentTree", all.algorithm().name());
        assertEquals(90, all.algorithm().n());

        RangeView feb = cashFlow.range("portfolio", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertEquals(rupees(172_000), feb.netCashFlowPaise());
        assertEquals(LocalDate.of(2026, 2, 15), feb.minDate());

        RangeView mar = cashFlow.range("portfolio", LocalDate.of(2026, 3, 1), MAR31);
        assertEquals(rupees(63_000), mar.netCashFlowPaise());
        assertEquals(rupees(-12_000), mar.minDailyPaise());
        assertEquals(rupees(60_000), mar.maxDailyPaise());

        RangeView p1Jan = cashFlow.range("P001", JAN1, LocalDate.of(2026, 1, 31));
        assertEquals(rupees(20_000), p1Jan.netCashFlowPaise());
        assertEquals(rupees(-5_000), p1Jan.minDailyPaise());
        assertEquals(rupees(25_000), p1Jan.maxDailyPaise());

        var months = cashFlow.periods("portfolio", null, null, "MONTH").periods();
        assertEquals(List.of(rupees(100_000), rupees(172_000), rupees(63_000)),
                months.stream().map(p -> p.netCashFlowPaise()).toList());
    }

    @Test
    @DisplayName("§5 Correction scenario: point update is reflected, then undone")
    void correctionScenario() {
        var view = cashFlow.adjust(new AdjustmentRequest("P002", LocalDate.of(2026, 1, 20), rupees(40_000),
                "Reversed property-tax charge"));
        assertTrue(view.algorithm().operations() > 0);

        RangeView jan = cashFlow.range("portfolio", JAN1, LocalDate.of(2026, 1, 31));
        assertEquals(rupees(-5_000), jan.minDailyPaise());
        assertEquals(LocalDate.of(2026, 1, 10), jan.minDate());
        assertEquals(1, jan.adjustmentsApplied());
        assertEquals(rupees(375_000), cashFlow.range("portfolio", null, null).netCashFlowPaise());
        assertEquals(0, cashFlow.range("P002", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 20)).netCashFlowPaise());
        // Adjustments affect only the explorer; Overview KPIs still come from the source data.
        assertEquals(rupees(335_000), portfolio.overview(null, null).netOperatingCashSurplusPaise());

        cashFlow.undo(view.adjustment().id());
        assertEquals(rupees(335_000), cashFlow.range("portfolio", null, null).netCashFlowPaise());
        assertEquals(LocalDate.of(2026, 1, 20), cashFlow.range("portfolio", null, null).minDate());
    }

    @Test
    @DisplayName("Ratios with a zero denominator are reported as not applicable (null), never 0")
    void zeroDenominator() {
        // P001 billed nothing before L001's first instalment on Jan 1, so a single-day range in which
        // nothing falls due gives no collection-efficiency figure.
        PropertyMetrics m = portfolio.performance(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3), null, null)
                .get(0);
        assertEquals(0, m.rentBilledPaise());
        assertNull(m.collectionEfficiencyPct());
    }
}
