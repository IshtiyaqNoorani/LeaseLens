package com.leaselens.generator;

import java.nio.file.Path;
import java.time.YearMonth;

/**
 * Command line entry point.
 *
 * <pre>
 * java -jar leaselens-generator/target/leaselens-generator.jar sample   [--out data/sample] [--seed N]
 *                                                              [--start 2023-11] [--months 36] [--properties 50]
 * java -jar leaselens-generator/target/leaselens-generator.jar fixtures [--golden data/golden] [--out data/invalid]
 * </pre>
 */
public final class GeneratorCli {

    private GeneratorCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || !(args[0].equals("sample") || args[0].equals("fixtures"))) {
            usage();
            System.exit(2);
        }
        if (args[0].equals("sample")) {
            GeneratorConfig d = GeneratorConfig.defaults();
            Path out = Path.of(option(args, "--out", "data/sample"));
            GeneratorConfig config = new GeneratorConfig(
                    Long.parseLong(option(args, "--seed", Long.toString(d.seed()))),
                    YearMonth.parse(option(args, "--start", d.start().toString())),
                    Integer.parseInt(option(args, "--months", Integer.toString(d.months()))),
                    Integer.parseInt(option(args, "--properties", Integer.toString(d.propertyCount()))));
            PortfolioGenerator.Summary s = new PortfolioGenerator(config).writeTo(out);
            System.out.printf("Wrote %s (seed %d): %d properties, %d tenants, %d leases, %d obligations, "
                            + "%d transactions, %d expenses; horizon %s to %s%n",
                    out, config.seed(), s.properties(), s.tenants(), s.leases(), s.obligations(),
                    s.transactions(), s.expenses(), s.horizonStart(), s.horizonEnd());
            System.out.printf("Planted ground truth: vacancy shock %s; degrading collections %s%n",
                    s.vacancyShockProperties(), s.degradingProperty());
        } else {
            Path golden = Path.of(option(args, "--golden", "data/golden"));
            Path out = Path.of(option(args, "--out", "data/invalid"));
            int count = InvalidFixtureWriter.writeAll(golden, out);
            System.out.printf("Wrote %d invalid-data fixtures to %s%n", count, out);
        }
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static void usage() {
        System.err.println("""
                Usage:
                  sample   [--out DIR] [--seed N] [--start YYYY-MM] [--months N] [--properties N]
                  fixtures [--golden DIR] [--out DIR]""");
    }
}
