package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleWiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The packaged jar works as a bundle in an OSGi framework (Apache Felix), run by failsafe after package. It could not
 * even resolve there in any release from at least 4.102.0: its manifest required JDK-internal packages no framework
 * exports. Inside the framework the bundle converts, finds the JDK's own classes by name through its loader (an OSGi
 * bundle's loader sees only java.* and its imports), passes over a context class loader that cannot see it (the
 * launcher's, as here), and builds CompactMap templates on its own loader.
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
class OsgiBundleIT {

    private static Framework framework;
    private static Bundle bundle;

    @BeforeAll
    static void startFramework() throws Exception {
        String jar = System.getProperty("java-util.jar");
        assertNotNull(jar, "failsafe sets java-util.jar to the packaged jar");
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE, Files.createTempDirectory("felix-java-util").toString());
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        framework = ServiceLoader.load(FrameworkFactory.class).iterator().next().newFramework(config);
        framework.start();
        bundle = framework.getBundleContext().installBundle(new File(jar).toURI().toString());
        bundle.start();
    }

    @AfterAll
    static void stopFramework() throws Exception {
        if (framework != null) {
            framework.stop();
            framework.waitForStop(10_000);
        }
    }

    private static Object call(Class<?> type, String method, Object target, Class<?>[] parameters, Object... args)
            throws Exception {
        Method m = type.getMethod(method, parameters);
        return m.invoke(target, args);
    }

    private static ClassLoader bundleLoader() {
        return bundle.adapt(BundleWiring.class).getClassLoader();
    }

    @Test
    void theBundleResolvesAndStarts() {
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    void itConvertsInsideTheFramework() throws Exception {
        Class<?> converterClass = bundle.loadClass("com.cedarsoftware.util.convert.Converter");
        Class<?> optionsClass = bundle.loadClass("com.cedarsoftware.util.convert.ConverterOptions");
        Object options = bundle.loadClass("com.cedarsoftware.util.convert.DefaultConverterOptions")
                .getConstructor().newInstance();
        Object converter = converterClass.getConstructor(optionsClass).newInstance(options);
        Class<?>[] convert = {Object.class, Class.class};
        assertEquals(42, call(converterClass, "convert", converter, convert, "42", Integer.class));
        assertEquals(new java.math.BigDecimal("0.1"), call(converterClass, "convert", converter, convert, 0.1d,
                java.math.BigDecimal.class));
        assertEquals(java.time.MonthDay.of(12, 25), call(converterClass, "convert", converter, convert, "12-25",
                java.time.MonthDay.class));
    }

    /** The class of every TimeZone is a JDK internal no bundle imports: it is found through the bundle's loader. */
    @Test
    void itFindsTheJdksOwnClassesThroughTheBundlesLoader() throws Exception {
        Class<?> classUtilities = bundle.loadClass("com.cedarsoftware.util.ClassUtilities");
        Class<?> zoneInfo = java.util.TimeZone.getTimeZone("America/New_York").getClass();
        assertSame(zoneInfo, call(classUtilities, "forName", null, new Class<?>[] {String.class, ClassLoader.class},
                zoneInfo.getName(), bundleLoader()));
    }

    /**
     * The launcher's context class loader holds its own copy of java-util (the test class path), not the bundle's:
     * getClassLoader(anchor) passes it over for the bundle's loader.
     */
    @Test
    void aContextLoaderThatCannotSeeTheBundleIsPassedOver() throws Exception {
        Class<?> classUtilities = bundle.loadClass("com.cedarsoftware.util.ClassUtilities");
        assertNotSame(ClassUtilities.class, classUtilities);
        Object loader = call(classUtilities, "getClassLoader", null, new Class<?>[] {Class.class}, classUtilities);
        assertSame(bundleLoader(), loader);
    }

    /** A CompactMap from the builder: its generated class is defined on the bundle's loader, and works. */
    @Test
    void itBuildsCompactMapsInsideTheFramework() throws Exception {
        Class<?> compactMap = bundle.loadClass("com.cedarsoftware.util.CompactMap");
        Object builder = call(compactMap, "builder", null, new Class<?>[0]);
        builder = call(builder.getClass(), "caseSensitive", builder, new Class<?>[] {boolean.class}, false);
        Object map = call(builder.getClass(), "build", builder, new Class<?>[0]);
        call(Map.class, "put", map, new Class<?>[] {Object.class, Object.class}, "Key", 1);
        assertEquals(1, call(Map.class, "get", map, new Class<?>[] {Object.class}, "KEY"));
    }
}
