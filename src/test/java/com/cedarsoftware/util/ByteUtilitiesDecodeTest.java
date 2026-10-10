package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ByteUtilities#decode(String)} reads a byte array in any common notation: hex (plain, 0x, grouped by
 * whitespace, ':' or '-'), escapes, the SQL literal, number lists, correctly padded Base64 and data URIs. Before, it
 * read plain hex only and returned null for the rest.
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
class ByteUtilitiesDecodeTest {

    private static final byte[] AFDE929F = {(byte) 0xAF, (byte) 0xDE, (byte) 0x92, (byte) 0x9F};

    @Test
    void everyNotationReads() {
        Map<String, byte[]> cases = new LinkedHashMap<>();
        // Hex digits, and the 0x prefix (an odd count takes a leading zero)
        cases.put("AFDE929F", AFDE929F);
        cases.put("afde929f", AFDE929F);
        cases.put("  AFDE929F\n", AFDE929F);
        cases.put("0xAFDE929F", AFDE929F);
        cases.put("0Xafde929f", AFDE929F);
        cases.put("0xF", new byte[] {0x0F});
        cases.put("0x1F8", new byte[] {0x01, (byte) 0xF8});
        // Hex in groups: hex dumps, MAC addresses, fingerprints
        cases.put("AF DE 92 9F", AFDE929F);
        cases.put("AF  DE\n92\t9F", AFDE929F);
        cases.put("af:de:92:9f", AFDE929F);
        cases.put("AF-DE-92-9F", AFDE929F);
        cases.put("AF : DE : 92 : 9F", AFDE929F);
        cases.put("af:de:\n    92:9f", AFDE929F);                 // OpenSSL's listing: a line break after a ':'
        cases.put("AFDE 929F", AFDE929F);                         // groups of four, as GPG prints a fingerprint
        cases.put("0xAF 0xDE 0x92 0x9F", AFDE929F);
        cases.put("0:1c:42:b", new byte[] {0x00, 0x1C, 0x42, 0x0B});   // macOS prints a MAC address so
        cases.put("1 2 3", new byte[] {1, 2, 3});
        cases.put("10 20", new byte[] {0x10, 0x20});              // hex: whitespace-separated groups
        // Escapes and the SQL literal
        cases.put("\\xAF\\xDE\\x92\\x9F", AFDE929F);
        cases.put("\\xafde929f", AFDE929F);                       // PostgreSQL bytea
        cases.put("X'AFDE929F'", AFDE929F);
        cases.put("x'afde929f'", AFDE929F);
        // Number lists: decimal (signed or unsigned) or 0x, in brackets, braces, parentheses, or bare with commas
        cases.put("[-81, -34, -110, -97]", AFDE929F);             // Arrays.toString(byte[])
        cases.put("[175,222,146,159]", AFDE929F);                 // a JSON array of unsigned bytes
        cases.put("[ 175  222 146 159]", AFDE929F);               // numpy
        cases.put("{0xAF, 0xDE, 0x92, 0x9F}", AFDE929F);          // C
        cases.put("new byte[] {(byte) 0xAF, (byte)0xDE, -110, 159,}", AFDE929F);   // Java, trailing comma
        cases.put("byte[]{-81, -34, -110, -97}", AFDE929F);
        cases.put("(175, 222, 146, 159)", AFDE929F);
        cases.put("175, 222, 146, 159", AFDE929F);
        cases.put("0xAF, 0xDE, 0x92, 0x9F", AFDE929F);
        cases.put("[10, 20]", new byte[] {10, 20});               // decimal: a list
        cases.put("[-1, 255, -128, 128, +7, 007]", new byte[] {-1, -1, -128, -128, 7, 7});
        cases.put("[]", new byte[0]);
        cases.put("{ }", new byte[0]);
        // Base64, padded correctly, standard or URL-safe; MIME line breaks ignored
        cases.put("r96Snw==", AFDE929F);
        cases.put("AQID", new byte[] {1, 2, 3});
        cases.put("-_8=", new byte[] {-5, -1});
        cases.put("QUJD\r\nREVG", "ABCDEF".getBytes(StandardCharsets.US_ASCII));
        // Data URIs (RFC 2397)
        cases.put("data:application/octet-stream;base64,r96Snw==", AFDE929F);
        cases.put("data:image/png;base64,iVBORw0KGgo=", new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        cases.put("DATA:text/plain;BASE64,SGk=", "Hi".getBytes(StandardCharsets.US_ASCII));
        cases.put("data:,Hello%2C%20World", "Hello, World".getBytes(StandardCharsets.US_ASCII));
        cases.put("data:text/plain;charset=UTF-8,na%C3%AFve", "naïve".getBytes(StandardCharsets.UTF_8));
        cases.put("data:,", new byte[0]);
        // Blank
        cases.put("", new byte[0]);
        cases.put("  \n ", new byte[0]);

        for (Map.Entry<String, byte[]> entry : cases.entrySet()) {
            assertArrayEquals(entry.getValue(), ByteUtilities.decode(entry.getKey()), entry.getKey());
            assertArrayEquals(entry.getValue(), ByteUtilities.decode(new StringBuilder(entry.getKey())),
                    entry.getKey());
        }
    }

    /** Text that fits two notations is hex: "CAFEBABE" is also six bytes of Base64, "AB-CD-EF" six of URL-safe. */
    @Test
    void hexWinsOverBase64() {
        assertArrayEquals(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE},
                ByteUtilities.decode("CAFEBABE"));
        assertArrayEquals(new byte[] {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF}, ByteUtilities.decode("AB-CD-EF"));
        assertArrayEquals(new byte[] {0, 31, -126, 15, -31, 5}, ByteUtilities.decodeBase64("AB-CD-EF"));
        assertArrayEquals(new byte[] {(byte) 0xAF}, ByteUtilities.decode("AF"));
        assertArrayEquals(new byte[] {8, 1, 68, 4, 0, 68}, ByteUtilities.decodeBase64("CAFEBABE"));
    }

