package com.leaselens.data.model;

import java.time.LocalDate;

/**
 * A lease of part of a property. Dates are inclusive.
 *
 * @param billingDay day of month (1 to 28) on which rent falls due
 */
public record Lease(String id, String propertyId, String tenantId, LocalDate startDate, LocalDate endDate,
                    long monthlyRentPaise, long leasedAreaSqft, int billingDay, LeaseStatus status) {

    /** True if the lease covers {@code date}. */
    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
