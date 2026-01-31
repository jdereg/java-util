package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security tests for Executor class.
 * Tests the security control where command execution is disabled by default and must be explicitly enabled.
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
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
public class ExecutorSecurityTest {
    
    private String originalExecutorEnabled;
    
    @BeforeEach
    void setUp() {
        // Save original system property value
        originalExecutorEnabled = System.getProperty("executor.enabled");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property value
        if (originalExecutorEnabled == null) {
            System.clearProperty("executor.enabled");
        } else {
            System.setProperty("executor.enabled", originalExecutorEnabled);
        }
    }
    
    @Test
    void testExecutorDisabledByDefault() {
        // Executor should be disabled by default for security
        System.clearProperty("executor.enabled"); // Ensure no explicit setting
        
        Executor executor = new Executor();
        
        // Should throw SecurityException by default
        SecurityException e = assertThrows(SecurityException.class, () -> {
            executor.execute("echo test");
        }, "Executor should be disabled by default");
        
        // Check that error message provides clear instructions
        assertTrue(e.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e.getMessage().contains("executor.enabled=true"));
    }
    
    @Test
    void testExecutorCanBeExplicitlyEnabled() {
        // Explicitly enable executor
        System.setProperty("executor.enabled", "true");
        
        Executor executor = new Executor();
        
        // Should be able to execute commands when explicitly enabled
        assertDoesNotThrow(() -> {
            ExecutionResult result = executor.execute("echo test");
            assertNotNull(result);
        }, "Executor should work when explicitly enabled");
    }
    
    @Test
    void testExecutorCanBeDisabled() {
        // Disable executor
        System.setProperty("executor.enabled", "false");
        
        Executor executor = new Executor();
        
        // All execute methods should throw SecurityException
        SecurityException e1 = assertThrows(SecurityException.class, 
            () -> executor.execute("echo test"),
            "execute(String) should throw SecurityException when disabled");
        assertTrue(e1.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e1.getMessage().contains("executor.enabled=true"));
        
        SecurityException e2 = assertThrows(SecurityException.class, 
            () -> executor.execute(new String[]{"echo", "test"}),
            "execute(String[]) should throw SecurityException when disabled");
        assertTrue(e2.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e2.getMessage().contains("executor.enabled=true"));
        
        SecurityException e3 = assertThrows(SecurityException.class, 
            () -> executor.execute("echo test", null),
            "execute(String, String[]) should throw SecurityException when disabled");
        assertTrue(e3.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e3.getMessage().contains("executor.enabled=true"));
        
        SecurityException e4 = assertThrows(SecurityException.class, 
            () -> executor.execute(new String[]{"echo", "test"}, null),
            "execute(String[], String[]) should throw SecurityException when disabled");
        assertTrue(e4.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e4.getMessage().contains("executor.enabled=true"));
        
        SecurityException e5 = assertThrows(SecurityException.class, 
            () -> executor.execute("echo test", null, null),
            "execute(String, String[], File) should throw SecurityException when disabled");
        assertTrue(e5.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e5.getMessage().contains("executor.enabled=true"));
        
        SecurityException e6 = assertThrows(SecurityException.class, 
            () -> executor.execute(new String[]{"echo", "test"}, null, null),
            "execute(String[], String[], File) should throw SecurityException when disabled");
        assertTrue(e6.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e6.getMessage().contains("executor.enabled=true"));
    }
    
