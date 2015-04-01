package com.cedarsoftware.ncube

import java.sql.SQLException

/**
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
interface TestingDatabaseManager
{
    public void setUp() throws SQLException
    public void tearDown() throws SQLException

    void insertCubeWithNoSha1(ApplicationID appId, String username, NCube cube) throws Exception
    public void addCubes(ApplicationID appId, String username, NCube[] cubes) throws Exception;
    public void removeBranches(ApplicationID[] branches) throws Exception;
    public void updateCube(ApplicationID appId, String username, NCube cube) throws Exception;
}
