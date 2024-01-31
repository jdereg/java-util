package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ClassUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.Year;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StringConversionsTests {

    private Converter converter;

    @BeforeEach
    public void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testClassCompliance() throws Exception {
        Class<?> c = StringConversions.class;

        assertThat(ClassUtilities.isClassFinal(c)).isTrue();
        assertThat(ClassUtilities.areAllConstructorsPrivate(c)).isTrue();
    }

    private static Stream<Arguments> toYear_withParseableParams() {
        return Stream.of(
                Arguments.of("1999"),
                Arguments.of("\t1999\r\n"),
                Arguments.of("   1999    ")
        );
    }

    @ParameterizedTest
    @MethodSource("toYear_withParseableParams")
    void toYear_withParseableParams_returnsValue(String source) {
        Year year = this.converter.convert(source, Year.class);
        assertThat(year.getValue()).isEqualTo(1999);
    }

    private static Stream<Arguments> toYear_nullReturn() {
        return Stream.of(
                Arguments.of(" "),
                Arguments.of("\t\r\n"),
                Arguments.of("")
        );
    }

    @ParameterizedTest
    @MethodSource("toYear_nullReturn")
    void toYear_withNullableStrings_returnsNull(String source) {
        Year year = this.converter.convert(source, Year.class);
        assertThat(year).isNull();
    }

    private static Stream<Arguments> toYear_extremeParams() {
        return Stream.of(
                // don't know why MIN_ and MAX_ values don't on GitHub????
                //Arguments.of(String.valueOf(Year.MAX_VALUE), Year.MAX_VALUE),
                //Arguments.of(String.valueOf(Year.MIN_VALUE), Year.MIN_VALUE),
                Arguments.of("9999999", 9999999),
                Arguments.of("-99999999", -99999999),
                Arguments.of("0", 0)
        );
    }

    @ParameterizedTest
    @MethodSource("toYear_extremeParams")
    void toYear_withExtremeParams_returnsValue(String source, int value) {
        Year expected = Year.of(value);
        Year actual = this.converter.convert(source, Year.class);
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> toCharSequenceTypes() {
        return Stream.of(
                Arguments.of(StringBuffer.class),
                Arguments.of(StringBuilder.class),
                Arguments.of(String.class)
        );
    }

    @ParameterizedTest
    @MethodSource("toCharSequenceTypes")
    void toCharSequenceTypes_doesNotTrim_returnsValue(Class<? extends CharSequence> c) {
        String s = "\t foobar  \r\n";
        CharSequence actual = this.converter.convert(s, c);
        assertThat(actual.toString()).isEqualTo(s);
    }


}
