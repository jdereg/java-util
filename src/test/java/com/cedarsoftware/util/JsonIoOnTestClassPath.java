package com.cedarsoftware.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * The json-io release java-util's tests run with. It trails java-util by one ({@code version.json-io} in the pom), so a
 * test of behavior that needs the json-io released alongside this java-util asks for it here, and runs once that
 * dependency moves up.
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
public final class JsonIoOnTestClassPath {
    private static final String VERSION = readVersion();

    private JsonIoOnTestClassPath() {
    }

    /** The json-io release on the test class path, from its jar's Maven metadata; null if it has none. */
    public static String version() {
        return VERSION;
    }

    /** True when the json-io on the test class path is {@code major.minor} or later. */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(VERSION, major, minor);
    }

    static boolean isAtLeast(String version, int major, int minor) {
        if (version == null) {
            return false;
        }
        String[] parts = version.split("[.-]");
        int vMajor = Integer.parseInt(parts[0]);
        int vMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return vMajor > major || (vMajor == major && vMinor >= minor);
    }

    private static String readVersion() {
        try (InputStream in = com.cedarsoftware.io.JsonIo.class.getResourceAsStream(
                "/META-INF/maven/com.cedarsoftware/json-io/pom.properties")) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version");
        } catch (Exception e) {
            return null;
        }
    }
}
