package com.cedarsoftware.util;


import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

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
        Constructor<ExceptionUtilities> con = ExceptionUtilities.class.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }


    @Test(expected=ThreadDeath.class)
    public void testThreadDeathThrown() {
        ExceptionUtilities.safelyIgnoreException(new ThreadDeath());
    }

    @Test(expected=OutOfMemoryError.class)
    public void testOutOfMemoryErrorThrown() {
        ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError());
    }

    @Test
    public void testIgnoredExceptions() {
        ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException());
    }
}
