package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for IOUtilities path validation to ensure minimal overhead.
 * These tests are only run when performRelease=true to keep regular builds fast.
 */
public class IOUtilitiesPathValidationPerformanceTest {
    
    private List<File> tempFiles;
    private Method validateFilePathMethod;
    
    @BeforeEach
    public void setUp() throws Exception {
        tempFiles = new ArrayList<>();
        
        // Access the private validateFilePath method via reflection for testing
        validateFilePathMethod = IOUtilities.class.getDeclaredMethod("validateFilePath", File.class);
        validateFilePathMethod.setAccessible(true);
        
        // Create some temporary files for realistic testing
        for (int i = 0; i < 10; i++) {
            Path tempFile = Files.createTempFile("perf_test_", ".tmp");
            tempFiles.add(tempFile.toFile());
        }
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up temp files
        for (File f : tempFiles) {
            try {
                Files.deleteIfExists(f.toPath());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    public void testPathValidationPerformance() throws Exception {
        // Test different scales to find performance characteristics
        int[] testSizes = {100, 1000, 5000, 10000};
        
        for (int testSize : testSizes) {
            long startTime = System.nanoTime();
            
            // Test with a mix of file types for realistic performance
            for (int i = 0; i < testSize; i++) {
                File testFile = tempFiles.get(i % tempFiles.size());
                validateFilePathMethod.invoke(null, testFile);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            // Calculate operations per second
            double opsPerSecond = (testSize * 1000.0) / durationMs;
            
            System.out.printf("Path validation performance: %d validations in %d ms (%.0f ops/sec)%n", 
                             testSize, durationMs, opsPerSecond);
            
            // Performance requirements:
            // - Must complete within 1 second for any test size
            // - Should achieve at least 1000 validations per second
            assertTrue(durationMs < 1000, 
                      String.format("Test with %d validations took %d ms, exceeds 1 second limit", testSize, durationMs));
            
            // Only enforce throughput requirement for larger test sizes (overhead dominates small tests)
            if (testSize >= 1000) {
                assertTrue(opsPerSecond >= 1000, 
                          String.format("Performance too slow: %.0f ops/sec, expected >= 1000 ops/sec", opsPerSecond));
            }
            
            // If any test takes too long, skip larger tests
            if (durationMs > 500) {
                System.out.printf("Stopping performance tests early - %d validations took %d ms%n", testSize, durationMs);
                break;
            }
        }
    }
    
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    public void testPathValidationWithVariousPathTypes() throws Exception {
        // Test with different path types that might have different performance characteristics
        List<File> testFiles = new ArrayList<>();
        
        // Regular files
        testFiles.addAll(tempFiles);
        
        // Different path patterns
        testFiles.add(new File("simple.txt"));
        testFiles.add(new File("path/to/file.txt"));
        testFiles.add(new File("/absolute/path/file.txt"));
        testFiles.add(new File("../relative/path.txt")); // This should be detected as traversal
        testFiles.add(new File("very/long/path/with/many/segments/file.txt"));
        
        int iterations = 1000;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            for (File testFile : testFiles) {
                try {
                    validateFilePathMethod.invoke(null, testFile);
                } catch (Exception e) {
                    // Expected for traversal paths - just continue
                    if (e.getCause() instanceof SecurityException && 
                        e.getCause().getMessage().contains("Path traversal")) {
                        continue;
                    }
                    throw e;
                }
            }
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        int totalValidations = iterations * testFiles.size();
        double opsPerSecond = (totalValidations * 1000.0) / durationMs;
        
        System.out.printf("Mixed path validation: %d validations in %d ms (%.0f ops/sec)%n", 
                         totalValidations, durationMs, opsPerSecond);
        
        // Must complete within 1 second
        assertTrue(durationMs < 1000, 
                  String.format("Mixed path test took %d ms, exceeds 1 second limit", durationMs));
        
        // Should maintain reasonable performance
        assertTrue(opsPerSecond >= 500, 
                  String.format("Mixed path performance too slow: %.0f ops/sec", opsPerSecond));
    }
    
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    public void testPathValidationOverhead() throws Exception {
        // Measure the overhead of validation vs just creating File objects
        int iterations = 5000;
        
        // Test 1: Just create File objects (baseline)
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            File f = new File("test_file_" + i + ".txt");
            // Just touch the object to ensure it's not optimized away
            f.getName();
        }
        long baselineTime = System.nanoTime() - startTime;
        
        // Test 2: Create File objects and validate them
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            File f = new File("test_file_" + i + ".txt");
            validateFilePathMethod.invoke(null, f);
        }
        long validationTime = System.nanoTime() - startTime;
        
        long baselineMs = baselineTime / 1_000_000;
        long validationMs = validationTime / 1_000_000;
        long overheadMs = validationMs - baselineMs;
        double overheadPercentage = (overheadMs * 100.0) / baselineMs;
        
        System.out.printf("Validation overhead: baseline=%d ms, with_validation=%d ms, overhead=%d ms (%.1f%%)%n",
                         baselineMs, validationMs, overheadMs, overheadPercentage);
        
        // Both tests should complete quickly
        assertTrue(validationMs < 1000, "Validation test took too long: " + validationMs + " ms");
        
        // Overhead should be reasonable for security feature (< 5000% increase)
        // Note: High percentage is expected because baseline is extremely fast (just creating File objects)
        // The absolute time is what matters - validation should still be very fast
        assertTrue(overheadPercentage < 5000, 
                  String.format("Validation overhead too high: %.1f%%", overheadPercentage));
    }
    
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    public void testDisabledValidationPerformance() throws Exception {
        // Test performance when validation is disabled
        System.setProperty("io.path.validation.disabled", "true");
        
        try {
            int iterations = 10000;
            long startTime = System.nanoTime();
            
            for (int i = 0; i < iterations; i++) {
                File f = tempFiles.get(i % tempFiles.size());
                validateFilePathMethod.invoke(null, f);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            double opsPerSecond = (iterations * 1000.0) / durationMs;
            
            System.out.printf("Disabled validation performance: %d validations in %d ms (%.0f ops/sec)%n", 
                             iterations, durationMs, opsPerSecond);
            
            // When disabled, should be extremely fast
            assertTrue(durationMs < 100, "Disabled validation should be very fast: " + durationMs + " ms");
            assertTrue(opsPerSecond >= 10000, "Disabled validation should be very fast: " + opsPerSecond + " ops/sec");
            
        } finally {
            System.clearProperty("io.path.validation.disabled");
        }
    }
}