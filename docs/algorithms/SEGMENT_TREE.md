# Segment Tree: design, pseudocode, correctness and complexity

Owner: **Raakin Majeed** · Syllabus: Unit 3 (trees and range queries) · Code: `leaselens-algorithms/src/main/java/com/leaselens/algorithms/segmenttree/SegmentTree.java` · Tests: `SegmentTreeTest` (22 test cases)

## 1. Business role
Each element `v[i]` is the **net rental cash flow on day i** in paise (rent received minus operating expenses). For any date range `[l, r]` the Cash-Flow Explorer needs:
- total net cash flow, `Σ v[l..r]`
- the worst day: `min v[l..r]` and the date it happened
- the best day: `max v[l..r]` and the date it happened

When an analyst corrects a day's figure (for example a reversed charge), later queries must reflect it. That is a **point update**.

## 2. Structure
A binary tree stored in arrays. Node 1 (the root) covers `[0, n−1]`. A node covering `[lo, hi]`, with `mid = ⌊(lo+hi)/2⌋`, has a left child `2·node` covering `[lo, mid]` and a right child `2·node+1` covering `[mid+1, hi]`. Each node stores `sum`, `min`, `minIdx`, `max` and `maxIdx` for its interval. Arrays of size `4n` are enough for this layout.

**Invariant (I):** for every node covering `[lo, hi]`, its fields equal the sum, minimum and maximum of `v[lo..hi]`, and the index fields hold the **earliest** position of the extreme value.

## 3. Pseudocode
```
BUILD(node, lo, hi)
    if lo = hi: set node from v[lo]; return
    mid ← ⌊(lo+hi)/2⌋
    BUILD(2node, lo, mid); BUILD(2node+1, mid+1, hi)
    PULL(node)

PULL(node)                                 ▷ combine children; ties keep the left (earlier) index
    sum[node] ← sum[L] + sum[R]            ▷ overflow-checked
    (min, minIdx)[node] ← (min[L] ≤ min[R]) ? (min, minIdx)[L] : (min, minIdx)[R]
    (max, maxIdx)[node] ← (max[L] ≥ max[R]) ? (max, maxIdx)[L] : (max, maxIdx)[R]

QUERY(node, lo, hi, l, r, acc)
    if r < lo or hi < l: return                       ▷ no overlap
    if l ≤ lo and hi ≤ r: acc ← MERGE(acc, node); return   ▷ full overlap
    mid ← ⌊(lo+hi)/2⌋
    QUERY(2node, lo, mid, l, r, acc)                  ▷ left first, so nodes are merged left to right
    QUERY(2node+1, mid+1, hi, l, r, acc)

UPDATE(node, lo, hi, i)                     ▷ v[i] has already been changed
    if lo = hi: set node from v[i]; return
    mid ← ⌊(lo+hi)/2⌋
    if i ≤ mid: UPDATE(2node, lo, mid, i) else UPDATE(2node+1, mid+1, hi, i)
    PULL(node)
```

## 4. Correctness
**Build.** By induction on interval length. A leaf `[i, i]` holds `v[i]`, so (I) holds. For an internal node, assume (I) holds for both children. The node's interval is the disjoint union of the children's intervals, so its sum is the sum of the children's sums and its min/max is the smaller/larger of theirs. PULL computes exactly this, and with `≤`/`≥` it keeps the left child's index on ties. Because every index in the left child is smaller than every index in the right, the earliest index is kept. So (I) holds at the root and at every node.

**Query.** QUERY merges a node only when its interval lies entirely inside `[l, r]`, and it explores children only on partial overlap. So the merged nodes (the "canonical nodes") are disjoint, and together they cover `[l, r]` exactly:
- every index in `[l, r]` lies in some leaf, and the first ancestor of that leaf whose interval fits inside `[l, r]` is merged;
- no index outside `[l, r]` is merged.

By (I), merging their aggregates gives the correct sum, min and max. Canonical nodes are visited left to right, so a strict `<` / `>` in MERGE keeps the earliest index on ties.

**Update.** Only nodes whose interval contains `i` can change. These are exactly the nodes on the root-to-leaf path that UPDATE follows. The leaf is reset from `v[i]` and each ancestor is recomputed by PULL after its child, bottom-up, so (I) is restored on the path. Nodes off the path are unaffected.

**Overflow.** Sums use `Math.addExact`, so a sum outside `[−2⁶³, 2⁶³−1]` raises an error instead of wrapping round to a wrong number. A failed update is rolled back, so (I) still holds.

## 5. Complexity
| Operation | Time | Reason |
|---|---|---|
| Build | **O(n)** | `T(n) = 2T(n/2) + O(1)`, i.e. about 2n − 1 nodes, each done in O(1) |
| Query | **O(log n)** | at most **4 nodes visited per level** (below) × (⌈log₂ n⌉ + 1) levels |
| Update | **O(log n)** | one root-to-leaf path of ⌈log₂ n⌉ + 1 nodes |
| Space | **O(n)** | 5 arrays of 4n entries, plus a copy of the input |

*Why at most 4 per level.* At any level, at most two visited nodes **partially** overlap `[l, r]`: the ones containing the boundaries `l` and `r`. A node strictly between them lies fully inside the range and is not split. Only partially overlapping nodes have children explored, so at most 2 × 2 = 4 nodes are visited at the next level. The tests check this bound for 2,000 random queries at n = 100,000 (`queryVisitsLogarithmicNodes`).

For n = 1,096 days (36 months), a query visits at most 4 × 12 = 48 nodes, while a scan reads up to 1,096 values.

## 6. Baselines and when the tree helps (measured in Phase 6)
| Method | Build | Query (sum/min/max) | Point update |
|---|---|---|---|
| Linear scan | none | O(n) | O(1) |
| Prefix sums | O(n) | O(1) for **sum only**; min/max not supported | O(n) rebuild |
| **Segment tree** | O(n) | O(log n) for all three | O(log n) |

Hypotheses to test (not results):
- Prefix sums should win for sum-only queries with no corrections.
- A scan should be competitive for very short ranges or very few queries.
- The segment tree should win when there are many min/max queries over long ranges, mixed with corrections.

**Measured (Phase 6, `benchmarks/BENCHMARKS.md`):** all three hypotheses held.
- Prefix sums were 20–70× faster for sum-only queries with no corrections.
- The linear scan was 3–10× faster for ranges of 32 days or fewer.
- The tree was about 40× faster than a scan at 100,000 for long ranges, and 1.4–7.9× faster than prefix sums once 10–50% of operations were corrections.
- Building the tree costs 4–27× more than building a prefix array.

## 7. Test evidence (`SegmentTreeTest`)
Single element · complete range · partial range · first and last boundaries · ties resolve to the earliest index · all-negative values · point update · repeated updates on one index · input copied · 5 invalid ranges · 3 invalid update indices · empty and null input · overflow detection with rollback · **differential test: 5 seeds × 10,000 mixed operations against a brute-force scan** · node-visit bound for queries · path-length bound for updates.

## 8. Viva checklist
- Explain invariant (I) and why PULL restores it.
- Explain why the merged nodes are disjoint and cover `[l, r]`.
- Explain why a query visits at most 4 nodes per level.
- Explain why the array size is 4n and why the earliest index wins ties (`≤` in PULL, `<` in MERGE).
- Know what happens with `query(5, 2)`, a range past the last day, an empty dataset, and overflow.
