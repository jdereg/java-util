package com.cedarsoftware.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Created by kpartlow on 4/30/2014.
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
