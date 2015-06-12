package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.StringValuePair;

import java.io.IOException;

/**
 * Created by kpartlow on 9/24/2014.
 */
public class NCubeTestWriter extends BaseJsonFormatter
{
    public String format(Object[] tests) throws IOException {
        startArray();
        if (tests != null && tests.length > 0)
        {
            boolean firstPass = true;
            for (Object test : tests)
            {
                if (!firstPass) {
                    comma();
                }
                writeTest((NCubeTest) test);
                firstPass = false;
            }
        }
        endArray();

        return builder.toString();
    }

    private void writeTest(NCubeTest test) throws IOException {
        startObject();
        writeObjectKeyValue("name", test.getName(), true);
        writeObjectKey("coord");
        writeCoord(test.getCoord());
        comma();
        writeObjectKey("assertions");
        writeAssertions(test.getAssertions());
        endObject();
    }

    private void writeCoord(StringValuePair<CellInfo>[] coord) throws IOException {
        startArray();
        if (coord != null && coord.length > 0)
        {
            boolean firstPass = true;
            for (StringValuePair<CellInfo> parameter : coord)
            {
                if (!firstPass) {
                    comma();
                }
                startObject();
                writeObjectKey(parameter.getKey());
                writeCellInfo(parameter.getValue());
                endObject();
                firstPass = false;
            }
        }
        endArray();
    }

    private void writeAssertions(CellInfo[] assertions) throws IOException {
        startArray();
        if (assertions != null && assertions.length > 0)
        {
            boolean firstPass = true;
            for (CellInfo item : assertions)
            {
                if (!firstPass) {
                    comma();
                }
                writeCellInfo(item);
                firstPass = false;
            }
        }
        endArray();
    }

    public void writeCellInfo(CellInfo info) throws IOException
    {
        startObject();
        if (info != null)
        {
            writeObjectKeyValue("type", info.dataType, true);
            writeObjectKeyValue("isUrl", info.isUrl, true);
            writeObjectKeyValue("value", info.value, false);
        }
        endObject();
    }
}
