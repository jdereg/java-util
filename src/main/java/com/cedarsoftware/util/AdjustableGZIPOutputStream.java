package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class AdjustableGZIPOutputStream extends GZIPOutputStream
{
    public AdjustableGZIPOutputStream(OutputStream out, int level) throws IOException
    {
        super(out);
        def.setLevel(level);
    }

    public AdjustableGZIPOutputStream(OutputStream out, int size, int level) throws IOException
    {
        super(out, size);
        def.setLevel(level);
    }
}
