package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnClassLoader;
import com.cedarsoftware.util.IOUtilities;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
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
    private static final Map<String, NCube> ncubeCache = new ConcurrentHashMap<>();
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);
    private static final Map<String, Map<String, Advice>> advices = new ConcurrentHashMap<>();
    private static final Map<String, GroovyClassLoader> urlClassLoaders = new ConcurrentHashMap<>();
    private static NCubePersister nCubePersister;

    private static final String CLASSPATH_CUBE = "sys.classpath";

    static
    {
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        urlClassLoaders.put(appId.getAppStr(""), new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true));
    }

    /**
     * Store the Persister to be used with the NCubeManager API (Dependency Injection API)
     */
    public static void setNCubePersister(NCubePersister nCubePersister)
    {
        NCubeManager.nCubePersister = nCubePersister;
    }

    /**
     * Fetch all the n-cube names for the given ApplicationID.  This API
     * expects that loadCubes() has already been called for the ApplicationID.
     * @return Set<String> n-cube names.
     */
    public static Set<String> getCubeNames(ApplicationID appId)
    {
        validateAppId(appId);
        Set<String> result = new TreeSet<>();
        Collection<NCube> cubes = ncubeCache.values();

        for (NCube ncube : cubes)
        {
            if (appId.equals(ncube.getApplicationID()))
            {
                result.add(ncube.getName());
            }
        }
        return result;
    }

    /**
     * Fetch an n-cube by name from the given ApplicationID.  It is
     * expected that loadCubes() has already been called for the ApplicationID.
     */
    public static NCube getCube(String name, ApplicationID appId)
    {
        validateAppId(appId);
        return ncubeCache.get(appId.getAppStr(name));
    }

    /**
     * Add to the classloader's classpath for the given ApplicationID.
s    */
    public static void addBaseResourceUrls(List<String> urls, ApplicationID appId)
    {
        validateAppId(appId);
        final String cacheKey = appId.getAppStr("");
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(cacheKey);

        if (urlClassLoader == null)
        {
            LOG.info("Creating ClassLoader, app: " + cacheKey + ", urls: " + urls);
            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
            urlClassLoaders.put(cacheKey, urlClassLoader);
        }
        else
        {
            LOG.info("Adding resource URLs, app: " + cacheKey + ", urls: " + urls);
        }

        addUrlsToClassLoader(urls, urlClassLoader);
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

    /**
     * Fetch the classloader for the given ApplicationID.
     */
    public static URLClassLoader getUrlClassLoader(String appStr)
    {
        return urlClassLoaders.get(appStr);
    }

    /**
     * Add a cube to the internal cache of available cubes.
     * @param ncube NCube to add to the list.
     */
    public static void addCube(NCube ncube, ApplicationID appId)
    {
        validateAppId(appId);
        ncubeCache.put(appId.getAppStr(ncube.getName()), ncube);

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

    /**
     * @return Map<String, NCube> of all NCubes that are currently
     * loaded (cached) in memory.  A copy of the internal cache
     * is returned.
     */
    public static Map<String, NCube> getCachedNCubes(ApplicationID appId)
    {
        validateAppId(appId);
        // TODO: Add appId cacheKey to caches
        return new TreeMap<>(ncubeCache);
    }

    /**
     * Used for testing.
     */
    public static void clearCubeList(ApplicationID appId)
    {
        // TODO: Add appId as appropriate cache keys
        validateAppId(appId);
        ncubeCache.clear();
        GroovyBase.clearCache();
        NCubeGroovyController.clearCache();
        for (Map.Entry<String, GroovyClassLoader> entry : urlClassLoaders.entrySet())
        {
            URLClassLoader classLoader = entry.getValue();
            ((GroovyClassLoader) classLoader).clearCache(); // free up Class cache
        }
        advices.clear();
    }

    /**
     * Associate Advice to all n-cubes that match the passed in regular expression.
     */
    public static void addAdvice(String wildcard, Advice advice)
    {
        Map<String, Advice> current = advices.get(wildcard);
        if (current == null)
        {
            current = new LinkedHashMap<>();
            advices.put(wildcard, current);
        }

        current.put(advice.getName(), advice);
        String regex = StringUtilities.wildcardToRegexString(wildcard);

        for (NCube ncube : ncubeCache.values())
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
            throw new IllegalArgumentException("null passed in for Set to hold referenced n-cube names, app: " + appId + ", n-cube: " + name);
        }
        NCube ncube = getCube(name, appId);
        if (ncube == null)
        {
            throw new IllegalArgumentException("n-cube: " + name + " is not loaded, app: " + appId);
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
     * Get Object[] of n-cube names for the given ApplicationID, filtered by the sqlLike clause.
     */
    public static Object[] getNCubes(ApplicationID appId, String sqlLike)
    {
        validateAppId(appId);
        return nCubePersister.getNCubes(appId, sqlLike);
    }

    /**
     * Duplicate the given n-cube specified by oldAppId and oldName to new ApplicationID and name,
     */
    public static void duplicate(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName)
    {
        NCube ncube = getCube(oldName, oldAppId);
        NCube copy = ncube.duplicate(newName);
        nCubePersister.createCube(newAppId, copy);
        String json = nCubePersister.getTestData(oldAppId, oldName);
        nCubePersister.updateTestData(newAppId, newName, json);
        String notes = nCubePersister.getNotes(oldAppId, oldName);
        nCubePersister.updateNotes(newAppId, newName, notes);
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public static Object[] getAppNames()
    {
        return nCubePersister.getAppNames();
    }

    /**
     * Get all of the versions that exist for the given ApplicationID (account and app).
     * @return Object[] of String version numbers.
     */
    public static Object[] getAppVersions(ApplicationID appId)
    {
        validateAppId(appId);
        return nCubePersister.getAppVersions(appId);
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT ncubes can be updated.
     *
     * @param ncube      NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public static boolean updateCube(ApplicationID appId, NCube ncube)
    {
        validateAppId(appId);
        validateCube(ncube);
        nCubePersister.updateCube(appId, ncube);
        return true;
    }

    /**
     * Perform release (SNAPSHOT to RELEASE) for the given ApplicationIDs n-cubes.
     */
    public static int releaseCubes(ApplicationID appId)
    {
        validateAppId(appId);
        return nCubePersister.releaseCubes(appId);
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

        //  TODO: we should remove old versioned cubes from the cache here since this is not additive
        ApplicationID newId = appId.createNewSnapshotId(newVersion);
        loadCubes(newId);
    }

    public static boolean renameCube(ApplicationID appId, String oldName, String newName)
    {
        validateAppId(appId);
        NCube.validateCubeName(oldName);
        NCube.validateCubeName(newName);

        if (oldName.equalsIgnoreCase(newName))
        {
            throw new IllegalArgumentException("Old name cannot be the same as the new name, name: " + oldName + ", app: " + appId);
        }

        //  assumes the cube is already loaded
        NCube ncube = getCube(oldName, appId);

        boolean result = nCubePersister.renameCube(appId, ncube, newName);

        // Any user of these old IDs will get the default (null) account
        ncubeCache.remove(appId.getAppStr(oldName));
        ncubeCache.put(appId.getAppStr(newName), ncube);
        return result;
    }

    /**
     * Delete the named NCube from the database
     *
     * @param cubeName   NCube to be deleted
     */
    public static boolean deleteCube(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);

        if (nCubePersister.deleteCube(appId, cubeName, false))
        {
            // Any user of these old APIs will get the default (null) account
            ncubeCache.remove(appId.getAppStr(cubeName));
            return true;
        }
        return false;
    }

    static boolean deleteCube(ApplicationID id, String cubeName, boolean allowDelete)
    {
        validateAppId(id);
        NCube.validateCubeName(cubeName);

        if (nCubePersister.deleteCube(id, cubeName, allowDelete))
        {
            // Any user of these old APIs will get the default (null) account
            ncubeCache.remove(id.getAppStr(cubeName));
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

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, and status.
     */
    public static void loadCubes(ApplicationID appId)
    {
        validateAppId(appId);
        List<NCube> ncubes = nCubePersister.loadCubes(appId);

        for (NCube ncube : ncubes)
        {
            addCube(ncube, appId);
        }

        resolveClassPath(appId);
    }

    public static void resolveClassPath(ApplicationID appId) {

        Map map = new HashMap();
        map.put("env", SystemUtilities.getExternalVariable("ENV_LEVEL"));
        map.put("useranem", System.getProperty("user.name"));

        NCube cube = getCube(CLASSPATH_CUBE, appId);
        String url = (String)cube.getCell(map);

        if (StringUtilities.isEmpty(url)) {
            throw new IllegalStateException("sys.classpath cube is not setup for this application:  " + appId);
        }

        StringTokenizer token = new StringTokenizer(url, ";,| ");
        List<String> list = new ArrayList();
        while (token.hasMoreTokens()) {
            String elem = token.nextToken();
            try
            {
                URL u = new URL(elem);
                list.add(elem);
            } catch (Exception e) {
                //TODO:  do we want to throw an exception or just not add this url into the path?
                throw new IllegalArgumentException("Invalid url (" + elem + ") in sys.classpath:  " + appId);
            }
        }
        addBaseResourceUrls(list, appId);
    }

    public static void createCube(ApplicationID appId, NCube ncube)
    {
        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null when creating a new n-cube");
        }

        validateAppId(appId);
        NCube.validateCubeName(ncube.getName());
        nCubePersister.createCube(appId, ncube);
        ncube.setApplicationID(appId);
        addCube(ncube, appId);
    }

    // --------------------------------------- Resource APIs -----------------------------------------------------------
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
            addCube(ncube, ncube.getApplicationID());
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
                // account: null, app: null, version: "file"
                addCube(nCube, nCube.getApplicationID());
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
    }
}
