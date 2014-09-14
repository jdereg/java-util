package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTestParser
{
    public List<NCubeTest> parse(String data) throws IOException
    {
        if (data == null) {
            return null;
        }

        Object[] items = (Object[])JsonReader.jsonToJava(data);

        List<NCubeTest> tests = new ArrayList<>(items.length);

        for(Object o : items)
        {
            Map<String, Object> map = (Map<String, Object>)o;

            String name = (String)map.get("name");
            Map<String, CellInfo> coords = new LinkedHashMap<>();

            Map<String, Map<String, Object>> c = (Map<String, Map<String, Object>>)map.get("coord");

            for (Map.Entry<String, Map<String, Object>> item : c.entrySet()) {

                coords.put(item.getKey(), buildCoord(item.getValue()));
            }

            Map<String, Map<String, Object>> r = (Map<String, Map<String, Object>>)map.get("result");

            Map<String, CellInfo> results = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> item : r.entrySet()) {

                results.put(item.getKey(), buildCoord(item.getValue()));
            }

            NCubeTest dto = new NCubeTest(name, coords, results);
            tests.add(dto);
        }

        return tests;
    }

    public CellInfo buildCoord(Map<String, Object> coord) {
        String type = (String)coord.get("type");
        String value = (String)coord.get("value");
        String isUrl = (String)coord.get("isUrl");
        String isCached = (String)coord.get("isCached");

        return new CellInfo(type, value, isUrl, isCached);
    }

}
