package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * build() finds a configuration's generated class without taking a lock. The class used to be published before its
 * configuration was injected into it, so a thread building the same new configuration at that moment got a class
 * with compactSize 0 and no ordering: it failed ("compactSize() must be >= 2") or misbehaved. In a probe, 441 of
 * 2,400 such builds failed. The configuration is now injected before the class is published.
 */
class CompactMapTemplateRaceTest {

    @Test
    void threadsBuildingANewConfigurationTogetherAllGetAWorkingMap() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            AtomicInteger failures = new AtomicInteger();
            for (int round = 0; round < 40; round++) {
                String singleKey = "race" + round + "_" + System.nanoTime();     // a configuration no one has built
                CyclicBarrier barrier = new CyclicBarrier(threads);
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        barrier.await();
                        CompactMap<String, Object> map = CompactMap.<String, Object>builder()
                                .singleValueKey(singleKey).compactSize(3).insertionOrder().build();
                        for (int i = 0; i < 5; i++) {
                            map.put("k" + i, i);        // past compactSize, getNewMap() reads the configuration
                        }
                        if (map.size() != 5 || !"k0".equals(map.keySet().iterator().next())) {
                            failures.incrementAndGet();
                        }
                        return null;
                    }));
                }
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        failures.incrementAndGet();
                    }
                }
            }
            assertEquals(0, failures.get(), "builds that failed or misbehaved");
        } finally {
            pool.shutdownNow();
        }
    }
}
