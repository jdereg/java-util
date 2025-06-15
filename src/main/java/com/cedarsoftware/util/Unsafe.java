package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.cedarsoftware.util.ClassUtilities.forName;
import static com.cedarsoftware.util.ClassUtilities.trySetAccessible;

/**
 * Wrapper for unsafe, decouples direct usage of sun.misc.* package.
 * @author Kai Hufenback
 */
final class Unsafe
{
    private final Object sunUnsafe;
    private final Method allocateInstance;

    /**
     * Constructs unsafe object, acting as a wrapper.
     * @throws InvocationTargetException
     */
    public Unsafe() throws InvocationTargetException {
        try {
            Class<?> unsafeClass = forName("sun.misc.Unsafe", ClassUtilities.getClassLoader(Unsafe.class));
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            trySetAccessible(f);
            sunUnsafe = f.get(null);
            allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            trySetAccessible(allocateInstance);
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to use sun.misc.Unsafe to construct objects.", e);
        }
    }

    /**
     * Creates an object without invoking constructor or initializing variables.
     * <b>Be careful using this with JDK objects, like URL or ConcurrentHashMap this may bring your VM into troubles.</b>
     * @param clazz to instantiate
     * @return allocated Object
     */
    public Object allocateInstance(Class<?> clazz)
    {
        if (clazz == null || clazz.isInterface()) {
            String name = clazz == null ? "null" : clazz.getName();
            throw new IllegalArgumentException("Unable to create instance of class: " + name);
        }

        try {
            return allocateInstance.invoke(sunUnsafe, clazz);
        }
        catch (IllegalAccessException | IllegalArgumentException e ) {
            String name = clazz.getName();
            throw new IllegalArgumentException("Unable to create instance of class: " + name, e);
        }
        catch (InvocationTargetException e) {
            String name = clazz.getName();
            throw new IllegalArgumentException("Unable to create instance of class: " + name, e.getCause() != null ? e.getCause() : e);
        }
    }
}
