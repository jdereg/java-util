package com.cedarsoftware.ncube

import com.cedarsoftware.util.ReflectionUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import java.lang.reflect.Field

import static org.junit.Assert.assertTrue
import static org.powermock.api.mockito.PowerMockito.mockStatic
import static org.powermock.api.mockito.PowerMockito.when

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
@RunWith(PowerMockRunner.class)
@PrepareForTest([ReflectionUtils.class, Field.class])
class TestObjectToMapIllegalAccessException
{
    static class dto
    {
        private Date when = new Date()
        String fname = "Albert";
        String lname = "Einstein";
    }

    @Test
    void testObjectToMapIllegalAccessException() throws Exception
    {
        def instance = new dto()
        mockStatic(ReflectionUtils.class)
        when(ReflectionUtils.getDeepDeclaredFields(instance.class)).thenThrow IllegalAccessException.class

        try
        {
            NCube.objectToMap instance
        }
        catch (RuntimeException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to access field"))
        }

    }
}
