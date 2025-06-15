package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import com.cedarsoftware.util.LoggingConfig;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;

/**
 * Generates guaranteed unique, time-based, monotonically increasing IDs within a distributed environment.
 * Each ID encodes three pieces of information:
 * <ul>
 *   <li>Timestamp - milliseconds since epoch (1970)</li>
 *   <li>Sequence number - counter for multiple IDs within same millisecond</li>
 *   <li>Server ID - unique identifier (0-99) for machine/instance in cluster</li>
 * </ul>
 *
 * <h2>Cluster Support</h2>
 * Server IDs are determined in the following priority order:
 * <ol>
 *   <li>Environment variable JAVA_UTIL_CLUSTERID (0-99)</li>
 *   <li>Kubernetes Pod ID (extracted from metadata)</li>
 *   <li>VMware Tanzu instance ID</li>
 *   <li>Cloud Foundry instance index (CF_INSTANCE_INDEX)</li>
 *   <li>Hash of hostname modulo 100</li>
 *   <li>Random number (0-99) if all else fails</li>
 * </ol>
 * 
 * <h2>Available APIs</h2>
 * Two ID generation methods are provided with different characteristics:
 * <pre>
 * getUniqueId()
 * - Format: timestampMs(13-14 digits).sequence(3 digits).serverId(2 digits)
 * - Rate: Up to 1,000 IDs per millisecond
 * - Range: Until year 5138
 * - Example: 1234567890123456.789.99
 *
 * getUniqueId19()
 * - Format: timestampMs(13 digits).sequence(4 digits).serverId(2 digits)
 * - Rate: Up to 10,000 IDs per millisecond
 * - Range: Until year 2286 (positive values)
 * - Example: 1234567890123.9999.99
 * </pre>
 *
 * <h2>Guarantees</h2>
 * The generator provides the following guarantees:
 * <ul>
 *   <li>IDs are unique across JVM restarts on the same machine</li>
 *   <li>IDs are unique across machines when proper server IDs are configured</li>
 *   <li>IDs are strictly monotonically increasing (each ID > previous ID)</li>
 *   <li>System clock regression is handled gracefully</li>
 *   <li>High sequence numbers cause waiting for next millisecond</li>
 * </ul>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Roger Judd (@HonorKnight on GitHub) for adding code to ensure increasing order
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
@SuppressWarnings("unchecked")
public final class UniqueIdGenerator {
    public static final String JAVA_UTIL_CLUSTERID = "JAVA_UTIL_CLUSTERID";
    public static final String KUBERNETES_POD_NAME = "HOSTNAME";
    public static final String TANZU_INSTANCE_ID = "VMWARE_TANZU_INSTANCE_ID";
    public static final String CF_INSTANCE_INDEX = "CF_INSTANCE_INDEX";

    private UniqueIdGenerator() {
    }

    private static final Lock lock = new ReentrantLock();
    private static final Lock lock19 = new ReentrantLock();
    private static final Logger LOG = Logger.getLogger(UniqueIdGenerator.class.getName());
    static { LoggingConfig.init(); }
    private static int count = 0;
    private static int count2 = 0;
    private static long lastTimeMillis = 0;
    private static long lastTimeMillis19 = 0;
    private static long lastGeneratedId = 0;
    private static long lastGeneratedId19 = 0;
    private static final int serverId;

    static {
        String setVia;

        // Try JAVA_UTIL_CLUSTERID first (maintain backward compatibility)
        int id = getServerId(JAVA_UTIL_CLUSTERID);
        setVia = "environment variable: " + JAVA_UTIL_CLUSTERID;

        if (id == -1) {
            // Try indirect environment variable
            String envName = SystemUtilities.getExternalVariable(JAVA_UTIL_CLUSTERID);
            if (StringUtilities.hasContent(envName)) {
                String envValue = SystemUtilities.getExternalVariable(envName);
                id = getServerId(envValue);
                setVia = "environment variable: " + envName;
            }
        }

        if (id == -1) {
            // Try Kubernetes Pod ID
            String podName = SystemUtilities.getExternalVariable(KUBERNETES_POD_NAME);
            if (StringUtilities.hasContent(podName)) {
                // Extract ordinal from pod name (typically ends with -0, -1, etc.)
                try {
                    if (podName.contains("-")) {
                        String ordinal = podName.substring(podName.lastIndexOf('-') + 1);
                        id = abs(parseInt(ordinal)) % 100;
                        setVia = "Kubernetes pod name: " + podName;
                    }
                } catch (Exception ignored) {
                    // Fall through to next strategy if pod name parsing fails
                }
            }
        }

        if (id == -1) {
            // Try Tanzu instance ID
            id = getServerId(TANZU_INSTANCE_ID);
            if (id != -1) {
                setVia = "VMware Tanzu instance ID";
            }
        }

        if (id == -1) {
            // Try Cloud Foundry instance index
            id = getServerId(CF_INSTANCE_INDEX);
            if (id != -1) {
                setVia = "Cloud Foundry instance index";
            }
        }

        if (id == -1) {
            // Try hostname hash
            String hostName = SystemUtilities.getExternalVariable("HOSTNAME");
            if (StringUtilities.hasContent(hostName)) {
                String hostnameSha256 = EncryptionUtilities.calculateSHA256Hash(hostName.getBytes(StandardCharsets.UTF_8));
                // Convert first 8 characters of the hash to an integer
                try {
                    String hashSegment = hostnameSha256.substring(0, 8);
                    int hashInt = Integer.parseUnsignedInt(hashSegment, 16);
                    id = hashInt % 100;
                    setVia = "hostname hash: " + hostName + " (" + hostnameSha256 + ")";
                } catch (Exception ignored) {
                    // Fall through to next strategy if pod name parsing fails
                }
            }
        }

        if (id == -1) {
            // Final fallback - use secure random
            SecureRandom random = new SecureRandom();
            id = abs(random.nextInt()) % 100;
            setVia = "SecureRandom";
        }

        LOG.info("java-util using node id=" + id + " for last two digits of generated unique IDs. Set using " + setVia);
        serverId = id;
    }

    /**
     * Generates a unique, monotonically increasing ID with millisecond precision that's cluster-safe.
     *
     * <h2>ID Format</h2>
     * The returned long value contains three components:
     * <pre>
     * [timestamp: 13-14 digits][sequence: 3 digits][serverId: 2 digits]
     * Example: 12345678901234.999.99 (dots for clarity, actual value has no dots)
     * </pre>
     *
     * <h2>Characteristics</h2>
     * <ul>
     *   <li>Supports up to 1,000 unique IDs per millisecond (sequence 000-999)</li>
     *   <li>Generates positive values until year 5138</li>
     *   <li>Guaranteed monotonically increasing even across millisecond boundaries</li>
     *   <li>Thread-safe through internal locking</li>
     *   <li>Handles system clock regression gracefully</li>
     *   <li>Blocks when sequence number exhausted within a millisecond</li>
     * </ul>
     *
     * @return A unique, time-based ID encoded as a long value
     * @see #getDate(long) To extract the timestamp from the generated ID
     */
    public static long getUniqueId() {
        lock.lock();
        try {
            long currentTime = currentTimeMillis();
            if (currentTime < lastTimeMillis) {
                // Clock went backwards - use last time
                currentTime = lastTimeMillis;
            }

            if (currentTime == lastTimeMillis) {
                count++;
                if (count >= 1000) {
                    // Wait for next millisecond
                    currentTime = waitForNextMillis(lastTimeMillis);
                    count = 0;
                }
            } else {
                count = 0;
                lastTimeMillis = currentTime;
            }

            long newId = currentTime * 100_000 + count * 100L + serverId;
            if (newId <= lastGeneratedId) {
                newId = lastGeneratedId + 1;
            }

            lastGeneratedId = newId;
            return newId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Generates a unique, monotonically increasing 19-digit ID optimized for higher throughput.
     *
     * <h2>ID Format</h2>
     * The returned long value contains three components:
     * <pre>
     * [timestamp: 13 digits][sequence: 4 digits][serverId: 2 digits]
     * Example: 1234567890123.9999.99 (dots for clarity, actual value has no dots)
     * </pre>
     *
     * <h2>Characteristics</h2>
     * <ul>
     *   <li>Supports up to 10,000 unique IDs per millisecond (sequence 0000-9999)</li>
     *   <li>Generates positive values until year 2286 (after which values may be negative)</li>
     *   <li>Guaranteed monotonically increasing even across millisecond boundaries</li>
     *   <li>Thread-safe through internal locking</li>
     *   <li>Handles system clock regression gracefully</li>
     *   <li>Blocks when sequence number exhausted within a millisecond</li>
     * </ul>
     *
     * <h2>Performance Comparison</h2>
     * This method is optimized for higher throughput compared to {@link #getUniqueId()}:
     * <ul>
     *   <li>Supports 10x more IDs per millisecond (10,000 vs 1,000)</li>
     *   <li>Trades timestamp range for increased sequence capacity</li>
     *   <li>Recommended for high-throughput scenarios through year 2286</li>
     * </ul>
     *
     * @return A unique, time-based ID encoded as a long value
     * @see #getDate19(long) To extract the timestamp from the generated ID
     */
    public static long getUniqueId19() {
        lock19.lock();
        try {
            long currentTime = currentTimeMillis();
            if (currentTime < lastTimeMillis19) {
                // Clock went backwards - use last time
                currentTime = lastTimeMillis19;
            }

            if (currentTime == lastTimeMillis19) {
                count2++;
                if (count2 >= 10_000) {
                    // Wait for next millisecond
                    currentTime = waitForNextMillis(lastTimeMillis19);
                    count2 = 0;
                }
            } else {
                count2 = 0;
                lastTimeMillis19 = currentTime;
            }

            long newId = currentTime * 1_000_000 + count2 * 100L + serverId;
            if (newId <= lastGeneratedId19) {
                newId = lastGeneratedId19 + 1;
            }

            lastGeneratedId19 = newId;
            return newId;
        } finally {
            lock19.unlock();
        }
    }

    /**
     * Extracts the date-time from an ID generated by {@link #getUniqueId()}.
     *
     * @param uniqueId A unique ID previously generated by {@link #getUniqueId()}
     * @return The Date representing when the ID was generated, accurate to the millisecond
     * @throws IllegalArgumentException if the ID was not generated by {@link #getUniqueId()}
     */
    public static Date getDate(long uniqueId) {
        return new Date(uniqueId / 100_000);
    }

    /**
     * Extracts the date-time from an ID generated by {@link #getUniqueId19()}.
     *
     * @param uniqueId19 A unique ID previously generated by {@link #getUniqueId19()}
     * @return The Date representing when the ID was generated, accurate to the millisecond
     * @throws IllegalArgumentException if the ID was not generated by {@link #getUniqueId19()}
     */
    public static Date getDate19(long uniqueId19) {
        return new Date(uniqueId19 / 1_000_000);
    }

    /**
     * Extracts the date-time from an ID generated by {@link #getUniqueId()}.
     *
     * @param uniqueId A unique ID previously generated by {@link #getUniqueId()}
     * @return The Instant representing when the ID was generated, accurate to the millisecond
     * @throws IllegalArgumentException if the ID was not generated by {@link #getUniqueId()}
     */
    public static Instant getInstant(long uniqueId) {
        if (uniqueId < 0) {
            throw new IllegalArgumentException("Invalid uniqueId: must be positive");
        }
        return Instant.ofEpochMilli(uniqueId / 100_000);
    }

    /**
     * Extracts the date-time from an ID generated by {@link #getUniqueId19()}.
     *
     * @param uniqueId19 A unique ID previously generated by {@link #getUniqueId19()}
     * @return The Instant representing when the ID was generated, accurate to the millisecond
     * @throws IllegalArgumentException if the ID was not generated by {@link #getUniqueId19()}
     */
    public static Instant getInstant19(long uniqueId19) {
        if (uniqueId19 < 0) {
            throw new IllegalArgumentException("Invalid uniqueId19: must be positive");
        }
        return Instant.ofEpochMilli(uniqueId19 / 1_000_000);
    }

    private static long waitForNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            LockSupport.parkNanos(1000); // small pause to reduce CPU usage
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private static int getServerId(String externalVarName) {
        try {
            String id = SystemUtilities.getExternalVariable(externalVarName);
            if (StringUtilities.isEmpty(id)) {
                return -1;
            }
            int parsedId = parseInt(id);
            return (int) (Math.abs((long) parsedId) % 100);
        } catch (NumberFormatException | SecurityException e) {
            LOG.fine("Unable to retrieve server id from " + externalVarName + ": " + e.getMessage());
            return -1;
        }
    }
}
