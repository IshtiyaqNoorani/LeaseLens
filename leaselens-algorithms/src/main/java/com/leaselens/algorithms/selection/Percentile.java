package com.leaselens.algorithms.selection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Nearest-rank percentile convention, computed with exact integer arithmetic.
 *
 * <p>For n observations and percentile P (0 &lt; P ≤ 100) the zero-based rank is
 * <pre>k = ceil(P / 100 × n) − 1</pre>
 * P is held in <b>hundredths of a percent</b> ({@code 9050} means 90.50%), so
 * {@code k = ceil(pH × n / 10000) − 1} is computed on {@code long} integers and is never affected
 * by floating-point rounding (with doubles, for example, 0.07 × 100 evaluates to 7.000000000000001).
 *
 * <p>Properties: P = 100 gives k = n − 1 (the maximum); a very small P gives k = 0 (the minimum);
 * the result is always an actual observed value, never an interpolation.
 */
public final class Percentile {

    /** 100% expressed in hundredths of a percent. */
    public static final int MAX_HUNDREDTHS = 10_000;

    private Percentile() {
    }

    /**
     * Zero-based nearest-rank index for {@code n} observations.
     *
     * @param n            number of observations, at least 1
     * @param pHundredths  percentile in hundredths of a percent, in {@code [1, 10000]}
     * @throws IllegalArgumentException if {@code n < 1} or {@code pHundredths} is out of range
     */
    public static int nearestRankIndex(int n, int pHundredths) {
        if (n < 1) {
            throw new IllegalArgumentException(
                    "A percentile needs at least one observation; the selection is empty.");
        }
        checkHundredths(pHundredths);
        long numerator = (long) pHundredths * n;                       // ≤ 10^4 × (2^31 − 1): fits in long
        long rank = (numerator + MAX_HUNDREDTHS - 1) / MAX_HUNDREDTHS; // ceil(numerator / 10000), in [1, n]
        return (int) (rank - 1);
    }

    /**
     * Parses a percentile written in percent, such as {@code "90"}, {@code "99.5"} or
     * {@code "0.01"}, into hundredths of a percent.
     *
     * @throws IllegalArgumentException if the text is not a number, has more than two decimal
     *                                  places, or is not in {@code (0, 100]}
     */
    public static int parseHundredths(String percentText) {
        Objects.requireNonNull(percentText, "percentile must not be null");
        BigDecimal percent;
        try {
            percent = new BigDecimal(percentText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Percentile '" + percentText + "' is not a number.");
        }
        BigDecimal hundredths = percent.movePointRight(2);
        if (hundredths.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "Percentile '" + percentText + "' has more than two decimal places.");
        }
        if (percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Percentile " + percentText + " must be greater than 0 and at most 100.");
        }
        int value = hundredths.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        checkHundredths(value);
        return value;
    }

    /** Formats hundredths of a percent for display, e.g. 9050 → "90.5", 9000 → "90". */
    public static String format(int pHundredths) {
        checkHundredths(pHundredths);
        return BigDecimal.valueOf(pHundredths, 2).stripTrailingZeros().toPlainString();
    }

    private static void checkHundredths(int pHundredths) {
        if (pHundredths < 1 || pHundredths > MAX_HUNDREDTHS) {
            throw new IllegalArgumentException(
                    "Percentile must be greater than 0 and at most 100 (got "
                            + BigDecimal.valueOf(pHundredths, 2).toPlainString() + ").");
        }
    }
}
