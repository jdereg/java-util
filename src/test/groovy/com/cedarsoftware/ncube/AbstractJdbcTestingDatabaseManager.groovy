package com.cedarsoftware.ncube

import com.cedarsoftware.util.StringUtilities

import java.sql.Connection

/**
 * @author Ken Partlow (kpartlow@gmail.com)
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
abstract class AbstractJdbcTestingDatabaseManager implements TestingDatabaseManager
{
    JdbcConnectionProvider provider;
    NCubeJdbcPersister persister = new NCubeJdbcPersister();

    AbstractJdbcTestingDatabaseManager(JdbcConnectionProvider p) {
        provider = p;
    }

    void insertCubeWithNoSha1(ApplicationID appId, String username, NCube cube)
    {
        Connection c = provider.connection;
        try
        {
            byte[] cubeData = StringUtilities.getBytes(cube.toFormattedJson(), "UTF-8");
            persister.insertCube(c, appId, cube.name, 0L, cubeData, (byte[])null, "Inserted without sha1-1", (Boolean)false, (String)null, (String)null, System.currentTimeMillis(), username)
    }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void addCubes(ApplicationID appId, String username, NCube[] cubes)
    {
        Connection c = provider.connection;


        try
        {
            for (NCube ncube : cubes)
            {
                persister.updateCube(c, appId, ncube, username);
            }
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void removeBranches(ApplicationID[] appIds)
    {
        Connection c = provider.connection;
        try
        {
            for (ApplicationID appId : appIds)
            {
                persister.deleteBranch(c, appId)
            }
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void updateCube(ApplicationID appId, String username, NCube ncube)
    {
        Connection c = provider.connection;
        try
        {
            persister.updateCube(c, appId, ncube, username);
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }
}
