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
        
        // Get the actual stored keys from the map entries
        for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
            Object[] storedKeys = entry.keys;
            log.info("Stored keys array length: " + storedKeys.length);
            log.info("Stored keys contents:");
            
            for (int i = 0; i < storedKeys.length; i++) {
                Object obj = storedKeys[i];
                log.info("  [" + i + "] " + obj + " (class: " + obj.getClass().getSimpleName() + ")");
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