package com.cedarsoftware.util.convert;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ArrayConversions.
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
class ArrayConversionsTest {

    private enum Color { RED, GREEN, BLUE }

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ---- enumSetToArray bug: Long[] target gets int ordinal → ArrayStoreException ----

    @Test
    void enumSetToArray_toLongObjectArray() {
        EnumSet<Color> colors = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
        Long[] result = (Long[]) ArrayConversions.enumSetToArray(colors, Long[].class);

        assertEquals(3, result.length);
        // EnumSet iterates in ordinal order
        assertEquals(0L, result[0]);
        assertEquals(1L, result[1]);
        assertEquals(2L, result[2]);
    }

    @Test
    void enumSetToArray_toLongPrimitiveArray() {
        EnumSet<Color> colors = EnumSet.of(Color.RED, Color.BLUE);
        long[] result = (long[]) ArrayConversions.enumSetToArray(colors, long[].class);

        assertEquals(2, result.length);
        assertEquals(0L, result[0]);
        assertEquals(2L, result[1]);
    }

    @Test
    void enumSetToArray_toIntegerObjectArray() {
        EnumSet<Color> colors = EnumSet.of(Color.GREEN);
        Integer[] result = (Integer[]) ArrayConversions.enumSetToArray(colors, Integer[].class);

        assertEquals(1, result.length);
        assertEquals(1, result[0]);
    }

    @Test
    void enumSetToArray_toIntPrimitiveArray() {
        EnumSet<Color> colors = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
        int[] result = (int[]) ArrayConversions.enumSetToArray(colors, int[].class);

        assertArrayEquals(new int[]{0, 1, 2}, result);
    }

    @Test
    void enumSetToArray_toStringArray() {
        EnumSet<Color> colors = EnumSet.of(Color.RED, Color.BLUE);
        String[] result = (String[]) ArrayConversions.enumSetToArray(colors, String[].class);

        assertEquals(2, result.length);
        assertEquals("RED", result[0]);
        assertEquals("BLUE", result[1]);
    }

    @Test
    void enumSetToArray_toShortObjectArray() {
        EnumSet<Color> colors = EnumSet.of(Color.GREEN);
        Short[] result = (Short[]) ArrayConversions.enumSetToArray(colors, Short[].class);

        assertEquals(1, result.length);
        assertEquals((short) 1, result[0]);
    }

    @Test
    void enumSetToArray_toByteObjectArray() {
        EnumSet<Color> colors = EnumSet.of(Color.RED);
        Byte[] result = (Byte[]) ArrayConversions.enumSetToArray(colors, Byte[].class);

        assertEquals(1, result.length);
        assertEquals((byte) 0, result[0]);
    }

    @Test
    void enumSetToArray_emptyEnumSet() {
        EnumSet<Color> empty = EnumSet.noneOf(Color.class);
        Long[] result = (Long[]) ArrayConversions.enumSetToArray(empty, Long[].class);
        assertEquals(0, result.length);
    }

    // ---- arrayToArray: primitive/wrapper compatibility (PERF verification) ----

    @Test
    void arrayToArray_integerArrayToIntArray() {
        Integer[] source = {1, 2, 3};
        int[] result = (int[]) ArrayConversions.arrayToArray(source, int[].class, converter);
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    @Test
    void arrayToArray_intArrayToIntegerArray() {
        int[] source = {10, 20, 30};
        Integer[] result = (Integer[]) ArrayConversions.arrayToArray(source, Integer[].class, converter);
        assertArrayEquals(new Integer[]{10, 20, 30}, result);
    }

    @Test
    void arrayToArray_longArrayToLongObjectArray() {
        long[] source = {100L, 200L};
        Long[] result = (Long[]) ArrayConversions.arrayToArray(source, Long[].class, converter);
        assertArrayEquals(new Long[]{100L, 200L}, result);
    }

    @Test
    void arrayToArray_longObjectArrayToLongPrimitiveArray() {
        Long[] source = {100L, 200L};
        long[] result = (long[]) ArrayConversions.arrayToArray(source, long[].class, converter);
        assertArrayEquals(new long[]{100L, 200L}, result);
    }
}
