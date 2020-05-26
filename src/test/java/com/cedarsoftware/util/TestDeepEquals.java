package com.cedarsoftware.util;

import org.agrona.collections.Object2ObjectHashMap;
import org.junit.Test;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static com.cedarsoftware.util.DeepEquals.deepHashCode;
import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * @author John DeRegnaucourt
 * @author sapradhan8
 * <br>
 *         Licensed under the Apache License, Version 2.0 (the "License"); you
 *         may not use this file except in compliance with the License. You may
 *         obtain a copy of the License at <br>
 * <br>
 *         http://www.apache.org/licenses/LICENSE-2.0 <br>
 * <br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *         implied. See the License for the specific language governing
 *         permissions and limitations under the License.
 */
public class TestDeepEquals
{
    @Test
    public void testSameObjectEquals()
    {
        Date date1 = new Date();
        Date date2 = date1;
        assertTrue(deepEquals(date1, date2));
    }

	@Test
	public void testEqualsWithNull()
    {
		Date date1 = new Date();
		assertFalse(deepEquals(null, date1));
		assertFalse(deepEquals(date1, null));
	}

	@Test
    public void testDeepEqualsWithOptions()
    {
        Person p1 = new Person("Jim Bob", 27);
        Person p2 = new Person("Jim Bob", 34);
        assert p1.equals(p2);
        assert deepEquals(p1, p2);

        Map<String, Object> options = new HashMap<>();
        Set<Class> skip = new HashSet<>();
        skip.add(Person.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert !deepEquals(p1, p2, options);       // told to skip Person's .equals() - so it will compare all fields

        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, new HashSet());
        assert !deepEquals(p1, p2, options);       // told to skip all custom .equals() - so it will compare all fields

        skip.clear();
        skip.add(Point.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert deepEquals(p1, p2, options);        // Not told to skip Person's .equals() - so it will compare on name only
    }

    @Test
    public void testBigDecimal()
    {
        BigDecimal ten = new BigDecimal("10.0");
        assert deepEquals(ten, 10.0f);
        assert deepEquals(ten, 10.0d);
        assert deepEquals(ten, 10);
        assert deepEquals(ten, 10l);
        assert deepEquals(ten, new BigInteger("10"));
        assert deepEquals(ten, new AtomicLong(10L));
        assert deepEquals(ten, new AtomicInteger(10));

        assert !deepEquals(ten, 10.01f);
        assert !deepEquals(ten, 10.01d);
        assert !deepEquals(ten, 11);
        assert !deepEquals(ten, 11l);
        assert !deepEquals(ten, new BigInteger("11"));
        assert !deepEquals(ten, new AtomicLong(11L));
        assert !deepEquals(ten, new AtomicInteger(11));

        BigDecimal x = new BigDecimal(new BigInteger("1"), -1);
        assert deepEquals(ten, x);
        x = new BigDecimal(new BigInteger("1"), -2);
        assert !deepEquals(ten, x);

        assert !deepEquals(ten, TimeZone.getDefault());
        assert !deepEquals(ten, "10");

        assert deepEquals(0.1d, new BigDecimal("0.1"));
        assert deepEquals(0.04d, new BigDecimal("0.04"));
        assert deepEquals(0.1f, new BigDecimal("0.1"));
        assert deepEquals(0.04f, new BigDecimal("0.04"));
    }

    @Test
    public void testBigInteger()
    {
        BigInteger ten = new BigInteger("10");
        assert deepEquals(ten, new BigInteger("10"));
        assert !deepEquals(ten, new BigInteger("11"));
        assert deepEquals(ten, 10.0f);
        assert !deepEquals(ten, 11.0f);
        assert deepEquals(ten, 10.0d);
        assert !deepEquals(ten, 11.0d);
        assert deepEquals(ten, 10);
        assert deepEquals(ten, 10l);
        assert deepEquals(ten, new BigDecimal("10.0"));
        assert deepEquals(ten, new AtomicLong(10L));
        assert deepEquals(ten, new AtomicInteger(10));

        assert !deepEquals(ten, 10.01f);
        assert !deepEquals(ten, 10.01d);
        assert !deepEquals(ten, 11);
        assert !deepEquals(ten, 11l);
        assert !deepEquals(ten, new BigDecimal("10.001"));
        assert !deepEquals(ten, new BigDecimal("11"));
        assert !deepEquals(ten, new AtomicLong(11L));
        assert !deepEquals(ten, new AtomicInteger(11));

        assert !deepEquals(ten, TimeZone.getDefault());
        assert !deepEquals(ten, "10");

        assert !deepEquals(new BigInteger("1"), new BigDecimal("0.99999999999999999999999999999"));
    }

    @Test
    public void testDifferentNumericTypes()
    {
        assert deepEquals(1.0f, 1L);
        assert deepEquals(1.0d, 1L);
        assert deepEquals(1L, 1.0f);
        assert deepEquals(1L, 1.0d);
        assert !deepEquals(1, TimeZone.getDefault());

        long x = Integer.MAX_VALUE;
        assert deepEquals(Integer.MAX_VALUE, x);
        assert deepEquals(x, Integer.MAX_VALUE);
        assert !deepEquals(Integer.MAX_VALUE, x + 1);
        assert !deepEquals(x + 1, Integer.MAX_VALUE);

        x = Integer.MIN_VALUE;
        assert deepEquals(Integer.MIN_VALUE, x);
        assert deepEquals(x, Integer.MIN_VALUE);
        assert !deepEquals(Integer.MIN_VALUE, x - 1);
        assert !deepEquals(x - 1, Integer.MIN_VALUE);

        BigDecimal y = new BigDecimal("1.7976931348623157e+308");
        assert deepEquals(Double.MAX_VALUE, y);
        assert deepEquals(y, Double.MAX_VALUE);
        y = y.add(BigDecimal.ONE);
        assert !deepEquals(Double.MAX_VALUE, y);
        assert !deepEquals(y, Double.MAX_VALUE);

        y = new BigDecimal("4.9e-324");
        assert deepEquals(Double.MIN_VALUE, y);
        assert deepEquals(y, Double.MIN_VALUE);
        y = y.subtract(BigDecimal.ONE);
        assert !deepEquals(Double.MIN_VALUE, y);
        assert !deepEquals(y, Double.MIN_VALUE);

        x = Byte.MAX_VALUE;
        assert deepEquals((byte)127, x);
        assert deepEquals(x, (byte)127);
        x++;
        assert !deepEquals((byte)127, x);
        assert !deepEquals(x, (byte)127);

        x = Byte.MIN_VALUE;
        assert deepEquals((byte)-128, x);
        assert deepEquals(x, (byte)-128);
        x--;
        assert !deepEquals((byte)-128, x);
        assert !deepEquals(x, (byte)-128);
    }

    @Test
    public void testAtomicStuff()
    {
        AtomicWrapper atomic1 = new AtomicWrapper(35);
        AtomicWrapper atomic2 = new AtomicWrapper(35);
        AtomicWrapper atomic3 = new AtomicWrapper(42);

        assert deepEquals(atomic1, atomic2);
        assert !deepEquals(atomic1, atomic3);

        Map<String, Object> options = new HashMap<>();
        Set<Class> skip = new HashSet<>();
        skip.add(AtomicWrapper.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, skip);
        assert deepEquals(atomic1, atomic2, options);
        assert !deepEquals(atomic1, atomic3, options);

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(true);

        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, new HashSet());
        assert !deepEquals(b1, b2);
        assert deepEquals(b1, b3);
        assert !deepEquals(b1, b2, options);
        assert deepEquals(b1, b3, options);
    }

