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

public class LocalDateConversions {

    private static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return ((LocalDate)from).atStartOfDay(options.getSourceZoneIdForLocalDates()).withZoneSameInstant(options.getZoneId());
    }

    static Instant toInstant(Object from, ConverterOptions options) {
        return toZonedDateTime(from, options).toInstant();
    }

    static long toLong(Object from, ConverterOptions options) {
        return toInstant(from, options).toEpochMilli();
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

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).withZoneSameInstant(options.getZoneId());
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toInstant();
    }


    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toInstant(from, options).toEpochMilli();
    }

    /**
     * Warning:  Can lose precision going from a full long down to a floating point number
     * @param from instance to convert
     * @param converter converter instance
     * @param options converter options
     * @return the floating point number cast from a lont.
     */
    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return toLong(from, converter, options);
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return toLong(from, converter, options);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from, options));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from, options));
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime time = toZonedDateTime(from, options);
        GregorianCalendar calendar = new GregorianCalendar(options.getTimeZone());
        calendar.setTimeInMillis(time.toInstant().toEpochMilli());
        return calendar;
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from, options));
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return new Date(toLong(from, options));
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from, options));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from, options));
    }
}