    @Test
    void testExecMethodsAlsoThrowWhenDisabled() {
        // Disable executor
        System.setProperty("executor.enabled", "false");
        
        Executor executor = new Executor();
        
        // All exec methods should also throw SecurityException
        SecurityException e1 = assertThrows(SecurityException.class, 
            () -> executor.exec("echo test"),
            "exec(String) should throw SecurityException when disabled");
        assertTrue(e1.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e1.getMessage().contains("executor.enabled=true"));
        
        SecurityException e2 = assertThrows(SecurityException.class, 
            () -> executor.exec(new String[]{"echo", "test"}),
            "exec(String[]) should throw SecurityException when disabled");
        assertTrue(e2.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e2.getMessage().contains("executor.enabled=true"));
        
        SecurityException e3 = assertThrows(SecurityException.class, 
            () -> executor.exec("echo test", null),
            "exec(String, String[]) should throw SecurityException when disabled");
        assertTrue(e3.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e3.getMessage().contains("executor.enabled=true"));
        
        SecurityException e4 = assertThrows(SecurityException.class, 
            () -> executor.exec(new String[]{"echo", "test"}, null),
            "exec(String[], String[]) should throw SecurityException when disabled");
        assertTrue(e4.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e4.getMessage().contains("executor.enabled=true"));
        
        SecurityException e5 = assertThrows(SecurityException.class, 
            () -> executor.exec("echo test", null, null),
            "exec(String, String[], File) should throw SecurityException when disabled");
        assertTrue(e5.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e5.getMessage().contains("executor.enabled=true"));
        
        SecurityException e6 = assertThrows(SecurityException.class, 
            () -> executor.exec(new String[]{"echo", "test"}, null, null),
            "exec(String[], String[], File) should throw SecurityException when disabled");
        assertTrue(e6.getMessage().contains("Command execution is disabled by default for security"));
        assertTrue(e6.getMessage().contains("executor.enabled=true"));
    }
    
    @Test
    void testSecuritySettingIsCaseInsensitive() {
        // Test various case combinations for "false"
        String[] falseValues = {"false", "False", "FALSE", "fAlSe"};
        
        for (String falseValue : falseValues) {
            System.setProperty("executor.enabled", falseValue);
            
            Executor executor = new Executor();
            
            SecurityException e = assertThrows(SecurityException.class, 
                () -> executor.execute("echo test"),
                "Should be disabled with value: " + falseValue);
            assertTrue(e.getMessage().contains("Command execution is disabled by default for security"));
        }
        
        // Test various case combinations for "true"
        String[] trueValues = {"true", "True", "TRUE", "tRuE"};
        
        for (String trueValue : trueValues) {
            System.setProperty("executor.enabled", trueValue);
            
            Executor executor = new Executor();
            
            assertDoesNotThrow(() -> {
                ExecutionResult result = executor.execute("echo test");
                assertNotNull(result);
            }, "Should be enabled with value: " + trueValue);
        }
    }
    
    @Test
    void testInvalidValuesTreatedAsFalse() {
        // Test that invalid values are treated as false (disabled)
        String[] invalidValues = {"", "yes", "no", "1", "0", "enabled", "disabled", "invalid"};
        
        for (String invalidValue : invalidValues) {
            System.setProperty("executor.enabled", invalidValue);
            
            Executor executor = new Executor();
            
            SecurityException e = assertThrows(SecurityException.class, 
                () -> executor.execute("echo test"),
                "Should be disabled with invalid value: " + invalidValue);
            assertTrue(e.getMessage().contains("Command execution is disabled by default for security"));
        }
    }
    
    @Test
    void testBreakingChangeRequiresExplicitEnable() {
        // Test that existing code now requires explicit enablement (breaking change)
        System.clearProperty("executor.enabled");
        
        Executor executor = new Executor();
        
        // Traditional usage should now throw SecurityException
        SecurityException e1 = assertThrows(SecurityException.class, () -> {
            executor.exec("echo backward_compatibility_test");
        }, "Existing code should now require explicit enablement");
        
        SecurityException e2 = assertThrows(SecurityException.class, () -> {
            executor.execute("echo test_result");
        }, "Existing code should now require explicit enablement");
        
        // Both should provide clear instructions on how to enable
        assertTrue(e1.getMessage().contains("executor.enabled=true"));
        assertTrue(e2.getMessage().contains("executor.enabled=true"));
    }
}