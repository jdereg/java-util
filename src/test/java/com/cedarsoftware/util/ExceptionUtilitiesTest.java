package com.cedarsoftware.util;


import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ken Partlow
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
public class ExceptionUtilitiesTest
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor con = ExceptionUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    
    @Test
    void testOutOfMemoryErrorThrown() {
        assertThatExceptionOfType(OutOfMemoryError.class)
                .isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError()));
    }

    @Test
    void testIgnoredExceptions() {
        assertThatNoException()
                .isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException()));
    }

    @Test
    void testGetDeepestException()
    {
        try
        {
            throw new Exception(new IllegalArgumentException("Unable to parse: foo"));
        }
        catch (Exception e)
        {
            Throwable t = ExceptionUtilities.getDeepestException(e);
            assert t != e;
            assert t.getMessage().contains("Unable to parse: foo");
        }
    }

    @Test
    void testCallableExceptionReturnsDefault() {
        String result = ExceptionUtilities.safelyIgnoreException(() -> {
            throw new Exception("fail");
        }, "default");
        assertEquals("default", result);
    }

    @Test
    void testCallableSuccessReturnsValue() {
        String result = ExceptionUtilities.safelyIgnoreException(() -> "value", "default");
        assertEquals("value", result);
    }

    @Test
    void testRunnableExceptionIgnored() {
        AtomicBoolean ran = new AtomicBoolean(false);
        ExceptionUtilities.safelyIgnoreException((Runnable) () -> {
            ran.set(true);
            throw new RuntimeException("boom");
        });
        assertTrue(ran.get());
    }
}
