package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.MathUtilities.parseToMinimalNumericType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
class TestMathUtilities
{
    @Test
    void testConstructorIsPrivate() throws Exception {
        Class<?> c = MathUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<?> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    void testMinimumLong()
    {
        long min = MathUtilities.minimum(0, 1, 2);
        assertEquals(0, min);
        min = MathUtilities.minimum(0, 1);
        assertEquals(0, min);
        min = MathUtilities.minimum(0);
        assertEquals(0, min);
        min = MathUtilities.minimum(-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(-10, min);
        min = MathUtilities.minimum(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertEquals(-10, min);

        min = MathUtilities.minimum(-1, 0, 1);
        assertEquals(-1, min);
        min = MathUtilities.minimum(-1, 1);
        assertEquals(-1, min);

        min = MathUtilities.minimum(-100000000, 0, 100000000);
        assertEquals(-100000000, min);
        min = MathUtilities.minimum(-100000000, 100000000);
        assertEquals(-100000000, min);

        long[] values = {45, -13, 123213123};
        assertEquals(-13, MathUtilities.minimum(values));
    }

    @Test
    void testMinimumDouble()
    {
        double min = MathUtilities.minimum(0.1, 1.1, 2.1);
        assertEquals(0.1, min);
        min = MathUtilities.minimum(-0.01, 1.0);
        assertEquals(-0.01, min);
        min = MathUtilities.minimum(0.0);
        assertEquals(0.0, min);
        min = MathUtilities.minimum(-10.0, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(-10.0, min);
        min = MathUtilities.minimum(10.0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertEquals(-10.0, min);

        min = MathUtilities.minimum(-1.0, 0.0, 1.0);
        assertEquals(-1.0, min);
        min = MathUtilities.minimum(-1.0, 1.0);
        assertEquals(-1.0, min);

        min = MathUtilities.minimum(-100000000.0, 0, 100000000.0);
        assertEquals(-100000000.0, min);
        min = MathUtilities.minimum(-100000000.0, 100000000.0);
        assertEquals(-100000000.0, min);

        double[] values = {45.1, -13.1, 123213123.1};
        assertEquals(-13.1, MathUtilities.minimum(values));
    }

    @Test
    void testMinimumBigInteger()
    {
        BigInteger minBi = MathUtilities.minimum(new BigInteger("-1"), new BigInteger("0"), new BigInteger("1"));
        assertEquals(new BigInteger("-1"), minBi);
        minBi = MathUtilities.minimum(new BigInteger("-121908747902834709812347908123432423"), new BigInteger("0"), new BigInteger("9780234508972317045230477890478903240978234"));
        assertEquals(new BigInteger("-121908747902834709812347908123432423"), minBi);

        BigInteger[] bigies = new BigInteger[] {new BigInteger("1"), new BigInteger("-1")};
        assertEquals(new BigInteger("-1"), MathUtilities.minimum(bigies));

        assertEquals(new BigInteger("500"), MathUtilities.minimum(new BigInteger("500")));

        try
        {
            MathUtilities.minimum((BigInteger)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("3"));
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    void testMinimumBigDecimal()
    {
        BigDecimal minBd = MathUtilities.minimum(new BigDecimal("-1"), new BigDecimal("0"), new BigDecimal("1"));
        assertEquals(new BigDecimal("-1"), minBd);
        minBd = MathUtilities.minimum(new BigDecimal("-121908747902834709812347908123432423.123"), new BigDecimal("0"), new BigDecimal("9780234508972317045230477890478903240978234.123"));
        assertEquals(new BigDecimal("-121908747902834709812347908123432423.123"), minBd);

        BigDecimal[] bigies = new BigDecimal[] {new BigDecimal("1.1"), new BigDecimal("-1.1")};
        assertEquals(new BigDecimal("-1.1"), MathUtilities.minimum(bigies));

        assertEquals(new BigDecimal("500.99"), MathUtilities.minimum(new BigDecimal("500.99")));
        try
        {
            MathUtilities.minimum((BigDecimal)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigDecimal("1"), null, new BigDecimal("3"));
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    void testMaximumLong()
    {
        long max = MathUtilities.maximum(0, 1, 2);
        assertEquals(2, max);
        max = MathUtilities.maximum(0, 1);
        assertEquals(1, max);
        max = MathUtilities.maximum(0);
        assertEquals(0, max);
        max = MathUtilities.maximum(-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(10, max);
        max = MathUtilities.maximum(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertEquals(10, max);

        max = MathUtilities.maximum(-1, 0, 1);
        assertEquals(1, max);
        max = MathUtilities.maximum(-1, 1);
        assertEquals(1, max);

        max = MathUtilities.maximum(-100000000, 0, 100000000);
        assertEquals(100000000, max);
        max = MathUtilities.maximum(-100000000, 100000000);
        assertEquals(100000000, max);

        long[] values = {45, -13, 123213123};
        assertEquals(123213123, MathUtilities.maximum(values));
    }

    @Test
    void testMaximumDouble()
    {
        double max = MathUtilities.maximum(0.1, 1.1, 2.1);
        assertEquals(2.1, max);
        max = MathUtilities.maximum(-0.01, 1.0);
        assertEquals(1.0, max);
        max = MathUtilities.maximum(0.0);
        assertEquals(0.0, max);
        max = MathUtilities.maximum(-10.0, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(10.0, max);
        max = MathUtilities.maximum(10.0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertEquals(10.0, max);

        max = MathUtilities.maximum(-1.0, 0.0, 1.0);
        assertEquals(1.0, max);
        max = MathUtilities.maximum(-1.0, 1.0);
        assertEquals(1.0, max);

        max = MathUtilities.maximum(-100000000.0, 0, 100000000.0);
        assertEquals(100000000.0, max);
        max = MathUtilities.maximum(-100000000.0, 100000000.0);
        assertEquals(100000000.0, max);

        double[] values = {45.1, -13.1, 123213123.1};
        assertEquals(123213123.1, MathUtilities.maximum(values));
    }

    @Test
    void testMaximumBigInteger()
    {
        BigInteger minBi = MathUtilities.minimum(new BigInteger("-1"), new BigInteger("0"), new BigInteger("1"));
        assertEquals(new BigInteger("-1"), minBi);
        minBi = MathUtilities.minimum(new BigInteger("-121908747902834709812347908123432423"), new BigInteger("0"), new BigInteger("9780234508972317045230477890478903240978234"));
        assertEquals(new BigInteger("-121908747902834709812347908123432423"), minBi);

        BigInteger[] bigies = new BigInteger[] {new BigInteger("1"), new BigInteger("-1")};
        assertEquals(new BigInteger("1"), MathUtilities.maximum(bigies));

        assertEquals(new BigInteger("500"), MathUtilities.maximum(new BigInteger("500")));

        try
        {
            MathUtilities.maximum((BigInteger)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("3"));
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    void testNullInMaximumBigInteger()
    {
        try
        {
            MathUtilities.maximum(new BigInteger("1"), null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException ignored) { }
    }

    @Test
    void testMaximumBigDecimal()
    {
        BigDecimal minBd = MathUtilities.maximum(new BigDecimal("-1"), new BigDecimal("0"), new BigDecimal("1"));
        assertEquals(new BigDecimal("1"), minBd);
        minBd = MathUtilities.maximum(new BigDecimal("-121908747902834709812347908123432423.123"), new BigDecimal("0"), new BigDecimal("9780234508972317045230477890478903240978234.123"));
        assertEquals(new BigDecimal("9780234508972317045230477890478903240978234.123"), minBd);

        BigDecimal[] bigies = new BigDecimal[] {new BigDecimal("1.1"), new BigDecimal("-1.1")};
        assertEquals(new BigDecimal("1.1"), MathUtilities.maximum(bigies));

        assertEquals(new BigDecimal("1.5"), MathUtilities.maximum(new BigDecimal("1.5")));

        try
        {
            MathUtilities.maximum((BigDecimal)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.maximum(new BigDecimal("1"), null, new BigDecimal("3"));
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    void testMaxLongBoundary() {
        String maxLong = String.valueOf(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, parseToMinimalNumericType(maxLong));
    }

    @Test
    void testMinLongBoundary() {
        String minLong = String.valueOf(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, parseToMinimalNumericType(minLong));
    }

    @Test
    void testBeyondMaxLongBoundary() {
        String beyondMaxLong = "9223372036854775808"; // Long.MAX_VALUE + 1
        assertEquals(new BigInteger("9223372036854775808"), parseToMinimalNumericType(beyondMaxLong));
    }

    @Test
    void testBeyondMinLongBoundary() {
        String beyondMinLong = "-9223372036854775809"; // Long.MIN_VALUE - 1
        assertEquals(new BigInteger("-9223372036854775809"), parseToMinimalNumericType(beyondMinLong));
    }

    @Test
    void testBeyondMaxDoubleBoundary() {
        String beyondMaxDouble = "1e309"; // A value larger than Double.MAX_VALUE
        assertEquals(new BigDecimal("1e309"), parseToMinimalNumericType(beyondMaxDouble));
    }

    @Test
    void testShouldSwitchToBigDec() {
        String maxDoubleSci = "8.7976931348623157e308"; // Double.MAX_VALUE in scientific notation
        assertEquals(new BigDecimal(maxDoubleSci), parseToMinimalNumericType(maxDoubleSci));
    }

    @Test
    void testInvalidScientificNotationExceedingDouble() {
        String invalidSci = "1e1024"; // Exceeds maximum exponent for Double
        assertEquals(new BigDecimal(invalidSci), parseToMinimalNumericType(invalidSci));
    }

    @Test
    void testExponentWithLeadingZeros()
    {
        String s = "1.45e+0000000000000000000000307";
        Number d = parseToMinimalNumericType(s);
        assert d instanceof Double;
    }

    // The very edges are hard to hit, without expensive additional processing to detect there difference in
    // Examples like this: "12345678901234567890.12345678901234567890" needs to be a BigDecimal, but Double
    // will parse this correctly in it's short handed notation.  My algorithm catches these.  However, the values
    // right near e+308 positive or negative will be returned as BigDecimals to ensure accuracy
    @Disabled
    @Test
    void testMaxDoubleScientificNotation() {
        String maxDoubleSci = "1.7976931348623157e308"; // Double.MAX_VALUE in scientific notation
        assertEquals(Double.parseDouble(maxDoubleSci), parseToMinimalNumericType(maxDoubleSci));
    }

    @Disabled
    @Test
    void testMaxDoubleBoundary() {
        assertEquals(Double.MAX_VALUE, parseToMinimalNumericType(Double.toString(Double.MAX_VALUE)));
    }

    @Disabled
    @Test
    void testMinDoubleBoundary() {
        assertEquals(-Double.MAX_VALUE, parseToMinimalNumericType(Double.toString(-Double.MAX_VALUE)));
    }

    @Disabled
    @Test
    void testTinyDoubleScientificNotation() {
        String tinyDoubleSci = "2.2250738585072014e-308"; // A very small double value
        assertEquals(Double.parseDouble(tinyDoubleSci), parseToMinimalNumericType(tinyDoubleSci));
    }
}
