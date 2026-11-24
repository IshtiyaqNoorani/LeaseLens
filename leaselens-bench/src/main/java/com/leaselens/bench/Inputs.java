package com.leaselens.bench;

import java.util.SplittableRandom;

/** Seeded benchmark inputs. The same (shape, n, seed) always gives the same array. */
public final class Inputs {

    private Inputs() {
    }

    public static final String[] SHAPES = {"uniform", "delays", "sorted", "reverse", "identical"};

    public static long[] shape(String shape, int n, long seed) {
        SplittableRandom rng = new SplittableRandom(seed * 31 + n);
        long[] a = new long[n];
        switch (shape) {
            case "uniform" -> {
                for (int i = 0; i < n; i++) a[i] = rng.nextLong(0, 1_000_000_000L);
            }
            case "delays" -> {
                // Shaped like the sample's days-late data: about two-thirds exactly 0, then a long right tail.
                for (int i = 0; i < n; i++) {
                    a[i] = rng.nextDouble() < 0.68 ? 0 : Math.min(200, 1 + (long) Math.ceil(-12 * Math.log(1 - rng.nextDouble())));
                }
            }
            case "sorted" -> {
                for (int i = 0; i < n; i++) a[i] = i;
            }
            case "reverse" -> {
                for (int i = 0; i < n; i++) a[i] = n - i;
            }
            case "identical" -> java.util.Arrays.fill(a, 7);
            case "distinct" -> {
                for (int i = 0; i < n; i++) a[i] = i;
                for (int i = n - 1; i > 0; i--) { // Fisher–Yates shuffle
                    int j = rng.nextInt(i + 1);
                    long t = a[i];
                    a[i] = a[j];
                    a[j] = t;
                }
            }
            default -> throw new IllegalArgumentException("Unknown shape " + shape);
        }
        return a;
    }

    /** Daily net cash flows in paise, roughly ±₹5 lakh per day. */
    public static long[] cashFlows(int n, long seed) {
        SplittableRandom rng = new SplittableRandom(seed * 17 + n);
        long[] a = new long[n];
        for (int i = 0; i < n; i++) a[i] = rng.nextLong(-50_000_000L, 50_000_000L);
        return a;
    }
}
