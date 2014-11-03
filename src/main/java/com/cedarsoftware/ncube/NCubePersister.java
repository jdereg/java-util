package com.cedarsoftware.ncube;

import java.util.List;

public interface NCubePersister
{
    void saveNCube(NCube ncube);
    
    void updateNCube(ApplicationID appId, NCube ncube);
        
    NCube findNCube(ApplicationID appId, String ncubeName, boolean includeTests);
    
    List<NCube> findAllNCubes(ApplicationID appId);
    
    void deleteNCube(ApplicationID appId, String ncubeName);

    String getTestData(ApplicationID appId, String ncubeName);
    
    void setNCubeConnectionProvider(NCubeConnectionProvider nCubeConnectionProvider);
}