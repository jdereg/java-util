package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DateConversion {
    public static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        Date from = (Date)fromInstance;
        return BigDecimal.valueOf(from.getTime());
    }

    public static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        Date from = (Date)fromInstance;
        return new AtomicLong(from.getTime());
    }
}
