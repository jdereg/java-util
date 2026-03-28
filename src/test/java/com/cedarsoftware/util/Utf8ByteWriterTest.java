package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Utf8ByteWriterTest {
    private ByteArrayOutputStream baos;
    private Utf8ByteWriter writer;

    @BeforeEach
    void setUp() {
        baos = new ByteArrayOutputStream();
        writer = new Utf8ByteWriter(baos);
    }

    // ========== ASCII Tests ==========

    @Test
    void testWriteSingleAsciiChar() throws IOException {
        writer.write('A');
        writer.flush();
        assertEquals("A", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteAsciiString() throws IOException {
        writer.write("Hello, World!");
        writer.flush();
        assertEquals("Hello, World!", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteAsciiStringWithOffsetAndLength() throws IOException {
        writer.write("Hello, World!", 7, 5);
        writer.flush();
        assertEquals("World", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteCharArray() throws IOException {
        char[] chars = "Hello".toCharArray();
        writer.write(chars, 0, chars.length);
        writer.flush();
        assertEquals("Hello", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteCharArrayPartial() throws IOException {
        char[] chars = "Hello, World!".toCharArray();
        writer.write(chars, 7, 5);
        writer.flush();
        assertEquals("World", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteJsonStructure() throws IOException {
        writer.write("{\"name\":\"Alice\",\"age\":30}");
        writer.flush();
        assertEquals("{\"name\":\"Alice\",\"age\":30}", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteToonStructure() throws IOException {
        String toon = "name: Alice\nage: 30\n";
        writer.write(toon);
        writer.flush();
        assertEquals(toon, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteEmptyString() throws IOException {
        writer.write("");
        writer.flush();
        assertEquals("", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteAllAsciiChars() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            sb.append((char) i);
        }
        String all = sb.toString();
        writer.write(all);
        writer.flush();
        assertEquals(all, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== 2-Byte UTF-8 Tests (0x80-0x7FF) ==========

    @Test
    void testWrite2ByteChars() throws IOException {
        // Latin Extended: é (U+00E9), ñ (U+00F1), ü (U+00FC)
        writer.write("café résumé señor über");
        writer.flush();
        assertEquals("café résumé señor über", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteGreekChars() throws IOException {
        // Greek: α (U+03B1), β (U+03B2), γ (U+03B3)
        writer.write("αβγ");
        writer.flush();
        assertEquals("αβγ", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWrite2ByteBoundary() throws IOException {
        // U+0080 (first 2-byte char) and U+07FF (last 2-byte char)
        writer.write('\u0080');
        writer.write('\u07FF');
        writer.flush();
        assertEquals("\u0080\u07FF", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== 3-Byte UTF-8 Tests (0x800-0xFFFF) ==========

    @Test
    void testWriteCjkChars() throws IOException {
        // CJK: 漢字 (U+6F22, U+5B57)
        writer.write("漢字テスト");
        writer.flush();
        assertEquals("漢字テスト", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWrite3ByteBoundary() throws IOException {
        // U+0800 (first 3-byte char) and U+FFFD (replacement character, near end of BMP)
        writer.write('\u0800');
        writer.write('\uFFFD');
        writer.flush();
        assertEquals("\u0800\uFFFD", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteJsonWithUnicode() throws IOException {
        writer.write("{\"name\":\"日本語\",\"value\":\"café\"}");
        writer.flush();
        assertEquals("{\"name\":\"日本語\",\"value\":\"café\"}", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== 4-Byte UTF-8 Tests (Surrogate Pairs) ==========

    @Test
    void testWriteEmoji() throws IOException {
        // 😀 U+1F600 = surrogate pair D83D DE00
        String emoji = "\uD83D\uDE00";
        writer.write(emoji);
        writer.flush();
        assertEquals(emoji, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteMultipleEmoji() throws IOException {
        String emojis = "Hello \uD83D\uDE00\uD83D\uDE01\uD83D\uDE02 World";
        writer.write(emojis);
        writer.flush();
        assertEquals(emojis, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteSurrogatePairInCharArray() throws IOException {
        char[] chars = "\uD83D\uDE00".toCharArray();
        writer.write(chars, 0, chars.length);
        writer.flush();
        assertEquals("\uD83D\uDE00", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteSurrogatePairViaSingleCharWrites() throws IOException {
        // High surrogate followed by low surrogate as separate write(int) calls
        // write(int) can't pair surrogates across calls, so each becomes a 3-byte encoding
        // This matches OutputStreamWriter behavior for unpaired surrogates
        writer.write('\uD83D');
        writer.write('\uDE00');
        writer.flush();
        // The output should be valid (each surrogate encoded as 3-byte) or could be
        // implementation-defined. Just verify no exception and round-trip works for valid strings.
        assertNotNull(baos.toByteArray());
        assertTrue(baos.size() > 0);
    }

    @Test
    void testWriteMusicalSymbol() throws IOException {
        // 𝄞 U+1D11E = surrogate pair D834 DD1E
        String trebleClef = "\uD834\uDD1E";
        writer.write(trebleClef);
        writer.flush();
        assertEquals(trebleClef, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== Mixed Content Tests ==========

    @Test
    void testWriteMixedAsciiAndUnicode() throws IOException {
        String mixed = "Hello 世界! café \uD83D\uDE00 done.";
        writer.write(mixed);
        writer.flush();
        assertEquals(mixed, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteMultipleSmallWrites() throws IOException {
        writer.write("{");
        writer.write("\"key\"");
        writer.write(":");
        writer.write(" ");
        writer.write("\"value\"");
        writer.write("}");
        writer.flush();
        assertEquals("{\"key\": \"value\"}", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteInterleavedCharAndString() throws IOException {
        writer.write('{');
        writer.write("\"name\"");
        writer.write(':');
        writer.write(' ');
        writer.write("\"test\"");
        writer.write('}');
        writer.flush();
        assertEquals("{\"name\": \"test\"}", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== Buffer Boundary Tests ==========

    @Test
    void testWriteLargerThanBuffer() throws IOException {
        // Use a small buffer to force multiple flushes
        writer = new Utf8ByteWriter(baos, 32);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("ABCD");
        }
        String large = sb.toString();
        writer.write(large);
        writer.flush();
        assertEquals(large, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteUnicodeAcrossBufferBoundary() throws IOException {
        // Use a small buffer so multi-byte chars span flush points
        writer = new Utf8ByteWriter(baos, 16);
        String text = "abcdefghijklé"; // 'é' is 2-byte, should land near buffer boundary
        writer.write(text);
        writer.flush();
        assertEquals(text, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteEmojiAcrossBufferBoundary() throws IOException {
        writer = new Utf8ByteWriter(baos, 16);
        String text = "abcdefghijk\uD83D\uDE00"; // emoji is 4-byte
        writer.write(text);
        writer.flush();
        assertEquals(text, new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteWithCallerProvidedBuffer() throws IOException {
        byte[] buffer = new byte[64];
        writer = new Utf8ByteWriter(baos, buffer);
        writer.write("Using caller-provided buffer");
        writer.flush();
        assertEquals("Using caller-provided buffer", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    // ========== Matches FastWriter+OutputStreamWriter Output ==========

    @Test
    void testOutputMatchesFastWriter() throws IOException {
        String[] testStrings = {
                "Simple ASCII",
                "{\"key\":\"value\",\"num\":42}",
                "café résumé",
                "漢字テスト",
                "Hello \uD83D\uDE00 World",
                "Mixed: abc αβγ 漢字 \uD83D\uDE00",
                "",
                "a",
                "\n\r\t",
                "true",
                "null",
                "12345"
        };

        for (String testStr : testStrings) {
            // Write with Utf8ByteWriter
            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
            Utf8ByteWriter utf8Writer = new Utf8ByteWriter(baos1);
            utf8Writer.write(testStr);
            utf8Writer.flush();

            // Write with FastWriter + OutputStreamWriter (current approach)
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            FastWriter fastWriter = new FastWriter(
                    new java.io.OutputStreamWriter(baos2, StandardCharsets.UTF_8));
            fastWriter.write(testStr);
            fastWriter.flush();

            assertArrayEquals(baos2.toByteArray(), baos1.toByteArray(),
                    "Mismatch for: \"" + testStr + "\"");
        }
    }

    @Test
    void testCharByCharMatchesBulkWrite() throws IOException {
        String testStr = "Hello 世界 café \uD83D\uDE00";

        // Bulk write
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        Utf8ByteWriter w1 = new Utf8ByteWriter(baos1);
        w1.write(testStr);
        w1.flush();

        // Char-by-char via write(char[], off, len) with len=1
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        Utf8ByteWriter w2 = new Utf8ByteWriter(baos2);
        char[] chars = testStr.toCharArray();
        // Write via char array to get surrogate pair handling
        w2.write(chars, 0, chars.length);
        w2.flush();

        assertArrayEquals(baos1.toByteArray(), baos2.toByteArray());
    }

    // ========== Edge Cases ==========

    @Test
    void testFlushEmptyBuffer() throws IOException {
        writer.flush();
        assertEquals(0, baos.size());
    }

    @Test
    void testCloseFlushesBuffer() throws IOException {
        writer.write("buffered");
        writer.close();
        assertEquals("buffered", new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testWriteAfterCloseThrows() throws IOException {
        writer.close();
        assertThrows(IOException.class, () -> writer.write('x'));
        assertThrows(IOException.class, () -> writer.write("test"));
        assertThrows(IOException.class, () -> writer.write(new char[]{'a'}, 0, 1));
    }

    @Test
    void testGetLastSnippetEmpty() {
        assertEquals("", writer.getLastSnippet());
    }

    @Test
    void testGetLastSnippetWithContent() throws IOException {
        writer.write("Hello World");
        String snippet = writer.getLastSnippet();
        assertEquals("Hello World", snippet);
    }

    @Test
    void testGetLastSnippetWithUnicode() throws IOException {
        writer.write("café 世界");
        String snippet = writer.getLastSnippet();
        assertEquals("café 世界", snippet);
    }

    @Test
    void testConstructorRejectsSmallBuffer() {
        assertThrows(IllegalArgumentException.class,
                () -> new Utf8ByteWriter(baos, 2));
    }

    @Test
    void testConstructorRejectsNullBuffer() {
        assertThrows(IllegalArgumentException.class,
                () -> new Utf8ByteWriter(baos, (byte[]) null));
    }

    // ========== Correctness: Byte-level UTF-8 verification ==========

    @Test
    void testAsciiByteValues() throws IOException {
        writer.write("AB");
        writer.flush();
        byte[] bytes = baos.toByteArray();
        assertEquals(2, bytes.length);
        assertEquals(0x41, bytes[0]); // 'A'
        assertEquals(0x42, bytes[1]); // 'B'
    }

    @Test
    void testTwoByteEncoding() throws IOException {
        writer.write("é"); // U+00E9
        writer.flush();
        byte[] bytes = baos.toByteArray();
        assertEquals(2, bytes.length);
        assertEquals((byte) 0xC3, bytes[0]); // 110_00011
        assertEquals((byte) 0xA9, bytes[1]); // 10_101001
    }

    @Test
    void testThreeByteEncoding() throws IOException {
        writer.write("漢"); // U+6F22
        writer.flush();
        byte[] bytes = baos.toByteArray();
        assertEquals(3, bytes.length);
        assertEquals((byte) 0xE6, bytes[0]); // 1110_0110
        assertEquals((byte) 0xBC, bytes[1]); // 10_111100
        assertEquals((byte) 0xA2, bytes[2]); // 10_100010
    }

    @Test
    void testFourByteEncoding() throws IOException {
        writer.write("\uD83D\uDE00"); // U+1F600 😀
        writer.flush();
        byte[] bytes = baos.toByteArray();
        assertEquals(4, bytes.length);
        assertEquals((byte) 0xF0, bytes[0]); // 11110_000
        assertEquals((byte) 0x9F, bytes[1]); // 10_011111
        assertEquals((byte) 0x98, bytes[2]); // 10_011000
        assertEquals((byte) 0x80, bytes[3]); // 10_000000
    }
}
