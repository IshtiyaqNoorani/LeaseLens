package com.leaselens.data.load;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.leaselens.data.csv.CsvTable;
import com.leaselens.data.validation.IssueCollector;
import com.leaselens.data.validation.Rule;

/**
 * Typed access to the fields of one CSV row. Each accessor records a validation issue and returns
 * a placeholder when the value is unusable; {@link #ok()} then reports false and the caller
 * discards the row. Missing values are never silently replaced by zero.
 */
final class RowReader {

    private final CsvTable table;
    private final CsvTable.Row row;
    private final IssueCollector issues;
    private boolean ok = true;

    RowReader(CsvTable table, CsvTable.Row row, IssueCollector issues) {
        this.table = table;
        this.row = row;
        this.issues = issues;
    }

    /** True if every field read so far was valid. */
    boolean ok() {
        return ok;
    }

    int line() {
        return row.lineNumber();
    }

    /** Raw trimmed value, or "" if blank. */
    String raw(String column) {
        int index = table.columnIndex(column);
        return index < 0 || index >= row.fields().size() ? "" : row.fields().get(index).trim();
    }

    /** Required non-blank text. */
    String text(String column) {
        String value = raw(column);
        if (value.isEmpty()) {
            fail(Rule.MISSING_VALUE, column, value, "'" + column + "' is blank; a value is required.");
        }
        return value;
    }

    /** Required ISO date (YYYY-MM-DD), checked strictly (e.g. 2026-02-30 is rejected). */
    LocalDate date(String column) {
        String value = text(column);
        return value.isEmpty() ? null : parseDate(column, value);
    }

    /** Optional ISO date: blank gives null. */
    LocalDate optionalDate(String column) {
        String value = raw(column);
        return value.isEmpty() ? null : parseDate(column, value);
    }

    private LocalDate parseDate(String column, String value) {
        try {
            return LocalDate.parse(value); // ISO_LOCAL_DATE uses the STRICT resolver
        } catch (DateTimeParseException e) {
            fail(Rule.INVALID_DATE, column, value,
                    "'" + value + "' in " + column + " is not a valid date; use YYYY-MM-DD.");
            return null;
        }
    }

    /**
     * Rupee amount with up to two decimals, returned in paise.
     *
     * @param allowZero true if zero is valid (e.g. amount paid); negative values are always rejected
     */
    long money(String column, boolean allowZero) {
        String value = text(column);
        if (value.isEmpty()) {
            return 0;
        }
        BigDecimal rupees;
        try {
            rupees = new BigDecimal(value);
        } catch (NumberFormatException e) {
            fail(Rule.INVALID_NUMBER, column, value, "'" + value + "' in " + column + " is not a number.");
            return 0;
        }
        if (rupees.stripTrailingZeros().scale() > 2) {
            fail(Rule.INVALID_NUMBER, column, value,
                    "'" + value + "' in " + column + " has more than two decimal places.");
            return 0;
        }
        long paise;
        try {
            paise = rupees.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            fail(Rule.INVALID_NUMBER, column, value, "'" + value + "' in " + column + " is too large.");
            return 0;
        }
        if (paise < 0 || (paise == 0 && !allowZero)) {
            fail(Rule.INVALID_AMOUNT, column, value, column + " must be "
                    + (allowZero ? "zero or more" : "greater than zero") + "; found " + value + ".");
        }
        return paise;
    }

    /** Whole number within {@code [min, max]}. */
    long wholeNumber(String column, long min, long max) {
        String value = text(column);
        if (value.isEmpty()) {
            return min;
        }
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException e) {
            fail(Rule.INVALID_NUMBER, column, value, "'" + value + "' in " + column + " is not a whole number.");
            return min;
        }
        if (parsed < min || parsed > max) {
            Rule rule = min > 0 && parsed <= 0 ? Rule.INVALID_AMOUNT : Rule.INVALID_VALUE;
            fail(rule, column, value, column + " must be between " + min + " and " + max + "; found " + value + ".");
        }
        return parsed;
    }

    /** One of the constants of {@code type}; matching ignores case. */
    <E extends Enum<E>> E enumValue(String column, Class<E> type) {
        String value = text(column);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            StringBuilder allowed = new StringBuilder();
            for (E constant : type.getEnumConstants()) {
                allowed.append(allowed.isEmpty() ? "" : ", ").append(constant.name());
            }
            fail(Rule.INVALID_VALUE, column, value,
                    "'" + value + "' is not a valid " + column + "; allowed values: " + allowed + ".");
            return null;
        }
    }

    private void fail(Rule rule, String column, String value, String message) {
        ok = false;
        issues.error(rule, table.fileName(), row.lineNumber(), column, value, message);
    }
}
