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

    @Test
    void testSingleElementArrayKeys3() {
        MultiKeyMap<String> map = new MultiKeyMap<>(true);

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");

        // When flattenDimensions=true, multi-dimensional arrays with same elements should be treated as same key
        // So we should have: "a", "b", "c", and the flattened array key ["a", "b", "c"] 
        assert map.size() == 4;  // "a", "b", "c", and the flattened multi-dimensional key
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals("[[[alpha, beta, gamma]]]", map.get(new String[]{"a", "b", "c"}));      // last put for flattened key
        
        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(new String[]{"a", "b", "c"});
        assert map.containsKey(new String[][]{{"a", "b", "c"}});
        assert map.containsKey(new String[][][]{{{"a", "b", "c"}}});
        
        assert map.containsMultiKey("a");
        assert map.containsMultiKey("b");
        assert map.containsMultiKey("c");
        assert map.containsMultiKey((Object) new String[]{"a", "b", "c"});
        assert map.containsMultiKey((Object) new String[][]{{"a", "b", "c"}});
        assert map.containsMultiKey((Object) new String[][][]{{{"a", "b", "c"}}});
        
        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(new String[]{"a", "b", "c"});
        assert map.isEmpty();
        
        map.putMultiKey("alpha", "a");
        map.putMultiKey("beta", "b");
        map.putMultiKey("gamma", "c");
        map.putMultiKey("[alpha, beta, gamma]", (Object) new String[]{"a", "b", "c"});
        map.putMultiKey("[[alpha, beta, gamma]]", (Object) new String[][]{{"a", "b", "c"}});
        map.putMultiKey("[[[alpha, beta, gamma]]]", (Object) new String[][][]{{{"a", "b", "c"}}});
        map.putMultiKey("collection: [alpha, beta, gamma]", (Object) CollectionUtilities.listOf("a", "b", "c"));

        // When flattenDimensions=true, arrays/collections with same elements flatten to same key
        // So we have: "a", "b", "c", and the flattened multi-element key (arrays + collection = same key)
        assert map.size() == 4;  // "a", "b", "c", and the flattened multi-element key
        
        map.removeMultiKey("a");
        assert map.size() == 3;
        map.removeMultiKey("b");
        assert map.size() == 2;
        map.removeMultiKey("c");
        assert map.size() == 1;
        map.removeMultiKey((Object) new String[]{"a", "b", "c"});
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        map.remove(new String[][][]{{{"a", "b", "c"}}});
        assert map.size() == 3;  // Still have "a", "b", "c" (the flattened multi-element key was removed)

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        map.removeMultiKey((Object) new String[][][]{{{"a", "b", "c"}}});
        assert map.size() == 3;  // Still have "a", "b", "c" (the flattened multi-element key was removed)
    }

    @Test
    void testSingleElementArrayKeysFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(true));

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        assert map.size() == 4;  // Individual keys and array/collection keys are different when flattened
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals("collection: [alpha, beta, gamma]", map.get(new String[]{"a", "b", "c"}));      // last put for flattened key

        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(new String[]{"a", "b", "c"});
        assert map.containsKey(CollectionUtilities.listOf("a", "b", "c"));

        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(new String[]{"a", "b", "c"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysNoFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(false));

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        assert map.size() == 4;  // All keys are different when not flattened: "a", "b", "c", array, collection
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals(null, map.get(new String[]{"a", "b", "c"}));  // Array key was overwritten by collection
        assertEquals("collection: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("a", "b", "c")));

        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(new String[]{"a", "b", "c"});
        assert map.containsKey(CollectionUtilities.listOf("a", "b", "c"));

        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(new String[]{"a", "b", "c"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(true));

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "[alpha, beta, gamma]");
        map.put(new String[]{"a", "b", "c"}, "array: [alpha, beta, gamma]");

        assert map.size() == 4;  // Individual keys and collection/array keys are different when flattened
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals("array: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("a", "b", "c")));      // last put for flattened key

        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(CollectionUtilities.listOf("a", "b", "c"));
        assert map.containsKey(new String[]{"a", "b", "c"});

        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("a", "b", "c"));
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysNoFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>(false));

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "[alpha, beta, gamma]");
        map.put(new String[]{"a", "b", "c"}, "array: [alpha, beta, gamma]");

        assert map.size() == 4;  // Keys when not flattened: "a", "b", "c", and collection/array (treated as same)
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals("array: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("a", "b", "c")));  // Array put last overwrites collection
        assertEquals("array: [alpha, beta, gamma]", map.get(new String[]{"a", "b", "c"}));

        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(CollectionUtilities.listOf("a", "b", "c"));
        assert map.containsKey(new String[]{"a", "b", "c"});

        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("a", "b", "c"));
        assert map.isEmpty();
    }
}
