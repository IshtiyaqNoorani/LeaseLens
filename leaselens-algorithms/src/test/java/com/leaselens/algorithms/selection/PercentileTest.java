package com.leaselens.algorithms.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.SplittableRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for the nearest-rank convention k = ceil(P/100 × n) − 1. */
class PercentileTest {

    @ParameterizedTest(name = "n = {0}, P = {1}/100 % -> k = {2}")
    @CsvSource({
        // n, P in hundredths, expected zero-based k (hand-computed)
        "1, 1, 0",        // a single observation is every percentile
        "1, 10000, 0",
        "10, 1, 0",       // ceil(0.001) = 1 -> k = 0 (minimum)
        "10, 5000, 4",    // ceil(5) = 5
        "10, 9000, 8",    // ceil(9) = 9
        "10, 10000, 9",   // P100 = maximum
        "4, 2500, 0",     // ceil(1) = 1
        "4, 5000, 1",     // ceil(2) = 2
        "4, 7500, 2",     // ceil(3) = 3
        "3, 5000, 1",     // ceil(1.5) = 2
        "7, 9000, 6",     // ceil(6.3) = 7
        "20, 9500, 18",   // ceil(19) = 19
        "100, 700, 6",    // exactly 7; floating point would give 7.000000000000001 -> wrong k = 7
        "100, 9999, 99",  // ceil(99.99) = 100
        "2147483647, 10000, 2147483646" // largest n: no overflow
    })
    @DisplayName("Nearest-rank index table")
    void nearestRankIndexTable(int n, int pHundredths, int expectedK) {
        assertEquals(expectedK, Percentile.nearestRankIndex(n, pHundredths));
    }

    @ParameterizedTest(name = "P = {0} hundredths is rejected")
    @ValueSource(ints = {0, -1, 10_001, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("Percentiles outside (0, 100] are rejected")
    void invalidPercentileRejected(int pHundredths) {
        assertThrows(IllegalArgumentException.class, () -> Percentile.nearestRankIndex(10, pHundredths));
    }

    @Test
    @DisplayName("An empty selection (n = 0) is rejected, not answered with a default")
    void emptySelectionRejected() {
        assertThrows(IllegalArgumentException.class, () -> Percentile.nearestRankIndex(0, 5000));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({"90, 9000", "99.5, 9950", "0.01, 1", "100, 10000", "100.00, 10000", "' 75 ', 7500", "7, 700"})
    @DisplayName("Parsing percent text into hundredths")
    void parseValid(String text, int expected) {
        assertEquals(expected, Percentile.parseHundredths(text));
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"0", "-5", "100.01", "99.999", "0.001", "abc", "", "  "})
    @DisplayName("Invalid percent text is rejected")
    void parseInvalid(String text) {
        assertThrows(IllegalArgumentException.class, () -> Percentile.parseHundredths(text));
    }

    @Test
    @DisplayName("Formatting for display")
    void format() {
        assertEquals("90.5", Percentile.format(9050));
        assertEquals("90", Percentile.format(9000));
        assertEquals("0.01", Percentile.format(1));
    }

    @Test
    @DisplayName("End to end: percentile via quickselect equals sorted[k] on duplicate-heavy delay data")
    void percentileThroughQuickselect() {
        SplittableRandom gen = new SplittableRandom(20261124);
        long[] daysLate = gen.longs(5_000, 0, 30).map(v -> v < 18 ? 0 : v).toArray(); // ~60% on time
        long[] sorted = daysLate.clone();
        Arrays.sort(sorted);

        int[] percentiles = {5000, 7500, 9000, 9500, 9900, 10000};
        int[] ranks = Arrays.stream(percentiles).map(p -> Percentile.nearestRankIndex(daysLate.length, p)).toArray();
        long[] values = RandomizedQuickselect.selectMany(daysLate, ranks, new SplittableRandom(1), null);
        for (int i = 0; i < ranks.length; i++) {
            assertEquals(sorted[ranks[i]], values[i], "P" + Percentile.format(percentiles[i]));
        }
    }
}
