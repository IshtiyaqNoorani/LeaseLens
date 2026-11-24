package com.leaselens.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seeded synthetic portfolio generator. The same {@link GeneratorConfig} always produces
 * byte-identical CSV files.
 *
 * <p>All modelling assumptions and planted patterns are documented in {@code data/GENERATOR_SPEC.md}.
 * The data is synthetic: no real people, properties or financial records are represented.
 */
public final class PortfolioGenerator {

    // ------------------------------------------------------------------ static assumptions

    enum Type { RESIDENTIAL, OFFICE, RETAIL, INDUSTRIAL }

    enum Behaviour { PUNCTUAL, OCCASIONAL, CHRONIC, DEFAULTER }

    /** Per-asset-class assumptions (₹ amounts are per month unless stated). */
    private record Profile(int unitsMin, int unitsMax, int unitAreaMin, int unitAreaMax,
                           double rateMin, double rateMax, double yieldMin, double yieldMax,
                           double initialOccupancy, int[] termMonths, double[] termWeights,
                           int vacancyMinDays, int vacancyMaxDays, double renewalChance,
                           double[] behaviourWeights, double securityPerSqft, double cleaningPerSqft,
                           double maintenanceRate, double maintenanceMedian, double repairMedian,
                           String[] suffixes) {
    }

    private static Profile profile(Type type) {
        return switch (type) {
            case RESIDENTIAL -> new Profile(2, 4, 650, 1600, 28, 45, 0.030, 0.045, 0.90,
                    new int[] {11, 23, 35}, new double[] {0.40, 0.35, 0.25}, 15, 75, 0.35,
                    new double[] {0.62, 0.24, 0.10, 0.04}, 1.2, 0.0, 1.0, 3_500, 15_000,
                    new String[] {"Residency", "Heights", "Apartments", "Court", "Enclave"});
            case OFFICE -> new Profile(1, 4, 1500, 6000, 70, 130, 0.070, 0.085, 0.82,
                    new int[] {36, 48, 60}, new double[] {0.5, 0.3, 0.2}, 60, 240, 0.50,
                    new double[] {0.66, 0.22, 0.09, 0.03}, 2.0, 1.5, 1.4, 6_000, 30_000,
                    new String[] {"House", "Tower", "Business Centre", "Corporate Park"});
            // Planted pattern 1: retail tenants have a heavier late-payment mix.
            case RETAIL -> new Profile(1, 3, 800, 3500, 90, 180, 0.075, 0.095, 0.85,
                    new int[] {36, 60}, new double[] {0.6, 0.4}, 45, 180, 0.50,
                    new double[] {0.45, 0.28, 0.20, 0.07}, 2.5, 1.5, 1.2, 5_000, 25_000,
                    new String[] {"Arcade", "Galleria", "Market", "Plaza"});
            case INDUSTRIAL -> new Profile(1, 2, 12_000, 40_000, 18, 32, 0.080, 0.095, 0.90,
                    new int[] {36, 60}, new double[] {0.5, 0.5}, 30, 150, 0.60,
                    new double[] {0.75, 0.17, 0.06, 0.02}, 0.6, 0.0, 0.8, 9_000, 45_000,
                    new String[] {"Logistics Park", "Warehousing Hub", "Industrial Estate"});
        };
    }

    private static final String[] NAME_WORDS = {
        "Cedar", "Meridian", "Banyan", "Silverline", "Lotus", "Harbour", "Orchid", "Granite", "Aurora",
        "Crescent", "Summit", "Maple", "Indigo", "Sapphire", "Willow", "Coral", "Neem", "Saffron", "Teak",
        "Zenith", "Horizon", "Pinnacle", "Tamarind", "Jasmine", "Sandstone", "Kestrel", "Riverbend",
        "Lighthouse", "Ashoka", "Gulmohar", "Emerald", "Falcon", "Monarch", "Palmgrove", "Quartz"
    };

    private record Zone(String name, double rateFactor) {
    }

