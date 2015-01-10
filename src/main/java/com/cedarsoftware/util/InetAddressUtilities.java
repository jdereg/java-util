package com.cedarsoftware.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Useful InetAddress Utilities
 *
 * @author Kenneth Partlow
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
public class InetAddressUtilities
{
    private static final Logger LOG = LogManager.getLogger(InetAddressUtilities.class);

    private InetAddressUtilities() {
        super();
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

    public static byte[] getIpAddress() {
        try
        {
            return getLocalHost().getAddress();
        }
        catch (Exception e)
        {
            LOG.warn("Failed to obtain computer's IP address", e);
            return new byte[] {0,0,0,0};
        }
    }

    public static String getHostName()
    {
        try
        {
            return getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            LOG.warn("Unable to fetch 'hostname'", e);
            return "localhost";
        }
    }


}
