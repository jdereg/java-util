package com.cedarsoftware.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A string is Base64 only when it is padded correctly -- exactly as an encoder writes it. The JDK's decoders also
 * take unpadded text ("AF" is {0}, "OK" is {56}) and padding whose left-over bits are not zero ("AF==" is {0} too),
 * so a short word became a byte. The oracle restates the rule: a string is accepted exactly when encoding what it
 * decodes to gives the string back.
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
class ByteUtilitiesBase64Test {

    private static final String STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URL_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    @Test
    void correctlyPaddedBase64Decodes() {
        assertArrayEquals(new byte[] {1, 2, 3}, ByteUtilities.decodeBase64("AQID"));
        assertArrayEquals(new byte[] {1}, ByteUtilities.decodeBase64("AQ=="));
        assertArrayEquals(new byte[] {1, 2}, ByteUtilities.decodeBase64("AQI="));
        assertArrayEquals(new byte[] {1, 2, -3}, ByteUtilities.decodeBase64("AQL9"));
        assertArrayEquals(new byte[] {12, 4, -64}, ByteUtilities.decodeBase64("DATA"));
        assertArrayEquals(new byte[0], ByteUtilities.decodeBase64(""));
        assertArrayEquals(new byte[] {-5, -1}, ByteUtilities.decodeBase64("+/8="));
        assertArrayEquals(new byte[] {-5, -1}, ByteUtilities.decodeBase64("-_8="));   // URL-safe
    }

    @Test
    void textThatIsNotCorrectlyPaddedBase64IsNot() {
        for (String text : new String[] {
                "AF", "OK", "AQ", "AQI", "-_8",         // no padding (the JDK reads {0}, {56}, {1}, {1, 2}, {-5, -1})
                "AQ=", "A", "AQIDB",                     // a length no encoder writes
                "AF==", "OK==", "AQJ=", "-_9=",         // padding whose left-over bits are not zero
                "====", "A===", "AQ==AQ==",             // padding in the wrong place
                "QUJD REVG", "QUJD\nREVG",              // whitespace
                "+_8=", "hello world", "naïve!!"}) {   // mixed alphabets, not Base64 at all
            assertNull(ByteUtilities.decodeBase64(text), text);
        }
        assertNull(ByteUtilities.decodeBase64(null));
    }

    /**
     * Every string of 2, 3 or 4 Base64 digits, padded with '=' to a group of 4 or not padded: accepted exactly when
     * encoding the result gives it back (the padded forms with zero left-over bits), in both alphabets.
     */
    @Test
    void acceptedExactlyWhenAnEncoderWritesIt() {
        checkEveryShortGroup(STANDARD, Base64.getEncoder());
        checkEveryShortGroup(URL_SAFE, Base64.getUrlEncoder());
    }

    private static void checkEveryShortGroup(String alphabet, Base64.Encoder encoder) {
        List<String> wrong = new ArrayList<>();
        int accepted = 0;
        char[] two = new char[2];
        char[] three = new char[3];
        for (int a = 0; a < 64; a++) {
            two[0] = three[0] = alphabet.charAt(a);
            for (int b = 0; b < 64; b++) {
                two[1] = three[1] = alphabet.charAt(b);
                accepted += check(new String(two), encoder, wrong);
                accepted += check(new String(two) + "==", encoder, wrong);
                for (int c = 0; c < 64; c++) {
                    three[2] = alphabet.charAt(c);
                    accepted += check(new String(three), encoder, wrong);
                    accepted += check(new String(three) + "=", encoder, wrong);
                    accepted += check(new String(three) + "A", encoder, wrong);
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong, e.g. " + wrong.subList(0, Math.min(5, wrong.size())));
        // 256 one-byte values ("xY=="), 65,536 two-byte values ("xyZ="), and every "xyzA" (64^3)
        assertEquals(256 + 65536 + 64 * 64 * 64, accepted);
    }

    private static int check(String text, Base64.Encoder encoder, List<String> wrong) {
        byte[] decoded = ByteUtilities.decodeBase64(text);
        boolean shouldAccept = text.length() % 4 == 0 && text.equals(encoder.encodeToString(
                Base64.getMimeDecoder().decode(text.replace('-', '+').replace('_', '/'))));
        if ((decoded != null) != shouldAccept
                || (decoded != null && !text.equals(encoder.encodeToString(decoded)))) {
            wrong.add(text + " -> " + Arrays.toString(decoded));
        }
        return decoded == null ? 0 : 1;
    }

    @Test
    void everyArrayRoundTripsThroughEitherEncoder() {
        Random random = new Random(11);
        for (int length = 0; length <= 64; length++) {
            for (int k = 0; k < 20; k++) {
                byte[] payload = new byte[length];
                random.nextBytes(payload);
                String standard = Base64.getEncoder().encodeToString(payload);
                String urlSafe = Base64.getUrlEncoder().encodeToString(payload);
                assertArrayEquals(payload, ByteUtilities.decodeBase64(standard), standard);
                assertArrayEquals(payload, ByteUtilities.decodeBase64(urlSafe), urlSafe);
            }
        }
    }

    /** The wrapped form {"value": "<base64>"} reads by the same rule; anything else is the String's own bytes. */
    @Test
    void theWrappedFormReadsByTheSameRule() {
        Converter converter = new Converter(new DefaultConverterOptions());
        assertArrayEquals(new byte[] {1}, converter.convert(value("AQ=="), byte[].class));
        assertArrayEquals(new byte[] {-5, -1}, converter.convert(value("-_8="), byte[].class));
        for (String text : new String[] {"AF", "OK", "AF==", "AQ"}) {
            byte[] own = text.getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(own, converter.convert(value(text), byte[].class), text);
            assertEquals(ByteBuffer.wrap(own), converter.convert(value(text), ByteBuffer.class), text);
        }
        assertEquals(ByteBuffer.wrap(new byte[] {-5, -1}), converter.convert(value("-_8="), ByteBuffer.class));
    }

    private static Map<String, Object> value(String text) {
        return Collections.singletonMap("value", text);
    }
}