    private static final Zone[] ZONES = {
        new Zone("Central Business District", 1.25), new Zone("Riverside", 1.10),
        new Zone("East Tech Corridor", 1.05), new Zone("North Corridor", 1.00),
        new Zone("West Suburbs", 0.90), new Zone("South Industrial Belt", 0.85)
    };

    // ------------------------------------------------------------------ generated rows

    private record PropertyRow(String id, String name, Type type, String zone, long area, long acquisitionPaise,
                               int units, long potentialMonthlyRentPaise, boolean vacancyShock, boolean degrading) {
    }

    private record TenantRow(String id, String segment, Behaviour behaviour) {
    }

    private record LeaseRow(String id, String propertyId, String tenantId, LocalDate start, LocalDate end,
                            long rentPaise, long area, int billingDay, String status) {
    }

    private record ObligationRow(String id, String leaseId, LocalDate due, long duePaise, long paidPaise,
                                 LocalDate settled, String status) {
    }

    private record TransactionRow(String obligationId, LocalDate date, long amountPaise) {
    }

    private record ExpenseRow(String propertyId, LocalDate date, String category, long amountPaise) {
    }

    private final GeneratorConfig config;
    private final LocalDate horizonStart;
    private final LocalDate horizonEnd;
    private final LocalDate shockStart;
    private final LocalDate shockEnd;

    private final List<PropertyRow> properties = new ArrayList<>();
    private final List<TenantRow> tenants = new ArrayList<>();
    private final List<LeaseRow> leases = new ArrayList<>();
    private final List<ObligationRow> obligations = new ArrayList<>();
    private final List<TransactionRow> transactions = new ArrayList<>();
    private final List<ExpenseRow> expenses = new ArrayList<>();

    public PortfolioGenerator(GeneratorConfig config) {
        this.config = config;
        this.horizonStart = config.start().atDay(1);
        this.horizonEnd = config.start().plusMonths(config.months() - 1L).atEndOfMonth();
        // Planted pattern 3: two offices lose all tenants for 8-10 months starting 10 months in.
        this.shockStart = config.start().plusMonths(10).atDay(1);
        this.shockEnd = shockStart.plusMonths(8);
    }

    /** Generates the portfolio and writes the six CSV files into {@code outputDir}. */
    public Summary writeTo(Path outputDir) throws IOException {
        generate();
        write(outputDir);
        List<String> shock = properties.stream().filter(PropertyRow::vacancyShock).map(PropertyRow::id).toList();
        String degrading = properties.stream().filter(PropertyRow::degrading).map(PropertyRow::id).findFirst().orElse("");
        return new Summary(properties.size(), tenants.size(), leases.size(), obligations.size(),
                transactions.size(), expenses.size(), horizonStart, horizonEnd, shock, degrading);
    }

    /**
     * Row counts, horizon and the properties carrying planted patterns 3 and 5 (ground truth for
     * checking the analysis; deliberately not written into the CSVs).
     */
    public record Summary(int properties, int tenants, int leases, int obligations, int transactions,
                          int expenses, LocalDate horizonStart, LocalDate horizonEnd,
                          List<String> vacancyShockProperties, String degradingProperty) {
    }

    // ------------------------------------------------------------------ generation

    private void generate() {
        Rng top = Rng.stream(config.seed(), 0);
        List<Type> types = typeMix(config.propertyCount(), top);
        int officeCount = 0;
        int shockOffices = 0;
        boolean degradingAssigned = false;
        Set<String> names = new HashSet<>();

        for (int i = 0; i < types.size(); i++) {
            Type type = types.get(i);
            Rng rng = Rng.stream(config.seed(), 1_000 + i);
            boolean shock = false;
            boolean degrading = false;
            if (type == Type.OFFICE) {
                officeCount++;
                if (shockOffices < 2) {
                    shock = true;
                    shockOffices++;
                } else if (!degradingAssigned && officeCount >= 3) {
                    degrading = true; // Planted pattern 5
                    degradingAssigned = true;
                }
            }
            generateProperty(i, type, shock, degrading, rng, names);
        }
    }

