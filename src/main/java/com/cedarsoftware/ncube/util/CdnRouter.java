package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.ApplicationID;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.Regexes;
import com.cedarsoftware.ncube.ReleaseStatus;
import com.cedarsoftware.util.StringUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Accept inbound requests for static content, route via n-cube.
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
public class CdnRouter
{
    private static CdnRoutingProvider provider;
    private static final Log LOG = LogFactory.getLog(CdnRouter.class);
    public static final String CUBE_NAME = "router.cubeName";
    public static final String CUBE_VERSION = "router.version";
    public static final String CONTENT_TYPE = "content.type";
    public static final String CONTENT_NAME = "content.name";
    public static final String HTTP_REQUEST = "http.request";
    public static final String HTTP_RESPONSE = "http.response";

    public static void setCdnRoutingProvider(CdnRoutingProvider p)
    {
        provider = p;
    }

    /**
     * Route the given request based on configured routing within n-cube
     */
    public void route(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            if (provider == null)
            {
                String msg = "CdnRouter - CdnRoutingProvider has not been set into the CdnRouter.";
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                return;
            }

            // Peel off mime type and logical file name
            final String servletPath = request.getServletPath();
            String[] info = getPathComponents(servletPath);
            if (info == null)
            {
                String msg = "CdnRouter - Invalid ServletPath (must start with /dyn/) request: " + servletPath;   // Thx Corey Crider
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
            String type = info[0];
            String logicalName = info[1];

            // Check for authorization
            if (!provider.isAuthorized(type))
            {   // Thx Raja Gade
                String msg = "CdnRouter - Unauthorized access, request: " + request.getRequestURL();
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, msg);
                return;
            }

            // Allow provider to establish base coordinate
            Map coord = new HashMap();
            provider.setupCoordinate(coord);

            String cubeName = (String) coord.get(CUBE_NAME);
            String version = (String) coord.get(CUBE_VERSION);
            if (StringUtilities.isEmpty(cubeName) || StringUtilities.isEmpty(version))
            {
                String msg = "CdnRouter - CdnRoutingProvider did not set up '" + CUBE_NAME + "' or '" + CUBE_VERSION + "' in the Map coordinate.";
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                return;
            }

            coord.put(CONTENT_NAME, logicalName);
            coord.put(CONTENT_TYPE, type);
            coord.put(HTTP_REQUEST, request);
            coord.put(HTTP_RESPONSE, response);
            Map output = new HashMap();
            // TODO: MUST send account, app, and status so that the router knows what cube to get.
            ApplicationID appId = new ApplicationID(null, null, version, ReleaseStatus.SNAPSHOT.name());
            NCube routingCube = NCubeManager.getCube(cubeName, appId);
            if (routingCube == null)
            {
                throw new IllegalStateException("In order to use the n-cube CDN routing capabilities, " +
                        "a CdnRouter n-cube must already be loaded, and it's name passed in as CdnRouter.CUBE_NAME");
            }
            routingCube.getCell(coord, output);
        }
        catch (Exception e)
        {
            LOG.error("CdnRouter exception occurred", e);
            // Required, so that error message is not double logged.
            try
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CdnRouter - Error occurred: " + e.getMessage());
            }
            catch (Exception ignore)
            { }
        }
    }

    /**
     * Send an HTTP error response
     */
    private static void sendErrorResponse(HttpServletResponse response, int error, String msg)
    {
        try
        {
            LOG.error(msg);
            response.sendError(error, msg);
        }
        catch (Exception ignore)
        { }
    }

    private static String[] getPathComponents(String pathInfo)
    {
        if (pathInfo == null)
        {
            return null;
        }

        Matcher matcher = Regexes.cdnUrlPattern.matcher(pathInfo);
        if (!matcher.find())
        {
            return null;
        }

        String[] info = new String[2];
        info[0] = matcher.group(1);
        info[1] = matcher.group(2);
        return info;
    }
}