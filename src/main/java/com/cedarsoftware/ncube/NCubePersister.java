package com.cedarsoftware.ncube;

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
public interface NCubePersister
{
    void createCube(ApplicationID appId, NCube cube, String username);
    void updateCube(ApplicationID appId, NCube cube, String username);

    List<NCube> loadCubes(ApplicationID appId);
    Object[] getNCubes(ApplicationID appId, String pattern);

    NCube findCube(ApplicationID appId, String ncubeName);

    boolean deleteCube(ApplicationID appId, String name, boolean allowDelete, String username);
    boolean doesCubeExist(ApplicationID appId, String name);

    Object[] getAppNames(String account);
    Object[] getAppVersions(ApplicationID appId);

    boolean updateNotes(ApplicationID appId, String cubeName, String notes);
    String getNotes(ApplicationID appId, String cubeName);

    int createSnapshotVersion(ApplicationID appId, String newVersion);
    int changeVersionValue(ApplicationID appId, String newVersion);
    int releaseCubes(ApplicationID appId);

    boolean renameCube(ApplicationID appId, NCube oldCube, String newName);

    boolean updateTestData(ApplicationID appId, String cubeName, String testData);
    String getTestData(ApplicationID appId, String cubeName);
}
