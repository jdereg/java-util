package com.cedarsoftware.lang;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kpartlow on 1/6/14.
 */
public class TestSystemUtilities
{
    @Test
    public void testConstructor() {
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(SystemUtilities.class);

    }

    @Test
    public void testGetExternalVariable()
    {
        Assert.assertNotNull(SystemUtilities.getExternalVariable("PATH"));
        Assert.assertNotNull(SystemUtilities.getExternalVariable("Path"));

        Assert.assertNull(SystemUtilities.getExternalVariable("foo"));

        Assert.assertNull(System.getenv("java.vm.version"));
        Assert.assertNotNull(SystemUtilities.getExternalVariable("java.vm.version"));
        Assert.assertNotNull(SystemUtilities.getExternalVariable("java.vm.vendor"));

        //System.out.println(System.getProperties().size());
        //Enumeration e = System.getProperties().keys();
        //while (e.hasMoreElements()) {
        //    System.out.println(e.nextElement());
        //}
        //Assert.assertNotNull(SystemUtilities.getExternalVariable(""));
    }
}
