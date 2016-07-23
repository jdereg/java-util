package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.ssl.SSLHandshakeException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

/**
 * @author Ken Partlow
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class, IOUtilities.class})
public class TestHandshakeException
{
    @Test
    public void testUrlUtilitiesHandshakeException() throws Exception
    {
        PowerMockito.mockStatic(IOUtilities.class);
        Mockito.when(IOUtilities.getInputStream(any(URLConnection.class))).thenThrow(new SSLHandshakeException("error"));

        assertNull(UrlUtilities.getContentFromUrl(new URL("http://www.google.com"), null, null, true));
        PowerMockito.verifyStatic(times(1));
    }
}
