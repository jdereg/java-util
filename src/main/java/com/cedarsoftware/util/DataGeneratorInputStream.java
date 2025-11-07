package com.cedarsoftware.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.function.IntSupplier;

/**
 * A flexible InputStream that generates data on-the-fly using various generation strategies.
 * This class is ideal for testing code that handles large streams, generating synthetic data,
 * or creating pattern-based input without consuming memory to store the data.
 *
 * <p>Data is generated as needed and immediately discarded, making it memory-efficient
 * for testing with very large stream sizes.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Random bytes
 * try (InputStream input = DataGeneratorInputStream.withRandomBytes(1024 * 1024)) {
 *     processStream(input);
 * }
 *
 * // Repeating pattern
 * try (InputStream input = DataGeneratorInputStream.withRepeatingPattern(1024, "Hello")) {
 *     processStream(input);
 * }
 *
 * // Custom generator
 * try (InputStream input = DataGeneratorInputStream.withGenerator(1024, () -&gt; 42)) {
 *     processStream(input);
 * }
 * </pre>
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
public class DataGeneratorInputStream extends InputStream {

    private final long size;
    private long bytesRead;
    private final IntSupplier generator;

    /**
     * Creates a DataGeneratorInputStream with the specified size and generator.
     *
     * @param size the number of bytes this stream will provide before returning -1 (EOF)
     * @param generator the IntSupplier that returns byte values (0-255)
     * @throws IllegalArgumentException if size is negative
     * @throws NullPointerException if generator is null
     */
    public DataGeneratorInputStream(long size, IntSupplier generator) {
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + size);
        }
        if (generator == null) {
            throw new NullPointerException("Generator cannot be null");
        }
        this.size = size;
        this.bytesRead = 0;
        this.generator = generator;
    }

    /**
     * Creates a stream that generates random bytes in the range 0-255 using the default seed (12345L).
     *
     * @param size the number of bytes to generate
     * @return a DataGeneratorInputStream that generates random bytes
     */
    public static DataGeneratorInputStream withRandomBytes(long size) {
        return withRandomBytes(size, 12345L, true);
    }

    /**
     * Creates a stream that generates random bytes in the range 0-255 using the specified seed.
     *
     * @param size the number of bytes to generate
     * @param seed the seed for the random number generator
     * @return a DataGeneratorInputStream that generates random bytes
     */
    public static DataGeneratorInputStream withRandomBytes(long size, long seed) {
        return withRandomBytes(size, seed, true);
    }

    /**
     * Creates a stream that generates random bytes using the specified seed.
     *
     * @param size the number of bytes to generate
     * @param seed the seed for the random number generator
     * @param includeZero if true, generates bytes 0-255; if false, generates bytes 1-255
     * @return a DataGeneratorInputStream that generates random bytes
     */
    public static DataGeneratorInputStream withRandomBytes(long size, long seed, boolean includeZero) {
        Random random = new Random(seed);
        if (includeZero) {
            return new DataGeneratorInputStream(size, () -> random.nextInt(256));
        } else {
            return new DataGeneratorInputStream(size, () -> 1 + random.nextInt(255));
        }
    }

    /**
     * Creates a stream that repeatedly outputs the bytes from the given string (UTF-8 encoded).
     *
     * @param size the number of bytes to generate
     * @param pattern the string pattern to repeat
     * @return a DataGeneratorInputStream that repeats the pattern
     * @throws NullPointerException if pattern is null
     * @throws IllegalArgumentException if pattern is empty
     */
    public static DataGeneratorInputStream withRepeatingPattern(long size, String pattern) {
        if (pattern == null) {
            throw new NullPointerException("Pattern cannot be null");
        }
        byte[] bytes = pattern.getBytes(StandardCharsets.UTF_8);
        return withRepeatingPattern(size, bytes);
    }

    /**
     * Creates a stream that repeatedly outputs the bytes from the given byte array.
     *
     * @param size the number of bytes to generate
     * @param pattern the byte pattern to repeat
     * @return a DataGeneratorInputStream that repeats the pattern
     * @throws NullPointerException if pattern is null
     * @throws IllegalArgumentException if pattern is empty
     */
    public static DataGeneratorInputStream withRepeatingPattern(long size, byte[] pattern) {
        if (pattern == null) {
            throw new NullPointerException("Pattern cannot be null");
        }
        if (pattern.length == 0) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        return new DataGeneratorInputStream(size, new IntSupplier() {
            private int index = 0;
            public int getAsInt() {
                int b = pattern[index] & 0xFF;
                index = (index + 1) % pattern.length;
                return b;
            }
        });
    }

    /**
     * Creates a stream that outputs the same byte value repeatedly.
     *
     * @param size the number of bytes to generate
     * @param constantByte the byte value to repeat (0-255)
     * @return a DataGeneratorInputStream that outputs the constant byte
     */
    public static DataGeneratorInputStream withConstantByte(long size, int constantByte) {
        int byteValue = constantByte & 0xFF;
        return new DataGeneratorInputStream(size, () -> byteValue);
    }

    /**
     * Creates a stream that counts sequentially between two byte values, wrapping when reaching the end.
     * If startByte &lt;= endByte, counts upward. If startByte &gt; endByte, counts downward.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>withSequentialBytes(size, 10, 20) generates: 10, 11, 12, ..., 20, 10, 11, ...</li>
     *   <li>withSequentialBytes(size, 20, 10) generates: 20, 19, 18, ..., 10, 20, 19, ...</li>
     * </ul>
     *
     * @param size the number of bytes to generate
     * @param startByte the starting byte value (0-255)
     * @param endByte the ending byte value (0-255)
     * @return a DataGeneratorInputStream that counts sequentially
     */
    public static DataGeneratorInputStream withSequentialBytes(long size, int startByte, int endByte) {
        final int start = startByte & 0xFF;
        final int end = endByte & 0xFF;

        return new DataGeneratorInputStream(size, new IntSupplier() {
            private int current = start;
            private final boolean countUp = start <= end;

            public int getAsInt() {
                int result = current;

                if (countUp) {
                    if (current == end) {
                        current = start;
                    } else {
                        current++;
                    }
                } else {
                    if (current == end) {
                        current = start;
                    } else {
                        current--;
                    }
                }

                return result;
            }
        });
    }

    /**
     * Creates a stream that generates random proper-case alphabetic strings (like "Xkqmz Pqwer Fgthn")
     * using {@link StringUtilities#getRandomString(Random, int, int)}.
     * Each generated string has its first character uppercase and remaining characters lowercase.
     * Strings are separated by the specified separator byte.
     *
     * @param size the number of bytes to generate
     * @param random the Random instance to use
     * @param minWordLen minimum length of each generated string (inclusive)
     * @param maxWordLen maximum length of each generated string (inclusive)
     * @param separator the byte to place between strings (e.g., ' ' or '\n')
     * @return a DataGeneratorInputStream that generates random strings
     * @throws NullPointerException if random is null
     */
    public static DataGeneratorInputStream withRandomStrings(long size, Random random, int minWordLen, int maxWordLen, int separator) {
        if (random == null) {
            throw new NullPointerException("Random cannot be null");
        }

        return new DataGeneratorInputStream(size, new IntSupplier() {
            private byte[] currentWord = null;
            private int wordIndex = 0;
            private boolean needsSeparator = false;

            public int getAsInt() {
                if (needsSeparator) {
                    needsSeparator = false;
                    currentWord = null;
                    wordIndex = 0;
                    return separator & 0xFF;
                }

                if (currentWord == null || wordIndex >= currentWord.length) {
                    String word = StringUtilities.getRandomString(random, minWordLen, maxWordLen);
                    currentWord = word.getBytes(StandardCharsets.UTF_8);
                    wordIndex = 0;
                    needsSeparator = false;
                }

                int result = currentWord[wordIndex++] & 0xFF;

                if (wordIndex >= currentWord.length) {
                    needsSeparator = true;
                }

                return result;
            }
        });
    }

    /**
     * Creates a stream that uses a custom IntSupplier for generating bytes.
     * The IntSupplier should return values in the range 0-255 representing byte values.
     *
     * @param size the number of bytes to generate
     * @param generator the IntSupplier that returns byte values (0-255)
     * @return a DataGeneratorInputStream that uses the custom generator
     * @throws NullPointerException if generator is null
     */
    public static DataGeneratorInputStream withGenerator(long size, IntSupplier generator) {
        return new DataGeneratorInputStream(size, generator);
    }

    /**
     * Reads the next byte of data from this input stream.
     *
     * @return the next byte of data (0-255), or -1 if the end of the stream is reached
     */
    @Override
    public int read() {
        if (bytesRead >= size) {
            return -1;
        }
        bytesRead++;
        return generator.getAsInt();
    }

    /**
     * Reads up to len bytes of data from this input stream into an array of bytes.
     * This method is more efficient than reading byte-by-byte.
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in array b at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if EOF
     * @throws NullPointerException if b is null
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException("Buffer cannot be null");
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        if (len == 0) {
            return 0;
        }
        if (bytesRead >= size) {
            return -1;
        }

        // Calculate how many bytes we can actually read
        long remaining = size - bytesRead;
        int bytesToRead = (int) Math.min(len, remaining);

        // Generate bytes
        for (int i = 0; i < bytesToRead; i++) {
            b[off + i] = (byte) generator.getAsInt();
        }

        bytesRead += bytesToRead;
        return bytesToRead;
    }

    /**
     * Returns an estimate of the number of bytes that can be read from this input stream
     * without blocking.
     *
     * @return the number of bytes remaining in this stream
     */
    @Override
    public int available() {
        long remaining = size - bytesRead;
        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    /**
     * Skips over and discards n bytes of data from this input stream.
     * Note: The generator's getAsInt() method is still called for each skipped byte
     * to maintain consistency with the generation sequence.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     */
    @Override
    public long skip(long n) {
        if (n <= 0) {
            return 0;
        }
        long remaining = size - bytesRead;
        long bytesToSkip = Math.min(n, remaining);

        // Call generator for each skipped byte to maintain sequence consistency
        for (long i = 0; i < bytesToSkip; i++) {
            generator.getAsInt();
        }

        bytesRead += bytesToSkip;
        return bytesToSkip;
    }
}