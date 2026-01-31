package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class WildcardTypeImplTest {

    @Test
    void testGetUpperBoundsReturnsCopy() throws Exception {
        Class<?> cls = Class.forName("com.cedarsoftware.util.TypeUtilities$WildcardTypeImpl");
        Constructor<?> ctor = cls.getDeclaredConstructor(Type[].class, Type[].class);
        ctor.setAccessible(true);
        Type[] upper = new Type[]{Number.class};
        Object instance = ctor.newInstance(upper, new Type[0]);

        Method getUpperBounds = cls.getMethod("getUpperBounds");
        Type[] first = (Type[]) getUpperBounds.invoke(instance);
        assertArrayEquals(upper, first);

        first[0] = String.class;
        Type[] second = (Type[]) getUpperBounds.invoke(instance);
        assertArrayEquals(new Type[]{Number.class}, second);
    }

    @Test
    void testEqualsAndHashCode() throws Exception {
        Class<?> cls = Class.forName("com.cedarsoftware.util.TypeUtilities$WildcardTypeImpl");
        Constructor<?> ctor = cls.getDeclaredConstructor(Type[].class, Type[].class);
        ctor.setAccessible(true);

        Object a = ctor.newInstance(new Type[]{Number.class}, new Type[]{String.class});
        Object b = ctor.newInstance(new Type[]{Number.class}, new Type[]{String.class});
        Object c = ctor.newInstance(new Type[]{Number.class}, new Type[]{Integer.class});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
        assertNotEquals(a, "other");
    }
}