    /** Fixed asset-class mix (40% residential, 28% office, 22% retail, 10% industrial), shuffled. */
    private static List<Type> typeMix(int count, Rng rng) {
        int office = (int) Math.round(count * 0.28);
        int retail = (int) Math.round(count * 0.22);
        int industrial = (int) Math.round(count * 0.10);
        int residential = count - office - retail - industrial;
        List<Type> types = new ArrayList<>();
        for (int i = 0; i < residential; i++) types.add(Type.RESIDENTIAL);
        for (int i = 0; i < office; i++) types.add(Type.OFFICE);
        for (int i = 0; i < retail; i++) types.add(Type.RETAIL);
        for (int i = 0; i < industrial; i++) types.add(Type.INDUSTRIAL);
        for (int i = types.size() - 1; i > 0; i--) { // Fisher–Yates
            int j = rng.between(0, i);
            Type t = types.get(i);
            types.set(i, types.get(j));
            types.set(j, t);
        }
        return types;
    }

    private void generateProperty(int index, Type type, boolean shock, boolean degrading, Rng rng, Set<String> names) {
        Profile p = profile(type);
        String id = String.format(Locale.ROOT, "P%03d", index + 1);
        Zone zone = type == Type.INDUSTRIAL && rng.chance(0.7) ? ZONES[5] : rng.pick(ZONES);
        String name = uniqueName(rng.pick(NAME_WORDS) + " " + rng.pick(p.suffixes()), names);

        int units = rng.between(p.unitsMin(), p.unitsMax());
        int[] unitAreas = new int[units];
        long area = 0;
        for (int u = 0; u < units; u++) {
            unitAreas[u] = roundTo(rng.between(p.unitAreaMin(), p.unitAreaMax()), 10);
            area += unitAreas[u];
        }
        double rate = rng.between(p.rateMin(), p.rateMax()) * zone.rateFactor(); // ₹ per sq ft per month
        long potentialMonthly = Math.round(area * rate) * 100;
        double yield = rng.between(p.yieldMin(), p.yieldMax());
        long acquisitionRupees = roundTo((long) (potentialMonthly / 100.0 * 12 / yield), 100_000);

        PropertyRow property = new PropertyRow(id, name, type, zone.name(), area, acquisitionRupees * 100, units,
                potentialMonthly, shock, degrading);
        properties.add(property);

        for (int u = 0; u < units; u++) {
            generateUnitLeases(property, p, unitAreas[u], rate, rng);
        }
        generateExpenses(property, p, rng);
    }

    private static String uniqueName(String candidate, Set<String> names) {
        String name = candidate;
        int n = 2;
        while (!names.add(name)) {
            name = candidate + " " + toRoman(n++);
        }
        return name;
    }

    private static String toRoman(int n) {
        return switch (n) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V" + (n - 5); };
    }

    // ------------------------------------------------------------------ leases

