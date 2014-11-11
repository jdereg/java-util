package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnClassLoader;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.MapUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<ApplicationID, Map<String, Object>> ncubeCache = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, Map<String, Advice>> advices = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, GroovyClassLoader> urlClassLoaders = new ConcurrentHashMap<>();
    private static NCubePersister nCubePersister;
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);
    private static final String CLASSPATH_CUBE = "sys.classpath";

    /**
     * Store the Persister to be used with the NCubeManager API (Dependency Injection API)
     */
    public static void setNCubePersister(NCubePersister persister)
    {
        nCubePersister = persister;
    }

    /**
     * Fetch all the n-cube names for the given ApplicationID.  This API
     * will load all cube records for the ApplicationID (NCubeInfoDtos),
     * and then get the names from them.
     *
     * @return Set<String> n-cube names.  If an empty Set is returned,
     * then there are no persisted n-cubes for the passed in ApplicationID.
     */
    public static Set<String> getCubeNames(ApplicationID appId)
    {
        Object[] cubeInfos = getCubeRecordsFromDatabase(appId, "");
        Set<String> names = new TreeSet<>();

        for (Object cubeInfo : cubeInfos)
        {
            NCubeInfoDto info = (NCubeInfoDto) cubeInfo;
            names.add(info.name);
        }

        if (names.isEmpty())
        {   // Support tests that load cubes from JSON files...
            for (Object value : getCacheForApp(appId).values())
            {
                String name;
                if (value instanceof NCube)
                {   // NCube info cache, get name from it.
                    NCube cube = (NCube) value;
                    name = cube.name;
                }
                else
                {   // NCubeInfoDto in cache, get name from it
                    NCubeInfoDto cubeInfo = (NCubeInfoDto) value;
                    name = cubeInfo.name;
                }
                names.add(name);
            }
        }
        return new CaseInsensitiveSet<>(names);
    }

    /**
     * Fetch an n-cube by name from the given ApplicationID.  If no n-cubes
     * are loaded, then a loadCubes() call is performed and then the
     * internal cache is checked again.  If the cube is not found, null is
     * returned.
     */
    public static NCube getCube(ApplicationID appId, String name)
    {
        validateAppId(appId);
        NCube.validateCubeName(name);
        Map<String, Object> cubes = getCacheForApp(appId);
        final String key = name.toLowerCase();

        if (cubes.containsKey(key))
        {   // pull from cache
            return ensureLoaded(cubes.get(key));
        }

        // Deep load the requested cube
        getCubeRecordsFromDatabase(appId, name);


        if (cubes.containsKey(key))
        {
            return ensureLoaded(cubes.get(key));
        }

        resolveClassPath(appId);

        return null;
    }

    private static NCube ensureLoaded(Object value)
    {
        if (value instanceof NCube)
        {
            return (NCube)value;
        }
        else if (value instanceof NCubeInfoDto)
        {   // Lazy load cube (make sure to apply any advices to it)
            NCube cube = nCubePersister.loadCube((NCubeInfoDto) value);
            applyAdvices(cube.getApplicationID(), cube);
            return cube;
        }
        else
        {
            throw new IllegalStateException("Failed to retrieve cube from cache, value: " + value);
        }
    }

    /**
     * Testing API (Cache validation)
     */
    static boolean isCubeCached(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        Map<String, Object> ncubes = getCacheForApp(appId);
        return ncubes.containsKey(cubeName.toLowerCase());
    }

    /**
     * Add to the classloader's classpath for the given ApplicationID.
s    */
//    private static void addBaseResourceUrls(ApplicationID appId, List<String> urls)
//    {
//        validateAppId(appId);
//        GroovyClassLoader urlClassLoader = urlClassLoaders.get(appId);
//
//        if (urlClassLoader == null)
//        {
//            LOG.debug("Creating ClassLoader, app: " + appId + ", urls: " + urls);
//            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
//            urlClassLoaders.put(appId, urlClassLoader);
//        }
//        else
//        {
//            LOG.debug("Adding resource URLs, app: " + appId + ", urls: " + urls);
//        }
//
//        addUrlsToClassLoader(urls, urlClassLoader);
//    }

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

    /**
     * Fetch the classloader for the given ApplicationID.
     */
    public static URLClassLoader getUrlClassLoader(ApplicationID appId)
    {
        validateAppId(appId);
        return urlClassLoaders.get(appId);
    }

    /**
     * Add a cube to the internal cache of available cubes.
     * @param ncube NCube to add to the list.
     */
    public static void addCube(ApplicationID appId, NCube ncube)
    {
        validateAppId(appId);
        validateCube(ncube);

        // Add the cube to the cache for this ApplicationID
        getCacheForApp(appId).put(ncube.name.toLowerCase(), ncube);

        // Apply any matching advices to it
        applyAdvices(appId, ncube);
    }

    private static void applyAdvices(ApplicationID appId, NCube ncube)
    {
        Map<String, Advice> appAdvices = advices.get(appId);

        if (MapUtilities.isEmpty(appAdvices))
        {
            return;
        }
        for (Map.Entry<String, Advice> entry : appAdvices.entrySet())
        {
            String regex = StringUtilities.wildcardToRegexString(entry.getKey());
            Advice advice = entry.getValue();
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

    /**
     * Fetch the Map of n-cubes for the given ApplicationID.  If no
     * cache yet exists, a new empty cache is added.
     */
    private static Map<String, Object> getCacheForApp(ApplicationID appId)
    {
        Map<String, Object> ncubes = ncubeCache.get(appId);

        if (ncubes == null)
        {
            synchronized (ncubeCache)
            {
                ncubes = ncubeCache.get(appId);
                if (ncubes == null)
                {
                    ncubes = new ConcurrentHashMap<>();
                    ncubeCache.put(appId, ncubes);
                }
            }
        }
        return ncubes;
    }

    public static void clearCache(ApplicationID appId)
    {
        validateAppId(appId);

        // Clear App cache
        Map<String, Object> appCache = ncubeCache.get(appId);
        if (appCache != null)
        {
            appCache.clear();
        }

        GroovyBase.clearCache(appId);
        NCubeGroovyController.clearCache(appId);

        // Clear Advice cache
        Map<String, Advice> adviceCache = advices.get(appId);
        if (adviceCache != null)
        {
            adviceCache.clear();
        }

        // Clear ClassLoader cache
        GroovyClassLoader classLoader = urlClassLoaders.get(appId);
        if (classLoader != null)
        {
            classLoader.clearCache();
        }
    }

    static void clearCache()
    {
        for (Map.Entry<ApplicationID, Map<String, Object>> applicationIDMapEntry : ncubeCache.entrySet())
        {
            applicationIDMapEntry.getValue().clear();
            GroovyBase.clearCache(applicationIDMapEntry.getKey());
            NCubeGroovyController.clearCache(applicationIDMapEntry.getKey());
        }

        for (Map.Entry<ApplicationID, Map<String, Advice>> applicationIDMapEntry : advices.entrySet())
        {
            applicationIDMapEntry.getValue().clear();
        }

        for (Map.Entry<ApplicationID, GroovyClassLoader> applicationIDGroovyClassLoaderEntry : urlClassLoaders.entrySet())
        {
            applicationIDGroovyClassLoaderEntry.getValue().clearCache();
        }
    }

    /**
     * Associate Advice to all n-cubes that match the passed in regular expression.
     */
    public static void addAdvice(ApplicationID appId, String wildcard, Advice advice)
    {
        validateAppId(appId);
        Map<String, Advice> current = advices.get(appId);
        if (current == null)
        {
            synchronized (advices)
            {
                current = new ConcurrentHashMap<>();
                current.put(wildcard, advice);
                advices.put(appId, current);
            }
        }

        current.put(wildcard, advice);

        // Apply newly added advice to any fully loaded (hydrated) cubes.
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Map<String, Object> cubes = getCacheForApp(appId);

        for (Object value : cubes.values())
        {
            if (value instanceof NCube)
            {   // apply advice to hydrated ncubes
                NCube ncube = (NCube) value;
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

    /**
     * Validate the passed in testData
     */
    public static void validateTestData(String testData)
    {
    }

    /**
     * See if the given n-cube exists for the given ApplicationID.  This
     * checks the persistent storage.
     * @return true if the n-cube exists, false otherwise.
     */
    public static boolean doesCubeExist(ApplicationID appId, String name)
    {
        validateAppId(appId);
        NCube.validateCubeName(name);
        return nCubePersister.doesCubeExist(appId, name);
    }

    /**
     * Retrieve all cube names that are deeply referenced by ApplicationID + n-cube name.
     */
    public static void getReferencedCubeNames(ApplicationID appId, String name, Set<String> refs)
    {
        if (refs == null)
        {
            throw new IllegalArgumentException("Could not get referenced cube names, null passed in for Set to hold referenced n-cube names, app: " + appId + ", n-cube: " + name);
        }
        validateAppId(appId);
        NCube.validateCubeName(name);
        NCube ncube = getCube(appId, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not get referenced cube names, n-cube: " + name + " does not exist in app: " + appId);
        }
        Set<String> subCubeList = ncube.getReferencedCubeNames();
        refs.addAll(subCubeList);

        for (String cubeName : subCubeList)
        {
            if (!refs.contains(cubeName))
            {
                getReferencedCubeNames(appId, cubeName, refs);
            }
        }
    }

    /**
     * Get Object[] of n-cube record DTOs for the given ApplicationID, filtered by the pattern.  If using
     * JDBC, it will be used with a LIKE clause.  For Mongo...TBD.
     * For any cube record loaded, for which there is no entry in the app's cube cache, an entry
     * is added mapping the cube name to the cube record (NCubeInfoDto).  This will be replaced
     * by an NCube if more than the name is required.
     */
    public static Object[] getCubeRecordsFromDatabase(ApplicationID appId, String pattern)
    {
        validateAppId(appId);
        Object[] cubes = nCubePersister.getCubeRecords(appId, pattern);
        Map<String, Object> appCache = getCacheForApp(appId);

        for (Object cube : cubes)
        {
            NCubeInfoDto cubeInfo = (NCubeInfoDto) cube;
            String key = cubeInfo.name.toLowerCase();
            if (!appCache.containsKey(key))
            {
                appCache.put(key, cubeInfo);
            }
        }
        return cubes;
    }

    /**
     * Get Object[] of n-cube record DTOs for the given ApplicationID, filtered by the pattern.  If using
     * JDBC, it will be used with a LIKE clause.  For Mongo...TBD.
     * For any cube record loaded, for which there is no entry in the app's cube cache, an entry
     * is added mapping the cube name to the cube record (NCubeInfoDto).  This will be replaced
     * by an NCube if more than the name is required.
     */
    public static Object[] getDeletedCubesFromDatabase(ApplicationID appId, String pattern)
    {
        validateAppId(appId);
        Object[] cubes = nCubePersister.getDeletedCubeRecords(appId, pattern);
        return cubes;
    }

    public static void restoreCube(ApplicationID appId, String cubeName, String username)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        nCubePersister.restoreCube(appId, cubeName, username);
    }

    public static Object[] getRevisionHistory(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        Object[] revisions = nCubePersister.getRevisions(appId, cubeName);
        return revisions;
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public static Object[] getAppNames(String tenant)
    {
        return nCubePersister.getAppNames(tenant);
    }

    /**
     * Get all of the versions that exist for the given ApplicationID (tenant and app).
     * @return Object[] of String version numbers.
     */
    public static Object[] getAppVersions(ApplicationID appId)
    {
        validateAppId(appId);
        return nCubePersister.getAppVersions(appId);
    }

    /**
     * Duplicate the given n-cube specified by oldAppId and oldName to new ApplicationID and name,
     */
    public static void duplicate(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName, String username)
    {
        NCube.validateCubeName(newName);
        NCube ncube = getCube(oldAppId, oldName);
        NCube copy = ncube.duplicate(newName);
        nCubePersister.createCube(newAppId, copy, username);
        String json = nCubePersister.getTestData(oldAppId, oldName);
        nCubePersister.updateTestData(newAppId, newName, json);
        String notes = nCubePersister.getNotes(oldAppId, oldName);
        nCubePersister.updateNotes(newAppId, newName, notes);
        broadcast(newAppId);
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT ncubes can be updated.
     *
     * @param ncube      NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public static boolean updateCube(ApplicationID appId, NCube ncube, String username)
    {
        validateAppId(appId);
        validateCube(ncube);
        nCubePersister.updateCube(appId, ncube, username);
        Map<String, Object> appCache = getCacheForApp(appId);
        appCache.remove(ncube.getName().toLowerCase());
        broadcast(appId);
        return true;
    }

    /**
     * Perform release (SNAPSHOT to RELEASE) for the given ApplicationIDs n-cubes.
     */
    public static int releaseCubes(ApplicationID appId)
    {
        validateAppId(appId);
        int rows = nCubePersister.releaseCubes(appId);
        broadcast(appId);
        return rows;
    }

    public static int createSnapshotCubes(ApplicationID appId, String newVersion)
    {
        validateAppId(appId);
        ApplicationID.validateVersion(newVersion);

        if (appId.getVersion().equals(newVersion))
        {
            throw new IllegalArgumentException("New SNAPSHOT version " + newVersion + " cannot be the same as the RELEASE version.");
        }

        return nCubePersister.createSnapshotVersion(appId, newVersion);
    }

    public static void changeVersionValue(ApplicationID appId, String newVersion)
    {
        validateAppId(appId);
        ApplicationID.validateVersion(newVersion);
        nCubePersister.changeVersionValue(appId, newVersion);
        clearCache(appId);
        broadcast(appId);
    }

    public static boolean renameCube(ApplicationID appId, String oldName, String newName)
    {
        validateAppId(appId);
        NCube.validateCubeName(oldName);
        NCube.validateCubeName(newName);

        if (oldName.equalsIgnoreCase(newName))
        {
            throw new IllegalArgumentException("Could not rename, old name cannot be the same as the new name, name: " + oldName + ", app: " + appId);
        }

        NCube ncube = getCube(appId, oldName);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not rename due to name: " + oldName + " does not exist within app: " + appId);
        }

        boolean result = nCubePersister.renameCube(appId, ncube, newName);
        Map<String, Object> appCache = getCacheForApp(appId);
        appCache.remove(oldName.toLowerCase());
        appCache.put(newName.toLowerCase(), ncube);
        broadcast(appId);
        return result;
    }

    /**
     * Delete the named NCube from the database
     *
     * @param cubeName   NCube to be deleted
     */
    public static boolean deleteCube(ApplicationID appId, String cubeName, String username)
    {
        return deleteCube(appId, cubeName, false, username);
    }

    static boolean deleteCube(ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);

        if (nCubePersister.deleteCube(appId, cubeName, allowDelete, username))
        {
            Map<String, Object> appCache = getCacheForApp(appId);
            appCache.remove(cubeName.toLowerCase());
            broadcast(appId);
            return true;
        }
        return false;
    }

    /**
     * Update the notes associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateNotes(ApplicationID appId, String cubeName, String notes)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        nCubePersister.updateNotes(appId, cubeName, notes);
        return true;
    }

    /**
     * Get the notes associated to an NCube
     *
     * @return String notes.
     */
    public static String getNotes(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return nCubePersister.getNotes(appId, cubeName);
    }

    /**
     * Update the test data associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateTestData(ApplicationID appId, String cubeName, String testData)
    {
        validateAppId(appId);
        validateTestData(testData);
        NCube.validateCubeName(cubeName);
        return nCubePersister.updateTestData(appId, cubeName, testData);
    }

    public static String getTestData(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return nCubePersister.getTestData(appId, cubeName);
    }


    public static void resolveClassPath(ApplicationID appId) {
        //  Need a default run items in, will be empty unless sys.classpath is set for our appid.
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(appId);

        if (urlClassLoader != null) {
            return;
        }

        //urlclassloader didn't exist
        urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
        urlClassLoaders.put(appId, urlClassLoader);

        Map map = new HashMap();
        map.put("env", SystemUtilities.getExternalVariable("ENV_LEVEL"));
        map.put("username", System.getProperty("user.name"));

        NCube cube = getCube(appId, CLASSPATH_CUBE);

        if (cube == null) {
            LOG.debug("no sys.classpath exists for this application:  " + appId);
            return;
        }

        List<String> urls = (List<String>)cube.getCell(map);
        addUrlsToClassLoader(urls, urlClassLoader);
    }




    public static void createCube(ApplicationID appId, NCube ncube, String username)
    {
        validateCube(ncube);
        validateAppId(appId);
        nCubePersister.createCube(appId, ncube, username);
        ncube.setApplicationID(appId);
        addCube(appId, ncube);
        broadcast(appId);
    }

    // ----------------------------------------- Resource APIs ---------------------------------------------------------
    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = NCubeManager.class.getResource("/" + name);
        IOUtilities.transfer(new File(url.getFile()), out);
        return new String(out.toByteArray(), "UTF-8");
    }

    static NCube getNCubeFromResource(String name)
    {
        return getNCubeFromResource(ApplicationID.defaultAppId, name);
    }

    public static NCube getNCubeFromResource(ApplicationID id, String name)
    {
        try
        {
            String json = getResourceAsString(name);
            NCube ncube = ncubeFromJson(json);
            ncube.setApplicationID(id);
            addCube(id, ncube);
            resolveClassPath(id);
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
                addCube(nCube.getApplicationID(), nCube);
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
                ncube.setMetaProperty("sha1", ncube.sha1());
                return ncube;
            }
            catch (Exception e1)
            {
                throw e;
            }
        }
    }

    // ---------------------------------------- Validation APIs --------------------------------------------------------
    static void validateAppId(ApplicationID appId)
    {
        if (appId == null)
        {
            throw new IllegalArgumentException("ApplicationID cannot be null");
        }
        appId.validate();
    }

    static void validateCube(NCube cube)
    {
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null");
        }
        NCube.validateCubeName(cube.getName());
    }

    // ---------------------- Broadcast APIs for notifying other services in cluster of cache changes ------------------
    static void broadcast(ApplicationID appId)
    {
        LOG.debug("Clear cache: " + appId);
        // Write to 'system' tenant, 'NCE' app, version '0.0.0', SNAPSHOT, cube: sys.cache
        // Separate thread reads from this table every 1 second, for new commands, for
        // example, clear cache
    }
}
