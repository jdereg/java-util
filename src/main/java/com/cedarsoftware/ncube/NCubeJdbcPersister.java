package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UniqueIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used to carry the NCube meta-information
 * to the client.
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
public class NCubeJdbcPersister
{
    private static final Log LOG = LogFactory.getLog(NCubeJdbcPersister.class);

    public void createCube(Connection c, ApplicationID appId, NCube cube, String username)
    {
        createCube(c, appId, cube, username, 0);
    }

    void createCube(Connection c, ApplicationID appId, NCube ncube, String username, long rev)
    {
        final String cubeName = ncube.getName();
        if (rev < 0)
        {
            throw new IllegalArgumentException("Cannot create cube: " + cubeName + " in app: " + appId + ", revision number cannot be negative.");
        }
        if (rev == 0 && doesCubeExist(c, appId, cubeName))
        {
            throw new IllegalStateException("Cannot create cube: " + cubeName + ".  It already exists in app: " + appId);
        }

        try
        {
            try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, revision_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"))
            {
                insert.setLong(1, UniqueIdGenerator.getUniqueId());
                insert.setString(2, appId.getApp());
                insert.setString(3, ncube.getName());
                insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                insert.setString(5, appId.getVersion());
                java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                insert.setDate(6, now);
                insert.setString(7, username);
                insert.setString(8, appId.getTenant());
                insert.setLong(9, rev);

                int rowCount = insert.executeUpdate();
                if (rowCount != 1)
                {
                    throw new IllegalStateException("error inserting new n-cube: " + ncube.getName() + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                }
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to insert cube: " + ncube.getName() + ", app: " + appId + " into database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    public void updateCube(Connection connection, ApplicationID appId, NCube cube, String username)
    {
        Long maxRev = getMaxRevision(connection, appId, cube.getName());
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update non-existing cube.");
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update deleted cube.  Restore it first.");
        }
        createCube(connection, appId, cube, username, maxRev + 1);
    }

    Long getMaxRevision(Connection c, ApplicationID appId, String name)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT revision_number FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());

            try (ResultSet rs = stmt.executeQuery())
            {
                Long maxRev = null;
                if (rs.next())
                {
                    maxRev = rs.getLong(1);
                }
                return maxRev;
            }
        }
        catch (Exception e)
        {
            String s = "Unable to get maximum revision number for cube: " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getCubeRecords(Connection c, ApplicationID appId, String sqlLike)
    {
        if (StringUtilities.isEmpty(sqlLike))
        {
            sqlLike = "%";
        }

        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n.n_cube_nm, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, n.revision_number FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm like ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                "  GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.revision_number >= 0 AND " +
                "n.n_cube_nm like ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ')"))
        {
            stmt.setString(1, sqlLike);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, sqlLike);
            stmt.setString(7, appId.getApp());
            stmt.setString(8, appId.getVersion());
            stmt.setString(9, appId.getStatus());
            stmt.setString(10, appId.getTenant());
            return getCubeInfoRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch cubes matching '" + sqlLike + "' from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getDeletedCubeRecords(Connection c, ApplicationID appId, String sqlLike)
    {
        if (StringUtilities.isEmpty(sqlLike))
        {
            sqlLike = "%";
        }

        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n.n_cube_nm, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, n.revision_number FROM n_cube n, " +
                        "( " +
                        "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                        "  FROM n_cube " +
                        "  WHERE n_cube_nm like ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                        "  GROUP BY n_cube_nm " +
                        ") m " +
                        "WHERE n.revision_number < 0 AND n.n_cube_nm like ? AND m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND " +
                        "n.app_cd = ? AND n.version_no_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ')"))
        {
            stmt.setString(1, sqlLike);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, sqlLike);
            stmt.setString(6, appId.getApp());
            stmt.setString(7, appId.getVersion());
            stmt.setString(8, appId.getTenant());
            return getCubeInfoRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch deleted cubes from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getRevisions(Connection c, ApplicationID appId, String cubeName)
    {
        if (!doesCubeExist(c, appId, cubeName))
        {
            throw new IllegalArgumentException("Cannot fetch revision history for cube: " + cubeName + " as it does not exist in app: " + appId);
        }
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, create_hid, revision_number " +
                "FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            return getCubeInfoRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to get revision history for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private Object[] getCubeInfoRecords(ApplicationID appId, PreparedStatement stmt) throws Exception
    {
        List<NCubeInfoDto> records = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.name = rs.getString("n_cube_nm");
                dto.tenant = appId.getTenant();
                dto.id = Long.toString(rs.getLong("n_cube_id"));
                byte[] notes = rs.getBytes("notes_bin");
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = appId.getVersion();
                dto.status = rs.getString("status_cd");
                dto.app = appId.getApp();
                dto.createDate = rs.getDate("create_dt");
                dto.createHid = rs.getString("create_hid");
                dto.revision = Long.toString(rs.getLong("revision_number"));
                records.add(dto);
            }
        }
        return records.toArray();
    }

    public NCube loadCube(Connection c, NCubeInfoDto cubeInfo)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT cube_value_bin FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND revision_number = ? AND tenant_cd = RPAD(?, 10, ' ')"))
        {
            stmt.setString(1, cubeInfo.name);
            stmt.setString(2, cubeInfo.app);
            stmt.setString(3, cubeInfo.version);
            stmt.setString(4, cubeInfo.status);
            stmt.setString(5, cubeInfo.revision);
            stmt.setString(6, cubeInfo.tenant);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube ncube = NCubeManager.ncubeFromJson(json);
                    ncube.name = cubeInfo.name;
                    ncube.setApplicationID(cubeInfo.getApplicationID());
                    return ncube;
                }
            }
            throw new IllegalArgumentException("Unable to load cube: " + cubeInfo.name + ", app: " + cubeInfo + " from database");
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to load cube: " + cubeInfo.name + ", app: " + cubeInfo + " from database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
     }

    public void restoreCube(Connection c, ApplicationID appId, String cubeName, String username)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot restore cube: " + cubeName + " as it does not exist in app: " + appId);
        }
        if (maxRev >= 0)
        {
            throw new IllegalArgumentException("Cube: " + cubeName + " is already restored in app: " + appId);
        }

       try
        {
            NCubeInfoDto cubeInfo = new NCubeInfoDto();
            cubeInfo.tenant = appId.getTenant();
            cubeInfo.name = cubeName;
            cubeInfo.app = appId.getApp();
            cubeInfo.version = appId.getVersion();
            cubeInfo.status = appId.getStatus();
            cubeInfo.revision = String.valueOf(maxRev);

            NCube ncube = loadCube(c, cubeInfo);
            String testData = getTestData(c, appId, cubeName);

            String insertSql =
                    "INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, revision_number, notes_bin, test_data_bin) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insert = c.prepareStatement(insertSql))
            {
                insert.setLong(1, UniqueIdGenerator.getUniqueId());
                insert.setString(2, appId.getApp());
                insert.setString(3, cubeName);
                insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                insert.setString(5, appId.getVersion());
                java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                insert.setDate(6, now);
                insert.setString(7, username);
                insert.setString(8, appId.getTenant());
                insert.setLong(9, Math.abs(maxRev) + 1);
                String note = "restored on " + now + " by " + username;
                insert.setBytes(10, note.getBytes("UTF-8"));
                insert.setBytes(11, testData == null ? null : testData.getBytes("UTF-8"));

                int rowCount = insert.executeUpdate();
                if (rowCount != 1)
                {
                    throw new IllegalStateException("Cannot restore n-cube: " + cubeName + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                }
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean deleteCube(Connection c, ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalStateException("Cannot delete cube: " + cubeName + " as cube does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalStateException("Cannot delete cube: " + cubeName + " as it is already deleted in app: " + appId);
        }

        if (allowDelete)
        {
            String sql = "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ')";

            try (PreparedStatement ps = c.prepareStatement(sql))
            {
                ps.setString(1, appId.getApp());
                ps.setString(2, cubeName);
                ps.setString(3, appId.getVersion());
                ps.setString(4, appId.getTenant());
                return ps.executeUpdate() > 0;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
        else
        {
            Object[] cubeInfo = getCubeRecords(c, appId, cubeName);
            if (ArrayUtilities.isEmpty(cubeInfo))
            {
                throw new IllegalArgumentException("Cannot delete cube: " + cubeName + ", unable to find it in app: " + appId);
            }
            NCube ncube = loadCube(c, (NCubeInfoDto) cubeInfo[0]);
            String testData = getTestData(c, appId, cubeName);

            try
            {
                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, revision_number, notes_bin, test_data_bin) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                {
                    insert.setLong(1, UniqueIdGenerator.getUniqueId());
                    insert.setString(2, appId.getApp());
                    insert.setString(3, cubeName);
                    insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                    insert.setString(5, appId.getVersion());
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    insert.setDate(6, now);
                    insert.setString(7, username);
                    insert.setString(8, appId.getTenant());
                    insert.setLong(9, -(maxRev + 1));
                    String note = "deleted on " + now + " by " + username;
                    insert.setBytes(10, note.getBytes("UTF-8"));
                    insert.setBytes(11, testData == null ? null : testData.getBytes("UTF-8"));

                    int rowCount = insert.executeUpdate();
                    if (rowCount != 1)
                    {
                        throw new IllegalStateException("Cannot delete n-cube: " + cubeName + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                    }
                    return true;
                }
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId;
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    public boolean updateNotes(Connection c, ApplicationID appId, String cubeName, String notes)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " is deleted in app: " + appId);
        }

        String statement = "UPDATE n_cube SET notes_bin = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, appId.getTenant());
            stmt.setLong(6, maxRev);
            int count = stmt.executeUpdate();
            if (count > 1)
            {
                throw new IllegalStateException("Cannot update notes, only one (1) row's notes should be updated, count: " + count + ", cube: " + cubeName + ", app: " + appId);
            }
            if (count == 0)
                {
                throw new IllegalStateException("Cannot update notes, no cube matches app: " + appId + ", name: " + cubeName);
            }
                    return true;
                }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getNotes(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT notes_bin FROM n_cube " +
                "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] notes = rs.getBytes("notes_bin");
                    return new String(notes == null ? "".getBytes() : notes, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Could not fetch notes, no cube: " + cubeName + " in app: " + appId);
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int createSnapshotVersion(Connection c, ApplicationID appId, String newVersion)
    {
        try
        {
            //  status does not affect uniqueness
            ApplicationID newId = appId.createNewSnapshotId(newVersion);
            if (doCubesExist(c, newId))
            {
                throw new IllegalStateException("Snapshot version ot created, as new SNAPSHOT version specified (" + newVersion + ") matches an existing version.  Specify new SNAPSHOT version that does not exist, app: " + appId);
            }

            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT n_cube_id, n.n_cube_nm, cube_value_bin, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, test_data_bin, n.revision_number FROM n_cube n, " +
                            "( " +
                            "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                            "  FROM n_cube " +
                            "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                            "  GROUP BY n_cube_nm " +
                            ") m " +
                            "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.revision_number >= 0 AND " +
                            "n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ')"))
            {
                stmt.setString(1, appId.getApp());
                stmt.setString(2, appId.getVersion());
                stmt.setString(3, ReleaseStatus.RELEASE.name());
                stmt.setString(4, appId.getTenant());
                stmt.setString(5, appId.getApp());
                stmt.setString(6, appId.getVersion());
                stmt.setString(7, ReleaseStatus.RELEASE.name());
                stmt.setString(8, appId.getTenant());
                try (ResultSet rs = stmt.executeQuery())
                {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, create_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd, revision_number)\n" +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    ))
                    {
                        int count = 0;

                        while (rs.next())
                        {
                            count++;
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));
                            insert.setBytes(3, rs.getBytes("cube_value_bin"));
                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(5, rs.getString("create_hid"));
                            insert.setString(6, newVersion);
                            insert.setString(7, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(8, appId.getApp());
                            insert.setBytes(9, rs.getBytes("test_data_bin"));
                            insert.setBytes(10, rs.getBytes("notes_bin"));
                            insert.setString(11, appId.getTenant());
                            insert.setLong(12, 0);      // New SNAPSHOT revision numbers start at 0
                            insert.executeUpdate();
                        }
                        return count;
                    }
                }
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create SNAPSHOT cubes for app: " + appId + ", new version: " + newVersion + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int releaseCubes(Connection c, ApplicationID appId)
    {
        if (doReleaseCubesExist(c, appId))
        {
            throw new IllegalStateException("A RELEASE version " + appId.getVersion() + " already exists. Have system admin renumber your SNAPSHOT version, app: " + appId);
        }

        try
        {
            try (PreparedStatement statement = c.prepareStatement(
                    "UPDATE n_cube SET create_dt = ?, status_cd = ? " +
                    "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ')"))
            {
                statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                statement.setString(2, ReleaseStatus.RELEASE.name());
                statement.setString(3, appId.getApp());
                statement.setString(4, appId.getVersion());
                statement.setString(5, ReleaseStatus.SNAPSHOT.name());
                statement.setString(6, appId.getTenant());
                return statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to release cubes for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int changeVersionValue(Connection c, ApplicationID appId, String newVersion)
    {
        if (doCubesExist(c, appId.createNewSnapshotId(newVersion)))
        {
            throw new IllegalStateException("Cannot change version value to " + newVersion + " because this version already exists.  Choose a different version number, app: " + appId);
        }

        try
        {
            try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ')"))
            {
                ps.setString(1, newVersion);
                ps.setString(2, appId.getApp());
                ps.setString(3, appId.getVersion());
                ps.setString(4, ReleaseStatus.SNAPSHOT.name());
                ps.setString(5, appId.getTenant());

                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + appId.getVersion() + ", therefore no versions updated, app: " + appId);
                }
                return count;
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to change SNAPSHOT version from " + appId.getVersion() + " to " + newVersion + " for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getTestData(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT test_data_bin FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') " +
                "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] testData = rs.getBytes("test_data_bin");
                    return testData == null ? "" : new String(testData, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Unable to fetch test data, cube: " + cubeName + ", app: " + appId + " does not exist.");
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch test data for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean updateTestData(Connection c, ApplicationID appId, String cubeName, String testData)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " is deleted in app: " + appId);
        }

        try (PreparedStatement stmt = c.prepareStatement(
                "UPDATE n_cube SET test_data_bin=? " +
                "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND revision_number = ?"))
        {
            stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, ReleaseStatus.SNAPSHOT.name());
            stmt.setString(6, appId.getTenant());
            stmt.setLong(7, maxRev);
            int count = stmt.executeUpdate();
            if (count > 1)
            {
                throw new IllegalStateException("Cannot update test data, only one (1) row's test data should be updated, cube: " + cubeName + ", app: " + appId + ", " + count + " rows updated.");
            }
            if (count == 0)
            {
                throw new IllegalStateException("Cannot updating test data, no cube matching app: " + appId + ", name: " + cubeName);
            }
            return true;
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update test data for NCube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean renameCube(Connection c, ApplicationID appId, NCube cube, String newName)
    {
        String cubeName = cube.getName();

        //  Save in case exception happens and we have to reset proper name on the cube.
        String oldName = cube.getName();

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE n_cube SET n_cube_nm = ?, cube_value_bin = ? " +
                "WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT.name() + "' AND tenant_cd = RPAD(?, 10, ' ')"))
        {
            //  We have to set the new  name on the cube toFormatJson with the proper name on it.
            cube.name = newName;

            ps.setString(1, newName);
            ps.setBytes(2, cube.toFormattedJson().getBytes("UTF-8"));   // Need to update cube name in JSON in bin column
            ps.setString(3, appId.getApp());
            ps.setString(4, appId.getVersion());
            ps.setString(5, oldName);
            ps.setString(6, appId.getTenant());

            int count = ps.executeUpdate();
            if (count < 1)
            {
                throw new IllegalArgumentException("Could not rename cube, no cube found to rename, for app:" + appId + ", original name: " + oldName + ", new name: " + newName);
            }

            return true;
        }
        catch (IllegalArgumentException e)
        {
            cube.name = oldName;
            throw e;
        }
        catch (Exception e)
        {
            cube.name = oldName;
            String s = "Unable to rename cube: " + oldName + ", app: " + appId + ", new name: " + newName + " due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ')";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for existing cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doReleaseCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ')";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, ReleaseStatus.RELEASE.name());
            ps.setString(4, appId.getTenant());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for release cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doesCubeExist(Connection c, ApplicationID appId, String name)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ')";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for cube:  " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public Object[] getAppNames(Connection connection, String tenant)
    {
        String sql = "SELECT DISTINCT app_cd FROM n_cube WHERE tenant_cd = RPAD(?, 10, ' ')";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            List<String> records = new ArrayList<>();

            stmt.setString(1, tenant);
            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }
            Collections.sort(records);
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all app names from database for tenant: " + tenant;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getAppVersions(Connection connection, ApplicationID appId)
    {
        final String sql = "SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ')";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, appId.getStatus());
            stmt.setString(3, appId.getTenant());

            List<String> records = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }

            Collections.sort(records);  // May need to enhance to ensure 2.19.1 comes after 2.2.1
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all versions for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }
}
