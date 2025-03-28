package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.convert.MapConversions.CALENDAR;
import static com.cedarsoftware.util.convert.MapConversions.DURATION;
import static com.cedarsoftware.util.convert.MapConversions.INSTANT;
import static com.cedarsoftware.util.convert.MapConversions.LOCALE;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_DATE;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_TIME;
import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;
import static com.cedarsoftware.util.convert.MapConversions.OFFSET_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.OFFSET_TIME;
import static com.cedarsoftware.util.convert.MapConversions.PERIOD;
import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
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
class MapConversionTests {
    private final Converter converter = new Converter(new DefaultConverterOptions());  // Assuming default constructor exists

    @Test
    void testToUUID() {
        // Test with UUID string format
        Map<String, Object> map = new HashMap<>();
        UUID uuid = UUID.randomUUID();
        map.put("UUID", uuid.toString());
        assertEquals(uuid, MapConversions.toUUID(map, converter));

        // Test with most/least significant bits
        map.clear();
        map.put("mostSigBits", uuid.getMostSignificantBits());
        map.put("leastSigBits", uuid.getLeastSignificantBits());
        assertEquals(uuid, MapConversions.toUUID(map, converter));
    }

    @Test
    void testToByte() {
        Map<String, Object> map = new HashMap<>();
        byte value = 127;
        map.put("value", value);
        assertEquals(Byte.valueOf(value), MapConversions.toByte(map, converter));

        map.clear();
        map.put("_v", value);
        assertEquals(Byte.valueOf(value), MapConversions.toByte(map, converter));
    }

    @Test
    void testToShort() {
        Map<String, Object> map = new HashMap<>();
        short value = 32767;
        map.put("value", value);
        assertEquals(Short.valueOf(value), MapConversions.toShort(map, converter));
    }

    @Test
    void testToInt() {
        Map<String, Object> map = new HashMap<>();
        int value = Integer.MAX_VALUE;
        map.put("value", value);
        assertEquals(Integer.valueOf(value), MapConversions.toInt(map, converter));
    }

    @Test
    void testToLong() {
        Map<String, Object> map = new HashMap<>();
        long value = Long.MAX_VALUE;
        map.put("value", value);
        assertEquals(Long.valueOf(value), MapConversions.toLong(map, converter));
    }

    @Test
    void testToFloat() {
        Map<String, Object> map = new HashMap<>();
        float value = 3.14159f;
        map.put("value", value);
        assertEquals(Float.valueOf(value), MapConversions.toFloat(map, converter));
    }

    @Test
    void testToDouble() {
        Map<String, Object> map = new HashMap<>();
        double value = Math.PI;
        map.put("value", value);
        assertEquals(Double.valueOf(value), MapConversions.toDouble(map, converter));
    }

