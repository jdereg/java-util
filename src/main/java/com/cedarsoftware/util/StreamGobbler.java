package com.cedarsoftware.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class is used in conjunction with the Executor class.  Example
 * usage:
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
public class StreamGobbler implements Runnable
{
    private final InputStream _inputStream;
    private final Charset _charset;
    private String _result;

    StreamGobbler(InputStream is)
    {
        this(is, StandardCharsets.UTF_8);
    }

    StreamGobbler(InputStream is, Charset charset)
    {
        _inputStream = is;
        _charset = charset;
    }

    /**
     * Returns all text that was read from the underlying input stream.
     *
     * @return captured output from the stream
     */
    public String getResult()
    {
        return _result;
    }

    /**
     * Continuously reads from the supplied input stream until it is exhausted.
     * The collected data is stored so it can be retrieved via {@link #getResult()}.
     */
    public void run()
    {
        String lineSeparator = System.lineSeparator();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(_inputStream, _charset))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
            {
                output.append(line);
                output.append(lineSeparator);
            }
            _result = output.toString();
        }
        catch (IOException e)
        {
            _result = e.getMessage();
        }
    }
}

