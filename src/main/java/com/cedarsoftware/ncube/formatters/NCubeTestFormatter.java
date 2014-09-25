package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 9/24/2014.
 */
public class NCubeTestFormatter extends BaseJsonFormatter
{
    public String format(Object[] tests) throws IOException {
        startArray();
        for (Object test : tests) {
            writeTest((NCubeTest)test);
            comma();
        }
        uncomma();
        endArray();

        return builder.toString();
    }

    private void writeTest(NCubeTest test) throws IOException {
        startObject();
        writeAttribute("name", test.getName(), true);
        writeAttributeIdentifier("coord");
        writeCoord(test.getCoord());
        comma();
        writeAttributeIdentifier("assertions");
        writeAssertions(test.getAssertions());
        endObject();
    }

    private void writeCoord(Map<String, CellInfo> coord) throws IOException {
        startObject();
        for (Map.Entry<String, CellInfo> parameter : coord.entrySet()) {
            writeAttributeIdentifier(parameter.getKey());
            writeCellInfo(parameter.getValue());
            comma();
        }
        uncomma();
        endObject();
    }

    private void writeAssertions(List<CellInfo> assertions) throws IOException {
        startArray();
        for (CellInfo item : assertions) {
            writeCellInfo(item);
            comma();
        }
        uncomma();
        endArray();
    }

    public void writeCellInfo(CellInfo info) throws IOException
    {
        startObject();
        writeAttribute("type", info.dataType, true);
        writeAttribute("isUrl", info.isUrl, true);
        writeAttribute("isCached", info.isCached, true);
        writeAttribute("value", info.value, false);
        endObject();
    }
}
