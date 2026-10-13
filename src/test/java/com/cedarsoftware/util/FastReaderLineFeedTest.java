package com.cedarsoftware.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FastReader#readLineLf} and {@link FastReader#readLineBorrowedLf}: lines that only a line feed ends. A
 * carriage return is content wherever it stands, the one before a line feed included. ({@code readLine} ends a
 * line at a lone carriage return too, and drops the one of a {@code \r\n} pair.)
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
class FastReaderLineFeedTest {

    /** A reader that counts the calls made on it. */
    private static final class CountingReader extends Reader {
        private final Reader delegate;
        int reads;

        CountingReader(String text) {
            delegate = new StringReader(text);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws java.io.IOException {
            reads++;
            return delegate.read(cbuf, off, len);
        }

        @Override
        public void close() {
        }
    }

    /**
     * The rule, restated on the text: the pieces between line feeds. A text that ends in a line feed has no line
     * after it, and the empty text has none at all.
     */
    private static List<String> linesOf(String text) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /** One line by the copying method, called again while it fills the room it was given, as a caller must. */
    private static String copyLine(FastReader reader, int room) {
        char[] dest = new char[room];
        StringBuilder line = new StringBuilder();
        while (true) {
            int count = reader.readLineLf(dest, 0, room);
            if (count < 0) {
                return line.length() > 0 ? line.toString() : null;
            }
            line.append(dest, 0, count);
            if (count < room) {
                return line.toString();
            }
        }
    }

    private static List<String> readByCopy(String text, int bufferSize, int pushback, int room) {
        FastReader reader = new FastReader(new StringReader(text), bufferSize, pushback);
        List<String> lines = new ArrayList<>();
        for (String line = copyLine(reader, room); line != null; line = copyLine(reader, room)) {
            lines.add(line);
        }
        return lines;
    }

    /** By borrowing where the line can be lent, and by copying where it cannot. */
    private static List<String> readByBorrowing(String text, int bufferSize, int pushback, int room, int[] lent) {
        FastReader reader = new FastReader(new StringReader(text), bufferSize, pushback);
        FastReader.BufferSlice slice = new FastReader.BufferSlice();
        List<String> lines = new ArrayList<>();
        while (true) {
            int count = reader.readLineBorrowedLf(slice);
            if (count == FastReader.COPY_REQUIRED) {
                String line = copyLine(reader, room);
                if (line == null) {
                    return lines;
                }
                lines.add(line);
            } else if (count < 0) {
                return lines;
            } else {
                lines.add(new String(slice.getBuffer(), slice.getOffset(), count));
                slice.release();
                lent[0]++;
            }
        }
    }

    @Test
    void onlyALineFeedEndsALine() {
        String text = "a\rb\r\nc\n\nd\r";
        int[] lent = {0};

        assertEquals(Arrays.asList("a\rb\r", "c", "", "d\r"), readByCopy(text, 64, 2, 16));
        assertEquals(Arrays.asList("a\rb\r", "c", "", "d\r"), readByBorrowing(text, 64, 2, 16, lent));
        assertEquals(3, lent[0], "the last line has no line feed in the buffer, and is copied");
    }

    /** readLine, beside it, ends a line at each carriage return: the two methods differ exactly there. */
    @Test
    void readLineSplitsAtACarriageReturnAndThisDoesNot() {
        FastReader reader = new FastReader(new StringReader("a\rb\r\nc"), 64, 2);
        char[] dest = new char[16];
        List<String> lines = new ArrayList<>();
        for (int count = reader.readLine(dest, 0, 16); count >= 0; count = reader.readLine(dest, 0, 16)) {
            lines.add(new String(dest, 0, count));
        }

        assertEquals(Arrays.asList("a", "b", "c"), lines);
        assertEquals(Arrays.asList("a\rb\r", "c"), readByCopy("a\rb\r\nc", 64, 2, 16));
    }

