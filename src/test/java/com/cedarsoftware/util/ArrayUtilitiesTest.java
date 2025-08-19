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
}
