package com.cedarsoftware.ncube.util;

import groovy.lang.GroovyClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.net.URL;

/**
 * Created by kpartlow on 6/23/2014.
 */
public class CdnClassLoader extends GroovyClassLoader
{
    private static final Log LOG = LogFactory.getLog(CdnClassLoader.class);
    //private String _baseUrl = "http://www.codetested.com/";
    private String _baseUrl;
    private ClassLoader _parent;

    public CdnClassLoader(ClassLoader parent, String baseUrl) {
        this._baseUrl = baseUrl;
        this._parent = parent;
    }

    public URL getResource(String name) {
        URL url;
        if (_parent != null) {
            url = _parent.getResource(name);
        } else {
            url = getBootstrapResource(name);
        }
        if (url == null) {
            url = findResource(name);
        }


      //  if (url == null) {
         //   try
       //     {
       //         url = new URL(_baseUrl + name);
       //     } catch (Exception e) {
                // do nothing right now.
      //      }

     //   }
        if (url == null) {
            LOG.error("Could not find:  " + name);
        }
        return url;
    }

    private static URL getBootstrapResource(String name) {
        URLClassPath ucp = getBootstrapClassPath();
        Resource res = ucp.getResource(name);
        return res != null ? res.getURL() : null;
    }

    static URLClassPath getBootstrapClassPath() {
        return sun.misc.Launcher.getBootstrapClassPath();
    }

}
