package com.cedarsoftware.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Useful Test utilities for common tasks
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
public class TestUtil
{
    /**
     * Ensure that the passed in source contains all the Strings passed in the 'contains' parameter AND
     * that they appear in the order they are passed in.  This is a better check than simply asserting
     * that a particular error message contains a set of tokens...it also ensures the order in which the
     * tokens appear.  If the strings passed in do not appear in the same order within the source string,
     * an assertion failure happens.  Finally, the Strings are NOT compared with case sensitivity.  This is
     * useful for testing exception message text - ensuring that key values are within the message, without
     * copying the exact message into the test.  This allows more freedom for the author of the code being
     * tested, where changes to the error message would be less likely to break the test.
     * @param source String source string to test, for example, an exception error message being tested.
     * @param contains String comma separated list of Strings that must appear in the source string.  Furthermore,
     * the strings in the contains comma separated list must appear in the source string, in the same order as they
     * are passed in.
     */
    public static void assertContainsIgnoreCase(String source, String... contains) {
        String lowerSource = source.toLowerCase();
        int offset = 0;
        for (String contain : contains) {
            String lowerContain = contain.toLowerCase();
            int idx = lowerSource.indexOf(lowerContain, offset);
            String msg = "'" + contain + "' not found in '" + source + "' (searching from position " + offset + ")";
            assert idx >= 0 : msg;
            offset = idx + lowerContain.length();
        }
    }

    /**
     * Ensure that the passed in source contains all the Strings passed in the 'contains' parameter AND
     * that they appear in the order they are passed in.  This is a better check than simply asserting
     * that a particular error message contains a set of tokens...it also ensures the order in which the
     * tokens appear.  If the strings passed in do not appear in the same order within the source string,
     * false is returned, otherwise true is returned. Finally, the Strings are NOT compared with case sensitivity.
     * This is useful for testing exception message text - ensuring that key values are within the message, without
     * copying the exact message into the test.  This allows more freedom for the author of the code being
     * tested, where changes to the error message would be less likely to break the test.
     * @param source String source string to test, for example, an exception error message being tested.
     * @param contains String comma separated list of Strings that must appear in the source string.  Furthermore,
     * the strings in the contains comma separated list must appear in the source string, in the same order as they
     * are passed in.
     */
    public static boolean checkContainsIgnoreCase(String source, String... contains) {
        String lowerSource = source.toLowerCase();
        int offset = 0;
        for (String contain : contains) {
            String lowerContain = contain.toLowerCase();
            int idx = lowerSource.indexOf(lowerContain, offset);
            if (idx == -1) {
                return false;
            }
            offset = idx + lowerContain.length();
        }
        return true;
    }

    /**
     * Load a resource from the classpath as a string.
     *
     * @param name the resource name relative to the classpath root
     * @return contents of the resource as a UTF-8 string
     * @throws RuntimeException if the resource cannot be read
     */
    public static String fetchResource(String name)
    {
        try
        {
            URL url = TestUtil.class.getResource("/" + name);
            if (url == null) {
                throw new IllegalArgumentException("Resource not found: " + name);
            }
            Path resPath = Paths.get(url.toURI());
            return new String(Files.readAllBytes(resPath), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // Unreachable, but required by compiler
        }
    }
    
    public static boolean isReleaseMode() {
        return Boolean.parseBoolean(System.getProperty("performRelease", "false"));
    }
}
