package com.leaselens.data.load;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.leaselens.data.csv.CsvFormatException;
import com.leaselens.data.csv.CsvParser;
import com.leaselens.data.csv.CsvTable;
import com.leaselens.data.model.Expense;
import com.leaselens.data.model.ExpenseCategory;
import com.leaselens.data.model.Lease;
import com.leaselens.data.model.LeaseStatus;
import com.leaselens.data.model.ObligationStatus;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.Property;
import com.leaselens.data.model.PropertyType;
import com.leaselens.data.model.RentObligation;
import com.leaselens.data.model.RentTransaction;
import com.leaselens.data.model.Tenant;
import com.leaselens.data.model.TenantSegment;
import com.leaselens.data.validation.FileSummary;
import com.leaselens.data.validation.IssueCollector;
import com.leaselens.data.validation.Rule;

/**
 * Reads the six CSV files, validates them and builds a {@link PortfolioDataset}.
 *
 * <p>Stages:
 * <ol>
 *   <li>File checks: present, well-formed CSV, required columns, not empty.</li>
 *   <li>Row checks: required values, dates, numbers, allowed values, positive amounts, duplicate ids.</li>
 *   <li>Cross-file checks: references, lease periods, area limits (sweep line over lease start and
 *       end dates), overpayment and reconciliation of declared payment fields with transactions.</li>
 * </ol>
 * All issues are collected (up to a cap) rather than stopping at the first, so a user can fix a
 * file in one pass. If there is any error, no dataset is returned.
 */
public final class DatasetLoader {

    private static final String P = DatasetFile.PROPERTIES.fileName();
    private static final String T = DatasetFile.TENANTS.fileName();
    private static final String L = DatasetFile.LEASES.fileName();
    private static final String O = DatasetFile.RENT_OBLIGATIONS.fileName();
    private static final String X = DatasetFile.RENT_TRANSACTIONS.fileName();
    private static final String E = DatasetFile.EXPENSES.fileName();

