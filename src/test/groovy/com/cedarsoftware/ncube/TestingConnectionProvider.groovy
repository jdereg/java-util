package com.cedarsoftware.ncube

import java.sql.Connection
import java.sql.DriverManager

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestingConnectionProvider implements JdbcConnectionProvider
{
    private String databaseUrl
    private String user
    private String password

    public TestingConnectionProvider(String driverClass, String databaseUrl, String user, String password)
    {
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass)
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException('Could not load:  ' + driverClass, e)
            }
        }

        this.databaseUrl = databaseUrl
        this.user = user
        this.password = password
    }

    public Connection getConnection()
    {
        try
        {
            return DriverManager.getConnection(databaseUrl, user, password)
        }
        catch (Exception e)
        {
            throw new IllegalStateException('Could not crete connection: ' + databaseUrl, e)
        }
    }

    public void releaseConnection(Connection c)
    {
        try
        {
            c.close()
        }
        catch (Exception ignore)
        { }
    }
}
