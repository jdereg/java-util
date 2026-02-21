package com.cedarsoftware.util;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Micro-benchmark to compare OR chain vs Set.contains() performance 
 * for checking 10 specific array classes.
 */
public class OrVsSetPerformanceTest {

    private static final Logger LOG = Logger.getLogger(OrVsSetPerformanceTest.class.getName());
    
    // The 10 classes from the code
    private static final Set<Class<?>> WRAPPER_ARRAY_CLASSES;
    static {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(String[].class);
        classes.add(Integer[].class);
        classes.add(Long[].class);
        classes.add(Double[].class);
        classes.add(Date[].class);
        classes.add(Boolean[].class);
        classes.add(Float[].class);
        classes.add(Short[].class);
        classes.add(Byte[].class);
        classes.add(Character[].class);
        WRAPPER_ARRAY_CLASSES = Collections.unmodifiableSet(classes);
    }
    
    // Test data - mix of classes that are and aren't in the set
    private static final Class<?>[] TEST_CLASSES = {
        String[].class,      // in set - first
        Integer[].class,     // in set - second  
        Double[].class,      // in set - middle
        Character[].class,   // in set - last
        Object[].class,      // not in set
        int[].class,         // not in set
        String.class,        // not in set
        List.class,          // not in set
        Map.class           // not in set
    };
    
    @Test
    void compareOrVsSetPerformance() {
        // Warmup
        for (int i = 0; i < 100_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                orChainMethod(clazz);
                setContainsMethod(clazz);
            }
        }
        
        // Test OR chain
        long startOr = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                orChainMethod(clazz);
            }
        }
        long endOr = System.nanoTime();
        long orTime = endOr - startOr;
        
        // Test Set contains
        long startSet = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                setContainsMethod(clazz);
            }
        }
        long endSet = System.nanoTime();
        long setTime = endSet - startSet;
        
        LOG.info("=== OR Chain vs Set.contains() Performance Comparison ===");
        LOG.info(String.format("OR chain time:    %,d ns (%.2f ms)", orTime, orTime / 1_000_000.0));
        LOG.info(String.format("Set contains time: %,d ns (%.2f ms)", setTime, setTime / 1_000_000.0));
        LOG.info(String.format("OR chain per operation: %.2f ns", orTime / 9_000_000.0));
        LOG.info(String.format("Set contains per operation: %.2f ns", setTime / 9_000_000.0));
        
        if (orTime < setTime) {
            double speedup = (double) setTime / orTime;
            LOG.info(String.format("OR chain is %.2fx faster", speedup));
        } else {
            double speedup = (double) orTime / setTime;
            LOG.info(String.format("Set contains is %.2fx faster", speedup));
        }
        
        // Verify both methods produce same results
        LOG.info("=== Correctness Verification ===");
        for (Class<?> clazz : TEST_CLASSES) {
            boolean orResult = orChainMethod(clazz);
            boolean setResult = setContainsMethod(clazz);
            LOG.info(String.format("%-20s: OR=%5s, Set=%5s %s",
                clazz.getSimpleName(), orResult, setResult,
                orResult == setResult ? "PASS" : "FAIL"));
        }
    }
    
    private static boolean orChainMethod(Class<?> clazz) {
        return clazz == String[].class || clazz == Integer[].class || clazz == Long[].class || 
               clazz == Double[].class || clazz == Date[].class || clazz == Boolean[].class || 
               clazz == Float[].class || clazz == Short[].class || clazz == Byte[].class || 
               clazz == Character[].class;
    }
    
    private static boolean setContainsMethod(Class<?> clazz) {
        return WRAPPER_ARRAY_CLASSES.contains(clazz);
    }
    
    @Test
    void analyzeDistribution() {
        // Test with different hit patterns to see if position matters
        LOG.info("=== Position Impact Analysis ===");
        
        Class<?>[] firstHit = {String[].class};        // First in OR chain
        Class<?>[] middleHit = {Date[].class};         // Middle in OR chain
        Class<?>[] lastHit = {Character[].class};      // Last in OR chain
        Class<?>[] noHit = {Object[].class};           // Not in set
        
        testPattern("First position hit", firstHit);
        testPattern("Middle position hit", middleHit);
        testPattern("Last position hit", lastHit);
        testPattern("No hit", noHit);
    }
    
    private void testPattern(String name, Class<?>[] classes) {
        // Warmup
        for (int i = 0; i < 50_000; i++) {
            for (Class<?> clazz : classes) {
                orChainMethod(clazz);
                setContainsMethod(clazz);
            }
        }
        
        // Test OR chain
        long startOr = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : classes) {
                orChainMethod(clazz);
            }
        }
        long endOr = System.nanoTime();
        
        // Test Set contains
        long startSet = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : classes) {
                setContainsMethod(clazz);
            }
        }
        long endSet = System.nanoTime();
        
        long orTime = endOr - startOr;
        long setTime = endSet - startSet;
        
        LOG.info(String.format("%-20s: OR=%6.2f ns, Set=%6.2f ns, OR is %.2fx %s",
            name,
            orTime / (double)(classes.length * 1_000_000),
            setTime / (double)(classes.length * 1_000_000),
            orTime < setTime ? (double) setTime / orTime : (double) orTime / setTime,
            orTime < setTime ? "faster" : "slower"
        ));
    }
}