package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A jar: URL wraps another URL, {@code jar:<inner URL>!/<entry>}, and IOUtilities judges the inner one too.
 * <p>
 * Only the outer protocol was judged, and "jar" is allowed. So {@code jar:ftp://host/x.jar!/a} had the JDK open an
 * FTP connection though ftp is not an allowed protocol, and {@code jar:file:/home/u/.aws/keys.zip!/credentials}
 * read an archive inside a directory that a file: URL is refused. The archive of a jar: URL must now be a local
 * file (file:, or Spring Boot's nested:), or come over a protocol that is allowed in its own right; and a local
 * archive may not lie in a hidden credentials directory or a Unix system directory.
 */
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class IOUtilitiesJarUrlTest {

    private static Path archive(Path file, String entry, String content) throws Exception {
        Files.createDirectories(file.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return file;
    }

    private static String read(URLConnection connection) throws Exception {
        try (InputStream in = IOUtilities.getInputStream(connection)) {
            return new String(IOUtilities.inputStreamToBytes(in), StandardCharsets.UTF_8);
        }
    }

    /** "passed" or the reason IOUtilities refuses the URL; a URL that passes and cannot be opened has passed. */
    private static String verdict(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setUseCaches(false);
            try (InputStream in = IOUtilities.getInputStream(connection)) {
                return "passed";
            }
        } catch (SecurityException e) {
            return "refused: " + e.getMessage();
        } catch (Exception e) {
            return "passed";
        }
    }

    @Test
    void anArchiveOnTheLocalDiskIsRead(@TempDir Path temp) throws Exception {
        Path jar = archive(temp.resolve("lib/app.jar"), "config/settings.txt", "from the jar");
        URLConnection connection = new URL("jar:" + jar.toUri() + "!/config/settings.txt").openConnection();
        connection.setUseCaches(false);
        assertEquals("from the jar", read(connection));
    }

    @Test
    void anArchiveInAHiddenCredentialsDirectoryIsRefused(@TempDir Path temp) throws Exception {
        for (String hidden : new String[]{".aws", ".ssh", ".gnupg", ".docker", ".AWS"}) {
            Path zip = archive(temp.resolve("home/" + hidden + "/keys.zip"), "credentials", "aws_secret_access_key=PLANTED");
            String url = "jar:" + zip.toUri() + "!/credentials";
            assertTrue(verdict(url).startsWith("refused: "), hidden + " -> " + verdict(url));
        }
    }

    @Test
    void theArchiveMustComeOverAnAllowedProtocol() {
        // ftp is not among the allowed protocols; wrapped in jar: it was opened
        String refused = verdict("jar:ftp://127.0.0.1:9/x.jar!/a");
        assertTrue(refused.startsWith("refused: ") && refused.contains("ftp"), refused);
        assertThrows(SecurityException.class,
                () -> IOUtilities.getInputStream(new URL("jar:ftp://127.0.0.1:9/x.jar!/a").openConnection()));
        // http and https are allowed in their own right, so an archive may come over them, as before
        assertEquals("passed", verdict("jar:http://127.0.0.1:9/x.jar!/a"));
    }

    @Test
    void aLocalArchiveIsJudgedByWhereItLies() {
        // the Unix system directories
        assertTrue(verdict("jar:file:/etc/app.jar!/a").startsWith("refused: "));
        assertTrue(verdict("jar:file:/proc/self/x.jar!/a").startsWith("refused: "));
        assertTrue(verdict("jar:file:/home/u/.ssh/x.jar!/a").startsWith("refused: "));
        assertTrue(verdict("jar:file:/home/u/%2Eaws/x.jar!/a").startsWith("refused: "));       // judged as opened
        // an application's own archives, wherever a deployment puts them: the word tests of a file: URL
        // ("private", a folder named windows) are not applied to the place an archive lies
        assertEquals("passed", verdict("jar:file:/opt/private-apps/lib/app.jar!/a"));
        assertEquals("passed", verdict("jar:file:/home/u/projects/windows/app.jar!/a"));
        assertEquals("passed", verdict("jar:file:/home/u/.m2/repository/com/x/lib.jar!/a"));
        assertEquals("passed", verdict("jar:file:/var/lib/app/app.jar!/BOOT-INF/classes/a.txt"));
        // the traversal check of a jar: URL, as before
        assertTrue(verdict("jar:file:/tmp/a/../b.jar!/a").startsWith("refused: "));
    }

    /**
     * Spring Boot 3.2 and later name a resource in a jar inside the application's jar with its own nested:
     * protocol. Its handler supplies the connection; here a stand-in does.
     */
    @Test
    void aSpringBootNestedArchiveIsRead() throws Exception {
        URLStreamHandler springBoot = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream("from a nested jar".getBytes(StandardCharsets.UTF_8));
                    }
                };
            }

            @Override
            protected void parseURL(URL u, String spec, int start, int limit) {
                setURL(u, "jar", "", -1, null, null, spec.substring(start, limit), null, null);
            }
        };
        URL nested = new URL(null, "jar:nested:/opt/app/app.jar/!BOOT-INF/lib/lib.jar!/config/aliases.txt", springBoot);
        assertEquals("from a nested jar", read(nested.openConnection()));

        URL hidden = new URL(null, "jar:nested:/home/u/.ssh/app.jar/!BOOT-INF/lib/lib.jar!/x", springBoot);
        assertThrows(SecurityException.class, () -> IOUtilities.getInputStream(hidden.openConnection()));
    }
}
