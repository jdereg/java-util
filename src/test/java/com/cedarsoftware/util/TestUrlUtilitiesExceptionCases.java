package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.ssl.SSLHandshakeException;
import java.net.URL;

import static org.junit.Assert.assertNull;


/**
 * Created by kpartlow on 4/19/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class})
public class TestUrlUtilitiesExceptionCases
{
    private String urlString = "http://www.google.com";

    @Test
    public void testGetIpAddressWithUnkownHost() throws Exception
    {
        URL u = new URL(urlString);
        PowerMockito.mockStatic(UrlUtilities.class);
        PowerMockito.when(UrlUtilities.getConnection(u, null, true, false, false, null, null)).thenThrow(new SSLHandshakeException("error"));

        assertNull(UrlUtilities.getContentFromUrl(urlString, null, null));
        assertNull(UrlUtilities.getContentFromUrl(urlString));
        assertNull(UrlUtilities.getContentFromUrl(urlString, null, null, null, null));

    }
}
