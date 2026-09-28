package groovy.lang;

/**
 * A stand-in for Groovy's class loader, so java-util's tests need no Groovy dependency. ClassUtilities recognizes a
 * Groovy class loader by this class name, and its subclasses by their superclass chain. It holds only what the tests
 * use: like Groovy's, it defines a class from its bytes on request.
 */
public class GroovyClassLoader extends ClassLoader {

    public GroovyClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