    /** Loads and validates the dataset in {@code source}. */
    public LoadResult load(DatasetSource source) throws IOException {
        IssueCollector issues = new IssueCollector();
        Map<DatasetFile, CsvTable> tables = new EnumMap<>(DatasetFile.class);
        Map<DatasetFile, int[]> rowCounts = new EnumMap<>(DatasetFile.class); // {rows, accepted}

        for (DatasetFile file : DatasetFile.values()) {
            readTable(source, file, issues).ifPresent(table -> tables.put(file, table));
        }

        Map<String, Line<Property>> properties = parseRows(tables, DatasetFile.PROPERTIES, issues, rowCounts,
                r -> new Property(r.text("property_id"), r.text("property_name"),
                        r.enumValue("property_type", PropertyType.class), r.text("location_zone"),
                        r.wholeNumber("rentable_area_sqft", 1, Long.MAX_VALUE),
                        r.money("acquisition_value", false),
                        (int) r.wholeNumber("total_units", 1, 100_000)),
                Property::id);
        Map<String, Line<Tenant>> tenants = parseRows(tables, DatasetFile.TENANTS, issues, rowCounts,
                r -> new Tenant(r.text("tenant_id"), r.enumValue("tenant_segment", TenantSegment.class)),
                Tenant::id);
        Map<String, Line<Lease>> leases = parseRows(tables, DatasetFile.LEASES, issues, rowCounts,
                r -> new Lease(r.text("lease_id"), r.text("property_id"), r.text("tenant_id"),
                        r.date("start_date"), r.date("end_date"), r.money("monthly_rent", false),
                        r.wholeNumber("leased_area_sqft", 1, Long.MAX_VALUE),
                        (int) r.wholeNumber("billing_day", 1, 28), r.enumValue("status", LeaseStatus.class)),
                Lease::id);
        Map<String, Line<RentObligation>> obligations = parseRows(tables, DatasetFile.RENT_OBLIGATIONS, issues,
                rowCounts,
                r -> new RentObligation(r.text("obligation_id"), r.text("lease_id"), r.date("due_date"),
                        r.money("due_amount", false), r.money("amount_paid", true), r.optionalDate("settled_date"),
                        r.enumValue("status", ObligationStatus.class)),
                RentObligation::id);
        Map<String, Line<RentTransaction>> transactions = parseRows(tables, DatasetFile.RENT_TRANSACTIONS, issues,
                rowCounts,
                r -> new RentTransaction(r.text("transaction_id"), r.text("obligation_id"),
                        r.date("transaction_date"), r.money("amount_received", false)),
                RentTransaction::id);
        Map<String, Line<Expense>> expenses = parseRows(tables, DatasetFile.EXPENSES, issues, rowCounts,
                r -> new Expense(r.text("expense_id"), r.text("property_id"), r.date("expense_date"),
                        r.enumValue("expense_category", ExpenseCategory.class), r.money("amount", false)),
                Expense::id);

        boolean haveProperties = tables.containsKey(DatasetFile.PROPERTIES);
        boolean haveTenants = tables.containsKey(DatasetFile.TENANTS);
        boolean haveLeases = tables.containsKey(DatasetFile.LEASES);
        boolean haveObligations = tables.containsKey(DatasetFile.RENT_OBLIGATIONS);

        checkLeases(leases, properties, tenants, haveProperties, haveTenants, issues);
        checkOccupancy(leases, properties, issues);
        checkObligations(obligations, leases, haveLeases, issues);
        Map<String, List<RentTransaction>> paymentsByObligation =
                checkTransactions(transactions, obligations, leases, haveObligations, issues);
        reconcile(obligations, paymentsByObligation, issues);
        checkExpenses(expenses, properties, haveProperties, issues);

        List<FileSummary> summaries = new ArrayList<>();
        for (DatasetFile file : DatasetFile.values()) {
            int[] counts = rowCounts.getOrDefault(file, new int[2]);
            summaries.add(new FileSummary(file.fileName(), tables.containsKey(file), counts[0], counts[1],
                    issues.errorCount(file.fileName()), issues.warningCount(file.fileName())));
        }

        if (issues.errorCount() > 0) {
            return new LoadResult(issues.report(summaries), Optional.empty());
        }
        List<Property> propertyList = values(properties);
        List<RentObligation> obligationList = values(obligations);
        List<RentTransaction> transactionList = values(transactions);
        List<Expense> expenseList = values(expenses);
        LocalDate[] horizon = horizon(obligationList, transactionList, expenseList);
        PortfolioDataset dataset = new PortfolioDataset(propertyList, values(tenants), values(leases),
                obligationList, transactionList, expenseList, horizon[0], horizon[1]);
        return new LoadResult(issues.report(summaries), Optional.of(dataset));
    }

    // ------------------------------------------------------------------ stage 1: files

    private Optional<CsvTable> readTable(DatasetSource source, DatasetFile file, IssueCollector issues)
            throws IOException {
        String name = file.fileName();
        Optional<Reader> reader = source.open(name);
        if (reader.isEmpty()) {
            issues.error(Rule.MISSING_FILE, name, null, null, null, name + " was not supplied.");
            return Optional.empty();
        }
        CsvTable table;
        try (Reader r = reader.get()) {
            table = CsvParser.parse(name, r);
        } catch (CsvFormatException e) {
            issues.error(Rule.MALFORMED_CSV, name, e.lineNumber(), null, null, e.getMessage());
            return Optional.empty();
        }
        if (table.header().isEmpty()) {
            issues.error(Rule.EMPTY_FILE, name, null, null, null, name + " is empty; it needs a header row and data.");
            return Optional.empty();
        }
        boolean missingColumn = false;
        for (String column : file.columns()) {
            if (table.columnIndex(column) < 0) {
                missingColumn = true;
                issues.error(Rule.MISSING_COLUMN, name, 1, column, null,
                        name + " has no '" + column + "' column. Required columns: " + String.join(", ", file.columns()) + ".");
            }
        }
        for (String column : table.header()) {
            if (file.columns().stream().noneMatch(c -> c.equalsIgnoreCase(column))) {
                issues.warning(Rule.UNKNOWN_COLUMN, name, 1, column, null,
                        "Column '" + column + "' in " + name + " is not used and will be ignored.");
            }
        }
        if (missingColumn) {
            return Optional.empty();
        }
        if (table.rows().isEmpty()) {
            String message = name + " has a header but no data rows.";
            if (file.dataRequired()) {
                issues.error(Rule.EMPTY_FILE, name, null, null, null, message);
            } else {
                issues.warning(Rule.EMPTY_FILE, name, null, null, null, message);
            }
        }
        return Optional.of(table);
    }

