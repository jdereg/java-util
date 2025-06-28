package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for race condition prevention in IOUtilities transfer callback buffer handling.
 * Verifies that buffer copies are properly defensive and thread-safe.
 */
public class IOUtilitiesRaceConditionTest {
    
    private Method createSafeCallbackBufferMethod;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Access the private createSafeCallbackBuffer method via reflection for testing
        createSafeCallbackBufferMethod = IOUtilities.class.getDeclaredMethod("createSafeCallbackBuffer", byte[].class, int.class);
        createSafeCallbackBufferMethod.setAccessible(true);
    }
    
    @Test
    public void testDefensiveCopyCreation() throws Exception {
        // Test that createSafeCallbackBuffer creates proper defensive copies
        byte[] originalBuffer = {1, 2, 3, 4, 5, 6, 7, 8};
        int count = 5;
        
        byte[] defensiveCopy = (byte[]) createSafeCallbackBufferMethod.invoke(null, originalBuffer, count);
        
        // Verify the copy has the correct size and content
        assertEquals(count, defensiveCopy.length, "Defensive copy should have the correct length");
        assertArrayEquals(Arrays.copyOf(originalBuffer, count), defensiveCopy, "Defensive copy should contain the correct data");
        
        // Verify it's actually a different array (not just a reference)
        assertNotSame(originalBuffer, defensiveCopy, "Defensive copy should be a different array instance");
        
        // Verify modifying the copy doesn't affect the original
        defensiveCopy[0] = 99;
        assertEquals(1, originalBuffer[0], "Modifying defensive copy should not affect original buffer");
    }
    
    @Test
    public void testDefensiveCopyWithZeroCount() throws Exception {
        // Test edge case with zero count
        byte[] originalBuffer = {1, 2, 3, 4, 5};
        
        byte[] defensiveCopy = (byte[]) createSafeCallbackBufferMethod.invoke(null, originalBuffer, 0);
        
        assertEquals(0, defensiveCopy.length, "Zero count should produce empty array");
    }
    
    @Test
    public void testDefensiveCopyWithNegativeCount() throws Exception {
        // Test edge case with negative count
        byte[] originalBuffer = {1, 2, 3, 4, 5};
        
        byte[] defensiveCopy = (byte[]) createSafeCallbackBufferMethod.invoke(null, originalBuffer, -1);
        
        assertEquals(0, defensiveCopy.length, "Negative count should produce empty array");
    }
    
    @Test
    public void testCallbackBufferIsolation() throws Exception {
        // Test that callback receives isolated buffer that can be safely modified
        byte[] testData = "Hello, World! This is test data for buffer isolation.".getBytes();
        
        AtomicReference<byte[]> receivedBuffer = new AtomicReference<>();
        AtomicInteger receivedCount = new AtomicInteger();
        AtomicBoolean bufferModified = new AtomicBoolean(false);
        
        IOUtilities.TransferCallback callback = (bytes, count) -> {
            receivedBuffer.set(bytes);
            receivedCount.set(count);
            
            // Modify the received buffer to test isolation
            if (bytes.length > 0) {
                bytes[0] = (byte) 0xFF;
                bufferModified.set(true);
            }
        };
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IOUtilities.transfer(input, output, callback);
            
            // Verify callback was called
            assertTrue(bufferModified.get(), "Callback should have been called and modified buffer");
            assertNotNull(receivedBuffer.get(), "Callback should have received a buffer");
            assertTrue(receivedCount.get() > 0, "Callback should have received a positive count");
            
            // Verify the output is correct and unaffected by callback buffer modification
            assertArrayEquals(testData, output.toByteArray(), "Output should be unchanged despite callback buffer modification");
            
            // Verify the callback received a defensive copy, not the original data
            byte[] callbackBuffer = receivedBuffer.get();
            assertEquals((byte) 0xFF, callbackBuffer[0], "Callback should have successfully modified its buffer copy");
        }
    }
    
    @Test
    public void testConcurrentCallbackAccess() throws Exception {
        // Test thread safety when multiple callbacks access buffers concurrently
        byte[] testData = new byte[10000];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        AtomicInteger callbackInvocations = new AtomicInteger(0);
        AtomicInteger dataCorruptions = new AtomicInteger(0);
        
        IOUtilities.TransferCallback callback = (bytes, count) -> {
            callbackInvocations.incrementAndGet();
            
            try {
                // Wait for all threads to be ready
                startLatch.await(5, TimeUnit.SECONDS);
                
                // Simulate concurrent buffer access by modifying the buffer
                // This should be safe due to defensive copying
                for (int i = 0; i < Math.min(bytes.length, 100); i++) {
                    bytes[i] = (byte) 0xAA;
                }
                
                // Small delay to increase chance of race conditions if they exist
                Thread.sleep(1);
                
                // Verify the buffer still contains our modifications
                for (int i = 0; i < Math.min(bytes.length, 100); i++) {
                    if (bytes[i] != (byte) 0xAA) {
                        dataCorruptions.incrementAndGet();
                        break;
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        };
        
        // Start multiple transfers concurrently
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try (ByteArrayInputStream input = new ByteArrayInputStream(testData);
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    
                    IOUtilities.transfer(input, output, callback);
                    
                    // Verify output is correct
                    byte[] outputData = output.toByteArray();
                    assertArrayEquals(testData, outputData, "Concurrent transfers should produce correct output");
                    
                } catch (Exception e) {
                    fail("Concurrent transfer failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All concurrent transfers should complete");
        executor.shutdown();
        
        // Verify results
        assertTrue(callbackInvocations.get() > 0, "Callbacks should have been invoked");
        assertEquals(0, dataCorruptions.get(), "No data corruptions should occur with defensive copying");
    }
    
    @Test
    public void testCallbackCancellation() throws Exception {
        // Test that cancellation works properly and doesn't cause race conditions
        byte[] testData = new byte[200000]; // Large enough to ensure multiple callback invocations (200KB)
        Arrays.fill(testData, (byte) 42);
        
        AtomicInteger callbackCount = new AtomicInteger(0);
        AtomicBoolean shouldCancel = new AtomicBoolean(false);
        
        IOUtilities.TransferCallback callback = new IOUtilities.TransferCallback() {
            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                int invocation = callbackCount.incrementAndGet();
                
                // Verify we got a defensive copy
                assertNotNull(bytes, "Callback should receive a buffer");
                assertEquals(count, bytes.length, "Buffer length should match count");
                
                // Cancel after the second callback to ensure we get at least 2 callbacks
                if (invocation >= 2) {
                    shouldCancel.set(true);
                }
            }
            
            @Override
            public boolean isCancelled() {
                return shouldCancel.get();
            }
        };
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IOUtilities.transfer(input, output, callback);
            
            // Verify cancellation worked
            assertTrue(callbackCount.get() >= 2, "At least 2 callbacks should have been invoked before cancellation");
            assertTrue(shouldCancel.get(), "Cancellation should have been triggered");
            
            // Verify partial data was transferred (unless the buffer is extremely large)
            byte[] outputData = output.toByteArray();
            assertTrue(outputData.length > 0, "Some data should have been transferred before cancellation");
            
            // Note: We can't always guarantee partial transfer because if the buffer size is very large,
            // cancellation might only take effect after all data is transferred
        }
    }
    
    @Test
    public void testBufferContentAccuracy() throws Exception {
        // Test that defensive copies contain accurate data
        String testMessage = "Test message for buffer accuracy verification!";
        byte[] testData = testMessage.getBytes();
        
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        
        IOUtilities.TransferCallback callback = (bytes, count) -> {
            // Convert received bytes back to string to verify content accuracy
            String message = new String(bytes, 0, count);
            receivedMessage.set(message);
        };
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IOUtilities.transfer(input, output, callback);
            
            // Verify callback received accurate data
            assertEquals(testMessage, receivedMessage.get(), "Callback should receive accurate buffer content");
            
            // Verify output is also correct
            assertEquals(testMessage, output.toString(), "Output should contain the original message");
        }
    }
    
    @Test
    public void testLargeDataTransferWithCallback() throws Exception {
        // Test defensive copying performance and correctness with large data
        int dataSize = 1024 * 1024; // 1MB
        byte[] testData = new byte[dataSize];
        
        // Fill with pattern data
        for (int i = 0; i < dataSize; i++) {
            testData[i] = (byte) (i % 127);
        }
        
        AtomicInteger totalBytesReceived = new AtomicInteger(0);
        AtomicInteger callbackInvocations = new AtomicInteger(0);
        
        IOUtilities.TransferCallback callback = (bytes, count) -> {
            callbackInvocations.incrementAndGet();
            totalBytesReceived.addAndGet(count);
            
            // Verify buffer integrity
            assertEquals(count, bytes.length, "Buffer length should match count");
            assertTrue(count > 0, "Count should be positive");
            
            // Verify data pattern in the buffer
            for (int i = 0; i < count; i++) {
                // We can't verify the exact pattern without knowing the offset,
                // but we can verify the values are within expected range
                assertTrue(bytes[i] >= 0 && bytes[i] < 127, "Buffer data should be within expected range");
            }
        };
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(testData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IOUtilities.transfer(input, output, callback);
            
            // Verify all data was transferred
            assertEquals(dataSize, totalBytesReceived.get(), "All bytes should be reported through callbacks");
            assertTrue(callbackInvocations.get() > 1, "Multiple callbacks should be invoked for large data");
            
            // Verify output integrity
            assertArrayEquals(testData, output.toByteArray(), "Output should match input exactly");
        }
    }
}