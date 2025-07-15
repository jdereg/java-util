package com.cedarsoftware.util;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiKeyMapTest {
    @Test
    void testSingleElementArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(true);

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        assert map.size() == 1;
        assertEquals("[[[alpha]]]", map.get("a"));      // last put

        assert map.containsKey("a");
        assert map.containsKey(new String[]{"a"});
        assert map.containsKey(new String[][]{{"a"}});
        assert map.containsKey(new String[][][]{{{"a"}}});
        
        assert map.containsMultiKey("a");
        assert map.containsMultiKey((Object) new String[]{"a"});
        assert map.containsMultiKey((Object) new String[][]{{"a"}});
        assert map.containsMultiKey((Object) new String[][][]{{{"a"}}});
        
        map.remove("a");
        assert map.isEmpty();
        
        map.putMultiKey("alpha", "a");
        map.putMultiKey("[alpha]", (Object) new String[]{"a"});
        map.putMultiKey("[[alpha]]", (Object) new String[][]{{"a"}});
        map.putMultiKey("[[[alpha]]]", (Object) new String[][][]{{{"a"}}});

        map.removeMultiKey("a");
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        map.remove(new String[][][]{{{"a"}}});
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        map.removeMultiKey((Object) new String[][][]{{{"a"}}});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(true));

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");

        assert map.size() == 1;
        assertEquals("[alpha]", map.get("a"));      // last put

        assert map.containsKey("a");
        assert map.containsKey(new String[]{"a"});

        map.remove("a");
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(false));

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");

        assert map.size() == 2;
        assertEquals("alpha", map.get("a"));                    // last put
        assertEquals("[alpha]", map.get(new String[]{"a"}));      // last put

        assert map.containsKey("a");
        assert map.containsKey(new String[]{"a"});

        map.remove("a");
        assert map.size() == 1;
        map.remove(new String[]{"a"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(true));

        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");

        assert map.size() == 1;
        assertEquals("[alpha]", map.get("a"));      // last put

        assert map.containsKey("a");
        assert map.containsKey(CollectionUtilities.listOf("a"));

        map.remove("a");
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(false));

        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");

        assert map.size() == 2;
        assertEquals("alpha", map.get("a"));                    // string key value
        assertEquals("[alpha]", map.get(CollectionUtilities.listOf("a")));      // collection key value

        assert map.containsKey("a");
        assert map.containsKey(CollectionUtilities.listOf("a"));

        map.remove("a");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("a"));
        assert map.isEmpty();
    }
}
