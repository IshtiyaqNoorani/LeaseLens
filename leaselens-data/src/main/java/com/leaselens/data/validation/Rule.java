package com.leaselens.data.validation;

/** Validation rules. The code appears in every issue so a user can look up what went wrong. */
public enum Rule {
    MISSING_FILE("A required CSV file was not supplied."),
    EMPTY_FILE("The file has no header or no data rows."),
    MALFORMED_CSV("The file is not valid CSV."),
    MISSING_COLUMN("A required column is missing from the header."),
    UNKNOWN_COLUMN("The header contains a column LeaseLens does not use."),
    MALFORMED_ROW("The row has a different number of values from the header."),
    MISSING_VALUE("A required value is blank."),
    INVALID_DATE("The value is not a valid ISO date (YYYY-MM-DD)."),
    INVALID_NUMBER("The value is not a valid number in the expected format."),
    INVALID_VALUE("The value is not one of the allowed values or is out of range."),
    INVALID_AMOUNT("The amount must be greater than zero (or zero or more where stated)."),
    DUPLICATE_ID("The identifier appears more than once."),
    UNKNOWN_REFERENCE("The row refers to an identifier that does not exist."),
    INVALID_LEASE_PERIOD("The lease ends before it starts."),
    LEASED_AREA_EXCEEDS_RENTABLE("A lease covers more area than the property has."),
    OCCUPANCY_EXCEEDS_RENTABLE("Leases running at the same time cover more area than the property has."),
    OVERPAYMENT("Payments against an obligation exceed the amount due."),
    RECONCILIATION_MISMATCH("Declared amount paid, settled date or status does not match the transactions."),
    OBLIGATION_OUTSIDE_LEASE("An obligation falls due outside its lease period."),
    PAYMENT_BEFORE_LEASE_START("A payment is dated before its lease started.");

    private final String description;

    Rule(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
