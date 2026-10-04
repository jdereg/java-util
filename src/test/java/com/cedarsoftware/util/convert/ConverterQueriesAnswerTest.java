package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Converter's "is this supported?" methods answer; they do not throw. Two things made them throw:
 * <ul>
 *   <li>An EnumSet target: isContainerConversionSupported() threw "specify the Enum class", and
 *       isConversionSupportedFor() through it -- for every array, collection or map source.</li>
 *   <li>A sort: the inheritance pairs were ordered by a comparator mixing assignability (which orders only related
 *       types) with inheritance distance -- not transitive, so TimSort threw "Comparison method violates its general
 *       contract!" for a Collection source and a Number target, from convert() too.</li>
 * </ul>
 */
class ConverterQueriesAnswerTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static Set<Class<?>> types() {
        Set<Class<?>> types = new LinkedHashSet<>(Converter.allSupportedConversions().keySet());
        for (Set<Class<?>> targets : Converter.allSupportedConversions().values()) {
            types.addAll(targets);
        }
        types.addAll(Arrays.asList(Object.class, TimeUnit.class, CharSequence.class, Comparable.class, Number.class,
                Iterable.class, Collection.class, List.class, Set.class, SortedSet.class, Map.class, String[].class,
                int[].class, Object[].class, Long[].class, int.class, long.class, double.class, boolean.class,
                EnumSet.class, ArrayList.class, LinkedList.class, HashSet.class, TreeSet.class, Vector.class,
                ArrayDeque.class, HashMap.class, TreeMap.class, EnumSet.noneOf(TimeUnit.class).getClass(),
                Collections.unmodifiableList(new ArrayList<>()).getClass(), Arrays.asList(1).getClass()));
        return types;
    }

    @Test
    void everyQueryAnswersWithoutThrowing() {
        Set<Class<?>> types = types();
        List<String> threw = new ArrayList<>();
        for (Class<?> source : types) {
            try {
                converter.isSimpleTypeConversionSupported(source);
            } catch (RuntimeException e) {
                threw.add("isSimpleTypeConversionSupported(" + source.getSimpleName() + "): " + e.getMessage());
            }
            for (Class<?> target : types) {
                try {
                    converter.isConversionSupportedFor(source, target);
                    converter.isSimpleTypeConversionSupported(source, target);
                    Converter.isContainerConversionSupported(source, target);
                } catch (RuntimeException e) {
                    threw.add(source.getSimpleName() + " -> " + target.getSimpleName() + ": " + e.getMessage());
                }
            }
        }
        assertTrue(threw.isEmpty(),
                threw.size() + " queries threw, e.g. " + threw.subList(0, Math.min(5, threw.size())));
    }

    @Test
    void anEnumSetTargetIsNotSupportedButConvertStillExplains() {
        assertFalse(Converter.isContainerConversionSupported(String[].class, EnumSet.class));
        assertFalse(converter.isConversionSupportedFor(ArrayList.class, EnumSet.class));
        assertFalse(converter.isConversionSupportedFor(Object[].class, EnumSet.class));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(new String[]{"SECONDS"}, EnumSet.class));
        assertTrue(e.getMessage().contains("specify the Enum class"), e.getMessage());

        // The way it can be converted: name the enum class
        Object set = converter.convert(new String[]{"SECONDS"}, TimeUnit.class);
        assertEquals(EnumSet.of(TimeUnit.SECONDS), set);
    }

    @Test
    void aCollectionToANumberIsSimplyUnsupported() {
        for (Class<?> source : Arrays.asList(ArrayList.class, HashSet.class, TreeSet.class, Vector.class)) {
            for (Class<?> target : Arrays.asList(Double.class, Float.class, Integer.class, Long.class)) {
                assertFalse(converter.isConversionSupportedFor(source, target), source + " -> " + target);
            }
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(new ArrayList<>(Arrays.asList(1, 2)), Double.class));
        assertTrue(e.getMessage().contains("Unsupported conversion"), "was: " + e.getMessage());
    }

    /** The ordering the sort now uses must keep the documented rule: a more specific target before its supertypes. */
    @Test
    void theInheritanceOrderPutsASubtypeTargetBeforeItsSupertypes() throws Exception {
        Method sorted = Converter.class.getDeclaredMethod("getSortedInheritancePairs", Class.class, Class.class);
        sorted.setAccessible(true);
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$InheritancePair");
        Field targetField = pairClass.getDeclaredField("target");
        targetField.setAccessible(true);

        Class<?>[][] cases = {{ArrayList.class, Double.class}, {TreeSet.class, Long.class},
                {java.sql.Timestamp.class, java.util.Date.class}, {Integer.class, Long.class},
                {StringBuilder.class, CharSequence.class}, {LinkedHashSet.class, Collection.class},
                {String[].class, Object[].class}, {Integer[].class, Number[].class}, {ArrayList.class, Iterable.class}};
        for (Class<?>[] c : cases) {
            Object[] pairs = (Object[]) sorted.invoke(null, c[0], c[1]);
            for (int i = 0; i < pairs.length; i++) {
                Class<?> earlier = (Class<?>) targetField.get(pairs[i]);
                for (int j = i + 1; j < pairs.length; j++) {
                    Class<?> later = (Class<?>) targetField.get(pairs[j]);
                    boolean laterIsStrictSubtype = later != earlier && earlier.isAssignableFrom(later);
                    assertFalse(laterIsStrictSubtype && earlier != c[1],
                            c[0].getSimpleName() + "->" + c[1].getSimpleName() + ": " + later.getSimpleName()
                                    + " is a subtype of " + earlier.getSimpleName() + " but sorts after it");
                }
            }
        }
    }

    /**
     * isConversionSupportedFor(Void, int) found the conversion through the wrapper (Void to Integer, which gives
     * null) and cached that converter under (Void, int); convert(null, int.class) -- 0 -- then answered from the
     * cache with null, for every later caller in the JVM. The cache is shared, so the entry is emptied first:
     * otherwise a conversion made earlier in the run would hide the bug.
     */
    @Test
    void askingAboutAPrimitiveTargetDoesNotChangeWhatConvertGives() throws Exception {
        Field cacheField = Converter.class.getDeclaredField("FULL_CONVERSION_CACHE");
        cacheField.setAccessible(true);
        Object cache = cacheField.get(null);
        Method remove = cache.getClass().getDeclaredMethod("remove", Class.class, Class.class);
        remove.setAccessible(true);
        Class<?>[][] pairs = {{Void.class, int.class}, {Void.class, long.class}, {Void.class, double.class},
                {Void.class, boolean.class}, {Void.class, char.class}};
        for (Class<?>[] pair : pairs) {
            remove.invoke(cache, pair[0], pair[1]);
            assertTrue(converter.isConversionSupportedFor(pair[0], pair[1]));
            Object converted = converter.convert(null, pair[1]);
            assertEquals(defaultOf(pair[1]), converted,
                    "convert(null, " + pair[1] + ") after asking whether it is supported");
        }
    }

    private static Object defaultOf(Class<?> primitive) {
        if (primitive == int.class) return 0;
        if (primitive == long.class) return 0L;
        if (primitive == double.class) return 0.0d;
        if (primitive == boolean.class) return false;
        if (primitive == char.class) return (char) 0;
        throw new IllegalArgumentException(primitive.toString());
    }
}
