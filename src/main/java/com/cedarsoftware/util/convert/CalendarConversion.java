package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CalendarConversion {
    public static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return from.getTime();
    }

    public static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return new AtomicLong(from.getTime().getTime());
    }

    public static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return BigDecimal.valueOf(from.getTime().getTime());
    }

    public static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return BigInteger.valueOf(from.getTime().getTime());
    }
}
