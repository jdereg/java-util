package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Class for testing the Converter's ability to convert between Arrays and Collections,
 * including specialized handling for EnumSet conversions.
 */
class ConverterArrayCollectionTest {

    private Converter converter;

    /**
     * Enum used for EnumSet conversion tests.
     */
    private enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    @BeforeEach
    void setUp() {
        ConverterOptions options = new DefaultConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Nested test class for Array to Collection and Collection to Array conversions.
     */
    @Nested
    @DisplayName("Array and Collection Conversion Tests")
    class ArrayCollectionConversionTests {

        /**
         * Helper method to create a sample int array.
         */
        private int[] createSampleIntArray() {
            return new int[]{1, 2, 3, 4, 5};
        }

        /**
         * Helper method to create a sample Integer array.
         */
        private Integer[] createSampleIntegerArray() {
            return new Integer[]{1, 2, 3, 4, 5};
        }

        /**
         * Helper method to create a sample String array.
         */
        private String[] createSampleStringArray() {
            return new String[]{"apple", "banana", "cherry"};
        }

        /**
         * Helper method to create a sample Date array.
         */
        private Date[] createSampleDateArray() {
            return new Date[]{new Date(0), new Date(100000), new Date(200000)};
        }

        /**
         * Helper method to create a sample UUID array.
         */
        private UUID[] createSampleUUIDArray() {
            return new UUID[]{
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            };
        }

        /**
         * Helper method to create a sample ZonedDateTime array.
         */
        private ZonedDateTime[] createSampleZonedDateTimeArray() {
            return new ZonedDateTime[]{
                    ZonedDateTime.now(),
                    ZonedDateTime.now().plusDays(1),
                    ZonedDateTime.now().plusDays(2)
            };
        }

        @Test
        void testEmptyCollectionConversion() {
            List<String> emptyList = new ArrayList<>();
            Set<String> emptySet = converter.convert(emptyList, Set.class);
            assertTrue(emptySet.isEmpty());
        }

        @Test
        void testCollectionOrderPreservation() {
            List<String> orderedList = Arrays.asList("a", "b", "c");

            // To LinkedHashSet (should preserve order)
            LinkedHashSet<String> linkedSet = converter.convert(orderedList, LinkedHashSet.class);
            Iterator<String> iter = linkedSet.iterator();
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
            assertEquals("c", iter.next());

            // To ArrayList (should preserve order)
            ArrayList<String> arrayList = converter.convert(orderedList, ArrayList.class);
            assertEquals(orderedList, arrayList);
        }

        @Test
        void testMixedTypeCollectionConversion() {
            List<Object> mixed = Arrays.asList("1", 2, 3.0);
            List<Integer> integers = converter.convert(mixed, List.class);
            assertEquals(Arrays.asList("1", 2, 3.0), integers); // Generics don't influence conversion
        }
        
        @Test
        @DisplayName("Convert int[] to List<Integer> and back")
        void testIntArrayToListAndBack() {
            int[] intArray = createSampleIntArray();
            List<Integer> integerList = converter.convert(intArray, List.class);
            assertNotNull(integerList, "Converted list should not be null");
            assertEquals(intArray.length, integerList.size(), "List size should match array length");

            for (int i = 0; i < intArray.length; i++) {
                assertEquals(intArray[i], integerList.get(i), "List element should match array element");
            }

            // Convert back to int[]
            int[] convertedBack = converter.convert(integerList, int[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertArrayEquals(intArray, convertedBack, "Round-trip conversion should maintain array integrity");
        }

        @Test
        @DisplayName("Convert Integer[] to Set<Integer> and back")
        void testIntegerArrayToSetAndBack() {
            Integer[] integerArray = createSampleIntegerArray();
            Set<Integer> integerSet = converter.convert(integerArray, Set.class);
            assertNotNull(integerSet, "Converted set should not be null");
            assertEquals(new HashSet<>(Arrays.asList(integerArray)).size(), integerSet.size(), "Set size should match unique elements in array");

            for (Integer val : integerArray) {
                assertTrue(integerSet.contains(val), "Set should contain all elements from array");
            }

            // Convert back to Integer[]
            Integer[] convertedBack = converter.convert(integerSet, Integer[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertEquals(integerSet.size(), convertedBack.length, "Array size should match set size");
            assertTrue(integerSet.containsAll(Arrays.asList(convertedBack)), "Converted back array should contain all elements from set");
        }

        @Test
        @DisplayName("Convert String[] to ArrayList<String> and back")
        void testStringArrayToArrayListAndBack() {
            String[] stringArray = createSampleStringArray();
            ArrayList<String> stringList = converter.convert(stringArray, ArrayList.class);
            assertNotNull(stringList, "Converted ArrayList should not be null");
            assertEquals(stringArray.length, stringList.size(), "List size should match array length");

            for (int i = 0; i < stringArray.length; i++) {
                assertEquals(stringArray[i], stringList.get(i), "List element should match array element");
            }

            // Convert back to String[]
            String[] convertedBack = converter.convert(stringList, String[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertArrayEquals(stringArray, convertedBack, "Round-trip conversion should maintain array integrity");
        }

        @Test
        @DisplayName("Convert Date[] to LinkedHashSet<Date> and back")
        void testDateArrayToLinkedHashSetAndBack() {
            Date[] dateArray = createSampleDateArray();
            LinkedHashSet<Date> dateSet = converter.convert(dateArray, LinkedHashSet.class);
            assertNotNull(dateSet, "Converted LinkedHashSet should not be null");
            assertEquals(dateArray.length, dateSet.size(), "Set size should match array length");

            for (Date date : dateArray) {
                assertTrue(dateSet.contains(date), "Set should contain all elements from array");
            }

            // Convert back to Date[]
            Date[] convertedBack = converter.convert(dateSet, Date[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertEquals(dateSet.size(), convertedBack.length, "Array size should match set size");
            assertTrue(dateSet.containsAll(Arrays.asList(convertedBack)), "Converted back array should contain all elements from set");
        }

        @Test
        @DisplayName("Convert UUID[] to ConcurrentSkipListSet<UUID> and back")
        void testUUIDArrayToConcurrentSkipListSetAndBack() {
            UUID[] uuidArray = createSampleUUIDArray();
            ConcurrentSkipListSet<UUID> uuidSet = converter.convert(uuidArray, ConcurrentSkipListSet.class);
            assertNotNull(uuidSet, "Converted ConcurrentSkipListSet should not be null");
            assertEquals(new TreeSet<>(Arrays.asList(uuidArray)).size(), uuidSet.size(), "Set size should match unique elements in array");

            for (UUID uuid : uuidArray) {
                assertTrue(uuidSet.contains(uuid), "Set should contain all elements from array");
            }

            // Convert back to UUID[]
            UUID[] convertedBack = converter.convert(uuidSet, UUID[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertEquals(uuidSet.size(), convertedBack.length, "Array size should match set size");
            assertTrue(uuidSet.containsAll(Arrays.asList(convertedBack)), "Converted back array should contain all elements from set");
        }

        @Test
        @DisplayName("Convert ZonedDateTime[] to List<ZonedDateTime> and back")
        void testZonedDateTimeArrayToListAndBack() {
            ZonedDateTime[] zdtArray = createSampleZonedDateTimeArray();
            List<ZonedDateTime> zdtList = converter.convert(zdtArray, List.class);
            assertNotNull(zdtList, "Converted List<ZonedDateTime> should not be null");
            assertEquals(zdtArray.length, zdtList.size(), "List size should match array length");

            for (int i = 0; i < zdtArray.length; i++) {
                assertEquals(zdtArray[i], zdtList.get(i), "List element should match array element");
            }

            // Convert back to ZonedDateTime[]
            ZonedDateTime[] convertedBack = converter.convert(zdtList, ZonedDateTime[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertArrayEquals(zdtArray, convertedBack, "Round-trip conversion should maintain array integrity");
        }

        @Test
        @DisplayName("Convert AtomicBoolean[] to Set<AtomicBoolean> and back")
        void testAtomicBooleanArrayToSetAndBack() {
            AtomicBoolean[] atomicBooleanArray = new AtomicBoolean[]{
                    new AtomicBoolean(true),
                    new AtomicBoolean(false),
                    new AtomicBoolean(true)
            };

            // Convert AtomicBoolean[] to Set<AtomicBoolean>
            Set<AtomicBoolean> atomicBooleanSet = converter.convert(atomicBooleanArray, Set.class);
            assertNotNull(atomicBooleanSet, "Converted Set<AtomicBoolean> should not be null");
            assertEquals(3, atomicBooleanSet.size(), "Set size should match unique elements in array");

            // Check that the Set contains the unique AtomicBoolean instances
            Set<Boolean> uniqueBooleans = new HashSet<>();
            for (AtomicBoolean ab : atomicBooleanArray) {
                uniqueBooleans.add(ab.get());
            }

            // Check that the Set contains the expected unique values based on boolean values
            for (AtomicBoolean ab : atomicBooleanSet) {
                assertTrue(uniqueBooleans.contains(ab.get()), "Set should contain unique boolean values from array");
            }

            // Convert back to AtomicBoolean[]
            AtomicBoolean[] convertedBack = converter.convert(atomicBooleanSet, AtomicBoolean[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertEquals(atomicBooleanSet.size(), convertedBack.length, "Array size should match set size");

            // Check that the converted array contains the correct boolean values
            Set<Boolean> convertedBackBooleans = new HashSet<>();
            for (AtomicBoolean ab : convertedBack) {
                convertedBackBooleans.add(ab.get());
            }

            assertTrue(uniqueBooleans.equals(convertedBackBooleans), "Converted back array should contain the same boolean values as the set");
        }

        @Test
        @DisplayName("Convert BigInteger[] to List<BigInteger> and back")
        void testBigIntegerArrayToListAndBack() {
            BigInteger[] bigIntegerArray = new BigInteger[]{
                    BigInteger.ONE,
                    BigInteger.TEN,
                    BigInteger.ONE // Duplicate to test List duplication
            };
            List<BigInteger> bigIntegerList = converter.convert(bigIntegerArray, List.class);
            assertNotNull(bigIntegerList, "Converted List<BigInteger> should not be null");
            assertEquals(bigIntegerArray.length, bigIntegerList.size(), "List size should match array length");

            for (int i = 0; i < bigIntegerArray.length; i++) {
                assertEquals(bigIntegerArray[i], bigIntegerList.get(i), "List element should match array element");
            }

            // Convert back to BigInteger[]
            BigInteger[] convertedBack = converter.convert(bigIntegerList, BigInteger[].class);
            assertNotNull(convertedBack, "Converted back array should not be null");
            assertArrayEquals(bigIntegerArray, convertedBack, "Round-trip conversion should maintain array integrity");
        }

        @Test
        void testMultidimensionalArrayConversion() {
            Integer[][] source = {{1, 2}, {3, 4}};
            Long[][] converted = converter.convert(source, Long[][].class);
            assertEquals(2, converted.length);
            assertArrayEquals(new Long[]{1L, 2L}, converted[0]);
            assertArrayEquals(new Long[]{3L, 4L}, converted[1]);
        }
    }

    /**
     * Nested test class for EnumSet-specific conversion tests.
     */
    @Nested
    @DisplayName("EnumSet Conversion Tests")
    class EnumSetConversionTests {
        @Test
        void testEnumSetWithNullElements() {
            Object[] arrayWithNull = {Day.MONDAY, null, Day.FRIDAY};
            EnumSet<Day> enumSet = (EnumSet<Day>)(Object)converter.convert(arrayWithNull, Day.class);
            assertEquals(2, enumSet.size()); // Nulls should be skipped
            assertTrue(enumSet.contains(Day.MONDAY));
            assertTrue(enumSet.contains(Day.FRIDAY));
        }
        
        @Test
        void testEnumSetToCollectionPreservesOrder() {
            EnumSet<Day> days = EnumSet.of(Day.FRIDAY, Day.MONDAY, Day.WEDNESDAY);
            List<Day> list = converter.convert(days, ArrayList.class);
            // EnumSet maintains natural enum order regardless of insertion order
            assertEquals(Arrays.asList(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY), list);
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to String[]")
        void testEnumSetToStringArray() {
            EnumSet<Day> daySet = EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY);
            String[] stringArray = converter.convert(daySet, String[].class);
            assertNotNull(stringArray, "Converted String[] should not be null");
            assertEquals(daySet.size(), stringArray.length, "String array size should match EnumSet size");

            List<String> expected = Arrays.asList("MONDAY", "WEDNESDAY", "FRIDAY");
            assertTrue(Arrays.asList(stringArray).containsAll(expected), "String array should contain all Enum names");
        }

        @Test
        @DisplayName("Convert String[] to EnumSet<Day>")
        void testStringArrayToEnumSet() {
            String[] stringArray = {"MONDAY", "WEDNESDAY", "FRIDAY"};
            EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(stringArray, Day.class);
            assertNotNull(daySet, "Converted EnumSet should not be null");
            assertEquals(3, daySet.size(), "EnumSet size should match array length");

            assertTrue(daySet.contains(Day.MONDAY), "EnumSet should contain MONDAY");
            assertTrue(daySet.contains(Day.WEDNESDAY), "EnumSet should contain WEDNESDAY");
            assertTrue(daySet.contains(Day.FRIDAY), "EnumSet should contain FRIDAY");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to int[]")
        void testEnumSetToIntArray() {
            EnumSet<Day> daySet = EnumSet.of(Day.TUESDAY, Day.THURSDAY);
            int[] intArray = converter.convert(daySet, int[].class);
            assertNotNull(intArray, "Converted int[] should not be null");
            assertEquals(daySet.size(), intArray.length, "int array size should match EnumSet size");

            List<Integer> expected = Arrays.asList(Day.TUESDAY.ordinal(), Day.THURSDAY.ordinal());
            for (int ordinal : intArray) {
                assertTrue(expected.contains(ordinal), "int array should contain correct Enum ordinals");
            }
        }

        @Test
        @DisplayName("Convert int[] to EnumSet<Day>")
        void testIntArrayToEnumSet() {
            int[] intArray = {Day.MONDAY.ordinal(), Day.FRIDAY.ordinal()};
            Object result = converter.convert(intArray, Day.class);
            EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(intArray, Day.class);
            assertNotNull(daySet, "Converted EnumSet should not be null");
            assertEquals(2, daySet.size(), "EnumSet size should match array length");

            assertTrue(daySet.contains(Day.MONDAY), "EnumSet should contain MONDAY");
            assertTrue(daySet.contains(Day.FRIDAY), "EnumSet should contain FRIDAY");

            assertNotNull(daySet, "Converted EnumSet should not be null");
            assertEquals(2, daySet.size(), "EnumSet size should match array length");

            assertTrue(daySet.contains(Day.MONDAY), "EnumSet should contain MONDAY");
            assertTrue(daySet.contains(Day.FRIDAY), "EnumSet should contain FRIDAY");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to Object[]")
        void testEnumSetToObjectArray() {
            EnumSet<Day> daySet = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
            Object[] objectArray = converter.convert(daySet, Object[].class);
            assertNotNull(objectArray, "Converted Object[] should not be null");
            assertEquals(daySet.size(), objectArray.length, "Object array size should match EnumSet size");

            for (Object obj : objectArray) {
                assertTrue(obj instanceof Day, "Object array should contain Day enums");
                assertTrue(daySet.contains(obj), "Object array should contain the same Enums as the source EnumSet");
            }
        }

        @Test
        @DisplayName("Convert Object[] to EnumSet<Day>")
        void testObjectArrayToEnumSet() {
            Object[] objectArray = {Day.MONDAY, Day.SUNDAY};
            EnumSet<Day> daySet = (EnumSet<Day>) (Object)converter.convert(objectArray, Day.class);
            assertNotNull(daySet, "Converted EnumSet should not be null");
            assertEquals(2, daySet.size(), "EnumSet size should match array length");

            assertTrue(daySet.contains(Day.MONDAY), "EnumSet should contain MONDAY");
            assertTrue(daySet.contains(Day.SUNDAY), "EnumSet should contain SUNDAY");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to Class[]")
        void testEnumSetToClassArray() {
            EnumSet<Day> daySet = EnumSet.of(Day.TUESDAY);
            Class<?>[] classArray = converter.convert(daySet, Class[].class);
            assertNotNull(classArray, "Converted Class[] should not be null");
            assertEquals(daySet.size(), classArray.length, "Class array size should match EnumSet size");

            for (Class<?> cls : classArray) {
                assertEquals(Day.class, cls, "Class array should contain the declaring class of the Enums");
            }
        }

        @Test
        @DisplayName("Convert Class[] to EnumSet<Day> should throw IllegalArgumentException")
        void testClassArrayToEnumSetShouldThrow() {
            Class<?>[] classArray = {Day.class};
            Executable conversion = () -> converter.convert(classArray, EnumSet.class);
            assertThrows(IllegalArgumentException.class, conversion, "To convert to EnumSet, specify the Enum class to convert to.  See convert() Javadoc for example.");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to EnumSet<Day> (identity conversion)")
        void testEnumSetToEnumSetIdentityConversion() {
            EnumSet<Day> daySet = EnumSet.of(Day.WEDNESDAY, Day.THURSDAY);
            EnumSet<Day> convertedSet = (EnumSet<Day>) (Object) converter.convert(daySet, Day.class);
            assertNotNull(convertedSet, "Converted EnumSet should not be null");
            assertEquals(daySet, convertedSet, "Converted EnumSet should be equal to the source EnumSet");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to Collection and verify Enums")
        void testEnumSetToCollection() {
            EnumSet<Day> daySet = EnumSet.of(Day.FRIDAY, Day.SATURDAY);
            Collection<Day> collection = converter.convert(daySet, Collection.class);
            assertNotNull(collection, "Converted Collection should not be null");
            assertEquals(daySet.size(), collection.size(), "Collection size should match EnumSet size");
            assertTrue(collection.containsAll(daySet), "Collection should contain all Enums from the source EnumSet");
        }

        @Test
        @DisplayName("Convert EnumSet<Day> to Object[] and back, verifying correctness")
        void testEnumSetToStringArrayAndBack() {
            EnumSet<Day> originalSet = EnumSet.of(Day.MONDAY, Day.THURSDAY);
            Object[] objectArray = converter.convert(originalSet, Object[].class);
            assertNotNull(objectArray, "Converted Object[] should not be null");
            assertEquals(originalSet.size(), objectArray.length, "String array size should match EnumSet size");

            EnumSet<Day> convertedSet = (EnumSet<Day>) (Object)converter.convert(objectArray, Day.class);
            assertNotNull(convertedSet, "Converted back EnumSet should not be null");
            assertEquals(originalSet, convertedSet, "Round-trip conversion should maintain EnumSet integrity");
        }
    }

    /**
     * Nested test class for Set to Set conversions.
     */
    @Nested
    @DisplayName("Set to Set Conversion Tests")
    class SetConversionTests {

        @Test
        @DisplayName("Convert HashSet<String> to LinkedHashSet<String> and verify contents")
        void testHashSetToLinkedHashSet() {
            HashSet<String> hashSet = new HashSet<>(Arrays.asList("apple", "banana", "cherry"));
            LinkedHashSet<String> linkedHashSet = converter.convert(hashSet, LinkedHashSet.class);
            assertNotNull(linkedHashSet, "Converted LinkedHashSet should not be null");
            assertEquals(hashSet.size(), linkedHashSet.size(), "LinkedHashSet size should match HashSet size");
            assertTrue(linkedHashSet.containsAll(hashSet), "LinkedHashSet should contain all elements from HashSet");
        }

        @Test
        @DisplayName("Convert LinkedHashSet<String> to ConcurrentSkipListSet<String> and verify contents")
        void testLinkedHashSetToConcurrentSkipListSet() {
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("delta", "alpha", "charlie"));
            ConcurrentSkipListSet<String> skipListSet = converter.convert(linkedHashSet, ConcurrentSkipListSet.class);
            assertNotNull(skipListSet, "Converted ConcurrentSkipListSet should not be null");
            assertEquals(linkedHashSet.size(), skipListSet.size(), "ConcurrentSkipListSet size should match LinkedHashSet size");
            assertTrue(skipListSet.containsAll(linkedHashSet), "ConcurrentSkipListSet should contain all elements from LinkedHashSet");
        }

        @Test
        @DisplayName("Convert Set<Day> to EnumSet<Day> and verify contents")
        void testSetToEnumSet() {
            Set<Day> daySet = new HashSet<>(Arrays.asList(Day.SUNDAY, Day.TUESDAY, Day.THURSDAY));
            EnumSet<Day> enumSet = (EnumSet<Day>) (Object)converter.convert(daySet, Day.class);
            assertNotNull(enumSet, "Converted EnumSet should not be null");
            assertEquals(daySet.size(), enumSet.size(), "EnumSet size should match Set size");
            assertTrue(enumSet.containsAll(daySet), "EnumSet should contain all Enums from the source Set");
        }
    }

    /**
     * Nested test class for List-specific conversion tests.
     */
    @Nested
    @DisplayName("List Conversion Tests")
    class ListConversionTests {

        @Test
        @DisplayName("Convert ArrayList<String> with duplicates to LinkedList<String> and verify duplicates")
        void testArrayListToLinkedListWithDuplicates() {
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "cherry", "banana"));
            LinkedList<String> linkedList = converter.convert(arrayList, LinkedList.class);
            assertNotNull(linkedList, "Converted LinkedList should not be null");
            assertEquals(arrayList.size(), linkedList.size(), "LinkedList size should match ArrayList size");
            for (int i = 0; i < arrayList.size(); i++) {
                assertEquals(arrayList.get(i), linkedList.get(i), "List elements should match at each index");
            }
        }

        @Test
        @DisplayName("Convert ArrayList<Integer> with duplicates to List<Integer> and verify duplicates")
        void testArrayListToListWithDuplicates() {
            ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(1, 2, 2, 3, 4, 4, 4, 5));
            List<Integer> list = converter.convert(arrayList, List.class);
            assertNotNull(list, "Converted List should not be null");
            assertEquals(arrayList.size(), list.size(), "List size should match ArrayList size");
            assertEquals(arrayList, list, "List should maintain the order and duplicates of the ArrayList");
        }

        @Test
        @DisplayName("Convert ArrayList<String> with duplicates to ArrayList<String> and verify duplicates")
        void testArrayListToArrayListWithDuplicates() {
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("one", "two", "two", "three", "three", "three"));
            ArrayList<String> convertedList = converter.convert(arrayList, ArrayList.class);
            assertNotNull(convertedList, "Converted ArrayList should not be null");
            assertEquals(arrayList.size(), convertedList.size(), "Converted ArrayList size should match original");
            assertEquals(arrayList, convertedList, "Converted ArrayList should maintain duplicates and order");
        }
    }

    /**
     * Nested test class for Primitive Array Conversions.
     */
    @Nested
    @DisplayName("Primitive Array Conversions")
    class PrimitiveArrayConversionTests {

        @Test
        void testPrimitiveArrayToWrapperArray() {
            int[] primitiveInts = {1, 2, 3};
            Integer[] wrapperInts = converter.convert(primitiveInts, Integer[].class);
            assertArrayEquals(new Integer[]{1, 2, 3}, wrapperInts);
        }
        
        @Test
        @DisplayName("Convert int[] to long[] and back without exceeding Integer.MAX_VALUE")
        void testIntArrayToLongArrayAndBack() {
            int[] intArray = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
            long[] longArray = converter.convert(intArray, long[].class);
            assertNotNull(longArray, "Converted long[] should not be null");
            assertEquals(intArray.length, longArray.length, "long[] length should match int[] length");

            for (int i = 0; i < intArray.length; i++) {
                assertEquals((long) intArray[i], longArray[i], "long array element should match int array element converted to long");
            }

            // Convert back to int[]
            int[] convertedBack = converter.convert(longArray, int[].class);
            assertNotNull(convertedBack, "Converted back int[] should not be null");
            assertArrayEquals(intArray, convertedBack, "Round-trip conversion should maintain int array integrity");
        }

        @Test
        @DisplayName("Convert long[] to int[] without exceeding Integer.MAX_VALUE")
        void testLongArrayToIntArray() {
            long[] longArray = {Integer.MIN_VALUE, -1L, 0L, 1L, Integer.MAX_VALUE};
            int[] intArray = converter.convert(longArray, int[].class);
            assertNotNull(intArray, "Converted int[] should not be null");
            assertEquals(longArray.length, intArray.length, "int[] length should match long[] length");

            for (int i = 0; i < longArray.length; i++) {
                assertEquals((int) longArray[i], intArray[i], "int array element should match long array element cast to int");
            }
        }

        @Test
        @DisplayName("Convert char[] to String[] with single-character Strings")
        void testCharArrayToStringArray() {
            char[] charArray = {'x', 'y', 'z'};
            String[] stringArray = converter.convert(charArray, String[].class);
            assertNotNull(stringArray, "Converted String[] should not be null");
            assertEquals(charArray.length, stringArray.length, "String[] length should match char[] length");

            for (int i = 0; i < charArray.length; i++) {
                assertEquals(String.valueOf(charArray[i]), stringArray[i], "String array element should be single-character String matching char array element");
            }
        }

        @Test
        @DisplayName("Convert ZonedDateTime[] to String[] and back, verifying correctness")
        void testZonedDateTimeArrayToStringArrayAndBack() {
            ZonedDateTime[] zdtArray = {
                    ZonedDateTime.parse("2024-04-27T10:15:30+01:00[Europe/London]", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    ZonedDateTime.parse("2024-05-01T12:00:00+02:00[Europe/Berlin]", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    ZonedDateTime.parse("2024-06-15T08:45:00-04:00[America/New_York]", DateTimeFormatter.ISO_ZONED_DATE_TIME)
            };
            String[] stringArray = converter.convert(zdtArray, String[].class);
            assertNotNull(stringArray, "Converted String[] should not be null");
            assertEquals(zdtArray.length, stringArray.length, "String[] length should match ZonedDateTime[] length");

            for (int i = 0; i < zdtArray.length; i++) {
                assertEquals(zdtArray[i].format(DateTimeFormatter.ISO_ZONED_DATE_TIME), stringArray[i], "String array element should match ZonedDateTime formatted string");
            }

            // Convert back to ZonedDateTime[]
            ZonedDateTime[] convertedBack = converter.convert(stringArray, ZonedDateTime[].class);
            assertNotNull(convertedBack, "Converted back ZonedDateTime[] should not be null");
            assertArrayEquals(zdtArray, convertedBack, "Round-trip conversion should maintain ZonedDateTime array integrity");
        }
    }

    /**
     * Nested test class for Unsupported Conversions.
     */
    @Nested
    @DisplayName("Unsupported Conversion Tests")
    class UnsupportedConversionTests {

        @Test
        @DisplayName("Convert String[] to char[] works if String is one character or is unicode digits that conver to a character")
        void testStringArrayToCharArrayWorksIfOneChar() {
            String[] stringArray = {"a", "b", "c"};
            char[] chars = converter.convert(stringArray, char[].class);
            assert chars.length == 3;
            assertEquals('a', chars[0]);
            assertEquals('b', chars[1]);
            assertEquals('c', chars[2]);
        }

        @Test
        @DisplayName("Convert String[] to char[] should throw IllegalArgumentException")
        void testStringArrayToCharArrayThrows() {
            String[] stringArray = {"alpha", "bravo", "charlie"};
            Executable conversion = () -> converter.convert(stringArray, char[].class);
            assertThrows(IllegalArgumentException.class, conversion, "Converting String[] to char[] should throw IllegalArgumentException if any Strings have more than 1 character");
        }
    }
}