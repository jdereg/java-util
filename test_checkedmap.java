import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class test_checkedmap {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        
        // Create a checked map
        Map<String, String> checked = Collections.checkedMap(map, String.class, String.class);
        System.out.println("CheckedMap class: " + checked.getClass().getName());
        
        // Test if it matches our current pattern detection
        String typeName = checked.getClass().getName();
        boolean isCheckedMap = typeName.contains("CheckedMap") || typeName.endsWith("$CheckedMap");
        System.out.println("Current detection would match: " + isCheckedMap);
        
        // Test different pattern approaches
        System.out.println("Contains 'Checked': " + typeName.contains("Checked"));
        System.out.println("EndsWith '$CheckedMap': " + typeName.endsWith("$CheckedMap"));
    }
}