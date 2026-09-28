package com.cedarsoftware.util.convert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversions to and from {@link InetAddress}. Its text is the IP address ("10.0.0.1", "2001:db8:0:0:0:0:0:1"). Its
 * Map form adds the host name when the address already carries one: {"host": "h", "address": "10.0.0.1"}.
 * <p>
 * No conversion looks a name up. Only IP literals are read -- a dotted-quad IPv4 address, parsed here, or an IPv6
 * literal, which InetAddress parses without DNS -- and a host name is kept beside its address, never resolved.
 * (Jackson resolves one: a network call per value read, to a host the input names.) Writing uses toString(), not
 * getHostName(), which may do a reverse lookup.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
final class InetAddressConversions {

    private InetAddressConversions() {
    }

    static String toString(Object from, Converter converter) {
        return ((InetAddress) from).getHostAddress();
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        InetAddress address = (InetAddress) from;
        Map<String, Object> map = new LinkedHashMap<>();
        String text = address.toString();                   // "host/literal", or "/literal" when it knows no name
        int slash = text.indexOf('/');
        if (slash > 0) {
            map.put(MapConversions.HOST, text.substring(0, slash));
        }
        map.put(MapConversions.ADDRESS, address.getHostAddress());
        return map;
    }

    /** An IP literal, or "host/literal" as InetAddress.toString() writes it (the host kept, not resolved). */
    static InetAddress fromText(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        int slash = text.indexOf('/');
        return slash < 0 ? fromParts(null, text) : fromParts(text.substring(0, slash), text.substring(slash + 1));
    }

    /** The address {@code literal} names, carrying {@code host} (null or empty for none) as its name. */
    static InetAddress fromParts(String host, String literal) {
        String name = host == null || host.isEmpty() ? null : host;
        try {
            byte[] ipv4 = parseIpv4(literal);
            if (ipv4 != null) {
                return InetAddress.getByAddress(name, ipv4);
            }
            if (literal.indexOf(':') >= 0) {
                // An IPv6 literal: parsed, never looked up (an invalid one fails as invalid, not as unknown)
                InetAddress parsed = InetAddress.getByName(literal);
                return name == null ? parsed : InetAddress.getByAddress(name, parsed.getAddress());
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Not an IP address: '" + literal + "'", e);
        }
        throw new IllegalArgumentException("Not an IP address: '" + literal
                + "'. Converter reads IP addresses only; it does not look up host names.");
    }

    /** The four bytes of a dotted-quad IPv4 address, or null when {@code text} is not one. */
    private static byte[] parseIpv4(String text) {
        byte[] bytes = new byte[4];
        int part = 0;
        int value = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                value = (value < 0 ? 0 : value * 10) + (c - '0');
                if (value > 255) {
                    return null;
                }
            } else if (c == '.' && value >= 0 && part < 3) {
                bytes[part++] = (byte) value;
                value = -1;
            } else {
                return null;
            }
        }
        if (part != 3 || value < 0) {
            return null;
        }
        bytes[3] = (byte) value;
        return bytes;
    }
}
