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
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Standalone utility to generate bytecode template for CompactMap.
 * This generates the hex string that will be embedded in CompactMap.java.
 *
 * Run with: javac StandaloneBytecodeGenerator.java && java StandaloneBytecodeGenerator
 */
public class StandaloneBytecodeGenerator {

    // Template source - uses static fields for configuration
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
        "    private static String _innerMapClassName;\n" +
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
        "            // Handle CaseInsensitiveMap specially - it needs an inner map\n" +
        "            if (_innerMapClassName != null && \"com.cedarsoftware.util.CaseInsensitiveMap\".equals(_mapClassName)) {\n" +
        "                Class<?> innerMapClass = Class.forName(_innerMapClassName);\n" +
        "                Map innerMap;\n" +
        "                // Create inner map with capacity if possible\n" +
        "                try {\n" +
        "                    java.lang.reflect.Constructor<?> ctor = innerMapClass.getConstructor(int.class);\n" +
        "                    innerMap = (Map) ctor.newInstance(_compactSize + 1);\n" +
        "                } catch (NoSuchMethodException e) {\n" +
        "                    innerMap = (Map) innerMapClass.getDeclaredConstructor().newInstance();\n" +
        "                }\n" +
        "                // Create CaseInsensitiveMap with the inner map\n" +
        "                java.lang.reflect.Constructor<?> ciCtor = mapClass.getConstructor(Map.class);\n" +
        "                return (Map) ciCtor.newInstance(innerMap);\n" +
        "            }\n" +
        "            // Try capacity constructor first (HashMap, LinkedHashMap, etc.)\n" +
        "            try {\n" +
        "                java.lang.reflect.Constructor<?> ctor = mapClass.getConstructor(int.class);\n" +
        "                return (Map) ctor.newInstance(_compactSize + 1);\n" +
        "            } catch (NoSuchMethodException e) {\n" +
        "                // Fall through\n" +
        "            }\n" +
        "            // Try Comparator constructor for sorted maps\n" +
        "            if (\"sorted\".equals(_ordering) || \"reverse\".equals(_ordering)) {\n" +
        "                try {\n" +
        "                    java.lang.reflect.Constructor<?> ctor = mapClass.getConstructor(Comparator.class);\n" +
        "                    Comparator<Object> comp = new CompactMap.CompactMapComparator(!_caseSensitive, \"reverse\".equals(_ordering));\n" +
        "                    return (Map) ctor.newInstance(comp);\n" +
        "                } catch (NoSuchMethodException e) {\n" +
        "                    // Fall through\n" +
        "                }\n" +
        "            }\n" +
        "            // Default constructor\n" +
        "            return (Map) mapClass.getDeclaredConstructor().newInstance();\n" +
        "        } catch (Exception e) {\n" +
        "            throw new IllegalStateException(\"Failed to create map: \" + _mapClassName, e);\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    public static final String CLASS_NAME_PLACEHOLDER = "0000000000000000";
    public static final String TEMPLATE_CLASS_NAME = "com.cedarsoftware.util.CompactMap$" + CLASS_NAME_PLACEHOLDER;

    public static void main(String[] args) {
        try {
            byte[] bytecode = compileToBytecode();
            String hexString = bytesToHex(bytecode);

            System.out.println("// Bytecode template for CompactMap generated subclasses");
            System.out.println("// Template class name: " + TEMPLATE_CLASS_NAME);
            System.out.println("// Placeholder: " + CLASS_NAME_PLACEHOLDER);
            System.out.println("// Bytecode length: " + bytecode.length + " bytes");
            System.out.println();
            System.out.println("private static final String BYTECODE_TEMPLATE =");

            // Print in 76-char lines for readability
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
            String placeholderHex = stringToHex(CLASS_NAME_PLACEHOLDER);
            System.out.println("// Placeholder hex: " + placeholderHex);
            int placeholderIndex = hexString.indexOf(placeholderHex);
            System.out.println("// Placeholder byte offset: " + (placeholderIndex / 2));

            // Find all occurrences of the placeholder
            System.out.println("\n// All placeholder locations:");
            int idx = 0;
            while ((idx = hexString.indexOf(placeholderHex, idx)) != -1) {
                System.out.println("//   Byte offset: " + (idx / 2));
                idx += placeholderHex.length();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] compileToBytecode() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler available. Run with JDK, not JRE.");
        }

        Map<String, ByteArrayOutputStream> classOutputs = new HashMap<>();
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);

        JavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(stdFileManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className,
                    JavaFileObject.Kind kind, FileObject sibling) {
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

        JavaFileObject sourceFile = new SimpleJavaFileObject(
                URI.create("string:///" + TEMPLATE_CLASS_NAME.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return TEMPLATE_SOURCE;
            }
        };

        // Get the classpath from the classloader to find CompactMap
        String classpath = getClasspathFromClassLoader();

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null,
                Arrays.asList("-proc:none", "-classpath", classpath),
                null,
                Collections.singletonList(sourceFile));

        if (!task.call()) {
            throw new IllegalStateException("Compilation failed");
        }

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
        return bytesToHex(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String getClasspathFromClassLoader() {
        Set<String> paths = new HashSet<>();
        ClassLoader cl = StandaloneBytecodeGenerator.class.getClassLoader();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) cl).getURLs()) {
                    if ("file".equals(url.getProtocol())) {
                        paths.add(new File(url.getPath()).getAbsolutePath());
                    }
                }
            }
            cl = cl.getParent();
        }

        // Also try to get from java.class.path as fallback
        String sysClasspath = System.getProperty("java.class.path");
        if (sysClasspath != null && !sysClasspath.isEmpty()) {
            paths.addAll(Arrays.asList(sysClasspath.split(File.pathSeparator)));
        }

        return String.join(File.pathSeparator, paths);
    }
}