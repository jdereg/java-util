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

    public MySqlTestingDatabaseManager(JdbcConnectionProvider p)
    {
        provider = p;
    }

    public void setUp() throws SQLException
    {
        Connection c = provider.getConnection();
        try (Statement s = c.createStatement())
        {
            // Normally this should NOT be used, however, for the first time creation of your MySQL
            // schema, you will want to run this one time.  You will also need to change
            // TestingDatabaseHelper.test_db = MYSQL instead of HSQL

//            s.execute("drop table if exists ncube.n_cube");
//            s.execute("CREATE TABLE if not exists n_cube (\n" +
//                    "  n_cube_id bigint(20) NOT NULL,\n" +
//                    "  n_cube_nm varchar(200) NOT NULL,\n" +
//                    "  tenant_cd char(10) CHARACTER SET ascii NOT NULL DEFAULT 'NONE',\n" +
//                    "  cube_value_bin longtext,\n" +
//                    "  create_dt date NOT NULL,\n" +
//                    "  create_hid varchar(20) DEFAULT NULL,\n" +
//                    "  version_no_cd varchar(16) NOT NULL,\n" +
//                    "  status_cd varchar(16) NOT NULL DEFAULT 'SNAPSHOT',\n" +
//                    "  app_cd varchar(20) DEFAULT NULL,\n" +
//                    "  test_data_bin longtext,\n" +
//                    "  notes_bin longtext,\n" +
//                    "  revision_number bigint(20) DEFAULT '1',\n" +
//                    "  PRIMARY KEY (n_cube_id),\n" +
//                    "  UNIQUE KEY n_cube_unique (tenant_cd, app_cd, version_no_cd, n_cube_nm, revision_number),\n" +
//                    "  KEY nameIdx (n_cube_nm),\n" +
//                    "  KEY versionIdx (version_no_cd)\n" +
//                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1;");

        } finally {
            provider.releaseConnection(c);
        }
    }

    public void tearDown() throws SQLException {
        // don't accidentally erase your MySql database.
    }
}
