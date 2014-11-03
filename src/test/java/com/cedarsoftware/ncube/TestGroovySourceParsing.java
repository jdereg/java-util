package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Test Groovy Source code parsing, including finding n-cube names within source (used with @, $, or APIs
 * like runRuleCube() or getCube(), as well as coordinate keys.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestGroovySourceParsing
{
    @After
    public void tearDown() throws Exception
    {
        TestNCube.tearDown();
    }

    @Test
    public void testFindCubeName()
    {
        NCube cube = NCubeManager.getNCubeFromResource("inlineExpression.json");
        Set<String> names = cube.getReferencedCubeNames();

        assertTrue(names.contains("Beretta"));
        assertTrue(names.contains("Bersa"));
        assertTrue(names.contains("Browning"));
        assertTrue(names.contains("Car"));
        assertTrue(names.contains("Colt"));
        assertTrue(names.contains("FNHerstal"));
        assertTrue(names.contains("Glock"));
        assertTrue(names.contains("Kimber"));
        assertTrue(names.contains("Marlin"));
        assertTrue(names.contains("Mossberg"));
        assertTrue(names.contains("Remington"));
        assertTrue(names.contains("RockRiverArms"));
        assertTrue(names.contains("Sig"));
        assertTrue(names.contains("SnW"));
        assertTrue(names.contains("Springfield"));
        assertTrue(names.contains("Winchester"));
    }
}