	@Test
	public void testDifferentClasses()
    {
		assertFalse(deepEquals(new Date(), "test"));
	}

    @Test
    public void testPOJOequals()
    {
        Class1 x = new Class1(true, tan(PI / 4), 1);
        Class1 y = new Class1(true, 1.0, 1);
        assertTrue(deepEquals(x, y));
        assertFalse(deepEquals(x, new Class1()));

        Class2 a = new Class2((float) atan(1.0), "hello", (short) 2,
                new Class1(false, sin(0.75), 5));
        Class2 b = new Class2((float) PI / 4, "hello", (short) 2,
                new Class1(false, 2 * cos(0.75 / 2) * sin(0.75 / 2), 5)
        );

        assertTrue(deepEquals(a, b));
        assertFalse(deepEquals(a, new Class2()));
    }

	@Test
	public void testPrimitiveArrays()
    {
		int array1[] = { 2, 4, 5, 6, 3, 1, 3, 3, 5, 22 };
		int array2[] = { 2, 4, 5, 6, 3, 1, 3, 3, 5, 22 };

		assertTrue(deepEquals(array1, array2));

		int array3[] = { 3, 4, 7 };

		assertFalse(deepEquals(array1, array3));

		float array4[] = { 3.4f, 5.5f };
		assertFalse(deepEquals(array1, array4));
	}

