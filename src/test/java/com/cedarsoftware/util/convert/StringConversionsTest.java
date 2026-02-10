package com.cedarsoftware.util.convert;

import java.time.ZoneId;

import com.cedarsoftware.util.geom.Color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for StringConversions bugs.
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
class StringConversionsTest {

    private static Converter converter() {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return ZoneId.of("UTC"); }
        };
        return new Converter(options);
    }

    // ---- Bug #1: toLong compareTo == -1 instead of < 0 ----

    @Test
    void toLong_belowMinLong_shouldThrow() {
        // BigDecimal.compareTo() contract only guarantees negative/zero/positive, not -1/0/1.
        Converter conv = converter();
        String belowMin = "-9223372036854775809"; // Long.MIN_VALUE - 1
        assertThrows(IllegalArgumentException.class, () -> conv.convert(belowMin, long.class));
    }

    @Test
    void toLong_aboveMaxLong_shouldThrow() {
        Converter conv = converter();
        String aboveMax = "9223372036854775808"; // Long.MAX_VALUE + 1
        assertThrows(IllegalArgumentException.class, () -> conv.convert(aboveMax, long.class));
    }

    @Test
    void toLong_atBoundaries_shouldSucceed() {
        Converter conv = converter();
        assertEquals(Long.MIN_VALUE, (long) conv.convert(String.valueOf(Long.MIN_VALUE), long.class));
        assertEquals(Long.MAX_VALUE, (long) conv.convert(String.valueOf(Long.MAX_VALUE), long.class));
    }

    // ---- Bug #2: toCharacter silent truncation for values > 65535 ----

    @Test
    void toCharacter_65_shouldReturnA() {
        Converter conv = converter();
        assertEquals('A', (char) conv.convert("65", char.class));
    }

    @Test
    void toCharacter_65536_shouldThrow() {
        Converter conv = converter();
        // 65536 > Character.MAX_VALUE (65535), should throw instead of silently truncating to '\0'
        assertThrows(IllegalArgumentException.class, () -> conv.convert("65536", char.class));
    }

    @Test
    void toCharacter_100000_shouldThrow() {
        Converter conv = converter();
        assertThrows(IllegalArgumentException.class, () -> conv.convert("100000", char.class));
    }

    @Test
    void toCharacter_maxCharValue_shouldSucceed() {
        Converter conv = converter();
        assertEquals('\uFFFF', (char) conv.convert("65535", char.class));
    }

    // ---- Bug #3: toColor rgb()/rgba() case-sensitive ----

    @Test
    void toColor_rgbUppercase_shouldWork() {
        Converter conv = converter();
        Color result = conv.convert("RGB(255, 0, 0)", Color.class);
        assertEquals(new Color(255, 0, 0), result);
    }

    @Test
    void toColor_rgbMixedCase_shouldWork() {
        Converter conv = converter();
        Color result = conv.convert("Rgb(0, 255, 0)", Color.class);
        assertEquals(new Color(0, 255, 0), result);
    }

    @Test
    void toColor_rgbaUppercase_shouldWork() {
        Converter conv = converter();
        Color result = conv.convert("RGBA(255, 0, 0, 128)", Color.class);
        assertEquals(new Color(255, 0, 0, 128), result);
    }

    @Test
    void toColor_rgbaLowercase_shouldWork() {
        Converter conv = converter();
        Color result = conv.convert("rgba(0, 0, 255, 200)", Color.class);
        assertEquals(new Color(0, 0, 255, 200), result);
    }
}
