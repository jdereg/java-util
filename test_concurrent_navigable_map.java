import java.util.concurrent.ConcurrentNavigableMap;
import java.util.HashMap;
import java.util.Map;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;

public class test_concurrent_navigable_map {
    public static void main(String[] args) {
        // Create a source map
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("key1", "value1");
        sourceMap.put("key2", "value2");
        sourceMap.put(null, "nullKeyValue");
        
        // Convert to ConcurrentNavigableMap interface
        Converter converter = new Converter(new ConverterOptions());
        ConcurrentNavigableMap<String, String> result = converter.convert(sourceMap, ConcurrentNavigableMap.class);
        
        System.out.println("Conversion successful!");
        System.out.println("Result type: " + result.getClass().getName());
        System.out.println("Size: " + result.size());
        System.out.println("Contains null key: " + result.containsKey(null));
        System.out.println("Null key value: " + result.get(null));
        System.out.println("All entries: " + result);
    }
}