    // ------------------------------------------------------------------ stage 2: rows

    /** A parsed record together with the line it came from, for error messages. */
    private record Line<R>(int line, R record) {
    }

    private <R> Map<String, Line<R>> parseRows(Map<DatasetFile, CsvTable> tables, DatasetFile file,
                                               IssueCollector issues, Map<DatasetFile, int[]> rowCounts,
                                               Function<RowReader, R> mapper, Function<R, String> id) {
        Map<String, Line<R>> byId = new LinkedHashMap<>();
        CsvTable table = tables.get(file);
        if (table == null) {
            return byId;
        }
        int accepted = 0;
        for (CsvTable.Row row : table.rows()) {
            if (row.fields().size() != table.header().size()) {
                issues.error(Rule.MALFORMED_ROW, file.fileName(), row.lineNumber(), null, null,
                        "Line " + row.lineNumber() + " has " + row.fields().size() + " values but the header has "
                                + table.header().size() + " columns.");
                continue;
            }
            RowReader reader = new RowReader(table, row, issues);
            R record = mapper.apply(reader);
            if (!reader.ok()) {
                continue;
            }
            String key = id.apply(record);
            Line<R> previous = byId.get(key);
            if (previous != null) {
                issues.error(Rule.DUPLICATE_ID, file.fileName(), row.lineNumber(), file.columns().get(0), key,
                        "'" + key + "' is already used on line " + previous.line() + " of " + file.fileName() + ".");
                continue;
            }
            byId.put(key, new Line<>(row.lineNumber(), record));
            accepted++;
        }
        rowCounts.put(file, new int[] {table.rows().size(), accepted});
        return byId;
    }

    // ------------------------------------------------------------------ stage 3: cross-file checks

    private void checkLeases(Map<String, Line<Lease>> leases, Map<String, Line<Property>> properties,
                             Map<String, Line<Tenant>> tenants, boolean haveProperties, boolean haveTenants,
                             IssueCollector issues) {
        for (Line<Lease> entry : leases.values()) {
            Lease lease = entry.record();
            if (haveProperties && !properties.containsKey(lease.propertyId())) {
                issues.error(Rule.UNKNOWN_REFERENCE, L, entry.line(), "property_id", lease.propertyId(),
                        "Lease " + lease.id() + " refers to property " + lease.propertyId() + ", which is not in " + P + ".");
            }
            if (haveTenants && !tenants.containsKey(lease.tenantId())) {
                issues.error(Rule.UNKNOWN_REFERENCE, L, entry.line(), "tenant_id", lease.tenantId(),
                        "Lease " + lease.id() + " refers to tenant " + lease.tenantId() + ", which is not in " + T + ".");
            }
            if (lease.endDate().isBefore(lease.startDate())) {
                issues.error(Rule.INVALID_LEASE_PERIOD, L, entry.line(), "end_date", lease.endDate().toString(),
                        "Lease " + lease.id() + " ends on " + lease.endDate() + ", before it starts on " + lease.startDate() + ".");
            }
            Line<Property> property = properties.get(lease.propertyId());
            if (property != null && lease.leasedAreaSqft() > property.record().rentableAreaSqft()) {
                issues.error(Rule.LEASED_AREA_EXCEEDS_RENTABLE, L, entry.line(), "leased_area_sqft",
                        Long.toString(lease.leasedAreaSqft()),
                        "Lease " + lease.id() + " covers " + lease.leasedAreaSqft() + " sq ft but property "
                                + lease.propertyId() + " has only " + property.record().rentableAreaSqft() + " sq ft.");
            }
        }
    }

