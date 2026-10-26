package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The upgrade note at the top of 4.118.0 in changelog.md -- "what can change a result" -- one test per row, so the
 * note says what the code does. Each row's example is the one the note gives; 4.117.0 answered every one of them
 * differently (the note's "4.117.0" column).
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
class UpgradeTo4118NotesTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    public static class Node {
        public String name;
        public Node parent;
        public List<Node> kids = new ArrayList<>();
    }

    public static class Policy {
        public Policy(String number) {
            if (number == null || !number.startsWith("P-")) {
                throw new IllegalArgumentException("policy number must start with P-");
            }
        }
    }

    /** 4.117.0: Infinity, Infinity, 8.000000000000002, BigDecimal, BigDecimal. */
    @Test
    void parseToMinimalNumericTypeGivesTheNumberTheTextNames() {
        assertEquals(new BigDecimal("99e307"), MathUtilities.parseToMinimalNumericType("99e307"));
        assertEquals(new BigDecimal("1000000e305"), MathUtilities.parseToMinimalNumericType("1000000e305"));
        assertEquals(new BigDecimal("8.000000000000001"), MathUtilities.parseToMinimalNumericType("8.000000000000001"));
        assertEquals(0.30000000000000004, MathUtilities.parseToMinimalNumericType("0.30000000000000004"));
        assertEquals(1e308, MathUtilities.parseToMinimalNumericType("1e308"));
    }

    /** 4.117.0: the zones GMT+05:00 and Etc/GMT, the same instants. */
    @Test
    void isoZonedTextReadsInTheZoneItNames() {
        ZonedDateTime offset = converter.convert("2026-01-15T10:00:00+05:00", ZonedDateTime.class);
        assertEquals(ZoneOffset.of("+05:00"), offset.getZone());
        assertEquals(ZonedDateTime.parse("2026-01-15T10:00:00+05:00"), offset);
        assertEquals(ZoneId.of("GMT"), converter.convert("2026-01-15T10:00:00Z[GMT]", ZonedDateTime.class).getZone());
    }

    /** 4.117.0: IllegalArgumentException, "other characters present: [+05:00]". */
    @Test
    void aBareOffsetInBracketsReads() {
        assertEquals(Instant.parse("2026-01-15T05:00:00Z"),
                converter.convert("2026-01-15T10:00:00+05:00[+05:00]", Instant.class));
    }

    /** 4.117.0: the kid's "parent" was null. */
    @Test
    void anObjectToMapKeepsItsLoop() {
        Node root = new Node();
        root.name = "root";
        Node kid = new Node();
        kid.name = "kid";
        kid.parent = root;
        root.kids.add(kid);
        Map<?, ?> map = converter.convert(root, Map.class);
        Map<?, ?> kidMap = (Map<?, ?>) ((List<?>) map.get("kids")).get(0);
        assertSame(map, kidMap.get("parent"));
    }

    /** 4.117.0: the built-in Map -> Timestamp ran (1700000000000), once a plain Converter had converted the pair. */
    @Test
    void aConvertersOwnSupertypeConversionRuns() {
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("epochMillis", 1700000000000L);
        assertEquals(1700000000000L, converter.convert(legacy, Timestamp.class).getTime());
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getConverterOverrides().put(Converter.pair(Map.class, Timestamp.class), (from, c) -> new Timestamp(42L));
        assertEquals(42L, new Converter(options).convert(legacy, Timestamp.class).getTime());
    }

    /** 4.117.0: the second call said only "Unable to instantiate (cached): ...Policy". */
    @Test
    void aRememberedInstantiationFailureSaysWhy() {
        for (int i = 0; i < 2; i++) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> ClassUtilities.newInstance(converter, Policy.class, (Object) null));
            assertTrue(e.getMessage().contains("policy number must start with P-"), e.getMessage());
        }
    }
}
