package com.cedarsoftware.util.convert;

import java.io.Externalizable;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.IdentitySet;
import com.cedarsoftware.util.LRUCache;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.TTLCache;
import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

/**
 * Instance conversion utility for converting objects between various types.
 * <p>
 * Supports conversion from primitive types to their corresponding wrapper classes, Number classes,
 * Date and Time classes (e.g., {@link Date}, {@link Timestamp}, {@link LocalDate}, {@link LocalDateTime},
 * {@link ZonedDateTime}, {@link Calendar}), {@link BigInteger}, {@link BigDecimal}, Atomic classes
 * (e.g., {@link AtomicBoolean}, {@link AtomicInteger}, {@link AtomicLong}), {@link Class}, {@link UUID},
 * {@link String}, Collection classes (e.g., {@link List}, {@link Set}, {@link Map}), ByteBuffer, CharBuffer,
 * and other related classes.
 * </p>
 * <p>
 * The Converter includes thousands of built-in conversions. Use the {@link #getSupportedConversions()}
 * API to view all source-to-target conversion mappings.
 * </p>
 * <p>
 * The primary API is {@link #convert(Object, Class)}. For example:
 * <pre>{@code
 *     Long x = convert("35", Long.class);
 *     Date d = convert("2015/01/01", Date.class);
 *     int y = convert(45.0, int.class);
 *     String dateStr = convert(date, String.class);
 *     String dateStr = convert(calendar, String.class);
 *     Short t = convert(true, short.class);     // returns (short) 1 or 0
 *     Long time = convert(calendar, long.class); // retrieves calendar's time as long
 *     Map<String, Object> map = Map.of("_v", "75.0");
 *     Double value = convert(map, double.class); // Extracts "_v" key and converts it
 * }</pre>
 * </p>
 * <p>
 * <strong>Null Handling:</strong> If a null value is passed as the source, the Converter returns:
 * <ul>
 *     <li>null for object types</li>
 *     <li>0 for numeric primitive types</li>
 *     <li>false for boolean primitives</li>
 *     <li>'\u0000' for char primitives</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Map Conversions:</strong> A {@code Map} can be converted to almost all supported JDK data classes.
 * For example, {@link UUID} can be converted to/from a {@code Map} with keys like "mostSigBits" and "leastSigBits".
 * Date/Time classes expect specific keys such as "time" or "nanos". For other classes, the Converter typically
 * looks for a "value" key to source the conversion.
 * </p>
 * <p>
 * <strong>Extensibility:</strong> Additional conversions can be added by specifying the source class, target class,
 * and a conversion function (e.g., a lambda). Use the {@link #addConversion(Class, Class, Convert)} method to register
 * custom converters. This allows for the inclusion of new Collection types and other custom types as needed.
 * </p>
 *
 * <p>
 * <strong>Supported Collection Conversions:</strong>
 * The Converter supports conversions involving various Collection types, including but not limited to:
 * <ul>
 *     <li>{@link List}</li>
 *     <li>{@link Set}</li>
 *     <li>{@link Map}</li>
 *     <li>{@link Collection}</li>
 *     <li>Arrays (e.g., {@code byte[]}, {@code char[]}, {@code ByteBuffer}, {@code CharBuffer})</li>
 * </ul>
 * These conversions facilitate seamless transformation between different Collection types and other supported classes.
 * </p>
 *
 * <p>
 * <strong>JDK Module Requirements:</strong>
 * <ul>
 *     <li><b>java.sql</b> (REQUIRED): Core date/time conversions use {@code java.sql.Timestamp} and {@code java.sql.Date}.
 *         This adds ~500KB to your runtime footprint but does NOT require database connectivity or JDBC drivers.
 *         For JPMS modules, this dependency is automatically transitive when you {@code requires com.cedarsoftware.util}.</li>
 *     <li><b>java.compiler</b> (OPTIONAL): Only needed for {@code CompactMap} runtime code generation.
 *         Most users don't need this. Available at compile time in JDK, may not be present in JRE-only environments.</li>
 *     <li><b>java.xml</b> (OPTIONAL): Only needed for XML-specific methods in {@code IOUtilities}.
 *         Marked as a static (optional) dependency in the JPMS module descriptor.</li>
 * </ul>
 * See the README for more details on module requirements and deployment considerations.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 *     ConverterOptions options = new ConverterOptions();
 *     Converter converter = new Converter(options);
 *
 *     // Convert String to Integer
 *     Integer number = converter.convert("123", Integer.class);
 *
 *     // Convert Enum to String
 *     Day day = Day.MONDAY;
 *     String dayStr = converter.convert(day, String.class);
 *
 *     // Convert Object[], String[], Collection, and primitive Arrays to EnumSet
 *     Object[] array = {Day.MONDAY, Day.WEDNESDAY, "FRIDAY", 4};
 *     EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);
 *
 *     Enum, String, and Number value in the source collection/array is properly converted
 *     to the correct Enum type and added to the returned EnumSet. Null values inside the
 *     source (Object[], Collection) are skipped.
 *
 *     When converting arrays or collections to EnumSet, you must use a double cast due to Java's
 *     type system and generic type erasure. The cast is safe as the converter guarantees return of
 *     an EnumSet when converting arrays/collections to enum types.
 *
 *     // Add a custom conversion from String to CustomType
 *     converter.addConversion(String.class, CustomType.class, (from, conv) -> new CustomType(from));
 *
 *     // Convert using the custom converter
 *     CustomType custom = converter.convert("customValue", CustomType.class);
 * }</pre>
 * </p>
 *
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public final class Converter {
    private static final Convert<?> UNSUPPORTED = Converter::unsupported;
    static final String VALUE = "_v";
    
    // Precision constants for time conversions
    public static final String PRECISION_MILLIS = "millis";
    public static final String PRECISION_NANOS = "nanos";
    /**
     * Per class, its complete type hierarchy in ClassLevel order (the class itself at level 0, then its supertypes
     * but the marker interfaces), as {Class<?>[] types, int[] levels}: JDK types, kept in the class itself, so they pin
     * no class loader. Two ClassValueMaps of SortedSet<ClassLevel> kept a java-util type in JDK classes.
     */
    private static final ClassValue<Object[]> TYPE_HIERARCHY = new ClassValue<Object[]>() {
        @Override
        protected Object[] computeValue(Class<?> type) {
            SortedSet<ClassLevel> hierarchy = new TreeSet<>();
            hierarchy.add(new ClassLevel(type, 0));
            ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(type);
            for (Map.Entry<Class<?>, Integer> entry : info.getDistanceMap().entrySet()) {
                Class<?> supertype = entry.getKey();
                int distance = entry.getValue();
                // Skip the class itself and marker interfaces
                if (distance > 0 &&
                        supertype != Serializable.class &&
                        supertype != Cloneable.class &&
                        supertype != Comparable.class &&
                        supertype != Externalizable.class) {
                    hierarchy.add(new ClassLevel(supertype, distance));
                }
            }
            Class<?>[] types = new Class<?>[hierarchy.size()];
            int[] levels = new int[hierarchy.size()];
            int i = 0;
            for (ClassLevel level : hierarchy) {
                types[i] = level.clazz;
                levels[i++] = level.level;
            }
            return new Object[]{types, levels};
        }
    };
    /**
     * How to make a class the Converter has no conversion to, from the value of its nearest supertype that it has,
     * keyed by that supertype: given the requested class, a function that makes one from the supertype's value, or
     * null when that class cannot be made. Asked for MyDate (extends Date), the inheritance walk finds, say, Long ->
     * Date, which makes a plain Date; the Date maker turns it into a MyDate (see toSubtype). The most specific maker on
     * the supertype's superclass chain applies (java.sql.Date and java.sql.Time use Date's). Makers exist for the value
     * types whose state can be set on a new instance (Date, Timestamp, Calendar, AtomicLong, AtomicInteger,
     * AtomicBoolean, BitSet), for Map, and for the immutable types a constructor makes (BigDecimal, BigInteger,
     * File). For Number, TimeZone, InetAddress and Buffer, whose conversions choose the result's class from the
     * value, the result must already be of the requested type. A supertype with no maker converts as before.
     */
    private static final Map<Class<?>, Function<Class<?>, BiFunction<Object, Converter, Object>>> SUBTYPE_MAKERS =
            subtypeMakers();

    private static Map<Class<?>, Function<Class<?>, BiFunction<Object, Converter, Object>>> subtypeMakers() {
        Map<Class<?>, Function<Class<?>, BiFunction<Object, Converter, Object>>> makers = new HashMap<>();
        makers.put(Date.class, type -> maker(type, long.class, value -> ((Date) value).getTime(),
                (made, millis) -> ((Date) made).setTime((Long) millis)));
        makers.put(Timestamp.class, type -> {
            BiFunction<Object, Converter, Object> date = SUBTYPE_MAKERS.get(Date.class).apply(type);
            return date == null ? null : (value, converter) -> {
                Timestamp made = (Timestamp) date.apply(value, converter);
                made.setNanos(((Timestamp) value).getNanos());
                return made;
            };
        });
        makers.put(Calendar.class, type -> maker(type, null, value -> value, (made, value) -> {
            Calendar calendar = (Calendar) made;
            Calendar from = (Calendar) value;
            calendar.setLenient(from.isLenient());
            calendar.setFirstDayOfWeek(from.getFirstDayOfWeek());
            calendar.setMinimalDaysInFirstWeek(from.getMinimalDaysInFirstWeek());
            if (calendar instanceof GregorianCalendar && from instanceof GregorianCalendar) {
                ((GregorianCalendar) calendar).setGregorianChange(((GregorianCalendar) from).getGregorianChange());
            }
            calendar.setTimeZone(from.getTimeZone());
            calendar.setTimeInMillis(from.getTimeInMillis());
        }));
        makers.put(AtomicLong.class, type -> maker(type, long.class, value -> ((AtomicLong) value).get(),
                (made, v) -> ((AtomicLong) made).set((Long) v)));
        makers.put(AtomicInteger.class, type -> maker(type, int.class, value -> ((AtomicInteger) value).get(),
                (made, v) -> ((AtomicInteger) made).set((Integer) v)));
        makers.put(AtomicBoolean.class, type -> maker(type, boolean.class, value -> ((AtomicBoolean) value).get(),
                (made, v) -> ((AtomicBoolean) made).set((Boolean) v)));
        makers.put(BitSet.class, type -> maker(type, null, value -> value,
                (made, value) -> ((BitSet) made).or((BitSet) value)));
        makers.put(Map.class, Converter::mapMaker);
        // Immutable: made only by the constructor the JDK type itself has, from the text that reads back exactly
        makers.put(BigDecimal.class, type -> constructedMaker(type, String.class, Object::toString));
        makers.put(BigInteger.class, type -> constructedMaker(type, String.class, Object::toString));
        makers.put(File.class, type -> constructedMaker(type, String.class, Object::toString));
        // Types whose conversions pick the class of the result from the value (a zone ID gives a ZoneInfo, text an
        // Inet4Address or an Inet6Address, a number its own type): a subclass asked for is converted to only when
        // the result is one. A SimpleTimeZone cannot follow a zone's rules, nor a LongAccumulator be rebuilt.
        makers.put(Number.class, Converter::onlyWhenAlready);
        makers.put(TimeZone.class, Converter::onlyWhenAlready);
        makers.put(InetAddress.class, Converter::onlyWhenAlready);
        makers.put(Buffer.class, Converter::onlyWhenAlready);
        return makers;
    }

    /**
     * For a type no value can be turned into: a result that is not one already is no conversion. A class that is not
     * public (the JDK's HeapCharBuffer, which json-io records as a buffer's class) cannot be what a caller must have,
     * since no caller can name it: it gets the conversion's result, the closest known type, as a JDK collection
     * class no factory makes does.
     */
    private static BiFunction<Object, Converter, Object> onlyWhenAlready(Class<?> type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return (value, converter) -> value;
        }
        return (value, converter) -> {
            throw new IllegalArgumentException("Unsupported conversion to " + type.getName()
                    + ": the value converts to a " + value.getClass().getName() + ", and a " + type.getName()
                    + " cannot be made from it");
        };
    }

    /**
     * Makes a subclass of an immutable type, whose state cannot be set after construction, with its constructor
     * taking {@code stateType} (the one the JDK type has: BigDecimal(String)), given {@code state} of the value. Null
     * when it has none.
     */
    private static BiFunction<Object, Converter, Object> constructedMaker(Class<?> type, Class<?> stateType,
                                                                          Function<Object, Object> state) {
        Constructor<?> ofState = constructor(type, stateType);
        return ofState == null ? null : (value, converter) -> instantiate(ofState, state.apply(value));
    }

    /**
     * Makes a subclass from its supertype's value: with its no-argument constructor and {@code setState}, else with
     * its constructor taking {@code stateType} (the one the JDK type has: Date(long), AtomicLong(long); null when
     * there is none to use), given {@code state} of the value. Null when it has neither constructor.
     */
    private static BiFunction<Object, Converter, Object> maker(Class<?> type, Class<?> stateType,
                                                               Function<Object, Object> state,
                                                               BiConsumer<Object, Object> setState) {
        Constructor<?> noArg = constructor(type);
        Constructor<?> ofState = noArg == null && stateType != null ? constructor(type, stateType) : null;
        if (noArg == null && ofState == null) {
            return null;
        }
        return (value, converter) -> {
            if (noArg == null) {
                return instantiate(ofState, state.apply(value));
            }
            Object made = instantiate(noArg);
            setState.accept(made, state.apply(value));
            return made;
        };
    }

    // Performance: All per-(source, target) lookups use ClassPairMap: a ClassValue read on one class of the pair and
    // an identity-hash probe for the other. No ConversionPair is allocated per lookup.
    private static final ClassPairMap<InheritancePair[]> cacheInheritancePairs = new ClassPairMap<>();
    private static final ClassPairMap<Convert<?>> CONVERSION_DB = new ClassPairMap<>();
    private final ClassPairMap<Convert<?>> USER_DB = new ClassPairMap<>(true);     // keeps every conversion added
    private static final ClassPairMap<Convert<?>> FULL_CONVERSION_CACHE = new ClassPairMap<>();
    private static final AtomicLong INSTANCE_ID_GENERATOR = new AtomicLong(1);

    // Identity converter for marking non-standard types and handling identity conversions
    private static final Convert<?> IDENTITY_CONVERTER = (source, converter) -> source;

    private final ConverterOptions options;
    private final long instanceId;
    private volatile boolean hasUserConversions;

    // Efficient key that combines two Class instances and instance ID for fast creation and lookup
    public static final class ConversionPair {
        private final Class<?> source;
        private final Class<?> target;
        private final long instanceId; // Unique instance identifier
        private final int hash;

        private ConversionPair(Class<?> source, Class<?> target, long instanceId) {
            this.source = source;
            this.target = target;
            this.instanceId = instanceId;
            // Combine class hash codes with instance ID
            this.hash = 31 * (31 * source.hashCode() + target.hashCode()) + Long.hashCode(instanceId);
        }

        public Class<?> getSource() {
            return source;
        }

        public Class<?> getTarget() {
            return target;
        }

        public long getInstanceId() {
            return instanceId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConversionPair)) {
                return false;
            }
            ConversionPair other = (ConversionPair) obj;
            return source == other.source && target == other.target && instanceId == other.instanceId;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // Helper method to create a conversion pair key with instance ID context.
    // Retained as public API: user code registers custom conversions via
    // ConverterOptions.getConverterOverrides() which returns Map<ConversionPair, Convert<?>>.
    // Internally, Converter no longer uses ConversionPair as a map key — see ClassPairMap below.
    public static ConversionPair pair(Class<?> source, Class<?> target, long instanceId) {
        return new ConversionPair(source, target, instanceId);
    }

    // Helper method for static contexts that don't have instance context (legacy support)
    public static ConversionPair pair(Class<?> source, Class<?> target) {
        return new ConversionPair(source, target, 0); // Use 0 for static/shared conversions
    }

    /**
     * A map keyed by (source, target) class pairs: no allocation per lookup, and it never keeps a class -- or its
     * ClassLoader -- alive. An entry is kept on one of its two classes, in a {@link ClassValue}, and on the right one:
     * an entry refers to both classes (an {@code InheritancePair[]} lists their supertypes), so kept on a class that
     * outlives the other it would keep that other class alive (a JDK class, say, holding an application's):
     * <ul>
     *   <li>Both classes of one ClassLoader (JDK to JDK, the common case), or the source's loader the deeper: kept on
     *       the source, keyed by the target.</li>
     *   <li>The target's loader the deeper (a JDK class converted to an application's): kept on the target, keyed by
     *       the source, which lives at least as long.</li>
     *   <li>Neither loader an ancestor of the other: not kept. A get() misses, and a put() keeps nothing.</li>
     * </ul>
     * <p>A class's entries are one small open-addressed table, probed by the other class's identity hash and at most
     * half full, held in an {@code AtomicReference} (a JDK type). A write replaces the table whole, under the map's
     * lock: each cache fills once, and the tables are small. A lookup is a ClassValue read, a volatile read and a
     * probe. (A ClassValueMap per class, as the inner level, attached a slot of every source's map to each target
     * class, which crowded the JDK's small per-class ClassValue cache.) The classes holding entries are listed weakly,
     * for {@link #forEach(TriConsumer)} and {@link #removeIf(java.util.function.BiPredicate)} (bridge expansion and
     * cache invalidation). A null class is a key like any other, of the same loader as every class.</p>
     */
    static final class ClassPairMap<V> {
        private static final Object[] EMPTY = new Object[2];    // one free slot, so every probe of it misses
        // Each made on its first write (a Converter builds a ClassPairMap per instance, and most stay empty):
        private volatile Side bySource;         // source -> (target -> value)
        private volatile Side byTarget;         // target -> (source -> value): the target's loader is the deeper
        // Pairs whose loaders are unrelated, source -> (target -> value), held by this map itself: for a map that must
        // not lose an entry -- a Converter's own conversions, which live as long as the Converter does. Null for a
        // cache, which simply misses such a pair. Written under the map's lock; read without it.
        private final Map<Class<?>, Map<Class<?>, Object>> unrelated;

        /** A cache: a pair of classes whose loaders are unrelated is not kept (a get() misses). */
        ClassPairMap() {
            this(false);
        }

        /**
         * @param keepsUnrelated true for a map whose entries must all be kept (a Converter's own conversions): a pair
         *                       of classes whose loaders are unrelated is then held by the map itself, as long as it is
         */
        ClassPairMap(boolean keepsUnrelated) {
            unrelated = keepsUnrelated ? new ConcurrentHashMap<>() : null;
        }

        private static final int SAME = 0;
        private static final int ON_SOURCE = 1;
        private static final int ON_TARGET = 2;
        private static final int NOT_KEPT = 3;

        /** Stands in for a null class, which a ClassValue cannot take. */
        private static final class NullClass {
        }

        /**
         * Per class, its table: {class, value, class, value, ...}. The classes holding one are listed weakly. This is
         * a ClassValueMap's own slot without its null-key and absent-value checks; a ClassValueMap&lt;Object[]&gt; here
         * measured slower: isConversionSupportedFor 3.4-3.5 ns against 3.1-3.2 on JDK 21, 6.8 against 2.7 on JDK 8.
         */
        private static final class Side extends ClassValue<AtomicReference<Object[]>> {
            final Map<Class<?>, Boolean> owners = new WeakHashMap<>();     // guarded by the map's lock

            @Override
            protected AtomicReference<Object[]> computeValue(Class<?> type) {
                return new AtomicReference<>(EMPTY);
            }
        }

        /** Where the (source, target) entry is kept. A null class (the null key) counts as the same loader. */
        private static int placement(Class<?> source, Class<?> target) {
            if (source == null || target == null) {
                return SAME;
            }
            ClassLoader sourceLoader = source.getClassLoader();
            ClassLoader targetLoader = target.getClassLoader();
            if (sourceLoader == targetLoader) {
                return SAME;
            }
            if (isAncestor(targetLoader, sourceLoader)) {
                return ON_SOURCE;
            }
            return isAncestor(sourceLoader, targetLoader) ? ON_TARGET : NOT_KEPT;
        }

        /**
         * True if {@code ancestor} is {@code loader} or one of its parents (null, the bootstrap loader, is everyone's).
         */
        private static boolean isAncestor(ClassLoader ancestor, ClassLoader loader) {
            if (ancestor == null) {
                return true;
            }
            try {
                for (ClassLoader l = loader; l != null; l = l.getParent()) {
                    if (l == ancestor) {
                        return true;
                    }
                }
            } catch (SecurityException e) {
                // cannot tell: treat as unrelated
            }
            return false;
        }

        private static Class<?> key(Class<?> type) {
            return type == null ? NullClass.class : type;
        }

        private static Class<?> unkey(Class<?> type) {
            return type == NullClass.class ? null : type;
        }

        /**
         * Looks up a value by (source, target). Zero allocation. Returns {@code null} if absent.
         */
        @SuppressWarnings("unchecked")
        V get(Class<?> source, Class<?> target) {
            // A pair is always kept in the same place, so a hit on the source settles it without comparing loaders:
            // the common case (both classes of one loader) is one ClassValue read and a probe
            Side sources = bySource;
            if (sources != null) {
                Object value = find(sources.get(key(source)).get(), key(target));
                if (value != null) {
                    return (V) value;
                }
            }
            Side targets = byTarget;
            if (targets != null && placement(source, target) == ON_TARGET) {
                return (V) find(targets.get(target).get(), source);
            }
            Map<Class<?>, Map<Class<?>, Object>> others = unrelated;
            if (others != null && !others.isEmpty()) {
                Map<Class<?>, Object> row = others.get(key(source));
                if (row != null) {
                    return (V) row.get(key(target));
                }
            }
            return null;
        }

        /**
         * Stores a value under (source, target). When the two classes' loaders are unrelated, kept by the map itself
         * if it keeps such pairs, else nowhere.
         */
        V put(Class<?> source, Class<?> target, V value) {
            return write(source, target, value, false);
        }

        /**
         * Atomic putIfAbsent on the (source, target) slot. When the two classes' loaders are unrelated, kept by the map
         * itself if it keeps such pairs, else nowhere.
         */
        V putIfAbsent(Class<?> source, Class<?> target, V value) {
            return write(source, target, value, true);
        }

        @SuppressWarnings("unchecked")
        private synchronized V write(Class<?> source, Class<?> target, V value, boolean ifAbsent) {
            int where = placement(source, target);
            if (where == NOT_KEPT) {
                if (unrelated == null) {
                    return null;
                }
                Map<Class<?>, Object> row = unrelated.computeIfAbsent(key(source), k -> new ConcurrentHashMap<>());
                V previous = (V) row.get(key(target));
                if (!ifAbsent || previous == null) {        // an entry holding null counts as absent
                    if (value == null) {
                        row.remove(key(target));
                    } else {
                        row.put(key(target), value);
                    }
                }
                return previous;
            }
            Side side;
            Class<?> owner;
            Class<?> other;
            if (where == ON_TARGET) {
                if (byTarget == null) {
                    byTarget = new Side();
                }
                side = byTarget;
                owner = target;
                other = source;
            } else {
                if (bySource == null) {
                    bySource = new Side();
                }
                side = bySource;
                owner = key(source);
                other = key(target);
            }
            AtomicReference<Object[]> slot = side.get(owner);
            Object[] table = slot.get();
            int i = indexOf(table, other);
            if (i < 0) {
                slot.set(rebuilt(table, null, other, value));
                side.owners.put(owner, Boolean.TRUE);
                return null;
            }
            V previous = (V) table[i + 1];
            if (!ifAbsent || previous == null) {        // an entry holding null counts as absent
                Object[] copy = table.clone();
                copy[i + 1] = value;
                slot.set(copy);
            }
            return previous;
        }

        /**
         * Removes the value at (source, target). Returns the previous value or {@code null}.
         */
        @SuppressWarnings("unchecked")
        synchronized V remove(Class<?> source, Class<?> target) {
            int where = placement(source, target);
            if (where == NOT_KEPT) {
                Map<Class<?>, Object> row = unrelated == null ? null : unrelated.get(key(source));
                return row == null ? null : (V) row.remove(key(target));
            }
            Side side = where == ON_TARGET ? byTarget : bySource;
            if (side == null) {
                return null;
            }
            Class<?> owner = where == ON_TARGET ? target : key(source);
            Class<?> other = where == ON_TARGET ? source : key(target);
            AtomicReference<Object[]> slot = side.get(owner);
            Object[] table = slot.get();
            int i = indexOf(table, other);
            if (i < 0) {
                return null;
            }
            boolean[] dropped = new boolean[table.length >> 1];
            dropped[i >> 1] = true;
            replace(side, owner, slot, rebuilt(table, dropped, null, null));
            return (V) table[i + 1];
        }

        /**
         * Returns true if there are no mappings.
         */
        synchronized boolean isEmpty() {
            if (unrelated != null) {
                for (Map<Class<?>, Object> row : unrelated.values()) {
                    if (!row.isEmpty()) {
                        return false;
                    }
                }
            }
            return (bySource == null || bySource.owners.isEmpty()) && (byTarget == null || byTarget.owners.isEmpty());
        }

        /**
         * Returns the total number of (source, target) entries.
         * O(n) where n is the number of classes holding entries — not called on the hot path.
         */
        int size() {
            int[] total = {0};
            forEach((source, target, value) -> total[0]++);
            return total[0];
        }

        /**
         * Iterates over every (source, target, value) entry, invoking the action for each. It walks the tables as
         * they were when it began, so the action may write to this map.
         */
        @SuppressWarnings("unchecked")
        void forEach(TriConsumer<Class<?>, Class<?>, V> action) {
            List<Class<?>> owners = new ArrayList<>();
            List<Object[]> tables = new ArrayList<>();
            int onTargets;
            List<Object[]> others = new ArrayList<>();      // {source, target, value}
            synchronized (this) {
                collect(bySource, owners, tables);
                onTargets = owners.size();
                collect(byTarget, owners, tables);
                if (unrelated != null) {
                    for (Map.Entry<Class<?>, Map<Class<?>, Object>> row : unrelated.entrySet()) {
                        for (Map.Entry<Class<?>, Object> cell : row.getValue().entrySet()) {
                            others.add(new Object[]{row.getKey(), cell.getKey(), cell.getValue()});
                        }
                    }
                }
            }
            for (Object[] other : others) {
                action.accept(unkey((Class<?>) other[0]), unkey((Class<?>) other[1]), (V) other[2]);
            }
            for (int n = 0; n < owners.size(); n++) {
                Class<?> owner = unkey(owners.get(n));
                Object[] table = tables.get(n);
                for (int i = 0; i < table.length; i += 2) {
                    if (table[i] != null) {
                        Class<?> other = unkey((Class<?>) table[i]);
                        if (n < onTargets) {
                            action.accept(owner, other, (V) table[i + 1]);
                        } else {
                            action.accept(other, owner, (V) table[i + 1]);
                        }
                    }
                }
            }
        }

        /**
         * Removes every (source, target) pair for which the predicate returns true.
         * Safe to call during cache invalidation; does not affect ongoing reads of
         * unrelated entries.
         */
        synchronized void removeIf(java.util.function.BiPredicate<Class<?>, Class<?>> predicate) {
            removeIf(bySource, false, predicate);
            removeIf(byTarget, true, predicate);
            if (unrelated != null) {
                for (Map.Entry<Class<?>, Map<Class<?>, Object>> row : unrelated.entrySet()) {
                    Class<?> source = unkey(row.getKey());
                    row.getValue().keySet().removeIf(target -> predicate.test(source, unkey(target)));
                }
            }
        }

        private static void removeIf(Side side, boolean onTarget,
                                     java.util.function.BiPredicate<Class<?>, Class<?>> predicate) {
            if (side == null) {
                return;
            }
            for (Class<?> owner : new ArrayList<>(side.owners.keySet())) {
                AtomicReference<Object[]> slot = side.get(owner);
                Object[] table = slot.get();
                boolean[] dropped = null;
                for (int i = 0; i < table.length; i += 2) {
                    if (table[i] == null) {
                        continue;
                    }
                    Class<?> other = unkey((Class<?>) table[i]);
                    if (onTarget ? predicate.test(other, owner) : predicate.test(unkey(owner), other)) {
                        if (dropped == null) {
                            dropped = new boolean[table.length >> 1];
                        }
                        dropped[i >> 1] = true;
                    }
                }
                if (dropped != null) {
                    replace(side, owner, slot, rebuilt(table, dropped, null, null));
                }
            }
        }

        private static void collect(Side side, List<Class<?>> owners, List<Object[]> tables) {
            if (side != null) {
                for (Class<?> owner : side.owners.keySet()) {
                    owners.add(owner);
                    tables.add(side.get(owner).get());
                }
            }
        }

        /** Publishes the owner's new table; an owner left with none is no longer listed. */
        private static void replace(Side side, Class<?> owner, AtomicReference<Object[]> slot, Object[] table) {
            slot.set(table);
            if (table == EMPTY) {
                side.owners.remove(owner);
            }
        }

        /** The value kept under {@code key} in the table, or null. */
        private static Object find(Object[] table, Class<?> key) {
            int mask = table.length - 2;
            for (int i = (System.identityHashCode(key) << 1) & mask; ; i = (i + 2) & mask) {
                Object k = table[i];
                if (k == key) {
                    return table[i + 1];
                }
                if (k == null) {
                    return null;
                }
            }
        }

        /** The index of {@code key} in the table, or -1. */
        private static int indexOf(Object[] table, Class<?> key) {
            int mask = table.length - 2;
            for (int i = (System.identityHashCode(key) << 1) & mask; ; i = (i + 2) & mask) {
                Object k = table[i];
                if (k == key) {
                    return i;
                }
                if (k == null) {
                    return -1;
                }
            }
        }

        /**
         * A new table: the entries of {@code table} except those {@code dropped} (by slot; null drops none), plus
         * {@code key -> value} when key is not null. At most half full.
         */
        private static Object[] rebuilt(Object[] table, boolean[] dropped, Class<?> key, Object value) {
            int count = key == null ? 0 : 1;
            for (int i = 0; i < table.length; i += 2) {
                if (table[i] != null && (dropped == null || !dropped[i >> 1])) {
                    count++;
                }
            }
            if (count == 0) {
                return EMPTY;
            }
            int length = 8;
            while (length < count << 2) {
                length <<= 1;
            }
            Object[] fresh = new Object[length];
            for (int i = 0; i < table.length; i += 2) {
                if (table[i] != null && (dropped == null || !dropped[i >> 1])) {
                    insert(fresh, table[i], table[i + 1]);
                }
            }
            if (key != null) {
                insert(fresh, key, value);
            }
            return fresh;
        }

        private static void insert(Object[] table, Object key, Object value) {
            int mask = table.length - 2;
            int i = (System.identityHashCode(key) << 1) & mask;
            while (table[i] != null) {
                i = (i + 2) & mask;
            }
            table[i] = key;
            table[i + 1] = value;
        }
    }

    /**
     * Triple-argument consumer used by {@link ClassPairMap#forEach(TriConsumer)}.
     */
    @FunctionalInterface
    interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    static {
        buildFactoryConversions();

    }

    /**
     * Retrieves the converter options associated with this Converter instance.
     *
     * @return The {@link ConverterOptions} used by this Converter.
     */
    public ConverterOptions getOptions() {
        return options;
    }

    /**
     * Initializes the built-in conversion mappings within the Converter.
     * <p>
     * This method populates the {@link #CONVERSION_DB} with a comprehensive set of predefined conversion functions
     * that handle a wide range of type transformations, including primitives, wrappers, numbers, dates, times,
     * collections, and more.
     * </p>
     * <p>
     * These conversions serve as the foundational capabilities of the Converter, enabling it to perform most
     * common type transformations out-of-the-box. Users can extend or override these conversions using the
     * {@link #addConversion(Class, Class, Convert)} method as needed.
     * </p>
     */
    private static void buildFactoryConversions() {
        // toNumber
        CONVERSION_DB.put(Byte.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Short.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Integer.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Long.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Float.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Double.class, Number.class, Converter::identity);
        CONVERSION_DB.put(AtomicInteger.class, Number.class, Converter::identity);
        CONVERSION_DB.put(AtomicLong.class, Number.class, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, Number.class, Converter::identity);
        CONVERSION_DB.put(BigDecimal.class, Number.class, Converter::identity);
        CONVERSION_DB.put(Duration.class, Number.class, DurationConversions::toNumber);
        CONVERSION_DB.put(String.class, Number.class, StringConversions::toNumber);

        // toByte
        CONVERSION_DB.put(Void.class, byte.class, NumberConversions::toByteZero);
        CONVERSION_DB.put(Void.class, Byte.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Byte.class, Converter::identity);
        CONVERSION_DB.put(Short.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Integer.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Long.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Float.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Double.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Boolean.class, Byte.class, BooleanConversions::toByte);
        CONVERSION_DB.put(Character.class, Byte.class, CharacterConversions::toByte);
        CONVERSION_DB.put(BigInteger.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(BigDecimal.class, Byte.class, NumberConversions::toByte);
        CONVERSION_DB.put(Map.class, Byte.class, MapConversions::toByte);
        CONVERSION_DB.put(String.class, Byte.class, StringConversions::toByte);

        // toShort
        CONVERSION_DB.put(Void.class, short.class, NumberConversions::toShortZero);
        CONVERSION_DB.put(Void.class, Short.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Short.class, Short.class, Converter::identity);
        CONVERSION_DB.put(Integer.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Long.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Float.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Double.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Boolean.class, Short.class, BooleanConversions::toShort);
        CONVERSION_DB.put(Character.class, Short.class, CharacterConversions::toShort);
        CONVERSION_DB.put(BigInteger.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(BigDecimal.class, Short.class, NumberConversions::toShort);
        CONVERSION_DB.put(Map.class, Short.class, MapConversions::toShort);
        CONVERSION_DB.put(String.class, Short.class, StringConversions::toShort);
        CONVERSION_DB.put(Year.class, Short.class, YearConversions::toShort);
        CONVERSION_DB.put(MonthDay.class, Short.class, MonthDayConversions::toShort);
        CONVERSION_DB.put(YearMonth.class, Short.class, YearMonthConversions::toShort);

        // toInteger
        CONVERSION_DB.put(Void.class, int.class, NumberConversions::toIntZero);
        CONVERSION_DB.put(AtomicInteger.class, int.class, UniversalConversions::atomicIntegerToInt);
        CONVERSION_DB.put(Void.class, Integer.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Short.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Integer.class, Integer.class, Converter::identity);
        CONVERSION_DB.put(Long.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Float.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Double.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Boolean.class, Integer.class, BooleanConversions::toInt);
        CONVERSION_DB.put(Character.class, Integer.class, CharacterConversions::toInt);
        CONVERSION_DB.put(AtomicInteger.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(BigInteger.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(BigDecimal.class, Integer.class, NumberConversions::toInt);
        CONVERSION_DB.put(Map.class, Integer.class, MapConversions::toInt);
        CONVERSION_DB.put(String.class, Integer.class, StringConversions::toInt);
        CONVERSION_DB.put(Color.class, Integer.class, ColorConversions::toInteger);
        CONVERSION_DB.put(Year.class, Integer.class, YearConversions::toInt);
        CONVERSION_DB.put(MonthDay.class, Integer.class, MonthDayConversions::toInt);
        CONVERSION_DB.put(YearMonth.class, Integer.class, YearMonthConversions::toInt);

        // toLong
        CONVERSION_DB.put(Void.class, long.class, NumberConversions::toLongZero);
        CONVERSION_DB.put(AtomicLong.class, long.class, UniversalConversions::atomicLongToLong);
        CONVERSION_DB.put(Void.class, Long.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Short.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Integer.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Long.class, Long.class, Converter::identity);
        CONVERSION_DB.put(Float.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Double.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Boolean.class, Long.class, BooleanConversions::toLong);
        CONVERSION_DB.put(Character.class, Long.class, CharacterConversions::toLong);
        CONVERSION_DB.put(BigInteger.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(BigDecimal.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(AtomicLong.class, Long.class, NumberConversions::toLong);
        CONVERSION_DB.put(Date.class, Long.class, DateConversions::toLong);
        CONVERSION_DB.put(java.sql.Date.class, Long.class, SqlDateConversions::toLong);
        CONVERSION_DB.put(Timestamp.class, Long.class, TimestampConversions::toLong);
        CONVERSION_DB.put(Instant.class, Long.class, InstantConversions::toLong);
        CONVERSION_DB.put(Duration.class, Long.class, DurationConversions::toLong);
        CONVERSION_DB.put(LocalTime.class, Long.class, LocalTimeConversions::toLong);
        CONVERSION_DB.put(OffsetTime.class, Long.class, OffsetTimeConversions::toLong);
        CONVERSION_DB.put(OffsetDateTime.class, Long.class, OffsetDateTimeConversions::toLong);
        CONVERSION_DB.put(ZonedDateTime.class, Long.class, ZonedDateTimeConversions::toLong);
        CONVERSION_DB.put(Map.class, Long.class, MapConversions::toLong);
        CONVERSION_DB.put(String.class, Long.class, StringConversions::toLong);
        CONVERSION_DB.put(Color.class, Long.class, ColorConversions::toLong);
        CONVERSION_DB.put(Year.class, Long.class, YearConversions::toLong);
        CONVERSION_DB.put(MonthDay.class, Long.class, MonthDayConversions::toLong);
        CONVERSION_DB.put(YearMonth.class, Long.class, YearMonthConversions::toLong);

        // toFloat
        CONVERSION_DB.put(Void.class, float.class, NumberConversions::toFloatZero);
        CONVERSION_DB.put(Void.class, Float.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Short.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Integer.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Long.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Float.class, Float.class, Converter::identity);
        CONVERSION_DB.put(Double.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Boolean.class, Float.class, BooleanConversions::toFloat);
        CONVERSION_DB.put(Character.class, Float.class, CharacterConversions::toFloat);
        CONVERSION_DB.put(BigInteger.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(BigDecimal.class, Float.class, NumberConversions::toFloat);
        CONVERSION_DB.put(Map.class, Float.class, MapConversions::toFloat);
        CONVERSION_DB.put(String.class, Float.class, StringConversions::toFloat);
        CONVERSION_DB.put(Year.class, Float.class, YearConversions::toFloat);
        CONVERSION_DB.put(MonthDay.class, Float.class, MonthDayConversions::toFloat);
        CONVERSION_DB.put(YearMonth.class, Float.class, YearMonthConversions::toFloat);

        // toDouble
        CONVERSION_DB.put(Void.class, double.class, NumberConversions::toDoubleZero);
        CONVERSION_DB.put(Year.class, double.class, YearConversions::toDouble);
        CONVERSION_DB.put(Void.class, Double.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(Short.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(Integer.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(Long.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(Float.class, Double.class, NumberConversions::floatToDouble);
        CONVERSION_DB.put(Double.class, Double.class, Converter::identity);
        CONVERSION_DB.put(Boolean.class, Double.class, BooleanConversions::toDouble);
        CONVERSION_DB.put(Character.class, Double.class, CharacterConversions::toDouble);
        CONVERSION_DB.put(Duration.class, Double.class, DurationConversions::toDouble);
        CONVERSION_DB.put(Instant.class, Double.class, InstantConversions::toDouble);
        CONVERSION_DB.put(LocalTime.class, Double.class, LocalTimeConversions::toDouble);
        CONVERSION_DB.put(ZonedDateTime.class, Double.class, ZonedDateTimeConversions::toDouble);
        CONVERSION_DB.put(OffsetTime.class, Double.class, OffsetTimeConversions::toDouble);
        CONVERSION_DB.put(OffsetDateTime.class, Double.class, OffsetDateTimeConversions::toDouble);
        CONVERSION_DB.put(Date.class, Double.class, DateConversions::toDouble);
        CONVERSION_DB.put(java.sql.Date.class, Double.class, SqlDateConversions::toDouble);
        CONVERSION_DB.put(Timestamp.class, Double.class, TimestampConversions::toDouble);
        CONVERSION_DB.put(BigInteger.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(BigDecimal.class, Double.class, NumberConversions::toDouble);
        CONVERSION_DB.put(Map.class, Double.class, MapConversions::toDouble);
        CONVERSION_DB.put(String.class, Double.class, StringConversions::toDouble);
        CONVERSION_DB.put(Year.class, Double.class, YearConversions::toDouble);
        CONVERSION_DB.put(MonthDay.class, Double.class, MonthDayConversions::toDouble);
        CONVERSION_DB.put(YearMonth.class, Double.class, YearMonthConversions::toDouble);

        // Boolean/boolean conversions supported
        CONVERSION_DB.put(Void.class, boolean.class, VoidConversions::toBoolean);
        CONVERSION_DB.put(AtomicBoolean.class, boolean.class, UniversalConversions::atomicBooleanToBoolean);
        CONVERSION_DB.put(Duration.class, boolean.class, DurationConversions::toBoolean);
        CONVERSION_DB.put(AtomicBoolean.class, Boolean.class, AtomicBooleanConversions::toBoolean);
        CONVERSION_DB.put(Duration.class, Boolean.class, DurationConversions::toBooleanWrapper);
        CONVERSION_DB.put(Void.class, Boolean.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Boolean.class, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Short.class, Boolean.class, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Integer.class, Boolean.class, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Long.class, Boolean.class, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Float.class, Boolean.class, NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(Double.class, Boolean.class, NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(Boolean.class, Boolean.class, Converter::identity);
        CONVERSION_DB.put(Character.class, Boolean.class, CharacterConversions::toBoolean);
        CONVERSION_DB.put(BigInteger.class, Boolean.class, NumberConversions::isBigIntegerNotZero);
        CONVERSION_DB.put(BigDecimal.class, Boolean.class, NumberConversions::isBigDecimalNotZero);
        CONVERSION_DB.put(Map.class, Boolean.class, MapConversions::toBoolean);
        CONVERSION_DB.put(String.class, Boolean.class, StringConversions::toBoolean);
        CONVERSION_DB.put(Dimension.class, Boolean.class, DimensionConversions::toBoolean);
        CONVERSION_DB.put(Point.class, Boolean.class, PointConversions::toBoolean);
        CONVERSION_DB.put(Rectangle.class, Boolean.class, RectangleConversions::toBoolean);
        CONVERSION_DB.put(Insets.class, Boolean.class, InsetsConversions::toBoolean);
        CONVERSION_DB.put(UUID.class, Boolean.class, UUIDConversions::toBoolean);

        // Character/char conversions supported
        CONVERSION_DB.put(Void.class, char.class, VoidConversions::toCharacter);
        CONVERSION_DB.put(Void.class, Character.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Character.class, ByteConversions::toCharacter);
        CONVERSION_DB.put(Short.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Integer.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Long.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Float.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Double.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Boolean.class, Character.class, BooleanConversions::toCharacter);
        CONVERSION_DB.put(Character.class, Character.class, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(BigDecimal.class, Character.class, NumberConversions::toCharacter);
        CONVERSION_DB.put(Map.class, Character.class, MapConversions::toCharacter);
        CONVERSION_DB.put(String.class, Character.class, StringConversions::toCharacter);

        // BigInteger versions supported
        CONVERSION_DB.put(Void.class, BigInteger.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, BigInteger.class, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Short.class, BigInteger.class, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Integer.class, BigInteger.class, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Long.class, BigInteger.class, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Float.class, BigInteger.class, NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(Double.class, BigInteger.class, NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(Boolean.class, BigInteger.class, BooleanConversions::toBigInteger);
        CONVERSION_DB.put(Character.class, BigInteger.class, CharacterConversions::toBigInteger);
        CONVERSION_DB.put(BigInteger.class, BigInteger.class, Converter::identity);
        CONVERSION_DB.put(BigDecimal.class, BigInteger.class, BigDecimalConversions::toBigInteger);
        CONVERSION_DB.put(Date.class, BigInteger.class, DateConversions::toBigInteger);
        CONVERSION_DB.put(java.sql.Date.class, BigInteger.class, SqlDateConversions::toBigInteger);
        CONVERSION_DB.put(Timestamp.class, BigInteger.class, TimestampConversions::toBigInteger);
        CONVERSION_DB.put(Duration.class, BigInteger.class, DurationConversions::toBigInteger);
        CONVERSION_DB.put(Instant.class, BigInteger.class, InstantConversions::toBigInteger);
        CONVERSION_DB.put(LocalTime.class, BigInteger.class, LocalTimeConversions::toBigInteger);
        CONVERSION_DB.put(ZonedDateTime.class, BigInteger.class, ZonedDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(OffsetTime.class, BigInteger.class, OffsetTimeConversions::toBigInteger);
        CONVERSION_DB.put(OffsetDateTime.class, BigInteger.class, OffsetDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(UUID.class, BigInteger.class, UUIDConversions::toBigInteger);
        CONVERSION_DB.put(Calendar.class, BigInteger.class, CalendarConversions::toBigInteger);  // Restored - bridge has precision difference (millis vs. nanos)
        CONVERSION_DB.put(Map.class, BigInteger.class, MapConversions::toBigInteger);
        CONVERSION_DB.put(String.class, BigInteger.class, StringConversions::toBigInteger);
        CONVERSION_DB.put(Year.class, BigInteger.class, YearConversions::toBigInteger);
        CONVERSION_DB.put(MonthDay.class, BigInteger.class, MonthDayConversions::toBigInteger);
        CONVERSION_DB.put(YearMonth.class, BigInteger.class, YearMonthConversions::toBigInteger);

        // BigDecimal conversions supported
        CONVERSION_DB.put(Void.class, BigDecimal.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, BigDecimal.class, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Short.class, BigDecimal.class, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Integer.class, BigDecimal.class, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Long.class, BigDecimal.class, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Float.class, BigDecimal.class, NumberConversions::floatToBigDecimal);
        CONVERSION_DB.put(Double.class, BigDecimal.class, NumberConversions::floatingPointToBigDecimal);
        CONVERSION_DB.put(Boolean.class, BigDecimal.class, BooleanConversions::toBigDecimal);
        CONVERSION_DB.put(Character.class, BigDecimal.class, CharacterConversions::toBigDecimal);
        CONVERSION_DB.put(BigDecimal.class, BigDecimal.class, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, BigDecimal.class, BigIntegerConversions::toBigDecimal);
        CONVERSION_DB.put(Date.class, BigDecimal.class, DateConversions::toBigDecimal);
        CONVERSION_DB.put(java.sql.Date.class, BigDecimal.class, SqlDateConversions::toBigDecimal);
        CONVERSION_DB.put(Timestamp.class, BigDecimal.class, TimestampConversions::toBigDecimal);
        CONVERSION_DB.put(Instant.class, BigDecimal.class, InstantConversions::toBigDecimal);
        CONVERSION_DB.put(Duration.class, BigDecimal.class, DurationConversions::toBigDecimal);
        CONVERSION_DB.put(LocalTime.class, BigDecimal.class, LocalTimeConversions::toBigDecimal);
        CONVERSION_DB.put(ZonedDateTime.class, BigDecimal.class, ZonedDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(OffsetTime.class, BigDecimal.class, OffsetTimeConversions::toBigDecimal);
        CONVERSION_DB.put(OffsetDateTime.class, BigDecimal.class, OffsetDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(UUID.class, BigDecimal.class, UUIDConversions::toBigDecimal);
        CONVERSION_DB.put(Color.class, BigDecimal.class, ColorConversions::toBigDecimal);
        CONVERSION_DB.put(Calendar.class, BigDecimal.class, CalendarConversions::toBigDecimal);
        CONVERSION_DB.put(Map.class, BigDecimal.class, MapConversions::toBigDecimal);
        CONVERSION_DB.put(Map.class, BitSet.class, MapConversions::toBitSet);
        CONVERSION_DB.put(String.class, BigDecimal.class, StringConversions::toBigDecimal);
        CONVERSION_DB.put(Year.class, BigDecimal.class, YearConversions::toBigDecimal);
        CONVERSION_DB.put(MonthDay.class, BigDecimal.class, MonthDayConversions::toBigDecimal);
        CONVERSION_DB.put(YearMonth.class, BigDecimal.class, YearMonthConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        // Most X → AtomicBoolean handled by surrogate system via X → Boolean → AtomicBoolean
        CONVERSION_DB.put(Void.class, AtomicBoolean.class, VoidConversions::toNull);
        CONVERSION_DB.put(Boolean.class, AtomicBoolean.class, BooleanConversions::toAtomicBoolean);  // Bridge
        CONVERSION_DB.put(AtomicBoolean.class, AtomicBoolean.class, AtomicBooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(Year.class, AtomicBoolean.class, YearConversions::toAtomicBoolean);  // No Year → Boolean
        CONVERSION_DB.put(Map.class, AtomicBoolean.class, MapConversions::toAtomicBoolean);  // Better error messages

        // AtomicInteger conversions supported
        // Most X → AtomicInteger handled by surrogate system via X → Integer → AtomicInteger
        CONVERSION_DB.put(Void.class, AtomicInteger.class, VoidConversions::toNull);
        CONVERSION_DB.put(Integer.class, AtomicInteger.class, NumberConversions::toAtomicInteger);  // Bridge
        CONVERSION_DB.put(AtomicInteger.class, AtomicInteger.class, AtomicIntegerConversions::toAtomicInteger);
        CONVERSION_DB.put(Map.class, AtomicInteger.class, MapConversions::toAtomicInteger);  // Better error messages

        // AtomicLong conversions supported
        // Most X → AtomicLong handled by surrogate system via X → Long → AtomicLong
        CONVERSION_DB.put(Void.class, AtomicLong.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, AtomicLong.class, NumberConversions::toAtomicLong);  // Bridge
        CONVERSION_DB.put(AtomicLong.class, AtomicLong.class, AtomicLongConversions::toAtomicLong);
        // An adder converts to a copy of itself; the rest of their conversions are bridged through Long and Double
        CONVERSION_DB.put(java.util.concurrent.atomic.LongAdder.class, java.util.concurrent.atomic.LongAdder.class,
                (from, converter) -> UniversalConversions.longToLongAdder(
                        UniversalConversions.longAdderToLong(from, converter), converter));
        CONVERSION_DB.put(java.util.concurrent.atomic.DoubleAdder.class, java.util.concurrent.atomic.DoubleAdder.class,
                (from, converter) -> UniversalConversions.doubleToDoubleAdder(
                        UniversalConversions.doubleAdderToDouble(from, converter), converter));
        CONVERSION_DB.put(Map.class, AtomicLong.class, MapConversions::toAtomicLong);  // Better error messages

        // Date conversions supported
        CONVERSION_DB.put(Void.class, Date.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Date.class, NumberConversions::toDate);
        CONVERSION_DB.put(Double.class, Date.class, DoubleConversions::toDate);
        CONVERSION_DB.put(BigInteger.class, Date.class, BigIntegerConversions::toDate);
        CONVERSION_DB.put(BigDecimal.class, Date.class, BigDecimalConversions::toDate);
        CONVERSION_DB.put(Date.class, Date.class, DateConversions::toDate);
        CONVERSION_DB.put(java.sql.Date.class, Date.class, SqlDateConversions::toDate);
        CONVERSION_DB.put(Timestamp.class, Date.class, TimestampConversions::toDate);
        CONVERSION_DB.put(Instant.class, Date.class, InstantConversions::toDate);
        CONVERSION_DB.put(ZonedDateTime.class, Date.class, ZonedDateTimeConversions::toDate);
        CONVERSION_DB.put(OffsetDateTime.class, Date.class, OffsetDateTimeConversions::toDate);
        CONVERSION_DB.put(Duration.class, Date.class, DurationConversions::toDate);
        CONVERSION_DB.put(Map.class, Date.class, MapConversions::toDate);
        CONVERSION_DB.put(String.class, Date.class, StringConversions::toDate);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(Void.class, java.sql.Date.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, java.sql.Date.class, NumberConversions::toSqlDate);
        CONVERSION_DB.put(Double.class, java.sql.Date.class, DoubleConversions::toSqlDate);
        CONVERSION_DB.put(BigInteger.class, java.sql.Date.class, BigIntegerConversions::toSqlDate);
        CONVERSION_DB.put(BigDecimal.class, java.sql.Date.class, BigDecimalConversions::toSqlDate);
        CONVERSION_DB.put(java.sql.Date.class, java.sql.Date.class, SqlDateConversions::toSqlDate);
        CONVERSION_DB.put(Calendar.class, java.sql.Date.class, CalendarConversions::toSqlDate);
        CONVERSION_DB.put(Timestamp.class, java.sql.Date.class, TimestampConversions::toSqlDate);
        CONVERSION_DB.put(Duration.class, java.sql.Date.class, DurationConversions::toSqlDate);
        CONVERSION_DB.put(ZonedDateTime.class, java.sql.Date.class, ZonedDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(Map.class, java.sql.Date.class, MapConversions::toSqlDate);
        CONVERSION_DB.put(String.class, java.sql.Date.class, StringConversions::toSqlDate);

        // java.sql.Time: JDBC's time of day (see SqlTimeConversions). Every other source that reaches Date reaches
        // Time too, through the Date → Time bridge in getPrimaryToSurrogatePairs(); it got a java.util.Date instead.
        CONVERSION_DB.put(Void.class, java.sql.Time.class, VoidConversions::toNull);
        CONVERSION_DB.put(java.sql.Time.class, java.sql.Time.class, SqlTimeConversions::toSqlTime);
        CONVERSION_DB.put(Date.class, java.sql.Time.class, DateConversions::toSqlTime);
        CONVERSION_DB.put(LocalTime.class, java.sql.Time.class, LocalTimeConversions::toSqlTime);
        CONVERSION_DB.put(String.class, java.sql.Time.class, StringConversions::toSqlTime);
        CONVERSION_DB.put(Map.class, java.sql.Time.class, MapConversions::toSqlTime);
        CONVERSION_DB.put(java.sql.Time.class, String.class, SqlTimeConversions::toString);
        CONVERSION_DB.put(java.sql.Time.class, Map.class, SqlTimeConversions::toMap);
        CONVERSION_DB.put(java.sql.Time.class, LocalTime.class, SqlTimeConversions::toLocalTime);

        // Timestamp conversions supported
        CONVERSION_DB.put(Void.class, Timestamp.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Timestamp.class, NumberConversions::toTimestamp);
        CONVERSION_DB.put(Double.class, Timestamp.class, DoubleConversions::toTimestamp);
        CONVERSION_DB.put(BigInteger.class, Timestamp.class, BigIntegerConversions::toTimestamp);
        CONVERSION_DB.put(BigDecimal.class, Timestamp.class, BigDecimalConversions::toTimestamp);
        CONVERSION_DB.put(Timestamp.class, Timestamp.class, TimestampConversions::toTimestamp);
        CONVERSION_DB.put(java.sql.Date.class, Timestamp.class, SqlDateConversions::toTimestamp);
        CONVERSION_DB.put(Date.class, Timestamp.class, DateConversions::toTimestamp);
        CONVERSION_DB.put(Duration.class, Timestamp.class, DurationConversions::toTimestamp);
        CONVERSION_DB.put(Instant.class, Timestamp.class, InstantConversions::toTimestamp);
        CONVERSION_DB.put(ZonedDateTime.class, Timestamp.class, ZonedDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(OffsetDateTime.class, Timestamp.class, OffsetDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(Map.class, Timestamp.class, MapConversions::toTimestamp);
        CONVERSION_DB.put(String.class, Timestamp.class, StringConversions::toTimestamp);

        // Calendar conversions supported
        CONVERSION_DB.put(Void.class, Calendar.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Calendar.class, NumberConversions::toCalendar);
        CONVERSION_DB.put(Double.class, Calendar.class, DoubleConversions::toCalendar);
        CONVERSION_DB.put(BigInteger.class, Calendar.class, BigIntegerConversions::toCalendar);
        CONVERSION_DB.put(BigDecimal.class, Calendar.class, BigDecimalConversions::toCalendar);
        CONVERSION_DB.put(Date.class, Calendar.class, DateConversions::toCalendar);
        CONVERSION_DB.put(java.sql.Date.class, Calendar.class, SqlDateConversions::toCalendar);
        CONVERSION_DB.put(Timestamp.class, Calendar.class, TimestampConversions::toCalendar);
        CONVERSION_DB.put(Instant.class, Calendar.class, InstantConversions::toCalendar);
        CONVERSION_DB.put(LocalTime.class, Calendar.class, LocalTimeConversions::toCalendar);
        CONVERSION_DB.put(ZonedDateTime.class, Calendar.class, ZonedDateTimeConversions::toCalendar);
        CONVERSION_DB.put(OffsetDateTime.class, Calendar.class, OffsetDateTimeConversions::toCalendar);
        CONVERSION_DB.put(Duration.class, Calendar.class, DurationConversions::toCalendar);
        CONVERSION_DB.put(Calendar.class, Calendar.class, CalendarConversions::clone);
        CONVERSION_DB.put(Map.class, Calendar.class, MapConversions::toCalendar);
        CONVERSION_DB.put(String.class, Calendar.class, StringConversions::toCalendar);

        // LocalDate conversions supported
        CONVERSION_DB.put(Void.class, LocalDate.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalDate.class, NumberConversions::toLocalDate);
        CONVERSION_DB.put(Double.class, LocalDate.class, DoubleConversions::toLocalDate);
        CONVERSION_DB.put(BigInteger.class, LocalDate.class, BigIntegerConversions::toLocalDate);
        CONVERSION_DB.put(BigDecimal.class, LocalDate.class, BigDecimalConversions::toLocalDate);
        CONVERSION_DB.put(java.sql.Date.class, LocalDate.class, SqlDateConversions::toLocalDate);
        CONVERSION_DB.put(Calendar.class, LocalDate.class, CalendarConversions::toLocalDate);
        CONVERSION_DB.put(LocalDate.class, LocalDate.class, Converter::identity);
        CONVERSION_DB.put(ZonedDateTime.class, LocalDate.class, ZonedDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(Duration.class, LocalDate.class, DurationConversions::toLocalDate);
        CONVERSION_DB.put(Map.class, LocalDate.class, MapConversions::toLocalDate);
        CONVERSION_DB.put(String.class, LocalDate.class, StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(Void.class, LocalDateTime.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalDateTime.class, NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(Double.class, LocalDateTime.class, DoubleConversions::toLocalDateTime);
        CONVERSION_DB.put(BigInteger.class, LocalDateTime.class, BigIntegerConversions::toLocalDateTime);
        CONVERSION_DB.put(BigDecimal.class, LocalDateTime.class, BigDecimalConversions::toLocalDateTime);
        CONVERSION_DB.put(java.sql.Date.class, LocalDateTime.class, SqlDateConversions::toLocalDateTime);
        CONVERSION_DB.put(Timestamp.class, LocalDateTime.class, TimestampConversions::toLocalDateTime);
        CONVERSION_DB.put(Calendar.class, LocalDateTime.class, CalendarConversions::toLocalDateTime);
        CONVERSION_DB.put(LocalDateTime.class, LocalDateTime.class, Converter::identity);
        CONVERSION_DB.put(ZonedDateTime.class, LocalDateTime.class, ZonedDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(Duration.class, LocalDateTime.class, DurationConversions::toLocalDateTime);
        CONVERSION_DB.put(Map.class, LocalDateTime.class, MapConversions::toLocalDateTime);
        CONVERSION_DB.put(String.class, LocalDateTime.class, StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        CONVERSION_DB.put(Void.class, LocalTime.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalTime.class, NumberConversions::longNanosToLocalTime);
        CONVERSION_DB.put(Double.class, LocalTime.class, DoubleConversions::toLocalTime);
        CONVERSION_DB.put(BigInteger.class, LocalTime.class, BigIntegerConversions::toLocalTime);
        CONVERSION_DB.put(BigDecimal.class, LocalTime.class, BigDecimalConversions::toLocalTime);
        CONVERSION_DB.put(LocalTime.class, LocalTime.class, Converter::identity);
        CONVERSION_DB.put(ZonedDateTime.class, LocalTime.class, ZonedDateTimeConversions::toLocalTime);
        // the Calendar's own zone, as its other conversions: the bridge through ZonedDateTime applies the Converter's
        CONVERSION_DB.put(Calendar.class, LocalTime.class, CalendarConversions::toLocalTime);
        CONVERSION_DB.put(Duration.class, LocalTime.class, DurationConversions::toLocalTime);
        CONVERSION_DB.put(Map.class, LocalTime.class, MapConversions::toLocalTime);
        CONVERSION_DB.put(String.class, LocalTime.class, StringConversions::toLocalTime);
        CONVERSION_DB.put(OffsetTime.class, LocalTime.class, OffsetTimeConversions::toLocalTime);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(Void.class, ZonedDateTime.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, ZonedDateTime.class, NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(Double.class, ZonedDateTime.class, DoubleConversions::toZonedDateTime);
        CONVERSION_DB.put(BigInteger.class, ZonedDateTime.class, BigIntegerConversions::toZonedDateTime);
        CONVERSION_DB.put(BigDecimal.class, ZonedDateTime.class, BigDecimalConversions::toZonedDateTime);
        CONVERSION_DB.put(java.sql.Date.class, ZonedDateTime.class, SqlDateConversions::toZonedDateTime);
        CONVERSION_DB.put(Timestamp.class, ZonedDateTime.class, TimestampConversions::toZonedDateTime);
        CONVERSION_DB.put(Date.class, ZonedDateTime.class, DateConversions::toZonedDateTime);
        CONVERSION_DB.put(Instant.class, ZonedDateTime.class, InstantConversions::toZonedDateTime);
        CONVERSION_DB.put(LocalDate.class, ZonedDateTime.class, LocalDateConversions::toZonedDateTime);
        CONVERSION_DB.put(LocalDateTime.class, ZonedDateTime.class, LocalDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(ZonedDateTime.class, ZonedDateTime.class, Converter::identity);
        CONVERSION_DB.put(OffsetDateTime.class, ZonedDateTime.class, OffsetDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(Calendar.class, ZonedDateTime.class, CalendarConversions::toZonedDateTime);
        CONVERSION_DB.put(Duration.class, ZonedDateTime.class, DurationConversions::toZonedDateTime);
        CONVERSION_DB.put(Map.class, ZonedDateTime.class, MapConversions::toZonedDateTime);
        CONVERSION_DB.put(String.class, ZonedDateTime.class, StringConversions::toZonedDateTime);

        // toOffsetDateTime
        CONVERSION_DB.put(Void.class, OffsetDateTime.class, VoidConversions::toNull);
        CONVERSION_DB.put(OffsetDateTime.class, OffsetDateTime.class, Converter::identity);
        CONVERSION_DB.put(Map.class, OffsetDateTime.class, MapConversions::toOffsetDateTime);
        CONVERSION_DB.put(String.class, OffsetDateTime.class, StringConversions::toOffsetDateTime);
        CONVERSION_DB.put(Long.class, OffsetDateTime.class, NumberConversions::toOffsetDateTime);
        CONVERSION_DB.put(Double.class, OffsetDateTime.class, DoubleConversions::toOffsetDateTime);
        CONVERSION_DB.put(BigInteger.class, OffsetDateTime.class, BigIntegerConversions::toOffsetDateTime);
        CONVERSION_DB.put(BigDecimal.class, OffsetDateTime.class, BigDecimalConversions::toOffsetDateTime);
        CONVERSION_DB.put(java.sql.Date.class, OffsetDateTime.class, SqlDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(Timestamp.class, OffsetDateTime.class, TimestampConversions::toOffsetDateTime);
        CONVERSION_DB.put(ZonedDateTime.class, OffsetDateTime.class, ZonedDateTimeConversions::toOffsetDateTime);
        CONVERSION_DB.put(Duration.class, OffsetDateTime.class, DurationConversions::toOffsetDateTime);

        // toOffsetTime
        CONVERSION_DB.put(Void.class, OffsetTime.class, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, OffsetTime.class, NumberConversions::toOffsetTime);
        CONVERSION_DB.put(Double.class, OffsetTime.class, DoubleConversions::toOffsetTime);
        CONVERSION_DB.put(BigInteger.class, OffsetTime.class, BigIntegerConversions::toOffsetTime);
        CONVERSION_DB.put(BigDecimal.class, OffsetTime.class, BigDecimalConversions::toOffsetTime);
        CONVERSION_DB.put(OffsetTime.class, OffsetTime.class, Converter::identity);
        CONVERSION_DB.put(OffsetDateTime.class, OffsetTime.class, OffsetDateTimeConversions::toOffsetTime);
        CONVERSION_DB.put(Map.class, OffsetTime.class, MapConversions::toOffsetTime);
        CONVERSION_DB.put(String.class, OffsetTime.class, StringConversions::toOffsetTime);
        CONVERSION_DB.put(LocalTime.class, OffsetTime.class, LocalTimeConversions::toOffsetTime);

        // UUID conversions supported
        CONVERSION_DB.put(Void.class, UUID.class, VoidConversions::toNull);
        CONVERSION_DB.put(UUID.class, UUID.class, Converter::identity);
        CONVERSION_DB.put(String.class, UUID.class, StringConversions::toUUID);
        CONVERSION_DB.put(Boolean.class, UUID.class, BooleanConversions::toUUID);
        CONVERSION_DB.put(BigInteger.class, UUID.class, BigIntegerConversions::toUUID);
        CONVERSION_DB.put(BigDecimal.class, UUID.class, BigDecimalConversions::toUUID);
        CONVERSION_DB.put(Map.class, UUID.class, MapConversions::toUUID);
        CONVERSION_DB.put(byte[].class, UUID.class, UUIDConversions::fromByteArray);
        CONVERSION_DB.put(ByteBuffer.class, UUID.class, UUIDConversions::fromByteBuffer);

        // Class conversions supported
        CONVERSION_DB.put(Void.class, Class.class, VoidConversions::toNull);
        CONVERSION_DB.put(Class.class, Class.class, Converter::identity);
        CONVERSION_DB.put(Map.class, Class.class, MapConversions::toClass);
        CONVERSION_DB.put(String.class, Class.class, StringConversions::toClass);

        // Color conversions supported
        CONVERSION_DB.put(Void.class, Color.class, VoidConversions::toNull);
        CONVERSION_DB.put(Color.class, Color.class, Converter::identity);
        CONVERSION_DB.put(String.class, Color.class, StringConversions::toColor);
        CONVERSION_DB.put(Map.class, Color.class, MapConversions::toColor);
        CONVERSION_DB.put(int[].class, Color.class, ArrayConversions::toColor);

        // Dimension conversions supported
        CONVERSION_DB.put(Void.class, Dimension.class, VoidConversions::toNull);
        CONVERSION_DB.put(Dimension.class, Dimension.class, Converter::identity);
        CONVERSION_DB.put(String.class, Dimension.class, StringConversions::toDimension);
        CONVERSION_DB.put(Map.class, Dimension.class, MapConversions::toDimension);
        CONVERSION_DB.put(int[].class, Dimension.class, ArrayConversions::toDimension);

        // Point conversions supported
        CONVERSION_DB.put(Void.class, Point.class, VoidConversions::toNull);
        CONVERSION_DB.put(Point.class, Point.class, Converter::identity);
        CONVERSION_DB.put(String.class, Point.class, StringConversions::toPoint);
        CONVERSION_DB.put(Map.class, Point.class, MapConversions::toPoint);
        CONVERSION_DB.put(int[].class, Point.class, ArrayConversions::toPoint);
        CONVERSION_DB.put(Dimension.class, Point.class, DimensionConversions::toPoint);

        // Rectangle conversions supported
        CONVERSION_DB.put(Void.class, Rectangle.class, VoidConversions::toNull);
        CONVERSION_DB.put(Rectangle.class, Rectangle.class, Converter::identity);
        CONVERSION_DB.put(String.class, Rectangle.class, StringConversions::toRectangle);
        CONVERSION_DB.put(Map.class, Rectangle.class, MapConversions::toRectangle);
        CONVERSION_DB.put(int[].class, Rectangle.class, ArrayConversions::toRectangle);
        CONVERSION_DB.put(Dimension.class, Rectangle.class, DimensionConversions::toRectangle);

        // Insets conversions supported
        CONVERSION_DB.put(Void.class, Insets.class, VoidConversions::toNull);
        CONVERSION_DB.put(Insets.class, Insets.class, Converter::identity);
        CONVERSION_DB.put(String.class, Insets.class, StringConversions::toInsets);
        CONVERSION_DB.put(Map.class, Insets.class, MapConversions::toInsets);
        CONVERSION_DB.put(int[].class, Insets.class, ArrayConversions::toInsets);
        CONVERSION_DB.put(Dimension.class, Insets.class, DimensionConversions::toInsets);

        // toFile
        CONVERSION_DB.put(Void.class, File.class, VoidConversions::toNull);
        CONVERSION_DB.put(File.class, File.class, Converter::identity);
        CONVERSION_DB.put(String.class, File.class, StringConversions::toFile);
        CONVERSION_DB.put(Map.class, File.class, MapConversions::toFile);
        CONVERSION_DB.put(URI.class, File.class, UriConversions::toFile);
        CONVERSION_DB.put(Path.class, File.class, PathConversions::toFile);
        CONVERSION_DB.put(char[].class, File.class, ArrayConversions::charArrayToFile);
        CONVERSION_DB.put(byte[].class, File.class, ArrayConversions::byteArrayToFile);

        // toPath
        CONVERSION_DB.put(Void.class, Path.class, VoidConversions::toNull);
        CONVERSION_DB.put(Path.class, Path.class, Converter::identity);
        CONVERSION_DB.put(String.class, Path.class, StringConversions::toPath);
        CONVERSION_DB.put(Map.class, Path.class, MapConversions::toPath);
        CONVERSION_DB.put(URI.class, Path.class, UriConversions::toPath);
        CONVERSION_DB.put(File.class, Path.class, FileConversions::toPath);
        CONVERSION_DB.put(char[].class, Path.class, ArrayConversions::charArrayToPath);
        CONVERSION_DB.put(byte[].class, Path.class, ArrayConversions::byteArrayToPath);

        // Locale conversions supported
        CONVERSION_DB.put(Void.class, Locale.class, VoidConversions::toNull);
        CONVERSION_DB.put(Locale.class, Locale.class, Converter::identity);
        CONVERSION_DB.put(String.class, Locale.class, StringConversions::toLocale);
        CONVERSION_DB.put(Map.class, Locale.class, MapConversions::toLocale);

        // Charset conversions supported
        CONVERSION_DB.put(Void.class, Charset.class, VoidConversions::toNull);
        CONVERSION_DB.put(Charset.class, Charset.class, Converter::identity);
        CONVERSION_DB.put(String.class, Charset.class, StringConversions::toCharset);
        CONVERSION_DB.put(Map.class, Charset.class, MapConversions::toCharset);

        // String conversions supported
        CONVERSION_DB.put(Void.class, String.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, String.class, StringConversions::toString);
        CONVERSION_DB.put(Short.class, String.class, StringConversions::toString);
        CONVERSION_DB.put(Integer.class, String.class, StringConversions::toString);
        CONVERSION_DB.put(Long.class, String.class, StringConversions::toString);
        CONVERSION_DB.put(Float.class, String.class, NumberConversions::floatToString);
        CONVERSION_DB.put(Double.class, String.class, NumberConversions::doubleToString);
        CONVERSION_DB.put(Boolean.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(Character.class, String.class, CharacterConversions::toString);
        CONVERSION_DB.put(BigInteger.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(BigDecimal.class, String.class, BigDecimalConversions::toString);
        CONVERSION_DB.put(byte[].class, String.class, ByteArrayConversions::toString);
        CONVERSION_DB.put(char[].class, String.class, CharArrayConversions::toString);
        CONVERSION_DB.put(Character[].class, String.class, CharacterArrayConversions::toString);
        CONVERSION_DB.put(ByteBuffer.class, String.class, ByteBufferConversions::toString);
        CONVERSION_DB.put(CharBuffer.class, String.class, CharBufferConversions::toString);
        CONVERSION_DB.put(Class.class, String.class, ClassConversions::toString);
        CONVERSION_DB.put(Date.class, String.class, DateConversions::toString);
        CONVERSION_DB.put(Calendar.class, String.class, CalendarConversions::toString);
        CONVERSION_DB.put(java.sql.Date.class, String.class, SqlDateConversions::toString);
        CONVERSION_DB.put(Timestamp.class, String.class, TimestampConversions::toString);
        CONVERSION_DB.put(LocalDate.class, String.class, LocalDateConversions::toString);
        CONVERSION_DB.put(LocalTime.class, String.class, LocalTimeConversions::toString);
        CONVERSION_DB.put(LocalDateTime.class, String.class, LocalDateTimeConversions::toString);
        CONVERSION_DB.put(ZonedDateTime.class, String.class, ZonedDateTimeConversions::toString);
        CONVERSION_DB.put(UUID.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(Color.class, String.class, ColorConversions::toString);
        CONVERSION_DB.put(Dimension.class, String.class, DimensionConversions::toString);
        CONVERSION_DB.put(Point.class, String.class, PointConversions::toString);
        CONVERSION_DB.put(Rectangle.class, String.class, RectangleConversions::toString);
        CONVERSION_DB.put(Insets.class, String.class, InsetsConversions::toString);
        CONVERSION_DB.put(Map.class, String.class, MapConversions::toString);
        CONVERSION_DB.put(Enum.class, String.class, StringConversions::enumToString);
        CONVERSION_DB.put(Enum.class, Integer.class, EnumConversions::enumToOrdinal);
        CONVERSION_DB.put(String.class, Enum.class, (ConvertWithTarget<Enum<?>>) EnumConversions::stringToEnum);
        CONVERSION_DB.put(int.class, Enum.class, (ConvertWithTarget<Enum<?>>) EnumConversions::intToEnum);
        CONVERSION_DB.put(Integer.class, Enum.class, (ConvertWithTarget<Enum<?>>) EnumConversions::intToEnum);
        CONVERSION_DB.put(Number.class, Enum.class, (ConvertWithTarget<Enum<?>>) EnumConversions::numberToEnum);
        CONVERSION_DB.put(String.class, String.class, Converter::identity);
        CONVERSION_DB.put(Duration.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(Instant.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(MonthDay.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(YearMonth.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(Period.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(ZoneId.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(ZoneOffset.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(OffsetTime.class, String.class, OffsetTimeConversions::toString);
        CONVERSION_DB.put(OffsetDateTime.class, String.class, OffsetDateTimeConversions::toString);
        CONVERSION_DB.put(Year.class, String.class, YearConversions::toString);
        CONVERSION_DB.put(Locale.class, String.class, LocaleConversions::toString);
        CONVERSION_DB.put(Charset.class, String.class, CharsetConversions::toString);
        CONVERSION_DB.put(URI.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(URL.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(File.class, String.class, FileConversions::toString);
        CONVERSION_DB.put(Path.class, String.class, PathConversions::toString);
        CONVERSION_DB.put(TimeZone.class, String.class, TimeZoneConversions::toString);
        CONVERSION_DB.put(Pattern.class, String.class, PatternConversions::toString);
        CONVERSION_DB.put(Currency.class, String.class, CurrencyConversions::toString);
        CONVERSION_DB.put(StringBuilder.class, String.class, UniversalConversions::toString);
        CONVERSION_DB.put(StringBuffer.class, String.class, UniversalConversions::toString);

        // Currency conversions
        CONVERSION_DB.put(Void.class, Currency.class, VoidConversions::toNull);
        CONVERSION_DB.put(Currency.class, Currency.class, Converter::identity);
        CONVERSION_DB.put(String.class, Currency.class, StringConversions::toCurrency);
        CONVERSION_DB.put(Map.class, Currency.class, MapConversions::toCurrency);

        // Pattern conversions
        CONVERSION_DB.put(Void.class, Pattern.class, VoidConversions::toNull);
        CONVERSION_DB.put(Pattern.class, Pattern.class, Converter::identity);
        CONVERSION_DB.put(String.class, Pattern.class, StringConversions::toPattern);
        CONVERSION_DB.put(Map.class, Pattern.class, MapConversions::toPattern);

        // URL conversions
        CONVERSION_DB.put(Void.class, URL.class, VoidConversions::toNull);
        CONVERSION_DB.put(URL.class, URL.class, Converter::identity);
        CONVERSION_DB.put(URI.class, URL.class, UriConversions::toURL);
        CONVERSION_DB.put(String.class, URL.class, StringConversions::toURL);
        CONVERSION_DB.put(Map.class, URL.class, MapConversions::toURL);
        CONVERSION_DB.put(File.class, URL.class, FileConversions::toURL);
        CONVERSION_DB.put(Path.class, URL.class, PathConversions::toURL);

        // URI Conversions
        // InetAddress: an IP address, never a looked-up name (see InetAddressConversions)
        CONVERSION_DB.put(Void.class, java.net.InetAddress.class, VoidConversions::toNull);
        CONVERSION_DB.put(java.net.InetAddress.class, java.net.InetAddress.class, Converter::identity);
        CONVERSION_DB.put(String.class, java.net.InetAddress.class, StringConversions::toInetAddress);
        CONVERSION_DB.put(Map.class, java.net.InetAddress.class, MapConversions::toInetAddress);
        CONVERSION_DB.put(java.net.InetAddress.class, String.class, InetAddressConversions::toString);
        CONVERSION_DB.put(java.net.InetAddress.class, Map.class, InetAddressConversions::toMap);

        CONVERSION_DB.put(Void.class, URI.class, VoidConversions::toNull);
        CONVERSION_DB.put(URI.class, URI.class, Converter::identity);
        CONVERSION_DB.put(URL.class, URI.class, UrlConversions::toURI);
        CONVERSION_DB.put(String.class, URI.class, StringConversions::toURI);
        CONVERSION_DB.put(Map.class, URI.class, MapConversions::toURI);
        CONVERSION_DB.put(File.class, URI.class, FileConversions::toURI);
        CONVERSION_DB.put(Path.class, URI.class, PathConversions::toURI);

        // TimeZone Conversions
        CONVERSION_DB.put(Void.class, TimeZone.class, VoidConversions::toNull);
        CONVERSION_DB.put(TimeZone.class, TimeZone.class, TimeZoneConversions::copy);
        CONVERSION_DB.put(String.class, TimeZone.class, StringConversions::toTimeZone);
        CONVERSION_DB.put(Map.class, TimeZone.class, MapConversions::toTimeZone);
        CONVERSION_DB.put(ZoneId.class, TimeZone.class, ZoneIdConversions::toTimeZone);
        CONVERSION_DB.put(ZoneOffset.class, TimeZone.class, ZoneOffsetConversions::toTimeZone);

        // Duration conversions supported
        CONVERSION_DB.put(Void.class, Duration.class, VoidConversions::toNull);
        CONVERSION_DB.put(Duration.class, Duration.class, Converter::identity);
        CONVERSION_DB.put(Long.class, Duration.class, NumberConversions::longNanosToDuration);
        CONVERSION_DB.put(Double.class, Duration.class, DoubleConversions::toDuration);
        CONVERSION_DB.put(BigInteger.class, Duration.class, BigIntegerConversions::toDuration);
        CONVERSION_DB.put(BigDecimal.class, Duration.class, BigDecimalConversions::toDuration);
        CONVERSION_DB.put(Timestamp.class, Duration.class, TimestampConversions::toDuration);
        CONVERSION_DB.put(String.class, Duration.class, StringConversions::toDuration);
        CONVERSION_DB.put(Map.class, Duration.class, MapConversions::toDuration);

        // Instant conversions supported
        CONVERSION_DB.put(Void.class, Instant.class, VoidConversions::toNull);
        CONVERSION_DB.put(Instant.class, Instant.class, Converter::identity);
        CONVERSION_DB.put(Long.class, Instant.class, NumberConversions::longNanosToInstant);
        CONVERSION_DB.put(Double.class, Instant.class, DoubleConversions::toInstant);
        CONVERSION_DB.put(BigInteger.class, Instant.class, BigIntegerConversions::toInstant);
        CONVERSION_DB.put(BigDecimal.class, Instant.class, BigDecimalConversions::toInstant);
        CONVERSION_DB.put(java.sql.Date.class, Instant.class, SqlDateConversions::toInstant);
        CONVERSION_DB.put(Timestamp.class, Instant.class, DateConversions::toInstant);
        CONVERSION_DB.put(Date.class, Instant.class, DateConversions::toInstant);
        CONVERSION_DB.put(ZonedDateTime.class, Instant.class, ZonedDateTimeConversions::toInstant);
        CONVERSION_DB.put(OffsetDateTime.class, Instant.class, OffsetDateTimeConversions::toInstant);
        CONVERSION_DB.put(Duration.class, Instant.class, DurationConversions::toInstant);

        CONVERSION_DB.put(String.class, Instant.class, StringConversions::toInstant);
        CONVERSION_DB.put(Map.class, Instant.class, MapConversions::toInstant);

        // ZoneId conversions supported
        CONVERSION_DB.put(Void.class, ZoneId.class, VoidConversions::toNull);
        CONVERSION_DB.put(ZoneId.class, ZoneId.class, Converter::identity);
        CONVERSION_DB.put(String.class, ZoneId.class, StringConversions::toZoneId);
        CONVERSION_DB.put(Map.class, ZoneId.class, MapConversions::toZoneId);
        CONVERSION_DB.put(TimeZone.class, ZoneId.class, TimeZoneConversions::toZoneId);
        CONVERSION_DB.put(ZoneOffset.class, ZoneId.class, ZoneOffsetConversions::toZoneId);

        // ZoneOffset conversions supported
        CONVERSION_DB.put(Void.class, ZoneOffset.class, VoidConversions::toNull);
        CONVERSION_DB.put(ZoneOffset.class, ZoneOffset.class, Converter::identity);
        CONVERSION_DB.put(String.class, ZoneOffset.class, StringConversions::toZoneOffset);
        CONVERSION_DB.put(Map.class, ZoneOffset.class, MapConversions::toZoneOffset);
        CONVERSION_DB.put(ZoneId.class, ZoneOffset.class, ZoneIdConversions::toZoneOffset);
        CONVERSION_DB.put(TimeZone.class, ZoneOffset.class, TimeZoneConversions::toZoneOffset);

        // MonthDay conversions supported
        CONVERSION_DB.put(Void.class, MonthDay.class, VoidConversions::toNull);
        CONVERSION_DB.put(MonthDay.class, MonthDay.class, Converter::identity);
        CONVERSION_DB.put(java.sql.Date.class, MonthDay.class, SqlDateConversions::toMonthDay);
        CONVERSION_DB.put(Calendar.class, MonthDay.class, CalendarConversions::toMonthDay);
        CONVERSION_DB.put(Timestamp.class, MonthDay.class, TimestampConversions::toMonthDay);
        CONVERSION_DB.put(ZonedDateTime.class, MonthDay.class, ZonedDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(String.class, MonthDay.class, StringConversions::toMonthDay);
        CONVERSION_DB.put(Map.class, MonthDay.class, MapConversions::toMonthDay);
        CONVERSION_DB.put(Short.class, MonthDay.class, NumberConversions::toMonthDay);
        CONVERSION_DB.put(Integer.class, MonthDay.class, NumberConversions::toMonthDay);
        CONVERSION_DB.put(Long.class, MonthDay.class, NumberConversions::toMonthDay);
        CONVERSION_DB.put(Float.class, MonthDay.class, NumberConversions::toMonthDay);
        CONVERSION_DB.put(Double.class, MonthDay.class, NumberConversions::toMonthDay);
        // AtomicInteger/AtomicLong → MonthDay handled by surrogate system via Integer/Long
        CONVERSION_DB.put(BigInteger.class, MonthDay.class, NumberConversions::toMonthDay);
        CONVERSION_DB.put(BigDecimal.class, MonthDay.class, NumberConversions::toMonthDay);

        // YearMonth conversions supported
        CONVERSION_DB.put(Void.class, YearMonth.class, VoidConversions::toNull);
        CONVERSION_DB.put(YearMonth.class, YearMonth.class, Converter::identity);
        CONVERSION_DB.put(java.sql.Date.class, YearMonth.class, SqlDateConversions::toYearMonth);
        CONVERSION_DB.put(Calendar.class, YearMonth.class, CalendarConversions::toYearMonth);
        CONVERSION_DB.put(Timestamp.class, YearMonth.class, TimestampConversions::toYearMonth);
        CONVERSION_DB.put(ZonedDateTime.class, YearMonth.class, ZonedDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(String.class, YearMonth.class, StringConversions::toYearMonth);
        CONVERSION_DB.put(Map.class, YearMonth.class, MapConversions::toYearMonth);
        CONVERSION_DB.put(Short.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(Integer.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(Long.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(Float.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(Double.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(BigInteger.class, YearMonth.class, NumberConversions::toYearMonth);
        CONVERSION_DB.put(BigDecimal.class, YearMonth.class, NumberConversions::toYearMonth);

        // Period conversions supported
        CONVERSION_DB.put(Void.class, Period.class, VoidConversions::toNull);
        CONVERSION_DB.put(Period.class, Period.class, Converter::identity);
        CONVERSION_DB.put(String.class, Period.class, StringConversions::toPeriod);
        CONVERSION_DB.put(Map.class, Period.class, MapConversions::toPeriod);

        // toStringBuffer
        CONVERSION_DB.put(Void.class, StringBuffer.class, VoidConversions::toNull);
        CONVERSION_DB.put(StringBuffer.class, StringBuffer.class, StringBufferConversions::copy);
        CONVERSION_DB.put(String.class, StringBuffer.class, StringConversions::toStringBuffer);

        // toStringBuilder - Bridge through String
        CONVERSION_DB.put(Void.class, StringBuilder.class, VoidConversions::toNull);
        CONVERSION_DB.put(StringBuilder.class, StringBuilder.class, StringBuilderConversions::copy);
        CONVERSION_DB.put(String.class, StringBuilder.class, StringConversions::toStringBuilder);

        // toByteArray
        CONVERSION_DB.put(Void.class, byte[].class, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, byte[].class, StringConversions::toByteArray);
        CONVERSION_DB.put(ByteBuffer.class, byte[].class, ByteBufferConversions::toByteArray);
        CONVERSION_DB.put(CharBuffer.class, byte[].class, CharBufferConversions::toByteArray);
        CONVERSION_DB.put(char[].class, byte[].class, (from, converter) -> ArrayConversions.arrayToArray(from, byte[].class, converter));
        CONVERSION_DB.put(byte[].class, byte[].class, ByteArrayConversions::copy);
        CONVERSION_DB.put(File.class, byte[].class, FileConversions::toByteArray);
        CONVERSION_DB.put(Map.class, byte[].class, MapConversions::toByteArray);
        CONVERSION_DB.put(Path.class, byte[].class, PathConversions::toByteArray);
        CONVERSION_DB.put(UUID.class, byte[].class, UUIDConversions::toByteArray);

        // toCharArray
        CONVERSION_DB.put(Void.class, char[].class, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, char[].class, StringConversions::toCharArray);
        CONVERSION_DB.put(ByteBuffer.class, char[].class, ByteBufferConversions::toCharArray);
        CONVERSION_DB.put(CharBuffer.class, char[].class, CharBufferConversions::toCharArray);
        CONVERSION_DB.put(char[].class, char[].class, CharArrayConversions::toCharArray);
        CONVERSION_DB.put(byte[].class, char[].class, (from, converter) -> ArrayConversions.arrayToArray(from, char[].class, converter));
        CONVERSION_DB.put(File.class, char[].class, FileConversions::toCharArray);
        CONVERSION_DB.put(Path.class, char[].class, PathConversions::toCharArray);

        // toCharacterArray
        CONVERSION_DB.put(Void.class, Character[].class, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, Character[].class, StringConversions::toCharacterArray);

        // toCharBuffer
        CONVERSION_DB.put(Void.class, CharBuffer.class, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, CharBuffer.class, StringConversions::toCharBuffer);
        CONVERSION_DB.put(ByteBuffer.class, CharBuffer.class, ByteBufferConversions::toCharBuffer);
        CONVERSION_DB.put(CharBuffer.class, CharBuffer.class, CharBufferConversions::toCharBuffer);
        CONVERSION_DB.put(char[].class, CharBuffer.class, CharArrayConversions::toCharBuffer);
        CONVERSION_DB.put(byte[].class, CharBuffer.class, ByteArrayConversions::toCharBuffer);
        CONVERSION_DB.put(Map.class, CharBuffer.class, MapConversions::toCharBuffer);

        // toByteBuffer
        CONVERSION_DB.put(Void.class, ByteBuffer.class, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, ByteBuffer.class, StringConversions::toByteBuffer);
        CONVERSION_DB.put(ByteBuffer.class, ByteBuffer.class, ByteBufferConversions::toByteBuffer);
        CONVERSION_DB.put(CharBuffer.class, ByteBuffer.class, CharBufferConversions::toByteBuffer);
        CONVERSION_DB.put(char[].class, ByteBuffer.class, CharArrayConversions::toByteBuffer);
        CONVERSION_DB.put(byte[].class, ByteBuffer.class, ByteArrayConversions::toByteBuffer);
        CONVERSION_DB.put(Map.class, ByteBuffer.class, MapConversions::toByteBuffer);
        CONVERSION_DB.put(UUID.class, ByteBuffer.class, UUIDConversions::toByteBuffer);

        // toYear
        CONVERSION_DB.put(Void.class, Year.class, NumberConversions::nullToYear);
        CONVERSION_DB.put(Year.class, Year.class, Converter::identity);
        CONVERSION_DB.put(Short.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(Integer.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(Long.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(Float.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(Double.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(BigInteger.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(BigDecimal.class, Year.class, NumberConversions::toYear);
        CONVERSION_DB.put(java.sql.Date.class, Year.class, SqlDateConversions::toYear);
        CONVERSION_DB.put(Calendar.class, Year.class, CalendarConversions::toYear);
        CONVERSION_DB.put(Timestamp.class, Year.class, TimestampConversions::toYear);
        CONVERSION_DB.put(ZonedDateTime.class, Year.class, ZonedDateTimeConversions::toYear);
        CONVERSION_DB.put(String.class, Year.class, StringConversions::toYear);
        CONVERSION_DB.put(Map.class, Year.class, MapConversions::toYear);

        // Throwable conversions supported
        CONVERSION_DB.put(Void.class, Throwable.class, VoidConversions::toNull);
        CONVERSION_DB.put(Map.class, Throwable.class, (ConvertWithTarget<Throwable>) MapConversions::toThrowable);

        // Map conversions supported
        CONVERSION_DB.put(Void.class, Map.class, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Short.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Integer.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Long.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Float.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Double.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Boolean.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(Character.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(BigInteger.class, Map.class, UniversalConversions::bigIntegerToMap);
        CONVERSION_DB.put(BigDecimal.class, Map.class, UniversalConversions::bigDecimalToMap);
        // AtomicBoolean/AtomicInteger/AtomicLong → Map handled by surrogate system via Boolean/Integer/Long
        CONVERSION_DB.put(Date.class, Map.class, DateConversions::toMap);
        CONVERSION_DB.put(java.sql.Date.class, Map.class, SqlDateConversions::toMap);
        CONVERSION_DB.put(Timestamp.class, Map.class, TimestampConversions::toMap);
        CONVERSION_DB.put(Calendar.class, Map.class, CalendarConversions::toMap);  // Restored - bridge produces different map key (zonedDateTime vs. calendar)
        CONVERSION_DB.put(LocalDate.class, Map.class, LocalDateConversions::toMap);
        CONVERSION_DB.put(LocalDateTime.class, Map.class, LocalDateTimeConversions::toMap);
        CONVERSION_DB.put(ZonedDateTime.class, Map.class, ZonedDateTimeConversions::toMap);
        CONVERSION_DB.put(Duration.class, Map.class, DurationConversions::toMap);
        CONVERSION_DB.put(Instant.class, Map.class, InstantConversions::toMap);
        CONVERSION_DB.put(LocalTime.class, Map.class, LocalTimeConversions::toMap);
        CONVERSION_DB.put(MonthDay.class, Map.class, MonthDayConversions::toMap);
        CONVERSION_DB.put(YearMonth.class, Map.class, YearMonthConversions::toMap);
        CONVERSION_DB.put(Period.class, Map.class, PeriodConversions::toMap);
        CONVERSION_DB.put(TimeZone.class, Map.class, TimeZoneConversions::toMap);
        CONVERSION_DB.put(ZoneId.class, Map.class, ZoneIdConversions::toMap);
        CONVERSION_DB.put(ZoneOffset.class, Map.class, ZoneOffsetConversions::toMap);
        CONVERSION_DB.put(Class.class, Map.class, UniversalConversions::toMap);
        CONVERSION_DB.put(UUID.class, Map.class, UUIDConversions::toMap);
        CONVERSION_DB.put(Color.class, Map.class, ColorConversions::toMap);
        CONVERSION_DB.put(Dimension.class, Map.class, DimensionConversions::toMap);
        CONVERSION_DB.put(Point.class, Map.class, PointConversions::toMap);
        CONVERSION_DB.put(Rectangle.class, Map.class, RectangleConversions::toMap);
        CONVERSION_DB.put(Insets.class, Map.class, InsetsConversions::toMap);
        CONVERSION_DB.put(String.class, Map.class, StringConversions::toMap);
        CONVERSION_DB.put(Enum.class, Map.class, EnumConversions::toMap);
        CONVERSION_DB.put(OffsetDateTime.class, Map.class, OffsetDateTimeConversions::toMap);
        CONVERSION_DB.put(OffsetTime.class, Map.class, OffsetTimeConversions::toMap);
        CONVERSION_DB.put(Year.class, Map.class, YearConversions::toMap);
        CONVERSION_DB.put(Locale.class, Map.class, LocaleConversions::toMap);
        CONVERSION_DB.put(Charset.class, Map.class, CharsetConversions::toMap);
        CONVERSION_DB.put(URI.class, Map.class, UriConversions::toMap);
        CONVERSION_DB.put(URL.class, Map.class, UrlConversions::toMap);
        CONVERSION_DB.put(Throwable.class, Map.class, ThrowableConversions::toMap);
        CONVERSION_DB.put(Pattern.class, Map.class, PatternConversions::toMap);
        CONVERSION_DB.put(Currency.class, Map.class, CurrencyConversions::toMap);
        CONVERSION_DB.put(ByteBuffer.class, Map.class, ByteBufferConversions::toMap);
        CONVERSION_DB.put(CharBuffer.class, Map.class, CharBufferConversions::toMap);
        CONVERSION_DB.put(File.class, Map.class, FileConversions::toMap);
        CONVERSION_DB.put(Path.class, Map.class, PathConversions::toMap);

        // toIntArray
        CONVERSION_DB.put(Color.class, int[].class, ColorConversions::toIntArray);
        CONVERSION_DB.put(Dimension.class, int[].class, DimensionConversions::toIntArray);
        CONVERSION_DB.put(Point.class, int[].class, PointConversions::toIntArray);
        CONVERSION_DB.put(Rectangle.class, int[].class, RectangleConversions::toIntArray);
        CONVERSION_DB.put(Insets.class, int[].class, InsetsConversions::toIntArray);

        // Array-like type bridges for universal array system access
        // ========================================
        // Atomic Array Bridges
        // ========================================

        // AtomicIntegerArray ↔ int[] bridges
        CONVERSION_DB.put(AtomicIntegerArray.class, int[].class, UniversalConversions::atomicIntegerArrayToIntArray);
        CONVERSION_DB.put(int[].class, AtomicIntegerArray.class, UniversalConversions::intArrayToAtomicIntegerArray);

        // AtomicLongArray ↔ long[] bridges  
        CONVERSION_DB.put(AtomicLongArray.class, long[].class, UniversalConversions::atomicLongArrayToLongArray);
        CONVERSION_DB.put(long[].class, AtomicLongArray.class, UniversalConversions::longArrayToAtomicLongArray);

        // AtomicReferenceArray ↔ Object[] bridges
        CONVERSION_DB.put(AtomicReferenceArray.class, Object[].class, UniversalConversions::atomicReferenceArrayToObjectArray);
        CONVERSION_DB.put(Object[].class, AtomicReferenceArray.class, UniversalConversions::objectArrayToAtomicReferenceArray);

        // AtomicReferenceArray ↔ String[] bridges
        CONVERSION_DB.put(AtomicReferenceArray.class, String[].class, UniversalConversions::atomicReferenceArrayToStringArray);
        CONVERSION_DB.put(String[].class, AtomicReferenceArray.class, UniversalConversions::stringArrayToAtomicReferenceArray);

        // ========================================
        // NIO Buffer Bridges
        // ========================================

        // IntBuffer ↔ int[] bridges
        CONVERSION_DB.put(IntBuffer.class, int[].class, UniversalConversions::intBufferToIntArray);
        CONVERSION_DB.put(int[].class, IntBuffer.class, UniversalConversions::intArrayToIntBuffer);

        // LongBuffer ↔ long[] bridges
        CONVERSION_DB.put(LongBuffer.class, long[].class, UniversalConversions::longBufferToLongArray);
        CONVERSION_DB.put(long[].class, LongBuffer.class, UniversalConversions::longArrayToLongBuffer);

        // FloatBuffer ↔ float[] bridges
        CONVERSION_DB.put(FloatBuffer.class, float[].class, UniversalConversions::floatBufferToFloatArray);
        CONVERSION_DB.put(float[].class, FloatBuffer.class, UniversalConversions::floatArrayToFloatBuffer);

        // DoubleBuffer ↔ double[] bridges
        CONVERSION_DB.put(DoubleBuffer.class, double[].class, UniversalConversions::doubleBufferToDoubleArray);
        CONVERSION_DB.put(double[].class, DoubleBuffer.class, UniversalConversions::doubleArrayToDoubleBuffer);

        // ShortBuffer ↔ short[] bridges
        CONVERSION_DB.put(ShortBuffer.class, short[].class, UniversalConversions::shortBufferToShortArray);
        CONVERSION_DB.put(short[].class, ShortBuffer.class, UniversalConversions::shortArrayToShortBuffer);

        // ========================================
        // BitSet Bridges
        // ========================================

        // BitSet ↔ boolean[] bridges
        CONVERSION_DB.put(BitSet.class, BitSet.class, UniversalConversions::copyBitSet);
        CONVERSION_DB.put(BitSet.class, boolean[].class, UniversalConversions::bitSetToBooleanArray);
        CONVERSION_DB.put(boolean[].class, BitSet.class, UniversalConversions::booleanArrayToBitSet);

        // BitSet ↔ int[] bridges (set bit indices)
        CONVERSION_DB.put(BitSet.class, int[].class, UniversalConversions::bitSetToIntArray);
        CONVERSION_DB.put(int[].class, BitSet.class, UniversalConversions::intArrayToBitSet);

        // BitSet ↔ byte[] bridges
        CONVERSION_DB.put(BitSet.class, byte[].class, UniversalConversions::bitSetToByteArray);
        CONVERSION_DB.put(byte[].class, BitSet.class, UniversalConversions::byteArrayToBitSet);

        // BitSet ↔ long bridges (lower 64 bits)
        CONVERSION_DB.put(BitSet.class, Long.class, UniversalConversions::bitSetToLong);
        CONVERSION_DB.put(Long.class, BitSet.class, UniversalConversions::longToBitSet);
        CONVERSION_DB.put(BitSet.class, long.class, UniversalConversions::bitSetToLong);
        CONVERSION_DB.put(long.class, BitSet.class, UniversalConversions::longToBitSet);

        // BitSet ↔ BigInteger bridges (arbitrary size)
        CONVERSION_DB.put(BitSet.class, BigInteger.class, UniversalConversions::bitSetToBigInteger);
        CONVERSION_DB.put(BigInteger.class, BitSet.class, UniversalConversions::bigIntegerToBitSet);

        // BitSet ↔ int bridges (lower 32 bits)
        CONVERSION_DB.put(BitSet.class, Integer.class, UniversalConversions::bitSetToInt);
        CONVERSION_DB.put(Integer.class, BitSet.class, UniversalConversions::intToBitSet);
        CONVERSION_DB.put(BitSet.class, int.class, UniversalConversions::bitSetToInt);
        CONVERSION_DB.put(int.class, BitSet.class, UniversalConversions::intToBitSet);

        // BitSet ↔ short bridges (lower 16 bits)
        CONVERSION_DB.put(BitSet.class, Short.class, UniversalConversions::bitSetToShort);
        CONVERSION_DB.put(Short.class, BitSet.class, UniversalConversions::shortToBitSet);
        CONVERSION_DB.put(BitSet.class, short.class, UniversalConversions::bitSetToShort);
        CONVERSION_DB.put(short.class, BitSet.class, UniversalConversions::shortToBitSet);

        // BitSet ↔ byte bridges (lower 8 bits)
        CONVERSION_DB.put(BitSet.class, Byte.class, UniversalConversions::bitSetToByte);
        CONVERSION_DB.put(Byte.class, BitSet.class, UniversalConversions::byteToBitSet);
        CONVERSION_DB.put(BitSet.class, byte.class, UniversalConversions::bitSetToByte);
        CONVERSION_DB.put(byte.class, BitSet.class, UniversalConversions::byteToBitSet);

        // BitSet ↔ AtomicInteger handled by surrogate system via Integer

        // BitSet ↔ BigDecimal bridges (arbitrary size)
        CONVERSION_DB.put(BitSet.class, BigDecimal.class, UniversalConversions::bitSetToBigDecimal);
        CONVERSION_DB.put(BigDecimal.class, BitSet.class, UniversalConversions::bigDecimalToBitSet);

        // BitSet ↔ Boolean bridges
        CONVERSION_DB.put(BitSet.class, Boolean.class, UniversalConversions::bitSetToBoolean);
        CONVERSION_DB.put(Boolean.class, BitSet.class, UniversalConversions::booleanToBitSet);

        // BitSet ↔ AtomicBoolean handled by surrogate system via Boolean

        // BitSet ↔ String bridges (binary string format, 0s and 1s only)
        CONVERSION_DB.put(BitSet.class, String.class, UniversalConversions::bitSetToString);
        CONVERSION_DB.put(String.class, BitSet.class, UniversalConversions::stringToBitSet);

        // BitSet ↔ ByteBuffer bridges (the same bytes as byte[])
        CONVERSION_DB.put(BitSet.class, ByteBuffer.class, UniversalConversions::bitSetToByteBuffer);
        CONVERSION_DB.put(ByteBuffer.class, BitSet.class, UniversalConversions::byteBufferToBitSet);

        // BitSet → CharBuffer, and BitSet ↔ char[] / Character[] bridges (the binary string's characters)
        CONVERSION_DB.put(BitSet.class, CharBuffer.class, UniversalConversions::bitSetToCharBuffer);
        CONVERSION_DB.put(BitSet.class, char[].class, UniversalConversions::bitSetToCharArray);
        CONVERSION_DB.put(char[].class, BitSet.class, UniversalConversions::charArrayToBitSet);
        CONVERSION_DB.put(BitSet.class, Character[].class, UniversalConversions::bitSetToCharacterArray);
        CONVERSION_DB.put(Character[].class, BitSet.class, UniversalConversions::characterArrayToBitSet);

        // BitSet → Map ({"value": "<binary string>"}, which Map → BitSet reads); null → BitSet is null
        CONVERSION_DB.put(BitSet.class, Map.class, UniversalConversions::bitSetToMap);
        CONVERSION_DB.put(Void.class, BitSet.class, VoidConversions::toNull);

        // ========================================
        // Stream Bridges
        // ========================================

        // Array → Stream bridges (Stream → Array removed due to single-use limitation)
        CONVERSION_DB.put(int[].class, IntStream.class, UniversalConversions::intArrayToIntStream);
        CONVERSION_DB.put(long[].class, LongStream.class, UniversalConversions::longArrayToLongStream);
        CONVERSION_DB.put(double[].class, DoubleStream.class, UniversalConversions::doubleArrayToDoubleStream);

        // Register Record.class -> Map.class conversion if Records are supported
        try {
            Class<?> recordClass = Class.forName("java.lang.Record");
            CONVERSION_DB.put(recordClass, Map.class, MapConversions::recordToMap);
        } catch (ClassNotFoundException e) {
            // Records not available in this JVM (JDK < 14)
        }

        // XMLGregorianCalendar and javax.xml.datatype.Duration, which JAXB generates for xs:dateTime, xs:date and
        // xs:duration: registered when the java.xml module is present. java-util requires it statically, so a modular
        // application may run without it, and no class here may name its types until this check has passed.
        try {
            Class.forName("javax.xml.datatype.DatatypeFactory", false, Converter.class.getClassLoader());
            XmlDatatypeConversions.register(CONVERSION_DB::put);
        } catch (ClassNotFoundException | LinkageError e) {
            // java.xml is not in this runtime
        }

        // Expand bridge conversions - discover multi-hop paths and add them to CONVERSION_DB
        expandBridgeConversions();

        // CONVERSION_DB is now ready for use (MultiKeyMap is inherently thread-safe)
    }

    /**
     * Cached list of surrogate → primary pairs for one-way expansion.
     */
    private static List<SurrogatePrimaryPair> SURROGATE_TO_PRIMARY_PAIRS = null;

    /**
     * Cached list of primary → surrogate pairs for reverse expansion.
     */
    private static List<SurrogatePrimaryPair> PRIMARY_TO_SURROGATE_PAIRS = null;

    /**
     * List 1: SURROGATE → PRIMARY (surrogateCanReachEverythingPrimaryCanReach)
     * Every "surrogate" on the left can be losslessly collapsed to the "primary" on the
     * right, so it is safe to give the surrogate all the outbound conversions that the
     * primary already owns.
     */
    private static List<SurrogatePrimaryPair> getSurrogateToPrimaryPairs() {
        if (SURROGATE_TO_PRIMARY_PAIRS == null) {
            SURROGATE_TO_PRIMARY_PAIRS = Arrays.asList(
                    // Primitives → Wrappers (lossless)
                    new SurrogatePrimaryPair(byte.class, Byte.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(short.class, Short.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(int.class, Integer.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(long.class, Long.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(float.class, Float.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(double.class, Double.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(char.class, Character.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(boolean.class, Boolean.class, UniversalConversions::primitiveToWrapper, null),

                    // Atomic types → Wrappers (lossless via .get())
                    new SurrogatePrimaryPair(AtomicBoolean.class, Boolean.class,
                            UniversalConversions::atomicBooleanToBoolean, null),
                    new SurrogatePrimaryPair(AtomicInteger.class, Integer.class,
                            UniversalConversions::atomicIntegerToInt, null),
                    new SurrogatePrimaryPair(AtomicLong.class, Long.class,
                            UniversalConversions::atomicLongToLong, null),
                    // LongAdder / DoubleAdder: their sum
                    new SurrogatePrimaryPair(java.util.concurrent.atomic.LongAdder.class, Long.class,
                            UniversalConversions::longAdderToLong, null),
                    new SurrogatePrimaryPair(java.util.concurrent.atomic.DoubleAdder.class, Double.class,
                            UniversalConversions::doubleAdderToDouble, null),

                    // String builders → String (lossless via .toString())
                    new SurrogatePrimaryPair(CharSequence.class, String.class,
                            UniversalConversions::charSequenceToString, null),

                    // Resource identifiers → URI (lossless via URL.toURI())
                    new SurrogatePrimaryPair(URL.class, URI.class,
                            UrlConversions::toURI, null),
                    
                    // Date & Time → ZonedDateTime (hub-and-spoke: each type converts to ZDT,
                    // then bridge expansion auto-generates X → ZDT → Target composites)
                    new SurrogatePrimaryPair(Calendar.class, ZonedDateTime.class,
                            CalendarConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(Instant.class, ZonedDateTime.class,
                            InstantConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(LocalDateTime.class, ZonedDateTime.class,
                            LocalDateTimeConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(LocalDate.class, ZonedDateTime.class,
                            LocalDateConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(OffsetDateTime.class, ZonedDateTime.class,
                            OffsetDateTimeConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(Date.class, ZonedDateTime.class,
                            DateConversions::toZonedDateTime, null),
                    new SurrogatePrimaryPair(Timestamp.class, ZonedDateTime.class,
                            TimestampConversions::toZonedDateTime, null)
            );
        }
        return SURROGATE_TO_PRIMARY_PAIRS;
    }

    /**
     * List 2: PRIMARY → SURROGATE (everythingThatCanReachPrimaryCanAlsoReachSurrogate)
     * These pairs let callers land on the surrogate instead of the primary when they
     * are traveling into the ecosystem. They do not guarantee the reverse trip is
     * perfect, so they only belong in this reverse list.
     */
    private static List<SurrogatePrimaryPair> getPrimaryToSurrogatePairs() {
        if (PRIMARY_TO_SURROGATE_PAIRS == null) {
            PRIMARY_TO_SURROGATE_PAIRS = Arrays.asList(
                    // Wrappers → Primitives (safe conversion via auto-unboxing)
                    new SurrogatePrimaryPair(Byte.class, byte.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Short.class, short.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Integer.class, int.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Long.class, long.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Float.class, float.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Double.class, double.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Character.class, char.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Boolean.class, boolean.class, null, UniversalConversions::wrapperToPrimitive),

                    // Wrappers → Atomic types (create new atomic with same value)
                    new SurrogatePrimaryPair(Boolean.class, AtomicBoolean.class, null,
                            UniversalConversions::booleanToAtomicBoolean),
                    new SurrogatePrimaryPair(Integer.class, AtomicInteger.class, null,
                            UniversalConversions::integerToAtomicInteger),
                    new SurrogatePrimaryPair(Long.class, AtomicLong.class, null,
                            UniversalConversions::longToAtomicLong),
                    // A new LongAdder / DoubleAdder holding the value
                    new SurrogatePrimaryPair(Long.class, java.util.concurrent.atomic.LongAdder.class, null,
                            UniversalConversions::longToLongAdder),
                    new SurrogatePrimaryPair(Double.class, java.util.concurrent.atomic.DoubleAdder.class, null,
                            UniversalConversions::doubleToDoubleAdder),

                    // String → String builders (create new mutable builder)
                    new SurrogatePrimaryPair(String.class, StringBuffer.class, null,
                            UniversalConversions::stringToStringBuffer),
                    new SurrogatePrimaryPair(String.class, StringBuilder.class, null,
                            UniversalConversions::stringToStringBuilder),
                    new SurrogatePrimaryPair(String.class, CharSequence.class, null,
                            UniversalConversions::stringToCharSequence),

                    // URI → URL (convert URI to URL for legacy compatibility)
                    new SurrogatePrimaryPair(URI.class, URL.class, null,
                            UriConversions::toURL),

                    // Date → java.sql.Time (whatever reaches a Date reaches a Time, as a Time)
                    new SurrogatePrimaryPair(Date.class, java.sql.Time.class, null,
                            DateConversions::toSqlTime)
            );
        }
        return PRIMARY_TO_SURROGATE_PAIRS;
    }

    /**
     * Represents a surrogate-primary class pair with bidirectional bridge conversion functions.
     */
    private static class SurrogatePrimaryPair {
        final Class<?> surrogateClass;
        final Class<?> primaryClass;
        final Convert<?> surrogateToPrimaryConversion;
        final Convert<?> primaryToSurrogateConversion;

        SurrogatePrimaryPair(Class<?> surrogateClass, Class<?> primaryClass,
                             Convert<?> surrogateToPrimaryConversion, Convert<?> primaryToSurrogateConversion) {
            this.surrogateClass = surrogateClass;
            this.primaryClass = primaryClass;
            this.surrogateToPrimaryConversion = surrogateToPrimaryConversion;
            this.primaryToSurrogateConversion = primaryToSurrogateConversion;
        }
    }

    /**
     * Direction enumeration for bridge expansion operations.
     */
    private enum BridgeDirection {
        SURROGATE_TO_PRIMARY,
        PRIMARY_TO_SURROGATE
    }

    /**
     * Expands bridge conversions by discovering multi-hop conversion paths and adding them to CONVERSION_DB.
     * This allows for code reduction by eliminating redundant conversion definitions while maintaining
     * the same or greater conversion capabilities.
     * <p>
     * For example, if we have:
     * - AtomicInteger → Integer (bridge)
     * - Integer → String (direct conversion)
     * <p>
     * This method will discover the AtomicInteger → String path and add it to CONVERSION_DB
     * as a composite conversion function.
     * <p>
     * The expansion iterates until convergence — each pass may discover new entries that enable
     * further bridging in the next pass. For example, the forward pass creates AtomicInteger → Long
     * (via Integer), and the reverse pass then creates AtomicInteger → AtomicLong (via Long).
     * Iterating ensures all reachable multi-hop paths are discovered regardless of definition order.
     */
    private static void expandBridgeConversions() {
        int prevSize;
        do {
            prevSize = CONVERSION_DB.size();
            expandSurrogateBridges(BridgeDirection.SURROGATE_TO_PRIMARY);
            expandSurrogateBridges(BridgeDirection.PRIMARY_TO_SURROGATE);
        } while (CONVERSION_DB.size() > prevSize);
    }

    /**
     * Consolidated method for expanding surrogate bridges in both directions.
     * Creates composite conversion functions that bridge through intermediate types.
     * 
     * @param direction The direction of bridge expansion (SURROGATE_TO_PRIMARY or PRIMARY_TO_SURROGATE)
     */
    private static void expandSurrogateBridges(BridgeDirection direction) {
        // Create a snapshot of existing (source, target) pairs to avoid mutating CONVERSION_DB
        // while iterating it. The values themselves will be re-fetched via CONVERSION_DB.get()
        // inside the loop using the current (possibly bridged) conversion.
        final List<Class<?>[]> existingPairs = new ArrayList<>(CONVERSION_DB.size());
        CONVERSION_DB.forEach((src, tgt, val) -> existingPairs.add(new Class<?>[] { src, tgt }));

        // Get the appropriate configuration list based on a direction
        List<SurrogatePrimaryPair> configs = (direction == BridgeDirection.SURROGATE_TO_PRIMARY) ?
            getSurrogateToPrimaryPairs() : getPrimaryToSurrogatePairs();

        // Process each surrogate configuration
        for (SurrogatePrimaryPair config : configs) {
            if (direction == BridgeDirection.SURROGATE_TO_PRIMARY) {
                // FORWARD BRIDGES: Surrogate → Primary → Target
                // Example: int.class → Integer.class → String.class
                Class<?> surrogateClass = config.surrogateClass;
                Class<?> primaryClass = config.primaryClass;

                // Find all targets that the primary class can convert to
                for (Class<?>[] entry : existingPairs) {
                    Class<?> sourceClass = entry[0];
                    Class<?> targetClass = entry[1];
                    if (sourceClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting to itself
                        if (CONVERSION_DB.get(surrogateClass, targetClass) == null && !targetClass.equals(surrogateClass)) {
                            // Create composite conversion: Surrogate → primary → target
                            Convert<?> originalConversion = CONVERSION_DB.get(sourceClass, targetClass);
                            Convert<?> bridgeConversion = createSurrogateToPrimaryBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(surrogateClass, targetClass, bridgeConversion);
                        }
                    }
                }
            } else {
                // REVERSE BRIDGES: Source → Primary → Surrogate
                // Example: String.class → Integer.class → int.class
                Class<?> primaryClass = config.surrogateClass;  // Note: in List 2, surrogate is the source
                Class<?> surrogateClass = config.primaryClass;  // and primary is the target

                // Find all sources that can convert to the primary class
                for (Class<?>[] entry : existingPairs) {
                    Class<?> sourceClass = entry[0];
                    Class<?> targetClass = entry[1];
                    if (targetClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting from itself
                        if (CONVERSION_DB.get(sourceClass, surrogateClass) == null && !sourceClass.equals(surrogateClass)) {
                            // Create composite conversion: Source → primary → surrogate
                            Convert<?> originalConversion = CONVERSION_DB.get(sourceClass, targetClass);
                            Convert<?> bridgeConversion = createPrimaryToSurrogateBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(sourceClass, surrogateClass, bridgeConversion);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a composite conversion function that bridges from surrogate type to target via primary.
     * Uses the configured bridge conversion to extract primary value, then applies existing primary conversion.
     */
    private static Convert<?> createSurrogateToPrimaryBridgeConversion(SurrogatePrimaryPair config, Convert<?> primaryToTargetConversion) {
        Convert<?> surrogateToPrimaryConversion = config.surrogateToPrimaryConversion;
        if (surrogateToPrimaryConversion == null) {
            throw new IllegalArgumentException("No surrogate→primary conversion found for: " + config.surrogateClass);
        }

        // Return ConvertWithTarget to pass target type through (works for both Convert and ConvertWithTarget)
        return (ConvertWithTarget<?>) (from, converter, target) -> {
            // First: Convert surrogate to primary (e.g., int → Integer, AtomicInteger → Integer)
            Object primaryValue = surrogateToPrimaryConversion.convert(from, converter);
            // Second: Convert primary to target using existing conversion, passing target type
            return primaryToTargetConversion.convert(primaryValue, converter, target);
        };
    }

    /**
     * Creates a composite conversion function that bridges from source type to surrogate via primary.
     * Uses the existing source-to-primary conversion, then applies configured primary-to-surrogate bridge.
     */
    private static Convert<?> createPrimaryToSurrogateBridgeConversion(SurrogatePrimaryPair config, Convert<?> sourceToPrimaryConversion) {
        Convert<?> primaryToSurrogateConversion = config.primaryToSurrogateConversion;
        if (primaryToSurrogateConversion == null) {
            throw new IllegalArgumentException("No primary→surrogate conversion found for: " + config.primaryClass);
        }

        // Return ConvertWithTarget to pass target type through (works for both Convert and ConvertWithTarget)
        return (ConvertWithTarget<?>) (from, converter, target) -> {
            // First: Convert a source to primary using existing conversion
            Object primaryValue = sourceToPrimaryConversion.convert(from, converter, target);
            // Second: Convert primary to surrogate (e.g., Integer → int, Integer → AtomicInteger)
            return primaryToSurrogateConversion.convert(primaryValue, converter, target);
        };
    }

    /**
     * Constructs a new Converter instance with the specified options.
     * <p>
     * The Converter initializes its internal conversion databases by merging the predefined
     * {@link #CONVERSION_DB} with any user-specified overrides provided in {@code options}.
     * </p>
     *
     * @param options The {@link ConverterOptions} that configure this Converter's behavior and conversions.
     * @throws NullPointerException if {@code options} is {@code null}.
     */
    public Converter(ConverterOptions options) {
        this.options = options;
        this.instanceId = INSTANCE_ID_GENERATOR.getAndIncrement();

        Map<ConversionPair, Convert<?>> overrides = this.options.getConverterOverrides();
        this.hasUserConversions = !overrides.isEmpty();

        for (Map.Entry<ConversionPair, Convert<?>> entry : overrides.entrySet()) {
            ConversionPair pair = entry.getKey();
            // USER_DB is an instance-scoped ClassPairMap keyed only by (source, target).
            // Register under all primitive/wrapper variations of the pair — the same expansion
            // addConversion() performs — so an override keyed with int.class also fires for
            // Integer.class lookups (and vice versa) instead of silently falling through to the
            // built-in conversion for the unregistered variant.
            for (Class<?> srcType : getTypeVariations(pair.getSource())) {
                for (Class<?> tgtType : getTypeVariations(pair.getTarget())) {
                    USER_DB.put(srcType, tgtType, entry.getValue());
                }
            }

            // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
            addIdentityConversionIfNeeded(pair.getSource());
            addIdentityConversionIfNeeded(pair.getTarget());
        }
    }

    /**
     * Converts the given source object to the specified target type.
     * <p>
     * The {@code convert} method serves as the primary API for transforming objects between various types.
     * It supports a wide range of conversions, including primitive types, wrapper classes, numeric types,
     * date and time classes, collections, and custom objects. Additionally, it allows for extensibility
     * by enabling the registration of custom converters.
     * </p>
     * <p>
     * <strong>Key Features:</strong>
     * <ul>
     *     <li><b>Wide Range of Supported Types:</b> Supports conversion between Java primitives, their corresponding
     *         wrapper classes, {@link Number} subclasses, date and time classes (e.g., {@link Date}, {@link LocalDateTime}),
     *         collections (e.g., {@link List}, {@link Set}, {@link Map}), {@link UUID}, and more.</li>
     *     <li><b>Null Handling:</b> Gracefully handles {@code null} inputs by returning {@code null} for object types,
     *         default primitive values (e.g., 0 for numeric types, {@code false} for boolean), and default characters.</li>
     *     <li><b>Inheritance-Based Conversions:</b> Automatically considers superclass and interface hierarchies
     *         to find the most suitable converter when a direct conversion is not available.</li>
     *     <li><b>Custom Converters:</b> Allows users to register custom conversion logic for specific source-target type pairs
     *         using the {@link #addConversion(Class, Class, Convert)} method.</li>
     *     <li><b>Thread-Safe:</b> Designed to be thread-safe, allowing concurrent conversions without compromising data integrity.</li>
     * </ul>
     * </p>
     *
     * <h3>Usage Examples:</h3>
     * <pre>{@code
     *     ConverterOptions options = new ConverterOptions();
     *     Converter converter = new Converter(options);
     *
     *     // Example 1: Convert String to Integer
     *     String numberStr = "123";
     *     Integer number = converter.convert(numberStr, Integer.class);
     *     LOG.info("Converted Integer: " + number); // Output: Converted Integer: 123
     *
     *     // Example 2: Convert String to Date
     *     String dateStr = "2024-04-27";
     *     LocalDate date = converter.convert(dateStr, LocalDate.class);
     *     LOG.info("Converted Date: " + date); // Output: Converted Date: 2024-04-27
     *
     *     // Example 3: Convert Enum to String
     *     Day day = Day.MONDAY;
     *     String dayStr = converter.convert(day, String.class);
     *     LOG.info("Converted Day: " + dayStr); // Output: Converted Day: MONDAY
     *
     *     // Example 4: Convert Array to List
     *     String[] stringArray = {"apple", "banana", "cherry"};
     *     List<String> stringList = converter.convert(stringArray, List.class);
     *     LOG.info("Converted List: " + stringList); // Output: Converted List: [apple, banana, cherry]
     *
     *     // Example 5: Convert Map to UUID
     *     Map<String, Object> uuidMap = Map.of("mostSigBits", 123456789L, "leastSigBits", 987654321L);
     *     UUID uuid = converter.convert(uuidMap, UUID.class);
     *     LOG.info("Converted UUID: " + uuid); // Output: Converted UUID: 00000000-075b-cd15-0000-0000003ade68
     *
     *     // Example 6: Convert Object[], String[], Collection, and primitive Arrays to EnumSet
     *     Object[] array = {Day.MONDAY, Day.WEDNESDAY, "FRIDAY", 4};
     *     EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);
     *
     *     Enum, String, and Number value in the source collection/array is properly converted
     *     to the correct Enum type and added to the returned EnumSet. Null values inside the
     *     source (Object[], Collection) are skipped.
     *
     *     When converting arrays or collections to EnumSet, you must use a double cast due to Java's
     *     type system and generic type erasure. The cast is safe as the converter guarantees return of
     *     an EnumSet when converting arrays/collections to enum types.
     *
     *     // Example 7: Register and Use a Custom Converter
     *     // Custom converter to convert String to CustomType
     *     converter.addConversion(String.class, CustomType.class, (from, conv) -> new CustomType(from));
     *
     *     String customStr = "customValue";
     *     CustomType custom = converter.convert(customStr, CustomType.class);
     *     LOG.info("Converted CustomType: " + custom); // Output: Converted CustomType: CustomType{value='customValue'}
     * }
     * </pre>
     *
     * <h3>Parameter Descriptions:</h3>
     * <ul>
     *     <li><b>from:</b> The source object to be converted. This can be any object, including {@code null}.
     *         The actual type of {@code from} does not need to match the target type; the Converter will attempt to
     *         perform the necessary transformation.</li>
     *     <li><b>toType:</b> The target class to which the source object should be converted. This parameter
     *         specifies the desired output type. It can be a primitive type (e.g., {@code int.class}), a wrapper class
     *         (e.g., {@link Integer}.class), or any other supported class.</li>
     * </ul>
     *
     * <h3>Return Value:</h3>
     * <p>
     * Returns an instance of the specified target type {@code toType}, representing the converted value of the source object {@code from}.
     * If {@code from} is {@code null}, the method returns:
     * <ul>
     *     <li>{@code null} for non-primitive target types.</li>
     *     <li>Default primitive values for primitive target types (e.g., 0 for numeric types, {@code false} for {@code boolean}, '\u0000' for {@code char}).</li>
     * </ul>
     * </p>
     *
     * <h3>Exceptions:</h3>
     * <ul>
     *     <li><b>IllegalArgumentException:</b> Thrown if the conversion from the source type to the target type is not supported,
     *         or if the target type {@code toType} is {@code null}.</li>
     *     <li><b>RuntimeException:</b> Any underlying exception thrown during the conversion process is propagated as a {@code RuntimeException}.</li>
     * </ul>
     *
     * <h3>Supported Conversions:</h3>
     * <p>
     * The Converter supports a vast array of conversions, including but not limited to:
     * <ul>
     *     <li><b>Primitives and Wrappers:</b> Convert between Java primitive types (e.g., {@code int}, {@code boolean}) and their corresponding wrapper classes (e.g., {@link Integer}, {@link Boolean}).</li>
     *     <li><b>Numbers:</b> Convert between different numeric types (e.g., {@link Integer} to {@link Double}, {@link BigInteger} to {@link BigDecimal}).</li>
     *     <li><b>Date and Time:</b> Convert between various date and time classes (e.g., {@link String} to {@link LocalDate}, {@link Date} to {@link Instant}, {@link Calendar} to {@link ZonedDateTime}).</li>
     *     <li><b>Collections:</b> Convert between different collection types (e.g., arrays to {@link List}, {@link Set} to {@link Map}, {@link StringBuilder} to {@link String}).</li>
     *     <li><b>Custom Objects:</b> Convert between complex objects (e.g., {@link UUID} to {@link Map}, {@link Class} to {@link String}, custom types via user-defined converters).</li>
     *     <li><b>Buffer Types:</b> Convert between buffer types (e.g., {@link ByteBuffer} to {@link String}, {@link CharBuffer} to {@link Byte}[]).</li>
     * </ul>
     * </p>
     *
     * <h3>Extensibility:</h3>
     * <p>
     * Users can extend the Converter's capabilities by registering custom converters for specific type pairs.
     * This is achieved using the {@link #addConversion(Class, Class, Convert)} method, which accepts the source type,
     * target type, and a {@link Convert} functional interface implementation that defines the conversion logic.
     * </p>
     *
     * <h3>Performance Considerations:</h3>
     * <p>
     * The Converter utilizes caching mechanisms to store and retrieve converters, ensuring efficient performance
     * even with a large number of conversion operations. However, registering an excessive number of custom converters
     * may impact memory usage. It is recommended to register only the necessary converters to maintain optimal performance.
     * </p>
     *
     * @param from   The source object to be converted. Can be any object, including {@code null}.
     * @param toType The target class to which the source object should be converted. Must not be {@code null}.
     * @param <T>    The type of the target object.
     * @return An instance of {@code toType} representing the converted value of {@code from}.
     * @throws IllegalArgumentException if {@code toType} is {@code null} or if the conversion is not supported.
     * @see #getSupportedConversions()
     * @see #addConversion(Class, Class, Convert)
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object from, Class<T> toType) {
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }

        // For null inputs, use Void.class so that e.g. convert(null, int.class) returns 0.
        Class<?> sourceType = (from == null) ? Void.class : from.getClass();

        // Instance-specific user conversions are checked FIRST (documented precedence: instance
        // conversions, then factory conversions, then inheritance). This must happen before the
        // shared FULL_CONVERSION_CACHE read: that cache is static and holds only CONVERSION_DB-derived
        // resolutions written by instances WITHOUT user conversions — so an entry populated by any
        // plain instance (e.g. json-io's default converter) would otherwise silently mask this
        // instance's override for the same (source, target) pair. Guarded by hasUserConversions so
        // the common no-override path pays only a volatile boolean read.
        if (hasUserConversions) {
            Convert<?> user = USER_DB.get(sourceType, toType);
            if (isValidConversion(user)) {
                return (T) user.convert(from, this, toType);
            }
        }

        // Shared resolution cache (built-in-derived entries only; see cacheConverter).
        Convert<?> cached = getCachedConverter(sourceType, toType);
        if (isValidConversion(cached)) {
            return (T) cached.convert(from, this, toType);
        }

        // Try container conversion (Arrays, Collections, Maps).
        if (from != null) {
            T result = attemptContainerConversion(from, sourceType, toType);
            if (result != null) {
                return result;
            }
        }

        // Then check the factory conversion database.
        Convert<?> conversionMethod = CONVERSION_DB.get(sourceType, toType);
        if (isValidConversion(conversionMethod)) {
            // Cache built-in conversions at shared level (0L) so all instances benefit
            FULL_CONVERSION_CACHE.put(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Attempt inheritance-based conversion.
        conversionMethod = getInheritedConverter(sourceType, toType);
        if (isValidConversion(conversionMethod)) {
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // If no specific converter found, check assignment compatibility as fallback [someone is doing convert(linkedMap, Map.class) for example]
        if (from != null && toType.isAssignableFrom(from.getClass())) {
            return (T) from; // Assignment compatible - use as-is
        }

        // Universal Object → Map conversion (only when no specific converter exists)
        if (!(from instanceof Map) && Map.class.isAssignableFrom(toType)) {
            // Skip collections and arrays - they have their own conversion paths
            if (!(from != null && from.getClass().isArray() || from instanceof Collection)) {
                // Create cached converter for Object→Map conversion
                final Class<?> finalToType = toType;
                Convert<?> objectConverter = (fromObj, converter) -> ObjectConversions.objectToMapWithTarget(fromObj, converter, finalToType);

                // Execute and cache successful conversions. A result that is not of the requested type (a Map type
                // that cannot be made: abstract, say) is no conversion.
                Object result = objectConverter.convert(from, this);
                if (result == null) {
                    return null;
                }
                if (toType.isInstance(result)) {
                    cacheConverter(sourceType, toType, objectConverter);
                    return (T) result;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported conversion, source type [" + name(from) +
                "] target type '" + getShortName(toType) + "'");
    }

    private Convert<?> getCachedConverter(Class<?> source, Class<?> target) {
        // ClassPairMap makes this a zero-allocation, thread-safe fast path.
        // No L1 slot: a volatile-field slot was measured to be ~3-4 ns per hit but
        // had a correctness hole (three non-atomic volatile writes could produce
        // stale mixed-state reads under contention), and a ThreadLocal slot would
        // cost more than the ClassPairMap lookup it was trying to avoid.
        return FULL_CONVERSION_CACHE.get(source, target);
    }

    private void cacheConverter(Class<?> source, Class<?> target, Convert<?> converter) {
        // Cache at the shared level only when this instance has no user conversions,
        // guaranteeing the resolved entry is derived purely from CONVERSION_DB (identical for
        // all instances). That includes UNSUPPORTED negatives: they make repeated
        // isConversionSupportedFor()/isSimpleTypeConversionSupported() misses O(1) instead of
        // re-walking the inheritance pair list every call (json-io's Resolver probes these
        // per value). Negatives cannot mask instance overrides — convert() and
        // isConversionSupportedFor() consult USER_DB before trusting the cache, and
        // addConversion() purges affected entries for inheritance-level additions.
        // (Caching keyed by instanceId was abandoned earlier: each short-lived Converter left
        // entries never looked up again after GC — unbounded static cache growth.)
        if (!hasUserConversions) {
            FULL_CONVERSION_CACHE.put(source, target, converter);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T attemptContainerConversion(Object from, Class<?> sourceType, Class<T> toType) {
        // Validate source type is a container type (Array, Collection, or Map)
        if (!(from.getClass().isArray() || from instanceof Collection || from instanceof Map)) {
            return null;
        }

        // If source is already an EnumSet and target is a compatible type (superclass/interface),
        // return as-is - no conversion needed. This preserves EnumSet when asked for Collection/Set.
        if (from instanceof EnumSet && toType.isAssignableFrom(sourceType)) {
            return (T) from;
        }

        // Check for EnumSet target first
        if (EnumSet.class.isAssignableFrom(toType)) {
            throw new IllegalArgumentException("To convert to EnumSet, specify the Enum class to convert to as the 'toType.' Example: EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);");
        }

        // Special handling for container → Enum conversions (creates EnumSet)
        if (toType.isEnum()) {
            if (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> EnumConversions.toEnumSet(fromObj, converter, toType));
            } else if (Map.class.isAssignableFrom(sourceType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> EnumConversions.mapToEnum((Map<?, ?>) fromObj, converter, toType));
            }
        }
        // EnumSet source conversions
        else if (EnumSet.class.isAssignableFrom(sourceType)) {
            if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> {
                            Collection<Object> target = (Collection<Object>) CollectionHandling.createCollection(fromObj, toType);
                            target.addAll((Collection<?>) fromObj);
                            return target;
                        });
            }
            if (toType.isArray()) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.enumSetToArray((EnumSet<?>) fromObj, toType));
            }
        }
        // Collection source conversions
        else if (Collection.class.isAssignableFrom(sourceType)) {
            if (toType.isArray()) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.collectionToArray((Collection<?>) fromObj, toType, converter));
            } else if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> CollectionConversions.collectionToCollection((Collection<?>) fromObj, toType));
            }
        }
        // Array source conversions
        else if (sourceType.isArray()) {
            if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> CollectionConversions.arrayToCollection(fromObj, (Class<? extends Collection<?>>) toType));
            } else if (toType.isArray() && !sourceType.getComponentType().equals(toType.getComponentType())) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.arrayToArray(fromObj, toType, converter));
            }
        }
        // Map source conversions
        else if (Map.class.isAssignableFrom(sourceType)) {
            if (Map.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> MapConversions.mapToMapWithTarget(fromObj, converter, toType));
            }
        }

        return null;
    }

    /**
     * Helper method to execute a converter and cache it if successful
     */
    @SuppressWarnings("unchecked")
    private <T> T executeAndCache(Class<?> sourceType, Class<?> toType, Object from, Convert<?> converter) {
        Object result = converter.convert(from, this);
        if (result != null) {
            cacheConverter(sourceType, toType, converter);
        }
        return (T) result;
    }
    
    /**
     * Retrieves the most suitable converter for converting from the specified source type to the desired target type.
     * This method searches through the class hierarchies of both source and target types to find the best matching
     * conversion, prioritizing matches in the following order:
     *
     * <ol>
     *   <li>Exact match to requested target type</li>
     *   <li>Most specific target type when considering inheritance (e.g., java.sql.Date over java.util.Date)</li>
     *   <li>Shortest combined inheritance distance from source and target types</li>
     *   <li>Concrete classes over interfaces at the same inheritance level</li>
     * </ol>
     *
     * <p>The method first checks user-defined conversions ({@code USER_DB}) before falling back to built-in
     * conversions ({@code CONVERSION_DB}). Class hierarchies are cached to improve performance of repeated lookups.</p>
     *
     * <p>For example, when converting to java.sql.Date, a converter to java.sql.Date will be chosen over a converter
     * to its parent class java.util.Date, even if the java.util.Date converter is closer in the source type's hierarchy.</p>
     *
     * @param sourceType The source type to convert from
     * @param toType     The target type to convert to
     * @return A {@link Convert} instance for the most appropriate conversion, or {@code null} if no suitable converter is found
     */
    private Convert<?> getInheritedConverter(Class<?> sourceType, Class<?> toType) {
        // Get cached sorted inheritance pairs (builds and caches on first call for this source/target combination)
        InheritancePair[] pairs = getSortedInheritancePairs(sourceType, toType);

        // Iterate over sorted pairs and check the converter databases.
        final boolean checkUserDb = hasUserConversions;   // skip guaranteed-empty USER_DB probes
        for (InheritancePair inheritancePair : pairs) {
            final Class<?> source = inheritancePair.source;
            final Class<?> target = inheritancePair.target;

            if (checkUserDb) {
                Convert<?> userConverter = USER_DB.get(source, target);
                if (userConverter != null) {
                    return target == toType ? userConverter : toSubtype(userConverter, target, toType);
                }
            }
            Convert<?> tempConverter = CONVERSION_DB.get(source, target);
            if (tempConverter != null) {
                return target == toType ? tempConverter : toSubtype(tempConverter, target, toType);
            }
        }
        return null;
    }

    /**
     * The conversion to {@code toType} when the walk found {@code found}, a conversion to its supertype
     * {@code baseType}. With a maker for baseType (see SUBTYPE_MAKERS): one that turns the found conversion's result
     * into a toType (a result that is a toType already is returned as it is), or null -- no conversion -- when
     * toType cannot be made. Without a maker, the found conversion, as before. The caller caches what this returns,
     * so the constructors are looked up once per (source, toType).
     */
    private static Convert<?> toSubtype(Convert<?> found, Class<?> baseType, Class<?> toType) {
        if (!isValidConversion(found)) {
            return found;
        }
        Function<Class<?>, BiFunction<Object, Converter, Object>> makers = null;
        for (Class<?> type = baseType; type != null && makers == null; type = type.getSuperclass()) {
            makers = SUBTYPE_MAKERS.get(type);
        }
        if (makers == null) {
            return found;
        }
        BiFunction<Object, Converter, Object> make = makers.apply(toType);
        if (make == null) {
            return null;
        }
        return (ConvertWithTarget<Object>) (from, converter, target) -> {
            Object value = found.convert(from, converter, toType);
            return value == null || toType.isInstance(value) ? value : make.apply(value, converter);
        };
    }

    /**
     * Makes a Map of the requested type from the Map a conversion made (a scalar converts to {_v: value}): as a Map
     * converts to that type, so an interface gets its usual implementation (SortedMap a TreeMap, ConcurrentMap a
     * ConcurrentHashMapNullSafe) and a Collections wrapper its factory; entries the type cannot hold are skipped, as
     * they are then. Null -- refused -- for a class that cannot be made: abstract, or with neither a no-argument nor
     * an (int) constructor. A result that still is not of the requested type is an IllegalArgumentException.
     */
    private static BiFunction<Object, Converter, Object> mapMaker(Class<?> type) {
        boolean makeable = type.isInterface() || type.getName().startsWith("java.util.Collections$")
                || constructor(type) != null || constructor(type, int.class) != null;
        if (!makeable) {
            return null;
        }
        return (value, converter) -> {
            Object made = MapConversions.mapToMapWithTarget(value, converter, type);
            if (!type.isInstance(made)) {
                throw new IllegalArgumentException("Cannot make a " + type.getName() + " to convert to");
            }
            return made;
        };
    }

    /** The accessible constructor of a concrete class with these parameter types, or null. */
    private static Constructor<?> constructor(Class<?> type, Class<?>... parameterTypes) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        try {
            return ReflectionUtils.getConstructor(type, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object instantiate(Constructor<?> constructor, Object... arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot make a " + constructor.getDeclaringClass().getName()
                    + " to convert to", e);
        }
    }

    /**
     * The length of the longest chain of supertypes above a type, following Java's assignability rather than
     * getSuperclass() alone: strictly greater for a subtype than for any of its supertypes. An interface is below
     * Object (every interface type is assignable to it); an array is below Object, Cloneable and Serializable, and
     * below the array of any supertype of its component (String[] is assignable to Object[]). Per class, computed
     * once. JDK types only, so it pins no class loader.
     */
    private static final ClassValue<Integer> TYPE_DEPTH = new ClassValue<Integer>() {
        @Override
        protected Integer computeValue(Class<?> type) {
            if (type.isArray()) {
                return get(type.getComponentType()) + 2;
            }
            int depth = type.isInterface() ? 1 : 0;
            Class<?> superclass = type.getSuperclass();
            if (superclass != null) {
                depth = Math.max(depth, get(superclass) + 1);
            }
            for (Class<?> iface : type.getInterfaces()) {
                depth = Math.max(depth, get(iface) + 1);
            }
            return depth;
        }
    };

    /**
     * Gets the sorted list of inheritance pairs for a given source/target type combination.
     * <p>
     * The pairs are sorted by:
     * <ol>
     *   <li>Exact target matches first</li>
     *   <li>More specific target types (assignability)</li>
     *   <li>Combined inheritance distance (lower is better)</li>
     *   <li>Concrete classes before interfaces</li>
     * </ol>
     * The result is cached per (sourceType, toType) combination.
     * </p>
     *
     * @param sourceType The source type
     * @param toType The target type
     * @return Cached, sorted array of InheritancePair objects
     */
    private static InheritancePair[] getSortedInheritancePairs(Class<?> sourceType, Class<?> toType) {
        InheritancePair[] cached = cacheInheritancePairs.get(sourceType, toType);
        if (cached != null) {
            return cached;
        }

        // Build pairs from complete hierarchies (already cached per type)
        Object[] sourceHierarchy = TYPE_HIERARCHY.get(sourceType);
        Object[] targetHierarchy = TYPE_HIERARCHY.get(toType);
        Class<?>[] sourceTypes = (Class<?>[]) sourceHierarchy[0];
        int[] sourceLevels = (int[]) sourceHierarchy[1];
        Class<?>[] targetTypes = (Class<?>[]) targetHierarchy[0];
        int[] targetLevels = (int[]) targetHierarchy[1];

        List<InheritancePair> pairs = new ArrayList<>(sourceTypes.length * targetTypes.length);
        for (int s = 0; s < sourceTypes.length; s++) {
            for (int t = 0; t < targetTypes.length; t++) {
                pairs.add(new InheritancePair(sourceTypes[s], targetTypes[t], sourceLevels[s], targetLevels[t]));
            }
        }

        // Sort the pairs by a composite of rules
        final Class<?> finalToType = toType;
        pairs.sort((p1, p2) -> {
            // Exact target matches first
            boolean p1ExactTarget = p1.target == finalToType;
            boolean p2ExactTarget = p2.target == finalToType;
            if (p1ExactTarget != p2ExactTarget) {
                return p1ExactTarget ? -1 : 1;
            }
            // More specific target types first: deeper in the type hierarchy. A subtype is always deeper than its
            // supertypes, so this keeps "a subtype before its supertype" -- which comparing by assignability did
            // too, but assignability only orders related types, and mixed with the distance rule below it was not
            // transitive: TimSort threw "Comparison method violates its general contract!" (a Collection source to
            // a Number target, for one), from isConversionSupportedFor() and convert() alike.
            if (p1.target != p2.target) {
                int depth1 = TYPE_DEPTH.get(p1.target);
                int depth2 = TYPE_DEPTH.get(p2.target);
                if (depth1 != depth2) {
                    return depth2 - depth1;
                }
            }
            // Combined inheritance distance
            int dist1 = p1.sourceLevel + p1.targetLevel;
            int dist2 = p2.sourceLevel + p2.targetLevel;
            if (dist1 != dist2) {
                return dist1 - dist2;
            }
            // Prefer concrete classes over interfaces (source)
            boolean p1FromInterface = p1.source.isInterface();
            boolean p2FromInterface = p2.source.isInterface();
            if (p1FromInterface != p2FromInterface) {
                return p1FromInterface ? 1 : -1;
            }
            // Prefer concrete classes over interfaces (target)
            boolean p1ToInterface = p1.target.isInterface();
            boolean p2ToInterface = p2.target.isInterface();
            if (p1ToInterface != p2ToInterface) {
                return p1ToInterface ? 1 : -1;
            }
            return 0;
        });

        // For the same target, a source before its own supertypes, as Java prefers the most specific overload: the
        // distance alone could rank a supertype first (an ArrayList is 2 steps from Object, through RandomAccess, and
        // 3 from Collection), so an added Object conversion won over a Collection one. Not a comparator rule -- mixed
        // with the distance it would not be transitive -- but a pass over the sorted list: each pair moves before the
        // first earlier one, for its target, whose source is a proper supertype of its own. The list before it has no
        // pair after one it should precede, and none between that position and its own can be a subtype of it.
        for (int j = 1; j < pairs.size(); j++) {
            InheritancePair pair = pairs.get(j);
            for (int i = 0; i < j; i++) {
                InheritancePair earlier = pairs.get(i);
                if (earlier.target == pair.target && earlier.source != pair.source
                        && earlier.source.isAssignableFrom(pair.source)) {
                    pairs.remove(j);
                    pairs.add(i, pair);
                    break;
                }
            }
        }

        InheritancePair[] pairsArray = pairs.toArray(new InheritancePair[0]);
        InheritancePair[] existing = cacheInheritancePairs.putIfAbsent(sourceType, toType, pairsArray);
        return existing != null ? existing : pairsArray;
    }

    /**
     * Represents a class along with its hierarchy level for ordering purposes.
     * <p>
     * This class is used internally to manage and compare classes based on their position within the class hierarchy.
     * </p>
     */
    static class ClassLevel implements Comparable<ClassLevel> {
        private final Class<?> clazz;
        private final int level;
        private final boolean isInterface;

        ClassLevel(Class<?> c, int level) {
            clazz = c;
            this.level = level;
            isInterface = c.isInterface();
        }

        @Override
        public int compareTo(ClassLevel other) {
            // Primary sort key: level (ascending)
            int levelComparison = Integer.compare(this.level, other.level);
            if (levelComparison != 0) {
                return levelComparison;
            }

            // Secondary sort key: concrete class before interface
            if (isInterface && !other.isInterface) {
                return 1;
            }
            if (!isInterface && other.isInterface) {
                return -1;
            }

            // Tertiary sort key: alphabetical order (for determinism)
            return this.clazz.getName().compareTo(other.clazz.getName());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassLevel)) {
                return false;
            }
            ClassLevel other = (ClassLevel) obj;
            return this.clazz.equals(other.clazz) && this.level == other.level;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() * 31 + level;
        }
    }

    /**
     * Represents a source/target class pair with their inheritance levels for converter lookup.
     * <p>
     * This class is used to cache the sorted list of inheritance pairs for a given source/target
     * type combination, avoiding repeated computation and sorting.
     * </p>
     */
    static final class InheritancePair {
        final Class<?> source;
        final Class<?> target;
        final int sourceLevel;
        final int targetLevel;

        InheritancePair(Class<?> source, Class<?> target, int sourceLevel, int targetLevel) {
            this.source = source;
            this.target = target;
            this.sourceLevel = sourceLevel;
            this.targetLevel = targetLevel;
        }
    }

    /**
     * Returns a short name for the given class.
     * <ul>
     * <li>For specific array types, returns the custom name</li>
     * <li>For other array types, returns the component's simple name + "[]"</li>
     * <li>For java.sql.Date and javax.xml.datatype.Duration, returns the fully qualified name</li>
     * <li>For all other classes, returns the simple name</li>
     * </ul>
     *
     * @param type The class to get the short name for
     * @return The short name of the class
     */
    static String getShortName(Class<?> type) {
        if (type.isArray()) {
            // The one array type with a custom short name (a table of one was a ClassValueMap)
            if (type == java.sql.Date[].class) {
                return "java.sql.Date[]";
            }
            // For other arrays, use component's simple name + "[]"
            Class<?> componentType = type.getComponentType();
            return componentType.getSimpleName() + "[]";
        }
        // A simple name two types share is the full name: java.sql.Date (java.util.Date) and javax.xml.datatype.Duration
        // (java.time.Duration). The latter by name, as java.xml may be absent.
        if (java.sql.Date.class.equals(type) || "javax.xml.datatype.Duration".equals(type.getName())) {
            return type.getName();
        }
        // Default: use simple name
        return type.getSimpleName();
    }

    /**
     * Generates a descriptive name for the given object.
     * <p>
     * If the object is {@code null}, returns "null". Otherwise, returns a string combining the short name
     * of the object's class and its {@code toString()} representation.
     * </p>
     *
     * @param from The object for which to generate a name.
     * @return A descriptive name of the object.
     */
    /**
     * The source as an error message shows it: its type, and its value cut to the first 100 characters, so that a
     * large collection does not put its whole content into every log that records the exception.
     */
    static private String name(Object from) {
        if (from == null) {
            return "null";
        }
        String value = String.valueOf(from);
        if (value.length() > 100) {
            value = value.substring(0, Character.isHighSurrogate(value.charAt(99)) ? 99 : 100) + "...";
        }
        return getShortName(from.getClass()) + " (" + value + ")";
    }

    /**
     * Determines if a container-based conversion is supported between the specified source and target types.
     * This method checks for valid conversions between arrays, collections, Maps, and EnumSets without actually
     * performing the conversion.
     *
     * <p>Supported conversions include:
     * <ul>
     *   <li>Array to Collection</li>
     *   <li>Collection to Array</li>
     *   <li>Array to Array (when component types differ)</li>
     *   <li>Map to Map</li>
     *   <li>Array, Collection, or Map to EnumSet (when target is an Enum type)</li>
     *   <li>EnumSet to Array or Collection</li>
     * </ul>
     * </p>
     *
     * @param sourceType The source type to convert from
     * @param target     The target type to convert to
     * @return true if a container-based conversion is supported between the types, false otherwise -- including
     *         for an {@code EnumSet} target, whose enum type cannot be known from the class alone (to convert, pass
     *         the enum class as the target instead: {@code convert(array, Day.class)} gives an {@code EnumSet<Day>})
     */
    public static boolean isContainerConversionSupported(Class<?> sourceType, Class<?> target) {
        // Quick check: If the source is not an array, a Collection, Map, or an EnumSet, no conversion is supported here.
        if (!(sourceType.isArray() || Collection.class.isAssignableFrom(sourceType) || Map.class.isAssignableFrom(sourceType) || EnumSet.class.isAssignableFrom(sourceType))) {
            return false;
        }

        // Target is EnumSet: the enum type cannot be determined from the class, so this is not a conversion that
        // can be made to it. A question, so an answer -- it threw, and so did isConversionSupportedFor() through it.
        // (convert() still explains, with an example, that the Enum class is the type to ask for.)
        if (EnumSet.class.isAssignableFrom(target)) {
            return false;
        }

        // If the target type is an Enum, then we're essentially looking to create an EnumSet<EnumType>.
        // For that, the source must be either an array, a collection, or a Map (via keySet) from which we can build the EnumSet.
        if (target.isEnum()) {
            return (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType) || Map.class.isAssignableFrom(sourceType));
        }

        // If the source is an EnumSet, it can be converted to either an array or another collection.
        if (EnumSet.class.isAssignableFrom(sourceType)) {
            return target.isArray() || Collection.class.isAssignableFrom(target);
        }

        // If the source is a Map, we support conversion to map types.
        if (Map.class.isAssignableFrom(sourceType)) {
            // Keep generic Map->Map capability checks conservative (legacy behavior),
            // while still reporting concrete map-to-map conversions.
            return Map.class.isAssignableFrom(target) && !(sourceType == Map.class && target == Map.class);
        }

        // If the source is a generic Collection, we only support converting it to an array or collection
        if (Collection.class.isAssignableFrom(sourceType)) {
            return target.isArray() || Collection.class.isAssignableFrom(target);
        }

        // If the source is an array:
        // 1. If the target is a Collection, we can always convert.
        // 2. If the target is another array, we must verify that component types differ,
        //    otherwise it's just a no-op (the caller might be expecting a conversion).
        if (sourceType.isArray()) {
            if (Collection.class.isAssignableFrom(target)) {
                return true;
            } else {
                return target.isArray() && !sourceType.getComponentType().equals(target.getComponentType());
            }
        }

        // Fallback: Shouldn't reach here given the initial conditions.
        return false;
    }

    /**
     * @deprecated Use {@link #isContainerConversionSupported(Class, Class)} instead.
     * This method will be removed in a future version.
     */
    @Deprecated
    public static boolean isCollectionConversionSupported(Class<?> sourceType, Class<?> target) {
        return isContainerConversionSupported(sourceType, target);
    }

    /**
     * Determines whether a conversion from the specified source type to the target type is supported,
     * excluding any conversions involving arrays or collections.
     *
     * <p>The method is particularly useful when you need to verify that a conversion is possible
     * between simple types without considering array or collection conversions. This can be helpful
     * in scenarios where you need to validate component type conversions separately from their
     * container types.</p>
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * Converter converter = new Converter(options);
     *
     * // Check if String can be converted to Integer
     * boolean canConvert = converter.isNonCollectionConversionSupportedFor(
     *     String.class, Integer.class);  // returns true
     *
     * // Check array conversion (always returns false)
     * boolean arrayConvert = converter.isNonCollectionConversionSupportedFor(
     *     String[].class, Integer[].class);  // returns false
     *
     * // Check collection conversion (always returns false)
     * boolean listConvert = converter.isNonCollectionConversionSupportedFor(
     *     List.class, Set.class);  // returns false
     * }</pre>
     *
     * @param source The source class type to check
     * @param target The target class type to check
     * @return {@code true} if a non-collection conversion exists between the types,
     * {@code false} if either type is an array/collection or no conversion exists
     * @see #isConversionSupportedFor(Class, Class)
     */
    public boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target) {
        // If user has registered custom converter overrides for this conversion pair,
        // do not treat it as a simple conversion.
        if (hasConverterOverrideFor(source, target)) {
            return false;
        }

        // Simple-type checks intentionally exclude container conversions.
        if (source.isArray() || target.isArray()
                || Collection.class.isAssignableFrom(source) || Collection.class.isAssignableFrom(target)
                || Map.class.isAssignableFrom(source) || Map.class.isAssignableFrom(target)) {
            return false;
        }

        // Delegate conversion capability to the general conversion support API.
        return isConversionSupportedFor(source, target);
    }

    /**
     * Compatibility overload for legacy callers that ask if a type is "simple".
     *
     * @param type the class to check
     * @return {@code true} if simple-type conversion is supported for {@code type -> type}
     * @deprecated Use {@link #isSimpleTypeConversionSupported(Class, Class)} with
     * {@code isSimpleTypeConversionSupported(type, type)}.
     */
    @Deprecated
    public boolean isSimpleTypeConversionSupported(Class<?> type) {
        return isSimpleTypeConversionSupported(type, type);
    }

    /**
     * Checks if custom converter overrides exist for the specified source-target conversion pair.
     * Uses efficient hash lookup in USER_DB instead of linear search.
     *
     * @param sourceType the source type to check for custom overrides
     * @param targetType the target type to check for custom overrides  
     * @return {@code true} if custom converter overrides exist for the conversion pair, {@code false} otherwise
     */
    private boolean hasConverterOverrideFor(Class<?> sourceType, Class<?> targetType) {
        if (!hasUserConversions) {
            return false;       // USER_DB is empty: skip the probe (json-io builds a Converter per read)
        }
        // Direct hash lookup in USER_DB using this instance's ID.
        // This finds both overrides from ConverterOptions (stored in constructor)
        // and conversions added dynamically via addConversion().
        Convert<?> converter = USER_DB.get(sourceType, targetType);
        return converter != null && converter != UNSUPPORTED;
    }

    /**
     * Determines whether a conversion from the specified source type to the target type is supported.
     * For array-to-array conversions, this method verifies that both array conversion and component type
     * conversions are supported.
     *
     * <p>The method checks three paths for conversion support:</p>
     * <ol>
     *   <li>Direct conversions as defined in the conversion maps</li>
     *   <li>Collection/Array/EnumSet conversions - for array-to-array conversions, also verifies
     *       that component type conversions are supported</li>
     *   <li>Inherited conversions (via superclasses and implemented interfaces)</li>
     * </ol>
     *
     * <p>For array conversions, this method performs a deep check to ensure both the array types
     * and their component types can be converted. For example, when checking if a String[] can be
     * converted to Integer[], it verifies both:</p>
     * <ul>
     *   <li>That array-to-array conversion is supported</li>
     *   <li>That String-to-Integer conversion is supported for the components</li>
     * </ul>
     *
     * @param source The source class type
     * @param target The target class type
     * @return true if the conversion is fully supported (including component type conversions for arrays),
     * false otherwise
     */
    public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        // Instance-specific user conversions take precedence and must be consulted before the
        // shared cache — same masking hazard as convert(): a shared entry written by a plain
        // instance must not answer for an instance that carries its own overrides.
        if (hasUserConversions) {
            Convert<?> user = USER_DB.get(ClassUtilities.toPrimitiveWrapperClass(source),
                    ClassUtilities.toPrimitiveWrapperClass(target));
            if (isValidConversion(user)) {
                return true;
            }
        }

        // Shared cache: positive resolutions and (built-in-derived) UNSUPPORTED negatives.
        Convert<?> cached = getCachedConverter(source, target);
        if (cached != null) {
            if (cached != UNSUPPORTED) {
                return true;
            }
            if (!hasUserConversions) {
                return false;   // trusted negative — nothing instance-specific can change it
            }
            // Fall through: this instance may still support the pair via inheritance-level
            // user conversions the shared negative cannot account for.
        }

        // Check direct conversion support in the primary databases.
        Convert<?> method = getConversionFromDBs(source, target);
        if (isValidConversion(method)) {
            // getConversionFromDBs() looks primitives up as their wrappers (int as Integer). A converter found that
            // way belongs to the wrapper pair, so it is cached only when that is the pair asked about. Cached under
            // (Void, int) it answered convert(null, int.class) -- which looks primitives up exactly, and gives 0 --
            // with the Void-to-Integer converter: null, for every later caller in the JVM.
            if (source == ClassUtilities.toPrimitiveWrapperClass(source)
                    && target == ClassUtilities.toPrimitiveWrapperClass(target)) {
                cacheConverter(source, target, method);
            }
            return true;
        }

        // Handle container conversions (arrays, collections, maps).
        if (isContainerConversionSupported(source, target)) {
            // Special handling for array-to-array conversions:
            if (source.isArray() && target.isArray()) {
                Class<?> srcComp = source.getComponentType();
                Class<?> tgtComp = target.getComponentType();
                // If either component is Object, be optimistic - we can't know actual element types at compile time
                // If both are specific types, recursively check if component conversion is supported
                return srcComp == Object.class || tgtComp == Object.class ||
                        isConversionSupportedFor(srcComp, tgtComp);
            }
            return true;  // All other collection conversions are supported.
        }

        // Finally, attempt inheritance-based conversion.
        method = getInheritedConverter(source, target);
        if (isValidConversion(method)) {
            cacheConverter(source, target, method);
            return true;
        }

        // Cache the negative result so subsequent lookups are O(1).
        // addConversion() calls clearCachesForTypeVariations() to invalidate if a conversion is added later.
        cacheConverter(source, target, UNSUPPORTED);
        return false;
    }

    private static boolean isValidConversion(Convert<?> method) {
        return method != null && method != UNSUPPORTED;
    }

    /**
     * Private helper method to check if a conversion exists directly in USER_DB or CONVERSION_DB.
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return Convert instance
     */
    private Convert<?> getConversionFromDBs(Class<?> source, Class<?> target) {
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);

        // Check instance-specific user conversions (ConverterOptions overrides + addConversion())
        Convert<?> method = USER_DB.get(source, target);
        if (isValidConversion(method)) {
            return method;
        }

        // Check built-in conversions
        method = CONVERSION_DB.get(source, target);
        if (isValidConversion(method)) {
            return method;
        }
        return UNSUPPORTED;
    }

    /**
     * Retrieves a map of all supported conversions, categorized by source and target classes.
     * <p>
     * The returned map's keys are source classes, and each key maps to a {@code Set} of target classes
     * that the source can be converted to.
     * </p>
     * <p>
     * This includes both simple type conversions from the conversion database and dynamic
     * array/collection conversions which are handled at runtime.
     * </p>
     *
     * @return A {@code Map<Class<?>, Set<Class<?>>>} representing all supported (built-in) conversions.
     */
    public static Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        Map<Class<?>, Set<Class<?>>> toFrom = new TreeMap<>(Comparator.comparing(Class::getName));
        addSupportedConversion(CONVERSION_DB, toFrom);
        addArrayCollectionConversions(toFrom);
        return toFrom;
    }

    /**
     * Retrieves a map of all supported conversions with class names instead of class objects.
     * <p>
     * The returned map's keys are source class names, and each key maps to a {@code Set} of target class names
     * that the source can be converted to.
     * </p>
     * <p>
     * This includes both simple type conversions from the conversion database and dynamic
     * array/collection conversions which are handled at runtime.
     * </p>
     *
     * @return A {@code Map<String, Set<String>>} representing all supported (built-int) conversions by class names.
     */
    public static Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareTo);
        addSupportedConversionName(CONVERSION_DB, toFrom);
        addArrayCollectionConversionNames(toFrom);
        return toFrom;
    }

    /**
     * Adds the dynamic array/collection conversion entries to the supported conversions map.
     * These conversions are handled at runtime by ArrayConversions and CollectionConversions
     * and support any component types where the component conversion is supported.
     */
    private static void addArrayCollectionConversions(Map<Class<?>, Set<Class<?>>> toFrom) {
        Comparator<Class<?>> classComparator = Comparator.comparing(Class::getName);

        // Array to Array (component types converted dynamically)
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // Array to Collection
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Collection.class);

        // Array to Enum (creates EnumSet)
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // Collection to Array
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // Collection to Collection
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Collection.class);

        // Collection to Enum (creates EnumSet)
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // Map to Enum (creates EnumSet from keySet)
        toFrom.computeIfAbsent(Map.class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // EnumSet to Array
        toFrom.computeIfAbsent(EnumSet.class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // EnumSet to Collection
        toFrom.computeIfAbsent(EnumSet.class, k -> new TreeSet<>(classComparator)).add(Collection.class);
    }

    /**
     * Adds the dynamic array/collection conversion entry names to the supported conversions map.
     */
    private static void addArrayCollectionConversionNames(Map<String, Set<String>> toFrom) {
        // Array to Array (component types converted dynamically)
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Object[]");

        // Array to Collection
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Collection");

        // Array to Enum (creates EnumSet)
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Enum");

        // Collection to Array
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Object[]");

        // Collection to Collection
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Collection");

        // Collection to Enum (creates EnumSet)
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Enum");

        // Map to Enum (creates EnumSet from keySet)
        toFrom.computeIfAbsent("Map", k -> new TreeSet<>()).add("Enum");

        // EnumSet to Array
        toFrom.computeIfAbsent("EnumSet", k -> new TreeSet<>()).add("Object[]");

        // EnumSet to Collection
        toFrom.computeIfAbsent("EnumSet", k -> new TreeSet<>()).add("Collection");
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions.
     */
    private static void addSupportedConversion(ClassPairMap<Convert<?>> db, Map<Class<?>, Set<Class<?>>> toFrom) {
        db.forEach((source, target, value) -> {
            if (value != UNSUPPORTED) {
                toFrom.computeIfAbsent(source, k -> new TreeSet<>(Comparator.comparing((Class<?> c) -> c.getName()))).add(target);
            }
        });
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database, using class names.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions by class names.
     */
    private static void addSupportedConversionName(ClassPairMap<Convert<?>> db, Map<String, Set<String>> toFrom) {
        db.forEach((source, target, value) -> {
            if (value != UNSUPPORTED) {
                toFrom.computeIfAbsent(getShortName(source), k -> new TreeSet<>(String::compareTo)).add(getShortName(target));
            }
        });
    }

    /**
     * @param conversionMethod A method that converts an instance of the source type to an instance of the target type.
     * @return The previous conversion method associated with the source and target types, or {@code null} if no conversion existed.
     * @deprecated Use {@link #addConversion(Convert, Class, Class)} instead. This method will be removed in a future version as it is less safe and does not handle all type variations correctly.
     */
    @Deprecated
    public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionMethod) {
        return addConversion(conversionMethod, source, target);
    }

    /**
     * Adds a new conversion function for converting from one type to another for this specific Converter instance.
     * <p>When {@code convert(source, target)} is called on this instance, the conversion function is located by:
     * <ol>
     *   <li>Checking instance-specific conversions first (added via this method)</li>
     *   <li>Checking factory conversions (built-in conversions)</li>
     *   <li>Attempting inheritance-based conversion lookup</li>
     * </ol></p>
     *
     * <p>This method automatically handles primitive types by converting them to their corresponding wrapper types
     * and stores conversions for all primitive/wrapper combinations, just like the static version.</p>
     *
     * <p><b>Class loaders.</b> The conversion is kept on one of its two classes -- the one whose class loader can see
     * the other's -- so it lives no longer than they do; when neither loader can see the other (two OSGi bundles, two
     * applications), this Converter holds it itself, for as long as the Converter lives. One case to know about in a
     * redeployable application (a web application's {@code WEB-INF/lib}): a conversion between two JDK classes (an
     * override of {@code String -> Long}, say) is kept on a JDK class, and when it is your application's code and
     * this Converter is held in one of your application's static fields, your application's class loader stays
     * reachable after a redeploy (a {@link ClassValue} entry whose value leads back to its own key). For such an
     * override, use a Converter that is not held in a static field.</p>
     *
     * @param conversionMethod A method that converts an instance of the source type to an instance of the target type.
     * @param source           The source class (type) to convert from.
     * @param target           The target class (type) to convert to.
     * @return The previous conversion method associated with the source and target types for this instance, or {@code null} if no conversion existed.
     */
    public Convert<?> addConversion(Convert<?> conversionMethod, Class<?> source, Class<?> target) {
        // Collect all type variations (primitive and wrapper) for both source and target
        Set<Class<?>> sourceTypes = getTypeVariations(source);
        Set<Class<?>> targetTypes = getTypeVariations(target);

        // Clear caches for all affected combinations in one pass
        clearCachesForTypeVariations(sourceTypes, targetTypes);

        // Store the wrapper version first to capture return value
        Class<?> wrapperSource = ClassUtilities.toPrimitiveWrapperClass(source);
        Class<?> wrapperTarget = ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> previous = USER_DB.get(wrapperSource, wrapperTarget);
        USER_DB.put(wrapperSource, wrapperTarget, conversionMethod);

        // Add all type combinations to USER_DB with this instance ID
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                USER_DB.put(srcType, tgtType, conversionMethod);
            }
        }

        // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
        addIdentityConversionIfNeeded(source);
        addIdentityConversionIfNeeded(target);

        this.hasUserConversions = true;
        return previous;
    }

    /**
     * Helper method to get all type variations (primitive and wrapper) for a given class.
     */
    private static Set<Class<?>> getTypeVariations(Class<?> clazz) {
        Set<Class<?>> types = new IdentitySet<>();
        types.add(clazz);
        
        if (clazz.isPrimitive()) {
            // If it's primitive, add the wrapper
            types.add(ClassUtilities.toPrimitiveWrapperClass(clazz));
        } else {
            // If it's not primitive, check if it's a wrapper and add the primitive
            Class<?> primitive = ClassUtilities.toPrimitiveClass(clazz);
            if (primitive != clazz) {  // toPrimitiveClass returns same class if not a wrapper
                types.add(primitive);
            }
        }
        
        return types;
    }

    /**
     * Adds an identity conversion (T -> T) for a non-standard type to enable O(1) lookup
     * in hasConverterOverrideFor. This serves as a marker that the type is involved in
     * custom conversions while also providing useful identity conversion functionality.
     * 
     * @param type the type to add identity conversion for
     */
    private void addIdentityConversionIfNeeded(Class<?> type) {
        if (type != null && USER_DB.get(type, type) == null) {
            USER_DB.put(type, type, IDENTITY_CONVERTER);
        }
    }

    /**
     * Performs an identity conversion, returning the source object as-is.
     *
     * @param from      The source object.
     * @param converter The Converter instance performing the conversion.
     * @param <T>       The type of the source and target object.
     * @return The source object unchanged.
     */
    public static <T> T identity(T from, Converter converter) {
        return from;
    }

    /**
     * Handles unsupported conversions by returning {@code null}.
     *
     * @param from      The source object.
     * @param converter The Converter instance performing the conversion.
     * @param <T>       The type of the source and target object.
     * @return {@code null} indicating the conversion is unsupported.
     */
    private static <T> T unsupported(T from, Converter converter) {
        return null;
    }

    private static void clearCachesForTypeVariations(Set<Class<?>> sourceTypes, Set<Class<?>> targetTypes) {
        // Note: Since cache keys now include instance ID, we need to clear all cache entries
        // that match the changed source/target classes regardless of instance.
        if (FULL_CONVERSION_CACHE.isEmpty()) {
            return;
        }

        // Use ClassPairMap.removeIf to atomically scan+remove entries matching our predicate.
        FULL_CONVERSION_CACHE.removeIf((sourceClass, targetClass) ->
                (sourceTypes.contains(sourceClass) && targetTypes.contains(targetClass)) ||
                isInheritanceRelatedToAny(sourceClass, sourceTypes) ||
                isInheritanceRelatedToAny(targetClass, targetTypes));
    }

    private static boolean isInheritanceRelatedToAny(Class<?> keyType, Set<Class<?>> changedTypes) {
        for (Class<?> changedType : changedTypes) {
            if (keyType != changedType &&
                (changedType.isAssignableFrom(keyType) || keyType.isAssignableFrom(changedType))) {
                return true;
            }
        }
        return false;
    }

}
