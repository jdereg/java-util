package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestMathUtilities
{
    @Test
    public void testMinimumLong()
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
    public void testMinimumDouble()
    {
        double min = MathUtilities.minimum(0.1, 1.1, 2.1);
        assertTrue(0.1 == min);
        min = MathUtilities.minimum(-0.01, 1.0);
        assertTrue(-0.01 == min);
        min = MathUtilities.minimum(0.0);
        assertTrue(0.0 == min);
        min = MathUtilities.minimum(-10.0, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertTrue(-10.0 == min);
        min = MathUtilities.minimum(10.0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertTrue(-10.0 == min);

        min = MathUtilities.minimum(-1.0, 0.0, 1.0);
        assertTrue(-1.0 == min);
        min = MathUtilities.minimum(-1.0, 1.0);
        assertTrue(-1.0 == min);

        min = MathUtilities.minimum(-100000000.0, 0, 100000000.0);
        assertTrue(-100000000.0 == min);
        min = MathUtilities.minimum(-100000000.0, 100000000.0);
        assertTrue(-100000000.0 == min);

        double[] values = {45.1, -13.1, 123213123.1};
        assertTrue(-13.1 == MathUtilities.minimum(values));
    }

    @Test
    public void testMinimumBigInteger()
    {
        BigInteger minBi = MathUtilities.minimum(new BigInteger("-1"), new BigInteger("0"), new BigInteger("1"));
        assertEquals(new BigInteger("-1"), minBi);
        minBi = MathUtilities.minimum(new BigInteger("-121908747902834709812347908123432423"), new BigInteger("0"), new BigInteger("9780234508972317045230477890478903240978234"));
        assertEquals(new BigInteger("-121908747902834709812347908123432423"), minBi);

        BigInteger[] bigies = new BigInteger[] {new BigInteger("1"), new BigInteger("-1")};
        assertEquals(new BigInteger("-1"), MathUtilities.minimum(bigies));

        try
        {
            MathUtilities.minimum((BigInteger)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigInteger[]{new BigInteger("1"), null, new BigInteger("3")});
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    public void testMinimumBigDecimal()
    {
        BigDecimal minBd = MathUtilities.minimum(new BigDecimal("-1"), new BigDecimal("0"), new BigDecimal("1"));
        assertEquals(new BigDecimal("-1"), minBd);
        minBd = MathUtilities.minimum(new BigDecimal("-121908747902834709812347908123432423.123"), new BigDecimal("0"), new BigDecimal("9780234508972317045230477890478903240978234.123"));
        assertEquals(new BigDecimal("-121908747902834709812347908123432423.123"), minBd);

        BigDecimal[] bigies = new BigDecimal[] {new BigDecimal("1.1"), new BigDecimal("-1.1")};
        assertEquals(new BigDecimal("-1.1"), MathUtilities.minimum(bigies));

        try
        {
            MathUtilities.minimum((BigDecimal)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigDecimal[]{new BigDecimal("1"), null, new BigDecimal("3")});
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    public void testMaximumLong()
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
    public void testMaximumDouble()
    {
        double max = MathUtilities.maximum(0.1, 1.1, 2.1);
        assertTrue(2.1 == max);
        max = MathUtilities.maximum(-0.01, 1.0);
        assertTrue(1.0 == max);
        max = MathUtilities.maximum(0.0);
        assertTrue(0.0 == max);
        max = MathUtilities.maximum(-10.0, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertTrue(10.0 == max);
        max = MathUtilities.maximum(10.0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertTrue(10.0 == max);

        max = MathUtilities.maximum(-1.0, 0.0, 1.0);
        assertTrue(1.0 == max);
        max = MathUtilities.maximum(-1.0, 1.0);
        assertTrue(1.0 == max);

        max = MathUtilities.maximum(-100000000.0, 0, 100000000.0);
        assertTrue(100000000.0 == max);
        max = MathUtilities.maximum(-100000000.0, 100000000.0);
        assertTrue(100000000.0 == max);

        double[] values = {45.1, -13.1, 123213123.1};
        assertTrue(123213123.1 == MathUtilities.maximum(values));
    }

    @Test
    public void testMaximumBigInteger()
    {
        BigInteger minBi = MathUtilities.minimum(new BigInteger("-1"), new BigInteger("0"), new BigInteger("1"));
        assertEquals(new BigInteger("-1"), minBi);
        minBi = MathUtilities.minimum(new BigInteger("-121908747902834709812347908123432423"), new BigInteger("0"), new BigInteger("9780234508972317045230477890478903240978234"));
        assertEquals(new BigInteger("-121908747902834709812347908123432423"), minBi);

        BigInteger[] bigies = new BigInteger[] {new BigInteger("1"), new BigInteger("-1")};
        assertEquals(new BigInteger("1"), MathUtilities.maximum(bigies));

        try
        {
            MathUtilities.maximum((BigInteger)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.minimum(new BigInteger[]{new BigInteger("1"), null, new BigInteger("3")});
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }

    @Test
    public void testMaximumBigDecimal()
    {
        BigDecimal minBd = MathUtilities.maximum(new BigDecimal("-1"), new BigDecimal("0"), new BigDecimal("1"));
        assertEquals(new BigDecimal("1"), minBd);
        minBd = MathUtilities.maximum(new BigDecimal("-121908747902834709812347908123432423.123"), new BigDecimal("0"), new BigDecimal("9780234508972317045230477890478903240978234.123"));
        assertEquals(new BigDecimal("9780234508972317045230477890478903240978234.123"), minBd);

        BigDecimal[] bigies = new BigDecimal[] {new BigDecimal("1.1"), new BigDecimal("-1.1")};
        assertEquals(new BigDecimal("1.1"), MathUtilities.maximum(bigies));

        try
        {
            MathUtilities.maximum((BigDecimal)null);
            fail("Should not make it here");
        }
        catch (Exception ignored) { }

        try
        {
            MathUtilities.maximum(new BigDecimal[]{new BigDecimal("1"), null, new BigDecimal("3")});
            fail("Should not make it here");
        }
        catch (Exception ignored) { }
    }
}
