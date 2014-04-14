package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.HashMap;


/**
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
public class TestUrlUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class c = UrlUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<UrlUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }


    @Test
    public void testGetContentFromUrlAsString() throws Exception
    {
        String content1 = UrlUtilities.getContentFromUrlAsString("https://www.google.com", Proxy.NO_PROXY);
        String content2 = UrlUtilities.getContentFromUrlAsString("https://www.google.com");

        Assert.assertTrue(content1.contains("google.com"));
        Assert.assertTrue(content2.contains("google.com"));

        //  Allow for 3% difference between pages between requests to handle time and hash value changes.
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content1, content2), .03);

        String content3 = UrlUtilities.getContentFromUrlAsString("http://www.google.com", Proxy.NO_PROXY);
        String content4 = UrlUtilities.getContentFromUrlAsString("http://www.google.com");

        Assert.assertTrue(content3.contains("google.com"));
        Assert.assertTrue(content4.contains("google.com"));

        //  Allow for 3% difference between pages between requests to handle time and hash value changes.
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content3, content4), .03);
    }

    @Test
    public void testGetContentFromUrl()
    {
        SSLSocketFactory f = UrlUtilities.buildNaiveSSLSocketFactory();
        HostnameVerifier v = UrlUtilities.NAIVE_VERIFIER;

        byte[] content1 = UrlUtilities.getContentFromUrl("https://www.google.com", Proxy.NO_PROXY);
        byte[] content2 = UrlUtilities.getContentFromUrl("https://www.google.com");
        byte[] content3 = UrlUtilities.getContentFromUrl("https://www.google.com", Proxy.NO_PROXY, f, v);
        byte[] content4 = UrlUtilities.getContentFromUrl("https://www.google.com", null, 0, null, null, true);
        byte[] content5 = UrlUtilities.getContentFromUrl("https://www.google.com", null, null, Proxy.NO_PROXY, f, v);

        //  Allow for 3% difference between pages between requests to handle time and hash value changes.
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content1, content2), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content2, content3), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content3, content4), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content4, content5), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content5, content1), .03);

        Assert.assertNull(UrlUtilities.getContentFromUrl("https://www.google.com", Proxy.NO_PROXY, null, null));
        Assert.assertNull(UrlUtilities.getContentFromUrl("https://www.google.com", null, null, Proxy.NO_PROXY, null, null));
        Assert.assertNull(UrlUtilities.getContentFromUrl("https://www.google.com", null, 0, null, null, false));

        byte[] content6 = UrlUtilities.getContentFromUrl("http://www.google.com", Proxy.NO_PROXY, null, null);
        byte[] content7 = UrlUtilities.getContentFromUrl("http://www.google.com", null, 0, null, null, false);
        byte[] content8 = UrlUtilities.getContentFromUrl("http://www.google.com", null, null, Proxy.NO_PROXY, null, null);

        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content6, content7), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content7, content8), .03);
        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content8, content1), .03);

        Assert.assertNull(UrlUtilities.getContentFromUrl("http://www.google.com/google-sucks.html", null, null, Proxy.NO_PROXY, null, null));
    }

    @Test
    public void testSSLTrust() throws Exception
    {
        String content1 = UrlUtilities.getContentFromUrlAsString("https://www.google.com", Proxy.NO_PROXY);
        String content2 = UrlUtilities.getContentFromUrlAsString("https://www.google.com", null, 0, null, null, true);

        Assert.assertTrue(content1.contains("google.com"));
        Assert.assertTrue(content2.contains("google.com"));

        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(content1, content2), .03);

    }

    @Test
    public void testCookies() throws Exception
    {
        HashMap cookies = new HashMap();

        byte[] bytes1 = UrlUtilities.getContentFromUrl("http://www.google.com", null, 0, cookies, cookies, false);

        Assert.assertEquals(1, cookies.size());
        Assert.assertTrue(cookies.containsKey("google.com"));

        byte[] bytes2 = UrlUtilities.getContentFromUrl("http://www.google.com", null, 0, cookies, cookies, false);

        Assert.assertEquals(1, cookies.size());
        Assert.assertTrue(cookies.containsKey("google.com"));

        Assert.assertEquals(0.0, ComputeLevenshteinDistancePercentage(bytes1, bytes2), .03);
    }

    public double ComputeLevenshteinDistancePercentage(String s,String t) {
        int[][] distance = new int[s.length() + 1][t.length() + 1];

        for (int i = 0; i <= s.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= t.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= s.length(); i++)
            for (int j = 1; j <= t.length(); j++)
                distance[i][j] = Math.min(Math.min(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1),
                        distance[i - 1][j - 1]+ ((s.charAt(i - 1) == t.charAt(j - 1)) ? 0 : 1));

        return (double)distance[s.length()][t.length()]/(double)Math.max(s.length(), t.length());
    }

    public double ComputeLevenshteinDistancePercentage(byte[] s, byte[] t) {
        int[][] distance = new int[s.length + 1][t.length + 1];

        for (int i = 0; i <= s.length; i++)
            distance[i][0] = i;
        for (int j = 1; j <= t.length; j++)
            distance[0][j] = j;

        for (int i = 1; i <= s.length; i++)
            for (int j = 1; j <= t.length; j++)
                distance[i][j] = Math.min(Math.min(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + ((s[i - 1] == t[j - 1]) ? 0 : 1));

        return (double)distance[s.length][t.length]/(double)Math.max(s.length, t.length);
    }

}
