package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.HashMap;
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
            Object result = parseExpectedResult((Map)map.get("expectedResult"));

            tests.put(name, new NCubeTestDto(name, coords, result));
        }

        return tests;
    }

    public Map<String,Object> resolveCoords(Map map) {
        Map<String, Object> coords = new LinkedHashMap<>();
        Iterator<Map.Entry<String,Object>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Object> item = i.next();
            Map typeValue = (Map)item.getValue();

            coords.put(item.getKey(), NCube.parseJsonValue((String) typeValue.get("type"), typeValue.get("value")));
        }
        return coords;
    }

    public Object parseExpectedResult(Map map) {
        Object o = NCube.parseJsonValue((String)map.get("type"), map.get("value"));
        if (o instanceof GroovyExpression) {
            GroovyExpression g = (GroovyExpression)o;
            g.execute(new HashMap());
            return NCube.parseJsonValue((String)map.get("type"), g.execute(new HashMap()));
        }

        return o;
    }
}