    @Test
    void textInNoNotationIsNull() {
        for (String text : new String[] {
                "456", "F", "GG", "OK", "AQ", "AF==",          // odd hex, not hex, Base64 not padded correctly
                "AF:DE-92", "AF:DE 92", "AF:DE:", ":AF", "AF::DE", "AF DE -",   // mixed or stray separators
                "AFD:E9", "0x", "0xG",                          // an odd group without 0x, no digits
                "[256]", "[-129]", "[0x100]", "[AF, DE]", "AF,DE", "[1,,2]", "[,1]", "[1 2", "1 2]", "[1;2]",
                "[(byte)]", "[1(byte)2]", "new byte[] [1]", "byte[]{1", "[1-2]", "[--1]",
                "\\xAF\\xD", "\\xAF\\yDE", "\\x", "\\xAFD", "X'AFD'", "X'AF",
                "data:,%4", "data:,%GG", "data:;base64,AF", "data:text/plain",
                "10.0.0.1", "hello world", "naïve"}) {
            assertNull(ByteUtilities.decode(text), text);
        }
        assertNull(ByteUtilities.decode((String) null));
        assertNull(ByteUtilities.decode((CharSequence) null));
    }

    /**
     * Random arrays of 0-48 bytes, written in each notation, read back as written. Base64 that is also hex (all hex
     * digits, perhaps after 0x, or URL-safe split by '-' into hex groups) reads as hex, by design, and is checked by
     * hexWinsOverBase64.
     */
    @Test
    void everyNotationRoundTrips() {
        Random random = new Random(5);
        List<String> wrong = new ArrayList<>();
        for (int length = 0; length <= 48; length++) {
            for (int k = 0; k < 20; k++) {
                byte[] payload = new byte[length];
                random.nextBytes(payload);
                List<String> spellings = new ArrayList<>();
                String hex = ByteUtilities.encode(payload);
                spellings.add(hex);
                spellings.add(hex.toLowerCase());
                if (length > 0) {
                    spellings.add("0x" + hex);
                    spellings.add("\\x" + hex);
                    spellings.add(groups(hex, " "));
                    spellings.add(groups(hex, ":").toLowerCase());
                    spellings.add(groups(hex, "-"));
                    spellings.add(groups(hex, "\n"));
                    spellings.add("\\x" + groups(hex, "\\x"));
                    spellings.add("0x" + groups(hex, " 0x"));
                }
                spellings.add("X'" + hex + "'");
                spellings.add(Arrays.toString(payload));
                spellings.add(unsignedList(payload));
                String standard = Base64.getEncoder().encodeToString(payload);
                String urlSafe = Base64.getUrlEncoder().encodeToString(payload);
                String mime = Base64.getMimeEncoder(8, "\r\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(payload);
                for (String base64 : new String[] {standard, urlSafe, mime}) {
                    if (!base64.matches("[0-9A-Fa-fxX\\-\\s]+")) {
                        spellings.add(base64);
                    }
                }
                spellings.add("data:application/octet-stream;base64," + standard);
                for (String spelling : spellings) {
                    byte[] back = ByteUtilities.decode(spelling);
                    if (!Arrays.equals(payload, back)) {
                        wrong.add(spelling + " -> " + Arrays.toString(back));
                    }
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong, e.g. " + wrong.subList(0, Math.min(5, wrong.size())));
    }

    private static String groups(String hex, String separator) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                out.append(separator);
            }
            out.append(hex, i, i + 2);
        }
        return out.toString();
    }

    private static String unsignedList(byte[] bytes) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            out.append(i == 0 ? "" : ",").append(bytes[i] & 0xFF);
        }
        return out.append(']').toString();
    }
}
