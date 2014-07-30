package com.cedarsoftware.ncube.util;

import java.sql.Connection;
import java.util.Map;

/**
 * Implement this interface to set up the coordinate used to
 * access a CDN routing, templating, n-cube.  A CDN routing n-cube allows
 * n-cube to route logical CDN locations to physical locations.  The additional
 * information added to the n-cube input coordinate from the setupCoordinate()
 * API, allows additional scoping to be added which may change the location a
 * logical item on the CDN lives.  For example, businessUnit could be an axis,
 * and setting the businessUnit on the coordinate before the CDN routing n-cube
 * is accessed, allows the physical location on the CDN to be different per
 * business unit.  Similarly, other criteria could be taken into account in the
 * decision to map the logical CDN reference to a physical CDN reference.
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
public interface CdnRoutingProvider
{
    void setupCoordinate(Map coord);
    boolean isAuthorized(String type);
    void doneWithConnection(Connection connection);
}
