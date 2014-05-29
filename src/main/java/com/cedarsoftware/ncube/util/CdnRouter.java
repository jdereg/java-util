package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.Regexes;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.StringUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

            NCube routingCube = NCubeManager.getCube(cubeName, version);
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

            coord.put(CONTENT_NAME, logicalName);
            coord.put(CONTENT_TYPE, type);
            Map output = new HashMap();
            Object content = routingCube.getCell(coord, output, new CdnUrlExecutor(request, response));

            try
            {
                setResponseHeaders(response);
                OutputStream o = new BufferedOutputStream(response.getOutputStream());
                if (content instanceof String)
                {
                    o.write(StringUtilities.getBytes((String)content, "UTF-8"));
                    IOUtilities.flush(o);
                }
                else if (content instanceof byte[])
                {
                    o.write((byte[])content);
                    IOUtilities.flush(o);
                }
                else if (content == null)
                {
                    String msg = "CdnRouter - No content at coordinate: " + coord;
                    sendErrorResponse(response, HttpServletResponse.SC_NO_CONTENT, msg);
                }
                else
                {
                    String msg = "CdnRouter - CDN returned content that is not a String or byte[], class = " + content.getClass().getName();
                    sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                }
            }
            catch (IOException e)
            {
                String msg = "CdnRouter - Error occurred writing HTTP response.";
                sendErrorResponse(response, HttpServletResponse.SC_NO_CONTENT, msg);
            }
        }
        catch (Exception e)
        {
            String msg = "CdnRouter - Error occurred writing HTTP response: " + e.getMessage();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    private void setResponseHeaders(HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "max-age=43200, proxy-revalidate");
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
