package com.cedarsoftware.ncube.util

import com.cedarsoftware.util.IOUtilities
import org.junit.Test

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
public class TestRoutingCapabilities
{
    @Test
    public void testNormalCase() throws Exception
    {
        String url = "http://www.cedarsoftware.com/tests/ncube/CS415P6.TXT"

        // ignore local caching
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
        conn.allowUserInteraction = false
        conn.requestMethod = "GET"
        //conn.setDoOutput(true)
        conn.doInput = true
        conn.useCaches = false
        conn.readTimeout = 220000
        conn.connectTimeout = 45000
        //conn.setIfModifiedSince(long)
        setupRequest conn
        conn.connect()

        InputStream input = new BufferedInputStream(conn.inputStream, 32768)

        //secondOut = response.getOutputStream()
        ByteArrayOutputStream bout = new ByteArrayOutputStream(8192)
        setupResponse conn
        IOUtilities.transfer input, bout
        IOUtilities.close input
        IOUtilities.close bout
        conn.disconnect()
    }

    public void setupRequest(HttpURLConnection c)
    {
        c.addRequestProperty "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        c.addRequestProperty "Accept-Encoding", "gzip,deflate"
        c.addRequestProperty "Accept-Language", "n-US,en;q=0.8"
        c.addRequestProperty "Host", "cedarsoftware.com"
        c.addRequestProperty "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36"
        c.addRequestProperty "If-None-Match", "162c-4f5ff5e69a7ec"
        c.addRequestProperty "If-Modified-Since", "1396377056000"
        c.addRequestProperty "Cache-Control", "max-age=60"
    }

    public void setupResponse(HttpURLConnection c)
    {
        //System.out.println("Content-Length:  " + c.getContentLength())
        //System.out.println("Last-Modified:  " + c.getLastModified())
        //System.out.println("Content-Encoding:  " + c.getContentEncoding())
        //System.out.println("Content-Type:  " + c.getContentType())
        //System.out.println("Cache-Control:  " + c.getHeaderField("Cache-Control"))
        //System.out.println("Etag:  " + c.getHeaderField("ETag"))
        //System.out.println("Expires:  " + c.getExpiration())
        //System.out.println("Accept-Ranges: " + c.getHeaderField("Accept-Ranges"))
        //System.out.println("Age: " + c.getHeaderField("Age"))
        //System.out.println("Date:  " + new Date(c.getHeaderFieldDate("Date", 0)))
        //System.out.println("Server:  " + c.getHeaderField("Server"))
    }
}
