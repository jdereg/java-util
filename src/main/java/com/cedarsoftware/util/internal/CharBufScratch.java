package com.cedarsoftware.util.internal;

/**
 * <h2>Internal API — not for external use.</h2>
 *
 * Per-thread scratch {@code char[]} buffer for callers that walk a {@link String} as
 * raw chars in a tight loop. {@link #getChars(String, int)} bulk-copies the input via
 * {@link String#getChars(int, int, char[], int)} — a HotSpot intrinsic that uses SIMD
 * (SSE2/AVX2/NEON) to expand compact-string {@code byte[]} storage into chars in one
 * pass. Subsequent {@code buf[i]} reads run as straight-line array access, avoiding
 * the per-character {@code charAt} dispatch (LATIN1/UTF16 coder branch + bounds check
 * + method dispatch). Typical wins are 20-50% on strings of 7+ characters.
 *
 * <p>This class is exposed to {@code com.cedarsoftware:json-io} via a qualified
 * JPMS export ({@code exports com.cedarsoftware.util.internal to com.cedarsoftware.io}).
 * Calling it from outside Cedar Software libraries is unsupported; signatures and
 * semantics may change without notice across minor releases. Use {@link String#getChars}
 * directly with your own buffer if you need this functionality from external code.</p>
 *
 * <h3>Re-entrancy contract</h3>
 * The returned array is a shared per-thread buffer. It is valid only until the next
 * call to {@link #getChars(String, int)} on the same thread. Consume the contents (or
 * copy them out) before invoking any other {@code getChars}-based helper or calling
 * methods that might do so transitively. Do NOT store the reference beyond the
 * immediate scope. If you need two buffers simultaneously on the same thread (e.g.,
 * comparing two strings), copy one out or allocate a local {@code new char[]}.
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
public final class CharBufScratch {

    private static final int CHAR_BUF_SIZE = 256;
    private static final ThreadLocal<char[]> TL_CHAR_BUF =
            ThreadLocal.withInitial(() -> new char[CHAR_BUF_SIZE]);

    private CharBufScratch() {
        // no instances
    }

    /**
     * Returns a thread-local {@code char[]} buffer populated with the first {@code len}
     * characters of {@code s} via {@link String#getChars(int, int, char[], int)}.
     *
     * <p>The two-arg signature lets the caller pass a length it already computed (avoiding
     * a redundant {@code s.length()} call). Passing {@code len < s.length()} copies only
     * the first {@code len} chars (prefix extraction); passing {@code len > s.length()}
     * raises {@link StringIndexOutOfBoundsException} via the underlying
     * {@code String.getChars} call.
     *
     * <p>See class-level Javadoc for the re-entrancy contract.
     *
     * @param s   the string whose characters to extract (must not be null)
     * @param len the number of characters to copy, starting at index 0. Must satisfy
     *            {@code 0 <= len <= s.length()}.
     * @return a thread-local char[] whose first {@code len} entries hold the string's
     *         characters; indices beyond that are unspecified.
     */
    public static char[] getChars(String s, int len) {
        char[] buf = getCharBuf(len);
        s.getChars(0, len, buf, 0);
        return buf;
    }

    /**
     * Convenience overload that computes {@code s.length()} once and delegates to
     * {@link #getChars(String, int)}. Prefer the two-arg form when the caller already
     * has the length available.
     *
     * <p>All thread-local semantics of {@link #getChars(String, int)} apply here.
     *
     * @param s the string whose characters to extract (must not be null)
     * @return a thread-local char[] whose first {@code s.length()} entries hold the
     *         string's characters; indices beyond that are unspecified.
     * @see #getChars(String, int)
     */
    public static char[] getChars(String s) {
        return getChars(s, s.length());
    }

    /** Internal: get a reusable char buffer from ThreadLocal, growing if needed. */
    private static char[] getCharBuf(int minSize) {
        char[] buf = TL_CHAR_BUF.get();
        if (minSize > buf.length) {
            buf = new char[minSize];
            TL_CHAR_BUF.set(buf);
        }
        return buf;
    }
}
