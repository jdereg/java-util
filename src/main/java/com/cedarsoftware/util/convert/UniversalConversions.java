package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
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
     * BigInteger → Map conversion.
     * Creates a Map with the string representation for JSON compatibility.
     */
    static Map<String, Object> bigIntegerToMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, ((BigInteger) from).toString(10));
        return target;
    }

    /**
     * BigDecimal → Map conversion.
     * Creates a Map with the string representation for JSON compatibility.
     * Uses toPlainString() to avoid scientific notation (e.g., "12300000000" not "1.23E+10").
     */
    static Map<String, Object> bigDecimalToMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, ((BigDecimal) from).toPlainString());
        return target;
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
        return value == null ? null : new AtomicInteger(value);
    }

    /**
     * Universal reverse bridge: Long → AtomicLong.
     * Creates AtomicLong from Long value for reverse bridge access.
     */
    static AtomicLong longToAtomicLong(Object from, Converter converter) {
        Long value = (Long) from;
        return value == null ? null : new AtomicLong(value);
    }

    static long longAdderToLong(Object from, Converter converter) {
        return ((LongAdder) from).sum();
    }

    static double doubleAdderToDouble(Object from, Converter converter) {
        return ((DoubleAdder) from).sum();
    }

    /** A new LongAdder holding {@code from}. */
    static LongAdder longToLongAdder(Object from, Converter converter) {
        Long value = (Long) from;
        if (value == null) {
            return null;
        }
        LongAdder adder = new LongAdder();
        adder.add(value);
        return adder;
    }

    /** A new DoubleAdder holding {@code from}. */
    static DoubleAdder doubleToDoubleAdder(Object from, Converter converter) {
        Double value = (Double) from;
        if (value == null) {
            return null;
        }
        DoubleAdder adder = new DoubleAdder();
        adder.add(value);
        return adder;
    }

    /**
     * Universal reverse bridge: Boolean → AtomicBoolean.
     * Creates AtomicBoolean from Boolean value for reverse bridge access.
     */
    static AtomicBoolean booleanToAtomicBoolean(Object from, Converter converter) {
        Boolean value = (Boolean) from;
        return value == null ? null : new AtomicBoolean(value);
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
        return new AtomicIntegerArray((int[]) from);
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
        return new AtomicLongArray((long[]) from);
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
        buffer.duplicate().get(result);
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
        buffer.duplicate().get(result);
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
        buffer.duplicate().get(result);
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
        buffer.duplicate().get(result);
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
        buffer.duplicate().get(result);
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
     * Copy a BitSet to a new BitSet instance.
     */
    static BitSet copyBitSet(Object from, Converter converter) {
        return (BitSet) ((BitSet) from).clone();
    }

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
     * Interprets the BitSet as a 64-bit two's-complement bitmask (bit 63 is the sign).
     * @throws IllegalArgumentException if a bit at index 64 or above is set
     */
    static long bitSetToLong(Object from, Converter converter) {
        return bitSetBits((BitSet) from, 64, "long");
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
     * Interprets the BitSet as a 32-bit two's-complement bitmask (bit 31 is the sign).
     * @throws IllegalArgumentException if a bit at index 32 or above is set
     */
    static int bitSetToInt(Object from, Converter converter) {
        return (int) bitSetBits((BitSet) from, 32, "int");
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
     * Interprets the BitSet as a 16-bit two's-complement bitmask (bit 15 is the sign).
     * @throws IllegalArgumentException if a bit at index 16 or above is set
     */
    static short bitSetToShort(Object from, Converter converter) {
        return (short) bitSetBits((BitSet) from, 16, "short");
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
     * Interprets the BitSet as a 8-bit two's-complement bitmask (bit 7 is the sign).
     * @throws IllegalArgumentException if a bit at index 8 or above is set
     */
    static byte bitSetToByte(Object from, Converter converter) {
        return (byte) bitSetBits((BitSet) from, 8, "byte");
    }

    /**
     * The BitSet's bits as a pattern {@code width} bits wide, whose top bit is the sign once the caller narrows it
     * (long: as is). A set bit at {@code width} or above does not fit: it was dropped without a word, so a BitSet
     * with bit 40 set came out as an int from its lower 32 bits.
     */
    private static long bitSetBits(BitSet bitSet, int width, String type) {
        int length = bitSet.length();       // highest set bit + 1
        if (length > width) {
            throw new IllegalArgumentException("Unable to convert BitSet to " + type + ": bit " + (length - 1)
                    + " is set, and " + type + " holds " + width + (width == 1 ? " bit" : " bits"));
        }
        return length == 0 ? 0L : bitSet.toLongArray()[0];
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
        BigInteger whole = BigDecimalConversions.wholePart(value);
        if (value.compareTo(new BigDecimal(whole)) != 0) {
            // 5.5 became {0, 2} (5) without a word
            // toString, not toPlainString: 1e-9999999 in plain notation is ten million characters
            throw new IllegalArgumentException("Unable to convert " + value
                    + " to BitSet: it is not a whole number");
        }
        return bigIntegerToBitSet(whole, converter);
    }

    /**
     * BitSet → Boolean conversion: one bit. {} is false and {0} is true.
     * @throws IllegalArgumentException if a bit at index 1 or above is set
     */
    static Boolean bitSetToBoolean(Object from, Converter converter) {
        // one bit: {} is false, {0} is true
        return bitSetBits((BitSet) from, 1, "boolean") != 0;
    }

    /**
     * Boolean → BitSet conversion.
     * true = BitSet with bit 0 set.
     * false = empty BitSet.
     */
    static BitSet booleanToBitSet(Object from, Converter converter) {
        Boolean value = (Boolean) from;
        BitSet bitSet = new BitSet();
        if (value) {
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
     * String → BitSet conversion: the binary text {@link #bitSetToString} writes, rightmost character bit 0
     * ({@code "101010"} has bits 1, 3 and 5 set). Surrounding whitespace is ignored and leading zeros are allowed; ""
     * is the empty BitSet.
     * @throws IllegalArgumentException for any character other than 0 and 1: "hello" and "5.0" became the empty
     *         BitSet, and "102" {2}, without a word
     */
    static BitSet stringToBitSet(Object from, Converter converter) {
        String binaryStr = ((String) from).trim();
        BitSet bitSet = new BitSet();
        int len = binaryStr.length();
        for (int i = 0; i < len; i++) {
            char ch = binaryStr.charAt(i);
            if (ch == '1') {
                bitSet.set(len - 1 - i);
            } else if (ch != '0') {
                String shown = binaryStr.length() > 100 ? binaryStr.substring(0, 100) + "..." : binaryStr;
                throw new IllegalArgumentException("Unable to convert '" + shown + "' to BitSet: only 0 and 1 are"
                        + " allowed, found '" + ch + "' at index " + i);
            }
        }
        return bitSet;
    }

    /** BitSet → CharBuffer: its binary string. */
    static CharBuffer bitSetToCharBuffer(Object from, Converter converter) {
        return CharBuffer.wrap(bitSetToString(from, converter));
    }

    /** BitSet → char[]: the characters of its binary string. */
    static char[] bitSetToCharArray(Object from, Converter converter) {
        return bitSetToString(from, converter).toCharArray();
    }

    /** char[] → BitSet: the characters read as a binary string, as {@link #stringToBitSet}. */
    static BitSet charArrayToBitSet(Object from, Converter converter) {
        return stringToBitSet(new String((char[]) from), converter);
    }

    /** BitSet → Character[]: the characters of its binary string. */
    static Character[] bitSetToCharacterArray(Object from, Converter converter) {
        String text = bitSetToString(from, converter);
        Character[] chars = new Character[text.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = text.charAt(i);
        }
        return chars;
    }

    /** Character[] → BitSet: the characters read as a binary string, as {@link #stringToBitSet}; null is not one. */
    static BitSet characterArrayToBitSet(Object from, Converter converter) {
        Character[] chars = (Character[]) from;
        StringBuilder text = new StringBuilder(chars.length);
        for (Character c : chars) {
            if (c == null) {
                throw new IllegalArgumentException("Unable to convert Character[] to BitSet: it holds a null");
            }
            text.append(c.charValue());
        }
        return stringToBitSet(text.toString(), converter);
    }

    /** BitSet → Map: {@code {"value": "<binary string>"}}, which Map → BitSet reads back. */
    static Map<String, Object> bitSetToMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, bitSetToString(from, converter));
        return map;
    }

    /** BitSet → ByteBuffer: the bytes {@link #bitSetToByteArray} gives, little-endian ({0, 2} is {5}). */
    static ByteBuffer bitSetToByteBuffer(Object from, Converter converter) {
        return ByteBuffer.wrap(((BitSet) from).toByteArray());
    }

    /** ByteBuffer → BitSet: the remaining bytes, little-endian, as {@link #byteArrayToBitSet}; the buffer is not moved. */
    static BitSet byteBufferToBitSet(Object from, Converter converter) {
        return BitSet.valueOf((ByteBuffer) from);
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
}