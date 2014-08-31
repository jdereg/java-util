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

}
