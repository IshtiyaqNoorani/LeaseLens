package com.leaselens.generator;

import java.util.SplittableRandom;

/**
 * Seeded random helpers. Uses {@link StrictMath} (not {@link Math}) so that log and exp give
 * bit-identical results on every JVM and CPU, which keeps the generated CSVs byte-identical.
 */
final class Rng {

    private final SplittableRandom random;

    Rng(long seed) {
        this.random = new SplittableRandom(seed);
    }

    /** Independent stream for sub-entity {@code index}, derived deterministically from a seed. */
    static Rng stream(long seed, long index) {
        long mixed = seed * 0x9E3779B97F4A7C15L + index * 0xBF58476D1CE4E5B9L + 0x94D049BB133111EBL;
        return new Rng(mixed);
    }

    double uniform() {
        return random.nextDouble();
    }

    /** Uniform integer in [min, max] inclusive. */
    int between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /** Uniform double in [min, max). */
    double between(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    boolean chance(double p) {
        return random.nextDouble() < p;
    }

    /** Standard normal via Box–Muller with StrictMath. */
    double gaussian() {
        double u1 = 1.0 - random.nextDouble(); // (0, 1]
        double u2 = random.nextDouble();
        return StrictMath.sqrt(-2.0 * StrictMath.log(u1)) * StrictMath.cos(2.0 * StrictMath.PI * u2);
    }

    /** Log-normal with the given median and log-scale spread. */
    double logNormal(double median, double sigma) {
        return median * StrictMath.exp(sigma * gaussian());
    }

    /** Exponential with the given mean, rounded up to a whole number of days (at least 1). */
    int exponentialDays(double mean) {
        double u = 1.0 - random.nextDouble();
        return (int) Math.max(1, StrictMath.ceil(-mean * StrictMath.log(u)));
    }

    /** Poisson count (Knuth's method; fine for the small means used here). */
    int poisson(double mean) {
        double limit = StrictMath.exp(-mean);
        double product = random.nextDouble();
        int count = 0;
        while (product > limit) {
            count++;
            product *= random.nextDouble();
        }
        return count;
    }

    /** Picks index i with probability weights[i] / sum(weights). */
    int weighted(double[] weights) {
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        double target = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            target -= weights[i];
            if (target < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    <T> T pick(T[] items) {
        return items[random.nextInt(items.length)];
    }
}
