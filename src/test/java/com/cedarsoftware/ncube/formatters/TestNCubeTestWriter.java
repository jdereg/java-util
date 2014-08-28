package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import org.junit.Test;

/**
 * Created by kpartlow on 8/27/2014.
 */
public class TestNCubeTestWriter
{
    @Test
    public void test() {
        NCube ncube = NCubeManager.getNCubeFromResource("stringIds.json");
        //List<NCubeTestDto> list = TestNCubeTestParser.getTestsFromResource("");

        ncube.generateNCubeTests();
        //ncube.setTestData();
    }
}
