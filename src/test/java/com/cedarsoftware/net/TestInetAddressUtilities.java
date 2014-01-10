/*
 *         Copyright (c) Cedar Software LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.net;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestInetAddressUtilities {

    //@Test
    //public void testConstructorIsPrivate() throws Exception {
    //    Asserter.assertClassOnlyHasAPrivateDefaultConstructor(InetAddressUtilities.class);
    //}

    @Test
    public void testGetLocalIpAddress() throws Exception {
        byte[] expected = InetAddress.getLocalHost().getAddress();
        byte[] actual = new InetAddressUtilities().getSafeLocalAddress();

        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetIpAddressErrorCase() {
        InetAddressUtilities tester = new InetAddressUtilitiesTestHarness();
        byte[] actual = tester.getSafeLocalAddress();
        Assert.assertEquals(actual[0], 0);
        Assert.assertEquals(actual[1], 0);
        Assert.assertEquals(actual[2], 0);
        Assert.assertEquals(actual[3], 0);
    }

    @Test
    public void testGetLastByteOfAddressWithDifferentProtocolLengths() throws Exception {
        InetAddressUtilities inet = new InetAddressUtilities();

        //ipv4 = 32 bits
        byte[] ipv4 = new byte[] {0x01, 0x02, 0x03, 0x04 };
        Assert.assertEquals(0x04, inet.getLastByteOfAddress(ipv4));

        //ipv6 = 128 bits
        byte[] ipv6 = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        Assert.assertEquals(0x0F, inet.getLastByteOfAddress(ipv6));

        //next gen addressing = ? bits
        byte[] nextGen = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13 };
        Assert.assertEquals(0x13, inet.getLastByteOfAddress(nextGen));

        // null will always return 0, as will empty arrays passed in.
        Assert.assertEquals(0, inet.getLastByteOfAddress(null));
        Assert.assertEquals(0, inet.getLastByteOfAddress(new byte[] {}));

    }

    @Test
    public void testGetLastByteOfAddress() throws Exception {
        byte[] bytes =  InetAddress.getLocalHost().getAddress();
        Assert.assertEquals(bytes[bytes.length-1], new InetAddressUtilities().getLastByteOfAddress());

    }

/*
//    @Test
//    public void testGetPaddedLongRepresentationOfIpAddress() {
//        runAssert(new byte[] {-1, -1, -1, -1}, "4244897280");
//        runAssert(new byte[] {10, 10, -1, -1}, "0166529280");
//        runAssert(new byte[] {-68, -55, 0, 2}, "3130368527");
//        runAssert(new byte[] {-64, -88, 35, -62}, "3194557319");
//        System.nanoTime();
//    }
//
//
//    public void runAssert(byte[] bytes, String expected) {
//        Assert.assertEquals(expected, new InetAddressUtilities().getPaddedLongRepresentationOfIpAddress(bytes));
//    }
*/


    private class InetAddressUtilitiesTestHarness extends InetAddressUtilities {

        @Override
        protected byte[] getLocalAddress() throws UnknownHostException {
            throw new UnknownHostException();
        }

    }

}
