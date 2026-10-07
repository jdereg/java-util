package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * String to Number (John's rule): whole text -- an optional sign and digits -- is a Long within a long's range and a
 * BigInteger beyond it; any other text BigDecimal reads is a BigDecimal; anything else is refused. There was no
 * String to Number conversion, so a field declared Number, or a type variable bounded by Number, could not take
 * number text ("Unsupported conversion ... target type 'Number'").
 *
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
class ConverterStringToNumberTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Number number(String text) {
        return converter.convert(text, Number.class);
    }

    @Test
    void wholeTextWithinALongIsALong() {
        assertEquals(Long.valueOf(5), number("5"));
        assertEquals(Long.valueOf(-5), number("-5"));
        assertEquals(Long.valueOf(5), number("+5"));
        assertEquals(Long.valueOf(7), number("007"));
        assertEquals(Long.valueOf(42), number(" 42 "));
        assertEquals(Long.MAX_VALUE, number("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, number("-9223372036854775808"));
        assertEquals(Long.valueOf(0), number(""));
        assertEquals(Long.valueOf(42), number("٤٢"));            // Arabic-Indic digits
    }

    @Test
    void wholeTextBeyondALongIsABigInteger() {
        assertEquals(new BigInteger("9223372036854775808"), number("9223372036854775808"));
        assertEquals(new BigInteger("-9223372036854775809"), number("-9223372036854775809"));
        assertEquals(new BigInteger("123456789012345678901234567890"), number("123456789012345678901234567890"));
    }

    @Test
    void otherNumberTextIsABigDecimal() {
        assertEquals(new BigDecimal("5.5"), number("5.5"));
        assertEquals(new BigDecimal("5.0"), number("5.0"));
        assertEquals(new BigDecimal("1E+3"), number("1e3"));
        assertEquals(new BigDecimal("0.5"), number(".5"));
        assertEquals(new BigDecimal("-2.5E-7"), number("-2.5e-7"));
        assertEquals(new BigDecimal("1E+999999999"), number("1e999999999"));    // a small object, not a billion digits
    }

    @Test
    void textThatIsNoNumberIsRefused() {
        for (String text : new String[] {"abc", "0x1F", "NaN", "Infinity", "1_000", "5 5", "-", "1e", "٤x"}) {
            assertThrows(IllegalArgumentException.class, () -> number(text), text);
        }
    }
}
