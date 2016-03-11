package com.cedarsoftware.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is used in conjunction with the Executor class.  Example
 * usage:
 *
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
public class StreamGobbler implements Runnable
{
    private final InputStream _inputStream;
    private String _result;

    StreamGobbler(InputStream is)
    {
        _inputStream = is;
    }

    public String getResult()
    {
        return _result;
    }

    public void run()
    {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try
        {
            isr = new InputStreamReader(_inputStream);
            br = new BufferedReader(isr);
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
            {
                output.append(line);
                output.append(System.getProperty("line.separator"));
            }
            _result = output.toString();
        }
        catch (IOException e)
        {
            _result = e.getMessage();
        }
        finally
        {
            IOUtilities.close(isr);
            IOUtilities.close(br);
        }
    }
}
