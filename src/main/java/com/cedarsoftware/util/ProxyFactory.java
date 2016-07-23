package com.cedarsoftware.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Handy utilities for working with Java arrays.
 *
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
public final class ProxyFactory
{
    /**
     * This class should be used statically
     */
    private ProxyFactory() {}

    /**
     * Returns an instance of a proxy class for the specified interfaces
     * that dispatches method invocations to the specified invocation
     * handler.
     *
     * @param	intf the interface for the proxy to implement
     * @param   h the invocation handler to dispatch method invocations to
     * @return	a proxy instance with the specified invocation handler of a
     *		proxy class that is defined by the specified class loader
     *		and that implements the specified interfaces
     * @throws	IllegalArgumentException if any of the restrictions on the
     *		parameters that may be passed to <code>getProxyClass</code>
     *		are violated
     * @throws	NullPointerException if the <code>interfaces</code> array
     *		argument or any of its elements are <code>null</code>, or
     *		if the invocation handler, <code>h</code>, is
     *		<code>null</code>
     */
    public static <T> T create(Class<T> intf, InvocationHandler h) {
        return create(h.getClass().getClassLoader(), intf, h);
    }

    /**
     * Returns an instance of a proxy class for the specified interfaces
     * that dispatches method invocations to the specified invocation
     * handler.
     *
     * @param	loader the class loader to define the proxy class
     * @param	intf the interface for the proxy to implement
     * @param   h the invocation handler to dispatch method invocations to
     * @return	a proxy instance with the specified invocation handler of a
     *		proxy class that is defined by the specified class loader
     *		and that implements the specified interfaces
     * @throws	IllegalArgumentException if any of the restrictions on the
     *		parameters that may be passed to <code>getProxyClass</code>
     *		are violated
     * @throws	NullPointerException if the <code>interfaces</code> array
     *		argument or any of its elements are <code>null</code>, or
     *		if the invocation handler, <code>h</code>, is
     *		<code>null</code>
     */
    public static <T> T create(ClassLoader loader, Class<T> intf, InvocationHandler h) {
        return (T)Proxy.newProxyInstance(loader, new Class[]{intf}, h);
    }
}
