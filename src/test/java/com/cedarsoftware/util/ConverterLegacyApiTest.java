package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConverterLegacyApiTest {
    @FunctionalInterface
    private interface ConversionFunction {
        Object apply(Object value);
    }

    private static Stream<Arguments> convert2GoodData() {
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convert2AtomicBoolean, "true", new AtomicBoolean(true)),
                Arguments.of((ConversionFunction) Converter::convert2AtomicInteger, "2", new AtomicInteger(2)),
                Arguments.of((ConversionFunction) Converter::convert2AtomicLong, "3", new AtomicLong(3L)),
                Arguments.of((ConversionFunction) Converter::convert2BigDecimal, "1.5", new BigDecimal("1.5")),
                Arguments.of((ConversionFunction) Converter::convert2BigInteger, "4", new BigInteger("4")),
                Arguments.of((ConversionFunction) Converter::convert2String, 7, "7"),
                Arguments.of((ConversionFunction) Converter::convert2boolean, "true", true),
                Arguments.of((ConversionFunction) (o -> Converter.convert2byte(o)), "8", (byte)8),
                Arguments.of((ConversionFunction) (o -> Converter.convert2char(o)), "A", 'A'),
                Arguments.of((ConversionFunction) (o -> Converter.convert2double(o)), "9.5", 9.5d),
                Arguments.of((ConversionFunction) (o -> Converter.convert2float(o)), "9.5", 9.5f),
                Arguments.of((ConversionFunction) (o -> Converter.convert2int(o)), "10", 10),
                Arguments.of((ConversionFunction) (o -> Converter.convert2long(o)), "11", 11L),
                Arguments.of((ConversionFunction) (o -> Converter.convert2short(o)), "12", (short)12)
        );
    }

    @ParameterizedTest
    @MethodSource("convert2GoodData")
    void convert2_goodData(ConversionFunction func, Object input, Object expected) {
        assertThat(func.apply(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> convert2NullData() {
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convert2AtomicBoolean, new AtomicBoolean(false)),
                Arguments.of((ConversionFunction) Converter::convert2AtomicInteger, new AtomicInteger(0)),
                Arguments.of((ConversionFunction) Converter::convert2AtomicLong, new AtomicLong(0L)),
                Arguments.of((ConversionFunction) Converter::convert2BigDecimal, BigDecimal.ZERO),
                Arguments.of((ConversionFunction) Converter::convert2BigInteger, BigInteger.ZERO),
                Arguments.of((ConversionFunction) Converter::convert2String, ""),
                Arguments.of((ConversionFunction) Converter::convert2boolean, false),
                Arguments.of((ConversionFunction) (o -> Converter.convert2byte(o)), (byte)0),
                Arguments.of((ConversionFunction) (o -> Converter.convert2char(o)), (char)0),
                Arguments.of((ConversionFunction) (o -> Converter.convert2double(o)), 0.0d),
                Arguments.of((ConversionFunction) (o -> Converter.convert2float(o)), 0.0f),
                Arguments.of((ConversionFunction) (o -> Converter.convert2int(o)), 0),
                Arguments.of((ConversionFunction) (o -> Converter.convert2long(o)), 0L),
                Arguments.of((ConversionFunction) (o -> Converter.convert2short(o)), (short)0)
        );
    }

    @ParameterizedTest
    @MethodSource("convert2NullData")
    void convert2_nullReturnsDefault(ConversionFunction func, Object expected) {
        assertThat(func.apply(null)).isEqualTo(expected);
    }

    private static Stream<Arguments> convert2BadData() {
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convert2AtomicInteger),
                Arguments.of((ConversionFunction) Converter::convert2AtomicLong),
                Arguments.of((ConversionFunction) Converter::convert2BigDecimal),
                Arguments.of((ConversionFunction) Converter::convert2BigInteger),
                Arguments.of((ConversionFunction) (o -> Converter.convert2byte(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2char(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2double(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2float(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2int(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2long(o))),
                Arguments.of((ConversionFunction) (o -> Converter.convert2short(o)))
        );
    }

    @ParameterizedTest
    @MethodSource("convert2BadData")
    void convert2_badDataThrows(ConversionFunction func) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> func.apply("bad"));
    }

    private static final String DATE_STR = "2020-01-02T03:04:05Z";

    private static Stream<Arguments> convertToGoodData() {
        ZonedDateTime zdt = ZonedDateTime.parse(DATE_STR);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        Date date = Date.from(zdt.toInstant());
        java.sql.Date sqlDate = java.sql.Date.valueOf(zdt.toLocalDate());
        Timestamp ts = Timestamp.from(zdt.toInstant());
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convertToAtomicBoolean, "true", new AtomicBoolean(true)),
                Arguments.of((ConversionFunction) Converter::convertToAtomicInteger, "2", new AtomicInteger(2)),
                Arguments.of((ConversionFunction) Converter::convertToAtomicLong, "3", new AtomicLong(3L)),
                Arguments.of((ConversionFunction) Converter::convertToBigDecimal, "1.5", new BigDecimal("1.5")),
                Arguments.of((ConversionFunction) Converter::convertToBigInteger, "4", new BigInteger("4")),
                Arguments.of((ConversionFunction) Converter::convertToBoolean, "true", Boolean.TRUE),
                Arguments.of((ConversionFunction) Converter::convertToByte, "5", Byte.valueOf("5")),
                Arguments.of((ConversionFunction) Converter::convertToCharacter, "A", 'A'),
                Arguments.of((ConversionFunction) Converter::convertToDouble, "2.2", 2.2d),
                Arguments.of((ConversionFunction) Converter::convertToFloat, "1.1", 1.1f),
                Arguments.of((ConversionFunction) Converter::convertToInteger, "6", 6),
                Arguments.of((ConversionFunction) Converter::convertToLong, "7", 7L),
                Arguments.of((ConversionFunction) Converter::convertToShort, "8", (short)8),
                Arguments.of((ConversionFunction) Converter::convertToString, 9, "9"),
                Arguments.of((ConversionFunction) Converter::convertToCalendar, DATE_STR, cal),
                Arguments.of((ConversionFunction) Converter::convertToDate, DATE_STR, date),
                Arguments.of((ConversionFunction) Converter::convertToLocalDate, DATE_STR, zdt.toLocalDate()),
                Arguments.of((ConversionFunction) Converter::convertToLocalDateTime, DATE_STR, zdt.toLocalDateTime()),
                Arguments.of((ConversionFunction) Converter::convertToSqlDate, DATE_STR, sqlDate),
                Arguments.of((ConversionFunction) Converter::convertToTimestamp, DATE_STR, ts),
                Arguments.of((ConversionFunction) Converter::convertToZonedDateTime, DATE_STR, zdt)
        );
    }

    @ParameterizedTest
    @MethodSource("convertToGoodData")
    void convertTo_goodData(ConversionFunction func, Object input, Object expected) {
        Object result = func.apply(input);
        if (result instanceof Calendar) {
            assertThat(((Calendar) result).getTime()).isEqualTo(((Calendar) expected).getTime());
        } else {
            assertThat(result).isEqualTo(expected);
        }
    }

    private static Stream<Arguments> convertToNullData() {
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convertToAtomicBoolean),
                Arguments.of((ConversionFunction) Converter::convertToAtomicInteger),
                Arguments.of((ConversionFunction) Converter::convertToAtomicLong),
                Arguments.of((ConversionFunction) Converter::convertToBigDecimal),
                Arguments.of((ConversionFunction) Converter::convertToBigInteger),
                Arguments.of((ConversionFunction) Converter::convertToBoolean),
                Arguments.of((ConversionFunction) Converter::convertToByte),
                Arguments.of((ConversionFunction) Converter::convertToCalendar),
                Arguments.of((ConversionFunction) Converter::convertToCharacter),
                Arguments.of((ConversionFunction) Converter::convertToDate),
                Arguments.of((ConversionFunction) Converter::convertToDouble),
                Arguments.of((ConversionFunction) Converter::convertToFloat),
                Arguments.of((ConversionFunction) Converter::convertToInteger),
                Arguments.of((ConversionFunction) Converter::convertToLocalDate),
                Arguments.of((ConversionFunction) Converter::convertToLocalDateTime),
                Arguments.of((ConversionFunction) Converter::convertToLong),
                Arguments.of((ConversionFunction) Converter::convertToShort),
                Arguments.of((ConversionFunction) Converter::convertToSqlDate),
                Arguments.of((ConversionFunction) Converter::convertToString),
                Arguments.of((ConversionFunction) Converter::convertToTimestamp),
                Arguments.of((ConversionFunction) Converter::convertToZonedDateTime)
        );
    }

    @ParameterizedTest
    @MethodSource("convertToNullData")
    void convertTo_nullReturnsNull(ConversionFunction func) {
        assertThat(func.apply(null)).isNull();
    }

    private static Stream<Arguments> convertToBadData() {
        return Stream.of(
                Arguments.of((ConversionFunction) Converter::convertToAtomicInteger),
                Arguments.of((ConversionFunction) Converter::convertToAtomicLong),
                Arguments.of((ConversionFunction) Converter::convertToBigDecimal),
                Arguments.of((ConversionFunction) Converter::convertToBigInteger),
                Arguments.of((ConversionFunction) Converter::convertToByte),
                Arguments.of((ConversionFunction) Converter::convertToCharacter),
                Arguments.of((ConversionFunction) Converter::convertToDouble),
                Arguments.of((ConversionFunction) Converter::convertToFloat),
                Arguments.of((ConversionFunction) Converter::convertToInteger),
                Arguments.of((ConversionFunction) Converter::convertToLong),
                Arguments.of((ConversionFunction) Converter::convertToShort),
                Arguments.of((ConversionFunction) Converter::convertToCalendar),
                Arguments.of((ConversionFunction) Converter::convertToDate),
                Arguments.of((ConversionFunction) Converter::convertToLocalDate),
                Arguments.of((ConversionFunction) Converter::convertToLocalDateTime),
                Arguments.of((ConversionFunction) Converter::convertToSqlDate),
                Arguments.of((ConversionFunction) Converter::convertToTimestamp),
                Arguments.of((ConversionFunction) Converter::convertToZonedDateTime)
        );
    }

    @ParameterizedTest
    @MethodSource("convertToBadData")
    void convertTo_badDataThrows(ConversionFunction func) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> func.apply("bad"));
    }

    @Test
    void conversionPairGetters() {
        com.cedarsoftware.util.convert.Converter.ConversionPair pair =
                com.cedarsoftware.util.convert.Converter.pair(String.class, Integer.class);
        assertThat(pair.getSource()).isSameAs(String.class);
        assertThat(pair.getTarget()).isSameAs(Integer.class);
    }

    @Test
    void identityReturnsSameObject() {
        Object obj = new Object();
        Object out = com.cedarsoftware.util.convert.Converter.identity(obj, null);
        assertThat(out).isSameAs(obj);
    }

    @Test
    void collectionConversionSupport() {
        assertTrue(com.cedarsoftware.util.convert.Converter.isCollectionConversionSupported(String[].class, java.util.List.class));
        assertFalse(com.cedarsoftware.util.convert.Converter.isCollectionConversionSupported(String.class, java.util.List.class));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> com.cedarsoftware.util.convert.Converter.isCollectionConversionSupported(String[].class, java.util.EnumSet.class));
    }

    @Test
    void simpleTypeConversionSupport() {
        assertTrue(com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        assertFalse(com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(java.util.List.class, java.util.Set.class));
    }

    @Test
    void localDateMillisConversions() {
        LocalDate date = LocalDate.of(2020, 1, 1);
        long expectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertThat(Converter.localDateToMillis(date)).isEqualTo(expectedDateMillis);

        LocalDateTime ldt = LocalDateTime.of(2020, 1, 1, 12, 0);
        long expectedLdtMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertThat(Converter.localDateTimeToMillis(ldt)).isEqualTo(expectedLdtMillis);
    }
}

