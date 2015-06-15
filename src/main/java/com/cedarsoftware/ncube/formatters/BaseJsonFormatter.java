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
        append("[");
    }

    void endArray() {
        append("]");
    }

    void startObject() {
        append("{");
    }

    void endObject() {
        append("}");
    }

    void comma() {
        append(",");
    }

    void append(Long id) {
        append(Long.toString(id));
    }

    void append(String id) {
        try {
            builder.append(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeObjectKey(String key)
    {
        append("\"" + key + "\":");
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
        append(value == null ? "null" : value.toString());
    }
}
