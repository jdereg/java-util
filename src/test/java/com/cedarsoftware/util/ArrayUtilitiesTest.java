package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * useful Array utilities
 *
 * @author Keneth Partlow
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
public class ArrayUtilitiesTest
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class<?> c = ArrayUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(ArrayUtilities.isEmpty(new byte[]{}));
        assertTrue(ArrayUtilities.isEmpty(null));
        assertFalse(ArrayUtilities.isEmpty(new byte[]{5}));
        assertTrue(ArrayUtilities.isNotEmpty(new byte[]{5}));
        assertFalse(ArrayUtilities.isNotEmpty(null));
    }

    @Test
    public void testSize() {
        assertEquals(0, ArrayUtilities.size(new byte[]{}));
        assertEquals(0, ArrayUtilities.size(null));
        assertEquals(1, ArrayUtilities.size(new byte[]{5}));
    }

    @Test
    public void testShallowCopy() {
        String[] strings = new String[] { "foo", "bar", "baz"};
        String[] copy = (String[]) ArrayUtilities.shallowCopy(strings);
        assertNotSame(strings, copy);
        int i=0;
        for (String s: strings)
        {
            assertSame(s, copy[i++]);
        }

        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testAddAll() {
        assertEquals(0, ArrayUtilities.size(new byte[]{}));

        //  Test One
        Long[] one = new Long[] { 1L, 2L };
        Object[] resultOne = ArrayUtilities.addAll(null, one);
        assertNotSame(one, resultOne);
        for (int i=0; i<one.length; i++)
        {
            assertSame(one[i], resultOne[i]);
        }

        //  Test Two
        Long[] two = new Long[] { 3L, 4L };
        Object[] resultTwo = ArrayUtilities.addAll(two, null);
        assertNotSame(two, resultTwo);
        for (int i=0; i<two.length; i++) {
            assertSame(two[i], resultTwo[i]);
        }

        // Test Three
        Object[] resultThree = ArrayUtilities.addAll(one, two);
        assertNotSame(one, resultThree);
        assertNotSame(two, resultThree);
        for (int i=0; i<one.length; i++) {
            assertSame(one[i], resultThree[i]);
        }
        for (int i=0; i<two.length; i++) {
            assertSame(two[i], resultThree[i + one.length]);
        }
    }

    public void testInvalidClassDuringAddAll()
    {
        Long[] one = new Long[] { 1L, 2L };
        String[] two = new String[] {"foo", "bar"};
        try
        {
            ArrayUtilities.addAll(one, two);
            fail("should not make it here");
        }
        catch (ArrayStoreException e)
        {
        }
    }

    @Test
    public void testRemoveItem()
    {
        String[] strings = new String[] { "foo", "bar", "baz"};
        assertEquals(3, strings.length);

        String[] test1 = ArrayUtilities.removeItem(strings, 2);
        String[] subsetTest1 = ArrayUtilities.getArraySubset(strings, 0, 2);
        String[] expected1 = new String[] { "foo", "bar" };

        assertArrayEquals(expected1, test1);
        assertArrayEquals(expected1, subsetTest1);

        String[] test2 = ArrayUtilities.removeItem(strings, 0);
        String[] subsetTest2 = ArrayUtilities.getArraySubset(strings, 1, 3);
        String[] expected2 = new String[] { "bar", "baz" };
        assertArrayEquals(expected2, test2);
        assertArrayEquals(expected2, subsetTest2);

        String[] test3 = ArrayUtilities.removeItem(strings, 1);
        String[] expected3 = new String[] { "foo", "baz" };

        assertArrayEquals(expected3, test3);
    }

    @Test
    public void testToArray()
    {
        Collection<String> strings = new ArrayList<>();
        strings.add("foo");
        strings.add("bar");
        strings.add("baz");
        String[] strs = ArrayUtilities.toArray(String.class, strings);
        assert strs.length == 3;
        assert strs[0] == "foo";
        assert strs[1] == "bar";
        assert strs[2] == "baz";
    }

    @Test
    public void testCreateArray()
    {
        String[] base = {"a", "b"};
        String[] copy = ArrayUtilities.createArray(base);
        assertNotSame(base, copy);
        assertArrayEquals(base, copy);

        assertNull(ArrayUtilities.createArray((String[]) null));
    }

    @Test
    public void testNullToEmpty()
    {
        String[] result = ArrayUtilities.nullToEmpty(String.class, null);
        assertNotNull(result);
        assertEquals(0, result.length);

        String[] source = {"a"};
        assertSame(source, ArrayUtilities.nullToEmpty(String.class, source));
    }

    @Test
    public void testAddItemAndIndexOf()
    {
        String[] data = {"a", "b"};
        data = ArrayUtilities.addItem(String.class, data, "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, data);
        assertEquals(1, ArrayUtilities.indexOf(data, "b"));
        assertEquals(2, ArrayUtilities.lastIndexOf(data, "c"));
        assertTrue(ArrayUtilities.contains(data, "c"));
    }

    @Test
    public void testRemoveItemInvalid()
    {
        String[] data = {"x", "y"};
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(data, -1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(data, 2));
    }

    @Test
    public void testDeepCopyContainers_SimpleArray()
    {
        String[] original = {"a", "b", "c"};
        String[] copy = ArrayUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);
        // Verify berries are same references
        for (int i = 0; i < original.length; i++) {
            assertSame(original[i], copy[i]);
        }
    }

    @Test
    public void testDeepCopyContainers_MultiDimensionalArray()
    {
        String[][] original = {{"a", "b"}, {"c", "d", "e"}};
        String[][] copy = ArrayUtilities.deepCopyContainers(original);
        
        // All arrays should be different
        assertNotSame(original, copy);
        assertNotSame(original[0], copy[0]);
        assertNotSame(original[1], copy[1]);
        
        // But berries (strings) should be same references
        assertSame(original[0][0], copy[0][0]);
        assertSame(original[0][1], copy[0][1]);
        assertSame(original[1][0], copy[1][0]);
    }

    @Test
    public void testDeepCopyContainers_ArrayWithCollections()
    {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d", "e");
        Object[] original = {list1, list2, "standalone"};
        
        Object[] copy = ArrayUtilities.deepCopyContainers(original);
        
        // Array should be different
        assertNotSame(original, copy);
        
        // Collections should ALSO be different (deep copy of containers)
        assertNotSame(original[0], copy[0]);
        assertNotSame(original[1], copy[1]);
        
        // But the standalone string should be the same reference
        assertSame(original[2], copy[2]);
        
        // Collections should have same content
        assertEquals(list1, copy[0]);
        assertEquals(list2, copy[1]);
    }

    @Test
    public void testDeepCopyContainers_PrimitiveArrays()
    {
        int[] original = {1, 2, 3, 4, 5};
        int[] copy = ArrayUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);
    }

    @Test
    public void testDeepCopyContainers_NestedArraysWithCollections()
    {
        // Test deeply nested mixed structures
        List<String> innerList = Arrays.asList("x", "y");
        Object[][] original = {
            {innerList, "a"},
            {new String[]{"p", "q"}, "b"}
        };
        
        Object[][] copy = ArrayUtilities.deepCopyContainers(original);
        
        // All containers should be different
        assertNotSame(original, copy);
        assertNotSame(original[0], copy[0]);
        assertNotSame(original[1], copy[1]);
        assertNotSame(original[0][0], copy[0][0]); // List is also copied
        assertNotSame(original[1][0], copy[1][0]); // Nested array is also copied
        
        // But berries are same
        assertSame(original[0][1], copy[0][1]);
        assertSame(original[1][1], copy[1][1]);
        
        // Content is equal
        assertEquals(innerList, copy[0][0]);
        assertArrayEquals((String[])original[1][0], (String[])copy[1][0]);
    }

    @Test
    public void testDeepCopyContainers_NullHandling()
    {
        // Test null input
        assertNull(ArrayUtilities.deepCopyContainers(null));
        
        // Test array with nulls
        String[] original = {"a", null, "c"};
        String[] copy = ArrayUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);
        assertSame(original[0], copy[0]);
        assertNull(copy[1]);
        assertSame(original[2], copy[2]);
    }

    @Test
    public void testDeepCopyContainers_NonContainerInput()
    {
        // Non-containers return same reference
        String notAContainer = "hello";
        Object result = ArrayUtilities.deepCopyContainers(notAContainer);
        assertSame(notAContainer, result);
        
        // But collections ARE containers and get copied
        List<String> list = Arrays.asList("a", "b");
        List<String> listCopy = ArrayUtilities.deepCopyContainers(list);
        assertNotSame(list, listCopy);
        assertEquals(list, listCopy);
    }

    @Test
    public void testDeepCopyContainers_EmptyArrays()
    {
        // Test empty array
        String[] original = {};
        String[] copy = ArrayUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertEquals(0, copy.length);
        
        // Test empty 2D array
        String[][] original2D = {{}};
        String[][] copy2D = ArrayUtilities.deepCopyContainers(original2D);
        
        assertNotSame(original2D, copy2D);
        assertNotSame(original2D[0], copy2D[0]);
        assertEquals(1, copy2D.length);
        assertEquals(0, copy2D[0].length);
    }

    @Test
    public void testDeepCopyContainers_CircularReference()
    {
        // Test circular reference handling
        Object[] array1 = new Object[2];
        Object[] array2 = new Object[2];
        array1[0] = "a";
        array1[1] = array2;
        array2[0] = "b";
        array2[1] = array1; // Circular reference
        
        Object[] copy = ArrayUtilities.deepCopyContainers(array1);
        
        // Should create new arrays
        assertNotSame(array1, copy);
        assertNotSame(array1[1], copy[1]);
        
        // But maintain the circular structure
        assertSame(copy, ((Object[])copy[1])[1]);
        
        // Berries are same
        assertSame("a", copy[0]);
        assertSame("b", ((Object[])copy[1])[0]);
    }

    @Test
    void testSetObjectArray()
    {
        // Test that setPrimitiveElement() throws exception for Object[] arrays
        // Object[] should use direct assignment instead
        Object[] array = new Object[5];

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            ArrayUtilities.setPrimitiveElement(array, 0, "Hello");
        });
        assertTrue(e.getMessage().contains("setPrimitiveElement() should only be used for primitive arrays"));
        assertTrue(e.getMessage().contains("java.lang.Object[]"));

        // Verify direct assignment works correctly
        array[0] = "Hello";
        array[1] = 42;
        array[2] = null;
        array[3] = new StringBuilder("test");
        array[4] = new int[]{1, 2, 3};

        assertEquals("Hello", array[0]);
        assertEquals(42, array[1]);
        assertNull(array[2]);
        assertEquals("test", array[3].toString());
        assertArrayEquals(new int[]{1, 2, 3}, (int[])array[4]);
    }

    @Test
    void testSetStringArray()
    {
        // Test that setPrimitiveElement() throws exception for String[] arrays
        // String[] should use direct assignment instead
        String[] strings = new String[3];

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            ArrayUtilities.setPrimitiveElement(strings, 0, "First");
        });
        assertTrue(e.getMessage().contains("setPrimitiveElement() should only be used for primitive arrays"));
        assertTrue(e.getMessage().contains("java.lang.String[]"));

        // Verify direct assignment works correctly
        strings[0] = "First";
        strings[1] = "Second";
        strings[2] = null;

        assertEquals("First", strings[0]);
        assertEquals("Second", strings[1]);
        assertNull(strings[2]);
    }

    @Test
    void testSetIntArray()
    {
        // Test int[] array with Integer objects
        int[] numbers = new int[5];

        ArrayUtilities.setPrimitiveElement(numbers, 0, 10);
        ArrayUtilities.setPrimitiveElement(numbers, 1, Integer.valueOf(20));
        ArrayUtilities.setPrimitiveElement(numbers, 2, null);  // Should set to 0
        ArrayUtilities.setPrimitiveElement(numbers, 3, 30);
        ArrayUtilities.setPrimitiveElement(numbers, 4, -40);

        assertEquals(10, numbers[0]);
        assertEquals(20, numbers[1]);
        assertEquals(0, numbers[2]);  // null converts to 0
        assertEquals(30, numbers[3]);
        assertEquals(-40, numbers[4]);
    }

    @Test
    void testSetLongArray()
    {
        // Test long[] array
        long[] longs = new long[4];

        ArrayUtilities.setPrimitiveElement(longs, 0, 100L);
        ArrayUtilities.setPrimitiveElement(longs, 1, Long.valueOf(200L));
        ArrayUtilities.setPrimitiveElement(longs, 2, null);  // Should set to 0L
        ArrayUtilities.setPrimitiveElement(longs, 3, Integer.valueOf(300));  // Auto-convert from int

        assertEquals(100L, longs[0]);
        assertEquals(200L, longs[1]);
        assertEquals(0L, longs[2]);
        assertEquals(300L, longs[3]);
    }

    @Test
    void testSetDoubleArray()
    {
        // Test double[] array
        double[] doubles = new double[4];

        ArrayUtilities.setPrimitiveElement(doubles, 0, 1.5);
        ArrayUtilities.setPrimitiveElement(doubles, 1, Double.valueOf(2.5));
        ArrayUtilities.setPrimitiveElement(doubles, 2, null);  // Should set to 0.0
        ArrayUtilities.setPrimitiveElement(doubles, 3, Integer.valueOf(3));  // Auto-convert from int

        assertEquals(1.5, doubles[0], 0.001);
        assertEquals(2.5, doubles[1], 0.001);
        assertEquals(0.0, doubles[2], 0.001);
        assertEquals(3.0, doubles[3], 0.001);
    }

    @Test
    void testSetFloatArray()
    {
        // Test float[] array
        float[] floats = new float[3];

        ArrayUtilities.setPrimitiveElement(floats, 0, 1.5f);
        ArrayUtilities.setPrimitiveElement(floats, 1, Float.valueOf(2.5f));
        ArrayUtilities.setPrimitiveElement(floats, 2, null);  // Should set to 0.0f

        assertEquals(1.5f, floats[0], 0.001f);
        assertEquals(2.5f, floats[1], 0.001f);
        assertEquals(0.0f, floats[2], 0.001f);
    }

    @Test
    void testSetBooleanArray()
    {
        // Test boolean[] array
        boolean[] booleans = new boolean[4];

        ArrayUtilities.setPrimitiveElement(booleans, 0, true);
        ArrayUtilities.setPrimitiveElement(booleans, 1, Boolean.valueOf(false));
        ArrayUtilities.setPrimitiveElement(booleans, 2, null);  // Should set to false
        ArrayUtilities.setPrimitiveElement(booleans, 3, Boolean.TRUE);

        assertTrue(booleans[0]);
        assertFalse(booleans[1]);
        assertFalse(booleans[2]);  // null converts to false
        assertTrue(booleans[3]);
    }

    @Test
    void testSetByteArray()
    {
        // Test byte[] array
        byte[] bytes = new byte[4];

        ArrayUtilities.setPrimitiveElement(bytes, 0, (byte)10);
        ArrayUtilities.setPrimitiveElement(bytes, 1, Byte.valueOf((byte)20));
        ArrayUtilities.setPrimitiveElement(bytes, 2, null);  // Should set to 0
        ArrayUtilities.setPrimitiveElement(bytes, 3, Integer.valueOf(30));  // Auto-convert from int

        assertEquals((byte)10, bytes[0]);
        assertEquals((byte)20, bytes[1]);
        assertEquals((byte)0, bytes[2]);
        assertEquals((byte)30, bytes[3]);
    }

    @Test
    void testSetCharArray()
    {
        // Test char[] array
        char[] chars = new char[5];

        ArrayUtilities.setPrimitiveElement(chars, 0, 'A');
        ArrayUtilities.setPrimitiveElement(chars, 1, Character.valueOf('B'));
        ArrayUtilities.setPrimitiveElement(chars, 2, null);  // Should set to '\0'
        ArrayUtilities.setPrimitiveElement(chars, 3, "C");  // String conversion
        ArrayUtilities.setPrimitiveElement(chars, 4, "Hello");  // Takes first char

        assertEquals('A', chars[0]);
        assertEquals('B', chars[1]);
        assertEquals('\0', chars[2]);
        assertEquals('C', chars[3]);
        assertEquals('H', chars[4]);  // First char of "Hello"
    }

    @Test
    void testSetShortArray()
    {
        // Test short[] array
        short[] shorts = new short[4];

        ArrayUtilities.setPrimitiveElement(shorts, 0, (short)100);
        ArrayUtilities.setPrimitiveElement(shorts, 1, Short.valueOf((short)200));
        ArrayUtilities.setPrimitiveElement(shorts, 2, null);  // Should set to 0
        ArrayUtilities.setPrimitiveElement(shorts, 3, Integer.valueOf(300));  // Auto-convert from int

        assertEquals((short)100, shorts[0]);
        assertEquals((short)200, shorts[1]);
        assertEquals((short)0, shorts[2]);
        assertEquals((short)300, shorts[3]);
    }

    @Test
    void testSetArrayIndexOutOfBounds()
    {
        // Test index out of bounds
        int[] array = new int[3];

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ArrayUtilities.setPrimitiveElement(array, 5, 10);
        });

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ArrayUtilities.setPrimitiveElement(array, -1, 10);
        });
    }

    @Test
    void testSetArrayTypeMismatch()
    {
        // Test type mismatch for Object[]
        String[] strings = new String[3];

        assertThrows(IllegalArgumentException.class, () -> {
            ArrayUtilities.setPrimitiveElement(strings, 0, new Integer(42));  // Integer can't go in String[]
        });
    }

    @Test
    void testSetNullArray()
    {
        // Test null array
        assertThrows(NullPointerException.class, () -> {
            ArrayUtilities.setPrimitiveElement(null, 0, "value");
        });
    }

    @Test
    void testSetPerformanceComparison()
    {
        // Performance test showing ArrayUtilities.setPrimitiveElement() is faster than Array.set()
        // This is more of a validation that our optimization works correctly
        int[] numbers = new int[1000];

        // Using ArrayUtilities.setPrimitiveElement() should work correctly
        for (int i = 0; i < numbers.length; i++) {
            ArrayUtilities.setPrimitiveElement(numbers, i, i * 2);
        }

        // Verify values
        for (int i = 0; i < numbers.length; i++) {
            assertEquals(i * 2, numbers[i]);
        }
    }

    @Test
    void testSetMixedTypes()
    {
        // Test that Number types can be converted to primitive arrays
        int[] ints = new int[3];

        ArrayUtilities.setPrimitiveElement(ints, 0, Integer.valueOf(10));
        ArrayUtilities.setPrimitiveElement(ints, 1, Long.valueOf(20L));     // Long to int
        ArrayUtilities.setPrimitiveElement(ints, 2, Double.valueOf(30.7));  // Double to int

        assertEquals(10, ints[0]);
        assertEquals(20, ints[1]);
        assertEquals(30, ints[2]);  // 30.7 truncated to 30
    }
}
