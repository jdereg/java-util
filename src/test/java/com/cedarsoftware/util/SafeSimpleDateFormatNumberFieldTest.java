package com.cedarsoftware.util;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A numeric field followed by the locale's decimal separator -- {@code ss.SSS}, {@code dd.MM.yyyy},
 * {@code yyyy.MM.dd}, {@code HH.mm}, or {@code ss,SSS} under a ',' locale -- parses exactly as it does with
 * {@link SimpleDateFormat}. The default number format read "28.144" as one number, so the seconds field swallowed
 * the milliseconds and the pattern's '.' found nothing to match. Up to 4.0.0 only the thread that constructed the
 * formatter was affected; from 4.1.0 every thread was, and the static {@code getDateFormat()} too.
 */
class SafeSimpleDateFormatNumberFieldTest {

    private Locale savedLocale;
    private TimeZone savedZone;

    @BeforeEach
    void pinDefaults() {
        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        clearCaches();
    }

    @AfterEach
    void restoreDefaults() {
        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
        clearCaches();
    }

    private static void clearCaches() {
        SafeSimpleDateFormat.clearThreadLocalCache();
        SafeSimpleDateFormat.clearStaticThreadLocalCache();
    }

    static Stream<Arguments> patterns() {
        return Stream.of(
                Arguments.of(Locale.US, "yyyy-MM-dd HH:mm:ss.SSS", "2026-09-24 13:33:28.144"),
                Arguments.of(Locale.US, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "2026-09-24T13:33:28.144-0400"),
                Arguments.of(Locale.US, "dd.MM.yyyy", "24.09.2026"),
                Arguments.of(Locale.US, "yyyy.MM.dd", "2026.09.24"),
                Arguments.of(Locale.US, "HH.mm", "13.33"),
                Arguments.of(Locale.US, "yyyy-MM-dd HH:mm:ss", "2026-09-24 13:33:28"),
                Arguments.of(Locale.US, "M/d/yyyy HH:mm:ss", "9/24/2026 13:33:28"),
                Arguments.of(Locale.GERMANY, "ss,SSS", "28,144"),
                Arguments.of(Locale.GERMANY, "dd.MM.yyyy HH:mm:ss,SSS", "24.09.2026 13:33:28,144"),
                Arguments.of(Locale.GERMANY, "yyyy-MM-dd HH:mm:ss.SSS", "2026-09-24 13:33:28.144"));
    }

    /** What the JDK's own SimpleDateFormat parses: the oracle. */
    private static Date jdk(String pattern, String text) throws Exception {
        return new SimpleDateFormat(pattern).parse(text);
    }

    private static Date parseFully(DateFormat format, String text) {
        ParsePosition pos = new ParsePosition(0);
        Date date = format.parse(text, pos);
        assertNotNull(date, "failed at index " + pos.getErrorIndex() + " of \"" + text + "\"");
        assertEquals(text.length(), pos.getIndex(), "did not consume all of \"" + text + "\"");
        return date;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("patterns")
    void parsesAsSimpleDateFormatDoesOnTheConstructingThread(Locale locale, String pattern, String text) throws Exception {
        Locale.setDefault(locale);
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        assertEquals(jdk(pattern, text), parseFully(safe, text));
        assertEquals(jdk(pattern, text), safe.parse(text));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("patterns")
    void parsesAsSimpleDateFormatDoesOnAnyOtherThread(Locale locale, String pattern, String text) throws Exception {
        Locale.setDefault(locale);
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        AtomicReference<Object> result = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                result.set(parseFully(safe, text));
            } catch (Throwable t) {
                result.set(t);
            }
        });
        thread.start();
        thread.join();
        if (result.get() instanceof Throwable) {
            throw new AssertionError("on another thread", (Throwable) result.get());
        }
        assertEquals(jdk(pattern, text), result.get());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("patterns")
    void theStaticAccessorParsesAsSimpleDateFormatDoes(Locale locale, String pattern, String text) throws Exception {
        Locale.setDefault(locale);
        assertEquals(jdk(pattern, text), parseFully(SafeSimpleDateFormat.getDateFormat(pattern), text));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("patterns")
    void formatsAsSimpleDateFormatDoes(Locale locale, String pattern, String text) throws Exception {
        Locale.setDefault(locale);
        Date date = jdk(pattern, text);
        assertEquals(new SimpleDateFormat(pattern).format(date), new SafeSimpleDateFormat(pattern).format(date));
        assertEquals(new SimpleDateFormat(pattern).format(date), SafeSimpleDateFormat.getDateFormat(pattern).format(date));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("milliseconds")
    void millisecondsAreReadExactly(String text, int expectedMillis) throws Exception {
        // The case the defect broke: the seconds field swallowed the milliseconds
        Locale.setDefault(Locale.US);
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = parseFully(safe, text);
        assertEquals(expectedMillis, (int) Math.floorMod(date.getTime(), 1000L), text);
        assertEquals(jdk("yyyy-MM-dd HH:mm:ss.SSS", text), date);
    }

    static Stream<Arguments> milliseconds() {
        return Stream.of(
                Arguments.of("2026-09-24 13:33:28.144", 144),
                Arguments.of("2026-09-24 13:33:28.044", 44),
                Arguments.of("2026-09-24 13:33:28.999", 999),
                Arguments.of("2026-09-24 13:33:28.000", 0),
                Arguments.of("2026-09-24 00:00:00.001", 1));
    }

    @Test
    void millisecondsRoundTripOnEveryThread() throws Exception {
        Locale.setDefault(Locale.US);
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        long[] instants = {1758735208144L, 1758735208001L, 1758735208999L, 0L, 1758735208000L};
        Thread[] threads = new Thread[8];
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        for (long instant : instants) {
                            String text = safe.format(new Date(instant));
                            assertEquals(instant, safe.parse(text).getTime(), text);
                        }
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        if (failure.get() != null) {
            throw new AssertionError("a thread failed", failure.get());
        }
    }

    @Test
    void theDefaultNumberFormatIsTheOneSimpleDateFormatUses() {
        Locale.setDefault(Locale.US);
        NumberFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd").getNumberFormat();
        NumberFormat jdk = new SimpleDateFormat("yyyy-MM-dd").getNumberFormat();
        assertTrue(safe.isParseIntegerOnly());
        assertFalse(safe.isGroupingUsed());
        assertEquals(jdk.isParseIntegerOnly(), safe.isParseIntegerOnly());
        assertEquals(jdk.isGroupingUsed(), safe.isGroupingUsed());
        assertEquals(jdk.getMaximumFractionDigits(), safe.getMaximumFractionDigits());
    }

    @Test
    void aNumberFormatTheCallerSetsIsStillTheOneUsed() throws Exception {
        // the caller's choice wins, including one that reads decimals -- and the workaround callers adopted
        // (setting an integer-only format themselves) keeps working
        Locale.setDefault(Locale.US);
        SafeSimpleDateFormat decimal = new SafeSimpleDateFormat("ss.SSS");
        decimal.setNumberFormat(NumberFormat.getNumberInstance(Locale.US));
        ParsePosition pos = new ParsePosition(0);
        assertNull(decimal.parse("28.144", pos), "a decimal-reading format swallows the '.'");

        SafeSimpleDateFormat integerOnly = new SafeSimpleDateFormat("ss.SSS");
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setParseIntegerOnly(true);
        integerOnly.setNumberFormat(nf);
        assertEquals(jdk("ss.SSS", "28.144"), integerOnly.parse("28.144"));
    }
}
