package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

public class LocalDateTimeConversions {
    private static ZonedDateTime toZonedDateTime(Object fromInstance, ConverterOptions options) {
        return ((LocalDateTime)fromInstance).atZone(options.getSourceZoneIdForLocalDates()).withZoneSameInstant(options.getZoneId());
    }

    private static Instant toInstant(Object fromInstance, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toInstant();
    }

    private static long toLong(Object fromInstance, ConverterOptions options) {
        return toInstant(fromInstance, options).toEpochMilli();
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options);
    }

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toLocalDate();
    }

    static LocalTime toLocalTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toLocalTime();
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        return toInstant(fromInstance, options);
    }

    static long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance, options);
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(fromInstance, options));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance, options));
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        ZonedDateTime time = toZonedDateTime(fromInstance, options);
        GregorianCalendar calendar = new GregorianCalendar(options.getTimeZone());
        calendar.setTimeInMillis(time.toInstant().toEpochMilli());
        return calendar;
    }

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(fromInstance, options));
    }

    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Date(toLong(fromInstance, options));
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(fromInstance, options));
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(fromInstance, options));
    }
}
