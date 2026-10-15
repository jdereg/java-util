package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XMLGregorianCalendar and javax.xml.datatype.Duration conversions (4.116.0) -- the types JAXB generates for
 * xs:dateTime, xs:date and xs:duration. A value's text is its XML Schema lexical form; its Map form holds that text,
 * or the fields one by one as json-io wrote them through 4.112.0.
 * <p>
 * Through 4.115.0 the Converter had no conversion for either type, and json-io wrote them by reflecting on the JDK's
 * implementation classes in com.sun.org.apache.xerces.internal.jaxp.datatype -- which java-util 4.113.0 blocks, and
 * which JDK 17+ does not open. These pairs move into ConverterEverythingTest once its test-scope json-io handles them.
 */
class XmlDatatypeConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());
    private static final DatatypeFactory FACTORY = newFactory();

    private static DatatypeFactory newFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every field as the value holds it, unset fields included: the oracle for "the same value". */
    private static String fields(XMLGregorianCalendar c) {
        return c.getEon() + "|" + c.getYear() + "|" + c.getMonth() + "|" + c.getDay() + "|" + c.getHour() + "|"
                + c.getMinute() + "|" + c.getSecond() + "|" + c.getFractionalSecond() + "|" + c.getTimezone();
    }

    private static String fields(Duration d) {
        return d.getSign() + "|" + d.getField(DatatypeConstants.YEARS) + "|" + d.getField(DatatypeConstants.MONTHS)
                + "|" + d.getField(DatatypeConstants.DAYS) + "|" + d.getField(DatatypeConstants.HOURS) + "|"
                + d.getField(DatatypeConstants.MINUTES) + "|" + d.getField(DatatypeConstants.SECONDS);
    }

    // Each of the XML Schema date/time types, in the form toXMLFormat() writes it
    @ParameterizedTest
    @ValueSource(strings = {
            "2026-10-15T10:20:30.123-04:00",    // xs:dateTime with a fraction and a zone
            "2026-10-15T10:20:30Z",
            "2026-10-15T10:20:30",              // no zone
            "2020-06-28",                       // xs:date
            "2020-06-28+05:30",
            "10:20:30.5",                       // xs:time
            "2026-10",                          // xs:gYearMonth
            "2026",                             // xs:gYear
            "--10-15",                          // xs:gMonthDay
            "---15",                            // xs:gDay
            "--10",                             // xs:gMonth
            "-0044-03-15",                      // a year before 1
            "12345678901-01-01T00:00:00Z"       // a year past 999,999,999: the eon
    })
    void anXmlGregorianCalendarIsItsLexicalForm(String text) {
        XMLGregorianCalendar expected = FACTORY.newXMLGregorianCalendar(text);
        XMLGregorianCalendar read = converter.convert(text, XMLGregorianCalendar.class);
        assertEquals(fields(expected), fields(read), text);
        assertEquals(text, converter.convert(read, String.class));

        Map<?, ?> map = converter.convert(read, Map.class);
        assertEquals(1, map.size());
        assertEquals(text, map.get("xmlGregorianCalendar"));
        assertEquals(fields(expected), fields(converter.convert(map, XMLGregorianCalendar.class)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"P1DT2H", "-P1Y2M", "PT0.5S", "P1Y2M3DT4H5M6.789S", "PT0S", "P0D", "P400000000000D"})
    void aDurationIsItsLexicalForm(String text) {
        Duration expected = FACTORY.newDuration(text);
        Duration read = converter.convert(text, Duration.class);
        assertEquals(fields(expected), fields(read), text);
        assertEquals(text, converter.convert(read, String.class));

        Map<?, ?> map = converter.convert(read, Map.class);
        assertEquals(1, map.size());
        assertEquals(text, map.get("xmlDuration"));
        assertEquals(fields(expected), fields(converter.convert(map, Duration.class)));
    }

    /**
     * 2,000 calendars from a fixed seed, each field set or unset at random (only the combinations XML Schema has a
     * type for), come back from their text and their Map with every field as it was.
     */
    @Test
    void randomCalendarsComeBackFieldForField() {
        Random random = new Random(20261015L);
        int checked = 0;
        for (int i = 0; i < 2000; i++) {
            XMLGregorianCalendar c = FACTORY.newXMLGregorianCalendar();
            int kind = random.nextInt(4);           // 0 dateTime, 1 date, 2 time, 3 gYearMonth
            if (kind != 2) {
                c.setYear(random.nextBoolean() ? 1 + random.nextInt(9999) : -(1 + random.nextInt(9999)));
                c.setMonth(1 + random.nextInt(12));
                if (kind != 3) {
                    c.setDay(1 + random.nextInt(28));
                }
            }
            if (kind == 0 || kind == 2) {
                c.setTime(random.nextInt(24), random.nextInt(60), random.nextInt(60));
                if (random.nextBoolean()) {
                    c.setFractionalSecond(new BigDecimal("0." + (1 + random.nextInt(999))));
                }
            }
            if (random.nextBoolean()) {
                c.setTimezone((random.nextInt(57) - 28) * 30);       // -14:00 .. +14:00 in half hours
            }
            String text = converter.convert(c, String.class);
            assertEquals(fields(c), fields(converter.convert(text, XMLGregorianCalendar.class)), text);
            assertEquals(fields(c), fields(converter.convert(converter.convert(c, Map.class), XMLGregorianCalendar.class)));
            checked++;
        }
        assertEquals(2000, checked);
    }

    @Test
    void randomDurationsComeBackFieldForField() {
        Random random = new Random(20261016L);
        for (int i = 0; i < 2000; i++) {
            BigInteger[] parts = new BigInteger[5];
            boolean any = false;
            for (int p = 0; p < parts.length; p++) {
                if (random.nextInt(3) == 0) {
                    parts[p] = BigInteger.valueOf(random.nextInt(100));
                    any = true;
                }
            }
            BigDecimal seconds = !any || random.nextBoolean() ? new BigDecimal(random.nextInt(60) + "." + random.nextInt(10)) : null;
            Duration d = FACTORY.newDuration(random.nextBoolean(), parts[0], parts[1], parts[2], parts[3], parts[4], seconds);
            String text = converter.convert(d, String.class);
            assertEquals(fields(d), fields(converter.convert(text, Duration.class)), text);
            assertEquals(fields(d), fields(converter.convert(converter.convert(d, Map.class), Duration.class)));
        }
    }

    @Test
    void theMapMayHoldTheTextUnderValueOrV() {
        String text = "2026-10-15T10:20:30.123-04:00";
        for (String key : new String[]{"xmlGregorianCalendar", "value", "_v"}) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, text);
            assertEquals(text, converter.convert(converter.convert(map, XMLGregorianCalendar.class), String.class), key);
        }
        for (String key : new String[]{"xmlDuration", "value", "_v"}) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, "P1DT2H");
            assertEquals("P1DT2H", converter.convert(converter.convert(map, Duration.class), String.class), key);
        }
        Map<String, Object> neither = new HashMap<>();
        neither.put("something", "else");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(neither, XMLGregorianCalendar.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(neither, Duration.class));
    }

    /** The fields as json-io 4.112.0 wrote them on JDK 8 and 11 -- these maps are its output, field for field. */
    @Test
    void theFieldsAsJsonIo4112WroteThemRead() {
        Map<String, Object> dateTime = new LinkedHashMap<>();
        dateTime.put("eon", null);
        dateTime.put("year", 2026L);
        dateTime.put("month", 10L);
        dateTime.put("day", 15L);
        dateTime.put("timezone", -240L);
        dateTime.put("hour", 10L);
        dateTime.put("minute", 20L);
        dateTime.put("second", 30L);
        dateTime.put("fractionalSecond", "0.123");
        assertEquals("2026-10-15T10:20:30.123-04:00",
                converter.convert(converter.convert(dateTime, XMLGregorianCalendar.class), String.class));

        Map<String, Object> date = new LinkedHashMap<>();       // unset is Integer.MIN_VALUE, as the class holds it
        date.put("eon", null);
        date.put("year", 2020L);
        date.put("month", 6L);
        date.put("day", 28L);
        date.put("timezone", (long) Integer.MIN_VALUE);
        date.put("hour", (long) Integer.MIN_VALUE);
        date.put("minute", (long) Integer.MIN_VALUE);
        date.put("second", (long) Integer.MIN_VALUE);
        date.put("fractionalSecond", null);
        assertEquals("2020-06-28", converter.convert(converter.convert(date, XMLGregorianCalendar.class), String.class));

        Map<String, Object> eon = new LinkedHashMap<>(date);    // year = eon + year
        eon.put("eon", "12000000000");
        eon.put("year", 345678901L);
        assertEquals("12345678901-06-28", converter.convert(converter.convert(eon, XMLGregorianCalendar.class), String.class));

        Map<String, Object> duration = new LinkedHashMap<>();
        duration.put("signum", 1L);
        duration.put("years", null);
        duration.put("months", null);
        duration.put("days", "1");
        duration.put("hours", "2");
        duration.put("minutes", null);
        duration.put("seconds", null);
        assertEquals("P1DT2H", converter.convert(converter.convert(duration, Duration.class), String.class));
        duration.put("signum", -1L);
        assertEquals("-P1DT2H", converter.convert(converter.convert(duration, Duration.class), String.class));
    }

    /** A field the Map leaves out is unset, as a null one is: the value has only the fields the Map gives. */
    @Test
    void theFieldFormNeedsOnlyTheFieldsItHas() {
        Map<String, Object> date = new HashMap<>();
        date.put("year", 2020L);
        date.put("month", 6L);
        date.put("day", 28L);
        assertEquals("2020-06-28", converter.convert(converter.convert(date, XMLGregorianCalendar.class), String.class));

        Map<String, Object> time = new HashMap<>();
        time.put("hour", 10L);
        time.put("minute", 20L);
        time.put("second", 30L);
        time.put("fractionalSecond", "0.5");
        assertEquals("10:20:30.5", converter.convert(converter.convert(time, XMLGregorianCalendar.class), String.class));

        Map<String, Object> duration = new HashMap<>();
        duration.put("minutes", "5");
        duration.put("seconds", "6.789");
        assertEquals("PT5M6.789S", converter.convert(converter.convert(duration, Duration.class), String.class));
        duration.remove("minutes");
        assertEquals("PT6.789S", converter.convert(converter.convert(duration, Duration.class), String.class));
    }

    /** Text, when the Map has it, is the value; fields beside it are not read. Blank text is no text. */
    @Test
    void textBesideFieldsWins() {
        Map<String, Object> both = new HashMap<>();
        both.put("value", "2026-10-15");
        both.put("year", 1999L);
        both.put("month", 1L);
        both.put("day", 2L);
        assertEquals("2026-10-15", converter.convert(converter.convert(both, XMLGregorianCalendar.class), String.class));
        both.put("value", " ");
        assertEquals("1999-01-02", converter.convert(converter.convert(both, XMLGregorianCalendar.class), String.class));

        Map<String, Object> duration = new HashMap<>();
        duration.put("xmlDuration", "P3D");
        duration.put("days", "9");
        assertEquals("P3D", converter.convert(converter.convert(duration, Duration.class), String.class));
        duration.put("xmlDuration", "");
        assertEquals("P9D", converter.convert(converter.convert(duration, Duration.class), String.class));
    }

    /** On JDK 17+ json-io 4.112.0 could not reach the fields and wrote zeros: that is no date, and says so. */
    @Test
    void theZerosJsonIo4112WroteOnJdk17AreRefused() {
        Map<String, Object> zeros = new LinkedHashMap<>();
        zeros.put("eon", null);
        for (String key : new String[]{"year", "month", "day", "timezone", "hour", "minute", "second"}) {
            zeros.put(key, 0L);
        }
        zeros.put("fractionalSecond", null);
        assertThrows(IllegalArgumentException.class, () -> converter.convert(zeros, XMLGregorianCalendar.class));

        Map<String, Object> noParts = new LinkedHashMap<>();
        noParts.put("signum", 0L);
        for (String key : new String[]{"years", "months", "days", "hours", "minutes", "seconds"}) {
            noParts.put(key, null);
        }
        assertThrows(IllegalArgumentException.class, () -> converter.convert(noParts, Duration.class));
    }

    @Test
    void textThatIsNoneIsRefusedAndBlankIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert("2020-13-45", XMLGregorianCalendar.class));
        assertTrue(e.getMessage().contains("2020-13-45"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> converter.convert("1 day", Duration.class));
        assertTrue(e.getMessage().contains("1 day"), e.getMessage());

        assertNull(converter.convert("", XMLGregorianCalendar.class));
        assertNull(converter.convert("   ", Duration.class));
        assertNull(converter.convert(null, XMLGregorianCalendar.class));
        assertNull(converter.convert(null, Duration.class));
        assertEquals("2020-06-28", converter.convert(converter.convert(" 2020-06-28 ", XMLGregorianCalendar.class), String.class));
        assertEquals("P1D", converter.convert(converter.convert(" P1D ", Duration.class), String.class));
    }

    /** An XMLGregorianCalendar has setters: converting one to its own type gives a copy, as Calendar does. */
    @Test
    void aCalendarConvertedToItsTypeIsACopy() {
        XMLGregorianCalendar original = FACTORY.newXMLGregorianCalendar("2026-10-15T10:20:30Z");
        XMLGregorianCalendar copy = converter.convert(original, XMLGregorianCalendar.class);
        assertNotSame(original, copy);
        assertEquals(fields(original), fields(copy));
        copy.setYear(1999);
        assertEquals(2026, original.getYear());

        Duration duration = FACTORY.newDuration("P1D");
        assertEquals(duration, converter.convert(duration, Duration.class));
    }

    /**
     * javax.xml.datatype.Duration shares its simple name with java.time.Duration: the name-keyed listing and the
     * messages give its full name, as they do java.sql.Date's, so the two never stand for each other.
     */
    @Test
    void theTwoDurationsAreNamedApart() {
        Map<String, java.util.Set<String>> byName = Converter.getSupportedConversions();
        assertEquals(Converter.allSupportedConversions().size(), byName.size());
        assertTrue(byName.get("javax.xml.datatype.Duration").contains("String"));
        assertTrue(byName.get("Duration").contains("Long"));            // java.time.Duration's own
        assertTrue(byName.get("XMLGregorianCalendar").contains("String"));

        Map<String, Object> neither = new HashMap<>();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(neither, Duration.class));
        assertTrue(e.getMessage().contains("'javax.xml.datatype.Duration'"), e.getMessage());
    }

    /** json-io asks about the runtime class, the JDK's internal one: it is supported through its public type. */
    @Test
    void theRuntimeClassesAreSupportedThroughTheirPublicTypes() {
        Class<?> calendarClass = FACTORY.newXMLGregorianCalendar("2020-06-28").getClass();
        Class<?> durationClass = FACTORY.newDuration("P1D").getClass();
        for (Class<?> type : new Class<?>[]{XMLGregorianCalendar.class, calendarClass, Duration.class, durationClass}) {
            assertTrue(converter.isSimpleTypeConversionSupported(type, type), type.getName());
            assertTrue(converter.isConversionSupportedFor(type, String.class), type.getName());
            assertTrue(converter.isConversionSupportedFor(String.class, type), type.getName());
            assertTrue(converter.isConversionSupportedFor(Map.class, type), type.getName());
        }
        assertEquals("2020-06-28", converter.convert(converter.convert("2020-06-28", calendarClass), String.class));
        assertEquals("P1D", converter.convert(converter.convert("P1D", durationClass), String.class));
    }
}
