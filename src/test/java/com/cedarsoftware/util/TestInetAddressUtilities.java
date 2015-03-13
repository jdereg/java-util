package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetAddress;

/**
 * useful InetAddress Utilities
 *
 * @author Kenneth Partlow
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
public class TestInetAddressUtilities
{
    @Test
    public void testMapUtilitiesConstructor() throws Exception
    {
        Constructor<InetAddressUtilities> con = InetAddressUtilities.class.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testGetIpAddress() throws Exception {
        byte[] bytes = InetAddress.getLocalHost().getAddress();
        Assert.assertArrayEquals(bytes, InetAddressUtilities.getIpAddress());
    }

    @Test
    public void testGetLocalHost() throws Exception {
        String name = InetAddress.getLocalHost().getHostName();
        Assert.assertEquals(name, InetAddressUtilities.getHostName());
    }


}
