package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static com.cedarsoftware.util.Converter.localDateTimeToMillis;
import static com.cedarsoftware.util.Converter.localDateToMillis;
import static com.cedarsoftware.util.Converter.zonedDateTimeToMillis;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.bar;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.foo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @aFuthor John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class ConverterTest
{

    private Converter converter;

    enum fubar
    {
        foo, bar, baz, quz
    }

    @BeforeEach
    public void before() {
        // create converter with default options
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> toByte_minValueParams() {
        return Stream.of(
                Arguments.of("-128"),
                Arguments.of(Byte.MIN_VALUE),
                Arguments.of((short)Byte.MIN_VALUE),
                Arguments.of((int)Byte.MIN_VALUE),
                Arguments.of((long)Byte.MIN_VALUE),
                Arguments.of(-128.0f),
                Arguments.of(-128.0d),
                Arguments.of( new BigDecimal("-128.0")),
                Arguments.of( new BigDecimal("-128.9")),
                Arguments.of( new BigInteger("-128")),
                Arguments.of( new AtomicInteger(-128)),
                Arguments.of( new AtomicLong(-128L)));
    }

    @ParameterizedTest
    @MethodSource("toByte_minValueParams")
    void toByte_convertsToByteMinValue(Object value)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isEqualTo(Byte.MIN_VALUE);
    }

    @ParameterizedTest
    @MethodSource("toByte_minValueParams")
    void toByteAsPrimitive_convertsToByteMinValue(Object value)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isEqualTo(Byte.MIN_VALUE);
    }


    private static Stream<Arguments> toByte_maxValueParams() {
        return Stream.of(
                Arguments.of("127.9"),
                Arguments.of("127"),
                Arguments.of(Byte.MAX_VALUE),
                Arguments.of((short)Byte.MAX_VALUE),
                Arguments.of((int)Byte.MAX_VALUE),
                Arguments.of((long)Byte.MAX_VALUE),
                Arguments.of(127.0f),
                Arguments.of(127.0d),
                Arguments.of( new BigDecimal("127.0")),
                Arguments.of( new BigInteger("127")),
                Arguments.of( new AtomicInteger(127)),
                Arguments.of( new AtomicLong(127L)));
    }

    @ParameterizedTest
    @MethodSource("toByte_maxValueParams")
    void toByte_returnsByteMaxValue(Object value)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isEqualTo(Byte.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("toByte_maxValueParams")
    void toByte_withPrimitiveType_returnsByteMaxVAlue(Object value)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isEqualTo(Byte.MAX_VALUE);
    }

    private static Stream<Arguments> toByte_booleanParams() {
        return Stream.of(
                Arguments.of( true, CommonValues.BYTE_ONE),
                Arguments.of( false, CommonValues.BYTE_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.BYTE_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.BYTE_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.BYTE_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.BYTE_ZERO));
    }

    @ParameterizedTest
    @MethodSource("toByte_booleanParams")
    void toByte_fromBoolean_isSameAsCommonValueObject(Object value, Byte expectedResult)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("toByte_booleanParams")
    void toByte_fromBoolean_usingPrimitive_isSameAsCommonValueObject(Object value, Byte expectedResult)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    private static Stream<Arguments> toByte_illegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a byte"),
                Arguments.of("-129", "not parseable as a byte"),
                Arguments.of("128", "not parseable as a byte"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("toByte_illegalArguments")
    void toByte_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, byte.class))
                .withMessageContaining(partialMessage);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void toByte_whenNullOrEmpty_andCovnertingToPrimitive_returnsZero(String s)
    {
        byte converted = this.converter.convert(s, byte.class);
        assertThat(converted).isZero();
    }

    @ParameterizedTest
    @NullSource
    void toByte_whenNull_andNotPrimitive_returnsNull(String s)
    {
        Byte converted = this.converter.convert(s, Byte.class);
        assertThat(converted).isNull();
    }

    @ParameterizedTest
    @EmptySource
    void toByte_whenEmpty_andNotPrimitive_returnsZero(String s)
    {
        Byte converted = this.converter.convert(s, Byte.class);
        assertThat(converted).isZero();
    }

    private static Stream<Arguments> toShortParams() {
        return Stream.of(
                Arguments.of("-32768.9", (short)-32768),
                Arguments.of("-32768", (short)-32768),
                Arguments.of("32767", (short)32767),
                Arguments.of("32767.9", (short)32767),
                Arguments.of(Byte.MIN_VALUE, (short)-128),
                Arguments.of(Byte.MAX_VALUE, (short)127),
                Arguments.of(Short.MIN_VALUE, (short)-32768),
                Arguments.of(Short.MAX_VALUE, (short)32767),
                Arguments.of(-25, (short)-25),
                Arguments.of(24, (short)24),
                Arguments.of(-128L, (short)-128),
                Arguments.of(127L, (short)127),
                Arguments.of(-128.0f, (short)-128),
                Arguments.of(127.0f, (short)127),
                Arguments.of(-128.0d, (short)-128),
                Arguments.of(127.0d, (short)127),
                Arguments.of( new BigDecimal("100"),(short)100),
                Arguments.of( new BigInteger("120"), (short)120),
                Arguments.of( new AtomicInteger(25), (short)25),
                Arguments.of( new AtomicLong(100L), (short)100)
        );
    }


    @ParameterizedTest
    @MethodSource("toShortParams")
    void toShort(Object value, Short expectedResult)
    {
        Short converted = this.converter.convert(value, Short.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("toShortParams")
    void toShort_usingPrimitiveClass(Object value, short expectedResult) {
        short converted = this.converter.convert(value, short.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> toShort_withBooleanPrams() {
        return Stream.of(
                Arguments.of( true, CommonValues.SHORT_ONE),
                Arguments.of( false, CommonValues.SHORT_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.SHORT_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.SHORT_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.SHORT_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.SHORT_ZERO));
    }

    @ParameterizedTest
    @MethodSource("toShort_withBooleanPrams")
    void toShort_withBooleanPrams_returnsCommonValue(Object value, Short expectedResult)
    {
        Short converted = this.converter.convert(value, Short.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("toShort_withBooleanPrams")
    void toShort_withBooleanPrams_usingPrimitive_returnsCommonValue(Object value, Short expectedResult)
    {
        short converted = this.converter.convert(value, short.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    private static Stream<Arguments> toShortParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of("-32769", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of("32768", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("toShortParams_withIllegalArguments")
    void toShort_withIllegalArguments_throwsException(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, short.class))
                .withMessageContaining(partialMessage);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void toShort_usingPrimitive_withNullAndEmptySource_returnsZero(String s)
    {
        short converted = this.converter.convert(s, short.class);
        assertThat(converted).isZero();
    }

    @ParameterizedTest
    @NullSource
    void toShort_whenNotPrimitive_whenNull_returnsNull(String s)
    {
        Short converted = this.converter.convert(s, Short.class);
        assertThat(converted).isNull();
    }

    @ParameterizedTest
    @EmptySource
    void toShort_whenNotPrimitive_whenEmptyString_returnsNull(String s)
    {
        Short converted = this.converter.convert(s, Short.class);
        assertThat(converted).isZero();
    }

    private static Stream<Arguments> toIntParams() {
        return Stream.of(
                Arguments.of("-32768", -32768),
                Arguments.of("-45000", -45000),
                Arguments.of("32767", 32767),
                Arguments.of(new BigInteger("8675309"), 8675309),
                Arguments.of(Byte.MIN_VALUE,-128),
                Arguments.of(Byte.MAX_VALUE, 127),
                Arguments.of(Short.MIN_VALUE, -32768),
                Arguments.of(Short.MAX_VALUE, 32767),
                Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE),
                Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE),
                Arguments.of(-128L, -128),
                Arguments.of(127L, 127),
                Arguments.of(3.14, 3),
                Arguments.of(-128.0f, -128),
                Arguments.of(127.0f, 127),
                Arguments.of(-128.0d, -128),
                Arguments.of(127.0d, 127),
                Arguments.of( new BigDecimal("100"),100),
                Arguments.of( new BigInteger("120"), 120),
                Arguments.of( new AtomicInteger(75), 75),
                Arguments.of( new AtomicInteger(1), 1),
                Arguments.of( new AtomicInteger(0), 0),
                Arguments.of( new AtomicLong(Integer.MAX_VALUE), Integer.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("toIntParams")
    void toInt(Object value, Integer expectedResult)
    {
        Integer converted = this.converter.convert(value, Integer.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("toIntParams")
    void toInt_usingPrimitives(Object value, int expectedResult)
    {
        int converted = this.converter.convert(value, int.class);
        assertThat(converted).isEqualTo(expectedResult);
    }


    private static Stream<Arguments> toInt_booleanParams() {
        return Stream.of(
                Arguments.of( true, CommonValues.INTEGER_ONE),
                Arguments.of( false, CommonValues.INTEGER_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.INTEGER_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.INTEGER_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.INTEGER_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.INTEGER_ZERO));
    }

    @ParameterizedTest
    @MethodSource("toInt_booleanParams")
    void toInt_fromBoolean_returnsCommonValue(Object value, Integer expectedResult)
    {
        Integer converted = this.converter.convert(value, Integer.class);
        assertThat(converted).isSameAs(expectedResult);
    }


    private static Stream<Arguments> toInt_illegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "Value: 45badNumber not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of( "12147483648", "Value: 12147483648 not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of("2147483649", "Value: 2147483649 not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("toInt_illegalArguments")
    void toInt_withIllegalArguments_throwsException(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, Integer.class))
                .withMessageContaining(partialMessage);
    }


    @ParameterizedTest
    @NullAndEmptySource
    void toInt_usingPrimitive_whenEmptyOrNullString_returnsZero(String s)
    {
        int converted = this.converter.convert(s, int.class);
        assertThat(converted).isZero();
    }

    @ParameterizedTest
    @NullSource
    void toInt_whenNotPrimitive_andNullString_returnsNull(String s)
    {
        Integer converted = this.converter.convert(s, Integer.class);
        assertThat(converted).isNull();
    }

    @ParameterizedTest
    @EmptySource
    void toInt_whenNotPrimitive_andEmptyString_returnsZero(String s)
    {
        Integer converted = this.converter.convert(s, Integer.class);
        assertThat(converted).isZero();
    }

    private static Stream<Arguments> toLongParams() {
        return Stream.of(
                Arguments.of("-32768", -32768L),
                Arguments.of("32767", 32767L),
                Arguments.of(Byte.MIN_VALUE,-128L),
                Arguments.of(Byte.MAX_VALUE, 127L),
                Arguments.of(Short.MIN_VALUE, -32768L),
                Arguments.of(Short.MAX_VALUE, 32767L),
                Arguments.of(Integer.MIN_VALUE, -2147483648L),
                Arguments.of(Integer.MAX_VALUE, 2147483647L),
                Arguments.of(Long.MIN_VALUE, -9223372036854775808L),
                Arguments.of(Long.MAX_VALUE, 9223372036854775807L),
                Arguments.of(-128.0f, -128L),
                Arguments.of(127.0f, 127L),
                Arguments.of(-128.0d, -128L),
                Arguments.of(127.0d, 127L),
                Arguments.of( new BigDecimal("100"), 100L),
                Arguments.of( new BigInteger("120"), 120L),
                Arguments.of( new AtomicInteger(25), 25L),
                Arguments.of( new AtomicLong(100L), 100L)
        );
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void toLong(Object value, Long expectedResult)
    {
        Long converted = this.converter.convert(value, Long.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void toLong_usingPrimitives(Object value, long expectedResult)
    {
        long converted = this.converter.convert(value, long.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> toLong_booleanParams() {
        return Stream.of(
                Arguments.of( true, CommonValues.LONG_ONE),
                Arguments.of( false, CommonValues.LONG_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.LONG_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.LONG_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.LONG_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.LONG_ZERO));
    }

    @ParameterizedTest
    @MethodSource("toLong_booleanParams")
    void toLong_withBooleanParams_returnsCommonValues(Object value, Long expectedResult)
    {
        Long converted = this.converter.convert(value, Long.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void toLong_whenPrimitive_andNullOrEmpty_returnsZero(String s)
    {
        long converted = this.converter.convert(s, long.class);
        assertThat(converted).isZero();
    }

    @ParameterizedTest
    @NullSource
    void toLong_whenNotPrimitive_andNull_returnsNull(String s)
    {
        Long converted = this.converter.convert(s, Long.class);
        assertThat(converted).isNull();
    }

    @ParameterizedTest
    @EmptySource
    void toLong_whenNotPrimitive_andEmptyString_returnsZero(String s)
    {
        Long converted = this.converter.convert(s, Long.class);
        assertThat(converted).isZero();
    }

    @Test
    void toLong_fromDate()
    {
        Date date = Date.from(Instant.now());
        Long converted = this.converter.convert(date, Long.class);
        assertThat(converted).isEqualTo(date.getTime());
    }

    @Test
    void toLong_fromCalendar()
    {
        Calendar date = Calendar.getInstance();
        Long converted = this.converter.convert(date, Long.class);
        assertThat(converted).isEqualTo(date.getTime().getTime());
    }




    private static Stream<Arguments> testLongParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a long value"),
                Arguments.of( "-9223372036854775809", "not parseable as a long value"),
                Arguments.of("9223372036854775808", "not parseable as a long value"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testLongParams_withIllegalArguments")
    void testLong_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, Long.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> testAtomicLongParams() {
        return Stream.of(
                Arguments.of("-32768", new AtomicLong(-32768L)),
                Arguments.of("32767", new AtomicLong(32767L)),
                Arguments.of(Byte.MIN_VALUE, new AtomicLong(-128L)),
                Arguments.of(Byte.MAX_VALUE, new AtomicLong(127L)),
                Arguments.of(Short.MIN_VALUE, new AtomicLong(-32768L)),
                Arguments.of(Short.MAX_VALUE, new AtomicLong(32767L)),
                Arguments.of(Integer.MIN_VALUE, new AtomicLong(-2147483648L)),
                Arguments.of(Integer.MAX_VALUE, new AtomicLong(2147483647L)),
                Arguments.of(Long.MIN_VALUE, new AtomicLong(-9223372036854775808L)),
                Arguments.of(Long.MAX_VALUE, new AtomicLong(9223372036854775807L)),
                Arguments.of(-128.0f, new AtomicLong(-128L)),
                Arguments.of(127.0f, new AtomicLong(127L)),
                Arguments.of(-128.0d, new AtomicLong(-128L)),
                Arguments.of(127.0d, new AtomicLong(127L)),
                Arguments.of( new BigDecimal("100"), new AtomicLong(100L)),
                Arguments.of( new BigInteger("120"), new AtomicLong(120L)),
                Arguments.of( new AtomicInteger(25), new AtomicLong(25L)),
                Arguments.of( new AtomicLong(100L), new AtomicLong(100L))
        );
    }

    @ParameterizedTest
    @MethodSource("testAtomicLongParams")
    void testAtomicLong(Object value, AtomicLong expectedResult)
    {
        AtomicLong converted = this.converter.convert(value, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(expectedResult.get());
    }

    private static Stream<Arguments> testAtomicLong_fromBooleanParams() {
        return Stream.of(
                Arguments.of( true, new AtomicLong(CommonValues.LONG_ONE)),
                Arguments.of( false, new AtomicLong(CommonValues.LONG_ZERO)),
                Arguments.of( Boolean.TRUE,  new AtomicLong(CommonValues.LONG_ONE)),
                Arguments.of( Boolean.FALSE, new AtomicLong(CommonValues.LONG_ZERO)),
                Arguments.of( new AtomicBoolean(true), new AtomicLong(CommonValues.LONG_ONE)),
                Arguments.of( new AtomicBoolean(false), new AtomicLong(CommonValues.LONG_ZERO)));
    }

    @ParameterizedTest
    @MethodSource("testAtomicLong_fromBooleanParams")
    void testAtomicLong_fromBoolean(Object value, AtomicLong expectedResult)
    {
        AtomicLong converted = this.converter.convert(value, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(expectedResult.get());
    }

    @ParameterizedTest
    @NullSource
    void testConvertToAtomicLong_whenNullString(String s)
    {
        AtomicLong converted = this.converter.convert(s, AtomicLong.class);
        assertThat(converted).isNull();
    }

    @ParameterizedTest
    @EmptySource
    void testConvertToAtomicLong_whenEmptyString(String s)
    {
        AtomicLong converted = this.converter.convert(s, AtomicLong.class);
        assertThat(converted.get()).isZero();
    }

    @Test
    void testAtomicLong_fromDate()
    {
        Date date = Date.from(Instant.now());
        AtomicLong converted = this.converter.convert(date, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(date.getTime());
    }

    @Test
    void testAtomicLong_fromCalendar()
    {
        Calendar date = Calendar.getInstance();
        AtomicLong converted = this.converter.convert(date, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(date.getTime().getTime());
    }

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    private static final ZoneId GMT = ZoneId.of("GMT");

    private static Stream<Arguments> toBooleanParams_trueCases() {
        return Stream.of(
                Arguments.of("true"),
                Arguments.of("True"),
                Arguments.of("TRUE"),
                Arguments.of("T"),
                Arguments.of("t"),
                Arguments.of("1"),
                Arguments.of('T'),
                Arguments.of('t'),
                Arguments.of('1'),
                Arguments.of(Short.MIN_VALUE),
                Arguments.of(Short.MAX_VALUE),
                Arguments.of(Integer.MAX_VALUE),
                Arguments.of(Integer.MIN_VALUE),
                Arguments.of(Long.MIN_VALUE),
                Arguments.of(Long.MAX_VALUE),
                Arguments.of(Boolean.TRUE),
                Arguments.of(new BigInteger("8675309")),
                Arguments.of(new BigDecimal("59.99")),
                Arguments.of(Double.MIN_VALUE),
                Arguments.of(Double.MAX_VALUE),
                Arguments.of(Float.MIN_VALUE),
                Arguments.of(Float.MAX_VALUE),
                Arguments.of(-128.0d),
                Arguments.of(127.0d),
                Arguments.of( new AtomicInteger(75)),
                Arguments.of( new AtomicInteger(1)),
                Arguments.of( new AtomicInteger(Integer.MAX_VALUE)),
                Arguments.of( new AtomicLong(Long.MAX_VALUE))
        );
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams_trueCases")
    void testToBoolean_trueCases(Object input) {
        assertThat(this.converter.convert(input, boolean.class)).isTrue();
    }

    private static Stream<Arguments> toBooleanParams_falseCases() {
        return Stream.of(
                Arguments.of("false"),
                Arguments.of("f"),
                Arguments.of("F"),
                Arguments.of("FALSE"),
                Arguments.of("9"),
                Arguments.of("0"),
                Arguments.of('F'),
                Arguments.of('f'),
                Arguments.of('0'),
                Arguments.of(Character.MAX_VALUE),
                Arguments.of((byte)0),
                Arguments.of((short)0),
                Arguments.of(0),
                Arguments.of(0L),
                Arguments.of(BigInteger.ZERO),
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(0.0f),
                Arguments.of(0.0d),
                Arguments.of( new AtomicInteger(0)),
                Arguments.of( new AtomicLong(0))
        );
    }

    @ParameterizedTest
    @MethodSource("toBooleanParams_falseCases")
    void testToBoolean_falseCases(Object input) {
        assertThat(this.converter.convert(input, boolean.class)).isFalse();
    }


    private static Stream<Arguments> epochMillis_withLocalDateTimeInformation() {
        return Stream.of(
                Arguments.of(1687622249729L, TOKYO, LocalDateTime.of(2023, 6, 25, 0, 57, 29, 729000000)),
                Arguments.of(1687622249729L, PARIS,  LocalDateTime.of(2023, 6, 24, 17, 57, 29, 729000000)),
                Arguments.of(1687622249729L, GMT,  LocalDateTime.of(2023, 6, 24, 15, 57, 29, 729000000)),
                Arguments.of(1687622249729L, NEW_YORK,  LocalDateTime.of(2023, 6, 24, 11, 57, 29, 729000000)),
                Arguments.of(1687622249729L, CHICAGO,  LocalDateTime.of(2023, 6, 24, 10, 57, 29, 729000000)),
                Arguments.of(1687622249729L, LOS_ANGELES,  LocalDateTime.of(2023, 6, 24, 8, 57, 29, 729000000)),
                Arguments.of(946702799959L, TOKYO,  LocalDateTime.of(2000, 1, 1, 13, 59, 59, 959000000)),
                Arguments.of(946702799959L, PARIS,  LocalDateTime.of(2000, 1, 1, 5, 59, 59, 959000000)),
                Arguments.of(946702799959L, GMT,  LocalDateTime.of(2000, 1, 1, 4, 59, 59, 959000000)),
                Arguments.of(946702799959L, NEW_YORK,  LocalDateTime.of(1999, 12, 31, 23, 59, 59, 959000000)),
                Arguments.of(946702799959L, CHICAGO,  LocalDateTime.of(1999, 12, 31, 22, 59, 59, 959000000)),
                Arguments.of(946702799959L, LOS_ANGELES,  LocalDateTime.of(1999, 12, 31, 20, 59, 59, 959000000))

        );
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMilli);

        LocalDateTime localDateTime = this.converter.convert(calendar, LocalDateTime.class, createConvertOptions(zoneId, zoneId));

        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalDateTime_whenCalendarTimeZoneMatches(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        LocalDateTime localDateTime = this.converter.convert(calendar, LocalDateTime.class, createConvertOptions(zoneId, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @Test
    void testCalendarToLocalDateTime_whenCalendarTimeZoneDoesNotMatch() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK));
        calendar.setTimeInMillis(1687622249729L);

        LocalDateTime localDateTime = this.converter.convert(calendar, LocalDateTime.class, createConvertOptions(NEW_YORK, TOKYO));

        System.out.println(localDateTime);

        assertThat(localDateTime)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(25)
                .hasHour(0)
                .hasMinute(57)
                .hasSecond(29)
                .hasNano(729000000);
    }

    @Test
    void testCalendar_roundTrip() {

        // Create LocalDateTime as CHICAGO TIME.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(CHICAGO));
        calendar.setTimeInMillis(1687622249729L);

        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(5);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(24);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(10);
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(57);
        assertThat(calendar.getTimeInMillis()).isEqualTo(1687622249729L);

        // Convert calendar calendar to TOKYO LocalDateTime
        LocalDateTime localDateTime = this.converter.convert(calendar, LocalDateTime.class, createConvertOptions(CHICAGO, TOKYO));

        assertThat(localDateTime)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(25)
                .hasHour(0)
                .hasMinute(57)
                .hasSecond(29)
                .hasNano(729000000);

        //  Convert Tokyo local date time to CHICAGO Calendar
        //  We don't know the source ZoneId we are trying to convert.
        Calendar actual = this.converter.convert(localDateTime, Calendar.class, createConvertOptions(TOKYO, CHICAGO));

        assertThat(actual.get(Calendar.MONTH)).isEqualTo(5);
        assertThat(actual.get(Calendar.DAY_OF_MONTH)).isEqualTo(24);
        assertThat(actual.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(actual.get(Calendar.HOUR_OF_DAY)).isEqualTo(10);
        assertThat(actual.get(Calendar.MINUTE)).isEqualTo(57);
        assertThat(actual.getTimeInMillis()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        LocalDateTime localDateTime = this.converter.convert(time, LocalDateTime.class, createConvertOptions(zoneId, zoneId));

        assertThat(time.toInstant().toEpochMilli()).isEqualTo(epochMilli);
        assertThat(localDateTime).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLong(long epochMilli, ZoneId zoneId, LocalDateTime localDateTime)
    {
        ZonedDateTime time = ZonedDateTime.of(localDateTime, zoneId);

        long instant = this.converter.convert(time, long.class, createConvertOptions(zoneId, zoneId));

        assertThat(instant).isEqualTo(epochMilli);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testLongToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        LocalDateTime localDateTime = this.converter.convert(epochMilli, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testAtomicLongToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        AtomicLong time = new AtomicLong(epochMilli);

        LocalDateTime localDateTime = this.converter.convert(time, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testBigIntegerToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        BigInteger bi = BigInteger.valueOf(epochMilli);

        LocalDateTime localDateTime = this.converter.convert(bi, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testBigDecimalToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        BigDecimal bd = BigDecimal.valueOf(epochMilli);

        LocalDateTime localDateTime = this.converter.convert(bd, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        LocalDateTime localDateTime = this.converter.convert(instant, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testDateToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Date date = new Date(epochMilli);
        LocalDateTime localDateTime = this.converter.convert(date, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testSqlDateToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        java.sql.Date date = new java.sql.Date(epochMilli);
        LocalDateTime localDateTime = this.converter.convert(date, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testTimestampToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Timestamp date = new Timestamp(epochMilli);
        LocalDateTime localDateTime = this.converter.convert(date, LocalDateTime.class, createConvertOptions(null, zoneId));
        assertThat(localDateTime).isEqualTo(expected);
    }


    private static Stream<Arguments> epochMillis_withLocalDateInformation() {
        return Stream.of(
                Arguments.of(1687622249729L, TOKYO, LocalDate.of(2023, 6, 25)),
                Arguments.of(1687622249729L, PARIS, LocalDate.of(2023, 6, 24)),
                Arguments.of(1687622249729L, GMT, LocalDate.of(2023, 6, 24)),
                Arguments.of(1687622249729L, NEW_YORK, LocalDate.of(2023, 6, 24)),
                Arguments.of(1687622249729L, CHICAGO, LocalDate.of(2023, 6, 24)),
                Arguments.of(1687622249729L, LOS_ANGELES, LocalDate.of(2023, 6, 24)),
                Arguments.of(946702799959L, TOKYO, LocalDate.of(2000, 1, 1)),
                Arguments.of(946702799959L, PARIS, LocalDate.of(2000, 1, 1)),
                Arguments.of(946702799959L, GMT, LocalDate.of(2000, 1, 1)),
                Arguments.of(946702799959L, NEW_YORK, LocalDate.of(1999, 12, 31)),
                Arguments.of(946702799959L, CHICAGO, LocalDate.of(1999, 12, 31)),
                Arguments.of(946702799959L, LOS_ANGELES, LocalDate.of(1999, 12, 31))

        );
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testCalendarToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMilli);

        LocalDate localDate = this.converter.convert(calendar, LocalDate.class, createConvertOptions(null, zoneId));
        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testCalendarToLocalDate_whenCalendarTimeZoneMatches(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        LocalDate localDate = this.converter.convert(calendar, LocalDate.class, createConvertOptions(null, zoneId));
        assertThat(localDate).isEqualTo(expected);
    }

    @Test
    void testCalendarToLocalDate_whenCalendarTimeZoneDoesNotMatchTarget_convertsTimeCorrectly() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK));
        calendar.setTimeInMillis(1687622249729L);

        LocalDate localDate = this.converter.convert(calendar, LocalDate.class, createConvertOptions(null, TOKYO));

        assertThat(localDate)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(25);
    }

    @Test
    void testCalendar_testData() {

        // Create LocalDateTime as CHICAGO TIME.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(CHICAGO));
        calendar.setTimeInMillis(1687622249729L);

        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(5);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(24);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(10);
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(57);
        assertThat(calendar.getTimeInMillis()).isEqualTo(1687622249729L);

        // Convert calendar calendar to TOKYO LocalDateTime
        LocalDateTime localDateTime = this.converter.convert(calendar, LocalDateTime.class, createConvertOptions(CHICAGO, TOKYO));

        assertThat(localDateTime)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(25)
                .hasHour(0)
                .hasMinute(57)
                .hasSecond(29)
                .hasNano(729000000);

        //  Convert Tokyo local date time to CHICAGO Calendar
        //  We don't know the source ZoneId we are trying to convert.
        Calendar actual = this.converter.convert(localDateTime, Calendar.class, createConvertOptions(TOKYO, CHICAGO));

        assertThat(actual.get(Calendar.MONTH)).isEqualTo(5);
        assertThat(actual.get(Calendar.DAY_OF_MONTH)).isEqualTo(24);
        assertThat(actual.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(actual.get(Calendar.HOUR_OF_DAY)).isEqualTo(10);
        assertThat(actual.get(Calendar.MINUTE)).isEqualTo(57);
        assertThat(actual.getTimeInMillis()).isEqualTo(1687622249729L);
    }


    @Test
    void toLong_fromLocalDate()
    {
        LocalDate localDate = LocalDate.now();
        ConverterOptions options = chicagoZone();
        Long converted = this.converter.convert(localDate, Long.class, options);
        assertThat(converted).isEqualTo(localDate.atStartOfDay(options.getZoneId()).toInstant().toEpochMilli());
    }




    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testLongToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        LocalDate localDate = this.converter.convert(epochMilli, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testZonedDateTimeToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        LocalDate localDate = this.converter.convert(epochMilli, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testInstantToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        LocalDate localDate = this.converter.convert(instant, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testDateToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Date date = new Date(epochMilli);
        LocalDate localDate = this.converter.convert(date, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testSqlDateToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        java.sql.Date date = new java.sql.Date(epochMilli);
        LocalDate localDate = this.converter.convert(date, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testTimestampToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Timestamp date = new Timestamp(epochMilli);
        LocalDate localDate = this.converter.convert(date, LocalDate.class, createConvertOptions(null, zoneId));

        assertThat(localDate).isEqualTo(expected);
    }


    private static final LocalDateTime LDT_TOKYO_1 = LocalDateTime.of(2023, 6, 25, 0, 57, 29, 729000000);
    private static final LocalDateTime LDT_PARIS_1 = LocalDateTime.of(2023, 6, 24, 17, 57, 29, 729000000);
    private static final LocalDateTime LDT_NY_1 = LocalDateTime.of(2023, 6, 24, 11, 57, 29, 729000000);
    private static final LocalDateTime LDT_LA_1 = LocalDateTime.of(2023, 6, 24, 8, 57, 29, 729000000);

    private static Stream<Arguments> localDateTimeConversion_params() {
        return Stream.of(
                Arguments.of(1687622249729L, NEW_YORK, LDT_NY_1, TOKYO, LDT_TOKYO_1),
                Arguments.of(1687622249729L, LOS_ANGELES, LDT_LA_1, PARIS, LDT_PARIS_1)
        );
    }


    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToLong(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        long milli = this.converter.convert(initial, long.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(milli).isEqualTo(epochMilli);

        LocalDateTime actual = this.converter.convert(milli, LocalDateTime.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToAtomicLong(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        AtomicLong milli = this.converter.convert(initial, AtomicLong.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(milli.longValue()).isEqualTo(epochMilli);

        LocalDateTime actual = this.converter.convert(milli, LocalDateTime.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToBigInteger(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        BigInteger milli = this.converter.convert(initial, BigInteger.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(milli.longValue()).isEqualTo(epochMilli);

        LocalDateTime actual = this.converter.convert(milli, LocalDateTime.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(actual).isEqualTo(expected);

    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToBigDecimal(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        BigDecimal milli = this.converter.convert(initial, BigDecimal.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(milli.longValue()).isEqualTo(epochMilli);

        LocalDateTime actual = this.converter.convert(milli, LocalDateTime.class, createConvertOptions(sourceZoneId, targetZoneId));
        assertThat(actual).isEqualTo(expected);
    }


    private static Stream<Arguments> testAtomicLongParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as an AtomicLong"),
                Arguments.of( "-9223372036854775809", "not parseable as an AtomicLong"),
                Arguments.of("9223372036854775808", "not parseable as an AtomicLong"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testAtomicLongParams_withIllegalArguments")
    void testAtomicLong_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, AtomicLong.class))
                .withMessageContaining(partialMessage);
    }


    private static Stream<Arguments> testStringParams() {
        return Stream.of(
                Arguments.of("-32768", "-32768"),
                Arguments.of("Hello", "Hello"),
                Arguments.of(Byte.MIN_VALUE, "-128"),
                Arguments.of(Byte.MAX_VALUE, "127"),
                Arguments.of(Short.MIN_VALUE, "-32768"),
                Arguments.of(Short.MAX_VALUE, "32767L"),
                Arguments.of(Integer.MIN_VALUE, "-2147483648L"),
                Arguments.of(Integer.MAX_VALUE, "2147483647L"),
                Arguments.of(Long.MIN_VALUE, "-9223372036854775808L"),
                Arguments.of(Long.MAX_VALUE, "9223372036854775807L"),
                Arguments.of(-128.0f, "-128"),
                Arguments.of(127.56f, "127.56"),
                Arguments.of(-128.0d, "-128"),
                Arguments.of(1.23456789d, "1.23456789"),
                Arguments.of(123456789.12345, "123456789.12345"),
                Arguments.of( new BigDecimal("9999999999999999999999999.99999999"), "9999999999999999999999999.99999999"),
                Arguments.of( new BigInteger("999999999999999999999999999999999999999999"), "999999999999999999999999999999999999999999"),
                Arguments.of( new AtomicInteger(25), "25"),
                Arguments.of( new AtomicLong(Long.MAX_VALUE), "9223372036854775807L"),
                Arguments.of(3.1415926535897932384626433e18, "3141592653589793300"),
                Arguments.of(true, "true"),
                Arguments.of(false, "false"),
                Arguments.of(Boolean.TRUE, "true"),
                Arguments.of(Boolean.FALSE, "false"),
                Arguments.of(new AtomicBoolean(true), "true"),
                Arguments.of(new AtomicBoolean(false), "false"),
                Arguments.of('J', "J"),
                Arguments.of(new BigDecimal("3.1415926535897932384626433"), "3.1415926535897932384626433"),
                Arguments.of(new BigInteger("123456789012345678901234567890"), "123456789012345678901234567890"));
    }

    @ParameterizedTest
    @MethodSource("testAtomicLongParams")
    void testStringParams(Object value, AtomicLong expectedResult)
    {
        AtomicLong converted = this.converter.convert(value, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(expectedResult.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testStringNullAndEmpty(String value) {
        String converted = this.converter.convert(value, String.class);
        assertThat(converted).isSameAs(value);
    }

    private static Stream<Arguments> testConvertStringParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of(ZoneId.systemDefault(), "Unsupported conversion"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testConvertStringParams_withIllegalArguments")
    void testConvertString_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, AtomicLong.class))
                .withMessageContaining(partialMessage);
    }

    @Test
    void testString_fromDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 8, 34, 49);

        Date date = cal.getTime();

        String converted = this.converter.convert(date, String.class);
        assertThat(converted).isEqualTo("2015-01-17T08:34:49");
    }

    @Test
    void testString_fromCalendar()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 8, 34, 49);
        assertEquals("2015-01-17T08:34:49", this.converter.convert(cal.getTime(), String.class));
        assertEquals("2015-01-17T08:34:49", this.converter.convert(cal, String.class));
    }

    @Test
    void testString_fromLocalDate()
    {
        LocalDate localDate = LocalDate.of(2015, 9, 3);
        String converted = this.converter.convert(localDate, String.class);
        assertThat(converted).isEqualTo("2015-09-03");
    }


    private static Stream<Arguments> testBigDecimalParams() {
        return Stream.of(
                Arguments.of("-45000", BigDecimal.valueOf(-45000L)),
                Arguments.of("-32768", BigDecimal.valueOf(-32768L)),
                Arguments.of("32767", BigDecimal.valueOf(32767L)),
                Arguments.of(Byte.MIN_VALUE, BigDecimal.valueOf((-128L)),
                Arguments.of(Byte.MAX_VALUE, BigDecimal.valueOf(127L)),
                Arguments.of(Short.MIN_VALUE, BigDecimal.valueOf(-32768L)),
                Arguments.of(Short.MAX_VALUE, BigDecimal.valueOf(32767L)),
                Arguments.of(Integer.MIN_VALUE, BigDecimal.valueOf(-2147483648L)),
                Arguments.of(Integer.MAX_VALUE, BigDecimal.valueOf(2147483647L)),
                Arguments.of(Long.MIN_VALUE, BigDecimal.valueOf(-9223372036854775808L)),
                Arguments.of(Long.MAX_VALUE, BigDecimal.valueOf(9223372036854775807L)),
                        Arguments.of(3.14, BigDecimal.valueOf(3.14)),
                        Arguments.of(-128.0f, BigDecimal.valueOf(-128.0f)),
                Arguments.of(127.0f, BigDecimal.valueOf(127.0f)),
                Arguments.of(-128.0d, BigDecimal.valueOf(-128.0d))),
                Arguments.of(127.0d, BigDecimal.valueOf(127.0d)),
                Arguments.of( new BigDecimal("100"), new BigDecimal("100")),
                Arguments.of( new BigInteger("8675309"), new BigDecimal("8675309")),
                Arguments.of( new BigInteger("120"), new BigDecimal("120")),
                Arguments.of( new AtomicInteger(25), new BigDecimal(25)),
                Arguments.of( new AtomicLong(100L), new BigDecimal(100))
        );
    }

    @ParameterizedTest
    @MethodSource("testBigDecimalParams")
    void testBigDecimal(Object value, BigDecimal expectedResult)
    {
        BigDecimal converted = this.converter.convert(value, BigDecimal.class);
        assertThat(converted).isEqualTo(expectedResult);
    }


    private static Stream<Arguments> testBigDecimalParams_withObjectsShouldBeSame() {
        return Stream.of(
                Arguments.of(new AtomicBoolean(true), BigDecimal.ONE),
                Arguments.of(new AtomicBoolean(false), BigDecimal.ZERO),
                Arguments.of(true, BigDecimal.ONE),
                Arguments.of(false, BigDecimal.ZERO),
                Arguments.of(Boolean.TRUE, BigDecimal.ONE),
                Arguments.of(Boolean.FALSE, BigDecimal.ZERO),
                Arguments.of("", BigDecimal.ZERO)
        );
    }
    @ParameterizedTest
    @MethodSource("testBigDecimalParams_withObjectsShouldBeSame")
    void testBigDecimal_withObjectsThatShouldBeSameAs(Object value, BigDecimal expected) {
        BigDecimal converted = this.converter.convert(value, BigDecimal.class);
        assertThat(converted).isSameAs(expected);
    }

    @Test
    void testBigDecimal_withDate() {
        Date now = new Date();
        BigDecimal bd = new BigDecimal(now.getTime());
        assertEquals(bd, this.converter.convert(now, BigDecimal.class));
    }

    @Test
    void testBigDecimal_witCalendar() {
        Calendar today = Calendar.getInstance();
        BigDecimal bd = new BigDecimal(today.getTime().getTime());
        assertEquals(bd, this.converter.convert(today, BigDecimal.class));
    }


    private static Stream<Arguments> testConvertToBigDecimalParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable"),
                Arguments.of(ZoneId.systemDefault(), "Unsupported conversion"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testConvertToBigDecimalParams_withIllegalArguments")
    void testConvertToBigDecimal_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, BigDecimal.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> testBigIntegerParams() {
        return Stream.of(
                Arguments.of("-32768", BigInteger.valueOf(-32768L)),
                Arguments.of("32767", BigInteger.valueOf(32767L)),
                Arguments.of((short)75, BigInteger.valueOf(75)),
                Arguments.of(Byte.MIN_VALUE, BigInteger.valueOf((-128L)),
                Arguments.of(Byte.MAX_VALUE, BigInteger.valueOf(127L)),
                Arguments.of(Short.MIN_VALUE, BigInteger.valueOf(-32768L)),
                Arguments.of(Short.MAX_VALUE, BigInteger.valueOf(32767L)),
                Arguments.of(Integer.MIN_VALUE, BigInteger.valueOf(-2147483648L)),
                Arguments.of(Integer.MAX_VALUE, BigInteger.valueOf(2147483647L)),
                Arguments.of(Long.MIN_VALUE, BigInteger.valueOf(-9223372036854775808L)),
                Arguments.of(Long.MAX_VALUE, BigInteger.valueOf(9223372036854775807L)),
                Arguments.of(-128.192f, BigInteger.valueOf(-128)),
                Arguments.of(127.5698f, BigInteger.valueOf(127)),
                Arguments.of(-128.0d, BigInteger.valueOf(-128))),
                Arguments.of(3.14d, BigInteger.valueOf(3)),
                Arguments.of("11.5", new BigInteger("11")),
                Arguments.of(127.0d, BigInteger.valueOf(127)),
                Arguments.of( new BigDecimal("100"), new BigInteger("100")),
                Arguments.of( new BigInteger("120"), new BigInteger("120")),
                Arguments.of( new AtomicInteger(25), BigInteger.valueOf(25)),
                Arguments.of( new AtomicLong(100L), BigInteger.valueOf(100))
        );
    }

    @ParameterizedTest
    @MethodSource("testBigIntegerParams")
    void testBigInteger(Object value, BigInteger expectedResult)
    {
        BigInteger converted = this.converter.convert(value, BigInteger.class);
        assertThat(converted).isEqualTo(expectedResult);
    }


    private static Stream<Arguments> testBigIntegerParams_withObjectsShouldBeSameAs() {
        return Stream.of(
                Arguments.of(CommonValues.INTEGER_ZERO, BigInteger.ZERO),
                Arguments.of(CommonValues.INTEGER_ONE, BigInteger.ONE),
                Arguments.of(CommonValues.LONG_ZERO, BigInteger.ZERO),
                Arguments.of(CommonValues.LONG_ONE, BigInteger.ONE),
                Arguments.of(new AtomicBoolean(true), BigInteger.ONE),
                Arguments.of(new AtomicBoolean(false), BigInteger.ZERO),
                Arguments.of(true, BigInteger.ONE),
                Arguments.of(false, BigInteger.ZERO),
                Arguments.of(Boolean.TRUE, BigInteger.ONE),
                Arguments.of(Boolean.FALSE, BigInteger.ZERO),
                Arguments.of("", BigInteger.ZERO),
                Arguments.of(BigInteger.ZERO, BigInteger.ZERO),
                Arguments.of(BigInteger.ONE, BigInteger.ONE),
               Arguments.of(BigInteger.TEN, BigInteger.TEN)
        );
    }
    @ParameterizedTest
    @MethodSource("testBigIntegerParams_withObjectsShouldBeSameAs")
    void testBigInteger_withObjectsShouldBeSameAs(Object value, BigInteger expected) {
        BigInteger converted = this.converter.convert(value, BigInteger.class);
        assertThat(converted).isSameAs(expected);
    }

    @Test
    void testBigInteger_withDate() {
        Date now = new Date();
        BigInteger bd = BigInteger.valueOf(now.getTime());
        assertEquals(bd, this.converter.convert(now, BigInteger.class));
    }

    @Test
    void testBigInteger_withCalendar() {
        Calendar today = Calendar.getInstance();
        BigInteger bd = BigInteger.valueOf(today.getTime().getTime());
        assertEquals(bd, this.converter.convert(today, BigInteger.class));
    }

    private static Stream<Arguments> testConvertToBigIntegerParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable"),
                Arguments.of(ZoneId.systemDefault(), "Unsupported conversion"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testConvertToBigIntegerParams_withIllegalArguments")
    void testConvertToBigInteger_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, BigInteger.class))
                .withMessageContaining(partialMessage);
    }


    @ParameterizedTest
    @MethodSource("toIntParams")
    void testAtomicInteger(Object value, int expectedResult)
    {
        AtomicInteger converted = this.converter.convert(value, AtomicInteger.class);
        assertThat(converted.get()).isEqualTo(new AtomicInteger(expectedResult).get());
    }

    @Test
    void testAtomicInteger_withEmptyString() {
        AtomicInteger converted = this.converter.convert("", AtomicInteger.class);
        //TODO:  Do we want nullable types to default to zero
        assertThat(converted.get()).isEqualTo(0);
    }

    private static Stream<Arguments> testAtomicIntegerParams_withBooleanTypes() {
        return Stream.of(
                Arguments.of(new AtomicBoolean(true), new AtomicInteger(1)),
                Arguments.of(new AtomicBoolean(false), new AtomicInteger(0)),
                Arguments.of(true,  new AtomicInteger(1)),
                Arguments.of(false, new AtomicInteger(0)),
                Arguments.of(Boolean.TRUE,  new AtomicInteger(1)),
                Arguments.of(Boolean.FALSE, new AtomicInteger(0))
        );
    }
    @ParameterizedTest
    @MethodSource("testAtomicIntegerParams_withBooleanTypes")
    void testAtomicInteger_withBooleanTypes(Object value, AtomicInteger expected) {
        AtomicInteger converted = this.converter.convert(value, AtomicInteger.class);
        assertThat(converted.get()).isEqualTo(expected.get());
    }

    private static Stream<Arguments> testAtomicInteger_withIllegalArguments_params() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable"),
                Arguments.of(ZoneId.systemDefault(), "Unsupported conversion"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testAtomicInteger_withIllegalArguments_params")
    void testAtomicInteger_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, BigInteger.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> epochMilli_exampleOneParams() {
        return Stream.of(
                Arguments.of(1705601070270L),
                Arguments.of( new Long(1705601070270L)),
                Arguments.of( new AtomicLong(1705601070270L)),
                Arguments.of( 1705601070270.798659898d),
                Arguments.of( BigInteger.valueOf(1705601070270L)),
                Arguments.of( BigDecimal.valueOf(1705601070270L)),
                Arguments.of("1705601070270")
        );
    }

    @ParameterizedTest
    @MethodSource("epochMilli_exampleOneParams")
    void testDate(Object value) {
        Date expected = new Date(1705601070270L);
        Date converted = this.converter.convert(value, Date.class);
        assertThat(converted).isEqualTo(expected);
    }

    // float doesn't have enough significant digits to accurately represent today's dates
    private static Stream<Arguments> conversionsWithPrecisionLoss_primitiveParams() {
        return Stream.of(
                // double ->
                Arguments.of( 1705601070270.89765d, float.class, 1705601010100f),
                Arguments.of( 1705601070270.89765d, Float.class, 1705601010100f),
                Arguments.of( 1705601070270.89765d, byte.class, (byte)-1),
                Arguments.of( 1705601070270.89765d, Byte.class, (byte)-1),
                Arguments.of( 1705601070270.89765d, short.class, (short)-1),
                Arguments.of( 1705601070270.89765d, Short.class, (short)-1),
                Arguments.of( 1705601070270.89765d, int.class, 2147483647),
                Arguments.of( 1705601070270.89765d, Integer.class, 2147483647),
                Arguments.of( 1705601070270.89765d, long.class, 1705601070270L),
                Arguments.of( 1705601070270.89765d, Long.class, 1705601070270L),

                // float ->
                Arguments.of( 65679.6f, byte.class, (byte)-113),
                Arguments.of( 65679.6f, Byte.class, (byte)-113),
                Arguments.of( 65679.6f, short.class, (short)143),
                Arguments.of( 65679.6f, Short.class, (short)143),
                Arguments.of( 65679.6f, int.class, 65679),
                Arguments.of( 65679.6f, Integer.class, 65679),
                Arguments.of( 65679.6f, long.class, 65679L),
                Arguments.of( 65679.6f, Long.class, 65679L),

                // long ->
                Arguments.of( new BigInteger("92233720368547738079919"), double.class, 92233720368547740000000.0d),
                Arguments.of( new BigInteger("92233720368547738079919"), Double.class, 92233720368547740000000.0d),
                Arguments.of( new BigInteger("92233720368547738079919"), float.class, 92233720368547760000000f),
                Arguments.of( new BigInteger("92233720368547738079919"), Float.class, 92233720368547760000000f),
                Arguments.of( new BigInteger("92233720368547738079919"), Byte.class, (byte)-81),
                Arguments.of( new BigInteger("92233720368547738079919"), byte.class, (byte)-81),
                Arguments.of( new BigInteger("92233720368547738079919"), short.class, (short)-11601),
                Arguments.of( new BigInteger("92233720368547738079919"), Short.class, (short)-11601),
                Arguments.of( new BigInteger("92233720368547738079919"), int.class, -20000081),
                Arguments.of( new BigInteger("92233720368547738079919"), Integer.class, -20000081),
                Arguments.of( new BigInteger("92233720368547738079919"), long.class, -20000081L),
                Arguments.of( new BigInteger("92233720368547738079919"), Long.class, -20000081L),


                // long ->
                Arguments.of( 9223372036854773807L, double.class, 9223372036854773800.0d),
                Arguments.of( 9223372036854773807L, Double.class, 9223372036854773800.0d),
                Arguments.of( 9223372036854773807L, float.class, 9223372036854776000.0f),
                Arguments.of( 9223372036854773807L, Float.class, 9223372036854776000.0f),
                Arguments.of( 9223372036854773807L, Byte.class, (byte)47),
                Arguments.of( 9223372036854773807L, byte.class, (byte)47),
                Arguments.of( 9223372036854773807L, short.class, (short)-2001),
                Arguments.of( 9223372036854773807L, Short.class, (short)-2001),
                Arguments.of( 9223372036854773807L, int.class, -2001),
                Arguments.of( 9223372036854773807L, Integer.class, -2001),

                // AtomicLong ->
                Arguments.of( new AtomicLong(9223372036854773807L), double.class, 9223372036854773800.0d),
                Arguments.of( new AtomicLong(9223372036854773807L), Double.class, 9223372036854773800.0d),
                Arguments.of( new AtomicLong(9223372036854773807L), float.class, 9223372036854776000.0f),
                Arguments.of( new AtomicLong(9223372036854773807L), Float.class, 9223372036854776000.0f),
                Arguments.of( new AtomicLong(9223372036854773807L), Byte.class, (byte)47),
                Arguments.of( new AtomicLong(9223372036854773807L), byte.class, (byte)47),
                Arguments.of( new AtomicLong(9223372036854773807L), short.class, (short)-2001),
                Arguments.of( new AtomicLong(9223372036854773807L), Short.class, (short)-2001),
                Arguments.of( new AtomicLong(9223372036854773807L), int.class, -2001),
                Arguments.of( new AtomicLong(9223372036854773807L), Integer.class, -2001),

                Arguments.of( 2147473647, float.class, 2147473664.0f),
                Arguments.of( 2147473647, Float.class, 2147473664.0f),
                Arguments.of( 2147473647, Byte.class, (byte)-17),
                Arguments.of( 2147473647, byte.class, (byte)-17),
                Arguments.of( 2147473647, short.class, (short)-10001),
                Arguments.of( 2147473647, Short.class, (short)-10001),

                // AtomicInteger ->
                Arguments.of( new AtomicInteger(2147473647), float.class, 2147473664.0f),
                Arguments.of( new AtomicInteger(2147473647), Float.class, 2147473664.0f),
                Arguments.of( new AtomicInteger(2147473647), Byte.class, (byte)-17),
                Arguments.of( new AtomicInteger(2147473647), byte.class, (byte)-17),
                Arguments.of( new AtomicInteger(2147473647), short.class, (short)-10001),
                Arguments.of( new AtomicInteger(2147473647), Short.class, (short)-10001),

                // short ->
                Arguments.of( (short)62212, Byte.class, (byte)4),
                Arguments.of( (short)62212, byte.class, (byte)4)
        );
    }

    @ParameterizedTest
    @MethodSource("conversionsWithPrecisionLoss_primitiveParams")
    void conversionsWithPrecisionLoss_primitives(Object value, Class c, Object expected) {
        Object converted = this.converter.convert(value, c);
        assertThat(converted).isEqualTo(expected);
    }


    // float doesn't have enough significant digits to accurately represent today's dates
    private static Stream<Arguments> conversionsWithPrecisionLoss_toAtomicIntegerParams() {
        return Stream.of(
                Arguments.of( 1705601070270.89765d, new AtomicInteger(2147483647)),
                Arguments.of( 65679.6f, new AtomicInteger(65679)),
                Arguments.of( 9223372036854773807L, new AtomicInteger(-2001)),
                Arguments.of( new AtomicLong(9223372036854773807L), new AtomicInteger(-2001))
        );
    }

    @ParameterizedTest
    @MethodSource("conversionsWithPrecisionLoss_toAtomicIntegerParams")
    void conversionsWithPrecisionLoss_toAtomicInteger(Object value, AtomicInteger expected) {
        AtomicInteger converted = this.converter.convert(value, AtomicInteger.class);
        assertThat(converted.get()).isEqualTo(expected.get());
    }

    private static Stream<Arguments> conversionsWithPrecisionLoss_toAtomicLongParams() {
        return Stream.of(
                // double ->
                Arguments.of( 1705601070270.89765d, new AtomicLong(1705601070270L)),
                Arguments.of( 65679.6f, new AtomicLong(65679L))
        );
    }

    @ParameterizedTest
    @MethodSource("conversionsWithPrecisionLoss_toAtomicLongParams")
    void conversionsWithPrecisionLoss_toAtomicLong(Object value, AtomicLong expected) {
        AtomicLong converted = this.converter.convert(value, AtomicLong.class);
        assertThat(converted.get()).isEqualTo(expected.get());
    }




    //    I think parsing a string double into date is gone now.            Arguments.of("11.5", new Date(11)),
    private static Stream<Arguments> extremeDateParams() {
        return Stream.of(
                Arguments.of((short)75, new Date(75)),
                Arguments.of(Byte.MIN_VALUE, new Date(Byte.MIN_VALUE)),
                Arguments.of(Byte.MAX_VALUE, new Date(Byte.MAX_VALUE)),
                Arguments.of(Short.MIN_VALUE, new Date(Short.MIN_VALUE)),
                Arguments.of(Short.MAX_VALUE, new Date(Short.MAX_VALUE)),
                Arguments.of(Integer.MIN_VALUE, new Date(Integer.MIN_VALUE)),
                Arguments.of(Integer.MAX_VALUE, new Date(Integer.MAX_VALUE)),
                Arguments.of(Long.MIN_VALUE,new Date(Long.MIN_VALUE)),
                Arguments.of(Long.MAX_VALUE, new Date(Long.MAX_VALUE)),
                Arguments.of(127.0d, new Date(127)),
                Arguments.of( new AtomicInteger(25), new Date(25))
        );
    }

    @ParameterizedTest
    @MethodSource("extremeDateParams")
    void testExtremeDateParams(Object value, Date expected) {
        Date converted = this.converter.convert(value, Date.class);
        assertThat(converted).isEqualTo(expected);
    }

    @Test
    void testDateFromOthers()
    {
        // Date to Date
        Date utilNow = new Date();
        Date coerced = this.converter.convert(utilNow, Date.class);
        assertEquals(utilNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);
        assert coerced != utilNow;

        // Date to java.sql.Date
        java.sql.Date sqlCoerced = this.converter.convert(utilNow, java.sql.Date.class);
        assertEquals(utilNow, sqlCoerced);

        // java.sql.Date to java.sql.Date
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());
        sqlCoerced = this.converter.convert(sqlNow, java.sql.Date.class);
        assertEquals(sqlNow, sqlCoerced);

        // java.sql.Date to Date
        coerced = this.converter.convert(sqlNow, Date.class);
        assertEquals(sqlNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);

        // Date to Timestamp
        Timestamp tstamp = this.converter.convert(utilNow, Timestamp.class);
        assertEquals(utilNow, tstamp);

        // Timestamp to Date
        Date someDate = this.converter.convert(tstamp, Date.class);
        assertEquals(utilNow, tstamp);
        assertFalse(someDate instanceof Timestamp);

        // java.sql.Date to Timestamp
        tstamp = this.converter.convert(sqlCoerced, Timestamp.class);
        assertEquals(sqlCoerced, tstamp);

        // Timestamp to java.sql.Date
        java.sql.Date someDate1 = this.converter.convert(tstamp, java.sql.Date.class);
        assertEquals(someDate1, utilNow);

        // String to Date
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);
        Date date = this.converter.convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // String to java.sql.Date
        java.sql.Date sqlDate = this.converter.convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals(cal.getTime(), sqlDate);
        assert sqlDate != null;

        // Calendar to Date
        date = this.converter.convert(cal, Date.class);
        assertEquals(date, cal.getTime());
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // Calendar to java.sql.Date
        sqlDate = this.converter.convert(cal, java.sql.Date.class);
        assertEquals(sqlDate, cal.getTime());
        assert sqlDate != null;

        // long to Date
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);
        Date converted = this.converter.convert(now, Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        Date sqlConverted = this.converter.convert(now, java.sql.Date.class);
        assertEquals(dateNow, sqlConverted);
        assert sqlConverted != null;

        // AtomicLong to Date
        now = System.currentTimeMillis();
        dateNow = new Date(now);
        converted = this.converter.convert(new AtomicLong(now), Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        dateNow = new java.sql.Date(now);
        sqlConverted = this.converter.convert(new AtomicLong(now), java.sql.Date.class);
        assert sqlConverted != null;
        assertEquals(dateNow, sqlConverted);

        // BigInteger to java.sql.Date
        BigInteger bigInt = new BigInteger("" + now);
        sqlDate = this.converter.convert(bigInt, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigDecimal to java.sql.Date
        BigDecimal bigDec = new BigDecimal(now);
        sqlDate = this.converter.convert(bigDec, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigInteger to Timestamp
        bigInt = new BigInteger("" + now);
        tstamp = this.converter.convert(bigInt, Timestamp.class);
        assert tstamp.getTime() == now;

        // BigDecimal to TimeStamp
        bigDec = new BigDecimal(now);
        tstamp = this.converter.convert(bigDec, Timestamp.class);
        assert tstamp.getTime() == now;

        // Invalid source type for Date
        try
        {
            this.converter.convert(TimeZone.getDefault(), Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        // Invalid source type for java.sql.Date
        try
        {
            this.converter.convert(TimeZone.getDefault(), java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        // Invalid source date for Date
        try
        {
            this.converter.convert("2015/01/33", Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("Day must be between 1 and 31 inclusive, date: 2015/01/33"));
        }

        // Invalid source date for java.sql.Date
        try
        {
            this.converter.convert("2015/01/33", java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("day must be between 1 and 31"));
        }
    }

    @Test
    void testBogusSqlDate2()
    {
        assertThatThrownBy(() -> this.converter.convert(true, java.sql.Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Boolean (true)] target type 'java.sql.Date'");
    }

    @Test
    void testCalendar()
    {
        // Date to Calendar
        Date now = new Date();
        Calendar calendar = this.converter.convert(new Date(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // SqlDate to Calendar
        java.sql.Date sqlDate = this.converter.convert(now, java.sql.Date.class);
        calendar = this.converter.convert(sqlDate, Calendar.class);
        assertEquals(calendar.getTime(), sqlDate);

        // Timestamp to Calendar
        Timestamp timestamp = this.converter.convert(now, Timestamp.class);
        calendar = this.converter.convert(timestamp, Calendar.class);
        assertEquals(calendar.getTime(), timestamp);

        // Long to Calendar
        calendar = this.converter.convert(now.getTime(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // AtomicLong to Calendar
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        calendar = this.converter.convert(atomicLong, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // String to Calendar
        String strDate = this.converter.convert(now, String.class);
        calendar = this.converter.convert(strDate, Calendar.class);
        String strDate2 = this.converter.convert(calendar, String.class);
        assertEquals(strDate, strDate2);

        // BigInteger to Calendar
        BigInteger bigInt = new BigInteger("" + now.getTime());
        calendar = this.converter.convert(bigInt, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // BigDecimal to Calendar
        BigDecimal bigDec = new BigDecimal(now.getTime());
        calendar = this.converter.convert(bigDec, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // Other direction --> Calendar to other date types

        // Calendar to Date
        calendar = this.converter.convert(now, Calendar.class);
        Date date = this.converter.convert(calendar, Date.class);
        assertEquals(calendar.getTime(), date);

        // Calendar to SqlDate
        sqlDate = this.converter.convert(calendar, java.sql.Date.class);
        assertEquals(calendar.getTime().getTime(), sqlDate.getTime());

        // Calendar to Timestamp
        timestamp = this.converter.convert(calendar, Timestamp.class);
        assertEquals(calendar.getTime().getTime(), timestamp.getTime());

        // Calendar to Long
        long tnow = this.converter.convert(calendar, long.class);
        assertEquals(calendar.getTime().getTime(), tnow);

        // Calendar to AtomicLong
        atomicLong = this.converter.convert(calendar, AtomicLong.class);
        assertEquals(calendar.getTime().getTime(), atomicLong.get());

        // Calendar to String
        strDate = this.converter.convert(calendar, String.class);
        strDate2 = this.converter.convert(now, String.class);
        assertEquals(strDate, strDate2);

        // Calendar to BigInteger
        bigInt = this.converter.convert(calendar, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // Calendar to BigDecimal
        bigDec = this.converter.convert(calendar, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());
    }



    @Test
    void testStringToLocalDate()
    {
        String testDate = "1705769204092";
        LocalDate ld = this.converter.convert(testDate, LocalDate.class);
        assert ld.getYear() == 2024;
        assert ld.getMonthValue() == 1;
        assert ld.getDayOfMonth() == 20;

        testDate = "2023-12-23";
        ld = this.converter.convert(testDate, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        testDate = "2023/12/23";
        ld = this.converter.convert(testDate, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        testDate = "12/23/2023";
        ld = this.converter.convert(testDate, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }

    @Test
    void testStringOnMapToLocalDate()
    {
        Map<String, Object> map = new HashMap<>();
        String testDate = "1705769204092";
        map.put("value", testDate);
        LocalDate ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2024;
        assert ld.getMonthValue() == 1;
        assert ld.getDayOfMonth() == 20;

        testDate = "2023-12-23";
        map.put("value", testDate);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        testDate = "2023/12/23";
        map.put("value", testDate);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        testDate = "12/23/2023";
        map.put("value", testDate);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }

    @Test
    void testStringKeysOnMapToLocalDate()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("day", "23");
        map.put("month", "12");
        map.put("year", "2023");
        LocalDate ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        map.put("day", 23);
        map.put("month", 12);
        map.put("year", 2023);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }


    private static Stream<Arguments> identityParams() {
        return Stream.of(
                Arguments.of(9L, Long.class),
                Arguments.of((short)10, Short.class),
                Arguments.of("foo", String.class),
                Arguments.of(LocalDate.now(), LocalDate.class),
                Arguments.of(LocalDateTime.now(), LocalDateTime.class)
        );
    }
    @ParameterizedTest
    @MethodSource("identityParams")
    void testConversions_whenClassTypeMatchesObjectType_returnsItself(Object o, Class<?> c) {
        Object converted = this.converter.convert(o, c);
        assertThat(converted).isSameAs(o);
    }

    private static Stream<Arguments> nonIdentityParams() {
        return Stream.of(
                Arguments.of(new Date(), Date.class),
                Arguments.of(new java.sql.Date(System.currentTimeMillis()), java.sql.Date.class),
                Arguments.of(new Timestamp(System.currentTimeMillis()), Timestamp.class),
                Arguments.of(Calendar.getInstance(), Calendar.class)
        );
    }

    @ParameterizedTest
    @MethodSource("nonIdentityParams")
    void testConversions_whenClassTypeMatchesObjectType_stillCreatesNewObject(Object o, Class<?> c) {
        Object converted = this.converter.convert(o, c);
        assertThat(converted).isNotSameAs(o);
    }

    @Test
    void testLocalDateTimeToOthers()
    {
        // String to LocalDateTime
//        String strDate = this.converter.convert(now, String.class);
//        localDateTime = this.converter.convert(strDate, LocalDateTime.class);
//        String strDate2 = this.converter.convert(localDateTime, String.class);
//        assert strDate.startsWith(strDate2);
//
//        // Other direction --> LocalDateTime to other date types
//
//        // LocalDateTime to Date
//        localDateTime = this.converter.convert(now, LocalDateTime.class);
//        Date date = this.converter.convert(localDateTime, Date.class);
//        assertEquals(localDateTimeToMillis(localDateTime), date.getTime());
//
//        // LocalDateTime to SqlDate
//        sqlDate = this.converter.convert(localDateTime, java.sql.Date.class);
//        assertEquals(localDateTimeToMillis(localDateTime), sqlDate.getTime());
//
//        // LocalDateTime to Timestamp
//        timestamp = this.converter.convert(localDateTime, Timestamp.class);
//        assertEquals(localDateTimeToMillis(localDateTime), timestamp.getTime());
//
//        // LocalDateTime to Long
//        long tnow = this.converter.convert(localDateTime, long.class);
//        assertEquals(localDateTimeToMillis(localDateTime), tnow);
//
//        // LocalDateTime to AtomicLong
//        atomicLong = this.converter.convert(localDateTime, AtomicLong.class);
//        assertEquals(localDateTimeToMillis(localDateTime), atomicLong.get());
//
//        // LocalDateTime to String
//        strDate = this.converter.convert(localDateTime, String.class);
//        strDate2 = this.converter.convert(now, String.class);
//        assert strDate2.startsWith(strDate);
//
//        // LocalDateTime to BigInteger
//        bigInt = this.converter.convert(localDateTime, BigInteger.class);
//        assertEquals(now.getTime(), bigInt.longValue());
//
//        // LocalDateTime to BigDecimal
//        bigDec = this.converter.convert(localDateTime, BigDecimal.class);
//        assertEquals(now.getTime(), bigDec.longValue());
//
//        // Error handling
//        try
//        {
//            this.converter.convert("2020-12-40", LocalDateTime.class);
//            fail();
//        }
//        catch (IllegalArgumentException e)
//        {
//            TestUtil.assertContainsIgnoreCase(e.getMessage(), "day must be between 1 and 31");
//        }
//
//        assert this.converter.convert(null, LocalDateTime.class) == null;
    }



    @Test
    void testDateErrorHandlingBadInput()
    {
        assertNull(this.converter.convert(" ", java.util.Date.class));
        assertNull(this.converter.convert("", java.util.Date.class));
        assertNull(this.converter.convert(null, java.util.Date.class));

        assertNull(this.converter.convert(" ", Date.class));
        assertNull(this.converter.convert("", Date.class));
        assertNull(this.converter.convert(null, Date.class));

        assertNull(this.converter.convert(" ", java.sql.Date.class));
        assertNull(this.converter.convert("", java.sql.Date.class));
        assertNull(this.converter.convert(null, java.sql.Date.class));

        assertNull(this.converter.convert(" ", java.sql.Date.class));
        assertNull(this.converter.convert("", java.sql.Date.class));
        assertNull(this.converter.convert(null, java.sql.Date.class));

        assertNull(this.converter.convert(" ", java.sql.Timestamp.class));
        assertNull(this.converter.convert("", java.sql.Timestamp.class));
        assertNull(this.converter.convert(null, java.sql.Timestamp.class));

        assertNull(this.converter.convert(" ", Timestamp.class));
        assertNull(this.converter.convert("", Timestamp.class));
        assertNull(this.converter.convert(null, Timestamp.class));
    }

    @Test
    void testTimestamp()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        assertEquals(now, this.converter.convert(now, Timestamp.class));
        assert this.converter.convert(now, Timestamp.class) instanceof Timestamp;

        Timestamp christmas = this.converter.convert("2015/12/25", Timestamp.class);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2015, 11, 25);
        assert christmas.getTime() == c.getTime().getTime();

        Timestamp christmas2 = this.converter.convert(c, Timestamp.class);

        assertEquals(christmas, christmas2);
        assertEquals(christmas2, this.converter.convert(christmas.getTime(), Timestamp.class));

        AtomicLong al = new AtomicLong(christmas.getTime());
        assertEquals(christmas2, this.converter.convert(al, Timestamp.class));

        ZonedDateTime zdt = ZonedDateTime.of(2020, 8, 30, 13, 11, 17, 0, ZoneId.systemDefault());
        Timestamp alexaBirthday = this.converter.convert(zdt, Timestamp.class);
        assert alexaBirthday.getTime() == zonedDateTimeToMillis(zdt);
        try
        {
            this.converter.convert(Boolean.TRUE, Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unsupported conversion, source type [boolean");
        }

        try
        {
            this.converter.convert("123dhksdk", Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unable to parse: 123");
        }
    }

    @Test
    void testFloat()
    {
        assert -3.14f == this.converter.convert(-3.14f, float.class);
        assert -3.14f == this.converter.convert(-3.14f, Float.class);
        assert -3.14f == this.converter.convert("-3.14", float.class);
        assert -3.14f == this.converter.convert("-3.14", Float.class);
        assert -3.14f == this.converter.convert(-3.14d, float.class);
        assert -3.14f == this.converter.convert(-3.14d, Float.class);
        assert 1.0f == this.converter.convert(true, float.class);
        assert 1.0f == this.converter.convert(true, Float.class);
        assert 0.0f == this.converter.convert(false, float.class);
        assert 0.0f == this.converter.convert(false, Float.class);

        assert 0.0f == this.converter.convert(new AtomicInteger(0), Float.class);
        assert 0.0f == this.converter.convert(new AtomicLong(0), Float.class);
        assert 0.0f == this.converter.convert(new AtomicBoolean(false), Float.class);
        assert 1.0f == this.converter.convert(new AtomicBoolean(true), Float.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45.6badNumber", Float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("45.6badnumber"));
        }
    }


    private static Stream<Arguments> testDoubleParams() {
        return Stream.of(
                Arguments.of("-32768", -32768),
                Arguments.of("-45000", -45000),
                Arguments.of("32767", 32767),
                Arguments.of(new BigInteger("8675309"), 8675309),
                Arguments.of(Byte.MIN_VALUE,-128),
                Arguments.of(Byte.MAX_VALUE, 127),
                Arguments.of(Short.MIN_VALUE, -32768),
                Arguments.of(Short.MAX_VALUE, 32767),
                Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE),
                Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE),
                Arguments.of(-128L, -128d),
                Arguments.of(127L, 127d),
                Arguments.of(3.14, 3.14d),
                Arguments.of(3.14159d, 3.14159d),
                Arguments.of(-128.0f, -128d),
                Arguments.of(127.0f, 127d),
                Arguments.of(-128.0d, -128d),
                Arguments.of(127.0d, 127d),
                Arguments.of( new BigDecimal("100"),100),
                Arguments.of( new BigInteger("120"), 120),
                Arguments.of( new AtomicInteger(75), 75),
                Arguments.of( new AtomicInteger(1), 1),
                Arguments.of( new AtomicInteger(0), 0),
                Arguments.of( new AtomicLong(Integer.MAX_VALUE), Integer.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("testDoubleParams")
    void testDouble(Object value, double expectedResult)
    {
        double converted = this.converter.convert(value, double.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("testDoubleParams")
    void testDouble_ObjectType(Object value, double expectedResult)
    {
        Double converted = this.converter.convert(value, Double.class);
        assertThat(converted).isEqualTo(Double.valueOf(expectedResult));
    }

    @Test
    void testDouble()
    {
        assert -3.14d == this.converter.convert(-3.14d, double.class);
        assert -3.14d == this.converter.convert(-3.14d, Double.class);
        assert -3.14d == this.converter.convert("-3.14", double.class);
        assert -3.14d == this.converter.convert("-3.14", Double.class);
        assert -3.14d == this.converter.convert(new BigDecimal("-3.14"), double.class);
        assert -3.14d == this.converter.convert(new BigDecimal("-3.14"), Double.class);
        assert 1.0d == this.converter.convert(true, double.class);
        assert 1.0d == this.converter.convert(true, Double.class);
        assert 0.0d == this.converter.convert(false, double.class);
        assert 0.0d == this.converter.convert(false, Double.class);

        assert 0.0d == this.converter.convert(new AtomicInteger(0), double.class);
        assert 0.0d == this.converter.convert(new AtomicLong(0), double.class);
        assert 0.0d == this.converter.convert(new AtomicBoolean(false), Double.class);
        assert 1.0d == this.converter.convert(new AtomicBoolean(true), Double.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45.6badNumber", Double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("45.6badnumber"));
        }
    }

    @Test
    void testBoolean()
    {
        /**
         *
         *         assertEquals(converter.convert(new BigInteger("314159"), Boolean.class), true);
         */
        assertEquals(true, this.converter.convert(-3.14d, boolean.class));
        assertEquals(false, this.converter.convert(0.0d, boolean.class));
        assertEquals(true, this.converter.convert(-3.14f, Boolean.class));
        assertEquals(false, this.converter.convert(0.0f, Boolean.class));

        assertEquals(false, this.converter.convert(new AtomicInteger(0), boolean.class));
        assertEquals(false, this.converter.convert(new AtomicLong(0), boolean.class));
        assertEquals(false, this.converter.convert(new AtomicBoolean(false), Boolean.class));
        assertEquals(true, this.converter.convert(new AtomicBoolean(true), Boolean.class));

        assertEquals(true, this.converter.convert("TRue", Boolean.class));
        assertEquals(true, this.converter.convert("true", Boolean.class));
        assertEquals(false, this.converter.convert("fALse", Boolean.class));
        assertEquals(false, this.converter.convert("false", Boolean.class));
        assertEquals(false, this.converter.convert("john", Boolean.class));

        assertEquals(true, this.converter.convert(true, Boolean.class));
        assertEquals(true, this.converter.convert(Boolean.TRUE, Boolean.class));
        assertEquals(false, this.converter.convert(false, Boolean.class));
        assertEquals(false, this.converter.convert(Boolean.FALSE, Boolean.class));

        try
        {
            this.converter.convert(new Date(), Boolean.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [date"));
        }
    }

    @Test
    void testAtomicBoolean()
    {
        assert (this.converter.convert(-3.14d, AtomicBoolean.class)).get();
        assert !(this.converter.convert(0.0d, AtomicBoolean.class)).get();
        assert (this.converter.convert(-3.14f, AtomicBoolean.class)).get();
        assert !(this.converter.convert(0.0f, AtomicBoolean.class)).get();

        assert !(this.converter.convert(new AtomicInteger(0), AtomicBoolean.class)).get();
        assert !(this.converter.convert(new AtomicLong(0), AtomicBoolean.class)).get();
        assert !(this.converter.convert(new AtomicBoolean(false), AtomicBoolean.class)).get();
        assert (this.converter.convert(new AtomicBoolean(true), AtomicBoolean.class)).get();

        assert (this.converter.convert("TRue", AtomicBoolean.class)).get();
        assert !(this.converter.convert("fALse", AtomicBoolean.class)).get();
        assert !(this.converter.convert("john", AtomicBoolean.class)).get();

        assert (this.converter.convert(true, AtomicBoolean.class)).get();
        assert (this.converter.convert(Boolean.TRUE, AtomicBoolean.class)).get();
        assert !(this.converter.convert(false, AtomicBoolean.class)).get();
        assert !(this.converter.convert(Boolean.FALSE, AtomicBoolean.class)).get();

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = this.converter.convert(b1, AtomicBoolean.class);
        assert b1 != b2; // ensure that it returns a different but equivalent instance
        assert b1.get() == b2.get();

        try {
            this.converter.convert(new Date(), AtomicBoolean.class);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [date"));
        }
    }

    @Test
    void testMapToAtomicBoolean()
    {
        final Map map = new HashMap();
        map.put("value", 57);
        AtomicBoolean ab = this.converter.convert(map, AtomicBoolean.class);
        assert ab.get();

        map.clear();
        map.put("value", "");
        ab = this.converter.convert(map, AtomicBoolean.class);
        assertFalse(ab.get());

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicBoolean.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicBoolean.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToAtomicInteger()
    {
        final Map map = new HashMap();
        map.put("value", 58);
        AtomicInteger ai = this.converter.convert(map, AtomicInteger.class);
        assert 58 == ai.get();

        map.clear();
        map.put("value", "");
        ai = this.converter.convert(map, AtomicInteger.class);
        assertEquals(new AtomicInteger(0).get(), ai.get());

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicInteger.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicInteger.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToAtomicLong()
    {
        final Map map = new HashMap();
        map.put("value", 58);
        AtomicLong al = this.converter.convert(map, AtomicLong.class);
        assert 58 == al.get();

        map.clear();
        map.put("value", "");
        al = this.converter.convert(map, AtomicLong.class);
        assert 0L == al.longValue();

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicLong.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicLong.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToCalendar()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", new Date(now));
        Calendar cal = this.converter.convert(map, Calendar.class);
        assert now == cal.getTimeInMillis();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Calendar.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time, zone], or '_v' or 'value'");
    }

    @Test
    void testMapToCalendarWithTimeZone()
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(now);

        final Map map = new HashMap();
        map.put("time", cal.getTimeInMillis());
        map.put("zone", cal.getTimeZone().getID());

        Calendar newCal = this.converter.convert(map, Calendar.class);
        assert cal.equals(newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToCalendarWithTimeNoZone()
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTimeInMillis(now);

        final Map map = new HashMap();
        map.put("time", cal.getTimeInMillis());

        Calendar newCal = this.converter.convert(map, Calendar.class);
        assert cal.equals(newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToGregCalendar()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", new Date(now));
        GregorianCalendar cal = this.converter.convert(map, GregorianCalendar.class);
        assert now == cal.getTimeInMillis();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, GregorianCalendar.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, GregorianCalendar.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, GregorianCalendar.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To convert from Map to Calendar, the map must include keys: [time, zone], or '_v' or 'value'");
    }

    @Test
    void testMapToDate() {

        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        Date date = this.converter.convert(map, Date.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Date.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Date.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time], or '_v' or 'value'");
    }

    @Test
    void testMapToSqlDate()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        java.sql.Date date = this.converter.convert(map, java.sql.Date.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, java.sql.Date.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, java.sql.Date.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, java.sql.Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("map must include keys");
    }

    @Test
    void testMapToTimestamp()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        Timestamp date = this.converter.convert(map, Timestamp.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Timestamp.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Timestamp.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Timestamp.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time, nanos], or '_v' or 'value'");
    }

    @Test
    void testMapToLocalDate()
    {
        LocalDate today = LocalDate.now();
        final Map map = new HashMap();
        map.put("value", today);
        LocalDate date = this.converter.convert(map, LocalDate.class);
        assert date.equals(today);

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, LocalDate.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, LocalDate.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, LocalDate.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to LocalDate, the map must include keys: [year, month, day], or '_v' or 'value'");
    }

    @Test
    void testMapToLocalDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        LocalDateTime ld = this.converter.convert(map, LocalDateTime.class);
        assert ld.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() == now;

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, LocalDateTime.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, LocalDateTime.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, LocalDateTime.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to LocalDateTime, the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToZonedDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        ZonedDateTime zd = this.converter.convert(map, ZonedDateTime.class);
        assert zd.toInstant().toEpochMilli() == now;

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, ZonedDateTime.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, ZonedDateTime.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to ZonedDateTime, the map must include keys: '_v' or 'value'");

    }

    @Test
    void testUnsupportedType()
    {
        try
        {
            this.converter.convert("Lamb", TimeZone.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [string"));
        }
    }



    private static Stream<Arguments> classesThatReturnNull_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(Class.class),
                Arguments.of(String.class),
                Arguments.of(AtomicLong.class),
                Arguments.of(AtomicInteger.class),
                Arguments.of(AtomicBoolean.class),
                Arguments.of(BigDecimal.class),
                Arguments.of(BigInteger.class),
                Arguments.of(Timestamp.class),
                Arguments.of(java.sql.Date.class),
                Arguments.of(Date.class),
                Arguments.of(Character.class),
                Arguments.of(Double.class),
                Arguments.of(Float.class),
                Arguments.of(Long.class),
                Arguments.of(Short.class),
                Arguments.of(Integer.class),
                Arguments.of(Byte.class),
                Arguments.of(Boolean.class),
                Arguments.of(Byte.class)
        );
    }

    @ParameterizedTest
    @MethodSource("classesThatReturnNull_whenConvertingFromNull")
    void testClassesThatReturnNull_whenConvertingFromNull(Class c)
    {
        assertThat(this.converter.convert(null, c)).isNull();
    }

    private static Stream<Arguments> classesThatReturnZero_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(byte.class, (byte)0),
                Arguments.of(int.class, 0),
                Arguments.of(short.class, (short)0),
                Arguments.of(char.class, (char)0),
                Arguments.of(long.class, 0L),
                Arguments.of(float.class, 0.0f),
                Arguments.of(double.class, 0.0d)
        );
    }

    @ParameterizedTest
    @MethodSource("classesThatReturnZero_whenConvertingFromNull")
    void testClassesThatReturnZero_whenConvertingFromNull(Class c, Object expected)
    {
        Object zero = this.converter.convert(null, c);
        assertThat(zero).isEqualTo(expected);
    }

    private static Stream<Arguments> classesThatReturnFalse_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(Boolean.class),
                Arguments.of(boolean.class)
        );
    }

    @Test
    void testConvertFromNullToBoolean() {
        boolean b = this.converter.convert(null, boolean.class);
        assertThat(b).isFalse();
    }

    @Test
    void testConvert2()
    {
        assert -8 == this.converter.convert("-8", byte.class);
        assert -8 == this.converter.convert("-8", int.class);
        assert -8 == this.converter.convert("-8", short.class);
        assert -8 == this.converter.convert("-8", long.class);
        assert -8.0f == this.converter.convert("-8", float.class);
        assert -8.0d == this.converter.convert("-8", double.class);
        assert 'A' == this.converter.convert(65, char.class);
        assert new BigInteger("-8").equals(this.converter.convert("-8", BigInteger.class));
        assert new BigDecimal(-8.0d).equals(this.converter.convert("-8", BigDecimal.class));
        assert this.converter.convert("true", AtomicBoolean.class).get();
        assert -8 == this.converter.convert("-8", AtomicInteger.class).get();
        assert -8L == this.converter.convert("-8", AtomicLong.class).get();
        assert "-8".equals(this.converter.convert(-8, String.class));
    }

    @Test
    void whenClassToConvertToIsNull_throwsException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.converter.convert("123", null))
                // TOTO:  in case you didn't see, No Message was coming through here and receiving NullPointerException -- changed to convention over in convert -- hopefully that's what you had in mind.
                .withMessageContaining("toType cannot be null");
    }



    @Test
    void testEnumSupport()
    {
        assertEquals("foo", this.converter.convert(foo, String.class));
        assertEquals("bar", this.converter.convert(bar, String.class));
    }

    @Test
    void testCharacterSupport()
    {
        assert 65 == this.converter.convert('A', Byte.class);
        assert 65 == this.converter.convert('A', byte.class);
        assert 65 == this.converter.convert('A', Short.class);
        assert 65 == this.converter.convert('A', short.class);
        assert 65 == this.converter.convert('A', Integer.class);
        assert 65 == this.converter.convert('A', int.class);
        assert 65 == this.converter.convert('A', Long.class);
        assert 65 == this.converter.convert('A', long.class);
        assert 65 == this.converter.convert('A', BigInteger.class).longValue();
        assert 65 == this.converter.convert('A', BigDecimal.class).longValue();

        assert 1 == this.converter.convert(true, char.class);
        assert 0 == this.converter.convert(false, char.class);
        assert 1 == this.converter.convert(new AtomicBoolean(true), char.class);
        assert 0 == this.converter.convert(new AtomicBoolean(false), char.class);
        assert 'z' == this.converter.convert('z', char.class);
        assert 0 == this.converter.convert("", char.class);
        assert 0 == this.converter.convert("", Character.class);
        assert 'A' == this.converter.convert("65", char.class);
        assert 'A' == this.converter.convert("65", Character.class);
        try
        {
            this.converter.convert("This is not a number", char.class);
            fail();
        }
        catch (IllegalArgumentException e) { }
        try
        {
            this.converter.convert(new Date(), char.class);
            fail();
        }
        catch (IllegalArgumentException e) { }

        assertThatThrownBy(() -> this.converter.convert(Long.MAX_VALUE, char.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value: 9223372036854775807 out of range to be converted to character");
    }

    @Test
    void testConvertUnknown()
    {
        try
        {
            this.converter.convert(TimeZone.getDefault(), String.class);
            fail();
        }
        catch (IllegalArgumentException e) { }
    }

    @Test
    void testLongToBigDecimal()
    {
        BigDecimal big = this.converter.convert(7L, BigDecimal.class);
        assert big instanceof BigDecimal;
        assert big.longValue() == 7L;

        big = this.converter.convert(null, BigDecimal.class);
        assert big == null;
    }


    @Test
    void testLocalDateTimeToBig()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, 8, 8, 13, 11, 1);   // 0-based for month

        BigDecimal big = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), BigDecimal.class);
        assert big.longValue() == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime();

        java.sql.Date sqlDate = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), java.sql.Date.class);
        assert sqlDate.getTime() == cal.getTime().getTime();

        Timestamp timestamp = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Timestamp.class);
        assert timestamp.getTime() == cal.getTime().getTime();

        Date date = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Date.class);
        assert date.getTime() == cal.getTime().getTime();

        Long lng = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Long.class);
        assert lng == cal.getTime().getTime();

        AtomicLong atomicLong = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), AtomicLong.class);
        assert atomicLong.get() == cal.getTime().getTime();
    }

    @Test
    void testLocalZonedDateTimeToBig()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, 8, 8, 13, 11, 1);   // 0-based for month

        BigDecimal big = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), BigDecimal.class);
        assert big.longValue() == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime();

        java.sql.Date sqlDate = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), java.sql.Date.class);
        assert sqlDate.getTime() == cal.getTime().getTime();

        Date date = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), Date.class);
        assert date.getTime() == cal.getTime().getTime();

        AtomicLong atomicLong = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), AtomicLong.class);
        assert atomicLong.get() == cal.getTime().getTime();
    }


    private static Stream<Arguments> stringToClassParams() {
        return Stream.of(
                Arguments.of("java.math.BigInteger"),
                Arguments.of("java.lang.String")
        );
    }
    @ParameterizedTest
    @MethodSource("stringToClassParams")
    void stringToClass(String className)
    {
        Class<?> c = this.converter.convert(className, Class.class);

        assertThat(c).isNotNull();
        assertThat(c.getName()).isEqualTo(className);
    }

    @Test
    void stringToClass_whenNotFound_throwsException() {
        assertThatThrownBy(() -> this.converter.convert("foo.bar.baz.Qux", Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert String 'foo.bar.baz.Qux' to class.  Class not found");
    }

    @Test
    void stringToClass_whenUnsupportedConversion_throwsException() {
        assertThatThrownBy(() -> this.converter.convert(16.0, Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Double (16.0)] target type 'Class'");
    }

    @Test
    void testClassToClass()
    {
        Class<?> clazz = this.converter.convert(ConverterTest.class, Class.class);
        assert clazz.getName() == ConverterTest.class.getName();
    }

    @Test
    void testStringToUUID()
    {
        UUID uuid = this.converter.convert("00000000-0000-0000-0000-000000000064", UUID.class);
        BigInteger bigInt = this.converter.convert(uuid, BigInteger.class);
        assert bigInt.intValue() == 100;

        assertThatThrownBy(() -> this.converter.convert("00000000", UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID string: 00000000");
    }

    @Test
    void testUUIDToUUID()
    {
        UUID uuid = this.converter.convert("00000007-0000-0000-0000-000000000064", UUID.class);
        UUID uuid2 = this.converter.convert(uuid, UUID.class);
        assert uuid.equals(uuid2);
    }

    @Test
    void testBogusToUUID()
    {
        assertThatThrownBy(() -> this.converter.convert((short) 77, UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Short (77)] target type 'UUID'");
    }

    @Test
    void testBigIntegerToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        BigInteger hundred = this.converter.convert(uuid, BigInteger.class);
        assert hundred.intValue() == 100;
    }

    @Test
    void testBigDecimalToUUID()
    {
        UUID uuid = this.converter.convert(new BigDecimal("100"), UUID.class);
        BigDecimal hundred = this.converter.convert(uuid, BigDecimal.class);
        assert hundred.intValue() == 100;

        uuid = this.converter.convert(new BigDecimal("100.4"), UUID.class);
        hundred = this.converter.convert(uuid, BigDecimal.class);
        assert hundred.intValue() == 100;
    }

    @Test
    void testUUIDToBigInteger()
    {
        BigInteger bigInt = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000064"), BigInteger.class);
        assert bigInt.intValue() == 100;

        bigInt = this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), BigInteger.class);
        assert bigInt.toString().equals("-18446744073709551617");

        bigInt = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), BigInteger.class);
        assert bigInt.intValue() == 0;

        assertThatThrownBy(() -> this.converter.convert(16.0, Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Double (16.0)] target type 'Class'");
    }

    @Test
    void testUUIDToBigDecimal()
    {
        BigDecimal bigDec = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000064"), BigDecimal.class);
        assert bigDec.intValue() == 100;

        bigDec = this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), BigDecimal.class);
        assert bigDec.toString().equals("-18446744073709551617");

        bigDec = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), BigDecimal.class);
        assert bigDec.intValue() == 0;
    }

    @Test
    void testMapToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", uuid.getMostSignificantBits());
        map.put("leastSigBits", uuid.getLeastSignificantBits());
        UUID hundred = this.converter.convert(map, UUID.class);
        assertEquals("00000000-0000-0000-0000-000000000064", hundred.toString());
    }

    @Test
    void testBadMapToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        Map<String, Object> map = new HashMap<>();
        map.put("leastSigBits", uuid.getLeastSignificantBits());
        assertThatThrownBy(() -> this.converter.convert(map, UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To convert Map to UUID, the Map must contain both 'mostSigBits' and 'leastSigBits' keys");
    }

    @Test
    void testClassToString()
    {
        String str = this.converter.convert(BigInteger.class, String.class);
        assert str.equals("java.math.BigInteger");

        str = this.converter.convert(null, String.class);
        assert str == null;
    }

    @Test
    void testSqlDateToString()
    {
        long now = System.currentTimeMillis();
        java.sql.Date date = new java.sql.Date(now);
        String strDate = this.converter.convert(date, String.class);
        Date x = this.converter.convert(strDate, Date.class);
        LocalDate l1 = this.converter.convert(date, LocalDate.class);
        LocalDate l2 = this.converter.convert(x, LocalDate.class);
        assertEquals(l1, l2);
    }

    @Test
    void tesTimestampToString()
    {
        long now = System.currentTimeMillis();
        Timestamp date = new Timestamp(now);
        String strDate = this.converter.convert(date, String.class);
        Date x = this.converter.convert(strDate, Date.class);
        String str2Date = this.converter.convert(x, String.class);
        assertEquals(str2Date, strDate);
    }

    @Test
    void testByteToMap()
    {
        byte b1 = (byte) 16;
        Map<?, ?> map = this.converter.convert(b1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (byte)16);
        assert map.get(Converter.VALUE).getClass().equals(Byte.class);

        Byte b2 = (byte) 16;
        map = this.converter.convert(b2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (byte)16);
        assert map.get(Converter.VALUE).getClass().equals(Byte.class);
    }

    @Test
    void testShortToMap()
    {
        short s1 = (short) 1600;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (short)1600);
        assert map.get(Converter.VALUE).getClass().equals(Short.class);

        Short s2 = (short) 1600;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (short)1600);
        assert map.get(Converter.VALUE).getClass().equals(Short.class);
    }

    @Test
    void testIntegerToMap()
    {
        int s1 = 1234567;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 1234567);
        assert map.get(Converter.VALUE).getClass().equals(Integer.class);

        Integer s2 = 1234567;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 1234567);
        assert map.get(Converter.VALUE).getClass().equals(Integer.class);
    }

    @Test
    void testLongToMap()
    {
        long s1 = 123456789012345L;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 123456789012345L);
        assert map.get(Converter.VALUE).getClass().equals(Long.class);

        Long s2 = 123456789012345L;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 123456789012345L);
        assert map.get(Converter.VALUE).getClass().equals(Long.class);
    }

    @Test
    void testFloatToMap()
    {
        float s1 = 3.141592f;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.141592f);
        assert map.get(Converter.VALUE).getClass().equals(Float.class);

        Float s2 = 3.141592f;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.141592f);
        assert map.get(Converter.VALUE).getClass().equals(Float.class);
    }

    @Test
    void testDoubleToMap()
    {
        double s1 = 3.14159265358979d;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.14159265358979d);
        assert map.get(Converter.VALUE).getClass().equals(Double.class);

        Double s2 = 3.14159265358979d;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.14159265358979d);
        assert map.get(Converter.VALUE).getClass().equals(Double.class);
    }

    @Test
    void testBooleanToMap()
    {
        boolean s1 = true;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), true);
        assert map.get(Converter.VALUE).getClass().equals(Boolean.class);

        Boolean s2 = true;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), true);
        assert map.get(Converter.VALUE).getClass().equals(Boolean.class);
    }

    @Test
    void testCharacterToMap()
    {
        char s1 = 'e';
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 'e');
        assert map.get(Converter.VALUE).getClass().equals(Character.class);

        Character s2 = 'e';
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 'e');
        assert map.get(Converter.VALUE).getClass().equals(Character.class);
    }

    @Test
    void testBigIntegerToMap()
    {
        BigInteger bi = BigInteger.valueOf(1234567890123456L);
        Map<?, ?> map = this.converter.convert(bi, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), bi);
        assert map.get(Converter.VALUE).getClass().equals(BigInteger.class);
    }

    @Test
    void testBigDecimalToMap()
    {
        BigDecimal bd = new BigDecimal("3.1415926535897932384626433");
        Map<?, ?> map = this.converter.convert(bd, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), bd);
        assert map.get(Converter.VALUE).getClass().equals(BigDecimal.class);
    }

    @Test
    void testAtomicBooleanToMap()
    {
        AtomicBoolean ab = new AtomicBoolean(true);
        Map<?, ?> map = this.converter.convert(ab, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), ab);
        assert map.get(Converter.VALUE).getClass().equals(AtomicBoolean.class);
    }

    @Test
    void testAtomicIntegerToMap()
    {
        AtomicInteger ai = new AtomicInteger(123456789);
        Map<?, ?> map = this.converter.convert(ai, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), ai);
        assert map.get(Converter.VALUE).getClass().equals(AtomicInteger.class);
    }

    @Test
    void testAtomicLongToMap()
    {
        AtomicLong al = new AtomicLong(12345678901234567L);
        Map<?, ?> map = this.converter.convert(al, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), al);
        assert map.get(Converter.VALUE).getClass().equals(AtomicLong.class);
    }

    @Test
    void testClassToMap()
    {
        Class<?> clazz = ConverterTest.class;
        Map<?, ?> map = this.converter.convert(clazz, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), clazz);
    }

    @Test
    void testUUIDToMap()
    {
        UUID uuid = new UUID(1L, 2L);
        Map<?, ?> map = this.converter.convert(uuid, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), uuid);
        assert map.get(Converter.VALUE).getClass().equals(UUID.class);
    }

    @Test
    void testCalendarToMap()
    {
        Calendar cal = Calendar.getInstance();
        Map<?, ?> map = this.converter.convert(cal, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), cal);
        assert map.get(Converter.VALUE) instanceof Calendar;
    }

    @Test
    void testDateToMap()
    {
        Date now = new Date();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(Date.class);
    }

    @Test
    void testSqlDateToMap()
    {
        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(java.sql.Date.class);
    }

    @Test
    void testTimestampToMap()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(Timestamp.class);
    }

    @Test
    void testLocalDateToMap()
    {
        LocalDate now = LocalDate.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(LocalDate.class);
    }

    @Test
    void testLocalDateTimeToMap()
    {
        LocalDateTime now = LocalDateTime.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(LocalDateTime.class);
    }

    @Test
    void testZonedDateTimeToMap()
    {
        ZonedDateTime now = ZonedDateTime.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(ZonedDateTime.class);
    }

    @Test
    void testUnknownType()
    {
        assertThatThrownBy(() -> this.converter.convert(null, Collection.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [null] target type 'Collection'");
    }

    @Test
    void testGetSupportedConversions()
    {
        Map map = this.converter.getSupportedConversions();
        assert map.size() > 10;
    }

    @Test
    void testAllSupportedConversions()
    {
        Map map = this.converter.allSupportedConversions();
        assert map.size() > 10;
    }

    @Test
    void testIsConversionSupport()
    {
        assert this.converter.isConversionSupportedFor(int.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(Integer.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(LocalDate.class, int.class);
        assert this.converter.isConversionSupportedFor(LocalDate.class, Integer.class);

        assert !this.converter.isDirectConversionSupportedFor(byte.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(byte.class, LocalDate.class);       // byte is upgraded to Byte, which is found as Number.

        assert this.converter.isConversionSupportedFor(Byte.class, LocalDate.class);       // Number is supported
        assert !this.converter.isDirectConversionSupportedFor(Byte.class, LocalDate.class);
        assert !this.converter.isConversionSupportedFor(LocalDate.class, byte.class);
        assert !this.converter.isConversionSupportedFor(LocalDate.class, Byte.class);

        assert this.converter.isConversionSupportedFor(UUID.class, String.class);
        assert this.converter.isConversionSupportedFor(UUID.class, Map.class);
        assert this.converter.isConversionSupportedFor(UUID.class, BigDecimal.class);
        assert this.converter.isConversionSupportedFor(UUID.class, BigInteger.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, long.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, Long.class);

        assert this.converter.isConversionSupportedFor(String.class, UUID.class);
        assert this.converter.isConversionSupportedFor(Map.class, UUID.class);
        assert this.converter.isConversionSupportedFor(BigDecimal.class, UUID.class);
        assert this.converter.isConversionSupportedFor(BigInteger.class, UUID.class);
    }

    static class DumbNumber extends BigInteger
    {
        DumbNumber(String val) {
            super(val);
        }

        public String toString() {
            return super.toString();
        }
    }

    @Test
    void testDumbNumberToByte()
    {
        DumbNumber dn = new DumbNumber("25");
        byte x = this.converter.convert(dn, byte.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToShort()
    {
        DumbNumber dn = new DumbNumber("25");
        short x = this.converter.convert(dn, short.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToShort2()
    {
        DumbNumber dn = new DumbNumber("25");
        Short x = this.converter.convert(dn, Short.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToInt()
    {
        DumbNumber dn = new DumbNumber("25");
        int x = this.converter.convert(dn, int.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToLong()
    {
        DumbNumber dn = new DumbNumber("25");
        long x = this.converter.convert(dn, long.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToFloat()
    {
        DumbNumber dn = new DumbNumber("3");
        float x = this.converter.convert(dn, float.class);
        assert x == 3;
    }

    @Test
    void testDumbNumberToDouble()
    {
        DumbNumber dn = new DumbNumber("3");
        double x = this.converter.convert(dn, double.class);
        assert x == 3;
    }

    @Test
    void testDumbNumberToBoolean()
    {
        DumbNumber dn = new DumbNumber("3");
        boolean x = this.converter.convert(dn, boolean.class);
        assert x;
    }

    @Test
    void testDumbNumberToCharacter()
    {
        DumbNumber dn = new DumbNumber("3");
        char x = this.converter.convert(dn, char.class);
        assert x == '\u0003';
    }

    @Test
    void testDumbNumberToBigInteger()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        BigInteger x = this.converter.convert(dn, BigInteger.class);
        assert x.toString().equals(dn.toString());
    }

    @Test
    void testDumbNumberToBigDecimal()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        BigDecimal x = this.converter.convert(dn, BigDecimal.class);
        assert x.toString().equals(dn.toString());
    }

    @Test
    void testDumbNumberToString()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        String x = this.converter.convert(dn, String.class);
        assert x.toString().equals("12345678901234567890");
    }

    @Test
    void testDumbNumberToUUIDProvesInheritance()
    {
        assert this.converter.isConversionSupportedFor(DumbNumber.class, UUID.class);
        assert !this.converter.isDirectConversionSupportedFor(DumbNumber.class, UUID.class);

        DumbNumber dn = new DumbNumber("1000");

        // Converts because DumbNumber inherits from Number.
        UUID uuid = this.converter.convert(dn, UUID.class);
        assert uuid.toString().equals("00000000-0000-0000-0000-0000000003e8");

        // Add in conversion
        this.converter.addConversion(DumbNumber.class, UUID.class, (fromInstance, converter, options) -> {
            DumbNumber bigDummy = (DumbNumber) fromInstance;
            BigInteger mask = BigInteger.valueOf(Long.MAX_VALUE);
            long mostSignificantBits = bigDummy.shiftRight(64).and(mask).longValue();
            long leastSignificantBits = bigDummy.and(mask).longValue();
            return new UUID(mostSignificantBits, leastSignificantBits);
        });

        // Still converts, but not using inheritance.
        uuid = this.converter.convert(dn, UUID.class);
        assert uuid.toString().equals("00000000-0000-0000-0000-0000000003e8");

        assert this.converter.isConversionSupportedFor(DumbNumber.class, UUID.class);
        assert this.converter.isDirectConversionSupportedFor(DumbNumber.class, UUID.class);
    }

    @Test
    void testUUIDtoDumbNumber()
    {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-0000000003e8");

        Object o = this.converter.convert(uuid, DumbNumber.class);
        assert o instanceof BigInteger;
        assert 1000L == ((Number) o).longValue();

        // Add in conversion
        this.converter.addConversion(UUID.class, DumbNumber.class, (fromInstance, converter, options) -> {
            UUID uuid1 = (UUID) fromInstance;
            BigInteger mostSignificant = BigInteger.valueOf(uuid1.getMostSignificantBits());
            BigInteger leastSignificant = BigInteger.valueOf(uuid1.getLeastSignificantBits());
            // Shift the most significant bits to the left and add the least significant bits
            return new DumbNumber(mostSignificant.shiftLeft(64).add(leastSignificant).toString());
        });

        // Converts!
        DumbNumber dn = this.converter.convert(uuid, DumbNumber.class);
        assert dn.toString().equals("1000");

        assert this.converter.isConversionSupportedFor(UUID.class, DumbNumber.class);
    }

    @Test
    void testUUIDtoBoolean()
    {
        assert !this.converter.isConversionSupportedFor(UUID.class, boolean.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, Boolean.class);

        assert !this.converter.isConversionSupportedFor(boolean.class, UUID.class);
        assert !this.converter.isConversionSupportedFor(Boolean.class, UUID.class);

        final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThatThrownBy(() -> this.converter.convert(uuid, boolean.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [UUID (00000000-0000-0000-0000-000000000000)] target type 'Boolean'");

        // Add in conversions
        this.converter.addConversion(UUID.class, boolean.class, (fromInstance, converter, options) -> {
            UUID uuid1 = (UUID) fromInstance;
            return !"00000000-0000-0000-0000-000000000000".equals(uuid1.toString());
        });

        // Add in conversions
        this.converter.addConversion(boolean.class, UUID.class, (fromInstance, converter, options) -> {
            boolean state = (Boolean)fromInstance;
            if (state) {
                return "00000000-0000-0000-0000-000000000001";
            } else {
                return "00000000-0000-0000-0000-000000000000";
            }
        });

        // Converts!
        assert !this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), boolean.class);
        assert this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000001"), boolean.class);
        assert this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), boolean.class);

        assert this.converter.isConversionSupportedFor(UUID.class, boolean.class);
        assert this.converter.isConversionSupportedFor(UUID.class, Boolean.class);

        assert this.converter.isConversionSupportedFor(boolean.class, UUID.class);
        assert this.converter.isConversionSupportedFor(Boolean.class, UUID.class);
    }

    @Test
    void testBooleanToUUID()
    {

    }

    static class Normie
    {
        String name;

        Normie(String name) {
            this.name = name;
        }

        void setName(String name)
        {
            this.name = name;
        }
    }

    static class Weirdo
    {
        String name;

        Weirdo(String name)
        {
            this.name = reverseString(name);
        }

        void setName(String name)
        {
            this.name = reverseString(name);
        }
    }

    static String reverseString(String in)
    {
        StringBuilder reversed = new StringBuilder();
        for (int i = in.length() - 1; i >= 0; i--) {
            reversed.append(in.charAt(i));
        }
        return reversed.toString();
    }

    @Test
    void testNormieToWeirdoAndBack()
    {
        this.converter.addConversion(Normie.class, Weirdo.class, (fromInstance, converter, options) -> {
            Normie normie = (Normie) fromInstance;
            Weirdo weirdo = new Weirdo(normie.name);
            return weirdo;
        });

        this.converter.addConversion(Weirdo.class, Normie.class, (fromInstance, converter, options) -> {
            Weirdo weirdo = (Weirdo) fromInstance;
            Normie normie = new Normie(reverseString(weirdo.name));
            return normie;
        });

        Normie normie = new Normie("Joe");
        Weirdo weirdo = this.converter.convert(normie, Weirdo.class);
        assertEquals(weirdo.name, "eoJ");

        weirdo = new Weirdo("Jacob");
        assertEquals(weirdo.name, "bocaJ");
        normie = this.converter.convert(weirdo, Normie.class);
        assertEquals(normie.name, "Jacob");

        assert this.converter.isConversionSupportedFor(Normie.class, Weirdo.class);
        assert this.converter.isConversionSupportedFor(Weirdo.class, Normie.class);
    }

    private static Stream<Arguments> emptyStringToType_params() {
        return Stream.of(
                Arguments.of("", byte.class, (byte)0),
                Arguments.of("", Byte.class, (byte)0),
                Arguments.of("", short.class, (short)0),
                Arguments.of("", Short.class, (short)0),
                Arguments.of("", int.class, 0),
                Arguments.of("", Integer.class, 0),
                Arguments.of("", long.class, 0L),
                Arguments.of("", Long.class, 0L),
                Arguments.of("", float.class, 0.0f),
                Arguments.of("", Float.class, 0.0f),
                Arguments.of("", double.class, 0.0d),
                Arguments.of("", Double.class, 0.0d),
                Arguments.of("", Boolean.class, false),
                Arguments.of("", boolean.class, false),
                Arguments.of("", BigDecimal.class, BigDecimal.ZERO),
                Arguments.of("", BigInteger.class, BigInteger.ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("emptyStringToType_params")
    void emptyStringToType(Object value, Class<?> type, Object expected)
    {
        Object converted = this.converter.convert(value, type);
        assertThat(converted).isEqualTo(expected);
    }

    @Test
    void emptyStringToAtomicBoolean()
    {
        AtomicBoolean converted = this.converter.convert("", AtomicBoolean.class);
        assertThat(converted.get()).isEqualTo(false);
    }

    @Test
    void emptyStringToAtomicInteger()
    {
        AtomicInteger converted = this.converter.convert("", AtomicInteger.class);
        assertThat(converted.get()).isEqualTo(0);
    }

    @Test
    void emptyStringToAtomicLong()
    {
        AtomicLong converted = this.converter.convert("", AtomicLong.class);
        assertThat(converted.get()).isEqualTo(0);
    }

    private ConverterOptions createConvertOptions(ZoneId sourceZoneId, final ZoneId targetZoneId)
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

            @Override
            public ZoneId getSourceZoneIdForLocalDates() {
                return sourceZoneId;
            }
        };
    }

    private ConverterOptions chicagoZone() { return createConvertOptions(null, CHICAGO); }
}
