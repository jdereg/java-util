package com.cedarsoftware.ncube;

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

//TODO: Need to add username to all mutable queries (ones that do UPDATEs or INSERTs)
public class NCubeJdbcPersister
{
    private static final Log LOG = LogFactory.getLog(NCubeJdbcPersister.class);

    public void createCube(Connection c, ApplicationID appId, NCube ncube)
    {
        if (doesCubeExist(c, appId, ncube.getName()))
        {
            throw new IllegalStateException("Cube: " + ncube.getName() + " already exists in app: " + appId);
        }

        try
        {
            try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, tenant_cd) VALUES (?, ?, ?, ?, ?, ?, ?)"))
            {
                insert.setLong(1, UniqueIdGenerator.getUniqueId());
                insert.setString(2, appId.getApp());
                insert.setString(3, ncube.getName());
                insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                insert.setString(5, appId.getVersion());
                java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                insert.setDate(6, now);
                insert.setString(7, appId.getAccount());
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
            throw new RuntimeException(s, e);
        }
    }

    public void updateCube(Connection connection, ApplicationID appId, NCube cube)
    {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE n_cube SET cube_value_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
        {
            stmt.setBytes(1, cube.toFormattedJson().getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, appId.getApp());
            stmt.setString(4, cube.getName());
            stmt.setString(5, appId.getVersion());
            stmt.setString(6, ReleaseStatus.SNAPSHOT.name());
            stmt.setString(7, appId.getAccount());

            int count = stmt.executeUpdate();
            if (count != 1)
            {
                throw new IllegalStateException("error updating cube: " + cube.getName() + "', app: " + appId + " (" + count + " rows updated, should be 1)");
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update cube: " + cube.getName() + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public NCube findCube(Connection c, ApplicationID appId, String cubeName)
    {
        String query = "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?";

        try (PreparedStatement stmt = c.prepareStatement(query))
        {
            NCube ncube = null;
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getAccount());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    ncube = NCubeManager.ncubeFromJson(json);

                    if (rs.next())
                    {
                        throw new IllegalStateException("More than one cube matching name: " + ncube.getName() + ", appId:  " + appId);
                    }
                }

                return ncube;
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to load cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getNCubes(Connection c, ApplicationID appId, String sqlLike)
    {
        if (sqlLike == null)
        {
            sqlLike = "%";
        }

        try (PreparedStatement stmt = c.prepareStatement("SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, update_dt, " +
                "create_hid, update_hid FROM n_cube WHERE n_cube_nm LIKE ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
        {
            stmt.setString(1, sqlLike);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getAccount());

            ResultSet rs = stmt.executeQuery();
            List<NCubeInfoDto> records = new ArrayList<>();

            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.id = Long.toString(rs.getLong("n_cube_id"));
                dto.name = rs.getString("n_cube_nm");
                byte[] notes = rs.getBytes("notes_bin");
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = rs.getString("version_no_cd");
                dto.status = rs.getString("status_cd");
                dto.app = rs.getString("app_cd");
                dto.createDate = rs.getDate("create_dt");
                dto.updateDate = rs.getDate("update_dt");
                dto.createHid = rs.getString("create_hid");
                dto.updateHid = rs.getString("update_hid");
                records.add(dto);
            }
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch cubes matching '" + sqlLike + "' from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public List<NCube> loadCubes(Connection c, ApplicationID appId)
    {
        try (PreparedStatement stmt = c.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
        {
            List<NCube> ncubes = new ArrayList<>();

            stmt.setString(1, appId.getApp());
            stmt.setString(2, appId.getVersion());
            stmt.setString(3, appId.getStatus());
            stmt.setString(4, appId.getAccount());
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
            {
                byte[] jsonBytes = rs.getBytes("cube_value_bin");
                String json = new String(jsonBytes, "UTF-8");
                try
                {
                    NCube ncube = NCubeManager.ncubeFromJson(json);
                    ncube.setApplicationID(appId);
                    ncubes.add(ncube);
                }
                catch (Exception e)
                {
                    int len = 60;
                    if (json.length() <= len)
                    {
                        len = json.length() - 1;
                    }
                    LOG.warn("app: " + appId + ", failed to load cube: " + json.substring(0, len));
                }
            }

            return ncubes;
        }
        catch (Exception e)
        {
            String s = "Unable to load cubes, app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
     }

    public boolean deleteCube(Connection c, ApplicationID appId, String cubeName, boolean allowDelete)
    {
        String statement = allowDelete ?
                "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = ?" :
                "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = ? AND status_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, cubeName);
            ps.setString(3, appId.getVersion());
            ps.setString(4, appId.getAccount());

            if (!allowDelete)
            {
                ps.setString(5, ReleaseStatus.SNAPSHOT.name());
            }

            return ps.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to delete cube: " + cubeName + ", app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean updateNotes(Connection c, ApplicationID appId, String cubeName, String notes)
    {
        String statement = "UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, appId.getApp());
            stmt.setString(4, cubeName);
            stmt.setString(5, appId.getVersion());
            stmt.setString(6, appId.getAccount());
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
        try (PreparedStatement stmt = c.prepareStatement("SELECT notes_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = ?"))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getAccount());
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
                throw new IllegalStateException("New SNAPSHOT Version specified (" + newVersion + ") matches an existing version.  Specify new SNAPSHOT version that does not exist, app: " + appId);
            }

            try (PreparedStatement select = c.prepareStatement(
                    "SELECT n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd\n" +
                            "FROM n_cube\n" +
                            "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"
            ))
            {

                select.setString(1, appId.getApp());
                select.setString(2, appId.getVersion());
                select.setString(3, ReleaseStatus.RELEASE.name());
                select.setString(4, appId.getAccount());

                try (ResultSet rs = select.executeQuery())
                {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd)\n" +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
                            insert.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(6, rs.getString("create_hid"));
                            insert.setString(7, rs.getString("update_hid"));
                            insert.setString(8, newVersion);
                            insert.setString(9, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(10, rs.getString("app_cd"));
                            insert.setBytes(11, rs.getBytes("test_data_bin"));
                            insert.setBytes(12, rs.getBytes("notes_bin"));
                            insert.setString(13, rs.getString("tenant_cd"));
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
            try (PreparedStatement statement = c.prepareStatement("UPDATE n_cube SET update_dt = ?, status_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
            {
                statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                statement.setString(2, ReleaseStatus.RELEASE.name());
                statement.setString(3, appId.getApp());
                statement.setString(4, appId.getVersion());
                statement.setString(5, ReleaseStatus.SNAPSHOT.name());
                statement.setString(6, appId.getAccount());
                return statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to release cubes for app: " + appId.getApp() + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int changeVersionValue(Connection c, ApplicationID appId, String newVersion)
    {
        if (doCubesExist(c, appId.createNewSnapshotId(newVersion)))
        {
            throw new IllegalStateException("RELEASE n-cubes found with version " + newVersion + ".  Choose a different SNAPSHOT version, app: " + appId);
        }

        try
        {
            try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET update_dt = ?, version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
            {
                ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                ps.setString(2, newVersion);
                ps.setString(3, appId.getApp());
                ps.setString(4, appId.getVersion());
                ps.setString(5, ReleaseStatus.SNAPSHOT.name());
                ps.setString(6, appId.getAccount());

                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + appId.getVersion() + ", therefore nothing changed, app: " + appId);
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
        try (PreparedStatement stmt = c.prepareStatement("SELECT test_data_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = ?"))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getAccount());
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                byte[] testData = rs.getBytes("test_data_bin");
                return testData == null ? new String() : new String(testData, "UTF-8");
            }
            throw new IllegalArgumentException("No cube: " + cubeName + ", app: " + appId + " matches passed in parameters.");
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
        try (PreparedStatement stmt = c.prepareStatement("UPDATE n_cube SET test_data_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?"))
        {
            stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, appId.getApp());
            stmt.setString(4, cubeName);
            stmt.setString(5, appId.getVersion());
            stmt.setString(6, ReleaseStatus.SNAPSHOT.name());
            stmt.setString(7, appId.getAccount());
            int count = stmt.executeUpdate();
            if (count > 1)
            {
                throw new IllegalStateException("Error updating test data, only one (1) row's test data should be updated, cube: " + cubeName + ", app: " + appId);
            }
            if (count == 0)
            {
                throw new IllegalStateException("Error updating test data, no cube matching app: " + appId + ", name: " + cubeName);
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
        //  Save in case exception happens and we have to reset proper name on the cube.
        String oldName = cube.getName();

        try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET n_cube_nm = ?, cube_value_bin = ? WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT.name() + "' AND tenant_cd = ?"))
        {
            //  We have to set the new  name on the cube toFormatJson with the proper name on it.
            cube.name = newName;

            ps.setString(1, newName);
            //John, is there any way to keep from having to reformat the whole cube when its name changes
            //Is this just because of loading a cube from disk?
            ps.setBytes(2, cube.toFormattedJson().getBytes("UTF-8"));
            ps.setString(3, appId.getApp());
            ps.setString(4, appId.getVersion());
            ps.setString(5, oldName);
            ps.setString(6, appId.getAccount());
            int count = ps.executeUpdate();
            if (count < 1)
            {
                throw new IllegalArgumentException("Rename cube failed, no cube found to rename, for app:" + appId + ", original name: " + oldName + ", new name: " + newName);
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
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getAccount());

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
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, ReleaseStatus.RELEASE.name());
            ps.setString(4, appId.getAccount());

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

    /**
     * This code does not take the typical application id because it allows for varying levels of
     * scope.  If status is passed in it is used and if name is passed in it is used, but
     * those can be left off to look for any cubes for an account, app, and version.
     *
     * Because we allow null on those values the ApplicationId could not be used.  We may be able
     * to hide this behind the persister proxy so the outside doesn't know about it later on.
     */
    public boolean doesCubeExist(Connection c, ApplicationID appId, String name)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND n_cube_nm = ? AND tenant_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getStatus());
            ps.setString(4, name);
            ps.setString(5, appId.getAccount());

            try (ResultSet rs = ps.executeQuery())
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

    //------------------------- private methods ---------------------------------------

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public Object[] getAppNames(Connection connection, String account)
    {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT app_cd FROM n_cube WHERE tenant_cd = ?"))
        {
            stmt.setString(1, account);
            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            Collections.sort(records);
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all app names from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getAppVersions(Connection connection, ApplicationID appId)
    {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? AND status_cd = ? AND tenant_cd = ?"))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, appId.getStatus());
            stmt.setString(3, appId.getAccount());

            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<>();

            while (rs.next())
            {
                records.add(rs.getString(1));
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
