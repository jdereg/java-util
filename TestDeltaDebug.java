import com.cedarsoftware.util.GraphComparator;
import com.cedarsoftware.util.GraphComparator.Delta;
import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.util.Traverser;
import java.util.List;

public class TestDeltaDebug {
    
    static class Pet {
        long id;
        String[] nickNames;
        
        Pet(long id, String[] nickNames) {
            this.id = id;
            this.nickNames = nickNames;
        }
    }
    
    static class Person {
        long id;
        Pet[] pets;
        
        Person(long id) {
            this.id = id;
        }
    }
    
    public static void main(String[] args) {
        // Create test data similar to the failing test
        Person person1 = new Person(UniqueIdGenerator.getUniqueId());
        Person person2 = new Person(person1.id);
        
        long petId = UniqueIdGenerator.getUniqueId();
        Pet pet1 = new Pet(petId, new String[]{"fido", "bruiser"});
        Pet pet2 = new Pet(petId, new String[0]);  // Empty array
        
        person1.pets = new Pet[]{pet1};
        person2.pets = new Pet[]{pet2};
        
        // Create ID fetcher
        GraphComparator.ID idFetcher = new GraphComparator.ID() {
            public Object getId(Object objectToFetch) {
                if (objectToFetch instanceof Person) {
                    return ((Person) objectToFetch).id;
                } else if (objectToFetch instanceof Pet) {
                    return ((Pet) objectToFetch).id;
                }
                return null;
            }
        };
        
        // Compare
        List<Delta> deltas = GraphComparator.compare(person1, person2, idFetcher);
        
        System.out.println("Number of deltas: " + deltas.size());
        for (Delta delta : deltas) {
            System.out.println("Delta: cmd=" + delta.getCmd() + 
                             ", fieldName=" + delta.getFieldName() + 
                             ", id=" + delta.getId() +
                             ", optionalKey=" + delta.getOptionalKey() +
                             ", sourceValue=" + delta.getSourceValue() +
                             ", targetValue=" + delta.getTargetValue());
        }
        
        // Check if person1.pets[0] has an ID
        System.out.println("\nBefore applying deltas:");
        System.out.println("person1.pets[0].id = " + person1.pets[0].id);
        System.out.println("petId = " + petId);
        
        // Debug: Let's see what's being traversed and what has IDs
        System.out.println("\nObjects being traversed:");
        Traverser.traverse(person1, visit -> {
            Object o = visit.getNode();
            boolean hasId = idFetcher.getId(o) != null;
            if (o instanceof Person) {
                System.out.println("  Person: id=" + ((Person)o).id + ", hasId=" + hasId);
                System.out.println("    Fields: " + visit.getFields());
            } else if (o instanceof Pet) {
                System.out.println("  Pet: id=" + ((Pet)o).id + ", hasId=" + hasId);
                System.out.println("    Fields: " + visit.getFields());
            } else if (o != null) {
                System.out.println("  " + o.getClass().getSimpleName() + ": " + o + ", hasId=" + hasId);
            }
        }, null);
        
        // Apply deltas
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(person1, deltas, idFetcher, GraphComparator.getJavaDeltaProcessor());
        
        System.out.println("\nErrors: " + errors.size());
        for (GraphComparator.DeltaError error : errors) {
            System.out.println("Error: " + error.getError());
        }
        
        System.out.println("\nAfter applying deltas:");
        System.out.println("person1.pets[0].nickNames.length = " + person1.pets[0].nickNames.length);
        System.out.println("Expected: 0");
    }
}