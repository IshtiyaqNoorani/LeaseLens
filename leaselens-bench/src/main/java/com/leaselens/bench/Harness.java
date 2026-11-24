package com.leaselens.bench;

import java.util.function.LongSupplier;

/**
 * Minimal timing harness. Each task returns a checksum, which is accumulated into a volatile sink so
 * the JIT cannot remove the work as dead code. Warm-up iterations are run and discarded first, so
 * measurements reflect JIT-compiled code.
 *
 * <p>Very short tasks are hard to time with one {@code nanoTime} pair, so each timed sample runs the
 * task {@code batch} times back to back, with {@code batch} chosen after warm-up so a sample lasts at
 * least {@value #MIN_SAMPLE_NANOS} ns. Reported times are per single run of the task (sample ÷ batch).
 */
public final class Harness {

    private static volatile long sink;
    static final long MIN_SAMPLE_NANOS = 1_000_000L;
    private static final int MAX_BATCH = 10_000;

    private final int warmups;
    private final int repetitions;

    public Harness(int warmups, int repetitions) {
        if (warmups < 0 || repetitions < 1) {
            throw new IllegalArgumentException("warmups >= 0 and repetitions >= 1 required");
        }
        this.warmups = warmups;
        this.repetitions = repetitions;
    }

    public int repetitions() {
        return repetitions;
    }

    /** Result of timing one task: timing statistics and the checksum it produced. */
    public record Timing(Stats nanos, long checksum, int batch) {
    }

    public Timing time(LongSupplier task) {
        long checksum = 0;
        for (int i = 0; i < warmups; i++) {
            checksum = task.getAsLong();
            sink += checksum;
        }
        long probeStart = System.nanoTime();
        sink += task.getAsLong();
        long probe = Math.max(1, System.nanoTime() - probeStart);
        int batch = (int) Math.min(MAX_BATCH, Math.max(1, (MIN_SAMPLE_NANOS + probe - 1) / probe));

        long[] samples = new long[repetitions];
        for (int i = 0; i < repetitions; i++) {
            long c = 0;
            long start = System.nanoTime();
            for (int b = 0; b < batch; b++) {
                c = task.getAsLong();
                sink += c;
            }
            samples[i] = (System.nanoTime() - start) / batch;
            if (i > 0 && c != checksum && warmups > 0) {
                throw new IllegalStateException("Non-deterministic task: checksum changed between repetitions");
            }
            checksum = c;
        }
        return new Timing(Stats.of(samples), checksum, batch);
    }
}
