package com.leaselens.bench.baseline;

/**
 * BASELINE (not assessed): answers range sum / min / max by scanning every element in the range.
 * Query O(r − l + 1); point update O(1).
 */
public final class LinearScan {

    private final long[] values;

    public LinearScan(long[] initial) {
        this.values = initial.clone();
    }

    /** Returns {sum, min, max} of values[l..r]. */
    public long[] query(int l, int r) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = l; i <= r; i++) {
            long v = values[i];
            sum += v;
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        return new long[] {sum, min, max};
    }

    public void update(int index, long value) {
        values[index] = value;
    }
}
