package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

/**
 * Debug test to understand what's in the flattened keys for complex structures
 */
class MultiKeyMapFlattenedKeysDebugTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapFlattenedKeysDebugTest.class.getName());

    @Test
    void debugFlattenedKeys() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a complex nested structure like in the failing tests
        String[] berries1D = {"strawberry", null, "blueberry"};
        String[][] berries2D = {{"raspberry", "blackberry"}, {null, "cranberry"}};
        
        List<String> berriesList1D = Arrays.asList("strawberry", null, "blueberry");
        List<List<String>> berriesList2D = Arrays.asList(
            Arrays.asList("raspberry", "blackberry"),
            Arrays.asList(null, "cranberry")
        );
        
        Object[] outerArray = {berriesList1D, "middle_string", berriesList2D, null};
        
        map.put(outerArray, "debug_value");
        
        log.info("=== Complex Structure Debug ===");
        log.info("Map toString output:");
        log.info(map.toString());
        log.info("");
        
        // Get access to the private fields
        Field nullSentinelField = MultiKeyMap.class.getDeclaredField("NULL_SENTINEL");
        nullSentinelField.setAccessible(true);
        Object NULL_SENTINEL = nullSentinelField.get(null);
        
        Field openField = MultiKeyMap.class.getDeclaredField("OPEN");
        openField.setAccessible(true);
        Object OPEN = openField.get(null);
        
        Field closeField = MultiKeyMap.class.getDeclaredField("CLOSE");
        closeField.setAccessible(true);
        Object CLOSE = closeField.get(null);
        
        // Verify entrySet() works
        log.info("Testing entrySet():");
        for (java.util.Map.Entry<Object, String> entry : map.entrySet()) {
            log.info("  Reconstructed key: " + entry.getKey());
            log.info("  Reconstructed key type: " + entry.getKey().getClass().getSimpleName());
        }
        log.info("");

        // Access internal bucket structure to examine flattened keys with markers
        Field bucketsField = MultiKeyMap.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        Object buckets = bucketsField.get(map);

        // Get actual stored keys from internal buckets
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

                    Object[] storedKeys;
                    if (keys instanceof Object[]) {
                        storedKeys = (Object[]) keys;
                    } else {
                        storedKeys = new Object[]{keys};
                    }

                    log.info("Stored keys array length: " + storedKeys.length);
                    log.info("Stored keys contents:");

                    for (int idx = 0; idx < storedKeys.length; idx++) {
                        Object obj = storedKeys[idx];
                        log.info("  [" + idx + "] " + obj + " (class: " + obj.getClass().getSimpleName() + ")");
                        log.info("      == NULL_SENTINEL: " + (obj == NULL_SENTINEL));
                        log.info("      == OPEN: " + (obj == OPEN));
                        log.info("      == CLOSE: " + (obj == CLOSE));

                        // If it's a collection, examine its contents
                        if (obj instanceof Collection) {
                            Collection<?> coll = (Collection<?>) obj;
                            log.info("      Collection size: " + coll.size());
                            log.info("      Collection contents:");
                            int j = 0;
                            for (Object element : coll) {
                                log.info("        [" + j + "] " + element + " (class: " + element.getClass().getSimpleName() + ")");
                                log.info("            == NULL_SENTINEL: " + (element == NULL_SENTINEL));
                                log.info("            == OPEN: " + (element == OPEN));
                                log.info("            == CLOSE: " + (element == CLOSE));
                                j++;
                                if (j > 10) { // Limit output for readability
                                    log.info("        ... (truncated)");
                                    break;
                                }
                            }
                        }
                    }
                    break; // Only process the first entry
                }
            }
        }
    }
}