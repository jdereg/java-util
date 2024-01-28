package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DateConversions {

    static long toLong(Object from) {
        return ((Date) from).getTime();
    }

    static Instant toInstant(Object from) {
        return Instant.ofEpochMilli(toLong(from));
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return toInstant(from).atZone(options.getZoneId());
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    /**
     * The input can be any of our Date type objects (java.sql.Date, Timestamp, Date, etc.) coming in so
     * we need to force the conversion by creating a new instance.
     * @param from - one of the date objects
     * @param converter - converter instance
     * @param options - converter options
     * @return newly created java.sql.Date
     */
    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    /**
     * The input can be any of our Date type objects (java.sql.Date, Timestamp, Date, etc.) coming in so
     * we need to force the conversion by creating a new instance.
     * @param from - one of the date objects
     * @param converter - converter instance
     * @param options - converter options
     * @return newly created Date
     */    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return new Date(toLong(from));
    }

    /**
     * The input can be any of our Date type objects (java.sql.Date, Timestamp, Date, etc.) coming in so
     * we need to force the conversion by creating a new instance.
     * @param from - one of the date objects
     * @param converter - converter instance
     * @param options - converter options
     * @return newly created Timestamp
     */
    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        return CalendarConversions.create(toLong(from), options);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        return toInstant(from);
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options);
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

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static String dateToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String sqlDateToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String timestampToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String calendarToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Calendar) from).getTime());
    }

    static String localDateToString(Object from, Converter converter, ConverterOptions options) {
        LocalDate localDate = (LocalDate) from;
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    static String localTimeToString(Object from, Converter converter, ConverterOptions options) {
        LocalTime localTime = (LocalTime) from;
        return localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    static String localDateTimeToString(Object from, Converter converter, ConverterOptions options) {
        LocalDateTime localDateTime = (LocalDateTime) from;
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static String zonedDateTimeToString(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime zonedDateTime = (ZonedDateTime) from;
        return zonedDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
