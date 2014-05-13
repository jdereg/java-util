package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 5/5/2014.
 */
public class TestProxyFactory
{
    @Test
    public void testClassCompliance() throws Exception {
        Class c = ProxyFactory.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<ProxyFactory> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);

        con.setAccessible(true);
        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testProxyFactory() {
        final Set<String> set = new HashSet<String>();

        AInt i = (AInt)ProxyFactory.create(AInt.class, new InvocationHandler(){

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                set.add(method.getName());
                return null;
            }
        });

        assertTrue(set.isEmpty());
        i.foo();
        assertTrue(set.contains("foo"));
        assertFalse(set.contains("bar"));
        assertFalse(set.contains("baz"));
        i.bar();
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("bar"));
        assertFalse(set.contains("baz"));
        i.baz();
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("bar"));
        assertTrue(set.contains("baz"));
    }

    private interface AInt
    {
        public void foo();
        public void bar();
        public void baz();
    }


}
