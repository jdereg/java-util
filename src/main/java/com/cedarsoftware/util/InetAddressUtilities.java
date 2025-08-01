package com.cedarsoftware.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cedarsoftware.util.LoggingConfig;

/**
 * Useful InetAddress Utilities
 *
 * @author Kenneth Partlow
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class InetAddressUtilities
{
    private static final Logger LOG = Logger.getLogger(InetAddressUtilities.class.getName());
    static { LoggingConfig.init(); }
    private InetAddressUtilities() {
        super();
    }

    public static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null;    // never reached
        }
    }

    public static byte[] getIpAddress() {
        try
        {
            return getLocalHost().getAddress();
        }
        catch (Exception e)
        {
            LOG.warning("Failed to obtain computer's IP address");
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
            LOG.warning("Unable to fetch 'hostname'");
            return "localhost";
        }
    }


}
