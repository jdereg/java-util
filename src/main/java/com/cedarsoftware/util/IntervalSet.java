package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.cedarsoftware.util.EncryptionUtilities.finalizeHash;

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
 *   <li><b>Auto-merging behavior</b> - Overlapping intervals are automatically merged</li>
 *   <li><b>Intelligent interval splitting</b> - Automatically splits intervals during removal operations</li>
 *   <li><b>Rich query API</b> - Comprehensive set of methods for finding, filtering, and navigating intervals</li>
 *   <li><b>Type-safe boundaries</b> - Supports precise boundary calculations for 20+ built-in types</li>
 * </ul>
 *
 * <h2>Auto-Merging Behavior</h2>
 * <p>
 * Overlapping intervals are automatically merged into larger, non-overlapping intervals:
 * </p>
 * <pre>{@code
 *   IntervalSet<Integer> set = new IntervalSet<>();
 *   set.add(1, 5);
 *   set.add(3, 8);    // Merges with [1,5] to create [1,8]
 *   set.add(10, 15);  // Separate interval since no overlap
 *   // Result: [1,8], [10,15]
 * }</pre>
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
 *   <li>{@link #snapshot()} - Get atomic point-in-time copy of all intervals</li>
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
 * <h2>Performance Characteristics</h2>
 * <p>
 * All operations maintain O(log n) complexity:
 * </p>
 * <ul>
 *   <li><b>Add:</b> O(log n) - May require merging adjacent intervals</li>
 *   <li><b>Remove/RemoveRange:</b> O(log n) - May require splitting intervals</li>
 *   <li><b>Contains:</b> O(log n) - Single floor lookup</li>
 *   <li><b>IntervalContaining:</b> O(log n) - Single floor lookup</li>
 *   <li><b>Navigation:</b> O(log n) - Leverages NavigableMap operations</li>
 *   <li><b>Iteration:</b> O(n) - Direct map iteration, no additional overhead</li>
 * </ul>
 *
 * @param <T> the type of interval boundaries, must implement {@link Comparable}
 * @see ConcurrentSkipListMap
 * @see NavigableMap
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
            return finalizeHash(Objects.hash(start, end));
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
    private final transient ReentrantLock lock = new ReentrantLock();   // guards writes only
    private final Function<T, T> previousFunction;
    private final Function<T, T> nextFunction;

    // ──────────────────────────────────────────────────────────────────────────
    // Constructors
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new IntervalSet.
     * Overlapping intervals will be automatically merged when added.
     */
    public IntervalSet() {
        this(null, null);
    }


    /**
     * Creates a new IntervalSet with custom boundary functions.
     * <p>
     * For custom types not supported by built-in previous/next logic, provide functions to compute
     * the previous and next values. If null, falls back to built-in support. Users must provide these
     * for non-built-in types to enable proper interval splitting during removals.
     * </p>
     *
     * @param previousFunction custom function to compute previous value, or null for built-in
     * @param nextFunction custom function to compute next value, or null for built-in
     */
    public IntervalSet(Function<T, T> previousFunction, Function<T, T> nextFunction) {
        this.previousFunction = previousFunction;
        this.nextFunction = nextFunction;
    }

    /**
     * Copy constructor: creates a deep copy of the given IntervalSet, including intervals and custom functions.
     *
     * @param other the IntervalSet to copy
     */
    public IntervalSet(IntervalSet<T> other) {
        this.previousFunction = other.previousFunction;
        this.nextFunction = other.nextFunction;
        this.intervals.putAll(other.intervals);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Add the inclusive interval [start,end]. Both start and end are inclusive.
     * <p>
     * Overlapping intervals are merged automatically.
     * When merging, if an interval with the same start key already exists, a union
     * is performed using the maximum end value of both intervals.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>Adding [1,5] then [1,8] results in [1,8] (union of overlapping intervals)</li>
     *   <li>Adding [1,5] then [3,8] results in [1,8] (overlapping intervals merged)</li>
     *   <li>Adding [1,5] then [1,3] results in [1,5] (smaller interval absorbed)</li>
     * </ul>
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
            addWithMerge(start, end);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add an interval with merging logic (original behavior).
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
     * Remove the inclusive interval [start,end], splitting existing intervals as needed.
     * Both start and end are treated as inclusive bounds.
     * <p>
     * Overlapping intervals are split where needed.
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
            T existingEnd = intervals.get(start);
            if (existingEnd != null && existingEnd.equals(end)) {
                intervals.remove(start);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the inclusive range [start, end] from the set, trimming and splitting intervals as necessary.
     * <p>
     * Intervals are trimmed and split as needed.
     * Any stored interval that overlaps the removal range:
     * <ul>
     *   <li>If an interval begins before <code>start</code>, its right boundary is trimmed to <code>start</code>.</li>
     *   <li>If an interval ends after <code>end</code>, its left boundary is trimmed to <code>end</code>.</li>
     *   <li>If an interval fully contains <code>[start,end]</code>, it is split into two intervals:
     *       one covering <code>[originalStart, start]</code> and one covering <code>[end, originalEnd]</code>.</li>
     *   <li>Intervals entirely within <code>[start,end]</code> are removed.</li>
     * </ul>
     * </p>
     * <p>This operation is thread-safe: it acquires the internal write lock during mutation.</p>
     * <p>
     * <b>Performance:</b> O(log n)
     * </p>
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
            removeRangeWithSplitting(start, end);
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
     * True if the value lies in <i>any</i> closed interval [start,end].
     * Both boundaries are inclusive.
     * <p>
     * <b>Performance:</b> O(log n)
     * </p>
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
     * <p>
     * <b>Performance:</b> O(log n)
     * </p>
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

        Interval<T> containing = intervalContaining(value);
        if (containing != null) {
            return containing;
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
     * @throws IllegalArgumentException if fromKey &gt; toKey
     */
    public List<Interval<T>> getIntervalsInRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        List<Interval<T>> result = new ArrayList<>();
        for (Map.Entry<T, T> entry : intervals.subMap(fromKey, true, toKey, true).entrySet()) {
            result.add(new Interval<>(entry.getKey(), entry.getValue()));
        }
        return result;
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
        List<Interval<T>> result = new ArrayList<>();
        for (Map.Entry<T, T> entry : intervals.headMap(toKey, false).entrySet()) {
            result.add(new Interval<>(entry.getKey(), entry.getValue()));
        }
        return result;
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
        List<Interval<T>> result = new ArrayList<>();
        for (Map.Entry<T, T> entry : intervals.tailMap(fromKey, true).entrySet()) {
            result.add(new Interval<>(entry.getKey(), entry.getValue()));
        }
        return result;
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
        return new Iterator<Interval<T>>() {
            private final Iterator<Map.Entry<T, T>> entryIterator = intervals.descendingMap().entrySet().iterator();

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

    /**
     * Returns a set of all start keys in the interval set.
     * <p>
     * This method uses {@link NavigableMap#navigableKeySet()} to provide efficient key operations.
     * The returned set supports range operations and is backed by the interval set.
     * </p>
     *
     * @return a navigable set of start keys
     */
    public NavigableSet<T> keySet() {
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
    public NavigableSet<T> descendingKeySet() {
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
     * @throws IllegalArgumentException if fromKey &gt; toKey
     */
    public int removeIntervalsInKeyRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        lock.lock();
        try {
            NavigableMap<T, T> subMap = intervals.subMap(fromKey, true, toKey, true);
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

    /**
     * Returns {@code true} if this set contains no intervals.
     *
     * @return {@code true} if this set contains no intervals
     */
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
     * Returns a snapshot copy of all intervals at the time of invocation.
     * <p>
     * This method provides a consistent point-in-time view of all intervals by acquiring
     * the internal write lock and creating a complete copy of the interval set. The returned
     * list is completely independent of the original IntervalSet and will not reflect any
     * subsequent modifications.
     * </p>
     * <p>
     * This is useful when you need a stable view of intervals that won't change during
     * processing, such as for bulk operations, reporting, analysis, or when integrating
     * with code that expects stable collections.
     * </p>
     * <p>
     * The returned list contains intervals in ascending order by start key, matching
     * the iteration order of this IntervalSet.
     * </p>
     * <p>
     * <b>Thread Safety:</b> This is the only "read" method that acquires the internal
     * write lock to ensure the atomicity of the snapshot operation. All other query operations
     * (contains, navigation, iteration) are lock-free. This method locks as a convenience
     * to provide a guaranteed atomic snapshot rather than requiring users to manage
     * external synchronization themselves.
     * </p>
     * <p>
     * <b>Performance:</b> O(n) where n is the number of intervals. The method creates
     * a new ArrayList with exact capacity and copies all interval objects.
     * </p>
     * <p>
     * <b>Memory:</b> The returned list and its interval objects are completely independent
     * copies. Modifying the returned list or the original IntervalSet will not affect
     * the other.
     * </p>
     *
     * @return a new list containing copies of all intervals at the time of invocation,
     *         ordered by start key. Never returns null; returns empty list if no intervals.
     */
    public List<Interval<T>> snapshot() {
        lock.lock();
        try {
            List<Interval<T>> result = new ArrayList<>(intervals.size());
            for (Map.Entry<T, T> entry : intervals.entrySet()) {
                result.add(new Interval<>(entry.getKey(), entry.getValue()));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Compute the total covered duration across all stored intervals using a default mapping.
     * <p>
     * This overload uses a default BiFunction based on the type of T:
     * - If T is Temporal (and supports SECONDS unit, e.g., Instant, LocalDateTime, etc.), uses Duration.between(start, end).
     * - If T is Number, computes (end.longValue() - start.longValue() + 1) and maps to Duration.ofNanos(diff) (arbitrary unit).
     * - Otherwise, throws UnsupportedOperationException.
     * </p>
     * <p>
     * For Temporal types like LocalDate that do not support SECONDS, this will throw DateTimeException.
     * For custom or unsupported types, use the BiFunction overload.
     * For numeric types, the unit (nanos) is arbitrary; use custom BiFunction for specific units.
     * </p>
     *
     * @return the sum of all interval durations
     * @throws UnsupportedOperationException if no default mapping for type T
     * @throws DateTimeException if Temporal type does not support Duration.between
     * @throws ArithmeticException if numeric computation overflows long
     */
    public Duration totalDuration() {
        return totalDuration(this::defaultToDuration);
    }

    private Duration defaultToDuration(T start, T end) {
        if (start instanceof Temporal && end instanceof Temporal) {
            return Duration.between((Temporal) start, (Temporal) end);
        } else if (start instanceof Number && end instanceof Number) {
            long diff = ((Number) end).longValue() - ((Number) start).longValue() + 1;
            return Duration.ofNanos(diff);
        } else {
            throw new UnsupportedOperationException("No default duration mapping for type " + start.getClass());
        }
    }

    /**
     * Compute the total covered duration across all stored intervals.
     * <p>
     * The caller provides a <code>toDuration</code> function that maps each interval's
     * start and end values to a {@link Duration}. This method sums those Durations
     * over all intervals in key order. This method uses the underlying set's lock-free
     * iterator and may reflect concurrent modifications made during iteration.
     * </p>
     *
     * @param toDuration a function that converts an interval [start, end] to a Duration
     * @return the sum of all interval durations
     */
    public Duration totalDuration(BiFunction<T, T, Duration> toDuration) {
        Duration d = Duration.ZERO;
        for (Interval<T> interval : this) {
            d = d.plus(toDuration.apply(interval.getStart(), interval.getEnd()));
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
     * The iterator reflects the state of the IntervalSet at the time of iteration
     * and may see concurrent modifications.
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
    // Set Operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a new IntervalSet that is the union of this set and the other.
     *
     * @param other the other IntervalSet
     * @return a new IntervalSet containing all intervals from both
     */
    public IntervalSet<T> union(IntervalSet<T> other) {
        IntervalSet<T> result = new IntervalSet<>(this);
        for (Interval<T> i : other) {
            result.add(i.getStart(), i.getEnd());
        }
        return result;
    }

    /**
     * Returns a new IntervalSet that is the intersection of this set and the other.
     * <p>
     * Computes overlapping parts of intervals.
     * </p>
     *
     * @param other the other IntervalSet
     * @return a new IntervalSet containing intersecting intervals
     */
    public IntervalSet<T> intersection(IntervalSet<T> other) {
        IntervalSet<T> result = new IntervalSet<>();
        Iterator<Interval<T>> it1 = iterator();
        Iterator<Interval<T>> it2 = other.iterator();
        Interval<T> a = it1.hasNext() ? it1.next() : null;
        Interval<T> b = it2.hasNext() ? it2.next() : null;
        
        while (a != null && b != null) {
            if (a.getEnd().compareTo(b.getStart()) < 0) {
                a = it1.hasNext() ? it1.next() : null;
                continue;
            }
            if (b.getEnd().compareTo(a.getStart()) < 0) {
                b = it2.hasNext() ? it2.next() : null;
                continue;
            }
            T maxStart = greaterOf(a.getStart(), b.getStart());
            T minEnd = a.getEnd().compareTo(b.getEnd()) <= 0 ? a.getEnd() : b.getEnd();
            if (maxStart.compareTo(minEnd) <= 0) {
                result.add(maxStart, minEnd);
            }
            if (a.getEnd().compareTo(b.getEnd()) <= 0) {
                a = it1.hasNext() ? it1.next() : null;
            } else {
                b = it2.hasNext() ? it2.next() : null;
            }
        }
        return result;
    }
    
    /**
     * Returns a new IntervalSet that is the difference of this set minus the other.
     * <p>
     * Equivalent to removing all intervals from other from this set.
     * </p>
     *
     * @param other the other IntervalSet to subtract
     * @return a new IntervalSet with intervals from other removed
     */
    public IntervalSet<T> difference(IntervalSet<T> other) {
        IntervalSet<T> result = new IntervalSet<>(this);
        for (Interval<T> interval : other) {
            result.removeRange(interval.getStart(), interval.getEnd());
        }
        return result;
    }

    /**
     * Returns true if this set intersects (overlaps) with the other set.
     * <p>
     * This method efficiently determines if any interval in this set overlaps with any interval
     * in the other set. Two intervals overlap if they share at least one common value.
     * This method provides an optimized check that avoids computing the full intersection.
     * </p>
     * <p>
     * <b>Performance:</b> O(n + m) where n and m are the sizes of the two sets, 
     * using a two-pointer merge algorithm to detect overlap without building intermediate results.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>[1,5] intersects with [3,8] → true (overlap: [3,5])</li>
     *   <li>[1,5] intersects with [6,10] → false (no overlap)</li>
     *   <li>[1,5] intersects with [5,10] → true (overlap at single point: 5)</li>
     * </ul>
     * </p>
     *
     * @param other the other IntervalSet to check for overlap
     * @return true if there is any overlap between intervals in this and other sets
     * @throws NullPointerException if other is null
     */
    public boolean intersects(IntervalSet<T> other) {
        Iterator<Interval<T>> it1 = iterator();
        Iterator<Interval<T>> it2 = other.iterator();
        Interval<T> a = it1.hasNext() ? it1.next() : null;
        Interval<T> b = it2.hasNext() ? it2.next() : null;
        
        while (a != null && b != null) {
            if (a.getEnd().compareTo(b.getStart()) < 0) {
                a = it1.hasNext() ? it1.next() : null;
                continue;
            }
            if (b.getEnd().compareTo(a.getStart()) < 0) {
                b = it2.hasNext() ? it2.next() : null;
                continue;
            }
            return true;
        }
        return false;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Equality, HashCode, toString
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntervalSet<?> that = (IntervalSet<?>) o;

        Iterator<Interval<T>> thisIt = iterator();
        Iterator<? extends Interval<?>> thatIt = that.iterator();

        while (thisIt.hasNext() && thatIt.hasNext()) {
            if (!thisIt.next().equals(thatIt.next())) {
                return false;
            }
        }
        return !thisIt.hasNext() && !thatIt.hasNext();
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (Interval<T> interval : this) {
            hash = 31 * hash + interval.hashCode();
        }
        return finalizeHash(hash);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Interval<T> i : this) {
            if (!first) sb.append(", ");
            sb.append("[").append(i.getStart()).append("-").append(i.getEnd()).append("]");
            first = false;
        }
        sb.append("}");
        return sb.toString();
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
     * If a custom previousFunction is provided, uses it. Otherwise, falls back to built-in logic.
     * </p>
     *
     * @param value the value for which to compute the previous value
     * @return the previous adjacent value
     * @throws UnsupportedOperationException if no custom function and type not supported by built-in
     * @throws ArithmeticException if the operation would cause numeric underflow
     */
    @SuppressWarnings("unchecked")
    private T previousValue(T value) {
        if (previousFunction != null) {
            return previousFunction.apply(value);
        }
        // Built-in logic
        if (value instanceof Number) {
            if (value instanceof Integer) {
                int i = (Integer) value;
                if (i == Integer.MIN_VALUE) {
                    throw new ArithmeticException("Integer underflow: cannot compute previous value for Integer.MIN_VALUE");
                }
                return (T) Integer.valueOf(i - 1);
            } else if (value instanceof Long) {
                long l = (Long) value;
                if (l == Long.MIN_VALUE) {
                    throw new ArithmeticException("Long underflow: cannot compute previous value for Long.MIN_VALUE");
                }
                return (T) Long.valueOf(l - 1L);
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
                byte b = (Byte) value;
                if (b == Byte.MIN_VALUE) {
                    throw new ArithmeticException("Byte underflow: cannot compute previous value for Byte.MIN_VALUE");
                }
                return (T) Byte.valueOf((byte) (b - 1));
            } else if (value instanceof Short) {
                short s = (Short) value;
                if (s == Short.MIN_VALUE) {
                    throw new ArithmeticException("Short underflow: cannot compute previous value for Short.MIN_VALUE");
                }
                return (T) Short.valueOf((short) (s - 1));
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
            if (c == Character.MIN_VALUE) {
                throw new ArithmeticException("Character underflow: cannot compute previous value for Character.MIN_VALUE");
            }
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
     * If a custom nextFunction is provided, uses it. Otherwise, falls back to built-in logic.
     * </p>
     *
     * @param value the value for which to compute the next value
     * @return the next adjacent value
     * @throws UnsupportedOperationException if no custom function and type not supported by built-in
     * @throws ArithmeticException if the operation would cause numeric overflow
     */
    @SuppressWarnings("unchecked")
    private T nextValue(T value) {
        if (nextFunction != null) {
            return nextFunction.apply(value);
        }
        // Built-in logic
        if (value instanceof Number) {
            if (value instanceof Integer) {
                int i = (Integer) value;
                if (i == Integer.MAX_VALUE) {
                    throw new ArithmeticException("Integer overflow: cannot compute next value for Integer.MAX_VALUE");
                }
                return (T) Integer.valueOf(i + 1);
            } else if (value instanceof Long) {
                long l = (Long) value;
                if (l == Long.MAX_VALUE) {
                    throw new ArithmeticException("Long overflow: cannot compute next value for Long.MAX_VALUE");
                }
                return (T) Long.valueOf(l + 1L);
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
                byte b = (Byte) value;
                if (b == Byte.MAX_VALUE) {
                    throw new ArithmeticException("Byte overflow: cannot compute next value for Byte.MAX_VALUE");
                }
                return (T) Byte.valueOf((byte) (b + 1));
            } else if (value instanceof Short) {
                short s = (Short) value;
                if (s == Short.MAX_VALUE) {
                    throw new ArithmeticException("Short overflow: cannot compute next value for Short.MAX_VALUE");
                }
                return (T) Short.valueOf((short) (s + 1));
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
            if (c == Character.MAX_VALUE) {
                throw new ArithmeticException("Character overflow: cannot compute next value for Character.MAX_VALUE");
            }
            return (T) Character.valueOf((char) (c + 1));
        }

        throw new UnsupportedOperationException("Cannot compute next value for type " + value.getClass());
    }
}
