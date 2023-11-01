package com.cedarsoftware.util;


import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ken Partlow
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
public class TestExceptionUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor con = ExceptionUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    
    public void testOutOfMemoryErrorThrown()
    {
        try
        {
            ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError());
            fail("should not make it here");
        }
        catch (OutOfMemoryError e)
        {
        }
    }

    @Test
    public void testIgnoredExceptions() {
        ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException());
    }

    @Test
    public void testGetDeepestException()
    {
        try
        {
            Converter.convert("foo", Date.class);
            fail();
        }
        catch (Exception e)
        {
            Throwable t = ExceptionUtilities.getDeepestException(e);
            assert t != e;
            assert t.getMessage().equals("Unable to parse: foo");
        }
    }
}
