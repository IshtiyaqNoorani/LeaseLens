package com.leaselens.api.store;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A manual correction to one property's net cash flow on one day, applied to the segment trees as
 * a point update. The original CSV data is never modified.
 */
public record Adjustment(String id, String propertyId, LocalDate date, long amountPaise, String reason,
                         Instant recordedAt) {
}
