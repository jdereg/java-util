package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * newInstance() with positional arguments -- json-io's path for classes compiled without {@code -parameters} --
 * caches per argument shape which constructor to use, then matched every argument to a parameter again on
 * every call (by exact type, then inheritance distance, then conversion): ~140 ns per object with 3 values and
 * ~500 ns with 20. And when every argument happened to fit its position, a cached call skipped matching and
 * passed them positionally, which could DISAGREE with the first call: {@code Pair(Object a, String b)} given
 * {@code ["x", "y"]} got a=y, b=x the first time (the String goes to the String parameter) and a=x, b=y after.
 * <p>
 * The plan now records what matching decided and replays it, so every call gets what the first call got. The
 * one value-dependent step -- a conversion that throws moves the choice on to the next candidate -- is handled
 * exactly: a plan recorded with a failed conversion matches in full every time, and a replayed conversion that
 * fails sends that call back to full matching.
 */
class ClassUtilitiesConstructorPlanReplayTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @BeforeEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    static class Pair {
        final Object a;
        final String b;

        Pair(Object a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Num {
        final long n;

        Num(long n) {
            this.n = n;
        }
    }

    static class TwoNums {
        final long first;
        final long second;

        TwoNums(long first, long second) {
            this.first = first;
            this.second = second;
        }
    }

    static class WithList {
        final String s;
        final List<?> list;

        WithList(String s, List<?> list) {
            this.s = s;
            this.list = list;
        }
    }

    /** Parameter names that look synthetic: named matching skips it, as for a class built without -parameters. */
    static class Legacy {
        final String name;
        final long age;

        Legacy(String arg0, long arg1) {
            this.name = arg0;
            this.age = arg1;
        }
    }

    private Object make(Class<?> c, Object... args) {
        return ClassUtilities.newInstance(converter, c, Arrays.asList(args));
    }

    @Test
    void aCachedCallGetsTheSameArgumentsAsTheFirst() {
        for (int i = 0; i < 3; i++) {
            Pair p = (Pair) make(Pair.class, "x", "y");
            assertEquals("y", p.a, "call " + i + ": matching puts the String in the String parameter first");
            assertEquals("x", p.b, "call " + i);
        }
    }

    @Test
    void recordedConversionsReplayWithEachCallsValues() {
        Point first = (Point) make(Point.class, 1L, 2L);
        assertEquals(1, first.x);
        assertEquals(2, first.y);
        Point second = (Point) make(Point.class, 3L, 4L);
        assertEquals(3, second.x);
        assertEquals(4, second.y);
    }

    @Test
    void aReplayedConversionThatFailsFallsBackToMatching() {
        assertEquals(42L, ((Num) make(Num.class, "42")).n);
        assertEquals(0L, ((Num) make(Num.class, "nope")).n, "no value converts: the parameter's default");
        assertEquals(7L, ((Num) make(Num.class, "7")).n);
    }

    @Test
    void aPlanRecordedWithAFailedConversionIsNotReplayed() {
        // "x" will not convert, so the first parameter takes "5" -- a choice these values made, not their types
        TwoNums first = (TwoNums) make(TwoNums.class, "x", "5");
        assertEquals(5L, first.first);
        assertEquals(0L, first.second);

        // Same shape, different values: each converts, so each goes to its own parameter
        TwoNums second = (TwoNums) make(TwoNums.class, "6", "7");
        assertEquals(6L, second.first);
        assertEquals(7L, second.second);
    }

    @Test
    void aDefaultIsAFreshObjectOnEveryCall() {
        WithList first = (WithList) make(WithList.class, "a");
        WithList second = (WithList) make(WithList.class, "b");
        assertEquals("a", first.s);
        assertEquals("b", second.s);
        assertNotNull(first.list);
        assertNotSame(first.list, second.list, "a default must not be shared between objects");
    }

    @Test
    void aNullValueIsTreatedAlikeOnEveryCall() {
        // The null matches nothing, so the Object parameter gets its default -- a fresh one each time
        Pair p = (Pair) make(Pair.class, null, "y");
        Pair q = (Pair) make(Pair.class, null, "z");
        assertEquals("y", p.b);
        assertEquals("z", q.b);
        assertEquals(Object.class, p.a.getClass());
        assertEquals(Object.class, q.a.getClass());
        assertNotSame(p.a, q.a);
    }

    @Test
    void theMapPathReplaysWithEachObjectsValues() {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", "person" + i);
            m.put("age", 40L + i);
            m.put("extra", 1.5 * i);
            Legacy l = (Legacy) ClassUtilities.newInstance(converter, Legacy.class, m);
            assertEquals("person" + i, l.name);
            assertEquals(40L + i, l.age);
        }
    }

    @Test
    void emptyArgumentsStillWork() {
        Legacy l = (Legacy) ClassUtilities.newInstance(converter, Legacy.class, Collections.emptyList());
        assertNotNull(l);
        assertEquals(0L, l.age);
    }

    /** Accepts a past date or none, and rejects one within the last day -- "now" is a Date parameter's default. */
    static class Stamp {
        static final AtomicInteger calls = new AtomicInteger();
        final Date when;
        final String label;

        Stamp(Date when, String label) {
            calls.incrementAndGet();
            if (when != null && System.currentTimeMillis() - when.getTime() < 86_400_000L) {
                throw new IllegalArgumentException("too recent: " + when);
            }
            this.when = when;
            this.label = label;
        }
    }

    @Test
    void whenAConversionFailsOnACachedCallTheNullRetryStillHappens() {
        Stamp first = (Stamp) make(Stamp.class, "x", "2020-01-01");
        assertEquals("x", first.label);
        assertNotNull(first.when);

        // "garbage" will not convert, so the Date gets its default ("now"), which the constructor rejects;
        // matching then retries with null for it. Two attempts, as on any call -- not a third through
        // re-discovery, which also dropped the cached plan.
        Stamp.calls.set(0);
        Stamp second = (Stamp) make(Stamp.class, "y", "garbage");
        assertEquals("y", second.label);
        assertNull(second.when);
        assertEquals(2, Stamp.calls.get());
    }

    static class Holder {
        final Long n;
        final String s;

        Holder(Long n, String s) {
            this.n = n;
            this.s = s;
        }
    }

    /**
     * A plan is cached per class and argument classes, not per Converter, and Converters differ in which
     * conversions matching may use: an override makes a pair "not a simple conversion". A plan made under one
     * converter must not carry its conversion choices over to a call with another.
     */
    @Test
    void aCachedPlanFollowsEachCallsConverter() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getConverterOverrides().put(Converter.pair(String.class, Long.class),
                (from, conv) -> Long.valueOf((String) from));
        Converter overriding = new Converter(options);
        Converter plain = new Converter(new DefaultConverterOptions());

        Holder viaOverriding = (Holder) ClassUtilities.newInstance(overriding, Holder.class, Arrays.asList("x", "42"));
        assertNull(viaOverriding.n, "matching leaves an overridden pair alone");
        assertEquals("x", viaOverriding.s);

        Holder viaPlain = (Holder) ClassUtilities.newInstance(plain, Holder.class, Arrays.asList("x", "42"));
        assertEquals(Long.valueOf(42), viaPlain.n, "matching with this converter converts \"42\"");
        assertEquals("x", viaPlain.s);
    }
}
