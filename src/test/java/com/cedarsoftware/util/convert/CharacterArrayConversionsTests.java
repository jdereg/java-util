package com.cedarsoftware.util.convert;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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
class CharacterArrayConversionsTests {
    private Converter converter;

    @BeforeEach
    public void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> charSequenceClasses() {
        return Stream.of(
                Arguments.of(String.class),
                Arguments.of(StringBuilder.class),
                Arguments.of(StringBuffer.class)
        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceClasses")
    void testConvert_toCharSequence_withDifferentCharTypes(Class<? extends CharSequence> c) {
        CharSequence s = this.converter.convert(new Character[] { 'a', '\t', '\u0006'}, c);
        assertThat(s.toString()).isEqualTo("a\t\u0006");
    }

    @ParameterizedTest
    @MethodSource("charSequenceClasses")
    void testConvert_toCharSequence_withEmptyArray_returnsEmptyString(Class<? extends CharSequence> c) {
        CharSequence s = this.converter.convert(new Character[]{}, c);
        assertThat(s.toString()).isEqualTo("");
    }
}
