package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicBooleanConversionsTests {

    private static Stream<Arguments> toByteParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.BYTE_ONE),
                Arguments.of(false, CommonValues.BYTE_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toByteParams")
    void testToByte(boolean value, Byte expected) {
        Byte actual = AtomicBooleanConversions.toByte(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toShortParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.SHORT_ONE),
                Arguments.of(false, CommonValues.SHORT_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toShortParams")
    void testToShort(boolean value, Short expected) {
        Short actual = AtomicBooleanConversions.toShort(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toIntegerParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.INTEGER_ONE),
                Arguments.of(false, CommonValues.INTEGER_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toIntegerParams")
    void testToInteger(boolean value, Integer expected) {
        Integer actual = AtomicBooleanConversions.toInteger(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toLongParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.LONG_ONE),
                Arguments.of(false, CommonValues.LONG_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void testToLong(boolean value, long expected) {
        long actual = AtomicBooleanConversions.toLong(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toFloatParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.FLOAT_ONE),
                Arguments.of(false, CommonValues.FLOAT_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toFloatParams")
    void testToFloat(boolean value, Float expected) {
        Float actual = AtomicBooleanConversions.toFloat(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }


    private static Stream<Arguments> toDoubleParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.DOUBLE_ONE),
                Arguments.of(false, CommonValues.DOUBLE_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toDoubleParams")
    void testToDouble(boolean value, Double expected) {
        Double actual = AtomicBooleanConversions.toDouble(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }


    private static Stream<Arguments> toBooleanParams() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams")
    void testToBoolean(boolean value) {
        boolean actual = AtomicBooleanConversions.toBoolean(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(Boolean.valueOf(value));
    }

    @ParameterizedTest
    @MethodSource("toIntegerParams")
    void testToAtomicInteger(boolean value, int integer) {
        AtomicInteger expected = new AtomicInteger(integer);;
        AtomicInteger actual = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(value), null, null);
        assertThat(actual.get()).isEqualTo(expected.get());
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void testToAtomicLong(boolean value, long expectedLong) {
        AtomicLong expected = new AtomicLong(expectedLong);
        AtomicLong actual = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(value), null, null);
        assertThat(actual.get()).isEqualTo(expected.get());
    }

    private static Stream<Arguments> toCharacter_withDefaultParams() {
        return Stream.of(
                Arguments.of(true, CommonValues.CHARACTER_ONE),
                Arguments.of(false, CommonValues.CHARACTER_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toCharacter_withDefaultParams")
    void testToCharacter_withDefaultParams(boolean value, char expected) {
        ConverterOptions options = createConvertOptions(CommonValues.CHARACTER_ONE, CommonValues.CHARACTER_ZERO);
        Character actual = AtomicBooleanConversions.toCharacter(new AtomicBoolean(value), null, options);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toCharacterCustomParams() {
        return Stream.of(
                Arguments.of('T', 'F', true, 'T'),
                Arguments.of('T', 'F', false, 'F')
        );
    }


    @ParameterizedTest
    @MethodSource("toCharacterCustomParams")
    void testToCharacter_withCustomChars(char trueChar, char falseChar, boolean value, char expected) {
        ConverterOptions options = createConvertOptions(trueChar, falseChar);
        char actual = BooleanConversions.toCharacter(value, null, options);
        assertThat(actual).isEqualTo(expected);
    }


    private static Stream<Arguments> toBigDecimalParams() {
        return Stream.of(
                Arguments.of(true, BigDecimal.ONE),
                Arguments.of(false, BigDecimal.ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toBigDecimalParams")
    void testToBigDecimal(boolean value, BigDecimal expected) {
        BigDecimal actual = AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private static Stream<Arguments> toBigIntegerParams() {
        return Stream.of(
                Arguments.of(true, BigInteger.ONE),
                Arguments.of(false, BigInteger.ZERO)
        );
    }
    @ParameterizedTest
    @MethodSource("toBigIntegerParams")
    void testToBigDecimal(boolean value, BigInteger expected) {
        BigInteger actual = AtomicBooleanConversions.toBigInteger(new AtomicBoolean(value), null, null);
        assertThat(actual).isSameAs(expected);
    }

    private ConverterOptions createConvertOptions(final char t, final char f)
    {
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

