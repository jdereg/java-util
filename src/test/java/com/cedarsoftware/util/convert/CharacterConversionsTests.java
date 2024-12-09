package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

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
class CharacterConversionsTests {

    private Converter converter;

    @BeforeEach
    void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    @ParameterizedTest
    @NullSource
    void toByteObject_whenCharacterIsNull_returnsNull(Character ch) {
        assertThat(this.converter.convert(ch, Byte.class))
                .isNull();
    }

    @ParameterizedTest
    @NullSource
    void toByte_whenCharacterIsNull_returnsCommonValuesZero(Character ch) {
        assertThat(this.converter.convert(ch, byte.class))
                .isSameAs(CommonValues.BYTE_ZERO);
    }

    @ParameterizedTest
    @NullSource
    void toIntObject_whenCharacterIsNull_returnsNull(Character ch) {
        assertThat(this.converter.convert(ch, Integer.class))
                .isNull();
    }

    @ParameterizedTest
    @NullSource
    void toInteger_whenCharacterIsNull_returnsCommonValuesZero(Character ch) {
        assertThat(this.converter.convert(ch, int.class))
                .isSameAs(CommonValues.INTEGER_ZERO);
    }
}
