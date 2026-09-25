package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CompactMap.builder() specializes a PRE-COMPILED class template, and java-util runs on JDK 8, so that
 * template must be Java 8 bytecode. Through 4.112.0 it was generated without a target release on a
 * JDK 17 compiler: class-file version 61 with invokedynamic string concatenation, so every builder()
 * map threw UnsupportedClassVersionError on JDK 8. The test suite never ran on JDK 8, so nothing
 * noticed. These checks are made on the bytes themselves and therefore fail on ANY build JDK.
 */
class CompactMapBytecodeTemplateTest {

    private static final int JAVA_8_MAJOR_VERSION = 52;

    private static byte[] templateBytes() throws Exception {
        Class<?> generator = Class.forName("com.cedarsoftware.util.CompactMap$TemplateGenerator");
        Field f = generator.getDeclaredField("BYTECODE_TEMPLATE");
        f.setAccessible(true);
        return ByteUtilities.decodeTrusted((String) f.get(null));
    }

    @Test
    void templateIsJava8ClassFile() throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(templateBytes()));
        assertEquals(0xCAFEBABE, in.readInt(), "class-file magic");
        in.readUnsignedShort();   // minor
        assertEquals(JAVA_8_MAJOR_VERSION, in.readUnsignedShort(),
                "template must be Java 8 bytecode (regenerate with StandaloneBytecodeGenerator)");
    }

    @Test
    void templateUsesNoJava9PlusConstantPoolEntries() throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(templateBytes()));
        in.skipBytes(8);   // magic, minor, major
        int count = in.readUnsignedShort();
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            // 17 CONSTANT_Dynamic (Java 11), 18 CONSTANT_InvokeDynamic -- javac emits the latter for
            // string concatenation from JDK 9 on; 19/20 Module/Package (Java 9)
            assertFalse(tag == 17 || tag == 18 || tag == 19 || tag == 20,
                    "constant pool entry #" + i + " has tag " + tag + ", which JDK 8 cannot load");
            i += skipEntry(in, tag);
        }
    }

    /** Skips one constant-pool entry's body; returns 1 for the two-slot Long/Double entries. */
    private static int skipEntry(DataInputStream in, int tag) throws IOException {
        switch (tag) {
            case 1: in.skipBytes(in.readUnsignedShort()); return 0;       // Utf8
            case 3: case 4: in.skipBytes(4); return 0;                    // Integer, Float
            case 5: case 6: in.skipBytes(8); return 1;                    // Long, Double
            case 7: case 8: case 16: case 19: case 20: in.skipBytes(2); return 0;
            case 9: case 10: case 11: case 12: case 17: case 18: in.skipBytes(4); return 0;
            case 15: in.skipBytes(3); return 0;                           // MethodHandle
            default: throw new AssertionError("unknown constant-pool tag " + tag);
        }
    }

    @Test
    void builderMapWorks() {
        Map<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false).compactSize(4).sortedOrder().build();
        for (int i = 0; i < 10; i++) {
            map.put("K" + i, i);
        }
        assertEquals(10, map.size());
        assertEquals(Integer.valueOf(3), map.get("k3"));
        assertTrue(map.getClass().getName().startsWith("com.cedarsoftware.util.CompactMap$"));
    }
}
