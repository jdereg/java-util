package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Test the detection logic for already-flattened collections
 */
class MultiKeyMapDetectionLogicTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapDetectionLogicTest.class.getName());

    @Test
    void testDetectionLogic() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a complex structure that will definitely create a flattened ArrayList
        String[][] berries2D = {{"raspberry", "blackberry"}, {null, "cranberry"}};
        List<String> berriesList1D = Arrays.asList("strawberry", null, "blueberry");
        Object[] complexArray = {berriesList1D, "middle_string", berries2D, null};
        map.put(complexArray, "debug_value");
        
        // Verify entrySet() works
        Object reconstructedKey = null;
        for (java.util.Map.Entry<Object, String> entry : map.entrySet()) {
            reconstructedKey = entry.getKey();
            break;
        }

        log.info("Reconstructed key type: " + reconstructedKey.getClass().getSimpleName());
        log.info("Reconstructed key: " + reconstructedKey);

        // Access internal bucket structure for testing flattened keys with markers
        Field bucketsField = MultiKeyMap.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        Object buckets = bucketsField.get(map);

        // Get first non-null bucket chain
        Object storedKey = null;
        if (buckets instanceof java.util.concurrent.atomic.AtomicReferenceArray) {
            @SuppressWarnings("unchecked")
            java.util.concurrent.atomic.AtomicReferenceArray<Object[]> bucketsArray =
                (java.util.concurrent.atomic.AtomicReferenceArray<Object[]>) buckets;
            for (int i = 0; i < bucketsArray.length(); i++) {
                Object[] chain = bucketsArray.get(i);
                if (chain != null && chain.length > 0) {
                    // Access MultiKey.keys field
                    Object multiKey = chain[0];
                    Field keysField = multiKey.getClass().getDeclaredField("keys");
                    keysField.setAccessible(true);
                    Object keys = keysField.get(multiKey);
                    if (keys instanceof Object[]) {
                        storedKey = ((Object[]) keys)[0];
                    } else {
                        storedKey = keys;
                    }
                    break;
                }
            }
        }

        log.info("Internal stored key type: " + (storedKey != null ? storedKey.getClass().getSimpleName() : "null"));
        log.info("Internal stored key: " + storedKey);
        
        if (!(storedKey instanceof Collection)) {
            log.info("Not a collection - using original debug test instead");
            return;
        }
        
        ArrayList<Object> flattenedList = (ArrayList<Object>) storedKey;
        
        log.info("=== Detection Logic Test ===");
        log.info("Flattened list: " + flattenedList);
        log.info("List size: " + flattenedList.size());
        
        // Get access to sentinel objects
        Field nullSentinelField = MultiKeyMap.class.getDeclaredField("NULL_SENTINEL");
        nullSentinelField.setAccessible(true);
        Object NULL_SENTINEL = nullSentinelField.get(null);
        
        Field openField = MultiKeyMap.class.getDeclaredField("OPEN");
        openField.setAccessible(true);
        Object OPEN = openField.get(null);
        
        Field closeField = MultiKeyMap.class.getDeclaredField("CLOSE");
        closeField.setAccessible(true);
        Object CLOSE = closeField.get(null);
        
        // Test the detection logic manually
        boolean isAlreadyFlattened = false;
        for (Object element : flattenedList) {
            log.info("Element: " + element + " (" + element.getClass().getSimpleName() + ")");
            log.info("  == NULL_SENTINEL: " + (element == NULL_SENTINEL));
            log.info("  == OPEN: " + (element == OPEN));
            log.info("  == CLOSE: " + (element == CLOSE));
            
            if (element == NULL_SENTINEL || element == OPEN || element == CLOSE) {
                log.info("  DETECTION: Found sentinel object!");
                isAlreadyFlattened = true;
                break;
            }
        }
        
        log.info("Final detection result: " + isAlreadyFlattened);
        
        // Now test dumpExpandedKeyStatic directly
        Method dumpMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpMethod.setAccessible(true);
        
        String result = (String) dumpMethod.invoke(null, flattenedList, true, map);
        log.info("dumpExpandedKeyStatic result: " + result);
    }
}