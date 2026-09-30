package com.cedarsoftware.util.convert;

import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asked for a class it has no conversion to, the Converter walks up that class's supertypes and uses the conversion
 * of the nearest one it has. For MyDate (extends Date) that is, say, Long -> Date, which makes a plain Date: the
 * caller asked for a MyDate and got a Date, a ClassCastException at the call site, while isConversionSupportedFor
 * said yes. The Converter now makes a MyDate -- with its no-argument constructor and setTime, else its (long)
 * constructor -- from the Date; a subclass it cannot make is refused, and isConversionSupportedFor says so.
 */
class ConverterSubtypeTargetTest {

    /** Loaded in a throwaway ClassLoader whose parent is the bootstrap loader: may depend only on java.base. */
    public static class LeakDate extends Date {
        public LeakDate() {
        }
    }

    public static class MyDate extends Date {
        public MyDate() {
        }
    }

    public static class MySqlDate extends java.sql.Date {
        public MySqlDate() {
            super(0L);
        }
    }

    public static class MyTimestamp extends Timestamp {
        public MyTimestamp() {
            super(0L);
        }
    }

    public static class MyTime extends java.sql.Time {
        public MyTime() {
            super(0L);
        }
    }

    /** Only a (long) constructor, the one every JDK Date type has. */
    public static class DateOfMillis extends Date {
        public DateOfMillis(long time) {
            super(time);
        }
    }

    public static class TimestampOfMillis extends Timestamp {
        public TimestampOfMillis(long time) {
            super(time);
        }
    }

    public static class MyCalendar extends GregorianCalendar {
        public MyCalendar() {
        }
    }

    public static class CalendarOfZone extends GregorianCalendar {
        public CalendarOfZone(TimeZone zone) {
            super(zone);
        }
    }

    public static class MyAtomicLong extends AtomicLong {
        public MyAtomicLong() {
        }
    }

    public static class AtomicLongOfValue extends AtomicLong {
        public AtomicLongOfValue(long value) {
            super(value);
        }
    }

    public static class MyAtomicInteger extends AtomicInteger {
        public MyAtomicInteger() {
        }
    }

    public static class MyAtomicBoolean extends AtomicBoolean {
        public MyAtomicBoolean() {
        }
    }

    public static class MyBitSet extends BitSet {
        public MyBitSet() {
        }
    }

    public static class MyMap extends HashMap<Object, Object> {
        public MyMap() {
        }
    }

