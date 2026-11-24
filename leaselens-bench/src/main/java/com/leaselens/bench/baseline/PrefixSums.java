package com.leaselens.bench.baseline;

/**
 * BASELINE (not assessed): prefix-sum array. Range SUM in O(1); it cannot answer range min/max.
 * A point update must rewrite every prefix from the changed index onward: O(n − index).
 */
public final class PrefixSums {

    private final long[] values;
    private final long[] prefix; // prefix[i] = values[0] + ... + values[i-1]

    public PrefixSums(long[] initial) {
        this.values = initial.clone();
        this.prefix = new long[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            prefix[i + 1] = prefix[i] + values[i];
        }
    }

    public long sum(int l, int r) {
        return prefix[r + 1] - prefix[l];
    }

    public void update(int index, long value) {
        long delta = value - values[index];
        values[index] = value;
        for (int i = index + 1; i < prefix.length; i++) {
            prefix[i] += delta;
        }
    }
}
