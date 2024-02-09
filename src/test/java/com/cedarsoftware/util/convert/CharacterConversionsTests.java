package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;

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
