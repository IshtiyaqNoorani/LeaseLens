package com.leaselens.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Builds one invalid dataset per validation rule by copying the golden dataset and introducing a
 * single, documented defect. Each fixture folder contains an {@code EXPECTED.txt} whose first line
 * is the rule code the loader must report. These folders are used by the automated tests and in
 * the live demo of error handling.
 */
final class InvalidFixtureWriter {

    private static final String[] FILES = {"properties.csv", "tenants.csv", "leases.csv", "rent_obligations.csv",
            "rent_transactions.csv", "expenses.csv"};

    /** Mutable in-memory copy of the golden CSVs: file name -> rows (row 0 is the header). */
    private static final class Files6 {
        final Map<String, List<List<String>>> tables = new LinkedHashMap<>();

        void set(String file, String id, String column, String value) {
            List<List<String>> rows = tables.get(file);
            int col = rows.get(0).indexOf(column);
            for (List<String> row : rows.subList(1, rows.size())) {
                if (row.get(0).equals(id)) {
                    row.set(col, value);
                    return;
                }
            }
            throw new IllegalStateException(file + " has no row " + id);
        }

        void dropColumn(String file, String column) {
            List<List<String>> rows = tables.get(file);
            int col = rows.get(0).indexOf(column);
            rows.forEach(row -> row.remove(col));
        }

        void addRow(String file, String... values) {
            tables.get(file).add(new ArrayList<>(Arrays.asList(values)));
        }
    }

    private record Fixture(String name, String rule, String description, Consumer<Files6> mutation) {
    }

    private static List<Fixture> fixtures() {
        return List.of(
            new Fixture("01-missing-file", "MISSING_FILE", "expenses.csv is not supplied.",
                f -> f.tables.remove("expenses.csv")),
            new Fixture("02-empty-file", "EMPTY_FILE", "properties.csv is completely empty.",
                f -> f.tables.get("properties.csv").clear()),
            new Fixture("03-missing-column", "MISSING_COLUMN", "leases.csv has no monthly_rent column.",
                f -> f.dropColumn("leases.csv", "monthly_rent")),
            new Fixture("04-duplicate-property-id", "DUPLICATE_ID", "P001 appears twice in properties.csv.",
                f -> f.addRow("properties.csv", "P001", "Golden Annexe", "OFFICE", "Zone C", "1500", "5000000.00", "1")),
            new Fixture("05-duplicate-lease-id", "DUPLICATE_ID", "Lease L003 is renamed L002, duplicating an id.",
                f -> f.set("leases.csv", "L003", "lease_id", "L002")),
            new Fixture("06-duplicate-transaction-id", "DUPLICATE_ID", "Transaction TX008 is renamed TX007.",
                f -> f.set("rent_transactions.csv", "TX008", "transaction_id", "TX007")),
            new Fixture("07-unknown-property-reference", "UNKNOWN_REFERENCE", "Lease L002 refers to property P999.",
                f -> f.set("leases.csv", "L002", "property_id", "P999")),
            new Fixture("08-unknown-lease-reference", "UNKNOWN_REFERENCE", "Obligation O008 refers to lease L999.",
                f -> f.set("rent_obligations.csv", "O008", "lease_id", "L999")),
            new Fixture("09-unknown-obligation-reference", "UNKNOWN_REFERENCE",
                "Transaction TX008 refers to obligation O999.",
                f -> f.set("rent_transactions.csv", "TX008", "obligation_id", "O999")),
            new Fixture("10-invalid-date", "INVALID_DATE", "Obligation O002 is due on 2026-02-30.",
                f -> f.set("rent_obligations.csv", "O002", "due_date", "2026-02-30")),
            new Fixture("11-invalid-lease-period", "INVALID_LEASE_PERIOD", "Lease L003 ends before it starts.",
                f -> f.set("leases.csv", "L003", "end_date", "2025-01-31")),
            new Fixture("12-negative-rent", "INVALID_AMOUNT", "Lease L001 has a monthly rent of -25000.00.",
                f -> f.set("leases.csv", "L001", "monthly_rent", "-25000.00")),
            new Fixture("13-negative-expense", "INVALID_AMOUNT", "Expense E003 has an amount of -12000.00.",
                f -> f.set("expenses.csv", "E003", "amount", "-12000.00")),
            new Fixture("14-invalid-transaction-amount", "INVALID_NUMBER", "Transaction TX005 has amount 'abc'.",
                f -> f.set("rent_transactions.csv", "TX005", "amount_received", "abc")),
            new Fixture("15-overpayment", "OVERPAYMENT",
                "Transaction TX008 pays 35000.00 against a 30000.00 obligation.",
                f -> f.set("rent_transactions.csv", "TX008", "amount_received", "35000.00")),
            new Fixture("16-reconciliation-mismatch", "RECONCILIATION_MISMATCH",
                "Obligation O005 declares amount_paid 100000.00 but its transactions total 120000.00.",
                f -> f.set("rent_obligations.csv", "O005", "amount_paid", "100000.00")),
            new Fixture("17-missing-value", "MISSING_VALUE", "Property P001 has a blank rentable_area_sqft.",
                f -> f.set("properties.csv", "P001", "rentable_area_sqft", "")),
            new Fixture("18-leased-area-exceeds-rentable", "LEASED_AREA_EXCEEDS_RENTABLE",
                "Lease L002 covers 5000 sq ft of a 3000 sq ft property.",
                f -> f.set("leases.csv", "L002", "leased_area_sqft", "5000")),
            new Fixture("19-overlapping-occupancy", "OCCUPANCY_EXCEEDS_RENTABLE",
                "From 2026-02-01, L001 (1000) and L003 (1500) together exceed P001's 2000 sq ft.",
                f -> f.set("leases.csv", "L003", "leased_area_sqft", "1500")),
            new Fixture("20-invalid-category", "INVALID_VALUE", "Expense E005 has category MISC.",
                f -> f.set("expenses.csv", "E005", "expense_category", "MISC")),
            new Fixture("21-malformed-row", "MALFORMED_ROW", "A tenants.csv row has an extra value.",
                f -> f.addRow("tenants.csv", "T004", "SME", "extra")),
            new Fixture("22-invalid-billing-day", "INVALID_VALUE", "Lease L001 has billing day 31 (allowed 1-28).",
                f -> f.set("leases.csv", "L001", "billing_day", "31")),
            new Fixture("23-no-data-rows", "EMPTY_FILE", "rent_obligations.csv has a header but no rows.",
                f -> {
                    List<List<String>> rows = f.tables.get("rent_obligations.csv");
                    rows.subList(1, rows.size()).clear();
                })
        );
    }

