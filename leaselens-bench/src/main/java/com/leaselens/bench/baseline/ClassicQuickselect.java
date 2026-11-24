package com.leaselens.bench.baseline;

import java.util.random.RandomGenerator;

import com.leaselens.algorithms.OperationCounter;

/**
 * BASELINE (not assessed): textbook quickselect with a two-way (Lomuto) partition. The pivot is
 * either the first element of the range (deterministic) or chosen at random. Used to show why the
 * assessed implementation randomizes the pivot and partitions three ways.
 *
 * <p>Known worst cases: first-element pivot on sorted input, and any pivot rule on input with many
 * equal values (each round removes only the pivot), both Θ(n²).
 */
public final class ClassicQuickselect {

    public enum Pivot { FIRST, RANDOM }

    private ClassicQuickselect() {
    }

    public static long select(long[] data, int k, Pivot pivotRule, RandomGenerator rng, OperationCounter counter) {
        long[] a = data.clone();
        int lo = 0;
        int hi = a.length - 1;
        while (lo < hi) {
            int p = pivotRule == Pivot.FIRST ? lo : lo + rng.nextInt(hi - lo + 1);
            swap(a, p, hi);
            long pivot = a[hi];
            int store = lo;
            for (int i = lo; i < hi; i++) {
                if (counter != null) {
                    counter.increment();
                }
                if (a[i] < pivot) {
                    swap(a, store++, i);
                }
            }
            swap(a, store, hi);
            if (k == store) {
                return a[k];
            }
            if (k < store) {
                hi = store - 1;
            } else {
                lo = store + 1;
            }
        }
        return a[lo];
    }

    private static void swap(long[] a, int i, int j) {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
