package com.cedarsoftware.ncube;

import java.util.List;

public interface NCubePersister
{
    void createCube(ApplicationID id, NCube cube);
    
    void updateCube(ApplicationID appId, NCube cube);

    //It doesn't look like findCube is used
    //NCube findCube(ApplicationID appId, String name);
    
    List<NCube> loadCubes(ApplicationID appId);
    Object[] getNCubes(ApplicationID appId, String sqlLike);
    
    boolean deleteCube(ApplicationID appId, String name, boolean allowDelete);

    String[] getAppNames();
    String[] getAppVersions(ApplicationID id);

    boolean updateNotes(ApplicationID id, String cubeName, String notes);
    String getNotes(ApplicationID id, String cubeName);

    int createSnapshotVersion(ApplicationID id, String newVersion);
    int changeVersionValue(ApplicationID id, String newVersion);
    int releaseCubes(ApplicationID id);

    boolean renameCube(ApplicationID id, NCube oldCube, String newName);

    boolean updateTestData(ApplicationID id, String cubeName, String testData);
    String getTestData(ApplicationID id, String cubeName);
}
