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
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    protected StringBuilder builder = new StringBuilder();
    protected String quotedStringFormat = "\"%s\"";

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

    protected void writeAttributeIdentifier(String attr)
    {
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(":");
    }

    protected void writeAttribute(String attr, Object value, boolean includeComma) throws IOException
    {
        if (value instanceof String)
        {
            StringWriter w = new StringWriter();
            JsonWriter.writeJsonUtf8String((String) value, w);
            value = w.toString();
        }
        writeAttributeIdentifier(attr);
        builder.append(value == null ? "null" : value.toString());
        if (includeComma)
        {
            comma();
        }
    }
}