    private void generateUnitLeases(PropertyRow property, Profile p, int unitArea, double rate, Rng rng) {
        LocalDate start;
        int firstTerm = 0;
        if (rng.chance(p.initialOccupancy())) {
            firstTerm = p.termMonths()[rng.weighted(p.termWeights())];
            start = horizonStart.minusMonths(rng.between(0, firstTerm - 1)); // already mid-lease at horizon start
        } else {
            start = horizonStart.plusDays(rng.between(p.vacancyMinDays(), p.vacancyMaxDays()));
        }
        TenantRow tenant = null;
        while (!start.isAfter(horizonEnd)) {
            if (property.vacancyShock() && !start.isBefore(shockStart) && start.isBefore(shockEnd)) {
                start = shockEnd.plusDays(rng.between(0, 60));
                tenant = null;
                continue;
            }
            // The first lease of an initially occupied unit keeps the term used to place its start,
            // so it is guaranteed to still be running when the horizon begins.
            int term = firstTerm > 0 ? firstTerm : p.termMonths()[rng.weighted(p.termWeights())];
            firstTerm = 0;
            LocalDate end = start.plusMonths(term).minusDays(1);
            if (property.vacancyShock() && start.isBefore(shockStart) && !end.isBefore(shockStart)) {
                end = shockStart.minusDays(1);
            }
            if (tenant == null) {
                tenant = newTenant(property.type(), p, rng);
            }
            String status = end.isBefore(horizonEnd) ? "EXPIRED" : "ACTIVE";

            // Defaulters stop paying after a few instalments; the lease is then terminated early.
            int defaultAfter = Integer.MAX_VALUE;
            if (tenant.behaviour() == Behaviour.DEFAULTER) {
                defaultAfter = rng.between(3, 10);
                LocalDate firstBillable = start.isBefore(horizonStart) ? horizonStart : start;
                LocalDate terminated = firstBillable.plusMonths(defaultAfter + 3L).minusDays(1);
                if (terminated.isBefore(end)) {
                    end = terminated;
                    status = end.isBefore(horizonEnd) ? "TERMINATED" : "ACTIVE";
                }
            }
            if (!end.isBefore(horizonStart)) {
                double escalation = StrictMath.pow(1.05, ChronoUnit.DAYS.between(horizonStart, start) / 365.0);
                long rentRupees = roundTo((long) (unitArea * rate * escalation), 100);
                int billingDay = property.type() == Type.RESIDENTIAL
                        ? (rng.chance(0.6) ? 1 : rng.between(2, 7))
                        : new int[] {1, 5, 7, 10}[rng.between(0, 3)];
                LeaseRow lease = new LeaseRow(String.format(Locale.ROOT, "L%04d", leases.size() + 1), property.id(), tenant.id(),
                        start, end, rentRupees * 100, unitArea, billingDay, status);
                leases.add(lease);
                generateObligations(property, lease, tenant.behaviour(), defaultAfter, rng);
            }

            boolean renew = tenant.behaviour() != Behaviour.DEFAULTER && rng.chance(p.renewalChance());
            if (renew) {
                start = end.plusDays(1);
            } else {
                start = end.plusDays(1 + rng.between(p.vacancyMinDays(), p.vacancyMaxDays()));
                tenant = null;
            }
        }
    }

    private TenantRow newTenant(Type type, Profile p, Rng rng) {
        String segment = switch (type) {
            case RESIDENTIAL -> "INDIVIDUAL";
            case OFFICE -> rng.chance(0.6) ? "SME" : "CORPORATE";
            case RETAIL -> rng.chance(0.7) ? "SME" : "CORPORATE";
            case INDUSTRIAL -> rng.chance(0.7) ? "CORPORATE" : "SME";
        };
        Behaviour behaviour = Behaviour.values()[rng.weighted(p.behaviourWeights())];
        TenantRow tenant = new TenantRow(String.format(Locale.ROOT, "T%04d", tenants.size() + 1), segment, behaviour);
        tenants.add(tenant);
        return tenant;
    }

    // ------------------------------------------------------------------ rent and payments

