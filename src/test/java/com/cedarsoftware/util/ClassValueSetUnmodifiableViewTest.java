package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassValueSetUnmodifiableViewTest {

    @Test
    public void testContainsAllAndIsEmpty() {
        ClassValueSet set = new ClassValueSet();
        Set<Class<?>> view = set.unmodifiableView();
        assertTrue(view.isEmpty());

        set.add(String.class);
        set.add(Integer.class);
        assertFalse(view.isEmpty());
        assertTrue(view.containsAll(Arrays.asList(String.class, Integer.class)));
        assertFalse(view.containsAll(Collections.singleton(Double.class)));
    }

    @Test
    public void testToArrayMethods() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(Integer.class);
        set.add(null);

        Set<Class<?>> view = set.unmodifiableView();

        Object[] objArray = view.toArray();
        assertEquals(3, objArray.length);
        assertTrue(new HashSet<>(Arrays.asList(objArray)).containsAll(Arrays.asList(String.class, Integer.class, null)));

        Class<?>[] typedArray = view.toArray(new Class<?>[0]);
        assertEquals(3, typedArray.length);
        assertTrue(new HashSet<>(Arrays.asList(typedArray)).containsAll(Arrays.asList(String.class, Integer.class, null)));
    }

    @Test
    public void testToStringHashCodeAndEquals() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(Integer.class);

        Set<Class<?>> view = set.unmodifiableView();

        assertEquals(set.toString(), view.toString());
        assertEquals(set.hashCode(), view.hashCode());
        assertEquals(set, view);
        assertEquals(view, set);

        ClassValueSet other = new ClassValueSet();
        other.add(String.class);
        other.add(Integer.class);
        Set<Class<?>> otherView = other.unmodifiableView();
        assertEquals(view, otherView);
        assertEquals(view.hashCode(), otherView.hashCode());
    }
}
