package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentNavigableMapNullSafeComparatorUtilTest {

    @SuppressWarnings("unchecked")
    private static Comparator<Object> getWrapped(Comparator<?> cmp) throws Exception {
        Method m = ConcurrentNavigableMapNullSafe.class.getDeclaredMethod("wrapComparator", Comparator.class);
        m.setAccessible(true);
        return (Comparator<Object>) m.invoke(null, cmp);
    }

    @Test
    void testActualNullHandling() throws Exception {
        Comparator<Object> comp = getWrapped(null);
        assertEquals(0, comp.compare(null, null));
        assertEquals(-1, comp.compare(null, "a"));
        assertEquals(1, comp.compare("a", null));
    }

    @Test
    void testComparableObjects() throws Exception {
        Comparator<Object> comp = getWrapped(null);
        assertTrue(comp.compare("a", "b") < 0);
        assertTrue(comp.compare("b", "a") > 0);
        assertEquals(0, comp.compare("x", "x"));
    }

    @Test
    void testDifferentNonComparableTypes() throws Exception {
        Comparator<Object> comp = getWrapped(null);
        Object one = new Object();
        Long two = 5L;
        int expected = one.getClass().getName().compareTo(two.getClass().getName());
        assertEquals(expected, comp.compare(one, two));
        assertEquals(-expected, comp.compare(two, one));
    }

    @Test
    void testSameClassNameDifferentClassLoaders() throws Exception {
        ClassLoader cl1 = new LoaderOne();
        ClassLoader cl2 = new LoaderTwo();
        Class<?> c1 = Class.forName("com.cedarsoftware.util.TestClass", true, cl1);
        Class<?> c2 = Class.forName("com.cedarsoftware.util.TestClass", true, cl2);
        Object o1 = c1.getDeclaredConstructor().newInstance();
        Object o2 = c2.getDeclaredConstructor().newInstance();

        Comparator<Object> comp = getWrapped(null);
        int expected = cl1.getClass().getName().compareTo(cl2.getClass().getName());
        assertEquals(expected, comp.compare(o1, o2));
        assertEquals(-expected, comp.compare(o2, o1));
    }

    private static URL[] getUrls() throws Exception {
        URL url = ConcurrentNavigableMapNullSafeComparatorUtilTest.class.getClassLoader().getResource("test.txt");
        String path = url.getPath();
        path = path.substring(0, path.length() - 8);
        List<URL> urls = new ArrayList<>();
        urls.add(new URL("file:" + path));
        return urls.toArray(new URL[1]);
    }

    static class LoaderOne extends URLClassLoader {
        LoaderOne() throws Exception { super(getUrls(), null); }
    }

    static class LoaderTwo extends URLClassLoader {
        LoaderTwo() throws Exception { super(getUrls(), null); }
    }
}
