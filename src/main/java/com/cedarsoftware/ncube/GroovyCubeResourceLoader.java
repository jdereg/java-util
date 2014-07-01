package com.cedarsoftware.ncube;

import groovy.lang.GroovyResourceLoader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created by kpartlow on 6/22/2014.
 */
public class GroovyCubeResourceLoader implements GroovyResourceLoader
{
    @Override
    public URL loadGroovySource(final String filename) throws MalformedURLException
    {
        return AccessController.doPrivileged(new PrivilegedAction<URL>()
        {
            public URL run()
            {
                try
                {
                    URL ret = getSourceFile(filename, "");
                    if (ret != null)
                        return ret;
                }
                catch (Throwable t)
                { //
                }
                return null;
           }
        });

        //URL url = GroovyCubeResourceLoader.class.getClassLoader().getResource(filename);

        //if (url == null) {

            //System.out.println("Tried to load:  " + filename);
        //}
        //return url;
    }


    private URL getSourceFile(String name, String extension) {
        String filename = name.replace('.', '/') + "." + extension;
        URL ret = GroovyCubeResourceLoader.class.getClassLoader().getResource(filename);
        if (isFile(ret) && getFileForUrl(ret, filename) == null) return null;
        return ret;
    }

    private boolean isFile(URL ret) {
        return ret != null && ret.getProtocol().equals("file");
    }

    private File getFileForUrl(URL ret, String filename) {
        String fileWithoutPackage = filename;
        if (fileWithoutPackage.indexOf('/') != -1) {
            int index = fileWithoutPackage.lastIndexOf('/');
            fileWithoutPackage = fileWithoutPackage.substring(index + 1);
        }
        return fileReallyExists(ret, fileWithoutPackage);
    }

    private File fileReallyExists(URL ret, String fileWithoutPackage) {
        File path;
        try {
            /* fix for GROOVY-5809 */
            path = new File(ret.toURI());
        } catch(URISyntaxException e) {
            path = new File(decodeFileName(ret.getFile()));
        }
        path = path.getParentFile();
        if (path.exists() && path.isDirectory()) {
            File file = new File(path, fileWithoutPackage);
            if (file.exists()) {
                // file.exists() might be case insensitive. Let's do
                // case sensitive match for the filename
                File parent = file.getParentFile();
                for (String child : parent.list()) {
                    if (child.equals(fileWithoutPackage)) return file;
                }
            }
        }
        //file does not exist!
        return null;
    }

    /**
     * This method will take a file name and try to "decode" any URL encoded characters.  For example
     * if the file name contains any spaces this method call will take the resulting %20 encoded values
     * and convert them to spaces.
     * <p>
     * This method was added specifically to fix defect:  Groovy-1787.  The defect involved a situation
     * where two scripts were sitting in a directory with spaces in its name.  The code would fail
     * when the class loader tried to resolve the file name and would choke on the URLEncoded space values.
     */
    private String decodeFileName(String fileName) {
        String decodedFile = fileName;
        try {
            decodedFile = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Encountered an invalid encoding scheme when trying to use URLDecoder.decode() inside of the GroovyClassLoader.decodeFileName() method.  Returning the unencoded URL.");
            System.err.println("Please note that if you encounter this error and you have spaces in your directory you will run into issues.  Refer to GROOVY-1787 for description of this bug.");
        }

        return decodedFile;
    }



}
