package com.cedarsoftware.ncube

import com.cedarsoftware.util.UrlUtilities
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
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

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest([UrlUtilities.class])
public class TestBinaryUrlCmd
{
    @Test
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception
    {
        Class c = BinaryUrlCmd.class
        Constructor<BinaryUrlCmd> con = c.getDeclaredConstructor()
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE)
        con.accessible = true
        Assert.assertNotNull con.newInstance()
    }

    @Test
    public void testSimpleFetchException()
    {
        NCube cube = NCubeBuilder.getTestNCube2D true
        BinaryUrlCmd cmd = new BinaryUrlCmd("http://www.cedarsoftware.com", false)

        PowerMockito.mockStatic UrlUtilities.class
        PowerMockito.when(UrlUtilities.getContentFromUrl(any(URL.class), (boolean) eq(true))).thenThrow IOException.class

        def args = [ncube:cube]

        try
        {
            cmd.simpleFetch(args)
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to load binary content"))
        }
    }
}
