package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.ncube.UrlCommandCell;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 8/27/2014.
 */
public class NCubeTestWriter extends AbstractJsonFormat
{

    public String write(List<NCubeTestDto> list) throws IOException
    {
        builder.setLength(0);

        if (list == null) {
            return null;
        }

        startArray();
        for (NCubeTestDto dto : list) {
            writeDto(dto);
            comma();
        }
        uncomma();
        endArray();

        return builder.toString();
    }

    public void writeDto(NCubeTestDto dto) throws IOException {
        startObject();
        writeAttribute("name", dto.name, true);
        writeCoords(dto.coords);
        writeExpectedResult(dto.expectedResult);
        endObject();
    }


    private void writeExpectedResult(Object o) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "expectedResult"));
        builder.append(':');

        writeCoordinate(o, "expectedResult");
    }

    private void writeCoordinate(Object o, String value) throws IOException
    {
        startObject();
        writeType(getCellType(o, value));

        if (o instanceof UrlCommandCell) {
            writeCommandCell((UrlCommandCell)o);
        } else {
            writeValue("value", o);
        }
        endObject();
    }

    public void writeCoords(Map<String, Object> coords) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "coords"));
        builder.append(':');

        startObject();

        if (coords != null)
        {
            for (Map.Entry<String, Object> entry : coords.entrySet())
            {
                writeCoordinate(entry.getValue(), "coordinate");
                comma();
            }
        }
        uncomma();
        endObject();
    }


}
