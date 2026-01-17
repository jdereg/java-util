package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Universal conversion bridges that can handle multiple types through common patterns.
 * This class implements the bridge pattern to reduce code duplication while maintaining
 * full conversion functionality.
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
final class UniversalConversions {

    private UniversalConversions() {}

    /**
     * Universal toString bridge for any object that has a meaningful toString() implementation.
     * This replaces dozens of individual toString() conversions with a single bridge.
     *
     * For objects that need specialized string formatting, use their dedicated conversion class.
     * This bridge is suitable for:
     * - Primitive wrappers (Boolean, Integer, Long, etc.)
     * - Atomic types (AtomicBoolean, AtomicInteger, AtomicLong)
     * - Simple value objects (UUID, BigInteger, BigDecimal)
     * - Time types that have ISO-8601 toString() (Duration, Period, etc.)
     */
    static String toString(Object from, Converter converter) {
        if (from == null) {
            return null;
        }
        return from.toString();
    }

    /**
     * Universal toMap bridge for simple value objects.
     * Creates a Map with a single "_v" key containing the object.
     * This replaces dozens of individual MapConversions::initMap calls.
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, from);
        return target;
    }

    /**
     * AtomicBoolean → Map conversion.
     * Creates a Map with the boolean value (not the AtomicBoolean wrapper).
     */
    static Map<String, Object> atomicBooleanToMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, ((AtomicBoolean) from).get());
        return target;
    }

    /**
     * AtomicInteger → Map conversion.
     * Creates a Map with the int value (not the AtomicInteger wrapper).
     */
    static Map<String, Object> atomicIntegerToMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, ((AtomicInteger) from).get());
        return target;
    }

    /**
     * AtomicLong → Map conversion.
     * Creates a Map with the long value (not the AtomicLong wrapper).
     */
    static Map<String, Object> atomicLongToMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, ((AtomicLong) from).get());
        return target;
    }

    // ========================================
    // String Builder → String Bridge Methods
    // ========================================

    /**
     * Universal bridge: StringBuilder → String.
     * Extracts the String value from StringBuilder for further conversion.
     */
    static String stringBuilderToString(Object from, Converter converter) {
        StringBuilder sb = (StringBuilder) from;
        return sb.toString();
    }

    /**
     * Universal bridge: StringBuffer → String.
     * Extracts the String value from StringBuffer for further conversion.
     */
    static String stringBufferToString(Object from, Converter converter) {
        StringBuffer sb = (StringBuffer) from;
        return sb.toString();
    }

    /**
     * Universal bridge: CharSequence → String.
     * Extracts the String value from CharSequence for further conversion.
     */
    static String charSequenceToString(Object from, Converter converter) {
        CharSequence cs = (CharSequence) from;
        return cs.toString();
    }

    // ========================================
    // Atomic → Primitive Bridge Methods
    // ========================================

    /**
     * Universal bridge: AtomicInteger → primitive int.
     * Extracts the int value from AtomicInteger and uses it for further conversion.
     */
    static int atomicIntegerToInt(Object from, Converter converter) {
        AtomicInteger atomic = (AtomicInteger) from;
        return atomic.get();
    }

    /**
     * Universal bridge: AtomicLong → primitive long.
     * Extracts the long value from AtomicLong and uses it for further conversion.
     */
    static long atomicLongToLong(Object from, Converter converter) {
        AtomicLong atomic = (AtomicLong) from;
        return atomic.get();
    }

    /**
     * Universal bridge: AtomicBoolean → primitive boolean.
     * Extracts the boolean value from AtomicBoolean and uses it for further conversion.
     */
    static boolean atomicBooleanToBoolean(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return atomic.get();
    }

    // ========================================
    // Reverse Bridge Methods (Primary → Surrogate)
    // ========================================

    /**
     * Universal reverse bridge: Integer → AtomicInteger.
     * Creates AtomicInteger from Integer value for reverse bridge access.
     */
    static AtomicInteger integerToAtomicInteger(Object from, Converter converter) {
        Integer value = (Integer) from;
        return new AtomicInteger(value);
    }

    /**
     * Universal reverse bridge: Long → AtomicLong.
     * Creates AtomicLong from Long value for reverse bridge access.
     */
    static AtomicLong longToAtomicLong(Object from, Converter converter) {
        Long value = (Long) from;
        return new AtomicLong(value);
    }

    /**
     * Universal reverse bridge: Boolean → AtomicBoolean.
     * Creates AtomicBoolean from Boolean value for reverse bridge access.
     */
    static AtomicBoolean booleanToAtomicBoolean(Object from, Converter converter) {
        Boolean value = (Boolean) from;
        return new AtomicBoolean(value);
    }

    /**
     * Universal reverse bridge: String → StringBuilder.
     * Creates StringBuilder from String value for reverse bridge access.
     */
    static StringBuilder stringToStringBuilder(Object from, Converter converter) {
        String value = (String) from;
        return new StringBuilder(value);
    }

    /**
     * Universal reverse bridge: String → StringBuffer.
     * Creates StringBuffer from String value for reverse bridge access.
     */
    static StringBuffer stringToStringBuffer(Object from, Converter converter) {
        String value = (String) from;
        return new StringBuffer(value);
    }

    /**
     * Universal reverse bridge: String → CharSequence.
     * Returns the String as CharSequence for reverse bridge access.
     */
    static CharSequence stringToCharSequence(Object from, Converter converter) {
        String value = (String) from;
        return value; // String implements CharSequence
    }

    // ========================================
    // Array Bridge Methods (Wrapper ↔ Primitive)
    // ========================================

    /**
     * Universal bridge: Character[] → char[].
     * Converts wrapper array to primitive array for bridge access to char[] conversions.
     */
    static char[] characterArrayToCharArray(Object from, Converter converter) {
        Character[] chars = (Character[]) from;
        char[] result = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            result[i] = chars[i] != null ? chars[i] : '\u0000'; // Handle null elements
        }
        return result;
    }

    /**
     * Universal reverse bridge: char[] → Character[].
     * Converts primitive array to wrapper array for reverse bridge access.
     */
    static Character[] charArrayToCharacterArray(Object from, Converter converter) {
        char[] chars = (char[]) from;
        Character[] result = new Character[chars.length];
        for (int i = 0; i < chars.length; i++) {
            result[i] = chars[i];
        }
        return result;
    }

    // ========================================
    // Primitive ↔ Wrapper Bridge Methods
    // ========================================

    /**
     * Universal bridge: primitive → wrapper.
     * Handles auto-boxing for all primitive types.
     */
    static Object primitiveToWrapper(Object from, Converter converter) {
        // The JVM automatically boxes primitives when they're cast to Object
        return from;
    }

    /**
     * Universal bridge: wrapper → primitive.
     * Handles auto-unboxing for all wrapper types.
     */
    static Object wrapperToPrimitive(Object from, Converter converter) {
        // Auto-unboxing will happen when the result is cast to the primitive type
        return from;
    }

    // ========================================
    // All Array Bridge Methods
    // ========================================

    static byte[] byteArrayToByteArray(Object from, Converter converter) {
        if (from instanceof Byte[]) {
            Byte[] array = (Byte[]) from;
            byte[] result = new byte[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0;
            }
            return result;
        } else {
            byte[] array = (byte[]) from;
            Byte[] result = new Byte[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (byte[]) (Object) result; // Type erasure workaround
        }
    }

    static boolean[] booleanArrayToBooleanArray(Object from, Converter converter) {
        if (from instanceof Boolean[]) {
            Boolean[] array = (Boolean[]) from;
            boolean[] result = new boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : false;
            }
            return result;
        } else {
            boolean[] array = (boolean[]) from;
            Boolean[] result = new Boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (boolean[]) (Object) result; // Type erasure workaround
        }
    }

    static short[] shortArrayToShortArray(Object from, Converter converter) {
        if (from instanceof Short[]) {
            Short[] array = (Short[]) from;
            short[] result = new short[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0;
            }
            return result;
        } else {
            short[] array = (short[]) from;
            Short[] result = new Short[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (short[]) (Object) result; // Type erasure workaround
        }
    }

    static int[] integerArrayToIntArray(Object from, Converter converter) {
        if (from instanceof Integer[]) {
            Integer[] array = (Integer[]) from;
            int[] result = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0;
            }
            return result;
        } else {
            int[] array = (int[]) from;
            Integer[] result = new Integer[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (int[]) (Object) result; // Type erasure workaround
        }
    }

    static long[] longArrayToLongArray(Object from, Converter converter) {
        if (from instanceof Long[]) {
            Long[] array = (Long[]) from;
            long[] result = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0L;
            }
            return result;
        } else {
            long[] array = (long[]) from;
            Long[] result = new Long[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (long[]) (Object) result; // Type erasure workaround
        }
    }

    static float[] floatArrayToFloatArray(Object from, Converter converter) {
        if (from instanceof Float[]) {
            Float[] array = (Float[]) from;
            float[] result = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0.0f;
            }
            return result;
        } else {
            float[] array = (float[]) from;
            Float[] result = new Float[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (float[]) (Object) result; // Type erasure workaround
        }
    }

    static double[] doubleArrayToDoubleArray(Object from, Converter converter) {
        if (from instanceof Double[]) {
            Double[] array = (Double[]) from;
            double[] result = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null ? array[i] : 0.0;
            }
            return result;
        } else {
            double[] array = (double[]) from;
            Double[] result = new Double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return (double[]) (Object) result; // Type erasure workaround
        }
    }

    // Reverse array conversions
    static Integer[] intArrayToIntegerArray(Object from, Converter converter) {
        int[] array = (int[]) from;
        Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    static Short[] shortArrayToShortArrayWrapper(Object from, Converter converter) {
        short[] array = (short[]) from;
        Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    static Boolean[] booleanArrayToBooleanArrayWrapper(Object from, Converter converter) {
        boolean[] array = (boolean[]) from;
        Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    static Long[] longArrayToLongArrayWrapper(Object from, Converter converter) {
        long[] array = (long[]) from;
        Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    static Float[] floatArrayToFloatArrayWrapper(Object from, Converter converter) {
        float[] array = (float[]) from;
        Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    static Double[] doubleArrayToDoubleArrayWrapper(Object from, Converter converter) {
        double[] array = (double[]) from;
        Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // ========================================
    // Atomic Array Bridge Methods
    // ========================================

    /**
     * Universal bridge: AtomicIntegerArray → int[].
     * Extracts the int array from AtomicIntegerArray for universal array system access.
     */
    static int[] atomicIntegerArrayToIntArray(Object from, Converter converter) {
        AtomicIntegerArray atomicArray = (AtomicIntegerArray) from;
        int length = atomicArray.length();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = atomicArray.get(i);
        }
        return result;
    }

    /**
     * Universal reverse bridge: int[] → AtomicIntegerArray.
     * Creates AtomicIntegerArray from int array for reverse bridge access.
     */
    static AtomicIntegerArray intArrayToAtomicIntegerArray(Object from, Converter converter) {
        int[] array = (int[]) from;
        AtomicIntegerArray result = new AtomicIntegerArray(array.length);
        for (int i = 0; i < array.length; i++) {
            result.set(i, array[i]);
        }
        return result;
    }

    /**
     * Universal bridge: AtomicLongArray → long[].
     * Extracts the long array from AtomicLongArray for universal array system access.
     */
    static long[] atomicLongArrayToLongArray(Object from, Converter converter) {
        AtomicLongArray atomicArray = (AtomicLongArray) from;
        int length = atomicArray.length();
        long[] result = new long[length];
        for (int i = 0; i < length; i++) {
            result[i] = atomicArray.get(i);
        }
        return result;
    }

    /**
     * Universal reverse bridge: long[] → AtomicLongArray.
     * Creates AtomicLongArray from long array for reverse bridge access.
     */
    static AtomicLongArray longArrayToAtomicLongArray(Object from, Converter converter) {
        long[] array = (long[]) from;
        AtomicLongArray result = new AtomicLongArray(array.length);
        for (int i = 0; i < array.length; i++) {
            result.set(i, array[i]);
        }
        return result;
    }

    /**
     * Universal bridge: AtomicReferenceArray → Object[].
     * Extracts the Object array from AtomicReferenceArray for universal array system access.
     */
    static Object[] atomicReferenceArrayToObjectArray(Object from, Converter converter) {
        AtomicReferenceArray<?> atomicArray = (AtomicReferenceArray<?>) from;
        int length = atomicArray.length();
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = atomicArray.get(i);
        }
        return result;
    }

    /**
     * Universal reverse bridge: Object[] → AtomicReferenceArray.
     * Creates AtomicReferenceArray from Object array for reverse bridge access.
     */
    static AtomicReferenceArray<Object> objectArrayToAtomicReferenceArray(Object from, Converter converter) {
        Object[] array = (Object[]) from;
        AtomicReferenceArray<Object> result = new AtomicReferenceArray<>(array.length);
        for (int i = 0; i < array.length; i++) {
            result.set(i, array[i]);
        }
        return result;
    }

    /**
     * Universal bridge: AtomicReferenceArray → String[].
     * Extracts the String array from AtomicReferenceArray for String array system access.
     */
    static String[] atomicReferenceArrayToStringArray(Object from, Converter converter) {
        AtomicReferenceArray<?> atomicArray = (AtomicReferenceArray<?>) from;
        int length = atomicArray.length();
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            Object element = atomicArray.get(i);
            result[i] = element != null ? element.toString() : null;
        }
        return result;
    }

    /**
     * Universal reverse bridge: String[] → AtomicReferenceArray.
     * Creates AtomicReferenceArray from String array for reverse bridge access.
     */
    static AtomicReferenceArray<String> stringArrayToAtomicReferenceArray(Object from, Converter converter) {
        String[] array = (String[]) from;
        AtomicReferenceArray<String> result = new AtomicReferenceArray<>(array.length);
        for (int i = 0; i < array.length; i++) {
            result.set(i, array[i]);
        }
        return result;
    }

    // ========================================
    // NIO Buffer Bridge Methods
    // ========================================

    /**
     * Universal bridge: IntBuffer → int[].
     * Extracts the int array from IntBuffer for universal array system access.
     */
    static int[] intBufferToIntArray(Object from, Converter converter) {
        IntBuffer buffer = (IntBuffer) from;
        int[] result = new int[buffer.remaining()];
        buffer.mark();
        buffer.get(result);
        buffer.reset();
        return result;
    }

    /**
     * Universal reverse bridge: int[] → IntBuffer.
     * Creates IntBuffer from int array for reverse bridge access.
     */
    static IntBuffer intArrayToIntBuffer(Object from, Converter converter) {
        int[] array = (int[]) from;
        return IntBuffer.wrap(array);
    }

    /**
     * Universal bridge: LongBuffer → long[].
     * Extracts the long array from LongBuffer for universal array system access.
     */
    static long[] longBufferToLongArray(Object from, Converter converter) {
        LongBuffer buffer = (LongBuffer) from;
        long[] result = new long[buffer.remaining()];
        buffer.mark();
        buffer.get(result);
        buffer.reset();
        return result;
    }

    /**
     * Universal reverse bridge: long[] → LongBuffer.
     * Creates LongBuffer from long array for reverse bridge access.
     */
    static LongBuffer longArrayToLongBuffer(Object from, Converter converter) {
        long[] array = (long[]) from;
        return LongBuffer.wrap(array);
    }

    /**
     * Universal bridge: FloatBuffer → float[].
     * Extracts the float array from FloatBuffer for universal array system access.
     */
    static float[] floatBufferToFloatArray(Object from, Converter converter) {
        FloatBuffer buffer = (FloatBuffer) from;
        float[] result = new float[buffer.remaining()];
        buffer.mark();
        buffer.get(result);
        buffer.reset();
        return result;
    }

    /**
     * Universal reverse bridge: float[] → FloatBuffer.
     * Creates FloatBuffer from float array for reverse bridge access.
     */
    static FloatBuffer floatArrayToFloatBuffer(Object from, Converter converter) {
        float[] array = (float[]) from;
        return FloatBuffer.wrap(array);
    }

    /**
     * Universal bridge: DoubleBuffer → double[].
     * Extracts the double array from DoubleBuffer for universal array system access.
     */
    static double[] doubleBufferToDoubleArray(Object from, Converter converter) {
        DoubleBuffer buffer = (DoubleBuffer) from;
        double[] result = new double[buffer.remaining()];
        buffer.mark();
        buffer.get(result);
        buffer.reset();
        return result;
    }

    /**
     * Universal reverse bridge: double[] → DoubleBuffer.
     * Creates DoubleBuffer from double array for reverse bridge access.
     */
    static DoubleBuffer doubleArrayToDoubleBuffer(Object from, Converter converter) {
        double[] array = (double[]) from;
        return DoubleBuffer.wrap(array);
    }

    /**
     * Universal bridge: ShortBuffer → short[].
     * Extracts the short array from ShortBuffer for universal array system access.
     */
    static short[] shortBufferToShortArray(Object from, Converter converter) {
        ShortBuffer buffer = (ShortBuffer) from;
        short[] result = new short[buffer.remaining()];
        buffer.mark();
        buffer.get(result);
        buffer.reset();
        return result;
    }

    /**
     * Universal reverse bridge: short[] → ShortBuffer.
     * Creates ShortBuffer from short array for reverse bridge access.
     */
    static ShortBuffer shortArrayToShortBuffer(Object from, Converter converter) {
        short[] array = (short[]) from;
        return ShortBuffer.wrap(array);
    }

    // ========================================
    // BitSet Bridge Methods
    // ========================================

    /**
     * Universal bridge: BitSet → boolean[].
     * Extracts the boolean array from BitSet for universal array system access.
     */
    static boolean[] bitSetToBooleanArray(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        boolean[] result = new boolean[bitSet.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bitSet.get(i);
        }
        return result;
    }

    /**
     * Universal reverse bridge: boolean[] → BitSet.
     * Creates BitSet from boolean array for reverse bridge access.
     */
    static BitSet booleanArrayToBitSet(Object from, Converter converter) {
        boolean[] array = (boolean[]) from;
        BitSet result = new BitSet(array.length);
        for (int i = 0; i < array.length; i++) {
            result.set(i, array[i]);
        }
        return result;
    }

    /**
     * Universal bridge: BitSet → int[].
     * Extracts the set bit indices as int array for universal array system access.
     */
    static int[] bitSetToIntArray(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        return bitSet.stream().toArray();
    }

    /**
     * Universal reverse bridge: int[] → BitSet.
     * Creates BitSet from int array of bit indices for reverse bridge access.
     */
    static BitSet intArrayToBitSet(Object from, Converter converter) {
        int[] array = (int[]) from;
        BitSet result = new BitSet();
        for (int bitIndex : array) {
            result.set(bitIndex);
        }
        return result;
    }

    /**
     * Universal bridge: BitSet → byte[].
     * Extracts the byte array from BitSet for universal array system access.
     */
    static byte[] bitSetToByteArray(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        return bitSet.toByteArray();
    }

    /**
     * Universal reverse bridge: byte[] → BitSet.
     * Creates BitSet from byte array for reverse bridge access.
     */
    static BitSet byteArrayToBitSet(Object from, Converter converter) {
        byte[] array = (byte[]) from;
        return BitSet.valueOf(array);
    }

    /**
     * BitSet → long conversion.
     * Interprets the BitSet as a bitmask and returns the long value.
     * For BitSets with more than 64 bits, only the lower 64 bits are returned.
     */
    static long bitSetToLong(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        long[] longs = bitSet.toLongArray();
        return longs.length == 0 ? 0L : longs[0];
    }

    /**
     * long → BitSet conversion.
     * Creates a BitSet from the long value interpreted as a bitmask.
     */
    static BitSet longToBitSet(Object from, Converter converter) {
        long value = ((Number) from).longValue();
        return BitSet.valueOf(new long[]{value});
    }

    /**
     * BitSet → BigInteger conversion.
     * Interprets the BitSet as a bitmask and returns the BigInteger value.
     * Handles BitSets of any size.
     */
    static BigInteger bitSetToBigInteger(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        byte[] bytes = bitSet.toByteArray();
        if (bytes.length == 0) {
            return BigInteger.ZERO;
        }
        // BitSet.toByteArray() returns little-endian, BigInteger expects big-endian
        // Reverse the byte array
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        return new BigInteger(1, reversed);
    }

    /**
     * BigInteger → BitSet conversion.
     * Creates a BitSet from the BigInteger value interpreted as a bitmask.
     */
    static BitSet bigIntegerToBitSet(Object from, Converter converter) {
        BigInteger value = (BigInteger) from;
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Cannot convert negative BigInteger to BitSet");
        }
        if (value.equals(BigInteger.ZERO)) {
            return new BitSet();
        }
        byte[] bytes = value.toByteArray();
        // BigInteger.toByteArray() returns big-endian, BitSet.valueOf() expects little-endian
        // Reverse the byte array, and skip leading zero byte if present (sign byte)
        int start = (bytes[0] == 0) ? 1 : 0;
        byte[] reversed = new byte[bytes.length - start];
        for (int i = start; i < bytes.length; i++) {
            reversed[bytes.length - 1 - i] = bytes[i];
        }
        return BitSet.valueOf(reversed);
    }

    /**
     * BitSet → int conversion.
     * Returns the lower 32 bits of the BitSet as an int.
     * For BitSets with more than 32 bits, only the lower 32 bits are returned.
     */
    static int bitSetToInt(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        long[] longs = bitSet.toLongArray();
        return longs.length == 0 ? 0 : (int) longs[0];
    }

    /**
     * int → BitSet conversion.
     * Creates a BitSet from the int value interpreted as a bitmask.
     */
    static BitSet intToBitSet(Object from, Converter converter) {
        int value = ((Number) from).intValue();
        return BitSet.valueOf(new long[]{value & 0xFFFFFFFFL});
    }

    /**
     * BitSet → short conversion.
     * Returns the lower 16 bits of the BitSet as a short.
     * For BitSets with more than 16 bits, only the lower 16 bits are returned.
     */
    static short bitSetToShort(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        long[] longs = bitSet.toLongArray();
        return longs.length == 0 ? 0 : (short) longs[0];
    }

    /**
     * short → BitSet conversion.
     * Creates a BitSet from the short value interpreted as a bitmask.
     */
    static BitSet shortToBitSet(Object from, Converter converter) {
        short value = ((Number) from).shortValue();
        return BitSet.valueOf(new long[]{value & 0xFFFFL});
    }

    /**
     * BitSet → byte conversion.
     * Returns the lower 8 bits of the BitSet as a byte.
     * For BitSets with more than 8 bits, only the lower 8 bits are returned.
     */
    static byte bitSetToByte(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        long[] longs = bitSet.toLongArray();
        return longs.length == 0 ? 0 : (byte) longs[0];
    }

    /**
     * byte → BitSet conversion.
     * Creates a BitSet from the byte value interpreted as a bitmask.
     */
    static BitSet byteToBitSet(Object from, Converter converter) {
        byte value = ((Number) from).byteValue();
        return BitSet.valueOf(new long[]{value & 0xFFL});
    }

    /**
     * BitSet → AtomicInteger conversion.
     * Returns the lower 32 bits of the BitSet as an AtomicInteger.
     */
    static AtomicInteger bitSetToAtomicInteger(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        long[] longs = bitSet.toLongArray();
        return new AtomicInteger(longs.length == 0 ? 0 : (int) longs[0]);
    }

    /**
     * AtomicInteger → BitSet conversion.
     * Creates a BitSet from the AtomicInteger value interpreted as a bitmask.
     */
    static BitSet atomicIntegerToBitSet(Object from, Converter converter) {
        int value = ((AtomicInteger) from).get();
        return BitSet.valueOf(new long[]{value & 0xFFFFFFFFL});
    }

    /**
     * BitSet → BigDecimal conversion.
     * Converts the BitSet to a BigInteger first, then to BigDecimal.
     * Handles BitSets of any size.
     */
    static BigDecimal bitSetToBigDecimal(Object from, Converter converter) {
        BigInteger bigInt = bitSetToBigInteger(from, converter);
        return new BigDecimal(bigInt);
    }

    /**
     * BigDecimal → BitSet conversion.
     * Converts to BigInteger (truncating decimal part) then to BitSet.
     */
    static BitSet bigDecimalToBitSet(Object from, Converter converter) {
        BigDecimal value = (BigDecimal) from;
        return bigIntegerToBitSet(value.toBigInteger(), converter);
    }

    /**
     * BitSet → AtomicBoolean conversion.
     * Returns AtomicBoolean(true) if any bit is set, AtomicBoolean(false) if empty.
     */
    static AtomicBoolean bitSetToAtomicBoolean(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        return new AtomicBoolean(!bitSet.isEmpty());
    }

    /**
     * AtomicBoolean → BitSet conversion.
     * AtomicBoolean(true) = BitSet with bit 0 set.
     * AtomicBoolean(false) = empty BitSet.
     */
    static BitSet atomicBooleanToBitSet(Object from, Converter converter) {
        AtomicBoolean value = (AtomicBoolean) from;
        BitSet bitSet = new BitSet();
        if (value.get()) {
            bitSet.set(0);
        }
        return bitSet;
    }

    /**
     * BitSet → String conversion.
     * Returns a binary string representation where rightmost character is bit 0.
     * Example: BitSet with bits 1,3,5 set → "101010"
     */
    static String bitSetToString(Object from, Converter converter) {
        BitSet bitSet = (BitSet) from;
        int length = bitSet.length(); // highest set bit + 1, or 0 if empty
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = length - 1; i >= 0; i--) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        return sb.toString();
    }

    /**
     * String → BitSet conversion.
     * Parses a binary string where rightmost character is bit 0.
     * Example: "101010" → BitSet with bits 1,3,5 set
     */
    static BitSet stringToBitSet(Object from, Converter converter) {
        String binaryStr = ((String) from).trim();
        BitSet bitSet = new BitSet();
        int len = binaryStr.length();
        for (int i = 0; i < len; i++) {
            char ch = binaryStr.charAt(i);
            if (ch == '1') {
                bitSet.set(len - 1 - i);
            }
        }
        return bitSet;
    }

    // ========================================
    // Stream Bridge Methods
    // ========================================

    /**
     * Array → Stream bridge: int[] → IntStream.
     * Creates IntStream from int array for functional programming access.
     */
    static IntStream intArrayToIntStream(Object from, Converter converter) {
        int[] array = (int[]) from;
        return IntStream.of(array);
    }

    /**
     * Array → Stream bridge: long[] → LongStream.  
     * Creates LongStream from long array for functional programming access.
     */
    static LongStream longArrayToLongStream(Object from, Converter converter) {
        long[] array = (long[]) from;
        return LongStream.of(array);
    }

    /**
     * Array → Stream bridge: double[] → DoubleStream.
     * Creates DoubleStream from double array for functional programming access.
     */
    static DoubleStream doubleArrayToDoubleStream(Object from, Converter converter) {
        double[] array = (double[]) from;
        return DoubleStream.of(array);
    }

    // ========================================
    // Date/Time Bridge Methods (placeholders for now)
    // ========================================

    static Object sqlDateToLocalDate(Object from, Converter converter) {
        // TODO: Implement using existing SqlDateConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static Object timestampToInstant(Object from, Converter converter) {
        // TODO: Implement using existing TimestampConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static ZonedDateTime calendarToZonedDateTime(Object from, Converter converter) {
        Calendar calendar = (Calendar) from;
        return calendar.toInstant().atZone(calendar.getTimeZone().toZoneId());
    }

    static Object timeZoneToZoneId(Object from, Converter converter) {
        // TODO: Implement using existing TimeZoneConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static Object instantToTimestamp(Object from, Converter converter) {
        // TODO: Implement using existing InstantConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static Object localDateToSqlDate(Object from, Converter converter) {
        // TODO: Implement using existing LocalDateConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static Object zonedDateTimeToCalendar(Object from, Converter converter) {
        // TODO: Implement using existing ZonedDateTimeConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static Object zoneIdToTimeZone(Object from, Converter converter) {
        // TODO: Implement using existing ZoneIdConversions
        throw new UnsupportedOperationException("Not yet implemented");
    }
}