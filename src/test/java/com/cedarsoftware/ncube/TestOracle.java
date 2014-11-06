package com.cedarsoftware.ncube;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 11/5/2014.
 */
public class TestOracle
{
    @Test
    public void testOracle() throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection c = DriverManager.getConnection("jdbc:oracle:thin:@10.50.214.14:1526:uwdeskd", "ra_desktop", "p0rtal");

        Object[] list = new NCubeJdbcPersister().getAppNames(c, "NONE");
        assertEquals(7, list.length);
    }
}
