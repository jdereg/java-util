package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ProxyFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Created by kpartlow on 11/1/2014.
 */
public abstract class TestAbstractJdbcPersistenceProxy
{
    @Before
    public void prepareSchema() throws Exception {
        try (Connection conn = getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE foo_table ( " +
                        "foo_id bigint NOT NULL, " +
                        "foo_name VARCHAR(100) NOT NULL, " +
                        "PRIMARY KEY (foo_id)" +
                        ");");
            }
        }
    }

    @After
    public void tearDown() throws Exception
    {
        try (Connection conn = getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("DROP TABLE foo_table;");
            }
        }
    }

    private Connection getConnection() throws Exception
    {
        return getDataSource().getConnection();
    }


    private DataSource getDataSource() {
        JDBCDataSource ds = new JDBCDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:testdb");
        ds.setUser("SA");
        ds.setPassword("");
        return ds;
    }

    @Test
    public void testCommit() {
        try
        {
            Connection c = mock(Connection.class);
            doThrow(SQLException.class).when(c).commit();
            AbstractJdbcPersistenceProxy.commit(c);
        } catch (Exception e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to commit active transaction...", e.getMessage());
        }
    }

    @Test
    public void testRollback() {
        try
        {
            Connection c = mock(Connection.class);
            doThrow(SQLException.class).when(c).rollback();
            AbstractJdbcPersistenceProxy.rollback(c);
        } catch (Exception e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to rollback active transaction...", e.getMessage());
        }
    }

    @Test
    public void testConnectionError() {
        try
        {
            Connection c = mock(Connection.class);
            doThrow(SQLException.class).when(c).rollback();
            FooJdbcPersistence foo = ProxyFactory.create(FooJdbcPersistence.class, new MockJdbcPersistenceProxy(FooJdbcPersistence.class, new FooJdbcPersistenceImpl()));
            foo.foo();
            fail("should not make it here");
        } catch (Exception e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to get Connection...", e.getMessage());
        }

    }

    public static class MockJdbcPersistenceProxy extends AbstractJdbcPersistenceProxy
    {
        public MockJdbcPersistenceProxy(Class c, Object o) {
            super(c, o);
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            throw new SQLException("foo");
        }
    }

    private static interface FooJdbcPersistence {
        int foo();
    }

    private static class FooJdbcPersistenceImpl {
        public int foo(Connection c) { return 0; }
    }

    public abstract InvocationHandler buildInvocationHandler(Object o);

    @Test
    public void testAdapter() {
        FooService service = ProxyFactory.create(FooService.class, buildInvocationHandler(new JdbcFooService()));
        assertEquals(null, service.getFoo(1));
        assertTrue(service.saveFoo(1, "baz"));
        assertEquals("baz", service.getFoo(1));

    }

    @Test
    public void testFooServiceThatDoesntAddConnection() {
        try {
            buildInvocationHandler(new FooServiceThatForgetsToImplementConnection());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("does not implement"));
        }
    }

    @Test
    public void testFooServiceThatHasDifferentReturnType() {
        try {
            buildInvocationHandler(new FooServiceThatHasDifferentReturnType());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Return types do not match"));
        }
    }

    @Test
    public void testExceptionThrownDuringCall() {

        try {
            FooService service = ProxyFactory.create(FooService.class, buildInvocationHandler(new FooServiceThatThrowsAnException()));
            service.getFoo(1);
        } catch (IllegalArgumentException e) {
            assertEquals("getFoo threw smart message", e.getMessage());
        }
    }

    interface FooService {
        String getFoo(int fooId);
        boolean saveFoo(int fooId, String name);
    }

    private class FooServiceThatForgetsToImplementConnection
    {
        public String getFoo(int fooId) { return null; }
        public boolean saveFoo(Connection c, int fooId, String name) { return true; }
    }

    private class FooServiceThatHasDifferentReturnType
    {
        public int getFoo(Connection c, int fooId) {  return 5; }
        public boolean saveFoo(Connection c, int fooId, String name) { return true; }
    }

    private class FooServiceThatThrowsAnException {
        public String getFoo(Connection c, int fooId) { throw new IllegalArgumentException("getFoo threw smart message"); }
        public boolean saveFoo(Connection c, int fooId, String name) { return true; }
    }

    public static class JdbcFooService {
        public String getFoo(Connection c, int fooId) {
            try (PreparedStatement statement = c.prepareStatement(
                    "SELECT foo_name FROM foo_table WHERE foo_id = ?"
            )) {
                statement.setInt(1, fooId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        public boolean saveFoo(Connection c, int fooId, String name) {
            try (PreparedStatement statement = c.prepareStatement(
                    "MERGE INTO foo_table USING (VALUES(?, ?)) " +
                            "AS vals(foo_id, foo_name) ON foo_table.foo_id = vals.foo_id " +
                            "WHEN MATCHED THEN UPDATE SET foo_table.foo_name = vals.foo_name " +
                            "WHEN NOT MATCHED THEN INSERT (foo_id, foo_name) values (vals.foo_id, vals.foo_name)"
            ))
            {

                statement.setInt(1, fooId);
                statement.setString(2, name);

                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Only one (1) row should be updated.");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }



}
