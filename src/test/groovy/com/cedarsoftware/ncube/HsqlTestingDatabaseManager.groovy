package com.cedarsoftware.ncube

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
class HsqlTestingDatabaseManager extends AbstractJdbcTestingDatabaseManager
{
    HsqlTestingDatabaseManager(JdbcConnectionProvider p)
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
                    "branch_id VARCHAR(80) DEFAULT 'HEAD' NOT NULL, " +
                    "PRIMARY KEY (n_cube_id), " +
                    "UNIQUE (tenant_cd, app_cd, version_no_cd, branch_id, n_cube_nm, revision_number) " +
                    ")")
        }
        finally
        {
            try { s.close() } catch(Exception e) {}
            provider.releaseConnection(c)
        }
    }

    void tearDown() throws SQLException
    {
        Connection c = provider.connection
        Statement s = null
        try
        {
            s = c.createStatement()
            s.execute("DROP TABLE n_cube;")
        }
        finally
        {
            provider.releaseConnection(c)
        }
    }
}
