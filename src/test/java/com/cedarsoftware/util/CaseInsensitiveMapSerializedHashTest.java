package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A CaseInsensitiveString was read back with the hash stored in the stream: the hash of the release that wrote it.
 * A map read back files each key by its hashCode(), so once hashCodeIgnoreCase() computed other values, a map
 * written by an older release answered null for keys it held. A key is now rebuilt from its text when it is read.
 */
class CaseInsensitiveMapSerializedHashTest {

    /** 4.112.0 hashed as String.hashCode() of the lower-cased text; this release clears the ASCII case bit. */
    @Test
    void aMapWrittenBy4112IsReadBackWithThisReleasesHashes() throws Exception {
        assertNotEquals("alpha".hashCode(), StringUtilities.hashCodeIgnoreCase("alpha"));
        for (CaseInsensitiveMap<String, Integer> map : maps()) {
            storeHashes(map.getWrappedMap(), text -> text.toLowerCase(Locale.ROOT).hashCode());
            assertFindsItsKeys(roundTrip(map));
        }
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Alpha");
        set.add("Beta");
        set.add("Gamma");
        storeHashes(backingMapOf(set).getWrappedMap(), text -> text.toLowerCase(Locale.ROOT).hashCode());
        CaseInsensitiveSet<String> readBack = roundTrip(set);
        assertTrue(readBack.contains("ALPHA"));
        assertTrue(readBack.contains("beta"));
        assertTrue(readBack.contains("gAmMa"));
    }

    /** Whatever hash the stream holds, the key read back carries the one this release computes. */
    @Test
    void anyStoredHashIsReplacedWhenRead() throws Exception {
        for (CaseInsensitiveMap<String, Integer> map : maps()) {
            storeHashes(map.getWrappedMap(), text -> ~StringUtilities.hashCodeIgnoreCase(text));
            assertFindsItsKeys(roundTrip(map));
        }
    }

    private static CaseInsensitiveMap<String, Integer>[] maps() {
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer>[] maps = new CaseInsensitiveMap[]{
                new CaseInsensitiveMap<String, Integer>(),
                new CaseInsensitiveMap<String, Integer>(Collections.emptyMap(), new HashMap<>()),
                new CaseInsensitiveMap<String, Integer>(Collections.emptyMap(), new ConcurrentHashMap<>())};
        for (CaseInsensitiveMap<String, Integer> map : maps) {
            map.put("Alpha", 1);
            map.put("Beta", 2);
            map.put("Gamma", 3);
        }
        return maps;
    }

    private static void assertFindsItsKeys(CaseInsensitiveMap<String, Integer> map) {
        String backing = map.getWrappedMap().getClass().getSimpleName();
        assertEquals(1, map.get("ALPHA"), backing);
        assertEquals(2, map.get("beta"), backing);
        assertEquals(3, map.get("gAmMa"), backing);
        assertTrue(map.containsKey("Alpha"), backing);
        assertEquals(3, map.size(), backing);
    }

    /** Sets each key's stored hash as another release would have written it. */
    private static void storeHashes(Map<?, ?> backing, ToIntFunction<String> hashOf) throws Exception {
        Field hash = CaseInsensitiveMap.CaseInsensitiveString.class.getDeclaredField("hash");
        hash.setAccessible(true);
        for (Object key : backing.keySet()) {
            hash.setInt(key, hashOf.applyAsInt(key.toString()));
        }
    }

    private static CaseInsensitiveMap<?, ?> backingMapOf(CaseInsensitiveSet<?> set) throws Exception {
        Field map = CaseInsensitiveSet.class.getDeclaredField("backingMap");
        map.setAccessible(true);
        return (CaseInsensitiveMap<?, ?>) map.get(set);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
