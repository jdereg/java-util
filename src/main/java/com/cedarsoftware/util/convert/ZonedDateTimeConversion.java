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

public class ZonedDateTimeConversion {

    static ZonedDateTime toDifferentZone(Object fromInstance, ConverterOptions options) {
        return ((ZonedDateTime)fromInstance).withZoneSameInstant(options.getZoneId());
    }

    static Instant toInstant(Object fromInstance) {
        return ((ZonedDateTime)fromInstance).toInstant();
    }

    static long toLong(Object fromInstance) {
        return toInstant(fromInstance).toEpochMilli();
    }

    static long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance);
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        return toInstant(fromInstance);
    }

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toDifferentZone(fromInstance, options).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return toDifferentZone(fromInstance, options).toLocalDate();
    }

    static LocalTime toLocalTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return toDifferentZone(fromInstance, options).toLocalTime();
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
       return new AtomicLong(toLong(fromInstance));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance));
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        return CalendarConversion.create(toLong(fromInstance), options);
    }

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(fromInstance));
    }

    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Date(toLong(fromInstance));
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(fromInstance));
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(fromInstance));
    }
}
