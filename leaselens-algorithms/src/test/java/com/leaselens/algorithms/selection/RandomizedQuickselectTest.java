package com.leaselens.algorithms.selection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.leaselens.algorithms.OperationCounter;

/**
 * Correctness and probabilistic-behaviour tests for {@link RandomizedQuickselect}.
 * The reference answer is always {@code sorted[k]} from {@link Arrays#sort(long[])}.
 * All randomness uses fixed seeds, so these tests give the same result on every run.
 */
class RandomizedQuickselectTest {

    private static SplittableRandom rng(long seed) {
        return new SplittableRandom(seed);
    }

    private static long reference(long[] data, int k) {
        long[] sorted = data.clone();
        Arrays.sort(sorted);
        return sorted[k];
    }

    @Test
    @DisplayName("Single element")
    void singleElement() {
        assertEquals(9, RandomizedQuickselect.select(new long[] {9}, 0, rng(1)));
    }

    @Test
    @DisplayName("Sorted input: every rank")
    void sortedInput() {
        long[] data = LongStream.rangeClosed(1, 200).toArray();
        for (int k = 0; k < data.length; k++) {
            assertEquals(k + 1, RandomizedQuickselect.select(data, k, rng(k)));
        }
    }

    @Test
    @DisplayName("Reverse-sorted input: every rank")
    void reverseSortedInput() {
        long[] data = LongStream.iterate(200, v -> v - 1).limit(200).toArray();
        for (int k = 0; k < data.length; k++) {
            assertEquals(k + 1, RandomizedQuickselect.select(data, k, rng(k)));
        }
    }

    @Test
    @DisplayName("Duplicates: a realistic days-late sample with many zeros")
    void duplicates() {
        long[] daysLate = {0, 0, 3, 0, 12, 0, 1, 3, 0, 45, 0, 3, 7, 0, 0};
        for (int k = 0; k < daysLate.length; k++) {
            assertEquals(reference(daysLate, k), RandomizedQuickselect.select(daysLate, k, rng(99)), "k=" + k);
        }
    }

    @Test
    @DisplayName("All identical values: correct, and a single linear pass (n comparisons)")
    void allIdentical() {
        long[] data = new long[10_000];
        Arrays.fill(data, 5);
        OperationCounter counter = new OperationCounter();
        assertEquals(5, RandomizedQuickselect.select(data, 4_321, rng(3), counter));
        assertEquals(data.length, counter.get());
    }

    @Test
    @DisplayName("Minimum (k = 0) and maximum (k = n-1) ranks, including extreme long values")
    void minimumAndMaximumRanks() {
        long[] data = {4, Long.MAX_VALUE, -7, 0, Long.MIN_VALUE, 12};
        assertEquals(Long.MIN_VALUE, RandomizedQuickselect.select(data, 0, rng(1)));
        assertEquals(Long.MAX_VALUE, RandomizedQuickselect.select(data, data.length - 1, rng(1)));
    }

