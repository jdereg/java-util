package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassUtilitiesResourceLoadingTest {
    static class MapClassLoader extends ClassLoader {
        private final String name;
        private final byte[] data;

        MapClassLoader(String name, byte[] data) {
            super(null);
            this.name = name;
            this.data = data;
        }

        @Override
        public InputStream getResourceAsStream(String resName) {
            if (name.equals(resName)) {
                return new ByteArrayInputStream(data);
            }
            return null;
        }
    }

    @Test
    void shouldLoadResourceFromContextClassLoader() {
        String resName = "context-only.txt";
        byte[] expected = "context loader".getBytes(StandardCharsets.UTF_8);
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new MapClassLoader(resName, expected));
            byte[] result = ClassUtilities.loadResourceAsBytes(resName);
            assertArrayEquals(expected, result);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    @Test
    void shouldThrowWhenResourceMissing() {
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(null);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> ClassUtilities.loadResourceAsBytes("missing.txt"));
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }
}
