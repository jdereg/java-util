package com.cedarsoftware.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.cedarsoftware.io.JsonIo;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * newInstance() with a Map of named values matches them to a constructor's parameters by name (classes compiled
 * with -parameters, records among them), converting each value to its parameter's type. When the value's type
 * cannot be converted at all -- a nested object, which reaches the constructor as a Map, for a parameter of a
 * class type -- convert() built and threw an exception (message and stack trace) that was caught at once, on every
 * object: ~0.5-0.8 us and ~1.5 KB each, and json-io reading 100 such objects ran 2.4-3.3x slower than it needs to.
 * The converter is now asked first. Which constructors match is unchanged: a conversion that is supported but fails
 * for a value is still tried and caught.
 */
class ClassUtilitiesNamedConversionTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    public static class Inner {
        public String x;

        public Inner() {
        }
    }

    public static class Outer {
        public String name;
        public long age;
        public Inner inner;

        public Outer() {
        }

        public Outer(String name, long age, Inner inner) {
            this.name = name;
            this.age = age;
            this.inner = inner;
        }
    }

    public static class Person {
        final String name;
        final long age;

        public Person(String name, long age) {
            this.name = name;
            this.age = age;
        }
    }

    public static class Units {
        final EnumSet<TimeUnit> units;

        public Units(EnumSet<TimeUnit> units) {
            this.units = units;
        }
    }

    @BeforeEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    private static Map<String, Object> outerArgs() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "y");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("name", "A");
        args.put("age", 1L);
        args.put("inner", inner);
        return args;
    }

    @Test
    void aValueOfAnUnconvertibleTypeIsRejectedWithoutAnException() {
        ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean, "needs per-thread allocation counters");
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) base;
        assumeTrue(mx.isThreadAllocatedMemorySupported() && mx.isThreadAllocatedMemoryEnabled());

        Map<String, Object> args = outerArgs();
        for (int i = 0; i < 5_000; i++) {
            ClassUtilities.newInstance(converter, Outer.class, args);
        }
        long threadId = Thread.currentThread().getId();
        int calls = 2_000;
        long before = mx.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < calls; i++) {
            ClassUtilities.newInstance(converter, Outer.class, args);
        }
        double bytesPerCall = (mx.getThreadAllocatedBytes(threadId) - before) / (double) calls;

        // Throwing and catching the conversion failure allocated ~1.5-1.9 KB per call; the object itself ~80 bytes
        assertTrue(bytesPerCall < 600, "newInstance allocated " + bytesPerCall + " bytes per call");
    }

    /**
     * By name, the three-argument constructor is passed over (a Map is not an Inner) and the no-argument one -- which
     * always matches -- is used, as before; json-io then sets the fields.
     */
    @Test
    void theSameConstructorIsChosen() {
        Outer outer = (Outer) ClassUtilities.newInstance(converter, Outer.class, outerArgs());
        assertNull(outer.name);
        assertEquals(0L, outer.age);
        assertNull(outer.inner);
    }

    @Test
    void aSupportedConversionStillHappensByName() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("name", "B");
        args.put("age", "42");
        Person p = (Person) ClassUtilities.newInstance(converter, Person.class, args);
        assertEquals("B", p.name);
        assertEquals(42L, p.age);
    }

    @Test
    void aSupportedConversionThatFailsForTheValueIsStillCaught() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("name", "C");
        args.put("age", "not a number");
        Person p = (Person) ClassUtilities.newInstance(converter, Person.class, args);
        assertNotNull(p);
        assertEquals("C", p.name);
    }

    /** The converter throws for an EnumSet target rather than answer; that must end as it always did. */
    @Test
    void aTargetTheConverterWillNotReasonAboutStillFallsBack() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("units", new Object[]{"SECONDS"});
        Units u = (Units) ClassUtilities.newInstance(converter, Units.class, args);
        assertNotNull(u);
    }

    @Test
    void jsonIoReadsANestedObjectParameter() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"name\":\"n").append(i).append("\",\"age\":").append(i)
                    .append(",\"inner\":{\"x\":\"y").append(i).append("\"}}");
        }
        Outer[] read = JsonIo.toJava(json.append(']').toString(), null).asClass(Outer[].class);
        assertEquals(20, read.length);
        assertEquals("n7", read[7].name);
        assertEquals(7L, read[7].age);
        assertEquals("y7", read[7].inner.x);
    }

    /**
     * The guard is safe only if the converter never says "unsupported" for a conversion convert() performs. Checked
     * across the value types json-io puts in a JsonObject (several values each) and every type the converter lists,
     * plus interfaces, arrays, primitives and a plain class. Guards against the two drifting apart.
     */
    @Test
    void theConverterNeverCallsUnsupportedAConversionItPerforms() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_v", "2020-01-01");
        Object[][] samples = {
                {"42", "3.5", "true", "2020-01-01", "2020-01-01T10:15:30Z", "abc", "", "SECONDS", "a", "PT1H", "USD",
                        "en-US", "0b3e1a2c-1111-2222-3333-444455556666", "12:30", "UTC", "2020-01"},
                {42L, 0L, 1L, 1700000000000L}, {3.5d, 0.0d, 1.0d}, {Boolean.TRUE, Boolean.FALSE},
                {new BigDecimal("1.50")}, {new BigInteger("12")}, {7}, {'c'},
                {map, new LinkedHashMap<>()},
                {new Object[]{1L, 2L}, new Object[]{"a"}, new Object[0]},
                {new ArrayList<>(Arrays.asList(1L, 2L)), new ArrayList<>()},
        };
        Set<Class<?>> targets = new LinkedHashSet<>(Converter.allSupportedConversions().keySet());
        for (Set<Class<?>> t : Converter.allSupportedConversions().values()) {
            targets.addAll(t);
        }
        targets.addAll(Arrays.asList(Object.class, Inner.class, TimeUnit.class, CharSequence.class, Comparable.class,
                java.io.Serializable.class, Number.class, List.class, Set.class, Collection.class, Map.class,
                SortedMap.class, String[].class, int[].class, long[].class, Object[].class, Long[].class, int.class,
                long.class, double.class, boolean.class, char.class, EnumSet.class, ArrayList.class));

        List<String> wronglyUnsupported = new ArrayList<>();
        for (Object[] group : samples) {
            Class<?> source = group[0].getClass();
            for (Class<?> target : targets) {
                boolean supported;
                try {
                    supported = converter.isConversionSupportedFor(source, target);
                } catch (Exception e) {
                    supported = false;      // the matcher catches this too, and rejects the constructor
                }
                if (supported) {
                    continue;
                }
                for (Object value : group) {
                    if (target.isInstance(value)) {
                        continue;           // passed through before any conversion
                    }
                    try {
                        converter.convert(value, target);
                        wronglyUnsupported.add(source.getSimpleName() + " -> " + target.getName());
                        break;
                    } catch (Exception expected) {
                        // unsupported, and convert() agrees
                    }
                }
            }
        }
        assertTrue(wronglyUnsupported.isEmpty(), "called unsupported, but convert() performs: " + wronglyUnsupported);
    }
}
