package com.cedarsoftware.ncube

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class MySqlTestingDatabaseManager extends AbstractJdbcTestingDatabaseManager
{
    MySqlTestingDatabaseManager(JdbcConnectionProvider p)
    {
        super(p);
    }

    void setUp() throws SQLException
    {
        Connection c = provider.connection
        Statement s = null
        try
        {
            s = c.createStatement()
            // Normally this should NOT be used, however, for the first time creation of your MySQL
            // schema, you will want to run this one time.  You will also need to change
            // TestingDatabaseHelper.test_db = MYSQL instead of HSQL

//            s.execute("drop table if exists ncube.n_cube");
//            s.execute("CREATE TABLE if not exists n_cube (\n" +
//                    "  n_cube_id bigint(20) NOT NULL,\n" +
//                    "  n_cube_nm varchar(250) NOT NULL,\n" +
//                    "  tenant_cd char(10) CHARACTER SET ascii NOT NULL DEFAULT 'NONE',\n" +
//                    "  cube_value_bin longblob,\n" +
//                    "  create_dt timestamp NOT NULL,\n" +
//                    "  create_hid varchar(20) DEFAULT NULL,\n" +
//                    "  version_no_cd varchar(16) NOT NULL,\n" +
//                    "  status_cd varchar(16) NOT NULL DEFAULT 'SNAPSHOT',\n" +
//                    "  app_cd varchar(20) NOT NULL,\n" +
//                    "  test_data_bin longblob,\n" +
//                    "  notes_bin longblob,\n" +
//                    "  revision_number bigint(20) DEFAULT '0',\n" +
//                    "  branch_id varchar(80) NOT NULL DEFAULT 'HEAD',\n" +
//                    "  sha1 varchar(40) DEFAULT NULL,\n" +
//                    "  head_sha1 varchar(40) DEFAULT NULL,\n" +
//                    "  changed int DEFAULT NULL, " +
//                    "  PRIMARY KEY (n_cube_id),\n" +
//                    "  UNIQUE KEY n_cube_unique (n_cube_nm, tenant_cd, app_cd, version_no_cd, branch_id, revision_number),\n" +
//                    "  KEY versionIdx (version_no_cd)\n" +
//                    "  KEY revIdx (revision_number)\n" +
//                    "  KEY branchIdx (branch_id)\n" +
//                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1;");

        }
        finally
        {
            provider.releaseConnection(c)
        }
    }

    void tearDown() throws SQLException
    {
        // don't accidentally erase your MySql database.
    }
}
