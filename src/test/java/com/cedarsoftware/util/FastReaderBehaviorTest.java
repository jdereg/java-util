package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FastReader's reads, pushback and line and token scanning, against a simple model of a pushback reader, and with
 * sources that block when they have nothing to give.
 */
class FastReaderBehaviorTest {

    private static final char[] ALPHABET = {'a', 'b', '"', '\\', '\r', '\n', '\u00e9', '\uD83D', '\uDE00', '\'', '\t',
            '\u001f', ' '};

    /** A pushback reader over a string: pushed-back characters come first, most recent first. */
    private static final class Model {
        final String text;
        int index;
        final ArrayDeque<Character> pushedBack = new ArrayDeque<>();

        Model(String text) {
            this.text = text;
        }

        int peek() {
            if (!pushedBack.isEmpty()) {
                return pushedBack.peek();
            }
            return index < text.length() ? text.charAt(index) : -1;
        }

        int read() {
            if (!pushedBack.isEmpty()) {
                return pushedBack.pop();
            }
            return index < text.length() ? text.charAt(index++) : -1;
        }

        Model copy() {
            Model m = new Model(text);
            m.index = index;
            m.pushedBack.addAll(pushedBack);
            return m;
        }
    }

    /**
     * Random reads, pushbacks, readUntil, readLine and read(char[]) over random text with every buffer size from 1
     * to 8, CR, LF, a tab, U+001F, quotes, a backslash and surrogate pairs included, each answer checked against the
     * model. readUntil and readUntilBorrowed are asked with two delimiters, or also to stop below '\0' (nothing),
     * below ' ' (a control character, as a strict JSON reader asks) or below 'b' (above both delimiters). A
     * pushback must succeed while the model holds fewer than the requested pushback capacity (it may succeed
     * beyond). A borrowed read may answer COPY_REQUIRED, and then must leave the reader unchanged.
     */
    @Test
    void everyOperationAnswersAsAPushbackReaderDoes() {
        Random random = new Random(42);
        for (int iteration = 0; iteration < 30_000; iteration++) {
            StringBuilder sb = new StringBuilder();
            for (int i = random.nextInt(40); i > 0; i--) {
                sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String text = sb.toString();
            int bufferSize = 1 + random.nextInt(8);
            int capacity = random.nextInt(5);
            FastReader reader = new FastReader(new StringReader(text), bufferSize, capacity);
            Model model = new Model(text);
            StringBuilder log = new StringBuilder("text=\"" + text.replace("\r", "\\r").replace("\n", "\\n")
                    + "\" buffer=" + bufferSize + " pushback=" + capacity + ":");
            for (int op = 0; op < 30; op++) {
                Model next = model.copy();
                String expected;
                String actual;
                int kind = random.nextInt(7);
                if (kind == 0) {
                    expected = "" + next.read();
                    actual = "" + reader.read();
                } else if (kind == 1) {
                    char c = ALPHABET[random.nextInt(ALPHABET.length)];
                    boolean pushed;
                    try {
                        reader.pushback(c);
                        pushed = true;
                    } catch (Exception e) {
                        pushed = false;
                    }
                    if (pushed) {
                        next.pushedBack.push(c);
                    }
                    expected = pushed || model.pushedBack.size() >= capacity ? "ok" : "pushback refused";
                    actual = "ok";
                } else if (kind == 2 || kind == 3) {
                    int maxLen = 1 + random.nextInt(6);
                    char delimiter = random.nextBoolean() ? '"' : '\'';
                    // 0: two delimiters; also below 1: '\0' (nothing), 2: ' ' (a control char), 3: 'b' (above both)
                    int stopKind = random.nextInt(4);
                    char stopBelow = stopKind == 3 ? 'b' : stopKind == 2 ? ' ' : '\0';
                    StringBuilder out = new StringBuilder();
                    int result;
                    while (true) {
                        if (out.length() >= maxLen) {
                            result = out.length();
                            break;
                        }
                        int c = next.peek();
                        if (c == -1) {
                            result = out.length() > 0 ? out.length() : -1;
                            break;
                        }
                        if (c == delimiter || c == '\\' || c < stopBelow) {
                            result = out.length();
                            break;
                        }
                        out.append((char) next.read());
                    }
                    expected = result + ":" + (result > 0 ? out : "");
                    if (kind == 2) {
                        char[] dest = new char[maxLen + 2];
                        int got = stopKind == 0 ? reader.readUntil(dest, 1, maxLen, delimiter, '\\')
                                : reader.readUntil(dest, 1, maxLen, delimiter, '\\', stopBelow);
                        actual = got + ":" + (got > 0 ? new String(dest, 1, got) : "");
                    } else {
                        FastReader.BufferSlice slice = new FastReader.BufferSlice();
                        int got = stopKind == 0 ? reader.readUntilBorrowed(slice, maxLen, delimiter, '\\')
                                : reader.readUntilBorrowed(slice, maxLen, delimiter, '\\', stopBelow);
                        if (got == FastReader.COPY_REQUIRED) {
                            next = model.copy();
                            expected = actual = "COPY_REQUIRED";
                        } else {
                            actual = got + ":" + (got > 0 ? new String(slice.getBuffer(), slice.getOffset(), got) : "");
                            slice.release();
                        }
                    }
                } else if (kind == 4 || kind == 5) {
                    boolean borrowed = kind == 5;
                    int maxLen = borrowed ? Integer.MAX_VALUE : 1 + random.nextInt(6);
                    StringBuilder out = new StringBuilder();
                    int result;
                    while (true) {
                        if (out.length() >= maxLen) {
                            result = out.length();
                            break;
                        }
                        int c = next.peek();
                        if (c == -1) {
                            result = out.length() > 0 ? out.length() : -1;
                            break;
                        }
                        next.read();
                        if (c == '\n') {
                            result = out.length();
                            break;
                        }
                        if (c == '\r') {
                            if (next.peek() == '\n') {
                                next.read();
                            }
                            result = out.length();
                            break;
                        }
                        out.append((char) c);
                    }
                    expected = result + ":" + (result > 0 ? out : "");
                    if (!borrowed) {
                        char[] dest = new char[maxLen + 2];
                        int got = reader.readLine(dest, 1, maxLen);
                        actual = got + ":" + (got > 0 ? new String(dest, 1, got) : "");
                    } else {
                        FastReader.BufferSlice slice = new FastReader.BufferSlice();
                        int got = reader.readLineBorrowed(slice);
                        if (got == FastReader.COPY_REQUIRED) {
                            next = model.copy();
                            expected = actual = "COPY_REQUIRED";
                        } else {
                            actual = got + ":" + (got > 0 ? new String(slice.getBuffer(), slice.getOffset(), got) : "");
                            slice.release();
                        }
                    }
                } else {
                    int len = 1 + random.nextInt(6);
                    StringBuilder out = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        int c = next.read();
                        if (c == -1) {
                            break;
                        }
                        out.append((char) c);
                    }
                    expected = (out.length() == 0 ? -1 : out.length()) + ":" + out;
                    char[] dest = new char[len];
                    int got = reader.read(dest, 0, len);
                    actual = got + ":" + (got > 0 ? new String(dest, 0, got) : "");
                }
                log.append(' ').append(kind).append('=').append(actual);
                assertEquals(expected, actual, log.toString());
                model = next;
            }
        }
    }

