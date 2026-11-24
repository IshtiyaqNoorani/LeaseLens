package com.leaselens.data.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A fully validated, immutable portfolio dataset.
 *
 * <p>Only {@code DatasetLoader} creates instances, and only when validation found no errors, so
 * code that receives a {@code PortfolioDataset} can rely on referential integrity and reconciled
 * balances.
 *
 * <p>The <b>horizon</b> runs from the first day of the month of the earliest record to the last day
 * of the month of the latest record.
 */
public final class PortfolioDataset {

    private final List<Property> properties;
    private final List<Tenant> tenants;
    private final List<Lease> leases;
    private final List<RentObligation> obligations;
    private final List<RentTransaction> transactions;
    private final List<Expense> expenses;
    private final LocalDate horizonStart;
    private final LocalDate horizonEnd;

    private final Map<String, Property> propertiesById;
    private final Map<String, Lease> leasesById;
    private final Map<String, RentObligation> obligationsById;

    public PortfolioDataset(List<Property> properties, List<Tenant> tenants, List<Lease> leases,
                            List<RentObligation> obligations, List<RentTransaction> transactions,
                            List<Expense> expenses, LocalDate horizonStart, LocalDate horizonEnd) {
        this.properties = List.copyOf(properties);
        this.tenants = List.copyOf(tenants);
        this.leases = List.copyOf(leases);
        this.obligations = List.copyOf(obligations);
        this.transactions = List.copyOf(transactions);
        this.expenses = List.copyOf(expenses);
        this.horizonStart = horizonStart;
        this.horizonEnd = horizonEnd;
        this.propertiesById = index(this.properties, Property::id);
        this.leasesById = index(this.leases, Lease::id);
        this.obligationsById = index(this.obligations, RentObligation::id);
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> key) {
        Map<String, T> map = new LinkedHashMap<>();
        for (T item : items) {
            map.put(key.apply(item), item);
        }
        return Collections.unmodifiableMap(map);
    }

    public List<Property> properties() { return properties; }
    public List<Tenant> tenants() { return tenants; }
    public List<Lease> leases() { return leases; }
    public List<RentObligation> obligations() { return obligations; }
    public List<RentTransaction> transactions() { return transactions; }
    public List<Expense> expenses() { return expenses; }
    public LocalDate horizonStart() { return horizonStart; }
    public LocalDate horizonEnd() { return horizonEnd; }

    public Property property(String id) { return propertiesById.get(id); }
    public Lease lease(String id) { return leasesById.get(id); }
    public RentObligation obligation(String id) { return obligationsById.get(id); }

    /** Number of days in the horizon, inclusive. */
    public int horizonDays() {
        return (int) (horizonEnd.toEpochDay() - horizonStart.toEpochDay()) + 1;
    }
}
