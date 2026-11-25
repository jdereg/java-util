package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code new SafeSimpleDateFormat(pattern)} formats and parses as {@code new SimpleDateFormat(pattern)} does. The
 * JDK's own formatter, built beside it under the same defaults, is the oracle throughout: nothing here states what a
 * date should look like.
 * <p>
 * From 4.1.0 through 4.118.0 it did not, in four ways this class pins. It built its per-thread formatters from the
 * default locale's {@code DateFormatSymbols}, which makes a {@code SimpleDateFormat} take its era and its month
 * names from those symbols and not from its calendar: with a Thai default locale the year 2567 was printed with the
 * Christian era's name, with the Japanese imperial calendar the era was left out, and a month on its own came out
 * in the form used beside a day ("июля", not "июль") in the twenty-odd languages that have two. It read
 * {@code Locale.getDefault()} where the JDK reads the FORMAT category. {@code setCalendar} kept the calendar's zone
 * and leniency and dropped its rules, {@code setNumberFormat} and {@code setTimeZone} did nothing when the new
 * value resembled the old one, and a pattern that is not one was accepted by the constructor.
 * <p>
 * Last, what a formatter shared by threads has to get right that one thread's SimpleDateFormat does not: a zone
 * name read by one thread's parse, a two-digit year, and what a pool thread is left holding.
 */
