package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for StringUtilities.
 * Verifies that security controls prevent injection attacks, resource exhaustion, 
 * and other security vulnerabilities.
 */
public class StringUtilitiesSecurityTest {
    
    // Test regex injection vulnerability fixes
    
    @Test
    public void testWildcardToRegexString_nullInput_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.wildcardToRegexString(null);
        });
        
        assertTrue(exception.getMessage().contains("cannot be null"), 
                  "Should reject null wildcard patterns");
    }
    
    @Test
    public void testWildcardToRegexString_tooLong_throwsException() {
        String longPattern = StringUtilities.repeat("a", 1001);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.wildcardToRegexString(longPattern);
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject patterns longer than 1000 characters");
    }
    
    @Test
    public void testWildcardToRegexString_tooManyWildcards_throwsException() {
        String pattern = StringUtilities.repeat("*", 101);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.wildcardToRegexString(pattern);
        });
        
        assertTrue(exception.getMessage().contains("Too many wildcards"), 
                  "Should reject patterns with more than 100 wildcards");
    }
    
    @Test
    public void testWildcardToRegexString_normalPattern_works() {
        String pattern = "test*.txt";
        String regex = StringUtilities.wildcardToRegexString(pattern);
        
        assertNotNull(regex, "Normal patterns should work");
        assertTrue(regex.startsWith("^"), "Should start with ^");
        assertTrue(regex.endsWith("$"), "Should end with $");
    }
    
    @Test
    public void testWildcardToRegexString_maxValidPattern_works() {
        // Create a pattern at the maximum allowed limit
        String pattern = StringUtilities.repeat("a", 900) + StringUtilities.repeat("*", 100);
        
        String regex = StringUtilities.wildcardToRegexString(pattern);
        assertNotNull(regex, "Pattern at limit should work");
    }
    
    // Test buffer overflow vulnerability fixes
    
    @Test
    public void testRepeat_tooLargeCount_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.repeat("a", 10001);
        });
        
        assertTrue(exception.getMessage().contains("count too large"), 
                  "Should reject count larger than 10000");
    }
    
    @Test
    public void testRepeat_integerOverflow_throwsException() {
        // Create a 2000-character string to test overflow
        StringBuilder sb = new StringBuilder(2000);
        for (int i = 0; i < 2000; i++) {
            sb.append('a');
        }
        String longString = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.repeat(longString, 6000); // 2000 * 6000 = 12M chars, exceeds 10M limit
        });
        
        assertTrue(exception.getMessage().contains("too large"), 
                  "Should prevent memory exhaustion through large multiplication");
    }
    
    @Test
    public void testRepeat_memoryExhaustion_throwsException() {
        String mediumString = StringUtilities.repeat("a", 5000);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.repeat(mediumString, 5000); // Would create 25MB string
        });
        
        assertTrue(exception.getMessage().contains("too large"), 
                  "Should prevent memory exhaustion attacks");
    }
    
    @Test
    public void testRepeat_normalUsage_works() {
        String result = StringUtilities.repeat("test", 5);
        assertEquals("testtesttesttesttest", result, "Normal repeat should work");
    }
    
    @Test
    public void testRepeat_maxValidSize_works() {
        String result = StringUtilities.repeat("a", 10000);
        assertEquals(10000, result.length(), "Maximum valid repeat should work");
    }
    
    // Test resource exhaustion vulnerability fixes
    
    @Test
    public void testLevenshteinDistance_tooLongFirst_throwsException() {
        // Create a long string without using repeat() method  
        StringBuilder sb = new StringBuilder(10001);
        for (int i = 0; i < 10001; i++) {
            sb.append('a');
        }
        String longString = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.levenshteinDistance(longString, "test");
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject first string longer than 10000 characters");
    }
    
    @Test
    public void testLevenshteinDistance_tooLongSecond_throwsException() {
        // Create a long string without using repeat() method
        StringBuilder sb = new StringBuilder(10001);
        for (int i = 0; i < 10001; i++) {
            sb.append('b');
        }
        String longString = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.levenshteinDistance("test", longString);
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject second string longer than 10000 characters");
    }
    
    @Test
    public void testLevenshteinDistance_normalUsage_works() {
        int distance = StringUtilities.levenshteinDistance("kitten", "sitting");
        assertEquals(3, distance, "Normal Levenshtein distance should work");
    }
    
    @Test
    public void testLevenshteinDistance_maxValidSize_works() {
        String maxString = StringUtilities.repeat("a", 10000);
        int distance = StringUtilities.levenshteinDistance(maxString, "b");
        assertEquals(10000, distance, "Maximum valid size should work");
    }
    
    @Test
    public void testDamerauLevenshteinDistance_tooLongSource_throwsException() {
        // Create a long string without using repeat() method
        StringBuilder sb = new StringBuilder(5001);
        for (int i = 0; i < 5001; i++) {
            sb.append('a');
        }
        String longString = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.damerauLevenshteinDistance(longString, "test");
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject source string longer than 5000 characters");
    }
    
    @Test
    public void testDamerauLevenshteinDistance_tooLongTarget_throwsException() {
        // Create a long string without using repeat() method
        StringBuilder sb = new StringBuilder(5001);
        for (int i = 0; i < 5001; i++) {
            sb.append('b');
        }
        String longString = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.damerauLevenshteinDistance("test", longString);
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject target string longer than 5000 characters");
    }
    
    @Test
    public void testDamerauLevenshteinDistance_normalUsage_works() {
        int distance = StringUtilities.damerauLevenshteinDistance("book", "back");
        assertEquals(2, distance, "Normal Damerau-Levenshtein distance should work");
    }
    
    @Test
    public void testDamerauLevenshteinDistance_maxValidSize_works() {
        String maxString = StringUtilities.repeat("a", 5000);
        int distance = StringUtilities.damerauLevenshteinDistance(maxString, "b");
        assertEquals(5000, distance, "Maximum valid size should work");
    }
    
    // Test input validation fixes
    
    @Test
    public void testDecode_nullInput_returnsNull() {
        byte[] result = StringUtilities.decode(null);
        assertNull(result, "Null input should return null");
    }
    
    @Test
    public void testDecode_tooLong_throwsException() {
        // Create a long hex string without using repeat() method
        StringBuilder sb = new StringBuilder(100001);
        for (int i = 0; i < 50001; i++) {
            sb.append("ab");
        }
        String longHex = sb.toString();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StringUtilities.decode(longHex);
        });
        
        assertTrue(exception.getMessage().contains("too long"), 
                  "Should reject hex strings longer than 100000 characters");
    }
    
    @Test
    public void testDecode_normalUsage_works() {
        byte[] result = StringUtilities.decode("48656c6c6f"); // "Hello" in hex
        assertNotNull(result, "Normal hex decoding should work");
        assertEquals("Hello", new String(result), "Should decode correctly");
    }
    
    @Test
    public void testDecode_maxValidSize_works() {
        // Create max valid hex string without using repeat() method  
        StringBuilder sb = new StringBuilder(100000);
        for (int i = 0; i < 50000; i++) {
            sb.append("ab");
        }
        String hexString = sb.toString(); // 100000 chars total
        
        byte[] result = StringUtilities.decode(hexString);
        assertNotNull(result, "Maximum valid size should work");
        assertEquals(50000, result.length, "Should decode to correct length");
    }
    
    // Test boundary conditions and edge cases
    
    @Test
    public void testSecurity_boundaryConditions() {
        // Test exact boundary values
        
        // Wildcard pattern: exactly 1000 chars should work
        String pattern1000 = StringUtilities.repeat("a", 1000);
        assertDoesNotThrow(() -> StringUtilities.wildcardToRegexString(pattern1000),
                "Pattern of exactly 1000 characters should work");
        
        // Repeat: exactly 10000 count should work  
        assertDoesNotThrow(() -> StringUtilities.repeat("a", 10000),
                "Repeat count of exactly 10000 should work");
        
        // Levenshtein: exactly 10000 chars should work
        String string10000 = StringUtilities.repeat("a", 10000);
        assertDoesNotThrow(() -> StringUtilities.levenshteinDistance(string10000, "b"),
                "Levenshtein with exactly 10000 characters should work");
        
        // Damerau-Levenshtein: exactly 5000 chars should work
        String string5000 = StringUtilities.repeat("a", 5000);
        assertDoesNotThrow(() -> StringUtilities.damerauLevenshteinDistance(string5000, "b"),
                "Damerau-Levenshtein with exactly 5000 characters should work");
        
        // Decode: exactly 100000 chars should work
        StringBuilder sb = new StringBuilder(100000);
        for (int i = 0; i < 50000; i++) {
            sb.append("ab");
        }
        String hex100000 = sb.toString();
        assertDoesNotThrow(() -> StringUtilities.decode(hex100000),
                "Hex decode of exactly 100000 characters should work");
    }
    
    @Test
    public void testSecurity_consistentErrorMessages() {
        // Verify error messages are consistent and don't expose sensitive info
        
        try {
            StringUtilities.wildcardToRegexString(StringUtilities.repeat("*", 200));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().contains("internal"), 
                    "Error message should not expose internal details");
            assertTrue(e.getMessage().contains("wildcards"), 
                    "Error message should indicate the problem");
        }
        
        try {
            StringUtilities.repeat("test", 50000);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().contains("memory"), 
                    "Error message should not expose memory details");
            assertTrue(e.getMessage().contains("large"), 
                    "Error message should indicate the problem");
        }
    }
}