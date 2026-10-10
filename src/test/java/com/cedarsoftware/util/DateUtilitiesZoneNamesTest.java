package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What Date.toString() prints reads back as the instant it printed, on every JDK.
 * <ul>
 *   <li>A zone with no abbreviation prints as {@code GMT-03:00}, and DateUtilities refused it. From JDK 23 the JDK's
 *       zone names come from CLDR alone, so that is most of the world's zones (200 of 545 regions on JDK 25, 8 on
 *       JDK 21).</li>
 *   <li>{@code AST}, {@code NST} and {@code SST} were not in DateUtilities' table, so they went to the JDK's old
 *       three-letter IDs, which give those names to other zones: Puerto Rico's {@code AST} (Atlantic, -04:00) read
 *       as Alaska, Newfoundland's {@code NST} (-03:30) as New Zealand, Samoa's {@code SST} (-11:00) as the Solomon
 *       Islands. They are in the table now, with ADT and NDT.</li>
 *   <li>An abbreviation outside the table is found among the names the JDK gives its zones, and the JDK gives every
 *       zone a daylight name, used or not: East Africa's unused EAST took the name from Easter Island, which prints
 *       it (JDK 22 and earlier). A daylight name now counts only for a zone that keeps daylight time.</li>
 *   <li>The old three-letter IDs that stay (ECT, CTT, BET...) keep the JDK's zones, but are read from
 *       {@code ZoneId.SHORT_IDS}: {@code TimeZone.getTimeZone()} gives the same zones and, from JDK 24 on, prints a
 *       deprecation warning on the console for each lookup.</li>
 * </ul>
 * The oracle is the JDK's own printing: every region's Date.toString() text, at two instants, must read back exactly,
 * unless its abbreviation names regions at more than one offset at that instant (CST is Chicago and China: the
 * table chooses), or is one of the JDK's old three-letter IDs outside the table (on JDK 21 Ecuador prints ECT, which
 * the JDK's ID, kept, says is Paris).
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
class DateUtilitiesZoneNamesTest {

