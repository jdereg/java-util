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

public class InstantConversions {

    static long toLong(Object from) {
        return ((Instant)from).toEpochMilli();
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return ((Instant)from).atZone(options.getZoneId());
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return new Date(toLong(from));
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        return CalendarConversions.create(toLong(from), options);
    }
    
    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
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
}
