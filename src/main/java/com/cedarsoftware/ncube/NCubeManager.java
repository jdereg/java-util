package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnClassLoader;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Map<String, NCube> cubeList = new ConcurrentHashMap<>();
    private static final Log LOG = LogFactory.getLog(NCubeManager.class);
    private static final Map<String, Map<String, Advice>> advices = new ConcurrentHashMap<>();
    private static final Map<String, GroovyClassLoader> urlClassLoaders = new ConcurrentHashMap<>();
    private static NCubePersister nCubePersister;

    static
    {
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        urlClassLoaders.put(appId.getAppStr(""), new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true));
    }

    public static void setNCubePersister(NCubePersister nCubePersister)
    {
        NCubeManager.nCubePersister = nCubePersister;
    }

    public static Set<String> getCubeNames(ApplicationID appId)
    {
        Set<String> result = new TreeSet<>();
        Collection<NCube> cubes = cubeList.values();

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
     * @param name String name of an NCube.
     * @return NCube instance with the given name.  Please note
     * that the cube must be loaded first before calling this.
     */
    public static NCube getCube(String name, ApplicationID appId)
    {
        return cubeList.get(appId.getAppStr(name));
    }

    public static void addBaseResourceUrls(List<String> urls, String appStr)
    {
        GroovyClassLoader urlClassLoader = urlClassLoaders.get(appStr);

        if (urlClassLoader == null)
        {
            LOG.info("Creating ClassLoader, n-cube version: " + appStr + ", urls: " + urls);
            urlClassLoader = new CdnClassLoader(NCubeManager.class.getClassLoader(), true, true);
            urlClassLoaders.put(appStr, urlClassLoader);
        }
        else
        {
            LOG.info("Adding resource URLs, n-cube version: " + appStr + ", urls: " + urls);
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

    public static URLClassLoader getUrlClassLoader(String appStr)
    {
        return urlClassLoaders.get(appStr);
    }

    /**
     * Add a cube to the internal map of available cubes.
     *
     * @param ncube NCube to add to the list.
     */
    public static void addCube(NCube ncube, ApplicationID appId)
    {
        //todo lose synch
        synchronized (cubeList)
        {
            cubeList.put(appId.getAppStr(ncube.getName()), ncube);

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
                ((GroovyClassLoader) classLoader).clearCache(); // free up Class cache
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

    public static void validateTestData(String testData)
    {
    }

    public static boolean doesCubeExist(ApplicationID id, String name)
    {
        return doesCubeExist(nCubePersister, id, name);
    }

    /**
     * Check for existence of a cube in the database
     *
     * @return true if one is present, otherwise false.
     */
    static boolean doesCubeExist(NCubePersister persister, ApplicationID id, String name)
    {
        validateId(id);
        NCube.validateCubeName(name);
        return persister.doesCubeExist(id, name);
    }

    /**
     * Retrieve all cube names that are deeply referenced by ApplicationID + n-cube name.
     */
    public static void getReferencedCubeNames(ApplicationID appId, String name, Set<String> refs)
    {
        if (refs == null)
        {
            throw new IllegalArgumentException("null passed in for Set to hold referenced n-cube names");
        }
        NCube ncube = getCube(name, appId);
        if (ncube == null)
        {
            throw new IllegalArgumentException("n-cube: " + name + " is not loaded.");
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
    public static Object[] getNCubes(ApplicationID id, String sqlLike)
    {
        validateId(id);
        return nCubePersister.getNCubes(id, sqlLike);
    }

    /**
     * Duplicate the given n-cube specified by oldId and oldName to new ApplicationID and name,
     */
    public static void duplicate(ApplicationID oldId, ApplicationID newId, String oldName, String newName)
    {
        duplicate(nCubePersister, oldId, newId, oldName, newName);
    }

    /**
     * Duplicate the specified n-cube, given it the new name, and the same app, version, status as the source n-cube.
     */
    static void duplicate(NCubePersister persister, ApplicationID oldId, ApplicationID newId, String oldName, String newName)
    {
        NCube ncube = getCube(oldName, oldId);
        NCube copy = ncube.duplicate(newName);

        persister.createCube(newId, copy);
        String json = persister.getTestData(oldId, oldName);
        persister.updateTestData(newId, newName, json);
        String notes = persister.getNotes(oldId, oldName);
        persister.updateNotes(newId, newName, notes);
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public static Object[] getAppNames()
    {
        return nCubePersister.getAppNames();
    }

    public static Object[] getAppVersions(ApplicationID id)
    {
        return getAppVersions(nCubePersister, id);
    }

    public static void validateId(ApplicationID id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("ApplicationId cannot be null");
        }
    }

    public static void validateCube(NCube cube)
    {
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null");
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    static Object[] getAppVersions(NCubePersister persister, ApplicationID id)
    {
        validateId(id);
        return persister.getAppVersions(id);
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT ncubes can be updated.
     *
     * @param ncube      NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public static boolean updateCube(ApplicationID id, NCube ncube)
    {
        validateId(id);
        validateCube(ncube);

        synchronized (cubeList)
        {
            nCubePersister.updateCube(id, ncube);
        }

        return true;
    }

    /**
     * Perform release (SNAPSHOT to RELEASE) for the given ApplicationIDs n-cubes.
     */
    public static int releaseCubes(ApplicationID id)
    {
        validateId(id);

        synchronized (cubeList)
        {
            return nCubePersister.releaseCubes(id);
        }
    }

    public static int createSnapshotCubes(ApplicationID id, String newVersion)
    {
        ApplicationID.validateVersion(newVersion);
        validateId(id);

        if (id.getVersion().equals(newVersion))
        {
            throw new IllegalArgumentException("New SNAPSHOT version " + newVersion + " cannot be the same as the RELEASE version.");
        }

        synchronized (cubeList)
        {
            return nCubePersister.createSnapshotVersion(id, newVersion);
        }
    }

    public static void changeVersionValue(ApplicationID id, String newVersion) {
        validateId(id);
        ApplicationID.validateVersion(newVersion);

        synchronized (cubeList)
        {
            nCubePersister.changeVersionValue(id, newVersion);

            //  TODO: we should remove old versioned cubes from the cache here since this is not additive
            ApplicationID newId = id.createNewSnapshotId(newVersion);
            loadCubes(newId);
        }
    }

    public static boolean renameCube(ApplicationID id, String oldName, String newName)
    {
        validateId(id);
        ApplicationID.validateVersion(id.getVersion());
        NCube.validateCubeName(oldName);
        NCube.validateCubeName(newName);

        id.validateIsSnapshot();

        if (oldName.equalsIgnoreCase(newName))
        {
            throw new IllegalArgumentException("Old name cannot be the same as the new name, name: " + oldName);
        }

        //  assumes the cube is already loaded
        NCube ncube = getCube(oldName, id);

        synchronized (cubeList)
        {

            boolean result = nCubePersister.renameCube(id, ncube, newName);

            // Any user of these old IDs will get the default (null) account
            cubeList.remove(id.getAppStr(oldName));
            cubeList.put(id.getAppStr(newName), ncube);
            return result;
        }
    }

    /**
     * Delete the named NCube from the database
     *
     * @param cubeName   NCube to be deleted
     */
    public static boolean deleteCube(ApplicationID id, String cubeName)
    {
        validateId(id);
        NCube.validateCubeName(cubeName);

        synchronized (cubeList)
        {
            if (nCubePersister.deleteCube(id, cubeName, false)) {
                // Any user of these old APIs will get the default (null) account
                cubeList.remove(id.getAppStr(cubeName));
                return true;
            }
        }
        return false;
    }

    static boolean deleteCube(ApplicationID id, String cubeName, boolean allowDelete)
    {
        validateId(id);
        NCube.validateCubeName(cubeName);

        synchronized (cubeList)
        {
            if (nCubePersister.deleteCube(id, cubeName, allowDelete)) {
                // Any user of these old APIs will get the default (null) account
                cubeList.remove(id.getAppStr(cubeName));
                return true;
            }
        }
        return false;
    }

    /**
     * Update the notes associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateNotes(ApplicationID id, String cubeName, String notes)
    {
        validateId(id);
        NCube.validateCubeName(cubeName);

        synchronized (cubeList)
        {
            nCubePersister.updateNotes(id, cubeName, notes);
        }
        return true;
    }

    /**
     * Get the notes associated to an NCube
     *
     * @return String notes.
     */
    public static String getNotes(ApplicationID id, String cubeName)
    {
        validateId(id);
        NCube.validateCubeName(cubeName);
        return nCubePersister.getNotes(id, cubeName);
    }

    /**
     * Update the test data associated to an NCube
     *
     * @return true if the update succeeds, false otherwise
     */
    public static boolean updateTestData(ApplicationID id, String cubeName, String testData)
    {
        validateId(id);
        validateTestData(testData);
        NCube.validateCubeName(cubeName);

        synchronized (cubeList)
        {
            return nCubePersister.updateTestData(id, cubeName, testData);
        }
    }

    public static String getTestData(ApplicationID id, String cubeName)
    {
        validateId(id);
        NCube.validateCubeName(cubeName);
        return nCubePersister.getTestData(id, cubeName);
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

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, and status.
     */
    public static void loadCubes(ApplicationID appId)
    {
        loadCubes(nCubePersister, appId);
    }

    /**
     * Load all n-cubes into NCubeManager's internal cache for a given app, version, status, and sysDate.
     */
    public static void loadCubes(NCubePersister persister, ApplicationID appId)
    {
        validateId(appId);

        List<NCube> ncubes = persister.loadCubes(appId);

        for (NCube ncube : ncubes)
        {
            addCube(ncube, appId);
        }
    }

    public static void createCube(ApplicationID id, NCube ncube)
    {
        createCube(nCubePersister, id, ncube);
    }

    /**
     * Persist the passed in NCube
     *
     * @param ncube      NCube to be persisted
     */
    static void createCube(NCubePersister persister, ApplicationID id, NCube ncube)
    {
        validateId(id);

        if (ncube == null)
        {
            throw new IllegalArgumentException("NCube cannot be null when creating a new n-cube");
        }

        NCube.validateCubeName(ncube.getName());

        synchronized (cubeList)
        {
            persister.createCube(id, ncube);
            ncube.setApplicationID(id);
            addCube(ncube, id);
        }
    }
}
