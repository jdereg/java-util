package com.cedarsoftware.util.convert;

import java.lang.ref.WeakReference;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A conversion added between classes of two unrelated loaders (OSGi bundles, two applications' classes) is kept and
 * used: they have no class to keep it on, so the Converter holds it itself, as long as it lives. Since the pair caches
 * stopped keeping classes alive, it was dropped -- the conversion was "unsupported". The loaders still go once the
 * Converter does. (A class of its own: its lambdas take a Converter, and the throwaway loaders, which see only the JDK,
 * load ConverterClassLoaderLeakTest when they are asked for its Fixture's enclosing class -- on JDK 8 that fails if
 * that class holds such a method.)
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
class ConverterUnrelatedLoadersTest {

    @Test
    void aConversionAddedAcrossUnrelatedLoadersIsKeptAndUsed() throws Exception {
        WeakReference<?>[] loaders = convertAcrossTwoLoaders();
        ConverterClassLoaderLeakTest.gcUntilCleared(loaders);
        assertNull(loaders[0].get(), "the conversion kept the first loader alive after the Converter went");
        assertNull(loaders[1].get(), "the conversion kept the second loader alive after the Converter went");
    }

    private static WeakReference<?>[] convertAcrossTwoLoaders() throws Exception {
        URLClassLoader first = ConverterClassLoaderLeakTest.throwawayLoader();
        URLClassLoader second = ConverterClassLoaderLeakTest.throwawayLoader();
        Class<?> source = Class.forName(ConverterClassLoaderLeakTest.FIXTURE, false, first);
        Class<?> target = Class.forName(ConverterClassLoaderLeakTest.FIXTURE, false, second);
        assertNotSame(source, target);
        Object converted = target.getConstructor().newInstance();

        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion((from, c) -> converted, source, target);
        assertTrue(converter.isConversionSupportedFor(source, target));
        assertSame(converted, converter.convert(source.getConstructor().newInstance(), target));

        DefaultConverterOptions options = new DefaultConverterOptions();     // and as an override in the options
        options.getConverterOverrides().put(Converter.pair(source, target), (from, c) -> converted);
        assertSame(converted, new Converter(options).convert(source.getConstructor().newInstance(), target));

        first.close();
        second.close();
        return new WeakReference<?>[]{new WeakReference<>(first), new WeakReference<>(second)};
    }
}
