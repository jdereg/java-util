package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

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

        //String cube = "{\"@type\":\"com.cedarsoftware.ncube.NCube\",\"name\":\"businessUnitDomainMapping\",\"axisList\":{\"@id\":73,\"@type\":\"java.util.LinkedHashMap\",\"@keys\":[\"domain\"],\"@items\":[{\"@type\":\"com.cedarsoftware.ncube.Axis\",\"id\":138991366092491074,\"name\":\"domain\",\"type\":{\"name\":\"SET\",\"ordinal\":2},\"valueType\":{\"name\":\"STRING\",\"ordinal\":0},\"columns\":{\"@type\":\"java.util.ArrayList\",\"@items\":[{\"@id\":63,\"@type\":\"com.cedarsoftware.ncube.Column\",\"id\":138991366092591174,\"displayOrder\":1,\"value\":{\"@type\":\"com.cedarsoftware.ncube.RangeSet\",\"items\":{\"@type\":\"java.util.ArrayList\",\"@items\":[\"udtruckingsb.td.afg\",\"udtruckingdev.td.afg\",\"udtruckingint.td.afg\",\"udtruckingcert.td.afg\",\"udtruckinguat.td.afg\",\"udtruckingprod.gaic.com\"]}}},{\"@id\":83,\"@type\":\"com.cedarsoftware.ncube.Column\",\"id\":138991366092490974,\"displayOrder\":2147483647,\"value\":null}]},\"defaultCol\":{\"@ref\":83},\"preferredOrder\":0,\"multiMatch\":false}]},\"cells\":{\"@type\":\"java.util.HashMap\",\"@keys\":[{\"@type\":\"java.util.HashSet\",\"@items\":[{\"@ref\":63}]}],\"@items\":[\"TRUCKING\"]},\"defaultCellValue\":{\"@type\":\"string\",\"value\":\"RABU\"},\"ruleMode\":false}";
        //NCube ncube = (NCube)JsonReader.jsonToJava(cube);
        //System.out.println(ncube.toFormattedJson());


        runAllTests();

        /*
        try
        {
            NCube init = NCubeManager.getNCubeFromResource("cpr.json");
            String s = init.toFormattedJson();
            System.out.println(s);
            NCube res = NCube.fromSimpleJson(s);
            Assert.assertTrue(res.equals(init));
        } catch (Exception e) {
            System.out.println("Exception:  " + e);
            e.printStackTrace();
        }
        */

    }

    public void runAllTests()
    {
        String[] strings = new String[] {
                //"2DSimpleJson.json",
                //"approvalLimits.json",
                "big5D.json",
                //"cpr.json",
                //"duplicateExpression.json",
                //"expressionAxis.json",
                //"expressionAxis2.json",
                //"idBasedCube.json",
                //"idBasedCubeSet.json",
                //"idNoValue.json",
                //"multiRule.json",
                //"multiRule2.json",
                //"multiRuleHalt.json",
                //"simpleJsonArrayTest.json",
                //"simpleJsonExpression.json",
                //"stringIds.json",
                //"template1.json",
                //"template2.json",
                //"testAtCommand.json",
                //"testCube1.json",
                //"testCube2.json",
                //"testCube3.json",
                //"testCube4.json",
                //"testCube5.json",
                //"testCube6.json",
                //"testCubeList.json",
                //"testGroovyMethods.json",
                //"updateColumns.json",
                //"urlContent.json",
                //"urlPieces.json",
                //"urlWithNcubeRefs.json"
        };
//        File dir = new File("C:\\Development\\Java\\Idea\\n-cube\\src\\test\\resources");
//        File[] files = dir.listFiles(new FilenameFilter() {
//            public boolean accept(File f, String s)
//            {
//                return s != null && s.endsWith(".json");
//            }
//        });

        for (String f : strings)
        {
            //System.out.print("\"" + f.getName() + "\",\n");


            try
            {
                NCube ncube = NCubeManager.getNCubeFromResource(f);
                String s = ncube.toFormattedJson();
                System.out.println(s);
                NCube res = NCube.fromSimpleJson(s);
                Assert.assertEquals(res, ncube);
            } catch (Exception e) {
                System.out.println(f);
                //System.out.println("Exception:  " + e);
            }

        }
    }
}