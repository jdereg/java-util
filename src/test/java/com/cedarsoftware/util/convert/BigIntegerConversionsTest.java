package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for BigIntegerConversions bugs.
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
class BigIntegerConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ---- Bug: toUUID silently truncates values > 128 bits ----

    @Test
    void toUUID_maxUuid_works() {
        // Max UUID = 2^128 - 1 = ffffffff-ffff-ffff-ffff-ffffffffffff
        BigInteger maxUuid = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
        UUID result = BigIntegerConversions.toUUID(maxUuid, converter);
        assertEquals("ffffffff-ffff-ffff-ffff-ffffffffffff", result.toString());
    }

    @Test
    void toUUID_zero_works() {
        UUID result = BigIntegerConversions.toUUID(BigInteger.ZERO, converter);
        assertEquals("00000000-0000-0000-0000-000000000000", result.toString());
    }

    @Test
    void toUUID_knownValue_roundTrips() {
        UUID original = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        BigInteger bigInt = new BigInteger(original.toString().replace("-", ""), 16);
        UUID result = BigIntegerConversions.toUUID(bigInt, converter);
        assertEquals(original, result);
    }

    @Test
    void toUUID_exceeds128bits_shouldThrow() {
        // 2^128 = exactly 1 bit over the max UUID value — should throw, not silently truncate
        BigInteger tooLarge = BigInteger.ONE.shiftLeft(128);
        assertThrows(IllegalArgumentException.class,
                () -> BigIntegerConversions.toUUID(tooLarge, converter));
    }

    @Test
    void toUUID_way_exceeds128bits_shouldThrow() {
        // 2^256 — definitely too large
        BigInteger wayTooLarge = BigInteger.ONE.shiftLeft(256);
        assertThrows(IllegalArgumentException.class,
                () -> BigIntegerConversions.toUUID(wayTooLarge, converter));
    }

    @Test
    void toUUID_negative_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> BigIntegerConversions.toUUID(BigInteger.valueOf(-1), converter));
    }

    @Test
    void toUUID_silentTruncationProducesWrongResult() {
        // Demonstrate the bug: 2^128 + 1 should throw, but instead it silently
        // truncates the high bit and returns UUID for value 1
        BigInteger tooLarge = BigInteger.ONE.shiftLeft(128).add(BigInteger.ONE);

        // Before fix: this would NOT throw — it would silently return 00000000-0000-0000-0000-000000000001
        // (dropping the high bit). After fix: should throw.
        assertThrows(IllegalArgumentException.class,
                () -> BigIntegerConversions.toUUID(tooLarge, converter));
    }
}
