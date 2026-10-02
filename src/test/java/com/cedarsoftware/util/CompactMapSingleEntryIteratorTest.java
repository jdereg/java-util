package com.cedarsoftware.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At size one a CompactMap holds the value itself (for the single-value key) or an entry, and replacing the value
 * replaced that object. Its iterators detect a concurrent change by the stored object's identity, so setValue() on
 * the iterated entry, or put() of the same key, made the iterator's remove() throw ConcurrentModificationException,
 * which no HashMap does: changing a value is not a structural change. A changed value under the same key now passes;
 * a different key still throws.
 */
class CompactMapSingleEntryIteratorTest {

    @Test
    void setValueThenRemoveThroughTheIterator() {
        for (String key : new String[]{"id", "other"}) {         // the value stored alone, and an entry
            CompactMap<String, Object> map = new CompactMap<>();
            map.put(key, 1);
            Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
            Map.Entry<String, Object> entry = it.next();
            assertEquals(1, entry.setValue(2));
            assertEquals(2, map.get(key));
            it.remove();
            assertTrue(map.isEmpty(), key);
            assertFalse(it.hasNext());
        }
    }

    @Test
    void putOfTheSameKeyDuringIteration() {
        for (String key : new String[]{"id", "other"}) {
            CompactMap<String, Object> map = new CompactMap<>();
            map.put(key, 1);
            Iterator<String> it = map.keySet().iterator();
            assertEquals(key, it.next());
            map.put(key, 3);
            it.remove();
            assertTrue(map.isEmpty(), key);
        }
    }

    @Test
    void aDifferentKeyIsStillAConcurrentModification() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        it.next();
        map.remove("a");
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::remove);

        CompactMap<String, Object> grown = new CompactMap<>();
        grown.put("a", 1);
        Iterator<String> it2 = grown.keySet().iterator();
        it2.next();
        grown.put("b", 2);                                    // a structural change: one entry became two
        assertThrows(ConcurrentModificationException.class, it2::remove);
    }
}
