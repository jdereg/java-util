package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class YearConversions {
    private YearConversions() {}

    static int toInt(Object from) {
        return ((Year)from).getValue();
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toInt(from);
    }

    static int toInt(Object from, Converter converter, ConverterOptions options) {
        return toInt(from);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        return new AtomicInteger(toInt(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toInt(from));
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return toInt(from);
    }

    static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        return toInt(from) == 0;
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        return new AtomicBoolean(toInt(from) == 0);
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toInt(from));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toInt(from));
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return ((Year)from).toString();
    }
}
