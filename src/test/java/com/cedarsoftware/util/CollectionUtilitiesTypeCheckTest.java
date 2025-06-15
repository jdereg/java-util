package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilitiesTypeCheckTest {
    @Test
    void isUnmodifiableReturnsTrueForUnmodifiableClass() {
        Class<?> wrapperClass = Collections.unmodifiableList(new ArrayList<>()).getClass();
        assertTrue(CollectionUtilities.isUnmodifiable(wrapperClass));
    }

    @Test
    void isUnmodifiableReturnsFalseForModifiableClass() {
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test
    void isUnmodifiableNullThrowsNpe() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> CollectionUtilities.isUnmodifiable(null));
        assertEquals("targetType (Class) cannot be null", e.getMessage());
    }

    @Test
    void isSynchronizedReturnsTrueForSynchronizedClass() {
        Class<?> wrapperClass = Collections.synchronizedList(new ArrayList<>()).getClass();
        assertTrue(CollectionUtilities.isSynchronized(wrapperClass));
    }

    @Test
    void isSynchronizedReturnsFalseForUnsynchronizedClass() {
        assertFalse(CollectionUtilities.isSynchronized(ArrayList.class));
    }

    @Test
    void isSynchronizedNullThrowsNpe() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> CollectionUtilities.isSynchronized(null));
        assertEquals("targetType (Class) cannot be null", e.getMessage());
    }
}
