package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.NCubeConnectionProvider.ContextKey;
import com.cedarsoftware.ncube.formatters.JsonFormatter;
import com.cedarsoftware.ncube.util.CdnClassLoader;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import groovy.lang.GroovyClassLoader;
import ncube.grv.method.NCubeGroovyController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * This class manages a list of NCubes.  This class is referenced
 * by NCube in one place - when it joins to other cubes, it consults
 * the NCubeManager to find the joined NCube.
 * <p/>
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
    private static final Map<String, NCube> cubeList = new ConcurrentHashMap<>();
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);
    private static Map<String, Map<String, Advice>> advices = new LinkedHashMap<>();
    private static Map<String, GroovyClassLoader> urlClassLoaders = new ConcurrentHashMap<>();

    private enum ConnectionType
    {
        JDBC, MONGO;

        static ConnectionType resolveConnectionType(Map<ContextKey,Object> connectionContext)
        {
            if (connectionContext.containsKey(ContextKey.JDBC_CONNECTION))
                return JDBC;

            if (connectionContext.containsKey(ContextKey.MONGO_CLIENT))
                return MONGO;

            throw new IllegalArgumentException("Unable to resolve connection type from input connection context...");
        }
    }

    static
    {
        urlClassLoaders.put("file", new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true));
    }

    /**
     * @param name String name of an NCube.
     * @return NCube instance with the given name.  Please note
     * that the cube must be loaded first before calling this.
     */
    public static NCube getCube(String name, String version)
    {
        return cubeList.get(makeCacheKey(name, version));
    }

    static String makeCacheKey(String name, String version)
    {
        return name + '.' + version;
    }

    public static void addBaseResourceUrls(List<String> urls, String version)
    {
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(version);

        if (urlClassLoader == null) {
            LOG.info("Creating ClassLoader, n-cube version: " + version + ", urls: " + urls);
            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
            urlClassLoaders.put(version, urlClassLoader);
        }
        else {
            LOG.info("Adding resource URLs, n-cube version: " + version + ", urls: " + urls);
        }

        addUrlsToClassLoader(urls, urlClassLoader);
    }

    /**
     * @Deprecated Call addBaseResourceUrls() instead.
     */
    @Deprecated
    public static void setBaseResourceUrls(List<String> urls, String version)
    {
        addBaseResourceUrls(urls, version);
    }

    /**
     * @Deprecated Call addBaseResourceUrls() instead.
     */
    @Deprecated
    public static void setUrlClassLoader(List<String> urls, String version)
    {
        addBaseResourceUrls(urls, version);
    }

    private static void addUrlsToClassLoader(List<String> urls, GroovyClassLoader urlClassLoader)
    {
        for (String url : urls)
        {
            try
            {
                if (!url.endsWith("/"))
                {
                    url += "/";
                }
                urlClassLoader.addURL(new URL(url));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("A URL in List of URLs is malformed: " + url, e);
            }
        }
    }

    public static URLClassLoader getUrlClassLoader(String version)
    {
        return urlClassLoaders.get(version);
    }

    /**
     * Add a cube to the internal map of available cubes.
     *
     * @param ncube NCube to add to the list.
     */
    static void addCube(NCube ncube, String version)
    {
        synchronized (cubeList)
        {
            ncube.setVersion(version);
            cubeList.put(makeCacheKey(ncube.getName(), version), ncube);

            for (Map.Entry<String, Map<String, Advice>> entry : advices.entrySet())
            {
                String regex = StringUtilities.wildcardToRegexString(entry.getKey());
                Axis axis = ncube.getAxis("method");
                if (axis != null)
                {   // Controller methods
                    for (Column column : axis.getColumnsWithoutDefault())
                    {
                        String method = column.getValue().toString();
                        String classMethod = ncube.getName() + '.' + method + "()";
                        if (classMethod.matches(regex))
                        {
                            for (Advice advice : entry.getValue().values())
                            {
                                ncube.addAdvice(advice, method);
                            }
                        }
                    }
                }
                else
                {   // Expressions
                    String classMethod = ncube.getName() + ".run()";
                    if (classMethod.matches(regex))
                    {
                        for (Advice advice : entry.getValue().values())
                        {
                            ncube.addAdvice(advice, "run");
                        }
                    }
                }
            }
        }
    }

    /**
     * @return Map<String, NCube> of all NCubes that are currently
     * loaded (cached) in memory.  A copy of the internal cache
     * is returned.
     */
    public static Map<String, NCube> getCachedNCubes()
    {
        synchronized (cubeList)
        {
            return new TreeMap<>(cubeList);
        }
    }

    /**
     * Used for testing.
     */
    public static void clearCubeList()
    {
        synchronized (cubeList)
        {
            cubeList.clear();
            GroovyBase.clearCache();
            NCubeGroovyController.clearCache();
            for (Map.Entry<String, GroovyClassLoader> entry : urlClassLoaders.entrySet())
            {
                URLClassLoader classLoader = entry.getValue();
                ((GroovyClassLoader)classLoader).clearCache(); // free up Class cache
            }
            advices.clear();
        }
    }

    /**
     * Associate Advice to all n-cubes that match the passed in regular expression.
     */
    public static void addAdvice(String wildcard, Advice advice)
    {
        synchronized (cubeList)
        {
            Map<String, Advice> current = advices.get(wildcard);
            if (current == null)
            {
                current = new LinkedHashMap<>();
                advices.put(wildcard, current);
            }

            current.put(advice.getName(), advice);
            String regex = StringUtilities.wildcardToRegexString(wildcard);

            for (NCube ncube : cubeList.values())
            {
                Axis axis = ncube.getAxis("method");
                if (axis != null)
                {   // Controller methods
                    for (Column column : axis.getColumnsWithoutDefault())
                    {
                        String method = column.getValue().toString();
                        String classMethod = ncube.getName() + '.' + method + "()";
                        if (classMethod.matches(regex))
                        {
                            ncube.addAdvice(advice, method);
                        }
                    }
                }
                else
                {   // Expressions
                    String classMethod = ncube.getName() + ".run()";
                    if (classMethod.matches(regex))
                    {
                        ncube.addAdvice(advice, "run");
                    }
                }
            }
        }
    }

    static void validateConnection(Connection c)
    {
        if (c == null)
        {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        try
        {
            if (!c.isValid(2))
            {
                throw new IllegalArgumentException("Jdbc connection is not a valid connection...");
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to perform idValid on input jdbc connection...", e);
        }
    }

    public static void validateApp(String app)
    {
        if (StringUtilities.isEmpty(app))
        {
            throw new IllegalArgumentException("App cannot be null or empty");
        }
    }

    public static void validateCubeName(String cubeName)
    {
        if (StringUtilities.isEmpty(cubeName))
        {
            throw new IllegalArgumentException("n-cube name cannot be null or empty");
        }

        Matcher m = Regexes.validCubeName.matcher(cubeName);
        if (m.find())
        {
            if (cubeName.equals(m.group(0)))
            {
                return;
            }
        }
        throw new IllegalArgumentException("n-cube name can only contain a-z, A-Z, 0-9, :, ., _, -, #, and |");
    }

    public static void validateVersion(String version)
    {
        if (StringUtilities.isEmpty(version))
        {
            throw new IllegalArgumentException("n-cube version cannot be null or empty");
        }

        Matcher m = Regexes.validVersion.matcher(version);
        if (m.find())
        {
            return;
        }
        throw new IllegalArgumentException("n-cube version must follow the form n.n.n where n is a number 0 or greater. The numbers stand for major.minor.revision");
    }

    public static void validateStatus(String status)
    {
        if ("RELEASE".equals(status) || "SNAPSHOT".equals(status))
        {
            return;
        }
        throw new IllegalArgumentException("n-cube status must be RELEASE or SNAPSHOT");
    }


    public static void validateTestData(String testData) {

    }

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, and status.
     */
    public static void loadCubes(Connection connection, String app, String version, String status)
    {
        loadCubes(connection, app, version, status, null);
    }

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, status, and sysDate.
     */
    public static void loadCubes(Connection connection, String app, String version, String status, Date sysDate)
    {
        validate(connection, app, version);
        validateStatus(status);

        if (sysDate == null)
        {
            sysDate = new Date();
        }

        synchronized (cubeList)
        {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?"))
            {
                java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());

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
                    NCube ncube = ncubeFromJson(json);
                    addCube(ncube, version);
                }
            }
            catch (Exception e)
            {
                String s = "Unable to load n-cubes, app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    public static NCube loadCube(Connection connection, String app, String name, String version, String status, Date sysDate, boolean includeTests)
    {
        validate(connection, app, version);
        validateCubeName(name);
        if (sysDate == null)
        {
            sysDate = new Date();
        }

        synchronized (cubeList)
        {
            String query = includeTests ?
                    "SELECT cube_value_bin, test_data_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?" :
                    "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?";

            try (PreparedStatement stmt = connection.prepareStatement(query))
            {
                java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());

                stmt.setString(1, name);
                stmt.setString(2, app);
                stmt.setDate(3, systemDate);
                stmt.setDate(4, systemDate);
                stmt.setString(5, version);
                stmt.setString(6, status);

                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        byte[] jsonBytes = rs.getBytes("cube_value_bin");
                        String json = new String(jsonBytes, "UTF-8");
                        NCube ncube = ncubeFromJson(json);


                        if (includeTests) {
                            byte[] bytes = rs.getBytes("test_data_bin");

                            if (bytes != null)
                            {
                                ncube.setTestData(new String(bytes, "UTF-8"));
                            }
                        }

                        if (rs.next())
                        {
                            throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate);
                        }

                        addCube(ncube, version);
                        Set<String> subCubeList = ncube.getReferencedCubeNames();

                        for (String cubeName : subCubeList)
                        {
                            final String cacheKey = makeCacheKey(cubeName, version);
                            if (!cubeList.containsKey(cacheKey))
                            {
                                loadCube(connection, app, cubeName, version, status, sysDate, includeTests);
                            }
                        }
                        return ncube;
                    }
                }
                return null; // Indicates not found
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to load nNCube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    public static NCube loadCube(Connection connection, String app, String name, String version, String status, Date sysDate)
    {
        return loadCube(connection, app, name, version, status, sysDate, false);
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    public static NCube loadCubeWithTests(Connection connection, String app, String name, String version, String status, Date sysDate)
    {
        return loadCube(connection, app, name, version, status, sysDate, true);
    }


    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    public static boolean doesCubeExist(Connection connection, String app, String name, String version, String status, Date sysDate)
    {
        validate(connection, app, version);

        StringBuilder builder = new StringBuilder("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)");

        if (status != null) {
            validateStatus(status);
            builder.append(" AND status_cd = ?");
        }

        if (name != null) {
            validateCubeName(name);
            builder.append(" AND n_cube_nm = ?");
        }

        java.sql.Date systemDate = new java.sql.Date((sysDate == null) ? new Date().getTime() : sysDate.getTime());

        try (PreparedStatement ps = connection.prepareStatement(builder.toString()))
        {

            ps.setString(1, app);
            ps.setString(2, version);
            ps.setDate(3, systemDate);
            ps.setDate(4, systemDate);

            int count = 4;
            if (status != null)
            {
                ps.setString(++count, status);
            }

            if (name != null)
            {
                ps.setString(++count, name);
            }

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Retrieve all cube names that are deeply referenced by the named app, cube (name), version, and status.
     */
    public static void getReferencedCubeNames(Connection connection, String app, String name, String version, String status, Date sysDate, Set<String> refs)
    {
        validate(connection, app, version);
        validateCubeName(name);
        validateStatus(status);

        if (sysDate == null)
        {
            sysDate = new Date();
        }
        if (refs == null)
        {
            throw new IllegalArgumentException("null passed in for Set to hold referenced n-cube names");
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try(PreparedStatement stmt = connection.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?"))
        {
            stmt.setString(1, name);
            stmt.setString(2, app);
            stmt.setDate(3, systemDate);
            stmt.setDate(4, systemDate);
            stmt.setString(5, version);
            stmt.setString(6, status);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube ncube = ncubeFromJson(json);

                    if (rs.next())
                    {
                        throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate);
                    }

                    Set<String> subCubeList = ncube.getReferencedCubeNames();
                    refs.addAll(subCubeList);

                    for (String cubeName : subCubeList)
                    {
                        if (!refs.contains(cubeName))
                        {
                            getReferencedCubeNames(connection, app, cubeName, version, status, sysDate, refs);
                        }
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
            String s = "Unable to load nNCube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Retrieve all n-cubes that have a name that matches the SQL like statement, within the specified app, status,
     * version, and system date.
     */
    public static Object[] getNCubes(Connection connection, String app, String version, String status, String sqlLike, Date sysDate)
    {
        validate(connection, app, version);
        validateStatus(status);

        if (sqlLike == null)
        {
            sqlLike = "%";
        }

        if (sysDate == null)
        {
            sysDate = new Date();
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try(PreparedStatement stmt = connection.prepareStatement("SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, update_dt, " +
                    "create_hid, update_hid, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt FROM n_cube WHERE n_cube_nm LIKE ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)"))
        {
            stmt.setString(1, sqlLike);
            stmt.setString(2, app);
            stmt.setString(3, version);
            stmt.setString(4, status);
            stmt.setDate(5, systemDate);
            stmt.setDate(6, systemDate);

            ResultSet rs = stmt.executeQuery();
            List<NCubeInfoDto> records = new ArrayList<NCubeInfoDto>();

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

    /**
     * Duplicate the specified n-cube, given it the new name, and the same app, version, status as the source n-cube.
     */
    public static void duplicate(Connection connection, String newName, String name, String newApp, String app, String newVersion, String version, String status, Date sysDate)
    {
        NCube ncube = loadCube(connection, app, name, version, status, sysDate);
        NCube copy = ncube.duplicate(newName);
        createCube(connection, newApp, copy, newVersion);
        String json = getTestData(connection, app, name, version, sysDate);
        updateTestData(connection, newApp, newName, newVersion, json);
        String notes = getNotes(connection, app, name, version, sysDate);
        updateNotes(connection, newApp, newName, newVersion, notes);
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public static Object[] getAppNames(Connection connection, Date sysDate)
    {
        validateConnection(connection);
        if (sysDate == null)
        {
            sysDate = new Date();
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try(PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT app_cd FROM n_cube WHERE sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)"))
        {
            stmt.setDate(1, systemDate);
            stmt.setDate(2, systemDate);
            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<String>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            Collections.sort(records);
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all ncube app names from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public static Object[] getAppVersions(Connection connection, String app, String status, Date sysDate)
    {
        validateConnection(connection);
        validateApp(app);
        validateStatus(status);
        if (sysDate == null)
        {
            sysDate = new Date();
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? and status_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)"))
        {
            stmt.setString(1, app);
            stmt.setString(2, status);
            stmt.setDate(3, systemDate);
            stmt.setDate(4, systemDate);

            ResultSet rs = stmt.executeQuery();
            List<String> records = new ArrayList<String>();

            while (rs.next())
            {
                records.add(rs.getString(1));
            }
            Collections.sort(records);  // May need to enhance to ensure 2.19.1 comes after 2.2.1
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all ncube app versions from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT ncubes can be updated.
     *
     * @param connection JDBC connection
     * @param ncube      NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public static boolean updateCube(Connection connection, String app, NCube ncube, String version)
    {
        validate(connection, app, version);
        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null for updating");
        }

        synchronized (cubeList)
        {
            try(PreparedStatement stmt = connection.prepareStatement("UPDATE n_cube SET cube_value_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'"))
            {
                stmt.setBytes(1, new JsonFormatter().format(ncube).getBytes("UTF-8"));
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(3, app);
                stmt.setString(4, ncube.getName());
                stmt.setString(5, version);
                int count = stmt.executeUpdate();
                if (count != 1)
                {
                    throw new IllegalStateException("Only one (1) row should be updated.");
                }
                ncube.setVersion(version);
                return true;
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to update NCube: " + ncube.getName() + ", app: " + app + ", version: " + version;
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Persist the passed in NCube
     *
     * @param connection JDBC connection
     * @param ncube      NCube to be persisted
     */
    public static void createCube(Connection connection, String app, NCube ncube, String version)
    {
        validate(connection, app, version);
        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null when creating a new n-cube");
        }
        validateCubeName(ncube.getName());

        synchronized (cubeList)
        {
            try
            {
                if (doesCubeExist(connection, app, ncube.getName(), version, null, null))
                {
                    throw new IllegalStateException("NCube '" + ncube.getName() + "' (" + app + " " + version + ") already exists.");
                }

                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)"))
                {
                    insert.setLong(1, UniqueIdGenerator.getUniqueId());
                    insert.setString(2, app);
                    insert.setString(3, ncube.getName());
                    String json = new JsonFormatter().format(ncube);
                    insert.setBytes(4, json.getBytes("UTF-8"));
                    insert.setString(5, version);
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    insert.setDate(6, now);
                    insert.setDate(7, now);
                    int rowCount = insert.executeUpdate();
                    if (rowCount != 1)
                    {
                        throw new IllegalStateException("error inserting new NCube: " + ncube.getName() + "', app: " + app + ", version: " + version + " (" + rowCount + " rows inserted, should be 1)");
                    }
                    addCube(ncube, version);
                }
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to save NCube: " + ncube.getName() + ", app: " + app + ", version: " + version + " to database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
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
     * @param version    String version to move from SNAPSHOT to RELEASE
     * @return int count of ncubes that were released
     */
    public static int releaseCubes(Connection connection, String app, String version)
    {
        validate(connection, app, version);

        synchronized (cubeList)
        {
            try
            {
                if (doesCubeExist(connection, app, null, version, ReleaseStatus.RELEASE.toString(), null))
                {
                    throw new IllegalStateException("A RELEASE version " + version + " already exists. Have system admin renumber your SNAPSHOT version.");
                }

                try (PreparedStatement statement = connection.prepareStatement("UPDATE n_cube SET update_dt = ?, status_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?"))
                {
                    statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                    statement.setString(2, ReleaseStatus.RELEASE.toString());
                    statement.setString(3, app);
                    statement.setString(4, version);
                    statement.setString(5, ReleaseStatus.SNAPSHOT.toString());
                    return statement.executeUpdate();
                }
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to release NCubes for app: " + app + ", version: " + version + ", due to an error: " + e.getMessage();
                LOG.error(s, e);
                throw new RuntimeException(s, e);
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
    public static int createSnapshotCubes(Connection connection, String app, String relVersion, String newSnapVer)
    {
        validate(connection, app, relVersion);
        validateVersion(newSnapVer);

        if (relVersion.equals(newSnapVer))
        {
            throw new IllegalArgumentException("New SNAPSHOT version " + relVersion + " cannot be the same as the RELEASE version.");
        }

        synchronized (cubeList)
        {
            try
            {
                if (doesCubeExist(connection, app, null, newSnapVer, null, null))
                {
                    throw new IllegalStateException("New SNAPSHOT Version specified (" + newSnapVer + ") matches an existing version.  Specify new SNAPSHOT version that does not exist.");
                }

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin\n" +
                                "FROM n_cube\n" +
                                "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?"
                ))
                {

                    select.setString(1, app);
                    select.setString(2, relVersion);
                    select.setString(3, ReleaseStatus.RELEASE.name());


                    try (ResultSet rs = select.executeQuery())
                    {

                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin)\n" +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
                                insert.setString(8, newSnapVer);
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
            catch (IllegalStateException e) {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to create SNAPSHOT NCubes for app: " + app + ", version: " + newSnapVer + ", due to an error: " + e.getMessage();
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    private static void validate(Connection connection, String app, String relVersion)
    {
        validateConnection(connection);
        validateApp(app);
        validateVersion(relVersion);
    }

    /**
     * Change the SNAPSHOT version value.
     */
    public static void changeVersionValue(Connection connection, String app, String currVersion, String newSnapVer)
    {
        validate(connection, app, currVersion);
        validateVersion(newSnapVer);

        synchronized (cubeList)
        {
            try
            {
                if (doesCubeExist(connection, app, null, newSnapVer, ReleaseStatus.RELEASE.name(), null))
                {
                    throw new IllegalStateException("RELEASE n-cubes found with version " + newSnapVer + ".  Choose a different SNAPSHOT version.");
                }

                try (PreparedStatement ps = connection.prepareStatement("UPDATE n_cube SET update_dt = ?, version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'"))
                {
                    ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                    ps.setString(2, newSnapVer);
                    ps.setString(3, app);
                    ps.setString(4, currVersion);
                    int count = ps.executeUpdate();
                    if (count < 1)
                    {
                        throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + currVersion + ", therefore nothing changed.");
                    }
                }
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to change SNAPSHOT version from " + currVersion + " to " + newSnapVer + " for app: " + app + ", due to an error: " + e.getMessage();
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }


    public static boolean renameCube(Connection connection, String oldName, String newName, String app, String version)
    {
        validate(connection, app, version);
        validateCubeName(oldName);
        validateCubeName(newName);

        if (oldName.equalsIgnoreCase(newName))
        {
            throw new IllegalArgumentException("Old name cannot be the same as the new name, name: " + oldName);
        }

        synchronized (cubeList)
        {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE n_cube SET n_cube_nm = ? WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'"))
            {
                ps.setString(1, newName);
                ps.setString(2, app);
                ps.setString(3, version);
                ps.setString(4, oldName);
                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalArgumentException("No n-cube found to rename, for app:" + app + ", version: " + version + ", original name: " + oldName);
                }

                cubeList.remove(makeCacheKey(oldName, version));
                return true;
            }
            catch (IllegalArgumentException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to rename n-cube due to an error: " + e.getMessage();
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Delete the named NCube from the database
     *
     * @param connection JDBC connection
     * @param name       NCube to be deleted
     */
    public static boolean deleteCube(Connection connection, String app, String name, String version, boolean allowDelete)
    {
        validate(connection, app, version);
        validateCubeName(name);

        synchronized (cubeList)
        {
            String statement = allowDelete ?
                    "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?" :
                    "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ?";

            try (PreparedStatement ps = connection.prepareStatement(statement))
            {
                ps.setString(1, app);
                ps.setString(2, name);
                ps.setString(3, version);

                if (!allowDelete) {
                    ps.setString(4, ReleaseStatus.SNAPSHOT.name());
                }

                int rows = ps.executeUpdate();
                if (rows > 0)
                {
                    cubeList.remove(makeCacheKey(name, version));
                    return true;
                }
                return false;
            }
            catch (Exception e)
            {
                String s = "Unable to delete NCube: " + name + ", app: " + app + ", version: " + version + " from database: " + e.getMessage();
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Update the notes associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateNotes(Connection connection, String app, String name, String version, String notes)
    {
        validate(connection, app, version);
        validateCubeName(name);

        synchronized (cubeList)
        {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?"))
            {
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
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to update notes for NCube: " + name + ", app: " + app + ", version: " + version, e);
            }
        }
    }

    /**
     * Get the notes associated to an NCube
     *
     * @return String notes.
     */
    public static String getNotes(Connection connection, String app, String name, String version, Date sysDate)
    {
        validate(connection, app, version);
        validateCubeName(name);

        if (sysDate == null)
        {
            sysDate = new Date();
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try (PreparedStatement stmt = connection.prepareStatement("SELECT notes_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)"))
        {
            stmt.setString(1, app);
            stmt.setString(2, name);
            stmt.setString(3, version);
            stmt.setDate(4, systemDate);
            stmt.setDate(5, systemDate);
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
            String s = "Unable to fetch notes for NCube: " + name + ", app: " + app + ", version: " + version;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    /**
     * Update the test data associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateTestData(Connection connection, String app, String name, String version, String testData)
    {
        validate(connection, app, version);
        validateCubeName(name);
        validateTestData(testData);

        synchronized (cubeList)
        {
            try (PreparedStatement stmt =  connection.prepareStatement("UPDATE n_cube SET test_data_bin=?, update_dt=? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'"))
            {
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
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to update test data for NCube: " + name + ", app: " + app + ", version: " + version;
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    /**
     * Get the Test Data associated to the NCube.
     *
     * @return String serialized JSON test data.  Use JsonReader to turn it back into
     * Java objects.
     */
    public static String getTestData(Connection connection, String app, String name, String version, Date sysDate)
    {
        validate(connection, app, version);
        validateCubeName(name);

        if (sysDate == null)
        {
            sysDate = new Date();
        }

        java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());
        try (PreparedStatement stmt = connection.prepareStatement("SELECT test_data_bin FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)"))
        {
            stmt.setString(1, app);
            stmt.setString(2, name);
            stmt.setString(3, version);
            stmt.setDate(4, systemDate);
            stmt.setDate(5, systemDate);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                byte[] testData = rs.getBytes("test_data_bin");
                return new String(testData == null ? "".getBytes() : testData, "UTF-8");
            }
            throw new IllegalArgumentException("No NCube matching passed in parameters.");
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch test data for NCube: " + name + ", app: " + app + ", version: " + version;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = NCubeManager.class.getResource("/" + name);
        IOUtilities.transfer(new File(url.getFile()), out);
        return new String(out.toByteArray(), "UTF-8");
    }

    public static NCube getNCubeFromResource(String name)
    {
        try
        {
            String json = getResourceAsString(name);
            NCube ncube = ncubeFromJson(json);
            addCube(ncube, "file");
            return ncube;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load ncube from resource: " + name, e);
        }
    }

    /**
     * Still used in getNCubesFromResource
     */
    private static Object[] getJsonObjectFromResource(String name) throws IOException
    {
        JsonReader reader = null;
        try
        {
            URL url = NCubeManager.class.getResource("/" + name);
            File jsonFile = new File(url.getFile());
            InputStream in = new BufferedInputStream(new FileInputStream(jsonFile));
            reader = new JsonReader(in, true);
            return (Object[]) reader.readObject();
        }
        finally
        {
            IOUtilities.close(reader);
        }
    }

    public static List<NCube> getNCubesFromResource(String name)
    {
        String lastSuccessful = "";
        try
        {
            Object[] cubes = getJsonObjectFromResource(name);
            List<NCube> cubeList = new ArrayList<>(cubes.length);

            for (Object cube : cubes)
            {
                JsonObject ncube = (JsonObject) cube;
                String json = JsonWriter.objectToJson(ncube);
                NCube nCube = NCube.fromSimpleJson(json);
                addCube(nCube, "file");
                lastSuccessful = nCube.getName();
                cubeList.add(nCube);
            }

            return cubeList;
        }
        catch (Exception e)
        {
            String s = "Failed to load ncubes from resource: " + name + ", last successful cube: " + lastSuccessful;
            LOG.warn(s);
            throw new RuntimeException(s, e);
        }
    }

    static NCube ncubeFromJson(String json) throws IOException
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
                return ncube;
            }
            catch (Exception e1)
            {
                throw e;
            }
        }
    }


    //----------------------new api's that take persistence connection provider to replace connection on param list-----------------------------

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, and status.
     */
    public static void loadCubes(NCubeConnectionProvider nCubeConnectionProvider, String app, String version, String status)
    {
        loadCubes(nCubeConnectionProvider, app, version, status, null);
    }


    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, status, and sysDate.
     * @param nCubeConnectionProvider
     * @param app
     * @param version
     * @param status
     * @param sysDate
     */
    public static void loadCubes(NCubeConnectionProvider nCubeConnectionProvider, String app, String version, String status, Date sysDate)
    {
        validateStatus(status);

        if (sysDate == null)
            sysDate = new Date();

        ConnectionType connectionType = ConnectionType.resolveConnectionType(nCubeConnectionProvider.getConnectionContext());

        switch (connectionType)
        {
            case JDBC:
                Connection connection = extractJdbcConnection(nCubeConnectionProvider.getConnectionContext());
                jdbcLoadCubes(connection, app, version, status, sysDate);
                break;
            case MONGO:
                throw new UnsupportedOperationException("Mongo support not yet implemented...");
            default:
                throw new IllegalArgumentException("Unsupported database/persistence connection type...");
        }
    }


    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     * @param nCubeConnectionProvider
     * @param app
     * @param name
     * @param version
     * @param status
     * @param sysDate
     * @param includeTests
     * @return NCube that matches, or null if not found.
     * @throws java.lang.IllegalArgumentException when persistence connection type can not be determined
     */
    public static NCube loadCube(NCubeConnectionProvider nCubeConnectionProvider, String app, String name, String version, String status, Date sysDate, boolean includeTests)
    {
        validateCubeName(name);

        if (sysDate == null)
            sysDate = new Date();

        ConnectionType connectionType = ConnectionType.resolveConnectionType(nCubeConnectionProvider.getConnectionContext());
        NCube loadedCube;

        switch (connectionType)
        {
            case JDBC:
                Connection connection = extractJdbcConnection(nCubeConnectionProvider.getConnectionContext());
                loadedCube = jdbcLoadCube(connection, app, name, version, status, sysDate, includeTests);
                break;
            case MONGO:
                throw new UnsupportedOperationException("Mongo support not yet implemented...");
            default:
                throw new IllegalArgumentException("Unsupported database/persistence connection type...");
        }

        return loadedCube;
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     * @param nCubeConnectionProvider
     * @param app
     * @param name
     * @param version
     * @param status
     * @param sysDate
     * @return NCube that matches, or null if not found.
     * @throws java.lang.IllegalArgumentException when persistence connection type can not be determined
     */
    public static NCube loadCube(NCubeConnectionProvider nCubeConnectionProvider, String app, String name, String version, String status, Date sysDate)
    {
        return loadCube(nCubeConnectionProvider, app, name, version, status, sysDate, false);
    }

    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     * @param nCubeConnectionProvider
     * @param app
     * @param name
     * @param version
     * @param status
     * @param sysDate
     * @return NCube that matches, or null if not found.
     * @throws java.lang.IllegalArgumentException when persistence connection type can not be determined
     */
    public static NCube loadCubeWithTests(NCubeConnectionProvider nCubeConnectionProvider, String app, String name, String version, String status, Date sysDate)
    {
        return loadCube(nCubeConnectionProvider, app, name, version, status, sysDate, true);
    }


    /**
     * Load an NCube from the database (any joined sub-cubes will also be loaded).
     *
     * @return NCube that matches, or null if not found.
     */
    public static boolean doesCubeExist(NCubeConnectionProvider nCubeConnectionProvider, String app, String name, String version, String status, Date sysDate)
    {
        ConnectionType connectionType = ConnectionType.resolveConnectionType(nCubeConnectionProvider.getConnectionContext());
        boolean ncubeExists;

        switch (connectionType)
        {
            case JDBC:
                Connection connection = extractJdbcConnection(nCubeConnectionProvider.getConnectionContext());
                ncubeExists = jdbcDoesNCubeExist(connection, app, name, version, status, sysDate);
                break;
            case MONGO:
                throw new UnsupportedOperationException("Mongo support not yet implemented...");
            default:
                throw new IllegalArgumentException("Unsupported database/persistence connection type...");
        }

        return ncubeExists;
    }


//    }

    private static void validate(String app, String version)
    {
        validateApp(app);
        validateVersion(version);
    }

    private static Connection extractJdbcConnection(Map<ContextKey,Object> connectionContext)
    {
        Connection connection = (Connection)connectionContext.get(ContextKey.JDBC_CONNECTION);
        validateConnection(connection);
        return connection;
    }

    private static void jdbcLoadCubes(Connection connection, String app, String version, String status, Date sysDate)
    {
        validate(app, version);

        synchronized (cubeList)
        {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?"))
            {
                java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());

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
                    NCube ncube = ncubeFromJson(json);
                    addCube(ncube, version);
                }
            }
            catch (Exception e)
            {
                String s = "Unable to load n-cubes, app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    private static NCube jdbcLoadCube(Connection connection, String app, String name, String version, String status, Date sysDate, boolean includeTests)
    {
        validate(app, version);

        synchronized (cubeList)
        {
            String query = includeTests ?
                "SELECT cube_value_bin, test_data_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?" :
                "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?";

            try (PreparedStatement stmt = connection.prepareStatement(query))
            {
                java.sql.Date systemDate = new java.sql.Date(sysDate.getTime());

                stmt.setString(1, name);
                stmt.setString(2, app);
                stmt.setDate(3, systemDate);
                stmt.setDate(4, systemDate);
                stmt.setString(5, version);
                stmt.setString(6, status);

                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        byte[] jsonBytes = rs.getBytes("cube_value_bin");
                        String json = new String(jsonBytes, "UTF-8");
                        NCube ncube = ncubeFromJson(json);


                        if (includeTests) {
                            byte[] bytes = rs.getBytes("test_data_bin");

                            if (bytes != null)
                            {
                                ncube.setTestData(new String(bytes, "UTF-8"));
                            }
                        }

                        if (rs.next())
                        {
                            throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate);
                        }

                        addCube(ncube, version);
                        Set<String> subCubeList = ncube.getReferencedCubeNames();

                        for (String cubeName : subCubeList)
                        {
                            final String cacheKey = makeCacheKey(cubeName, version);
                            if (!cubeList.containsKey(cacheKey))
                            {
                                loadCube(connection, app, cubeName, version, status, sysDate, includeTests);
                            }
                        }
                        return ncube;
                    }
                }
                return null; // Indicates not found
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to load nNCube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    private static boolean jdbcDoesNCubeExist(Connection connection, String app, String name, String version, String status, Date sysDate)
    {
        validate(app, version);

        StringBuilder builder = new StringBuilder("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)");

        if (status != null) {
            validateStatus(status);
            builder.append(" AND status_cd = ?");
        }

        if (name != null) {
            validateCubeName(name);
            builder.append(" AND n_cube_nm = ?");
        }

        java.sql.Date systemDate = new java.sql.Date((sysDate == null) ? new Date().getTime() : sysDate.getTime());

        try (PreparedStatement ps = connection.prepareStatement(builder.toString()))
        {

            ps.setString(1, app);
            ps.setString(2, version);
            ps.setDate(3, systemDate);
            ps.setDate(4, systemDate);

            int count = 4;
            if (status != null)
            {
                ps.setString(++count, status);
            }

            if (name != null)
            {
                ps.setString(++count, name);
            }

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cube: " + name + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }
}