    /** Neither a no-argument nor a (long) constructor: the Converter cannot make one. */
    public static class DateOfText extends Date {
        public DateOfText(String text) {
            super(Long.parseLong(text));
        }
    }

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /** Every kind of value that converts to a Date. */
    private static Object[] dateSources() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_700_000_000_123L);
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 1_700_000_000_123L);
        return new Object[]{1_700_000_000_123L, "2026-01-15T10:15:30.123Z", new BigDecimal("1700000000.123"),
                Instant.ofEpochMilli(1_700_000_000_123L), new Date(1_700_000_000_123L),
                new java.sql.Date(1_700_000_000_123L), new Timestamp(1_700_000_000_123L), calendar,
                ZonedDateTime.of(2026, 1, 15, 10, 15, 30, 123_000_000, ZoneOffset.UTC),
                LocalDateTime.of(2026, 1, 15, 10, 15, 30, 123_000_000), map};
    }

    @Test
    void aDateSubclassIsMadeFromEverySourceADateComesFrom() {
        for (Object source : dateSources()) {
            Date expected = converter.convert(source, Date.class);
            for (int i = 0; i < 2; i++) {               // the second time answers from the cache
                Object converted = converter.convert(source, MyDate.class);
                assertSame(MyDate.class, converted.getClass(), "from " + source.getClass().getName());
                assertEquals(expected.getTime(), ((Date) converted).getTime(), "from " + source.getClass().getName());
            }
            assertTrue(converter.isConversionSupportedFor(source.getClass(), MyDate.class));
        }
        assertTrue(converter.isSimpleTypeConversionSupported(Long.class, MyDate.class));
    }

    @Test
    void subclassesOfTheJdkDateTypesAreMadeLikeTheirParents() {
        for (Object source : dateSources()) {
            java.sql.Date sqlDate = converter.convert(source, java.sql.Date.class);
            Object mySqlDate = converter.convert(source, MySqlDate.class);
            assertSame(MySqlDate.class, mySqlDate.getClass());
            assertEquals(sqlDate.getTime(), ((Date) mySqlDate).getTime());
            assertEquals(sqlDate.toString(), mySqlDate.toString());

            Timestamp timestamp = converter.convert(source, Timestamp.class);
            Object myTimestamp = converter.convert(source, MyTimestamp.class);
            assertSame(MyTimestamp.class, myTimestamp.getClass());
            assertEquals(timestamp, myTimestamp);
        }
        // A Timestamp's nanoseconds come along
        Object precise = converter.convert("2026-01-15T10:15:30.123456789Z", MyTimestamp.class);
        assertEquals(123_456_789, ((Timestamp) precise).getNanos());

        java.sql.Time time = converter.convert("10:15:30", java.sql.Time.class);
        Object myTime = converter.convert("10:15:30", MyTime.class);
        assertSame(MyTime.class, myTime.getClass());
        assertEquals(time.getTime(), ((Date) myTime).getTime());
    }

    @Test
    void aValueThatIsAlreadyTheSubclassIsKept() {
        MyDate mine = new MyDate();
        mine.setTime(42L);
        Object converted = converter.convert(mine, MyDate.class);
        assertSame(MyDate.class, converted.getClass());
        assertEquals(42L, ((Date) converted).getTime());
        assertNull(converter.convert(null, MyDate.class));
    }

    /** Without a no-argument constructor, the (long) one makes it. */
    @Test
    void aSubclassWithOnlyAMillisConstructorIsMadeWithIt() {
        Object converted = converter.convert("2026-01-15T10:15:30.123Z", DateOfMillis.class);
        assertSame(DateOfMillis.class, converted.getClass());
        assertEquals(converter.convert("2026-01-15T10:15:30.123Z", Date.class).getTime(), ((Date) converted).getTime());
        assertTrue(converter.isSimpleTypeConversionSupported(DateOfMillis.class, DateOfMillis.class));

        Object precise = converter.convert("2026-01-15T10:15:30.123456789Z", TimestampOfMillis.class);
        assertSame(TimestampOfMillis.class, precise.getClass());
        assertEquals(converter.convert("2026-01-15T10:15:30.123456789Z", Timestamp.class), precise);
        assertEquals(123_456_789, ((Timestamp) precise).getNanos());
    }

    @Test
    void aSubclassItCannotMakeIsRefused() {
        assertFalse(converter.isConversionSupportedFor(Long.class, DateOfText.class));
        assertFalse(converter.isSimpleTypeConversionSupported(Long.class, DateOfText.class));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(1L, DateOfText.class));
        assertTrue(e.getMessage().contains("Unsupported conversion"), e.getMessage());
    }

    @Test
    void aCalendarSubclassKeepsTheInstantZoneAndRules() {
        Calendar paris = new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris"));
        paris.setTimeInMillis(1_700_000_000_123L);
        paris.setFirstDayOfWeek(Calendar.MONDAY);
        paris.setMinimalDaysInFirstWeek(4);
        paris.setLenient(false);
        Object[] sources = {1_700_000_000_123L, "2026-01-15T10:15:30.123Z", new Date(1_700_000_000_123L), paris,
                ZonedDateTime.of(2026, 1, 15, 10, 15, 30, 0, ZoneOffset.ofHours(5))};
        for (Object source : sources) {
            Calendar expected = converter.convert(source, Calendar.class);
            Object converted = converter.convert(source, MyCalendar.class);
            assertSame(MyCalendar.class, converted.getClass(), "from " + source.getClass().getName());
            Calendar made = (Calendar) converted;
            assertEquals(expected.getTimeInMillis(), made.getTimeInMillis());
            assertEquals(expected.getTimeZone().getID(), made.getTimeZone().getID());
            assertEquals(expected.getFirstDayOfWeek(), made.getFirstDayOfWeek());
            assertEquals(expected.getMinimalDaysInFirstWeek(), made.getMinimalDaysInFirstWeek());
            assertEquals(expected.isLenient(), made.isLenient());
            assertEquals(expected.get(Calendar.HOUR_OF_DAY), made.get(Calendar.HOUR_OF_DAY));
        }
        // A Calendar subclass has no constructor taking a Calendar's state: without a no-argument one it is refused
        assertFalse(converter.isConversionSupportedFor(Long.class, CalendarOfZone.class));
    }

    @Test
    void atomicSubclassesKeepTheirValue() {
        Object[] sources = {42L, 42, "42", new BigDecimal("42"), new AtomicLong(42)};
        for (Object source : sources) {
            assertAtomic(MyAtomicLong.class, 42L, converter.convert(source, MyAtomicLong.class));
            assertAtomic(AtomicLongOfValue.class, 42L, converter.convert(source, AtomicLongOfValue.class));
            assertAtomic(MyAtomicInteger.class, 42, converter.convert(source, MyAtomicInteger.class));
        }
        assertAtomic(MyAtomicBoolean.class, true, converter.convert("true", MyAtomicBoolean.class));
        assertAtomic(MyAtomicBoolean.class, false, converter.convert(0, MyAtomicBoolean.class));
    }

    private static void assertAtomic(Class<?> type, Object expected, Object converted) {
        assertSame(type, converted.getClass());
        Object value = converted instanceof AtomicLong ? ((AtomicLong) converted).get()
                : converted instanceof AtomicInteger ? ((AtomicInteger) converted).get()
                : ((AtomicBoolean) converted).get();
        assertEquals(expected, value);
    }

    @Test
    void aBitSetSubclassKeepsItsBits() {
        Object[] sources = {"101001", 41L, new int[]{0, 3, 5}, BitSet.valueOf(new long[]{41L})};
        for (Object source : sources) {
            BitSet expected = converter.convert(source, BitSet.class);
            Object converted = converter.convert(source, MyBitSet.class);
            assertSame(MyBitSet.class, converted.getClass(), "from " + source.getClass().getName());
            assertEquals(expected, converted);
        }
    }

    /**
     * A value converts to a Map as {_v: value}; asked for a particular Map type, it gets that type, made as a Map
     * converts to it (an interface gets its usual implementation), rather than a LinkedHashMap.
     */
    @Test
    void aValueConvertsToTheMapTypeAskedFor() {
        Class<?>[] types = {SortedMap.class, NavigableMap.class, TreeMap.class, ConcurrentMap.class,
                ConcurrentNavigableMap.class, ConcurrentHashMap.class, ConcurrentSkipListMap.class, Hashtable.class,
                Properties.class, WeakHashMap.class, IdentityHashMap.class, LinkedHashMap.class, MyMap.class};
        Object[] sources = {1L, 42, new BigDecimal("1.5"), true, new Date(5L)};
        for (Object source : sources) {
            Map<?, ?> expected = converter.convert(source, Map.class);
            for (Class<?> type : types) {
                Object converted = converter.convert(source, type);
                assertTrue(type.isInstance(converted), source + " to " + type.getName() + " gave "
                        + converted.getClass().getName());
                assertEquals(new HashMap<>(expected), new HashMap<>((Map<?, ?>) converted));
                assertTrue(converter.isConversionSupportedFor(source.getClass(), type));
            }
        }
        assertSame(MyMap.class, converter.convert(1L, MyMap.class).getClass());
    }

    /** A Map converted to SortedMap or NavigableMap is a TreeMap, in the source's order when it has one. */
    @Test
    void aMapConvertsToTheSortedInterfaces() {
        Map<String, Integer> plain = new LinkedHashMap<>();
        plain.put("b", 1);
        plain.put("a", 2);
        TreeMap<String, Integer> reversed = new TreeMap<>(java.util.Comparator.reverseOrder());
        reversed.putAll(plain);
        for (Class<?> type : new Class<?>[]{SortedMap.class, NavigableMap.class}) {
            Object sorted = converter.convert(plain, type);
            assertSame(TreeMap.class, sorted.getClass());
            assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(((Map<?, ?>) sorted).keySet()));
            Object ordered = converter.convert(reversed, type);
            assertEquals(java.util.Arrays.asList("b", "a"), new java.util.ArrayList<>(((Map<?, ?>) ordered).keySet()));
        }
    }

    @Test
    void aMapTypeItCannotMakeIsRefused() {
        // Abstract, and a class with no no-argument or (int) constructor
        for (Class<?> type : new Class<?>[]{java.security.Provider.class, EnumMap.class}) {
            assertFalse(converter.isConversionSupportedFor(Long.class, type), type.getName());
            assertThrows(IllegalArgumentException.class, () -> converter.convert(1L, type), type.getName());
        }
        // From a Map, the same types cannot be made either (it used to give a LinkedHashMap)
        Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        for (Class<?> type : new Class<?>[]{java.security.Provider.class, EnumMap.class}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert(map, type), type.getName());
        }
        // One it can make, whose entries do not fit (Attributes takes Attributes.Name keys only): as when a Map
        // converts to it, those entries are skipped
        Object attributes = converter.convert(1L, java.util.jar.Attributes.class);
        assertSame(java.util.jar.Attributes.class, attributes.getClass());
        assertTrue(((Map<?, ?>) attributes).isEmpty());
    }

    /** A conversion the caller added for the parent type is the one used, and makes the subclass too. */
    @Test
    void aCallersConversionToTheParentMakesTheSubclass() {
        Converter own = new Converter(new DefaultConverterOptions());
        own.addConversion((from, conv) -> new Date(((Long) from) * 2), Long.class, Date.class);
        Object converted = own.convert(21L, MyDate.class);
        assertSame(MyDate.class, converted.getClass());
        assertEquals(42L, ((Date) converted).getTime());
        assertEquals(1_700_000_000_123L, converter.convert(1_700_000_000_123L, MyDate.class).getTime());
    }

    @Test
    void theParentTypesStillConvertAsBefore() {
        assertSame(Date.class, converter.convert(1L, Date.class).getClass());
        assertSame(java.sql.Date.class, converter.convert(1L, java.sql.Date.class).getClass());
        assertSame(Timestamp.class, converter.convert(1L, Timestamp.class).getClass());
        assertEquals(new BigDecimal("5"), converter.convert(5L, BigDecimal.class));
    }

    /** The conversion kept for (Long, LeakDate) refers to LeakDate: it must not keep LeakDate's loader alive. */
    @Test
    void aSubclassOfAnotherLoaderIsNotKeptAlive() throws Exception {
        WeakReference<ClassLoader> loader = convertToADateOfAThrowawayLoader();
        for (int i = 0; i < 100 && loader.get() != null; i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        assertNull(loader.get(), "the Converter kept the subclass's class loader alive");
    }

    private WeakReference<ClassLoader> convertToADateOfAThrowawayLoader() throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
        Class<?> leakDate = Class.forName(LeakDate.class.getName(), false, loader);
        assertNotSame(LeakDate.class, leakDate);
        for (int i = 0; i < 2; i++) {
            Object converted = converter.convert(1_700_000_000_123L, leakDate);
            assertSame(leakDate, converted.getClass());
            assertTrue(converter.isConversionSupportedFor(Long.class, leakDate));
        }
        loader.close();
        return new WeakReference<>(loader);
    }
}
