package com.cedarsoftware.util;

import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * On JDK 17+ the module system keeps a JDK class's private fields from reflection, and DeepEquals skipped every field
 * it could not read: two Randoms with different seeds, a locked and an unlocked ReentrantLock, StringJoiners holding
 * different text, all compared equal -- nothing was compared. A field it cannot read now leaves the answer to the
 * class's own equals() (identity for a class without one); SKIP_UNREADABLE_FIELDS restores the old behavior. The JDK
 * holders common in object graphs -- exceptions, atomic references and arrays -- are compared by what they expose, on
 * every JDK. And a Date compared to a Timestamp differed by direction: Date.compareTo ignores the Timestamp's
 * nanoseconds (and on JDK 8 reads its time truncated to the second).
 */
class DeepEqualsUnreadableFieldsTest {

    private static boolean fieldsOfJdkClassesAreUnreadable() {
        String version = System.getProperty("java.specification.version");
        return !version.startsWith("1.") && Integer.parseInt(version) >= 17;
    }

    @Test
    void objectsWhoseFieldsCannotBeReadAreNotShownEqual() {
        assertFalse(DeepEquals.deepEquals(new Random(1), new Random(2)));
        ReentrantLock locked = new ReentrantLock();
        locked.lock();
        try {
            assertFalse(DeepEquals.deepEquals(locked, new ReentrantLock()));
        } finally {
            locked.unlock();
        }
        assertFalse(DeepEquals.deepEquals(new StringJoiner(",").add("a"), new StringJoiner(",").add("b")));

        Random same = new Random(1);
        assertTrue(DeepEquals.deepEquals(same, same));
    }

    /** Such a field inside a caller's object makes the objects unequal too, unless it is the same instance. */
    @Test
    void anUnreadableFieldInsideAnObjectCountsToo() {
        assertFalse(DeepEquals.deepEquals(new Holder(new Random(1)), new Holder(new Random(2))));
        Random shared = new Random(1);
        assertTrue(DeepEquals.deepEquals(new Holder(shared), new Holder(shared)));
    }

    /** Asked to ignore custom equals() methods, a class whose fields cannot be read is still judged by its equals(). */
    @Test
    void theClassesOwnEqualsDecidesWhenItsFieldsCannotBeRead() {
        Map<String, Object> ignoreAll = new HashMap<>();
        ignoreAll.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertTrue(DeepEquals.deepEquals(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 1),
                new HashMap<>(ignoreAll)));
        assertFalse(DeepEquals.deepEquals(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2),
                new HashMap<>(ignoreAll)));
    }

    @Test
    void theDifferenceNamesTheFieldThatCouldNotBeRead() {
        assumeTrue(fieldsOfJdkClassesAreUnreadable());
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(new Random(1), new Random(2), options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("could not be read"), diff);
        assertTrue(diff.contains("'seed'"), diff);
    }

    @Test
    void skippingUnreadableFieldsRestoresTheOldAnswer() {
        assumeTrue(fieldsOfJdkClassesAreUnreadable());
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.SKIP_UNREADABLE_FIELDS, true);
        assertTrue(DeepEquals.deepEquals(new Random(1), new Random(2), options));
        options.put(DeepEquals.SKIP_UNREADABLE_FIELDS, true);
        assertTrue(DeepEquals.deepEquals(new Holder(new Random(1)), new Holder(new Random(2)), options));
        // In a Set the elements are matched by nested comparisons, which see the option too
        options.put(DeepEquals.SKIP_UNREADABLE_FIELDS, true);
        assertTrue(DeepEquals.deepEquals(Collections.singleton(new Random(1)), Collections.singleton(new Random(2)),
                options));
    }

    /** Atomic holders have no equals(), and hide their state on JDK 17+: compared by what they hold, on every JDK. */
    @Test
    void atomicHoldersAreComparedByWhatTheyHold() {
        assertTrue(DeepEquals.deepEquals(new AtomicIntegerArray(new int[]{1, 2}), new AtomicIntegerArray(new int[]{1, 2})));
        assertFalse(DeepEquals.deepEquals(new AtomicIntegerArray(new int[]{1, 2}), new AtomicIntegerArray(new int[]{1, 3})));
        assertFalse(DeepEquals.deepEquals(new AtomicIntegerArray(2), new AtomicIntegerArray(3)));
        assertTrue(DeepEquals.deepEquals(new AtomicLongArray(new long[]{7}), new AtomicLongArray(new long[]{7})));
        assertFalse(DeepEquals.deepEquals(new AtomicLongArray(new long[]{7}), new AtomicLongArray(new long[]{8})));
        assertTrue(DeepEquals.deepEquals(new AtomicReferenceArray<>(new Object[]{"a", 1}),
                new AtomicReferenceArray<>(new Object[]{"a", 1L})));        // elements compared deeply: 1 == 1L
        assertFalse(DeepEquals.deepEquals(new AtomicReferenceArray<>(new Object[]{"a"}),
                new AtomicReferenceArray<>(new Object[]{"b"})));
        assertTrue(DeepEquals.deepEquals(new AtomicReference<>(Arrays.asList(1, 2)), new AtomicReference<>(Arrays.asList(1, 2))));
        assertFalse(DeepEquals.deepEquals(new AtomicReference<>("a"), new AtomicReference<>("b")));
    }

    /**
     * An exception's own state (message, cause, suppressed) is hidden on JDK 17+: compared by its accessors, on every
     * JDK. The stack trace is not compared -- an exception made elsewhere (a copy read back from JSON, say) has another.
     */
    @Test
    void exceptionsAreComparedByMessageCauseAndSuppressed() {
        assertTrue(DeepEquals.deepEquals(new IllegalStateException("a"), new IllegalStateException("a")));
        assertFalse(DeepEquals.deepEquals(new IllegalStateException("a"), new IllegalStateException("b")));
        assertFalse(DeepEquals.deepEquals(new IllegalStateException("a", new RuntimeException("x")),
                new IllegalStateException("a", new RuntimeException("y"))));
        assertFalse(DeepEquals.deepEquals(new IllegalStateException("a", new RuntimeException("x")),
                new IllegalStateException("a")));
        IllegalStateException withSuppressed = new IllegalStateException("a");
        withSuppressed.addSuppressed(new RuntimeException("s"));
        assertFalse(DeepEquals.deepEquals(withSuppressed, new IllegalStateException("a")));
    }

    @Test
    void aDateAndATimestampCompareTheSameEitherWay() {
        Timestamp withNanos = new Timestamp(1000);
        withNanos.setNanos(500);                    // 0.000000500 s past the second: not a whole millisecond
        Date date = new Date(withNanos.getTime());
        assertFalse(DeepEquals.deepEquals(date, withNanos));
        assertFalse(DeepEquals.deepEquals(withNanos, date));

        Timestamp wholeMillis = new Timestamp(1000);
        assertTrue(DeepEquals.deepEquals(new Date(1000), wholeMillis));
        assertTrue(DeepEquals.deepEquals(wholeMillis, new Date(1000)));
        assertTrue(DeepEquals.deepEquals(new java.sql.Date(86_400_000L), new Date(86_400_000L)));
        assertTrue(DeepEquals.deepEquals(new Date(86_400_000L), new java.sql.Date(86_400_000L)));
    }

    private static final class Holder {
        private final Random random;

        Holder(Random random) {
            this.random = random;
        }
    }
}
