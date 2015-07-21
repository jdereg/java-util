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
    NCube loadCube(NCubeInfoDto dto)
    NCube loadCube(ApplicationID appId, String name)
    NCube loadCubeByRevision(ApplicationID appId, String name, long revision)
    NCube loadCubeBySha1(ApplicationID appId, String name, String sha1)

    List<String> getAppNames(String tenant, String status, String branch)
    List<String> getAppVersions(String tenant, String app, String status, String branch)
    boolean doesCubeExist(ApplicationID appId, String cubeName)

    List<NCubeInfoDto> getRevisions(ApplicationID appId, String cubeName)
    List<NCubeInfoDto> search(ApplicationID appId, String cubeNamePattern, String searchValue, Map options)

    Set<String> getBranches(String tenant)

    String getTestData(ApplicationID appId, String cubeName)
    String getNotes(ApplicationID appId, String cubeName)

}
