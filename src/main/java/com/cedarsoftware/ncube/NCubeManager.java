package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.BranchMergeException;
import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.MapUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import groovy.lang.GroovyClassLoader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import ncube.grv.method.NCubeGroovyController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class NCubeManager
{
    private static final String SYS_BOOTSTRAP = "sys.bootstrap";
    private static final String CLASSPATH_CUBE = "sys.classpath";
    private static final Map<ApplicationID, Map<String, Object>> ncubeCache = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, Map<String, Advice>> advices = new ConcurrentHashMap<>();
    private static final Map<ApplicationID, GroovyClassLoader> localClassLoaders = new ConcurrentHashMap<>();
    public static final String NCUBE_PARAMS = "NCUBE_PARAMS";
    private static NCubePersister nCubePersister;
    private static final Logger LOG = LogManager.getLogger(NCubeManager.class);

    // not private in case we want to tweak things for testing.
    private static volatile Map<String, Object> systemParams = null;

    /**
     * Store the Persister to be used with the NCubeManager API (Dependency Injection API)
     */
    public static void setNCubePersister(NCubePersister persister)
    {
        nCubePersister = persister;
    }

    public static NCubePersister getPersister()
    {
        if (nCubePersister == null)
        {
            throw new IllegalStateException("Persister not set into NCubeManager.");
        }
        return nCubePersister;
    }

    public static Map<String, Object> getSystemParams()
    {
        final Map<String, Object> params = systemParams;

        if (params != null)
        {
            return params;
        }

        synchronized(NCubeManager.class)
        {
            if(systemParams == null)
            {
                String jsonParams = SystemUtilities.getExternalVariable(NCUBE_PARAMS);

                systemParams = new ConcurrentHashMap();

                if (StringUtilities.hasContent(jsonParams))
                {
                    try
                    {
                        systemParams = new ConcurrentHashMap(JsonReader.jsonToMaps(jsonParams));
                    }
                    catch (Exception e)
                    {
                        LOG.warn("Parsing of NCUBE_PARAMS failed.  " + jsonParams);
                    }

                }
            }
        }
        return systemParams;
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
        Object[] cubeInfos = getCubeRecordsFromDatabase(appId, "", true);
        Set<String> names = new TreeSet<>();

        for (Object cubeInfo : cubeInfos)
        {
            NCubeInfoDto info = (NCubeInfoDto) cubeInfo;
            names.add(info.name);
        }

        if (names.isEmpty())
        {   // Support tests that load cubes from JSON files...
            // can only be in there as ncubes, not ncubeDtoInfo
            for (Object value : getCacheForApp(appId).values())
            {
                NCube cube = (NCube) value;
                names.add(cube.getName());
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
        final String lowerCubeName = name.toLowerCase();

        if (cubes.containsKey(lowerCubeName))
        {   // pull from cache
            return ensureLoaded(cubes.get(lowerCubeName));
        }

        // now even items with metaProperties(cache = 'false') can be retrieved
        // and normal app processing doesn't do two queries anymore.
        // used to do getCubeInfoRecords() -> dto
        // and then dto -> loadCube(id)
        NCube ncube = getPersister().loadCube(appId, name);
        if (ncube == null) {
            return null;
        }
        return prepareCube(ncube);
    }

    static NCube ensureLoaded(Object value)
    {
        if (value instanceof NCube)
        {
            return (NCube)value;
        }

        if (value instanceof NCubeInfoDto)
        {   // Lazy load cube (make sure to apply any advices to it)
            NCubeInfoDto dto = (NCubeInfoDto) value;
            return prepareCube(getPersister().loadCube(dto.id));
        }

        throw new IllegalStateException("Failed to retrieve cube from cache, value: " + value);
    }

    private static NCube prepareCube(NCube cube)
    {
        applyAdvices(cube.getApplicationID(), cube);
        String cubeName = cube.getName().toLowerCase();
        if (!cube.getMetaProperties().containsKey("cache") || Boolean.TRUE.equals(cube.getMetaProperty("cache")))
        {   // Allow cubes to not be cached by specified 'cache':false as a cube meta-property.
            getCacheForApp(cube.getApplicationID()).put(cubeName, cube);
        }
        return cube;
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
     * Fetch the classloader for the given ApplicationID.
     */
    static URLClassLoader getUrlClassLoader(ApplicationID appId, Map input)
    {
        NCube cpCube = getCube(appId, CLASSPATH_CUBE);
        if (cpCube == null)
        {   // No sys.classpath cube exists, just create regular GroovyClassLoader with no URLs set into it.
            // Scope the GroovyClassLoader per ApplicationID
            return getLocalClassloader(appId);
        }

        final String envLevel = SystemUtilities.getExternalVariable("ENV_LEVEL");
        if (!input.containsKey("env") && StringUtilities.hasContent(envLevel))
        {   // Add in the 'ENV_LEVEL" environment variable when looking up sys.* cubes,
            // if there was not already an entry for it.
            input.put("env", envLevel);
        }
        if (!input.containsKey("username"))
        {   // same as ENV_LEVEL, add it in if not already there.
            input.put("username", System.getProperty("user.name"));
        }
        Object urlCpLoader = cpCube.getCell(input);

        if (urlCpLoader instanceof URLClassLoader)
        {
            return (URLClassLoader)urlCpLoader;
        }

        throw new IllegalStateException("If the sys.classpath cube exists it must return a URLClassLoader.");
    }

    static URLClassLoader getLocalClassloader(ApplicationID appId) {
        GroovyClassLoader gcl = localClassLoaders.get(appId);
        if (gcl == null)
        {
            synchronized (appId.cacheKey().intern())
            {
                gcl = localClassLoaders.get(appId);
                if (gcl != null)
                {
                    return gcl;
                }
                LOG.debug("No sys.classpath exists for this application: " + appId + ". No URL (external) resources will be loadable.");
                gcl = new GroovyClassLoader();
                localClassLoaders.put(appId, gcl);
            }
        }
        return gcl;
    }

    /**
     * Add a cube to the internal cache of available cubes.
     * @param ncube NCube to add to the list.
     */
    public static void addCube(ApplicationID appId, NCube ncube)
    {
        validateAppId(appId);
        validateCube(ncube);

        String cubeName = ncube.getName().toLowerCase();

        if (!cubeName.startsWith("tx."))
        {
            getCacheForApp(appId).put(cubeName, ncube);
        }

        // Apply any matching advices to it
        applyAdvices(appId, ncube);
    }

    /**
     * Fetch the Map of n-cubes for the given ApplicationID.  If no
     * cache yet exists, a new empty cache is added.
     */
    static Map<String, Object> getCacheForApp(ApplicationID appId)
    {
        Map<String, Object> ncubes = ncubeCache.get(appId);

        if (ncubes == null)
        {   // Avoid synchronization unless appId cache has not yet been created.
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

    public static void clearCacheForBranches(ApplicationID appId)
    {
        synchronized (ncubeCache)
        {
            List<ApplicationID> list = new ArrayList<>();

            for (ApplicationID id : ncubeCache.keySet())
            {
                if (id.cacheKey().startsWith(appId.branchAgnosticCacheKey()))
                {
                    list.add(id);
                }
            }

            for (ApplicationID appId1 : list)
            {
                clearCache(appId1);
            }
        }
    }

    /**
     * Clear the cube (and other internal caches) for a given ApplicationID.
     * This will remove all the n-cubes from memory, compiled Groovy code,
     * caches related to expressions, caches related to method support,
     * advice caches, and local classes loaders (used when no sys.classpath is
     * present).
     *
     * @param appId ApplicationID for which the cache is to be cleared.
     */
    public static void clearCache(ApplicationID appId)
    {
        synchronized (ncubeCache)
        {
            validateAppId(appId);

            Map<String, Object> appCache = getCacheForApp(appId);
            clearGroovyClassLoaderCache(appCache);

            appCache.clear();
            GroovyBase.clearCache(appId);
            NCubeGroovyController.clearCache(appId);

            // Clear Advice cache
            Map<String, Advice> adviceCache = advices.get(appId);
            if (adviceCache != null)
            {
                adviceCache.clear();
            }

            // Clear ClassLoader cache
            GroovyClassLoader classLoader = localClassLoaders.get(appId);
            if (classLoader != null)
            {
                classLoader.clearCache();
                localClassLoaders.remove(appId);
            }
        }
    }

    /**
     * This method will clear all caches for all ApplicationIDs.
     * Do not call it for anything other than test purposes.
     */
    public static void clearCache()
    {
        synchronized (ncubeCache)
        {
            List<ApplicationID> list = new ArrayList<>();

            for (ApplicationID appId : ncubeCache.keySet())
            {
                list.add(appId);
            }

            for (ApplicationID appId1 : list)
            {
                clearCache(appId1);
            }
        }
    }

    private static void clearGroovyClassLoaderCache(Map<String, Object> appCache)
    {
        Object cube = appCache.get(CLASSPATH_CUBE);
        if (cube instanceof NCube)
        {
            NCube cpCube = (NCube) cube;
            for (Object content : cpCube.cells.values())
            {
                if (content instanceof UrlCommandCell)
                {
                    ((UrlCommandCell)content).clearClassLoaderCache();
                }
            }
        }
    }

    /**
     * Associate Advice to all n-cubes that match the passed in regular expression.
     */
    public static void addAdvice(ApplicationID appId, String wildcard, Advice advice)
    {
        validateAppId(appId);
        Map<String, Advice> current = advices.get(appId);
        if (current == null) {
            synchronized (advices)
            {
                current = new ConcurrentHashMap<>();
                advices.put(appId, current);
            }
        }

        current.put(advice.getName() + '/' + wildcard, advice);

        // Apply newly added advice to any fully loaded (hydrated) cubes.
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Map<String, Object> cubes = getCacheForApp(appId);

        for (Object value : cubes.values())
        {
            if (value instanceof NCube)
            {   // apply advice to hydrated cubes
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
     * Apply existing advices loaded into the NCubeManager, to the passed in
     * n-cube.  This allows advices to be added first, and then let them be
     * applied 'on demand' as an n-cube is loaded later.
     * @param appId ApplicationID
     * @param ncube NCube to which all matching advices will be applied.
     */
    private static void applyAdvices(ApplicationID appId, NCube ncube)
    {
        final Map<String, Advice> appAdvices = advices.get(appId);

        if (MapUtilities.isEmpty(appAdvices))
        {
            return;
        }
        for (Map.Entry<String, Advice> entry : appAdvices.entrySet())
        {
            final Advice advice = entry.getValue();
            final String wildcard = entry.getKey().replace(advice.getName() + '/', "");
            final String regex = StringUtilities.wildcardToRegexString(wildcard);
            final Axis axis = ncube.getAxis("method");

            if (axis != null)
            {   // Controller methods
                for (Column column : axis.getColumnsWithoutDefault())
                {
                    final String method = column.getValue().toString();
                    final String classMethod = ncube.getName() + '.' + method + "()";
                    if (classMethod.matches(regex))
                    {
                        ncube.addAdvice(advice, method);
                    }
                }
            }
            else
            {   // Expressions
                final String classMethod = ncube.getName() + ".run()";
                if (classMethod.matches(regex))
                {
                    ncube.addAdvice(advice, "run");
                }
            }
        }
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
        return getPersister().doesCubeExist(appId, name);
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

        // TODO: Use explicit stack, NOT recursion

        for (String cubeName : subCubeList)
        {
            if (!refs.contains(cubeName))
            {
                refs.add(cubeName);
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
     * @param pattern A cube name pattern, using '*' for matches 0 or more characters and '?' for matches any
     * one (1) character.  This is universal whether using a SQL perister or Mongo persister.
     */
    public static Object[] getCubeRecordsFromDatabase(ApplicationID appId, String pattern)
    {
        return getCubeRecordsFromDatabase(appId, pattern, true);
    }

    /**
     * Get Object[] of n-cube record DTOs for the given ApplicationID, filtered by the pattern.  If using
     * JDBC, it will be used with a LIKE clause.  For Mongo...TBD.
     * For any cube record loaded, for which there is no entry in the app's cube cache, an entry
     * is added mapping the cube name to the cube record (NCubeInfoDto).  This will be replaced
     * by an NCube if more than the name is required.
     * @param pattern A cube name pattern, using '*' for matches 0 or more characters and '?' for matches any
     * one (1) character.  This is universal whether using a SQL perister or Mongo persister.
     */
    public static Object[] getCubeRecordsFromDatabase(ApplicationID appId, String pattern, boolean activeOnly)
    {
        validateAppId(appId);
        Object[] cubes = getPersister().getCubeRecords(appId, pattern, activeOnly);
        cacheCubes(appId, cubes);
        return cubes;
    }

    /**
     * Get Object[] of n-cube record DTOs for the given ApplicationID (branch only).  If using
     * For any cube record loaded, for which there is no entry in the app's cube cache, an entry
     * is added mapping the cube name to the cube record (NCubeInfoDto).  This will be replaced
     * by an NCube if more than the name is required.
     * one (1) character.  This is universal whether using a SQL perister or Mongo persister.
     */
    public static Object[] getBranchChangesFromDatabase(ApplicationID appId)
    {
        validateAppId(appId);
        if (appId.getBranch().equals(ApplicationID.HEAD)) {
            throw new IllegalArgumentException("Cannot get branch changes from HEAD");
        }

        ApplicationID headAppId = appId.asHead();
        Map<String, NCubeInfoDto> headMap = new TreeMap<>();
        Object[] branchList = getPersister().getChangedRecords(appId);
        Object[] headList = getPersister().getCubeRecords(headAppId, null, false);
        List<NCubeInfoDto> list = new ArrayList<>();

        //  build map of head objects for reference.
        for (Object item : headList)
        {
            NCubeInfoDto info = (NCubeInfoDto) item;
            headMap.put(info.name, info);
        }

        for (Object dto : branchList)
        {
            NCubeInfoDto info = (NCubeInfoDto)dto;

            long revision = Long.parseLong(info.revision);
            NCubeInfoDto head = headMap.get(info.name);

            //  we created this guy locally
            if (info.headSha1 == null)
            {
                if (head == null)
                {
                    //  only add if not deleted.
                    if (revision >= 0)
                    {
                        info.changeType = ChangeType.CREATED.getCode();
                        list.add(info);
                    }
                }
                else
                {
                    // someone added this one to the head already
                    info.changeType = ChangeType.CONFLICT.getCode();
                    list.add(info);
                }
            }
            else if (head != null)
            {
                if (StringUtilities.equalsIgnoreCase(info.headSha1, head.sha1))
                {
                    if (StringUtilities.equalsIgnoreCase(info.sha1, info.headSha1))
                    {
                        // only net change could be revision deleted or restored.  check head.
                        long headRev = Long.parseLong(head.revision);

                        if (headRev < 0 != revision < 0)
                        {
                            if (revision < 0)
                            {
                                info.changeType = ChangeType.DELETED.getCode();
                            }
                            else
                            {
                                info.changeType = ChangeType.RESTORED.getCode();
                            }

                            list.add(info);
                        }
                    }
                    else
                    {
                        info.changeType = ChangeType.UPDATED.getCode();
                        list.add(info);
                    }
                }
                else
                {
                    info.changeType = ChangeType.CONFLICT.getCode();
                    list.add(info);
                }
            }
        }

        Object[] cubes = list.toArray();
        cacheCubes(appId, cubes);
        return cubes;
    }

    private static void cacheCubes(ApplicationID appId, Object[] cubes)
    {
        Map<String, Object> appCache = getCacheForApp(appId);

        for (Object cube : cubes)
        {
            NCubeInfoDto cubeInfo = (NCubeInfoDto) cube;
            String key = cubeInfo.name.toLowerCase();

            if (!cubeInfo.revision.startsWith("-"))
            {
                if (!appCache.containsKey(key))
                {
                    appCache.put(key, cubeInfo);
                }
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
    public static Object[] getDeletedCubesFromDatabase(ApplicationID appId, String pattern)
    {
        validateAppId(appId);
        Object[] cubes = getPersister().getDeletedCubeRecords(appId, pattern);
        return cubes;
    }

    public static void restoreCube(ApplicationID appId, Object[] cubeNames, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();

        if (appId.isRelease())
        {
            throw new IllegalArgumentException(ReleaseStatus.RELEASE + " cubes cannot be restored, app: " + appId);
        }

        if (ArrayUtilities.isEmpty(cubeNames))
        {
            throw new IllegalArgumentException("Empty array of cube names passed to be restored.");
        }

        for (Object cubeName : cubeNames)
        {
            if ((cubeName instanceof String))
            {
                NCube.validateCubeName((String) cubeName);
                getPersister().restoreCube(appId, (String) cubeName, username);
            }
            else
            {
                throw new IllegalArgumentException("Non string name given for cube to restore: " + cubeName);
            }
        }
    }

    public static Object[] getRevisionHistory(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        Object[] revisions = getPersister().getRevisions(appId, cubeName);
        return revisions;
    }

    /**
     * Return an array [] of Strings containing all unique App names for the given tenant.
     */
    public static Object[] getAppNames(String tenant, String status, String branch)
    {
        return getPersister().getAppNames(tenant, status, branch);
    }

    /**
     * Get all of the versions that exist for the given ApplicationID (tenant and app).
     * @return Object[] of String version numbers.
     */
    public static Object[] getAppVersions(String tenant, String app, String status, String branch)
    {
        ApplicationID.validateTenant(tenant);
        ApplicationID.validateApp(app);
        ApplicationID.validateBranch(branch);
        return getPersister().getAppVersions(tenant, app, status, branch);
    }

    /**
     * Duplicate the given n-cube specified by oldAppId and oldName to new ApplicationID and name,
     */
    public static void duplicate(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName, String username)
    {
        validateAppId(oldAppId);
        validateAppId(newAppId);

        newAppId.validateBranchIsNotHead();

        if (newAppId.isRelease())
        {
            throw new IllegalArgumentException("Cubes cannot be duplicated into a " + ReleaseStatus.RELEASE + " version, cube: " + newName + ", app: " + newAppId);
        }

        NCube.validateCubeName(oldName);
        NCube.validateCubeName(newName);

        if (oldName.equalsIgnoreCase(newName) && oldAppId.equals(newAppId))
        {
            throw new IllegalArgumentException("Could not duplicate, old name cannot be the same as the new name when oldAppId matches newAppId, name: " + oldName + ", app: " + oldAppId);
        }

        getPersister().duplicateCube(oldAppId, newAppId, oldName, newName, username);

        if (CLASSPATH_CUBE.equalsIgnoreCase(newName))
        {   // If another cube is renamed into sys.classpath,
            // then the entire class loader must be dropped (and then lazily rebuilt).
            clearCache(newAppId);
        }
        else
        {
            Map<String, Object> appCache = getCacheForApp(newAppId);
            appCache.remove(newName.toLowerCase());
        }


        broadcast(newAppId);
    }

    /**
     * Update the passed in NCube.  Only SNAPSHOT cubes can be updated.
     *
     * @param ncube      NCube to be updated.
     * @return boolean true on success, false otherwise
     */
    public static boolean updateCube(ApplicationID appId, NCube ncube, String username)
    {
        validateAppId(appId);
        validateCube(ncube);

        if (appId.isRelease())
        {
            throw new IllegalArgumentException(ReleaseStatus.RELEASE + " cubes cannot be updated, cube: " + ncube.getName() + ", app: " + appId);
        }

        appId.validateBranchIsNotHead();

        final String cubeName = ncube.getName();
        getPersister().updateCube(appId, ncube, username);

        if (CLASSPATH_CUBE.equalsIgnoreCase(cubeName))
        {   // If the sys.classpath cube is changed, then the entire class loader must be dropped.  It will be lazily rebuilt.
            clearCache(appId);
        }
        else
        {
            Map<String, Object> appCache = getCacheForApp(appId);
            appCache.remove(cubeName.toLowerCase());
        }

        broadcast(appId);
        return true;
    }

    /**
     * Create a branch off of a SNAPSHOT for the given ApplicationIDs n-cubes.
     */
    public static int createBranch(ApplicationID appId)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();
        int rows = getPersister().createBranch(appId);
        broadcast(appId);
        return rows;
    }

    public static boolean mergeOverwriteHeadCube(ApplicationID appId, String cubeName, String headSha1, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();

        boolean ret = getPersister().mergeOverwriteHeadCube(appId, cubeName, headSha1, username);

        Map<String, Object> appCache = getCacheForApp(appId.asHead());
        appCache.remove(cubeName.toLowerCase());

        return ret;
    }

    public static boolean mergeOverwriteBranchCube(ApplicationID appId, String cubeName, String branchSha1, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();

        boolean ret = getPersister().mergeOverwriteBranchCube(appId, cubeName, branchSha1, username);

        Map<String, Object> appCache = getCacheForApp(appId);
        appCache.remove(cubeName.toLowerCase());

        return ret;
    }

    /**
     * Commit the passed in changed cube records identified by NCubeInfoDtos.
     * @return array of NCubeInfoDtos that were committed.
     */
    public static Object[] commitBranch(ApplicationID appId, Object[] infoDtos, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();

        ApplicationID headAppId = appId.asHead();
        Map<String, NCubeInfoDto> headMap = new TreeMap<>();
        Object[] headInfo = getPersister().getCubeRecords(headAppId, null, false);

        //  build map of head objects for reference.
        for (Object cubeInfo : headInfo)
        {
            NCubeInfoDto info = (NCubeInfoDto) cubeInfo;
            headMap.put(info.name, info);
        }

        List<NCubeInfoDto> dtos = new ArrayList<>(infoDtos.length);
        Map<String, Map> errors = new LinkedHashMap<>();

        for (Object dto : infoDtos)
        {
            NCubeInfoDto info = (NCubeInfoDto)dto;

            // All changes go through here.
            if (info.isChanged())
            {
                NCubeInfoDto head = headMap.get(info.name);
                long infoRev = Long.parseLong(info.revision);

                if (info.headSha1 == null)
                {
                    if (head == null)
                    {
                        //  only add if not deleted.
                        if (infoRev >= 0)
                        {
                            info.changeType = ChangeType.CREATED.getCode();
                            dtos.add(info);
                        }
                    }
                    else if (info.sha1.equals(head.sha1))
                    {
                        // items changed, but shas match so identical
                        info.changeType = ChangeType.UPDATED.getCode();
                        dtos.add(info);
                    }
                    else
                    {
                        String message = "Conflict merging " + info.name + ". A cube with the same name was added to HEAD since your branch was created.";
                        NCube mergedCube = checkForConflicts(appId, errors, message, info, head, false);
                        if (mergedCube != null) {
                            getPersister().updateCube(appId, mergedCube, username);
                            Object[] updated = getPersister().getCubeRecords(appId, info.name, false);
                            dtos.add((NCubeInfoDto)updated[0]);
                        }
                    }
                }
                else if (head == null)
                {
                    String message = "Conflict merging " + info.name + ". The cube refers to a HEAD cube that does not exist.";
                    checkForConflicts(appId, errors, message, info, head, false);
                }
                else if (info.headSha1.equals(head.sha1))
                {
                    if (StringUtilities.equalsIgnoreCase(info.sha1, info.headSha1))
                    {
                        long headRev = Long.parseLong(head.revision);
                        if (infoRev < 0 != headRev < 0)
                        {
                            info.changeType = infoRev < 0 ? ChangeType.DELETED.getCode() : ChangeType.RESTORED.getCode();
                            dtos.add(info);
                        }
                    }
                    else
                    {
                        info.changeType = ChangeType.UPDATED.getCode();
                        dtos.add(info);
                    }
                }
                else if (info.sha1.equals(head.sha1))
                {
                    // items changed, but shas match so identical
                    info.changeType = ChangeType.UPDATED.getCode();
                    dtos.add(info);
                }
                else
                {
                    String message = "Conflict merging " + info.name + ". The cube has changed since your last update.";
                    NCube mergedCube = checkForConflicts(appId, errors, message, info, head, false);
                    if (mergedCube != null) {
                        getPersister().updateCube(appId, mergedCube, username);
                        Object[] updated = getPersister().getCubeRecords(appId, info.name, false);
                        dtos.add((NCubeInfoDto)updated[0]);
                    }
                }
            }
        }

        if (!errors.isEmpty())
        {
            throw new BranchMergeException(errors.size() + " merge conflict(s) committing branch.  Update your branch and retry commit.", errors);
        }

        Object[] values = getPersister().commitBranch(appId, dtos, username);
        clearCache(appId);
        clearCache(headAppId);
        broadcast(appId);
        return values;
    }

    private static NCube checkForConflicts(ApplicationID appId, Map errors, String message, NCubeInfoDto info, NCubeInfoDto head, boolean reverse)
    {
        Map map = new LinkedHashMap();
        map.put("message", message);
        map.put("sha1", info.sha1);
        map.put("headSha1", head != null ? head.sha1 : null);

        try
        {
            if (head != null)
            {
                NCube branchCube = getPersister().loadCube(info.id);
                NCube headCube = getPersister().loadCube(head.id);

                if (info.headSha1 != null)
                {
                    NCube baseCube = getPersister().loadCubeBySha1(appId, info.name, info.headSha1);

                    Map delta1 = baseCube.getCellChangeSet(branchCube);
                    Map delta2 = baseCube.getCellChangeSet(headCube);

                    if (NCube.areCellChangeSetsCompatible(delta1, delta2))
                    {
                        if (reverse) {
                            headCube.mergeCellChangeSet(delta1);
                            return headCube;
                        } else {
                            branchCube.mergeCellChangeSet(delta2);
                            return branchCube;
                        }
                    }
                }

                map.put("diff", branchCube.getDeltaDescription(headCube));
            }
            else
            {
                map.put("diff", null);
            }
        }
        catch (Exception e)
        {
            map.put("diff", e.getMessage());
        }
        errors.put(info.name, map);
        return null;
    }

    /**
     * Commit the passed in changed cube records identified by NCubeInfoDtos.
     * @return Map ['added': 10, 'deleted': 3, 'updated': 7]
     */
    public static int rollbackBranch(ApplicationID appId, Object[] infoDtos)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();
        int ret = getPersister().rollbackBranch(appId, infoDtos);
        clearCache(appId);
        return ret;
    }

    public static Object[] updateBranch(ApplicationID appId, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        appId.validateStatusIsNotRelease();

        ApplicationID headAppId = appId.asHead();
        Object[] records = getCubeRecordsFromDatabase(appId, null, false);
        Object[] headRecords = getCubeRecordsFromDatabase(headAppId, null, false);

        //  build map of head objects for reference.
        Map<String, NCubeInfoDto> recordMap = new LinkedHashMap<>();

        for (Object cubeInfo : records)
        {
            NCubeInfoDto info = (NCubeInfoDto) cubeInfo;
            recordMap.put(info.name, info);
        }

        List<NCubeInfoDto> updates = new ArrayList<>(records.length);

        Map<String, Map> conflicts = new LinkedHashMap<>();

        for (Object dto : headRecords)
        {
            NCubeInfoDto head = (NCubeInfoDto) dto;
            NCubeInfoDto info = recordMap.get(head.name);

            if (info == null)
            {
                updates.add(head);
                continue;
            }

            long infoRev = Long.parseLong(info.revision);
            long headRev = Long.parseLong(head.revision);

            if (!info.isChanged())
            {
                if (StringUtilities.equalsIgnoreCase(info.headSha1, head.sha1))
                {
                    if (infoRev < 0 != headRev < 0)
                    {
                        updates.add(head);
                    }
                }
                else
                {
                    updates.add(head);
                }
            }
            else if (!StringUtilities.equalsIgnoreCase(info.headSha1, head.sha1))
            {
                String message = "Cube was changed in HEAD";
                NCube cube = checkForConflicts(appId, conflicts, message, info, head, true);

                if (cube != null) {
                    getPersister().updateCube(headAppId, cube, username);
                    Object[] updated = getPersister().getCubeRecords(headAppId, info.name, false);
                    updates.add((NCubeInfoDto)updated[0]);
                }
            }
        }

        if (!conflicts.isEmpty())
        {
            throw new BranchMergeException("Conflict(s) updating branch", conflicts);
        }

        Object[] ret = getPersister().updateBranch(appId, updates, username);

        clearCache(appId);
        return ret;
    }

    /**
     * Perform release (SNAPSHOT to RELEASE) for the given ApplicationIDs n-cubes.
     */
    public static int releaseCubes(ApplicationID appId, String newSnapVer)
    {
        validateAppId(appId);
        ApplicationID.validateVersion(newSnapVer);
        int rows = getPersister().releaseCubes(appId, newSnapVer);
        clearCacheForBranches(appId);
        //TODO:  Does broadcast need to send all branches that have changed as a result of this?
        broadcast(appId);
        return rows;
    }

    public static void changeVersionValue(ApplicationID appId, String newVersion)
    {
        validateAppId(appId);

        if (appId.isRelease())
        {
            throw new IllegalArgumentException("Cannot change the version of a " + ReleaseStatus.RELEASE + " app, app: " + appId);
        }
        ApplicationID.validateVersion(newVersion);
        getPersister().changeVersionValue(appId, newVersion);
        clearCache(appId);
        broadcast(appId);
    }

    public static boolean renameCube(ApplicationID appId, String oldName, String newName, String username)
    {
        validateAppId(appId);
        appId.validateBranchIsNotHead();

        if (appId.isRelease())
        {
            throw new IllegalArgumentException("Cannot rename a " + ReleaseStatus.RELEASE + " cube, cube: " + oldName + ", app: " + appId);
        }

        NCube.validateCubeName(oldName);
        NCube.validateCubeName(newName);

        if (oldName.equalsIgnoreCase(newName))
        {
            throw new IllegalArgumentException("Could not rename, old name cannot be the same as the new name, name: " + oldName + ", app: " + appId);
        }

        boolean result = getPersister().renameCube(appId, oldName, newName, username);

        if (CLASSPATH_CUBE.equalsIgnoreCase(oldName) || CLASSPATH_CUBE.equalsIgnoreCase(newName))
        {   // If the sys.classpath cube is renamed, or another cube is renamed into sys.classpath,
            // then the entire class loader must be dropped (and then lazily rebuilt).
            clearCache(appId);
        }
        else
        {
            Map<String, Object> appCache = getCacheForApp(appId);
            appCache.remove(oldName.toLowerCase());
            appCache.remove(newName.toLowerCase());
        }

        broadcast(appId);
        return result;
    }

    public static boolean deleteBranch(ApplicationID appId)
    {
        appId.validateBranchIsNotHead();
        return getPersister().deleteBranch(appId);
    }

    /**
     * Delete the named NCube from the database
     *
     * @param cubeName   NCube to be deleted
     */
    public static boolean deleteCube(ApplicationID appId, String cubeName, String username)
    {
        appId.validateBranchIsNotHead();
        return deleteCube(appId, cubeName, false, username);
    }

    static boolean deleteCube(ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        if (!allowDelete)
        {
            if (appId.isRelease())
            {
                throw new IllegalArgumentException(ReleaseStatus.RELEASE + " cubes cannot be deleted, cube: " + cubeName + ", app: " + appId);
            }
        }

        if (getPersister().deleteCube(appId, cubeName, allowDelete, username))
        {
            Map<String, Object> appCache = getCacheForApp(appId);
            appCache.remove(cubeName.toLowerCase());
            broadcast(appId);
            return true;
        }
        return false;
    }

    public static boolean updateTestData(ApplicationID appId, String cubeName, String testData)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return getPersister().updateTestData(appId, cubeName, testData);
    }

    public static String getTestData(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return getPersister().getTestData(appId, cubeName);
    }

    public static boolean updateNotes(ApplicationID appId, String cubeName, String notes)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return getPersister().updateNotes(appId, cubeName, notes);
    }

    public static String getNotes(ApplicationID appId, String cubeName)
    {
        validateAppId(appId);
        NCube.validateCubeName(cubeName);
        return getPersister().getNotes(appId, cubeName);
    }

    public static void createCube(ApplicationID appId, NCube ncube, String username) {
        validateCube(ncube);
        validateAppId(appId);
        appId.validateBranchIsNotHead();
        getPersister().createCube(appId, ncube, username);
        ncube.setApplicationID(appId);
        addCube(appId, ncube);
        broadcast(appId);
    }

    public static Set<String> getBranches(String tenant)
    {
        ApplicationID.validateTenant(tenant);
        return getPersister().getBranches(tenant);
    }

    public static ApplicationID getApplicationID(String tenant, String app, Map<String, Object> coord)
    {
        ApplicationID.validateTenant(tenant);
        ApplicationID.validateApp(tenant);

        if (coord == null)
        {
            coord = new HashMap();
        }

        NCube bootCube = getCube(ApplicationID.getBootVersion(tenant, app), SYS_BOOTSTRAP);

        if (bootCube == null)
        {
             throw new IllegalStateException("Missing " + SYS_BOOTSTRAP + " cube in the 0.0.0 version for the app: " + app);
        }

        ApplicationID bootAppId = (ApplicationID) bootCube.getCell(coord);
        String version = bootAppId.getVersion();
        String status = bootAppId.getStatus();
        String branch = bootAppId.getBranch();

        if (!tenant.equalsIgnoreCase(bootAppId.getTenant()))
        {
            LOG.warn("sys.bootstrap cube for tenant '" + tenant + "', app '" + app + "' is returning a different tenant '" + bootAppId.getTenant() + "' than requested. Using '" + tenant + "' instead.");
        }

        if (!app.equalsIgnoreCase(bootAppId.getApp()))
        {
            LOG.warn("sys.bootstrap cube for tenant '" + tenant + "', app '" + app + "' is returning a different app '" + bootAppId.getApp() + "' than requested. Using '" + app + "' instead.");
        }

        return new ApplicationID(tenant, app, version, status, branch);
    }

    /**
     * Fetch an array of NCubeInfoDto's where the cube names match the cubeNamePattern (contains) and
     * the content (in JSON format) 'contains' the passed in content String.
     * @param appId ApplicationID on which we are working
     * @param cubeNamePattern String pattern to match cube names
     * @param content String value that is 'contained' within the cube's JSON
     * @return Object[] of NCubeInfoDto instances.
     */
    public static Object[] search(ApplicationID appId, String cubeNamePattern, String content)
    {
        return getPersister().search(appId, cubeNamePattern, content);
    }

    public static String resolveRelativeUrl(ApplicationID appId, String relativeUrl)
    {
        validateAppId(appId);
        if (StringUtilities.isEmpty(relativeUrl))
        {
            throw new IllegalArgumentException("Cannot resolve relative url - relative url cannot be null or empty string.");
        }
        final String loUrl = relativeUrl.toLowerCase();
        if (loUrl.startsWith("http:") || loUrl.startsWith("https:") || loUrl.startsWith("file:"))
        {
            return relativeUrl;
        }

        URLClassLoader classLoader = getUrlClassLoader(appId, new HashMap());
        URL absUrl = classLoader.getResource(relativeUrl);
        return absUrl != null ? absUrl.toString() : null;
    }

    // ----------------------------------------- Resource APIs ---------------------------------------------------------
    public static String getResourceAsString(String name) throws Exception
    {
        URL url = NCubeManager.class.getResource('/' + name);
        Path resPath = Paths.get(url.toURI());
        return new String(Files.readAllBytes(resPath), "UTF-8");
    }

    static NCube getNCubeFromResource(String name)
    {
        return getNCubeFromResource(ApplicationID.testAppId, name);
    }

    public static NCube getNCubeFromResource(ApplicationID id, String name)
    {
        try
        {
            String json = getResourceAsString(name);
            NCube ncube = NCube.fromSimpleJson(json);
            ncube.setApplicationID(id);
            ncube.sha1();
            addCube(id, ncube);
            return ncube;
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException)e;
            }
            throw new RuntimeException("Failed to load cube from resource: " + name, e);
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
                nCube.sha1();
                addCube(nCube.getApplicationID(), nCube);
                lastSuccessful = nCube.getName();
                cubeList.add(nCube);
            }

            return cubeList;
        }
        catch (Exception e)
        {
            String s = "Failed to load cubes from resource: " + name + ", last successful cube: " + lastSuccessful;
            LOG.warn(s);
            throw new RuntimeException(s, e);
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
        // Write to 'system' tenant, 'NCE' app, version '0.0.0', SNAPSHOT, cube: sys.cache
        // Separate thread reads from this table every 1 second, for new commands, for
        // example, clear cache
    }
}
