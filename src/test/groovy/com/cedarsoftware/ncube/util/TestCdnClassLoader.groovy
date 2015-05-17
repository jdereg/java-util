package com.cedarsoftware.ncube.util

import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestCdnClassLoader
{
    @Test
    void testLocalResources()
    {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.classLoader, true, true)
        assert testLoader1.isLocalOnlyResource("META-INF/org.codehaus.groovy.transform.ASTTransformation")
        assert testLoader1.isLocalOnlyResource("ncube/grv/exp/GroovyExpression")
        assert testLoader1.isLocalOnlyResource("ncube/grv/method/GroovyMethod")
        assert testLoader1.isLocalOnlyResource("FooBeanInfo.groovy")
        assert testLoader1.isLocalOnlyResource("FooCustomizer.groovy")

        CdnClassLoader testLoader2 = new CdnClassLoader(TestCdnClassLoader.class.classLoader, false, false)
        assert testLoader2.isLocalOnlyResource("META-INF/org.codehaus.groovy.transform.ASTTransformation")
        assert testLoader2.isLocalOnlyResource("ncube/grv/exp/NCubeGroovyExpression.groovy")
        assert testLoader2.isLocalOnlyResource("ncube/grv/method/NCubeGroovyController.groovy")
        assert !testLoader2.isLocalOnlyResource("ncube/grv/closure/NCubeGroovyController.groovy")
        assert !testLoader2.isLocalOnlyResource("FooBeanInfo.groovy")
        assert !testLoader2.isLocalOnlyResource("FooCustomizer.groovy")
    }

    @Test
    void testGetResource()
    {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.classLoader, true, true)
        assertNotNull testLoader1.getResource("cdnRouter.json")
        assertNull testLoader1.getResource("ncube/grv/method/NCubeGroovyController.class")
        assertNotNull TestCdnClassLoader.class.classLoader.getResource("ncube/grv/method/NCubeGroovyController.class")
    }

    @Test
    void testGetResources()
    {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.classLoader, true, true)
        assert testLoader1.getResources("cdnRouter.json").hasMoreElements()
        assert !testLoader1.getResources("ncube/grv/method/NCubeGroovyController.class").hasMoreElements()
        assert TestCdnClassLoader.class.classLoader.getResources("ncube/grv/method/NCubeGroovyController.class").hasMoreElements()
    }

    @Test(expected = NoSuchElementException.class)
    void testGetResourcesWithLocalResource()
    {
        new CdnClassLoader(TestCdnClassLoader.class.classLoader, true, true).getResources("ncube/grv/method/NCubeGroovyController.class").nextElement()
    }
}