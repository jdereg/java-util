package com.cedarsoftware.ncube;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by kpartlow on 8/6/2014.
 */
public class TestBinaryUrlCmd
{
    @Test(expected=IllegalStateException.class)
    public void testSimpleFetchException() {
        NCube mock = Mockito.mock(NCube.class);
        BinaryUrlCmd cmd = new BinaryUrlCmd("http://www.cedarsoftware.com", false);

        Map map = new HashMap();
        map.put("ncube", mock);

        when(mock.getName()).thenReturn("foo");
        when(mock.getVersion()).thenThrow(RuntimeException.class);
        cmd.simpleFetch(map);
    }

}
