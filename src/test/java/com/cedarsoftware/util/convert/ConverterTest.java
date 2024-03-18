package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.cedarsoftware.util.DeepEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static com.cedarsoftware.util.Converter.zonedDateTimeToMillis;
import static com.cedarsoftware.util.StringUtilities.EMPTY;
import static com.cedarsoftware.util.convert.Converter.VALUE;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.bar;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.foo;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.EPOCH_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
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
    private static final LocalDateTime LDT_2023_TOKYO = LocalDateTime.of(2023, 6, 25, 0, 57, 29, 729000000);
    private static final LocalDateTime LDT_2023_PARIS = LocalDateTime.of(2023, 6, 24, 17, 57, 29, 729000000);
    private static final LocalDateTime LDT_2023_GMT = LocalDateTime.of(2023, 6, 24, 15, 57, 29, 729000000);
    private static final LocalDateTime LDT_2023_NY = LocalDateTime.of(2023, 6, 24, 11, 57, 29, 729000000);
    private static final LocalDateTime LDT_2023_CHICAGO = LocalDateTime.of(2023, 6, 24, 10, 57, 29, 729000000);
    private static final LocalDateTime LDT_2023_LA = LocalDateTime.of(2023, 6, 24, 8, 57, 29, 729000000);
    private static final LocalDateTime LDT_MILLENNIUM_TOKYO = LocalDateTime.of(2000, 1, 1, 13, 59, 59, 959000000);
    private static final LocalDateTime LDT_MILLENNIUM_PARIS = LocalDateTime.of(2000, 1, 1, 5, 59, 59, 959000000);
    private static final LocalDateTime LDT_MILLENNIUM_GMT = LocalDateTime.of(2000, 1, 1, 4, 59, 59, 959000000);
    private static final LocalDateTime LDT_MILLENNIUM_NY = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 959000000);
    private static final LocalDateTime LDT_MILLENNIUM_CHICAGO = LocalDateTime.of(1999, 12, 31, 22, 59, 59, 959000000);
    private static final LocalDateTime LDT_MILLENNIUM_LA = LocalDateTime.of(1999, 12, 31, 20, 59, 59, 959000000);
    private Converter converter;


    private static final LocalDate LD_MILLENNIUM_NY = LocalDate.of(1999, 12, 31);
    private static final LocalDate LD_MILLENNIUM_TOKYO = LocalDate.of(2000, 1, 1);

    private static final LocalDate LD_MILLENNIUM_CHICAGO = LocalDate.of(1999, 12, 31);

    private static final LocalDate LD_2023_NY = LocalDate.of(2023, 6, 24);

    enum fubar
    {
        foo, bar, baz, quz
    }

    @BeforeEach
    public void before() {
        // create converter with default options
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static  <T extends Number> Stream<Arguments> paramsForIntegerTypes(T min, T max) {
        List<Arguments> arguments = new ArrayList(20);
        arguments.add(Arguments.of("3.159", 3));
        arguments.add(Arguments.of("3.519", 3));
        arguments.add(Arguments.of("-3.159", -3));
        arguments.add(Arguments.of("-3.519", -3));
        arguments.add(Arguments.of("" + min, min));
        arguments.add(Arguments.of("" + max, max));
        arguments.add(Arguments.of("" + min + ".25", min));
        arguments.add(Arguments.of("" + max + ".75", max));
        arguments.add(Arguments.of((byte)-3, -3));
        arguments.add(Arguments.of((byte)3, 3));
        arguments.add(Arguments.of((short)-9, -9));
        arguments.add(Arguments.of((short)9, 9));
        arguments.add(Arguments.of(-13,  -13));
        arguments.add(Arguments.of(13, 13));
        arguments.add(Arguments.of(-7L,  -7));
        arguments.add(Arguments.of(7L, 7));
        arguments.add(Arguments.of(-11.0d, -11));
        arguments.add(Arguments.of(11.0d, 11));
        arguments.add(Arguments.of(3.14f, 3));
        arguments.add(Arguments.of(3.59f, 3));
        arguments.add(Arguments.of(-3.14f, -3));
        arguments.add(Arguments.of(-3.59f, -3));
        arguments.add(Arguments.of(3.14d, 3));
        arguments.add(Arguments.of(3.59d, 3));
        arguments.add(Arguments.of(-3.14d, -3));
        arguments.add(Arguments.of(-3.59d, -3));
        arguments.add(Arguments.of( new AtomicInteger(0), 0));
        arguments.add(Arguments.of( new AtomicLong(9), 9));
        arguments.add(Arguments.of( BigInteger.valueOf(13), 13));
        arguments.add(Arguments.of( BigDecimal.valueOf(23), 23));

        return arguments.stream();
    }

    private static  <T extends Number> Stream<Arguments> paramsForFloatingPointTypes(T min, T max) {
        List<Arguments> arguments = new ArrayList(20);
        arguments.add(Arguments.of("3.159", 3.159d));
        arguments.add(Arguments.of("3.519", 3.519d));
        arguments.add(Arguments.of("-3.159", -3.159d));
        arguments.add(Arguments.of("-3.519", -3.519d));
        arguments.add(Arguments.of("" + min, min));
        arguments.add(Arguments.of("" + max, max));
        arguments.add(Arguments.of(min.doubleValue() + .25, min.doubleValue() + .25d));
        arguments.add(Arguments.of(max.doubleValue() - .75, max.doubleValue() - .75d));
        arguments.add(Arguments.of((byte)-3, -3));
        arguments.add(Arguments.of((byte)3, 3));
        arguments.add(Arguments.of((short)-9, -9));
        arguments.add(Arguments.of((short)9, 9));
        arguments.add(Arguments.of(-13,  -13));
        arguments.add(Arguments.of(13, 13));
        arguments.add(Arguments.of(-7L,  -7));
        arguments.add(Arguments.of(7L, 7));
        arguments.add(Arguments.of(-11.0d, -11.0d));
        arguments.add(Arguments.of(11.0d, 11.0d));
        arguments.add(Arguments.of(3.0f, 3.0d));
        arguments.add(Arguments.of(-5.0f, -5.0d));
        arguments.add(Arguments.of(-3.14d, -3.14d));
        arguments.add(Arguments.of(-3.59d, -3.59d));
        arguments.add(Arguments.of( new AtomicInteger(0), 0));
        arguments.add(Arguments.of( new AtomicLong(9), 9));
        arguments.add(Arguments.of( BigInteger.valueOf(13), 13));
        arguments.add(Arguments.of( BigDecimal.valueOf(23), 23));

        return arguments.stream();
    }


    private static Stream<Arguments> toByteParams() {
        return paramsForIntegerTypes(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }


    @ParameterizedTest
    @MethodSource("toByteParams")
    void toByte(Object source, Number number)
    {
        byte expected = number.byteValue();
        Byte converted = this.converter.convert(source, Byte.class);
        assertThat(converted).isEqualTo((byte)expected);
    }

    @ParameterizedTest
    @MethodSource("toByteParams")
    void toByteUsingPrimitive(Object source, Number number)
    {
        byte expected = number.byteValue();
        byte converted = this.converter.convert(source, byte.class);
        assertThat(converted).isEqualTo(expected);
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
    void toByte_whenNullOrEmpty_andConvertingToPrimitive_returnsZero(String s)
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
        return paramsForIntegerTypes(Short.MIN_VALUE, Short.MAX_VALUE);
    }


    @ParameterizedTest
    @MethodSource("toShortParams")
    void toShort(Object value, Number number)
    {
        short expected = number.shortValue();
        Short converted = this.converter.convert(value, Short.class);
        assertThat(converted).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("toShortParams")
    void toShort_usingPrimitiveClass(Object value, Number number) {
        short expected = number.shortValue();
        short converted = this.converter.convert(value, short.class);
        assertThat(converted).isEqualTo(expected);
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
        return paramsForIntegerTypes(Integer.MIN_VALUE, Integer.MAX_VALUE);
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
                Arguments.of("45badNumber", "not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of( "9999999999", "not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of( "12147483648", "not parseable as an int value or outside -2147483648 to 2147483647"),
                Arguments.of("2147483649", "not parseable as an int value or outside -2147483648 to 2147483647"),
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
        return paramsForIntegerTypes(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void toLong(Object value, Number number)
    {
        Long expected = number.longValue();
        Long converted = this.converter.convert(value, Long.class);
        assertThat(converted).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void toLong_usingPrimitives(Object value, Number number)
    {
        long expected = number.longValue();
        long converted = this.converter.convert(value, long.class);
        assertThat(converted).isEqualTo(expected);
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
    
    private static Stream<Arguments> toLongWithIllegalParams() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a long value or outside -9223372036854775808 to 9223372036854775807"),
                Arguments.of( "-9223372036854775809", "not parseable as a long value or outside -9223372036854775808 to 9223372036854775807"),
                Arguments.of("9223372036854775808", "not parseable as a long value or outside -9223372036854775808 to 9223372036854775807"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("toLongWithIllegalParams")
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

    private static final ZoneId IGNORED = ZoneId.of("Antarctica/South_Pole");
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


    private static Stream<Arguments> epochMilliWithZoneId() {
        return Stream.of(
                Arguments.of("946702799959", TOKYO),
                Arguments.of("946702799959", PARIS),
                Arguments.of("946702799959", GMT),
                Arguments.of("946702799959", NEW_YORK),
                Arguments.of("946702799959", CHICAGO),
                Arguments.of("946702799959", LOS_ANGELES)
        );
    }


    private static Stream<Arguments> dateStringNoZoneOffset() {
        return Stream.of(
                Arguments.of("2000-01-01T13:59:59", TOKYO),
                Arguments.of("2000-01-01T05:59:59", PARIS),
                Arguments.of("2000-01-01T04:59:59", GMT),
                Arguments.of("1999-12-31T23:59:59", NEW_YORK),
                Arguments.of("1999-12-31T22:59:59", CHICAGO),
                Arguments.of("1999-12-31T20:59:59", LOS_ANGELES)
        );
    }


    private static Stream<Arguments> dateStringInIsoOffsetDateTime() {
        return Stream.of(
                Arguments.of("2000-01-01T13:59:59+09:00"),
                Arguments.of("2000-01-01T05:59:59+01:00"),
                Arguments.of("2000-01-01T04:59:59Z"),
                Arguments.of("1999-12-31T23:59:59-05:00"),
                Arguments.of("1999-12-31T22:59:59-06:00"),
                Arguments.of("1999-12-31T20:59:59-08:00")
        );
    }

    private static Stream<Arguments> dateStringInIsoOffsetDateTimeWithMillis() {
        return Stream.of(
                Arguments.of("2000-01-01T13:59:59.959+09:00"),
                Arguments.of("2000-01-01T05:59:59.959+01:00"),
                Arguments.of("2000-01-01T04:59:59.959Z"),
                Arguments.of("1999-12-31T23:59:59.959-05:00"),
                Arguments.of("1999-12-31T22:59:59.959-06:00"),
                Arguments.of("1999-12-31T20:59:59.959-08:00")
        );
    }

    private static Stream<Arguments> dateStringInIsoZoneDateTime() {
        return Stream.of(
                Arguments.of("2000-01-01T13:59:59.959+09:00[Asia/Tokyo]"),
                Arguments.of("2000-01-01T05:59:59.959+01:00[Europe/Paris]"),
                Arguments.of("2000-01-01T04:59:59.959Z[GMT]"),
                Arguments.of("1999-12-31T23:59:59.959-05:00[America/New_York]"),
                Arguments.of("1999-12-31T22:59:59.959-06:00[America/Chicago]"),
                Arguments.of("1999-12-31T20:59:59.959-08:00[America/Los_Angeles]")
        );
    }

    @ParameterizedTest
    @MethodSource("epochMilliWithZoneId")
    void testEpochMilliWithZoneId(String epochMilli, ZoneId zoneId) {
        Converter converter = new Converter(createCustomZones(NEW_YORK));
        LocalDateTime localDateTime = converter.convert(epochMilli, LocalDateTime.class);

        assertThat(localDateTime)
                .hasYear(1999)
                .hasMonthValue(12)
                .hasDayOfMonth(31)
                .hasHour(23)
                .hasMinute(59)
                .hasSecond(59);
    }

    @ParameterizedTest
    @MethodSource("dateStringNoZoneOffset")
    void testStringDateWithNoTimeZoneInformation(String date, ZoneId zoneId) {
        // times with zoneid passed in to convert to ZonedDateTime
        Converter converter = new Converter(createCustomZones(zoneId));
        ZonedDateTime zdt = converter.convert(date, ZonedDateTime.class);

        // convert to local time NY
        ZonedDateTime nyTime = zdt.withZoneSameInstant(NEW_YORK);

        assertThat(nyTime.toLocalDateTime())
                .hasYear(1999)
                .hasMonthValue(12)
                .hasDayOfMonth(31)
                .hasHour(23)
                .hasMinute(59)
                .hasSecond(59);
    }


    @ParameterizedTest
    @MethodSource("dateStringInIsoOffsetDateTime")
    void testStringDateWithTimeZoneToLocalDateTime(String date) {
        //  source is TOKYO, should be ignored when zone is provided on string.
        Converter converter = new Converter(createCustomZones(IGNORED));
        ZonedDateTime zdt = converter.convert(date, ZonedDateTime.class);

        ZonedDateTime nyTime = zdt.withZoneSameInstant(NEW_YORK);

        assertThat(nyTime.toLocalDateTime())
                .hasYear(1999)
                .hasMonthValue(12)
                .hasDayOfMonth(31)
                .hasHour(23)
                .hasMinute(59)
                .hasSecond(59);
    }


    @ParameterizedTest
    @MethodSource("dateStringInIsoOffsetDateTimeWithMillis")
    void testStringDateWithTimeZoneToLocalDateTimeIncludeMillis(String date) {
        // will come in with the zone from the string.
        Converter converter = new Converter(createCustomZones(IGNORED));
        ZonedDateTime zdt = converter.convert(date, ZonedDateTime.class);

        // create zoned date time from the localDateTime from string, providing NEW_YORK as time zone.
        LocalDateTime localDateTime = zdt.withZoneSameInstant(NEW_YORK).toLocalDateTime();

        assertThat(localDateTime)
                .hasYear(1999)
                .hasMonthValue(12)
                .hasDayOfMonth(31)
                .hasHour(23)
                .hasMinute(59)
                .hasSecond(59)
                .hasNano(959 * 1_000_000);
    }

    @ParameterizedTest
    @MethodSource("dateStringInIsoZoneDateTime")
    void testStringDateWithTimeZoneToLocalDateTimeWithZone(String date) {
        // will come in with the zone from the string.
        Converter converter = new Converter(createCustomZones(IGNORED));
        ZonedDateTime zdt = converter.convert(date, ZonedDateTime.class);

        // create localDateTime in NEW_YORK time.
        LocalDateTime localDateTime = zdt.withZoneSameInstant(NEW_YORK).toLocalDateTime();

        assertThat(localDateTime)
                .hasYear(1999)
                .hasMonthValue(12)
                .hasDayOfMonth(31)
                .hasHour(23)
                .hasMinute(59)
                .hasSecond(59)
                .hasNano(959 * 1_000_000);
    }
    
    private static Stream<Arguments> epochMillis_withLocalDateTimeInformation() {
        return Stream.of(
                Arguments.of(1687622249729L, TOKYO, LDT_2023_TOKYO),
                Arguments.of(1687622249729L, PARIS, LDT_2023_PARIS),
                Arguments.of(1687622249729L, GMT, LDT_2023_GMT),
                Arguments.of(1687622249729L, NEW_YORK, LDT_2023_NY),
                Arguments.of(1687622249729L, CHICAGO, LDT_2023_CHICAGO),
                Arguments.of(1687622249729L, LOS_ANGELES, LDT_2023_LA),
                Arguments.of(946702799959L, TOKYO, LDT_MILLENNIUM_TOKYO),
                Arguments.of(946702799959L, PARIS, LDT_MILLENNIUM_PARIS),
                Arguments.of(946702799959L, GMT, LDT_MILLENNIUM_GMT),
                Arguments.of(946702799959L, NEW_YORK, LDT_MILLENNIUM_NY),
                Arguments.of(946702799959L, CHICAGO, LDT_MILLENNIUM_CHICAGO),
                Arguments.of(946702799959L, LOS_ANGELES, LDT_MILLENNIUM_LA)
        );
    }

    private static Stream<Arguments> epochNanos_withLocalDateTimeInformation() {
        return Stream.of(
                Arguments.of(1687622249729000000L, TOKYO, LDT_2023_TOKYO),
                Arguments.of(1687622249729000000L, PARIS, LDT_2023_PARIS),
                Arguments.of(1687622249729000000L, GMT, LDT_2023_GMT),
                Arguments.of(1687622249729000000L, NEW_YORK, LDT_2023_NY),
                Arguments.of(1687622249729000000L, CHICAGO, LDT_2023_CHICAGO),
                Arguments.of(1687622249729000000L, LOS_ANGELES, LDT_2023_LA),
                Arguments.of(946702799959000000L, TOKYO, LDT_MILLENNIUM_TOKYO),
                Arguments.of(946702799959000000L, PARIS, LDT_MILLENNIUM_PARIS),
                Arguments.of(946702799959000000L, GMT, LDT_MILLENNIUM_GMT),
                Arguments.of(946702799959000000L, NEW_YORK, LDT_MILLENNIUM_NY),
                Arguments.of(946702799959000000L, CHICAGO, LDT_MILLENNIUM_CHICAGO),
                Arguments.of(946702799959000000L, LOS_ANGELES, LDT_MILLENNIUM_LA)
        );
    }

    @Test
    void testEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1687622249729L);

        ZonedDateTime tokyo = instant.atZone(TOKYO);
        assertThat(tokyo.toString()).contains("2023-06-25T00:57:29.729");
        assertThat(tokyo.toInstant().toEpochMilli()).isEqualTo(1687622249729L);

        ZonedDateTime ny = instant.atZone(NEW_YORK);
        assertThat(ny.toString()).contains("2023-06-24T11:57:29.729");
        assertThat(ny.toInstant().toEpochMilli()).isEqualTo(1687622249729L);

        ZonedDateTime converted = tokyo.withZoneSameInstant(NEW_YORK);
        assertThat(ny).isEqualTo(converted);
        assertThat(converted.toInstant().toEpochMilli()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(IGNORED));
        LocalDateTime localDateTime = converter.convert(calendar, LocalDateTime.class);

        assertThat(localDateTime).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalDateTime_whenCalendarTimeZoneMatches(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(calendar, LocalDateTime.class);

        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalDateTime_whenCalendarTimeZoneDoesNotMatch(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(IGNORED));
        LocalDateTime localDateTime = converter.convert(calendar, LocalDateTime.class);

        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendar_roundTrip(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(expected.getYear());
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(expected.getMonthValue()-1);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(expected.getDayOfMonth());
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(expected.getHour());
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(expected.getMinute());
        assertThat(calendar.get(Calendar.SECOND)).isEqualTo(expected.getSecond());
        assertThat(calendar.getTimeInMillis()).isEqualTo(epochMilli);
    }


    private static Stream<Arguments> roundTrip_tokyoTime() {
        return Stream.of(
                Arguments.of(946652400000L, TOKYO, LD_MILLENNIUM_TOKYO),
                Arguments.of(946652400000L, NEW_YORK, LD_MILLENNIUM_NY),
                Arguments.of(946652400000L, CHICAGO, LD_MILLENNIUM_CHICAGO)
        );
    }

    @ParameterizedTest
    @MethodSource("roundTrip_tokyoTime")
    void testCalendar_toLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(expected.getYear());
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(expected.getMonthValue()-1);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(expected.getDayOfMonth());
        assertThat(calendar.getTimeInMillis()).isEqualTo(epochMilli);

        Converter converter = new Converter(createCustomZones(IGNORED));
        LocalDate localDate = converter.convert(calendar, LocalDate.class);
        assertThat(localDate).isEqualTo(expected);
    }

    private static Stream<Arguments> localDateToLong() {
        return Stream.of(
                Arguments.of(946616400000L, NEW_YORK, LD_MILLENNIUM_NY),
                Arguments.of(1687532400000L, TOKYO, LD_2023_NY)
        );
    }
    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testConvertLocalDateToLong(long epochMilli, ZoneId zoneId, LocalDate expected) {

        Converter converter = new Converter(createCustomZones(zoneId));
        long intermediate = converter.convert(expected, long.class);

        assertThat(intermediate).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToInstant(long epochMilli, ZoneId zoneId, LocalDate expected) {

        Converter converter = new Converter(createCustomZones(zoneId));
        Instant intermediate = converter.convert(expected, Instant.class);

        assertThat(intermediate.toEpochMilli()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToDouble(long epochMilli, ZoneId zoneId, LocalDate expected) {

        Converter converter = new Converter(createCustomZones(zoneId));
        double intermediate = converter.convert(expected, double.class);

        assertThat(intermediate * 1000.0).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToAtomicLong(long epochMilli, ZoneId zoneId, LocalDate expected) {

        Converter converter = new Converter(createCustomZones(zoneId));
        AtomicLong intermediate = converter.convert(expected, AtomicLong.class);

        assertThat(intermediate.get()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToDate(long epochMilli, ZoneId zoneId, LocalDate expected) {

        Converter converter = new Converter(createCustomZones(zoneId));
        Date intermediate = converter.convert(expected,Date.class);

        assertThat(intermediate.getTime()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateSqlDate(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Converter converter = new Converter(createCustomZones(zoneId));
        java.sql.Date intermediate = converter.convert(expected, java.sql.Date.class);
        assertThat(intermediate.getTime()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateTimestamp(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Converter converter = new Converter(createCustomZones(zoneId));
        Timestamp intermediate = converter.convert(expected, Timestamp.class);
        assertTrue(intermediate.toInstant().toString().startsWith(expected.toString()));
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateZonedDateTime(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Converter converter = new Converter(createCustomZones(zoneId));
        ZonedDateTime intermediate = converter.convert(expected, ZonedDateTime.class);
        assertThat(intermediate.toInstant().toEpochMilli()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToBigInteger(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Converter converter = new Converter(createCustomZones(zoneId));
        BigInteger intermediate = converter.convert(expected, BigInteger.class);
        assertThat(intermediate.longValue()).isEqualTo(epochMilli * 1_000_000);
    }

    @ParameterizedTest
    @MethodSource("localDateToLong")
    void testLocalDateToBigDecimal(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Converter converter = new Converter(createCustomZones(zoneId));
        BigDecimal intermediate = converter.convert(expected, BigDecimal.class);
        assertThat(intermediate.longValue() * 1000).isEqualTo(epochMilli);
    }
    
    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(time, LocalDateTime.class);

        assertThat(time.toInstant().toEpochMilli()).isEqualTo(epochMilli);
        assertThat(localDateTime).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLocalTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalTime actual = converter.convert(time, LocalTime.class);

        assertThat(actual).isEqualTo(expected.toLocalTime());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLocalDate(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate actual = converter.convert(time, LocalDate.class);

        assertThat(actual).isEqualTo(expected.toLocalDate());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToInstant(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        Instant actual = converter.convert(time, Instant.class);

        assertThat(actual).isEqualTo(time.toInstant());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToCalendar(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        ZonedDateTime time = Instant.ofEpochMilli(epochMilli).atZone(zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        Calendar actual = converter.convert(time, Calendar.class);

        assertThat(actual.getTime().getTime()).isEqualTo(time.toInstant().toEpochMilli());
        assertThat(actual.getTimeZone()).isEqualTo(TimeZone.getTimeZone(zoneId));
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testZonedDateTimeToLong(long epochMilli, ZoneId zoneId, LocalDateTime localDateTime)
    {
        ZonedDateTime time = ZonedDateTime.of(localDateTime, zoneId);

        Converter converter = new Converter(createCustomZones(zoneId));
        long instant = converter.convert(time, long.class);

        assertThat(instant).isEqualTo(epochMilli);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testLongToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(epochMilli, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testAtomicLongToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        AtomicLong time = new AtomicLong(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(time, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testLongToInstant(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(zoneId));
        Instant actual = converter.convert(epochMilli, Instant.class);
        assertThat(actual).isEqualTo(Instant.ofEpochMilli(epochMilli));
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testBigDecimalToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        BigDecimal bd = BigDecimal.valueOf(epochMilli);
        bd = bd.divide(BigDecimal.valueOf(1000));

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(bd, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(instant, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testDateToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Date date = new Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(date, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testDateToZonedDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Date date = new Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        ZonedDateTime zonedDateTime = converter.convert(date, ZonedDateTime.class);
        assertThat(zonedDateTime.toLocalDateTime()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToZonedDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant date = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        ZonedDateTime zonedDateTime = converter.convert(date, ZonedDateTime.class);
        assertThat(zonedDateTime.toInstant()).isEqualTo(date);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testDateToInstant(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Date date = new Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        Instant actual = converter.convert(date, Instant.class);
        assertThat(actual.toEpochMilli()).isEqualTo(epochMilli);
    }
    
    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testSqlDateToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        java.sql.Date date = new java.sql.Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(date, LocalDateTime.class);
        assertThat(localDateTime).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToLong(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        long actual = converter.convert(instant, long.class);
        assertThat(actual).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToAtomicLong(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        AtomicLong actual = converter.convert(instant, AtomicLong.class);
        assertThat(actual.get()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToDouble(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        double actual = converter.convert(instant, double.class);
        assertThat(actual).isEqualTo((double)epochMilli / 1000.0);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToTimestamp(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        Timestamp actual = converter.convert(instant, Timestamp.class);
        assertThat(actual.getTime()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToDate(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        Date actual = converter.convert(instant, Date.class);
        assertThat(actual.getTime()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToSqlDate(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        java.sql.Date actual = converter.convert(instant, java.sql.Date.class);
        assertThat(actual.getTime()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToCalendar(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        Calendar actual = converter.convert(instant, Calendar.class);
        assertThat(actual.getTime().getTime()).isEqualTo(epochMilli);
        assertThat(actual.getTimeZone()).isEqualTo(TimeZone.getTimeZone(zoneId));
    }
    
    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToBigDecimal(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        BigDecimal actual = converter.convert(instant, BigDecimal.class);
        assertThat(actual.multiply(BigDecimal.valueOf(1000)).longValue()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToLocalDate(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate actual = converter.convert(instant, LocalDate.class);
        assertThat(actual).isEqualTo(expected.toLocalDate());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testInstantToLocalTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalTime actual = converter.convert(instant, LocalTime.class);
        assertThat(actual).isEqualTo(expected.toLocalTime());
    }
    
    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testTimestampToLocalDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Timestamp date = new Timestamp(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDateTime localDateTime = converter.convert(date, LocalDateTime.class);
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
    void testCalendarToDouble(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        double d = converter.convert(calendar, double.class);
        assertThat(d * 1000).isEqualTo((double)epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testCalendarToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(calendar, LocalDate.class);
        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToLocalTime(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalTime actual = converter.convert(calendar, LocalTime.class);
        assertThat(actual).isEqualTo(expected.toLocalTime());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToZonedDateTime(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(IGNORED));
        ZonedDateTime actual = converter.convert(calendar, ZonedDateTime.class);
        assertThat(actual.toLocalDateTime()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToInstant(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        Instant actual = converter.convert(calendar, Instant.class);
        assertThat(actual.toEpochMilli()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToBigDecimal(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        BigDecimal actual = converter.convert(calendar, BigDecimal.class);
        actual = actual.multiply(BigDecimal.valueOf(1000));
        assertThat(actual.longValue()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testCalendarToBigInteger(long epochMilli, ZoneId zoneId, LocalDateTime expected)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        BigInteger actual = converter.convert(calendar, BigInteger.class);
        actual = actual.divide(BigInteger.valueOf(1_000_000));
        assertThat(actual.longValue()).isEqualTo(epochMilli);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateTimeInformation")
    void testDateToLocalTime(long epochMilli, ZoneId zoneId, LocalDateTime expected) {
        Date date = new Date(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalTime actual = converter.convert(date, LocalTime.class);
        assertThat(actual).isEqualTo(expected.toLocalTime());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testCalendarToLocalDate_whenCalendarTimeZoneMatches(long epochMilli, ZoneId zoneId, LocalDate expected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        calendar.setTimeInMillis(epochMilli);

        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(calendar, LocalDate.class);
        assertThat(localDate).isEqualTo(expected);
    }

    @Test
    void testCalendarToLocalDate_whenCalendarTimeZoneDoesNotMatchTarget_convertsTimeCorrectly() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK));
        calendar.setTimeInMillis(1687622249729L);

        Converter converter = new Converter(createCustomZones(IGNORED));
        LocalDate localDate = converter.convert(calendar, LocalDate.class);

        assertThat(localDate)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(24);
    }

    @Test
    void testCalendar_testRoundTripWithLocalDate() {

        // Create LocalDateTime as CHICAGO TIME.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(CHICAGO));
        calendar.setTimeInMillis(1687622249729L);

        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(5);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(24);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(10);
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(57);
        assertThat(calendar.get(Calendar.SECOND)).isEqualTo(29);
        assertThat(calendar.getTimeInMillis()).isEqualTo(1687622249729L);

        // Convert calendar calendar to TOKYO LocalDateTime
        Converter converter = new Converter(createCustomZones(IGNORED));
        LocalDateTime localDateTime = converter.convert(calendar, LocalDateTime.class);

        assertThat(localDateTime)
                .hasYear(2023)
                .hasMonthValue(6)
                .hasDayOfMonth(24)
                .hasHour(10)
                .hasMinute(57)
                .hasSecond(29)
                .hasNano(729000000);

        //  Convert Tokyo local date time to CHICAGO Calendar
        //  We don't know the source ZoneId we are trying to convert.
        converter = new Converter(createCustomZones(CHICAGO));
        Calendar actual = converter.convert(localDateTime, Calendar.class);

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
        Converter converter = new Converter(options);
        Long converted = converter.convert(localDate, Long.class);
        assertThat(converted).isEqualTo(localDate.atStartOfDay(options.getZoneId()).toInstant().toEpochMilli());
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testLongToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(epochMilli, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testZonedDateTimeToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(epochMilli, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }


    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testInstantToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(instant, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testDateToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Date date = new Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(date, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testSqlDateToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        java.sql.Date date = new java.sql.Date(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(date, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("epochMillis_withLocalDateInformation")
    void testTimestampToLocalDate(long epochMilli, ZoneId zoneId, LocalDate expected)
    {
        Timestamp date = new Timestamp(epochMilli);
        Converter converter = new Converter(createCustomZones(zoneId));
        LocalDate localDate = converter.convert(date, LocalDate.class);

        assertThat(localDate).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("toLongParams")
    void testLongToBigInteger(Object source, Number number)
    {
        long expected = number.longValue();
        Converter converter = new Converter(createCustomZones(null));
        BigInteger actual = converter.convert(source, BigInteger.class);

        assertThat(actual).isEqualTo(BigInteger.valueOf(expected));
    }
    
    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateToLong(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        long milli = converter.convert(initial, long.class);
        assertThat(milli).isEqualTo(epochMilli);
    }


    private static Stream<Arguments> localDateTimeConversion_params() {
        return Stream.of(
                Arguments.of(1687622249729L, NEW_YORK, LDT_2023_NY, TOKYO, LDT_2023_TOKYO),
                Arguments.of(1687622249729L, LOS_ANGELES, LDT_2023_LA, PARIS, LDT_2023_PARIS)
        );
    }


    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToLong(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        long milli = converter.convert(initial, long.class);
        assertThat(milli).isEqualTo(epochMilli);

        converter = new Converter(createCustomZones(targetZoneId));
        LocalDateTime actual = converter.convert(milli, LocalDateTime.class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToInstant(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        Instant intermediate = converter.convert(initial, Instant.class);
        assertThat(intermediate.toEpochMilli()).isEqualTo(epochMilli);

        converter = new Converter(createCustomZones(targetZoneId));
        LocalDateTime actual = converter.convert(intermediate, LocalDateTime.class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToAtomicLong(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        AtomicLong milli = converter.convert(initial, AtomicLong.class);
        assertThat(milli.longValue()).isEqualTo(epochMilli);

        converter = new Converter(createCustomZones(targetZoneId));
        LocalDateTime actual = converter.convert(milli, LocalDateTime.class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToZonedDateTime(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        ZonedDateTime intermediate = converter.convert(initial, ZonedDateTime.class);
        assertThat(intermediate.toInstant().toEpochMilli()).isEqualTo(epochMilli);

        converter = new Converter(createCustomZones(targetZoneId));
        LocalDateTime actual = converter.convert(intermediate, LocalDateTime.class);
        assertThat(actual).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @MethodSource("localDateTimeConversion_params")
    void testLocalDateTimeToBigDecimal(long epochMilli, ZoneId sourceZoneId, LocalDateTime initial, ZoneId targetZoneId, LocalDateTime expected)
    {
        Converter converter = new Converter(createCustomZones(sourceZoneId));
        BigDecimal milli = converter.convert(initial, BigDecimal.class);
        milli = milli.multiply(BigDecimal.valueOf(1000));
        assertThat(milli.longValue()).isEqualTo(epochMilli);

        converter = new Converter(createCustomZones(targetZoneId));
        LocalDateTime actual = converter.convert(milli.longValue(), LocalDateTime.class);
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> testAtomicLongParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a long value"),
                Arguments.of( "-9223372036854775809", "not parseable as a long value"),
                Arguments.of("9223372036854775808", "not parseable as a long value"),
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
        assertThat(converted).startsWith("2015-01-17T08:34:49");
    }

    @Test
    void testString_fromCalendar()
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(1421483689000L);

        Converter converter1 = new Converter(new ConverterOptions() {
            public ZoneId getZoneId() { return ZoneId.of("GMT"); }
        });
        assertEquals("2015-01-17T08:34:49.000Z", converter1.convert(cal.getTime(), String.class));
        assertEquals("2015-01-17T08:34:49.000Z", converter1.convert(cal, String.class));
    }

    @Test
    void testString_fromLocalDate()
    {
        LocalDate localDate = LocalDate.of(2015, 9, 3);
        String converted = this.converter.convert(localDate, String.class);
        assertThat(converted).isEqualTo("2015-09-03");
    }


    private static Stream<Arguments> testBigDecimalParams() {
        return paramsForFloatingPointTypes(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("testBigDecimalParams")
    void testBigDecimal(Object value, Number number)
    {
        BigDecimal converted = this.converter.convert(value, BigDecimal.class);
        assertThat(converted).isEqualTo(new BigDecimal(number.toString()));
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
    void testBigDecimal_witCalendar() {
        Calendar today = Calendar.getInstance();
        BigDecimal bd = new BigDecimal(today.getTime().getTime()).divide(BigDecimal.valueOf(1000));
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
        return paramsForIntegerTypes(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("testBigIntegerParams")
    void testBigInteger(Object value, Number number)
    {
        BigInteger converted = this.converter.convert(value, BigInteger.class);
        assertThat(converted).isEqualTo(new BigInteger(number.toString()));
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
    void testBigInteger_withCalendar() {
        Calendar today = Calendar.getInstance();
        BigInteger bd = BigInteger.valueOf(today.getTime().getTime()).multiply(BigInteger.valueOf(1_000_000));
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
                Arguments.of( new AtomicLong(1705601070270L)),
                Arguments.of( 1705601070.270798659898d),
                Arguments.of( BigInteger.valueOf(1705601070270000000L)),
                Arguments.of( new BigDecimal("1705601070.270")),
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

    private static Stream<Arguments> extremeDateParams() {
        return Stream.of(
                Arguments.of(Long.MIN_VALUE,new Date(Long.MIN_VALUE)),
                Arguments.of(Long.MAX_VALUE, new Date(Long.MAX_VALUE)),
                Arguments.of(127.0d, new Date(127*1000))
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
        BigInteger bigInt = new BigInteger("" + now * 1_000_000);
        sqlDate = this.converter.convert(bigInt, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigDecimal to java.sql.Date
        BigDecimal bigDec = new BigDecimal(now);
        bigDec = bigDec.divide(BigDecimal.valueOf(1000));
        sqlDate = this.converter.convert(bigDec, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigInteger to Timestamp
        bigInt = new BigInteger("" + now * 1000000L);
        tstamp = this.converter.convert(bigInt, Timestamp.class);
        assert tstamp.getTime() == now;

        // BigDecimal to TimeStamp
        bigDec = new BigDecimal(now);
        bigDec = bigDec.divide(BigDecimal.valueOf(1000));
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

    private static Stream<Arguments> toCalendarParams() {
        return Stream.of(
                Arguments.of(new Date(1687622249729L)),
                Arguments.of(new java.sql.Date(1687622249729L)),
                Arguments.of(new Timestamp(1687622249729L)),
                Arguments.of(Instant.ofEpochMilli(1687622249729L)),
                Arguments.of(1687622249729L),
                Arguments.of(new BigInteger("1687622249729000000")),
                Arguments.of(BigDecimal.valueOf(1687622249.729)),
                Arguments.of("1687622249729"),
                Arguments.of(new AtomicLong(1687622249729L))
        );
    }

    @ParameterizedTest
    @MethodSource("toCalendarParams")
    void toCalendar(Object source)
    {
        Long epochMilli = 1687622249729L;

        Calendar calendar = this.converter.convert(source, Calendar.class);
        assertEquals(calendar.getTime().getTime(), epochMilli);

        // BigInteger to Calendar
        // Other direction --> Calendar to other date types

        Calendar now = Calendar.getInstance();
        
        // Calendar to Date
        calendar = this.converter.convert(now, Calendar.class);
        Date date = this.converter.convert(calendar, Date.class);
        assertEquals(calendar.getTime(), date);

        // Calendar to SqlDate
        java.sql.Date sqlDate = this.converter.convert(calendar, java.sql.Date.class);
        assertEquals(calendar.getTime().getTime(), sqlDate.getTime());

        // Calendar to Timestamp
        Timestamp timestamp = this.converter.convert(calendar, Timestamp.class);
        assertEquals(calendar.getTime().getTime(), timestamp.getTime());

        // Calendar to Long
        long tnow = this.converter.convert(calendar, long.class);
        assertEquals(calendar.getTime().getTime(), tnow);

        // Calendar to AtomicLong
        AtomicLong atomicLong = this.converter.convert(calendar, AtomicLong.class);
        assertEquals(calendar.getTime().getTime(), atomicLong.get());

        // Calendar to String
        String strDate = this.converter.convert(calendar, String.class);
        String strDate2 = this.converter.convert(now, String.class);
        assertEquals(strDate, strDate2);

        // Calendar to BigInteger
        BigInteger bigInt = this.converter.convert(calendar, BigInteger.class);
        assertEquals(now.getTime().getTime() * 1_000_000, bigInt.longValue());

        // Calendar to BigDecimal
        BigDecimal bigDec = this.converter.convert(calendar, BigDecimal.class);
        bigDec = bigDec.multiply(BigDecimal.valueOf(1000));
        assertEquals(now.getTime().getTime(), bigDec.longValue());
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
        map.put("date", "2023-12-23");
        LocalDate ld = converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        map.put("value", "2023-12-23");
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        map.put("_v", "2023-12-23");
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
    void testConvertStringToLocalDateTime_withParseError() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.converter.convert("2020-12-40", LocalDateTime.class))
                .withMessageContaining("Day must be between 1 and 31");
    }

    private static Stream<Arguments> unparseableDates() {
        return Stream.of(
                Arguments.of(" "),
                Arguments.of("")
        );
    }

    @ParameterizedTest
    @MethodSource("unparseableDates")
    void testUnparseableDates_Date(String date)
    {
        assertNull(this.converter.convert(date, Date.class));
    }

    @ParameterizedTest
    @MethodSource("unparseableDates")
    void testUnparseableDates_SqlDate(String date)
    {
        assertNull(this.converter.convert(date, java.sql.Date.class));
    }

    @ParameterizedTest
    @MethodSource("unparseableDates")
    void testUnparseableDates_Timestamp(String date)
    {
        assertNull(this.converter.convert(date, Timestamp.class));
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

    private static Stream<Arguments> toFloatParams() {
        return paramsForFloatingPointTypes(Float.MIN_VALUE, Float.MAX_VALUE);
    }

    @ParameterizedTest()
    @MethodSource("toFloatParams")
    void toFloat(Object initial, Number number)
    {
        float expected = number.floatValue();
        float f = this.converter.convert(initial, float.class);
        assertThat(f).isEqualTo(expected);
    }

    @ParameterizedTest()
    @MethodSource("toFloatParams")
    void toFloat_objectType(Object initial, Number number)
    {
        Float expected = number.floatValue();
        float f = this.converter.convert(initial, Float.class);
        assertThat(f).isEqualTo(expected);
    }

    private static Stream<Arguments> toFloat_illegalArguments() {
        return Stream.of(
                Arguments.of(TimeZone.getDefault(), "Unsupported conversion"),
                Arguments.of("45.6badNumber", "not parseable")
        );
    }

    @ParameterizedTest()
    @MethodSource("toFloat_illegalArguments")
    void testConvertToFloat_withIllegalArguments(Object initial, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(initial, float.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> toFloat_booleanArguments() {
        return Stream.of(
                Arguments.of(true, CommonValues.FLOAT_ONE),
                Arguments.of(false, CommonValues.FLOAT_ZERO),
                Arguments.of(Boolean.TRUE, CommonValues.FLOAT_ONE),
                Arguments.of(Boolean.FALSE, CommonValues.FLOAT_ZERO),
                Arguments.of(new AtomicBoolean(true), CommonValues.FLOAT_ONE),
                Arguments.of(new AtomicBoolean(false), CommonValues.FLOAT_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("toFloat_booleanArguments")
    void toFloat_withBooleanArguments_returnsCommonValue(Object initial, Float expected)
    {
        Float f = this.converter.convert(initial, Float.class);
        assertThat(f).isSameAs(expected);
    }

    @ParameterizedTest
    @MethodSource("toFloat_booleanArguments")
    void toFloat_withBooleanArguments_returnsCommonValueWhenPrimitive(Object initial, float expected)
    {
        float f = this.converter.convert(initial, float.class);
        assertThat(f).isEqualTo(expected);
    }


    private static Stream<Arguments> toDoubleParams() {
        return paramsForFloatingPointTypes(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("toDoubleParams")
    void testDouble(Object value, Number number)
    {
        double converted = this.converter.convert(value, double.class);
        assertThat(converted).isEqualTo(number.doubleValue());
    }

    @ParameterizedTest
    @MethodSource("toDoubleParams")
    void testDouble_ObjectType(Object value, Number number)
    {
        Double converted = this.converter.convert(value, Double.class);
        assertThat(converted).isEqualTo(number.doubleValue());
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
        assertEquals(true, converter.convert(new BigInteger("314159"), Boolean.class));
        assertEquals(true, converter.convert(-3.14d, boolean.class));
        assertEquals(false, converter.convert(0.0d, boolean.class));
        assertEquals(true, converter.convert(-3.14f, Boolean.class));
        assertEquals(false, converter.convert(0.0f, Boolean.class));

        assertEquals(false, converter.convert(new AtomicInteger(0), boolean.class));
        assertEquals(false, converter.convert(new AtomicLong(0), boolean.class));
        assertEquals(false, converter.convert(new AtomicBoolean(false), Boolean.class));
        assertEquals(true, converter.convert(new AtomicBoolean(true), Boolean.class));

        assertEquals(true, converter.convert("TRue", Boolean.class));
        assertEquals(true, converter.convert("true", Boolean.class));
        assertEquals(false, converter.convert("fALse", Boolean.class));
        assertEquals(false, converter.convert("false", Boolean.class));
        assertEquals(false, converter.convert("john", Boolean.class));

        assertEquals(true, converter.convert(true, Boolean.class));
        assertEquals(true, converter.convert(Boolean.TRUE, Boolean.class));
        assertEquals(false, converter.convert(false, Boolean.class));
        assertEquals(false, converter.convert(Boolean.FALSE, Boolean.class));

        try
        {
            converter.convert(new Date(), Boolean.class);
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
        assert (converter.convert(-3.14d, AtomicBoolean.class)).get();
        assert !(converter.convert(0.0d, AtomicBoolean.class)).get();
        assert (converter.convert(-3.14f, AtomicBoolean.class)).get();
        assert !(converter.convert(0.0f, AtomicBoolean.class)).get();

        assert !(converter.convert(new AtomicInteger(0), AtomicBoolean.class)).get();
        assert !(converter.convert(new AtomicLong(0), AtomicBoolean.class)).get();
        assert !(converter.convert(new AtomicBoolean(false), AtomicBoolean.class)).get();
        assert (converter.convert(new AtomicBoolean(true), AtomicBoolean.class)).get();

        assert (converter.convert("TRue", AtomicBoolean.class)).get();
        assert !(converter.convert("fALse", AtomicBoolean.class)).get();
        assert !(converter.convert("john", AtomicBoolean.class)).get();

        assert (converter.convert(true, AtomicBoolean.class)).get();
        assert (converter.convert(Boolean.TRUE, AtomicBoolean.class)).get();
        assert !(converter.convert(false, AtomicBoolean.class)).get();
        assert !(converter.convert(Boolean.FALSE, AtomicBoolean.class)).get();

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = converter.convert(b1, AtomicBoolean.class);
        assert b1 != b2; // ensure that it returns a different but equivalent instance
        assert b1.get() == b2.get();

        try {
            converter.convert(new Date(), AtomicBoolean.class);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [date"));
        }
    }

    @Test
    void testMapToAtomicBoolean()
    {
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to AtomicBoolean the map must include one of the following");
    }

    @Test
    void testMapToAtomicInteger()
    {
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to AtomicInteger the map must include one of the following");
    }

    @Test
    void testMapToAtomicLong()
    {
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to AtomicLong the map must include one of the following");
    }
    
    @ParameterizedTest
    @MethodSource("toCalendarParams")
    void testMapToCalendar(Object value)
    {
        final Map map = new HashMap<>();
        map.put("value", value);

        Calendar cal = this.converter.convert(map, Calendar.class);
        assertThat(cal).isNotNull();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Calendar.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to Calendar the map must include one of the following: [date, time, zone]");
    }

    @Test
    void testMapToCalendarWithTimeZone()
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.clear();
        cal.setTimeInMillis(now);
//        System.out.println("cal = " + cal.getTime());

        ZonedDateTime zdt = cal.toInstant().atZone(cal.getTimeZone().toZoneId());
//        System.out.println("zdt = " + zdt);
        
        final Map map = new HashMap<>();
        map.put("date", zdt.toLocalDate());
        map.put("time", zdt.toLocalTime());
        map.put("zone", cal.getTimeZone().toZoneId());
//        System.out.println("map = " + map);

        Calendar newCal = this.converter.convert(map, Calendar.class);
//        System.out.println("newCal = " + newCal.getTime());
        assertEquals(cal, newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToCalendarWithTimeNoZone()
    {
        TimeZone tz = TimeZone.getDefault();
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(tz);
        cal.setTimeInMillis(now);

        Instant instant = Instant.ofEpochMilli(now);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, tz.toZoneId());

        final Map map = new HashMap<>();
        map.put("date", zdt.toLocalDate());
        map.put("time", zdt.toLocalTime());
        Calendar newCal = this.converter.convert(map, Calendar.class);
        assert cal.equals(newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToGregCalendar()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("Map to Calendar the map must include one of the following: [date, time, zone]");
    }

    @Test
    void testMapToDate() {

        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to Date the map must include one of the following");
    }

    @Test
    void testMapToSqlDate()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to java.sql.Date the map must include");
    }

    @Test
    void testMapToTimestamp()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to Timestamp the map must include one of the following");
    }

    @Test
    void testMapToLocalDate()
    {
        LocalDate today = LocalDate.now();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to LocalDate the map must include one of the following: [date], [_v], or [value] with associated values");
    }

    @Test
    void testMapToLocalDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
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
                .hasMessageContaining("To convert from Map to LocalDateTime the map must include one of the following: [date, time], [_v], or [value] with associated values");
    }

    @Test
    void testMapToZonedDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap<>();
        map.put("value", now);
        ZonedDateTime zd = this.converter.convert(map, ZonedDateTime.class);
        assert zd.toInstant().toEpochMilli() == now;

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, ZonedDateTime.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, ZonedDateTime.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To convert from Map to ZonedDateTime the map must include one of the following: [zone, dateTime], [_v], or [value] with associated values");

    }

    private static Stream<Arguments> classesThatReturnZero_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(byte.class, CommonValues.BYTE_ZERO),
                Arguments.of(int.class, CommonValues.INTEGER_ZERO),
                Arguments.of(short.class, CommonValues.SHORT_ZERO),
                Arguments.of(char.class, CommonValues.CHARACTER_ZERO),
                Arguments.of(long.class, CommonValues.LONG_ZERO),
                Arguments.of(float.class, CommonValues.FLOAT_ZERO),
                Arguments.of(double.class, CommonValues.DOUBLE_ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource("classesThatReturnZero_whenConvertingFromNull")
    void testClassesThatReturnZero_whenConvertingFromNull(Class c, Object expected)
    {
        Object zero = this.converter.convert(null, c);
        assertThat(zero).isSameAs(expected);
    }

    private static Stream<Arguments> classesThatReturnFalse_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(Boolean.class),
                Arguments.of(boolean.class)
        );
    }

    @Test
    void testConvertFromNullToBoolean() {
        assertThat(this.converter.convert(null, boolean.class)).isFalse();
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

    private static Stream<Arguments> toCharacterParams() {
        return Stream.of(
                Arguments.of((byte)65),
                Arguments.of((short)65),
                Arguments.of(65),
                Arguments.of(65L),
                Arguments.of(65.0),
                Arguments.of(65.0d),
                Arguments.of(Byte.valueOf("65")),
                Arguments.of(Short.valueOf("65")),
                Arguments.of(Integer.valueOf("65")),
                Arguments.of(Long.valueOf("65")),
                Arguments.of(Float.valueOf("65")),
                Arguments.of(Double.valueOf("65")),
                Arguments.of(BigInteger.valueOf(65)),
                Arguments.of(BigDecimal.valueOf(65)),
                Arguments.of('A'),
                Arguments.of("A")
        );
    }

    @ParameterizedTest
    @MethodSource("toCharacterParams")
    void toCharacter_ObjectType(Object source) {
        Character ch = this.converter.convert(source, Character.class);
        assertThat(ch).isEqualTo('A');

        Object roundTrip = this.converter.convert(ch, source.getClass());
        assertThat(source).isEqualTo(roundTrip);
    }

    @ParameterizedTest
    @MethodSource("toCharacterParams")
    void toCharacter(Object source) {
        char ch = this.converter.convert(source, char.class);
        assertThat(ch).isEqualTo('A');

        Object roundTrip = this.converter.convert(ch, source.getClass());
        assertThat(source).isEqualTo(roundTrip);
    }

    @Test
    void toCharacterMiscellaneous() {
        assertThat(this.converter.convert('z', char.class)).isEqualTo('z');
    }

    @Test
    void toCharacter_whenStringIsLongerThanOneCharacter_AndIsANumber() {
        char ch = this.converter.convert("65", char.class);
        assertThat(ch).isEqualTo('A');
    }

    private static Stream<Arguments> toChar_illegalArguments() {
        return Stream.of(
                Arguments.of(TimeZone.getDefault(), "Unsupported conversion"),
                Arguments.of(Integer.MAX_VALUE, "out of range to be converted to character")
        );
    }

    @ParameterizedTest()
    @MethodSource("toChar_illegalArguments")
    void testConvertTCharacter_withIllegalArguments(Object initial, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(initial, Character.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> toChar_numberFormatException() {
        return Stream.of(
                Arguments.of("45.number", "Unable to parse '45.number' as a Character"),
                Arguments.of("AB", "Unable to parse 'AB' as a Character")
        );
    }

    @ParameterizedTest()
    @MethodSource("toChar_numberFormatException")
    void testConvertTCharacter_withNumberFormatExceptions(Object initial, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(initial, Character.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> trueValues() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(Boolean.TRUE),
                Arguments.of(new AtomicBoolean(true))
        );
    }


    @ParameterizedTest
    @MethodSource("trueValues")
    void toCharacter_whenTrue_withDefaultOptions_returnsCommonValue(Object source)
    {
        assertThat(this.converter.convert(source, char.class)).isSameAs(CommonValues.CHARACTER_ONE);
    }

    @ParameterizedTest
    @MethodSource("trueValues")
    void toCharacter_whenTrue_withDefaultOptions_andObjectType_returnsCommonValue(Object source)
    {
        assertThat(this.converter.convert(source, Character.class)).isSameAs(CommonValues.CHARACTER_ONE);
    }

    @ParameterizedTest
    @MethodSource("trueValues")
    void toCharacter_whenTrue_withCustomOptions_returnsTrueCharacter(Object source)
    {
        Converter converter = new Converter(TF_OPTIONS);
        assertThat(converter.convert(source, Character.class)).isEqualTo('T');

        converter = new Converter(YN_OPTIONS);
        assertThat(converter.convert(source, Character.class)).isEqualTo('Y');
    }


    private static final ConverterOptions TF_OPTIONS = createCustomBooleanCharacter('T', 'F');
    private static final ConverterOptions YN_OPTIONS = createCustomBooleanCharacter('Y', 'N');

    private static Stream<Arguments> falseValues() {
        return Stream.of(
                Arguments.of(false),
                Arguments.of(Boolean.FALSE),
                Arguments.of(new AtomicBoolean(false))
        );
    }

    @ParameterizedTest
    @MethodSource("falseValues")
    void toCharacter_whenFalse_withDefaultOptions_returnsCommonValue(Object source)
    {
        assertThat(this.converter.convert(source, char.class)).isSameAs(CommonValues.CHARACTER_ZERO);
    }

    @ParameterizedTest
    @MethodSource("falseValues")
    void toCharacter_whenFalse_withDefaultOptions_andObjectType_returnsCommonValue(Object source)
    {
        assertThat(this.converter.convert(source, Character.class)).isSameAs(CommonValues.CHARACTER_ZERO);
    }

    @ParameterizedTest
    @MethodSource("falseValues")
    void toCharacter_whenFalse_withCustomOptions_returnsTrueCharacter(Object source)
    {
        Converter converter = new Converter(TF_OPTIONS);
        assertThat(converter.convert(source, Character.class)).isEqualTo('F');

        converter = new Converter(YN_OPTIONS);
        assertThat(converter.convert(source, Character.class)).isEqualTo('N');
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
        assert big.longValue() * 1000 == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime() * 1_000_000;

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
        assert big.multiply(BigDecimal.valueOf(1000L)).longValue() == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime() * 1_000_000;

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
        assert bigInt.toString().equals("340282366920938463463374607431768211455");

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
        assert bigDec.toString().equals("340282366920938463463374607431768211455");

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
                .hasMessageContaining("To convert from Map to UUID the map must include one of the following");
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
        assertEquals(map.get(VALUE), (byte)16);
        assert map.get(VALUE).getClass().equals(Byte.class);

        Byte b2 = (byte) 16;
        map = this.converter.convert(b2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), (byte)16);
        assert map.get(VALUE).getClass().equals(Byte.class);
    }

    @Test
    void testShortToMap()
    {
        short s1 = (short) 1600;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), (short)1600);
        assert map.get(VALUE).getClass().equals(Short.class);

        Short s2 = (short) 1600;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), (short)1600);
        assert map.get(VALUE).getClass().equals(Short.class);
    }

    @Test
    void testIntegerToMap()
    {
        int s1 = 1234567;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 1234567);
        assert map.get(VALUE).getClass().equals(Integer.class);

        Integer s2 = 1234567;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 1234567);
        assert map.get(VALUE).getClass().equals(Integer.class);
    }

    @Test
    void testLongToMap()
    {
        long s1 = 123456789012345L;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 123456789012345L);
        assert map.get(VALUE).getClass().equals(Long.class);

        Long s2 = 123456789012345L;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 123456789012345L);
        assert map.get(VALUE).getClass().equals(Long.class);
    }

    @Test
    void testFloatToMap()
    {
        float s1 = 3.141592f;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 3.141592f);
        assert map.get(VALUE).getClass().equals(Float.class);

        Float s2 = 3.141592f;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 3.141592f);
        assert map.get(VALUE).getClass().equals(Float.class);
    }

    @Test
    void testDoubleToMap()
    {
        double s1 = 3.14159265358979d;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 3.14159265358979d);
        assert map.get(VALUE).getClass().equals(Double.class);

        Double s2 = 3.14159265358979d;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 3.14159265358979d);
        assert map.get(VALUE).getClass().equals(Double.class);
    }

    @Test
    void testBooleanToMap()
    {
        boolean s1 = true;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), true);
        assert map.get(VALUE).getClass().equals(Boolean.class);

        Boolean s2 = true;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), true);
        assert map.get(VALUE).getClass().equals(Boolean.class);
    }

    @Test
    void testCharacterToMap()
    {
        char s1 = 'e';
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 'e');
        assert map.get(VALUE).getClass().equals(Character.class);

        Character s2 = 'e';
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), 'e');
        assert map.get(VALUE).getClass().equals(Character.class);
    }

    @Test
    void testBigIntegerToMap()
    {
        BigInteger bi = BigInteger.valueOf(1234567890123456L);
        Map<?, ?> map = this.converter.convert(bi, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), bi);
        assert map.get(VALUE).getClass().equals(BigInteger.class);
    }

    @Test
    void testBigDecimalToMap()
    {
        BigDecimal bd = new BigDecimal("3.1415926535897932384626433");
        Map<?, ?> map = this.converter.convert(bd, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), bd);
        assert map.get(VALUE).getClass().equals(BigDecimal.class);
    }

    @Test
    void testAtomicBooleanToMap()
    {
        AtomicBoolean ab = new AtomicBoolean(true);
        Map<?, ?> map = this.converter.convert(ab, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), ab);
        assert map.get(VALUE).getClass().equals(AtomicBoolean.class);
    }

    @Test
    void testAtomicIntegerToMap()
    {
        AtomicInteger ai = new AtomicInteger(123456789);
        Map<?, ?> map = this.converter.convert(ai, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), ai);
        assert map.get(VALUE).getClass().equals(AtomicInteger.class);
    }

    @Test
    void testAtomicLongToMap()
    {
        AtomicLong al = new AtomicLong(12345678901234567L);
        Map<?, ?> map = this.converter.convert(al, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), al);
        assert map.get(VALUE).getClass().equals(AtomicLong.class);
    }

    @Test
    void testClassToMap()
    {
        Class<?> clazz = ConverterTest.class;
        Map<?, ?> map = this.converter.convert(clazz, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), clazz);
    }

    @Test
    void testUUIDToMap()
    {
        UUID uuid = new UUID(1L, 2L);
        Map<?, ?> map = this.converter.convert(uuid, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), uuid);
        assert map.get(VALUE).getClass().equals(UUID.class);
    }

    @Test
    void testCalendarToMap()
    {
        Calendar cal = Calendar.getInstance();
        Map<?, ?> map = this.converter.convert(cal, Map.class);
        assert map.size() == 3; // date, time, zone
    }

    @Test
    void testDateToMap()
    {
        Date now = new Date();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 4;    // date, time, zone, epochMillis
        assertEquals(map.get(MapConversions.EPOCH_MILLIS), now.getTime());
        assert map.get(MapConversions.EPOCH_MILLIS).getClass().equals(Long.class);
    }

    @Test
    void testSqlDateToMap()
    {
        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 4;    // date, time, zone, epochMillis
        assertEquals(map.get(MapConversions.EPOCH_MILLIS), now.getTime());
        assert map.get(MapConversions.EPOCH_MILLIS).getClass().equals(Long.class);
    }

    @Test
    void testTimestampToMap()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 5;  // date, time, zone, epoch_mills, nanos
        assertEquals(map.get(EPOCH_MILLIS), now.getTime());
        assert map.get(EPOCH_MILLIS).getClass().equals(Long.class);
    }

    @Test
    void testLocalDateToMap()
    {
        LocalDate now = LocalDate.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(DATE), now.toString());
        assert map.get(DATE).getClass().equals(String.class);
    }

    @Test
    void testLocalDateTimeToMap()
    {
        LocalDateTime now = LocalDateTime.now();
        Map<?, ?> map = converter.convert(now, Map.class);
        assert map.size() == 2; // date, time
        LocalDateTime now2 = converter.convert(map, LocalDateTime.class);
        assertEquals(now, now2);
    }

    @Test
    void testZonedDateTimeToMap()
    {
        ZonedDateTime now = ZonedDateTime.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(VALUE), now);
        assert map.get(VALUE).getClass().equals(ZonedDateTime.class);
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
        assert !this.converter.isConversionSupportedFor(int.class, LocalDate.class);
        assert !this.converter.isConversionSupportedFor(Integer.class, LocalDate.class);

        assert !this.converter.isDirectConversionSupportedFor(byte.class, LocalDate.class);
        assert !this.converter.isConversionSupportedFor(byte.class, LocalDate.class);

        assert !this.converter.isConversionSupportedFor(Byte.class, LocalDate.class);
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
        this.converter.addConversion(DumbNumber.class, UUID.class, (fromInstance, converter) -> {
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
        this.converter.addConversion(UUID.class, DumbNumber.class, (fromInstance, converter) -> {
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
        this.converter.addConversion(UUID.class, boolean.class, (fromInstance, converter) -> {
            UUID uuid1 = (UUID) fromInstance;
            return !"00000000-0000-0000-0000-000000000000".equals(uuid1.toString());
        });

        // Add in conversions
        this.converter.addConversion(boolean.class, UUID.class, (fromInstance, converter) -> {
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
        this.converter.addConversion(Normie.class, Weirdo.class, (fromInstance, converter) -> {
            Normie normie = (Normie) fromInstance;
            Weirdo weirdo = new Weirdo(normie.name);
            return weirdo;
        });

        this.converter.addConversion(Weirdo.class, Normie.class, (fromInstance, converter) -> {
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

    private static Stream<Arguments> emptyStringTypes_withSameAsReturns() {
        return Stream.of(
                Arguments.of("", byte.class, CommonValues.BYTE_ZERO),
                Arguments.of("", Byte.class, CommonValues.BYTE_ZERO),
                Arguments.of("", short.class, CommonValues.SHORT_ZERO),
                Arguments.of("", Short.class, CommonValues.SHORT_ZERO),
                Arguments.of("", int.class, CommonValues.INTEGER_ZERO),
                Arguments.of("", Integer.class, CommonValues.INTEGER_ZERO),
                Arguments.of("", long.class, CommonValues.LONG_ZERO),
                Arguments.of("", Long.class, CommonValues.LONG_ZERO),
                Arguments.of("", float.class, CommonValues.FLOAT_ZERO),
                Arguments.of("", Float.class, CommonValues.FLOAT_ZERO),
                Arguments.of("", double.class, CommonValues.DOUBLE_ZERO),
                Arguments.of("", Double.class, CommonValues.DOUBLE_ZERO),
                Arguments.of("", boolean.class, Boolean.FALSE),
                Arguments.of("", Boolean.class, Boolean.FALSE),
                Arguments.of("", char.class, CommonValues.CHARACTER_ZERO),
                Arguments.of("", Character.class, CommonValues.CHARACTER_ZERO),
                Arguments.of("", BigDecimal.class, BigDecimal.ZERO),
                Arguments.of("", BigInteger.class, BigInteger.ZERO),
                Arguments.of("", String.class, EMPTY),
                Arguments.of("", byte[].class, EMPTY_BYTE_ARRAY),
                Arguments.of("", char[].class, EMPTY_CHAR_ARRAY)
        );
    }

    @ParameterizedTest
    @MethodSource("emptyStringTypes_withSameAsReturns")
    void testEmptyStringToType_whereTypeReturnsSpecificObject(Object value, Class<?> type, Object expected)
    {
        Object converted = this.converter.convert(value, type);
        assertThat(converted).isSameAs(expected);
    }

    private static Stream<Arguments> emptyStringTypes_notSameObject() {
        return Stream.of(
                Arguments.of("", ByteBuffer.class, ByteBuffer.wrap(EMPTY_BYTE_ARRAY)),
                Arguments.of("", CharBuffer.class, CharBuffer.wrap(EMPTY_CHAR_ARRAY))
        );
    }

    @ParameterizedTest
    @MethodSource("emptyStringTypes_notSameObject")
    void testEmptyStringToType_whereTypeIsEqualButNotSameAs(Object value, Class<?> type, Object expected)
    {
        Object converted = this.converter.convert(value, type);
        assertThat(converted).isNotSameAs(expected);
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

    private static Stream<Arguments> stringToByteArrayParams() {
        return Stream.of(
                Arguments.of("$1,000", StandardCharsets.US_ASCII, new byte[] { 36, 49, 44, 48, 48, 48 }),
                Arguments.of("$1,000", StandardCharsets.ISO_8859_1, new byte[] { 36, 49, 44, 48, 48, 48 }),
                Arguments.of("$1,000", StandardCharsets.UTF_8, new byte[] { 36, 49, 44, 48, 48, 48 }),
                Arguments.of("£1,000", StandardCharsets.ISO_8859_1, new byte[] { -93, 49, 44, 48, 48, 48 }),
                Arguments.of("£1,000", StandardCharsets.UTF_8, new byte[] { -62, -93, 49, 44, 48, 48, 48 }),
                Arguments.of("€1,000", StandardCharsets.UTF_8, new byte[] { -30, -126, -84, 49, 44, 48, 48, 48 })
        );
    }

    private static Stream<Arguments> stringToCharArrayParams() {
        return Stream.of(
                Arguments.of("$1,000", StandardCharsets.US_ASCII, new char[] { '$', '1', ',', '0', '0', '0' }),
                Arguments.of("$1,000", StandardCharsets.ISO_8859_1, new char[] { '$', '1', ',', '0', '0', '0' }),
                Arguments.of("$1,000", StandardCharsets.UTF_8, new char[] { '$', '1', ',', '0', '0', '0' }),
                Arguments.of("£1,000", StandardCharsets.ISO_8859_1, new char[] { '£', '1', ',', '0', '0', '0' }),
                Arguments.of("£1,000", StandardCharsets.UTF_8, new char[] { '£', '1', ',', '0', '0', '0' }),
                Arguments.of("€1,000", StandardCharsets.UTF_8, new char[] { '€', '1', ',', '0', '0', '0' })
        );
    }

    @ParameterizedTest
    @MethodSource("stringToByteArrayParams")
    void testStringToByteArray(String source, Charset charSet, byte[] expected) {
        Converter converter = new Converter(createCharsetOptions(charSet));
        byte[] actual = converter.convert(source, byte[].class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("stringToByteArrayParams")
    void testStringToByteBuffer(String source, Charset charSet, byte[] expected) {
        Converter converter = new Converter(createCharsetOptions(charSet));
        ByteBuffer actual = converter.convert(source, ByteBuffer.class);
        assertThat(actual).isEqualTo(ByteBuffer.wrap(expected));
    }

    @ParameterizedTest
    @MethodSource("stringToByteArrayParams")
    void testByteArrayToString(String expected, Charset charSet, byte[] source) {
        Converter converter = new Converter(createCharsetOptions(charSet));
        String actual = converter.convert(source, String.class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("stringToCharArrayParams")
    void testCharArrayToString(String expected, Charset charSet, char[] source) {
        Converter converter = new Converter(createCharsetOptions(charSet));
        String actual = converter.convert(source, String.class);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("stringToCharArrayParams")
    void testStringToCharArray(String source, Charset charSet, char[] expected) {
        Converter converter = new Converter(createCharsetOptions(charSet));
        char[] actual = converter.convert(source, char[].class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testTimestampAndOffsetDateTimeSymmetry()
    {
        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
        Instant instant1 = ts1.toInstant();

        OffsetDateTime odt = converter.convert(ts1, OffsetDateTime.class);
        Instant instant2 = odt.toInstant();

        assertEquals(instant1, instant2);

        Timestamp ts2 = converter.convert(odt, Timestamp. class);
        assertEquals(ts1, ts2);
    }

    @Test
    void testKnownUnsupportedConversions() {
        assertThatThrownBy(() -> converter.convert((byte)50, Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion");

        assertThatThrownBy(() -> converter.convert((short)300, Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion");

        assertThatThrownBy(() -> converter.convert(100000, Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion");
    }

    private ConverterOptions createCharsetOptions(final Charset charset) {
        return new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public Charset getCharset () {
                return charset;
            }
        };
    }
    
    private ConverterOptions createCustomZones(final ZoneId targetZoneId) {
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

    private static ConverterOptions createCustomBooleanCharacter(final Character trueChar, final Character falseChar) {
        return new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public Character trueChar() {
                return trueChar;
            }

            @Override
            public Character falseChar() {
                return falseChar;
            }
        };
    }
    
    private ConverterOptions chicagoZone() { return createCustomZones(CHICAGO); }
}
