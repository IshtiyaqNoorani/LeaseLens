package com.leaselens.generator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.leaselens.data.load.DatasetFile;
import com.leaselens.data.load.DatasetLoader;
import com.leaselens.data.load.DatasetSource;
import com.leaselens.data.load.LoadResult;
import com.leaselens.data.model.Expense;
import com.leaselens.data.model.ExpenseCategory;
import com.leaselens.data.model.Lease;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.PropertyType;
import com.leaselens.data.model.RentObligation;

class PortfolioGeneratorTest {

    @TempDir
    static Path temp;

    private static Path defaultRun;
    private static LoadResult loaded;
    private static PortfolioGenerator.Summary summary;

    @BeforeAll
    static void generateDefault() throws Exception {
        defaultRun = temp.resolve("default");
        summary = new PortfolioGenerator(GeneratorConfig.defaults()).writeTo(defaultRun);
        loaded = new DatasetLoader().load(DatasetSource.directory(defaultRun));
    }

    private static Path repoData(String child) {
        String configured = System.getProperty("leaselens.dataDir");
        return (configured != null ? Path.of(configured) : Path.of("..", "data")).resolve(child).normalize();
    }

    @Test
    @DisplayName("Same seed gives byte-identical files")
    void sameSeedSameBytes() throws Exception {
        Path again = temp.resolve("again");
        new PortfolioGenerator(GeneratorConfig.defaults()).writeTo(again);
        for (DatasetFile f : DatasetFile.values()) {
            assertArrayEquals(Files.readAllBytes(defaultRun.resolve(f.fileName())),
                    Files.readAllBytes(again.resolve(f.fileName())), f.fileName());
        }
    }

    @Test
    @DisplayName("The committed data/sample is exactly what the default seed produces")
    void committedSampleIsReproducible() throws Exception {
        for (DatasetFile f : DatasetFile.values()) {
            assertArrayEquals(Files.readAllBytes(defaultRun.resolve(f.fileName())),
                    Files.readAllBytes(repoData("sample").resolve(f.fileName())),
                    f.fileName() + " differs: regenerate with the generator 'sample' command");
        }
    }

    @Test
    @DisplayName("A different seed gives different data")
    void differentSeedDifferentData() throws Exception {
        Path other = temp.resolve("other");
        GeneratorConfig d = GeneratorConfig.defaults();
        new PortfolioGenerator(new GeneratorConfig(d.seed() + 1, d.start(), d.months(), d.propertyCount())).writeTo(other);
        assertFalse(Arrays.equals(Files.readAllBytes(defaultRun.resolve("rent_transactions.csv")),
                Files.readAllBytes(other.resolve("rent_transactions.csv"))));
    }

    @Test
    @DisplayName("Generated data passes every validation rule with no errors and no warnings")
    void passesValidation() {
        assertTrue(loaded.report().valid(), () -> loaded.report().issues().toString());
        assertEquals(0, loaded.report().warningCount(), () -> loaded.report().issues().toString());
    }

