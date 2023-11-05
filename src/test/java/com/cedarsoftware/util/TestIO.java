package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class TestIO
{
    @Test
    public void testFastReader() throws Exception
    {
        String content = TestUtilTest.fetchResource("prettyPrint.json");
        ByteArrayInputStream bin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        FastReader reader = new FastReader(new InputStreamReader(bin, StandardCharsets.UTF_8), 1024,10);
        assert reader.read() == '{';
        int c;
        boolean done = false;
        while ((c = reader.read()) != -1 && !done)
        {
            if (c == '{')
            {
                assert reader.getLine() == 4;
                assert reader.getCol() == 11;
                reader.pushback('n');
                reader.pushback('h');
                reader.pushback('o');
                reader.pushback('j');
                StringBuilder sb = new StringBuilder();
                sb.append((char)reader.read());
                sb.append((char)reader.read());
                sb.append((char)reader.read());
                sb.append((char)reader.read());
                assert sb.toString().equals("john");

                Set<Character> chars = new HashSet<>();
                chars.add('}');
                readUntil(reader, chars);
                c = reader.read();
                assert c == ',';
                assert reader.getLastSnippet().length() > 25;
                char[] buf = new char[12];
                reader.read(buf);
                String s = new String(buf);
                assert s.contains("true");
                done = true;
            }
        }
        reader.close();
    }

    @Test
    public void testFastWriter() throws Exception
    {
        String content = TestUtilTest.fetchResource("prettyPrint.json");
        ByteArrayInputStream bin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        FastReader reader = new FastReader(new InputStreamReader(bin, StandardCharsets.UTF_8), 1024,10);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastWriter out = new FastWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        int c;
        boolean done = false;
        while ((c = reader.read()) != -1 && !done)
        {
            out.write(c);
        }
        reader.close();
        out.flush();
        out.close();

        assert content.equals(new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFastWriterCharBuffer() throws Exception
    {
        String content = TestUtilTest.fetchResource("prettyPrint.json");
        ByteArrayInputStream bin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        FastReader reader = new FastReader(new InputStreamReader(bin, StandardCharsets.UTF_8), 1024,10);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastWriter out = new FastWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        
        char buffer[] = new char[100];
        reader.read(buffer);
        out.write(buffer, 0, 100);
        reader.close();
        out.flush();
        out.close();

        for (int i=0; i < 100; i++)
        {
            assert content.charAt(i) == buffer[i];
        }
    }

    @Test
    public void testFastWriterString() throws Exception
    {
        String content = TestUtilTest.fetchResource("prettyPrint.json");
        ByteArrayInputStream bin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        FastReader reader = new FastReader(new InputStreamReader(bin, StandardCharsets.UTF_8), 1024,10);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastWriter out = new FastWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        char buffer[] = new char[100];
        reader.read(buffer);
        String s = new String(buffer);
        out.write(s, 0, 100);
        reader.close();
        out.flush();
        out.close();

        for (int i=0; i < 100; i++)
        {
            assert content.charAt(i) == s.charAt(i);
        }
    }

    private int readUntil(FastReader input, Set<Character> chars) throws IOException
    {
        FastReader in = input;
        int c;
        do
        {
            c = in.read();
        } while (!chars.contains((char)c) && c != -1);
        return c;
    }
}
