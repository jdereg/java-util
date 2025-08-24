package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utilities to simplify writing reflective code as well as improve performance of reflective operations like
 * method and annotation lookups.
 *
 * <h2>Security Configuration</h2>
 * <p>ReflectionUtils provides configurable security controls to prevent various attack vectors including
 * unauthorized access to dangerous classes, sensitive field exposure, and reflection-based attacks.
 * All security features are <strong>disabled by default</strong> for backward compatibility.</p>
 *
 * <p>Security controls can be enabled via system properties:</p>
 * <ul>
 *   <li><code>reflectionutils.security.enabled=false</code> &mdash; Master switch for all security features</li>
 *   <li><code>reflectionutils.dangerous.class.validation.enabled=false</code> &mdash; Block dangerous class access</li>
 *   <li><code>reflectionutils.sensitive.field.validation.enabled=false</code> &mdash; Block sensitive field access</li>
 *   <li><code>reflectionutils.max.cache.size=50000</code> &mdash; Maximum cache size per cache type</li>
 *   <li><code>reflectionutils.dangerous.class.patterns=java.lang.Runtime,java.lang.Process,...</code> &mdash; Comma-separated dangerous class patterns</li>
 *   <li><code>reflectionutils.sensitive.field.patterns=password,secret,apikey,...</code> &mdash; Comma-separated sensitive field patterns</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>Dangerous Class Protection:</b> Prevents reflection access to system classes that could enable privilege escalation</li>
 *   <li><b>Sensitive Field Protection:</b> Blocks access to fields containing sensitive information (passwords, tokens, etc.)</li>
 *   <li><b>Cache Size Limits:</b> Configurable limits to prevent memory exhaustion attacks</li>
 *   <li><b>Trusted Caller Validation:</b> Allows java-util library internal access while blocking external callers</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Enable security with custom settings
 * System.setProperty("reflectionutils.security.enabled", "true");
 * System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
 * System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");
 * System.setProperty("reflectionutils.max.cache.size", "10000");
 *
 * // These will now enforce security controls
 * Method method = ReflectionUtils.getMethod(String.class, "valueOf", int.class);
 * Field field = ReflectionUtils.getField(MyClass.class, "normalField");
 * }</pre>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class ReflectionUtils {
    /** System property key controlling the reflection cache size. */
    private static final String CACHE_SIZE_PROPERTY = "reflection.utils.cache.size";
    private static final int DEFAULT_CACHE_SIZE = 1500;
    private static final int DEFAULT_MAX_CACHE_SIZE = 50000; // Default max to prevent memory exhaustion
    
    private static final Logger LOG = Logger.getLogger(ReflectionUtils.class.getName());
    
    // Default dangerous class patterns (moved to system properties in static initializer)
    private static final String DEFAULT_DANGEROUS_CLASS_PATTERNS = 
        "java.lang.Runtime,java.lang.Process,java.lang.ProcessBuilder,sun.misc.Unsafe,jdk.internal.misc.Unsafe,javax.script.ScriptEngine,javax.script.ScriptEngineManager";
    
    // Default sensitive field patterns (moved to system properties in static initializer)
    private static final String DEFAULT_SENSITIVE_FIELD_PATTERNS = 
        "password,passwd,secret,secretkey,apikey,api_key,authtoken,accesstoken,credential,confidential,adminkey,private";
    
    static {
        // Initialize system properties with defaults if not already set (backward compatibility)
        initializeSystemPropertyDefaults();
    }
    
    private static void initializeSystemPropertyDefaults() {
        // Set dangerous class patterns if not explicitly configured
        if (System.getProperty("reflectionutils.dangerous.class.patterns") == null) {
            System.setProperty("reflectionutils.dangerous.class.patterns", DEFAULT_DANGEROUS_CLASS_PATTERNS);
        }
        
        // Set sensitive field patterns if not explicitly configured  
        if (System.getProperty("reflectionutils.sensitive.field.patterns") == null) {
            System.setProperty("reflectionutils.sensitive.field.patterns", DEFAULT_SENSITIVE_FIELD_PATTERNS);
        }
        
        // Set max cache size if not explicitly configured
        if (System.getProperty("reflectionutils.max.cache.size") == null) {
            System.setProperty("reflectionutils.max.cache.size", String.valueOf(DEFAULT_MAX_CACHE_SIZE));
        }
    }
    
    // Security configuration methods
    
    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("reflectionutils.security.enabled", "false"));
    }
    
    private static boolean isDangerousClassValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("reflectionutils.dangerous.class.validation.enabled", "false"));
    }
    
    private static boolean isSensitiveFieldValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("reflectionutils.sensitive.field.validation.enabled", "false"));
    }
    
    private static int getMaxCacheSize() {
        String maxSizeProp = System.getProperty("reflectionutils.max.cache.size");
        if (maxSizeProp != null) {
            try {
                return Math.max(1, Integer.parseInt(maxSizeProp));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return isSecurityEnabled() ? DEFAULT_MAX_CACHE_SIZE : Integer.MAX_VALUE;
    }
    
    private static Set<String> getDangerousClassPatterns() {
        String patterns = System.getProperty("reflectionutils.dangerous.class.patterns", DEFAULT_DANGEROUS_CLASS_PATTERNS);
        return new HashSet<>(Arrays.asList(patterns.split(",")));
    }
    
    private static Set<String> getSensitiveFieldPatterns() {
        String patterns = System.getProperty("reflectionutils.sensitive.field.patterns", DEFAULT_SENSITIVE_FIELD_PATTERNS);
        return new HashSet<>(Arrays.asList(patterns.split(",")));
    }
    
    private static final int CACHE_SIZE = Math.max(1, Math.min(getMaxCacheSize(),
            Integer.getInteger(CACHE_SIZE_PROPERTY, DEFAULT_CACHE_SIZE)));

    // Add a new cache for storing the sorted constructor arrays
    private static final AtomicReference<Map<? super SortedConstructorsCacheKey, Constructor<?>[]>> SORTED_CONSTRUCTORS_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    private static final AtomicReference<Map<? super ConstructorCacheKey, Constructor<?>>> CONSTRUCTOR_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    private static final AtomicReference<Map<? super MethodCacheKey, Method>> METHOD_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    private static final AtomicReference<Map<? super FieldsCacheKey, Collection<Field>>> FIELDS_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    private static final AtomicReference<Map<? super FieldNameCacheKey, Field>> FIELD_NAME_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE * 10)));

    private static final AtomicReference<Map<? super ClassAnnotationCacheKey, Annotation>> CLASS_ANNOTATION_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    private static final AtomicReference<Map<? super MethodAnnotationCacheKey, Annotation>> METHOD_ANNOTATION_CACHE =
            new AtomicReference<>(ensureThreadSafe(new LRUCache<>(CACHE_SIZE)));

    /** Wrap the map if it is not already concurrent. */
    private static <K, V> Map<K, V> ensureThreadSafe(Map<K, V> candidate) {
        if (candidate instanceof ConcurrentMap || candidate instanceof LRUCache) {
            return candidate;                     // already thread-safe
        }
        return new ConcurrentHashMapNullSafe<>(candidate);
    }

    private static <T> void swap(AtomicReference<T> ref, T newValue) {
        Objects.requireNonNull(newValue, "cache must not be null");
        ref.set(newValue);                        // atomic & happens-before
    }

    /**
     * Sets a custom cache implementation for method lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing method lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setMethodCache(Map<Object, Method> cache) {
        swap(METHOD_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for field lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing field lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassFieldsCache(Map<Object, Collection<Field>> cache) {
        swap(FIELDS_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for field lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing field lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setFieldCache(Map<Object, Field> cache) {
        swap(FIELD_NAME_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for class annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing class annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassAnnotationCache(Map<Object, Annotation> cache) {
        swap(CLASS_ANNOTATION_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for method annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing method annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setMethodAnnotationCache(Map<Object, Annotation> cache) {
        swap(METHOD_ANNOTATION_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for constructor lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing constructor lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setConstructorCache(Map<Object, Constructor<?>> cache) {
        swap(CONSTRUCTOR_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Sets a custom cache implementation for sorted constructors lookup.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing constructor lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setSortedConstructorsCache(Map<Object, Constructor<?>[]> cache) {
        swap(SORTED_CONSTRUCTORS_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Securely sets the accessible flag on a reflection object with proper security checks.
     * <p>
     * This method wraps ClassUtilities.trySetAccessible() with additional security validation
     * to prevent unauthorized access control bypass. It verifies that the caller has the
     * necessary permissions before attempting to suppress access checks.
     * </p>
     *
     * @param obj The AccessibleObject (Field, Method, or Constructor) to make accessible
     * @throws SecurityException if the caller lacks suppressAccessChecks permission
     */
    private static void secureSetAccessible(java.lang.reflect.AccessibleObject obj) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
            } catch (SecurityException e) {
                throw new SecurityException("Access denied: Insufficient permissions to bypass access controls for " + obj.getClass().getSimpleName(), e);
            }
        }
        
        // Additional security validation for fields
        if (obj instanceof Field) {
            validateFieldAccess((Field) obj);
        }
        
        ClassUtilities.trySetAccessible(obj);
    }
    
    /**
     * Validates that a field is safe to access via reflection.
     * 
     * @param field the field to validate
     * @throws SecurityException if the field should not be accessible and validation is enabled
     */
    private static void validateFieldAccess(Field field) {
        // Only validate if security features are enabled
        if (!isSecurityEnabled()) {
            return;
        }
        
        Class<?> declaringClass = field.getDeclaringClass();
        String fieldName = field.getName().toLowerCase();
        String className = declaringClass.getName();
        
        // Check if the declaring class is dangerous (if dangerous class validation is enabled)
        if (isDangerousClassValidationEnabled() && isDangerousClass(declaringClass)) {
            LOG.log(Level.WARNING, "Access to field blocked in dangerous class: " + sanitizeClassName(className) + "." + fieldName);
            throw new SecurityException("Access denied: Field access not permitted in security-sensitive class");
        }
        
        // Only apply sensitive field validation if enabled and for non-JDK classes
        if (!isSensitiveFieldValidationEnabled()) {
            return;
        }
        
        // This prevents blocking legitimate JDK internal fields while still protecting user classes
        if (className.startsWith("java.") || className.startsWith("javax.") || 
            className.startsWith("sun.") || className.startsWith("com.sun.")) {
            return; // Allow access to JDK classes
        }
        
        // Allow access to normal fields that start with "normal"
        if (fieldName.startsWith("normal")) {
            return;
        }
        
        // Check if the field name suggests sensitive content (only for user classes)
        Set<String> sensitivePatterns = getSensitiveFieldPatterns();
        for (String pattern : sensitivePatterns) {
            if (fieldName.contains(pattern.trim().toLowerCase())) {
                LOG.log(Level.WARNING, "Access to sensitive field blocked: " + sanitizeClassName(className) + "." + fieldName);
                throw new SecurityException("Access denied: Sensitive field access not permitted");
            }
        }
    }
    
    /**
     * Checks if a class is considered dangerous for reflection operations.
     * 
     * @param clazz the class to check
     * @return true if the class is dangerous and the caller is not trusted, and validation is enabled
     */
    private static boolean isDangerousClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // Only check if security and dangerous class validation are enabled
        if (!isSecurityEnabled() || !isDangerousClassValidationEnabled()) {
            return false;
        }
        
        String className = clazz.getName();
        Set<String> dangerousPatterns = getDangerousClassPatterns();
        
        // Check if class name matches any dangerous patterns
        boolean isDangerous = false;
        for (String pattern : dangerousPatterns) {
            pattern = pattern.trim();
            if (className.equals(pattern)) {
                isDangerous = true;
                break;
            }
        }
        
        if (!isDangerous) {
            return false;
        }
        
        // Allow trusted internal callers (java-util library) to access dangerous classes
        // This is necessary for legitimate functionality like Unsafe usage by ClassUtilities
        if (isTrustedCaller()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the current caller is from a trusted package (java-util library).
     * 
     * @return true if the caller is trusted
     */
    private static boolean isTrustedCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        
        // Look through the call stack for trusted callers
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            
            // Allow calls from java-util library itself
            if (className.startsWith("com.cedarsoftware.util.")) {
                // Skip ReflectionUtils itself to avoid infinite recursion
                if (!className.equals("com.cedarsoftware.util.ReflectionUtils")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Sanitizes class names for safe logging.
     * 
     * @param className the class name to sanitize
     * @return sanitized class name safe for logging
     */
    private static String sanitizeClassName(String className) {
        if (className == null) {
            return "[null]";
        }
        
        if (className.length() <= 10) {
            return "[class:" + className.length() + "-chars]";
        }
        
        return className.substring(0, 5) + "***" + className.substring(className.length() - 5);
    }

    private ReflectionUtils() { }

    private static final class ClassAnnotationCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final Class<? extends Annotation> annotationClass;
        private final int hash;

        ClassAnnotationCacheKey(Class<?> clazz, Class<? extends Annotation> annotationClass) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.annotationClass = Objects.requireNonNull(annotationClass, "annotationClass cannot be null");
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), System.identityHashCode(annotationClass));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassAnnotationCacheKey)) return false;
            ClassAnnotationCacheKey that = (ClassAnnotationCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz && this.annotationClass == that.annotationClass;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class MethodAnnotationCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Method method;
        private final Class<? extends Annotation> annotationClass;
        private final int hash;

        MethodAnnotationCacheKey(Method method, Class<? extends Annotation> annotationClass) {
            this.method = Objects.requireNonNull(method, "method cannot be null");
            this.annotationClass = Objects.requireNonNull(annotationClass, "annotationClass cannot be null");
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(method), System.identityHashCode(annotationClass));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodAnnotationCacheKey)) return false;
            MethodAnnotationCacheKey that = (MethodAnnotationCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.method == that.method && this.annotationClass == that.annotationClass;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class ConstructorCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final Class<?>[] parameterTypes;
        private final int hash;

        ConstructorCacheKey(Class<?> clazz, Class<?>... types) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.parameterTypes = types.clone(); // Defensive copy
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), Arrays.hashCode(parameterTypes));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConstructorCacheKey)) return false;
            ConstructorCacheKey that = (ConstructorCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz && Arrays.equals(this.parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // Add this class definition with the other cache keys
    private static final class SortedConstructorsCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final int hash;

        SortedConstructorsCacheKey(Class<?> clazz) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = System.identityHashCode(clazz);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SortedConstructorsCacheKey)) return false;
            SortedConstructorsCacheKey that = (SortedConstructorsCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class FieldNameCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final String fieldName;
        private final int hash;

        FieldNameCacheKey(Class<?> clazz, String fieldName) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.fieldName = Objects.requireNonNull(fieldName, "fieldName cannot be null");
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), fieldName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldNameCacheKey)) return false;
            FieldNameCacheKey that = (FieldNameCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz && Objects.equals(this.fieldName, that.fieldName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class FieldsCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final Predicate<Field> predicate;
        private final boolean deep;
        private final int hash;

        FieldsCacheKey(Class<?> clazz, Predicate<Field> predicate, boolean deep) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.predicate = Objects.requireNonNull(predicate, "predicate cannot be null");
            this.deep = deep;
            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), deep, System.identityHashCode(predicate));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldsCacheKey)) return false;
            FieldsCacheKey other = (FieldsCacheKey) o;
            return deep == other.deep &&
                    this.clazz == other.clazz && // Use reference equality to prevent spoofing
                    predicate == other.predicate; // Use identity comparison for predicates
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class MethodCacheKey {
        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] parameterTypes;
        private final int hash;

        public MethodCacheKey(Class<?> clazz, String methodName, Class<?>... types) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.methodName = Objects.requireNonNull(methodName, "methodName cannot be null");
            this.parameterTypes = types.clone(); // Defensive copy

            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), methodName, Arrays.hashCode(parameterTypes));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCacheKey)) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz &&
                    Objects.equals(this.methodName, that.methodName) &&
                    Arrays.equals(this.parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final Predicate<Field> DEFAULT_FIELD_FILTER = field -> {
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }

        String fieldName = field.getName();
        Class<?> declaringClass = field.getDeclaringClass();

        if (declaringClass.isEnum() &&
                ("internal".equals(fieldName) || "ENUM$VALUES".equals(fieldName))) {
            return false;
        }

        if ("metaClass".equals(fieldName) &&
                "groovy.lang.MetaClass".equals(field.getType().getName())) {
            return false;
        }

        return !(declaringClass.isAssignableFrom(Enum.class) &&
                (fieldName.equals("hash") || fieldName.equals("ordinal")));
    };

    /**
     * Searches for a specific annotation on a class, examining the entire inheritance hierarchy.
     * Results (including misses) are cached for performance.
     * <p>
     * This method performs an exhaustive search through:
     * <ul>
     *     <li>The class itself</li>
     *     <li>All superclasses</li>
     *     <li>All implemented interfaces</li>
     *     <li>All super-interfaces</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Caches both found annotations and misses (nulls)</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Uses depth-first search through the inheritance hierarchy</li>
     *     <li>Prevents circular reference issues</li>
     *     <li>Returns the first matching annotation found</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * JsonObject anno = ReflectionUtils.getClassAnnotation(MyClass.class, JsonObject.class);
     * if (anno != null) {
     *     // Process annotation...
     * }
     * </pre>
     *
     * @param classToCheck The class to search for the annotation
     * @param annoClass The annotation class to search for
     * @param <T> The type of the annotation
     * @return The annotation if found, null otherwise
     * @throws IllegalArgumentException if either classToCheck or annoClass is null
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        if (classToCheck == null) {
            return null;
        }
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        final ClassAnnotationCacheKey key = new ClassAnnotationCacheKey(classToCheck, annoClass);

        // Use computeIfAbsent to ensure only one instance (or null) is stored per key
        Annotation annotation = CLASS_ANNOTATION_CACHE.get().computeIfAbsent(key, k -> {
            // If findClassAnnotation() returns null, that null will be stored in the cache
            return findClassAnnotation(classToCheck, annoClass);
        });

        // Cast the stored Annotation (or null) back to the desired type
        return (T) annotation;
    }

    private static <T extends Annotation> T findClassAnnotation(Class<?> classToCheck, Class<T> annoClass) {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk)) {
                continue;
            }
            visited.add(classToChk);
            T a = classToChk.getAnnotation(annoClass);
            if (a != null) {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }
        return null;
    }

    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack) {
        for (Class<?> interFace : classToCheck.getInterfaces()) {
            stack.push(interFace);
        }
    }

    /**
     * Searches for a specific annotation on a method, examining the entire inheritance hierarchy.
     * Results (including misses) are cached for performance.
     * <p>
     * This method performs an exhaustive search through:
     * <ul>
     *     <li>The method in the declaring class</li>
     *     <li>Matching methods in all superclasses</li>
     *     <li>Matching methods in all implemented interfaces</li>
     *     <li>Matching methods in all super-interfaces</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Caches both found annotations and misses (nulls)</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Uses depth-first search through the inheritance hierarchy</li>
     *     <li>Matches methods by name and parameter types</li>
     *     <li>Prevents circular reference issues</li>
     *     <li>Returns the first matching annotation found</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Method method = obj.getClass().getMethod("processData", String.class);
     * JsonProperty anno = ReflectionUtils.getMethodAnnotation(method, JsonProperty.class);
     * if (anno != null) {
     *     // Process annotation...
     * }
     * </pre>
     *
     * @param method The method to search for the annotation
     * @param annoClass The annotation class to search for
     * @param <T> The type of the annotation
     * @return The annotation if found, null otherwise
     * @throws IllegalArgumentException if either method or annoClass is null
     */
    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass) {
        Convention.throwIfNull(method, "method cannot be null");
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        final MethodAnnotationCacheKey key = new MethodAnnotationCacheKey(method, annoClass);

        // Atomically retrieve or compute the annotation from the cache
        Annotation annotation = METHOD_ANNOTATION_CACHE.get().computeIfAbsent(key, k -> {
            // Search the class hierarchy
            Class<?> currentClass = method.getDeclaringClass();
            while (currentClass != null) {
                try {
                    Method currentMethod = currentClass.getDeclaredMethod(
                            method.getName(),
                            method.getParameterTypes()
                    );
                    T found = currentMethod.getAnnotation(annoClass);
                    if (found != null) {
                        return found;  // store in cache
                    }
                } catch (Exception ignored) {
                    // Not found in currentClass, go to superclass
                }
                currentClass = currentClass.getSuperclass();
            }

            // Check interfaces
            for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
                try {
                    Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                    T found = ifaceMethod.getAnnotation(annoClass);
                    if (found != null) {
                        return found;  // store in cache
                    }
                } catch (Exception ignored) {
                    // Not found in this interface, move on
                }
            }

            // No annotation found - store null
            return null;
        });

        // Cast result back to T (or null)
        return (T) annotation;
    }

    /**
     * Retrieves a specific field from a class by name, searching through the entire class hierarchy
     * (including superclasses). Results are cached for performance.
     * <p>
     * This method:
     * <ul>
     *     <li>Searches through all fields (public, protected, package, private)</li>
     *     <li>Includes fields from superclasses</li>
     *     <li>Excludes static fields</li>
     *     <li>Makes non-public fields accessible</li>
     *     <li>Caches results (including misses) for performance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Field nameField = ReflectionUtils.getField(Employee.class, "name");
     * if (nameField != null) {
     *     nameField.set(employee, "John");
     * }
     * </pre>
     *
     * @param c The class to search for the field
     * @param fieldName The name of the field to find
     * @return The Field object if found, null if the field doesn't exist
     * @throws IllegalArgumentException if either the class or fieldName is null
     */
    public static Field getField(Class<?> c, String fieldName) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fieldName, "fieldName cannot be null");

        final FieldNameCacheKey key = new FieldNameCacheKey(c, fieldName);

        // Atomically retrieve or compute the field from the cache
        Field field = FIELD_NAME_CACHE.get().computeIfAbsent(key, k -> {
            Collection<Field> fields = getAllDeclaredFields(c);  // returns all fields in c's hierarchy
            for (Field f : fields) {
                if (fieldName.equals(f.getName())) {
                    return f;
                }
            }
            return null;  // no matching field
        });
        
        // Security: Validate field access before returning
        if (field != null) {
            validateFieldAccess(field);
        }
        
        return field;
    }

    /**
     * Retrieves the declared fields of a class (not it's parent) using a custom field filter, with caching for
     * performance. This method provides direct field access with customizable filtering criteria.
     * <p>
     * Key features:
     * <ul>
     *     <li>Custom field filtering through provided Predicate</li>
     *     <li>Returns only fields declared directly on the specified class (not from superclasses)</li>
     *     <li>Caches results for both successful lookups and misses</li>
     *     <li>Makes non-public fields accessible when possible</li>
     *     <li>Returns an unmodifiable List to prevent modification</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *     <li>Thread-safe caching mechanism</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Maintains consistent order of fields</li>
     *     <li>Caches results per class/filter combination</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Get non-static public fields only
     * List<Field> publicFields = getDeclaredFields(MyClass.class,
     *     field -> !Modifier.isStatic(field.getModifiers()) &&
     *              Modifier.isPublic(field.getModifiers()));
     *
     * // Get fields with specific names
     * Set<String> allowedNames = Set.of("id", "name", "value");
     * List<Field> specificFields = getDeclaredFields(MyClass.class,
     *     field -> allowedNames.contains(field.getName()));
     * }</pre>
     *
     * @param c The class whose declared fields are to be retrieved (must not be null)
     * @param fieldFilter Predicate to determine which fields should be included (must not be null)
     * @return An unmodifiable list of fields that match the filter criteria
     * @throws IllegalArgumentException if either the class or fieldFilter is null
     * @see Field
     * @see Predicate
     * @see #getAllDeclaredFields(Class) For retrieving fields from the entire class hierarchy
     */
    public static List<Field> getDeclaredFields(final Class<?> c, final Predicate<Field> fieldFilter) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fieldFilter, "fieldFilter cannot be null");

        final FieldsCacheKey key = new FieldsCacheKey(c, fieldFilter, false);

        // Atomically compute and cache the unmodifiable List<Field> if absent
        Collection<Field> cachedFields = FIELDS_CACHE.get().computeIfAbsent(key, k -> {
            Field[] declared = c.getDeclaredFields();
            List<Field> filteredList = new ArrayList<>(declared.length);

            for (Field field : declared) {
                if (!fieldFilter.test(field)) {
                    continue;
                }
                secureSetAccessible(field);
                filteredList.add(field);
            }

            // Return an unmodifiable List so it cannot be mutated later
            return Collections.unmodifiableList(filteredList);
        });

        // Cast back to List<Field> (we stored an unmodifiable List in the map)
        return (List<Field>) cachedFields;
    }

    /**
     * Retrieves the declared fields of a class (not it's parent) using the default field filter, with caching for
     * performance. This method provides the same functionality as {@link #getDeclaredFields(Class, Predicate)}
     * but uses the default field filter.
     * <p>
     * The default filter excludes:
     * <ul>
     *     <li>Static fields</li>
     *     <li>Internal enum fields ("internal" and "ENUM$VALUES")</li>
     *     <li>Enum base class fields ("hash" and "ordinal")</li>
     *     <li>Groovy's metaClass field</li>
     * </ul>
     * <p>
     *
     * @param c The class whose complete field hierarchy is to be retrieved
     * @return An unmodifiable list of all fields in the class hierarchy that pass the default filter
     * @throws IllegalArgumentException if the class is null
     * @see #getDeclaredFields(Class, Predicate) For retrieving fields with a custom filter
     */
    public static List<Field> getDeclaredFields(final Class<?> c) {
        return getDeclaredFields(c, DEFAULT_FIELD_FILTER);
    }

    /**
     * Retrieves all fields from a class and its complete inheritance hierarchy using a custom field filter.
     * <p>
     * Key features:
     * <ul>
     *     <li>Custom field filtering through provided Predicate</li>
     *     <li>Includes fields from the specified class and all superclasses</li>
     *     <li>Caches results for performance optimization</li>
     *     <li>Makes non-public fields accessible when possible</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *     <li>Thread-safe caching mechanism</li>
     *     <li>Maintains consistent order (subclass fields before superclass fields)</li>
     *     <li>Returns an unmodifiable List to prevent modification</li>
     *     <li>Uses recursive caching strategy for optimal performance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Get all non-transient fields in hierarchy
     * List<Field> persistentFields = getAllDeclaredFields(MyClass.class,
     *     field -> !Modifier.isTransient(field.getModifiers()));
     *
     * // Get all fields matching specific name pattern
     * List<Field> matchingFields = getAllDeclaredFields(MyClass.class,
     *     field -> field.getName().startsWith("customer"));
     * }</pre>
     *
     * @param c The class whose complete field hierarchy is to be retrieved (must not be null)
     * @param fieldFilter Predicate to determine which fields should be included (must not be null)
     * @return An unmodifiable list of all matching fields in the class hierarchy
     * @throws IllegalArgumentException if either the class or fieldFilter is null
     * @see Field
     * @see Predicate
     * @see #getAllDeclaredFields(Class) For retrieving fields using the default filter
     */
    public static List<Field> getAllDeclaredFields(final Class<?> c, final Predicate<Field> fieldFilter) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fieldFilter, "fieldFilter cannot be null");
        
        // Security: Check if the class is dangerous before proceeding
        if (isDangerousClass(c)) {
            LOG.log(Level.WARNING, "Field access blocked for dangerous class: " + sanitizeClassName(c.getName()));
            throw new SecurityException("Access denied: Field access not permitted for security-sensitive class");
        }

        final FieldsCacheKey key = new FieldsCacheKey(c, fieldFilter, true);

        // Atomically compute and cache the unmodifiable list, if not already present
        Collection<Field> cached = FIELDS_CACHE.get().computeIfAbsent(key, k -> {
            // Collect fields from class + superclasses
            List<Field> allFields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && !isDangerousClass(current)) {
                allFields.addAll(getDeclaredFields(current, fieldFilter));
                current = current.getSuperclass();
            }
            // Return an unmodifiable list to prevent further modification
            return Collections.unmodifiableList(allFields);
        });

        // We know we stored a List<Field>, so cast is safe
        return (List<Field>) cached;
    }

    /**
     * Retrieves all fields from a class and its complete inheritance hierarchy using the default field filter.
     * The default filter excludes:
     * <ul>
     *     <li>Static fields</li>
     *     <li>Internal enum fields ("internal" and "ENUM$VALUES")</li>
     *     <li>Enum base class fields ("hash" and "ordinal")</li>
     *     <li>Groovy's metaClass field</li>
     * </ul>
     * <p>
     * This method is equivalent to calling {@link #getAllDeclaredFields(Class, Predicate)} with the default
     * field filter.
     *
     * @param c The class whose complete field hierarchy is to be retrieved
     * @return An unmodifiable list of all fields in the class hierarchy that pass the default filter
     * @throws IllegalArgumentException if the class is null
     * @see #getAllDeclaredFields(Class, Predicate) For retrieving fields with a custom filter
     */
    public static List<Field> getAllDeclaredFields(final Class<?> c) {
        return getAllDeclaredFields(c, DEFAULT_FIELD_FILTER);
    }

    /**
     * Returns all Fields from a class (including inherited) as a Map filtered by the provided predicate.
     * <p>
     * The returned Map uses String field names as keys and Field objects as values, with special
     * handling for name collisions across the inheritance hierarchy.
     * <p>
     * Field name mapping rules:
     * <ul>
     *     <li>Simple field names (e.g., "name") are used when no collision exists</li>
     *     <li>On collision, fully qualified names (e.g., "com.example.Parent.name") are used</li>
     *     <li>Child class fields take precedence for simple name mapping</li>
     *     <li>Parent class fields use fully qualified names when shadowed</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Get all non-transient fields
     * Map<String, Field> persistentFields = getAllDeclaredFieldsMap(
     *     MyClass.class,
     *     field -> !Modifier.isTransient(field.getModifiers())
     * );
     *
     * // Get all fields with specific annotation
     * Map<String, Field> annotatedFields = getAllDeclaredFieldsMap(
     *     MyClass.class,
     *     field -> field.isAnnotationPresent(MyAnnotation.class)
     * );
     * }</pre>
     *
     * @param c Class whose fields are being fetched (must not be null)
     * @param fieldFilter Predicate to determine which fields should be included (must not be null)
     * @return Map of filtered fields, keyed by field name (or fully qualified name on collision)
     * @throws IllegalArgumentException if either the class or fieldFilter is null
     * @see #getAllDeclaredFields(Class, Predicate)
     * @see #getAllDeclaredFieldsMap(Class)
     */
    public static Map<String, Field> getAllDeclaredFieldsMap(Class<?> c, Predicate<Field> fieldFilter) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fieldFilter, "fieldFilter cannot be null");

        Map<String, Field> fieldMap = new LinkedHashMap<>();
        Collection<Field> fields = getAllDeclaredFields(c, fieldFilter);  // Uses FIELDS_CACHE internally

        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldMap.containsKey(fieldName)) {   // Can happen when parent and child class both have private field with same name
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            } else {
                fieldMap.put(fieldName, field);
            }
        }

        return fieldMap;
    }

    /**
     * Returns all Fields from a class (including inherited) as a Map, using the default field filter.
     * This method provides the same functionality as {@link #getAllDeclaredFieldsMap(Class, Predicate)}
     * but uses the default field filter which excludes:
     * <ul>
     *     <li>Static fields</li>
     *     <li>Internal enum fields ("internal" and "ENUM$VALUES")</li>
     *     <li>Enum base class fields ("hash" and "ordinal")</li>
     *     <li>Groovy's metaClass field</li>
     * </ul>
     *
     * @param c Class whose fields are being fetched
     * @return Map of filtered fields, keyed by field name (or fully qualified name on collision)
     * @throws IllegalArgumentException if the class is null
     * @see #getAllDeclaredFieldsMap(Class, Predicate)
     */
    public static Map<String, Field> getAllDeclaredFieldsMap(Class<?> c) {
        return getAllDeclaredFieldsMap(c, DEFAULT_FIELD_FILTER);
    }

    /**
     * @deprecated As of 3.0.0, replaced by {@link #getAllDeclaredFields(Class)}.
     * Note that getAllDeclaredFields() includes transient fields and synthetic fields
     * (like "this$"). If you need the old behavior, filter the additional fields:
     * <pre>{@code
     * // Get fields excluding transient and synthetic fields
     * Map<String, Field> fields = getAllDeclaredFields(MyClass.class, field ->
     *     DEFAULT_FIELD_FILTER.test(field) &&
     *     !Modifier.isTransient(field.getModifiers()) &&
     *     !field.isSynthetic()
     * );
     * }</pre>
     * This method may be removed in 3.0.0.
     */
    @Deprecated
    public static Collection<Field> getDeepDeclaredFields(Class<?> c) {
        Convention.throwIfNull(c, "Class cannot be null");

        // Combine DEFAULT_FIELD_FILTER with additional criteria for legacy behavior
        Predicate<Field> legacyFilter = field ->
                DEFAULT_FIELD_FILTER.test(field) &&
                        !Modifier.isTransient(field.getModifiers()) &&
                        !field.isSynthetic();

        // Use the getAllDeclaredFields with the combined filter
        return getAllDeclaredFields(c, legacyFilter);
    }

    /**
     * @deprecated As of 3.0.0, replaced by {@link #getAllDeclaredFieldsMap(Class)}.
     * Note that getAllDeclaredFieldsMap() includes transient fields and synthetic fields
     * (like "this$"). If you need the old behavior, filter the additional fields:
     * <pre>{@code
     * // Get fields excluding transient and synthetic fields
     * List<Field> fields = getAllDeclaredFieldsMap(MyClass.class, field ->
     *     DEFAULT_FIELD_FILTER.test(field) &&
     *     !Modifier.isTransient(field.getModifiers()) &&
     *     !field.isSynthetic()
     * );
     * }</pre>
     * This method may be removed in 3.0.0.
     */
    @Deprecated
    public static Map<String, Field> getDeepDeclaredFieldMap(Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");

        // Combine DEFAULT_FIELD_FILTER with additional criteria for legacy behavior
        Predicate<Field> legacyFilter = field ->
                DEFAULT_FIELD_FILTER.test(field) &&
                        !Modifier.isTransient(field.getModifiers()) &&
                        !field.isSynthetic();

        return getAllDeclaredFieldsMap(c, legacyFilter);
    }

    /**
     * @deprecated As of 3.0.0, replaced by {@link #getAllDeclaredFields(Class)}.
     * Note that getAllDeclaredFields() includes transient fields and synthetic fields
     * (like "this$"). If you need the old behavior, filter the additional fields:
     * <pre>{@code
            // Combine DEFAULT_FIELD_FILTER with additional criteria for legacy behavior
            Predicate<Field> legacyFilter = field ->
            DEFAULT_FIELD_FILTER.test(field) &&
            !Modifier.isTransient(field.getModifiers()) &&
            !field.isSynthetic();
     * }</pre>
     * This method will be removed in 3.0.0 or soon after.
     */
    @Deprecated
    public static void getDeclaredFields(Class<?> c, Collection<Field> fields) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fields, "fields collection cannot be null");

        try {
            // Combine DEFAULT_FIELD_FILTER with additional criteria for legacy behavior
            Predicate<Field> legacyFilter = field ->
                    DEFAULT_FIELD_FILTER.test(field) &&
                            !Modifier.isTransient(field.getModifiers()) &&
                            !field.isSynthetic();

            // Get filtered fields and add them to the provided collection
            List<Field> filteredFields = getDeclaredFields(c, legacyFilter);
            fields.addAll(filteredFields);
        } catch (Throwable t) {
            ExceptionUtilities.safelyIgnoreException(t);
        }
    }

    /**
     * Simplifies reflective method invocation by wrapping checked exceptions into runtime exceptions.
     * This method provides a cleaner API for reflection-based method calls.
     * <p>
     * Key features:
     * <ul>
     *     <li>Converts checked exceptions to runtime exceptions</li>
     *     <li>Preserves the original exception cause</li>
     *     <li>Provides clear error messages</li>
     *     <li>Handles null checking for both method and instance</li>
     * </ul>
     * <p>
     * Exception handling:
     * <ul>
     *     <li>IllegalAccessException → RuntimeException</li>
     *     <li>InvocationTargetException → RuntimeException (with target exception)</li>
     *     <li>Null method → IllegalArgumentException</li>
     *     <li>Null instance → IllegalArgumentException</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Method method = ReflectionUtils.getMethod(obj.getClass(), "processData", String.class);
     * Object result = ReflectionUtils.call(obj, method, "input data");
     *
     * // No need for try-catch blocks for checked exceptions
     * // Just handle RuntimeException if needed
     * </pre>
     *
     * @param instance The object instance on which to call the method
     * @param method The Method object representing the method to call
     * @param args The arguments to pass to the method (may be empty)
     * @return The result of the method invocation, or null for void methods
     * @throws IllegalArgumentException if either method or instance is null
     * @throws RuntimeException if the method is inaccessible or throws an exception
     * @see Method#invoke(Object, Object...) For the underlying reflection mechanism
     */
    public static Object call(Object instance, Method method, Object... args) {
        if (method == null) {
            String className = (instance == null) ? "null instance" : instance.getClass().getName();
            throw new IllegalArgumentException("null Method passed to ReflectionUtils.call() on instance of type: " + className);
        }
        if (instance == null) {
            throw new IllegalArgumentException("Cannot call [" + method.getName() + "()] on a null object.");
        }
        
        // Security check: Verify permission for reflection access
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
            } catch (SecurityException e) {
                throw new SecurityException("Access denied: ReflectionUtils.call() requires suppressAccessChecks permission for method: " + method.getName(), e);
            }
        }
        
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // never executed
        }
    }

    /**
     * Provides a simplified, cached reflection API for method invocation using method name.
     * This method combines method lookup and invocation in one step, with results cached
     * for performance.
     * <p>
     * Key features:
     * <ul>
     *     <li>Caches method lookups for improved performance</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Converts checked exceptions to runtime exceptions</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Limitations:
     * <ul>
     *     <li>Does not distinguish between overloaded methods with same parameter count</li>
     *     <li>Only matches by method name and parameter count</li>
     *     <li>Always selects the first matching method found</li>
     *     <li>Only finds public methods</li>
     * </ul>
     * <p>
     * Exception handling:
     * <ul>
     *     <li>Method not found → IllegalArgumentException</li>
     *     <li>IllegalAccessException → RuntimeException</li>
     *     <li>InvocationTargetException → RuntimeException (with target exception)</li>
     *     <li>Null instance/methodName → IllegalArgumentException</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * // Simple case - no method overloading
     * Object result = ReflectionUtils.call(myObject, "processData", "input");
     *
     * // For overloaded methods, use the more specific call() method:
     * Method specific = ReflectionUtils.getMethod(myObject.getClass(), "processData", String.class);
     * Object result = ReflectionUtils.call(myObject, specific, "input");
     * </pre>
     *
     * @param instance The object instance on which to call the method
     * @param methodName The name of the method to call
     * @param args The arguments to pass to the method (may be empty)
     * @return The result of the method invocation, or null for void methods
     * @throws IllegalArgumentException if the method cannot be found, or if instance/methodName is null
     * @throws RuntimeException if the method is inaccessible or throws an exception
     * @see #call(Object, Method, Object...) For handling overloaded methods
     * @see #getMethod(Class, String, Class...) For explicit method lookup with parameter types
     */
    public static Object call(Object instance, String methodName, Object... args) {
        // Security check: Verify permission for reflection access
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
            } catch (SecurityException e) {
                throw new SecurityException("Access denied: ReflectionUtils.call() requires suppressAccessChecks permission for method: " + methodName, e);
            }
        }
        
        Method method = getMethod(instance, methodName, args.length);
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // never executed
        }
    }

    /**
     * Retrieves a method of any access level by name and parameter types, with sophisticated
     * caching for optimal performance. This method searches through the class hierarchy and
     * attempts to make non-public methods accessible.
     * <p>
     * Key features:
     * <ul>
     *     <li>Finds methods of any access level (public, protected, package, private)</li>
     *     <li>Includes bridge methods (compiler-generated for generic type erasure)</li>
     *     <li>Includes synthetic methods (compiler-generated for lambdas, inner classes)</li>
     *     <li>Attempts to make non-public methods accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Searches entire inheritance hierarchy</li>
     * </ul>
     *
     * @param c The class to search for the method
     * @param methodName The name of the method to find
     * @param types The parameter types for the method (empty array for no-arg methods)
     * @return The Method object if found and made accessible, null if not found
     * @throws IllegalArgumentException if class or methodName is null
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>... types) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(methodName, "methodName cannot be null");
        
        // Security: Check if the class is dangerous before proceeding
        if (isDangerousClass(c)) {
            LOG.log(Level.WARNING, "Method access blocked for dangerous class: " + sanitizeClassName(c.getName()) + "." + methodName);
            throw new SecurityException("Access denied: Method access not permitted for security-sensitive class");
        }

        final MethodCacheKey key = new MethodCacheKey(c, methodName, types);

        // Atomically retrieve (or compute) the method
        return METHOD_CACHE.get().computeIfAbsent(key, k -> {
            Method method = null;
            Class<?> current = c;

            while (current != null && method == null) {
                try {
                    method = current.getDeclaredMethod(methodName, types);
                    secureSetAccessible(method);
                } catch (Exception ignored) {
                    // Move on up the superclass chain
                }
                current = current.getSuperclass();
            }
            // Will be null if not found
            return method;
        });
    }

    /**
     * Retrieves a method by name and argument count from an object instance (or Class), using a
     * deterministic selection strategy when multiple matching methods exist.
     * <p>
     * Key features:
     * <ul>
     *     <li>Finds methods of any access level (public, protected, package, private)</li>
     *     <li>Uses deterministic method selection strategy</li>
     *     <li>Attempts to make non-public methods accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Searches entire inheritance hierarchy</li>
     * </ul>
     * <p>
     * Method selection priority (when multiple methods match):
     * <ul>
     *     <li>1. Non-synthetic/non-bridge methods preferred</li>
     *     <li>2. Higher accessibility preferred (public > protected > package > private)</li>
     *     <li>3. Most specific declaring class in hierarchy preferred</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * // Will select most accessible, non-synthetic method with two parameters
     * Method method = ReflectionUtils.getMethod(myObject, "processData", 2);
     *
     * // For exact method selection, use getMethod with specific types:
     * Method specific = ReflectionUtils.getMethod(
     *     myObject.getClass(),
     *     "processData",
     *     String.class, Integer.class
     * );
     * </pre>
     *
     * @param instance The object instance on which to find the method (can also be a Class)
     * @param methodName The name of the method to find
     * @param argCount The number of parameters the method should have
     * @return The Method object, made accessible if necessary
     * @throws IllegalArgumentException if the method is not found or if bean/methodName is null
     * @see #getMethod(Class, String, Class...) For finding methods with specific parameter types
     */
    public static Method getMethod(Object instance, String methodName, int argCount) {
        Convention.throwIfNull(instance, "Object instance cannot be null");
        Convention.throwIfNull(methodName, "Method name cannot be null");
        if (argCount < 0) {
            throw new IllegalArgumentException("Argument count cannot be negative");
        }

        Class<?> beanClass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();

        Class<?>[] types = new Class<?>[argCount];
        Arrays.fill(types, Object.class);
        MethodCacheKey key = new MethodCacheKey(beanClass, methodName, types);

        // Check cache first
        Method cached = METHOD_CACHE.get().get(key);
        if (cached != null || METHOD_CACHE.get().containsKey(key)) {
            return cached;
        }

        // Collect all matching methods
        List<Method> candidates = new ArrayList<>();
        Class<?> current = beanClass;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                    candidates.add(method);
                }
            }
            current = current.getSuperclass();
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' with %d parameters not found in %s or its superclasses",
                            methodName, argCount, beanClass.getName())
            );
        }

        // Select the best matching method using our composite strategy
        Method selected = selectMethod(candidates);

        // Attempt to make the method accessible
        secureSetAccessible(selected);

        // Cache the result
        METHOD_CACHE.get().put(key, selected);
        return selected;
    }

    /**
     * Selects the most appropriate method using a composite selection strategy.
     * Selection criteria are applied in order of priority.
     */
    private static Method selectMethod(List<Method> candidates) {
        return candidates.stream()
                .min((m1, m2) -> {
                    // First, prefer non-synthetic/non-bridge methods
                    if (m1.isSynthetic() != m2.isSynthetic()) {
                        return m1.isSynthetic() ? 1 : -1;
                    }
                    if (m1.isBridge() != m2.isBridge()) {
                        return m1.isBridge() ? 1 : -1;
                    }

                    // Then, prefer more accessible methods
                    int accessDiff = getAccessibilityScore(m2.getModifiers()) -
                            getAccessibilityScore(m1.getModifiers());
                    if (accessDiff != 0) return accessDiff;

                    // Finally, prefer methods declared in most specific class
                    if (m1.getDeclaringClass().isAssignableFrom(m2.getDeclaringClass())) return 1;
                    if (m2.getDeclaringClass().isAssignableFrom(m1.getDeclaringClass())) return -1;

                    return 0;
                })
                .orElse(candidates.get(0));
    }

    /**
     * Returns an accessibility score for method modifiers.
     * Higher scores indicate greater accessibility.
     */
    private static int getAccessibilityScore(int modifiers) {
        if (Modifier.isPublic(modifiers)) return 4;
        if (Modifier.isProtected(modifiers)) return 3;
        if (Modifier.isPrivate(modifiers)) return 1;
        return 2; // package-private
    }

    /**
     * Retrieves a constructor for the given class and parameter types.
     * Uses a cache to speed up repeated lookups.
     *
     * @param clazz The class for which to get the constructor.
     * @param parameterTypes The parameter types of the constructor.
     * @param <T> The type of the class.
     * @return The constructor, or null if not found or not accessible.
     */
    @SuppressWarnings("unchecked") // For the cast from cached Constructor<?> to Constructor<T>
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        Convention.throwIfNull(clazz, "class cannot be null");
        
        // Security: Check if the class is dangerous before proceeding
        if (isDangerousClass(clazz)) {
            LOG.log(Level.WARNING, "Constructor access blocked for dangerous class: " + sanitizeClassName(clazz.getName()));
            throw new SecurityException("Access denied: Constructor access not permitted for security-sensitive class");
        }

        final ConstructorCacheKey key = new ConstructorCacheKey(clazz, parameterTypes);

        // Atomically retrieve or compute the cached constructor
        // The mapping function returns Constructor<T>, which is compatible with Constructor<?> for storage.
        // The final return then casts the Constructor<?> from the cache to Constructor<T>.
        // This cast is safe because the key ensures we're getting the constructor for Class<T>.
        Constructor<?> cachedCtor = CONSTRUCTOR_CACHE.get().computeIfAbsent(key, k -> {
            try {
                // Try to fetch the constructor reflectively
                Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes); // This already returns Constructor<T>
                secureSetAccessible(ctor); // Secure method with proper security checks
                return ctor;
            } catch (NoSuchMethodException ignored) { // Be more specific with exceptions
                // If no such constructor exists, store null in the cache
                return null;
            } catch (SecurityException ignored) {
                // If security manager denies access
                return null;
            }
        });
        return (Constructor<T>) cachedCtor; // This cast is necessary and what @SuppressWarnings("unchecked") is for
    }

    /**
     * Returns all constructors for a class, ordered optimally for instantiation.
     * Constructors are ordered by accessibility (public, protected, package, private)
     * and within each level by parameter count (most specific first).
     *
     * @param clazz The class to get constructors for
     * @return Array of constructors in optimal order
     */
    public static Constructor<?>[] getAllConstructors(Class<?> clazz) {
        if (clazz == null) {
            return new Constructor<?>[0];
        }
        
        // Security: Check if the class is dangerous before proceeding
        if (isDangerousClass(clazz)) {
            LOG.log(Level.WARNING, "Constructor enumeration blocked for dangerous class: " + sanitizeClassName(clazz.getName()));
            throw new SecurityException("Access denied: Constructor enumeration not permitted for security-sensitive class");
        }

        // Create proper cache key with classloader information
        SortedConstructorsCacheKey key = new SortedConstructorsCacheKey(clazz);

        // Use the cache to avoid repeated sorting
        return SORTED_CONSTRUCTORS_CACHE.get().computeIfAbsent(key,
                k -> getAllConstructorsInternal(clazz));
    }

    /**
     * Worker method that retrieves and sorts constructors.
     * This method ensures all constructors are accessible and cached individually.
     */
    private static Constructor<?>[] getAllConstructorsInternal(Class<?> clazz) {
        // Get the declared constructors
        Constructor<?>[] declared = clazz.getDeclaredConstructors();
        if (declared.length == 0) {
            return declared;
        }

        // Cache each constructor individually and ensure they're accessible
        for (int i = 0; i < declared.length; i++) {
            final Constructor<?> ctor = declared[i];
            Class<?>[] paramTypes = ctor.getParameterTypes();
            ConstructorCacheKey key = new ConstructorCacheKey(clazz, paramTypes);

            // Retrieve from cache or add to cache
            declared[i] = CONSTRUCTOR_CACHE.get().computeIfAbsent(key, k -> {
                secureSetAccessible(ctor);
                return ctor;
            });
        }

        // Create a sorted copy of the constructors
        Constructor<?>[] result = new Constructor<?>[declared.length];
        System.arraycopy(declared, 0, result, 0, declared.length);

        // Sort the constructors in optimal order if there's more than one
        if (result.length > 1) {
            boolean isFinal = Modifier.isFinal(clazz.getModifiers());
            boolean isException = Throwable.class.isAssignableFrom(clazz);

            Arrays.sort(result, (c1, c2) -> {
                // First, sort by accessibility (public > protected > package > private)
                int mod1 = c1.getModifiers();
                int mod2 = c2.getModifiers();

                boolean isPublic1 = Modifier.isPublic(mod1);
                boolean isPublic2 = Modifier.isPublic(mod2);
                boolean isProtected1 = Modifier.isProtected(mod1);
                boolean isProtected2 = Modifier.isProtected(mod2);
                boolean isPrivate1 = Modifier.isPrivate(mod1);
                boolean isPrivate2 = Modifier.isPrivate(mod2);

                // Compare accessibility levels
                if (isPublic1 != isPublic2) {
                    return isPublic1 ? -1 : 1;  // public first
                }
                if (isProtected1 != isProtected2) {
                    return isProtected1 ? -1 : 1;  // protected before package/private
                }
                if (isPrivate1 != isPrivate2) {
                    return isPrivate1 ? 1 : -1;  // private last
                }

                // Within same accessibility level, sort by parameter count
                int paramDiff = c1.getParameterCount() - c2.getParameterCount();

                // For exceptions/final classes: prefer more parameters
                // For regular classes: also prefer more parameters (more specific first)
                if (isFinal || isException) {
                    return -paramDiff;  // More parameters first
                } else {
                    return -paramDiff;  // More parameters first (more specific)
                }
            });
        }

        return result;
    }

    private static String makeParamKey(Class<?>... parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(32);
        builder.append(':');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append('|');
            }
            Class<?> param = parameterTypes[i];
            builder.append(param.getName());
        }
        return builder.toString();
    }

    /**
     * Fetches a no-argument method from the specified class, caching the result for subsequent lookups.
     * This is intended for methods that are not overloaded and require no arguments
     * (e.g., simple getter methods).
     * <p>
     * If the class contains multiple methods with the same name, an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param clazz      the class that contains the desired method
     * @param methodName the name of the no-argument method to locate
     * @return the {@code Method} instance found on the given class
     * @throws IllegalArgumentException if the method is not found or if multiple
     *                                  methods with the same name exist
     */
    public static Method getNonOverloadedMethod(Class<?> clazz, String methodName) {
        if (clazz == null) {
            throw new IllegalArgumentException("Attempted to call getMethod() [" + methodName + "()] on a null class.");
        }
        if (StringUtilities.isEmpty(methodName)) {
            throw new IllegalArgumentException("Attempted to call getMethod() with a null or blank method name on class: " + clazz.getName());
        }

        // Create a cache key for a method with no parameters
        MethodCacheKey key = new MethodCacheKey(clazz, methodName);

        return METHOD_CACHE.get().computeIfAbsent(key, k -> {
            Method foundMethod = null;
            for (Method m : clazz.getMethods()) {
                if (methodName.equals(m.getName())) {
                    if (foundMethod != null) {
                        throw new IllegalArgumentException("Method: " + methodName + "() called on a class with overloaded methods "
                                + "- ambiguous as to which one to return. Use getMethod() with argument types or argument count.");
                    }
                    foundMethod = m;
                }
            }

            if (foundMethod == null) {
                throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + clazz.getName()
                        + ". Perhaps the method is protected, private, or misspelled?");
            }

            return foundMethod;
        });
    }

    /**
     * Return the name of the class on the object, or "null" if the object is null.
     * @param o Object to get the class name.
     * @return String name of the class or "null"
     */
    public static String getClassName(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    // Constant pool tags
    private final static int CONSTANT_UTF8 = 1;
    private final static int CONSTANT_INTEGER = 3;
    private final static int CONSTANT_FLOAT = 4;
    private final static int CONSTANT_LONG = 5;
    private final static int CONSTANT_DOUBLE = 6;
    private final static int CONSTANT_CLASS = 7;
    private final static int CONSTANT_STRING = 8;
    private final static int CONSTANT_FIELDREF = 9;
    private final static int CONSTANT_METHODREF = 10;
    private final static int CONSTANT_INTERFACEMETHODREF = 11;
    private final static int CONSTANT_NAMEANDTYPE = 12;
    private final static int CONSTANT_METHODHANDLE = 15;
    private final static int CONSTANT_METHODTYPE = 16;
    private final static int CONSTANT_DYNAMIC = 17;
    private final static int CONSTANT_INVOKEDYNAMIC = 18;
    private final static int CONSTANT_MODULE = 19;
    private final static int CONSTANT_PACKAGE = 20;

    /**
     * Given a byte[] of a Java .class file (compiled Java), this code will retrieve the class name from those bytes.
     * This method supports class files up to the latest JDK version.
     *
     * @param byteCode byte[] of compiled byte code
     * @return String fully qualified class name
     * @throws IOException if there are problems reading the byte code (thrown as unchecked)
     * @throws IllegalStateException if the class file format is not recognized
     */
    public static String getClassNameFromByteCode(byte[] byteCode) {
        try (InputStream is = new ByteArrayInputStream(byteCode);
             DataInputStream dis = new DataInputStream(is)) {

            dis.readInt(); // magic number
            dis.readShort(); // minor version
            dis.readShort(); // major version
            int cpcnt = (dis.readShort() & 0xffff) - 1;
            int[] classes = new int[cpcnt];
            String[] strings = new String[cpcnt];
            int t;

            for (int i = 0; i < cpcnt; i++) {
                t = dis.read(); // tag - 1 byte

                switch (t) {
                    case CONSTANT_UTF8:
                        strings[i] = dis.readUTF();
                        break;

                    case CONSTANT_INTEGER:
                    case CONSTANT_FLOAT:
                        dis.readInt(); // bytes
                        break;

                    case CONSTANT_LONG:
                    case CONSTANT_DOUBLE:
                        dis.readInt(); // high_bytes
                        dis.readInt(); // low_bytes
                        i++; // All 8-byte constants take up two entries
                        break;

                    case CONSTANT_CLASS:
                        classes[i] = dis.readShort() & 0xffff;
                        break;

                    case CONSTANT_STRING:
                        dis.readShort(); // string_index
                        break;

                    case CONSTANT_FIELDREF:
                    case CONSTANT_METHODREF:
                    case CONSTANT_INTERFACEMETHODREF:
                        dis.readShort(); // class_index
                        dis.readShort(); // name_and_type_index
                        break;

                    case CONSTANT_NAMEANDTYPE:
                        dis.readShort(); // name_index
                        dis.readShort(); // descriptor_index
                        break;

                    case CONSTANT_METHODHANDLE:
                        dis.readByte(); // reference_kind
                        dis.readShort(); // reference_index
                        break;

                    case CONSTANT_METHODTYPE:
                        dis.readShort(); // descriptor_index
                        break;

                    case CONSTANT_DYNAMIC:
                    case CONSTANT_INVOKEDYNAMIC:
                        dis.readShort(); // bootstrap_method_attr_index
                        dis.readShort(); // name_and_type_index
                        break;

                    case CONSTANT_MODULE:
                    case CONSTANT_PACKAGE:
                        dis.readShort(); // name_index
                        break;

                    default:
                        throw new IllegalStateException("Unrecognized constant pool tag: " + t);
                }
            }

            dis.readShort(); // access flags
            int thisClassIndex = dis.readShort() & 0xffff; // this_class
            int stringIndex = classes[thisClassIndex - 1];
            String className = strings[stringIndex - 1];
            return className.replace('/', '.');
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }
    }

    /**
     * Returns true if the JavaCompiler (JDK) is available at runtime, false if running under a JRE.
     */
    public static boolean isJavaCompilerAvailable() {
        // Allow tests to simulate running on a JRE by setting a system property.
        if (Boolean.getBoolean("java.util.force.jre")) {
            return false;
        }

        try {
            Class<?> toolProvider = Class.forName("javax.tools.ToolProvider");
            Object compiler = toolProvider.getMethod("getSystemJavaCompiler").invoke(null);
            return compiler != null;
        } catch (Throwable t) {
            return false;
        }
    }
    
    /**
     * Return a String representation of the class loader, or "bootstrap" if null.
     *
     * @param c The class whose class loader is to be identified.
     * @return A String representing the class loader.
     */
    private static String getClassLoaderName(Class<?> c) {
        ClassLoader loader = c.getClassLoader();
        if (loader == null) {
            return "bootstrap";
        }
        // Example: "org.example.MyLoader@1a2b3c4"
        return loader.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(loader));
    }
    
    // Record support (Java 14+) - using reflection to maintain Java 8 compatibility
    private static Method isRecordMethod = null;
    private static Method getRecordComponentsMethod = null;
    private static boolean recordSupportChecked = false;
    
    /**
     * Check if a class is a Record (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     * 
     * @param clazz The class to check
     * @return true if the class is a Record, false otherwise or if running on Java &lt; 14
     */
    public static boolean isRecord(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        if (!recordSupportChecked) {
            try {
                isRecordMethod = Class.class.getMethod("isRecord");
                recordSupportChecked = true;
            } catch (NoSuchMethodException e) {
                // Running on Java < 14
                recordSupportChecked = true;
                return false;
            }
        }
        
        if (isRecordMethod == null) {
            return false;
        }
        
        try {
            return (Boolean) isRecordMethod.invoke(clazz);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the record components of a Record class (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     * 
     * @param clazz The Record class
     * @return Array of RecordComponent objects, or null if not a Record or running on Java &lt; 14
     */
    public static Object[] getRecordComponents(Class<?> clazz) {
        if (!isRecord(clazz)) {
            return null;
        }
        
        if (getRecordComponentsMethod == null) {
            try {
                getRecordComponentsMethod = Class.class.getMethod("getRecordComponents");
            } catch (NoSuchMethodException e) {
                // Running on Java < 14
                return null;
            }
        }
        
        try {
            return (Object[]) getRecordComponentsMethod.invoke(clazz);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the name of a RecordComponent (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     * 
     * @param recordComponent The RecordComponent object
     * @return The name of the component, or null if error
     */
    public static String getRecordComponentName(Object recordComponent) {
        if (recordComponent == null) {
            return null;
        }
        
        try {
            Method getNameMethod = recordComponent.getClass().getMethod("getName");
            return (String) getNameMethod.invoke(recordComponent);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the value of a RecordComponent from a Record instance (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     * 
     * @param recordComponent The RecordComponent object
     * @param recordInstance The Record instance
     * @return The value of the component in the instance
     */
    public static Object getRecordComponentValue(Object recordComponent, Object recordInstance) {
        if (recordComponent == null || recordInstance == null) {
            return null;
        }
        
        try {
            // RecordComponent has an accessor() method that returns the Method to get the value
            Method getAccessorMethod = recordComponent.getClass().getMethod("getAccessor");
            Method accessor = (Method) getAccessorMethod.invoke(recordComponent);
            return accessor.invoke(recordInstance);
        } catch (Exception e) {
            return null;
        }
    }
}