    /**
     * Sweep line per property: +area on each lease's start date, −area on the day after it ends.
     * Processing removals before additions on the same day lets one lease end the day before the
     * next begins. If the running total ever exceeds the rentable area, the leases overlap too much.
     */
    private void checkOccupancy(Map<String, Line<Lease>> leases, Map<String, Line<Property>> properties,
                                IssueCollector issues) {
        record Event(LocalDate date, long delta, String leaseId, int line) {
        }
        Map<String, List<Event>> eventsByProperty = new HashMap<>();
        for (Line<Lease> entry : leases.values()) {
            Lease lease = entry.record();
            if (lease.endDate().isBefore(lease.startDate()) || !properties.containsKey(lease.propertyId())) {
                continue; // already reported
            }
            List<Event> events = eventsByProperty.computeIfAbsent(lease.propertyId(), k -> new ArrayList<>());
            events.add(new Event(lease.startDate(), lease.leasedAreaSqft(), lease.id(), entry.line()));
            events.add(new Event(lease.endDate().plusDays(1), -lease.leasedAreaSqft(), lease.id(), entry.line()));
        }
        for (Map.Entry<String, List<Event>> e : eventsByProperty.entrySet()) {
            long capacity = properties.get(e.getKey()).record().rentableAreaSqft();
            List<Event> events = e.getValue();
            events.sort(Comparator.comparing(Event::date).thenComparingLong(Event::delta));
            long occupied = 0;
            for (Event event : events) {
                occupied += event.delta();
                if (occupied > capacity) {
                    issues.error(Rule.OCCUPANCY_EXCEEDS_RENTABLE, L, event.line(), "leased_area_sqft",
                            Long.toString(occupied),
                            "On " + event.date() + " leases on property " + e.getKey() + " (including " + event.leaseId()
                                    + ") cover " + occupied + " sq ft, more than its " + capacity + " sq ft.");
                    break; // one report per property is enough to locate the problem
                }
            }
        }
    }

    private void checkObligations(Map<String, Line<RentObligation>> obligations, Map<String, Line<Lease>> leases,
                                  boolean haveLeases, IssueCollector issues) {
        for (Line<RentObligation> entry : obligations.values()) {
            RentObligation o = entry.record();
            Line<Lease> lease = leases.get(o.leaseId());
            if (lease == null) {
                if (haveLeases) {
                    issues.error(Rule.UNKNOWN_REFERENCE, O, entry.line(), "lease_id", o.leaseId(),
                            "Obligation " + o.id() + " refers to lease " + o.leaseId() + ", which is not in " + L + ".");
                }
                continue;
            }
            if (!lease.record().isActiveOn(o.dueDate())) {
                issues.warning(Rule.OBLIGATION_OUTSIDE_LEASE, O, entry.line(), "due_date", o.dueDate().toString(),
                        "Obligation " + o.id() + " is due on " + o.dueDate() + ", outside lease " + o.leaseId()
                                + " (" + lease.record().startDate() + " to " + lease.record().endDate() + ").");
            }
        }
    }

    private Map<String, List<RentTransaction>> checkTransactions(Map<String, Line<RentTransaction>> transactions,
                                                                 Map<String, Line<RentObligation>> obligations,
                                                                 Map<String, Line<Lease>> leases,
                                                                 boolean haveObligations, IssueCollector issues) {
        Map<String, List<RentTransaction>> byObligation = new HashMap<>();
        for (Line<RentTransaction> entry : transactions.values()) {
            RentTransaction t = entry.record();
            Line<RentObligation> obligation = obligations.get(t.obligationId());
            if (obligation == null) {
                if (haveObligations) {
                    issues.error(Rule.UNKNOWN_REFERENCE, X, entry.line(), "obligation_id", t.obligationId(),
                            "Transaction " + t.id() + " refers to obligation " + t.obligationId() + ", which is not in " + O + ".");
                }
                continue;
            }
            byObligation.computeIfAbsent(t.obligationId(), k -> new ArrayList<>()).add(t);
            Line<Lease> lease = leases.get(obligation.record().leaseId());
            if (lease != null && t.transactionDate().isBefore(lease.record().startDate())) {
                issues.warning(Rule.PAYMENT_BEFORE_LEASE_START, X, entry.line(), "transaction_date",
                        t.transactionDate().toString(),
                        "Transaction " + t.id() + " on " + t.transactionDate() + " is before lease "
                                + lease.record().id() + " starts (" + lease.record().startDate() + ").");
            }
        }
        return byObligation;
    }

