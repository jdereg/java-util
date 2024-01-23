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

public class DateConversion {

    static long toLong(Object fromInstance) {
        return ((Date) fromInstance).getTime();
    }

    static Instant toInstant(Object fromInstance) {
        return Instant.ofEpochMilli(toLong(fromInstance));
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, ConverterOptions options) {
        return toInstant(fromInstance).atZone(options.getZoneId());
    }

    static long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance);
    }

    static Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(fromInstance));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance));
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        return CalendarConversion.create(toLong(fromInstance), options);
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(fromInstance));
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        return toInstant(fromInstance);
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

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(fromInstance));
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(fromInstance));
    }
}
