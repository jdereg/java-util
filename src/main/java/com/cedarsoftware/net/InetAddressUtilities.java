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

/**
 * Useful InetAddress Utilities for common tasks.  Also, see
 * {@see CharSequenceUtilities} for string methods that
 * can also apply to StringBuffer, StringBuilder, and other
 * classes that implement {@see CharSequence.class}.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Ken Partlow (kpartlow@gmail.com)
 *
 */
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This utility assumes you're using IPv4 for your internet addressing
 *
 * @author Kenneth partlow
 *
 */
public class InetAddressUtilities {

    private static final byte[] SAFE_IPV4_ARRAY = {0, 0, 0, 0};

    protected InetAddressUtilities() {
    }

    public byte[] getSafeLocalAddress() {
        try {
            return getLocalAddress();
        } catch (UnknownHostException e) {
            return SAFE_IPV4_ARRAY;
        }
    }

    protected byte[] getLocalAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getAddress();
    }

    public byte getLastByteOfAddress() {
        return getLastByteOfAddress(getSafeLocalAddress());
    }

    protected byte getLastByteOfAddress(byte[] address) {
        if (address == null || address.length == 0) {
            return 0;
        }
        return address[address.length - 1];
    }

	/*
	 * public int getLongRepresentationOfLastTwoHexBytes(byte[] address) {
	 * return (ByteUtilities.toUnsignedByte(address[2]) * 255) +
	 * (ByteUtilities.toUnsignedByte(address[3])); }
	 *
	 * public String getPaddedLongRepresentationOfLastTwoHexBytes(byte[]
	 * address) { StringBuilder buffer = new StringBuilder(5);
	 * buffer.append(getLongRepresentationOfLastTwoHexBytes(address)); if
	 * (buffer.length() == 5) { return buffer.toString(); }
	 * StringPaddingUtilities.zeroPadBeginning(5, buffer); return
	 * buffer.toString(); }
	 */

    // public String getPaddedLongRepresentationOfIpAddress(byte[] address) {
    // StringBuilder buffer = new StringBuilder(10);
    // buffer.append(getLongRepresentationOfIpAddress(address));
    // if (buffer.length() == 10) {
    // return buffer.toString();
    // }
    // StringPaddingUtilities.zeroPadBeginning(10, buffer);
    // return buffer.toString();
    // }

    // public String getHexRepresentationOfIpAddress() {
    // byte[] address = getSafeLocalAddress();

    // StringBuffer buffer = new StringBuffer(8);
    // buffer.append(ByteUtilities.toUnsignedByte(address[0]));
    // buffer.append(ByteUtilities.toUnsignedByte(address[1]));
    // buffer.append(ByteUtilities.toUnsignedByte(address[2]));
    // buffer.append(ByteUtilities.toUnsignedByte(address[3]));
    // return buffer.toString();
    // }

    // public String getLastTwoHexBytesOfIpAddress() {
    // return getHexRepresentationOfIpAddress().substring(4, 8);
    // }

    // public String getHexRepresentationOfIpAddress() throws
    // UnknownHostException {
    // byte[] address = getLocalIpAddress();
    //
    // StringBuffer buffer = new StringBuffer(8);
    // buffer.append(ByteUtilities.toZeroPaddedHexByte(address[0]));
    // buffer.append(ByteUtilities.toZeroPaddedHexByte(address[1]));
    // buffer.append(ByteUtilities.toZeroPaddedHexByte(address[2]));
    // buffer.append(ByteUtilities.toZeroPaddedHexByte(address[3]));
    // return buffer.toString();
    // }

    // public String getLastTwoHexBytesOfIpAddress() throws UnknownHostException
    // {
    // return getHexRepresentationOfIpAddress().substring(4, 8);
    // }

    // public long getLastTwoSectionsOfIpAddress() throws UnknownHostException {
    // byte[] address = getLocalIpAddress();
    // return getLongRepresentationOfIpAddress(new byte[] {address[2],
    // address[3]});
    // }

    // public String getPaddedLongRepresentationOfLastTwoBytesInIpAddress(byte[]
    // address) {
    // StringBuffer buffer = new StringBuffer(5);
    // buffer.append(getLongRepresentationOfIpAddress(new byte[] {address[2],
    // address[3]}));
    // if (buffer.length() == 5) {
    // return buffer.toString();
    // }
    // return StringPaddingUtilities.zeroPrefixPadding(5, buffer).toString();
    // }

    // Most ip address with padding can be is 4244897280, so we pad accordingly.
    // public String getPaddedLongRepresentationOfLastTwoBytesInIpAddress()
    // throws UnknownHostException {
    // return
    // getPaddedLongRepresentationOfLastTwoBytesInIpAddress(getLocalIpAddress());
    // }

    // public String getPaddedLongRepresentationOfIpAddress() {
    // return getPaddedLongRepresentationOfIpAddress(getSafeLocalAddress());
    // }

    // public String getMaxPaddedLongRepresentationOfIpAddress() {
    // return getPaddedLongRepresentationOfIpAddress(new byte[] {-1, -1, -1,
    // -1});
    // }

    // public String getPaddedLongRepresentationOfLastTwoHexBytes() {
    // return getPaddedLongRepresentationOfLastTwoHexBytes(getIpAddress());
    // return getPaddedLongRepresentationOfLastTwoHexBytes(new byte[] {-1, -1,
    // -1, -1});

    // }

    // public String getPaddedLongRepresentationOfLastByte(byte[] address) {
    // StringBuilder builder = new StringBuilder();
    // builder.append(ByteUtilities.toUnsignedByte(address[3]));
    // if (address[3] < 100) {
    // builder.insert(0, '0');
    // if (address[3] < 10) {
    // builder.insert(0, '0');
    // }
    // }
    // return builder.toString();
    // }

    // public String getLongRepresentationOfIpAddress() throws
    // UnknownHostException {
    // return
    // Long.toString(getLongRepresentationOfIpAddress(getLocalIpAddress()));
    // }

}
