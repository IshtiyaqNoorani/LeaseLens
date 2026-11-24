package com.leaselens.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Percentage and ratio helpers. A zero denominator gives null ("not applicable"), never 0. */
public final class Ratios {

    private Ratios() {
    }

    /** numerator / denominator × 100, rounded half-up to 2 decimals; null if the denominator is 0. */
    public static Double pct(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP).doubleValue();
    }

    /** numerator / denominator rounded half-up to 2 decimals; null if the denominator is 0. */
    public static Double ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Paise per unit converted to rupees with 2 decimals; null if the denominator is 0. */
    public static Double rupeesPer(long paise, long units) {
        if (units == 0) {
            return null;
        }
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(units), 0, RoundingMode.HALF_UP)
                .movePointLeft(2).doubleValue();
    }
}
