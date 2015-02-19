package com.cedarsoftware.ncube

import org.junit.Test

/**
 * NCube Advice Tests (Advice often used for security annotations on Groovy Methods / Expressions)
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
class TestDelta
{
    @Test
    void testDeltaApis()
    {
        Delta x = new Delta(Delta.Location.AXIS, Delta.Type.ADD, "foo")
        assert x.location == Delta.Location.AXIS
        assert x.type == Delta.Type.ADD
        assert x.description == 'foo'
        assert x.toString() == 'foo'
    }
}
