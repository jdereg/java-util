package com.cedarsoftware.ncube;

import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTestDto
{
    private String name;
    private Map<String, Object> coords;
    private Object expectedResult;

    public NCubeTestDto(String name, Map<String, Object> coords, Object expectedResult)
    {
        this.name = name;
        this.coords = coords;
        this.expectedResult = expectedResult;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, Object> getCoords()
    {
        return coords;
    }

    public Object getExpectedResult()
    {
        return expectedResult;
    }
}
