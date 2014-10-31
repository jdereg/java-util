package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class HsqlTestingDatabaseManager
{
    public void setUp(Connection c) throws SQLException
    {
        try (Statement s = c.createStatement())
        {
            s.execute("CREATE TABLE n_cube ( " +
                    "n_cube_id bigint NOT NULL, " +
                    "n_cube_nm VARCHAR(100) NOT NULL, " +
                    "tenant_cd CHAR(10), " +
                    "cube_value_bin varbinary(999999), " +
                    "create_dt DATE NOT NULL, " +
                    "update_dt DATE DEFAULT NULL, " +
                    "create_hid VARCHAR(20), " +
                    "update_hid VARCHAR(20), " +
                    "version_no_cd VARCHAR(16) DEFAULT '0.1.0' NOT NULL, " +
                    "status_cd VARCHAR(16) DEFAULT 'SNAPSHOT' NOT NULL, " +
                    "sys_effective_dt DATE DEFAULT SYSDATE NOT NULL, " +
                    "sys_expiration_dt DATE, " +
                    "business_effective_dt DATE DEFAULT SYSDATE, " +
                    "business_expiration_dt DATE, " +
                    "app_cd VARCHAR(20), " +
                    "test_data_bin varbinary(999999), " +
                    "notes_bin varbinary(999999), " +
                    "revision_number bigint, " +
                    "PRIMARY KEY (n_cube_id), " +
                    "UNIQUE (tenant_cd, n_cube_nm, version_no_cd, app_cd, status_cd, revision_number) " +
                    ");");
        }
    }

    public void tearDown(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("DROP TABLE n_cube;");
        }
    }
}
