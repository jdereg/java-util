package com.cedarsoftware.ncube.util;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 *  @author Ken Partlow (kpartlow@gmail.com)
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
public class CdnClassLoader extends GroovyClassLoader
{
    private final boolean _preventRemoteBeanInfo;
    private final boolean _preventRemoteCustomizer;

    /**
     * creates a GroovyClassLoader using the given ClassLoader as parent
     */
    public CdnClassLoader(ClassLoader loader, boolean preventRemoteBeanInfo, boolean preventRemoteCustomizer)
    {
        super(loader, null);
        _preventRemoteBeanInfo = preventRemoteBeanInfo;
        _preventRemoteCustomizer = preventRemoteCustomizer;
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @throws ClassNotFoundException if the class could not be found,
     *                                or if the loader is closed.
     */
    protected Class<?> findClass(final String name) throws ClassNotFoundException
    {
        return isLocalOnlyClass(name) ? super.getParent().loadClass(name) : super.findClass(name);
    }

    boolean isLocalOnlyClass(String name) {
        if (name.startsWith("java.lang") ||
            name.startsWith("java.io") ||
            name.startsWith("java.net") ||
            name.startsWith("java.util") ||
            name.startsWith("groovy.lang") ||
            name.startsWith("groovy.util") ||
            name.startsWith("ncube.grv")) {
            return true;
        }

        if (_preventRemoteBeanInfo && name.endsWith("BeanInfo")) {
            return true;
        }

        return _preventRemoteBeanInfo && name.endsWith("Customizer");
    }
    /**
     * @param name Name of resource
     * @return true if we should only look locally.
     */
    boolean isLocalOnlyResource(String name)
    {
        if (name.startsWith("META-INF") ||
            name.startsWith("ncube/grv/exp") ||
            name.startsWith("ncube/grv/method"))
        {
            return true;
        }

        if (_preventRemoteBeanInfo && name.endsWith("BeanInfo.groovy"))
        {
            return true;
        }

        return (_preventRemoteCustomizer && name.endsWith("Customizer.groovy"));
    }

    public Enumeration<URL> getResources(String name) throws IOException
    {
        if (isLocalOnlyResource(name))
        {
            return new Enumeration<URL>()
            {
                public boolean hasMoreElements()
                {
                    return false;
                }

                public URL nextElement()
                {
                    throw new NoSuchElementException();
                }
            };
        }
        return super.getResources(name);
    }

    public URL getResource(String name)
    {
        if (isLocalOnlyResource(name))
        {
            return null;
        }
        return super.getResource(name);
    }
}
