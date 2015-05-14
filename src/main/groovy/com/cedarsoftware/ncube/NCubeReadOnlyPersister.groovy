package com.cedarsoftware.ncube

import groovy.transform.CompileStatic

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
@CompileStatic
interface NCubeReadOnlyPersister
{
    NCube loadCube(String id)
    NCube loadCube(ApplicationID appId, String name)
    NCube loadCube(ApplicationID appId, String name, String sha1)

    Object[] getCubeRecords(ApplicationID appId, String pattern, boolean activeOnly)
    Object[] getChangedRecords(ApplicationID appId)
    Object[] getAppNames(String tenant, String status, String branch)
    Object[] getAppVersions(String tenant, String app, String status, String branch)
    boolean doesCubeExist(ApplicationID appId, String cubeName)

    Object[] getDeletedCubeRecords(ApplicationID appId, String pattern)
    Object[] getRevisions(ApplicationID appId, String cubeName)
    Object[] search(ApplicationID appId, String cubeNamePattern, String searchValue)

    Set<String> getBranches(String tenant)

    String getTestData(ApplicationID appId, String cubeName)
    String getNotes(ApplicationID appId, String cubeName)

}
