package com.cedarsoftware.util.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code convert(Object, Type)} converts to a generic type: a {@code List<LocalDate>}'s elements, a
 * {@code Map<String, ZonedDateTime>}'s keys and values and an {@code Optional<UUID>}'s content are each converted to
 * their own type, which a Class target cannot name. A Class, and a generic type that asks nothing of what it holds, is
 * converted to as {@code convert(Object, Class)} converts to it. {@code isConversionSupportedFor(Type, Type)} answers
 * for it.
 */
class ConverterGenericTypeTest {
    private static final String ID = "123e4567-e89b-12d3-a456-426614174000";

    public enum Day { MON, TUE }

    /** Its one type argument is the map's value; its keys are Strings. */
    public static class Codes<V> extends LinkedHashMap<String, V> {
    }

    public static class Box<T extends Number> {
        public T value;
        public List<T> values;
    }

    public static class Bag<T> {
        public Set<T> items;
    }

    public static class Flags<E extends Enum<E>> {
        public EnumSet<E> set;
    }

    public static class Person {
        public String name = "Ann";
        public int age = 41;
    }

    /** The generic types converted to, by field name. */
    public static class Types {
        public List<LocalDate> dates;
        public Set<Integer> ints;
        public TreeSet<Integer> sorted;
        public Deque<Integer> deque;
        public Collection<Integer> collection;
        public EnumSet<Day> days;
        public Map<String, ZonedDateTime> zoned;
        public Map<Integer, Long> numbers;
        public SortedMap<Integer, String> sortedMap;
        public Map<String, Object> textKeys;
        public Codes<Integer> codes;
        public Optional<UUID> uuid;
        public Optional<Integer> number;
        public Optional<?> anything;
        public List<List<Integer>> nested;
        public Set<List<Integer>> setOfLists;
        public Map<String, List<LocalDate>> datesByName;
        public List<Integer>[] arrayOfLists;
        public List<? extends Number> extendsNumber;
        public List<? super Integer> superInteger;
        public List<?> list;
        public Set<?> set;
        public Set<Object> objects;
        public Map<Object, Object> map;
        public Comparable<String> comparable;
        public List<String> strings;
        public List<UUID> uuids;
        public Map<String, String> textMap;
        public Map<Integer, UUID> uuidMap;
        public Map<String, LocalDate> dateMap;
        public Optional<LocalDate> date;
        @SuppressWarnings("rawtypes")
        public List raw;
    }

    private static Type type(String field) {
        try {
            return Types.class.getField(field).getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A parameterized type of a class no declaration can name (Collections' wrappers). */
    private static Type parameterized(Class<?> raw, Type... args) {
        return new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return args.clone();
            }

            public Type getRawType() {
                return raw;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void collectionElements() {
        List<LocalDate> dates = converter.convert(Arrays.asList("2026-01-15", "2026-02-01"), type("dates"));
        assertEquals(Arrays.asList(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1)), dates);
        assertEquals(Collections.singletonList(LocalDate.of(2026, 1, 15)),
                converter.convert(new String[] {"2026-01-15"}, type("dates")));
        Set<Integer> ints = converter.convert(Arrays.asList("1", "2", "1"), type("ints"));
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2)), ints);
        assertInstanceOf(LinkedHashSet.class, ints);
        assertEquals(new LinkedHashSet<>(Arrays.asList(3, 4)), converter.convert(new int[] {3, 4}, type("ints")));
        // a sorted set orders its elements as converted: 9 before 10, where the text sorts "10" first
        TreeSet<Integer> sorted = converter.convert(Arrays.asList("10", "9"), type("sorted"));
        assertEquals(Arrays.asList(9, 10), new ArrayList<>(sorted));
        Deque<Integer> deque = converter.convert(Arrays.asList("5"), type("deque"));
        assertInstanceOf(ArrayDeque.class, deque);
        assertEquals(5, deque.getFirst());
        assertEquals(Collections.singletonList(5), converter.convert(Arrays.asList("5"), type("collection")));
        EnumSet<Day> days = converter.convert(Arrays.asList("MON"), type("days"));
        assertEquals(EnumSet.of(Day.MON), days);
        assertInstanceOf(EnumSet.class, days);
    }

