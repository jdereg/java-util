package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe set of closed intervals <b>[start, end]</b> (both boundaries inclusive) for any Comparable type.
 *
 * <h2>Core Capabilities</h2>
 * <p>
 * IntervalSet efficiently manages collections of intervals with the following key features:
 * </p>
 * <ul>
 *   <li><b>O(log n) performance</b> - Uses {@link ConcurrentSkipListMap} for efficient lookups, insertions, and range queries</li>
 *   <li><b>Thread-safe</b> - Lock-free reads with minimal locking for writes only</li>
 *   <li><b>Flexible merging behavior</b> - Configurable auto-merge vs. discrete storage modes</li>
 *   <li><b>Intelligent interval splitting</b> - Automatically splits intervals during removal operations</li>
 *   <li><b>Rich query API</b> - Comprehensive set of methods for finding, filtering, and navigating intervals</li>
 *   <li><b>Type-safe boundaries</b> - Supports precise boundary calculations for 20+ built-in types</li>
 * </ul>
 *
 * <h2>Auto-Merge vs. Discrete Modes</h2>
 * <p>
 * The behavior of interval storage is controlled by the <b>autoMerge</b> flag set during construction:
 * </p>
 *
 * <h3>Auto-Merge Mode (default: {@code autoMerge = true})</h3>
 * <p>
 * Overlapping intervals are automatically merged into larger, non-overlapping intervals:
 * </p>
 * <pre>{@code
 *   IntervalSet<Integer> set = new IntervalSet<>();  // autoMerge = true by default
 *   set.add(1, 5);
 *   set.add(3, 8);    // Merges with [1,5] to create [1,8]
 *   set.add(10, 15);  // Separate interval since no overlap
 *   // Result: [1,8], [10,15]
 * }</pre>
 *
 * <h3>Discrete Mode ({@code autoMerge = false})</h3>
 * <p>
 * Intervals are stored separately even if they overlap, useful for audit trails, tracking individual
 * operations, or maintaining historical records:
 * </p>
 * <pre>{@code
 *   IntervalSet<Integer> audit = new IntervalSet<>(false);  // discrete mode
 *   audit.add(1, 5);     // First verification
 *   audit.add(3, 8);     // Second verification (overlaps but kept separate)
 *   audit.add(10, 15);   // Third verification
 *   // Result: [1,5], [3,8], [10,15] - all intervals preserved for audit purposes
 * }</pre>
 *
 * <p>
 * <b>Important:</b> Regardless of storage mode, all query APIs ({@link #contains}, {@link #intervalContaining},
 * navigation methods) work identically - they provide a unified logical view across all stored intervals.
 * Only the internal storage representation differs.
 * </p>
 *
 * <h2>Primary Client APIs</h2>
 *
 * <h3>Basic Operations</h3>
 * <ul>
 *   <li>{@link #add(T, T)} - Add an interval [start, end]</li>
 *   <li>{@link #remove(T, T)} - Remove an interval, splitting existing ones as needed</li>
 *   <li>{@link #removeExact(T, T)} - Remove only exact interval matches</li>
 *   <li>{@link #removeRange(T, T)} - Remove a range, trimming overlapping intervals</li>
 *   <li>{@link #contains(T)} - Test if a value falls within any interval</li>
 *   <li>{@link #clear()} - Remove all intervals</li>
 * </ul>
 *
 * <h3>Query and Navigation</h3>
 * <ul>
 *   <li>{@link #intervalContaining(T)} - Find the interval containing a specific value</li>
 *   <li>{@link #nextInterval(T)} - Find the next interval at or after a value</li>
 *   <li>{@link #higherInterval(T)} - Find the next interval strictly after a value</li>
 *   <li>{@link #previousInterval(T)} - Find the previous interval at or before a value</li>
 *   <li>{@link #lowerInterval(T)} - Find the previous interval strictly before a value</li>
 *   <li>{@link #first()} / {@link #last()} - Get the first/last intervals</li>
 * </ul>
 *
 * <h3>Bulk Operations and Iteration</h3>
 * <ul>
 *   <li>{@link #asList()} / {@link #snapshot()} - Get all intervals as an immutable list</li>
 *   <li>{@link #iterator()} - Iterate intervals in ascending order</li>
 *   <li>{@link #descendingIterator()} - Iterate intervals in descending order</li>
 *   <li>{@link #getIntervalsInRange(T, T)} - Get intervals within a key range</li>
 *   <li>{@link #getIntervalsBefore(T)} - Get intervals before a key</li>
 *   <li>{@link #getIntervalsFrom(T)} - Get intervals from a key onward</li>
 *   <li>{@link #removeIntervalsInKeyRange(T, T)} - Bulk removal by key range</li>
 * </ul>
 *
 * <h3>Introspection and Utilities</h3>
 * <ul>
 *   <li>{@link #size()} / {@link #isEmpty()} - Get count and emptiness state</li>
 *   <li>{@link #keySet()} / {@link #descendingKeySet()} - Access start keys as NavigableSet</li>
 *   <li>{@link #totalDuration(java.util.function.BiFunction)} - Compute total duration across intervals</li>
 * </ul>
 *
 * <h2>Supported Types</h2>
 * <p>
 * IntervalSet provides intelligent boundary calculation for interval splitting/merging operations
 * across a wide range of types:
 * </p>
 * <ul>
 *   <li><b>Numeric:</b> Byte, Short, Integer, Long, Float, Double, BigInteger, BigDecimal</li>
 *   <li><b>Character:</b> Character (Unicode-aware)</li>
 *   <li><b>Temporal:</b> Date, java.sql.Date, Time, Timestamp, Instant, LocalDate, LocalTime, LocalDateTime,
 *       ZonedDateTime, OffsetDateTime, OffsetTime, Duration</li>
 *   <li><b>Custom:</b> Any type implementing Comparable (with manual boundary handling if needed)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * IntervalSet is fully thread-safe with an optimized locking strategy:
 * </p>
 * <ul>
 *   <li><b>Lock-free reads:</b> All query operations (contains, navigation, iteration) require no locking</li>
 *   <li><b>Minimal write locking:</b> Only mutation operations acquire the internal ReentrantLock</li>
 *   <li><b>Weakly consistent iteration:</b> Iterators don't throw ConcurrentModificationException</li>
 * </ul>
 *
 * <h2>Common Use Cases</h2>
 *
 * <h3>Time Range Management</h3>
 * <pre>{@code
 *   IntervalSet<ZonedDateTime> schedule = new IntervalSet<>();
 *   schedule.add(meeting1Start, meeting1End);
 *   schedule.add(meeting2Start, meeting2End);
 *
 *   if (schedule.contains(proposedMeetingTime)) {
 *       System.out.println("Time conflict detected");
 *   }
 * }</pre>
 *
 * <h3>Numeric Range Tracking</h3>
 * <pre>{@code
 *   IntervalSet<Long> processedIds = new IntervalSet<>();
 *   processedIds.add(1000L, 1999L);    // First batch
 *   processedIds.add(2000L, 2999L);    // Second batch - automatically merges to [1000, 2999]
 *
 *   Duration totalWork = processedIds.totalDuration((start, end) ->
 *       Duration.ofMillis(end - start + 1));
 * }</pre>
 *
 * <h3>Audit Trail with Discrete Mode</h3>
 * <pre>{@code
 *   IntervalSet<LocalDate> auditLog = new IntervalSet<>(false);  // Keep all entries
 *   auditLog.add(verification1Start, verification1End);
 *   auditLog.add(verification2Start, verification2End);  // Overlaps preserved for audit
 *
 *   // Query APIs still work across all intervals
 *   boolean dateVerified = auditLog.contains(targetDate);
 * }</pre>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><b>Add:</b> O(log n) - May require merging adjacent intervals in auto-merge mode</li>
 *   <li><b>Remove:</b> O(log n) - May require splitting intervals</li>
 *   <li><b>Contains:</b> O(log n) - Single floor lookup</li>
 *   <li><b>Navigation:</b> O(log n) - Leverages NavigableMap operations</li>
 *   <li><b>Iteration:</b> O(n) - Direct map iteration, no additional overhead</li>
 * </ul>
 *
 * @param <T> the type of interval boundaries, must implement {@link Comparable}
 * @see ConcurrentSkipListMap
 * @see NavigableMap
 * @since 3.7.0
 */
public class IntervalSet<T extends Comparable<? super T>> implements Iterable<IntervalSet.Interval<T>> {
    /**
     * Immutable value object representing one interval.
     */
    public static final class Interval<T extends Comparable<? super T>> implements Comparable<Interval<T>> {
        private final T start;
        private final T end;

        Interval(T start, T end) {
            this.start = start;
            this.end = end;
        }

        public T getStart() {
            return start;
        }

        public T getEnd() {
            return end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Interval && start.equals(((Interval<?>) o).start) && end.equals(((Interval<?>) o).end);
        }

        @Override
        public String toString() {
            return "[" + start + " – " + end + "]";
        }

        @Override
        public int compareTo(Interval<T> o) {
            int cmp = start.compareTo(o.start);
            return (cmp != 0) ? cmp : end.compareTo(o.end);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────────
    private final ConcurrentSkipListMap<T, T> intervals = new ConcurrentSkipListMap<>();
    private final ReentrantLock lock = new ReentrantLock();   // guards writes only
    private final boolean autoMerge;                          // whether to merge overlapping intervals

    // ──────────────────────────────────────────────────────────────────────────
    // Constructors
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new IntervalSet with auto-merge enabled (default behavior).
     * Overlapping intervals will be automatically merged when added.
     */
    public IntervalSet() {
        this.autoMerge = true;
    }

    /**
     * Creates a new IntervalSet with configurable merge behavior.
     *
     * @param autoMerge if true, overlapping intervals are merged automatically;
     *                  if false, intervals are stored discretely but queries
     *                  still work across all intervals
     */
    public IntervalSet(boolean autoMerge) {
        this.autoMerge = autoMerge;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Add the inclusive interval [start,end]. Both start and end are inclusive.
     * <p>
     * If autoMerge is true (default), overlapping intervals are merged automatically.
     * If autoMerge is false, intervals are stored discretely but queries still work
     * across all intervals to provide a unified view.
     * </p>
     */
    public void add(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) < 0) {
            throw new IllegalArgumentException("end < start");
        }

        lock.lock();
        try {
            if (autoMerge) {
                addWithMerge(start, end);
            } else {
                addDiscrete(start, end);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add interval with merging logic (original behavior).
     */
    private void addWithMerge(T start, T end) {
        T newStart = start;
        T newEnd = end;

        // 1) absorb potential lower neighbour that overlaps
        Map.Entry<T, T> lower = intervals.lowerEntry(start);
        if (lower != null && lower.getValue().compareTo(start) >= 0) {
            newStart = lower.getKey();
            newEnd = greaterOf(lower.getValue(), end);
            intervals.remove(lower.getKey());
        }

        // 2) absorb all following intervals that intersect the new one
        for (Iterator<Map.Entry<T, T>> it = intervals.tailMap(start, true).entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<T, T> e = it.next();
            if (e.getKey().compareTo(newEnd) > 0) {
                break;           // gap → stop
            }
            newEnd = greaterOf(newEnd, e.getValue());
            it.remove();                                           // consumed
        }
        intervals.put(newStart, newEnd);
    }

    /**
     * Add interval without merging (discrete storage).
     */
    private void addDiscrete(T start, T end) {
        if (intervals.containsKey(start)) {
            throw new IllegalArgumentException("Duplicate start key not allowed in discrete mode");
        }
        intervals.put(start, end);
    }

    /**
     * Remove the inclusive interval [start,end], splitting existing intervals as needed.
     * Both start and end are treated as inclusive bounds.
     * <p>
     * If autoMerge is true, overlapping intervals are split where needed.
     * If autoMerge is false, overlapping discrete intervals are removed entirely.
     * </p>
     */
    public void remove(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) < 0) {
            throw new IllegalArgumentException("end < start");
        }
        removeRange(start, end);
    }


    /**
     * Remove discrete intervals that overlap with the removal range.
     * For discrete mode, we remove entire intervals that overlap rather than splitting.
     */
    private boolean removeDiscrete(T start, T end) {
        boolean changed = false;
        for (Iterator<Map.Entry<T, T>> it = intervals.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<T, T> entry = it.next();
            T s = entry.getKey();
            T v = entry.getValue();
            // Check if intervals overlap: interval [s,v] overlaps with removal [start,end]
            if (!(v.compareTo(start) < 0 || s.compareTo(end) > 0)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Remove an exact interval [start, end] that matches a stored interval exactly.
     * <p>
     * This operation acts only on a single stored interval whose start and end
     * exactly match the specified values. No other intervals are merged, split,
     * or trimmed as a result of this call. To remove a sub-range or to split
     * existing intervals, use {@link #remove(T, T)} or {@link #removeRange(T, T)}.
     * </p>
     * <p>
     * Both <code>start</code> and <code>end</code> are treated as inclusive bounds.
     * If no matching interval exists, the set remains unchanged.
     * This method is thread-safe: it acquires the internal lock to perform removal
     * under concurrent access but does not affect merging or splitting logic.
     * </p>
     *
     * @param start the inclusive start key of the interval to remove (must match exactly)
     * @param end   the inclusive end key of the interval to remove (must match exactly)
     * @return {@code true} if an interval with exactly this start and end was found and removed;
     * {@code false} otherwise (no change to the set)
     */
    public boolean removeExact(T start, T end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        lock.lock();
        try {
            return intervals.remove(start, end);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the inclusive range [start, end] from the set, trimming and splitting intervals as necessary.
     * <p>
     * If autoMerge is true, intervals are trimmed and split as needed.
     * If autoMerge is false, overlapping discrete intervals are removed entirely.
     * </p>
     * <p>
     * For autoMerge=true, any stored interval that overlaps the removal range:
     * <ul>
     *   <li>If an interval begins before <code>start</code>, its right boundary is trimmed to <code>start</code>.</li>
     *   <li>If an interval ends after <code>end</code>, its left boundary is trimmed to <code>end</code>.</li>
     *   <li>If an interval fully contains <code>[start,end]</code>, it is split into two intervals:
     *       one covering <code>[originalStart, start]</code> and one covering <code>[end, originalEnd]</code>.</li>
     *   <li>Intervals entirely within <code>[start,end]</code> are removed.</li>
     * </ul>
     * </p>
     * <p>This operation is thread-safe: it acquires the internal write lock during mutation.</p>
     *
     * @param start inclusive start of the range to remove
     * @param end   inclusive end of the range to remove
     * @throws IllegalArgumentException if <code>end &lt; start</code>
     */
    public void removeRange(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) < 0) {
            throw new IllegalArgumentException("end < start");
        }

        lock.lock();
        try {
            if (autoMerge) {
                removeRangeWithSplitting(start, end);
            } else {
                removeRangeDiscrete(start, end);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove range with interval splitting (original behavior for merged intervals).
     */
    private void removeRangeWithSplitting(T start, T end) {
        Map.Entry<T, T> lower = intervals.lowerEntry(start);
        if (lower != null && lower.getValue().compareTo(start) >= 0) {
            T lowerKey = lower.getKey();
            T lowerValue = lower.getValue();
            intervals.remove(lowerKey);

            if (lowerKey.compareTo(start) < 0) {
                T leftEnd = previousValue(start);
                if (lowerKey.compareTo(leftEnd) <= 0) {
                    intervals.put(lowerKey, leftEnd);
                }
            }

            if (lowerValue.compareTo(end) > 0) {
                T rightStart = nextValue(end);
                if (rightStart.compareTo(lowerValue) <= 0) {
                    intervals.put(rightStart, lowerValue);
                }
                return;
            }
        }

        for (Iterator<Map.Entry<T, T>> it = intervals.tailMap(start, true).entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<T, T> e = it.next();
            if (e.getKey().compareTo(end) >= 0) {
                break;
            }
            T entryValue = e.getValue();
            it.remove();

            if (entryValue.compareTo(end) > 0) {
                T rightStart = nextValue(end);
                if (rightStart.compareTo(entryValue) <= 0) {
                    intervals.put(rightStart, entryValue);
                }
                break;
            }
        }
    }

    /**
     * Remove discrete intervals that overlap with the removal range.
     */
    private void removeRangeDiscrete(T start, T end) {
        for (Iterator<Map.Entry<T, T>> it = intervals.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<T, T> entry = it.next();
            T s = entry.getKey();
            T v = entry.getValue();
            // Check if intervals overlap: interval [s,v] overlaps with removal [start,end]
            if (!(v.compareTo(start) < 0 || s.compareTo(end) > 0)) {
                it.remove();
            }
        }
    }

    /**
     * True if the value lies in <i>any</i> closed interval [start,end].
     * Both boundaries are inclusive.
     */
    public boolean contains(T value) {
        Objects.requireNonNull(value);
        Map.Entry<T, T> e = intervals.floorEntry(value);
        return e != null && e.getValue().compareTo(value) >= 0;
    }

    /**
     * Return the interval covering the specified <code>value</code>, or {@code null} if no interval contains it.
     * <p>Intervals are closed and inclusive on both ends ([start, end]), so a value v is contained
     * in an interval {@code if start <= v <= end }. This method performs a lock-free read
     * via {@link ConcurrentSkipListMap#floorEntry(Object)} and does not mutate the underlying set.</p>
     *
     * @param value the non-null value to locate within stored intervals
     * @return an {@link Interval} whose start and end bracket <code>value</code>, or {@code null} if none
     * @throws NullPointerException if <code>value</code> is null
     */
    public Interval<T> intervalContaining(T value) {
        Objects.requireNonNull(value);
        Map.Entry<T, T> e = intervals.floorEntry(value);
        return (e != null && e.getValue().compareTo(value) >= 0) ? new Interval<>(e.getKey(), e.getValue()) : null;
    }

    /**
     * Unmodifiable snapshot of all intervals (ordered).
     */
    public List<Interval<T>> asList() {
        List<Interval<T>> list = new ArrayList<>(intervals.size());
        intervals.forEach((s, e) -> list.add(new Interval<>(s, e)));
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns an unmodifiable snapshot of all intervals (ordered). Iterate this list for a applications
     * that need to read the intervals without locking.  Use when clients have correctness requirements
     * you can’t delegate (billing, audit, verification where missing an interval is catastrophic).
     *
     * @return
     */
    public List<Interval<T>> snapshot() {
        return asList();   // already returns unmodifiable copy
    }

    /**
     * Returns the <i>first</i> (lowest key) interval or {@code null}.
     */
    public Interval<T> first() {
        Map.Entry<T, T> e = intervals.firstEntry();
        return e == null ? null : new Interval<>(e.getKey(), e.getValue());
    }

    /**
     * Returns the <i>last</i> (highest key) interval or {@code null}.
     */
    public Interval<T> last() {
        Map.Entry<T, T> e = intervals.lastEntry();
        return e == null ? null : new Interval<>(e.getKey(), e.getValue());
    }

    /**
     * Returns the next interval that contains the given value, or the next interval that starts after the value.
     * <p>
     * If the value falls within an existing interval, that interval is returned.
     * Otherwise, returns the next interval that starts after the value.
     * Uses {@link NavigableMap#floorEntry(Object)} and {@link NavigableMap#ceilingEntry(Object)} for O(log n) performance.
     * </p>
     *
     * @param value the value to search from
     * @return the interval containing the value or the next interval after it, or {@code null} if none
     */
    public Interval<T> nextInterval(T value) {
        Objects.requireNonNull(value);
        
        // First check if the value falls within an existing interval
        Map.Entry<T, T> containing = intervals.floorEntry(value);
        if (containing != null && containing.getValue().compareTo(value) >= 0) {
            // Value is contained within this interval
            return new Interval<>(containing.getKey(), containing.getValue());
        }
        
        // Value is not contained, find the next interval that starts at or after the value
        Map.Entry<T, T> entry = intervals.ceilingEntry(value);
        return entry != null ? new Interval<>(entry.getKey(), entry.getValue()) : null;
    }

    /**
     * Returns the next interval that starts strictly after the given value, or {@code null} if none exists.
     * <p>
     * This method uses {@link NavigableMap#higherEntry(Object)} for O(log n) performance.
     * </p>
     *
     * @param value the value to search from (exclusive)
     * @return the next interval strictly after the value, or {@code null} if none
     */
    public Interval<T> higherInterval(T value) {
        Objects.requireNonNull(value);
        Map.Entry<T, T> entry = intervals.higherEntry(value);
        return entry != null ? new Interval<>(entry.getKey(), entry.getValue()) : null;
    }

    /**
     * Returns the previous interval that starts at or before the given value, or {@code null} if none exists.
     * <p>
     * This method uses {@link NavigableMap#floorEntry(Object)} for O(log n) performance.
     * Note: This returns the interval by start key, not necessarily the interval containing the value.
     * Use {@link #intervalContaining} to find the interval that actually contains a value.
     * </p>
     *
     * @param value the value to search from (inclusive)
     * @return the previous interval at or before the value, or {@code null} if none
     */
    public Interval<T> previousInterval(T value) {
        Objects.requireNonNull(value);
        Map.Entry<T, T> entry = intervals.floorEntry(value);
        return entry != null ? new Interval<>(entry.getKey(), entry.getValue()) : null;
    }

    /**
     * Returns the previous interval that starts strictly before the given value, or {@code null} if none exists.
     * <p>
     * This method uses {@link NavigableMap#lowerEntry(Object)} for O(log n) performance.
     * </p>
     *
     * @param value the value to search from (exclusive)
     * @return the previous interval strictly before the value, or {@code null} if none
     */
    public Interval<T> lowerInterval(T value) {
        Objects.requireNonNull(value);
        Map.Entry<T, T> entry = intervals.lowerEntry(value);
        return entry != null ? new Interval<>(entry.getKey(), entry.getValue()) : null;
    }

    /**
     * Returns all intervals whose start keys fall within the specified range [fromKey, toKey].
     * <p>
     * This method uses {@link NavigableMap#subMap(Object, boolean, Object, boolean)} for efficient range queries.
     * The returned list is ordered by start key.
     * </p>
     *
     * @param fromKey the start of the range (inclusive)
     * @param toKey the end of the range (inclusive)
     * @return a list of intervals within the specified range, ordered by start key
     * @throws IllegalArgumentException if fromKey > toKey
     */
    public List<Interval<T>> getIntervalsInRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        return intervals.subMap(fromKey, true, toKey, true)
                .entrySet()
                .stream()
                .map(e -> new Interval<>(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns all intervals whose start keys are before the specified key.
     * <p>
     * This method uses {@link NavigableMap#headMap(Object, boolean)} for efficient queries.
     * </p>
     *
     * @param toKey the upper bound (exclusive)
     * @return a list of intervals before the specified key, ordered by start key
     */
    public List<Interval<T>> getIntervalsBefore(T toKey) {
        Objects.requireNonNull(toKey);
        return intervals.headMap(toKey, false)
                .entrySet()
                .stream()
                .map(e -> new Interval<>(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns all intervals whose start keys are at or after the specified key.
     * <p>
     * This method uses {@link NavigableMap#tailMap(Object, boolean)} for efficient queries.
     * </p>
     *
     * @param fromKey the lower bound (inclusive)
     * @return a list of intervals at or after the specified key, ordered by start key
     */
    public List<Interval<T>> getIntervalsFrom(T fromKey) {
        Objects.requireNonNull(fromKey);
        return intervals.tailMap(fromKey, true)
                .entrySet()
                .stream()
                .map(e -> new Interval<>(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns an iterator over all intervals in descending order by start key.
     * <p>
     * This method uses {@link NavigableMap#descendingMap()} for efficient reverse iteration.
     * Like the standard iterator, this is weakly consistent and lock-free.
     * </p>
     *
     * @return an iterator over intervals in descending order by start key
     */
    public Iterator<Interval<T>> descendingIterator() {
        return intervals.descendingMap()
                .entrySet()
                .stream()
                .map(e -> new Interval<>(e.getKey(), e.getValue()))
                .iterator();
    }

    /**
     * Returns a set of all start keys in the interval set.
     * <p>
     * This method uses {@link NavigableMap#navigableKeySet()} to provide efficient key operations.
     * The returned set supports range operations and is backed by the interval set.
     * </p>
     *
     * @return a navigable set of start keys
     */
    public java.util.NavigableSet<T> keySet() {
        return intervals.navigableKeySet();
    }

    /**
     * Returns a set of all start keys in descending order.
     * <p>
     * This method uses {@link NavigableMap#descendingKeySet()} for efficient reverse key iteration.
     * </p>
     *
     * @return a navigable set of start keys in descending order
     */
    public java.util.NavigableSet<T> descendingKeySet() {
        return intervals.descendingKeySet();
    }

    /**
     * Removes all intervals whose start keys fall within the specified range [fromKey, toKey].
     * <p>
     * This method uses {@link NavigableMap#subMap(Object, boolean, Object, boolean)} for efficient bulk removal.
     * This is more efficient than calling {@link #removeExact} multiple times.
     * </p>
     *
     * @param fromKey the start of the range (inclusive)
     * @param toKey the end of the range (inclusive)
     * @return the number of intervals removed
     * @throws IllegalArgumentException if fromKey > toKey
     */
    public int removeIntervalsInKeyRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        lock.lock();
        try {
            java.util.NavigableMap<T, T> subMap = intervals.subMap(fromKey, true, toKey, true);
            int count = subMap.size();
            subMap.clear();
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Number of stored, non-overlapping intervals.
     */
    public int size() {
        return intervals.size();
    }

    public boolean isEmpty() {
        return intervals.isEmpty();
    }

    /**
     * Remove all stored intervals from the set.
     * <p>
     * Thread-safe: acquires the write lock to clear the underlying map.
     * After this call, {@link #size()} returns 0, {@link #isEmpty()} is true,
     * and {@link #first()} and {@link #last()} return {@code null}.
     * </p>
     */
    public void clear() {
        lock.lock();
        try {
            intervals.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Compute the total covered duration across all stored intervals.
     * <p>
     * The caller provides a <code>toDuration</code> function that maps each interval's
     * start and end values to a {@link Duration}. This method sums those Durations
     * over all intervals in key order. The read is lock-free (no locking).
     * </p>
     */
    public Duration totalDuration(java.util.function.BiFunction<T, T, Duration> toDuration) {
        Duration d = Duration.ZERO;
        for (Map.Entry<T, T> e : intervals.entrySet()) {
            d = d.plus(toDuration.apply(e.getKey(), e.getValue()));
        }
        return d;
    }

    /**
     * Returns an iterator over all stored intervals in ascending order by start key.
     * <p>
     * This iterator is weakly consistent and lock-free, meaning it reflects live
     * changes to the IntervalSet as they occur during iteration. The iterator does not throw
     * {@link java.util.ConcurrentModificationException} and does not require external
     * synchronization for reading.
     * </p>
     * <p>
     * For strongly consistent iteration that captures a snapshot at call time,
     * use {@link #snapshot()}.iterator() instead.
     * </p>
     *
     * @return an iterator over the intervals in this set, ordered by start key
     */
    @Override
    public Iterator<Interval<T>> iterator() {
        return new Iterator<Interval<T>>() {
            private final Iterator<Map.Entry<T, T>> entryIterator = intervals.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public Interval<T> next() {
                Map.Entry<T, T> entry = entryIterator.next();
                return new Interval<>(entry.getKey(), entry.getValue());
            }
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the greater of two comparable values.
     * <p>
     * Compares the two values using their natural ordering via {@link Comparable#compareTo(Object)}.
     * If the values are equal, returns the first argument.
     * </p>
     *
     * @param <T> the type of the values being compared
     * @param a   the first value to compare
     * @param b   the second value to compare
     * @return the greater of {@code a} and {@code b}, or {@code a} if they are equal
     */
    private static <T extends Comparable<? super T>> T greaterOf(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * Computes the previous adjacent value for splitting intervals.
     * <p>
     * This method is used internally when removing intervals to create proper
     * boundaries for split intervals. It computes the largest value that is
     * strictly less than the given value for supported types.
     * </p>
     * <p>
     * Supported types:
     * <ul>
     *   <li>{@link Byte} - returns value - 1</li>
     *   <li>{@link Short} - returns value - 1</li>
     *   <li>{@link Integer} - returns value - 1</li>
     *   <li>{@link Long} - returns value - 1L</li>
     *   <li>{@link Float} - returns previous representable float value</li>
     *   <li>{@link Double} - returns previous representable double value</li>
     *   <li>{@link BigInteger} - returns value - 1</li>
     *   <li>{@link BigDecimal} - returns value minus smallest unit at current scale</li>
     *   <li>{@link Character} - returns previous Unicode character</li>
     *   <li>{@link Date} - returns value minus 1 millisecond</li>
     *   <li>{@link java.sql.Date} - returns value minus 1 day</li>
     *   <li>{@link java.sql.Time} - returns value minus 1 millisecond</li>
     *   <li>{@link Timestamp} - returns value minus 1 nanosecond</li>
     *   <li>{@link Instant} - returns value minus 1 nanosecond</li>
     *   <li>{@link LocalDate} - returns value minus 1 day</li>
     *   <li>{@link LocalTime} - returns value minus 1 nanosecond</li>
     *   <li>{@link LocalDateTime} - returns value minus 1 nanosecond</li>
     *   <li>{@link ZonedDateTime} - returns value minus 1 nanosecond</li>
     *   <li>{@link OffsetDateTime} - returns value minus 1 nanosecond</li>
     *   <li>{@link OffsetTime} - returns value minus 1 nanosecond</li>
     *   <li>{@link Duration} - returns value minus 1 nanosecond</li>
     * </ul>
     * </p>
     *
     * @param value the value for which to compute the previous value
     * @return the previous adjacent value
     * @throws UnsupportedOperationException if the value type is not supported
     */
    @SuppressWarnings("unchecked")
    private T previousValue(T value) {
        // Handle Number types
        if (value instanceof Number) {
            if (value instanceof Integer) {
                return (T) Integer.valueOf(((Integer) value) - 1);
            } else if (value instanceof Long) {
                return (T) Long.valueOf(((Long) value) - 1L);
            } else if (value instanceof Float) {
                float f = (Float) value;
                return (T) Float.valueOf(Math.nextDown(f));
            } else if (value instanceof Double) {
                double d = (Double) value;
                return (T) Double.valueOf(Math.nextDown(d));
            } else if (value instanceof BigInteger) {
                return (T) ((BigInteger) value).subtract(BigInteger.ONE);
            } else if (value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) value;
                BigDecimal increment = BigDecimal.ONE.scaleByPowerOfTen(-bd.scale());
                return (T) bd.subtract(increment);
            } else if (value instanceof Byte) {
                return (T) Byte.valueOf((byte) (((Byte) value) - 1));
            } else if (value instanceof Short) {
                return (T) Short.valueOf((short) (((Short) value) - 1));
            }
        }

        // Handle Date and its subclasses
        if (value instanceof Date) {
            if (value instanceof Timestamp) {
                Timestamp ts = (Timestamp) value;
                Timestamp result = new Timestamp(ts.getTime());
                result.setNanos(ts.getNanos() - 1);
                if (result.getNanos() < 0) {
                    result.setNanos(999999999);
                    result.setTime(result.getTime() - 1);
                }
                return (T) result;
            } else if (value instanceof java.sql.Date) {
                java.sql.Date date = (java.sql.Date) value;
                return (T) new java.sql.Date(date.getTime() - 86400000L); // minus 1 day in milliseconds
            } else {
                Date date = (Date) value;
                return (T) new Date(date.getTime() - 1);
            }
        }

        // Handle java.time types (Temporal)
        if (value instanceof java.time.temporal.Temporal) {
            if (value instanceof Instant) {
                return (T) ((Instant) value).minusNanos(1);
            } else if (value instanceof LocalDate) {
                return (T) ((LocalDate) value).minusDays(1);
            } else if (value instanceof LocalTime) {
                return (T) ((LocalTime) value).minusNanos(1);
            } else if (value instanceof LocalDateTime) {
                return (T) ((LocalDateTime) value).minusNanos(1);
            } else if (value instanceof ZonedDateTime) {
                return (T) ((ZonedDateTime) value).minusNanos(1);
            } else if (value instanceof OffsetDateTime) {
                return (T) ((OffsetDateTime) value).minusNanos(1);
            } else if (value instanceof OffsetTime) {
                return (T) ((OffsetTime) value).minusNanos(1);
            }
        }

        // Handle Character
        if (value instanceof Character) {
            char c = (Character) value;
            return (T) Character.valueOf((char) (c - 1));
        }

        // Handle Duration
        if (value instanceof Duration) {
            return (T) ((Duration) value).minusNanos(1);
        }

        throw new UnsupportedOperationException("Cannot compute previous value for type " + value.getClass());
    }

    /**
     * Computes the next adjacent value for splitting intervals.
     * <p>
     * This method is used internally when removing intervals to create proper
     * boundaries for split intervals. It computes the smallest value that is
     * strictly greater than the given value for supported types.
     * </p>
     * <p>
     * Supported types:
     * <ul>
     *   <li>{@link Byte} - returns value + 1</li>
     *   <li>{@link Short} - returns value + 1</li>
     *   <li>{@link Integer} - returns value + 1</li>
     *   <li>{@link Long} - returns value + 1L</li>
     *   <li>{@link Float} - returns next representable float value</li>
     *   <li>{@link Double} - returns next representable double value</li>
     *   <li>{@link BigInteger} - returns value + 1</li>
     *   <li>{@link BigDecimal} - returns value plus smallest unit at current scale</li>
     *   <li>{@link Character} - returns next Unicode character</li>
     *   <li>{@link Date} - returns value plus 1 millisecond</li>
     *   <li>{@link java.sql.Date} - returns value plus 1 day</li>
     *   <li>{@link java.sql.Time} - returns value plus 1 millisecond</li>
     *   <li>{@link Timestamp} - returns value plus 1 nanosecond</li>
     *   <li>{@link Instant} - returns value plus 1 nanosecond</li>
     *   <li>{@link LocalDate} - returns value plus 1 day</li>
     *   <li>{@link LocalTime} - returns value plus 1 nanosecond</li>
     *   <li>{@link LocalDateTime} - returns value plus 1 nanosecond</li>
     *   <li>{@link ZonedDateTime} - returns value plus 1 nanosecond</li>
     *   <li>{@link OffsetDateTime} - returns value plus 1 nanosecond</li>
     *   <li>{@link OffsetTime} - returns value plus 1 nanosecond</li>
     *   <li>{@link Duration} - returns value plus 1 nanosecond</li>
     * </ul>
     * </p>
     *
     * @param value the value for which to compute the next value
     * @return the next adjacent value
     * @throws UnsupportedOperationException if the value type is not supported
     */
    @SuppressWarnings("unchecked")
    private T nextValue(T value) {
        // Handle Number types
        if (value instanceof Number) {
            if (value instanceof Integer) {
                return (T) Integer.valueOf(((Integer) value) + 1);
            } else if (value instanceof Long) {
                return (T) Long.valueOf(((Long) value) + 1L);
            } else if (value instanceof Float) {
                float f = (Float) value;
                return (T) Float.valueOf(Math.nextUp(f));
            } else if (value instanceof Double) {
                double d = (Double) value;
                return (T) Double.valueOf(Math.nextUp(d));
            } else if (value instanceof BigInteger) {
                return (T) ((BigInteger) value).add(BigInteger.ONE);
            } else if (value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) value;
                BigDecimal increment = BigDecimal.ONE.scaleByPowerOfTen(-bd.scale());
                return (T) bd.add(increment);
            } else if (value instanceof Byte) {
                return (T) Byte.valueOf((byte) (((Byte) value) + 1));
            } else if (value instanceof Short) {
                return (T) Short.valueOf((short) (((Short) value) + 1));
            }
        }

        // Handle Date and its subclasses
        if (value instanceof Date) {
            if (value instanceof Timestamp) {
                Timestamp ts = (Timestamp) value;
                Timestamp result = new Timestamp(ts.getTime());
                result.setNanos(ts.getNanos() + 1);
                if (result.getNanos() >= 1000000000) {
                    result.setNanos(0);
                    result.setTime(result.getTime() + 1);
                }
                return (T) result;
            } else if (value instanceof java.sql.Date) {
                java.sql.Date date = (java.sql.Date) value;
                return (T) new java.sql.Date(date.getTime() + 86400000L); // plus 1 day in milliseconds
            } else {
                Date date = (Date) value;
                return (T) new Date(date.getTime() + 1);
            }
        }

        // Handle java.time types (Temporal)
        if (value instanceof java.time.temporal.Temporal) {
            if (value instanceof Instant) {
                return (T) ((Instant) value).plusNanos(1);
            } else if (value instanceof LocalDate) {
                return (T) ((LocalDate) value).plusDays(1);
            } else if (value instanceof LocalTime) {
                return (T) ((LocalTime) value).plusNanos(1);
            } else if (value instanceof LocalDateTime) {
                return (T) ((LocalDateTime) value).plusNanos(1);
            } else if (value instanceof ZonedDateTime) {
                return (T) ((ZonedDateTime) value).plusNanos(1);
            } else if (value instanceof OffsetDateTime) {
                return (T) ((OffsetDateTime) value).plusNanos(1);
            } else if (value instanceof OffsetTime) {
                return (T) ((OffsetTime) value).plusNanos(1);
            }
        }

        // Handle Duration
        if (value instanceof Duration) {
            return (T) ((Duration) value).plusNanos(1);
        }

        // Handle Character
        if (value instanceof Character) {
            char c = (Character) value;
            return (T) Character.valueOf((char) (c + 1));
        }

        throw new UnsupportedOperationException("Cannot compute next value for type " + value.getClass());
    }
}
