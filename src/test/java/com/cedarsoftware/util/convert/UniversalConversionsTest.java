package com.cedarsoftware.util.convert;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for UniversalConversions bugs.
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
class UniversalConversionsTest {

    private static Converter converter() {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return ZoneId.of("UTC"); }
        };
        return new Converter(options);
    }

    // ---- Bug #1: else branches cast Wrapper[] to primitive[] — ClassCastException ----

    @Test
    void byteArrayToByteArray_primitiveInput_shouldReturnWrapperNotCrash() {
        Converter conv = converter();
        byte[] input = {1, 2, 3};
        // The else branch tried (byte[]) (Object) Byte[], always ClassCastException
        Object result = UniversalConversions.byteArrayToByteArray(input, conv);
        // Should return Byte[] since input was byte[]
        assertArrayEquals(new Byte[]{1, 2, 3}, (Byte[]) result);
    }

    @Test
    void integerArrayToIntArray_primitiveInput_shouldReturnWrapperNotCrash() {
        Converter conv = converter();
        int[] input = {10, 20, 30};
        Object result = UniversalConversions.integerArrayToIntArray(input, conv);
        assertArrayEquals(new Integer[]{10, 20, 30}, (Integer[]) result);
    }

    @Test
    void longArrayToLongArray_primitiveInput_shouldReturnWrapperNotCrash() {
        Converter conv = converter();
        long[] input = {100L, 200L};
        Object result = UniversalConversions.longArrayToLongArray(input, conv);
        assertArrayEquals(new Long[]{100L, 200L}, (Long[]) result);
    }

    @Test
    void doubleArrayToDoubleArray_primitiveInput_shouldReturnWrapperNotCrash() {
        Converter conv = converter();
        double[] input = {1.5, 2.5};
        Object result = UniversalConversions.doubleArrayToDoubleArray(input, conv);
        assertArrayEquals(new Double[]{1.5, 2.5}, (Double[]) result);
    }

    // ---- Bug #2: NIO buffer mark()/reset() destroys pre-existing marks ----

    @Test
    void intBufferToIntArray_shouldPreserveExistingMark() {
        IntBuffer buffer = IntBuffer.wrap(new int[]{10, 20, 30, 40, 50});
        buffer.position(1); // position at 20
        buffer.mark();       // mark at position 1
        buffer.position(2); // advance past mark to position 2 (remaining: 30, 40, 50)

        Converter conv = converter();
        int[] result = UniversalConversions.intBufferToIntArray(buffer, conv);

        assertArrayEquals(new int[]{30, 40, 50}, result);
        // Position should be restored
        assertEquals(2, buffer.position());
        // Original mark should still work
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    void longBufferToLongArray_shouldPreserveExistingMark() {
        LongBuffer buffer = LongBuffer.wrap(new long[]{10L, 20L, 30L});
        buffer.position(0);
        buffer.mark();       // mark at 0
        buffer.position(1); // advance to 1

        Converter conv = converter();
        long[] result = UniversalConversions.longBufferToLongArray(buffer, conv);

        assertArrayEquals(new long[]{20L, 30L}, result);
        assertEquals(1, buffer.position());
        buffer.reset(); // should still go to 0
        assertEquals(0, buffer.position());
    }

    @Test
    void floatBufferToFloatArray_shouldPreserveExistingMark() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[]{1.0f, 2.0f, 3.0f});
        buffer.position(0);
        buffer.mark();
        buffer.position(1);

        Converter conv = converter();
        float[] result = UniversalConversions.floatBufferToFloatArray(buffer, conv);

        assertArrayEquals(new float[]{2.0f, 3.0f}, result);
        assertEquals(1, buffer.position());
        buffer.reset();
        assertEquals(0, buffer.position());
    }

    @Test
    void doubleBufferToDoubleArray_shouldPreserveExistingMark() {
        DoubleBuffer buffer = DoubleBuffer.wrap(new double[]{1.0, 2.0, 3.0});
        buffer.position(0);
        buffer.mark();
        buffer.position(2);

        Converter conv = converter();
        double[] result = UniversalConversions.doubleBufferToDoubleArray(buffer, conv);

        assertArrayEquals(new double[]{3.0}, result);
        assertEquals(2, buffer.position());
        buffer.reset();
        assertEquals(0, buffer.position());
    }

    @Test
    void shortBufferToShortArray_shouldPreserveExistingMark() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[]{1, 2, 3, 4});
        buffer.position(0);
        buffer.mark();
        buffer.position(2);

        Converter conv = converter();
        short[] result = UniversalConversions.shortBufferToShortArray(buffer, conv);

        assertArrayEquals(new short[]{3, 4}, result);
        assertEquals(2, buffer.position());
        buffer.reset();
        assertEquals(0, buffer.position());
    }
}
