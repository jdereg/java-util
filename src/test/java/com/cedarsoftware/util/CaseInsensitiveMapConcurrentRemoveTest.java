package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entry and key views removed in steps: entrySet().remove(entry) checked the value, then removed the key, and
 * keySet().remove(key) compared the map's size before and after. On a concurrent backing map another thread's write
 * lands between the steps. The backing map here makes that write at the point where it happened, deterministically.
 */
class CaseInsensitiveMapConcurrentRemoveTest {

    /** A ConcurrentHashMap that runs another thread's write after the n-th get(), or inside the next remove(). */
    static final class Interleaving extends ConcurrentHashMap<Object, Object> {
        int getsBeforeWrite = -1;
        boolean writeOnRemove;
        Runnable write;
        boolean wrote;

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            if (getsBeforeWrite > 0 && --getsBeforeWrite == 0) {
                runWrite();
            }
            return value;
        }

        @Override
        public Object remove(Object key) {
            Object previous = super.remove(key);
            if (writeOnRemove) {
                writeOnRemove = false;
                runWrite();
            }
            return previous;
        }

        private void runWrite() {
            wrote = true;
            write.run();
        }
    }

    @Test
    void removingAnEntryNeverRemovesANewerValue() {
        Interleaving backing = new Interleaving();
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        map.put("Key", 1);
        backing.write = () -> map.put("KEY", 2);        // another thread replaces the value...
        backing.getsBeforeWrite = 2;                     // ...after the value was read and checked
        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("key", 1));
        if (backing.wrote) {
            // The newer value was put after the one removed was checked: it must survive, and the entry (key, 1)
            // was not the one there any more
            assertEquals(2, map.get("key"), "the newer value was removed");
            assertFalse(removed);
        } else {
            assertTrue(removed);
            assertNull(map.get("key"));
        }
    }

    @Test
    void removingAKeyReportsItWhileAnotherKeyIsAdded() {
        Interleaving backing = new Interleaving();
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        map.put("Key", 1);
        backing.write = () -> map.put("Other", 2);      // another thread adds a key while this one removes
        backing.writeOnRemove = true;
        assertTrue(map.keySet().remove("KEY"), "the key was removed");
        assertFalse(map.containsKey("key"));
        assertTrue(map.containsKey("other"));
    }

    @Test
    void theViewsRemoveAsBefore() {
        for (Map<Object, Object> map : new Map[]{new CaseInsensitiveMap<>(), CaseInsensitiveMap.concurrent(),
                CaseInsensitiveMap.concurrentSorted()}) {
            boolean sorted = ((CaseInsensitiveMap<?, ?>) map).getWrappedMap() instanceof java.util.SortedMap;
            map.put("Alpha", 1);
            map.put("beta", null);
            map.put(sorted ? "seven" : 7, "seven");       // a sorted map cannot hold an Integer beside Strings
            assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("ALPHA", 2)), "another value");
            assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("gamma", null)), "absent, null value");
            assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("BETA", null)), "present, null value");
            assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("alpha", 1)));
            assertFalse(map.keySet().remove("alpha"));
            assertTrue(map.keySet().remove(sorted ? "SEVEN" : 7));
            assertTrue(map.isEmpty(), map.getClass().getSimpleName());
            map.put(null, "none");
            assertTrue(map.keySet().remove(null));
            assertTrue(map.isEmpty());
        }
    }
}
