package com.cedarsoftware.util;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.CompactMap.DEFAULT_COMPACT_SIZE;
import static com.cedarsoftware.util.CompactMap.SORTED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for miscellaneous CompactMap methods.
 */
public class CompactMapMethodsTest {

    /**
     * DISABLED: This test was used to verify the custom JavaFileManager that captured bytecode
     * during runtime Java compilation. The old TemplateGenerator used JavaCompiler to compile
     * template classes at runtime.
     * <p>
     * The current implementation uses a pre-compiled bytecode template (BYTECODE_TEMPLATE in
     * CompactMap.TemplateGenerator) that gets patched at runtime, eliminating the need for
     * runtime compilation.
     * <p>
     * TO REGENERATE THE BYTECODE TEMPLATE:
     * 1. Use StandaloneBytecodeGenerator (or similar tool) to compile a template class
     * 2. The template class should be named: com.cedarsoftware.util.CompactMap$0000000000000000
     * 3. It should extend CompactMap and override: isCaseInsensitive(), compactSize(),
     *    getSingleValueKey(), getOrdering(), getNewMap()
     * 4. Convert the compiled .class file to hex string for BYTECODE_TEMPLATE
     * <p>
     * This test is kept for historical reference in case the bytecode generation approach
     * needs to be revisited or debugged.
     */
    @Test
    @Disabled("Old runtime compilation approach replaced with pre-compiled bytecode template")
    public void testGetJavaFileForOutputAndOpenOutputStream() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler required for test");
        StandardJavaFileManager std = compiler.getStandardFileManager(null, null, null);

        Class<?> fmClass = Class.forName("com.cedarsoftware.util.CompactMap$TemplateGenerator$1");
        Constructor<?> ctor = fmClass.getDeclaredConstructor(StandardJavaFileManager.class, Map.class);
        ctor.setAccessible(true);
        Map<String, ByteArrayOutputStream> outputs = new HashMap<>();
        Object fileManager = ctor.newInstance(std, outputs);

        Method method = fmClass.getMethod("getJavaFileForOutput",
                JavaFileManager.Location.class, String.class,
                JavaFileObject.Kind.class, javax.tools.FileObject.class);

        JavaFileObject classObj = (JavaFileObject) method.invoke(fileManager,
                StandardLocation.CLASS_OUTPUT, "a.b.Test", JavaFileObject.Kind.CLASS, null);
        OutputStream out = (OutputStream) classObj.getClass().getMethod("openOutputStream").invoke(classObj);
        assertSame(outputs.get("a.b.Test"), out);
        out.write(new byte[]{1, 2});
        out.close();
        assertArrayEquals(new byte[]{1, 2}, outputs.get("a.b.Test").toByteArray());

        int sizeBefore = outputs.size();
        JavaFileObject srcObj = (JavaFileObject) method.invoke(fileManager,
                StandardLocation.SOURCE_OUTPUT, "a.b.Test", JavaFileObject.Kind.SOURCE, null);
        assertNotNull(srcObj);
        assertEquals(sizeBefore, outputs.size(), "non-class output should not modify map");

        std.close();
    }

    @Test
    public void testIsDefaultCompactMap() {
        CompactMap<String, String> def = new CompactMap<>();
        assertTrue(def.isDefaultCompactMap(), "Default configuration should return true");

        CompactMap<String, String> diffSize = new CompactMap<String, String>() {
            @Override
            protected int compactSize() { return DEFAULT_COMPACT_SIZE + 1; }
        };
        assertFalse(diffSize.isDefaultCompactMap());

        CompactMap<String, String> caseIns = new CompactMap<String, String>() {
            @Override
            protected boolean isCaseInsensitive() { return true; }
        };
        assertFalse(caseIns.isDefaultCompactMap());

        CompactMap<String, String> diffOrder = new CompactMap<String, String>() {
            @Override
            protected String getOrdering() { return SORTED; }
        };
        assertFalse(diffOrder.isDefaultCompactMap());

        CompactMap<String, String> diffKey = new CompactMap<String, String>() {
            @Override
            protected String getSingleValueKey() { return "uuid"; }
        };
        assertFalse(diffKey.isDefaultCompactMap());

        CompactMap<String, String> diffMap = new CompactMap<String, String>() {
            @Override
            protected Map<String, String> getNewMap() { return new TreeMap<>(); }
        };
        assertFalse(diffMap.isDefaultCompactMap());
    }

    @Test
    public void testMinusThrows() {
        CompactMap<String, String> map = new CompactMap<>();
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> map.minus("foo"));
        assertTrue(ex.getMessage().contains("minus"));
    }

    @Test
    public void testPlusThrows() {
        CompactMap<String, String> map = new CompactMap<>();
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> map.plus("foo"));
        assertTrue(ex.getMessage().contains("plus"));
    }
}