	@Test
	public void testOrderedCollection()
    {
        List<String> a = asList("one", "two", "three", "four", "five");
		List<String> b = new LinkedList<>(a);

		assertTrue(deepEquals(a, b));

		List<Integer> c = asList(1, 2, 3, 4, 5);

		assertFalse(deepEquals(a, c));

		List<Integer> d = asList(4, 6);

		assertFalse(deepEquals(c, d));

		List<Class1> x1 = asList(new Class1(true, log(pow(E, 2)), 6), new Class1(true, tan(PI / 4), 1));
		List<Class1> x2 = asList(new Class1(true, 2, 6), new Class1(true, 1, 1));
		assertTrue(deepEquals(x1, x2));
	}

	@Test
	public void testUnorderedCollection()
    {
        Set<String> a = new HashSet<>(asList("one", "two", "three", "four", "five"));
		Set<String> b = new HashSet<>(asList("three", "five", "one", "four", "two"));
		assertTrue(deepEquals(a, b));

		Set<Integer> c = new HashSet<>(asList(1, 2, 3, 4, 5));
		assertFalse(deepEquals(a, c));

		Set<Integer> d = new HashSet<>(asList(4, 2, 6));
		assertFalse(deepEquals(c, d));

		Set<Class1> x1 = new HashSet<>(asList(new Class1(true, log(pow(E, 2)), 6), new Class1(true, tan(PI / 4), 1)));
		Set<Class1> x2 = new HashSet<>(asList(new Class1(true, 1, 1), new Class1(true, 2, 6)));
		assertTrue(deepEquals(x1, x2));

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
		assert deepEquals(d1, d2);

        d2.clear();
        d2.add(new DumbHash("bravo"));
        d2.add(new DumbHash("alpha"));
        d2.add(new DumbHash("delta"));
        assert !deepEquals(d2, d1);
    }

    @Test
    public void testEquivalentMaps()
    {
        Map map1 = new LinkedHashMap();
        fillMap(map1);
        Map map2 = new HashMap();
        fillMap(map2);
        assertTrue(deepEquals(map1, map2));
        assertEquals(deepHashCode(map1), deepHashCode(map2));

        map1 = new TreeMap();
        fillMap(map1);
        map2 = new TreeMap();
        map2 = Collections.synchronizedSortedMap((SortedMap) map2);
        fillMap(map2);
        assertTrue(deepEquals(map1, map2));
        assertEquals(deepHashCode(map1), deepHashCode(map2));

        // Uses flyweight entries
        map1 = new Object2ObjectHashMap();
        fillMap(map1);
        map2 = new Object2ObjectHashMap();
        fillMap(map2);
        assertTrue(deepEquals(map1, map2));
        assertEquals(deepHashCode(map1), deepHashCode(map2));
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

        assert deepEquals(map1, map2);

        map2.clear();
        map2.put(new DumbHash("bravo"), "bravo");
        map2.put(new DumbHash("alpha"), "alpha");
        map2.put(new DumbHash("delta"), "delta");
        assert !deepEquals(map1, map2);
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

        assert deepEquals(map1, map2);

        map2.clear();
        map2.put("bravo", new DumbHash("bravo"));
        map2.put("alpha", new DumbHash("alpha"));
        map2.put("delta", new DumbHash("delta"));
        assert !deepEquals(map1, map2);
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

        assert deepEquals(map1, map2);

        map2.clear();
        map2.put(new DumbHash("bravo"), new DumbHash("bravo"));
        map2.put(new DumbHash("alpha"), new DumbHash("alpha"));
        map2.put(new DumbHash("delta"), new DumbHash("delta"));
        assert !deepEquals(map1, map2);
    }

