package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 8/27/2014.
 */
public class NCubeTestWriter extends JsonFormatter
{

    public String write(List<NCubeTest> list) throws IOException
    {
        builder.setLength(0);

        if (list == null) {
            return null;
        }

        startArray();
        for (NCubeTest dto : list) {
            writeDto(dto);
            comma();
        }
        uncomma();
        endArray();

        return builder.toString();
    }

    public void writeDto(NCubeTest dto) throws IOException {
        startObject();
        writeAttribute("name", dto.getName(), true);
        writeDescriptions("coord", dto.getCoord());
        comma();
        writeDescriptions("expected", dto.getExpected());
        endObject();
    }


    private void writeDescription(String attr, CellInfo info) throws IOException
    {
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(':');

        startObject();
        writeValue("type", info.dataType);
        comma();
        writeValue("value", info.value);

        if (info.isUrl)
        {
            comma();
            writeValue("isUrl", Boolean.valueOf(info.isUrl));
        }

        if (info.isCached)
        {
            comma();
            writeValue("isCached", Boolean.valueOf(info.isCached));
        }

        endObject();
    }

    public void writeDescriptions(String name, Map<String, CellInfo> descriptions) throws IOException
    {
        builder.append(String.format(quotedStringFormat, name));
        builder.append(':');

        startObject();

        if (descriptions != null)
        {
            for (Map.Entry<String, CellInfo> entry : descriptions.entrySet())
            {
                writeDescription(entry.getKey(), entry.getValue());
                comma();
            }
        }
        uncomma();
        endObject();
    }


}
