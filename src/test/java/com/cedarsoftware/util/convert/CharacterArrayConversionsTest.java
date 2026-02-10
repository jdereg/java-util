package com.cedarsoftware.util.convert;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for CharacterArrayConversions bugs.
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
class CharacterArrayConversionsTest {

    private static Converter converter() {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return ZoneId.of("UTC"); }
        };
        return new Converter(options);
    }

    @Test
    void toString_nullElement_shouldSkipNotProduceLiteralNull() {
        Converter conv = converter();
        Character[] chars = {'a', null, 'b'};
        String result = conv.convert(chars, String.class);
        // Should be "ab", not "anullb"
        assertEquals("ab", result);
    }

    @Test
    void toStringBuilder_nullElement_shouldSkipNotProduceLiteralNull() {
        Converter conv = converter();
        Character[] chars = {'h', null, 'i'};
        StringBuilder result = conv.convert(chars, StringBuilder.class);
        assertEquals("hi", result.toString());
    }

    @Test
    void toStringBuffer_nullElement_shouldSkipNotProduceLiteralNull() {
        Converter conv = converter();
        Character[] chars = {null, 'x', null};
        StringBuffer result = conv.convert(chars, StringBuffer.class);
        assertEquals("x", result.toString());
    }
}
