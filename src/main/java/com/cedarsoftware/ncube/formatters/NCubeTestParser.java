package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
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
            Map<String, Map<String, Object>> coord = (Map<String, Map<String, Object>>)map.get("coord");
            Map<String, Object> result = (Map<String, Object>) map.get("expectedResult");

            NCubeTest dto = new NCubeTest(name, coord, result);
            tests.add(dto);
        }

        return tests;
    }

    /**
     * Resolve coordinates for the test.
     */
    public Map<String,Object> resolveCoords(Map<String, Map<String, Object>> map) {
        Map<String, Object> coords = new LinkedHashMap<>();
        Iterator<Map.Entry<String, Map<String, Object>>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Map<String, Object>> item = i.next();
            coords.put(item.getKey(), parseValue(item.getValue()));
        }
        return coords;
    }

    public Object parseValue(Map<String, Object> map) {
        Object value = map.get("value");
        String url = (String)map.get("url");
        String type = (String)map.get("type");

        if (value == null && StringUtilities.isEmpty(url)) {
            throw new IllegalArgumentException("Test Items must have either a url or a type");
        }
        return CellInfo.parseJsonValue(value, url, type, false);
    }

    public void write(Map<String, NCubeTestDto> items) {

    }

}