    /** An EnumSet whose element is no enum (a type variable) is refused as convert(.., EnumSet.class) refuses it. */
    @Test
    void anEnumSetOfNoEnum() throws NoSuchFieldException {
        Type flags = Flags.class.getField("set").getGenericType();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(Arrays.asList("MON"), flags));
        assertTrue(e.getMessage().startsWith("To convert to EnumSet, specify the Enum class"), e.getMessage());
        assertFalse(converter.isConversionSupportedFor(type("strings"), flags));
    }

    /** Collections' wrapper classes: an unmodifiable list is one, an empty list holds nothing, as the Class makes them. */
    @Test
    void collectionsWrappers() {
        List<Integer> unmodifiable = converter.convert(Arrays.asList("1"),
                parameterized(CollectionsWrappers.getUnmodifiableListClass(), Integer.class));
        assertEquals(Collections.singletonList(1), unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(2));
        List<Integer> synced = converter.convert(Arrays.asList("1"),
                parameterized(CollectionsWrappers.getSynchronizedListClass(), Integer.class));
        assertEquals(Collections.singletonList(1), synced);
        assertInstanceOf(CollectionsWrappers.getSynchronizedListClass(), synced);
        List<Integer> empty = converter.convert(Arrays.asList("1"),
                parameterized(CollectionsWrappers.getEmptyListClass(), Integer.class));
        assertEquals(Collections.emptyList(), empty);
    }

    /** From what converts to the container class: its elements are then converted, and a null is null. */
    @Test
    void fromWhatConvertsToTheContainer() {
        Converter own = new Converter(new DefaultConverterOptions());
        own.addConversion((from, c) -> "none".equals(from) ? null : Arrays.asList(((String) from).split(",")),
                String.class, List.class);
        own.addConversion((from, c) -> null, String.class, Map.class);
        own.addConversion((from, c) -> null, String.class, List[].class);
        assertEquals(Arrays.asList(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1)),
                own.convert("2026-01-15,2026-02-01", type("dates")));
        assertNull(own.convert("none", type("dates")));
        assertNull(own.convert("none", type("arrayOfLists")));
        assertNull(own.convert("x", type("numbers")));
        Map<String, String> person = converter.convert(new Person(), type("textMap"));
        assertEquals("41", person.get("age"));
        assertEquals("Ann", person.get("name"));
    }

    @Test
    void mapKeysAndValues() {
        Map<String, String> text = new LinkedHashMap<>();
        text.put("a", "2026-01-15T10:00:00Z");
        Map<String, ZonedDateTime> zoned = converter.convert(text, type("zoned"));
        assertEquals(ZonedDateTime.parse("2026-01-15T10:00:00Z"), zoned.get("a"));
        Map<String, String> numbers = new LinkedHashMap<>();
        numbers.put("10", "2");
        numbers.put("9", "3");
        Map<Integer, Long> converted = converter.convert(numbers, type("numbers"));
        assertEquals(2L, converted.get(10));
        assertEquals(3L, converted.get(9));
        // a sorted map orders its keys as converted
        SortedMap<Integer, String> sortedMap = converter.convert(numbers, type("sortedMap"));
        assertEquals(Arrays.asList(9, 10), new ArrayList<>(sortedMap.keySet()));
        Map<Object, Object> numberKeys = new LinkedHashMap<>();
        numberKeys.put(1, 2);
        Map<String, Object> textKeys = converter.convert(numberKeys, type("textKeys"));
        assertEquals(2, textKeys.get("1"));
        // a map class's own type argument is its value's
        Codes<Integer> codes = converter.convert(numberKeys, type("codes"));
        assertInstanceOf(Codes.class, codes);
        assertEquals(2, codes.get("1"));
    }

    @Test
    void optionalContent() {
        UUID id = UUID.fromString(ID);
        assertEquals(Optional.of(id), converter.convert(ID, type("uuid")));
        assertEquals(Optional.of(id), converter.convert(Optional.of(ID), type("uuid")));
        assertEquals(Optional.empty(), converter.convert(null, type("uuid")));
        assertEquals(Optional.empty(), converter.convert(Optional.empty(), type("number")));
        assertEquals(Optional.of(7), converter.convert(7L, type("number")));
        assertEquals(Optional.of("x"), converter.convert("x", type("anything")));
    }

    /** An element that is a container is converted to its own type, and stays the collection type it names. */
    @Test
    void nestedTypes() {
        List<List<Integer>> nested = converter.convert(
                Arrays.asList(Arrays.asList("1"), Arrays.asList("2", "3")), type("nested"));
        assertEquals(Arrays.asList(Collections.singletonList(1), Arrays.asList(2, 3)), nested);
        Set<List<Integer>> setOfLists = converter.convert(Arrays.asList(Arrays.asList("1")), type("setOfLists"));
        assertInstanceOf(List.class, setOfLists.iterator().next());
        Map<String, Object> byName = new LinkedHashMap<>();
        byName.put("due", new String[] {"2026-01-15"});
        Map<String, List<LocalDate>> datesByName = converter.convert(byName, type("datesByName"));
        assertEquals(Collections.singletonList(LocalDate.of(2026, 1, 15)), datesByName.get("due"));
        List<Integer>[] arrayOfLists = converter.convert(Arrays.asList(Arrays.asList("1")), type("arrayOfLists"));
        assertArrayEquals(new Object[] {Collections.singletonList(1)}, arrayOfLists);
        assertThrows(IllegalArgumentException.class, () -> converter.convert(null, type("arrayOfLists")));
    }

    @Test
    void wildcardsAndTypeVariables() throws NoSuchFieldException {
        List<? extends Number> numbers = converter.convert(Arrays.asList("5"), type("extendsNumber"));
        assertEquals(converter.convert("5", Number.class), numbers.get(0));
        List<? super Integer> integers = converter.convert(Arrays.asList("5"), type("superInteger"));
        assertEquals(5, integers.get(0));
        Type t = Box.class.getField("value").getGenericType();
        assertEquals(converter.convert("5", Number.class), converter.convert("5", t));
        List<?> values = converter.convert(Arrays.asList("5"), Box.class.getField("values").getGenericType());
        assertEquals(converter.convert("5", Number.class), values.get(0));
    }

    /** A Class, and a type asking nothing of what it holds, converts as convert(Object, Class) does. */
    @Test
    void asTheClassConverts() {
        assertEquals(5, (Integer) converter.convert("5", (Type) Integer.class));
        assertEquals(converter.convert(Arrays.asList("5"), List.class), converter.convert(Arrays.asList("5"), type("list")));
        // Set<?> is Set: its nested list becomes a set, as convert(.., Set.class) makes it
        Class<?> nestedAsSet = converter.convert(Arrays.asList(Arrays.asList(1)), Set.class).iterator().next().getClass();
        for (Type asksNothing : new Type[] {type("set"), type("objects"), bagItems()}) {
            Set<?> set = converter.convert(Arrays.asList(Arrays.asList(1)), asksNothing);
            assertEquals(nestedAsSet, set.iterator().next().getClass(), asksNothing.getTypeName());
        }
        Map<Object, Object> numberKeys = new LinkedHashMap<>();
        numberKeys.put(1, "2");
        assertEquals(numberKeys, converter.convert(numberKeys, type("map")));
        assertEquals("5", converter.convert("5", type("comparable")));
        assertNull(converter.convert(null, type("numbers")));           // as convert(null, Map.class)
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert("x", (Type) null));
        assertEquals("toType cannot be null", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> converter.convert("5", type("ints")));      // as Set.class
        assertThrows(IllegalArgumentException.class, () -> converter.convert(Arrays.asList("x"), type("ints")));
    }

    private static Type bagItems() {
        try {
            return Bag.class.getField("items").getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void staticFacade() {
        Set<Integer> ints = com.cedarsoftware.util.Converter.convert(Arrays.asList("1"), type("ints"));
        assertEquals(Collections.singleton(1), ints);
        assertTrue(com.cedarsoftware.util.Converter.isConversionSupportedFor(type("strings"), type("dates")));
    }

    @Test
    void conversionSupported() {
        assertTrue(converter.isConversionSupportedFor(type("strings"), type("dates")));
        assertFalse(converter.isConversionSupportedFor(type("dates"), type("uuids")));     // LocalDate to UUID: no
        assertFalse(converter.isConversionSupportedFor(LocalDate.class, UUID.class));
        assertTrue(converter.isConversionSupportedFor(String[].class, type("dates")));
        assertFalse(converter.isConversionSupportedFor(LocalDate[].class, type("uuids")));
        assertFalse(converter.isConversionSupportedFor(type("dates"), UUID[].class));
        // where the source does not say what it holds, its elements are taken to convert
        assertTrue(converter.isConversionSupportedFor(type("raw"), type("uuids")));
        assertTrue(converter.isConversionSupportedFor(List.class, type("uuids")));
        assertTrue(converter.isConversionSupportedFor(type("list"), type("uuids")));
        // Map to Map converts its keys and values (the Class form answers no for Map to Map)
        assertTrue(converter.isConversionSupportedFor(type("textMap"), type("uuidMap")));
        assertFalse(converter.isConversionSupportedFor(type("dateMap"), type("uuidMap")));
        assertFalse(converter.isConversionSupportedFor(Map.class, Map.class));
        assertFalse(converter.isConversionSupportedFor(type("strings"), type("uuidMap")));
        assertTrue(converter.isConversionSupportedFor(type("strings"), type("days")));
        assertFalse(converter.isConversionSupportedFor(String.class, type("ints")));
        assertTrue(converter.isConversionSupportedFor(String.class, type("uuid")));
        assertFalse(converter.isConversionSupportedFor(LocalDate.class, type("uuid")));
        assertFalse(converter.isConversionSupportedFor(type("date"), type("uuid")));
        assertTrue(converter.isConversionSupportedFor(Optional.class, type("uuid")));
        assertTrue(converter.isConversionSupportedFor(LocalDate.class, type("anything")));
        assertFalse(converter.isConversionSupportedFor(type("uuids"), Optional.class));
        assertTrue(converter.isConversionSupportedFor(type("strings"), type("extendsNumber")));
        assertTrue(converter.isConversionSupportedFor(type("strings"), type("superInteger")));     // String to Integer
        assertThrows(IllegalArgumentException.class, () -> converter.isConversionSupportedFor((Type) null, type("ints")));
        // a null beside a class is answered as the Class form answers it (Groovy passes a null to this form)
        String asTheClassForm = assertThrows(IllegalArgumentException.class,
                () -> converter.isConversionSupportedFor((Class<?>) null, String.class)).getMessage();
        assertEquals(asTheClassForm, assertThrows(IllegalArgumentException.class,
                () -> converter.isConversionSupportedFor((Type) null, (Type) String.class)).getMessage());
    }
}
