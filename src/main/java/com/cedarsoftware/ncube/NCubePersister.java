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
    void createCube(ApplicationID id, NCube cube);
    
    void updateCube(ApplicationID appId, NCube cube);

    //It doesn't look like findCube is used
    //NCube findCube(ApplicationID appId, String name);
    
    List<NCube> loadCubes(ApplicationID appId);
    Object[] getNCubes(ApplicationID appId, String sqlLike);

    NCube findCube(ApplicationID appId, String ncubeName);

    boolean deleteCube(ApplicationID appId, String name, boolean allowDelete);
    boolean doesCubeExist(ApplicationID id, String name);

    Object[] getAppNames();
    Object[] getAppVersions(ApplicationID id);

    boolean updateNotes(ApplicationID id, String cubeName, String notes);
    String getNotes(ApplicationID id, String cubeName);

    int createSnapshotVersion(ApplicationID id, String newVersion);
    int changeVersionValue(ApplicationID id, String newVersion);
    int releaseCubes(ApplicationID id);

    boolean renameCube(ApplicationID id, NCube oldCube, String newName);

    boolean updateTestData(ApplicationID id, String cubeName, String testData);
    String getTestData(ApplicationID id, String cubeName);
}
