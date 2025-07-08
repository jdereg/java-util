package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enhanced file access validation and symlink attack prevention in IOUtilities.
 * Verifies that the validateFilePath method properly detects and prevents various file system security attacks.
 */
public class IOUtilitiesFileValidationTest {
    private static final Logger LOG = Logger.getLogger(IOUtilitiesFileValidationTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    private Method validateFilePathMethod;
    private String originalValidationDisabled;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Access the private validateFilePath method via reflection for testing
        validateFilePathMethod = IOUtilities.class.getDeclaredMethod("validateFilePath", File.class);
        validateFilePathMethod.setAccessible(true);
        
        // Store original validation setting
        originalValidationDisabled = System.getProperty("io.path.validation.disabled");
        // Ensure validation is enabled for tests
        System.clearProperty("io.path.validation.disabled");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original validation setting
        if (originalValidationDisabled != null) {
            System.setProperty("io.path.validation.disabled", originalValidationDisabled);
        } else {
            System.clearProperty("io.path.validation.disabled");
        }
    }
    
    @Test
    public void testBasicPathTraversalDetection() throws Exception {
        // Test various path traversal attempts
        String[] maliciousPaths = {
            "../etc/passwd",
            "..\\windows\\system32\\config\\sam",
            "/legitimate/path/../../../etc/passwd",
            "C:\\legitimate\\path\\..\\..\\..\\windows\\system32",
            "./../../etc/shadow",
            "..\\..\\.\\windows\\system32"
        };
        
        for (String path : maliciousPaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Path traversal attempt detected"), 
                      "Should detect path traversal in: " + path);
        }
    }
    
    @Test
    public void testNullByteInjectionDetection() throws Exception {
        // Test null byte injection attempts
        String[] nullBytePaths = {
            "/etc/passwd\0.txt",
            "C:\\windows\\system32\\config\\sam\0.log",
            "normal/path\0/file.txt"
        };
        
        for (String path : nullBytePaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Null byte in file path"), 
                      "Should detect null byte injection in: " + path + ". Actual message: " + cause.getMessage());
        }
    }
    
    @Test
    public void testSuspiciousCharacterDetection() throws Exception {
        // Test command injection character detection
        String[] suspiciousPaths = {
            "/tmp/file|rm -rf /",
            "C:\\temp\\file;del C:\\windows",
            "/tmp/file&whoami",
            "/tmp/file`cat /etc/passwd`",
            "/tmp/file$HOME/.ssh/id_rsa"
        };
        
        for (String path : suspiciousPaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Suspicious characters detected"), 
                      "Should detect suspicious characters in: " + path);
        }
    }
    
    @Test
    @EnabledOnOs(OS.LINUX)
    public void testUnixSystemDirectoryProtection() throws Exception {
        // Test Unix/Linux system directory protection
        String[] systemPaths = {
            "/proc/self/mem",
            "/sys/kernel/debug",
            "/dev/mem",
            "/etc/passwd",
            "/etc/shadow", 
            "/etc/ssh/ssh_host_rsa_key"
        };
        
        for (String path : systemPaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Access to system directory/file denied"), 
                      "Should block access to system path: " + path);
        }
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testWindowsSystemDirectoryProtection() throws Exception {
        // Test Windows system directory protection
        String[] systemPaths = {
            "C:\\windows\\system32\\config\\sam",
            "C:\\Windows\\System32\\drivers\\etc\\hosts",
            "C:\\windows\\syswow64\\kernel32.dll",
            "C:\\windows\\system32\\ntdll.dll"
        };
        
        for (String path : systemPaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Access to Windows system directory/file denied"), 
                      "Should block access to Windows system path: " + path);
        }
    }
    
    @Test
    public void testSensitiveHiddenDirectoryProtection() throws Exception {
        // Test protection of sensitive hidden directories
        String[] sensitivePaths = {
            "/home/user/.ssh/id_rsa",
            "/home/user/.gnupg/secring.gpg", 
            "/home/user/.aws/credentials",
            "/home/user/.docker/config.json"
        };
        
        for (String path : sensitivePaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Access to sensitive hidden directory denied"), 
                      "Should block access to sensitive directory: " + path);
        }
    }
    
    @Test
    public void testPathLengthValidation() throws Exception {
        // Test overly long path rejection
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longPath.append("a");
        }
        
        File file = new File(longPath.toString());
        Exception exception = assertThrows(Exception.class, () -> {
            validateFilePathMethod.invoke(null, file);
        });
        // Unwrap InvocationTargetException to get the actual SecurityException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof SecurityException, 
                  "Expected SecurityException but got: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
        assertTrue(cause.getMessage().contains("File path too long") || 
                  cause.getMessage().contains("Unable to validate file path security"), 
                  "Should reject overly long paths. Actual message: " + cause.getMessage());
    }
    
    @Test
    public void testInvalidCharactersInPathElements() throws Exception {
        // Test path elements with control characters
        String[] invalidPaths = {
            "/tmp/file\tname",
            "/tmp/file\nname", 
            "/tmp/file\rname"
        };
        
        for (String path : invalidPaths) {
            File file = new File(path);
            Exception exception = assertThrows(Exception.class, () -> {
                validateFilePathMethod.invoke(null, file);
            });
            // Unwrap InvocationTargetException to get the actual SecurityException
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SecurityException, 
                      "Expected SecurityException but got: " + cause.getClass().getSimpleName());
            assertTrue(cause.getMessage().contains("Invalid characters in path element"), 
                      "Should reject path with control characters: " + path);
        }
    }
    
    @Test
    public void testLegitimatePathsAllowed() throws Exception {
        // Test that legitimate paths are allowed
        String[] legitimatePaths = {
            "/tmp/legitimate_file.txt",
            "/home/user/documents/file.pdf",
            "C:\\Users\\Public\\Documents\\file.docx",
            "./relative/path/file.txt",
            "data/config.json"
        };
        
        for (String path : legitimatePaths) {
            File file = new File(path);
            // Should not throw any exception
            assertDoesNotThrow(() -> {
                validateFilePathMethod.invoke(null, file);
            }, "Legitimate path should be allowed: " + path);
        }
    }
    
    @Test
    public void testValidationCanBeDisabled() throws Exception {
        // Test that validation can be disabled via system property
        System.setProperty("io.path.validation.disabled", "true");
        
        // Even malicious paths should be allowed when validation is disabled
        File file = new File("../../../etc/passwd");
        assertDoesNotThrow(() -> {
            validateFilePathMethod.invoke(null, file);
        }, "Validation should be disabled");
    }
    
    @Test
    public void testSymlinkDetectionInTempDirectory() throws Exception {
        // Only run this test if we can create symlinks (Unix-like systems)
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return; // Skip on Windows as symlink creation requires admin privileges
        }
        
        try {
            // Create a temporary directory for testing
            Path tempDir = Files.createTempDirectory("ioutil_test");
            Path targetFile = tempDir.resolve("target.txt");
            Path symlinkFile = tempDir.resolve("symlink.txt");
            
            // Create target file
            Files.write(targetFile, "test content".getBytes());
            
            // Create symbolic link (this might fail on some systems)
            try {
                Files.createSymbolicLink(symlinkFile, targetFile);
                
                // Validation should detect the symlink
                File file = symlinkFile.toFile();
                assertDoesNotThrow(() -> {
                    validateFilePathMethod.invoke(null, file);
                }, "Symlink detection should log but not prevent access in temp directory");
                
            } catch (UnsupportedOperationException | IOException e) {
                // Symlink creation not supported on this system, skip test
                LOG.info("Symlink test skipped - not supported on this system");
            } finally {
                // Clean up
                Files.deleteIfExists(symlinkFile);
                Files.deleteIfExists(targetFile);
                Files.deleteIfExists(tempDir);
            }
        } catch (IOException e) {
            // Test environment doesn't support this test
            LOG.info("Symlink test skipped due to IO error: " + e.getMessage());
        }
    }
    
    @Test
    public void testNullFileInput() throws Exception {
        // Test null file input
        Exception exception = assertThrows(Exception.class, () -> {
            validateFilePathMethod.invoke(null, (File) null);
        });
        // Unwrap InvocationTargetException to get the actual IllegalArgumentException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof IllegalArgumentException, 
                  "Expected IllegalArgumentException but got: " + cause.getClass().getSimpleName());
        assertTrue(cause.getMessage().contains("File cannot be null"), 
                  "Should reject null file input");
    }
}