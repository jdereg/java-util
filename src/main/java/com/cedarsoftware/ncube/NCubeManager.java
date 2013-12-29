package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages a list of NCubes.  This class is referenced
 * by NCube in one place - when it joins to other cubes, it consults
 * the NCubeManager to find the joined NCube.
 *
 * This class takes care of creating, loading, updating, releasing,
 * and deleting NCubes.  It also allows you to get a list of NCubes
 * matching a wildcard (SQL Like) string.
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
public class NCubeManager
{
    private final Map<String, NCube> cubeList = new ConcurrentHashMap<String, NCube>();
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);

    public static NCubeManager getInstance()
    {
        return new NCubeManager();
    }

    /**
     * @param name String name of an NCube.
     * @return NCube instance with the given name.  Please note
     * that the cube must be loaded first before calling this.
     */
    public NCube getCube(String name)
    {
   		return cubeList.get(name);
    }

    /**
     * Add a cube to the internal map of available cubes.
     * @param ncube NCube to add to the list.
     */
    public void addCube(NCube ncube)
    {
        synchronized(cubeList)
        {
            cubeList.put(ncube.getName(), ncube);
            ncube.setManager(this);
        }
    }

    /**
     * @return Map<String, NCube> of all NCubes that are currently
     * loaded (cached) in memory.  A copy of the internal cache
     * is returned.
     */
    public Map<String, NCube> getCachedNCubes()
    {
        synchronized (cubeList)
        { return new TreeMap<String, NCube>(cubeList); }
    }

    /**
     * Used for testing.
     */
    void clearCubeList()
    {
        synchronized(cubeList)
        {
            cubeList.clear();
        }
    }

    private static void jdbcCleanup(PreparedStatement stmt)
    {
        if (stmt != null)
        {
            try
            {
                stmt.close();
            }
            catch (SQLException e) { LOG.error("Error closing JDBC Statement", e); }
        }
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     * @return NCube that matches, or null if not found.
     */
    public NCube loadCube(Connection connection, String app, String name, String version, String status, Date sysDate)
    {
        if (connection == null || app == null || name == null || version == null || status == null || sysDate == null)
        {
            throw new IllegalArgumentException("None of the arguments to loadCube() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
                stmt = connection.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?");

                stmt.setString(1, name);
                stmt.setString(2, app);
                stmt.setDate(3, systemDate);
                stmt.setDate(4, systemDate);
                stmt.setString(5, version);
                stmt.setString(6, status);
                ResultSet rs = stmt.executeQuery();

                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube<Object> ncube = (NCube) JsonReader.jsonToJava(json);
                    for (Axis axis : ncube.getAxes())
                    {
                        axis.buildScaffolding();
                    }

                    if (rs.next())
                    {
                        throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate);
                    }

                    cubeList.put(ncube.getName(), ncube);
                    Set<String> subCubeList = ncube.getReferencedCubeNames();

                    for (String cubeName : subCubeList)
                    {
                        if (!cubeList.containsKey(cubeName))
                        {
                            loadCube(connection, app, cubeName, version, status, sysDate);
                        }
                    }
                    ncube.setManager(this);
                    return ncube;
                }
                return null; // Indicates not found
            }
            catch (IllegalStateException e) { throw e; }
            catch (Exception e) { throw new RuntimeException("Unable to load nNCube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database", e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    public Object[] getNCubes(Connection connection, String app, String version, String status, String sqlLike)
    {
        if (connection == null || sqlLike == null)
        {
            throw new IllegalArgumentException("None of the arguments to getNCubes() can be null. Filter: " + sqlLike + ", connection: " + connection);
        }

        if (!"RELEASE".equals(status) && !"SNAPSHOT".equals(status))
        {
            throw new IllegalArgumentException("Status must be 'RELEASE' or 'SNAPSHOT'");
        }

        PreparedStatement stmt = null;
        try
        {
            stmt = connection.prepareStatement("SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, update_dt, " +
                    "create_hid, update_hid, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt FROM n_cube WHERE n_cube_nm LIKE ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ?");
            stmt.setString(1, sqlLike);
            stmt.setString(2, app);
            stmt.setString(3, version);
            stmt.setString(4, status);
            ResultSet rs = stmt.executeQuery();
            List<NCubeInfoDto> records = new ArrayList<NCubeInfoDto>();

            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.id = rs.getLong("n_cube_id");
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
        catch (Exception e) { throw new RuntimeException("Unable to fetch NCubes matching '" + sqlLike + "' from database", e); }
        finally
        {
            jdbcCleanup(stmt);
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public Object[] getAppNames(Connection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("Connection cannot be null in getApps(connection) call.");
        }

        PreparedStatement stmt = null;
        try
        {
            stmt = connection.prepareStatement("SELECT app_cd FROM n_cube GROUP BY app_cd");
            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<String>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            return records.toArray();
        }
        catch (Exception e) { throw new RuntimeException("Unable to fetch all ncube app names from database", e); }
        finally
        {
            jdbcCleanup(stmt);
        }
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT ncubes can be updated.
     * @param connection JDBC connection
     * @param ncube NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public boolean updateCube(Connection connection, String app, NCube ncube, String version)
    {
        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null, app: " + app + ", version: " + version);
        }

        if (connection == null || app == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to updateCube() can be null. App: " + app + ", NCube: " + ncube.getName() + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                stmt = connection.prepareStatement("UPDATE n_cube SET cube_value_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'");
                stmt.setBytes(1, JsonWriter.objectToJson(ncube).getBytes("UTF-8"));
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(3, app);
                stmt.setString(4, ncube.getName());
                stmt.setString(5, version);
                int count = stmt.executeUpdate();
                if (count != 1)
                {
                    throw new IllegalStateException("Only one (1) row should be updated.");
                }
                return true;
            }
            catch(IllegalStateException e) { throw e; }
            catch(Exception e) { throw new RuntimeException("Unable to update NCube: " + ncube.getName() + ", app: " + app + ", version: " + version, e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * Persist the passed in NCube
     * @param connection JDBC connection
     * @param ncube NCube to be persisted
     */
    public void createCube(Connection connection, String app, NCube ncube, String version)
    {
        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null, app: " + app + ", version: " + version);
        }

        if (connection == null || app == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to createCube() can be null. App: " + app + ", NCube: " + ncube.getName() + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                stmt = connection.prepareStatement("SELECT n_cube_id AS \"id\" FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?");
                stmt.setString(1, app);
                stmt.setString(2, ncube.getName());
                stmt.setString(3, version);
                ResultSet rs = stmt.executeQuery();

                if (rs.next())
                {	// NCube with same name and version number already exists.
                    throw new IllegalStateException("NCube '" + ncube.getName() + "' (" + version + ") already exists.");
                }
                else
                {   // Do INSERT
                    stmt.close();
                    stmt = connection.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    stmt.setLong(1, UniqueIdGenerator.getUniqueId());
                    stmt.setString(2, app);
                    stmt.setString(3, ncube.getName());
                    String json = JsonWriter.objectToJson(ncube);
                    stmt.setBytes(4, json.getBytes("UTF-8"));
                    stmt.setString(5, version);
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    stmt.setDate(6, now);
                    stmt.setDate(7, now);
                    int rowCount = stmt.executeUpdate();
                    if (rowCount != 1)
                    {
                        throw new IllegalStateException("error saving new NCube: " + ncube.getName() + "', app: " + app + ", version: " + version + " (" + rowCount + " rows inserted, should be 1)");
                    }
                }

                cubeList.put(ncube.getName(), ncube);
            }
            catch (IllegalStateException e) { throw e; }
            catch (Exception e) { throw new RuntimeException("Unable to save NCube: " + ncube.getName() + ", app: " + app + ", version: " + version + " to database", e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * Move ncubes matching the passed in version and APP_CD from SNAPSHOT to RELEASE
     * state. All ncubes move version at the same time.  This is by design so that the
     * cube join commands do not need to mess with determining what ncube versions
     * they join with.
     *
     * @param connection JDBC connection
     * @param version String version to move from SNAPSHOT to RELEASE
     * @return int count of ncubes that were released
     */
    public int releaseCubes(Connection connection, String app, String version)
    {
        if (connection == null || app == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to releaseCubes() can be null. App: " + app + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;

            try
            {
                stmt = connection.prepareStatement("UPDATE n_cube SET update_dt = ?, status_cd = '" + ReleaseStatus.RELEASE + "' WHERE app_cd = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'");
                stmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(2, app);
                stmt.setString(3, version);
                return stmt.executeUpdate();
            }
            catch (Exception e) { throw new RuntimeException("Unable to release NCubes for app: " + app + ", version: " + version + ", due to an error: ", e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * This API creates a SNAPSHOT set of cubes by copying all of
     * the RELEASE ncubes that match the oldVersion and app to
     * the new version in SNAPSHOT mode.  Basically, it duplicates
     * an entire set of NCubes and places a new version label on them,
     * in SNAPSHOT status.
     */
    public int createSnapshotCubes(Connection connection, String app, String oldVersion, String newVersion)
    {
        if (connection == null || app == null || oldVersion == null || newVersion == null)
        {
            throw new IllegalArgumentException("None of the arguments to createSnapshotCubes() can be null. App: " + app + ", oldVersion: " + oldVersion + ", newVersion: " + newVersion + ", connection: " + connection);
        }

        if (oldVersion.equals(newVersion))
        {
            throw new IllegalArgumentException("The version number must be different for the new SNAPSHOT version. App: " + app + ", oldVersion: " + oldVersion + ", newVersion: " + newVersion);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            boolean autoCommit = true;

            try
            {
                autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                stmt = connection.prepareStatement(
                        "SELECT n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin\n" +
                        "FROM n_cube\n" +
                        "WHERE app_cd = ? AND version_no_cd = ?");

                stmt.setString(1, app);
                stmt.setString(2, oldVersion);
                ResultSet rs = stmt.executeQuery();

                stmt2 = connection.prepareStatement(
                        "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin)\n" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                int count = 0;

                while (rs.next())
                {
                    count++;
                    stmt2.setLong(1, UniqueIdGenerator.getUniqueId());
                    stmt2.setString(2, rs.getString("n_cube_nm"));
                    stmt2.setBytes(3, rs.getBytes("cube_value_bin"));
                    stmt2.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                    stmt2.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                    stmt2.setString(6, rs.getString("create_hid"));
                    stmt2.setString(7, rs.getString("update_hid"));
                    stmt2.setString(8, newVersion);
                    stmt2.setString(9, ReleaseStatus.SNAPSHOT.name());
                    stmt2.setDate(10, rs.getDate("sys_effective_dt"));
                    stmt2.setDate(11, rs.getDate("sys_expiration_dt"));
                    stmt2.setDate(12, rs.getDate("business_effective_dt"));
                    stmt2.setDate(13, rs.getDate("business_expiration_dt"));
                    stmt2.setString(14, rs.getString("app_cd"));
                    stmt2.setBytes(15, rs.getBytes("test_data_bin"));
                    stmt2.setBytes(16, rs.getBytes("notes_bin"));
                    stmt2.executeUpdate();
                }
                connection.commit();
                return count;
            }
            catch(Exception e)
            {
                try
                {
                    connection.rollback();
                }
                catch (SQLException e1) { LOG.error("Failed to rollback transaction", e1); }
                throw new RuntimeException("Unable to create SNAPSHOT NCubes for app: " + app + ", version: " + oldVersion + ", due to an error: ", e);
            }
            finally
            {
                try
                {
                    connection.setAutoCommit(autoCommit);
                }
                catch (SQLException e) { LOG.error("Failed to restore Connection autoCommit", e); }
                jdbcCleanup(stmt);
                jdbcCleanup(stmt2);
            }
        }
    }

    /**
     * Delete the named NCube from the database
     * @param connection JDBC connection
     * @param name NCube to be deleted
     */
    public boolean deleteCube(Connection connection, String app, String name, String version, boolean allowDelete)
    {
        if (connection == null || app == null || name == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to deleteCube() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                if (allowDelete)
                {
                    stmt = connection.prepareStatement("DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?");
                }
                else
                {
                    stmt = connection.prepareStatement("DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'");
                }
                stmt.setString(1, app);
                stmt.setString(2, name);
                stmt.setString(3, version);
                int rows = stmt.executeUpdate();
                if (rows > 0)
                {
                    cubeList.remove(name);
                    return true;
                }
                return false;
            }
            catch (Exception e) { throw new RuntimeException("Unable to delete NCube: " + name + ", app: " + app + ", version: " + version + " from database", e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * Update the notes associated to an NCube
     * @return true if the update succeeds, false otherwise
     */
    public boolean updateNotes(Connection connection, String app, String name, String version, String notes)
    {
        if (connection == null || app == null || name == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to updateNotes() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                stmt = connection.prepareStatement("UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?");
                stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(3, app);
                stmt.setString(4, name);
                stmt.setString(5, version);
                int count = stmt.executeUpdate();
                if (count > 1)
                {
                    throw new IllegalStateException("Only one (1) row's notes should be updated.");
                }
                if (count == 0)
                {
                    throw new IllegalStateException("No NCube matching app: " + app + ", name: " + name + ", version: " + version);
                }
                return true;
            }
            catch(IllegalStateException e) { throw e; }
            catch(Exception e) { throw new RuntimeException("Unable to update notes for NCube: " + name + ", app: " + app + ", version: " + version, e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * Get the notes associated to an NCube
     * @return String notes.
     */
    public String getNotes(Connection connection, String app, String name, String version)
    {
        if (connection == null || app == null || name == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to getNotes() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", connection: " + connection);
        }

        PreparedStatement stmt = null;
        try
        {
            stmt = connection.prepareStatement("SELECT notes_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?");
            stmt.setString(1, app);
            stmt.setString(2, name);
            stmt.setString(3, version);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                byte[] notes = rs.getBytes("notes_bin");
                return new String(notes == null ? "".getBytes() : notes, "UTF-8");
            }
            throw new IllegalArgumentException("No NCube matching passed in parameters.");
        }
        catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Unable to fetch notes for NCube: " + name + ", app: " + app + ", version: " + version, e); }
        finally
        {
            jdbcCleanup(stmt);
        }
    }

    /**
     * Update the test data associated to an NCube
     * @return true if the update succeeds, false otherwise
     */
    public boolean updateTestData(Connection connection, String app, String name, String version, String testData)
    {
        if (connection == null || app == null || name == null || version == null)
        {
            throw new IllegalArgumentException("None of the arguments to updateTestData() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", connection: " + connection);
        }

        synchronized(cubeList)
        {
            PreparedStatement stmt = null;
            try
            {
                stmt = connection.prepareStatement("UPDATE n_cube SET test_data_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'");
                stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(3, app);
                stmt.setString(4, name);
                stmt.setString(5, version);
                int count = stmt.executeUpdate();
                if (count > 1)
                {
                    throw new IllegalStateException("Only one (1) row's test data should be updated.");
                }
                if (count == 0)
                {
                    throw new IllegalStateException("No NCube matching app: " + app + ", name: " + name + ", version: " + version);
                }
                return true;
            }
            catch(IllegalStateException e) { throw e; }
            catch(Exception e) { throw new RuntimeException("Unable to update test data for NCube: " + name + ", app: " + app + ", version: " + version, e); }
            finally
            {
                jdbcCleanup(stmt);
            }
        }
    }

    /**
     * Get the Test Data associated to the NCube.
     * @return String serialized JSON test data.  Use JsonReader to turn it back into
     * Java objects.
     */
    public static String getTestData(Connection connection, String app, String name, String version)
    {
        PreparedStatement stmt = null;
        try
        {
            if (connection == null || app == null || name == null || version == null)
            {
                throw new IllegalArgumentException("None of the arguments to getTestData() can be null. App: " + app + ", NCube: " + name + ", version: " + version + ", connection: " + connection);
            }
            stmt = connection.prepareStatement("SELECT test_data_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?");
            stmt.setString(1, app);
            stmt.setString(2, name);
            stmt.setString(3, version);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                byte[] testData = rs.getBytes("test_data_bin");
                return new String(testData == null ? "".getBytes() : testData, "UTF-8");
            }
            throw new IllegalArgumentException("No NCube matching passed in parameters.");
        }
        catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Unable to fetch test data for NCube: " + name + ", app: " + app + ", version: " + version, e); }
        finally
        {
            jdbcCleanup(stmt);
        }
    }

    public NCube getNCubeFromResource(String name)
    {
        try
        {
            URL url = NCubeManager.class.getResource("/" + name);
            File jsonFile = new File(url.getFile());
            FileInputStream in = new FileInputStream(jsonFile);
            JsonReader reader = new JsonReader(in, true);
            JsonObject jObj = (JsonObject) reader.readObject();
            reader.close();
            String json = JsonWriter.objectToJson(jObj);
            NCube ncube = NCube.fromSimpleJson(json);
            addCube(ncube);
            return ncube;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load ncube from resource: " + name, e);
        }
    }

    public List<NCube> getNCubesFromResource(String name)
    {
        String lastSuccessful = "";
        try
        {
            URL url = NCubeManager.class.getResource("/" + name);
            File arrayJsonCubes = new File(url.getFile());
            FileInputStream in = new FileInputStream(arrayJsonCubes);
            JsonReader reader = new JsonReader(in, true);
            JsonObject ncubes = (JsonObject) reader.readObject();
            Object[] cubes = ncubes.getArray();
            List<NCube> cubeList = new ArrayList<NCube>(cubes.length);

            for (Object cube : cubes)
            {
                JsonObject ncube = (JsonObject) cube;
                String json = JsonWriter.objectToJson(ncube);
                NCube nCube = NCube.fromSimpleJson(json);
                addCube(nCube);
                lastSuccessful = nCube.getName();
                cubeList.add(nCube);
            }

            return cubeList;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load ncubes from resource: " + name + ", last successful cube: " + lastSuccessful, e);
        }
    }
}
