package com.leaselens.data.model;

import java.time.LocalDate;

/**
 * One rent instalment due under a lease.
 *
 * <p>{@code amountPaidPaise}, {@code settledDate} and {@code status} are the values <em>declared</em>
 * in the CSV. The loader checks them against the transactions and rejects the dataset if they
 * disagree, so after loading they are guaranteed to be consistent.
 *
 * @param settledDate date the obligation was paid in full; null while it is still open
 */
public record RentObligation(String id, String leaseId, LocalDate dueDate, long dueAmountPaise,
                             long amountPaidPaise, LocalDate settledDate, ObligationStatus status) {

    /** Amount still owed. */
    public long balancePaise() {
        return dueAmountPaise - amountPaidPaise;
    }

    /** True if paid in full. */
    public boolean isSettled() {
        return status == ObligationStatus.PAID;
    }
}
