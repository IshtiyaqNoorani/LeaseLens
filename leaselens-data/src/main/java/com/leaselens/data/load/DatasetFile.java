package com.leaselens.data.load;

import java.util.List;

/** The six CSV files that make up a dataset, with their required columns. */
public enum DatasetFile {
    PROPERTIES("properties.csv", true, "property_id", "property_name", "property_type", "location_zone",
            "rentable_area_sqft", "acquisition_value", "total_units"),
    TENANTS("tenants.csv", true, "tenant_id", "tenant_segment"),
    LEASES("leases.csv", true, "lease_id", "property_id", "tenant_id", "start_date", "end_date",
            "monthly_rent", "leased_area_sqft", "billing_day", "status"),
    RENT_OBLIGATIONS("rent_obligations.csv", true, "obligation_id", "lease_id", "due_date", "due_amount",
            "amount_paid", "settled_date", "status"),
    RENT_TRANSACTIONS("rent_transactions.csv", false, "transaction_id", "obligation_id", "transaction_date",
            "amount_received"),
    EXPENSES("expenses.csv", false, "expense_id", "property_id", "expense_date", "expense_category", "amount");

    private final String fileName;
    private final boolean dataRequired;
    private final List<String> columns;

    DatasetFile(String fileName, boolean dataRequired, String... columns) {
        this.fileName = fileName;
        this.dataRequired = dataRequired;
        this.columns = List.of(columns);
    }

    public String fileName() {
        return fileName;
    }

    /** True if a file with no data rows is an error; otherwise it is only a warning. */
    public boolean dataRequired() {
        return dataRequired;
    }

    public List<String> columns() {
        return columns;
    }
}
