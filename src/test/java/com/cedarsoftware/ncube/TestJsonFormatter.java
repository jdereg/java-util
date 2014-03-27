package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Created by kpartlow on 3/18/14.
 */
public class TestJsonFormatter {

    @Test
    public void testFormatter() throws Exception {
        //NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonArrayTest.json");

        //assertNotNull(ncube.toFormattedJson());

        //System.out.println(JsonWriter.objectToJson(ncube));

        //System.out.println("\n------------------\n");
        //System.out.println(ncube.toFormattedJson());

        String cube = "{\"@type\":\"com.cedarsoftware.ncube.NCube\",\"name\":\"businessUnitDomainMapping\",\"axisList\":{\"@id\":73,\"@type\":\"java.util.LinkedHashMap\",\"@keys\":[\"domain\"],\"@items\":[{\"@type\":\"com.cedarsoftware.ncube.Axis\",\"id\":138991366092491074,\"name\":\"domain\",\"type\":{\"name\":\"SET\",\"ordinal\":2},\"valueType\":{\"name\":\"STRING\",\"ordinal\":0},\"columns\":{\"@type\":\"java.util.ArrayList\",\"@items\":[{\"@id\":63,\"@type\":\"com.cedarsoftware.ncube.Column\",\"id\":138991366092591174,\"displayOrder\":1,\"value\":{\"@type\":\"com.cedarsoftware.ncube.RangeSet\",\"items\":{\"@type\":\"java.util.ArrayList\",\"@items\":[\"udtruckingsb.td.afg\",\"udtruckingdev.td.afg\",\"udtruckingint.td.afg\",\"udtruckingcert.td.afg\",\"udtruckinguat.td.afg\",\"udtruckingprod.gaic.com\"]}}},{\"@id\":83,\"@type\":\"com.cedarsoftware.ncube.Column\",\"id\":138991366092490974,\"displayOrder\":2147483647,\"value\":null}]},\"defaultCol\":{\"@ref\":83},\"preferredOrder\":0,\"multiMatch\":false}]},\"cells\":{\"@type\":\"java.util.HashMap\",\"@keys\":[{\"@type\":\"java.util.HashSet\",\"@items\":[{\"@ref\":63}]}],\"@items\":[\"TRUCKING\"]},\"defaultCellValue\":{\"@type\":\"string\",\"value\":\"RABU\"},\"ruleMode\":false}";
        NCube ncube = (NCube)JsonReader.jsonToJava(cube);
        System.out.println(ncube.toFormattedJson());
    }
}