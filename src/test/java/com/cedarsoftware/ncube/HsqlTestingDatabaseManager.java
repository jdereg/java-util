package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class HsqlTestingDatabaseManager extends AbstractJdbcTestingDatabaseManager
{

    public HsqlTestingDatabaseManager(JdbcConnectionProvider p) {
        super(p);
    }

    public void setUp() throws SQLException
    {
        Connection c = provider.getConnection();
        try (Statement s = c.createStatement())
        {
            s.execute("CREATE TABLE n_cube ( " +
                    "n_cube_id bigint NOT NULL, " +
                    "n_cube_nm VARCHAR(100) NOT NULL, " +
                    "tenant_cd CHAR(10) DEFAULT 'NONE', " +
                    "cube_value_bin varbinary(999999), " +
                    "create_dt DATE NOT NULL, " +
                    "create_hid VARCHAR(20), " +
                    "version_no_cd VARCHAR(16) DEFAULT '0.1.0' NOT NULL, " +
                    "status_cd VARCHAR(16) DEFAULT 'SNAPSHOT' NOT NULL, " +
                    "app_cd VARCHAR(20), " +
                    "test_data_bin varbinary(999999), " +
                    "notes_bin varbinary(999999), " +
                    "revision_number bigint DEFAULT '0' NOT NULL, " +
                    "PRIMARY KEY (n_cube_id), " +
                    "UNIQUE (tenant_cd, app_cd, version_no_cd, n_cube_nm, revision_number) " +
                    ");");
        } finally {
            provider.releaseConnection(c);
        }
    }

    public void tearDown() throws SQLException {
        Connection c = provider.getConnection();
        try (Statement s = c.createStatement()) {
            s.execute("DROP TABLE n_cube;");
        } finally {
            provider.releaseConnection(c);
        }
    }

}