    /**
     * A source that gives some characters and then blocks (a pipe, a socket): read(char[]) returns what it has
     * instead of blocking for the rest, as BufferedReader does.
     */
    @Test
    void readOfACharArrayReturnsWhatItHasInsteadOfBlocking() throws Exception {
        CountDownLatch never = new CountDownLatch(1);
        Reader trickle = new Reader() {
            boolean gave;

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                if (!gave) {
                    gave = true;
                    "hello".getChars(0, 5, cbuf, off);
                    return 5;
                }
                try {
                    never.await();                  // nothing more yet: blocks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return -1;
            }

            @Override
            public boolean ready() {
                return false;
            }

            @Override
            public void close() {
            }
        };
        FastReader reader = new FastReader(trickle, 16, 4);
        char[] dest = new char[64];
        int[] got = new int[1];
        Thread thread = new Thread(() -> got[0] = reader.read(dest, 0, 64));
        thread.setDaemon(true);
        thread.start();
        thread.join(5000);
        boolean blocked = thread.isAlive();
        never.countDown();
        assertTrue(!blocked, "read(char[]) blocked although it had 5 characters");
        assertEquals(5, got[0]);
        assertEquals("hello", new String(dest, 0, 5));
    }

    /**
     * readLine meeting CR as the last character of a buffer fill looks for a following LF. It did so by reading
     * and pushing back, which failed outright with no pushback capacity, and otherwise took a pushback slot the
     * caller had not used.
     */
    @Test
    void readLineLooksForLfWithoutUsingPushback() {
        FastReader none = new FastReader(new StringReader("ab\rcd"), 3, 0);
        char[] dest = new char[16];
        assertEquals(2, none.readLine(dest, 0, 16));
        assertEquals('c', none.read());

        FastReader one = new FastReader(new StringReader("ab\rcd"), 3, 1);
        assertEquals(2, one.readLine(dest, 0, 16));
        int c = one.read();
        one.pushback((char) c);                   // the caller's one pushback slot is still free
        assertEquals('c', one.read());
    }

