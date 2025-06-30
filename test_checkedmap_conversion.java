import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

public class test_checkedmap_conversion {
    public static void main(String[] args) {
        // Create source map
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("key1", "value1");
        sourceMap.put("key2", "value2");
        
        // Create a CheckedMap to get its class
        Map<String, String> checkedExample = Collections.checkedMap(new HashMap<>(), String.class, String.class);
        Class<?> checkedMapClass = checkedExample.getClass();
        
        System.out.println("Attempting to convert to: " + checkedMapClass.getName());
        
        try {
            // Attempt conversion
            Converter converter = new Converter(new DefaultConverterOptions());
            Object result = converter.convert(sourceMap, checkedMapClass);
            
            System.out.println("Conversion successful!");
            System.out.println("Result type: " + result.getClass().getName());
            System.out.println("Result: " + result);
            
        } catch (Exception e) {
            System.out.println("Conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}