package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This adapter could be replaced by an adapting proxy.  Then you could
 * implement the interface and the class and not need this class.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class NCubeJdbcPersisterAdapter implements NCubePersister
{
    private final NCubeJdbcPersister persister = new NCubeJdbcPersister();
    private final JdbcConnectionProvider connectionProvider;

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

    public NCube loadCube(NCubeInfoDto dto)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCube(c, dto);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCube(ApplicationID appId, String name)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCube(c, appId, name);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCubeBySha1(ApplicationID appId, String name, String sha1)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubeBySha1(c, appId, name, sha1);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCubeByRevision(ApplicationID appId, String name, long revision)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubeByRevision(c, appId, name, revision);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> getCubeRecords(ApplicationID appId, String pattern, boolean activeOnly)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getCubeRecords(c, appId, pattern, activeOnly);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> getChangedRecords(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getChangedRecords(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> getDeletedCubeRecords(ApplicationID appId, String pattern)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getDeletedCubeRecords(c, appId, pattern);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public void restoreCube(ApplicationID appId, String cubeName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.restoreCube(c, appId, cubeName, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> getRevisions(ApplicationID appId, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getRevisions(c, appId, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public Set<String> getBranches(String tenant)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getBranches(c, tenant);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean deleteBranch(ApplicationID branchId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.deleteBranch(c, branchId);
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

    public List<String> getAppNames(String tenant, String status, String branch)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppNames(c, tenant, status, branch);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<String> getAppVersions(String tenant, String app, String status, String branch)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppVersions(c, tenant, app, status, branch);
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

    public int releaseCubes(ApplicationID appId, String newSnapVer)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.releaseCubes(c, appId, newSnapVer);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int createBranch(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.createBranch(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean renameCube(ApplicationID appId, String oldName, String newName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.renameCube(c, appId, oldName, newName, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean mergeOverwriteHeadCube(ApplicationID appId, String cubeName, String headSha1, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.mergeOverwriteHeadCube(c, appId, cubeName, headSha1, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean mergeOverwriteBranchCube(ApplicationID appId, String cubeName, String branchSha1, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.mergeOverwriteBranchCube(c, appId, cubeName, branchSha1, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean duplicateCube(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.duplicateCube(c, oldAppId, newAppId, oldName, newName, username);
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

    public List<NCubeInfoDto> commitBranch(ApplicationID appId, Collection<NCubeInfoDto> dtos, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitBranch(c, appId, dtos, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto commitMergedCubeToHead(ApplicationID appId, NCube cube, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitMergedCubeToHead(c, appId, cube, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto commitMergedCubeToBranch(ApplicationID appId, NCube cube, String headSha1, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitMergedCubeToBranch(c, appId, cube, headSha1, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int rollbackBranch(ApplicationID appId, Object[] infoDtos)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.rollbackBranch(c, appId, infoDtos);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> updateBranch(ApplicationID appId, Collection<NCubeInfoDto> updates, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateBranch(c, appId, updates, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> search(ApplicationID appId, String cubeNamePattern, String searchValue)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.search(c, appId, cubeNamePattern, searchValue, null);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }
}
