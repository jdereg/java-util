package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class InstantConversion {
    static long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        Instant from = (Instant)fromInstance;
        return from.toEpochMilli();
    }

    static float toFloat(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance, converter, options);
    }

    static double toDouble(Object fromInstance, Converter converter, ConverterOptions options) {
        return toLong(fromInstance, converter, options);
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(fromInstance, converter, options));
    }

    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(fromInstance, converter, options));
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        long localDateMillis = toLong(fromInstance, converter, options);
        return CalendarConversion.create(localDateMillis, options);
    }

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(fromInstance, converter, options));
    }

    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return new Date(toLong(fromInstance, converter, options));
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(fromInstance, converter, options));
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(fromInstance, converter, options));
    }

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        Instant from = (Instant)fromInstance;
        return from.atZone(options.getZoneId()).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object fromInstance, Converter converter, ConverterOptions options) {
        Instant from = (Instant)fromInstance;
        return from.atZone(options.getZoneId()).toLocalDate();
    }


}
