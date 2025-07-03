package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
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
class AtomicBooleanConversionsTests {

    private static Stream<Arguments> toBooleanParams() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams")
    void testToBoolean(boolean value) {
        boolean actual = AtomicBooleanConversions.toBoolean(new AtomicBoolean(value), null);
        assertThat(actual).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams")
    void testToAtomicBoolean(boolean value) {
        AtomicBoolean actual = AtomicBooleanConversions.toAtomicBoolean(new AtomicBoolean(value), null);
        assertThat(actual.get()).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams")
    void testToCharacter(boolean value) {
        ConverterOptions options = createConvertOptions('T', 'F');
        Converter converter = new Converter(options);
        Character actual = AtomicBooleanConversions.toCharacter(new AtomicBoolean(value), converter);
        Character expected = value ? 'T' : 'F';
        assertThat(actual).isEqualTo(expected);
    }

    private ConverterOptions createConvertOptions(final char t, final char f) {
        return new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public Character trueChar() { return t; }

            @Override
            public Character falseChar() { return f; }
        };
    }
}