    // 2024-01-15T10:30Z and 2024-07-04T12:30Z: winter and summer in both hemispheres
    private static final long[] INSTANTS = {1705314600000L, 1720096200000L};
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static String dateToString(long millis, String zone) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(zone));
        return format.format(new Date(millis));
    }

    private static long parsedMillis(String text) {
        return DateUtilities.parseDate(text, UTC, true).toInstant().toEpochMilli();
    }

    @Test
    void aGmtOffsetReadsAsDateToStringPrintsIt() {
        assertEquals(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli(),
                parsedMillis("Mon Jan 15 07:30:00 GMT-03:00 2024"));
        assertEquals(Instant.parse("2024-07-04T12:30:00Z").toEpochMilli(),
                parsedMillis("Thu Jul 04 18:00:00 GMT+05:30 2024"));
        assertEquals(Instant.parse("2024-07-04T12:30:00Z").toEpochMilli(),
                parsedMillis("Jul 04 18:00:00 UTC+05:30 2024"));
        Converter converter = new Converter(new DefaultConverterOptions());
        assertEquals(new Date(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli()),
                converter.convert("Mon Jan 15 07:30:00 GMT-03:00 2024", Date.class));
    }

    @Test
    void abbreviationsMeanTheZonesDateToStringPrintsThemFor() {
        assertEquals(ZoneOffset.ofHours(-4), DateUtilities.parseDate("Mon Jan 15 06:30:00 AST 2024", UTC, true)
                .getOffset(), "Atlantic (Puerto Rico), not Alaska");
        assertEquals(ZoneOffset.ofHoursMinutes(-3, -30), DateUtilities.parseDate("Mon Jan 15 07:00:00 NST 2024",
                UTC, true).getOffset(), "Newfoundland, not New Zealand");
        assertEquals(ZoneOffset.ofHours(-11), DateUtilities.parseDate("Sun Jan 14 23:30:00 SST 2024", UTC, true)
                .getOffset(), "Samoa, not the Solomon Islands");
        assertEquals(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli(),
                parsedMillis(dateToString(INSTANTS[0], "America/Puerto_Rico")));
        assertEquals(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli(),
                parsedMillis(dateToString(INSTANTS[0], "America/St_Johns")));
        assertEquals(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli(),
                parsedMillis(dateToString(INSTANTS[0], "Pacific/Pago_Pago")));
    }

    @Test
    void everyRegionReadsBackWhereItsAbbreviationNamesOneOffset() {
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        // What each abbreviation stands for, winter and summer: PST is Manila (+08:00) all year and Los Angeles
        // (-08:00) in winter, so it names two offsets even in July, when only Manila prints it
        Map<String, Set<ZoneOffset>> offsetsOf = new HashMap<>();
        List<Map<String, String>> textsAt = new ArrayList<>();
        for (long millis : INSTANTS) {
            Instant instant = Instant.ofEpochMilli(millis);
            Map<String, String> textOf = new HashMap<>();
            for (String region : ZoneId.getAvailableZoneIds()) {
                if (region.indexOf('/') < 0 || region.startsWith("Etc/") || region.startsWith("SystemV/")) {
                    continue;
                }
                String text = dateToString(millis, region);
                textOf.put(region, text);
                offsetsOf.computeIfAbsent(text.substring(20, text.length() - 5), k -> new HashSet<>())
                        .add(ZoneId.of(region).getRules().getOffset(instant));
            }
            textsAt.add(textOf);
        }
        for (int at = 0; at < INSTANTS.length; at++) {
            long millis = INSTANTS[at];
            for (Map.Entry<String, String> entry : textsAt.get(at).entrySet()) {
                String text = entry.getValue();
                String abbreviation = text.substring(20, text.length() - 5);
                if (offsetsOf.get(abbreviation).size() > 1) {
                    continue;       // an abbreviation shared by different offsets: the table chooses
                }
                if (ZoneId.SHORT_IDS.containsKey(abbreviation)
                        && !DateUtilities.ABBREVIATION_TO_TIMEZONE.containsKey(abbreviation)) {
                    continue;       // one of the JDK's old three-letter IDs: it keeps the JDK's zone
                }
                checked++;
                try {
                    if (parsedMillis(text) != millis) {
                        wrong.add(entry.getKey() + ": " + text + " -> " + DateUtilities.parseDate(text, UTC, true));
                    }
                } catch (RuntimeException e) {
                    wrong.add(entry.getKey() + ": " + text + " -> " + e);
                }
            }
        }
        assertTrue(checked > 600, "checked " + checked);
        assertTrue(wrong.isEmpty(), wrong.size() + " of " + checked + " wrong, e.g. "
                + wrong.subList(0, Math.min(5, wrong.size())));
    }

    /** The old three-letter IDs outside the table keep the JDK's zones: ECT is Paris, CTT Shanghai, BET Sao Paulo. */
    @Test
    void theJdksOldThreeLetterIdsKeepTheirZones() {
        int checked = 0;
        for (String id : ZoneId.SHORT_IDS.keySet()) {
            if (DateUtilities.ABBREVIATION_TO_TIMEZONE.containsKey(id)) {
                continue;       // the table's zone (EST, CAT...)
            }
            checked++;
            assertEquals(ZoneId.of(id, ZoneId.SHORT_IDS).getRules().getOffset(Instant.parse("2024-01-15T00:00:00Z")),
                    DateUtilities.parseDate("2024-01-15 00:00:00 " + id, UTC, true).getOffset(), id);
        }
        assertTrue(checked >= 10, "checked " + checked);
    }

    /** From JDK 24 a lookup of a legacy three-letter ID prints a deprecation warning; parsing must not cause one. */
    @Test
    void parsingPrintsNoDeprecationWarning() {
        String spec = System.getProperty("java.specification.version");
        assumeTrue(!spec.startsWith("1.") && Integer.parseInt(spec) >= 24, "the JDK warns from 24 on");
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, "UTF-8"));
            for (String text : new String[] {"Mon Jan 15 06:30:00 AST 2024", "Mon Jan 15 07:00:00 NST 2024",
                    "Sun Jan 14 23:30:00 SST 2024", "2024-01-15 06:30:00 ECT", "2024-01-15 06:30:00 BET",
                    "Mon Jan 15 06:30:00 CTT 2024", "2024-01-15 06:30:00 CAT"}) {
                DateUtilities.parseDate(text, UTC, true);
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        } finally {
            System.setErr(original);
        }
        String printed = new String(captured.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(printed.contains("deprecated"), printed);
    }
}
