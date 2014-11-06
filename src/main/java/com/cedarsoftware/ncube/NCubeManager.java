package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnClassLoader;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.StringUtilities;
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
import java.util.Collections;
import java.util.Comparator;
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
    private static final Map<ApplicationID, Map<String, NCube>> ncubeCache = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, Map<String, Advice>> advices = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, GroovyClassLoader> urlClassLoaders = new ConcurrentHashMap<>();
    private static NCubePersister nCubePersister;
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);

    private static final String CLASSPATH_CUBE = "sys.classpath";

    static
    {
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        urlClassLoaders.put(appId, new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true));
    }

    /**
     * Store the Persister to be used with the NCubeManager API (Dependency Injection API)
     */
    public static void setNCubePersister(NCubePersister persister)
    {
        nCubePersister = persister;
    }

    /**
     * Fetch all the n-cube names for the given ApplicationID.  This API
     * will attempt to loadCubes() if there are none loaded for the given
     * ApplicationID.
     * @return Set<String> n-cube names.  If an empty Set is returned,
     * then there are no persisted n-cubes for the passed in ApplicationID.
     */
    public static Set<String> getCubeNames(ApplicationID appId)
    {
        validateAppId(appId);
        Map<String, NCube> appCache = getCacheForApp(appId);
        Set<String> names = new TreeSet<>();
        if (appCache.isEmpty())
        {   // Get names quickly without hydrating n-cube JSON or Test JSON
            Object[] cubeInfos = nCubePersister.getNCubes(appId, "%");
            for (Object cubeInfo : cubeInfos)
            {
                NCubeInfoDto info = (NCubeInfoDto) cubeInfo;
                names.add(info.name);
            }
        }
        else
        {   // Get names quickly from cache
            for (NCube ncube : appCache.values())
            {
                names.add(ncube.name);
            }
        }
        return new CaseInsensitiveSet<>(names);
    }

    /**
     * Fetch an n-cube by name from the given ApplicationID.  If no n-cubes
     * are loaded, then a loadCubes() call is performed and then the
     * internal cache is checked again.
     */
    public static NCube getCube(ApplicationID appId, String name)
    {
        validateAppId(appId);
        NCube.validateCubeName(name);
        Map<String, NCube> cubes = getCacheForApp(appId);
        final String key = name.toLowerCase();
        if (cubes.containsKey(key))
        {
            return cubes.get(key);
        }
        loadCubes(appId);
        cubes = getCacheForApp(appId);
        return cubes.containsKey(key) ? cubes.get(key) : null;

    }

    /**
     * @return Set<NCube> of all NCubes for the given ApplicationID.
     * If no cubes are loaded, then it will load them first and then return
     * the list.
     */
    public static Set<NCube> getCubes(ApplicationID appId)
    {
        validateAppId(appId);
        Map<String, NCube> ncubes = getCubesInternal(appId);
        List<NCube> cubes = new ArrayList();
        for (NCube ncube : ncubes.values())
        {
            cubes.add(ncube);
        }
        Collections.sort(cubes, new Comparator<NCube>()
        {
            public int compare(NCube c1, NCube c2)
            {
                return c1.name.compareToIgnoreCase(c2.name);
            }
        });
        return new CaseInsensitiveSet<>(cubes);
    }


    /**
     * Testing API (Cache validation)
     */
    static boolean isCubeCached(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        Map<String, NCube> ncubes = getCacheForApp(appId);
        return ncubes.containsKey(cubeName.toLowerCase());
    }

    /**
     * @return Map<String, NCube> of all NCubes for the given ApplicationID.
     * If no cubes are loaded, then it will load them first and then return
     * the list.
     */
    static Map<String, NCube> getCubesInternal(ApplicationID appId)
    {
        Map<String, NCube> ncubes = getCacheForApp(appId);
        if (ncubes.isEmpty())
        {
            loadCubes(appId);
            ncubes = getCacheForApp(appId);
        }
        return ncubes;
    }

    /**
     * Add to the classloader's classpath for the given ApplicationID.
s    */
    public static void addBaseResourceUrls(ApplicationID appId, List<String> urls)
    {
        validateAppId(appId);
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(appId);

        if (urlClassLoader == null)
        {
            LOG.debug("Creating ClassLoader, app: " + appId + ", urls: " + urls);
            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
            urlClassLoaders.put(appId, urlClassLoader);
        }
        else
        {
            LOG.debug("Adding resource URLs, app: " + appId + ", urls: " + urls);
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

        Map<String, NCube> appCache = getCacheForApp(appId);
        appCache.put(ncube.name.toLowerCase(), ncube);
        Map<String, Advice> appAdvices = advices.get(appId);

        if (appAdvices != null && !appAdvices.isEmpty())
        {
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
    }

    /**
     * Fetch the Map of n-cubes for the given ApplicationID.  If no
     * cache yet exists, a new empty cache is added.
     */
    private static Map<String, NCube> getCacheForApp(ApplicationID appId)
    {
        Map<String, NCube> ncubes = ncubeCache.get(appId);

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
        Map<String, NCube> appCache = ncubeCache.get(appId);
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
        for (Map.Entry<ApplicationID, Map<String, NCube>> applicationIDMapEntry : ncubeCache.entrySet())
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
                advices.put(appId, current);
            }
        }

        current.put(wildcard, advice);
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Set<NCube> cubes = getCubes(appId);

        for (NCube ncube : cubes)
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
        validateAppId(appId);
        NCube.validateCubeName(name);
        NCube ncube = getCube(appId, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("n-cube: " + name + " does not exist in app: " + appId);
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
    public static Object[] getNCubes(ApplicationID appId, String pattern)
    {
        validateAppId(appId);
        return nCubePersister.getNCubes(appId, pattern);
    }

    /**
     * Duplicate the given n-cube specified by oldAppId and oldName to new ApplicationID and name,
     */
    public static void duplicate(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName)
    {
        NCube.validateCubeName(newName);
        NCube ncube = getCube(oldAppId, oldName);
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
    public static Object[] getAppNames(String account)
    {
        return nCubePersister.getAppNames(account);
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
        clearCache(appId);
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
        Map<String, NCube> appCache = getCacheForApp(appId);
        appCache.remove(oldName.toLowerCase());
        appCache.put(newName.toLowerCase(), ncube);
        return result;
    }

    /**
     * Delete the named NCube from the database
     *
     * @param cubeName   NCube to be deleted
     */
    public static boolean deleteCube(ApplicationID appId, String cubeName)
    {
        return deleteCube(appId, cubeName, false);
    }

    static boolean deleteCube(ApplicationID appId, String cubeName, boolean allowDelete)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);

        if (nCubePersister.deleteCube(appId, cubeName, allowDelete))
        {
            Map<String, NCube> appCache = getCacheForApp(appId);
            appCache.remove(cubeName.toLowerCase());
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
    static void loadCubes(ApplicationID appId)
    {
        List<NCube> ncubes = nCubePersister.loadCubes(appId);

        for (NCube ncube : ncubes)
        {
            addCube(appId, ncube);
        }

        //resolveClassPath(appId);
    }

    /*
    public static void resolveClassPath(ApplicationID appId) {
        //  Need a default run items in, will be empty unless sys.classpath is set for our appid.
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(appId);

        if (urlClassLoader == null)
        {
            //urlclassloader didn't exist
            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
            urlClassLoaders.put(appId, urlClassLoader);
        }
        else if (urlClassLoader.getURLs().length < 1)
        {
            //  urlclassloader exists and has been setup already.
            return;
        }

        Map map = new HashMap();
        map.put("env", SystemUtilities.getExternalVariable("ENV_LEVEL"));
        map.put("useranem", System.getProperty("user.name"));

        NCube cube = getCube(zeroAppId, CLASSPATH_CUBE);

        if (cube == null) {
            LOG.debug("sys.classpath cube is not setup for this application:  " + appId);
            return;
        }

        String url = (String)cube.getCell(map);

        if (StringUtilities.isEmpty(url)) {
            LOG.debug("sys.classpath cube is not setup for this application:  " + appId);
            return;
        }

        StringTokenizer token = new StringTokenizer(url, ";,| ");
        List<String> urls = new ArrayList();
        while (token.hasMoreTokens()) {
            String elem = token.nextToken();
            try
            {
                URL u = new URL(elem);
                urls.add(elem);
            } catch (Exception e) {
                //TODO:  Do we need to test each url to make sure it is valid?  They could be invalid
                //TODO:  even though a subpath resouce could still be valid.
                LOG.debug("Invalid url in sys.Classpath cube  " + appId);
            }
        }
        addUrlsToClassLoader(urls, urlClassLoader);
    }
    */

    public static void createCube(ApplicationID appId, NCube ncube)
    {
        validateCube(ncube);
        validateAppId(appId);
        nCubePersister.createCube(appId, ncube);
        ncube.setApplicationID(appId);
        addCube(appId, ncube);
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
            addCube(ncube.getApplicationID(), ncube);
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
}
