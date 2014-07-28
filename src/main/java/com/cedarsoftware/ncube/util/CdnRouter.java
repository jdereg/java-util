package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.Regexes;
import com.cedarsoftware.util.StringUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Accept inbound requests for static content, route via n-cube.
 *
 * @author John DeRegnaucourt
 */
public class CdnRouter
{
    private static CdnRoutingProvider provider;
    private static final Log LOG = LogFactory.getLog(CdnRouter.class);
    public static final String CUBE_NAME = "router.cubeName";
    public static final String CUBE_VERSION = "router.version";
    public static final String APP = "router.app";
    public static final String CONNECTION = "router.connection";
    public static final String STATUS = "router.status";
    public static final String DATE = "router.date";
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
                String msg = "CdnRouter - File not found - servletPath: " + servletPath;   // Thx Corey Crider
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
            NCube routingCube = NCubeManager.getCube(cubeName, version);
            if (routingCube == null)
            {
                routingCube = NCubeManager.getCube("cdnRouter", version);
            }
            if (routingCube == null)
            {
                Connection connection = (Connection) coord.get(CONNECTION);
                String app = (String) coord.get(APP);
                String status = (String) coord.get(STATUS);
                Date date = (Date) coord.get(DATE);

                if (connection == null)
                {
                    String msg = "CdnRouter - CdnRoutingProvider did not set up '" + CONNECTION + "' in the Map coordinate.";
                    sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                    return;
                }

                if (StringUtilities.isEmpty(app))
                {
                    String msg = "CdnRouter - CdnRoutingProvider did not set '" + APP + "' in the Map coordinate.";
                    sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                    return;
                }

                if (StringUtilities.isEmpty(status))
                {
                    String msg = "CdnRouter - CdnRoutingProvider did not set '" + STATUS + "' in the Map coordinate.";
                    sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                    return;
                }
                routingCube = NCubeManager.loadCube(connection, app, cubeName, version, status, date);
            }
            routingCube.getCell(coord, output);
        }
        catch (Exception e)
        {
            LOG.error("CdnRouter exception occurred", e);
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