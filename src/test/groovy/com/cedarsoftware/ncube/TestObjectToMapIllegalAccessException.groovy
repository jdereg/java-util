package com.cedarsoftware.ncube

import com.cedarsoftware.util.ReflectionUtils
import groovy.mock.interceptor.MockFor
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

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
class TestObjectToMapIllegalAccessException
{
    static class dto
    {
        private Date when = new Date()
        String fname = "Albert";
        String lname = "Einstein";
    }

    @Test
    void testObjectToMapIllegalAccessException()
    {
        MockFor mock = new MockFor(ReflectionUtils);
        mock.demand.getDeepDeclaredFields(0..1) { throw new IllegalAccessException("foo") }

        mock.use
        {
            try {
                NCube.objectToMap new dto()
            }
            catch (RuntimeException e) {
                assertEquals("foo", e.message);
                assertTrue(e.message.toLowerCase().contains("failed to access field"))
                assertTrue(e.cause.message.toLowerCase().contains("foo"))
            }
        }
    }
}
