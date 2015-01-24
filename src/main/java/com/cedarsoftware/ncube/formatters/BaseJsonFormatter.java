package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.util.SafeSimpleDateFormat;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by kpartlow on 9/25/2014.
 */
public class BaseJsonFormatter
{
    public static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    protected final StringBuilder builder = new StringBuilder();

    void startArray() {
        builder.append("[");
    }

    void endArray() {
        builder.append("]");
    }

    void startObject() {
        builder.append("{");
    }

    void endObject() {
        builder.append("}");
    }

    void comma() {
        builder.append(",");
    }

    void uncomma()
    {
        builder.setLength(builder.length() - 1);
    }

    protected void writeObjectKey(String key)
    {
        builder.append('"');
        builder.append(key);
        builder.append('"');
        builder.append(":");
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
