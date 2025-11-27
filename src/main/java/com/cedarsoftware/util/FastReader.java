package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Reader;

/**
 * Buffered, Pushback, Reader that does not use synchronization.   Much faster than the JDK variants because
 * they use synchronization.  Typically, this class is used with a separate instance per thread, so
 * synchronization is not needed.
 * <p>
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
public final class FastReader extends Reader {
    private Reader in;
    private final char[] buf;
    private final int bufferSize;
    private final int pushbackBufferSize;
    private int position; // Current position in the buffer
    private int limit;    // Number of characters currently in the buffer, or -1 for EOF
    private final char[] pushbackBuffer;
    private int pushbackPosition; // Current position in the pushback buffer
    // Line/col tracking removed for performance - use getLastSnippet() for error context

    public FastReader(Reader in) {
        this(in, 8192, 16);
    }

    public FastReader(Reader in, int bufferSize, int pushbackBufferSize) {
        super(in);
        if (bufferSize <= 0 || pushbackBufferSize < 0) {
            throw new IllegalArgumentException("Buffer sizes must be positive");
        }
        this.in = in;
        this.bufferSize = bufferSize;
        this.pushbackBufferSize = pushbackBufferSize;
        this.buf = new char[bufferSize];
        this.pushbackBuffer = new char[pushbackBufferSize];
        this.position = 0;
        this.limit = 0;
        this.pushbackPosition = pushbackBufferSize; // Start from the end of pushbackBuffer
    }

    private void fill() {
        // Once EOF is reached, avoid re-reading.
        if (limit == -1) {
            return;
        }
        if (position >= limit) {
            try {
                limit = in.read(buf, 0, bufferSize);
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            if (limit > 0) {
                position = 0;
            }
        }
    }

    public void pushback(char ch) {
        if (pushbackPosition == 0) {
            ExceptionUtilities.uncheckedThrow(new IOException("Pushback buffer is full"));
        }
        pushbackBuffer[--pushbackPosition] = ch;
    }

    @Override
    public int read() {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }

        // First, serve from pushback buffer if available
        if (pushbackPosition < pushbackBufferSize) {
            return pushbackBuffer[pushbackPosition++];
        }

        fill();
        if (limit == -1) {
            return -1;
        }

        return buf[position++];
    }

    /**
     * Efficiently skip whitespace characters (space, tab, newline, carriage return).
     * This method advances the buffer pointer directly, avoiding repeated method calls
     * and overhead. Optimized for JSON parsing hot paths.
     *
     * @return the first non-whitespace character encountered, or -1 for EOF
     */
    public int skipWhitespace() {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }

        while (true) {
            // Check pushback buffer first
            while (pushbackPosition < pushbackBufferSize) {
                char ch = pushbackBuffer[pushbackPosition++];
                if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
                    return ch;
                }
            }

            // Check main buffer
            fill();
            if (limit == -1) {
                return -1; // EOF
            }

            while (position < limit) {
                char ch = buf[position++];
                if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
                    return ch;
                }
            }
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("inputReader is null"));
        }
        if (len == 0) {
            return 0;
        }

        int charsRead = 0;

        while (len > 0) {
            // Consume from pushback buffer first
            int availableFromPushback = pushbackBufferSize - pushbackPosition;
            if (availableFromPushback > 0) {
                int toRead = Math.min(availableFromPushback, len);
                System.arraycopy(pushbackBuffer, pushbackPosition, cbuf, off, toRead);
                pushbackPosition += toRead;
                off += toRead;
                len -= toRead;
                charsRead += toRead;
            } else {
                // Consume from main buffer
                fill();
                if (limit == -1) {
                    // EOF: return what we've got, or -1 if we have nothing
                    return charsRead > 0 ? charsRead : -1;
                }

                int availableFromMain = limit - position;
                if (availableFromMain <= 0) {
                    // Shouldn't normally happen if fill() behaves, but guard anyway
                    return charsRead > 0 ? charsRead : -1;
                }

                int toRead = Math.min(availableFromMain, len);
                System.arraycopy(buf, position, cbuf, off, toRead);
                position += toRead;
                off += toRead;
                len -= toRead;
                charsRead += toRead;
            }
        }

        return charsRead;
    }

    @Override
    public void close() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            in = null;
        }
    }

    /**
     * @return 0 - line tracking removed for performance. Use getLastSnippet() for error context.
     * @deprecated Line tracking removed for performance optimization
     */
    @Deprecated
    public int getLine() {
        return 0;
    }

    /**
     * @return 0 - column tracking removed for performance. Use getLastSnippet() for error context.
     * @deprecated Column tracking removed for performance optimization
     */
    @Deprecated
    public int getCol() {
        return 0;
    }

    public String getLastSnippet() {
        StringBuilder s = new StringBuilder(position);
        for (int i = 0; i < position; i++) {
            s.append(buf[i]);
        }
        return s.toString();
    }

    /**
     * Scans ahead in buffer to find closing quote, detecting if string has escapes.
     * This is a performance optimization for the common case where JSON strings
     * contain no escape sequences (field names, simple values, etc).
     *
     * - Scans buffer directly (tight loop, CPU cache friendly)
     * - Detects escape-free strings (80-90% of cases)
     * - Avoids StringBuilder overhead for simple strings
     *
     * @return length of string (excluding quotes) if no escapes found, -1 if escapes present or other complexity
     */
    public int scanStringNoEscape() {
        // If pushback buffer has content, use slow path for safety
        if (pushbackPosition < pushbackBufferSize) {
            return -1;
        }

        // Ensure buffer has data
        fill();
        if (limit == -1) {
            return -1; // EOF
        }

        int startPos = position;

        // Scan for closing quote while detecting escapes
        // Note: Direct comparisons outperform lookup tables for char[] buffers
        while (position < limit) {
            char c = buf[position];

            if (c == '"') {
                // Found closing quote - no escapes encountered!
                int length = position - startPos;
                // Don't advance position - caller will consume via extractString()
                position = startPos; // Reset so extractString can advance properly
                return length;
            }

            // Check for escape sequences, control characters, or non-ASCII
            if (c == '\\' || c < 0x20 || c >= 128) {
                // Has escape, control char, or non-ASCII - needs slow path
                position = startPos; // Reset position
                return -1;
            }

            position++;
        }

        // String spans buffer boundary or EOF - use slow path
        position = startPos; // Reset position
        return -1;
    }

    /**
     * Extracts string directly from buffer at current position.
     * Only call this after scanStringNoEscape() returns a valid length.
     *
     * Performance: Creates String directly from buffer without StringBuilder,
     * similar to Jackson/Gson's fast-path string extraction.
     *
     * @param length the length of string to extract (from prior scanStringNoEscape call)
     * @return String extracted from buffer
     */
    public String extractString(int length) {
        // Extract string from buffer (no copy needed - String constructor handles it)
        String result = new String(buf, position, length);

        // Advance position past string content and closing quote
        position += length + 1;

        return result;
    }

    /**
     * Result of scanning a field name from the buffer.
     * Used for fast-path field name parsing in JSON.
     */
    public static final class FieldNameScanResult {
        /** Start position in buffer where field name begins */
        public int startPos;
        /** Length of field name (excluding quotes) */
        public int length;
        /** Pre-computed hash code matching String.hashCode() algorithm */
        public int hash;
        /** True if scan was successful */
        public boolean success;

        private FieldNameScanResult() {}

        void reset() {
            startPos = 0;
            length = 0;
            hash = 0;
            success = false;
        }
    }

    // Reusable result object to avoid allocation
    private final FieldNameScanResult fieldNameScanResult = new FieldNameScanResult();

    /**
     * Scans a JSON field name directly from the buffer, computing hash as we go.
     * This is optimized for the common case where field names are:
     * - Short (< 32 chars)
     * - ASCII only (no escapes)
     * - Entirely within the current buffer
     *
     * The method skips leading whitespace and the opening quote, then scans
     * the field name while computing its hash code.
     *
     * Performance: This enables looking up cached field names without creating
     * intermediate String objects. The hash is computed inline during scanning.
     *
     * @return FieldNameScanResult with position, length, and pre-computed hash
     *         (success=false if escapes found, spans buffer, or other complexity)
     */
    public FieldNameScanResult scanFieldName() {
        FieldNameScanResult result = fieldNameScanResult;
        result.reset();

        // If pushback buffer has content, use slow path
        if (pushbackPosition < pushbackBufferSize) {
            return result;
        }

        // Ensure buffer has data
        fill();
        if (limit == -1) {
            return result; // EOF
        }

        int scanPos = position;

        // Skip whitespace
        while (scanPos < limit) {
            char c = buf[scanPos];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
            scanPos++;
        }

        // Need to refill if we consumed all whitespace
        if (scanPos >= limit) {
            return result; // Spans buffer - use slow path
        }

        // Expect opening quote
        if (buf[scanPos] != '"') {
            return result; // Not a quoted field name - use slow path
        }
        scanPos++;

        // Check buffer space for field name
        if (scanPos >= limit) {
            return result; // Spans buffer
        }

        int startPos = scanPos;
        int hash = 0;

        // Scan field name characters and compute hash inline
        // Note: Direct comparisons outperform lookup tables for char[] buffers
        // because the JIT optimizes simple comparisons very well
        while (scanPos < limit) {
            char c = buf[scanPos];

            if (c == '"') {
                // Found closing quote - success!
                result.startPos = startPos;
                result.length = scanPos - startPos;
                result.hash = hash;
                result.success = true;

                // Advance position past field name and closing quote
                position = scanPos + 1;
                return result;
            }

            // Check for escapes, control characters, or non-ASCII
            if (c == '\\' || c < 0x20 || c >= 128) {
                return result; // Use slow path
            }

            // Simple ASCII character - compute hash and continue
            hash = 31 * hash + c;
            scanPos++;
        }

        // Field name spans buffer boundary - use slow path
        return result;
    }

    /**
     * Creates a String from a previously scanned field name.
     * Call this after scanFieldName() returns success=true.
     *
     * @param scanResult the result from scanFieldName()
     * @return the field name as a String
     */
    public String extractFieldName(FieldNameScanResult scanResult) {
        return new String(buf, scanResult.startPos, scanResult.length);
    }

    /**
     * Compares a scanned field name against a cached String.
     * This avoids creating a String just for comparison.
     *
     * @param scanResult the result from scanFieldName()
     * @param cached the cached string to compare against
     * @return true if they match
     */
    public boolean fieldNameEquals(FieldNameScanResult scanResult, String cached) {
        if (scanResult.length != cached.length()) {
            return false;
        }
        int pos = scanResult.startPos;
        for (int i = 0; i < scanResult.length; i++) {
            if (buf[pos + i] != cached.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Result of scanning a number from the buffer.
     * Contains the parsed value and information about why scanning stopped.
     */
    public static final class NumberScanResult {
        /** The parsed numeric value (valid only if digitCount > 0) */
        public long value;
        /** Number of digits parsed (0 means no valid number found) */
        public int digitCount;
        /** True if the number is negative */
        public boolean negative;
        /** The character that terminated parsing (-1 for EOF, or '.', 'e', 'E', or other terminator) */
        public int stopChar;
        /** True if parsing stopped due to '.', 'e', or 'E' (floating point indicator) */
        public boolean isFloatingPoint;
        /** True if overflow was detected during parsing */
        public boolean overflow;

        // Private constructor - use the instance from FastReader
        private NumberScanResult() {}

        /** Reset for reuse */
        void reset() {
            value = 0;
            digitCount = 0;
            negative = false;
            stopChar = 0;
            isFloatingPoint = false;
            overflow = false;
        }

        /** Check if this represents a successfully parsed simple integer */
        public boolean isSimpleInteger() {
            return digitCount > 0 && !isFloatingPoint && !overflow;
        }
    }

    // Reusable result object to avoid allocation
    private final NumberScanResult numberScanResult = new NumberScanResult();

    /**
     * Scans a number from the buffer, returning detailed information about what was parsed.
     * This method is optimized for JSON number parsing and handles:
     * - Simple integers (returns complete value)
     * - Floating point numbers (returns integer part and sets isFloatingPoint)
     * - Overflow detection (returns partial value and sets overflow)
     *
     * Performance: Scans buffer directly in a tight loop. When a floating point
     * indicator is found, the integer portion is preserved so callers don't need
     * to re-parse those digits.
     *
     * @param firstChar the first character already read (typically '-' or a digit)
     * @return NumberScanResult with parsed value and metadata (reused instance - copy if needed)
     */
    public NumberScanResult scanNumber(int firstChar) {
        NumberScanResult result = numberScanResult;
        result.reset();

        // If pushback buffer has content, signal caller to use slow path
        if (pushbackPosition < pushbackBufferSize) {
            return result; // digitCount = 0 signals failure
        }

        // Ensure buffer has data
        fill();
        if (limit == -1) {
            result.stopChar = -1;
            return result; // EOF
        }

        int scanPos = position;
        long value = 0;
        int digitCount = 0;
        boolean negative = false;

        // Handle first character (already consumed by caller)
        if (firstChar == '-') {
            negative = true;
            // First digit must come from buffer
            if (scanPos >= limit) {
                return result; // Need more data - use slow path
            }
            char firstDigit = buf[scanPos];
            if (firstDigit < '0' || firstDigit > '9') {
                return result; // Invalid number - no digits after '-'
            }
            value = firstDigit - '0';
            digitCount = 1;
            scanPos++;
        } else if (firstChar >= '0' && firstChar <= '9') {
            value = firstChar - '0';
            digitCount = 1;
        } else {
            return result; // Invalid start character
        }

        // Scan remaining digits from buffer
        while (scanPos < limit) {
            char c = buf[scanPos];

            if (c >= '0' && c <= '9') {
                long prevValue = value;
                value = value * 10 + (c - '0');
                digitCount++;

                // Check for overflow
                if (value < prevValue) {
                    // Overflow detected - return what we have
                    result.value = negative ? -prevValue : prevValue;
                    result.digitCount = digitCount - 1;
                    result.negative = negative;
                    result.stopChar = c;
                    result.overflow = true;
                    // Advance position to just before the overflow digit
                    position = scanPos;
                    return result;
                }

                scanPos++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                // Floating point - return integer portion
                result.value = negative ? -value : value;
                result.digitCount = digitCount;
                result.negative = negative;
                result.stopChar = c;
                result.isFloatingPoint = true;
                // Advance position past the digits we consumed, but NOT past the '.' or 'e'
                position = scanPos;
                return result;
            } else {
                // End of number - found non-digit terminator
                break;
            }
        }

        // Check if we hit buffer boundary (number might continue in next buffer)
        if (scanPos >= limit) {
            // Number might span buffer boundary - signal to use slow path
            // Don't advance position - caller needs to re-read
            return result; // digitCount > 0 but we return without advancing position
        }

        // Success - complete integer parsed
        result.value = negative ? -value : value;
        result.digitCount = digitCount;
        result.negative = negative;
        result.stopChar = buf[scanPos]; // The terminating character

        // Advance position past the digits we consumed
        position = scanPos;

        return result;
    }
}