    @Test
    public void testInequivalentMaps()
    {
        Map map1 = new TreeMap();
        fillMap(map1);
        Map map2 = new HashMap();
        fillMap(map2);
        // Sorted versus non-sorted Map
        assertFalse(deepEquals(map1, map2));

        // Hashcodes are equals because the Maps have same elements
        assertEquals(deepHashCode(map1), deepHashCode(map2));

        map2 = new TreeMap();
        fillMap(map2);
        map2.remove("kilo");
        assertFalse(deepEquals(map1, map2));

        // Hashcodes are different because contents of maps are different
        assertNotEquals(deepHashCode(map1), deepHashCode(map2));

        // Inequality because ConcurrentSkipListMap is a SortedMap
        map1 = new HashMap();
        fillMap(map1);
        map2 = new ConcurrentSkipListMap();
        fillMap(map2);
        assertFalse(deepEquals(map1, map2));

        map1 = new TreeMap();
        fillMap(map1);
        map2 = new ConcurrentSkipListMap();
        fillMap(map2);
        assertTrue(deepEquals(map1, map2));
        map2.remove("papa");
        assertFalse(deepEquals(map1, map2));

        map1 = new HashMap();
        map1.put("foo", "bar");
        map1.put("baz", "qux");
        map2 = new HashMap();
        map2.put("foo", "bar");
        assert !deepEquals(map1, map2);
    }

    @Test
    public void testNumbersAndStrings()
    {
        Map<String, Boolean> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assert !deepEquals("10", 10);
        assert deepEquals("10", 10, options);
        assert deepEquals(10, "10", options);
        assert deepEquals(10, "10.0", options);
        assert deepEquals(10.0f, "10.0", options);
        assert deepEquals(10.0f, "10", options);
        assert deepEquals(10.0d, "10.0", options);
        assert deepEquals(10.0d, "10", options);
        assert !deepEquals(10.0d, "10.01", options);
        assert !deepEquals(10.0d, "10.0d", options);
        assert deepEquals(new BigDecimal("3.14159"), 3.14159d, options);
        assert !deepEquals(new BigDecimal("3.14159"), "3.14159");
        assert deepEquals(new BigDecimal("3.14159"), "3.14159", options);
    }

    @Test
    public void testEquivalentCollections()
    {
        // ordered Collection
        Collection col1 = new ArrayList();
        fillCollection(col1);
        Collection col2 = new LinkedList();
        fillCollection(col2);
        assertTrue(deepEquals(col1, col2));
        assertEquals(deepHashCode(col1), deepHashCode(col2));

        // unordered Collections (Set)
        col1 = new LinkedHashSet();
        fillCollection(col1);
        col2 = new HashSet();
        fillCollection(col2);
        assertTrue(deepEquals(col1, col2));
        assertEquals(deepHashCode(col1), deepHashCode(col2));

        col1 = new TreeSet();
        fillCollection(col1);
        col2 = new TreeSet();
        Collections.synchronizedSortedSet((SortedSet) col2);
        fillCollection(col2);
        assertTrue(deepEquals(col1, col2));
        assertEquals(deepHashCode(col1), deepHashCode(col2));
    }

    @Test
    public void testInequivalentCollections()
    {
        Collection col1 = new TreeSet();
        fillCollection(col1);
        Collection col2 = new HashSet();
        fillCollection(col2);
        assertFalse(deepEquals(col1, col2));
        assertEquals(deepHashCode(col1), deepHashCode(col2));

        col2 = new TreeSet();
        fillCollection(col2);
        col2.remove("lima");
        assertFalse(deepEquals(col1, col2));
        assertNotEquals(deepHashCode(col1), deepHashCode(col2));

        assertFalse(deepEquals(new HashMap(), new ArrayList()));
        assertFalse(deepEquals(new ArrayList(), new HashMap()));
    }

    @Test
    public void testArray()
    {
        Object[] a1 = new Object[] {"alpha", "bravo", "charlie", "delta"};
        Object[] a2 = new Object[] {"alpha", "bravo", "charlie", "delta"};

        assertTrue(deepEquals(a1, a2));
        assertEquals(deepHashCode(a1), deepHashCode(a2));
        a2[3] = "echo";
        assertFalse(deepEquals(a1, a2));
        assertNotEquals(deepHashCode(a1), deepHashCode(a2));
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
        boolean one = deepEquals(new ArrayList<String>(), new EmptyClass());
        boolean two = deepEquals(new EmptyClass(), new ArrayList<String>());
        assert one == two;
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

    private void fillMap(Map map)
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

    private void fillCollection(Collection col)
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
