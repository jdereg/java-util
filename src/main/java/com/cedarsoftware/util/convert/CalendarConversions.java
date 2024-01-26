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

public class CalendarConversions {

    static Date toDate(Object fromInstance) {
        return ((Calendar)fromInstance).getTime();
    }

    static long toLong(Object fromInstance) {
        return toDate(fromInstance).getTime();
    }

    static Instant toInstant(Object fromInstance) {
        return ((Calendar)fromInstance).toInstant();
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, ConverterOptions options) {
        return toInstant(fromInstance).atZone(options.getZoneId());
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options);
    }

    static Long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance);
    }

    static double toDouble(Object fromInstance, Converter converter, ConverterOptions options) {
        return (double)toLong(fromInstance);
    }


    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return toDate(fromInstance);
    }

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(fromInstance));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance));
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(fromInstance));
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        return toInstant(fromInstance);
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

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(fromInstance));
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(fromInstance));
    }

    static Calendar clone(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        // mutable class, so clone it.
        return (Calendar)from.clone();
    }

    static Calendar create(long epochMilli, ConverterOptions options) {
        Calendar cal = Calendar.getInstance(options.getTimeZone());
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }
}
