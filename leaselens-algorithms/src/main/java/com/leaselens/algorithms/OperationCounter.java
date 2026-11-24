package com.leaselens.algorithms;

/**
 * Counts elementary operations performed by an algorithm run.
 *
 * <p>The Segment Tree counts tree nodes visited; Randomized Quickselect counts element-to-pivot
 * comparisons. Counts do not depend on the machine, so they complement wall-clock timings when
 * checking empirical behaviour against the theoretical complexity. Not thread-safe: use one
 * counter per run.
 */
public final class OperationCounter {

    private long count;

    /** Records one operation. */
    public void increment() {
        count++;
    }

    /** Returns the number of operations recorded since creation or the last {@link #reset()}. */
    public long get() {
        return count;
    }

    /** Sets the count back to zero. */
    public void reset() {
        count = 0;
    }
}
