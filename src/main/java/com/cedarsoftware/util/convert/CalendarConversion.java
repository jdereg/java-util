package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class CalendarConversion {
    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return from.getTime();
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return new AtomicLong(from.getTime().getTime());
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return BigDecimal.valueOf(from.getTime().getTime());
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        Calendar from = (Calendar)fromInstance;
        return BigInteger.valueOf(from.getTime().getTime());
    }
}