    private void generateObligations(PropertyRow property, LeaseRow lease, Behaviour behaviour, int defaultAfter,
                                     Rng rng) {
        YearMonth month = YearMonth.from(lease.start());
        YearMonth last = YearMonth.from(lease.end());
        int instalment = 0;
        for (; !month.isAfter(last); month = month.plusMonths(1)) {
            LocalDate due = month.atDay(lease.billingDay());
            if (due.isBefore(lease.start()) || due.isAfter(lease.end())
                    || due.isBefore(horizonStart) || due.isAfter(horizonEnd)) {
                continue;
            }
            instalment++;
            String obligationId = String.format(Locale.ROOT, "OB%06d", obligations.size() + 1);
            long duePaise = lease.rentPaise();

            boolean defaulted = behaviour == Behaviour.DEFAULTER && instalment > defaultAfter;
            Behaviour effective = behaviour == Behaviour.DEFAULTER ? Behaviour.CHRONIC : behaviour;
            int monthsIn = (int) ChronoUnit.MONTHS.between(YearMonth.from(horizonStart), month);

            long collectible = duePaise;
            if (defaulted) {
                collectible = 0;
            } else if (property.degrading() && monthsIn >= 18 && rng.chance(0.30)) {
                collectible = roundTo((long) (duePaise * rng.between(0.4, 0.8)), 10_000L); // part-paid, rest never arrives
            } else if (rng.chance(shortPayChance(effective, property.type()))) {
                // Short payment: part of the rent is disputed or withheld and is still owed at the horizon end.
                collectible = roundTo((long) (duePaise * rng.between(0.5, 0.9)), 10_000L);
            }
            int delay = delayDays(effective, property, monthsIn, rng);
            LocalDate finalDate = due.plusDays(delay);
            // An early payment is never dated before the lease or the horizon begins.
            if (finalDate.isBefore(lease.start())) {
                finalDate = lease.start();
            }
            if (finalDate.isBefore(horizonStart)) {
                finalDate = horizonStart;
            }

            List<TransactionRow> payments = schedule(obligationId, due, finalDate, collectible, effective, rng);
            long paid = 0;
            LocalDate settled = null;
            for (TransactionRow t : payments) {
                if (t.date().isAfter(horizonEnd)) {
                    continue; // not yet received at the end of the horizon
                }
                transactions.add(t);
                paid += t.amountPaise();
                if (paid >= duePaise && settled == null) {
                    settled = t.date();
                }
            }
            String status = paid == 0 ? "UNPAID" : paid >= duePaise ? "PAID" : "PARTIAL";
            obligations.add(new ObligationRow(obligationId, lease.id(), due, duePaise, paid, settled, status));
        }
    }

    /** Probability that an instalment is short-paid and the remainder stays outstanding. */
    private static double shortPayChance(Behaviour behaviour, Type type) {
        double base = switch (behaviour) {
            case PUNCTUAL -> 0.002;
            case OCCASIONAL -> 0.03;
            case CHRONIC, DEFAULTER -> 0.12;
        };
        return type == Type.RETAIL ? base * 1.5 : base;
    }

    /** Days between due date and final payment (negative means paid early). */
    private int delayDays(Behaviour behaviour, PropertyRow property, int monthsIn, Rng rng) {
        double r = rng.uniform();
        int late = switch (behaviour) {
            case PUNCTUAL -> r < 0.25 ? -rng.between(1, 4) : r < 0.85 ? 0 : rng.between(1, 5);
            case OCCASIONAL -> r < 0.10 ? -rng.between(1, 3) : r < 0.50 ? 0 : rng.exponentialDays(9);
            case CHRONIC, DEFAULTER -> r < 0.10 ? 0 : 4 + rng.exponentialDays(20);
        };
        if (late > 0 && property.type() == Type.RETAIL) {
            late = (int) Math.ceil(late * 1.4); // Planted pattern 1: heavier retail tail
        }
        if (late > 0 && property.degrading() && monthsIn >= 18) {
            late = (int) Math.ceil(late * (1 + 0.08 * (monthsIn - 17))); // Planted pattern 5
        }
        return Math.min(late, 150);
    }

