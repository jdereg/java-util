package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.util.SafeSimpleDateFormat;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by kpartlow on 9/25/2014.
 */
public class BaseJsonFormatter
{
    public static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    //protected final StringBuilder builder = new StringBuilder();
    protected final Writer builder;

    BaseJsonFormatter() {
        builder = new StringWriter(8192);
    }

    BaseJsonFormatter(OutputStream stream) {
        builder = new OutputStreamWriter(stream);
    }

    void startArray() {
        try {
            builder.append("[");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void endArray() {
        try {
            builder.append("]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void startObject() {
        try {
            builder.append("{");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void endObject() {
        try {
            builder.append("}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void comma() {
        try {
            builder.append(",");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void append(Long id) {
        try {
            builder.append(Long.toString(id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void append(String id) {
        try {
            builder.append(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    void uncomma()
//    {
//        builder.setLength(builder.length() - 1);
//    }

    protected void writeObjectKey(String key)
    {
        try {
            builder.append('"');
            builder.append(key);
            builder.append('"');
            builder.append(":");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected void writeObjectKeyValue(String key, Object value, boolean includeComma) throws IOException
    {
        writeObjectKey(key);
        writeObjectValue(value);
        if (includeComma)
        {
            comma();
        }
    }

    protected void writeObjectValue(Object value) throws IOException
    {
        if (value instanceof String)
        {
            StringWriter w = new StringWriter();
            JsonWriter.writeJsonUtf8String((String) value, w);
            value = w.toString();
        }
        builder.append(value == null ? "null" : value.toString());
    }
}