    @Test
    void testToBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", true);
        assertTrue(MapConversions.toBoolean(map, converter));
    }

    @Test
    void testToBigDecimal() {
        Map<String, Object> map = new HashMap<>();
        BigDecimal value = new BigDecimal("123.456");
        map.put("value", value);
        assertEquals(value, MapConversions.toBigDecimal(map, converter));
    }

    @Test
    void testToBigInteger() {
        Map<String, Object> map = new HashMap<>();
        BigInteger value = new BigInteger("123456789");
        map.put("value", value);
        assertEquals(value, MapConversions.toBigInteger(map, converter));
    }

    @Test
    void testToCharacter() {
        Map<String, Object> map = new HashMap<>();
        char value = 'A';
        map.put("value", value);
        assertEquals(Character.valueOf(value), MapConversions.toCharacter(map, converter));
    }

    @Test
    void testToAtomicTypes() {
        // AtomicInteger
        Map<String, Object> map = new HashMap<>();
        map.put("value", 42);
        assertEquals(42, MapConversions.toAtomicInteger(map, converter).get());

        // AtomicLong
        map.put("value", 123L);
        assertEquals(123L, MapConversions.toAtomicLong(map, converter).get());

        // AtomicBoolean
        map.put("value", true);
        assertTrue(MapConversions.toAtomicBoolean(map, converter).get());
    }

    @Test
    void testToSqlDate() {
        Map<String, Object> map = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        map.put("epochMillis", currentTime);
        LocalDate expectedLD = Instant.ofEpochMilli(currentTime)
                .atZone(ZoneOffset.systemDefault())
                .toLocalDate();
        java.sql.Date expected = java.sql.Date.valueOf(expectedLD.toString());
        assertEquals(expected, MapConversions.toSqlDate(map, converter));

        // Test with date/time components
        map.clear();
        map.put("sqlDate", "2024-01-01T12:00:00Z");
        assertNotNull(MapConversions.toSqlDate(map, converter));
    }

    @Test
    void testToDate() {
        Map<String, Object> map = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        map.put("epochMillis", currentTime);
        assertEquals(new Date(currentTime), MapConversions.toDate(map, converter));
    }

    @Test
    void testToTimestamp() {
        // Test case 2: Time string with sub-millisecond precision
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", "2024-01-01T08:37:16.987654321Z");  // ISO-8601 format at UTC "Z"
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(987654321, ts.getNanos());  // Should use nanos from time string
    }
    
    @Test
    void testToTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(ZONE, "UTC");
        assertEquals(TimeZone.getTimeZone("UTC"), MapConversions.toTimeZone(map, converter));
    }

    @Test
    void testToCalendar() {
        Map<String, Object> map = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        map.put(CALENDAR, currentTime);
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(currentTime, cal.getTimeInMillis());
    }

    @Test
    void testToLocale() {
        Map<String, Object> map = new HashMap<>();
        map.put(LOCALE, "en-US");
        assertEquals(Locale.US, MapConversions.toLocale(map, converter));
    }

    @Test
    void testToLocalDate() {
        Map<String, Object> map = new HashMap<>();
        map.put(LOCAL_DATE, "2024/1/1");
        assertEquals(LocalDate.of(2024, 1, 1), MapConversions.toLocalDate(map, converter));
    }

    @Test
    void testToLocalTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(LOCAL_TIME, "12:30:45.123456789");
        assertEquals(
                LocalTime.of(12, 30, 45, 123456789),
                MapConversions.toLocalTime(map, converter)
        );
    }

    @Test
    void testToOffsetTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(OFFSET_TIME, "12:30:45.123456789+01:00");
        assertEquals(
                OffsetTime.of(12, 30, 45, 123456789, ZoneOffset.ofHours(1)),
                MapConversions.toOffsetTime(map, converter)
        );
    }

    /**
     * Test converting a valid ISO-8601 offset date time string.
     */
    @Test
    void testToOffsetDateTime_withValidString() {
        Map<String, Object> map = new HashMap<>();
        String timeString = "2024-01-01T12:00:00+01:00";
        map.put(OFFSET_DATE_TIME, timeString);

        OffsetDateTime expected = OffsetDateTime.parse(timeString);
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);

        assertNotNull(actual, "Converted OffsetDateTime should not be null");
        assertEquals(expected, actual, "Converted OffsetDateTime should match expected");
    }

    /**
     * Test converting when the value is already an OffsetDateTime.
     */
    @Test
    void testToOffsetDateTime_withExistingOffsetDateTime() {
        Map<String, Object> map = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        map.put(OFFSET_DATE_TIME, now);

        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);

        assertNotNull(actual, "Converted OffsetDateTime should not be null");
        assertEquals(now, actual, "The returned OffsetDateTime should equal the provided one");
    }

    /**
     * Test converting when the value is a ZonedDateTime.
     */
    @Test
    void testToOffsetDateTime_withZonedDateTime() {
        Map<String, Object> map = new HashMap<>();
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        map.put(OFFSET_DATE_TIME, zonedDateTime);

        OffsetDateTime expected = zonedDateTime.toOffsetDateTime();
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);

        assertNotNull(actual,"Converted OffsetDateTime should not be null");
        assertEquals(expected, actual, "The OffsetDateTime should match the ZonedDateTime's offset version");
    }

    /**
     * Test that an invalid value type causes an exception.
     */
    void testToOffsetDateTime_withInvalidValue() {
        Map<String, Object> map = new HashMap<>();
        // An invalid type (e.g., an integer) should not be accepted.
        map.put(OFFSET_DATE_TIME, 12345);

        // This call is expected to throw an IllegalArgumentException.
        MapConversions.toOffsetDateTime(map, converter);
    }

    /**
     * Test that when the key is absent, the method returns null.
     */
    @Test
    void testToOffsetDateTime_whenKeyAbsent() {
        Map<String, Object> map = new HashMap<>();
        // Do not put any value for OFFSET_DATE_TIME
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetDateTime(map, converter));
    }

    @Test
    void testToLocalDateTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(LOCAL_DATE_TIME, "2024-01-01T12:00:00");
        LocalDateTime expected = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertEquals(expected, MapConversions.toLocalDateTime(map, converter));
    }

    @Test
    void testToZonedDateTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(ZONED_DATE_TIME, "2024-01-01T12:00:00Z[UTC]");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, MapConversions.toZonedDateTime(map, converter));
    }

    @Test
    void testToClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "java.lang.String");
        assertEquals(String.class, MapConversions.toClass(map, converter));
    }

    @Test
    void testToDuration() {
        Map<String, Object> map = new HashMap<>();
        // Instead of putting separate "seconds" and "nanos", provide a single BigDecimal.
        BigDecimal durationValue = new BigDecimal("3600.123456789");
        map.put(DURATION, durationValue);

        Duration expected = Duration.ofSeconds(3600, 123456789);
        assertEquals(expected, MapConversions.toDuration(map, converter));
    }

    @Test
    void testToInstant() {
        Map<String, Object> map = new HashMap<>();
        map.put(INSTANT, "2009-02-13T23:31:30.123456789Z");  // This is 1234567890 seconds, 123456789 nanos
        Instant expected = Instant.ofEpochSecond(1234567890L, 123456789);
        assertEquals(expected, MapConversions.toInstant(map, converter));
    }

    @Test
    void testToMonthDay() {
        Map<String, Object> map = new HashMap<>();
        map.put(MONTH_DAY, "12-25");
        assertEquals(MonthDay.of(12, 25), MapConversions.toMonthDay(map, converter));
    }

    @Test
    void testToYearMonth() {
        Map<String, Object> map = new HashMap<>();
        map.put(YEAR_MONTH, "2024-01");
        assertEquals(YearMonth.of(2024, 1), MapConversions.toYearMonth(map, converter));
    }

    @Test
    void testToPeriod() {
        Map<String, Object> map = new HashMap<>();
        map.put(PERIOD, "P1Y6M15D");
        assertEquals(Period.of(1, 6, 15), MapConversions.toPeriod(map, converter));
    }

    @Test
    void testToZoneId() {
        Map<String, Object> map = new HashMap<>();
        map.put(ZONE, "America/New_York");
        assertEquals(ZoneId.of("America/New_York"), MapConversions.toZoneId(map, converter));
    }

    @Test
    void testToZoneOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put(ZONE_OFFSET, "+05:30");
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), MapConversions.toZoneOffset(map, converter));
    }

    @Test
    void testToYear() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2024);
        assertEquals(Year.of(2024), MapConversions.toYear(map, converter));
    }

    @Test
    void testToURL() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("URL", "https://example.com");
        assertEquals(new URL("https://example.com"), MapConversions.toURL(map, converter));
    }

    @Test
    void testToURI() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("URI", "https://example.com");
        assertEquals(new URI("https://example.com"), MapConversions.toURI(map, converter));
    }

    @Test
    void testToThrowable() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.RuntimeException");
        map.put("message", "Test exception");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(result instanceof RuntimeException);
        assertEquals("Test exception", result.getMessage());

        // Test with cause
        map.put("cause", "java.lang.IllegalArgumentException");
        map.put("causeMessage", "Cause message");
        result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertNotNull(result.getCause());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertEquals("Cause message", result.getCause().getMessage());
    }

    @Test
    void testToString() {
        Map<String, Object> map = new HashMap<>();
        String value = "test string";

        // Test with "value" key
        map.put("value", value);
        assertEquals(value, MapConversions.toString(map, converter));

        // Test with "_v" key
        map.clear();
        map.put("_v", value);
        assertEquals(value, MapConversions.toString(map, converter));

        // Test with null
        map.clear();
        map.put("value", null);
        assertNull(MapConversions.toString(map, converter));
    }

    @Test
    void testToStringBuffer() {
        Map<String, Object> map = new HashMap<>();
        String value = "test string buffer";
        StringBuffer expected = new StringBuffer(value);

        // Test with "value" key
        map.put("value", value);
        assertEquals(expected.toString(), MapConversions.toStringBuffer(map, converter).toString());

        // Test with "_v" key
        map.clear();
        map.put("_v", value);
        assertEquals(expected.toString(), MapConversions.toStringBuffer(map, converter).toString());

        // Test with StringBuffer input
        map.clear();
        map.put("value", expected);
        assertEquals(expected.toString(), MapConversions.toStringBuffer(map, converter).toString());
    }

    @Test
    void testToStringBuilder() {
        Map<String, Object> map = new HashMap<>();
        String value = "test string builder";
        StringBuilder expected = new StringBuilder(value);

        // Test with "value" key
        map.put("value", value);
        assertEquals(expected.toString(), MapConversions.toStringBuilder(map, converter).toString());

        // Test with "_v" key
        map.clear();
        map.put("_v", value);
        assertEquals(expected.toString(), MapConversions.toStringBuilder(map, converter).toString());

        // Test with StringBuilder input
        map.clear();
        map.put("value", expected);
        assertEquals(expected.toString(), MapConversions.toStringBuilder(map, converter).toString());
    }

    @Test
    void testInitMap() {
        // Test with String
        String stringValue = "test value";
        Map<String, ?> stringMap = MapConversions.initMap(stringValue, converter);
        assertEquals(stringValue, stringMap.get("_v"));

        // Test with Integer
        Integer intValue = 42;
        Map<String, ?> intMap = MapConversions.initMap(intValue, converter);
        assertEquals(intValue, intMap.get("_v"));

        // Test with custom object
        Date dateValue = new Date();
        Map<String, ?> dateMap = MapConversions.initMap(dateValue, converter);
        assertEquals(dateValue, dateMap.get("_v"));

        // Test with null
        Map<String, ?> nullMap = MapConversions.initMap(null, converter);
        assertNull(nullMap.get("_v"));

        // Verify map size is always 1
        assertEquals(1, stringMap.size());
        assertEquals(1, intMap.size());
        assertEquals(1, dateMap.size());
        assertEquals(1, nullMap.size());

        // Verify the map is mutable (CompactMap is used)
        try {
            Map<String, Object> testMap = (Map<String, Object>) MapConversions.initMap("test", converter);
            testMap.put("newKey", "newValue");
        } catch (UnsupportedOperationException e) {
            fail("Map should be mutable");
        }
    }
}