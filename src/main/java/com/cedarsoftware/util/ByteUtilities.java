/*
 * Copyright (c) Cedar Software, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.util;

public final class ByteUtilities
{
	private static final char[] _hex =
    {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};


    /**
     * <p>
     * {@code StringUtilities} instances should NOT be constructed in standard
     * programming. Instead, the class should be used statically as
     * {@code StringUtilities.trim();}.
     * </p>
     */
	private ByteUtilities() {
		super();
	}

	// Turn hex String into byte[]
	// If string is not even length, return null.

	public static byte[] decode(final String s)
	{
		int len = s.length();
		if (len % 2 != 0)
		{
			return null;
		}

		byte[] bytes = new byte[len / 2];
		int pos = 0;

		for (int i = 0; i < len; i += 2)
		{
			byte hi = (byte)Character.digit(s.charAt(i), 16);
			byte lo = (byte)Character.digit(s.charAt(i + 1), 16);
			bytes[pos++] = (byte)(hi * 16 + lo);
		}

		return bytes;
	}

	/**
	 * Convert a byte array into a printable format containing a String of hex
	 * digit characters (two per byte).
	 *
	 * @param bytes array representation
     * @return String hex digits
	 */
	public static String encode(final byte[] bytes)
	{
		StringBuilder sb = new StringBuilder(bytes.length << 1);
		for (byte aByte : bytes)
		{
			sb.append(convertDigit(aByte >> 4));
			sb.append(convertDigit(aByte & 0x0f));
		}
		return sb.toString();
	}

	/**
	 * Convert the specified value (0 .. 15) to the corresponding hex digit.
	 *
	 * @param value
	 *            to be converted
	 * @return '0'..'F' in char format.
	 */
	private static char convertDigit(final int value)
	{
		return _hex[value & 0x0f];
	}

	/**
	 * @param bytes byte[] of bytes to test
	 * @return true if bytes are gzip compressed, false otherwise.
	 */
	public static boolean isGzipped(byte[] bytes)
	{
		return bytes[0] == (byte)0x1f && bytes[1] == (byte)0x8b;
	}
}
