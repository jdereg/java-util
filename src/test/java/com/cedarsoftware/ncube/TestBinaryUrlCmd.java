package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UrlUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kpartlow on 8/6/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class})
public class TestBinaryUrlCmd
{
    @Test
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception {
        Class c = BinaryUrlCmd.class;
        Constructor<BinaryUrlCmd> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);
        Assert.assertNotNull(con.newInstance());
    }


    @Test
    public void testSimpleFetchException()
    {
        NCube mock = Mockito.mock(NCube.class);
        BinaryUrlCmd cmd = new BinaryUrlCmd("http://www.cedarsoftware.com", false);

        PowerMockito.mockStatic(UrlUtilities.class);
        PowerMockito.when(UrlUtilities.getContentFromUrl(any(URL.class), (boolean)eq(true))).thenThrow(IOException.class);

        Map map = new HashMap();
        map.put("ncube", mock);

        when(mock.getName()).thenReturn("foo");
        when(mock.getVersion()).thenReturn("1.0.0");

        try
        {
            cmd.simpleFetch(map);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Failed to load binary content"));
        }
    }

}
