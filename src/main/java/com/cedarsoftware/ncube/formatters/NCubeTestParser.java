package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.util.CaseInsensitiveMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by kpartlow on 8/11/2014.
 */
public class NCubeTestParser extends AbstractJsonFormat
{
    public String write(CaseInsensitiveMap<String, NCubeTest> test) {
        try
        {
            StringBuilder builder = new StringBuilder();
            Iterator<Map.Entry<String, NCubeTest>> set = test.entrySet().iterator();

            while (set.hasNext()) {
                Map.Entry<String, NCubeTest> item = set.next();
                startObject();
                endObject();
            }

            return builder.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(String.format("Unable to format NCube tests", e));
        }
    }
}
