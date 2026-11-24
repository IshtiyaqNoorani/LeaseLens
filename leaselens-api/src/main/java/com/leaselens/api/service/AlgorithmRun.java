package com.leaselens.api.service;

/**
 * Shows which hand-written algorithm produced a result and what it cost (PDF §5 "algorithm
 * visibility").
 *
 * @param operations machine-independent cost: tree nodes visited or element comparisons
 * @param seed       random seed used (randomized algorithms only)
 */
public record AlgorithmRun(String name, int syllabusUnit, String operation, String timeComplexity, int n,
                           long operations, long elapsedNanos, Long seed) {

    public static AlgorithmRun segmentTree(String operation, String complexity, int n, long ops, long nanos) {
        return new AlgorithmRun("SegmentTree", 3, operation, complexity, n, ops, nanos, null);
    }

    public static AlgorithmRun quickselect(String operation, int n, long ops, long nanos, long seed) {
        return new AlgorithmRun("RandomizedQuickselect", 6, operation, "expected O(n)", n, ops, nanos, seed);
    }
}
