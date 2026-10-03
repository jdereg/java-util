package com.cedarsoftware.util;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every zone DateUtilities' abbreviation table names must exist on the JDK it runs on. EET and EEST named Europe/Kyiv,
 * an ID the time-zone data gained in 2022 (2022b); a JDK built with older data knows only Europe/Kiev, and there "EET"
 * failed to parse ("Invalid timezone abbreviation"). The table now takes Europe/Kyiv where the JDK knows it and
 * Europe/Kiev otherwise. The JDKs this suite runs on all know Kyiv, so the choice itself is tested with the set of
 * known IDs an older JDK would report.
 */
class DateUtilitiesZoneTableTest {

    @Test
    void everyZoneTheTableNamesExistsHere() {
        for (Map.Entry<String, String> entry : DateUtilities.ABBREVIATION_TO_TIMEZONE.entrySet()) {
            assertTrue(ZoneId.getAvailableZoneIds().contains(entry.getValue()), entry.toString());
        }
    }

    @Test
    void eetAndEestParse() {
        ZonedDateTime winter = DateUtilities.parseDate("2024-01-15 12:00 EET", ZoneId.of("UTC"), true);
        ZonedDateTime summer = DateUtilities.parseDate("2024-07-04 12:00 EEST", ZoneId.of("UTC"), true);
        assertEquals(ZoneOffset.ofHours(2), winter.getOffset());
        assertEquals(ZoneOffset.ofHours(3), summer.getOffset());
        assertTrue(winter.getZone().getId().startsWith("Europe/Ky") || winter.getZone().getId().equals("Europe/Kiev"));
    }

    @Test
    void kyivWhereTheJdkKnowsItKievWhereItDoesNot() {
        Set<String> olderData = new HashSet<>(Arrays.asList("Europe/Kiev", "Europe/London"));
        Set<String> newerData = new HashSet<>(Arrays.asList("Europe/Kiev", "Europe/Kyiv", "Europe/London"));
        assertEquals("Europe/Kiev", DateUtilities.firstKnownZone(olderData, "Europe/Kyiv", "Europe/Kiev"));
        assertEquals("Europe/Kyiv", DateUtilities.firstKnownZone(newerData, "Europe/Kyiv", "Europe/Kiev"));
    }
}
