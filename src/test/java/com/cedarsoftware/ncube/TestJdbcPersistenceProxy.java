package com.cedarsoftware.ncube;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by ken on 8/21/2014.
 */
public class TestJdbcPersistenceProxy extends TestAbstractJdbcPersistenceProxy
{
    @Override
    public InvocationHandler buildInvocationHandler(Object o)
    {
        return new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", "SA", "", FooService.class, o);
    }

    @Test
    public void testAbstractConstructor()
    {
        try
        {
            new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", "SA", "", null, new JdbcFooService());
        }
        catch (NullPointerException e)
        {
            assertEquals("The interface for a persistence proxy cannot be null", e.getMessage());
        }

        try
        {
            new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", "SA", "", FooService.class, null);
        }
        catch (NullPointerException e)
        {
            assertEquals("The adapter for a persistence proxy cannot be null", e.getMessage());
        }
    }


    @Test
    public void testConstructor() throws Exception {
        try {
            new JdbcPersistenceProxy("org.does.not.exist", "jdbc:hsqldb:mem:testdb", "SA", "", FooService.class, new JdbcFooService());
        } catch (RuntimeException e) {
            assertEquals(ClassNotFoundException.class, e.getCause().getClass());
            assertTrue(e.getMessage().startsWith("Unable to locate driver"));
        }

        try {
            new JdbcPersistenceProxy(null, null, "SA", "", FooService.class, new JdbcFooService());
        } catch (NullPointerException e) {
            assertEquals("database url cannot be null...", e.getMessage());
        }

        try {
            new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", null, "", FooService.class, new JdbcFooService());
        } catch (NullPointerException e) {
            assertEquals("database user cannot be null...", e.getMessage());
        }

        try {
            new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", "SA", null, FooService.class, new JdbcFooService());
        } catch (NullPointerException e) {
            assertEquals("database password cannot be null...", e.getMessage());
        }

            JdbcPersistenceProxy proxy = new JdbcPersistenceProxy("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:testdb", "SA", "", FooService.class, new JdbcFooService());
            try (Connection c = proxy.getConnection()) {
                assertNotNull(c);
            }
    }
}
