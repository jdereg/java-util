// Quick debug test
public class TestDebug {
    public static void main(String[] args) {
        try {
            Class<?> clazz = com.cedarsoftware.util.ClassUtilities.forName("java.lang.String", null);
            System.out.println("Result: " + clazz);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }
    }
}