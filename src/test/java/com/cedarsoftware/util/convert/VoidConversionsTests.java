package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
public class VoidConversionsTests {

    private Converter converter;

    @BeforeEach
    public void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> classesThatReturnNull_whenConvertingFromNull() {
        return Stream.of(
                Arguments.of(char[].class),
                Arguments.of(byte[].class),
                Arguments.of(Character[].class),
                Arguments.of(CharBuffer.class),
                Arguments.of(ByteBuffer.class),
                Arguments.of(Class.class),
                Arguments.of(String.class),
                Arguments.of(StringBuffer.class),
                Arguments.of(StringBuilder.class),
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
                Arguments.of(Instant.class),
                Arguments.of(Date.class),
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
    @MethodSource("classesThatReturnNull_whenConvertingFromNull")
    void testClassesThatReturnNull_whenConvertingFromNull(Class<?> c)
    {
        assertThat(this.converter.convert(null, c)).isNull();
    }
}
