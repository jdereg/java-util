package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import com.cedarsoftware.util.DeepEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
class ZonedDateTimeConversionsTests {

    private Converter converter;


    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId ALASKA = ZoneId.of("America/Anchorage");

    private static final ZonedDateTime ZDT_1 = ZonedDateTime.of(LocalDateTime.of(2019, 12, 15, 9, 7, 16, 2000), CHICAGO);
    private static final ZonedDateTime ZDT_2 = ZonedDateTime.of(LocalDateTime.of(2027, 12, 23, 9, 7, 16, 2000), TOKYO);
    private static final ZonedDateTime ZDT_3 = ZonedDateTime.of(LocalDateTime.of(2027, 12, 23, 9, 7, 16, 2000), ALASKA);

    @BeforeEach
    public void before() {
        // create converter with default options
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> roundTripZDT() {
        return Stream.of(
                Arguments.of(ZDT_1),
                Arguments.of(ZDT_2),
                Arguments.of(ZDT_3)
        );
    }

    @ParameterizedTest
    @MethodSource("roundTripZDT")
    void testZonedDateTime(ZonedDateTime zdt) {

        String value = this.converter.convert(zdt, String.class);
        ZonedDateTime actual = this.converter.convert(value, ZonedDateTime.class);

        assertTrue(DeepEquals.deepEquals(actual, zdt));

        value = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zdt);
        actual = this.converter.convert(value, ZonedDateTime.class);

        assertTrue(DeepEquals.deepEquals(actual, zdt));
    }
}
