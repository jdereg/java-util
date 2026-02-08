# Future: IntervalMap<K, V> for java-util + Guava TreeRangeMap Replacement

*Drafted: 2026-02-06. Deferred for future implementation.*

## Summary

Add `IntervalMap<K, V>` to java-util as a thread-safe map from non-overlapping intervals to values. This would allow replacing Guava's `TreeRangeMap` in n-cube's `Axis.java`, further reducing the Guava dependency surface.

## Motivation

After replacing Guava's Joiner/Splitter/Iterables in n-cube with JDK equivalents (commit `83c10d8e`), the remaining Guava surface area is:
1. **TreeRangeMap** in `Axis.java` - maps intervals to Column objects for RANGE/SET axes
2. **Cache** in `GCacheManager.java` / `GuavaCache.java` - keeping for now

IntervalMap would also be useful for general-purpose interval-to-value mappings (e.g., partition tracking with metadata, time-range scheduling with associated data).

## Why IntervalSet Cannot Wrap IntervalMap

We explored whether `IntervalSet` could be simplified to wrap `IntervalMap` (like `CompactSet` wraps `CompactMap`). The answer is **no**:

- **IntervalSet auto-merges**: Adding [1,5) then [3,8) merges to [1,8). This is core to its semantics.
- **IntervalMap cannot auto-merge**: [1,5)->A and [3,8)->B cannot merge because A != B.
- IntervalSet's merge logic is tightly coupled to `ConcurrentSkipListMap` operations. Wrapping would either break encapsulation or duplicate all merge logic with no code reuse.
- **Decision**: IntervalMap should be a standalone class sharing design patterns with IntervalSet.

## Standard Interface Fit Analysis

Neither class maps cleanly to standard Java collection interfaces:

- **IntervalMap vs NavigableMap/SortedMap**: Map interfaces have one key type for both storage and lookup. IntervalMap stores by interval (two bounds) but looks up by point (one value). `put(K, V)` can't express an interval.
- **IntervalSet vs NavigableSet/SortedSet**: Auto-merge violates Set's contract. After `add(a)` then `add(b)`, the set may contain neither original element.
- **Good fits**: `Iterable` (both), `Serializable` (both), `Function<K, V>` (IntervalMap point lookup), `Predicate<T>` (IntervalSet point containment).

## IntervalMap<K, V> Design

### Key Requirement: Point Intervals

n-cube's SET axis stores **both** half-open ranges [low, high) and discrete point values. Currently handled via Guava's `Range.closedOpen()` and `Range.closed()`. IntervalMap must support both:
- `put(start, end, value)` for half-open [start, end) ranges
- `putPoint(point, value)` for discrete point values

### Internal Storage

```java
private final ConcurrentSkipListMap<K, Node<K, V>> intervals = new ConcurrentSkipListMap<>();
private final transient ReentrantLock lock = new ReentrantLock();

private static class Node<K, V> {
    final K end;      // null for point intervals
    final V value;
}
```

### Proposed API

```java
public class IntervalMap<K extends Comparable<? super K>, V> implements Iterable<IntervalMap.Entry<K, V>> {

    public static final class Entry<K extends Comparable<? super K>, V> {
        // start, end (null for points), value
        // isPoint(), getStart(), getEnd(), getValue()
    }

    // Mutations (write-locked)
    void put(K start, K end, V value);       // [start, end) range
    void putPoint(K point, V value);         // exact point
    boolean remove(K start, K end);          // remove exact range
    boolean removePoint(K point);            // remove exact point
    void clear();

    // Point lookup (lock-free)
    V get(K point);                          // searches both ranges and points

    // Overlap queries (lock-free) -- replaces Guava's subRangeMap()
    boolean overlaps(K start, K end);        // any entry overlaps [start, end)?
    boolean containsPoint(K point);          // any entry contains this point?
    List<Entry<K, V>> getOverlapping(K start, K end);

    // Iteration (lock-free, weakly consistent)
    Iterator<Entry<K, V>> iterator();        // all entries ordered by start
    List<Entry<K, V>> snapshot();            // atomic copy
    int size();
    boolean isEmpty();
}
```

### Core Algorithm: get(K point)
1. `floorEntry(point)` -- find largest key <= point
2. If node.end == null -- point interval, return value only if exact match
3. If node.end != null -- range [start, end), return value if end > point

### Core Algorithm: overlaps(K start, K end)
1. Check `floorEntry(start)` -- a range starting before `start` might extend into [start, end)
2. Check all entries in `subMap(start, false, end, false)` -- any entry starting within (start, end) overlaps
3. For point entries, check if point falls within [start, end)

## n-cube Axis.java Migration

**File**: `src/main/java/com/cedarsoftware/ncube/Axis.java`

Axis.java is the **only file** in n-cube using TreeRangeMap. The migration involves 13 localized changes:

### Current Guava Usage

| Operation | Line(s) | Purpose | IntervalMap Replacement |
|-----------|---------|---------|------------------------|
| `TreeRangeMap.create()` | 110 | Field init | `new IntervalMap<>()` |
| `put(Range, Column)` | 618, 620-625 | Index column | `put(low, high, col)` / `putPoint(val, col)` |
| `get(point)` | 1739 | Point lookup | `get(point)` (same signature) |
| `remove(Range)` | 1093, 1095-1100 | Deindex column | `remove(low, high)` / `removePoint(val)` |
| `clear()` | 829-830 | Clear indexes | `clear()` (same) |
| `subRangeMap()` | 1867, 1882 | Overlap detection | `overlaps(low, high)` / `containsPoint(val)` |
| `asMapOfRanges().values()` | 1922 | Get unique columns | `for (Entry e : map)` + LinkedHashSet |
| `asMapOfRanges().size()` | 1997 | Size (testing) | `size()` |
| `asMapOfRanges().forEach()` | 2055-2059 | Iterate entries | `for (Entry e : map)` |

### Key Migration Details

**indexColumn() -- SET axis** needs to distinguish Range from discrete:
```java
// Before:
rangeToCol.put(valueToRange(elem), column);

// After:
if (elem instanceof com.cedarsoftware.ncube.Range range) {
    rangeToCol.put(range.getLow(), range.getHigh(), column);
} else {
    rangeToCol.putPoint(elem, column);
}
```

**doesOverlap()** simplifies significantly:
```java
// Before (3 lines):
RangeMap<Comparable, Column> ranges = rangeToCol.subRangeMap(valueToRange(range));
return !ranges.asMapOfRanges().isEmpty();

// After (1 line):
return rangeToCol.overlaps(range.getLow(), range.getHigh());
```

**valueToRange() helper** (lines 1844-1856) can be deleted entirely -- conversion is inlined at each call site.

### Post-Migration Guava Status

After this migration, only `GCacheManager.java` and `GuavaCache.java` would reference Guava (for Cache). The `com.google.common.collect.Range`, `RangeMap`, and `TreeRangeMap` imports would be fully eliminated.

## Implementation Sequence

1. Create `IntervalMap.java` in java-util (~600-800 lines)
2. Create `IntervalMapTest.java` with comprehensive tests
3. Update java-util changelog, release
4. Update n-cube's java-util dependency version
5. Apply 13 changes to Axis.java, delete `valueToRange()`
6. Run full n-cube test suite (`./gradlew clean test`)
7. Verify no Guava Range/RangeMap imports remain

## Verification Criteria

1. All java-util tests pass, IntervalMapTest >90% coverage
2. All n-cube tests pass after migration
3. `grep -r "com.google.common.collect.Range" src/main/` returns no hits
