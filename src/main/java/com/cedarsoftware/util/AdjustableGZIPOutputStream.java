package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A customizable extension of {@link GZIPOutputStream} that allows users to specify the compression level.
 * <p>
 * {@code AdjustableGZIPOutputStream} enhances the functionality of {@code GZIPOutputStream} by providing
 * constructors that let users configure the compression level, enabling control over the trade-off between
 * compression speed and compression ratio.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Supports all compression levels defined by {@link java.util.zip.Deflater}, including:
 *       <ul>
 *           <li>{@link java.util.zip.Deflater#DEFAULT_COMPRESSION}</li>
 *           <li>{@link java.util.zip.Deflater#BEST_SPEED}</li>
 *           <li>{@link java.util.zip.Deflater#BEST_COMPRESSION}</li>
 *           <li>Specific levels from 0 (no compression) to 9 (maximum compression).</li>
 *       </ul>
 *   </li>
 *   <li>Provides constructors to set both the compression level and buffer size.</li>
 *   <li>Fully compatible with the standard {@code GZIPOutputStream} API.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try (OutputStream fileOut = Files.newOutputStream(Paths.get("compressed.gz"));
 *      AdjustableGZIPOutputStream gzipOut = new AdjustableGZIPOutputStream(fileOut, Deflater.BEST_COMPRESSION)) {
 *     gzipOut.write("Example data to compress".getBytes(StandardCharsets.UTF_8));
 * }
 * }</pre>
 *
 * <h2>Additional Notes</h2>
 * <ul>
 *   <li>If the specified compression level is invalid, a {@link java.lang.IllegalArgumentException} will be thrown.</li>
 *   <li>The default compression level is {@link java.util.zip.Deflater#DEFAULT_COMPRESSION} when not specified.</li>
 *   <li>The {@code AdjustableGZIPOutputStream} inherits all thread-safety properties of {@code GZIPOutputStream}.</li>
 * </ul>
 *
 * @see GZIPOutputStream
 * @see java.util.zip.Deflater
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
public class AdjustableGZIPOutputStream extends GZIPOutputStream {
    public AdjustableGZIPOutputStream(OutputStream out, int level) throws IOException {
        super(out);
        def.setLevel(level);
    }

    public AdjustableGZIPOutputStream(OutputStream out, int size, int level) throws IOException {
        super(out, size);
        def.setLevel(level);
    }
}
