package com.cedarsoftware.util;


import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void testUncheckedThrowRethrows() {
        assertThatExceptionOfType(java.io.IOException.class)
                .isThrownBy(() -> ExceptionUtilities.uncheckedThrow(new java.io.IOException("fail")));
    }

    @Test
    void testSafelyIgnoreExceptionNullCallableThrows() {
        // Previously the documented IllegalArgumentException was swallowed by the
        // catch(Throwable) and defaultValue was silently returned
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(null, "default"));
    }

    /**
     * A swallowed InterruptedException does not swallow the interrupt. The task overloads catch every Throwable;
     * for an InterruptedException the thread's interrupt flag was left cleared, so a thread asked to stop by
     * shutdownNow() or cancel(true) went on as if nobody had asked.
     */
    @Test
    void safelyIgnoreExceptionKeepsTheInterrupt() {
        try {
            String result = ExceptionUtilities.safelyIgnoreException(() -> {
                throw new InterruptedException("stop");
            }, "default");
            assertEquals("default", result);
            assertTrue(Thread.interrupted(), "the Callable overload restores the interrupt");

            ExceptionUtilities.safelyIgnoreException(() -> ExceptionUtilities.uncheckedThrow(new InterruptedException("stop")));
            assertTrue(Thread.interrupted(), "the Runnable overload restores the interrupt");

            // any other exception leaves the flag alone
            assertEquals("default", ExceptionUtilities.safelyIgnoreException(() -> {
                throw new IllegalStateException("other");
            }, "default"));
            ExceptionUtilities.safelyIgnoreException(() -> {
                throw new IllegalStateException("other");
            });
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();           // never leave the test thread interrupted
        }
    }

    /** A cause chain that loops back on itself ends: the walk stops at the first exception it meets again. */
    @Test
    @org.junit.jupiter.api.Timeout(value = 10, threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD)
    void getDeepestExceptionEndsOnACauseChainThatLoops() {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        a.initCause(b);                                     // a -> b -> a
        Throwable deepest = ExceptionUtilities.getDeepestException(a);
        assertTrue(deepest == a || deepest == b);
        Exception outer = new Exception("outer", a);        // outer -> a -> b -> a
        deepest = ExceptionUtilities.getDeepestException(outer);
        assertTrue(deepest == a || deepest == b);
    }
}
