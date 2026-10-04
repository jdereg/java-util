package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * UniqueIdGenerator behavior found by review. Each test runs a fresh copy of the generator in its own class loader,
 * so it can set the generator's state without disturbing the ids the rest of the JVM gets.
 */
class UniqueIdGeneratorBehaviorTest {

    /**
     * After the wall clock steps back (an NTP correction, a VM snapshot restored), ids keep coming once a millisecond's
     * capacity is used. They are never issued for a time before ids already issued, so the generator waited for the
     * wall clock to catch up: every caller blocked for as long as the clock had stepped back, an hour for an hour. It
     * now moves on by one millisecond of its own per real millisecond.
     */
    @Test
    void aClockThatSteppedBackDoesNotStallIdGeneration() throws Exception {
        URL[] javaUtil = {new File("target/classes").toURI().toURL()};
        try (URLClassLoader fresh = new URLClassLoader(javaUtil, ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> generator = Class.forName(UniqueIdGenerator.class.getName(), true, fresh);
            Field lastIdField = generator.getDeclaredField("LAST_ID_16");
            lastIdField.setAccessible(true);
            AtomicLong lastId = (AtomicLong) lastIdField.get(null);
            Method getUniqueId = generator.getMethod("getUniqueId");
            int serverId = (Integer) generator.getMethod("getServerIdConfigured").invoke(null);

            long issuedUpTo = System.currentTimeMillis() + 3000;      // ids issued before the clock stepped back 3 s
            lastId.set(issuedUpTo * 100_000L + serverId);
            long start = System.nanoTime();
            long previous = lastId.get();
            for (int i = 0; i < 1_100; i++) {                         // a millisecond holds 1,000
                long id = (Long) getUniqueId.invoke(null);
                assertTrue(id > previous, "ids keep increasing");
                previous = id;
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(elapsedMs < 1000, "1,100 ids after the clock stepped back 3 s took " + elapsedMs + " ms");
        }
    }

    private static int serverIdFor(String hostName, String kubernetesServiceHost) throws Exception {
        String[] keys = {"HOSTNAME", "KUBERNETES_SERVICE_HOST"};
        String[] values = {hostName, kubernetesServiceHost};
        String[] saved = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            saved[i] = System.getProperty(keys[i]);
            if (values[i] == null) {
                System.clearProperty(keys[i]);
            } else {
                System.setProperty(keys[i], values[i]);
            }
        }
        URL[] javaUtil = {new File("target/classes").toURI().toURL()};
        try (URLClassLoader fresh = new URLClassLoader(javaUtil, ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> generator = Class.forName(UniqueIdGenerator.class.getName(), true, fresh);
            return (Integer) generator.getMethod("getServerIdConfigured").invoke(null);
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (saved[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], saved[i]);
                }
            }
        }
    }

    /**
     * A hostname ending in -digits is read as a Kubernetes pod ordinal only inside Kubernetes. Anywhere, it was:
     * EC2 hosts ip-10-0-1-23, ip-10-0-2-123 and ip-172-31-40-223 all got server id 23, and db-prod-01 and
     * app-prod-101 both got 1, so their ids could collide. Outside Kubernetes the hostname is hashed, as any other.
     */
    @Test
    void aHostnameIsReadAsAPodOrdinalOnlyInsideKubernetes() throws Exception {
        assumeTrue(System.getenv("HOSTNAME") == null && System.getenv("JAVA_UTIL_CLUSTERID") == null
                && System.getenv("KUBERNETES_SERVICE_HOST") == null && System.getenv("VMWARE_TANZU_INSTANCE_ID") == null
                && System.getenv("CF_INSTANCE_INDEX") == null, "the environment sets a server id itself");
        for (String host : new String[]{"ip-10-0-1-23", "ip-10-0-2-123", "ip-172-31-40-223", "app-prod-101"}) {
            assertEquals(Math.floorMod(host.hashCode(), 100), serverIdFor(host, null), host + " outside Kubernetes");
        }
        assertEquals(3, serverIdFor("web-3", "10.96.0.1"), "a StatefulSet pod's ordinal");
        assertEquals(23, serverIdFor("db-123", "10.96.0.1"), "a pod ordinal, modulo 100");
    }
}
