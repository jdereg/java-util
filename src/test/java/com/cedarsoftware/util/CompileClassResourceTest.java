package com.cedarsoftware.util;

import javax.tools.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

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

        @Override
        public StandardJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject> dl,
                                                              Locale locale, Charset charset) {
            StandardJavaFileManager fm = delegate.getStandardFileManager(dl, locale, charset);
            return new ForwardingJavaFileManager<>(fm) {
                @Override
                public void close() throws IOException {
                    closed.set(true);
                    super.close();
                }
            };
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

        @Override
        public String name() {
            return delegate.name();
        }
    }

    @Test
    public void testFileManagerClosed() throws Exception {
        JavaCompiler real = ToolProvider.getSystemJavaCompiler();
        TrackingJavaCompiler tracking = new TrackingJavaCompiler(real);
        try (MockedStatic<ToolProvider> mocked = mockStatic(ToolProvider.class)) {
            mocked.when(ToolProvider::getSystemJavaCompiler).thenReturn(tracking);

            Class<?> tmplGen = null;
            for (Class<?> cls : CompactMap.class.getDeclaredClasses()) {
                if (cls.getSimpleName().equals("TemplateGenerator")) {
                    tmplGen = cls;
                    break;
                }
            }
            assertNotNull(tmplGen);

            Method compile = tmplGen.getDeclaredMethod("compileClass", String.class, String.class);
            compile.setAccessible(true);
            String src = "package com.cedarsoftware.util; public class Tmp {}";
            Object clsObj = compile.invoke(null, "com.cedarsoftware.util.Tmp", src);
            assertNotNull(clsObj);
        }
        assertTrue(tracking.closed.get(), "FileManager should be closed");
    }
}
