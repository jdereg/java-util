package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

/**
 * Generates guaranteed unique, time-based, strictly monotonically increasing IDs for distributed systems.
 * Each ID encodes:
 * <ul>
 *   <li><b>Timestamp</b> - milliseconds since epoch</li>
 *   <li><b>Sequence number</b> - counter for multiple IDs within the same millisecond</li>
 *   <li><b>Server ID</b> - a 2-digit node identifier (00–99) appended to the ID</li>
 * </ul>
 *
 * <h2>Cluster Support</h2>
 * Server IDs are determined in the following priority order (0–99):
 * <ol>
 *   <li>Environment variable {@code JAVA_UTIL_CLUSTERID}</li>
 *   <li>Indirect environment variable via {@code SystemUtilities.getExternalVariable(JAVA_UTIL_CLUSTERID)} (value is another env var name)</li>
 *   <li>Kubernetes pod ordinal (last dash-separated token of {@code HOSTNAME})</li>
 *   <li>VMware Tanzu instance ID ({@code VMWARE_TANZU_INSTANCE_ID})</li>
 *   <li>Cloud Foundry instance index ({@code CF_INSTANCE_INDEX})</li>
 *   <li>Hash of {@code HOSTNAME} modulo 100</li>
 *   <li>Secure random (fallback)</li>
 * </ol>
 *
 * <h2>Available APIs</h2>
 * Two ID generation methods are provided:
 * <pre>
 * getUniqueId()
 * - Layout: [timestampMs][sequence-3-digits][serverId-2-digits]
 * - Capacity: up to 1,000 IDs per millisecond
 * - Positive range: until 4892-10-07T21:52:48.547Z
 *
 * getUniqueId19()
 * - Layout: [timestampMs][sequence-4-digits][serverId-2-digits]
 * - Capacity: up to 10,000 IDs per millisecond
 * - Positive range: until 2262-04-11T23:47:16.854Z
 * </pre>
 *
 * <h2>Guarantees</h2>
 * <ul>
 *   <li>IDs are strictly monotonically increasing within a process.</li>
 *   <li>Clock regressions are handled by not allowing time to go backwards in the ID stream.</li>
 *   <li>When per-millisecond capacity is exhausted, generation waits for the next millisecond (no “future” timestamps).</li>
 *   <li>Server ID is preserved in the last two digits of every ID; no arithmetic “+1” leaks into serverId.</li>
 *   <li><b>Note:</b> Strict uniqueness across JVM restarts on the same machine is best-effort unless you persist the last
 *       seen millisecond and restart from {@code max(persistedMs, now)}.</li>
 * </ul>
 *
 * <h2>Extraction</h2>
 * <ul>
 *   <li>{@link #getDate(long)} / {@link #getInstant(long)} operate on {@link #getUniqueId()} IDs.</li>
 *   <li>{@link #getDate19(long)} / {@link #getInstant19(long)} operate on {@link #getUniqueId19()} IDs.</li>
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
public final class UniqueIdGenerator {
    // ---- Public environment variable names ----
    public static final String JAVA_UTIL_CLUSTERID = "JAVA_UTIL_CLUSTERID";
    public static final String KUBERNETES_POD_NAME = "HOSTNAME";                 // K8s commonly injects pod name into HOSTNAME
    public static final String TANZU_INSTANCE_ID   = "VMWARE_TANZU_INSTANCE_ID";
    public static final String CF_INSTANCE_INDEX   = "CF_INSTANCE_INDEX";

    // ---- Logging ----
    private static final Logger LOG = Logger.getLogger(UniqueIdGenerator.class.getName());
    static { LoggingConfig.init(); }

    // ---- Encoding constants ----
    // IDs end with two digits reserved for serverId; increment step = 100 to preserve that suffix.
    private static final long SEQUENCE_STEP     = 100L;

    private static final long FACTOR_16         = 100_000L;     // [timestampMs]*100_000 + [seq]*100 + [serverId]
    private static final int  SEQ_LIMIT_16      = 1_000;        // 000–999 per millisecond

    private static final long FACTOR_19         = 1_000_000L;   // [timestampMs]*1_000_000 + [seq]*100 + [serverId]
    private static final int  SEQ_LIMIT_19      = 10_000;       // 0000–9999 per millisecond

    // ---- State (lock-free) ----
    private static final AtomicLong LAST_ID_16 = new AtomicLong(0L);
    private static final AtomicLong LAST_ID_19 = new AtomicLong(0L);

    // ---- Server ID (00–99) ----
    private static final int serverId;

    private UniqueIdGenerator() { }

    static {
        String setVia;

        int id = getServerIdFromVarName(JAVA_UTIL_CLUSTERID);
        setVia = "environment variable: " + JAVA_UTIL_CLUSTERID;

        if (id == -1) {
            // Indirect: JAVA_UTIL_CLUSTERID contains the *name* of another env var
            String indirection = SystemUtilities.getExternalVariable(JAVA_UTIL_CLUSTERID);
            if (StringUtilities.hasContent(indirection)) {
                id = getServerIdFromVarName(indirection);
                if (id != -1) {
                    setVia = "indirect environment variable: " + indirection;
                }
            }
        }

        if (id == -1) {
            // K8s pod ordinal from HOSTNAME (e.g., service-abc-0 -> 0)
            String podName = SystemUtilities.getExternalVariable(KUBERNETES_POD_NAME);
            if (StringUtilities.hasContent(podName)) {
                try {
                    int dash = podName.lastIndexOf('-');
                    if (dash >= 0 && dash + 1 < podName.length()) {
                        String ordinal = podName.substring(dash + 1);
                        id = Math.floorMod(parseInt(ordinal), 100);
                        setVia = "Kubernetes pod ordinal from HOSTNAME: " + podName;
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        if (id == -1) {
            id = getServerIdFromVarName(TANZU_INSTANCE_ID);
            if (id != -1) setVia = "VMware Tanzu instance ID";
        }

        if (id == -1) {
            id = getServerIdFromVarName(CF_INSTANCE_INDEX);
            if (id != -1) setVia = "Cloud Foundry instance index";
        }

        if (id == -1) {
            // Hostname hash -> 0..99 (print abbreviated hash only)
            String hostName = SystemUtilities.getExternalVariable("HOSTNAME");
            if (StringUtilities.hasContent(hostName)) {
                try {
                    String sha256 = EncryptionUtilities.calculateSHA256Hash(hostName.getBytes(StandardCharsets.UTF_8));
                    String head8  = sha256.substring(0, 8);
                    int hashInt   = Integer.parseUnsignedInt(head8, 16);
                    id = Math.floorMod(hashInt, 100);
                    setVia = "hostname hash: " + hostName + " (sha256[0..8]=" + head8 + ")";
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        if (id == -1) {
            // Final fallback: secure random (runs once at startup)
            SecureRandom random = new SecureRandom();
            id = Math.floorMod(random.nextInt(), 100);
            setVia = "SecureRandom";
        }

        serverId = id;
        LOG.info("java-util using node id=" + id + " (last two digits of UniqueId). Set via " + setVia);
    }

    /**
     * Generates a unique, strictly monotonically increasing ID with millisecond precision.
     * <p>
     * Layout: {@code [timestampMs][sequence-3-digits][serverId-2-digits]}.
     * Supports up to 1,000 IDs per millisecond. When capacity is exhausted, waits for the next millisecond.
     * <p>
     * Positive range until {@code 4892-10-07T21:52:48.547Z}.
     *
     * @return unique time-based ID as a {@code long}
     */
    public static long getUniqueId() {
        return nextId(LAST_ID_16, FACTOR_16, SEQ_LIMIT_16);
    }

    /**
     * Generates a unique, strictly monotonically increasing 19-digit ID optimized for higher throughput.
     * <p>
     * Layout: {@code [timestampMs][sequence-4-digits][serverId-2-digits]}.
     * Supports up to 10,000 IDs per millisecond. When capacity is exhausted, waits for the next millisecond.
     * <p>
     * Positive range until {@code 2262-04-11T23:47:16.854Z}.
     *
     * @return unique time-based ID as a {@code long}
     */
    public static long getUniqueId19() {
        return nextId(LAST_ID_19, FACTOR_19, SEQ_LIMIT_19);
    }

    /**
     * Extract timestamp as {@link Date} from {@link #getUniqueId()} values.
     */
    public static Date getDate(long uniqueId) {
        return new Date(uniqueId / FACTOR_16);
    }

    /**
     * Extract timestamp as {@link Date} from {@link #getUniqueId19()} values.
     */
    public static Date getDate19(long uniqueId19) {
        return new Date(uniqueId19 / FACTOR_19);
    }

    /**
     * Extract timestamp as {@link Instant} from {@link #getUniqueId()} values.
     * @throws IllegalArgumentException if {@code uniqueId} is negative (out of supported range)
     */
    public static Instant getInstant(long uniqueId) {
        if (uniqueId < 0) {
            throw new IllegalArgumentException("Invalid uniqueId: must be non-negative");
        }
        return Instant.ofEpochMilli(uniqueId / FACTOR_16);
    }

    /**
     * Extract timestamp as {@link Instant} from {@link #getUniqueId19()} values.
     * @throws IllegalArgumentException if {@code uniqueId19} is negative (out of supported range)
     */
    public static Instant getInstant19(long uniqueId19) {
        if (uniqueId19 < 0) {
            throw new IllegalArgumentException("Invalid uniqueId19: must be non-negative");
        }
        return Instant.ofEpochMilli(uniqueId19 / FACTOR_19);
    }

    /**
     * Returns the configured server ID (00–99).
     */
    public static int getServerIdConfigured() {
        return serverId;
    }

    // -------------------- Internals --------------------

    /**
     * Lock-free generator that preserves the decimal structure and the serverId suffix.
     * It also never advances the timestamp into the future: if a millisecond's sequence is exhausted, we wait.
     */
    private static long nextId(AtomicLong lastId, long factor, int perMsLimit) {
        long now = currentTimeMillis();
        for (;;) {
            final long prev = lastId.get();

            // Compute the millisecond to use: never go backwards relative to last issued ID
            final long prevMs = prev / factor;
            final long baseMs = Math.max(now, prevMs);

            final long base = baseMs * factor + serverId;

            // If previous ID was in this same ms, bump sequence by +1 step; else start at sequence=0
            long cand = base;
            if (prev >= base) {
                long seqIndex = ((prev - base) / SEQUENCE_STEP) + 1; // next sequence slot
                cand = base + (seqIndex * SEQUENCE_STEP);

                // Sequence capacity exhausted for this millisecond? Wait for next ms and retry.
                if (seqIndex >= perMsLimit) {
                    // Block outside of any locks; we will recalc after the clock ticks.
                    final long nextMs = waitForNextMillis(baseMs);
                    now = nextMs; // update now and loop
                    continue;
                }
            }

            if (lastId.compareAndSet(prev, cand)) {
                return cand;
            }

            // Mild backoff on contention
            onSpinWait();
        }
    }

    /**
     * Wait until the system clock advances beyond the given millisecond.
     * Uses short spin with occasional nanosleep to reduce CPU.
     */
    private static long waitForNextMillis(long lastMs) {
        long ts = currentTimeMillis();
        int spins = 0;

        while (ts <= lastMs) {
            // Short spin phases interleaved with a tiny park to avoid burning CPU
            for (int i = 0; i < 64; i++) {
                onSpinWait();
                ts = currentTimeMillis();
                if (ts > lastMs) {
                    return ts;
                }
            }
            // ~100 microseconds backoff
            LockSupport.parkNanos(100_000L);
            ts = currentTimeMillis();
            spins++;
            if (spins > 64) {
                // Safety valve: if the clock is stuck (virtualized env), back off a bit more
                LockSupport.parkNanos(1_000_000L);
            }
        }
        return ts;
    }

    private static final java.lang.reflect.Method ON_SPIN_WAIT_METHOD;
    static {
        java.lang.reflect.Method method = null;
        try {
            method = Thread.class.getMethod("onSpinWait");
        } catch (NoSuchMethodException e) {
            // Java 8 or older
        }
        ON_SPIN_WAIT_METHOD = method;
    }
    
    private static void onSpinWait() {
        // Java 9+ hint; safe no-op if older runtime
        if (ON_SPIN_WAIT_METHOD != null) {
            try {
                ON_SPIN_WAIT_METHOD.invoke(null);
            } catch (Exception ignored) {
                // no-op
            }
        }
    }

    private static int getServerIdFromVarName(String externalVarName) {
        try {
            String id = SystemUtilities.getExternalVariable(externalVarName);
            if (StringUtilities.isEmpty(id)) {
                return -1;
            }
            int parsedId = parseInt(id.trim());
            // Safe abs for Integer.MIN_VALUE
            int normalized = (int) Math.abs((long) parsedId);
            return Math.floorMod(normalized, 100);
        } catch (NumberFormatException | SecurityException e) {
            LOG.fine("Unable to retrieve server id from " + externalVarName + ": " + e.getMessage());
            return -1;
        }
    }
}
