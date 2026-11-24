package com.leaselens.algorithms.segmenttree;

/**
 * Aggregate of a closed index range {@code [left, right]} returned by {@link SegmentTree#query}.
 *
 * @param left     first index of the range (inclusive)
 * @param right    last index of the range (inclusive)
 * @param sum      sum of all values in the range
 * @param min      smallest value in the range
 * @param minIndex index of the smallest value; the earliest index if several are equal
 * @param max      largest value in the range
 * @param maxIndex index of the largest value; the earliest index if several are equal
 */
public record RangeResult(int left, int right, long sum, long min, int minIndex, long max, int maxIndex) {

    /** Number of elements in the range. */
    public int length() {
        return right - left + 1;
    }
}
