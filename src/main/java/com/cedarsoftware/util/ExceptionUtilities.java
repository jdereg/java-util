package com.cedarsoftware.util;

import java.util.concurrent.Callable;

/**
 * Useful Exception Utilities. This class also provides the
 * {@code uncheckedThrow(Throwable)} helper which allows rethrowing any
 * {@link Throwable} without declaring it.
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public final class ExceptionUtilities {
    private ExceptionUtilities() {
        super();
    }

    /**
     * @return Throwable representing the actual cause (most nested exception).
     */
    public static Throwable getDeepestException(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }

        return e;
    }

    /**
     * Executes the provided {@link Callable} and returns its result. If the callable throws any {@link Throwable},
     * the method returns the specified {@code defaultValue} instead.
     *
     * <p>
     * <b>Warning:</b> This method suppresses all {@link Throwable} instances, including {@link Error}s
     * and {@link RuntimeException}s. Use this method with caution, as it can make debugging difficult
     * by hiding critical errors.
     * </p>
     *
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>{@code
     * // Example using safelyIgnoreException with a Callable that may throw an exception
     * String result = safelyIgnoreException(() -> potentiallyFailingOperation(), "defaultValue");
     * LOG.info(result); // Outputs the result of the operation or "defaultValue" if an exception was thrown
     * }</pre>
     *
     * <p>
     * <b>When to Use:</b> Use this method in scenarios where you want to execute a task that might throw
     * an exception, but you prefer to provide a fallback value instead of handling the exception explicitly.
     * This can simplify code in cases where exception handling is either unnecessary or handled elsewhere.
     * </p>
     *
     * <p>
     * <b>Caution:</b> Suppressing all exceptions can obscure underlying issues, making it harder to identify and
     * fix problems. It is generally recommended to handle specific exceptions that you expect and can recover from,
     * rather than catching all {@link Throwable} instances.
     * </p>
     *
     * @param <T>          the type of the result returned by the callable
     * @param callable     the {@link Callable} to execute
     * @param defaultValue the default value to return if the callable throws an exception
     * @return the result of {@code callable.call()} if no exception is thrown, otherwise {@code defaultValue}
     *
     * @throws IllegalArgumentException if {@code callable} is {@code null}
     *
     * @see Callable
     */
    public static <T> T safelyIgnoreException(Callable<T> callable, T defaultValue) {
        try {
            return callable.call();
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Executes the provided {@link Runnable} and safely ignores any exceptions thrown during its execution.
     *
     * <p>
     * <b>Warning:</b> This method suppresses all {@link Throwable} instances, including {@link Error}s
     * and {@link RuntimeException}s. Use this method with caution, as it can make debugging difficult
     * by hiding critical errors.
     * </p>
     *
     * @param runnable the {@code Runnable} to execute
     */
    public static void safelyIgnoreException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Safely Ignore a Throwable or rethrow if it is a Throwable that should
     * not be ignored.
     *
     * @param t Throwable to possibly ignore (ThreadDeath and OutOfMemory are not ignored).
     */
    public static void safelyIgnoreException(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            throw (OutOfMemoryError) t;
        }
    }

    /**
     * Throws any {@link Throwable} without declaring it. Useful when converting Groovy code to Java or otherwise
     * bypassing checked exceptions. No longer do you need to declare checked exceptions, which are not always best
     * handled by the immediate calling class. This will still an IOException, for example, without you declaring as
     * a throws clause forcing the caller to deal with it, where as a higher level more suitable place that catches
     * Exception will still catch it as an IOException (in this case). Helps the shift away from Checked exceptions,
     * which imho, was not a good choice for the Java language.
     *
     * @param t throwable to be rethrown unchecked
     * @param <T> type parameter used to trick the compiler
     * @throws T never actually thrown, but declared for compiler satisfaction
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;  // the cast fools the compiler into thinking this is unchecked
    }
}