    @Test
    @DisplayName("Scale and horizon match the specification")
    void scaleAndHorizon() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        assertEquals(50, d.properties().size());
        assertTrue(d.leases().size() >= 150 && d.leases().size() <= 260, "leases: " + d.leases().size());
        assertEquals(LocalDate.of(2023, 11, 1), d.horizonStart());
        assertEquals(LocalDate.of(2026, 10, 31), d.horizonEnd());
        assertEquals(1_096, d.horizonDays());
        long open = d.obligations().stream().filter(o -> !o.isSettled()).count();
        long partial = d.obligations().stream().filter(o -> o.status().name().equals("PARTIAL")).count();
        assertTrue(open > 0 && partial > 0, "needs a mix of paid, partial and unpaid obligations");
    }

    // ------------------------------------------------------------------ planted patterns (GENERATOR_SPEC.md)

    @Test
    @DisplayName("Planted 1: retail payments are later on average than residential payments")
    void retailHeavierTail() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        double retail = meanDaysLate(d, PropertyType.RETAIL);
        double residential = meanDaysLate(d, PropertyType.RESIDENTIAL);
        assertTrue(retail > residential * 1.5, "retail " + retail + " vs residential " + residential);
    }

    @Test
    @DisplayName("Planted 2: maintenance and repair spending is higher per month in the monsoon")
    void monsoonMaintenance() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        Map<Boolean, long[]> totals = new HashMap<>(); // monsoon? -> {paise, months}
        for (Expense e : d.expenses()) {
            if (e.category() == ExpenseCategory.MAINTENANCE || e.category() == ExpenseCategory.REPAIRS) {
                int m = e.expenseDate().getMonthValue();
                totals.computeIfAbsent(m >= 6 && m <= 9, k -> new long[1])[0] += e.amountPaise();
            }
        }
        double monsoonPerMonth = totals.get(true)[0] / 12.0;  // 3 years x 4 monsoon months
        double otherPerMonth = totals.get(false)[0] / 24.0;   // 3 years x 8 other months
        assertTrue(monsoonPerMonth > otherPerMonth * 1.5, monsoonPerMonth + " vs " + otherPerMonth);
    }

    @Test
    @DisplayName("Planted 3: two offices are completely vacant for at least 8 months")
    void vacancyShock() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        LocalDate probe = YearMonth.of(2025, 1).atDay(15); // inside the shock window
        long emptyOffices = d.properties().stream()
                .filter(p -> p.type() == PropertyType.OFFICE)
                .filter(p -> d.leases().stream().noneMatch(l -> l.propertyId().equals(p.id()) && l.isActiveOn(probe)))
                .filter(p -> d.leases().stream().filter(l -> l.propertyId().equals(p.id()))
                        .noneMatch(l -> l.startDate().isBefore(LocalDate.of(2025, 5, 1))
                                && l.endDate().isAfter(LocalDate.of(2024, 8, 31))))
                .count();
        assertTrue(emptyOffices >= 2, "offices empty for the whole window: " + emptyOffices);
    }

    @Test
    @DisplayName("Planted 4: a few repair records are far above the typical repair")
    void outsizedRepairs() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        long[] repairs = d.expenses().stream().filter(e -> e.category() == ExpenseCategory.REPAIRS)
                .mapToLong(Expense::amountPaise).sorted().toArray();
        long median = repairs[repairs.length / 2];
        long outsized = Arrays.stream(repairs).filter(a -> a > 8 * median).count();
        assertTrue(outsized >= 1 && outsized < repairs.length * 0.05, "outsized repairs: " + outsized);
    }

    @Test
    @DisplayName("Planted 5: one office's collection efficiency falls in the second half of the horizon")
    void degradingCollections() {
        PortfolioDataset d = loaded.dataset().orElseThrow();
        String property = summary.degradingProperty();
        assertEquals(PropertyType.OFFICE, d.property(property).type());
        LocalDate cutoff = YearMonth.of(2025, 5).atDay(1); // month 18 of the horizon
        long[] early = new long[2];
        long[] late = new long[2];
        for (RentObligation o : d.obligations()) {
            if (d.lease(o.leaseId()).propertyId().equals(property)) {
                long[] bucket = o.dueDate().isBefore(cutoff) ? early : late;
                bucket[0] += o.dueAmountPaise();
                bucket[1] += o.amountPaidPaise();
            }
        }
        double before = 100.0 * early[1] / early[0];
        double after = 100.0 * late[1] / late[0];
        assertTrue(after < before - 5, property + ": " + before + "% -> " + after + "%");
    }

    @Test
    @DisplayName("Ground truth names two shock offices and a degrading office that is not one of them")
    void groundTruthSummary() {
        assertEquals(2, summary.vacancyShockProperties().size());
        assertFalse(summary.vacancyShockProperties().contains(summary.degradingProperty()));
    }

    private static double meanDaysLate(PortfolioDataset d, PropertyType type) {
        Map<String, PropertyType> leaseType = new HashMap<>();
        for (Lease l : d.leases()) {
            leaseType.put(l.id(), d.property(l.propertyId()).type());
        }
        return d.obligations().stream()
                .filter(RentObligation::isSettled)
                .filter(o -> leaseType.get(o.leaseId()) == type)
                .mapToLong(o -> Math.max(0, ChronoUnit.DAYS.between(o.dueDate(), o.settledDate())))
                .average().orElseThrow();
    }
}
