/**
 * Created by kpartlow on 2/26/2015.
 */
class Test {
    public void test()
    {
        Map map = null;
        if (com.cedarsoftware.util.StringUtilities.hasContent(com.cedarsoftware.util.SystemUtilities.getExternalVariable('NCUBE_BOOTSTRAP')))
        {
            map = com.cedarsoftware.util.io.JsonReader.jsonToMaps(com.cedarsoftware.util.SystemUtilities.getExternalVariable('NCUBE_BOOTSTRAP'));
        }
        String tenant = map?.containsKey('tenant') ? map.tenant : input.tenant;
        String app = map?.containsKey('app') ? map.app : input.app;
        String version = map?.containsKey('version') ? map.version : input.version;
        String status = map?.containsKey('status') ? map.status : input.status;
        String branch = map?.containsKey('branch') ? map.branch : input.branch;
        new com.cedarsoftware.ncube.ApplicationID(tenant, app, version, status, branch);
    }
}
