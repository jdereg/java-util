package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class LocalDateConversions {

    private static ZonedDateTime toZonedDateTime(Object fromInstance, ConverterOptions options) {
        return ((LocalDate)fromInstance).atStartOfDay(options.getZoneId());
    }

    static Instant toInstant(Object fromInstance, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toInstant();
    }

    static long toLong(Object fromInstance, ConverterOptions options) {
        return toInstant(fromInstance, options).toEpochMilli();
    }

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).withZoneSameInstant(options.getZoneId());
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        return toZonedDateTime(fromInstance, options).toInstant();
    }


    static long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return toInstant(fromInstance, options).toEpochMilli();
    }

    static float toFloat(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance, converter, options);
    }

    static double toDouble(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance, converter, options);
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        LocalDate from = (LocalDate)fromInstance;
        return new AtomicLong(toLong(from, options));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance, options));
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        return CalendarConversions.create(toLong(fromInstance, options), options);
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
