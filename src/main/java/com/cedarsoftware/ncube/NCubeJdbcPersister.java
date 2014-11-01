package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.util.io.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NCubeJdbcPersister
{
    private static final Log LOG = LogFactory.getLog(NCubeJdbcPersister.class);
    
    public void createCube(Connection c, ApplicationID id, NCube ncube)
    {
        if (doesCubeExist(c, id, ncube.getName())) {
            throw new IllegalStateException("Cube already exists:  " + ncube.getName() + " " + id.toString());
        }

        try
        {

            try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)"))
            {
                // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
                // TODO: see if the column exists, store the result for the entire app life cycle.
                // TODO: If account column does not exist, then account is null.
                insert.setLong(1, UniqueIdGenerator.getUniqueId());
                insert.setString(2, id.getApp());
                insert.setString(3, ncube.getName());
                insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                insert.setString(5, id.getVersion());
                java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                insert.setDate(6, now);
                insert.setDate(7, now);
                int rowCount = insert.executeUpdate();
                if (rowCount != 1)
                {
                    throw new IllegalStateException("error inserting new NCube: " + ncube.getName() + "', app: " + id.getApp() + ", version: " + id.getVersion() + " (" + rowCount + " rows inserted, should be 1)");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create NCube: " + ncube.getName() + ", app: " + id.getApp() + ", version: " + id.getVersion() + " to database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }



    public void updateCube(Connection connection, ApplicationID id, NCube cube)
    {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE n_cube SET cube_value_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setBytes(1, cube.toFormattedJson().getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, id.getApp());
            stmt.setString(4, cube.getName());
            stmt.setString(5, id.getVersion());
            stmt.setString(6, ReleaseStatus.SNAPSHOT.name());

            int count = stmt.executeUpdate();
            if (count != 1)
            {
                throw new IllegalStateException("Only one (1) row should be updated.");
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update NCube: " + cube.getName() + ", app: " + id.getApp() + ", version: " + id.getVersion();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }


    public NCube findCube(Connection c, ApplicationID appId, String cubeName)
    {
        String query = "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ?";

        try (PreparedStatement stmt = c.prepareStatement(query))
        {
            NCube ncube = null;
            
            java.sql.Date systemDate = new java.sql.Date(new Date().getTime());
            
            //todo - remove sys effective date and expiration date
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    ncube = ncubeFromJson(json);

                    if (rs.next())
                    {
                        throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", appId:  " + appId.toString());
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
            String s = "Unable to load nNCube: " + cubeName + ", app: " + appId.toString();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getNCubes(Connection c, ApplicationID id, String sqlLike) {
        if (sqlLike == null)
        {
            sqlLike = "%";
        }

        try (PreparedStatement stmt = c.prepareStatement("SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, update_dt, " +
                "create_hid, update_hid, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt FROM n_cube WHERE n_cube_nm LIKE ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setString(1, sqlLike);
            stmt.setString(2, id.getApp());
            stmt.setString(3, id.getVersion());
            stmt.setString(4, id.getStatus());

            ResultSet rs = stmt.executeQuery();
            List<NCubeInfoDto> records = new ArrayList<NCubeInfoDto>();

            while (rs.next())
            {
                // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
                // TODO: see if the column exists, store the result for the entire app life cycle.
                // TODO: If account column does not exist, then account is null.
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
                dto.sysEffDate = rs.getDate("sys_effective_dt");
                dto.sysEndDate = rs.getDate("sys_expiration_dt");
                dto.bizEffDate = rs.getDate("business_effective_dt");
                dto.bizExpDate = rs.getDate("business_expiration_dt");
                records.add(dto);
            }
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch NCubes matching '" + sqlLike + "' from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public List<NCube> loadCubes(Connection c, ApplicationID appId)
    {        
        String app = appId.getApp();
        String version = appId.getVersion();
        String status = appId.getStatus();
        
        try (PreparedStatement stmt = c.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?"))
        {
            List<NCube> ncubes = new ArrayList<>();
            java.sql.Date systemDate = new java.sql.Date(new Date().getTime());

            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            
            //TODO: remove date params
            stmt.setString(1, app);
            stmt.setDate(2, systemDate);
            stmt.setDate(3, systemDate);
            stmt.setString(4, version);
            stmt.setString(5, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
            {
                byte[] jsonBytes = rs.getBytes("cube_value_bin");
                String json = new String(jsonBytes, "UTF-8");
                try
                {
                    NCube ncube = ncubeFromJson(json);
                    ncube.setApplicationID(appId);
                    ncubes.add(ncube);
                }
                catch (Exception e)
                {
                    LOG.warn("account: " + appId.getAccount() + ", app: " + appId.getApp() + ", version: " + appId.getVersion() + ", Failed to load n-cube: " + json.substring(0, 40));
                }
            }
            
            return ncubes;
        }
        catch (Exception e)
        {
            String s = "Unable to load n-cubes, app: " + app + ", version: " + version + ", status: " + status + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
     }

    public boolean deleteCube(Connection c, ApplicationID id, String cubeName, boolean allowDelete)
    {
        String statement = allowDelete ?
                "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?" :
                "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, id.getApp());
            ps.setString(2, cubeName);
            ps.setString(3, id.getVersion());

            if (!allowDelete)
            {
                ps.setString(4, ReleaseStatus.SNAPSHOT.name());
            }

            return ps.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to delete NCube: " + cubeName + ", app: " + id.getApp() + ", version: " + id.getVersion() + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean updateNotes(Connection c, ApplicationID id, String cubeName, String notes)
    {
        String statement = "UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, id.getApp());
            stmt.setString(4, cubeName);
            stmt.setString(5, id.getVersion());
            int count = stmt.executeUpdate();
            if (count > 1)
            {
                throw new IllegalStateException("Only one (1) row's notes should be updated.");
            }
            if (count == 0)
            {
                throw new IllegalStateException("No NCube matching app: " + id.getApp() + ", name: " + cubeName + ", version: " + id.getVersion());
            }
            return true;
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update notes for NCube: " + cubeName + ", app: " + id.getApp() + ", version: " + id.getVersion();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getNotes(Connection c, ApplicationID id, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement("SELECT notes_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setString(1, id.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, id.getVersion());
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] notes = rs.getBytes("notes_bin");
                    return new String(notes == null ? "".getBytes() : notes, "UTF-8");
                }
            }
            throw new IllegalArgumentException("No NCube matching passed in parameters.");
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch notes for NCube: " + cubeName + ", app: " + id.getApp() + ", version: " + id.getVersion();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int createSnapshotVersion(Connection c, ApplicationID id, String newVersion) {
        try
        {
            //  status does not affect uniqueness
            ApplicationID newId = id.createNewSnapshotId(newVersion);
            if (doCubesExist(c, newId))
            {
                throw new IllegalStateException("New SNAPSHOT Version specified (" + newVersion + ") matches an existing version.  Specify new SNAPSHOT version that does not exist.");
            }

            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            try (PreparedStatement select = c.prepareStatement(
                    "SELECT n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin\n" +
                            "FROM n_cube\n" +
                            "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?"
            ))
            {

                select.setString(1, id.getApp());
                select.setString(2, id.getVersion());
                select.setString(3, ReleaseStatus.RELEASE.name());


                try (ResultSet rs = select.executeQuery())
                {

                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin)\n" +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    ))
                    {
                        int count = 0;

                        while (rs.next())
                        {
                            count++;
                            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
                            // TODO: see if the column exists, store the result for the entire app life cycle.
                            // TODO: If account column does not exist, then account is null.

                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));
                            insert.setBytes(3, rs.getBytes("cube_value_bin"));
                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(6, rs.getString("create_hid"));
                            insert.setString(7, rs.getString("update_hid"));
                            insert.setString(8, newVersion);
                            insert.setString(9, ReleaseStatus.SNAPSHOT.name());
                            insert.setDate(10, rs.getDate("sys_effective_dt"));
                            insert.setDate(11, rs.getDate("sys_expiration_dt"));
                            insert.setDate(12, rs.getDate("business_effective_dt"));
                            insert.setDate(13, rs.getDate("business_expiration_dt"));
                            insert.setString(14, rs.getString("app_cd"));
                            insert.setBytes(15, rs.getBytes("test_data_bin"));
                            insert.setBytes(16, rs.getBytes("notes_bin"));
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
            String s = "Unable to create SNAPSHOT NCubes for app: " + id.getApp() + ", version: " + newVersion + ", due to an error: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }


    public int releaseCubes(Connection c, ApplicationID id) {
        if (doReleaseCubesExist(c, id))
        {
            throw new IllegalStateException("A RELEASE version " + id.getVersion() + " already exists. Have system admin renumber your SNAPSHOT version.");
        }

        try
        {

            try (PreparedStatement statement = c.prepareStatement("UPDATE n_cube SET update_dt = ?, status_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?"))
            {
                // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
                // TODO: see if the column exists, store the result for the entire app life cycle.
                // TODO: If account column does not exist, then account is null.
                statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                statement.setString(2, ReleaseStatus.RELEASE.name());
                statement.setString(3, id.getApp());
                statement.setString(4, id.getVersion());
                statement.setString(5, ReleaseStatus.SNAPSHOT.name());
                return statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to release NCubes for app: " + id.getApp() + ", version: " + id.getVersion() + ", due to an error: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int changeVersionValue(Connection c, ApplicationID id, String newVersion)
    {
        if (doCubesExist(c, id.createNewSnapshotId(newVersion)))
        {
            throw new IllegalStateException("RELEASE n-cubes found with version " + newVersion + ".  Choose a different SNAPSHOT version.");
        }

        try
        {

            try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET update_dt = ?, version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?"))
            {
                // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
                // TODO: see if the column exists, store the result for the entire app life cycle.
                // TODO: If account column does not exist, then account is null.
                ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                ps.setString(2, newVersion);
                ps.setString(3, id.getApp());
                ps.setString(4, id.getVersion());
                ps.setString(5, ReleaseStatus.SNAPSHOT.name());

                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + id.getVersion() + ", therefore nothing changed.");
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
            String s = "Unable to change SNAPSHOT version from " + id.getVersion() + " to " + newVersion + " for app: " + id.getApp() + ", due to an error: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getTestData(Connection c, ApplicationID id, String cubeName) {
        try (PreparedStatement stmt = c.prepareStatement("SELECT test_data_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setString(1, id.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, id.getVersion());
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                byte[] testData = rs.getBytes("test_data_bin");
                return testData == null ? new String() : new String(testData, "UTF-8");
            }
            throw new IllegalArgumentException("No NCube matching passed in parameters.");
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch test data for NCube: " + cubeName + ", app: " + id.getApp() + ", version: " + id.getVersion();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean updateTestData(Connection c, ApplicationID id, String cubeName, String testData)
    {
        try (PreparedStatement stmt = c.prepareStatement("UPDATE n_cube SET test_data_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setString(3, id.getApp());
            stmt.setString(4, cubeName);
            stmt.setString(5, id.getVersion());
            stmt.setString(6, ReleaseStatus.SNAPSHOT.name());
            int count = stmt.executeUpdate();
            if (count > 1)
            {
                throw new IllegalStateException("Only one (1) row's test data should be updated.");
            }
            if (count == 0)
            {
                throw new IllegalStateException("No NCube matching app: " + id.getApp() + ", name: " + cubeName + ", version: " + id.getVersion());
            }
            return true;
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to update test data for NCube: " + cubeName + ", app: " + id.getApp() + ", version: " + id.getVersion();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean renameCube(Connection c, ApplicationID id, NCube cube, String newName) {

        //  Save in case exception happens and we have to reset proper name on the cube.
        String oldName = cube.getName();

        try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET n_cube_nm = ?, cube_value_bin = ? WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT.name() + "'"))
        {
            //  We have to set the new  name on the cube toFormatJson with the proper name on it.
            cube.name = newName;

            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, newName);
            //John, is there any way to keep from having to reformat the whole cube when its name changes
            //Is this just because of loading a cube from disk?
            ps.setBytes(2, cube.toFormattedJson().getBytes("UTF-8"));
            ps.setString(3, id.getApp());
            ps.setString(4, id.getVersion());
            ps.setString(5, oldName);
            int count = ps.executeUpdate();
            if (count < 1)
            {
                throw new IllegalArgumentException("No n-cube found to rename, for app:" + id.getApp() + ", version: " + id.getVersion() + ", original name: " + oldName);
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
            String s = "Unable to rename n-cube due to an error: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean doCubesExist(Connection c, ApplicationID id)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, id.getApp());
            ps.setString(2, id.getVersion());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cubes:  " + id.toString();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean doReleaseCubesExist(Connection c, ApplicationID id)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, id.getApp());
            ps.setString(2, id.getVersion());
            ps.setString(3, ReleaseStatus.RELEASE.name());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cubes:  " + id.toString();
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
     * @param c
     * @param id
     * @param name
     * @return
     */
    public boolean doesCubeExist(Connection c, ApplicationID id, String name)
    {
        if (name == null)
        {
            throw new NullPointerException("n-cube name cannot be null to check for existence");
        }

        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? and n_cube_nm = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, id.getApp());
            ps.setString(2, id.getVersion());
            ps.setString(3, id.getStatus());
            ps.setString(4, name);

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cube:  " + name + " : " + id.toString();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    //------------------------- private methods ---------------------------------------

    private NCube ncubeFromJson(String json) throws IOException
    {
        try
        {
            return NCube.fromSimpleJson(json);
        }
        catch (Exception e)
        {
            try
            {   // 2nd attempt in old format - when n-cubes where written by json-io (not the custom writer).
                NCube ncube = (NCube) JsonReader.jsonToJava(json);
                List<Axis> axes = ncube.getAxes();
                for (Axis axis : axes)
                {
                    axis.buildScaffolding();
                }
                ncube.setMetaProperty("sha1", ncube.sha1());
                return ncube;
            }
            catch (Exception e1)
            {
                throw e;
            }
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    // TODO: Mark API as @Deprecated when this API is available with ApplicationID as a parameter
    //todo replace with new api
    public String[] getAppNames(Connection connection)
    {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT app_cd FROM n_cube"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<String>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            Collections.sort(records);
            return records.toArray(new String[0]);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all ncube app names from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String[] getAppVersions(Connection connection, ApplicationID id) {

        try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? and status_cd = ?"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            stmt.setString(1, id.getApp());
            stmt.setString(2, id.getStatus());

            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<String>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            Collections.sort(records);  // May need to enhance to ensure 2.19.1 comes after 2.2.1
            return records.toArray(new String[0]);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all ncube app versions from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

}
