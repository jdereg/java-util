package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.util.List;

/**
 * This adapter could be replaced by an adapting proxy.  Then you could
 * implement the interface and the class and not need this class.
 *
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
public class NCubeJdbcPersisterAdapter implements NCubePersister
{
    private NCubeJdbcPersister persister = new NCubeJdbcPersister();
    private JdbcConnectionProvider connectionProvider;

    public NCubeJdbcPersisterAdapter(JdbcConnectionProvider provider)
    {
        connectionProvider = provider;
    }

    public void createCube(ApplicationID appId, NCube cube, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.createCube(c, appId, cube, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public void updateCube(ApplicationID appId, NCube cube, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.updateCube(c, appId, cube, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCube> loadCubes(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubes(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public Object[] getNCubes(ApplicationID appId, String sqlLike)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getNCubes(c, appId, sqlLike);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean deleteCube(ApplicationID appId, String name, boolean allowDelete, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.deleteCube(c, appId, name, allowDelete, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean doesCubeExist(ApplicationID appId, String name)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.doesCubeExist(c, appId, name);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public Object[] getAppNames(String account)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppNames(c, account);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public Object[] getAppVersions(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppVersions(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean updateNotes(ApplicationID appId, String cubeName, String notes)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateNotes(c, appId, cubeName, notes);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public String getNotes(ApplicationID appId, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getNotes(c, appId, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int createSnapshotVersion(ApplicationID appId, String newVersion)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.createSnapshotVersion(c, appId, newVersion);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int changeVersionValue(ApplicationID appId, String newVersion)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.changeVersionValue(c, appId, newVersion);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int releaseCubes(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.releaseCubes(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean renameCube(ApplicationID appId, NCube oldCube, String newName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.renameCube(c, appId, oldCube, newName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean updateTestData(ApplicationID appId, String cubeName, String testData)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateTestData(c, appId, cubeName, testData);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public String getTestData(ApplicationID appId, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getTestData(c, appId, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }
}
