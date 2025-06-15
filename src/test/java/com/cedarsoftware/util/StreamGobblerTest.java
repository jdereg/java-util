package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StreamGobblerTest {

    @Test
    void getResultInitiallyNull() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(in);
        assertNull(gobbler.getResult());
    }

    @Test
    void getResultAfterRun() {
        String text = "hello\nworld";
        InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(in);
        gobbler.run();
        String expected = "hello" + System.lineSeparator() + "world" + System.lineSeparator();
        assertEquals(expected, gobbler.getResult());
    }

    private static class ThrowingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("boom");
        }
    }

    @Test
    void getResultWhenIOExceptionOccurs() {
        InputStream in = new ThrowingInputStream();
        StreamGobbler gobbler = new StreamGobbler(in);
        gobbler.run();
        assertEquals("boom", gobbler.getResult());
    }
}
