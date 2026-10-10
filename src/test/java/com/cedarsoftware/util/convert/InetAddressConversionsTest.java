package com.cedarsoftware.util.convert;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InetAddress conversions (4.113.0). An InetAddress is its IP address; its Map form keeps a host name it already
 * carries. Nothing is looked up: only IP literals are read, and a name travels beside its address.
 * <p>
 * The pairs are in ConverterEverythingTest too (loadInetAddressTests), round-tripped through the test-scope json-io;
 * this class keeps the checks a pair cannot state: the address family, and that nothing is looked up.
 */
class InetAddressConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void ipv4AndIpv6LiteralsRoundTrip() throws Exception {
        InetAddress v4 = converter.convert("10.0.0.1", InetAddress.class);
        assertInstanceOf(Inet4Address.class, v4);
        assertEquals(InetAddress.getByAddress(new byte[]{10, 0, 0, 1}), v4);
        assertEquals("10.0.0.1", converter.convert(v4, String.class));

        InetAddress v6 = converter.convert("2001:db8::1", InetAddress.class);
        assertInstanceOf(Inet6Address.class, v6);
        assertEquals("2001:db8:0:0:0:0:0:1", converter.convert(v6, String.class));
        assertEquals(v6, converter.convert(converter.convert(v6, String.class), InetAddress.class));
        assertEquals(InetAddress.getByName("::1"), converter.convert("[::1]", InetAddress.class));

        assertInstanceOf(Inet4Address.class, converter.convert("192.168.1.20", Inet4Address.class));
        assertEquals(v4, converter.convert(new StringBuilder(" 10.0.0.1 "), InetAddress.class));
        assertNull(converter.convert("", InetAddress.class));
        assertNull(converter.convert(null, InetAddress.class));
    }

    @Test
    void aHostNameTravelsBesideItsAddressUnresolved() throws Exception {
        InetAddress named = InetAddress.getByAddress("build-server", new byte[]{10, 0, 0, 7});
        Map<?, ?> map = converter.convert(named, Map.class);
        assertEquals("build-server", map.get("host"));
        assertEquals("10.0.0.7", map.get("address"));

        InetAddress back = converter.convert(map, InetAddress.class);
        assertEquals("build-server/10.0.0.7", back.toString());
        assertEquals(named, back);

        // As InetAddress.toString() writes it
        assertEquals("build-server/10.0.0.7", converter.convert("build-server/10.0.0.7", InetAddress.class).toString());

        // An address without a name has no "host" entry
        Map<?, ?> unnamed = converter.convert(InetAddress.getByAddress(new byte[]{10, 0, 0, 8}), Map.class);
        assertEquals(1, unnamed.size());
        assertEquals("10.0.0.8", unnamed.get("address"));
        Map<String, Object> value = new HashMap<>();
        value.put("value", "10.0.0.8");
        assertEquals(InetAddress.getByAddress(new byte[]{10, 0, 0, 8}), converter.convert(value, InetAddress.class));
    }

    /**
     * No name is looked up: "localhost" would resolve, and "10.0.0.300" is not a literal InetAddress.getByName()
     * accepts, so it would try DNS. Both are refused before any lookup.
     */
    @ParameterizedTest
    @ValueSource(strings = {"localhost", "example.com", "10.0.0.300", "10.0.0", "1.2.3.4.5", "10..0.1",
            "h/example.com"})
    void anythingButAnIpAddressIsRefusedWithoutALookup(String text) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(text, InetAddress.class));
        assertTrue(e.getMessage().startsWith("Not an IP address"), e.getMessage());
    }

    @Test
    void anInvalidIpv6LiteralFailsAsInvalid() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert("2001:db8::zz", InetAddress.class));
        assertTrue(e.getMessage().startsWith("Not an IP address"), e.getMessage());
    }

    @Test
    void anInetAddressConvertsToItself() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[]{10, 0, 0, 1});
        assertSame(address, converter.convert(address, InetAddress.class));
    }
}
