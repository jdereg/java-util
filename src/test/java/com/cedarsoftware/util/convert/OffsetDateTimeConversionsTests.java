package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class OffsetDateTimeConversionsTests {

    private Converter converter;

    @BeforeEach
    public void beforeEach() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    // epoch milli 1687622249729L
    private static Stream<Arguments> offsetDateTime_asString_withMultipleOffsets_sameEpochMilli() {
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
    @MethodSource("offsetDateTime_asString_withMultipleOffsets_sameEpochMilli")
    void toLong_differentZones_sameEpochMilli(String input) {
        OffsetDateTime initial = OffsetDateTime.parse(input);
        long actual = converter.convert(initial, long.class);
        assertThat(actual).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_asString_withMultipleOffsets_sameEpochMilli")
    void toDate_differentZones_sameEpochMilli(String input) {
        OffsetDateTime initial = OffsetDateTime.parse(input);
        Date actual = converter.convert(initial, Date.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_asString_withMultipleOffsets_sameEpochMilli")
    void toSqlDate_differentZones_sameEpochMilli(String input) {
        OffsetDateTime initial = OffsetDateTime.parse(input);
        java.sql.Date actual = converter.convert(initial, java.sql.Date.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_asString_withMultipleOffsets_sameEpochMilli")
    void toTimestamp_differentZones_sameEpochMilli(String input) {
        OffsetDateTime initial = OffsetDateTime.parse(input);
        Timestamp actual = converter.convert(initial, Timestamp.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }

    // epoch milli 1687622249729L
    private static Stream<Arguments> offsetDateTime_withMultipleOffset_sameEpochMilli() {
        return Stream.of(
                Arguments.of(OffsetDateTime.of(2023, 06, 25, 0, 57, 29, 729000000, ZoneOffset.of("+09:00"))),
                Arguments.of(OffsetDateTime.of(2023, 06, 24, 17, 57, 29, 729000000, ZoneOffset.of("+02:00"))),
                Arguments.of(OffsetDateTime.of(2023, 06, 24, 15, 57, 29, 729000000, ZoneOffset.of("Z"))),
                Arguments.of(OffsetDateTime.of(2023, 06, 24, 11, 57, 29, 729000000, ZoneOffset.of("-04:00"))),
                Arguments.of(OffsetDateTime.of(2023, 06, 24, 10, 57, 29, 729000000, ZoneOffset.of("-05:00"))),
                Arguments.of(OffsetDateTime.of(2023, 06, 24, 8, 57, 29, 729000000, ZoneOffset.of("-07:00")))
        );
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_withMultipleOffset_sameEpochMilli")
    void toLong_differentZones_sameEpochMilli(OffsetDateTime initial) {
        long actual = converter.convert(initial, long.class);
        assertThat(actual).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_withMultipleOffset_sameEpochMilli")
    void toDate_differentZones_sameEpochMilli(OffsetDateTime initial) {
        Date actual = converter.convert(initial, Date.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_withMultipleOffset_sameEpochMilli")
    void toSqlDate_differentZones_sameEpochMilli(OffsetDateTime initial) {
        java.sql.Date actual = converter.convert(initial, java.sql.Date.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTime_withMultipleOffset_sameEpochMilli")
    void toTimestamp_differentZones_sameEpochMilli(OffsetDateTime initial) {
        Timestamp actual = converter.convert(initial, Timestamp.class);
        assertThat(actual.getTime()).isEqualTo(1687622249729L);
    }



}
