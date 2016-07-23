package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.UnknownHostException;


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
@PrepareForTest({InetAddress.class, InetAddressUtilities.class})
public class TestInetAddressUnknownHostException
{
    @Test
    public void testGetIpAddressWithUnkownHost() throws Exception
    {
        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getLocalHost()).thenThrow(new UnknownHostException());
        Assert.assertArrayEquals(new byte[]{0, 0, 0, 0}, InetAddressUtilities.getIpAddress());
        Assert.assertEquals("localhost", InetAddressUtilities.getHostName());
    }
}
