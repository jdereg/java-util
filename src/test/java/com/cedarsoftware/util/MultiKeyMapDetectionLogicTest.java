package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Test the detection logic for already-flattened collections
 */
class MultiKeyMapDetectionLogicTest {

    @Test
    void testDetectionLogic() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a complex structure that will definitely create a flattened ArrayList
        String[][] berries2D = {{"raspberry", "blackberry"}, {null, "cranberry"}};
        List<String> berriesList1D = Arrays.asList("strawberry", null, "blueberry");
        Object[] complexArray = {berriesList1D, "middle_string", berries2D, null};
        map.put(complexArray, "debug_value");
        
        // Get the stored flattened structure
        Object storedKey = null;
        for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
            storedKey = entry.keys[0];
            break;
        }
        
        System.out.println("Stored key type: " + storedKey.getClass().getSimpleName());
        System.out.println("Stored key: " + storedKey);
        
        if (!(storedKey instanceof Collection)) {
            System.out.println("Not a collection - using original debug test instead");
            return;
        }
        
        ArrayList<Object> flattenedList = (ArrayList<Object>) storedKey;
        
        System.out.println("=== Detection Logic Test ===");
        System.out.println("Flattened list: " + flattenedList);
        System.out.println("List size: " + flattenedList.size());
        
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
            System.out.println("Element: " + element + " (" + element.getClass().getSimpleName() + ")");
            System.out.println("  == NULL_SENTINEL: " + (element == NULL_SENTINEL));
            System.out.println("  == OPEN: " + (element == OPEN));
            System.out.println("  == CLOSE: " + (element == CLOSE));
            
            if (element == NULL_SENTINEL || element == OPEN || element == CLOSE) {
                System.out.println("  DETECTION: Found sentinel object!");
                isAlreadyFlattened = true;
                break;
            }
        }
        
        System.out.println("Final detection result: " + isAlreadyFlattened);
        
        // Now test dumpExpandedKeyStatic directly
        Method dumpMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpMethod.setAccessible(true);
        
        String result = (String) dumpMethod.invoke(null, flattenedList, true, map);
        System.out.println("dumpExpandedKeyStatic result: " + result);
    }
}