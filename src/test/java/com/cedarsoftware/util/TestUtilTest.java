package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
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
public class TestUtilTest
{
    @Test
    public void testAssert()
    {
        TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test");
        try
        {
            TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Source", "string", "Text");
        }
        catch (AssertionError e)
        {
            TestUtil.assertContainsIgnoreCase(e.getMessage(), "not found", "string","test");
        }

        try
        {
            TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Test", "Source", "string");
        }
        catch (AssertionError e)
        {
            TestUtil.assertContainsIgnoreCase(e.getMessage(), "source", "not found", "test");
        }

    }
    @Test
    public void testContains()
    {
        assert TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test");
        assert !TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Text");
        assert !TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Test", "Source", "string");
    }

    @Test
    public void testIsReleaseModeDefaultFalse()
    {
        String original = System.getProperty("performRelease");
        System.clearProperty("performRelease");
        try
        {
            assertFalse(TestUtil.isReleaseMode());
        }
        finally
        {
            if (original != null)
            {
                System.setProperty("performRelease", original);
            }
        }
    }

    @Test
    public void testIsReleaseModeTrue()
    {
        String original = System.getProperty("performRelease");
        System.setProperty("performRelease", "true");
        try
        {
            assertTrue(TestUtil.isReleaseMode());
        }
        finally
        {
            if (original == null)
            {
                System.clearProperty("performRelease");
            }
            else
            {
                System.setProperty("performRelease", original);
            }
        }
    }

    @Test
    public void testIsReleaseModeExplicitFalse()
    {
        String original = System.getProperty("performRelease");
        System.setProperty("performRelease", "false");
        try
        {
            assertFalse(TestUtil.isReleaseMode());
        }
        finally
        {
            if (original == null)
            {
                System.clearProperty("performRelease");
            }
            else
            {
                System.setProperty("performRelease", original);
            }
        }
    }
}
