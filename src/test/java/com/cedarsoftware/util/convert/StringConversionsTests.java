package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import com.cedarsoftware.util.ClassUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class StringConversionsTests {

    private Converter converter;

    @BeforeEach
    public void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testClassCompliance() throws Exception {
        Class<?> c = StringConversions.class;

        assertTrue(ClassUtilities.isClassFinal(c));
        assertTrue(ClassUtilities.areAllConstructorsPrivate(c));
    }

    private static Stream<Arguments> toYear_withParseableParams() {
        return Stream.of(
//                Arguments.of("1999"),
                Arguments.of("\t1999\r\n")
//                Arguments.of("   1999    ")
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
    void toYear_withNullableStrings_returnsYear0(String source) {
        Year year = this.converter.convert(source, Year.class);
        assertEquals(Year.of(0), year);
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
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> toCharParams() {
        return Stream.of(
                Arguments.of("0000", '\u0000'),
                Arguments.of("65", 'A'),
                Arguments.of("\t", '\t'),
                Arguments.of("\u0005", '\u0005')
        );
    }

    @ParameterizedTest
    @MethodSource("toCharParams")
    void toChar(String source, char value) {
        char actual = this.converter.convert(source, char.class);
        //LOG.info(Integer.toHexString(actual) + " = " + Integer.toHexString(value));
        assertThat(actual).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("toCharParams")
    void toChar(String source, Character value) {
        Character actual = this.converter.convert(source, Character.class);
        //LOG.info(Integer.toHexString(actual) + " = " + Integer.toHexString(value));
        assertThat(actual).isEqualTo(value);
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


    private static Stream<Arguments> offsetDateTime_isoFormat_sameEpochMilli() {
        return Stream.of(
                Arguments.of("2023-06-25T00:57:29.729+09:00"),
                Arguments.of("2023-06-24T17:57:29.729+02:00"),
                Arguments.of("2023-06-24T15:57:29.729Z"),
                Arguments.of("2023-06-24T11:57:29.729-04:00"),
                Arguments.of("2023-06-24T10:57:29.729-05:00"),
                Arguments.of("2023-06-24T08:57:29.729-07:00")
        );
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_isoFormat_sameEpochMilli")
    void toOffsetDateTime_parsingIsoFormat_returnsCorrectInstant(String input) {
        OffsetDateTime expected = OffsetDateTime.parse(input);
        OffsetDateTime actual = converter.convert(input, OffsetDateTime.class);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.toInstant().toEpochMilli()).isEqualTo(1687622249729L);
    }


    private static Stream<Arguments> dateUtilitiesParseFallback() {
        return Stream.of(
            Arguments.of("2024-01-19T15:30:45[Europe/London]", 1705678245000L),
            Arguments.of("2024-01-19T10:15:30[Asia/Tokyo]", 1705626930000L),
            Arguments.of("2024-01-19T20:45:00[America/New_York]", 1705715100000L),
            Arguments.of("2024-01-19T15:30:45 Europe/London", 1705678245000L),
            Arguments.of("2024-01-19T10:15:30 Asia/Tokyo", 1705626930000L),
            Arguments.of("2024-01-19T20:45:00 America/New_York", 1705715100000L),
            Arguments.of("2024-01-19T07:30GMT", 1705649400000L),
            Arguments.of("2024-01-19T07:30[GMT]", 1705649400000L),
            Arguments.of("2024-01-19T07:30 GMT", 1705649400000L),
            Arguments.of("2024-01-19T07:30 [GMT]", 1705649400000L),
            Arguments.of("2024-01-19T07:30  GMT", 1705649400000L),
            Arguments.of("2024-01-19T07:30  [GMT] ", 1705649400000L),

            Arguments.of("2024-01-19T07:30  GMT ", 1705649400000L),
            Arguments.of("2024-01-19T07:30:01 GMT", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01 [GMT]", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01GMT", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01[GMT]", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01.1 GMT", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.1 [GMT]", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.1GMT", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.1[GMT]", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12GMT", 1705649401120L),

            Arguments.of("2024-01-19T07:30:01Z", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01.1Z", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12Z", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01UTC", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01.1UTC", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12UTC", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01[UTC]", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01.1[UTC]", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12[UTC]", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01 UTC", 1705649401000L),

            Arguments.of("2024-01-19T07:30:01.1 UTC", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12 UTC", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01 [UTC]", 1705649401000L),
            Arguments.of("2024-01-19T07:30:01.1 [UTC]", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12 [UTC]", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01.1 UTC", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12 UTC", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01.1 [UTC]", 1705649401100L),
            Arguments.of("2024-01-19T07:30:01.12 [UTC]", 1705649401120L),

            Arguments.of("2024-01-19T07:30:01.12[GMT]", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01.12 GMT", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01.12 [GMT]", 1705649401120L),
            Arguments.of("2024-01-19T07:30:01.123GMT", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.123[GMT]", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.123 GMT", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.123 [GMT]", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.1234GMT", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.1234[GMT]", 1705649401123L),
            Arguments.of("2024-01-19T07:30:01.1234 GMT", 1705649401123L),

            Arguments.of("2024-01-19T07:30:01.1234 [GMT]", 1705649401123L),

            Arguments.of("07:30EST 2024-01-19", 1705667400000L),
            Arguments.of("07:30[EST] 2024-01-19", 1705667400000L),
            Arguments.of("07:30 EST 2024-01-19", 1705667400000L),

            Arguments.of("07:30 [EST] 2024-01-19", 1705667400000L),
            Arguments.of("07:30:01EST 2024-01-19", 1705667401000L),
            Arguments.of("07:30:01[EST] 2024-01-19", 1705667401000L),
            Arguments.of("07:30:01 EST 2024-01-19", 1705667401000L),
            Arguments.of("07:30:01 [EST] 2024-01-19", 1705667401000L),
            Arguments.of("07:30:01.123 EST 2024-01-19", 1705667401123L),
            Arguments.of("07:30:01.123 [EST] 2024-01-19", 1705667401123L)
        );
    }

    private static final ZoneId SOUTH_POLE = ZoneId.of("Antarctica/South_Pole");


    @ParameterizedTest
    @MethodSource("dateUtilitiesParseFallback")
    void toOffsetDateTime_dateUtilitiesParseFallback(String input, long epochMilli) {
        // ZoneId options not used since all string format has zone in it somewhere.
        // This is how json-io would use the convert.
        ConverterOptions options = createCustomZones(SOUTH_POLE);
        Converter converter = new Converter(options);
        OffsetDateTime actual = converter.convert(input, OffsetDateTime.class);
        assertThat(actual.toInstant().toEpochMilli()).isEqualTo(epochMilli);

        assertThat(actual.getOffset()).isNotEqualTo(ZoneOffset.of("+13:00"));
    }

    private static Stream<Arguments> classesThatReturnNull_whenTrimmedToEmpty() {
        return Stream.of(
                Arguments.of(Timestamp.class),
                Arguments.of(java.sql.Date.class),
                Arguments.of(Instant.class),
                Arguments.of(java.sql.Date.class),
                Arguments.of(Timestamp.class),
                Arguments.of(ZonedDateTime.class),
                Arguments.of(OffsetDateTime.class),
                Arguments.of(OffsetTime.class),
                Arguments.of(LocalDateTime.class),
                Arguments.of(LocalDate.class),
                Arguments.of(LocalTime.class)
        );
    }


    @ParameterizedTest
    @MethodSource("classesThatReturnNull_whenTrimmedToEmpty")
    void testClassesThatReturnNull_whenReceivingEmptyString(Class<?> c)
    {
        assertThat(this.converter.convert("", c)).isNull();
    }

    @ParameterizedTest
    @MethodSource("classesThatReturnNull_whenTrimmedToEmpty")
    void testClassesThatReturnNull_whenReceivingStringThatTrimsToEmptyString(Class<?> c)
    {
        assertThat(this.converter.convert("\t \r\n", c)).isNull();
    }

    private ConverterOptions createCustomZones(final ZoneId targetZoneId)
    {
        return new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public ZoneId getZoneId() {
                return targetZoneId;
            }
        };
    }

}