    /**
     * 6,000 texts of line feeds, carriage returns and letters from a fixed seed, read with buffers of 1 to 9
     * characters, pushback room of 0 to 2 and 1 to 7 characters of room for a line: both methods give the lines
     * that splitting the text at its line feeds gives.
     */
    @Test
    void everyTextEveryBufferSize() {
        Random random = new Random(20261012L);
        char[] alphabet = {'a', 'b', '\r', '\n', '\n'};
        int[] lent = {0};
        int lines = 0;
        for (int n = 0; n < 6000; n++) {
            StringBuilder text = new StringBuilder();
            for (int length = random.nextInt(40); length > 0; length--) {
                text.append(alphabet[random.nextInt(alphabet.length)]);
            }
            int bufferSize = 1 + random.nextInt(9);
            int pushback = random.nextInt(3);
            int room = 1 + random.nextInt(7);
            String where = "buffer " + bufferSize + ", pushback " + pushback + ", room " + room + ": "
                    + text.toString().replace("\r", "\\r").replace("\n", "\\n");
            List<String> expected = linesOf(text.toString());

            assertEquals(expected, readByCopy(text.toString(), bufferSize, pushback, room), where);
            assertEquals(expected, readByBorrowing(text.toString(), bufferSize, pushback, room, lent), where);
            lines += expected.size();
        }
        assertTrue(lines > 30_000 && lent[0] > 5_000, lines + " lines, " + lent[0] + " of them lent");
    }

    /** With the room filled before a line feed is met, the line feed stays unread and the next call goes on. */
    @Test
    void aLineLongerThanTheRoomComesInPieces() {
        FastReader reader = new FastReader(new StringReader("abcdef\nxy\n"), 64, 2);
        char[] dest = new char[8];

        assertEquals(3, reader.readLineLf(dest, 1, 3));
        assertEquals("abc", new String(dest, 1, 3));
        assertEquals(3, reader.readLineLf(dest, 0, 3));
        assertEquals("def", new String(dest, 0, 3));
        assertEquals(0, reader.readLineLf(dest, 0, 3), "the rest of the line is empty, and its line feed is taken");
        assertEquals(2, reader.readLineLf(dest, 0, 3));
        assertEquals("xy", new String(dest, 0, 2));
        assertEquals(-1, reader.readLineLf(dest, 0, 3));
    }

    /** Once the input has ended the answer is -1, without asking the underlying reader again. */
    @Test
    void theEndOfTheInput() {
        CountingReader counting = new CountingReader("a\nb");
        FastReader reader = new FastReader(counting, 64, 2);
        FastReader.BufferSlice slice = new FastReader.BufferSlice();
        char[] dest = new char[8];

        assertEquals(1, reader.readLineLf(dest, 0, 8));
        assertEquals(1, reader.readLineLf(dest, 0, 8));
        assertEquals('b', dest[0], "input that ends without a line feed gives its last line");
        assertEquals(-1, reader.readLineLf(dest, 0, 8));
        int reads = counting.reads;
        assertEquals(-1, reader.readLineLf(dest, 0, 8));
        assertEquals(-1, reader.readLineBorrowedLf(slice));
        assertEquals(reads, counting.reads);

        assertEquals(-1, new FastReader(new StringReader(""), 8, 0).readLineLf(dest, 0, 8));
        assertEquals(-1, new FastReader(new StringReader(""), 8, 0).readLineBorrowedLf(slice));
    }

    /** A line that cannot be lent is left where it is, for the copying method to read. */
    @Test
    void aLineThatCannotBeLentIsNotConsumed() {
        FastReader reader = new FastReader(new StringReader("abcdefgh\nij\n"), 4, 0);
        FastReader.BufferSlice slice = new FastReader.BufferSlice();
        char[] dest = new char[16];

        assertEquals(FastReader.COPY_REQUIRED, reader.readLineBorrowedLf(slice));
        assertEquals(FastReader.COPY_REQUIRED, reader.readLineBorrowedLf(slice));
        assertEquals(8, reader.readLineLf(dest, 0, 16));
        assertEquals("abcdefgh", new String(dest, 0, 8));
        assertEquals(2, reader.readLineBorrowedLf(slice));
        assertEquals("ij", new String(slice.getBuffer(), slice.getOffset(), slice.getLength()));
        slice.release();
        assertEquals(-1, reader.readLineBorrowedLf(slice));
    }

    /** The line methods read on from wherever the reader stands, a pushed-back character included. */
    @Test
    void afterOtherReadsAndAPushback() {
        FastReader reader = new FastReader(new StringReader("abc\r\ndef\n"), 64, 2);
        FastReader.BufferSlice slice = new FastReader.BufferSlice();
        char[] dest = new char[16];

        assertEquals('a', reader.read());
        assertEquals(3, reader.readLineLf(dest, 0, 16));
        assertEquals("bc\r", new String(dest, 0, 3));
        assertEquals('d', reader.read());
        reader.pushback('d');
        reader.pushback('>');
        assertEquals(4, reader.readLineBorrowedLf(slice));
        assertEquals(">def", new String(slice.getBuffer(), slice.getOffset(), slice.getLength()));
        slice.release();
        assertEquals(-1, reader.read());
    }
}
