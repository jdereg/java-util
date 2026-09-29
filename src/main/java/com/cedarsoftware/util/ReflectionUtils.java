package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 *   <li><code>reflectionutils.dangerous.class.patterns=java.lang.Runtime,java.lang.Process,...</code> &mdash; Comma-separated dangerous class patterns</li>
 *   <li><code>reflectionutils.sensitive.field.patterns=password,secret,apikey,...</code> &mdash; Comma-separated sensitive field patterns</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>Dangerous Class Protection:</b> Prevents reflection access to system classes that could enable privilege escalation</li>
 *   <li><b>Sensitive Field Protection:</b> Blocks access to fields containing sensitive information (passwords, tokens, etc.)</li>
 *   <li><b>Trusted Caller Validation:</b> Allows java-util library internal access while blocking external callers</li>
 * </ul>
 *
 * <h2>Caching</h2>
 * <p>What ReflectionUtils finds out about a class is kept on that class, in a {@link ClassValue}: worked out
 * once, then read by every thread without locking or writing. It is bounded by the class itself (its members),
 * nothing is evicted, and a name the class does not have is simply not found, storing nothing. It never keeps a
 * class, or its ClassLoader, alive: it goes away with the class.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Enable security with custom settings
 * System.setProperty("reflectionutils.security.enabled", "true");
 * System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
 * System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");
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
    private static final Logger LOG = Logger.getLogger(ReflectionUtils.class.getName());
    
    // Default dangerous class patterns (moved to system properties in static initializer)
    private static final String DEFAULT_DANGEROUS_CLASS_PATTERNS = 
        "java.lang.Runtime,java.lang.Process,java.lang.ProcessBuilder,sun.misc.Unsafe,jdk.internal.misc.Unsafe,javax.script.ScriptEngine,javax.script.ScriptEngineManager";
    
    // Default sensitive field patterns (moved to system properties in static initializer)
    private static final String DEFAULT_SENSITIVE_FIELD_PATTERNS = 
        "password,passwd,secret,secretkey,apikey,api_key,authtoken,accesstoken,credential,confidential,adminkey,private";
    
    // Removed static initializer that was mutating System properties
    // Properties are now only read, not set
    
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

    // Single volatile reference pairs a property string with its parsed pattern set,
    // ensuring atomic visibility of both values (no TOCTOU race).
    private static volatile Map.Entry<String, Set<String>> cachedDangerousClass = null;
    private static volatile Map.Entry<String, Set<String>> cachedSensitiveField = null;

    private static Set<String> getDangerousClassPatterns() {
        String currentProperty = System.getProperty("reflectionutils.dangerous.class.patterns", DEFAULT_DANGEROUS_CLASS_PATTERNS);
        Map.Entry<String, Set<String>> holder = cachedDangerousClass;
        if (holder != null && currentProperty.equals(holder.getKey())) {
            return holder.getValue();
        }
        Set<String> set = new HashSet<>();
        for (String pattern : currentProperty.split(",")) {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        Set<String> patterns = Collections.unmodifiableSet(set);
        cachedDangerousClass = new AbstractMap.SimpleImmutableEntry<>(currentProperty, patterns);
        return patterns;
    }

    private static Set<String> getSensitiveFieldPatterns() {
        String currentProperty = System.getProperty("reflectionutils.sensitive.field.patterns", DEFAULT_SENSITIVE_FIELD_PATTERNS);
        Map.Entry<String, Set<String>> holder = cachedSensitiveField;
        if (holder != null && currentProperty.equals(holder.getKey())) {
            return holder.getValue();
        }
        Set<String> set = new HashSet<>();
        for (String pattern : currentProperty.split(",")) {
            String processed = pattern.trim().toLowerCase();
            if (!processed.isEmpty()) {
                set.add(processed);
            }
        }
        Set<String> patterns = Collections.unmodifiableSet(set);
        cachedSensitiveField = new AbstractMap.SimpleImmutableEntry<>(currentProperty, patterns);
        return patterns;
    }
    
    // ---------------------------------------------------------------------------------------------
    // What ReflectionUtils finds out about a class never changes while the class is loaded, so it is kept on the class
    // itself, in a ClassValue: worked out on first use, then read by every thread without locking or writing anything.
    // (The LRU caches this replaces wrote on every hit -- a timestamp or a lock -- so threads reading the same entry
    // fought over it: on JDK 21 a call on each of 8 threads took 4-14x as long as on one.) Nothing is evicted, and
    // nothing needs to be: a class has a fixed set of members, and a name it does not have is simply not found, so
    // misses store nothing and a caller asking for any number of made-up names adds nothing. A class's entries go away
    // with the class, so a class from a discarded loader (redeployed webapp, plugin, recompiled script) is never kept
    // alive.
    //
    // Values are JDK types only (arrays, JDK collections and references, Field / Method / Constructor /
    // Annotation). A ClassValue's value lives inside its Class: a java-util type stored in a JDK class
    // would keep java-util's own class loader (a webapp's, say) alive for the life of the JVM.
    // ---------------------------------------------------------------------------------------------

    private static <V> ClassValue<V> perClass(Function<Class<?>, V> compute) {
        return new ClassValue<V>() {
            @Override
            protected V computeValue(Class<?> type) {
                return compute.apply(type);
            }
        };
    }

    /** Each class's declared fields, fetched once: every field list below holds these same Field instances. */
    private static final ClassValue<Field[]> DECLARED_FIELDS = perClass(Class::getDeclaredFields);

    /** getDeclaredFields(c) and getAllDeclaredFields(c): the DEFAULT_FIELD_FILTER lists. */
    private static final ClassValue<List<Field>> DECLARED_FIELDS_DEFAULT =
            perClass(c -> filterDeclaredFields(c, ReflectionUtils.DEFAULT_FIELD_FILTER));
    private static final ClassValue<List<Field>> ALL_FIELDS_DEFAULT =
            perClass(c -> collectAllDeclaredFields(c, ReflectionUtils.DEFAULT_FIELD_FILTER));

    /** getField(): each name's first field in getAllDeclaredFields(c) order. */
    private static final ClassValue<Map<String, Field>> FIELDS_BY_NAME = perClass(ReflectionUtils::fieldsByName);

    /**
     * A caller's own field filter: the lists for the last {@value #FILTER_SLOTS} filters used on the class, newest
     * first, as {WeakReference(filter), List} pairs. The filter is held weakly, so a class never keeps a caller's
     * filter -- or its class loader -- alive. A filter created on every call simply misses.
     */
    private static final int FILTER_SLOTS = 8;
    private static final ClassValue<AtomicReference<Object[]>> DECLARED_FIELDS_FILTERED =
            perClass(c -> new AtomicReference<>(new Object[0]));
    private static final ClassValue<AtomicReference<Object[]>> ALL_FIELDS_FILTERED =
            perClass(c -> new AtomicReference<>(new Object[0]));

    /** Each class's declared methods, by name, in declared order; shared by every lookup that walks the class. */
    private static final ClassValue<Map<String, Method[]>> DECLARED_METHODS =
            perClass(c -> methodsByName(c.getDeclaredMethods()));

    /** Each class's public methods (Class.getMethods(), inherited ones included), by name. */
    private static final ClassValue<Map<String, Method[]>> PUBLIC_METHODS =
            perClass(c -> methodsByName(c.getMethods()));

    /** getMethod(c, name, types) answers found so far: name -> {parameter types, Method} pairs. */
    private static final ClassValue<ConcurrentHashMap<String, Object[]>> METHODS_FOUND =
            perClass(c -> new ConcurrentHashMap<>());

    /** getMethod(instance, name, argCount) answers found so far: name -> the Method chosen, by argument count. */
    private static final ClassValue<ConcurrentHashMap<String, Method[]>> METHODS_BY_ARG_COUNT =
            perClass(c -> new ConcurrentHashMap<>());

    /**
     * call(instance, name, args): name -> by argument count, NO_OVERLOADS or {Method[] overloads, the last choice as
     * AtomicReference of {Class[] argument classes, Method}}. JDK types only (see perClass).
     */
    private static final ClassValue<ConcurrentHashMap<String, Object[][]>> OVERLOADS =
            perClass(c -> new ConcurrentHashMap<>());
    private static final Object[] NO_OVERLOADS = new Object[0];

    /** Each class's declared constructors, as {parameter types, Constructor} pairs. */
    private static final ClassValue<Object[]> DECLARED_CONSTRUCTORS = perClass(ReflectionUtils::constructorTable);

    /** getAllConstructors(): the same Constructor instances, made accessible, in preference order. */
    private static final ClassValue<Constructor<?>[]> SORTED_CONSTRUCTORS =
            perClass(ReflectionUtils::sortedConstructors);

    /** getClassAnnotation(): per annotation type, the first found in the class's whole type hierarchy. */
    private static final ClassValue<Map<Class<?>, Annotation>> HIERARCHY_ANNOTATIONS =
            perClass(ReflectionUtils::hierarchyAnnotations);

    /** getMethodAnnotation(): per method of the class asked about, the annotations found on it or what it overrides. */
    private static final ClassValue<ConcurrentHashMap<Method, Map<Class<?>, Annotation>>> METHOD_ANNOTATIONS =
            perClass(c -> new ConcurrentHashMap<>());

    // A Map installed through one of the setXxxCache() methods is consulted first and filled, keyed as it always
    // was; the answers themselves come from the per-class caches above. None is installed by default.
    private static final AtomicReference<Map<Object, Constructor<?>[]>> SORTED_CONSTRUCTORS_CACHE =
            new AtomicReference<>();
    private static final AtomicReference<Map<? super ConstructorCacheKey, Constructor<?>>> CONSTRUCTOR_CACHE =
            new AtomicReference<>();
    private static final AtomicReference<Map<? super MethodCacheKey, Method>> METHOD_CACHE = new AtomicReference<>();
    private static final AtomicReference<Map<? super FieldsCacheKey, Collection<Field>>> FIELDS_CACHE =
            new AtomicReference<>();
    private static final AtomicReference<Map<? super FieldNameCacheKey, Field>> FIELD_NAME_CACHE =
            new AtomicReference<>();
    private static final AtomicReference<Map<? super ClassAnnotationCacheKey, Annotation>> CLASS_ANNOTATION_CACHE =
            new AtomicReference<>();
    private static final AtomicReference<Map<? super MethodAnnotationCacheKey, Annotation>> METHOD_ANNOTATION_CACHE =
            new AtomicReference<>();

    private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];

    // Sentinels an installed Map holds for a negative answer
    private static final Method NOT_FOUND_METHOD;
    private static final Method OVERLOADED_METHOD;
    private static final Annotation NOT_FOUND_ANNOTATION = new Annotation() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Annotation.class;
        }
    };
    private static final Field NOT_FOUND_FIELD;

    private static final class MissingFieldSentinel {
        @SuppressWarnings("unused")
        private Object missingField;
    }

    static {
        try {
            NOT_FOUND_METHOD = Object.class.getDeclaredMethod("getClass");
            OVERLOADED_METHOD = Object.class.getDeclaredMethod("hashCode");
            NOT_FOUND_FIELD = MissingFieldSentinel.class.getDeclaredField("missingField");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Wrap the map if it is not already concurrent. */
    private static <K, V> Map<K, V> ensureThreadSafe(Map<K, V> candidate) {
        if (candidate == null || candidate instanceof ConcurrentMap || candidate instanceof LRUCache) {
            return candidate;                     // none, or already thread-safe
        }
        return new ConcurrentHashMapNullSafe<>(candidate);
    }

    private static <T> void swap(AtomicReference<T> ref, T newValue) {
        ref.set(newValue);                        // atomic & happens-before; null: none installed
    }

    /**
     * Reads an installed cache Map (see the setXxxCache() methods) with {@link Map#get} first, falling back to
     * {@link Map#computeIfAbsent} only on a miss, so a hit takes no lock in a map whose computeIfAbsent does.
     * Negative answers are stored as sentinels, never {@code null}, so a {@code null} from {@code get} means absent.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> V getOrCompute(Map<? super K, V> cache, K key,
                                         Function<? super K, ? extends V> mappingFunction) {
        V existing = cache.get(key);
        if (existing != null) {
            return existing;
        }
        // View the `? super K` cache as an invariant Map<K, V> for computeIfAbsent: its
        // key-type wildcard otherwise rejects the Function<? super K, ...> argument. Safe
        // because this helper only ever reads/writes the cache with key type K and value
        // type V — the same contract every call site already satisfies.
        return ((Map<K, V>) cache).computeIfAbsent(key, mappingFunction);
    }

    /**
     * Installs a Map for method lookups (getMethod(), getNonOverloadedMethod()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setMethodCache(Map<Object, Method> cache) {
        swap(METHOD_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for field-list lookups (getDeclaredFields(), getAllDeclaredFields()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setClassFieldsCache(Map<Object, Collection<Field>> cache) {
        swap(FIELDS_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for field-by-name lookups (getField()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setFieldCache(Map<Object, Field> cache) {
        swap(FIELD_NAME_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for class annotation lookups (getClassAnnotation()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setClassAnnotationCache(Map<Object, Annotation> cache) {
        swap(CLASS_ANNOTATION_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for method annotation lookups (getMethodAnnotation()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setMethodAnnotationCache(Map<Object, Annotation> cache) {
        swap(METHOD_ANNOTATION_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for constructor lookups (getConstructor()) to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
     */
    public static void setConstructorCache(Map<Object, Constructor<?>> cache) {
        swap(CONSTRUCTOR_CACHE, ensureThreadSafe(cache));
    }

    /**
     * Installs a Map for getAllConstructors() to consult and fill,
     * keyed as in earlier versions.
     * <p>
     * Not needed: ReflectionUtils keeps what it finds on each class (see the class documentation), and the answers
     * come from there whether or not a Map is installed. An installed Map must be thread-safe (one that is not is
     * wrapped); it holds negative answers as sentinel values, never {@code null}.
     * </p>
     *
     * @param cache the Map to consult and fill, or {@code null} to install none (the default)
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
        // Additional security validation for fields
        if (obj instanceof Field) {
            validateFieldAccess((Field) obj);
        }

        // ALWAYS attempt to set accessible for json-io compatibility
        // This is required for:
        // - Module system boundaries (Java 9+)
        // - Security manager environments
        // - Performance optimizations
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
        
        // Removed special case for "normal" prefix - all fields follow same validation rules
        
        // Check if the field name suggests sensitive content (only for user classes)
        // Patterns are pre-processed (trimmed and lowercased) in getSensitiveFieldPatterns
        for (String pattern : getSensitiveFieldPatterns()) {
            if (fieldName.contains(pattern)) {
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

        // Patterns are pre-processed (trimmed) in getDangerousClassPatterns
        if (!getDangerousClassPatterns().contains(className)) {
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
     * Skips frames from ReflectionUtils itself AND internal infrastructure
     * (caches, lambdas) that can appear on the stack during normal operation
     * without representing a real caller.
     * <p>
     * <b>Why this is careful:</b> When ReflectionUtils uses its own cache
     * (LRUCache, LockingLRUCacheStrategy) via {@code computeIfAbsent}, those
     * cache classes appear on the stack between ReflectionUtils and the
     * security check. A naive {@code startsWith("com.cedarsoftware.util.")}
     * check would incorrectly treat those cache frames as "trusted callers"
     * and bypass security for EVERY caller, including external ones.
     * </p>
     *
     * @return true if the external caller chain includes a trusted java-util
     *         class (e.g., a java-util internal helper that legitimately needs
     *         reflective access to dangerous classes)
     */
    private static boolean isTrustedCaller() {
        String thisClassName = ReflectionUtils.class.getName();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stack) {
            String className = element.getClassName();

            // Skip ReflectionUtils frames (always present) and Thread.getStackTrace. ReflectionUtils' own nested
            // classes count as ReflectionUtils: a per-class cache computes its value inside ReflectionUtils$N, and
            // that frame, read as a java-util caller, would trust every caller.
            if (isFrameOf(className, thisClassName) || className.equals("java.lang.Thread")) {
                continue;
            }

            // Skip internal infrastructure classes that appear on the stack as
            // cache callback frames (not real callers).
            if (isInfrastructureFrame(className)) {
                continue;
            }

            // Check if any other caller is from the trusted java-util package
            if (className.startsWith("com.cedarsoftware.util.")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the given class name is a java-util infrastructure class
     * that may appear on the stack during callback/lambda execution but does
     * not represent a real caller. These are intermediaries between
     * ReflectionUtils and its actual caller.
     */
    private static boolean isInfrastructureFrame(String className) {
        return isFrameOf(className, "com.cedarsoftware.util.LRUCache")
                || isFrameOf(className, "com.cedarsoftware.util.cache.LockingLRUCacheStrategy")
                || isFrameOf(className, "com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy")
                || isFrameOf(className, "com.cedarsoftware.util.ConcurrentHashMapNullSafe")
                // Null-safe map superclass where compute()/computeIfAbsent() wrapping runs — the
                // THREADED strategy's backing ConcurrentHashMapNullSafe invokes the ReflectionUtils
                // mapping function from inside this class's wrapping lambda.
                || isFrameOf(className, "com.cedarsoftware.util.AbstractConcurrentNullSafeMap");
    }

    /**
     * True if {@code className} is {@code owner} itself OR one of its synthetic/inner frames
     * (lambda, nested class), which the JVM names {@code owner$...} (e.g.
     * {@code ...ConcurrentHashMapNullSafe$$Lambda/0x...}). Matching only via {@code equals()}
     * misses these: the THREADED cache strategy invokes the ReflectionUtils mapping function from
     * inside a lambda defined in {@code ConcurrentHashMapNullSafe}, so that lambda frame must still
     * be recognized as cache plumbing — otherwise {@link #isTrustedCaller()}'s
     * {@code startsWith("com.cedarsoftware.util.")} test would misread it as a real trusted caller
     * and bypass the dangerous-class check. The {@code '$'} boundary avoids matching sibling
     * classes such as {@code ConcurrentHashMapNullSafeHelper}.
     */
    private static boolean isFrameOf(String className, String owner) {
        return className.equals(owner) || className.startsWith(owner + "$");
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
        
        // Check if log obfuscation is enabled
        if (!isLogObfuscationEnabled()) {
            return className; // Return full class name when obfuscation disabled
        }
        
        if (className.length() <= 10) {
            return "[class:" + className.length() + "-chars]";
        }
        
        return className.substring(0, 5) + "***" + className.substring(className.length() - 5);
    }
    
    private static boolean isLogObfuscationEnabled() {
        return Boolean.parseBoolean(System.getProperty("reflectionutils.log.obfuscation.enabled", "false"));
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
        private static final byte EXACT_SIGNATURE_LOOKUP = 0;
        private static final byte ARG_COUNT_LOOKUP = 1;
        private static final byte NON_OVERLOADED_LOOKUP = 2;

        // Use object identity instead of string names to prevent cache poisoning
        private final Class<?> clazz;
        private final String methodName;
        private final byte lookupType;
        private final Class<?>[] parameterTypes;
        private final int hash;

        public MethodCacheKey(Class<?> clazz, String methodName, Class<?>... types) {
            this(clazz, methodName, EXACT_SIGNATURE_LOOKUP, types);
        }

        public MethodCacheKey(Class<?> clazz, String methodName, byte lookupType, Class<?>... types) {
            this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
            this.methodName = Objects.requireNonNull(methodName, "methodName cannot be null");
            this.lookupType = lookupType;
            this.parameterTypes = types.clone(); // Defensive copy

            // Use System.identityHashCode to prevent hash manipulation
            this.hash = Objects.hash(System.identityHashCode(clazz), methodName, lookupType, Arrays.hashCode(parameterTypes));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCacheKey)) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            // Use reference equality to prevent spoofing
            return this.clazz == that.clazz &&
                    this.lookupType == that.lookupType &&
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

        // Filter out synthetic enum fields
        if (declaringClass.isEnum() && "ENUM$VALUES".equals(fieldName)) {
            return false;
        }

        // Filter out Groovy metaclass fields
        if ("metaClass".equals(fieldName) &&
                "groovy.lang.MetaClass".equals(field.getType().getName())) {
            return false;
        }

        // Filter out internal enum fields that should not be modified
        return !(Enum.class.isAssignableFrom(declaringClass) &&
                (fieldName.equals("hash") || fieldName.equals("ordinal")));
    };

    /**
     * Searches for an annotation anywhere in a class's <strong>entire type hierarchy</strong> —
     * the class itself, every superclass, every implemented interface, and every
     * super-interface — and returns the first one found. Intended for framework-style,
     * Spring-{@code AnnotationUtils.findAnnotation()}-equivalent lookup where a type is
     * considered "marked with {@code @X}" whenever {@code @X} appears anywhere in its
     * declared contract.
     * <p>
     * <strong>This method intentionally goes beyond the JDK's built-in
     * {@link Class#getAnnotation(Class)} in two ways:</strong>
     * <ol>
     *     <li>It walks the <em>full superclass chain regardless of whether the annotation
     *         is marked {@link java.lang.annotation.Inherited @Inherited}</em>. The JDK
     *         only walks superclasses for {@code @Inherited} annotations.</li>
     *     <li>It also walks all implemented <em>interfaces</em>. The JDK never walks
     *         interfaces for class annotations, even when {@code @Inherited} is present.</li>
     * </ol>
     * <p>
     * <strong>When to use this method:</strong>
     * <ul>
     *     <li>Framework scanning: "Is this type {@code @Component} / {@code @Transactional} /
     *         {@code @Audit} anywhere in its contract hierarchy?"</li>
     *     <li>Interface-defined markers: an annotation on a role interface (e.g.
     *         {@code @Auditable interface Auditable}) should apply to implementing
     *         classes without re-annotation.</li>
     *     <li>AOP / interceptor discovery: checking whether a class participates in a
     *         cross-cutting concern declared on a base class or interface.</li>
     * </ul>
     * <p>
     * <strong>When NOT to use this method:</strong>
     * <ul>
     *     <li>"Identity-tied" annotations whose meaning is specific to the declaring
     *         class — e.g. JPA {@code @Entity}, json-io {@code @IoNaming} /
     *         {@code @IoTypeName}, Jackson {@code @JsonNaming}. For those, use
     *         {@link #getDirectClassAnnotation(Class, Class)} to match the JDK's
     *         {@code @Inherited}-respecting semantics.</li>
     *     <li>Code that must match {@link Class#getAnnotation} exactly — again,
     *         use {@link #getDirectClassAnnotation(Class, Class)}.</li>
     * </ul>
     * <p>
     * <strong>Implementation details:</strong>
     * <ul>
     *     <li>Depth-first traversal via a work stack with an {@link IdentitySet}
     *         visited guard, so type diamonds (interface inherited through multiple
     *         paths) are visited once.</li>
     *     <li>The walk runs once per class and records the first annotation of each type
     *         it finds, kept on the class: every later lookup, found or not, is a map read.</li>
     *     <li>Thread-safe; lookups take no lock.</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Transactional anno = ReflectionUtils.getClassAnnotation(UserService.class, Transactional.class);
     * if (anno != null) {
     *     // UserService, a superclass, OR any implemented interface declared @Transactional
     * }
     * </pre>
     *
     * @param classToCheck the class whose hierarchy should be searched (may be null)
     * @param annoClass the annotation class to search for (must not be null)
     * @param <T> the annotation type
     * @return the annotation if found anywhere in the type hierarchy, {@code null} if
     *         not found or if {@code classToCheck} is null
     * @throws IllegalArgumentException if {@code annoClass} is null
     * @see #getDirectClassAnnotation(Class, Class) For strict JDK {@code getAnnotation} semantics
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        if (classToCheck == null) {
            return null;
        }
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        Map<? super ClassAnnotationCacheKey, Annotation> installed = CLASS_ANNOTATION_CACHE.get();
        if (installed == null) {
            return (T) HIERARCHY_ANNOTATIONS.get(classToCheck).get(annoClass);
        }
        Annotation annotation = getOrCompute(installed, new ClassAnnotationCacheKey(classToCheck, annoClass), k -> {
            Annotation found = HIERARCHY_ANNOTATIONS.get(classToCheck).get(annoClass);
            return found == null ? NOT_FOUND_ANNOTATION : found;
        });
        return annotation == NOT_FOUND_ANNOTATION ? null : (T) annotation;
    }

    /**
     * Returns the annotation if it is present on {@code classToCheck} exactly as the
     * JDK's {@link Class#getAnnotation(Class)} would — respecting
     * {@link java.lang.annotation.Inherited @Inherited} for the superclass chain,
     * but <strong>never</strong> walking interfaces. This is the right method when
     * you want an annotation's author-declared inheritance semantics honored.
     * <p>
     * <strong>Semantics (identical to {@link Class#getAnnotation(Class)}):</strong>
     * <ul>
     *     <li>If {@code @X} is declared directly on {@code classToCheck}, it is returned.</li>
     *     <li>If {@code @X} is marked {@code @Inherited} and declared on a superclass
     *         but not on {@code classToCheck}, it is returned (JDK walks the superclass
     *         chain for {@code @Inherited} annotations automatically).</li>
     *     <li>If {@code @X} is declared only on an interface implemented by
     *         {@code classToCheck}, {@code null} is returned. The JDK never considers
     *         interface annotations as inheritable, even under {@code @Inherited}.</li>
     * </ul>
     * <p>
     * <strong>When to use this method:</strong>
     * <ul>
     *     <li>"Identity-tied" annotations — JPA {@code @Entity}, json-io
     *         {@code @IoNaming}, {@code @IoTypeName}, Jackson {@code @JsonNaming},
     *         Lombok {@code @Data}, etc. For these, the author deliberately chose not
     *         to mark the annotation {@code @Inherited}; that choice should be
     *         honored, and interface walks would silently change serialization /
     *         persistence / processing semantics in surprising ways.</li>
     *     <li>Any code that was written against {@link Class#getAnnotation(Class)}
     *         and must preserve its exact behavior.</li>
     * </ul>
     * <p>
     * <strong>When to use {@link #getClassAnnotation(Class, Class)} instead:</strong>
     * Framework-style lookup where a class should be considered "marked with {@code @X}"
     * when {@code @X} appears anywhere in its type hierarchy (any superclass or
     * interface), regardless of {@code @Inherited}. See that method's documentation
     * for details and use cases.
     * <p>
     * <strong>Caching:</strong> none of its own. The JVM already caches annotation instances
     * per class internally (via {@code Class.reflectionData()}), so
     * {@code clazz.getAnnotation(annoClass)} is effectively a fast identity-keyed
     * lookup after the first call.
     * <p>
     * Example usage:
     * <pre>
     * &#64;IoNaming(IoNaming.Strategy.SNAKE_CASE)
     * class BaseDto { ... }
     *
     * class ChildDto extends BaseDto { ... }
     *
     * // Returns the @IoNaming — only if it is @Inherited. @IoNaming is not,
     * // so this returns null. Semantic correctness: ChildDto didn't opt in.
     * IoNaming anno = ReflectionUtils.getDirectClassAnnotation(ChildDto.class, IoNaming.class);
     * </pre>
     *
     * @param classToCheck the class to inspect (may be null)
     * @param annoClass the annotation class to look up (must not be null)
     * @param <T> the annotation type
     * @return the annotation if present directly on {@code classToCheck} — or on a
     *         superclass when the annotation is {@code @Inherited} — otherwise
     *         {@code null}; also {@code null} if {@code classToCheck} is null
     * @throws IllegalArgumentException if {@code annoClass} is null
     * @see #getClassAnnotation(Class, Class) For framework-style full-hierarchy lookup
     *      (walks interfaces and superclasses regardless of {@code @Inherited})
     */
    public static <T extends Annotation> T getDirectClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        if (classToCheck == null) {
            return null;
        }
        Convention.throwIfNull(annoClass, "annotation class cannot be null");
        return classToCheck.getAnnotation(annoClass);
    }

    /**
     * Every annotation on the class's type hierarchy -- the class, its superclasses and its interfaces, searched
     * depth-first as getClassAnnotation() always has -- keeping the first found of each type.
     */
    private static Map<Class<?>, Annotation> hierarchyAnnotations(Class<?> classToCheck) {
        final Map<Class<?>, Annotation> found = new HashMap<>();
        final Set<Class<?>> visited = new IdentitySet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk)) {
                continue;
            }
            visited.add(classToChk);
            // getAnnotations() holds exactly what getAnnotation(type) returns, @Inherited annotations included
            for (Annotation a : classToChk.getAnnotations()) {
                found.putIfAbsent(a.annotationType(), a);
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }
        return found;
    }

    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack) {
        for (Class<?> interFace : classToCheck.getInterfaces()) {
            stack.push(interFace);
        }
    }

    /**
     * Searches for a specific annotation on a method, examining the entire inheritance hierarchy.
     * The search runs once per method; what it finds is kept on the method's declaring class.
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
     *     <li>Searches once per method: every later lookup, found or not, is a map read</li>
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
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass) {
        Convention.throwIfNull(method, "method cannot be null");
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        Map<? super MethodAnnotationCacheKey, Annotation> installed = METHOD_ANNOTATION_CACHE.get();
        if (installed == null) {
            return (T) methodAnnotations(method).get(annoClass);
        }
        Annotation annotation = getOrCompute(installed, new MethodAnnotationCacheKey(method, annoClass), k -> {
            Annotation found = methodAnnotations(method).get(annoClass);
            return found == null ? NOT_FOUND_ANNOTATION : found;
        });
        return annotation == NOT_FOUND_ANNOTATION ? null : (T) annotation;
    }

    /** The annotations on {@code method} or the methods it overrides, first found of each type; kept per method. */
    private static Map<Class<?>, Annotation> methodAnnotations(Method method) {
        ConcurrentHashMap<Method, Map<Class<?>, Annotation>> byMethod =
                METHOD_ANNOTATIONS.get(method.getDeclaringClass());
        Map<Class<?>, Annotation> found = byMethod.get(method);
        return found != null ? found : byMethod.computeIfAbsent(method, ReflectionUtils::findMethodAnnotations);
    }

    /**
     * Searches the method's declaring class, then its superclasses and interfaces breadth-first, for a method of the
     * same name and parameter types, collecting the first annotation found of each type.
     */
    private static Map<Class<?>, Annotation> findMethodAnnotations(Method method) {
        final Map<Class<?>, Annotation> found = new HashMap<>();
        final String name = method.getName();
        final Class<?>[] types = method.getParameterTypes();
        Set<Class<?>> visited = new IdentitySet<>();
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.add(method.getDeclaringClass());

        while (!toVisit.isEmpty()) {
            Class<?> currentClass = toVisit.poll();
            if (!visited.add(currentClass)) {
                continue; // Already processed this class/interface
            }

            // Try to find the method in the current class/interface
            try {
                Method currentMethod = declaredMethod(currentClass, name, types);
                if (currentMethod != null) {
                    for (Annotation a : currentMethod.getAnnotations()) {
                        found.putIfAbsent(a.annotationType(), a);
                    }
                }
            } catch (Exception ignored) {
                // This class's methods or annotations could not be read: search on
            }

            // Add superclass to visit (if exists)
            Class<?> superclass = currentClass.getSuperclass();
            if (superclass != null) {
                toVisit.add(superclass);
            }

            // Add all interfaces to visit
            for (Class<?> iface : currentClass.getInterfaces()) {
                toVisit.add(iface);
            }
        }
        return found;
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
     *     <li>Kept on the class: after the first lookup, any name, found or not, is a map read</li>
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

        Field field;
        Map<? super FieldNameCacheKey, Field> installed = FIELD_NAME_CACHE.get();
        if (installed == null) {
            field = FIELDS_BY_NAME.get(c).get(fieldName);
        } else {
            field = getOrCompute(installed, new FieldNameCacheKey(c, fieldName), k -> {
                Field found = FIELDS_BY_NAME.get(c).get(fieldName);
                return found == null ? NOT_FOUND_FIELD : found;
            });
            if (field == NOT_FOUND_FIELD) {
                field = null;
            }
        }

        // Security: Validate field access before returning
        if (field != null) {
            validateFieldAccess(field);
        }
        return field;
    }

    private static Map<String, Field> fieldsByName(Class<?> c) {
        Map<String, Field> byName = new HashMap<>();
        for (Field field : getAllDeclaredFields(c)) {  // all fields in c's hierarchy, c's own first
            byName.putIfAbsent(field.getName(), field);
        }
        return byName;
    }

    /**
     * Retrieves the declared fields of a class (not it's parent) using a custom field filter, with caching for
     * performance. This method provides direct field access with customizable filtering criteria.
     * <p>
     * Key features:
     * <ul>
     *     <li>Custom field filtering through provided Predicate</li>
     *     <li>Returns only fields declared directly on the specified class (not from superclasses)</li>
     *     <li>Caches the list on the class (see below)</li>
     *     <li>Makes non-public fields accessible when possible</li>
     *     <li>Returns an unmodifiable List to prevent modification</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *     <li>Thread-safe caching mechanism</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Maintains consistent order of fields</li>
     *     <li>Caches per class: the default filter's list always; for a caller's own filter, the lists of the
     *         last 8 filters used on the class (a filter is held weakly, and one created on every call is not
     *         reused)</li>
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

        Map<? super FieldsCacheKey, Collection<Field>> installed = FIELDS_CACHE.get();
        if (installed == null) {
            return declaredFields(c, fieldFilter);
        }
        return (List<Field>) getOrCompute(installed, new FieldsCacheKey(c, fieldFilter, false),
                k -> declaredFields(c, fieldFilter));
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
     *     <li>Each class's own list is cached and shared by its subclasses' lists</li>
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

        Map<? super FieldsCacheKey, Collection<Field>> installed = FIELDS_CACHE.get();
        if (installed == null) {
            return allDeclaredFields(c, fieldFilter);
        }
        return (List<Field>) getOrCompute(installed, new FieldsCacheKey(c, fieldFilter, true),
                k -> allDeclaredFields(c, fieldFilter));
    }

    /** The class's own fields that pass the filter; see DECLARED_FIELDS_DEFAULT and FILTER_SLOTS. */
    private static List<Field> declaredFields(Class<?> c, Predicate<Field> fieldFilter) {
        return fieldFilter == DEFAULT_FIELD_FILTER ? DECLARED_FIELDS_DEFAULT.get(c) : filtered(c, fieldFilter, false);
    }

    /** The fields of the class and its superclasses that pass the filter; see ALL_FIELDS_DEFAULT and FILTER_SLOTS. */
    private static List<Field> allDeclaredFields(Class<?> c, Predicate<Field> fieldFilter) {
        return fieldFilter == DEFAULT_FIELD_FILTER ? ALL_FIELDS_DEFAULT.get(c) : filtered(c, fieldFilter, true);
    }

    /** The list for a caller's own filter: from the class's filter slots, or worked out and added to them. */
    private static List<Field> filtered(Class<?> c, Predicate<Field> fieldFilter, boolean deep) {
        AtomicReference<Object[]> slots = (deep ? ALL_FIELDS_FILTERED : DECLARED_FIELDS_FILTERED).get(c);
        List<Field> fields = inSlots(slots.get(), fieldFilter);
        if (fields != null) {
            return fields;
        }
        fields = deep ? collectAllDeclaredFields(c, fieldFilter) : filterDeclaredFields(c, fieldFilter);
        while (true) {
            Object[] current = slots.get();
            List<Field> raced = inSlots(current, fieldFilter);
            if (raced != null) {
                return raced;                   // another thread got there first: one list per filter
            }
            int keep = Math.min(current.length, 2 * (FILTER_SLOTS - 1));
            Object[] next = new Object[keep + 2];
            next[0] = new WeakReference<>(fieldFilter);
            next[1] = fields;
            System.arraycopy(current, 0, next, 2, keep);
            if (slots.compareAndSet(current, next)) {
                return fields;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Field> inSlots(Object[] slots, Predicate<Field> fieldFilter) {
        for (int i = 0; i < slots.length; i += 2) {
            if (((WeakReference<?>) slots[i]).get() == fieldFilter) {
                return (List<Field>) slots[i + 1];
            }
        }
        return null;
    }

    /** The class's own fields that pass the filter, made accessible (and vetted, when security is on). */
    private static List<Field> filterDeclaredFields(Class<?> c, Predicate<Field> fieldFilter) {
        Field[] declared = DECLARED_FIELDS.get(c);
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
    }

    /** The fields of the class and its superclasses that pass the filter, the class's own first. */
    private static List<Field> collectAllDeclaredFields(Class<?> c, Predicate<Field> fieldFilter) {
        List<Field> allFields = new ArrayList<>();
        Class<?> current = c;
        while (current != null && !isDangerousClass(current)) {
            allFields.addAll(declaredFields(current, fieldFilter));
            current = current.getSuperclass();
        }
        // Return an unmodifiable list to prevent further modification
        return Collections.unmodifiableList(allFields);
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
        Collection<Field> fields = getAllDeclaredFields(c, fieldFilter);  // cached on the class

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
     *     <li>Caches what it finds on the class; a name the class does not have stores nothing</li>
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
        Method method = getMethod(instance, methodName, args.length);
        if (args.length > 0) {
            // Among overloads with that many parameters, the one Java would call with these arguments
            Class<?> beanClass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();
            Method overload = chooseOverload(overloads(beanClass, methodName, args.length), args);
            if (overload != null) {
                secureSetAccessible(overload);
                method = overload;
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
     * Retrieves a method of any access level by name and parameter types, cached on the class. This method
     * searches through the class hierarchy and attempts to make non-public methods accessible.
     * <p>
     * Key features:
     * <ul>
     *     <li>Finds methods of any access level (public, protected, package, private)</li>
     *     <li>Includes bridge methods (compiler-generated for generic type erasure)</li>
     *     <li>Includes synthetic methods (compiler-generated for lambdas, inner classes)</li>
     *     <li>Attempts to make non-public methods accessible</li>
     *     <li>Caches what it finds on the class; a name the class does not have stores nothing</li>
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
        Objects.requireNonNull(types, "types cannot be null");

        Map<? super MethodCacheKey, Method> installed = METHOD_CACHE.get();
        if (installed == null) {
            return findMethod(c, methodName, types);
        }
        Method method = getOrCompute(installed, new MethodCacheKey(c, methodName, types), k -> {
            Method found = findMethod(c, methodName, types);
            return found == null ? NOT_FOUND_METHOD : found;
        });
        return method == NOT_FOUND_METHOD ? null : method;
    }

    /** getMethod(c, name, types): what is found is kept on the class; a miss is looked up again, storing nothing. */
    private static Method findMethod(Class<?> c, String methodName, Class<?>[] types) {
        ConcurrentHashMap<String, Object[]> found = METHODS_FOUND.get(c);
        Method method = inPairs(found.get(methodName), types);
        if (method != null) {
            return method;
        }
        method = resolveMethod(c, methodName, types);
        if (method == null) {
            return null;
        }
        secureSetAccessible(method);
        final Method resolved = method;
        final Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] pairs = found.compute(methodName, (name, current) -> {
            if (inPairs(current, parameterTypes) != null) {
                return current;                 // another thread got there first
            }
            int n = current == null ? 0 : current.length;
            Object[] next = current == null ? new Object[2] : Arrays.copyOf(current, n + 2);
            next[n] = parameterTypes;
            next[n + 1] = resolved;
            return next;
        });
        return inPairs(pairs, parameterTypes);
    }

    private static Method inPairs(Object[] pairs, Class<?>[] types) {
        if (pairs != null) {
            for (int i = 0; i < pairs.length; i += 2) {
                if (Arrays.equals((Class<?>[]) pairs[i], types)) {
                    return (Method) pairs[i + 1];
                }
            }
        }
        return null;
    }

    /** getMethod()'s search: the class and its superclasses, then interfaces breadth-first, then public methods. */
    private static Method resolveMethod(Class<?> c, String methodName, Class<?>[] types) {
        // 1) Walk class chain first
        for (Class<?> current = c; current != null; current = current.getSuperclass()) {
            Method method = declaredMethod(current, methodName, types);
            if (method != null) {
                return method;
            }
        }

        // 2) Walk interface graph (BFS) for default methods
        Set<Class<?>> seen = new IdentitySet<>();
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.add(c);

        while (!toVisit.isEmpty()) {
            Class<?> x = toVisit.poll();
            if (!seen.add(x)) continue;

            for (Class<?> iface : x.getInterfaces()) {
                Method method = declaredMethod(iface, methodName, types);
                if (method != null) {
                    return method;
                }
                toVisit.add(iface);
            }

            // Also check superclass interfaces
            Class<?> superclass = x.getSuperclass();
            if (superclass != null) {
                toVisit.add(superclass);
            }
        }

        // 3) Fallback to JDK resolution for public methods across interfaces (asked only when one exists)
        Method[] named = PUBLIC_METHODS.get(c).get(methodName);
        if (named != null) {
            for (Method method : named) {
                if (sameParameters(method, types)) {
                    try {
                        return c.getMethod(methodName, types);
                    } catch (NoSuchMethodException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * What {@code c.getDeclaredMethod(name, types)} returns, without the exception when there is none: of the
     * class's declared methods with that name and those parameter types, the one with the most specific return
     * type (a covariant override over its bridge method), chosen as the JDK chooses it.
     */
    private static Method declaredMethod(Class<?> c, String methodName, Class<?>[] types) {
        Method[] named = DECLARED_METHODS.get(c).get(methodName);
        if (named == null) {
            return null;
        }
        Method result = null;
        for (Method method : named) {
            if (sameParameters(method, types) && (result == null
                    || (result.getReturnType() != method.getReturnType()
                        && result.getReturnType().isAssignableFrom(method.getReturnType())))) {
                result = method;
            }
        }
        return result;
    }

    private static boolean sameParameters(Method method, Class<?>[] types) {
        return method.getParameterCount() == types.length && Arrays.equals(method.getParameterTypes(), types);
    }

    private static Map<String, Method[]> methodsByName(Method[] methods) {
        Map<String, List<Method>> grouped = new HashMap<>();
        for (Method method : methods) {
            grouped.computeIfAbsent(method.getName(), k -> new ArrayList<>(1)).add(method);
        }
        Map<String, Method[]> byName = new HashMap<>(grouped.size() * 4 / 3 + 1);
        for (Map.Entry<String, List<Method>> entry : grouped.entrySet()) {
            byName.put(entry.getKey(), entry.getValue().toArray(new Method[0]));
        }
        return byName;
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
     *     <li>Caches what it finds on the class; a name the class does not have stores nothing</li>
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

        if (isDangerousClass(beanClass)) {
            LOG.log(Level.WARNING, "Method access blocked for dangerous class: " + sanitizeClassName(beanClass.getName()) + "." + methodName);
            throw new SecurityException("Access denied: Method access not permitted for security-sensitive class");
        }

        if (argCount == 0) {
            Method exact = getMethod(beanClass, methodName, NO_PARAMETERS);
            if (exact != null) {
                return exact;
            }
        }

        Method result;
        Map<? super MethodCacheKey, Method> installed = METHOD_CACHE.get();
        if (installed == null) {
            result = methodByArgCount(beanClass, methodName, argCount);
        } else {
            Class<?>[] types = new Class<?>[argCount];
            Arrays.fill(types, Object.class);
            MethodCacheKey key = new MethodCacheKey(beanClass, methodName, MethodCacheKey.ARG_COUNT_LOOKUP, types);
            result = getOrCompute(installed, key, k -> {
                Method found = methodByArgCount(beanClass, methodName, argCount);
                return found == null ? NOT_FOUND_METHOD : found;
            });
            if (result == NOT_FOUND_METHOD) {
                result = null;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' with %d parameters not found in %s, its superclasses, or interfaces",
                            methodName, argCount, beanClass.getName())
            );
        }
        return result;
    }

    /** getMethod(instance, name, argCount): what is found is kept on the class; a miss is looked up again. */
    private static Method methodByArgCount(Class<?> c, String methodName, int argCount) {
        ConcurrentHashMap<String, Method[]> found = METHODS_BY_ARG_COUNT.get(c);
        Method[] byCount = found.get(methodName);
        if (byCount != null && argCount < byCount.length && byCount[argCount] != null) {
            return byCount[argCount];
        }
        Method selected = selectByArgCount(c, methodName, argCount);
        if (selected == null) {
            return null;
        }
        secureSetAccessible(selected);
        byCount = found.compute(methodName, (name, current) -> {
            if (current != null && argCount < current.length && current[argCount] != null) {
                return current;                 // another thread got there first
            }
            int length = current == null ? argCount + 1 : Math.max(current.length, argCount + 1);
            Method[] next = current == null ? new Method[length] : Arrays.copyOf(current, length);
            next[argCount] = selected;
            return next;
        });
        return byCount[argCount];
    }

    /** Of the methods with that name and argument count anywhere in the hierarchy (breadth-first), the best one. */
    private static Method selectByArgCount(Class<?> c, String methodName, int argCount) {
        List<Method> candidates = methodsNamed(c, methodName, argCount);
        // Select the best matching method using our composite strategy
        return candidates.isEmpty() ? null : selectMethod(candidates);
    }

    /**
     * The overloads call() chooses among: the methods with that name and argument count in the hierarchy, one per
     * parameter list (the one selectMethod() prefers, e.g. an override over the method it overrides), bridge and
     * synthetic methods left out, as the compiler leaves them out. Per class, name and count, computed once.
     */
    private static Object[] overloads(Class<?> c, String methodName, int argCount) {
        ConcurrentHashMap<String, Object[][]> byName = OVERLOADS.get(c);
        Object[][] byCount = byName.get(methodName);
        if (byCount != null && argCount < byCount.length && byCount[argCount] != null) {
            return byCount[argCount];
        }
        List<List<Method>> groups = new ArrayList<>();
        for (Method method : methodsNamed(c, methodName, argCount)) {
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            List<Method> group = null;
            for (List<Method> g : groups) {
                if (Arrays.equals(g.get(0).getParameterTypes(), method.getParameterTypes())) {
                    group = g;
                    break;
                }
            }
            if (group == null) {
                group = new ArrayList<>(2);
                groups.add(group);
            }
            group.add(method);
        }
        Object[] entry;
        if (groups.size() < 2) {
            entry = NO_OVERLOADS;
        } else {
            Method[] overloads = new Method[groups.size()];
            for (int i = 0; i < overloads.length; i++) {
                overloads[i] = selectMethod(groups.get(i));
            }
            entry = new Object[]{overloads, new AtomicReference<Object[]>()};
        }
        byName.compute(methodName, (name, current) -> {
            int length = current == null ? argCount + 1 : Math.max(current.length, argCount + 1);
            Object[][] next = current == null ? new Object[length][] : Arrays.copyOf(current, length);
            if (next[argCount] == null) {
                next[argCount] = entry;
            }
            return next;
        });
        return entry;
    }

    /**
     * The overload Java would choose for these arguments (JLS 15.12.2): of those the arguments can be passed to
     * without unboxing -- or, when none, with unboxing and widening, as Method.invoke() does -- the most specific
     * (every parameter type a subtype of the other's, JLS 4.10). Null when there are not two overloads, when none
     * takes the arguments, or when no single one is most specific; call() then uses getMethod(instance, name, count).
     * The choice depends only on the arguments' classes, so the last one is remembered with them.
     */
    @SuppressWarnings("unchecked")
    private static Method chooseOverload(Object[] entry, Object[] args) {
        if (entry == NO_OVERLOADS) {
            return null;
        }
        AtomicReference<Object[]> memo = (AtomicReference<Object[]>) entry[1];
        Object[] last = memo.get();
        if (last != null && sameClasses((Class<?>[]) last[0], args)) {
            return (Method) last[1];
        }
        Method chosen = chooseOverload((Method[]) entry[0], args);
        Class<?>[] classes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            classes[i] = args[i] == null ? null : args[i].getClass();
        }
        memo.set(new Object[]{classes, chosen});
        return chosen;
    }

    private static boolean sameClasses(Class<?>[] classes, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (classes[i] != (args[i] == null ? null : args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Method chooseOverload(Method[] overloads, Object[] args) {
        for (int phase = 1; phase <= 2; phase++) {
            List<Method> applicable = new ArrayList<>(overloads.length);
            for (Method m : overloads) {
                if (accepts(m.getParameterTypes(), args, phase == 2)) {
                    applicable.add(m);
                }
            }
            if (applicable.isEmpty()) {
                continue;
            }
            Method best = null;
            for (Method m : applicable) {
                boolean beaten = false;
                for (Method other : applicable) {
                    beaten |= other != m && moreSpecific(other.getParameterTypes(), m.getParameterTypes());
                }
                if (!beaten) {
                    if (best != null) {
                        return null;        // ambiguous
                    }
                    best = m;
                }
            }
            return best;
        }
        return null;
    }

    /** Whether each argument can be passed as it is (with unboxing and primitive widening when allowed). */
    private static boolean accepts(Class<?>[] types, Object[] args, boolean unboxing) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Object arg = args[i];
            if (arg == null) {
                if (type.isPrimitive()) {
                    return false;
                }
            } else if (type.isPrimitive()) {
                if (!unboxing || ClassUtilities.computeInheritanceDistance(arg.getClass(), type) < 0) {
                    return false;       // not a wrapper, or not one that widens to this primitive
                }
            } else if (!type.isInstance(arg)) {
                return false;
            }
        }
        return true;
    }

    /** Every parameter type of {@code a} a subtype of {@code b}'s (primitives by widening), and one a proper one. */
    private static boolean moreSpecific(Class<?>[] a, Class<?>[] b) {
        boolean stricter = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == b[i]) {
                continue;
            }
            boolean sub = a[i].isPrimitive() && b[i].isPrimitive()
                    ? ClassUtilities.computeInheritanceDistance(a[i], b[i]) > 0     // int widens to long
                    : !a[i].isPrimitive() && !b[i].isPrimitive() && b[i].isAssignableFrom(a[i]);
            if (!sub) {
                return false;
            }
            stricter = true;
        }
        return stricter;
    }

    /** The methods with that name and argument count anywhere in the hierarchy, breadth-first. */
    private static List<Method> methodsNamed(Class<?> c, String methodName, int argCount) {
        // Collect all matching methods from class hierarchy and interfaces
        List<Method> candidates = new ArrayList<>();
        Set<Class<?>> visited = new IdentitySet<>();
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.add(c);

        while (!toVisit.isEmpty()) {
            Class<?> current = toVisit.poll();
            if (!visited.add(current)) {
                continue; // Already processed this class/interface
            }

            // Check declared methods in current class/interface
            Method[] named = DECLARED_METHODS.get(current).get(methodName);
            if (named != null) {
                for (Method method : named) {
                    if (method.getParameterCount() == argCount) {
                        candidates.add(method);
                    }
                }
            }

            // Add superclass to visit (if exists)
            Class<?> superclass = current.getSuperclass();
            if (superclass != null) {
                toVisit.add(superclass);
            }

            // Add interfaces to visit
            for (Class<?> iface : current.getInterfaces()) {
                toVisit.add(iface);
            }
        }
        return candidates;
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
        Objects.requireNonNull(parameterTypes, "parameterTypes cannot be null");

        Map<? super ConstructorCacheKey, Constructor<?>> installed = CONSTRUCTOR_CACHE.get();
        if (installed == null) {
            return (Constructor<T>) findConstructor(clazz, parameterTypes);
        }
        // A missing constructor is not stored (null)
        return (Constructor<T>) getOrCompute(installed, new ConstructorCacheKey(clazz, parameterTypes),
                k -> findConstructor(clazz, parameterTypes));
    }

    /** The declared constructor with exactly these parameter types, made accessible; null if there is none. */
    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] parameterTypes) {
        try {
            Object[] table = DECLARED_CONSTRUCTORS.get(clazz);
            for (int i = 0; i < table.length; i += 2) {
                if (Arrays.equals((Class<?>[]) table[i], parameterTypes)) {
                    Constructor<?> ctor = (Constructor<?>) table[i + 1];
                    secureSetAccessible(ctor); // Secure method with proper security checks
                    return ctor;
                }
            }
            return null;
        } catch (SecurityException ignored) {
            // If security manager denies access
            return null;
        }
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

        Map<Object, Constructor<?>[]> installed = SORTED_CONSTRUCTORS_CACHE.get();
        if (installed == null) {
            return SORTED_CONSTRUCTORS.get(clazz);
        }
        Constructor<?>[] cached = installed.get(clazz);
        if (cached != null) {
            return cached;
        }
        return getOrCompute(installed, clazz, k -> SORTED_CONSTRUCTORS.get(clazz));
    }

    /** The class's declared constructors with their parameter types: {Class<?>[], Constructor} pairs. */
    private static Object[] constructorTable(Class<?> clazz) {
        Constructor<?>[] declared = clazz.getDeclaredConstructors();
        Object[] table = new Object[declared.length * 2];
        for (int i = 0; i < declared.length; i++) {
            table[2 * i] = declared[i].getParameterTypes();
            table[2 * i + 1] = declared[i];
        }
        return table;
    }

    /**
     * The class's constructors, made accessible, sorted in preference order. They are the same instances
     * getConstructor() returns.
     */
    private static Constructor<?>[] sortedConstructors(Class<?> clazz) {
        Object[] table = DECLARED_CONSTRUCTORS.get(clazz);
        Constructor<?>[] result = new Constructor<?>[table.length / 2];
        for (int i = 0; i < result.length; i++) {
            Constructor<?> ctor = (Constructor<?>) table[2 * i + 1];
            secureSetAccessible(ctor);
            result[i] = ctor;
        }

        // Sort the constructors in optimal order if there's more than one
        if (result.length > 1) {
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
                // Always prefer more parameters (more specific constructors first)
                int countDiff = c2.getParameterCount() - c1.getParameterCount();
                if (countDiff != 0) {
                    return countDiff;
                }

                // Tie-breaker: sort by parameter type names for deterministic ordering.
                // Class.getDeclaredConstructors() returns constructors in JVM-dependent
                // order that can vary across runs (especially with bytecode instrumentation
                // like JaCoCo). Without this tie-breaker, constructor selection becomes
                // non-deterministic when multiple constructors have identical modifiers
                // and parameter counts (e.g., String... vs Integer...).
                // Prefer String parameters first (most widely convertible), then alphabetical.
                Class<?>[] types1 = c1.getParameterTypes();
                Class<?>[] types2 = c2.getParameterTypes();
                for (int i = 0; i < types1.length; i++) {
                    Class<?> t1 = types1[i];
                    Class<?> t2 = types2[i];
                    if (t1 == t2) continue;
                    // For varargs, unwrap the array component type for comparison
                    Class<?> compare1 = t1.isArray() ? t1.getComponentType() : t1;
                    Class<?> compare2 = t2.isArray() ? t2.getComponentType() : t2;
                    // Prefer String — it's the most widely convertible type via Converter
                    if (compare1 == String.class && compare2 != String.class) return -1;
                    if (compare2 == String.class && compare1 != String.class) return 1;
                    // Fall back to alphabetical by name for stable ordering
                    int cmp = t1.getName().compareTo(t2.getName());
                    if (cmp != 0) return cmp;
                }
                return 0;
            });
        }

        return result;
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

        if (isDangerousClass(clazz)) {
            LOG.log(Level.WARNING, "Method access blocked for dangerous class: " + sanitizeClassName(clazz.getName()) + "." + methodName);
            throw new SecurityException("Access denied: Method access not permitted for security-sensitive class");
        }

        Method result;
        Map<? super MethodCacheKey, Method> installed = METHOD_CACHE.get();
        if (installed == null) {
            result = nonOverloadedMethod(clazz, methodName);
        } else {
            MethodCacheKey key = new MethodCacheKey(clazz, methodName, MethodCacheKey.NON_OVERLOADED_LOOKUP);
            result = getOrCompute(installed, key, k -> nonOverloadedMethod(clazz, methodName));
        }

        // Check for sentinels and throw appropriate exception
        if (result == OVERLOADED_METHOD) {
            throw new IllegalArgumentException("Method: " + methodName + "() called on a class with overloaded methods "
                    + "- ambiguous as to which one to return. Use getMethod() with argument types or argument count.");
        }
        if (result == NOT_FOUND_METHOD) {
            throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + clazz.getName()
                    + ". Perhaps the method is protected, private, or misspelled?");
        }
        return result;
    }

    /** The class's one public method of that name if it takes no arguments; otherwise a sentinel saying why not. */
    private static Method nonOverloadedMethod(Class<?> clazz, String methodName) {
        Method[] named = PUBLIC_METHODS.get(clazz).get(methodName);
        if (named == null) {
            return NOT_FOUND_METHOD;
        }
        if (named.length > 1) {
            return OVERLOADED_METHOD;       // multiple methods with this name: ambiguous
        }
        Method method = named[0];
        if (method.getParameterCount() != 0) {
            return NOT_FOUND_METHOD;        // no 0-arg method
        }
        secureSetAccessible(method);
        return method;
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
            // constant_pool_count must be >= 1 per the JVM spec, so cpcnt (count - 1) must be >= 0.
            // Guard so a malformed count of 0 throws the documented IllegalStateException instead of
            // a NegativeArraySizeException from the array allocation below.
            if (cpcnt < 0) {
                throw new IllegalStateException("Invalid constant pool count: " + (cpcnt + 1));
            }
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

            // Bounds checking for malformed class files
            if (thisClassIndex < 1 || thisClassIndex > classes.length) {
                throw new IllegalStateException("Invalid this_class index: " + thisClassIndex);
            }
            int stringIndex = classes[thisClassIndex - 1];
            if (stringIndex < 1 || stringIndex > strings.length) {
                throw new IllegalStateException("Invalid string index: " + stringIndex);
            }
            String className = strings[stringIndex - 1];
            if (className == null) {
                throw new IllegalStateException("Class name not found in constant pool");
            }
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
    
    // Record support (Java 14+) - using holder class for thread-safe lazy initialization
    // with zero synchronization overhead after init
    private static class RecordSupport {
        static final Method IS_RECORD_METHOD;
        static final Method GET_RECORD_COMPONENTS_METHOD;
        static final Method COMPONENT_GET_NAME_METHOD;
        static final Method COMPONENT_GET_ACCESSOR_METHOD;
        static final boolean SUPPORTED;

        static {
            Method isRecord = null;
            Method getComponents = null;
            Method getName = null;
            Method getAccessor = null;
            boolean supported = false;
            try {
                isRecord = Class.class.getMethod("isRecord");
                getComponents = Class.class.getMethod("getRecordComponents");
                // Get RecordComponent methods by getting the class first
                Class<?> recordComponentClass = Class.forName("java.lang.reflect.RecordComponent");
                getName = recordComponentClass.getMethod("getName");
                getAccessor = recordComponentClass.getMethod("getAccessor");
                supported = true;
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                // Running on Java < 14
            }
            IS_RECORD_METHOD = isRecord;
            GET_RECORD_COMPONENTS_METHOD = getComponents;
            COMPONENT_GET_NAME_METHOD = getName;
            COMPONENT_GET_ACCESSOR_METHOD = getAccessor;
            SUPPORTED = supported;
        }
    }

    /**
     * Check if a class is a Record (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     *
     * @param clazz The class to check
     * @return true if the class is a Record, false otherwise or if running on Java {@code < 14}
     */
    public static boolean isRecord(Class<?> clazz) {
        if (clazz == null || !RecordSupport.SUPPORTED) {
            return false;
        }
        try {
            return (Boolean) RecordSupport.IS_RECORD_METHOD.invoke(clazz);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the record components of a Record class (Java 14+).
     * Uses reflection to maintain compatibility with Java 8.
     *
     * @param clazz The Record class
     * @return Array of RecordComponent objects, or null if not a Record or running on Java {@code < 14}
     */
    public static Object[] getRecordComponents(Class<?> clazz) {
        if (!isRecord(clazz)) {
            return null;
        }
        try {
            return (Object[]) RecordSupport.GET_RECORD_COMPONENTS_METHOD.invoke(clazz);
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
        if (recordComponent == null || !RecordSupport.SUPPORTED) {
            return null;
        }

        try {
            return (String) RecordSupport.COMPONENT_GET_NAME_METHOD.invoke(recordComponent);
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
        if (recordComponent == null || recordInstance == null || !RecordSupport.SUPPORTED) {
            return null;
        }

        try {
            // RecordComponent has an accessor() method that returns the Method to get the value
            Method accessor = (Method) RecordSupport.COMPONENT_GET_ACCESSOR_METHOD.invoke(recordComponent);
            return accessor.invoke(recordInstance);
        } catch (Exception e) {
            return null;
        }
    }
}
