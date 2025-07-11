import com.cedarsoftware.util.CaseInsensitiveMap;
import java.util.*;

public class debug_test {
    public static void main(String[] args) {
        System.out.println("Creating CaseInsensitiveMap...");
        Map<String, Object> map = new CaseInsensitiveMap<>();
        System.out.println("Map created, size: " + map.size());
        
        System.out.println("Adding entries...");
        map.put("One", null);
        System.out.println("Added 'One', size: " + map.size());
        map.put("Two", null);
        System.out.println("Added 'Two', size: " + map.size());
        map.put("Three", null);
        System.out.println("Added 'Three', size: " + map.size());
        
        System.out.println("Keys in map: " + map.keySet());
        
        System.out.println("Starting iteration...");
        Iterator<String> i = map.keySet().iterator();
        while (i.hasNext()) {
            String elem = i.next();
            System.out.println("Current element: " + elem);
            if (elem.equals("One")) {
                System.out.println("Found 'One', removing...");
                i.remove();
                System.out.println("After remove, size: " + map.size());
            }
        }
        
        System.out.println("Final map size: " + map.size());
        System.out.println("Final keys: " + map.keySet());
    }
}
EOF < /dev/null