    /**
     * readUntil and readUntilBorrowed asked to stop below ' ' stop at a control character as at a delimiter, and
     * leave it unread; asked to stop below '\0', they stop where the two-delimiter forms stop.
     */
    @Test
    void readUntilStopsBelowAControlCharacterAndLeavesItUnread() {
        char[] dest = new char[16];
        FastReader reader = new FastReader(new StringReader("ab\tc\"d"));
        assertEquals(2, reader.readUntil(dest, 0, 16, '"', '\\', ' '));
        assertEquals("ab", new String(dest, 0, 2));
        assertEquals('\t', reader.read());
        assertEquals(1, reader.readUntil(dest, 0, 16, '"', '\\', ' '));      // "c", up to the quote
        assertEquals('"', reader.read());

        FastReader borrowing = new FastReader(new StringReader("xyz\u0001rest\""));
        FastReader.BufferSlice slice = new FastReader.BufferSlice();
        assertEquals(3, borrowing.readUntilBorrowed(slice, 64, '"', '\\', ' '));
        assertEquals("xyz", new String(slice.getBuffer(), slice.getOffset(), 3));
        slice.release();
        assertEquals(1, borrowing.read());

        // a buffer of 2: the control character is found after a refill
        FastReader small = new FastReader(new StringReader("abcde\nf"), 2, 1);
        assertEquals(5, small.readUntil(dest, 0, 16, '"', '\\', ' '));
        assertEquals('\n', small.read());

        FastReader none = new FastReader(new StringReader("a\tb\"c"));
        assertEquals(3, none.readUntil(dest, 0, 16, '"', '\\', '\0'));       // a tab is plain text below '\0'
        assertEquals("a\tb", new String(dest, 0, 3));
    }

    /** readUntil satisfied by pushed-back characters alone does not read the source (which may block). */
    @Test
    void readUntilSatisfiedByPushbackDoesNotReadTheSource() throws Exception {
        CountDownLatch never = new CountDownLatch(1);
        Reader blocking = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) {
                try {
                    never.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return -1;
            }

            @Override
            public void close() {
            }
        };
        FastReader reader = new FastReader(blocking, 16, 4);
        reader.pushback('x');
        char[] dest = new char[4];
        int[] got = new int[1];
        Thread thread = new Thread(() -> got[0] = reader.readUntil(dest, 0, 1, '"', '\\'));
        thread.setDaemon(true);
        thread.start();
        thread.join(5000);
        boolean blocked = thread.isAlive();
        never.countDown();
        assertTrue(!blocked, "readUntil blocked although the pushback held what it asked for");
        assertEquals(1, got[0]);
        assertEquals('x', dest[0]);

        FastReader atEnd = new FastReader(new StringReader(""), 16, 4);
        assertEquals(-1, atEnd.read());
        atEnd.pushback('z');
        assertEquals(0, atEnd.readUntil(dest, 0, 0, '"', '\\'));  // nothing asked: 0, not end of input
        assertEquals('z', atEnd.read());
    }
}
