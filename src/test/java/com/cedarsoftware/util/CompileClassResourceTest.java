package com.cedarsoftware.util;

import javax.tools.*;
import javax.lang.model.SourceVersion;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompileClassResourceTest {
    static class TrackingJavaCompiler implements JavaCompiler {
        private final JavaCompiler delegate;
        final AtomicBoolean closed = new AtomicBoolean(false);

        TrackingJavaCompiler(JavaCompiler delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompilationTask getTask(Writer out, JavaFileManager fileManager,
                                       DiagnosticListener<? super JavaFileObject> diagnosticListener,
                                       Iterable<String> options, Iterable<String> classes,
                                       Iterable<? extends JavaFileObject> compilationUnits) {
            return delegate.getTask(out, fileManager, diagnosticListener, options, classes, compilationUnits);
        }

        // Inner class that properly implements StandardJavaFileManager
        private class TrackingStandardJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
                implements StandardJavaFileManager {

            TrackingStandardJavaFileManager(StandardJavaFileManager fileManager) {
                super(fileManager);
            }

            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }

            // Delegate StandardJavaFileManager specific methods
            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
                return fileManager.getJavaFileObjectsFromFiles(files);
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
                return fileManager.getJavaFileObjects(files);
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
                return fileManager.getJavaFileObjectsFromStrings(names);
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
                return fileManager.getJavaFileObjects(names);
            }

            @Override
            public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
                fileManager.setLocation(location, path);
            }

            @Override
            public Iterable<? extends File> getLocation(Location location) {
                return fileManager.getLocation(location);
            }
        }

        @Override
        public StandardJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject> dl,
                                                              Locale locale, Charset charset) {
            StandardJavaFileManager fm = delegate.getStandardFileManager(dl, locale, charset);
            return new TrackingStandardJavaFileManager(fm);
        }

        @Override
        public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
            return delegate.run(in, out, err, arguments);
        }

        @Override
        public Set<SourceVersion> getSourceVersions() {
            return delegate.getSourceVersions();
        }

        @Override
        public int isSupportedOption(String option) {
            return delegate.isSupportedOption(option);
        }
    }

    @Test
    public void testFileManagerClosed() throws Exception {
        // Get the real compiler
        JavaCompiler realCompiler = ToolProvider.getSystemJavaCompiler();

        // Create our tracking wrapper
        TrackingJavaCompiler trackingCompiler = new TrackingJavaCompiler(realCompiler);

        // Get file manager from our tracking compiler
        StandardJavaFileManager fileManager = trackingCompiler.getStandardFileManager(null, null, null);

        // Compile some simple code using the file manager
        String source = "public class TestClass { public static void main(String[] args) {} }";
        JavaFileObject sourceFile = new SimpleJavaFileObject(
                URI.create("string:///TestClass.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        // Create compilation task
        JavaCompiler.CompilationTask task = trackingCompiler.getTask(
                null, fileManager, null, null, null,
                java.util.Collections.singletonList(sourceFile)
        );

        // Compile
        task.call();

        // Close the file manager
        fileManager.close();

        // Verify it was closed
        assertTrue(trackingCompiler.closed.get(), "FileManager should be closed");
    }
}