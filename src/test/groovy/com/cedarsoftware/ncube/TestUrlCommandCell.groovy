package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.util.CdnRouter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.fail
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.never
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

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

public class TestUrlCommandCell
{
    @Test
    public void testDefaultConstructorIsProtected() throws Exception
    {
        Class c = UrlCommandCell.class
        Constructor<UrlCommandCell> con = c.getDeclaredConstructor()
        assert Modifier.PROTECTED == (con.modifiers & Modifier.PROTECTED)
        assert Modifier.ABSTRACT == (c.modifiers & Modifier.ABSTRACT)
    }

    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    public void testCachingInputStreamRead() throws Exception
    {
        String s = 'foo-bar';
        ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes('UTF-8'))
        UrlCommandCell.CachingInputStream input = new UrlCommandCell.CachingInputStream(stream)

        assert 102 == input.read()
        assert 111 == input.read()
        assert 111 == input.read()
        assert 45 == input.read()
        assert 98 == input.read()
        assert 97 == input.read()
        assert 114 == input.read()
        assert -1 == input.read()
    }

    @Test
    public void testCachingInputStreamReadBytes() throws Exception
    {
        String s = 'foo-bar';
        ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes('UTF-8'))
        UrlCommandCell.CachingInputStream input = new UrlCommandCell.CachingInputStream(stream)

        byte[] bytes = new byte[7];
        assert 7 == input.read(bytes, 0, 7)
        assert 'foo-bar' == new String(bytes, 'UTF-8')
        assert -1 == input.read(bytes, 0, 7)
    }

    private static class EmptyUrlCommandCell extends UrlCommandCell
    {
        EmptyUrlCommandCell(String cmd, String url, boolean cacheable)
        {
            super(cmd, url, cacheable)
        }

        protected Object executeInternal(Object data, Map<String, Object> ctx)
        {
            return null;
        }
    }

    @Test
    public void testBadUrlCommandCell()
    {
        try
        {
            new EmptyUrlCommandCell('', null, false)
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('cannot be empty')
        }

        UrlCommandCell cell = new EmptyUrlCommandCell("println 'hello'", null, false)

        // Nothing more than covering method calls and lines.  These methods
        // do nothing, therefore there is nothing to
        cell.getCubeNamesFromCommandText null
        cell.getScopeKeys null

        assert !cell.equals('String')

        def coord = ['content.type':'view','content.name':'badProtocol']
        NCube cube = NCubeManager.getNCubeFromResource 'cdnRouterTest.json'
        try
        {
            cube.getCell coord
            fail 'Should not make it here'
        }
        catch (Exception e)
        {
            assert e.message.toLowerCase().contains('error occurred executing')
        }

        coord['content.name'] = 'badRelative'
        try
        {
            cube.getCell coord
            fail 'Should not make it here'
        }
        catch (Exception e)
        {
            assert e.message.contains('not found on axis')
        }
    }

    @Test
    public void testProxyFetchSocketTimeout() throws Exception
    {
        UrlCommandCell cell = new StringUrlCmd('http://www.cedarsoftware.com', false)

        NCube ncube = Mockito.mock NCube.class
        HttpServletResponse response = Mockito.mock HttpServletResponse.class
        HttpServletRequest request = Mockito.mock HttpServletRequest.class

        when(request.getHeaderNames()).thenThrow SocketTimeoutException.class
        when(ncube.getName()).thenReturn 'foo-cube'
        when(ncube.getVersion()).thenReturn 'foo-version'

        def args = [ncube:ncube]
        def input = [(CdnRouter.HTTP_RESPONSE):response, (CdnRouter.HTTP_REQUEST):request]
        args.input = input
        cell.proxyFetch args
        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND, 'File not found: http://www.cedarsoftware.com')
    }

    @Test
    public void testProxyFetchSocketTimeoutWithResponseSendErrorIssue() throws Exception
    {
        UrlCommandCell cell = new StringUrlCmd('http://www.cedarsoftware.com', false)

        NCube ncube = Mockito.mock NCube.class
        HttpServletResponse response = Mockito.mock HttpServletResponse.class
        HttpServletRequest request = Mockito.mock HttpServletRequest.class

        when(request.getHeaderNames()).thenThrow SocketTimeoutException.class
        doThrow(IOException.class).when(response).sendError HttpServletResponse.SC_NOT_FOUND, 'File not found: http://www.cedarsoftware.com'
        when(ncube.getName()).thenReturn 'foo-cube'
        when(ncube.getVersion()).thenReturn 'foo-version'

        def args = [ncube:ncube]
        def input = [(CdnRouter.HTTP_REQUEST):request, (CdnRouter.HTTP_RESPONSE):response]
        args.put('input', input)
        cell.proxyFetch args
    }

    @Test
    public void testAddFileHeaderWithNullUrl() throws Exception
    {
        // Causes short-circuit return to get executed, and therefore does not get NPE on null HttpServletResponse
        // being passed in.  Verify the method was never called
        HttpServletResponse response = Mockito.mock HttpServletResponse.class
        UrlCommandCell.addFileHeader null, null
        verify(response, never()).addHeader anyString(), anyString()
    }

    @Test
    public void testAddFileHeaderWithExtensionNotFound() throws Exception
    {
        // Causes short-circuit return to get executed, and therefore does not get NPE on null HttpServletResponse
        // being passed in.
        HttpServletResponse response = Mockito.mock HttpServletResponse.class
        UrlCommandCell.addFileHeader new URL('http://www.google.com/index.foo'), response
        verify(response, never()).addHeader anyString(), anyString()
    }

    @Test
    public void testAddFileWithNoExtensionAndDotDomainAhead() throws Exception
    {
        // Causes short-circuit return to get executed, and therefore does not get NPE on null HttpServletResponse
        // being passed in.
        HttpServletResponse response = Mockito.mock HttpServletResponse.class
        UrlCommandCell.addFileHeader new URL('http://www.google.com/index'), response
        verify(response, never()).addHeader anyString(), anyString()
    }
}
