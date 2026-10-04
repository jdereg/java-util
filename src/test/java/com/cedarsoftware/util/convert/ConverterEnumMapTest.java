package com.cedarsoftware.util.convert;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An enum converted to a Map converts back to that enum.
 */
class ConverterEnumMapTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * Enum to Map gives {"name": "SECONDS"}. Map to an enum read the Map's keys as the names of an EnumSet's members,
     * so the way back failed with "No enum constant java.util.concurrent.TimeUnit.name". A Map whose one entry is
     * "name" now gives that constant.
     */
    @Test
    void anEnumRoundTripsThroughItsMap() {
        for (TimeUnit unit : TimeUnit.values()) {
            Map<?, ?> map = converter.convert(unit, Map.class);
            assertEquals(unit, converter.convert(map, TimeUnit.class));
        }
        Map<String, Object> named = new LinkedHashMap<>();
        named.put("name", "DAYS");
        assertEquals(TimeUnit.DAYS, converter.convert(named, TimeUnit.class));
    }

    /** Any other Map still gives the EnumSet its keys name. */
    @Test
    void otherMapsStillGiveAnEnumSet() {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("SECONDS", 1);
        keys.put("DAYS", 2);
        assertEquals(EnumSet.of(TimeUnit.SECONDS, TimeUnit.DAYS), converter.convert(keys, TimeUnit.class));
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("HOURS", "name");
        assertEquals(EnumSet.of(TimeUnit.HOURS), converter.convert(one, TimeUnit.class));
    }
}
