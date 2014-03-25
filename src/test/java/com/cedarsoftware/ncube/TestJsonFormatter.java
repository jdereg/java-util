package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonWriter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by kpartlow on 3/18/14.
 */
public class TestJsonFormatter {

    @Test
    public void testFormatter() throws Exception {
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonArrayTest.json");

        assertNotNull(ncube.toFormattedJson());

        System.out.println(JsonWriter.objectToJson(ncube));

        System.out.println("\n------------------\n");
        System.out.println(ncube.toFormattedJson());

    }
}