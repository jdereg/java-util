package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.ssl.SSLHandshakeException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;


/**
 * Created by kpartlow on 4/19/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class, IOUtilities.class})
public class TestHandshakeException
{
    @Test
    public void testUrlUtilitiesHandshakeException() throws Exception
    {
        PowerMockito.mockStatic(IOUtilities.class);
        PowerMockito.when(IOUtilities.getInputStream(any(URLConnection.class))).thenThrow(new SSLHandshakeException("error"));

        assertNull(UrlUtilities.getContentFromUrl(new URL("http://www.google.com"), null, null, true));
        PowerMockito.verifyStatic(times(1));
    }
}
