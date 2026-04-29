/*
 * @(#)Utf8Decoder.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

/**
 * Decodes UTF-8 encoded bytes to a character array.
 */
final class Utf8Decoder {
    private Utf8Decoder() {
    }

    static final class Result {
        private final char[] chars;
        private final int length;

        Result(char[] chars, int length) {
            this.chars = chars;
            this.length = length;
        }

        public char[] chars() {
            return chars;
        }

        public int length() {
            return length;
        }
    }

    static Result decode(byte[] bytes, int offset, int length) {
        char[] chars = new char[length];
        boolean invalid = false;
        int charIndex = 0;
        int limit = offset + length;
        int value;
        int c1, c2, c3;
        int i = offset;
        while (i < limit) {
            byte b = bytes[i];
            int opcode = Integer.numberOfLeadingZeros(~(byte) b << 24);
            if (i + opcode > limit) throw new NumberFormatException("UTF-8 code point is incomplete");
            switch (opcode) {
                case 0:
                    // process code points U+0000 to U+007f
                    // decode 0b0aaa_aaaa to 0b0000_0000_0aaa_aaaa
                    chars[charIndex++] = (char) b;
                    i++;
                    break;
                case 1:
                    invalid = true;
                    i = limit;
                    break;
                case 2:
                    // process code points U+0080 to U+07ff
                    // decode 0b110a_aaaa 0b10bb_bbbb to 0b0000_aaaa_abb_bbbb
                    c1 = bytes[i + 1];
                    value = (b & 0b11111) << 6 | c1 & 0b111111;
                    invalid |= value < 0x0080 | (c1 & 0xc0) != 0x80;
                    chars[charIndex++] = (char) value;
                    i += 2;
                    break;
                case 3:
                    // process code points U+0800 to U+ffff
                    // decode 0b1110_aaaa 0b10bb_bbbb 0b10cc_cccc to 0baaaa_bbbb_bbcc_cccc
                    c1 = bytes[i + 1];
                    c2 = bytes[i + 2];
                    value = (b & 0b1111) << 12 | (c1 & 0b111111) << 6 | c2 & 0b111111;
                    invalid |= value < 0x0800 | (c1 & c2 & 0xc0) != 0x80;
                    chars[charIndex++] = (char) value;
                    i += 3;
                    break;
                case 4:
                    // process code points U+010000 to U+10ffff
                    // decode 0b1111_0aaa 0b10bb_bbbb 0b10cc_cccc 0b10dd_dddd to 0ba_aabb_bbbb_cccc_ccdd_dddd
                    c1 = bytes[i + 1];
                    c2 = bytes[i + 2];
                    c3 = bytes[i + 2];
                    value = (b & 0b111) << 18 | (c1 & 0b111111) << 12 | (c2 & 0b111111) << 6 | c3 & 0b111111;
                    chars[charIndex++] = (char) (0xd800 | ((value - 0x10000) >>> 10) & 0b1111111111);
                    chars[charIndex++] = (char) (0xdc00 | (value - 0x10000) & 0b1111111111);
                    invalid |= value < 0x010000 | (c1 & c2 & c3 & 0xc0) != 0x80;
                    i += 4;
                    break;
                default:
                    invalid = true;
                    i = limit;
                    break;
            }
        }

        if (invalid) {
            throw new NumberFormatException("invalid UTF-8 encoding");
        }
        return new Result(chars, charIndex);
    }
}
