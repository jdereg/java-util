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
import java.util.concurrent.atomic.AtomicLong;

public class CalendarConversions {

    static Date toDate(Object from) {
        return ((Calendar)from).getTime();
    }

    static long toLong(Object from) {
        return toDate(from).getTime();
    }

    static Instant toInstant(Object from) {
        return ((Calendar)from).toInstant();
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return toInstant(from).atZone(options.getZoneId());
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options);
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return (double)toLong(from);
    }


    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return toDate(from);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        return toInstant(from);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalTime();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static Calendar clone(Object from, Converter converter, ConverterOptions options) {
        Calendar calendar = (Calendar)from;
        // mutable class, so clone it.
        return (Calendar)calendar.clone();
    }

    static Calendar create(long epochMilli, ConverterOptions options) {
        Calendar cal = Calendar.getInstance(options.getTimeZone());
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }
}
