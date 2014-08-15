package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTestParser
{
    public Map<String, NCubeTestDto> parse(String data) throws IOException
    {
        if (data == null) {
            return null;
        }

        Map maps = JsonReader.jsonToMaps(data);

        Map<String, NCubeTestDto> tests = new LinkedHashMap<>();

        Object[] items = (Object[])maps.get("@items");

        for(Object o : items)
        {
            Map map = (Map)o;
            String name = (String)map.get("name");
            Map<String,Object> coords = resolveCoords((Map)map.get("coords"));
            Object result = parseValue((Map) map.get("expectedResult"));

            tests.put(name, new NCubeTestDto(name, coords, result));
        }

        return tests;
    }

    public Map<String,Object> resolveCoords(Map map) {
        Map<String, Object> coords = new LinkedHashMap<>();
        Iterator<Map.Entry<String,Object>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Object> item = i.next();
            coords.put(item.getKey(), parseValue((Map)item.getValue()));
        }
        return coords;
    }

    public Object parseValue(Map map) {
        return NCube.parseJsonValue((String)map.get("type"), map.get("value"));
    }

}
