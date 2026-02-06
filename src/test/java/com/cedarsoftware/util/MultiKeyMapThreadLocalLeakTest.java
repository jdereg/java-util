package com.cedarsoftware.util;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for bug #6: ThreadLocal lookup arrays leak references.
 *
 * Bug: The getMultiKey(k1, k2, ...) and containsMultiKey(k1, k2, ...) methods
 * fill ThreadLocal Object[] arrays with key references but never null them out
 * after the operation completes. In thread-pool environments, this pins
 * references to arbitrary user objects for the lifetime of the thread,
 * preventing garbage collection.
 *
 * Fix: Null out array elements in a finally block after get()/containsKey().
 */
class MultiKeyMapThreadLocalLeakTest {

    @SuppressWarnings("unchecked")
    private Object[] getThreadLocalArray(String fieldName) throws Exception {
        Field field = MultiKeyMap.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ThreadLocal<Object[]> tl = (ThreadLocal<Object[]>) field.get(null);
        return tl.get();
    }

    // --- getMultiKey tests ---

    @Test
    void testGetMultiKey2ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b"}, "value");

        map.getMultiKey("a", "b");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_2");
        assertNull(key[0], "LOOKUP_KEY_2[0] should be cleared after getMultiKey");
        assertNull(key[1], "LOOKUP_KEY_2[1] should be cleared after getMultiKey");
    }

    @Test
    void testGetMultiKey3ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c"}, "value");

        map.getMultiKey("a", "b", "c");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_3");
        assertNull(key[0], "LOOKUP_KEY_3[0] should be cleared after getMultiKey");
        assertNull(key[1], "LOOKUP_KEY_3[1] should be cleared after getMultiKey");
        assertNull(key[2], "LOOKUP_KEY_3[2] should be cleared after getMultiKey");
    }

    @Test
    void testGetMultiKey4ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c", "d"}, "value");

        map.getMultiKey("a", "b", "c", "d");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_4");
        for (int i = 0; i < 4; i++) {
            assertNull(key[i], "LOOKUP_KEY_4[" + i + "] should be cleared after getMultiKey");
        }
    }

    @Test
    void testGetMultiKey5ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c", "d", "e"}, "value");

        map.getMultiKey("a", "b", "c", "d", "e");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_5");
        for (int i = 0; i < 5; i++) {
            assertNull(key[i], "LOOKUP_KEY_5[" + i + "] should be cleared after getMultiKey");
        }
    }

    // --- containsMultiKey tests ---

    @Test
    void testContainsMultiKey2ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b"}, "value");

        map.containsMultiKey("a", "b");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_2");
        assertNull(key[0], "LOOKUP_KEY_2[0] should be cleared after containsMultiKey");
        assertNull(key[1], "LOOKUP_KEY_2[1] should be cleared after containsMultiKey");
    }

    @Test
    void testContainsMultiKey3ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c"}, "value");

        map.containsMultiKey("a", "b", "c");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_3");
        assertNull(key[0], "LOOKUP_KEY_3[0] should be cleared after containsMultiKey");
        assertNull(key[1], "LOOKUP_KEY_3[1] should be cleared after containsMultiKey");
        assertNull(key[2], "LOOKUP_KEY_3[2] should be cleared after containsMultiKey");
    }

    @Test
    void testContainsMultiKey4ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c", "d"}, "value");

        map.containsMultiKey("a", "b", "c", "d");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_4");
        for (int i = 0; i < 4; i++) {
            assertNull(key[i], "LOOKUP_KEY_4[" + i + "] should be cleared after containsMultiKey");
        }
    }

    @Test
    void testContainsMultiKey5ClearsThreadLocal() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b", "c", "d", "e"}, "value");

        map.containsMultiKey("a", "b", "c", "d", "e");

        Object[] key = getThreadLocalArray("LOOKUP_KEY_5");
        for (int i = 0; i < 5; i++) {
            assertNull(key[i], "LOOKUP_KEY_5[" + i + "] should be cleared after containsMultiKey");
        }
    }
}
