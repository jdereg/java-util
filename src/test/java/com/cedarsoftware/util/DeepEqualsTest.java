package com.cedarsoftware.util;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.agrona.collections.Object2ObjectHashMap;
import org.junit.jupiter.api.Test;

import static java.lang.Math.E;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt
 * @author sapradhan8
 * <br>
 *         Licensed under the Apache License, Version 2.0 (the "License"); you
 *         may not use this file except in compliance with the License. You may
 *         obtain a copy of the License at <br>
 * <br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a> <br>
 * <br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *         implied. See the License for the specific language governing
 *         permissions and limitations under the License.
 */
public class DeepEqualsTest
{
    @Test
    public void testSameObjectEquals()
    {
        Date date1 = new Date();
        Date date2 = date1;
        assertTrue(DeepEquals.deepEquals(date1, date2));
    }

	@Test
	public void testEqualsWithNull()
    {
		Date date1 = new Date();
		assertFalse(DeepEquals.deepEquals(null, date1));
		assertFalse(DeepEquals.deepEquals(date1, null));
	}

	@Test
    public void testDeepEqualsWithOptions()
    {
        Person p1 = new Person("Jim Bob", 27);
        Person p2 = new Person("Jim Bob", 34);
        assert p1.equals(p2);
        assert DeepEquals.deepEquals(p1, p2);

        Map<String, Object> options = new HashMap<>();
        Set<Class<?>> skip = new HashSet<>();
        skip.add(Person.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert !DeepEquals.deepEquals(p1, p2, options);       // told to skip Person's .equals() - so it will compare all fields

        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, new HashSet<>());
        assert !DeepEquals.deepEquals(p1, p2, options);       // told to skip all custom .equals() - so it will compare all fields

        skip.clear();
        skip.add(Point.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert DeepEquals.deepEquals(p1, p2, options);        // Not told to skip Person's .equals() - so it will compare on name only
    }

    @Test
    public void testBigDecimal()
    {
        BigDecimal ten = new BigDecimal("10.0");
        assert DeepEquals.deepEquals(ten, 10.0f);
        assert DeepEquals.deepEquals(ten, 10.0d);
        assert DeepEquals.deepEquals(ten, 10);
        assert DeepEquals.deepEquals(ten, 10l);
        assert DeepEquals.deepEquals(ten, new BigInteger("10"));
        assert DeepEquals.deepEquals(ten, new AtomicLong(10L));
        assert DeepEquals.deepEquals(ten, new AtomicInteger(10));

        assert !DeepEquals.deepEquals(ten, 10.01f);
        assert !DeepEquals.deepEquals(ten, 10.01d);
        assert !DeepEquals.deepEquals(ten, 11);
        assert !DeepEquals.deepEquals(ten, 11l);
        assert !DeepEquals.deepEquals(ten, new BigInteger("11"));
        assert !DeepEquals.deepEquals(ten, new AtomicLong(11L));
        assert !DeepEquals.deepEquals(ten, new AtomicInteger(11));

        BigDecimal x = new BigDecimal(new BigInteger("1"), -1);
        assert DeepEquals.deepEquals(ten, x);
        x = new BigDecimal(new BigInteger("1"), -2);
        assert !DeepEquals.deepEquals(ten, x);

        assert !DeepEquals.deepEquals(ten, TimeZone.getDefault());
        assert !DeepEquals.deepEquals(ten, "10");

        assert DeepEquals.deepEquals(0.1d, new BigDecimal("0.1"));
        assert DeepEquals.deepEquals(0.04d, new BigDecimal("0.04"));
        assert DeepEquals.deepEquals(0.1f, new BigDecimal("0.1").floatValue());
        assert DeepEquals.deepEquals(0.04f, new BigDecimal("0.04").floatValue());
    }

    @Test
    public void testBigInteger()
    {
        BigInteger ten = new BigInteger("10");
        assert DeepEquals.deepEquals(ten, new BigInteger("10"));
        assert !DeepEquals.deepEquals(ten, new BigInteger("11"));
        assert DeepEquals.deepEquals(ten, 10.0f);
        assert !DeepEquals.deepEquals(ten, 11.0f);
        assert DeepEquals.deepEquals(ten, 10.0d);
        assert !DeepEquals.deepEquals(ten, 11.0d);
        assert DeepEquals.deepEquals(ten, 10);
        assert DeepEquals.deepEquals(ten, 10l);
        assert DeepEquals.deepEquals(ten, new BigDecimal("10.0"));
        assert DeepEquals.deepEquals(ten, new AtomicLong(10L));
        assert DeepEquals.deepEquals(ten, new AtomicInteger(10));

        assert !DeepEquals.deepEquals(ten, 10.01f);
        assert !DeepEquals.deepEquals(ten, 10.01d);
        assert !DeepEquals.deepEquals(ten, 11);
        assert !DeepEquals.deepEquals(ten, 11l);
        assert !DeepEquals.deepEquals(ten, new BigDecimal("10.001"));
        assert !DeepEquals.deepEquals(ten, new BigDecimal("11"));
        assert !DeepEquals.deepEquals(ten, new AtomicLong(11L));
        assert !DeepEquals.deepEquals(ten, new AtomicInteger(11));

        assert !DeepEquals.deepEquals(ten, TimeZone.getDefault());
        assert !DeepEquals.deepEquals(ten, "10");

        assert !DeepEquals.deepEquals(new BigInteger("1"), new BigDecimal("0.99999999999999999999999999999"));
    }

    @Test
    public void testDifferentNumericTypes()
    {
        assert DeepEquals.deepEquals(1.0f, 1L);
        assert DeepEquals.deepEquals(1.0d, 1L);
        assert DeepEquals.deepEquals(1L, 1.0f);
        assert DeepEquals.deepEquals(1L, 1.0d);
        assert !DeepEquals.deepEquals(1, TimeZone.getDefault());

        long x = Integer.MAX_VALUE;
        assert DeepEquals.deepEquals(Integer.MAX_VALUE, x);
        assert DeepEquals.deepEquals(x, Integer.MAX_VALUE);
        assert !DeepEquals.deepEquals(Integer.MAX_VALUE, x + 1);
        assert !DeepEquals.deepEquals(x + 1, Integer.MAX_VALUE);

        x = Integer.MIN_VALUE;
        assert DeepEquals.deepEquals(Integer.MIN_VALUE, x);
        assert DeepEquals.deepEquals(x, Integer.MIN_VALUE);
        assert !DeepEquals.deepEquals(Integer.MIN_VALUE, x - 1);
        assert !DeepEquals.deepEquals(x - 1, Integer.MIN_VALUE);

        BigDecimal y = new BigDecimal("1.7976931348623157e+308");
        assert DeepEquals.deepEquals(Double.MAX_VALUE, y);
        assert DeepEquals.deepEquals(y, Double.MAX_VALUE);
        y = y.add(BigDecimal.ONE);
        assert !DeepEquals.deepEquals(Double.MAX_VALUE, y);
        assert !DeepEquals.deepEquals(y, Double.MAX_VALUE);

        y = new BigDecimal("4.9e-324");
        assert DeepEquals.deepEquals(Double.MIN_VALUE, y);
        assert DeepEquals.deepEquals(y, Double.MIN_VALUE);
        y = y.subtract(BigDecimal.ONE);
        assert !DeepEquals.deepEquals(Double.MIN_VALUE, y);
        assert !DeepEquals.deepEquals(y, Double.MIN_VALUE);

        x = Byte.MAX_VALUE;
        assert DeepEquals.deepEquals((byte)127, x);
        assert DeepEquals.deepEquals(x, (byte)127);
        x++;
        assert !DeepEquals.deepEquals((byte)127, x);
        assert !DeepEquals.deepEquals(x, (byte)127);

        x = Byte.MIN_VALUE;
        assert DeepEquals.deepEquals((byte)-128, x);
        assert DeepEquals.deepEquals(x, (byte)-128);
        x--;
        assert !DeepEquals.deepEquals((byte)-128, x);
        assert !DeepEquals.deepEquals(x, (byte)-128);
    }

    @Test
    public void testAtomicStuff()
    {
        AtomicWrapper atomic1 = new AtomicWrapper(35);
        AtomicWrapper atomic2 = new AtomicWrapper(35);
        AtomicWrapper atomic3 = new AtomicWrapper(42);

        assert DeepEquals.deepEquals(atomic1, atomic2);
        assert !DeepEquals.deepEquals(atomic1, atomic3);

        Map<String, Object> options = new HashMap<>();
        Set<Class> skip = new HashSet<>();
        skip.add(AtomicWrapper.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert DeepEquals.deepEquals(atomic1, atomic2, options);
        assert !DeepEquals.deepEquals(atomic1, atomic3, options);

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(true);

        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, new HashSet<>());
        assert !DeepEquals.deepEquals(b1, b2);
        assert DeepEquals.deepEquals(b1, b3);
        assert !DeepEquals.deepEquals(b1, b2, options);
        assert DeepEquals.deepEquals(b1, b3, options);
    }

	@Test
	public void testDifferentClasses()
    {
		assertFalse(DeepEquals.deepEquals(new Date(), "test"));
	}

    @Test
    public void testPOJOequals()
    {
        Class1 x = new Class1(true, tan(PI / 4), 1);
        Class1 y = new Class1(true, 1.0, 1);
        assertTrue(DeepEquals.deepEquals(x, y));
        assertFalse(DeepEquals.deepEquals(x, new Class1()));

        Class2 a = new Class2((float) atan(1.0), "hello", (short) 2,
                new Class1(false, sin(0.75), 5));
        Class2 b = new Class2((float) PI / 4, "hello", (short) 2,
                new Class1(false, 2 * cos(0.75 / 2) * sin(0.75 / 2), 5)
        );

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, new Class2()));
    }

	@Test
	public void testPrimitiveArrays()
    {
		int array1[] = { 2, 4, 5, 6, 3, 1, 3, 3, 5, 22 };
		int array2[] = { 2, 4, 5, 6, 3, 1, 3, 3, 5, 22 };

		assertTrue(DeepEquals.deepEquals(array1, array2));

		int array3[] = { 3, 4, 7 };

		assertFalse(DeepEquals.deepEquals(array1, array3));

		float array4[] = { 3.4f, 5.5f };
		assertFalse(DeepEquals.deepEquals(array1, array4));
	}

	@Test
	public void testArrayOrder()
    {
		int array1[] = { 3, 4, 7 };
		int array2[] = { 7, 3, 4 };

        int x = DeepEquals.deepHashCode(array1);
        int y = DeepEquals.deepHashCode(array2);
        assertNotEquals(x, y);

        assertFalse(DeepEquals.deepEquals(array1, array2));
	}

	@Test
	public void testOrderedCollection()
    {
        List<String> a = asList("one", "two", "three", "four", "five");
		List<String> b = new LinkedList<>(a);

		assertTrue(DeepEquals.deepEquals(a, b));

		List<Integer> c = asList(1, 2, 3, 4, 5);

		assertFalse(DeepEquals.deepEquals(a, c));

		List<Integer> d = asList(4, 6);

		assertFalse(DeepEquals.deepEquals(c, d));

		List<Class1> x1 = asList(new Class1(true, log(pow(E, 2)), 6), new Class1(true, tan(PI / 4), 1));
		List<Class1> x2 = asList(new Class1(true, 2, 6), new Class1(true, 1, 1));
		assertTrue(DeepEquals.deepEquals(x1, x2));
	}

    @Test
    public void testOrderedDoubleCollection() {
        List<Number> aa = asList(log(pow(E, 2)), tan(PI / 4));
        List<Number> bb = asList(2.0, 1.0);
        List<Number> cc = asList(1.0, 2.0);
        assertEquals(DeepEquals.deepHashCode(aa), DeepEquals.deepHashCode(bb));
        assertNotEquals(DeepEquals.deepHashCode(aa), DeepEquals.deepHashCode(cc));
        assertNotEquals(DeepEquals.deepHashCode(bb), DeepEquals.deepHashCode(cc));
    }

    @Test
    public void testOrderedFloatCollection() {
        List<Number> aa = asList((float)log(pow(E, 2)), (float)tan(PI / 4));
        List<Number> bb = asList(2.0f, 1.0f);
        List<Number> cc = asList(1.0f, 2.0f);
        assertEquals(DeepEquals.deepHashCode(aa), DeepEquals.deepHashCode(bb));
        assertNotEquals(DeepEquals.deepHashCode(aa), DeepEquals.deepHashCode(cc));
        assertNotEquals(DeepEquals.deepHashCode(bb), DeepEquals.deepHashCode(cc));
    }

    @Test
	public void testUnorderedCollection()
    {
        Set<String> a = new HashSet<>(asList("one", "two", "three", "four", "five"));
		Set<String> b = new HashSet<>(asList("three", "five", "one", "four", "two"));
		assertTrue(DeepEquals.deepEquals(a, b));

		Set<Integer> c = new HashSet<>(asList(1, 2, 3, 4, 5));
		assertFalse(DeepEquals.deepEquals(a, c));

		Set<Integer> d = new HashSet<>(asList(4, 2, 6));
		assertFalse(DeepEquals.deepEquals(c, d));

		Set<Class1> x1 = new LinkedHashSet<>();
        x1.add(new Class1(true, log(pow(E, 2)), 6));
        x1.add(new Class1(true, tan(PI / 4), 1));

		Set<Class1> x2 = new HashSet<>();
        x2.add(new Class1(true, 1, 1));
        x2.add(new Class1(true, 2, 6));

        int x = DeepEquals.deepHashCode(x1);
        int y = DeepEquals.deepHashCode(x2);

        assertEquals(x, y);
        assertTrue(DeepEquals.deepEquals(x1, x2));
        
		// Proves that objects are being compared against the correct objects in each collection (all objects have same
        // hash code, so the unordered compare must handle checking item by item for hash-collided items)
		Set<DumbHash> d1 = new LinkedHashSet<>();
		Set<DumbHash> d2 = new LinkedHashSet<>();
		d1.add(new DumbHash("alpha"));
		d1.add(new DumbHash("bravo"));
		d1.add(new DumbHash("charlie"));
		
		d2.add(new DumbHash("bravo"));
		d2.add(new DumbHash("alpha"));
		d2.add(new DumbHash("charlie"));
		assert DeepEquals.deepEquals(d1, d2);

        d2.clear();
        d2.add(new DumbHash("bravo"));
        d2.add(new DumbHash("alpha"));
        d2.add(new DumbHash("delta"));
        assert !DeepEquals.deepEquals(d2, d1);
    }

    @Test
    public void testSetOrder() {
        Set<String> a = new LinkedHashSet<>();
        Set<String> b = new LinkedHashSet<>();
        a.add("a");
        a.add("b");
        a.add("c");

        b.add("c");
        b.add("a");
        b.add("b");
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEquivalentMaps()
    {
        Map<String, Object> map1 = new LinkedHashMap<>();
        fillMap(map1);
        Map<String, Object> map2 = new HashMap<>();
        fillMap(map2);
        assertTrue(DeepEquals.deepEquals(map1, map2));
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));

        map1 = new TreeMap<>();
        fillMap(map1);
        map2 = new TreeMap<>();
        map2 = Collections.synchronizedSortedMap((SortedMap) map2);
        fillMap(map2);
        assertTrue(DeepEquals.deepEquals(map1, map2));
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));

        // Uses flyweight entries
        map1 = new Object2ObjectHashMap();
        fillMap(map1);
        map2 = new Object2ObjectHashMap();
        fillMap(map2);
        assertTrue(DeepEquals.deepEquals(map1, map2));
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));
    }

    @Test
    public void testUnorderedMapsWithKeyHashCodeCollisions()
    {
        Map<DumbHash, String> map1 = new LinkedHashMap<>();
        map1.put(new DumbHash("alpha"), "alpha");
        map1.put(new DumbHash("bravo"), "bravo");
        map1.put(new DumbHash("charlie"), "charlie");

        Map<DumbHash, String> map2 = new LinkedHashMap<>();
        map2.put(new DumbHash("bravo"), "bravo");
        map2.put(new DumbHash("alpha"), "alpha");
        map2.put(new DumbHash("charlie"), "charlie");

        assert DeepEquals.deepEquals(map1, map2);

        map2.clear();
        map2.put(new DumbHash("bravo"), "bravo");
        map2.put(new DumbHash("alpha"), "alpha");
        map2.put(new DumbHash("delta"), "delta");
        assert !DeepEquals.deepEquals(map1, map2);
    }

    @Test
    public void testUnorderedMapsWithValueHashCodeCollisions()
    {
        Map<String, DumbHash> map1 = new LinkedHashMap<>();
        map1.put("alpha", new DumbHash("alpha"));
        map1.put("bravo", new DumbHash("bravo"));
        map1.put("charlie", new DumbHash("charlie"));

        Map<String, DumbHash> map2 = new LinkedHashMap<>();
        map2.put("bravo", new DumbHash("bravo"));
        map2.put("alpha", new DumbHash("alpha"));
        map2.put("charlie", new DumbHash("charlie"));

        assert DeepEquals.deepEquals(map1, map2);

        map2.clear();
        map2.put("bravo", new DumbHash("bravo"));
        map2.put("alpha", new DumbHash("alpha"));
        map2.put("delta", new DumbHash("delta"));
        assert !DeepEquals.deepEquals(map1, map2);
    }

    @Test
    public void testUnorderedMapsWithKeyValueHashCodeCollisions()
    {
        Map<DumbHash, DumbHash> map1 = new LinkedHashMap<>();
        map1.put(new DumbHash("alpha"), new DumbHash("alpha"));
        map1.put(new DumbHash("bravo"), new DumbHash("bravo"));
        map1.put(new DumbHash("charlie"), new DumbHash("charlie"));

        Map<DumbHash, DumbHash> map2 = new LinkedHashMap<>();
        map2.put(new DumbHash("bravo"), new DumbHash("bravo"));
        map2.put(new DumbHash("alpha"), new DumbHash("alpha"));
        map2.put(new DumbHash("charlie"), new DumbHash("charlie"));

        assert DeepEquals.deepEquals(map1, map2);

        map2.clear();
        map2.put(new DumbHash("bravo"), new DumbHash("bravo"));
        map2.put(new DumbHash("alpha"), new DumbHash("alpha"));
        map2.put(new DumbHash("delta"), new DumbHash("delta"));
        assert !DeepEquals.deepEquals(map1, map2);
    }

    @Test
    public void testInequivalentMaps()
    {
        Map<String, Object> map1 = new TreeMap<>();
        fillMap(map1);
        Map<String, Object> map2 = new HashMap<>();
        fillMap(map2);
        // Sorted versus non-sorted Map
        assertTrue(DeepEquals.deepEquals(map1, map2));

        // Hashcodes are equals because the Maps have same elements
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));

        map2 = new TreeMap<>();
        fillMap(map2);
        map2.remove("kilo");
        assertFalse(DeepEquals.deepEquals(map1, map2));

        // Hashcodes are different because contents of maps are different
        assertNotEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));

        // Inequality because ConcurrentSkipListMap is a SortedMap
        map1 = new HashMap<>();
        fillMap(map1);
        map2 = new ConcurrentSkipListMap<>();
        fillMap(map2);
        assertTrue(DeepEquals.deepEquals(map1, map2));

        map1 = new TreeMap<>();
        fillMap(map1);
        map2 = new ConcurrentSkipListMap<>();
        fillMap(map2);
        assertTrue(DeepEquals.deepEquals(map1, map2));
        map2.remove("papa");
        assertFalse(DeepEquals.deepEquals(map1, map2));

        map1 = new HashMap<>();
        map1.put("foo", "bar");
        map1.put("baz", "qux");
        map2 = new HashMap<>();
        map2.put("foo", "bar");
        assert !DeepEquals.deepEquals(map1, map2);
    }

    @Test
    public void testNumbersAndStrings()
    {
        Map<String, Boolean> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assert !DeepEquals.deepEquals("10", 10);
        assert DeepEquals.deepEquals("10", 10, options);
        assert DeepEquals.deepEquals(10, "10", options);
        assert DeepEquals.deepEquals(10, "10.0", options);
        assert DeepEquals.deepEquals(10.0f, "10.0", options);
        assert DeepEquals.deepEquals(10.0f, "10", options);
        assert DeepEquals.deepEquals(10.0d, "10.0", options);
        assert DeepEquals.deepEquals(10.0d, "10", options);
        assert !DeepEquals.deepEquals(10.0d, "10.01", options);
        assert !DeepEquals.deepEquals(10.0d, "10.0d", options);
        assert DeepEquals.deepEquals(new BigDecimal("3.14159"), 3.14159d, options);
        assert !DeepEquals.deepEquals(new BigDecimal("3.14159"), "3.14159");
        assert DeepEquals.deepEquals(new BigDecimal("3.14159"), "3.14159", options);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEquivalentCollections()
    {
        // ordered Collection
        Collection<String> col1 = new ArrayList<>();
        fillCollection(col1);
        Collection<String> col2 = new LinkedList<>();
        fillCollection(col2);
        assertTrue(DeepEquals.deepEquals(col1, col2));
        assertEquals(DeepEquals.deepHashCode(col1), DeepEquals.deepHashCode(col2));

        // unordered Collections (Set)
        col1 = new LinkedHashSet<>();
        fillCollection(col1);
        col2 = new HashSet<>();
        fillCollection(col2);
        assertTrue(DeepEquals.deepEquals(col1, col2));
        assertEquals(DeepEquals.deepHashCode(col1), DeepEquals.deepHashCode(col2));

        col1 = new TreeSet<>();
        fillCollection(col1);
        col2 = new TreeSet<>();
        Collections.synchronizedSortedSet((SortedSet) col2);
        fillCollection(col2);
        assertTrue(DeepEquals.deepEquals(col1, col2));
        assertEquals(DeepEquals.deepHashCode(col1), DeepEquals.deepHashCode(col2));
    }

    @Test
    public void testInequivalentCollections()
    {
        Collection<String> col1 = new TreeSet<>();
        fillCollection(col1);
        Collection<String> col2 = new HashSet<>();
        fillCollection(col2);
        assertTrue(DeepEquals.deepEquals(col1, col2));
        assertEquals(DeepEquals.deepHashCode(col1), DeepEquals.deepHashCode(col2));

        col2 = new TreeSet<>();
        fillCollection(col2);
        col2.remove("lima");
        assertFalse(DeepEquals.deepEquals(col1, col2));
        assertNotEquals(DeepEquals.deepHashCode(col1), DeepEquals.deepHashCode(col2));

        assertFalse(DeepEquals.deepEquals(new HashMap<>(), new ArrayList<>()));
        assertFalse(DeepEquals.deepEquals(new ArrayList<>(), new HashMap<>()));
    }

    @Test
    public void testArray()
    {
        Object[] a1 = new Object[] {"alpha", "bravo", "charlie", "delta"};
        Object[] a2 = new Object[] {"alpha", "bravo", "charlie", "delta"};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
        a2[3] = "echo";
        assertFalse(DeepEquals.deepEquals(a1, a2));
        assertNotEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
    }

	@Test
	public void testHasCustomMethod()
    {
		assertFalse(DeepEquals.hasCustomEquals(EmptyClass.class));
		assertFalse(DeepEquals.hasCustomHashCode(Class1.class));

		assertTrue(DeepEquals.hasCustomEquals(EmptyClassWithEquals.class));
		assertTrue(DeepEquals.hasCustomHashCode(EmptyClassWithEquals.class));
	}

    @Test
    public void testSymmetry()
    {
        boolean one = DeepEquals.deepEquals(new ArrayList<String>(), new EmptyClass());
        boolean two = DeepEquals.deepEquals(new EmptyClass(), new ArrayList<String>());
        assert one == two;

        one = DeepEquals.deepEquals(new HashSet<String>(), new EmptyClass());
        two = DeepEquals.deepEquals(new EmptyClass(), new HashSet<String>());
        assert one == two;

        one = DeepEquals.deepEquals(new HashMap<>(), new EmptyClass());
        two = DeepEquals.deepEquals(new EmptyClass(), new HashMap<>());
        assert one == two;

        one = DeepEquals.deepEquals(new Object[]{}, new EmptyClass());
        two = DeepEquals.deepEquals(new EmptyClass(), new Object[]{});
        assert one == two;
    }

    @Test
    public void testSortedAndUnsortedMap()
    {
        Map<String, String> map1 = new LinkedHashMap<>();
        Map<String, String> map2 = new TreeMap<>();
        map1.put("C", "charlie");
        map1.put("A", "alpha");
        map1.put("B", "beta");
        map2.put("C", "charlie");
        map2.put("B", "beta");
        map2.put("A", "alpha");
        assert DeepEquals.deepEquals(map1, map2);

        map1 = new TreeMap<>(Comparator.naturalOrder());
        map1.put("a", "b");
        map1.put("c", "d");
        map2 = new TreeMap<>(Comparator.reverseOrder());
        map2.put("a", "b");
        map2.put("c", "d");
        assert DeepEquals.deepEquals(map1, map2);
    }

    @Test
    public void testSortedAndUnsortedSet()
    {
        SortedSet<String> set1 = new TreeSet<>();
        Set<String> set2 = new HashSet<>();
        assert DeepEquals.deepEquals(set1, set2);

        set1 = new TreeSet<>();
        set1.add("a");
        set1.add("b");
        set1.add("c");
        set1.add("d");
        set1.add("e");

        set2 = new LinkedHashSet<>();
        set2.add("e");
        set2.add("d");
        set2.add("c");
        set2.add("b");
        set2.add("a");
        assert DeepEquals.deepEquals(set1, set2);
    }

    @Test
    public void testMapContentsFormatting() {
        ComplexObject expected = new ComplexObject("obj1");
        expected.addMapEntry("key1", "value1");
        expected.addMapEntry("key2", "value2");
        expected.addMapEntry("key3", "value3");
        expected.addMapEntry("key4", "value4");
        expected.addMapEntry("key5", "value5");

        ComplexObject found = new ComplexObject("obj1");
        found.addMapEntry("key1", "value1");
        found.addMapEntry("key2", "differentValue");  // This will cause difference
        found.addMapEntry("key3", "value3");
        found.addMapEntry("key4", "value4");
        found.addMapEntry("key5", "value5");

        assertFalse(DeepEquals.deepEquals(expected, found));
    }

    @Test
    public void test3DVs2DArray() {
        // Create a 3D array
        int[][][] array3D = new int[2][2][2];
        array3D[0][0][0] = 1;
        array3D[0][0][1] = 2;
        array3D[0][1][0] = 3;
        array3D[0][1][1] = 4;
        array3D[1][0][0] = 5;
        array3D[1][0][1] = 6;
        array3D[1][1][0] = 7;
        array3D[1][1][1] = 8;

        // Create a 2D array
        int[][] array2D = new int[2][2];
        array2D[0][0] = 1;
        array2D[0][1] = 2;
        array2D[1][0] = 3;
        array2D[1][1] = 4;

        // Create options map to capture the diff
        Map<String, Object> options = new HashMap<>();

        // Perform deep equals comparison
        boolean result = DeepEquals.deepEquals(array3D, array2D, options);

        // Assert the arrays are not equal
        assertFalse(result);

        // Get the diff string from options
        String diff = (String) options.get("diff");

        // Assert the diff contains dimensionality information
        assertNotNull(diff);
        assertTrue(diff.contains("dimensionality"));
    }

    @Test
    public void test3DArraysDifferentLength() {
        // Create first 3D array [2][3][2]
        long[][][] array1 = new long[2][3][2];
        array1[0][0][0] = 1L;
        array1[0][0][1] = 2L;
        array1[0][1][0] = 3L;
        array1[0][1][1] = 4L;
        array1[0][2][0] = 5L;
        array1[0][2][1] = 6L;
        array1[1][0][0] = 7L;
        array1[1][0][1] = 8L;
        array1[1][1][0] = 9L;
        array1[1][1][1] = 10L;
        array1[1][2][0] = 11L;
        array1[1][2][1] = 12L;

        // Create second 3D array [2][2][2] - different length in second dimension
        long[][][] array2 = new long[2][2][2];
        array2[0][0][0] = 1L;
        array2[0][0][1] = 2L;
        array2[0][1][0] = 3L;
        array2[0][1][1] = 4L;
        array2[1][0][0] = 7L;
        array2[1][0][1] = 8L;
        array2[1][1][0] = 9L;
        array2[1][1][1] = 10L;

        // Create options map to capture the diff
        Map<String, Object> options = new HashMap<>();

        // Perform deep equals comparison
        boolean result = DeepEquals.deepEquals(array1, array2, options);

        // Assert the arrays are not equal
        assertFalse(result);

        // Get the diff string from options
        String diff = (String) options.get("diff");

        // Assert the diff contains length information
        assertNotNull(diff);
        assertTrue(diff.contains("Expected"));
        assertTrue(diff.contains("Found"));
    }

    @Test
    public void testObjectArrayWithDifferentInnerTypes() {
        // Create first Object array containing int[]
        Object[] array1 = new Object[2];
        array1[0] = new int[] {1, 2, 3};
        array1[1] = new int[] {4, 5, 6};

        // Create second Object array containing long[]
        Object[] array2 = new Object[2];
        array2[0] = new long[] {1L, 2L, 3L};
        array2[1] = new long[] {4L, 5L, 6L};

        // Create options map to capture the diff
        Map<String, Object> options = new HashMap<>();

        // Perform deep equals comparison
        boolean result = DeepEquals.deepEquals(array1, array2, options);

        // Assert the arrays are not equal
        assertFalse(result);

        // Get the diff string from options
        String diff = (String) options.get("diff");

        // Assert the diff contains type information
        assertNotNull(diff);
        assertTrue(diff.contains("type"));
    }

    @Test
    public void testObjectFieldFormatting() {
        // Test class with all field types
        class Address {
            String street = "123 Main St";
        }

        class TestObject {
            // Array fields
            int[] emptyArray = new int[0];
            String[] multiArray = new String[] {"a", "b", "c"};
            double[] nullArray = null;

            // Collection fields
            List<String> emptyList = new ArrayList<>();
            Set<Address> multiSet = new HashSet<>(Arrays.asList(new Address(), new Address()));
            Collection<Integer> nullCollection = null;

            // Map fields
            Map<String, Integer> emptyMap = new HashMap<>();
            Map<String, String> multiMap = new HashMap<String, String>() {{
                put("a", "1");
                put("b", "2");
                put("c", "3");
            }};
            Map<String, Double> nullMap = null;

            // Object fields
            Address emptyAddress = new Address();
            Address nullAddress = null;
        }

        TestObject obj1 = new TestObject();
        TestObject obj2 = new TestObject();
        // Modify one value to force difference
        obj2.multiArray[0] = "x";

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(obj1, obj2, options);
        assertFalse(result);

        String diff = (String) options.get("diff");

        assert diff.contains("emptyArray: int[∅]");
        assert diff.contains("multiArray: String[0..2]");
        assert diff.contains("nullArray: null");
        assert diff.contains("emptyList: List(∅)");
        assert diff.contains("multiSet: Set(0..1)");
        assert diff.contains("nullCollection: null");
        assert diff.contains("emptyMap: Map(∅)");
        assert diff.contains("multiMap: Map(0..2)");
        assert diff.contains("nullMap: null");
        assert diff.contains("emptyAddress: {..}");
        assert diff.contains("nullAddress: null");
    }

    @Test
    public void testCollectionTypeFormatting() {
        class Person {
            String name;
            Person(String name) { this.name = name; }
        }

        class Container {
            List<String> strings = Arrays.asList("a", "b", "c");
            List<Integer> numbers = Arrays.asList(1, 2, 3);
            List<Person> people = Arrays.asList(new Person("John"), new Person("Jane"));
            List<Object> objects = Arrays.asList("mixed", 123, new Person("Bob"));
        }

        Container obj1 = new Container();
        Container obj2 = new Container();
        // Modify one value to force difference
        obj2.strings.set(0, "x");

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(obj1, obj2, options);
        assertFalse(result);

        String diff = (String) options.get("diff");

        assert diff.contains("strings: List(0..2)");
        assert diff.contains("numbers: List(0..2)");
        assert diff.contains("people: List(0..1)");
        assert diff.contains("objects: List(0..2)");
    }

    @Test
    public void testArrayDirectCycle() {
        Object[] array1 = new Object[1];
        array1[0] = array1;  // Direct cycle

        Object[] array2 = new Object[1];
        array2[0] = array2;  // Direct cycle

        assertTrue(DeepEquals.deepEquals(array1, array2));
    }

    @Test
    public void testCollectionDirectCycle() {
        List<Object> list1 = new ArrayList<>();
        list1.add(list1);  // Direct cycle

        List<Object> list2 = new ArrayList<>();
        list2.add(list2);  // Direct cycle

        assertTrue(DeepEquals.deepEquals(list1, list2));
    }

    @Test
    public void testMapKeyCycle() {
        Map<Object, String> map1 = new LinkedHashMap<>();
        map1.put(map1, "value");  // Cycle in key

        Map<Object, String> map2 = new LinkedHashMap<>();
        map2.put(map2, "value");  // Cycle in key

        assertTrue(DeepEquals.deepEquals(map1, map2));
        map1.put(new int[]{4, 5, 6}, "value456");
        map2.put(new int[]{4, 5, 7}, "value456");

        assertFalse(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    public void testMapDeepHashcodeCycle() {
        Map<Object, String> map1 = new HashMap<>();
        map1.put(map1, "value");  // Cycle in key

        assert DeepEquals.deepHashCode(map1) != 0;
    }

    @Test
    public void testMapValueCycle() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", map1);  // Cycle in value

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", map2);  // Cycle in value

        assertTrue(DeepEquals.deepEquals(map1, map2));
        map1.put("array", new int[]{4, 5, 6});
        map2.put("array", new int[]{4, 5, 7});

        assertFalse(DeepEquals.deepEquals(map1, map2));
        
    }

    @Test
    public void testObjectFieldCycle() {
        class CyclicObject {
            CyclicObject self;
        }

        CyclicObject obj1 = new CyclicObject();
        obj1.self = obj1;  // Direct cycle

        CyclicObject obj2 = new CyclicObject();
        obj2.self = obj2;  // Direct cycle

        assertTrue(DeepEquals.deepEquals(obj1, obj2));
    }

    @Test
    public void testArrayIndirectCycle() {
        class ArrayHolder {
            Object[] array;
        }

        Object[] array1 = new Object[1];
        ArrayHolder holder1 = new ArrayHolder();
        holder1.array = array1;
        array1[0] = holder1;  // Indirect cycle

        Object[] array2 = new Object[1];
        ArrayHolder holder2 = new ArrayHolder();
        holder2.array = array2;
        array2[0] = holder2;  // Indirect cycle

        assertTrue(DeepEquals.deepEquals(array1, array2));
    }

    @Test
    public void testCollectionIndirectCycle() {
        class CollectionHolder {
            Collection<Object> collection;
        }

        List<Object> list1 = new ArrayList<>();
        CollectionHolder holder1 = new CollectionHolder();
        holder1.collection = list1;
        list1.add(holder1);  // Indirect cycle

        List<Object> list2 = new ArrayList<>();
        CollectionHolder holder2 = new CollectionHolder();
        holder2.collection = list2;
        list2.add(holder2);  // Indirect cycle

        assertTrue(DeepEquals.deepEquals(list1, list2));
    }

    @Test
    public void testMapKeyIndirectCycle() {
        class MapHolder {
            Map<Object, String> map;
        }

        Map<Object, String> map1 = new HashMap<>();
        MapHolder holder1 = new MapHolder();
        holder1.map = map1;
        map1.put(holder1, "value");  // Indirect cycle

        Map<Object, String> map2 = new HashMap<>();
        MapHolder holder2 = new MapHolder();
        holder2.map = map2;
        map2.put(holder2, "value");  // Indirect cycle

        assertTrue(DeepEquals.deepEquals(map1, map2));

        map1.put(new int[]{4, 5, 6}, "value456");
        map2.put(new int[]{4, 5, 7}, "value456");

        assertFalse(DeepEquals.deepEquals(map1, map2));

    }

    @Test
    public void testMapValueIndirectCycle() {
        class MapHolder {
            Map<String, Object> map;
        }

        Map<String, Object> map1 = new HashMap<>();
        MapHolder holder1 = new MapHolder();
        holder1.map = map1;
        map1.put("key", holder1);  // Indirect cycle

        Map<String, Object> map2 = new HashMap<>();
        MapHolder holder2 = new MapHolder();
        holder2.map = map2;
        map2.put("key", holder2);  // Indirect cycle

        assertTrue(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    public void testObjectIndirectCycle() {
        class ObjectA {
            Object refToB;
        }

        class ObjectB {
            ObjectA refToA;
        }

        ObjectA objA1 = new ObjectA();
        ObjectB objB1 = new ObjectB();
        objA1.refToB = objB1;
        objB1.refToA = objA1;  // Indirect cycle

        ObjectA objA2 = new ObjectA();
        ObjectB objB2 = new ObjectB();
        objA2.refToB = objB2;
        objB2.refToA = objA2;  // Indirect cycle

        assertTrue(DeepEquals.deepEquals(objA1, objA2));
    }

    // Additional test to verify unequal cycles are detected
    @Test
    public void testUnequalCycles() {
        class CyclicObject {
            CyclicObject self;
            int value;

            CyclicObject(int value) {
                this.value = value;
            }
        }

        CyclicObject obj1 = new CyclicObject(1);
        obj1.self = obj1;

        CyclicObject obj2 = new CyclicObject(2);  // Different value
        obj2.self = obj2;

        assertFalse(DeepEquals.deepEquals(obj1, obj2));
    }

    @Test
    void testArrayKey() {
        Map<Object, Object> map1 = new HashMap<>();
        Map<Object, Object> map2 = new HashMap<>();

        int[] value1 = new int[] {9, 3, 7};
        int[] value2 = new int[] {9, 3, 7};
        map1.put(new int[] {1, 2, 3, 4, 5}, value1);
        map2.put(new int[] {1, 2, 3, 4, 5}, value2);

        assertFalse(map1.containsKey(new int[] {1, 2, 3, 4, 5}));   // Arrays use Object hashCode() and Object equals()
        assertTrue(DeepEquals.deepEquals(map1, map2));              // Maps are DeepEquals()
        value2[2] = 77;
        assertFalse(DeepEquals.deepEquals(map1, map2));
    }
    
    @Test
    void test2DArrayKey() {
        Map<Object, Object> map1 = new HashMap<>();
        Map<Object, Object> map2 = new HashMap<>();

        int[] value1 = new int[] {9, 3, 7};
        int[] value2 = new int[] {9, 3, 7};
        map1.put(new int[][] {new int[]{1, 2, 3}, null, new int[] {}, new int[]{1}}, value1);
        map2.put(new int[][] {new int[]{1, 2, 3}, null, new int[] {}, new int[]{1}}, value2);

        assertFalse(map1.containsKey(new int[] {1, 2, 3, 4, 5}));   // Arrays use Object.hashCode() [not good key]
        assertTrue(DeepEquals.deepEquals(map1, map2));              // Maps are DeepEquals()
        value2[1] = 33;
        assertFalse(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    void testComplex2DArrayKey() {
        ComplexObject co1 = new ComplexObject("Yum");
        co1.addMapEntry("foo", "bar");
        ComplexObject co2 = new ComplexObject("Yum");
        co2.addMapEntry("foo", "bar");
        Map<Object, Object> map1 = new HashMap<>();
        Map<Object, Object> map2 = new HashMap<>();

        int[] value1 = new int[] {9, 3, 7};
        int[] value2 = new int[] {9, 3, 7};

        map1.put(new Object[] {co1}, value1);
        map2.put(new Object[] {co2}, value2);
        
        assertFalse(map1.containsKey(new Object[] {co1}));
        assertTrue(DeepEquals.deepEquals(map1, map2));              // Maps are DeepEquals()
        value2[0] = 99;
        assertFalse(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    void test2DCollectionKey() {
        Map<Object, Object> map1 = new HashMap<>();
        Map<Object, Object> map2 = new HashMap<>();
        
        map1.put(Arrays.asList(asList(1, 2, 3), null, Collections.emptyList(), asList(9)), new int[] {9, 3, 7});
        map2.put(Arrays.asList(asList(1, 2, 3), null, Collections.emptyList(), asList(9)), new int[] {9, 3, 44});
        assert map2.containsKey((Arrays.asList(asList(1, 2, 3), null, Collections.emptyList(), asList(9))));

        assertFalse(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    void test2DCollectionArrayKey() {
        Map<Object, Object> map1 = new HashMap<>();
        Map<Object, Object> map2 = new HashMap<>();

        map1.put(Arrays.asList(new int[]{1, 2 ,3}, null, Collections.emptyList(), new int[]{9}), new int[] {9, 3, 7});
        map2.put(Arrays.asList(new int[]{1, 2, 3}, null, Collections.emptyList(), new int[]{9}), new int[] {9, 3, 44});

        assertFalse(DeepEquals.deepEquals(map1, map2));
    }

    private static class ComplexObject {
        private final String name;
        private final Map<String, String> dataMap = new LinkedHashMap<>();

        public ComplexObject(String name) {
            this.name = name;
        }

        public void addMapEntry(String key, String value) {
            dataMap.put(key, value);
        }

        @Override
        public String toString() {
            return "ComplexObject[" + name + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComplexObject that = (ComplexObject) o;
            boolean namesEqual = Objects.equals(name, that.name);
            boolean keysEquals = Objects.equals(dataMap.keySet().toString(), that.dataMap.keySet().toString());
            boolean valuesEquals = Objects.equals(dataMap.values().toString(), that.dataMap.values().toString());
            return namesEqual && keysEquals && valuesEquals;
        }

        @Override
        public int hashCode() {
            int name_hc = name.hashCode();
            int keySet_hc = dataMap.keySet().toString().hashCode();
            int values_hc = dataMap.values().toString().hashCode();
            return name_hc + keySet_hc + values_hc;
        }
    }
    
    static class DumbHash
    {
        String s;

        DumbHash(String str)
        {
            s = str;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DumbHash dumbHash = (DumbHash) o;
            return s != null ? s.equals(dumbHash.s) : dumbHash.s == null;
        }

        public int hashCode()
        {
            return 1;   // dumb, but valid
        }
    }

	static class EmptyClass
    {

	}

	static class EmptyClassWithEquals
    {
		public boolean equals(Object obj) {
			return obj instanceof EmptyClassWithEquals;
		}

		public int hashCode() {
			return 0;
		}
	}

	static class Class1
    {
		private boolean b;
		private double d;
		int i;

		public Class1() { }

		public Class1(boolean b, double d, int i)
        {
			super();
			this.b = b;
			this.d = d;
			this.i = i;
		}

	}

	static class Class2
    {
		private Float f;
		String s;
		short ss;
		Class1 c;

		public Class2(float f, String s, short ss, Class1 c)
        {
			super();
			this.f = f;
			this.s = s;
			this.ss = ss;
			this.c = c;
		}

		public Class2() { }
	}

	private static class Person
    {
        private String name;
        private int age;

        Person(String name, int age)
        {
            this.name = name;
            this.age = age;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Person))
            {
                return false;
            }

            Person other = (Person) obj;
            return name.equals(other.name);
        }

        public int hashCode()
        {
            return name == null ? 0 : name.hashCode();
        }
    }

    private static class AtomicWrapper
    {
        private AtomicLong n;

        AtomicWrapper(long n)
        {
            this.n = new AtomicLong(n);
        }

        long getValue()
        {
            return n.longValue();
        }
    }

    private void fillMap(Map<String, Object> map)
    {
        map.put("zulu", 26);
        map.put("alpha", 1);
        map.put("bravo", 2);
        map.put("charlie", 3);
        map.put("delta", 4);
        map.put("echo", 5);
        map.put("foxtrot", 6);
        map.put("golf", 7);
        map.put("hotel", 8);
        map.put("india", 9);
        map.put("juliet", 10);
        map.put("kilo", 11);
        map.put("lima", 12);
        map.put("mike", 13);
        map.put("november", 14);
        map.put("oscar", 15);
        map.put("papa", 16);
        map.put("quebec", 17);
        map.put("romeo", 18);
        map.put("sierra", 19);
        map.put("tango", 20);
        map.put("uniform", 21);
        map.put("victor", 22);
        map.put("whiskey", 23);
        map.put("xray", 24);
        map.put("yankee", 25);
    }

    private void fillCollection(Collection<String> col)
    {
        col.add("zulu");
        col.add("alpha");
        col.add("bravo");
        col.add("charlie");
        col.add("delta");
        col.add("echo");
        col.add("foxtrot");
        col.add("golf");
        col.add("hotel");
        col.add("india");
        col.add("juliet");
        col.add("kilo");
        col.add("lima");
        col.add("mike");
        col.add("november");
        col.add("oscar");
        col.add("papa");
        col.add("quebec");
        col.add("romeo");
        col.add("sierra");
        col.add("tango");
        col.add("uniform");
        col.add("victor");
        col.add("whiskey");
        col.add("xray");
        col.add("yankee");
    }
}
