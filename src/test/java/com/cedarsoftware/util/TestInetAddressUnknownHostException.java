package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.UnknownHostException;



/**
 * Created by kpartlow on 4/19/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({InetAddressUtilities.class})
public class TestInetAddressUnknownHostException
{
    @Test
    public void testGetIpAddressWithUnkownHost() throws Exception {
        PowerMockito.stub(PowerMockito.method(InetAddressUtilities.class, "getLocalHost")).toThrow(new UnknownHostException());
        Assert.assertArrayEquals(new byte[] {0,0,0,0}, InetAddressUtilities.getIpAddress());
        PowerMockito.verifyStatic(Mockito.times(1));
    }
}
