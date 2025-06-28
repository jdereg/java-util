package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for unbounded memory allocation protection in IOUtilities.
 * Verifies that size limits prevent DoS attacks through excessive memory consumption.
 * 
 * NOTE: Only inputStreamToBytes() and uncompressBytes() have size limits.
 * Transfer methods do NOT have size limits as they are used for large file transfers between servers.
 */
public class IOUtilitiesUnboundedMemoryTest {
    
    private String originalMaxStreamSize;
    private String originalMaxDecompressionSize;
    
    @BeforeEach
    public void setUp() {
        // Store original system properties
        originalMaxStreamSize = System.getProperty("io.max.stream.size");
        originalMaxDecompressionSize = System.getProperty("io.max.decompression.size");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system properties
        restoreProperty("io.max.stream.size", originalMaxStreamSize);
        restoreProperty("io.max.decompression.size", originalMaxDecompressionSize);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    public void testInputStreamToBytesWithSizeLimit() {
        // Create test data larger than our limit
        byte[] testData = new byte[1000];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData)) {
            // Test normal conversion within limit
            byte[] result = IOUtilities.inputStreamToBytes(new ByteArrayInputStream(testData), 2000);
            assertArrayEquals(testData, result);
        } catch (IOException e) {
            fail("IOException should not occur in test setup: " + e.getMessage());
        }
        
        // Test size limit enforcement
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData)) {
            Exception exception = assertThrows(Exception.class, () -> {
                IOUtilities.inputStreamToBytes(input, 500); // Smaller than testData
            });
            assertTrue(exception instanceof IOException);
            assertTrue(exception.getMessage().contains("Stream exceeds maximum allowed size"));
        } catch (IOException e) {
            fail("IOException should not occur in test setup: " + e.getMessage());
        }
    }
    
    @Test
    public void testUncompressBytesWithSizeLimit() {
        // Create a simple compressed byte array
        byte[] originalData = "Hello, World! This is test data for compression.".getBytes();
        byte[] compressedData = IOUtilities.compressBytes(originalData);
        
        // Test normal decompression works
        byte[] decompressed = IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, 1000);
        assertArrayEquals(originalData, decompressed);
        
        // Test size limit enforcement - try to decompress with very small limit
        Exception exception = assertThrows(Exception.class, () -> {
            IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, 10);
        });
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    public void testUncompressBytesNonGzippedData() {
        // Test that non-GZIP data is returned unchanged regardless of size limit
        byte[] plainData = "This is not compressed data".getBytes();
        byte[] result = IOUtilities.uncompressBytes(plainData, 0, plainData.length, 10);
        assertArrayEquals(plainData, result);
    }
    
    @Test
    public void testDefaultSizeLimitsFromSystemProperties() {
        // Test that system properties are respected for default limits
        System.setProperty("io.max.stream.size", "500");
        System.setProperty("io.max.decompression.size", "500");
        
        try {
            // Create large data that will definitely exceed the limits
            byte[] largeData = new byte[2000];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }
            
            // Test inputStreamToBytes with system property limit
            try (ByteArrayInputStream input = new ByteArrayInputStream(largeData)) {
                Exception streamException = assertThrows(Exception.class, () -> {
                    IOUtilities.inputStreamToBytes(input);
                });
                assertTrue(streamException instanceof IOException);
                assertTrue(streamException.getMessage().contains("Stream exceeds maximum allowed size"));
            } catch (IOException e) {
                fail("IOException should not occur in test setup: " + e.getMessage());
            }
            
            // Test uncompressBytes with system property limit
            byte[] compressedData = IOUtilities.compressBytes(largeData);
            Exception decompressionException = assertThrows(Exception.class, () -> {
                IOUtilities.uncompressBytes(compressedData);
            });
            assertTrue(decompressionException instanceof RuntimeException);
            assertTrue(decompressionException.getCause() instanceof IOException);
            assertTrue(decompressionException.getCause().getMessage().contains("Stream exceeds maximum allowed size"));
            
        } finally {
            // Clean up system properties in case of test failure
            System.clearProperty("io.max.stream.size");
            System.clearProperty("io.max.decompression.size");
        }
    }
    
    @Test
    public void testInvalidSizeLimits() {
        // Test that invalid size limits are rejected
        byte[] testData = "test".getBytes();
        byte[] compressedData = IOUtilities.compressBytes(testData);
        
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtilities.inputStreamToBytes(new ByteArrayInputStream(testData), 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtilities.inputStreamToBytes(new ByteArrayInputStream(testData), -1);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, -1);
        });
    }
    
    @Test
    public void testZipBombProtection() {
        // Test protection against zip bomb attacks
        // Create a highly compressible payload (lots of zeros)
        byte[] highlyCompressibleData = new byte[10000];
        // Fill with repeated pattern for good compression
        for (int i = 0; i < highlyCompressibleData.length; i++) {
            highlyCompressibleData[i] = 0;
        }
        
        byte[] compressedData = IOUtilities.compressBytes(highlyCompressibleData);
        
        // The compressed data should be much smaller than the original
        assertTrue(compressedData.length < highlyCompressibleData.length / 10, 
                  "Compressed data should be much smaller for zip bomb test");
        
        // Now test that decompression with a small limit fails
        Exception exception = assertThrows(Exception.class, () -> {
            IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, 1000);
        }, "Should reject decompression that exceeds size limit");
        assertTrue(exception instanceof RuntimeException);
        
        // But should work with adequate limit
        byte[] decompressed = IOUtilities.uncompressBytes(compressedData, 0, compressedData.length, 20000);
        assertArrayEquals(highlyCompressibleData, decompressed);
    }
    
    @Test
    public void testTransferMethodsHaveNoSizeLimits() {
        // Verify that transfer methods work with large data and have no size limits
        byte[] largeData = new byte[5000]; // Reasonably large for test
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        // Test basic transfer method
        try (ByteArrayInputStream input = new ByteArrayInputStream(largeData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IOUtilities.transfer(input, output);
            assertArrayEquals(largeData, output.toByteArray());
        } catch (Exception e) {
            fail("Transfer methods should not have size limits: " + e.getMessage());
        }
        
        // Test transfer with callback
        try (ByteArrayInputStream input = new ByteArrayInputStream(largeData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            final int[] bytesTransferred = {0};
            IOUtilities.TransferCallback callback = (buffer, count) -> {
                bytesTransferred[0] += count;
            };
            
            IOUtilities.transfer(input, output, callback);
            assertEquals(largeData.length, bytesTransferred[0]);
            assertArrayEquals(largeData, output.toByteArray());
        } catch (Exception e) {
            fail("Transfer methods should not have size limits: " + e.getMessage());
        }
    }
}