package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
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
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.cedarsoftware.util.EncryptionUtilities.finalizeHash;

/**
 * Thread-safe set of half-open intervals <b>[start, end)</b> (start inclusive, end exclusive) for any Comparable type.
 *
 * <h2>Core Capabilities</h2>
 * <p>
 * IntervalSet efficiently manages collections of intervals with the following key features:
 * </p>
 * <ul>
 *   <li><b>O(log n) performance</b> - Uses {@link ConcurrentSkipListMap} for efficient lookups, insertions, and range queries</li>
 *   <li><b>Thread-safe</b> - Each read answers for the set as it stood between writes; reads take no lock
 *       unless a write overlaps them</li>
 *   <li><b>Auto-merging behavior</b> - Overlapping or adjacent intervals are automatically merged</li>
 *   <li><b>Intelligent interval splitting</b> - Automatically splits intervals during removal operations</li>
 *   <li><b>Rich query API</b> - Comprehensive set of methods for finding, filtering, and navigating intervals</li>
 * </ul>
 *
 * <h2>Auto-Merging Behavior</h2>
 * <p>
 * Overlapping or adjacent intervals are automatically merged into larger, non-overlapping intervals:
 * </p>
 * <pre>{@code
 *   IntervalSet<Integer> set = new IntervalSet<>();
 *   set.add(1, 5);
 *   set.add(3, 8);    // Merges with [1,5) to create [1,8)
 *   set.add(8, 15);   // Merges with [1,8) to create [1,15) since adjacent
 *   set.add(10, 15);  // Already covered
 *   // Result: [1,15)
 * }</pre>
 *
 * <h2>Primary Client APIs</h2>
 *
 * <h3>Basic Operations</h3>
 * <ul>
 *   <li>{@link #add(T, T)} - Add an interval [start, end)</li>
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
 * <h2>Half-Open Interval Semantics: [start, end)</h2>
 * <p>
 * IntervalSet uses half-open intervals where the start is <b>inclusive</b> and the end is <b>exclusive</b>.
 * This means interval [5, 10) includes 5, 6, 7, 8, 9 but NOT 10.
 * </p>
 * <pre>{@code
 *   IntervalSet<Integer> set = new IntervalSet<>();
 *   set.add(5, 10);  // Creates interval [5, 10)
 *   
 *   assertTrue(set.contains(5));   // ✓ start is inclusive
 *   assertTrue(set.contains(9));   // ✓ values between start and end
 *   assertFalse(set.contains(10)); // ✗ end is exclusive
 * }</pre>
 * 
 * <p>
 * Half-open intervals eliminate ambiguity in adjacent ranges and simplify interval arithmetic.
 * Adjacent intervals [1, 5) and [5, 10) can be merged cleanly into [1, 10) without overlap or gaps.
 * </p>
 * 
 * <h3>Creating Minimal Intervals (Quanta)</h3>
 * <p>
 * To create the smallest possible interval for a data type, you need to calculate the next representable value.
 * This is useful when you need single-point intervals or want to work with the minimum granularity of a type:
 * </p>
 * 
 * <h4>Floating Point Types (Float, Double)</h4>
 * <pre>{@code
 *   // Minimal interval containing exactly one floating point value
 *   double value = 5.0;
 *   double nextValue = Math.nextUp(value);  // 5.000000000000001
 *   set.add(value, nextValue);  // Creates [5.0, 5.000000000000001)
 *   
 *   // For Float:
 *   float floatValue = 5.0f;
 *   float nextFloat = Math.nextUp(floatValue);
 *   set.add(floatValue, nextFloat);
 * }</pre>
 * 
 * <h4>Temporal Types</h4>
 * <pre>{@code
 *   // java.util.Date - minimum resolution is 1 millisecond
 *   Date dateValue = new Date();
 *   Date nextDate = new Date(dateValue.getTime() + 1);  // +1 millisecond
 *   set.add(dateValue, nextDate);
 *   
 *   // java.sql.Date - minimum practical resolution is 1 day
 *   java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
 *   java.sql.Date nextSqlDate = new java.sql.Date(sqlDate.getTime() + 86400000L); // +1 day
 *   set.add(sqlDate, nextSqlDate);
 *   
 *   // java.sql.Timestamp - minimum resolution is 1 nanosecond
 *   Timestamp timestamp = new Timestamp(System.currentTimeMillis());
 *   Timestamp nextTimestamp = new Timestamp(timestamp.getTime());
 *   nextTimestamp.setNanos(timestamp.getNanos() + 1); // +1 nanosecond
 *   set.add(timestamp, nextTimestamp);
 *   
 *   // java.time.LocalDateTime - minimum resolution is 1 nanosecond
 *   LocalDateTime localDateTime = LocalDateTime.now();
 *   LocalDateTime nextLocalDateTime = localDateTime.plusNanos(1);
 *   set.add(localDateTime, nextLocalDateTime);
 *   
 *   // java.time.LocalDate - minimum resolution is 1 day
 *   LocalDate localDate = LocalDate.now();
 *   LocalDate nextLocalDate = localDate.plusDays(1);
 *   set.add(localDate, nextLocalDate);
 *   
 *   // java.time.Instant - minimum resolution is 1 nanosecond
 *   Instant instant = Instant.now();
 *   Instant nextInstant = instant.plusNanos(1);
 *   set.add(instant, nextInstant);
 * }</pre>
 * 
 * <h4>Integer Types</h4>
 * <pre>{@code
 *   // Minimal interval containing exactly one integer value
 *   int intValue = 5;
 *   int nextInt = intValue + 1;  // 6
 *   set.add(intValue, nextInt);  // Creates [5, 6) which contains only 5
 *   
 *   // Works similarly for Long, Short, Byte
 *   long longValue = 1000L;
 *   set.add(longValue, longValue + 1L);
 * }</pre>
 * 
 * <h4>Character Type</h4>
 * <pre>{@code
 *   // Minimal interval containing exactly one character
 *   char charValue = 'A';
 *   char nextChar = (char) (charValue + 1);  // 'B'
 *   set.add(charValue, nextChar);  // Creates ['A', 'B') which contains only 'A'
 * }</pre>
 *
 * <h2>Supported Types</h2>
 * <p>
 * IntervalSet supports any Comparable type. No special boundary calculations are needed due to half-open semantics.
 * </p>
 * <ul>
 *   <li><b>Numeric:</b> Byte, Short, Integer, Long, Float, Double, BigInteger, BigDecimal</li>
 *   <li><b>Character:</b> Character (Unicode-aware)</li>
 *   <li><b>Temporal:</b> Date, java.sql.Date, Time, Timestamp, Instant, LocalDate, LocalTime, LocalDateTime,
 *       ZonedDateTime, OffsetDateTime, OffsetTime, Duration</li>
 *   <li><b>Custom:</b> Any type implementing Comparable</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * IntervalSet is fully thread-safe:
 * </p>
 * <ul>
 *   <li><b>Writes</b> (add, remove, clear, ...) hold the internal {@link StampedLock} exclusively, one at a time.</li>
 *   <li><b>Reads see whole writes:</b> contains, the navigation methods, the range lists, {@link #snapshot()},
 *       copies, the set operations, equals, hashCode, toString and totalDuration each answer for the set as it
 *       stood before or after any write, never partway through one. A read runs without locking and is kept if no
 *       write overlapped it; otherwise it runs again under the read lock, or over a copy taken under it (so the
 *       bounds' own hashCode, equals, toString and a caller's function never run under the lock).</li>
 *   <li><b>Weakly consistent iteration:</b> {@link #iterator()}, {@link #descendingIterator()} and the key sets
 *       walk the live set without locking and never throw ConcurrentModificationException, so they can show part
 *       of a concurrent write. Use {@link #snapshot()} for a consistent list.</li>
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
 *   processedIds.add(1000L, 2000L);    // First batch
 *   processedIds.add(2000L, 3000L);    // Second batch - automatically merges to [1000, 3000)
 *
 *   Duration totalWork = processedIds.totalDuration((start, end) ->
 *       Duration.ofMillis(end - start));
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
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
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
            return hash(start, end);
        }

        /** {@code finalizeHash(Objects.hash(start, end))}, without the varargs array, by value (see sameValue). */
        static int hash(Object start, Object end) {
            return finalizeHash(31 * (31 + valueHash(start)) + valueHash(end));
        }

        /**
         * Equal by value, as every other IntervalSet operation compares: [1, 2.0) equals [1, 2.00). BigDecimal's
         * equals also compares the number of decimal places.
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Interval && sameValue(start, ((Interval<?>) o).start)
                    && sameValue(end, ((Interval<?>) o).end);
        }

        /**
         * Two bounds are the same value when equal, or of one class and equal by compareTo: 2.0 and 2.00. Bounds of
         * different classes are not compared (an Integer and a Long are different values here).
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        static boolean sameValue(Object a, Object b) {
            return a.equals(b) || (a.getClass() == b.getClass() && ((Comparable) a).compareTo(b) == 0);
        }

        /**
         * A hash that agrees with sameValue: a BigDecimal without its trailing zeros (2.0 and 2.00 hash alike), any
         * zero alike. A bound type whose equals disagrees with its compareTo in some other way gets sameValue's
         * equality but not a hash to match.
         */
        static int valueHash(Object value) {
            if (value instanceof BigDecimal) {
                BigDecimal number = (BigDecimal) value;
                return number.signum() == 0 ? 0 : number.stripTrailingZeros().hashCode();
            }
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "[" + start + " – " + end + ")";
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
    private final ConcurrentSkipListMap<T, T> intervals;
    // Writers hold it exclusively. Readers validate an optimistic stamp against it (see read).
    private final transient StampedLock lock = new StampedLock();

    // ──────────────────────────────────────────────────────────────────────────
    // Constructors
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new IntervalSet.
     * Overlapping or adjacent intervals will be automatically merged when added.
     */
    public IntervalSet() {
        intervals = new ConcurrentSkipListMap<>();
    }

    /**
     * Copy constructor: creates a deep copy of the given IntervalSet, including intervals.
     *
     * @param other the IntervalSet to copy
     */
    public IntervalSet(IntervalSet<T> other) {
        intervals = other.copy();
    }

    /**
     * Creates a new IntervalSet from a list of intervals.
     * <p>
     * This constructor enables JSON deserialization by allowing reconstruction of an IntervalSet
     * from a previously serialized list of intervals. The intervals are added in order, with
     * automatic merging of overlapping or adjacent intervals as per normal IntervalSet behavior.
     * </p>
     * <p>
     * This is typically used in conjunction with {@link #snapshot()} for serialization workflows:
     * </p>
     * <pre>{@code
     *   // Serialize: get snapshot for JSON serialization
     *   List<Interval<T>> intervals = intervalSet.snapshot();
     *   // ... serialize intervals to JSON ...
     *
     *   // Deserialize: reconstruct from JSON-deserialized list
     *   IntervalSet<T> restored = new IntervalSet<>(intervals);
     * }</pre>
     *
     * @param intervals the list of intervals to populate this set with
     * @throws NullPointerException if intervals list or any interval is null
     */
    public IntervalSet(List<Interval<T>> intervals) {
        Objects.requireNonNull(intervals, "intervals");
        this.intervals = new ConcurrentSkipListMap<>();
        for (Interval<T> interval : intervals) {
            Objects.requireNonNull(interval, "interval");
            add(interval.getStart(), interval.getEnd());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Add the half-open interval [start,end). Start is inclusive, end is exclusive.
     * <p>
     * Overlapping or adjacent intervals are merged automatically.
     * When merging, if an interval with the same start key already exists, a union
     * is performed using the maximum end value of both intervals.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>Adding [1,5) then [1,8) results in [1,8) (union of overlapping intervals)</li>
     *   <li>Adding [1,5) then [3,8) results in [1,8) (overlapping intervals merged)</li>
     *   <li>Adding [1,5) then [5,8) results in [1,8) (adjacent intervals merged)</li>
     *   <li>Adding [1,5) then [1,3) results in [1,5) (smaller interval absorbed)</li>
     * </ul>
     * </p>
     */
    public void add(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) <= 0) {
            return;  // Empty interval, ignore
        }

        long stamp = lock.writeLock();
        try {
            addWithMerge(start, end);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Add an interval with merging logic.
     * <p>
     * Readers do not act on the steps in between, with one exception: {@link #contains}
     * trusts a covering entry it finds mid-write. That is sound because every step here
     * stores only coverage the merged set will have: the merged interval grows toward its
     * final end, and absorbed intervals lie inside it. The merged interval is also written
     * (put) <i>before</i> any absorbed interval is removed, so coverage never dips.
     * </p>
     */
    private void addWithMerge(T start, T end) {
        T newStart = start;
        T newEnd = end;

        // 1) absorb potential lower neighbor that overlaps or touches — extend it in
        //    place (no removal) rather than remove-then-re-put
        Map.Entry<T, T> lower = intervals.lowerEntry(start);
        if (lower != null && lower.getValue().compareTo(start) >= 0) {
            newStart = lower.getKey();
            newEnd = greaterOf(lower.getValue(), end);
        } else {
            // Fold in an interval that starts exactly at 'start' so the put below
            // never shrinks it
            T existingEnd = intervals.get(start);
            if (existingEnd != null) {
                newEnd = greaterOf(existingEnd, newEnd);
            }
        }
        intervals.put(newStart, newEnd);

        // 2) absorb all following intervals that intersect or touch the new one.
        //    Extend the base entry over each absorbed interval BEFORE removing it.
        for (Iterator<Map.Entry<T, T>> it = intervals.tailMap(newStart, false).entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<T, T> e = it.next();
            if (e.getKey().compareTo(newEnd) > 0) {
                break;           // gap → stop
            }
            if (e.getValue().compareTo(newEnd) > 0) {
                newEnd = e.getValue();
                intervals.put(newStart, newEnd);
            }
            it.remove();                                           // consumed
        }
    }

    /**
     * Remove the half-open interval [start,end), splitting existing intervals as needed.
     * Start is inclusive, end is exclusive.
     * <p>
     * Overlapping intervals are split where needed.
     * </p>
     */
    public void remove(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) <= 0) {
            return;  // Empty, no-op
        }
        removeRange(start, end);
    }

    /**
     * Remove an exact interval [start, end) that matches a stored interval exactly.
     * <p>
     * This operation acts only on a single stored interval whose start and end
     * match the specified values as the set orders them ({@code compareTo}: for
     * BigDecimal, an end of 2.00 matches 2.0). No other intervals are merged, split,
     * or trimmed as a result of this call. To remove a sub-range or to split
     * existing intervals, use {@link #remove(T, T)} or {@link #removeRange(T, T)}.
     * </p>
     * <p>
     * Start is inclusive, end is exclusive.
     * If no matching interval exists, the set remains unchanged.
     * This method is thread-safe: it acquires the internal lock to perform removal
     * under concurrent access but does not affect merging or splitting logic.
     * </p>
     *
     * @param start the inclusive start key of the interval to remove (must match exactly)
     * @param end   the exclusive end key of the interval to remove (must match exactly)
     * @return {@code true} if an interval with exactly this start and end was found and removed;
     * {@code false} otherwise (no change to the set)
     */
    public boolean removeExact(T start, T end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        long stamp = lock.writeLock();
        try {
            T existingEnd = intervals.get(start);
            if (existingEnd != null && existingEnd.compareTo(end) == 0) {
                intervals.remove(start);
                return true;
            }
            return false;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Remove the half-open range [start, end) from the set, trimming and splitting intervals as necessary.
     * <p>
     * Intervals are trimmed and split as needed.
     * Any stored interval that overlaps the removal range:
     * <ul>
     *   <li>If an interval begins before <code>start</code>, its right boundary is trimmed to <code>start</code>.</li>
     *   <li>If an interval ends after <code>end</code>, its left boundary is trimmed to <code>end</code>.</li>
     *   <li>If an interval fully contains <code>[start,end)</code>, it is split into two intervals:
     *       one covering <code>[originalStart, start)</code> and one covering <code>[end, originalEnd)</code>.</li>
     *   <li>Intervals entirely within <code>[start,end)</code> are removed.</li>
     * </ul>
     * </p>
     * <p>This operation is thread-safe: it acquires the internal write lock during mutation.</p>
     * <p>
     * <b>Performance:</b> O(log n)
     * </p>
     *
     * @param start inclusive start of the range to remove
     * @param end   exclusive end of the range to remove
     * If {@code end <= start}, this method performs no operation.
     */
    public void removeRange(T start, T end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) <= 0) {
            return;
        }

        long stamp = lock.writeLock();
        try {
            removeRangeWithSplitting(start, end);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Remove range with interval splitting.
     * <p>
     * Readers do not act on the steps in between, with one exception: {@link #contains}
     * trusts a covering entry it finds mid-write. That is sound because every step here
     * stores only coverage the set already had: a retained fragment is carved from an
     * interval still stored, and trims and removals only shrink. The range being removed
     * may read as covered until the operation completes, which linearizes it at its end.
     * </p>
     */
    private void removeRangeWithSplitting(T start, T end) {
        Map.Entry<T, T> lower = intervals.lowerEntry(start);
        if (lower != null && lower.getValue().compareTo(start) > 0) {
            T lowerKey = lower.getKey();     // strictly < start, per lowerEntry contract
            T lowerValue = lower.getValue();

            // Right-hand fragment first (new key — cannot collide: lower spans 'end',
            // so no other interval can start there), then trim the left-hand interval
            // in place. No removal needed.
            if (lowerValue.compareTo(end) > 0) {
                intervals.put(end, lowerValue);
            }
            intervals.put(lowerKey, start);
        }

        for (Iterator<Map.Entry<T, T>> it = intervals.tailMap(start, true).entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<T, T> e = it.next();
            if (e.getKey().compareTo(end) >= 0) {
                break;
            }
            T entryValue = e.getValue();

            // Retained fragment first, then remove the absorbed entry
            if (entryValue.compareTo(end) > 0) {
                intervals.put(end, entryValue);
            }
            it.remove();
        }
    }

    /**
     * True if the value lies in <i>any</i> half-open interval [start,end).
     * Start is inclusive, end is exclusive.
     * <p>
     * <b>Performance:</b> O(log n)
     * </p>
     */
    public boolean contains(T value) {
        Objects.requireNonNull(value);
        long stamp = lock.tryOptimisticRead();
        if (covers(intervals.floorEntry(value), value) != null) {
            // Trusted without validation: every step of a write leaves the stored intervals covering no more
            // than the set did before or will after it (see addWithMerge, removeRangeWithSplitting), so a value
            // found covered is covered in a state the set really held. Only "not covered" can be an artifact.
            return true;
        }
        if (stamp != 0L && lock.validate(stamp)) {
            return false;
        }
        stamp = lock.readLock();
        try {
            return covers(intervals.floorEntry(value), value) != null;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Return the interval covering the specified <code>value</code>, or {@code null} if no interval contains it.
     * <p>Intervals are half-open ([start, end)), so a value v is contained
     * in an interval {@code if start <= v < end }. This method reads the set as it stood between writes
     * (see the class's Thread Safety notes) via {@link ConcurrentSkipListMap#floorEntry(Object)}.</p>
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
        return toInterval(entry(CONTAINING, value));
    }

    /**
     * Returns the <i>first</i> (lowest key) interval or {@code null}.
     */
    public Interval<T> first() {
        return toInterval(entry(FIRST, null));
    }

    /**
     * Returns the <i>last</i> (highest key) interval or {@code null}.
     */
    public Interval<T> last() {
        return toInterval(entry(LAST, null));
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
        return toInterval(entry(NEXT, value));
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
        return toInterval(entry(HIGHER, value));
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
        return toInterval(entry(FLOOR, value));
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
        return toInterval(entry(LOWER, value));
    }

    /**
     * Returns all intervals whose start keys fall within the specified range [fromKey, toKey].
     * <p>
     * This method uses {@link NavigableMap#subMap(Object, boolean, Object, boolean)} for efficient range queries.
     * The returned list is ordered by start key.
     * </p>
     *
     * @param fromKey the start of the range (inclusive)
     * @param toKey   the end of the range (inclusive)
     * @return a list of intervals within the specified range, ordered by start key
     * @throws IllegalArgumentException if {@code fromKey > toKey}
     */
    public List<Interval<T>> getIntervalsInRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        return read((map, from) -> toList(map.subMap(from, true, toKey, true), new ArrayList<>()), fromKey);
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
        return read((map, to) -> toList(map.headMap(to, false), new ArrayList<>()), toKey);
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
        return read((map, from) -> toList(map.tailMap(from, true), new ArrayList<>()), fromKey);
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
     * The returned set supports range operations and is backed by the interval set. It is read-only
     * (change the set through its own methods, which keep intervals merged and take its lock) and,
     * like {@link #iterator()}, weakly consistent.
     * </p>
     *
     * @return a read-only navigable set of start keys
     */
    public NavigableSet<T> keySet() {
        return Collections.unmodifiableNavigableSet(intervals.navigableKeySet());
    }

    /**
     * Returns a set of all start keys in descending order.
     * <p>
     * This method uses {@link NavigableMap#descendingKeySet()} for efficient reverse key iteration.
     * Like {@link #keySet()}, the returned set is a read-only, weakly consistent view.
     * </p>
     *
     * @return a read-only navigable set of start keys in descending order
     */
    public NavigableSet<T> descendingKeySet() {
        return Collections.unmodifiableNavigableSet(intervals.descendingKeySet());
    }

    /**
     * Removes all intervals whose start keys fall within the specified range [fromKey, toKey].
     * <p>
     * This method uses {@link NavigableMap#subMap(Object, boolean, Object, boolean)} for efficient bulk removal.
     * This is more efficient than calling {@link #removeExact} multiple times.
     * </p>
     *
     * @param fromKey the start of the range (inclusive)
     * @param toKey   the end of the range (inclusive)
     * @return the number of intervals removed
     * @throws IllegalArgumentException if {@code fromKey > toKey}
     */
    public int removeIntervalsInKeyRange(T fromKey, T toKey) {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (toKey.compareTo(fromKey) < 0) {
            throw new IllegalArgumentException("toKey < fromKey");
        }

        long stamp = lock.writeLock();
        try {
            NavigableMap<T, T> subMap = intervals.subMap(fromKey, true, toKey, true);
            int count = 0;
            for (Iterator<Map.Entry<T, T>> it = subMap.entrySet().iterator(); it.hasNext(); ) {
                it.next();
                it.remove();
                count++;
            }
            return count;
        } finally {
            lock.unlockWrite(stamp);
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
        long stamp = lock.writeLock();
        try {
            intervals.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns a snapshot copy of all intervals at the time of invocation.
     * <p>
     * This method provides a consistent point-in-time view of all intervals: a complete copy
     * of the interval set as it stood between writes. The returned
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
     * <b>Thread Safety:</b> The copy is taken without locking and kept if no write overlapped
     * it; otherwise it is taken again under the read lock. Unlike {@link #iterator()}, it never
     * shows part of a concurrent write.
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
     * ordered by start key. Never returns null; returns empty list if no intervals.
     */
    public List<Interval<T>> snapshot() {
        return read((map, unused) -> toList(map, new ArrayList<>(map.size())), null);
    }

    /**
     * Compute the total covered duration across all stored intervals using a default mapping.
     * <p>
     * This overload uses a default BiFunction based on the type of T:
     * - If T is Temporal (and supports SECONDS unit, e.g., Instant, LocalDateTime, etc.), uses Duration.between(start, end).
     * - If T is Number, computes end - start over the bounds' whole parts (as longValue() takes them), exactly,
     *   and maps it to Duration.ofNanos(diff) (arbitrary unit).
     * - If T is Date (or subclasses), computes Duration.ofMillis(end.getTime() - start.getTime()).
     * - If T is Character, computes (end - start) and maps to Duration.ofNanos(diff) (arbitrary unit).
     * - If T is Duration, computes end.minus(start).
     * - Otherwise, throws UnsupportedOperationException.
     * </p>
     * <p>
     * For Temporal types like LocalDate that do not support SECONDS, this will throw DateTimeException.
     * For custom or unsupported types, use the BiFunction overload.
     * For numeric types and characters, the unit (nanos) is arbitrary; use custom BiFunction for specific units.
     * </p>
     *
     * @return the sum of all interval durations
     * @throws UnsupportedOperationException if no default mapping for type T
     * @throws DateTimeException             if Temporal type does not support Duration.between
     * @throws ArithmeticException           if numeric/date-time subtraction overflows long (a BigInteger or
     *                                       BigDecimal difference past a long, a double bound past a long, NaN
     *                                       or infinite, included)
     */
    public Duration totalDuration() {
        return totalDuration(this::defaultToDuration);
    }

    private Duration defaultToDuration(T start, T end) {
        if (start instanceof Temporal && end instanceof Temporal) {
            return Duration.between((Temporal) start, (Temporal) end);
        } else if (start instanceof Number && end instanceof Number) {
            return Duration.ofNanos(wholeDifference((Number) start, (Number) end));
        } else if (start instanceof Date && end instanceof Date) {
            long startMillis = ((Date) start).getTime();
            long endMillis = ((Date) end).getTime();
            return Duration.ofMillis(Math.subtractExact(endMillis, startMillis));
        } else if (start instanceof Character && end instanceof Character) {
            int diff = ((Character) end) - ((Character) start);
            return Duration.ofNanos(diff); // Arbitrary unit for character ranges
        } else if (start instanceof Duration && end instanceof Duration) {
            Duration startDuration = (Duration) start;
            Duration endDuration = (Duration) end;
            return endDuration.minus(startDuration);
        } else {
            throw new UnsupportedOperationException("No default duration mapping for type " + start.getClass());
        }
    }

    /**
     * {@code end - start} over the bounds' whole parts (each as {@code longValue()} takes it), exactly: a difference
     * that does not fit a long, or a bound that is NaN, infinite or past a long, throws ArithmeticException instead
     * of wrapping or saturating as {@code longValue()} does.
     */
    private static long wholeDifference(Number start, Number end) {
        if (start instanceof BigInteger || start instanceof BigDecimal
                || end instanceof BigInteger || end instanceof BigDecimal) {
            return whole(end).subtract(whole(start)).longValueExact();
        }
        return Math.subtractExact(exactLong(end), exactLong(start));
    }

    private static BigInteger whole(Number n) {
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).toBigInteger();
        }
        return n instanceof BigInteger ? (BigInteger) n : BigInteger.valueOf(exactLong(n));
    }

    private static long exactLong(Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (!(d >= -0x1p63 && d < 0x1p63)) {          // NaN, infinite, or past a long
                throw new ArithmeticException("Interval bound outside the long range: " + n);
            }
        }
        return n.longValue();
    }

    /**
     * Compute the total covered duration across all stored intervals.
     * <p>
     * The caller provides a <code>toDuration</code> function that maps each interval's
     * start and end values to a {@link Duration}. This method sums those Durations
     * over all intervals in key order, as they stood between writes. If a write overlaps
     * the call, {@code toDuration} may be applied to an interval more than once (as a
     * {@link ConcurrentSkipListMap#compute} function may be), but only one pass is summed.
     * </p>
     *
     * @param toDuration a function that converts an interval [start, end) to a Duration
     * @return the sum of all interval durations
     */
    public Duration totalDuration(BiFunction<T, T, Duration> toDuration) {
        Objects.requireNonNull(toDuration, "toDuration");
        return readWhole(map -> {
            Duration d = Duration.ZERO;
            for (Map.Entry<T, T> entry : map.entrySet()) {
                d = d.plus(toDuration.apply(entry.getKey(), entry.getValue()));
            }
            return d;
        });
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
     * and may see concurrent modifications, including part of a write in progress
     * (e.g. both halves of an interval being split, or neither). Use {@link #snapshot()}
     * for a list that never does.
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
        for (Interval<T> i : other.snapshot()) {
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
        Objects.requireNonNull(other, "other");
        return readBoth(other, IntervalSet::intersect);
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
        for (Interval<T> interval : other.snapshot()) {
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
     *   <li>[1,5) intersects with [3,8) → true (overlap: [3,5))</li>
     *   <li>[1,5) intersects with [5,10) → false (adjacent but no overlap)</li>
     *   <li>[1,5) intersects with [6,10) → false (no overlap)</li>
     * </ul>
     * </p>
     *
     * @param other the other IntervalSet to check for overlap
     * @return true if there is any overlap between intervals in this and other sets
     * @throws NullPointerException if other is null
     */
    public boolean intersects(IntervalSet<T> other) {
        Objects.requireNonNull(other, "other");
        return readBoth(other, IntervalSet::overlap);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Equality, HashCode, toString
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        IntervalSet<T> that = (IntervalSet<T>) o;     // bounds compared by value (Interval.sameValue)
        return readBoth(that, IntervalSet::sameIntervals);
    }

    @Override
    public int hashCode() {
        return finalizeHash(readWhole(map -> {
            int hash = 1;                                  // List.hashCode of the intervals
            for (Map.Entry<T, T> entry : map.entrySet()) {
                hash = 31 * hash + Interval.hash(entry.getKey(), entry.getValue());
            }
            return hash;
        }));
    }

    @Override
    public String toString() {
        return readWhole(map -> {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<T, T> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("[").append(entry.getKey()).append("-").append(entry.getValue()).append(")");
                first = false;
            }
            return sb.append("}").toString();
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Runs {@code lookup} against the map as it stood between writes. A lookup over several entries, or even one
     * {@code floorEntry} (it finds a node, then reads its end), can see part of a write: while an interval is split,
     * a value in its right half could read as uncovered. So the lookup runs without locking and its answer is kept
     * if no write started or finished meanwhile (the skip list is safe to read during a write); otherwise it runs
     * again under the read lock. {@code lookup} may run twice, so it must have no side effects, and it must not
     * call back into this set.
     */
    private <R> R read(BiFunction<ConcurrentSkipListMap<T, T>, T, R> lookup, T value) {
        long stamp = lock.tryOptimisticRead();     // 0 while a write holds the lock
        if (stamp != 0L) {
            R result = lookup.apply(intervals, value);
            if (lock.validate(stamp)) {
                return result;
            }
        }
        stamp = lock.readLock();
        try {
            return lookup.apply(intervals, value);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // The single-entry lookups entry() runs. A switch, not a lambda: a lambda per reader, passed through one
    // shared method, cost JDK 8 12% to 50% per lookup.
    private static final int FLOOR = 0;
    private static final int LOWER = 1;
    private static final int HIGHER = 2;
    private static final int FIRST = 3;
    private static final int LAST = 4;
    private static final int CONTAINING = 5;
    private static final int NEXT = 6;

    /** {@link #read} for a single-entry lookup: the entry {@code lookup} names, as the set stood between writes. */
    private Map.Entry<T, T> entry(int lookup, T value) {
        long stamp = lock.tryOptimisticRead();     // 0 while a write holds the lock
        if (stamp != 0L) {
            Map.Entry<T, T> e = lookup(lookup, value);
            if (lock.validate(stamp)) {
                return e;
            }
        }
        stamp = lock.readLock();
        try {
            return lookup(lookup, value);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    private Map.Entry<T, T> lookup(int lookup, T value) {
        switch (lookup) {
            case FLOOR:
                return intervals.floorEntry(value);
            case LOWER:
                return intervals.lowerEntry(value);
            case HIGHER:
                return intervals.higherEntry(value);
            case FIRST:
                return intervals.firstEntry();
            case LAST:
                return intervals.lastEntry();
            case CONTAINING:
                return covers(intervals.floorEntry(value), value);
            default: {                              // NEXT: the interval containing value, else the next one
                Map.Entry<T, T> e = covers(intervals.floorEntry(value), value);
                return e != null ? e : intervals.ceilingEntry(value);
            }
        }
    }

    /**
     * Runs {@code f} over the whole map as it stood between writes: without locking, kept if no write started or
     * finished meanwhile, otherwise run again over a {@link #copy()}. For readers that call the bounds' own
     * methods (hashCode, toString, a caller's function), which therefore never run under this set's lock.
     */
    private <R> R readWhole(Function<NavigableMap<T, T>, R> f) {
        long stamp = lock.tryOptimisticRead();
        if (stamp != 0L) {
            try {
                R result = f.apply(intervals);
                if (lock.validate(stamp)) {
                    return result;
                }
            } catch (RuntimeException e) {
                if (lock.validate(stamp)) {
                    throw e;                       // not an artifact of a concurrent write
                }
            }
        }
        return f.apply(copy());
    }

    /**
     * {@link #readWhole} over two sets: each map as it stood between its own writes. Kept if neither set was
     * written meanwhile, otherwise run again over a {@link #copy()} of each. It never holds both sets' locks, so
     * {@code a.intersects(b)} and {@code b.intersects(a)} cannot deadlock against waiting writers.
     */
    private <R> R readBoth(IntervalSet<T> other, BiFunction<NavigableMap<T, T>, NavigableMap<T, T>, R> f) {
        long mine = lock.tryOptimisticRead();
        long theirs = other.lock.tryOptimisticRead();
        if (mine != 0L && theirs != 0L) {
            try {
                R result = f.apply(intervals, other.intervals);
                if (lock.validate(mine) && other.lock.validate(theirs)) {
                    return result;
                }
            } catch (RuntimeException e) {
                if (lock.validate(mine) && other.lock.validate(theirs)) {
                    throw e;
                }
            }
        }
        return f.apply(copy(), other.copy());
    }

    /** A copy of the map as it stood between writes, built in linear time (no comparisons). */
    private ConcurrentSkipListMap<T, T> copy() {
        return read((map, unused) -> new ConcurrentSkipListMap<>(map), null);
    }

    /** {@code floor} if its interval contains {@code value} (floor = the entry at or below it), else null. */
    private static <T extends Comparable<? super T>> Map.Entry<T, T> covers(Map.Entry<T, T> floor, T value) {
        return floor != null && floor.getValue().compareTo(value) > 0 ? floor : null;
    }

    private static <T extends Comparable<? super T>> Interval<T> toInterval(Map.Entry<T, T> e) {
        return e == null ? null : new Interval<>(e.getKey(), e.getValue());
    }

    private static <T extends Comparable<? super T>> List<Interval<T>> toList(NavigableMap<T, T> map,
                                                                              List<Interval<T>> result) {
        for (Map.Entry<T, T> entry : map.entrySet()) {
            result.add(new Interval<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /** The overlapping parts of two maps' intervals, merged into a new set (two-pointer walk, O(n + m)). */
    private static <T extends Comparable<? super T>> IntervalSet<T> intersect(NavigableMap<T, T> mine,
                                                                              NavigableMap<T, T> theirs) {
        IntervalSet<T> result = new IntervalSet<>();
        Iterator<Map.Entry<T, T>> it1 = mine.entrySet().iterator();
        Iterator<Map.Entry<T, T>> it2 = theirs.entrySet().iterator();
        Map.Entry<T, T> a = it1.hasNext() ? it1.next() : null;
        Map.Entry<T, T> b = it2.hasNext() ? it2.next() : null;

        while (a != null && b != null) {
            T aStart = a.getKey();
            T aEnd = a.getValue();
            T bStart = b.getKey();
            T bEnd = b.getValue();

            if (aEnd.compareTo(bStart) <= 0) {
                a = it1.hasNext() ? it1.next() : null;
                continue;
            }
            if (bEnd.compareTo(aStart) <= 0) {
                b = it2.hasNext() ? it2.next() : null;
                continue;
            }
            T maxStart = greaterOf(aStart, bStart);
            T minEnd = aEnd.compareTo(bEnd) <= 0 ? aEnd : bEnd;
            if (maxStart.compareTo(minEnd) < 0) {
                result.add(maxStart, minEnd);
            }
            if (aEnd.compareTo(bEnd) <= 0) {
                a = it1.hasNext() ? it1.next() : null;
            } else {
                b = it2.hasNext() ? it2.next() : null;
            }
        }
        return result;
    }

    /** True if any interval of one map overlaps one of the other (two-pointer walk, stops at the first). */
    private static <T extends Comparable<? super T>> boolean overlap(NavigableMap<T, T> mine,
                                                                     NavigableMap<T, T> theirs) {
        Iterator<Map.Entry<T, T>> it1 = mine.entrySet().iterator();
        Iterator<Map.Entry<T, T>> it2 = theirs.entrySet().iterator();
        Map.Entry<T, T> a = it1.hasNext() ? it1.next() : null;
        Map.Entry<T, T> b = it2.hasNext() ? it2.next() : null;

        while (a != null && b != null) {
            if (a.getValue().compareTo(b.getKey()) <= 0) {
                a = it1.hasNext() ? it1.next() : null;
                continue;
            }
            if (b.getValue().compareTo(a.getKey()) <= 0) {
                b = it2.hasNext() ? it2.next() : null;
                continue;
            }
            return true;
        }
        return false;
    }

    /** True if both maps hold equal intervals in the same order. */
    private static <T extends Comparable<? super T>> boolean sameIntervals(NavigableMap<T, T> mine,
                                                                           NavigableMap<T, T> theirs) {
        Iterator<Map.Entry<T, T>> it1 = mine.entrySet().iterator();
        Iterator<Map.Entry<T, T>> it2 = theirs.entrySet().iterator();
        while (it1.hasNext() && it2.hasNext()) {
            Map.Entry<T, T> a = it1.next();
            Map.Entry<T, T> b = it2.next();
            if (!Interval.sameValue(a.getKey(), b.getKey()) || !Interval.sameValue(a.getValue(), b.getValue())) {
                return false;
            }
        }
        return !it1.hasNext() && !it2.hasNext();
    }

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
}
