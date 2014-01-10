/*
 *         Copyright (c) Cedar Software LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.test;

import com.cedarsoftware.lang.reflect.ConstructorUtilities;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class Asserter {
    public static <T> void assertClassOnlyHasAPrivateDefaultConstructor(Class<T> c) {
        // Ensures coverage through private constructor and that private
        // constructor exists.
        Constructor<?>[] cons = c.getDeclaredConstructors();
        Assert.assertEquals(1, cons.length);
        Assert.assertTrue(Modifier.isPrivate(cons[0].getModifiers()));

        Assert.assertTrue(Modifier.isPublic(c.getModifiers()));
        Assert.assertTrue(Modifier.isFinal(c.getModifiers()));
        Assert.assertNotNull(ConstructorUtilities.callPrivateConstructor(c));
    }
}
