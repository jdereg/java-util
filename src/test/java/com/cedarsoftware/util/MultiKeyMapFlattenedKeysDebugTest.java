package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Debug test to understand what's in the flattened keys for complex structures
 */
class MultiKeyMapFlattenedKeysDebugTest {

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
        
        System.out.println("=== Complex Structure Debug ===");
        System.out.println("Map toString output:");
        System.out.println(map.toString());
        System.out.println();
        
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
            System.out.println("Stored keys array length: " + storedKeys.length);
            System.out.println("Stored keys contents:");
            
            for (int i = 0; i < storedKeys.length; i++) {
                Object obj = storedKeys[i];
                System.out.println("  [" + i + "] " + obj + " (class: " + obj.getClass().getSimpleName() + ")");
                System.out.println("      == NULL_SENTINEL: " + (obj == NULL_SENTINEL));
                System.out.println("      == OPEN: " + (obj == OPEN));
                System.out.println("      == CLOSE: " + (obj == CLOSE));
                
                // If it's a collection, examine its contents
                if (obj instanceof Collection) {
                    Collection<?> coll = (Collection<?>) obj;
                    System.out.println("      Collection size: " + coll.size());
                    System.out.println("      Collection contents:");
                    int j = 0;
                    for (Object element : coll) {
                        System.out.println("        [" + j + "] " + element + " (class: " + element.getClass().getSimpleName() + ")");
                        System.out.println("            == NULL_SENTINEL: " + (element == NULL_SENTINEL));
                        System.out.println("            == OPEN: " + (element == OPEN));
                        System.out.println("            == CLOSE: " + (element == CLOSE));
                        j++;
                        if (j > 10) { // Limit output for readability
                            System.out.println("        ... (truncated)");
                            break;
                        }
                    }
                }
            }
            break; // Only process the first entry
        }
    }
}