package com.cedarsoftware.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.atan;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.E;
import static java.lang.Math.PI;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author sapradhan8 <br/>
 * <br/>
 *         Licensed under the Apache License, Version 2.0 (the "License"); you
 *         may not use this file except in compliance with the License. You may
 *         obtain a copy of the License at <br/>
 * <br/>
 *         http://www.apache.org/licenses/LICENSE-2.0 <br/>
 * <br/>
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
	public void testOrderedCollection()
    {
		List<String> a = Lists.newArrayList("one", "two", "three", "four", "five");
		// TODO different impl of list be equivalent
		// List<String> b = Lists.newLinkedList(a);
		List<String> b = Lists.newArrayList("one", "two", "three", "four", "five");

		assertTrue(DeepEquals.deepEquals(a, b));

		List<Integer> c = Lists.newArrayList(1, 2, 3, 4, 5);

		assertFalse(DeepEquals.deepEquals(a, c));

		List<Integer> d = Lists.newArrayList(4, 6);

		assertFalse(DeepEquals.deepEquals(c, d));

		List<Class1> x1 = Lists.newArrayList(new Class1(true, log(pow(E, 2)), 6), new Class1(true, tan(PI / 4), 1));
		List<Class1> x2 = Lists.newArrayList(new Class1(true, 2, 6), new Class1(true, 1, 1));
		assertTrue(DeepEquals.deepEquals(x1, x2));

	}

	@Test
	public void testUnorderedCollection()
    {
		Set<String> a = Sets.newHashSet("one", "two", "three", "four", "five");
		Set<String> b = Sets.newHashSet("three", "five", "one", "four", "two");
		assertTrue(DeepEquals.deepEquals(a, b));

		Set<Integer> c = Sets.newHashSet(1, 2, 3, 4, 5);
		assertFalse(DeepEquals.deepEquals(a, c));

		Set<Integer> d = Sets.newHashSet(4, 2, 6);
		assertFalse(DeepEquals.deepEquals(c, d));

		Set<Class1> x1 = Sets.newHashSet(new Class1(true, log(pow(E, 2)), 6), new Class1(true, tan(PI / 4), 1));
		Set<Class1> x2 = Sets.newHashSet(new Class1(true, 1, 1), new Class1(true, 2, 6));
		assertTrue(DeepEquals.deepEquals(x1, x2));
	}

    @Test
    public void testUnorderedMap()
    {
        Map<String, Integer> a = ImmutableMap.<String, Integer>of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
        // TODO different impl of maps be equivalent
        // Map<String, Integer> b = Maps.newHashMap(ImmutableMap
        // .<String, Integer> of("one", 1, "four", 4, "five", 5, "three",
        // 3, "two", 2));
        Map<String, Integer> b = ImmutableMap.<String, Integer>of("one", 1, "four", 4, "five", 5, "three", 3, "two", 2);

        assertTrue(DeepEquals.deepEquals(a, b));
    }

	@Test
	public void testHasCustomxxx()
    {
		assertFalse(DeepEquals.hasCustomEquals(EmptyClass.class));
		assertFalse(DeepEquals.hasCustomHashCode(Class1.class));

		assertTrue(DeepEquals.hasCustomEquals(EmptyClassWithEquals.class));
		assertTrue(DeepEquals.hasCustomHashCode(EmptyClassWithEquals.class));
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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
}
