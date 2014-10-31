package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by ken on 8/22/2014.
 */
public class JdbcPersistenceProxy extends AbstractJdbcPersistenceProxy {

    private String url;
    private String user;
    private String password;

    public JdbcPersistenceProxy(String driverClass, String url, String user, String password, Class service, Object adapter) {
        super(service, adapter);

        //  If using a JDBC 4.0 driver we don't need to load the driver Class into memory anymore
        //  because they will have a META-INF/services file specifying the driver classname to be loaded.
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to locate driver class: " + driverClass, e);
            }
        }

        if (url == null) {
            throw new NullPointerException("database url cannot be null...");
        }

        if (user == null) {
            throw new NullPointerException("database user cannot be null...");
        }

        if (password == null) {
            throw new NullPointerException("database password cannot be null...");
        }

        this.url = url;
        this.user = user;
        this.password = password;
    }


    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