    @ParameterizedTest(name = "k = {0} is rejected")
    @ValueSource(ints = {-1, 5, 6, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("Invalid ranks are rejected with a clear message")
    void invalidRanks(int k) {
        long[] data = {1, 2, 3, 4, 5};
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RandomizedQuickselect.select(data, k, rng(1)));
        assertTrue(e.getMessage().contains("outside the valid range"), e.getMessage());
    }

    @Test
    @DisplayName("Empty data, null data and a null random generator are rejected")
    void emptyAndNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> RandomizedQuickselect.select(new long[0], 0, rng(1)));
        assertThrows(NullPointerException.class, () -> RandomizedQuickselect.select(null, 0, rng(1)));
        assertThrows(NullPointerException.class, () -> RandomizedQuickselect.select(new long[] {1}, 0, null));
    }

    @Test
    @DisplayName("select() never modifies the caller's array")
    void inputNotModified() {
        long[] data = {5, 3, 9, 1, 7, 3};
        long[] before = data.clone();
        RandomizedQuickselect.select(data, 2, rng(1));
        assertArrayEquals(before, data);
    }

    @Test
    @DisplayName("In-place result satisfies the partition property used in the correctness proof")
    void partitionPropertyAfterInPlaceSelect() {
        SplittableRandom data = rng(11);
        for (int trial = 0; trial < 200; trial++) {
            long[] a = data.longs(1 + data.nextInt(500), 0, 50).toArray(); // many duplicates
            long[] original = a.clone();
            int k = data.nextInt(a.length);
            long result = RandomizedQuickselect.selectInPlace(a, k, rng(trial), null);

            assertEquals(result, a[k]);
            for (int i = 0; i < k; i++) {
                assertTrue(a[i] <= result);
            }
            for (int i = k + 1; i < a.length; i++) {
                assertTrue(a[i] >= result);
            }
            long[] sortedOriginal = original.clone();
            long[] sortedAfter = a.clone();
            Arrays.sort(sortedOriginal);
            Arrays.sort(sortedAfter);
            assertArrayEquals(sortedOriginal, sortedAfter, "selection must only permute the data");
        }
    }

    @Test
    @DisplayName("Reproducibility: same seed gives the same comparison count; any seed gives the same answer")
    void seedReproducibility() {
        long[] data = rng(5).longs(5_000, 0, 1_000_000).toArray();
        OperationCounter first = new OperationCounter();
        OperationCounter second = new OperationCounter();
        long a = RandomizedQuickselect.select(data, 2_500, rng(42), first);
        long b = RandomizedQuickselect.select(data, 2_500, rng(42), second);
        assertEquals(a, b);
        assertEquals(first.get(), second.get());

        for (long seed = 0; seed < 50; seed++) {
            assertEquals(a, RandomizedQuickselect.select(data, 2_500, rng(seed)));
        }
    }

    @Test
    @DisplayName("Las Vegas property: 1,000 seeds x 6 input shapes always equal the sorted reference")
    void differentialAcrossSeedsAndShapes() {
        int n = 501;
        SplittableRandom gen = rng(2026);
        long[] uniform = gen.longs(n, -1_000_000, 1_000_000).toArray();
        long[] sorted = LongStream.range(0, n).toArray();
        long[] reverse = LongStream.range(0, n).map(i -> n - i).toArray();
        long[] fewDistinct = gen.longs(n, 0, 4).toArray();
        long[] identical = new long[n];
        long[] organPipe = LongStream.range(0, n).map(i -> Math.min(i, n - 1 - i)).toArray();
        long[][] shapes = {uniform, sorted, reverse, fewDistinct, identical, organPipe};

        for (long[] shape : shapes) {
            long[] expectedSorted = shape.clone();
            Arrays.sort(expectedSorted);
            for (int seed = 0; seed < 1_000; seed++) {
                int k = Math.floorMod(seed * 7919, n);
                assertEquals(expectedSorted[k], RandomizedQuickselect.select(shape, k, rng(seed)),
                        "seed " + seed + ", k " + k);
            }
        }
    }

    @Test
    @DisplayName("selectMany returns the same values as separate selections")
    void selectManyMatchesReference() {
        long[] data = rng(8).longs(2_000, 0, 120).toArray();
        int[] ranks = {0, 999, 1_499, 1_799, 1_999};
        long[] results = RandomizedQuickselect.selectMany(data, ranks, rng(8), null);
        for (int i = 0; i < ranks.length; i++) {
            assertEquals(reference(data, ranks[i]), results[i]);
        }
    }

    @Test
    @DisplayName("Probabilistic behaviour: mean comparisons for the median stay near the 3.39n expectation")
    void expectedComparisonsAreLinear() {
        int n = 10_001;
        long[] distinct = LongStream.range(0, n).toArray();
        // shuffle once with a fixed seed so the input order is arbitrary but reproducible
        SplittableRandom shuffle = rng(77);
        for (int i = n - 1; i > 0; i--) {
            int j = shuffle.nextInt(i + 1);
            long t = distinct[i];
            distinct[i] = distinct[j];
            distinct[j] = t;
        }

        int trials = 500;
        double total = 0;
        long worst = 0;
        for (int seed = 0; seed < trials; seed++) {
            OperationCounter counter = new OperationCounter();
            RandomizedQuickselect.select(distinct, n / 2, rng(seed), counter);
            total += counter.get();
            worst = Math.max(worst, counter.get());
        }
        double meanPerElement = total / trials / n;
        // Theory: expected comparisons for the median ≈ 2(1 + ln 2) n ≈ 3.386 n.
        assertTrue(meanPerElement > 2.0 && meanPerElement < 3.6,
                "mean comparisons per element = " + meanPerElement);
        // Every run (not just the average) stayed far below quadratic behaviour.
        assertTrue(worst < 10L * n, "worst run used " + worst + " comparisons");
    }

    @Test
    @DisplayName("Randomization matters: a fixed first-element pivot grows super-linearly on sorted input; random stays linear")
    void randomPivotAvoidsAdversarialInput() {
        // Comparisons per element at n and 4n. Linear growth keeps this ratio roughly constant.
        double fixedSmall = comparisonsPerElement(4_000, new FirstElementGenerator());
        double fixedLarge = comparisonsPerElement(16_000, new FirstElementGenerator());
        double randomSmall = comparisonsPerElement(4_000, rng(1));
        double randomLarge = comparisonsPerElement(16_000, rng(1));

        // Fixed pivot: cost per element keeps rising as n grows (measured ~59.6 -> ~119.3).
        assertTrue(fixedLarge > 1.8 * fixedSmall,
                "fixed pivot per-element cost " + fixedSmall + " -> " + fixedLarge);
        // Random pivot: cost per element stays bounded by a small constant.
        assertTrue(randomSmall < 4.0 && randomLarge < 4.0,
                "random pivot per-element cost " + randomSmall + " -> " + randomLarge);
        assertTrue(fixedLarge > 20 * randomLarge);
    }

    /** Comparisons per element to select the maximum of the already-sorted array 0..n-1. */
    private static double comparisonsPerElement(int n, java.util.random.RandomGenerator generator) {
        long[] sorted = LongStream.range(0, n).toArray();
        OperationCounter counter = new OperationCounter();
        assertEquals(n - 1, RandomizedQuickselect.select(sorted, n - 1, generator, counter));
        return (double) counter.get() / n;
    }

    /** Deterministic "random" source that always picks offset 0, i.e. the first element. */
    private static final class FirstElementGenerator implements java.util.random.RandomGenerator {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
