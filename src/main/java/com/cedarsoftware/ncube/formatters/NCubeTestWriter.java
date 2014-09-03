package com.cedarsoftware.ncube.formatters;

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
        writeCoordinate(dto.getCoordDescription());
        comma();
        writeDescription("expectedResult", dto.getExpectedResultDescription());
        endObject();
    }


    private void writeDescription(String attr, Map<String, Object> map) throws IOException
    {
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(':');

        startObject();
        for (Map.Entry<String, Object> item : map.entrySet()) {
            writeValue(item.getKey(), item.getValue());
            comma();
        }
        uncomma();
        endObject();
    }

    public void writeCoordinate(Map<String, Map<String, Object>> coord) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "coord"));
        builder.append(':');

        startObject();

        if (coord != null)
        {
            for (Map.Entry<String, Map<String, Object>> entry : coord.entrySet())
            {
                writeDescription(entry.getKey(), entry.getValue());
                comma();
            }
        }
        uncomma();
        endObject();
    }


}
