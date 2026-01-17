package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Enum conversion support in Converter.
 * Tests String → Enum (by name) and Number → Enum (by ordinal) conversions.
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
class EnumConversionsTest {

    private Converter converter;

    // Test enum
    enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    // Test enum with explicit values
    enum Status {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }

    @BeforeEach
    void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    // ==================== String → Enum Tests ====================

    @Test
    void stringToEnum_validName_returnsEnumConstant() {
        Day result = converter.convert("MONDAY", Day.class);
        assertThat(result).isEqualTo(Day.MONDAY);
    }

    @Test
    void stringToEnum_allValues_returnCorrectConstants() {
        for (Day day : Day.values()) {
            Day result = converter.convert(day.name(), Day.class);
            assertThat(result).isEqualTo(day);
        }
    }

    @Test
    void stringToEnum_withWhitespace_trimmedAndConverted() {
        Day result = converter.convert("  FRIDAY  ", Day.class);
        assertThat(result).isEqualTo(Day.FRIDAY);
    }

    @Test
    void stringToEnum_invalidName_throwsException() {
        assertThatThrownBy(() -> converter.convert("INVALID", Day.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    void stringToEnum_caseSensitive_wrongCaseThrows() {
        assertThatThrownBy(() -> converter.convert("monday", Day.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stringToEnum_emptyString_throwsException() {
        assertThatThrownBy(() -> converter.convert("", Day.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stringToEnum_differentEnumType_worksCorrectly() {
        Status result = converter.convert("ACTIVE", Status.class);
        assertThat(result).isEqualTo(Status.ACTIVE);
    }

    // ==================== Number → Enum Tests ====================

    @Test
    void integerToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(0, Day.class);
        assertThat(result).isEqualTo(Day.MONDAY);
    }

    @Test
    void integerToEnum_allOrdinals_returnCorrectConstants() {
        for (Day day : Day.values()) {
            Day result = converter.convert(day.ordinal(), Day.class);
            assertThat(result).isEqualTo(day);
        }
    }

    @Test
    void longToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(6L, Day.class);
        assertThat(result).isEqualTo(Day.SUNDAY);
    }

    @Test
    void shortToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert((short) 2, Day.class);
        assertThat(result).isEqualTo(Day.WEDNESDAY);
    }

    @Test
    void byteToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert((byte) 3, Day.class);
        assertThat(result).isEqualTo(Day.THURSDAY);
    }

    @Test
    void doubleToEnum_truncatedToOrdinal_returnsEnumConstant() {
        Day result = converter.convert(4.9, Day.class);
        assertThat(result).isEqualTo(Day.FRIDAY);
    }

    @Test
    void floatToEnum_truncatedToOrdinal_returnsEnumConstant() {
        Day result = converter.convert(5.1f, Day.class);
        assertThat(result).isEqualTo(Day.SATURDAY);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 7, 100, Integer.MAX_VALUE})
    void integerToEnum_invalidOrdinal_throwsException(int ordinal) {
        assertThatThrownBy(() -> converter.convert(ordinal, Day.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ordinal value")
                .hasMessageContaining(String.valueOf(ordinal));
    }

    // ==================== Various Number Types → Enum Tests ====================

    @Test
    void bigIntegerToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(BigInteger.valueOf(3), Day.class);
        assertThat(result).isEqualTo(Day.THURSDAY);
    }

    @Test
    void bigDecimalToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(BigDecimal.valueOf(4), Day.class);
        assertThat(result).isEqualTo(Day.FRIDAY);
    }

    @Test
    void atomicIntegerToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(new AtomicInteger(5), Day.class);
        assertThat(result).isEqualTo(Day.SATURDAY);
    }

    @Test
    void atomicLongToEnum_validOrdinal_returnsEnumConstant() {
        Day result = converter.convert(new AtomicLong(6), Day.class);
        assertThat(result).isEqualTo(Day.SUNDAY);
    }

    @Test
    void negativeOrdinal_throwsDescriptiveException() {
        assertThatThrownBy(() -> converter.convert(-5, Day.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-5")
                .hasMessageContaining("Day")
                .hasMessageContaining("0")
                .hasMessageContaining("6");
    }

    @Test
    void numberToEnum_differentEnumType_worksCorrectly() {
        Status result = converter.convert(2, Status.class);
        assertThat(result).isEqualTo(Status.COMPLETED);
    }

    // ==================== isConversionSupportedFor Tests ====================

    @Test
    void isConversionSupportedFor_stringToEnum_returnsTrue() {
        assertThat(converter.isConversionSupportedFor(String.class, Day.class)).isTrue();
    }

    @Test
    void isConversionSupportedFor_integerToEnum_returnsTrue() {
        assertThat(converter.isConversionSupportedFor(Integer.class, Day.class)).isTrue();
    }

    @Test
    void isConversionSupportedFor_longToEnum_returnsTrue() {
        assertThat(converter.isConversionSupportedFor(Long.class, Day.class)).isTrue();
    }

    // ==================== Enum → String Tests (existing functionality) ====================

    @Test
    void enumToString_returnsEnumName() {
        String result = converter.convert(Day.WEDNESDAY, String.class);
        assertThat(result).isEqualTo("WEDNESDAY");
    }

    // ==================== Enum → Integer Tests ====================

    @Test
    void enumToInteger_returnsOrdinal() {
        Integer result = converter.convert(Day.MONDAY, Integer.class);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void enumToInteger_allValues_returnCorrectOrdinals() {
        for (Day day : Day.values()) {
            Integer result = converter.convert(day, Integer.class);
            assertThat(result).isEqualTo(day.ordinal());
        }
    }

    @Test
    void enumToInt_returnsOrdinal() {
        int result = converter.convert(Day.SUNDAY, int.class);
        assertThat(result).isEqualTo(6);
    }

    // ==================== Round-trip Tests ====================

    @Test
    void stringToEnumRoundTrip_preservesValue() {
        String original = "THURSDAY";
        Day enumValue = converter.convert(original, Day.class);
        String result = converter.convert(enumValue, String.class);
        assertThat(result).isEqualTo(original);
    }

    @Test
    void ordinalToEnumRoundTrip_preservesValue() {
        int original = 4;
        Day enumValue = converter.convert(original, Day.class);
        int result = enumValue.ordinal();
        assertThat(result).isEqualTo(original);
    }

    // ==================== Advertised Conversions Tests ====================

    @Test
    void allSupportedConversions_includesStringToEnum() {
        java.util.Map<Class<?>, java.util.Set<Class<?>>> conversions = Converter.allSupportedConversions();
        assertThat(conversions.get(String.class)).contains(Enum.class);
    }

    @Test
    void allSupportedConversions_includesNumberToEnum() {
        java.util.Map<Class<?>, java.util.Set<Class<?>>> conversions = Converter.allSupportedConversions();
        assertThat(conversions.get(Number.class)).contains(Enum.class);
    }

    @Test
    void getSupportedConversions_includesStringToEnum() {
        java.util.Map<String, java.util.Set<String>> conversions = Converter.getSupportedConversions();
        assertThat(conversions.get("String")).contains("Enum");
    }

    @Test
    void getSupportedConversions_includesNumberToEnum() {
        java.util.Map<String, java.util.Set<String>> conversions = Converter.getSupportedConversions();
        assertThat(conversions.get("Number")).contains("Enum");
    }
}
