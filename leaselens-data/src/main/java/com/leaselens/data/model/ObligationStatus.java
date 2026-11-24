package com.leaselens.data.model;

/** Settlement state of a rent obligation, derived from its transactions. */
public enum ObligationStatus {
    /** Paid in full. */
    PAID,
    /** Some, but not all, of the amount due has been received. */
    PARTIAL,
    /** Nothing has been received. */
    UNPAID;

    /** Status implied by the amount paid against the amount due. */
    public static ObligationStatus derive(long paidPaise, long duePaise) {
        if (paidPaise <= 0) {
            return UNPAID;
        }
        return paidPaise >= duePaise ? PAID : PARTIAL;
    }
}
