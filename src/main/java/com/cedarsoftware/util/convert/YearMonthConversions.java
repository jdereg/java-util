package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
final class YearMonthConversions {

    private YearMonthConversions() {}

    static int toInt(Object from, Converter converter) {
        YearMonth ym = (YearMonth) from;
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    static long toLong(Object from, Converter converter) {
        return toInt(from, converter);
    }

    static short toShort(Object from, Converter converter) {
        return (short) toInt(from, converter);
    }

    static double toDouble(Object from, Converter converter) {
        return toInt(from, converter);
    }

    static float toFloat(Object from, Converter converter) {
        return toInt(from, converter);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(toInt(from, converter));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toInt(from, converter));
    }

    static Map toMap(Object from, Converter converter) {
        YearMonth yearMonth = (YearMonth) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(YEAR_MONTH, yearMonth.toString());
        return target;
    }
}
