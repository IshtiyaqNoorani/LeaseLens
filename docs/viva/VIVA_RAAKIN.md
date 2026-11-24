# Viva preparation: Raakin Majeed (2404537)

## Two-minute answer
1. **My contribution:** I own the Segment Tree (`leaselens-algorithms/.../segmenttree/SegmentTree.java`) and its 22 tests; the cash-flow service and correction ledger (`leaselens-api/.../store/CashFlowIndex.java`, `service/CashFlowService.java`); the Cash-Flow Explorer screen; the segment-tree benchmarks; and the CSV validation module (`leaselens-data`).
2. **The algorithm:** a binary tree over daily net cash flow. Each node stores the sum, minimum and maximum of its interval and the earliest index of each extreme. One tree per property plus one for the portfolio.
3. **Correctness:** invariant: every node holds the aggregate of its interval. PULL restores it from the two children. A query merges only nodes fully inside the range; those are disjoint and cover it exactly. An update changes only the root-to-leaf path containing the index.
4. **Complexity:** build O(n); query O(log n) (at most 4 nodes per level, tested at n = 100,000); update O(log n); space O(n) with 4n-sized arrays.

## Methods to be able to walk through
| Method | File and line | Key point |
|---|---|---|
| `query(left, right, counter)` | SegmentTree.java:101 | validates the range, then recursive query with an accumulator |
| private `query(node, lo, hi, …)` | SegmentTree.java:171 | three cases: no overlap, full overlap (merge), partial (recurse left then right) |
| `pull(node)` | SegmentTree.java:196 | `Math.addExact`; `<=` / `>=` keep the left child, so the earliest day wins ties |
| private `update(node, lo, hi, index, …)` | SegmentTree.java:149 | descend to the leaf, then pull on the way up; rollback on overflow |
| `CashFlowIndex.adjust` | leaselens-api/.../store/CashFlowIndex.java | two point updates: property tree and portfolio tree |

## Likely questions
- **Why not prefix sums?** They give O(1) sums but cannot answer min/max, and a correction costs O(n). Benchmarks: prefix sums win for sum-only with no corrections; they are 1.44× slower at 100k with 10% corrections and 7.9× slower with 50%.
- **Why a tree for only 1,096 days?** At that size anything is fast. The tree gives sum, min and max together, O(log n) corrections, and it scales. A scan is faster for ranges of 32 days or fewer, and we say so.
- **Why 4n arrays?** A recursive layout on non-power-of-two n needs up to about 4n slots.
- **What if l > r, or the range is past the data?** An IllegalArgumentException with a message, shown as a 400 in the API.
- **Overflow?** Sums use `Math.addExact`, so an overflow raises an error and a failed update is rolled back.
- **What is validated on upload?** 20 rules, including a sweep-line check that leases running at the same time never exceed the rentable area.
