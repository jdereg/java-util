package com.cedarsoftware.ncube.util;

import groovy.lang.GroovyClassLoader;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Created by Kenny on 7/12/2014.
 */
public class CdnClassLoader extends GroovyClassLoader
{
//    Pattern GroovyExpression = Pattern.compile("patternize this later");



    private boolean _preventRemoteBeanInfo;
    private boolean _preventRemoteCusomizer;
    /**
     * creates a GroovyClassLoader using the given ClassLoader as parent
     */
    public CdnClassLoader(ClassLoader loader, boolean preventRemoteBeanInfo, boolean preventRemoteCusomizer) {
        super(loader, null);
        _preventRemoteBeanInfo = preventRemoteBeanInfo;
        _preventRemoteCusomizer = preventRemoteCusomizer;
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found,
     *            or if the loader is closed.
     */
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException
    {
        if (_preventRemoteBeanInfo)
        {
           if (name.endsWith("BeanInfo"))
            {
                return super.getParent().loadClass(name);
            }
        }

        if (_preventRemoteCusomizer)
        {
            if (name.endsWith("Customizer"))
            {
                return super.getParent().loadClass(name);
            }
        }

        if (name.startsWith("java.lang") ||
            name.startsWith("java.io") ||
            name.startsWith("java.net") ||
            name.startsWith("java.util") ||
            name.startsWith("groovy.util") ||
            name.startsWith("groovy.lang")) {

            return super.getParent().loadClass(name);
        }

        if (name.endsWith("NCubeGroovyExpression") ||
            name.endsWith("NCubeGroovyController")) {
            return super.getParent().loadClass(name);
        }
        return super.findClass(name);
    }

    public URL getResource(String name) {
        if (name.endsWith("NCubeGroovyExpression.groovy") ||
            name.endsWith("NCubeGroovyController.groovy"))
        {
            return null;
        }

        if (_preventRemoteBeanInfo)
        {
            if (name.endsWith("BeanInfo.groovy"))
            {
                return null;
            }
        }

        if (_preventRemoteCusomizer)
        {
            if (name.endsWith("Customizer.groovy"))
            {
                return null;
            }
        }

        return super.getResource(name);
    }



}