    /** Splits a payment into 1 to 3 instalments, the last on {@code finalDate}. Amounts in paise. */
    private List<TransactionRow> schedule(String obligationId, LocalDate due, LocalDate finalDate, long amount,
                                          Behaviour behaviour, Rng rng) {
        List<TransactionRow> payments = new ArrayList<>();
        if (amount <= 0) {
            return payments;
        }
        double splitChance = switch (behaviour) {
            case PUNCTUAL -> 0.03;
            case OCCASIONAL -> 0.12;
            case CHRONIC, DEFAULTER -> 0.25;
        };
        long gap = ChronoUnit.DAYS.between(due, finalDate);
        int parts = gap >= 4 && rng.chance(splitChance) ? rng.between(2, 3) : 1;
        long remaining = amount;
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < parts - 1; i++) {
            dates.add(due.plusDays(rng.between(0, (int) gap - 1)));
        }
        dates.sort(Comparator.naturalOrder());
        for (LocalDate date : dates) {
            long part = roundTo((long) (amount * rng.between(0.2, 0.5)), 10_000); // ₹100 steps
            if (part <= 0 || remaining - part < 10_000) {
                break; // keep at least ₹100 for the final instalment
            }
            payments.add(new TransactionRow(obligationId, date, part));
            remaining -= part;
        }
        payments.add(new TransactionRow(obligationId, finalDate, remaining));
        return payments;
    }

    // ------------------------------------------------------------------ expenses

    private void generateExpenses(PropertyRow property, Profile p, Rng rng) {
        long monthlyRent = property.potentialMonthlyRentPaise();
        int feeDay = rng.between(5, 9);
        Month insuranceMonth = Month.of(rng.between(1, 12));
        double securityRate = p.securityPerSqft() * rng.between(0.9, 1.1);
        double cleaningRate = p.cleaningPerSqft() * rng.between(0.9, 1.1);

        for (YearMonth month = YearMonth.from(horizonStart); !month.isAfter(YearMonth.from(horizonEnd));
             month = month.plusMonths(1)) {
            boolean monsoon = month.getMonthValue() >= 6 && month.getMonthValue() <= 9;
            double season = switch (month.getMonthValue()) {
                case 4, 5, 6 -> 1.30; // summer cooling load
                case 7, 8, 9 -> 1.10;
                default -> 0.95;
            };

            addExpense(property, month.atDay(feeDay), "MANAGEMENT", Math.round(monthlyRent * 0.04 / 100.0) * 100);
            addExpense(property, month.atDay(Math.min(feeDay + 3, 28)), "SECURITY",
                    Math.round(property.area() * securityRate) * 100);
            if (cleaningRate > 0) {
                addExpense(property, month.atDay(Math.min(feeDay + 5, 28)), "CLEANING",
                        Math.round(property.area() * cleaningRate) * 100);
            }
            addExpense(property, randomDay(month, rng), "UTILITIES",
                    Math.round(monthlyRent * 0.02 * season * rng.between(0.9, 1.1) / 100.0) * 100);
            if (month.getMonth() == Month.JUNE) {
                addExpense(property, month.atDay(15), "PROPERTY_TAX", Math.round(monthlyRent * 12 * 0.05 / 100.0) * 100);
            }
            if (month.getMonth() == insuranceMonth) {
                addExpense(property, month.atDay(feeDay), "INSURANCE",
                        Math.round(property.acquisitionPaise() * 0.001 / 100.0) * 100);
            }

            // Planted pattern 2: maintenance and repairs rise in the monsoon (June to September).
            int jobs = rng.poisson(p.maintenanceRate() * (monsoon ? 1.8 : 1.0));
            for (int j = 0; j < jobs; j++) {
                double amount = rng.logNormal(p.maintenanceMedian(), 0.6) * (monsoon ? 1.25 : 1.0);
                addExpense(property, randomDay(month, rng), "MAINTENANCE", Math.round(amount) * 100);
            }
            int repairs = rng.poisson(0.25 * (monsoon ? 1.6 : 1.0));
            for (int j = 0; j < repairs; j++) {
                double amount = rng.logNormal(p.repairMedian(), 0.7);
                if (rng.chance(0.012)) {
                    amount *= rng.between(8.0, 15.0); // Planted pattern 4: rare outsized one-off repair
                }
                addExpense(property, randomDay(month, rng), "REPAIRS", Math.round(amount) * 100);
            }
        }
    }

    private void addExpense(PropertyRow property, LocalDate date, String category, long amountPaise) {
        if (amountPaise > 0) {
            expenses.add(new ExpenseRow(property.id(), date, category, amountPaise));
        }
    }

    private static LocalDate randomDay(YearMonth month, Rng rng) {
        return month.atDay(rng.between(1, month.lengthOfMonth()));
    }

    // ------------------------------------------------------------------ output

    private void write(Path dir) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (PropertyRow p : properties) {
            rows.add(List.of(p.id(), p.name(), p.type().name(), p.zone(), Long.toString(p.area()),
                    CsvWriter.rupees(p.acquisitionPaise()), Integer.toString(p.units())));
        }
        CsvWriter.write(dir.resolve("properties.csv"), List.of("property_id", "property_name", "property_type",
                "location_zone", "rentable_area_sqft", "acquisition_value", "total_units"), rows);

        rows = new ArrayList<>();
        for (TenantRow t : tenants) {
            rows.add(List.of(t.id(), t.segment()));
        }
        CsvWriter.write(dir.resolve("tenants.csv"), List.of("tenant_id", "tenant_segment"), rows);

        rows = new ArrayList<>();
        for (LeaseRow l : leases) {
            rows.add(List.of(l.id(), l.propertyId(), l.tenantId(), l.start().toString(), l.end().toString(),
                    CsvWriter.rupees(l.rentPaise()), Long.toString(l.area()), Integer.toString(l.billingDay()), l.status()));
        }
        CsvWriter.write(dir.resolve("leases.csv"), List.of("lease_id", "property_id", "tenant_id", "start_date",
                "end_date", "monthly_rent", "leased_area_sqft", "billing_day", "status"), rows);

        rows = new ArrayList<>();
        for (ObligationRow o : obligations) {
            rows.add(List.of(o.id(), o.leaseId(), o.due().toString(), CsvWriter.rupees(o.duePaise()),
                    CsvWriter.rupees(o.paidPaise()), o.settled() == null ? "" : o.settled().toString(), o.status()));
        }
        CsvWriter.write(dir.resolve("rent_obligations.csv"), List.of("obligation_id", "lease_id", "due_date",
                "due_amount", "amount_paid", "settled_date", "status"), rows);

        List<TransactionRow> sortedTx = new ArrayList<>(transactions);
        sortedTx.sort(Comparator.comparing(TransactionRow::date).thenComparing(TransactionRow::obligationId));
        rows = new ArrayList<>();
        for (int i = 0; i < sortedTx.size(); i++) {
            TransactionRow t = sortedTx.get(i);
            rows.add(List.of(String.format(Locale.ROOT, "TX%07d", i + 1), t.obligationId(), t.date().toString(),
                    CsvWriter.rupees(t.amountPaise())));
        }
        CsvWriter.write(dir.resolve("rent_transactions.csv"), List.of("transaction_id", "obligation_id",
                "transaction_date", "amount_received"), rows);

        List<ExpenseRow> sortedExp = new ArrayList<>(expenses);
        sortedExp.sort(Comparator.comparing(ExpenseRow::date).thenComparing(ExpenseRow::propertyId)
                .thenComparing(ExpenseRow::category));
        rows = new ArrayList<>();
        for (int i = 0; i < sortedExp.size(); i++) {
            ExpenseRow e = sortedExp.get(i);
            rows.add(List.of(String.format(Locale.ROOT, "E%07d", i + 1), e.propertyId(), e.date().toString(), e.category(),
                    CsvWriter.rupees(e.amountPaise())));
        }
        CsvWriter.write(dir.resolve("expenses.csv"), List.of("expense_id", "property_id", "expense_date",
                "expense_category", "amount"), rows);
    }

    private static int roundTo(int value, int step) {
        return Math.max(step, Math.round(value / (float) step) * step);
    }

    private static long roundTo(long value, long step) {
        return Math.max(step, Math.round(value / (double) step) * step);
    }
}
