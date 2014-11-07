package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class MySqlTestingDatabaseManager implements TestingDatabaseManager
{
    private JdbcConnectionProvider provider;
    public MySqlTestingDatabaseManager(JdbcConnectionProvider p) {
        provider = p;
    }

    public void setUp() throws SQLException {
        Connection c = provider.getConnection();
        try (Statement s = c.createStatement())
        {

            s.execute("drop table if exists ncube.n_cube");

            s.execute("CREATE TABLE `ncube`.n_cube (\n" +
                    "                                n_cube_id bigint NOT NULL,\n" +
                    "                                n_cube_nm varchar(100) NOT NULL,\n" +
                    "                                tenant_cd char(10),\n" +
                    "                                cube_value_bin longtext,\n" +
                    "                                create_dt date NOT NULL,\n" +
                    "                                create_hid varchar(20),\n" +
                    "                                update_hid varchar(20),\n" +
                    "                                version_no_cd varchar(16) NOT NULL,\n" +
                    "                            status_cd varchar(16) DEFAULT 'SNAPSHOT' NOT NULL,\n" +
                    "                            app_cd varchar(20),\n" +
                    "                                test_data_bin longtext,\n" +
                    "                                notes_bin longtext,\n" +
                    "                                revision_number bigint,\n" +
                    "                                PRIMARY KEY (n_cube_id),\n" +
                    "                                UNIQUE (tenant_cd, app_cd, n_cube_nm, version_no_cd, revision_number)\n" +
                    "                            );");

        } finally {
            provider.releaseConnection(c);
        }
    }

    public void tearDown() throws SQLException {
        // don't accidentally erase your MySql database.
    }
}
