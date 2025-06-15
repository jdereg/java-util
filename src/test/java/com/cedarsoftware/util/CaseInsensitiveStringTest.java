package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseInsensitiveStringTest {

    @AfterEach
    public void cleanup() {
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    @Test
    void testOfCaching() {
        CaseInsensitiveMap.CaseInsensitiveString first = CaseInsensitiveMap.CaseInsensitiveString.of("Alpha");
        CaseInsensitiveMap.CaseInsensitiveString second = CaseInsensitiveMap.CaseInsensitiveString.of("Alpha");
        assertSame(first, second);

        CaseInsensitiveMap.CaseInsensitiveString diffCase = CaseInsensitiveMap.CaseInsensitiveString.of("ALPHA");
        assertNotSame(first, diffCase);
        assertEquals(first, diffCase);

        assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.CaseInsensitiveString.of(null));
    }

    @Test
    void testContains() {
        CaseInsensitiveMap.CaseInsensitiveString cis = CaseInsensitiveMap.CaseInsensitiveString.of("HelloWorld");
        assertTrue(cis.contains("hell"));
        assertTrue(cis.contains("WORLD"));
        assertFalse(cis.contains("xyz"));
    }

    @Test
    void testSerializationReadObject() throws Exception {
        CaseInsensitiveMap.CaseInsensitiveString original = CaseInsensitiveMap.CaseInsensitiveString.of("SerializeMe");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(original);
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
        CaseInsensitiveMap.CaseInsensitiveString copy =
                (CaseInsensitiveMap.CaseInsensitiveString) in.readObject();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertEquals(original.toString(), copy.toString());
    }
}
