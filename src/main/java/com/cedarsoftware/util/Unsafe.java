package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.cedarsoftware.util.ClassUtilities.trySetAccessible;

/**
 * Provides constructor-bypassing object instantiation using two strategies:
 * <ol>
 *   <li><b>ReflectionFactory</b> (preferred) — creates a synthetic constructor that runs
 *       {@code Object.<init>()} instead of the class's own constructors. This is the same
 *       mechanism used by {@code ObjectInputStream}. Fields get Java default values and the
 *       object header is properly initialized. May not be accessible on JDK 17+ without
 *       {@code --add-opens java.base/jdk.internal.reflect=ALL-UNNAMED}.</li>
 *   <li><b>sun.misc.Unsafe</b> (fallback) — allocates raw memory without any constructor call.
 *       Still accessible on current JDKs but deprecated for removal starting in JDK 26.</li>
 * </ol>
 *
 * @author Kai Hufenback
 *         John DeRegnaucourt (jdereg@cedarsoft.com)
 */
final class Unsafe {
    private final Object reflectionFactory;
    private final Method newConstructorForSerialization;
    private final Constructor<?> objectConstructor;
    private final Object sunUnsafe;
    private final Method unsafeAllocateInstance;
    private final ConcurrentMap<Class<?>, Constructor<?>> serializationConstructorCache = new ConcurrentHashMap<>();

    /**
     * Constructs the wrapper, reflectively loading ReflectionFactory and sun.misc.Unsafe.
     * At least one must be available, otherwise throws IllegalStateException.
     */
    public Unsafe() {
        // ── Strategy 1: ReflectionFactory (preferred, same as ObjectInputStream) ──
        Object rfInstance = null;
        Method ncsMethod = null;
        Constructor<?> objCtor = null;
        try {
            // JDK 9+: jdk.internal.reflect.ReflectionFactory
            // JDK 8:  sun.reflect.ReflectionFactory
            Class<?> rfClass;
            try {
                rfClass = Class.forName("sun.reflect.ReflectionFactory");
            } catch (ClassNotFoundException e) {
                rfClass = Class.forName("jdk.internal.reflect.ReflectionFactory");
            }

            Method getFactory = rfClass.getDeclaredMethod("getReflectionFactory");
            rfInstance = getFactory.invoke(null);
            ncsMethod = rfClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class);
            objCtor = Object.class.getDeclaredConstructor();

            // Verify it works
            Constructor<?> test = (Constructor<?>) ncsMethod.invoke(rfInstance, Object.class, objCtor);
            if (test == null) {
                rfInstance = null;
                ncsMethod = null;
                objCtor = null;
            }
        } catch (Exception ignored) {
            rfInstance = null;
            ncsMethod = null;
            objCtor = null;
        }
        this.reflectionFactory = rfInstance;
        this.newConstructorForSerialization = ncsMethod;
        this.objectConstructor = objCtor;

        // ── Strategy 2: sun.misc.Unsafe (fallback, deprecated in JDK 23+) ──
        Object unsafeObj = null;
        Method allocMethod = null;
        try {
            Class<?> unsafeClass = ClassUtilities.forName("sun.misc.Unsafe", ClassUtilities.getClassLoader(Unsafe.class));
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            trySetAccessible(f);
            unsafeObj = f.get(null);
            allocMethod = ReflectionUtils.getMethod(unsafeClass, "allocateInstance", Class.class);
        } catch (Exception ignored) {
            // sun.misc.Unsafe not available (JDK 26+ or security restriction)
        }
        this.sunUnsafe = unsafeObj;
        this.unsafeAllocateInstance = allocMethod;

        if (reflectionFactory == null && sunUnsafe == null) {
            throw new IllegalStateException("Neither ReflectionFactory nor sun.misc.Unsafe is available for constructor-bypassing instantiation.");
        }
    }

    /**
     * Creates an object without invoking the class's own constructors.
     * Tries ReflectionFactory first (safer), then falls back to sun.misc.Unsafe.
     *
     * @param clazz the class to instantiate
     * @return allocated Object
     * @throws IllegalArgumentException if the class cannot be instantiated
     */
    public Object allocateInstance(Class<?> clazz) {
        if (clazz == null || clazz.isInterface()) {
            String name = clazz == null ? "null" : clazz.getName();
            throw new IllegalArgumentException("Unable to create instance of class: " + name);
        }

        // Strategy 1: ReflectionFactory — serialization constructor cached per class
        if (reflectionFactory != null) {
            try {
                Constructor<?> ctor = serializationConstructorCache.get(clazz);
                if (ctor == null) {
                    ctor = createSerializationConstructor(clazz);
                    if (ctor != null) {
                        serializationConstructorCache.put(clazz, ctor);
                        return ctor.newInstance();
                    }
                } else {
                    return ctor.newInstance();
                }
            } catch (Exception ignored) {
                // ReflectionFactory failed for this class — fall through to Unsafe
            }
        }

        // Strategy 2: sun.misc.Unsafe — raw allocation fallback
        if (sunUnsafe != null && unsafeAllocateInstance != null) {
            try {
                return ReflectionUtils.call(sunUnsafe, unsafeAllocateInstance, clazz);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to create instance of class: " + clazz.getName(), e);
            }
        }

        throw new IllegalArgumentException("Unable to create instance of class: " + clazz.getName());
    }

    private Constructor<?> createSerializationConstructor(Class<?> clazz) {
        try {
            Constructor<?> ctor = (Constructor<?>) newConstructorForSerialization.invoke(reflectionFactory, clazz, objectConstructor);
            if (ctor != null) {
                trySetAccessible(ctor);
            }
            return ctor;
        } catch (Exception e) {
            return null;
        }
    }
}
