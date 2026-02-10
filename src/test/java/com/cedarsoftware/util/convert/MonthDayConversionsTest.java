package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for MonthDayConversions bugs.
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
class MonthDayConversionsTest {

    private static final Converter converter = new Converter(new ConverterOptions() {
        @Override
        public <T> T getCustomOption(String name) { return null; }

        @Override
        public ZoneId getZoneId() { return ZoneId.of("UTC"); }
    });

    // ---- Bug: toByte overflows for MMDD values > 127 (nearly all MonthDay values) ----

    @Test
    void toByte_january27_fitsInByte() {
        // MMDD = 127, max that fits in byte
        assertEquals((byte) 127, MonthDayConversions.toByte(MonthDay.of(1, 27), converter));
    }

    @Test
    void toByte_january28_overflows() {
        // MMDD = 128, exceeds Byte.MAX_VALUE (127)
        assertThrows(IllegalArgumentException.class,
                () -> MonthDayConversions.toByte(MonthDay.of(1, 28), converter));
    }

    @Test
    void toByte_february1_overflows() {
        // MMDD = 201, exceeds Byte.MAX_VALUE
        assertThrows(IllegalArgumentException.class,
                () -> MonthDayConversions.toByte(MonthDay.of(2, 1), converter));
    }

    @Test
    void toByte_december25_overflows() {
        // MMDD = 1225, exceeds Byte.MAX_VALUE
        assertThrows(IllegalArgumentException.class,
                () -> MonthDayConversions.toByte(MonthDay.of(12, 25), converter));
    }
}
