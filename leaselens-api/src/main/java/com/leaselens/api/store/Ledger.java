package com.leaselens.api.store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.RentObligation;
import com.leaselens.data.model.RentTransaction;

/**
 * Payment history per obligation, used to answer "how much had been paid, and was it settled,
 * <em>as of</em> a given date". This lets every KPI respect the analysis date: payments recorded
 * after it are ignored.
 */
public final class Ledger {

    private final Map<String, List<RentTransaction>> paymentsByObligation = new HashMap<>();

    public Ledger(PortfolioDataset dataset) {
        for (RentTransaction t : dataset.transactions()) {
            paymentsByObligation.computeIfAbsent(t.obligationId(), k -> new ArrayList<>()).add(t);
        }
        Comparator<RentTransaction> order = Comparator.comparing(RentTransaction::transactionDate)
                .thenComparing(RentTransaction::id);
        paymentsByObligation.values().forEach(list -> list.sort(order));
    }

    /** Payments for an obligation, oldest first. */
    public List<RentTransaction> payments(String obligationId) {
        return paymentsByObligation.getOrDefault(obligationId, List.of());
    }

    /** Amount paid on or before {@code asOf}, in paise. */
    public long paidAsOf(RentObligation o, LocalDate asOf) {
        long paid = 0;
        for (RentTransaction t : payments(o.id())) {
            if (t.transactionDate().isAfter(asOf)) {
                break;
            }
            paid += t.amountPaise();
        }
        return paid;
    }

    /** Balance still owed at the end of {@code asOf}, in paise. */
    public long balanceAsOf(RentObligation o, LocalDate asOf) {
        return o.dueAmountPaise() - paidAsOf(o, asOf);
    }

    /** Date the obligation was paid in full, if that happened on or before {@code asOf}; otherwise null. */
    public LocalDate settledAsOf(RentObligation o, LocalDate asOf) {
        LocalDate settled = o.settledDate();
        return settled != null && !settled.isAfter(asOf) ? settled : null;
    }
}
