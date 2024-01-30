package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.io.ReadOptionsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CharArrayConversionsTests {

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
        CharSequence s = this.converter.convert(new char[] { 'a', '\t', '\u0005'}, c);
        assertThat(s.toString()).isEqualTo("a\t\u0005");
    }

    @ParameterizedTest
    @MethodSource("charSequenceClasses")
    void testConvert_toCharSequence_withEmptyArray_returnsEmptyString(Class<? extends CharSequence> c) {
        CharSequence s = this.converter.convert(new char[]{}, String.class);
        assertThat(s.toString()).isEqualTo("");
    }
}
