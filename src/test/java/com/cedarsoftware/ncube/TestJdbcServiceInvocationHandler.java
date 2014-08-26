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

/**
 * Created by ken on 8/21/2014.
 */
public class TestJdbcServiceInvocationHandler
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
    public void testAdapter() {
        InvocationHandler h = new JdbcServiceInvocationHandler(getDataSource(), FooService.class, new JdbcFooService());
        FooService service = ProxyFactory.create(FooService.class, h);
        assertEquals(null, service.getFoo(1));
        assertTrue(service.saveFoo(1, "baz"));
        assertEquals("baz", service.getFoo(1));

    }

    @Test
    public void testFooServiceThatDoesntAddConnection() {
        try {
            new JdbcServiceInvocationHandler(getDataSource(), FooService.class, new FooServiceThatForgetsToImplementConnection());
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: Adapter class 'FooServiceThatForgetsToImplementConnection' does not implement: getFoo(Connection,int)", e.toString());
        }
    }

    @Test
    public void testExceptionThrowDuringCall() {

        try {
            JdbcServiceInvocationHandler h = new JdbcServiceInvocationHandler(getDataSource(), FooService.class, new FooServiceThatThrowsAnException());
            FooService service = ProxyFactory.create(FooService.class, h);
            service.getFoo(1);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private interface FooService {
        String getFoo(int fooId);
        boolean saveFoo(int fooId, String name);
    }

    private class FooServiceThatForgetsToImplementConnection
    {
        String getFoo(int fooId) { return null; }
        boolean saveFoo(int fooId, String name) { return true; }
    }

    private class FooServiceThatThrowsAnException {
        public String getFoo(Connection c, int fooId) { throw new IllegalArgumentException(); }
        public boolean saveFoo(Connection c, int fooId, String name) { return true; }
    }

    private class JdbcFooService {
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
            )) {

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
