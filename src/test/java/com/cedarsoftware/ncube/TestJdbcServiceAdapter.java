package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ProxyFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
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
public class TestJdbcServiceAdapter {


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
        JdbcServiceAdapter h = new JdbcServiceAdapter(getDataSource(), FooService.class, new JdbcFooService());
        FooService service = ProxyFactory.create(FooService.class, h);
        assertEquals(null, service.getFoo(1));
        assertTrue(service.saveFoo(1, "baz"));
        assertEquals("baz", service.getFoo(1));
    }

    private interface FooService {
        public String getFoo(int fooId);
        public boolean saveFoo(int fooId, String name);
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
                    "INSERT INTO foo_table (foo_id, foo_name) VALUES (?, ?)"
            )) {

//            try(PreparedStatement statement = c.prepareStatement(
//                    "MERGE INTO foo_table USING (VALUES(CAST (? AS INT), CAST (? AS VARCHAR(100)))) " +
//                    "AS I(foo_id, foo_name) ON foo_table.foo_id = I.foo_id " +
//                    "WHEN MATCHED THEN UPDATE SET foo_table.foo_name = I.foo_name" +
//                    "WHEN NOT MATCHED THEN INSERT (foo_id, foo_name) values (I.foo_id, I.foo_name)")) {

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