    /** Writes every fixture under {@code outDir}, replacing earlier copies. Returns the count. */
    static int writeAll(Path goldenDir, Path outDir) throws IOException {
        if (Files.isDirectory(outDir)) {
            try (Stream<Path> walk = Files.walk(outDir)) {
                for (Path p : walk.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.delete(p);
                }
            }
        }
        List<Fixture> fixtures = fixtures();
        StringBuilder index = new StringBuilder("""
                # Invalid-data fixtures

                Each folder is the golden dataset with exactly one defect. The loader must reject it and report the rule shown.
                Regenerate with `java -jar leaselens-generator/target/leaselens-generator.jar fixtures`.

                | Folder | Expected rule | Defect |
                |---|---|---|
                """);
        for (Fixture fixture : fixtures) {
            Files6 files = readGolden(goldenDir);
            fixture.mutation().accept(files);
            Path dir = outDir.resolve(fixture.name());
            Files.createDirectories(dir);
            for (Map.Entry<String, List<List<String>>> e : files.tables.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (List<String> row : e.getValue()) {
                    sb.append(String.join(",", row)).append('\n');
                }
                Files.writeString(dir.resolve(e.getKey()), sb.toString(), StandardCharsets.UTF_8);
            }
            Files.writeString(dir.resolve("EXPECTED.txt"), fixture.rule() + "\n" + fixture.description() + "\n",
                    StandardCharsets.UTF_8);
            index.append("| `").append(fixture.name()).append("` | `").append(fixture.rule()).append("` | ")
                    .append(fixture.description()).append(" |\n");
        }
        Files.writeString(outDir.resolve("README.md"), index.toString(), StandardCharsets.UTF_8);
        return fixtures.size();
    }

    private static Files6 readGolden(Path goldenDir) throws IOException {
        Files6 files = new Files6();
        for (String name : FILES) {
            List<List<String>> rows = new ArrayList<>();
            for (String line : Files.readAllLines(goldenDir.resolve(name), StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    rows.add(new ArrayList<>(Arrays.asList(line.split(",", -1))));
                }
            }
            files.tables.put(name, rows);
        }
        return files;
    }
}