    /**
     * Recomputes amount paid, settled date and status from the transactions and compares them with
     * the declared values. The settled date is the date of the payment that brought the cumulative
     * total up to the amount due (payments processed by date, then id).
     */
    private void reconcile(Map<String, Line<RentObligation>> obligations,
                           Map<String, List<RentTransaction>> paymentsByObligation, IssueCollector issues) {
        for (Line<RentObligation> entry : obligations.values()) {
            RentObligation o = entry.record();
            List<RentTransaction> payments = new ArrayList<>(paymentsByObligation.getOrDefault(o.id(), List.of()));
            payments.sort(Comparator.comparing(RentTransaction::transactionDate).thenComparing(RentTransaction::id));
            long paid = 0;
            LocalDate settled = null;
            for (RentTransaction t : payments) {
                paid = Math.addExact(paid, t.amountPaise());
                if (settled == null && paid >= o.dueAmountPaise()) {
                    settled = t.transactionDate();
                }
            }
            if (paid > o.dueAmountPaise()) {
                issues.error(Rule.OVERPAYMENT, X, null, "amount_received", rupees(paid),
                        "Payments for obligation " + o.id() + " total ₹" + rupees(paid) + ", more than the ₹"
                                + rupees(o.dueAmountPaise()) + " due.");
                continue;
            }
            ObligationStatus status = ObligationStatus.derive(paid, o.dueAmountPaise());
            if (paid != o.amountPaidPaise()) {
                mismatch(issues, entry, "amount_paid", rupees(o.amountPaidPaise()),
                        "declares amount_paid ₹" + rupees(o.amountPaidPaise()) + " but its transactions total ₹" + rupees(paid));
            }
            if (!Objects.equals(settled, o.settledDate())) {
                mismatch(issues, entry, "settled_date", String.valueOf(o.settledDate()),
                        "declares settled_date " + (o.settledDate() == null ? "(blank)" : o.settledDate())
                                + " but its transactions give " + (settled == null ? "(not settled)" : settled));
            }
            if (status != o.status()) {
                mismatch(issues, entry, "status", String.valueOf(o.status()),
                        "declares status " + o.status() + " but its transactions give " + status);
            }
        }
    }

    private static void mismatch(IssueCollector issues, Line<RentObligation> entry, String column, String value,
                                 String detail) {
        issues.error(Rule.RECONCILIATION_MISMATCH, O, entry.line(), column, value,
                "Obligation " + entry.record().id() + " " + detail + ".");
    }

    private void checkExpenses(Map<String, Line<Expense>> expenses, Map<String, Line<Property>> properties,
                               boolean haveProperties, IssueCollector issues) {
        if (!haveProperties) {
            return;
        }
        for (Line<Expense> entry : expenses.values()) {
            Expense e = entry.record();
            if (!properties.containsKey(e.propertyId())) {
                issues.error(Rule.UNKNOWN_REFERENCE, E, entry.line(), "property_id", e.propertyId(),
                        "Expense " + e.id() + " refers to property " + e.propertyId() + ", which is not in " + P + ".");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static <R> List<R> values(Map<String, Line<R>> map) {
        return map.values().stream().map(Line::record).toList();
    }

    /** First day of the earliest record's month to the last day of the latest record's month. */
    private static LocalDate[] horizon(List<RentObligation> obligations, List<RentTransaction> transactions,
                                       List<Expense> expenses) {
        LocalDate min = LocalDate.MAX;
        LocalDate max = LocalDate.MIN;
        for (RentObligation o : obligations) {
            min = earlier(min, o.dueDate());
            max = later(max, o.dueDate());
        }
        for (RentTransaction t : transactions) {
            min = earlier(min, t.transactionDate());
            max = later(max, t.transactionDate());
        }
        for (Expense e : expenses) {
            min = earlier(min, e.expenseDate());
            max = later(max, e.expenseDate());
        }
        return new LocalDate[] {YearMonth.from(min).atDay(1), YearMonth.from(max).atEndOfMonth()};
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return b.isBefore(a) ? b : a;
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return b.isAfter(a) ? b : a;
    }

    private static String rupees(long paise) {
        return java.math.BigDecimal.valueOf(paise, 2).toPlainString();
    }
}
