package com.cedarsoftware.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Comprehensive test cases for FastWriter
 */
public class FastWriterTest {

    private StringWriter stringWriter;
    private FastWriter fastWriter;
    private static final int CUSTOM_BUFFER_SIZE = 16;

    @BeforeEach
    public void setUp() {
        stringWriter = new StringWriter();
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    // Constructor Tests
    @Test
    public void testConstructorWithDefaultSize() {
        fastWriter = new FastWriter(stringWriter);
        assertNotNull(fastWriter);
    }

    @Test
    public void testConstructorWithCustomSize() {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        assertNotNull(fastWriter);
    }

    // Single Character Write Tests
    @Test
    public void testWriteSingleChar() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write('a');
        fastWriter.flush();
        assertEquals("a", stringWriter.toString());
    }

    @Test
    public void testWriteMultipleChars() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write('a');
        fastWriter.write('b');
        fastWriter.write('c');
        fastWriter.flush();
        assertEquals("abc", stringWriter.toString());
    }

    @Test
    public void testWriteCharsToFillBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // Write enough characters to fill buffer minus one
        for (int i = 0; i < CUSTOM_BUFFER_SIZE - 1; i++) {
            fastWriter.write('x');
        }

        // At this point, buffer should be filled but not flushed
        assertEquals("", stringWriter.toString());

        // Create a string of 'x' characters (CUSTOM_BUFFER_SIZE - 1) times
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE - 1; i++) {
            expected.append('x');
        }
        String expectedString = expected.toString();

        // This will trigger a flush due to buffer being full
        fastWriter.write('y');
        assertEquals(expectedString, stringWriter.toString());

        // Final character should still be in buffer
        fastWriter.flush();
        assertEquals(expectedString + 'y', stringWriter.toString());
    }

    // Character Array Tests
    @Test
    public void testWriteEmptyCharArray() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write(new char[0], 0, 0);
        fastWriter.flush();
        assertEquals("", stringWriter.toString());
    }

    @Test
    public void testWriteSmallCharArray() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write(new char[]{'a', 'b', 'c'}, 0, 3);
        fastWriter.flush();
        assertEquals("abc", stringWriter.toString());
    }

    @Test
    public void testWriteCharArrayWithOffset() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write(new char[]{'a', 'b', 'c', 'd', 'e'}, 1, 3);
        fastWriter.flush();
        assertEquals("bcd", stringWriter.toString());
    }

    @Test
    public void testWriteCharArrayExactlyBufferSize() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        char[] array = new char[CUSTOM_BUFFER_SIZE];
        for (int i = 0; i < array.length; i++) {
            array[i] = (char)('a' + i % 26);
        }

        // When writing an array exactly the buffer size,
        // it will write directly to the underlying writer
        fastWriter.write(array, 0, array.length);
        String expected = new String(array);
        assertEquals(expected, stringWriter.toString());

        // Buffer should be empty, we can write more
        fastWriter.write('!');
        fastWriter.flush();
        assertEquals(expected + "!", stringWriter.toString());
    }

    @Test
    public void testWriteCharArrayLargerThanBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        char[] array = new char[CUSTOM_BUFFER_SIZE * 2 + 5];
        for (int i = 0; i < array.length; i++) {
            array[i] = (char)('a' + i % 26);
        }

        fastWriter.write(array, 0, array.length);
        // Array larger than buffer should be written directly
        String expected = new String(array);
        assertEquals(expected, stringWriter.toString());
    }

    // String Write Tests
    @Test
    public void testWriteEmptyString() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("", 0, 0);
        fastWriter.flush();
        assertEquals("", stringWriter.toString());
    }

    @Test
    public void testWriteSmallString() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("Hello, world!", 0, 13);
        fastWriter.flush();
        assertEquals("Hello, world!", stringWriter.toString());
    }

    @Test
    public void testWriteStringWithOffset() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("Hello, world!", 7, 5);
        fastWriter.flush();
        assertEquals("world", stringWriter.toString());
    }

    @Test
    public void testWriteStringExactlyBufferSize() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        String str = "abcdefghijklmnop"; // 16 chars to match CUSTOM_BUFFER_SIZE

        fastWriter.write(str, 0, CUSTOM_BUFFER_SIZE);
        // String fills buffer exactly, which triggers an auto-flush
        assertEquals(str, stringWriter.toString());
    }

    @Test
    public void testWriteStringLargerThanBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 3 + 5; i++) {
            sb.append((char)('a' + i % 26));
        }
        String str = sb.toString();

        fastWriter.write(str, 0, str.length());
        // The final chunk (< buffer size) remains buffered and needs to be flushed
        fastWriter.flush();
        assertEquals(str, stringWriter.toString());
    }

    @Test
    public void testWriteMultipleStringsWithBufferOverflow() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        fastWriter.write("abcdefg", 0, 7); // 7 chars
        fastWriter.write("hijklmn", 0, 7); // 7 more chars (14 total)

        // Buffer still not full
        assertEquals("", stringWriter.toString());

        // This will fill and overflow the buffer (16 + 5 = 21 chars total)
        fastWriter.write("opqrs", 0, 5);
        // The buffer will be filled exactly (14+2=16 chars) before flushing
        assertEquals("abcdefghijklmnop", stringWriter.toString());

        fastWriter.flush();
        // After flushing, we'll see all characters
        assertEquals("abcdefghijklmnopqrs", stringWriter.toString());
    }

    @Test
    public void testWriteLargeStringWithPartialBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
        fastWriter.write("abc", 0, 3); // Fill buffer partially

        // Now write a string larger than remaining buffer space (13 chars)
        String largeString = "defghijklmnopqrstuvwxyz"; // 23 chars
        fastWriter.write(largeString, 0, largeString.length());

        // Buffer should be flushed and entire content written
        fastWriter.flush();
        assertEquals("abc" + largeString, stringWriter.toString());
    }

    @Test
    public void testConstructorWithInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new FastWriter(stringWriter, 0));
    }

    @Test
    public void testConstructorWithNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new FastWriter(stringWriter, -10));
    }

    @Test
    public void testWriteCharToClosedWriter() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.close();
        assertThrows(IOException.class, () -> fastWriter.write('x'));
    }

    @Test
    public void testWriteCharArrayWithNegativeOffset() {
        fastWriter = new FastWriter(stringWriter);
        assertThrows(IndexOutOfBoundsException.class,
                () -> fastWriter.write(new char[]{'a', 'b', 'c'}, -1, 2));
    }

    @Test
    public void testWriteCharArrayWithNegativeLength() {
        fastWriter = new FastWriter(stringWriter);
        assertThrows(IndexOutOfBoundsException.class,
                () -> fastWriter.write(new char[]{'a', 'b', 'c'}, 0, -1));
    }

    @Test
    public void testWriteCharArrayWithInvalidRange() {
        fastWriter = new FastWriter(stringWriter);
        assertThrows(IndexOutOfBoundsException.class,
                () -> fastWriter.write(new char[]{'a', 'b', 'c'}, 1, 3));
    }

    @Test
    public void testWriteCharArrayToClosedWriter() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.close();
        assertThrows(IOException.class,
                () -> fastWriter.write(new char[]{'a', 'b', 'c'}, 0, 3));
    }

    @Test
    public void testWriteStringToClosedWriter() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.close();
        assertThrows(IOException.class, () -> fastWriter.write("test", 0, 4));
    }

    // Flush Tests
    @Test
    public void testFlushEmptyBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.flush(); // Should not do anything with empty buffer
        assertEquals("", stringWriter.toString());
    }

    @Test
    public void testFlushWithContent() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("test");
        assertEquals("", stringWriter.toString()); // No output yet

        fastWriter.flush();
        assertEquals("test", stringWriter.toString()); // Content flushed
    }

    // Close Tests
    @Test
    public void testCloseFlushesBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("test");
        assertEquals("", stringWriter.toString()); // No output yet

        fastWriter.close();
        assertEquals("test", stringWriter.toString()); // Content flushed on close
    }

    @Test
    public void testDoubleClose() throws IOException {
        fastWriter = new FastWriter(stringWriter);
        fastWriter.write("test");
        fastWriter.close();
        fastWriter.close(); // Second close should be a no-op
        assertEquals("test", stringWriter.toString());
    }

    // Mock Writer Tests
    @Test
    public void testWithMockWriter() throws IOException {
        MockWriter mockWriter = new MockWriter();
        fastWriter = new FastWriter(mockWriter, CUSTOM_BUFFER_SIZE);

        fastWriter.write("test");
        assertEquals(0, mockWriter.getWriteCount()); // Nothing written yet

        fastWriter.flush();
        assertEquals(1, mockWriter.getWriteCount()); // One write operation
        assertEquals("test", mockWriter.getOutput());

        fastWriter.write("more");
        fastWriter.close();
        assertEquals(2, mockWriter.getWriteCount()); // Second write on close
        assertEquals("testmore", mockWriter.getOutput());
        assertTrue(mockWriter.isClosed());
    }

    @Test
    public void testWriteCharArrayPartiallyFilledBuffer() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // First, partially fill the buffer (fill 10 chars of our 16-char buffer)
        String firstPart = "abcdefghij";
        fastWriter.write(firstPart, 0, firstPart.length());

        // At this point, buffer has 10 chars, with 6 spaces remaining
        assertEquals("", stringWriter.toString()); // Nothing flushed yet

        // Now write 8 chars - smaller than buffer size (16) but larger than remaining space (6)
        // This should trigger the flush condition: if (len > cb.length - nextChar)
        char[] secondPart = {'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r'};
        fastWriter.write(secondPart, 0, secondPart.length);

        // First part should be flushed
        assertEquals(firstPart, stringWriter.toString());

        // Second part is in the buffer
        fastWriter.flush();
        assertEquals(firstPart + new String(secondPart), stringWriter.toString());
    }

    @Test
    public void testWriteStringExactMultipleOfBufferSize() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // Create a string exactly 2 times the buffer size (32 chars for 16-char buffer)
        // This ensures len will be 0 after processing full chunks
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 2; i++) {
            sb.append((char)('a' + i % 26));
        }
        String str = sb.toString();

        // Write the string - it should process in exactly 2 full chunks
        fastWriter.write(str, 0, str.length());

        // All content should be written since it's processed in full buffer chunks
        // with nothing left for the "final fragment" code path
        assertEquals(str, stringWriter.toString());

        // Write something else to confirm the buffer is empty
        fastWriter.write('!');
        fastWriter.flush();
        assertEquals(str + "!", stringWriter.toString());
    }

    @Test
    public void testWriteStringWhenBufferExactlyFull() throws IOException {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // First completely fill the buffer via string writing
        // This is important because it behaves differently from char writing
        String fillContent = "abcdefghijklmnop"; // Exactly CUSTOM_BUFFER_SIZE chars
        fastWriter.write(fillContent, 0, fillContent.length());

        // At this point the buffer is full and already flushed (String write behavior)
        assertEquals(fillContent, stringWriter.toString());

        // Now nextChar is 0 (empty buffer), we'll make it full without flushing
        // by accessing the buffer directly using reflection
        try {
            java.lang.reflect.Field nextCharField = FastWriter.class.getDeclaredField("nextChar");
            nextCharField.setAccessible(true);
            nextCharField.setInt(fastWriter, CUSTOM_BUFFER_SIZE);

            // Now write a string when buffer is exactly full (available = 0)
            String additionalContent = "MoreContent";
            fastWriter.write(additionalContent, 0, additionalContent.length());

            // Since available was 0, it skipped the first if-block
            assertEquals(fillContent, stringWriter.toString());

            fastWriter.flush();
            assertEquals(fillContent + additionalContent, stringWriter.toString());
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }

    @Test
    public void testWriteCharArrayWithIntegerOverflow() {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // Create a character array
        char[] cbuf = {'a', 'b', 'c', 'd'};

        // Test the integer overflow condition ((off + len) < 0)
        // This happens when off is positive and len is negative but has a large absolute value
        // such that their sum overflows to a negative number
        int off = Integer.MAX_VALUE - 10;
        int len = 20; // when added to off, this will cause overflow to a negative number

        // This should throw IndexOutOfBoundsException because (off + len) < 0 due to integer overflow
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(cbuf, off, len));
    }

    @Test
    public void testWriteCharArrayWithNegativeArraySizeCheck() {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // Create a character array
        char[] cbuf = {'a', 'b', 'c', 'd'};

        // Test with offset that is beyond array bounds
        int off = cbuf.length + 1; // One past the end of the array
        int len = 1;

        // This should throw IndexOutOfBoundsException because off > cbuf.length
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(cbuf, off, len));
    }

    @Test
    public void testWriteCharArrayWithExplicitIntegerOverflow() {
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);

        // Create a larger character array to avoid off > cbuf.length condition
        char[] cbuf = new char[100];

        // The key is to use values that will definitely cause integer overflow
        // but not trigger the other conditions first
        int off = 10; // Positive and < cbuf.length
        int len = Integer.MAX_VALUE; // Adding this to off will overflow

        // This should hit the (off + len) < 0 condition specifically
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(cbuf, off, len));
    }
    
    /**
     * A mock Writer implementation that tracks write operations
     */
    private static class MockWriter extends Writer {
        private final StringBuilder sb = new StringBuilder();
        private int writeCount = 0;
        private boolean closed = false;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            writeCount++;
            sb.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            // No action needed
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        public String getOutput() {
            return sb.toString();
        }

        public int getWriteCount() {
            return writeCount;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}