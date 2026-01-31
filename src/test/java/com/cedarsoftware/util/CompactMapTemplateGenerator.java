package com.cedarsoftware.util;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to generate the bytecode template for CompactMap.
 * Run this once to generate the hex string constant to embed in CompactMap.
 *
 * This class is only used during development to generate the bytecode template.
 * It is NOT used at runtime.
 */
public class CompactMapTemplateGenerator {

    // Template class source - uses static fields for all configuration
    // The class name uses a 16-char placeholder that will be patched at runtime
    private static final String TEMPLATE_SOURCE =
        "package com.cedarsoftware.util;\n" +
        "\n" +
        "import java.util.Comparator;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "public class CompactMap$0000000000000000 extends CompactMap {\n" +
        "    // Static fields - injected after class loading\n" +
        "    private static boolean _caseSensitive;\n" +
        "    private static int _compactSize;\n" +
        "    private static String _singleKey;\n" +
        "    private static String _ordering;\n" +
        "    private static String _mapClassName;\n" +
        "\n" +
        "    @Override\n" +
        "    protected boolean isCaseInsensitive() {\n" +
        "        return !_caseSensitive;\n" +
        "    }\n" +
        "\n" +
        "    @Override\n" +
        "    protected int compactSize() {\n" +
        "        return _compactSize;\n" +
        "    }\n" +
        "\n" +
        "    @Override\n" +
        "    protected Object getSingleValueKey() {\n" +
        "        return _singleKey;\n" +
        "    }\n" +
        "\n" +
        "    @Override\n" +
        "    protected String getOrdering() {\n" +
        "        return _ordering;\n" +
        "    }\n" +
        "\n" +
        "    @Override\n" +
        "    protected Map getNewMap() {\n" +
        "        try {\n" +
        "            Class<?> mapClass = Class.forName(_mapClassName);\n" +
        "            // Try capacity constructor first (for HashMap, LinkedHashMap, etc.)\n" +
        "            try {\n" +
        "                java.lang.reflect.Constructor<?> capacityCtor = mapClass.getConstructor(int.class);\n" +
        "                return (Map) capacityCtor.newInstance(_compactSize + 1);\n" +
        "            } catch (NoSuchMethodException e) {\n" +
        "                // Fall through to try Comparator constructor\n" +
        "            }\n" +
        "            // Try Comparator constructor (for TreeMap with custom ordering)\n" +
        "            if (\"sorted\".equals(_ordering) || \"reverse\".equals(_ordering)) {\n" +
        "                try {\n" +
        "                    java.lang.reflect.Constructor<?> comparatorCtor = mapClass.getConstructor(Comparator.class);\n" +
        "                    Comparator<Object> comp = new CompactMapComparator(!_caseSensitive, \"reverse\".equals(_ordering));\n" +
        "                    return (Map) comparatorCtor.newInstance(comp);\n" +
        "                } catch (NoSuchMethodException e) {\n" +
        "                    // Fall through to default constructor\n" +
        "                }\n" +
        "            }\n" +
        "            // Default constructor\n" +
        "            return (Map) mapClass.getDeclaredConstructor().newInstance();\n" +
        "        } catch (Exception e) {\n" +
        "            throw new IllegalStateException(\"Failed to create map instance: \" + _mapClassName, e);\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    // The placeholder in class name that will be patched
    public static final String CLASS_NAME_PLACEHOLDER = "0000000000000000";

    // Full class name with placeholder
    public static final String TEMPLATE_CLASS_NAME = "com.cedarsoftware.util.CompactMap$" + CLASS_NAME_PLACEHOLDER;

    public static void main(String[] args) {
        try {
            byte[] bytecode = compileToBytecode();
            String hexString = bytesToHex(bytecode);

            System.out.println("// Bytecode template for CompactMap generated subclasses");
            System.out.println("// Template class name: " + TEMPLATE_CLASS_NAME);
            System.out.println("// Placeholder to patch: " + CLASS_NAME_PLACEHOLDER);
            System.out.println("// Bytecode length: " + bytecode.length + " bytes");
            System.out.println();
            System.out.println("private static final String BYTECODE_TEMPLATE = ");

            // Print in 80-char lines
            int lineLen = 76;
            for (int i = 0; i < hexString.length(); i += lineLen) {
                int end = Math.min(i + lineLen, hexString.length());
                String line = hexString.substring(i, end);
                if (end >= hexString.length()) {
                    System.out.println("    \"" + line + "\";");
                } else {
                    System.out.println("    \"" + line + "\" +");
                }
            }

            System.out.println();
            System.out.println("// To find placeholder location in bytecode:");
            String placeholderHex = stringToHex(CLASS_NAME_PLACEHOLDER);
            System.out.println("// Placeholder hex: " + placeholderHex);
            int placeholderIndex = hexString.indexOf(placeholderHex);
            System.out.println("// Placeholder byte offset: " + (placeholderIndex / 2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] compileToBytecode() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler available. Run with JDK, not JRE.");
        }

        // Create in-memory file manager
        Map<String, ByteArrayOutputStream> classOutputs = new HashMap<>();
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);

        JavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(stdFileManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className,
                    JavaFileObject.Kind kind, FileObject sibling) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                classOutputs.put(className, baos);
                return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + ".class"), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        return baos;
                    }
                };
            }
        };

        // Create source file object
        JavaFileObject sourceFile = new SimpleJavaFileObject(
                URI.create("string:///" + TEMPLATE_CLASS_NAME.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return TEMPLATE_SOURCE;
            }
        };

        // Compile
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null,
                Collections.singletonList("-proc:none"),
                null,
                Collections.singletonList(sourceFile));

        if (!task.call()) {
            throw new IllegalStateException("Compilation failed");
        }

        // Get bytecode
        ByteArrayOutputStream baos = classOutputs.get(TEMPLATE_CLASS_NAME);
        if (baos == null) {
            throw new IllegalStateException("No bytecode generated for " + TEMPLATE_CLASS_NAME);
        }
        return baos.toByteArray();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    private static String stringToHex(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return bytesToHex(bytes);
    }
}