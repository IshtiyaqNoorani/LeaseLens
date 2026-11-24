package com.leaselens.api.store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.leaselens.algorithms.OperationCounter;
import com.leaselens.algorithms.segmenttree.RangeResult;
import com.leaselens.algorithms.segmenttree.SegmentTree;
import com.leaselens.data.model.Expense;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.Property;
import com.leaselens.data.model.RentTransaction;

/**
 * Daily net cash flow indexed by our {@link SegmentTree}: one tree per property and one for the
 * whole portfolio. Day {@code i} is {@code horizonStart + i}. Net flow = rent received that day
 * minus operating expenses paid that day, in paise.
 *
 * <p>Corrections are point updates applied to the property's tree and, with the same delta, to the
 * portfolio tree, so later queries reflect them in O(log n). All methods are synchronized because
 * updates and queries may arrive concurrently over HTTP.
 */
public final class CashFlowIndex {

    public static final String PORTFOLIO = "portfolio";

    private final LocalDate horizonStart;
    private final int days;
    private final Map<String, long[]> receipts = new HashMap<>();
    private final Map<String, long[]> expenses = new HashMap<>();
    private final Map<String, SegmentTree> trees = new LinkedHashMap<>();
    private final List<Adjustment> adjustments = new ArrayList<>();
    private int nextAdjustment = 1;

    public CashFlowIndex(PortfolioDataset dataset) {
        this.horizonStart = dataset.horizonStart();
        this.days = dataset.horizonDays();
        long[] portfolioReceipts = new long[days];
        long[] portfolioExpenses = new long[days];
        for (Property p : dataset.properties()) {
            receipts.put(p.id(), new long[days]);
            expenses.put(p.id(), new long[days]);
        }
        for (RentTransaction t : dataset.transactions()) {
            String property = dataset.lease(dataset.obligation(t.obligationId()).leaseId()).propertyId();
            int day = dayIndex(t.transactionDate());
            receipts.get(property)[day] = Math.addExact(receipts.get(property)[day], t.amountPaise());
            portfolioReceipts[day] = Math.addExact(portfolioReceipts[day], t.amountPaise());
        }
        for (Expense e : dataset.expenses()) {
            int day = dayIndex(e.expenseDate());
            expenses.get(e.propertyId())[day] = Math.addExact(expenses.get(e.propertyId())[day], e.amountPaise());
            portfolioExpenses[day] = Math.addExact(portfolioExpenses[day], e.amountPaise());
        }
        receipts.put(PORTFOLIO, portfolioReceipts);
        expenses.put(PORTFOLIO, portfolioExpenses);
        rebuildTrees();
    }

    private void rebuildTrees() {
        trees.clear();
        for (String scope : receipts.keySet()) {
            trees.put(scope, new SegmentTree(net(scope)));
        }
    }

    private long[] net(String scope) {
        long[] r = receipts.get(scope);
        long[] e = expenses.get(scope);
        long[] net = new long[days];
        for (int i = 0; i < days; i++) {
            net[i] = Math.subtractExact(r[i], e[i]);
        }
        return net;
    }

    public int days() {
        return days;
    }

    public LocalDate horizonStart() {
        return horizonStart;
    }

    public boolean hasScope(String scope) {
        return trees.containsKey(scope);
    }

    public int dayIndex(LocalDate date) {
        return (int) (date.toEpochDay() - horizonStart.toEpochDay());
    }

    public LocalDate dateOf(int index) {
        return horizonStart.plusDays(index);
    }

    /** Range sum / min / max of daily net cash flow for {@code scope} over day indices [from, to]. */
    public synchronized RangeResult query(String scope, int from, int to, OperationCounter counter) {
        return tree(scope).query(from, to, counter);
    }

    /** Current net value for one day (includes adjustments). */
    public synchronized long netAt(String scope, int day) {
        return tree(scope).valueAt(day);
    }

    public long receiptsAt(String scope, int day) {
        return receipts.get(scope)[day];
    }

    public long expensesAt(String scope, int day) {
        return expenses.get(scope)[day];
    }

    /**
     * Records a correction and applies it as two point updates (property tree and portfolio tree).
     * Returns the stored adjustment.
     */
    public synchronized Adjustment adjust(String propertyId, LocalDate date, long amountPaise, String reason,
                                          OperationCounter counter) {
        int day = dayIndex(date);
        applyDelta(propertyId, day, amountPaise, counter);
        Adjustment a = new Adjustment(String.format(java.util.Locale.ROOT, "ADJ-%04d", nextAdjustment++),
                propertyId, date, amountPaise, reason, java.time.Instant.now());
        adjustments.add(a);
        return a;
    }

    /** Reverses one adjustment with the inverse point updates. Returns false if it does not exist. */
    public synchronized boolean undo(String adjustmentId, OperationCounter counter) {
        for (int i = 0; i < adjustments.size(); i++) {
            Adjustment a = adjustments.get(i);
            if (a.id().equals(adjustmentId)) {
                applyDelta(a.propertyId(), dayIndex(a.date()), Math.negateExact(a.amountPaise()), counter);
                adjustments.remove(i);
                return true;
            }
        }
        return false;
    }

    /** Removes all adjustments by rebuilding every tree from the original data (O(n) per tree). */
    public synchronized void reset() {
        adjustments.clear();
        rebuildTrees();
    }

    public synchronized List<Adjustment> adjustments() {
        return List.copyOf(adjustments);
    }

    /** Number of adjustments affecting {@code scope} within [fromDay, toDay]. */
    public synchronized int adjustmentsIn(String scope, int fromDay, int toDay) {
        int count = 0;
        for (Adjustment a : adjustments) {
            int day = dayIndex(a.date());
            if (day >= fromDay && day <= toDay && (scope.equals(PORTFOLIO) || scope.equals(a.propertyId()))) {
                count++;
            }
        }
        return count;
    }

    private void applyDelta(String propertyId, int day, long delta, OperationCounter counter) {
        SegmentTree property = tree(propertyId);
        SegmentTree portfolio = tree(PORTFOLIO);
        long newProperty = Math.addExact(property.valueAt(day), delta);
        long newPortfolio = Math.addExact(portfolio.valueAt(day), delta);
        property.update(day, newProperty, counter);
        try {
            portfolio.update(day, newPortfolio, counter);
        } catch (ArithmeticException overflow) {
            property.update(day, Math.subtractExact(newProperty, delta)); // keep both trees consistent
            throw overflow;
        }
    }

    private SegmentTree tree(String scope) {
        SegmentTree tree = trees.get(scope);
        if (tree == null) {
            throw new IllegalArgumentException("Unknown scope '" + scope + "'.");
        }
        return tree;
    }
}
