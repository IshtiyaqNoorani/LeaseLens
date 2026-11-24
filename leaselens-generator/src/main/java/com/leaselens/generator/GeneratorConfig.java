package com.leaselens.generator;

import java.time.YearMonth;

/**
 * Generator settings. The defaults reproduce the committed sample dataset.
 *
 * @param seed          master random seed
 * @param start         first month of the horizon
 * @param months        number of months in the horizon
 * @param propertyCount number of properties
 */
public record GeneratorConfig(long seed, YearMonth start, int months, int propertyCount) {

    public static final long DEFAULT_SEED = 20261124L;

    public GeneratorConfig {
        if (months < 12 || months > 120) {
            throw new IllegalArgumentException("months must be between 12 and 120");
        }
        if (propertyCount < 10 || propertyCount > 5_000) {
            throw new IllegalArgumentException("propertyCount must be between 10 and 5000");
        }
    }

    /** 50 properties, November 2023 to October 2026 (1,096 days), seed 20261124. */
    public static GeneratorConfig defaults() {
        return new GeneratorConfig(DEFAULT_SEED, YearMonth.of(2023, 11), 36, 50);
    }
}
