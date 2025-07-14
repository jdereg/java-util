package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiKeyMapTest {
    @Test
    void testSingleElementArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(MultiKeyMap.CollectionKeyMode.COLLECTIONS_EXPANDED, true);

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
        
        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

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
}
