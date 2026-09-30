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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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
