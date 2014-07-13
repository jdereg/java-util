package com.cedarsoftware.ncube.util;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

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
        if (isLocalOnlyResource(name)) {
            return super.getParent().loadClass(name);
        }
        return super.findClass(name);
    }

    /**
     * Thse need to be changed to some sort of pattern recognition
     * @param name Name of resource
     * @return true if we should only look locally.
     */
    protected boolean isLocalOnlyResource(String name) {
        if (_preventRemoteBeanInfo)
        {
            if (name.endsWith("BeanInfo") ||
                name.endsWith("BeanInfo.groovy"))
            {
                return true;
            }
        }

        if (_preventRemoteCusomizer)
        {
            if (name.endsWith("Customizer") ||
                name.endsWith("Customizer.groovy"))
            {
                return true;
            }
        }

        if (name.startsWith("java.lang") ||
                name.startsWith("java.io") ||
                name.startsWith("java.net") ||
                name.startsWith("java.util") ||
                name.startsWith("groovy.util") ||
                name.startsWith("groovy.lang")) {

            return true;
        }

        if (name.endsWith("NCubeGroovyExpression") ||
                name.endsWith("NCubeGroovyController") ||
                name.endsWith("NCubeGroovyExpression.groovy") ||
                name.endsWith("NCubeGroovyController.groovy"))
        {
            return true;
        }

        return name.endsWith("org.codehaus.groovy.transform.ASTTransformation");
    }

    public Enumeration<URL> getResources(String name) throws IOException
    {
        if (isLocalOnlyResource(name)) {
            return new Enumeration<URL>() {

                @Override
                public boolean hasMoreElements()
                {
                    return false;
                }

                @Override
                public URL nextElement()
                {
                    throw new NoSuchElementException();
                }
            };
        }
        return super.getResources(name);
    }

    public URL getResource(String name) {
        if (isLocalOnlyResource(name)) {
            return null;
        }
        return super.getResource(name);
    }



}
