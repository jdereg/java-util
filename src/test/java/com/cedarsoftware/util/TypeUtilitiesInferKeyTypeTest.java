package com.cedarsoftware.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TypeUtilities.inferKeyType: a Map container type's key type, through its class hierarchy, as inferElementType does
 * for the value. json-io read the key as the container's first type argument, which is the value's in
 * StringMap&lt;V&gt; extends HashMap&lt;String, V&gt;: for a field StringMap&lt;Integer&gt; the key "k" was parsed as
 * an int.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TypeUtilitiesInferKeyTypeTest {

    static class StringMap<V> extends HashMap<String, V> {
    }

    static class Reversed<V, K> extends LinkedHashMap<K, V> {
    }

    static class Names extends TreeMap<String, Integer> {
    }

    static class Deeper<X> extends StringMap<List<X>> {
    }

    static class MultiMap<K, V> extends HashMap<K, List<V>> {
    }

    @SuppressWarnings("unused")
    static class Fields {
        Map<Long, String> plain;
        StringMap<Integer> stringMap;
        Reversed<Integer, Character> reversed;
        Deeper<Double> deeper;
        MultiMap<Short, Byte> multiMap;
        Map<List<String>, Integer> listKey;
        HashMap<String, Integer> hashMap;
        List<String> notAMap;
    }

    private static Type field(String name) throws Exception {
        return Fields.class.getDeclaredField(name).getGenericType();
    }

    @Test
    void theKeyThroughTheHierarchy() throws Exception {
        assertEquals(Long.class, TypeUtilities.inferKeyType(field("plain")));
        assertEquals(String.class, TypeUtilities.inferKeyType(field("stringMap")));
        assertEquals(Character.class, TypeUtilities.inferKeyType(field("reversed")));
        assertEquals(String.class, TypeUtilities.inferKeyType(field("deeper")));
        assertEquals(Short.class, TypeUtilities.inferKeyType(field("multiMap")));
        assertEquals("java.util.List<java.lang.String>", TypeUtilities.inferKeyType(field("listKey")).getTypeName());
        assertEquals(String.class, TypeUtilities.inferKeyType(field("hashMap")));
    }

    /** The value, by inferElementType, agrees: the container's arguments are matched to Map's, not taken in order. */
    @Test
    void theValueAgrees() throws Exception {
        assertEquals(Integer.class, TypeUtilities.inferElementType(field("stringMap"), null));
        assertEquals(Integer.class, TypeUtilities.inferElementType(field("reversed"), null));
    }

    @Test
    void aClassWhoseHierarchyFixesTheKey() {
        assertEquals(String.class, TypeUtilities.inferKeyType(Names.class));
        assertEquals(String.class, TypeUtilities.inferKeyType(StringMap.class));    // raw, but its key is fixed
    }

    @Test
    void unknownOrNotAMap() throws Exception {
        assertNull(TypeUtilities.inferKeyType(HashMap.class));          // raw: its own K, unbound
        assertNull(TypeUtilities.inferKeyType(field("notAMap")));
        assertNull(TypeUtilities.inferKeyType(String.class));
        assertNull(TypeUtilities.inferKeyType(null));
    }
}
