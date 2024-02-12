package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.cedarsoftware.util.DeepEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