class SafeSimpleDateFormatAgainstJdkTest {
    private static final String[] PATTERNS = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "M/d/yyyy HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "EEE, dd-MMM-yyyy hh:mm:ss z", "MMMM yyyy", "LLLL yyyy", "d MMMM yyyy",
            "MMMM", "MMM", "LLL", "EEEE", "E", "G yyyy", "GGGG y", "yy-MM-dd", "h:mm a", "yyyy-MM-dd HH:mm:ss zzzz",
            "yyyy-MM-dd'T'HH:mm:ssXXX", "YYYY-'W'ww-u", "D", "F", "W", "k K", "MMM d, yyyy", "dd.MM.yyyy", "ss.SSS",
            "HH,mm"};
    private static final long[] TIMES = {0L, 1700000000000L, 951782400000L, 1711846800000L, -2208988800000L,
            1720000000000L, -62198755200000L, 4102444800000L};
    private static final String[] ZONES = {"America/New_York", "Asia/Kolkata"};
    // every language found to differ, and a spread of the rest: other calendars, other digits, right to left
    private static final String[] LOCALES = {"en-US", "en-GB", "de-DE", "fr-FR", "ru-RU", "pl-PL", "cs-CZ", "fi-FI",
            "el-GR", "uk-UA", "ca-ES", "th-TH", "th-TH-u-nu-thai", "ja-JP", "ja-JP-u-ca-japanese", "ar-EG", "ar-SA",
            "fa-IR", "hi-IN", "zh-CN", "ko-KR", "tr-TR", "he-IL", "pt-BR", "es-ES", "it-IT", "nl-NL", "sv-SE", "hu-HU",
            "lt-LT", "sk-SK", "hr-HR", "sl-SI", "bg-BG", "vi-VN", "hy-AM", "lv-LV", "ro-RO", "da-DK", "nb-NO"};
    private static final Date JULY_3_2024 = new Date(1720000000000L);       // 2024-07-03T09:46:40Z

    private Locale savedLocale;
    private Locale savedDisplay;
    private Locale savedFormat;
    private TimeZone savedZone;

    @BeforeEach
    void rememberDefaults() {
        savedLocale = Locale.getDefault();
        savedDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        savedFormat = Locale.getDefault(Locale.Category.FORMAT);
        savedZone = TimeZone.getDefault();
        usa();
    }

    @AfterEach
    void restoreDefaults() {
        Locale.setDefault(savedLocale);
        Locale.setDefault(Locale.Category.DISPLAY, savedDisplay);
        Locale.setDefault(Locale.Category.FORMAT, savedFormat);
        TimeZone.setDefault(savedZone);
        clearCaches();
    }

    private static void usa() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        clearCaches();
    }

    private static void clearCaches() {
        SafeSimpleDateFormat.clearThreadLocalCache();
        SafeSimpleDateFormat.clearStaticThreadLocalCache();
    }

    // ---------------------------------------------------------------------------------------------------------
    // as the JDK, under every default
    // ---------------------------------------------------------------------------------------------------------

    @Test
    void formatsAndParsesAsTheJdkUnderEveryDefaultLocaleAndZone() {
        List<Locale> locales = new ArrayList<>();
        for (String tag : LOCALES) {
            locales.add(Locale.forLanguageTag(tag));
        }
        locales.add(new Locale("th", "TH", "TH"));          // the two the JDK has had since before language tags
        locales.add(new Locale("ja", "JP", "JP"));
        List<String> differences = new ArrayList<>();
        int cases = 0;
        for (Locale locale : locales) {
            Locale.setDefault(locale);
            for (String zone : ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                clearCaches();
                cases += compareWithTheJdk(locale.toLanguageTag() + " " + zone, differences);
            }
        }
        assertTrue(differences.isEmpty(), differences.size() + " of " + cases + " differ from SimpleDateFormat, e.g. "
                + differences.subList(0, Math.min(6, differences.size())));
        assertEquals(locales.size() * ZONES.length * PATTERNS.length * TIMES.length, cases);
    }

    /** Where the two differ, the JDK formats with the FORMAT locale: English menus, German dates. */
    @Test
    void theLocaleUsedIsTheFormatLocale() {
        List<String> differences = new ArrayList<>();
        int cases = 0;
        for (String tag : new String[]{"de-DE", "fr-FR", "ru-RU", "th-TH", "ar-EG", "en-GB"}) {
            Locale.setDefault(Locale.US);
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag(tag));
            clearCaches();
            cases += compareWithTheJdk("format locale " + tag, differences);
        }
        assertTrue(differences.isEmpty(), differences.size() + " of " + cases + " differ from SimpleDateFormat, e.g. "
                + differences.subList(0, Math.min(6, differences.size())));

        Locale.setDefault(Locale.US);
        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
        clearCaches();
        assertEquals(new SimpleDateFormat("EEEE, d. MMMM yyyy").format(JULY_3_2024),
                new SafeSimpleDateFormat("EEEE, d. MMMM yyyy").format(JULY_3_2024));
        assertTrue(new SafeSimpleDateFormat("MMMM").format(JULY_3_2024).startsWith("Jul"));
        assertEquals("Juli", new SafeSimpleDateFormat("MMMM").format(JULY_3_2024));
        // and a week starts on Monday there: the calendar is the format locale's too
        assertEquals(new SimpleDateFormat("YYYY-'W'ww-u").format(new Date(951782400000L)),
                new SafeSimpleDateFormat("YYYY-'W'ww-u").format(new Date(951782400000L)));
        assertEquals(Calendar.MONDAY, new SafeSimpleDateFormat("yyyy").getCalendar().getFirstDayOfWeek());
    }

    /** Both ways in, for every pattern and time; returns how many were compared. */
    private static int compareWithTheJdk(String where, List<String> differences) {
        int cases = 0;
        for (String pattern : PATTERNS) {
            SimpleDateFormat jdk = new SimpleDateFormat(pattern);
            DateFormat safe = new SafeSimpleDateFormat(pattern);
            DateFormat legacy = SafeSimpleDateFormat.getDateFormat(pattern);
            if (!jdk.getTimeZone().equals(safe.getTimeZone()) || jdk.isLenient() != safe.isLenient()
                    || jdk.getCalendar().getClass() != safe.getCalendar().getClass()
                    || !jdk.getNumberFormat().equals(safe.getNumberFormat())) {
                differences.add(where + " " + pattern + ": zone, leniency, calendar or number format");
            }
            for (long time : TIMES) {
                cases++;
                Date date = new Date(time);
                String expected = jdk.format(date);
                String actual = safe.format(date);
                if (!expected.equals(actual) || !expected.equals(legacy.format(date))) {
                    differences.add(where + " " + pattern + ": JDK [" + expected + "] safe [" + actual
                            + "] getDateFormat [" + legacy.format(date) + "]");
                    continue;
                }
                ParsePosition jdkAt = new ParsePosition(0);
                ParsePosition safeAt = new ParsePosition(0);
                ParsePosition legacyAt = new ParsePosition(0);
                Date jdkDate = jdk.parse(expected, jdkAt);
                Date safeDate = safe.parse(expected, safeAt);
                Date legacyDate = legacy.parse(expected, legacyAt);
                boolean same = (jdkDate == null ? safeDate == null && legacyDate == null
                        : jdkDate.equals(safeDate) && jdkDate.equals(legacyDate))
                        && jdkAt.getIndex() == safeAt.getIndex() && jdkAt.getErrorIndex() == safeAt.getErrorIndex()
                        && jdkAt.getIndex() == legacyAt.getIndex();
                if (!same) {
                    differences.add(where + " " + pattern + " parse [" + expected + "]: JDK " + jdkDate + " at "
                            + jdkAt.getIndex() + ", safe " + safeDate + " at " + safeAt.getIndex());
                }
            }
        }
        return cases;
    }

    /** The cases the comparison found, stated: the era is the calendar's, and a month on its own stands alone. */
    @Test
    void anEraIsTheCalendarsAndAMonthAloneStandsAlone() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Locale.setDefault(Locale.forLanguageTag("th-TH"));
        clearCaches();
        String thai = new SafeSimpleDateFormat("G yyyy-MM-dd").format(JULY_3_2024);
        assertEquals(new SimpleDateFormat("G yyyy-MM-dd").format(JULY_3_2024), thai);
        assertTrue(thai.endsWith(" 2567-07-03") && thai.startsWith("พ"), thai);      // Buddhist era, year 2567
        assertEquals(JULY_3_2024.getTime() / 86400000L,
                new SafeSimpleDateFormat("G yyyy-MM-dd").parse(thai).getTime() / 86400000L);

        Locale.setDefault(Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        clearCaches();
        String reiwa = new SafeSimpleDateFormat("GGGG y").format(JULY_3_2024);
        assertEquals(new SimpleDateFormat("GGGG y").format(JULY_3_2024), reiwa);
        assertEquals("令和 6", reiwa);                                            // Reiwa 6, not " 6"

        for (String tag : new String[]{"ru-RU", "pl-PL", "cs-CZ", "fi-FI", "uk-UA", "hr-HR", "lt-LT"}) {
            Locale.setDefault(Locale.forLanguageTag(tag));
            clearCaches();
            String alone = new SafeSimpleDateFormat("MMMM").format(JULY_3_2024);
            String besideADay = new SafeSimpleDateFormat("d MMMM").format(JULY_3_2024).substring(2);
            assertEquals(new SimpleDateFormat("MMMM").format(JULY_3_2024), alone, tag);
            assertEquals(new SimpleDateFormat("d MMMM").format(JULY_3_2024).substring(2), besideADay, tag);
            assertNotEquals(alone.toLowerCase(Locale.ROOT), besideADay.toLowerCase(Locale.ROOT),
                    tag + " has two forms");
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // the constructor and the setters
    // ---------------------------------------------------------------------------------------------------------

    @Test
    void aPatternThatIsNotOneIsRefusedByTheConstructor() {
        IllegalArgumentException jdk = assertThrows(IllegalArgumentException.class,
                () -> new SimpleDateFormat("yyyy-MM-dd b"));
        IllegalArgumentException safe = assertThrows(IllegalArgumentException.class,
                () -> new SafeSimpleDateFormat("yyyy-MM-dd b"));
        assertEquals(jdk.getMessage(), safe.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new SafeSimpleDateFormat("yyyy 'unclosed"));
        assertThrows(IllegalArgumentException.class, () -> SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd b"));
        assertThrows(NullPointerException.class, () -> new SafeSimpleDateFormat(null));
    }

    @Test
    void setCalendarKeepsTheCalendarsRules() throws Exception {
        // ISO weeks: 2021-01-01, a Friday, is in week 53 of 2020; by the US rules it is week 1 of 2021
        GregorianCalendar iso = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        iso.setFirstDayOfWeek(Calendar.MONDAY);
        iso.setMinimalDaysInFirstWeek(4);
        Date newYear2021 = new Date(1609459200000L);
        SimpleDateFormat jdk = new SimpleDateFormat("YYYY-'W'ww");
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("YYYY-'W'ww");
        assertEquals("2021-W01", safe.format(newYear2021));
        jdk.setCalendar((Calendar) iso.clone());
        safe.setCalendar((Calendar) iso.clone());
        assertEquals("2020-W53", jdk.format(newYear2021));
        assertEquals("2020-W53", safe.format(newYear2021));
        assertEquals(jdk.parse("2020-W53"), safe.parse("2020-W53"));

        // no Julian calendar before 1582: the instant java.time calls 0001-01-01
        GregorianCalendar proleptic = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        proleptic.setGregorianChange(new Date(Long.MIN_VALUE));
        Date yearOne = new Date(-62135596800000L);
        jdk = new SimpleDateFormat("yyyy-MM-dd");
        safe = new SafeSimpleDateFormat("yyyy-MM-dd");
        assertEquals("0001-01-03", safe.format(yearOne), "the default calendar is Julian that far back");
        jdk.setCalendar((Calendar) proleptic.clone());
        safe.setCalendar((Calendar) proleptic.clone());
        assertEquals("0001-01-01", jdk.format(yearOne));
        assertEquals("0001-01-01", safe.format(yearOne));
        assertEquals(yearOne, safe.parse("0001-01-01"));

        // another kind of calendar altogether, and the zone and leniency it carries
        Calendar buddhist = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"),
                Locale.forLanguageTag("th-TH"));
        buddhist.setLenient(false);
        jdk = new SimpleDateFormat("G yyyy-MM-dd HH:mm");
        safe = new SafeSimpleDateFormat("G yyyy-MM-dd HH:mm");
        jdk.setCalendar((Calendar) buddhist.clone());
        safe.setCalendar((Calendar) buddhist.clone());
        assertEquals(jdk.format(JULY_3_2024), safe.format(JULY_3_2024));
        assertTrue(safe.format(JULY_3_2024).contains("2567-07-03 16:46"), safe.format(JULY_3_2024));
        assertFalse(safe.isLenient());
        assertEquals("Asia/Bangkok", safe.getTimeZone().getID());

        // a later setTimeZone and setLenient win over the calendar's, as in the JDK
        jdk.setTimeZone(TimeZone.getTimeZone("UTC"));
        safe.setTimeZone(TimeZone.getTimeZone("UTC"));
        jdk.setLenient(true);
        safe.setLenient(true);
        assertEquals(jdk.format(JULY_3_2024), safe.format(JULY_3_2024));
        assertTrue(safe.format(JULY_3_2024).contains("2567-07-03 09:46"));

        // and two formatters that differ only in those rules are not equal, so they do not share a formatter
        SafeSimpleDateFormat plain = new SafeSimpleDateFormat("YYYY-'W'ww");
        SafeSimpleDateFormat isoWeeks = new SafeSimpleDateFormat("YYYY-'W'ww");
        isoWeeks.setCalendar((Calendar) iso.clone());
        plain.setCalendar(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US));
        assertNotEquals(plain, isoWeeks);
        assertEquals("2021-W01", plain.format(newYear2021));
        assertEquals("2020-W53", isoWeeks.format(newYear2021));
    }

    @Test
    void setNumberFormatUsesTheFormatItIsGiven() throws Exception {
        // same class, grouping and digit counts as the default: only the digits differ
        DecimalFormat arabicIndic = (DecimalFormat) NumberFormat.getIntegerInstance(Locale.US);
        arabicIndic.setGroupingUsed(false);
        DecimalFormatSymbols symbols = arabicIndic.getDecimalFormatSymbols();
        symbols.setZeroDigit('٠');
        arabicIndic.setDecimalFormatSymbols(symbols);

        SimpleDateFormat jdk = new SimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd");
        jdk.setNumberFormat(arabicIndic);
        safe.setNumberFormat(arabicIndic);
        assertEquals("٢٠٢٤-٠٧-٠٣", jdk.format(JULY_3_2024));
        assertEquals(jdk.format(JULY_3_2024), safe.format(JULY_3_2024));
        assertEquals(jdk.parse(jdk.format(JULY_3_2024)), safe.parse(jdk.format(JULY_3_2024)));
        assertNotEquals(new SafeSimpleDateFormat("yyyy-MM-dd"), safe);
        assertEquals("2024-07-03", new SafeSimpleDateFormat("yyyy-MM-dd").format(JULY_3_2024));
    }

    @Test
    void twoZonesWithOneIdAreTwoZones() {
        TimeZone plusOne = new SimpleTimeZone(3_600_000, "Plant");
        TimeZone plusFive = new SimpleTimeZone(5 * 3_600_000, "Plant");
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("HH:mm");
        safe.setTimeZone(plusOne);
        assertEquals("10:46", safe.format(JULY_3_2024));
        safe.setTimeZone(plusFive);
        assertEquals("14:46", safe.format(JULY_3_2024));

        SafeSimpleDateFormat one = new SafeSimpleDateFormat("HH:mm");
        SafeSimpleDateFormat five = new SafeSimpleDateFormat("HH:mm");
        one.setTimeZone(plusOne);
        five.setTimeZone(plusFive);
        assertNotEquals(one, five);
        assertEquals("10:46 14:46", one.format(JULY_3_2024) + " " + five.format(JULY_3_2024));
    }

    /** What a shared formatter has to do differently: it keeps a copy, so nobody changes it from outside. */
    @Test
    void aSetterTakesACopyOfWhatItIsGiven() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm 'w'ww");
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        TimeZone zone = new SimpleTimeZone(0, "Mine");
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);
        numbers.setGroupingUsed(false);
        safe.setCalendar(calendar);
        safe.setTimeZone(zone);
        safe.setNumberFormat(numbers);
        String before = safe.format(JULY_3_2024);
        assertEquals("2024-07-03 09:46 w27", before);

        calendar.setMinimalDaysInFirstWeek(7);
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        zone.setRawOffset(9 * 3_600_000);
        numbers.setMinimumIntegerDigits(9);
        safe.getNumberFormat().setMinimumIntegerDigits(9);
        safe.getTimeZone().setRawOffset(9 * 3_600_000);
        safe.getCalendar().setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        safe.getCalendar().setLenient(false);
        assertEquals(before, safe.format(JULY_3_2024));
        SafeSimpleDateFormat.clearThreadLocalCache();           // a formatter built now is built from the copies
        assertEquals(before, safe.format(JULY_3_2024));

        // and the getters say what is in force, not what was done to their last answer
        assertEquals(0, safe.getTimeZone().getRawOffset());
        assertEquals("Mine", safe.getTimeZone().getID());
        assertTrue(safe.isLenient());
        assertEquals(1, safe.getNumberFormat().getMinimumIntegerDigits());
        assertEquals("Mine", safe.getCalendar().getTimeZone().getID());
        assertTrue(safe.getCalendar().isLenient());
        assertEquals(1, safe.getCalendar().getMinimalDaysInFirstWeek());
        safe.setLenient(false);
        assertFalse(safe.isLenient());
        assertFalse(safe.getCalendar().isLenient());
        assertNull(safe.parse("2024-02-30 09:46 w09", new ParsePosition(0)));
    }

    @Test
    void theFieldsAreReportedAsTheJdkReportsThem() {
        SimpleDateFormat jdk = new SimpleDateFormat("EEEE, MMMM d, yyyy");
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("EEEE, MMMM d, yyyy");
        for (int field : new int[]{DateFormat.MONTH_FIELD, DateFormat.DATE_FIELD, DateFormat.YEAR_FIELD,
                DateFormat.ERA_FIELD}) {
            FieldPosition jdkAt = new FieldPosition(field);
            FieldPosition safeAt = new FieldPosition(field);
            StringBuffer jdkText = jdk.format(JULY_3_2024, new StringBuffer("on "), jdkAt);
            StringBuffer safeText = safe.format(JULY_3_2024, new StringBuffer("on "), safeAt);
            assertEquals(jdkText.toString(), safeText.toString());
            assertEquals(jdkAt.getBeginIndex(), safeAt.getBeginIndex(), "field " + field);
            assertEquals(jdkAt.getEndIndex(), safeAt.getEndIndex(), "field " + field);
        }
        assertEquals("on Wednesday, July 3, 2024",
                safe.format(JULY_3_2024, new StringBuffer("on "), new FieldPosition(0)).toString());
        assertEquals("Wednesday, July 3, 2024", safe.format(JULY_3_2024));
        assertEquals("Wednesday, July 3, 2024", safe.format((Object) JULY_3_2024));
        assertEquals(jdk.format(Long.valueOf(0L)), safe.format(Long.valueOf(0L)));

        AttributedCharacterIterator jdkFields = jdk.formatToCharacterIterator(JULY_3_2024);
        AttributedCharacterIterator safeFields = safe.formatToCharacterIterator(JULY_3_2024);
        assertEquals(jdkFields.getAllAttributeKeys(), safeFields.getAllAttributeKeys());
        assertEquals(4, safeFields.getAllAttributeKeys().size());
        safeFields.setIndex(11);                                // inside "July"
        assertNotNull(safeFields.getAttribute(DateFormat.Field.MONTH));
        assertEquals(11, safeFields.getRunStart(DateFormat.Field.MONTH));
        assertEquals(15, safeFields.getRunLimit(DateFormat.Field.MONTH));
    }

    // ---------------------------------------------------------------------------------------------------------
    // what a thread keeps, and what a stream holds
    // ---------------------------------------------------------------------------------------------------------

    /**
     * A thread keeps a formatter for each of the last 16 configurations it used, so one that takes 16 in turn
     * builds each once. It kept 4, and the fifth in rotation cost a new SimpleDateFormat on every call: 2,049 ns
     * a format where four cost 151 (JDK 8). Formatters with one configuration share one.
     */
    @Test
    void aThreadKeepsAFormatterForEachOfSixteenConfigurations() throws Exception {
        SafeSimpleDateFormat[] formatters = new SafeSimpleDateFormat[16];
        for (int i = 0; i < formatters.length; i++) {
            formatters[i] = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS".substring(0, 4 + i));
        }
        Map<?, ?> kept = formattersOfThisThread();
        List<Object> first = new ArrayList<>(kept.values());
        assertEquals(16, first.size());
        for (int round = 0; round < 5; round++) {
            for (SafeSimpleDateFormat formatter : formatters) {
                formatter.format(JULY_3_2024);
            }
        }
        List<Object> after = new ArrayList<>(kept.values());
        assertEquals(16, after.size());
        for (Object formatter : first) {
            assertTrue(after.stream().anyMatch(each -> each == formatter), "one was built again");
        }

        // the same configuration, the same formatter; and a setter that changes nothing changes nothing
        SafeSimpleDateFormat twin = new SafeSimpleDateFormat("yyyy");
        twin.setLenient(true);
        twin.setTimeZone(TimeZone.getDefault());
        twin.format(JULY_3_2024);
        assertEquals(16, kept.size());
        assertEquals(formatters[0], twin);
        assertEquals(formatters[0].hashCode(), twin.hashCode());

        // going back and forth between two settings builds each once
        twin.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        twin.format(JULY_3_2024);
        twin.setTimeZone(TimeZone.getDefault());
        twin.format(JULY_3_2024);
        twin.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        twin.format(JULY_3_2024);
        assertEquals(16, kept.size(), "never more than sixteen");
        assertNotEquals(formatters[0], twin);
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> formattersOfThisThread() throws Exception {
        Field field = SafeSimpleDateFormat.class.getDeclaredField("TL");
        field.setAccessible(true);
        return ((ThreadLocal<Map<?, ?>>) field.get(null)).get();
    }

    /**
     * getDateFormat() hands out the thread's own SimpleDateFormat, and its Javadoc has always said that a change
     * made to it lasts. From 4.1.0 it lasted until the thread had asked for 16 other patterns, when the formatter
     * was dropped and the next call built a new one, in the default zone again. A formatter that was changed is
     * kept; one that was not is still dropped, so patterns without end do not fill memory.
     */
    @Test
    @SuppressWarnings("unchecked")
    void aFormatterChangedThroughGetDateFormatIsKept() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        clearCaches();
        SimpleDateFormat changed = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        changed.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat lenientOff = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        lenientOff.setLenient(false);
        SimpleDateFormat untouched = SafeSimpleDateFormat.getDateFormat("HH:mm");
        assertEquals("2024-07-03 09:46", changed.format(JULY_3_2024));

        for (int i = 0; i < 300; i++) {
            assertEquals("2024 n" + i, SafeSimpleDateFormat.getDateFormat("yyyy 'n" + i + "'").format(JULY_3_2024));
        }

        assertTrue(changed == SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm"), "the changed one was replaced");
        assertEquals("2024-07-03 09:46", SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm").format(JULY_3_2024));
        assertTrue(lenientOff == SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd"));
        assertFalse(SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd").isLenient());
        assertTrue(untouched != SafeSimpleDateFormat.getDateFormat("HH:mm"), "one nobody changed is built again");
        assertEquals("05:46", SafeSimpleDateFormat.getDateFormat("HH:mm").format(JULY_3_2024));

        Field recent = SafeSimpleDateFormat.class.getDeclaredField("STATIC_TL");
        Field kept = SafeSimpleDateFormat.class.getDeclaredField("STATIC_CHANGED");
        recent.setAccessible(true);
        kept.setAccessible(true);
        assertTrue(((ThreadLocal<Map<?, ?>>) recent.get(null)).get().size() <= 16);
        assertEquals(2, ((ThreadLocal<Map<?, ?>>) kept.get(null)).get().size(), "the two that were changed");

        SafeSimpleDateFormat.clearStaticThreadLocalCache();
        assertTrue(changed != SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm"));
        assertEquals("2024-07-03 05:46", SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm").format(JULY_3_2024));
    }

    /**
     * Configurations without end do not fill memory: the table that lets equal formatters share one is emptied
     * when it is full, and the formatters made before and after go on working.
     */
    @Test
    void moreConfigurationsThanTheTableHoldsStillWork() throws Exception {
        Field field = SafeSimpleDateFormat.class.getDeclaredField("CONFIGURATIONS");
        field.setAccessible(true);
        Map<?, ?> configurations = (Map<?, ?>) field.get(null);
        SafeSimpleDateFormat early = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        for (int i = 0; i < 700; i++) {
            SafeSimpleDateFormat each = new SafeSimpleDateFormat("yyyy-MM-dd 'n" + i + "'");
            assertEquals("2024-07-03 n" + i, each.format(JULY_3_2024));
            assertTrue(configurations.size() <= 256, "holds " + configurations.size());
        }
        SafeSimpleDateFormat late = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        assertEquals("2024-07-03T09:46", early.format(JULY_3_2024));
        assertEquals("2024-07-03T09:46", late.format(JULY_3_2024));
        assertEquals(early, late);
        assertEquals(early.hashCode(), late.hashCode());
        assertTrue(formattersOfThisThread().size() <= 16);
    }

    // ---------------------------------------------------------------------------------------------------------
    // what only a shared formatter can get wrong
    // ---------------------------------------------------------------------------------------------------------

    /**
     * A SimpleDateFormat that reads a zone name becomes a formatter for that zone. The thread's SimpleDateFormat
     * did too, so after one parse of "... PST" that thread printed Pacific time, for this formatter and for any
     * other with the same configuration, while every other thread printed Eastern.
     */
    @Test
    void aZoneNameInTheTextDoesNotChangeTheZoneOfTheFormatter() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        clearCaches();
        Date noonUtc = new Date(1764072000000L);                // 2025-11-25T12:00:00Z
        SimpleDateFormat jdk = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm z");
        SafeSimpleDateFormat twin = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm z");
        assertEquals("2025-11-25 07:00 EST", safe.format(noonUtc));

        assertEquals(jdk.parse("2025-11-25 04:00 PST"), safe.parse("2025-11-25 04:00 PST"));
        assertEquals(noonUtc, safe.parse("2025-11-25 04:00 PST"));
        assertEquals("2025-11-25 04:00 PST", jdk.format(noonUtc), "the JDK's own is a Pacific formatter now");
        assertEquals("2025-11-25 07:00 EST", safe.format(noonUtc));
        assertEquals("2025-11-25 07:00 EST", twin.format(noonUtc));
        assertEquals("America/New_York", safe.getTimeZone().getID());
        assertEquals(noonUtc, safe.parse("2025-11-25 12:00 UTC"));
        assertEquals("2025-11-25 07:00 EST", safe.format(noonUtc));

        // a zone that was set is the one put back, not the default
        safe.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        assertEquals(noonUtc, safe.parse("2025-11-25 04:00 PST"));
        assertEquals("2025-11-25 17:30 IST", safe.format(noonUtc));
    }

    /**
     * A two-digit year is read against a hundred years that start 80 years before "now", and each thread's
     * formatter took its own "now" when the thread first needed it. A moment 80 years ago to within the time
     * between two threads' first uses was last century on one thread and next century on the other.
     */
    @Test
    void aTwoDigitYearIsTheSameYearOnEveryThread() throws Exception {
        Calendar eightyYearsAgo = Calendar.getInstance();
        eightyYearsAgo.add(Calendar.YEAR, -80);
        eightyYearsAgo.add(Calendar.MILLISECOND, 250);          // just inside the hundred years, for now
        final int year = eightyYearsAgo.get(Calendar.YEAR);
        final String text = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS").format(eightyYearsAgo.getTime());
        final SafeSimpleDateFormat safe = new SafeSimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
        final Calendar read = Calendar.getInstance();
        read.setTime(safe.parse(text));
        assertEquals(year, read.get(Calendar.YEAR), text);

        Thread.sleep(600);                                      // the moment is outside them for a formatter built now
        final AtomicInteger elsewhere = new AtomicInteger();
        final AtomicInteger legacyElsewhere = new AtomicInteger();
        Thread later = new Thread(() -> {
            try {
                Calendar there = Calendar.getInstance();
                there.setTime(safe.parse(text));
                elsewhere.set(there.get(Calendar.YEAR));
                there.setTime(SafeSimpleDateFormat.getDateFormat("MM/dd/yy HH:mm:ss.SSS").parse(text));
                legacyElsewhere.set(there.get(Calendar.YEAR));
            } catch (Exception e) {
                elsewhere.set(-1);
            }
        });
        later.start();
        later.join();
        assertEquals(year, elsewhere.get(), text + " on a thread that first used the formatter later");
        assertEquals(year, legacyElsewhere.get(), text + " through getDateFormat() on that thread");

        SafeSimpleDateFormat.clearThreadLocalCache();           // and on this thread, when it has to build another
        read.setTime(safe.parse(text));
        assertEquals(year, read.get(Calendar.YEAR), text + " after this thread built its formatter again");

        // the hundred years the caller chooses are still the caller's
        safe.set2DigitYearStart(new Date(JULY_3_2024.getTime()));
        read.setTime(safe.parse(text));
        assertEquals(year + 100, read.get(Calendar.YEAR));
    }

    /**
     * What a thread keeps after formatting is JDK classes and nothing of this library, so a thread that outlives
     * an application does not keep the application's class loader. Here the library is loaded a second time by a
     * loader of its own, used on this thread through both ways in, and let go.
     */
    @Test
    void aThreadThatFormattedDoesNotKeepTheClassLoader() throws Exception {
        URL classes = SafeSimpleDateFormat.class.getProtectionDomain().getCodeSource().getLocation();
        URLClassLoader loader = new URLClassLoader(new URL[]{classes}, null);
        Class<?> another = loader.loadClass(SafeSimpleDateFormat.class.getName());
        assertTrue(another != SafeSimpleDateFormat.class && another.getClassLoader() == loader);
        Object formatter = another.getConstructor(String.class).newInstance("yyyy-MM-dd HH:mm");
        assertEquals("2024-07-03 09:46", ((DateFormat) formatter).format(JULY_3_2024));
        assertEquals(JULY_3_2024.getTime() / 60_000,
                ((DateFormat) formatter).parse("2024-07-03 09:46").getTime() / 60_000);
        ((DateFormat) formatter).setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        assertEquals("2024-07-03 15:16", ((DateFormat) formatter).format(JULY_3_2024));
        Object legacy = another.getMethod("getDateFormat", String.class).invoke(null, "yyyy/MM/dd");
        assertEquals("2024/07/03", ((DateFormat) legacy).format(JULY_3_2024));
        ((DateFormat) legacy).setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));     // changed: the thread keeps it
        for (int i = 0; i < 20; i++) {
            another.getMethod("getDateFormat", String.class).invoke(null, "yyyy/MM/dd 'n" + i + "'");
        }
        assertTrue(legacy == another.getMethod("getDateFormat", String.class).invoke(null, "yyyy/MM/dd"));

        WeakReference<ClassLoader> released = new WeakReference<>(loader);
        formatter = null;
        legacy = null;
        another = null;
        loader.close();
        loader = null;
        for (int i = 0; i < 50 && released.get() != null; i++) {
            System.gc();
            Thread.sleep(40);
        }
        assertNull(released.get(), "the loader is still reachable from this thread's thread-locals");
    }

    /**
     * A formatter written by 4.118.0 (on JDK 8, 45 KB: it carried the name of every time zone) reads here and
     * formats as it did. One written now is a few KB, most of it the time zone's rules.
     */
    @Test
    void aFormatterSerializedBy4118ReadsAndOneWrittenNowIsSmall() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        clearCaches();
        DateFormat revived;
        try (InputStream in = SafeSimpleDateFormatAgainstJdkTest.class
                .getResourceAsStream("/safe-simple-date-format-4.118.0.ser.gz");
             ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(in))) {
            revived = (DateFormat) ois.readObject();
        }
        // as it was configured there: "EEE yyyy/MM/dd HH:mm z", Asia/Kolkata, not lenient
        assertEquals("Wed 2024/07/03 15:16 IST", revived.format(JULY_3_2024));
        assertEquals("EEE yyyy/MM/dd HH:mm z", revived.toString());
        assertFalse(revived.isLenient());
        assertEquals("Asia/Kolkata", revived.getTimeZone().getID());
        assertNull(revived.parse("Wed 2024/07/35 15:16 IST", new ParsePosition(0)));

        SafeSimpleDateFormat now = new SafeSimpleDateFormat("EEE yyyy/MM/dd HH:mm z");
        now.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        now.setLenient(false);
        GregorianCalendar iso = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"), Locale.US);
        iso.setMinimalDaysInFirstWeek(4);
        iso.setLenient(false);
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        symbols.setShortWeekdays(new String[]{"", "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"});
        now.setCalendar(iso);
        now.setDateFormatSymbols(symbols);
        now.set2DigitYearStart(new Date(0));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
            oos.writeObject(new SafeSimpleDateFormat("yyyy-MM-dd"));
        }
        assertTrue(bytes.size() < 12_000, "a formatter nobody configured: " + bytes.size() + " bytes");
        bytes.reset();
        try (ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
            oos.writeObject(now);
        }
        SafeSimpleDateFormat back;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            back = (SafeSimpleDateFormat) ois.readObject();
        }
        assertEquals("We 2024/07/03 15:16 IST", now.format(JULY_3_2024));
        assertEquals(now.format(JULY_3_2024), back.format(JULY_3_2024));
        assertEquals(now, back);
        assertEquals(now.hashCode(), back.hashCode());
    }

    /** Threads formatting and parsing through shared formatters while another thread reconfigures one of them. */
    @Test
    void sharedByThreadsWhileOneIsReconfigured() throws Exception {
        final String[] patterns = {"yyyy-MM-dd HH:mm:ss.SSS", "EEE, d MMM yyyy HH:mm:ss Z", "yyyyMMdd'T'HHmmss"};
        final SafeSimpleDateFormat[] shared = new SafeSimpleDateFormat[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            shared[i] = new SafeSimpleDateFormat(patterns[i]);
        }
        final SafeSimpleDateFormat flipped = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        final TimeZone[] zones = {TimeZone.getTimeZone("UTC"), TimeZone.getTimeZone("Asia/Kolkata")};
        final String[] eitherZone = {"2024-07-03 09:46", "2024-07-03 15:16"};
        final AtomicReference<String> wrong = new AtomicReference<>();
        final CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            final int seed = t;
            threads[t] = new Thread(() -> {
                try {
                    SimpleDateFormat[] own = new SimpleDateFormat[patterns.length];
                    for (int i = 0; i < patterns.length; i++) {
                        own[i] = new SimpleDateFormat(patterns[i]);
                    }
                    go.await();
                    for (int i = 0; i < 20_000 && wrong.get() == null; i++) {
                        int which = (i + seed) % patterns.length;
                        Date date = new Date(1_700_000_000_000L + (long) i * 7_919_000L * (seed + 1));
                        String expected = own[which].format(date);
                        String actual = shared[which].format(date);
                        if (!expected.equals(actual)
                                || !own[which].parse(expected).equals(shared[which].parse(expected))) {
                            wrong.compareAndSet(null, patterns[which] + ": [" + expected + "] but [" + actual + "]");
                        }
                        String either = flipped.format(JULY_3_2024);
                        if (!either.equals(eitherZone[0]) && !either.equals(eitherZone[1])) {
                            wrong.compareAndSet(null, "reconfigured: [" + either + "]");
                        }
                    }
                } catch (Throwable e) {
                    wrong.compareAndSet(null, e.toString());
                }
            });
            threads[t].start();
        }
        go.countDown();
        for (int i = 0; threads[0].isAlive(); i++) {
            flipped.setTimeZone(zones[i & 1]);
            flipped.setLenient((i & 2) == 0);
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertNull(wrong.get());
    }
}
