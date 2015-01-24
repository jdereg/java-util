package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.StringValuePair;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 9/25/2014.
 */
public class NCubeTestReader
{
    public static List<NCubeTest> convert(String s) throws IOException
    {
        List<NCubeTest> list = new ArrayList<>();
        if (StringUtilities.isEmpty(s)) {
            return list;
        }

        Map map = JsonReader.jsonToMaps(s);

        Object[] items = (Object[])map.get("@items");

        for (Object o : items) {
            JsonObject item = (JsonObject)o;

            String name = (String)item.get("name");
            List<StringValuePair<CellInfo>> coord = createCoord((JsonObject) item.get("coord"));
            List<CellInfo> assertions = createAssertions((JsonObject) item.get("assertions"));

            NCubeTest test = new NCubeTest(name, coord.toArray(new StringValuePair[coord.size()]), assertions.toArray(new CellInfo[assertions.size()]));
            list.add(test);
        }
        return list;
    }

    public static List<StringValuePair<CellInfo>> createCoord(JsonObject<String, Object> o)
    {
        List<StringValuePair<CellInfo>> list = new ArrayList<>();

        for (Object item : (o.getArray()))
        {
            JsonObject<String, Object> jo = (JsonObject<String, Object>)item;
            for (Map.Entry<String, Object> entry : jo.entrySet())
            {
                list.add(new StringValuePair(entry.getKey(), createCellInfo((JsonObject)entry.getValue())));
            }
        }

        return list;
    }

    public static CellInfo createCellInfo(JsonObject o) {
        String type = (String)o.get("type");
        String value = (String)o.get("value");

        return new CellInfo(type, value, o.get("isUrl"), false);
    }

    public static List<CellInfo> createAssertions(JsonObject o) {
        List<CellInfo> list = new ArrayList<>();
        for (Object item : o.getArray()) {
            list.add(createCellInfo((JsonObject)item));
        }
        return list;
    }
}
