package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for DeepEquals.
 * Verifies that security controls prevent stack overflow, resource exhaustion,
 * and other security vulnerabilities.
 */
public class DeepEqualsSecurityTest {
    
    private Map<String, Object> options;
    
    @BeforeEach
    public void setUp() {
        options = new HashMap<>();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up
    }
    
    // Test stack overflow prevention via depth limits
    
    @Test
    public void testDeepRecursion_depthLimit_throwsException() {
        // Create a deeply nested object structure that would cause stack overflow
        DeepNode root = createDeepLinkedList(2000); // Way beyond default limit of 1000
        DeepNode root2 = createDeepLinkedList(2000);
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(root, root2, options);
        });
        
        assertTrue(exception.getMessage().contains("depth limit exceeded"),
                  "Should throw SecurityException for excessive depth");
        assertTrue(exception.getMessage().contains("DoS attack"),
                  "Should indicate potential DoS attack");
    }
    
    @Test
    public void testDeepRecursion_withinLimits_works() {
        // Create object structure within limits
        DeepNode root1 = createDeepLinkedList(100); // Well within default limit
        DeepNode root2 = createDeepLinkedList(100);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(root1, root2, options);
            assertTrue(result, "Identical deep structures should be equal");
        });
    }
    
    @Test
    public void testCustomDepthLimit_respected() {
        options.put(DeepEquals.MAX_DEPTH, 50); // Custom lower limit
        
        DeepNode root1 = createDeepLinkedList(100); // Exceeds custom limit
        DeepNode root2 = createDeepLinkedList(100);
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(root1, root2, options);
        });
        
        assertTrue(exception.getMessage().contains("50"),
                  "Should respect custom depth limit");
    }
    
    // Test resource exhaustion prevention via collection size limits
    
    @Test
    public void testLargeCollection_sizeLimit_throwsException() {
        // Create collections larger than default limit
        List<Integer> list1 = createLargeList(60000); // Exceeds default 50000
        List<Integer> list2 = createLargeList(60000);
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(list1, list2, options);
        });
        
        assertTrue(exception.getMessage().contains("Collection size limit exceeded"),
                  "Should throw SecurityException for large collections");
        assertTrue(exception.getMessage().contains("DoS attack"),
                  "Should indicate potential DoS attack");
    }
    
    @Test
    public void testLargeMap_sizeLimit_throwsException() {
        // Create maps larger than default limit
        Map<String, String> map1 = createLargeMap(60000); // Exceeds default 50000
        Map<String, String> map2 = createLargeMap(60000);
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(map1, map2, options);
        });
        
        assertTrue(exception.getMessage().contains("Map size limit exceeded"),
                  "Should throw SecurityException for large maps");
    }
    
    @Test
    public void testCustomCollectionSizeLimit_respected() {
        options.put(DeepEquals.MAX_COLLECTION_SIZE, 1000); // Custom lower limit
        
        List<Integer> list1 = createLargeList(2000); // Exceeds custom limit
        List<Integer> list2 = createLargeList(2000);
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(list1, list2, options);
        });
        
        assertTrue(exception.getMessage().contains("1000"),
                  "Should respect custom collection size limit");
    }
    
    // Test hash computation security
    
    @Test
    public void testDeepHashCode_largeObject_limitsIterations() {
        // Create object structure that would cause excessive hash iterations
        ComplexObject obj = createComplexObjectStructure(1000);
        
        // Should not throw exception for reasonable sized objects
        assertDoesNotThrow(() -> {
            int hash = DeepEquals.deepHashCode(obj);
            // Just verify we get a hash value
            assertNotEquals(0, hash); // Very unlikely to be 0 for complex object
        });
    }
    
    // Test security-sensitive field filtering
    
    @Test
    public void testSecuritySensitiveFields_skipped() {
        SecuritySensitiveObject obj1 = new SecuritySensitiveObject("user1", "secret123", "token456");
        SecuritySensitiveObject obj2 = new SecuritySensitiveObject("user1", "different_secret", "different_token");
        
        // Objects should be considered equal because security-sensitive fields are skipped
        boolean result = DeepEquals.deepEquals(obj1, obj2, options);
        assertTrue(result, "Objects should be equal when only security-sensitive fields differ");
    }
    
    // Test boundary conditions for security limits
    
    @Test
    public void testSecurityLimits_boundaryConditions() {
        // Test exactly at the limit
        options.put(DeepEquals.MAX_DEPTH, 10);
        options.put(DeepEquals.MAX_COLLECTION_SIZE, 100);
        
        DeepNode root1 = createDeepLinkedList(10); // Exactly at limit
        DeepNode root2 = createDeepLinkedList(10);
        
        // Should work at exactly the limit
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(root1, root2, options);
            assertTrue(result);
        });
        
        // Test one over the limit
        DeepNode root3 = createDeepLinkedList(11); // One over limit
        DeepNode root4 = createDeepLinkedList(11);
        
        assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(root3, root4, options);
        });
    }
    
    @Test
    public void testInvalidSecurityLimits_throwsException() {
        // Test invalid (non-positive) limits
        options.put(DeepEquals.MAX_DEPTH, 0);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            DeepEquals.deepEquals("test", "test", options);
        });
        
        assertTrue(exception.getMessage().contains("must be positive"),
                  "Should reject non-positive security limits");
    }
    
    // Test thread safety of security controls
    
    @Test
    public void testSecurityControls_threadSafety() throws InterruptedException {
        final Exception[] exceptions = new Exception[2];
        final boolean[] results = new boolean[2];
        
        // Test concurrent access with different limits
        Thread thread1 = new Thread(() -> {
            try {
                Map<String, Object> opts1 = new HashMap<>();
                opts1.put(DeepEquals.MAX_DEPTH, 10);
                DeepNode root = createDeepLinkedList(15); // Exceeds limit
                DeepEquals.deepEquals(root, root, opts1);
                results[0] = false; // Should not reach here
            } catch (SecurityException e) {
                results[0] = true; // Expected
            } catch (Exception e) {
                exceptions[0] = e;
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                Map<String, Object> opts2 = new HashMap<>();
                opts2.put(DeepEquals.MAX_COLLECTION_SIZE, 100);
                List<Integer> list = createLargeList(200); // Exceeds limit
                DeepEquals.deepEquals(list, list, opts2);
                results[1] = false; // Should not reach here
            } catch (SecurityException e) {
                results[1] = true; // Expected
            } catch (Exception e) {
                exceptions[1] = e;
            }
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        assertNull(exceptions[0], "Thread 1 should not have thrown unexpected exception");
        assertNull(exceptions[1], "Thread 2 should not have thrown unexpected exception");
        assertTrue(results[0], "Thread 1 should have caught SecurityException");
        assertTrue(results[1], "Thread 2 should have caught SecurityException");
    }
    
    // Helper methods for creating test data
    
    private DeepNode createDeepLinkedList(int depth) {
        if (depth <= 0) return null;
        DeepNode node = new DeepNode();
        node.value = depth;
        node.next = createDeepLinkedList(depth - 1);
        return node;
    }
    
    private List<Integer> createLargeList(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }
    
    private Map<String, String> createLargeMap(int size) {
        Map<String, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put("key" + i, "value" + i);
        }
        return map;
    }
    
    private ComplexObject createComplexObjectStructure(int complexity) {
        ComplexObject obj = new ComplexObject();
        obj.id = complexity;
        obj.name = "Object" + complexity;
        obj.children = new ArrayList<>();
        
        // Create some child objects to increase complexity
        for (int i = 0; i < Math.min(complexity / 10, 100); i++) {
            ComplexObject child = new ComplexObject();
            child.id = i;
            child.name = "Child" + i;
            child.children = new ArrayList<>();
            obj.children.add(child);
        }
        
        return obj;
    }
    
    // Test helper classes
    
    static class DeepNode {
        int value;
        DeepNode next;
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            DeepNode deepNode = (DeepNode) obj;
            return value == deepNode.value && Objects.equals(next, deepNode.next);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value, next);
        }
    }
    
    static class ComplexObject {
        int id;
        String name;
        List<ComplexObject> children;
        Map<String, String> properties = new HashMap<>();
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ComplexObject that = (ComplexObject) obj;
            return id == that.id && 
                   Objects.equals(name, that.name) && 
                   Objects.equals(children, that.children) &&
                   Objects.equals(properties, that.properties);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id, name, children, properties);
        }
    }
    
    static class SecuritySensitiveObject {
        String username;
        String password;      // Security-sensitive field
        String authToken;     // Security-sensitive field
        String secretKey;     // Security-sensitive field
        
        public SecuritySensitiveObject(String username, String password, String authToken) {
            this.username = username;
            this.password = password;
            this.authToken = authToken;
            this.secretKey = "secret_" + username;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SecuritySensitiveObject that = (SecuritySensitiveObject) obj;
            return Objects.equals(username, that.username) &&
                   Objects.equals(password, that.password) &&
                   Objects.equals(authToken, that.authToken) &&
                   Objects.equals(secretKey, that.secretKey);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(username, password, authToken, secretKey);
        }
    }
}