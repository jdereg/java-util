package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UniqueIdGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Class used to carry the NCube meta-information
 * to the client.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class NCubeJdbcPersister
{
    private static final Logger LOG = LogManager.getLogger(NCubeJdbcPersister.class);
    public static final String TEST_DATA_BIN = "test_data_bin";
    public static final String NOTES_BIN = "notes_bin";

    public void createCube(Connection c, ApplicationID appId, NCube cube, String username)
    {
        if (doesCubeExist(c, appId, cube.getName())) {
            throw new IllegalStateException("Cannot create cube: " + cube.getName() + ".  It already exists in app: " + appId);
        }

        createCube(c, appId, cube, username, 0);
    }

    void createCube(Connection c, ApplicationID appId, NCube ncube, String username, long rev)
    {
        try
        {
            try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, test_data_bin, notes_bin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
            {
                insert.setLong(1, UniqueIdGenerator.getUniqueId());
                insert.setString(2, appId.getApp());
                insert.setString(3, ncube.getName());
                insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
                insert.setString(5, appId.getVersion());
                java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                insert.setDate(6, now);
                insert.setString(7, username);
                insert.setString(8, appId.getTenant());
                insert.setString(9, appId.getBranch());
                insert.setLong(10, rev);
                insert.setBytes(11, null);
                insert.setBytes(12, StringUtilities.getBytes("Cube created", "UTF-8"));

                int rowCount = insert.executeUpdate();
                if (rowCount != 1)
                {
                    throw new IllegalStateException("error inserting new n-cube: " + ncube.getName() + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to insert cube: " + ncube.getName() + ", app: " + appId + " into database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    void replaceHeadSha1(Connection c, ApplicationID appId, String cubeName, String sha1, Long revision)
    {
        String sql = "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setLong(7, revision);


            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");

                    if (!ArrayUtilities.isEmpty(jsonBytes))
                    {
                        jsonBytes = resetHeadSha1AndChangeType(jsonBytes, sha1);
                    }

                    try (PreparedStatement insert = c.prepareStatement("UPDATE n_cube set cube_value_bin = ? WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?"))
                    {
                        insert.setBytes(1, jsonBytes);
                        insert.setString(2, cubeName);
                        insert.setString(3, appId.getApp());
                        insert.setString(4, appId.getVersion());
                        insert.setString(5, appId.getStatus());
                        insert.setString(6, appId.getTenant());
                        insert.setString(7, appId.getBranch());
                        insert.setLong(8, revision);

                        if (insert.executeUpdate() != 1)
                        {
                            throw new IllegalStateException("error updating n-cube: " + cubeName + "', app: " + appId + ", row was not updated");
                        }
                    }
                }

            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to replace headSha1: " + cubeName + " from app: " + appId;
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    boolean copyBranchCubeToHead(Connection c, ApplicationID srcBranch, ApplicationID tgtBranch, String cubeName, String username, Long revision)
    {
        if (revision == null)
        {
            throw new IllegalArgumentException("Revision number cannot be null to copyBranchCubeToHead");
        }

        String sql = "SELECT n_cube_nm, app_cd, version_no_cd, status_cd, revision_number, branch_id, cube_value_bin, test_data_bin, notes_bin from n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, srcBranch.getApp());
            stmt.setString(3, srcBranch.getVersion());
            stmt.setString(4, srcBranch.getStatus());
            stmt.setString(5, srcBranch.getTenant());
            stmt.setString(6, srcBranch.getBranch());
            stmt.setLong(7, revision);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");

                    if (tgtBranch.isHead())
                    {
                        jsonBytes = removeHeadSha1FromBytes(jsonBytes);
                    }

                    Long maxRevision = getMaxRevision(c, tgtBranch, cubeName);

                    //  create case because maxrevision was not found.
                    if (maxRevision == null)
                    {
                        maxRevision = revision < 0 ? new Long(-1) : new Long(0);
                    }
                    else if (revision < 0)
                    {
                        // cube deleted in branch
                        maxRevision = -(Math.abs(maxRevision)+1);
                    }
                    else
                    {
                        maxRevision = Math.abs(maxRevision)+1;
                    }

                    try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, test_data_bin, notes_bin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        insert.setLong(1, UniqueIdGenerator.getUniqueId());
                        insert.setString(2, tgtBranch.getApp());
                        insert.setString(3, cubeName);
                        insert.setBytes(4, jsonBytes);
                        insert.setString(5, tgtBranch.getVersion());
                        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                        insert.setDate(6, now);
                        insert.setString(7, username);
                        insert.setString(8, tgtBranch.getTenant());
                        insert.setString(9, tgtBranch.getBranch());
                        insert.setLong(10, maxRevision);

                        insert.setBytes(11, rs.getBytes(TEST_DATA_BIN));
                        insert.setBytes(12, rs.getBytes(NOTES_BIN));

                        int rowCount = insert.executeUpdate();
                        if (rowCount != 1)
                        {
                            String s = "Unable to copy cube: " + cubeName + " from app: " + srcBranch + " into app:  " + tgtBranch;
                            throw new IllegalStateException(s);
                        }
                        return true;
                    }
                }
                return false;
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to copy cube: " + cubeName + " from app: " + srcBranch + " into app:  " + tgtBranch;
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    private byte[] removeHeadSha1FromBytes(byte[] jsonBytes)
    {
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.headSha1Pattern.matcher(json);
        StringBuffer sb = new StringBuffer();
        if (m.find())
        {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        jsonBytes = StringUtilities.getBytes(sb.toString(), "UTF-8");
        return jsonBytes;
    }


    public void updateCube(Connection connection, ApplicationID appId, NCube cube, String username)
    {
        String sql = "SELECT n.n_cube_nm, app_cd, version_no_cd, status_cd, n.revision_number as rev, n.branch_id, n.cube_value_bin, n.test_data_bin, n.notes_bin FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                "  GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.n_cube_nm = ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, cube.name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setString(7, cube.name);
            stmt.setString(8, appId.getApp());
            stmt.setString(9, appId.getVersion());
            stmt.setString(10, appId.getStatus());
            stmt.setString(11, appId.getTenant());
            stmt.setString(12, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Long revision = rs.getLong("rev");

                    if (revision < 0)
                    {
                        throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update deleted cube.  Restore it first.");
                    }

                    //TODO:  This code may be necessary for supporting Ken Sayer's loading from files on disk.
//                    String headSha1 = getHeadSha1(rs.getBytes("cube_value_bin"));
//                    cube.setHeadSha1(headSha1);

                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, test_data_bin, notes_bin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        insert.setLong(1, UniqueIdGenerator.getUniqueId());
                        insert.setString(2, appId.getApp());
                        insert.setString(3, cube.name);
                        insert.setBytes(4, cube.toFormattedJson().getBytes("UTF-8"));
                        insert.setString(5, appId.getVersion());
                        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                        insert.setDate(6, now);
                        insert.setString(7, username);
                        insert.setString(8, appId.getTenant());
                        insert.setString(9, appId.getBranch());
                        insert.setLong(10, revision + 1);

                        insert.setBytes(11, rs.getBytes(TEST_DATA_BIN));
                        insert.setBytes(12, rs.getBytes(NOTES_BIN));

                        if (insert.executeUpdate() != 1)
                        {
                            throw new IllegalStateException("error updating n-cube: " + cube.name + "', app: " + appId + ", row was not updated");
                        }
                        return;
                    }
                }

                throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update non-existing cube.");
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to insert cube: " + cube.name + ", app: " + appId + " into database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    Long getMaxRevision(Connection c, ApplicationID appId, String name)
    {
        //TODO:  status_cd
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT revision_number FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? " +
                "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
        catch (Exception e)
        {
            String s = "Unable to get maximum revision number for cube: " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }


    public Object[] getBranchChanges(Connection c, ApplicationID appId)
    {
        String sql = "SELECT n_cube_id, n.n_cube_nm, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, n.revision_number, n.branch_id, n.cube_value_bin FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                " GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) " +
                "AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql))
        {

            stmt.setString(1, appId.getApp());
            stmt.setString(2, appId.getVersion());
            stmt.setString(3, appId.getStatus());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());
            stmt.setString(6, appId.getApp());
            stmt.setString(7, appId.getVersion());
            stmt.setString(8, appId.getStatus());
            stmt.setString(9, appId.getTenant());
            stmt.setString(10, appId.getBranch());
            return getChangedRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch branch cubes matching from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getCubeRecords(Connection c, ApplicationID appId, String pattern)
    {
        pattern = convertPattern(pattern);

        String sql = "SELECT n_cube_id, n.n_cube_nm, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, n.revision_number, n.branch_id, n.cube_value_bin FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm like ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') and branch_id = ?" +
                " GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.revision_number >= 0 AND " +
                "n.n_cube_nm like ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') and n.branch_id = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {

            stmt.setString(1, pattern);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setString(7, pattern);
            stmt.setString(8, appId.getApp());
            stmt.setString(9, appId.getVersion());
            stmt.setString(10, appId.getStatus());
            stmt.setString(11, appId.getTenant());
            stmt.setString(12, appId.getBranch());
            return getCubeInfoRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch cubes matching '" + pattern + "' from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getDeletedCubeRecords(Connection c, ApplicationID appId, String pattern)
    {
        pattern = convertPattern(pattern);

        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n.n_cube_nm, app_cd, notes_bin, version_no_cd, status_cd, create_dt, create_hid, n.revision_number, n.branch_id, n.cube_value_bin FROM n_cube n, " +
                        "( " +
                        "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                        "  FROM n_cube " +
                        "  WHERE n_cube_nm like ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                        "  GROUP BY n_cube_nm " +
                        ") m " +
                        "WHERE n.revision_number < 0 AND n.n_cube_nm like ? AND m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND " +
                        "n.app_cd = ? AND n.version_no_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?"))
        {
            stmt.setString(1, pattern);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());
            stmt.setString(6, pattern);
            stmt.setString(7, appId.getApp());
            stmt.setString(8, appId.getVersion());
            stmt.setString(9, appId.getTenant());
            stmt.setString(10, appId.getBranch());
            return getCubeInfoRecords(appId, stmt);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch deleted cubes from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getRevisions(Connection c, ApplicationID appId, String cubeName)
    {
        //TODO:  status_cd?
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, create_hid, revision_number, branch_id, cube_value_bin " +
                "FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            Object[] records = getCubeInfoRecords(appId, stmt);
            if (records.length == 0)
            {
                throw new IllegalArgumentException("Cannot fetch revision history for cube:  " + cubeName + " as it does not exist in app:  " + appId);
            }
            return records;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to get revision history for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private Object[] getChangedRecords(ApplicationID appId, PreparedStatement stmt) throws Exception
    {
        List<NCubeInfoDto> list = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.name = rs.getString("n_cube_nm");
                dto.branch = rs.getString("branch_id");
                dto.tenant = appId.getTenant();
                byte[] notes = rs.getBytes(NOTES_BIN);
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = appId.getVersion();
                dto.status = rs.getString("status_cd");
                dto.app = appId.getApp();
                dto.createDate = rs.getDate("create_dt");
                dto.createHid = rs.getString("create_hid");
                dto.revision = Long.toString(rs.getLong("revision_number"));
                byte[] jsonBytes = rs.getBytes("cube_value_bin");

                if (!ArrayUtilities.isEmpty(jsonBytes))
                {
                    String json = StringUtilities.createString(jsonBytes, "UTF-8");
                    Matcher m = Regexes.changeTypePattern.matcher(json);
                    if (m.find() && m.groupCount() > 0)
                    {
                        dto.changeType = m.group(1);
                    }

                    if (dto.changeType == null) {
                        continue;
                    }

                    m = Regexes.sha1Pattern.matcher(json);
                    if (m.find() && m.groupCount() > 0)
                    {
                        dto.sha1 = m.group(1);
                    }

                    //  Have to pull out the original head sha1 from which this branch was made.
                    //  We cannot calculate this on saves so has to be stored.
                    m = Regexes.headSha1Pattern.matcher(json);
                    if (m.find() && m.groupCount() > 0)
                    {
                        dto.headSha1 = m.group(1);
                    }

                }

                list.add(dto);
            }
        }
        return list.toArray(new NCubeInfoDto[list.size()]);
    }

    private Object[] getCubeInfoRecords(ApplicationID appId, PreparedStatement stmt) throws Exception
    {
        List<NCubeInfoDto> list = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.name = rs.getString("n_cube_nm");
                dto.branch = rs.getString("branch_id");
                dto.tenant = appId.getTenant();
                byte[] notes = rs.getBytes(NOTES_BIN);
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = appId.getVersion();
                dto.status = rs.getString("status_cd");
                dto.app = appId.getApp();
                dto.createDate = rs.getDate("create_dt");
                dto.createHid = rs.getString("create_hid");
                dto.revision = Long.toString(rs.getLong("revision_number"));
                byte[] jsonBytes = rs.getBytes("cube_value_bin");

                if (!ArrayUtilities.isEmpty(jsonBytes))
                {
                    String json = StringUtilities.createString(jsonBytes, "UTF-8");
                    Matcher m = Regexes.sha1Pattern.matcher(json);
                    if (m.find() && m.groupCount() > 0)
                    {
                        dto.sha1 = m.group(1);
                    }

                    //  Only do this when not head.
                    if (!ApplicationID.HEAD.equals(appId.getBranch()))
                    {
                        //  Have to pull out the original head sha1 from which this branch was made.
                        //  We cannot calculate this on saves so has to be stored.
                        m = Regexes.headSha1Pattern.matcher(json);
                        if (m.find() && m.groupCount() > 0)
                        {
                            dto.headSha1 = m.group(1);
                        }

                        m = Regexes.changeTypePattern.matcher(json);
                        if (m.find() && m.groupCount() > 0)
                        {
                            dto.changeType = m.group(1);
                        }

                    }
                }

                list.add(dto);
            }
        }
        return list.toArray();
    }

    public NCube loadCube(Connection c, NCubeInfoDto cubeInfo, Integer revision)
    {
        final ApplicationID appId = cubeInfo.getApplicationID();
        String rev = revision == null ? "abs(n.revision_number)" : revision.toString();


        //  Specific Cube, no branch magic on HEAD vs. BRANCH
        String sql = "SELECT n.n_cube_nm, app_cd, version_no_cd, status_cd, n.revision_number, n.branch_id, n.cube_value_bin FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                "  GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = " + rev +
                " AND n.n_cube_nm = ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeInfo.name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setString(7, cubeInfo.name);
            stmt.setString(8, appId.getApp());
            stmt.setString(9, appId.getVersion());
            stmt.setString(10, appId.getStatus());
            stmt.setString(11, appId.getTenant());
            stmt.setString(12, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube ncube = NCubeManager.ncubeFromJson(json);
                    ncube.name = cubeInfo.name;
                    ncube.setApplicationID(cubeInfo.getApplicationID());
                    return ncube;
                }
            }
        }
        catch (Exception e)
        {
            String s = "Unable to load cube: " + cubeInfo + " from database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }

        throw new IllegalArgumentException("Unable to load cube: " + cubeInfo + " from database");
     }

    public void restoreCube(Connection c, ApplicationID appId, String cubeName, String username)
    {
        String sql = "SELECT n.n_cube_nm, app_cd, version_no_cd, status_cd, n.revision_number as rev, n.branch_id, n.cube_value_bin, n.test_data_bin, n.notes_bin FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                "  GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.n_cube_nm = ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setString(7, cubeName);
            stmt.setString(8, appId.getApp());
            stmt.setString(9, appId.getVersion());
            stmt.setString(10, appId.getStatus());
            stmt.setString(11, appId.getTenant());
            stmt.setString(12, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Long revision = rs.getLong("rev");

                    if (revision >= 0)
                    {
                        throw new IllegalArgumentException("Cube: " + cubeName + " is already restored in app: " + appId);
                    }

                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    jsonBytes = setChangeType(jsonBytes, ChangeType.RESTORED);


                    String insertSql =
                            "INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, notes_bin, test_data_bin) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insert = c.prepareStatement(insertSql))
                    {
                        insert.setLong(1, UniqueIdGenerator.getUniqueId());
                        insert.setString(2, appId.getApp());
                        insert.setString(3, cubeName);
                        insert.setBytes(4, jsonBytes);
                        insert.setString(5, appId.getVersion());
                        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                        insert.setDate(6, now);
                        insert.setString(7, username);
                        insert.setString(8, appId.getTenant());
                        insert.setString(9, appId.getBranch());
                        insert.setLong(10, Math.abs(revision) + 1);
                        insert.setBytes(11, StringUtilities.getBytes("restored on " + now + " by " + username, "UTF-8"));
                        insert.setBytes(12, rs.getBytes(TEST_DATA_BIN));

                        int rowCount = insert.executeUpdate();

                        //TODO:  This cannot happen because of the getMaxRevision() check at the beginning of the method
                        //TODO:  We may need to just replace this with a return of the update count and let the controllers
                        //TODO:  handle it.  I've mocked it out anyway for now.
                        if (rowCount != 1)
                        {
                            throw new IllegalStateException("Cannot restore n-cube: " + cubeName + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                        }
                        return;
                    }
                }

                throw new IllegalArgumentException("Cannot restore cube: " + cubeName + " as it does not exist in app: " + appId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (SQLException e) {
            String s = "Unable to restore cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean deleteCubes(Connection c, ApplicationID appId)
    {
        String sql = "DELETE FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement ps = c.prepareStatement(sql))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());
            ps.setString(4, appId.getBranch());
            return ps.executeUpdate() > 0;
        }
        catch (Exception e)
        {
            String s = "Unable to delete cubes for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean rollbackCube(Connection c, ApplicationID appId, String cubeName)
    {
        //TODO  AND STATUS_CD?
        String sql = "DELETE FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND n_cube_nm = ? AND revision_number <> 0 AND revision_number <> -1";

        try (PreparedStatement ps = c.prepareStatement(sql))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());
            ps.setString(4, appId.getBranch());
            ps.setString(5, cubeName);
            return ps.executeUpdate() > 0;
        }
        catch (Exception e)
        {
            String s = "Unable to rollback cube: " + cubeName + " for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean deleteCube(Connection c, ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        if (allowDelete)
        {
            String sql = "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

            try (PreparedStatement ps = c.prepareStatement(sql))
            {
                ps.setString(1, appId.getApp());
                ps.setString(2, cubeName);
                ps.setString(3, appId.getVersion());
                ps.setString(4, appId.getTenant());
                ps.setString(5, appId.getBranch());
                return ps.executeUpdate() > 0;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
        else
        {
            String sql = "SELECT n.n_cube_nm, app_cd, version_no_cd, status_cd, n.revision_number as rev, n.branch_id, n.cube_value_bin, n.test_data_bin, n.notes_bin FROM n_cube n, " +
                    "( " +
                    "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                    "  FROM n_cube " +
                    "  WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                    "  GROUP BY n_cube_nm " +
                    ") m " +
                    "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.revision_number >= 0 AND n.n_cube_nm = ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";

            try (PreparedStatement stmt = c.prepareStatement(sql))
            {
                stmt.setString(1, cubeName);
                stmt.setString(2, appId.getApp());
                stmt.setString(3, appId.getVersion());
                stmt.setString(4, appId.getStatus());
                stmt.setString(5, appId.getTenant());
                stmt.setString(6, appId.getBranch());
                stmt.setString(7, cubeName);
                stmt.setString(8, appId.getApp());
                stmt.setString(9, appId.getVersion());
                stmt.setString(10, appId.getStatus());
                stmt.setString(11, appId.getTenant());
                stmt.setString(12, appId.getBranch());

                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        Long revision = rs.getLong("rev");

                        //  TODO:  In restoreCube() and updateCube() we throw an exception when there is nothing to update.
                        //  TODO:  For consistency do we want to do the same thing here?
                        if (revision == null)
                        {
                            return false;
                        }

                        byte[] jsonBytes = rs.getBytes("cube_value_bin");

                        jsonBytes = setChangeType(jsonBytes, ChangeType.DELETED);

                        try (PreparedStatement insert = c.prepareStatement(
                                "INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, notes_bin, test_data_bin) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                        {
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, appId.getApp());
                            insert.setString(3, cubeName);
                            insert.setBytes(4, jsonBytes);
                            insert.setString(5, appId.getVersion());
                            java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                            insert.setDate(6, now);
                            insert.setString(7, username);
                            insert.setString(8, appId.getTenant());
                            insert.setString(9, appId.getBranch());
                            insert.setLong(10, -(revision + 1));
                            String note = "deleted on " + now + " by " + username;
                            insert.setBytes(11, note.getBytes("UTF-8"));
                            insert.setBytes(12, rs.getBytes(TEST_DATA_BIN));

                            if (insert.executeUpdate() != 1)
                            {
                                throw new IllegalStateException("Cannot delete n-cube: " + cubeName + "', app: " + appId + ", row was not deleted");
                            }
                            return true;
                        }
                    }
                }
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId;
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
        return false;
    }

    public boolean updateNotes(Connection c, ApplicationID appId, String cubeName, String notes)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " is deleted in app: " + appId);
        }

        String statement = "UPDATE n_cube SET notes_bin = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setLong(7, maxRev);
            return stmt.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to update notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getNotes(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT notes_bin FROM n_cube " +
                        "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                        "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] notes = rs.getBytes("notes_bin");
                    return new String(notes == null ? "".getBytes() : notes, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Could not fetch notes, no cube: " + cubeName + " in app: " + appId);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }


    public int createBranch(Connection c, ApplicationID appId)
    {
        if (doCubesExist(c, appId))
        {
            throw new IllegalStateException("Branch already exists, app: " + appId);
        }

        try
        {
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT n_cube_id, n.n_cube_nm, cube_value_bin, app_cd, notes_bin, version_no_cd, status_cd, branch_id, create_dt, create_hid, test_data_bin, revision_number as rev FROM n_cube n, " +
                            "( " +
                            "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                            "  FROM n_cube " +
                            "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                            "  GROUP BY n_cube_nm " +
                            ") m " +
                            "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND " +
                            "n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?"))
            {
                stmt.setString(1, appId.getApp());
                stmt.setString(2, appId.getVersion());
                stmt.setString(3, ReleaseStatus.SNAPSHOT.name());
                stmt.setString(4, appId.getTenant());
                stmt.setString(5, ApplicationID.HEAD);
                stmt.setString(6, appId.getApp());
                stmt.setString(7, appId.getVersion());
                stmt.setString(8, ReleaseStatus.SNAPSHOT.name());
                stmt.setString(9, appId.getTenant());
                stmt.setString(10, ApplicationID.HEAD);
                try (ResultSet rs = stmt.executeQuery())
                {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, create_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd, branch_id, revision_number) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        int count = 0;
                        while (rs.next())
                        {
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));

                            byte[] jsonBytes = rs.getBytes("cube_value_bin");

                            if (!ArrayUtilities.isEmpty(jsonBytes))
                            {
                                jsonBytes = injectHeadSha1FromSha1(jsonBytes);
                            }
                            insert.setBytes(3, jsonBytes);

                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(5, rs.getString("create_hid"));
                            insert.setString(6, appId.getVersion());
                            insert.setString(7, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(8, appId.getApp());
                            insert.setBytes(9, rs.getBytes(TEST_DATA_BIN));
                            insert.setBytes(10, rs.getBytes(NOTES_BIN));
                            insert.setString(11, appId.getTenant());
                            insert.setString(12, appId.getBranch());
                            insert.setLong(13, (rs.getLong("rev") >= 0) ? 0 : -1);
                            insert.addBatch();
                            count++;
                        }
                        if (count > 0)
                        {
                            insert.executeBatch();
                        }
                        return count;
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create new BRANCH for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

//  TODO:  Might be needed for loading database with cubes from disk
//    public String getHeadSha1(byte[] jsonBytes)
//    {
//        StringBuffer sb = new StringBuffer();
//        String json = StringUtilities.createString(jsonBytes, "UTF-8");
//        Matcher m = Regexes.headSha1Pattern.matcher(json);
//        if (m.find() && m.groupCount() > 0)
//        {
//            return m.group(1);
//        }
//        return null;
//    }

    private byte[] injectHeadSha1FromSha1(byte[] jsonBytes)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.sha1Pattern.matcher(json);
        if (m.find() && m.groupCount() > 0)
        {
            String replacement = ", \"sha1\":\"" + m.group(1) + "\", \"headSha1\":\"" + m.group(1) + "\"";
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return StringUtilities.getBytes(sb.toString(), "UTF-8");
    }

    private byte[] resetHeadSha1AndChangeType(byte[] jsonBytes, String newSha1)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.headSha1Pattern.matcher(json);
        //  replace headSha1 with existing sha1
        if (m.find() && m.groupCount() > 0)
        {
            m.appendReplacement(sb, ", \"headSha1\":\"" + newSha1 + "\"");
        }
        m.appendTail(sb);
        json = sb.toString();

        // remove change type
        sb = new StringBuffer();
        m = Regexes.changeTypePattern.matcher(json);
        if (m.find() && m.groupCount() > 0)
        {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        json = sb.toString();

        return StringUtilities.getBytes(json, "UTF-8");
    }

    private byte[] setChangeType(byte[] jsonBytes, ChangeType type)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.changeTypePattern.matcher(json);
        if (m.find() && m.groupCount() > 0)
        {
            m.appendReplacement(sb, ", \"changeType\":\"" + type.toString() + "\"");
            m.appendTail(sb);
        }
        else
        {
            m = Regexes.sha1Pattern.matcher(json);
            if (m.find() && m.groupCount() > 0) {
                m.appendReplacement(sb, ", \"sha1\":\"" + m.group(1) + "\", \"changeType\":\"" + type.toString() + "\"");
                m.appendTail(sb);
            }

        }
        return StringUtilities.getBytes(sb.toString(), "UTF-8");
    }


    public int releaseCubes(Connection c, ApplicationID appId, String newSnapVer)
    {
        if (doReleaseCubesExist(c, appId))
        {
            throw new IllegalStateException("A RELEASE version " + appId.getVersion() + " already exists, app: " + appId);
        }

        // Step 1: Update version number to new version where branch != HEAD (and rest of appId matches) ignore revision
        try
        {
            try (PreparedStatement statement = c.prepareStatement(
                    "UPDATE n_cube SET version_no_cd = ? " +
                    "WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id != 'HEAD'"))
            {
                statement.setString(1, newSnapVer);
                statement.setString(2, appId.getApp());
                statement.setString(3, appId.getVersion());
                statement.setString(4, appId.getTenant());
                statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to move branched snapshot cubes for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

        // Step 2: Release cubes where branch == HEAD (change their status from SNAPSHOT to RELEASE)
        try
        {
            try (PreparedStatement statement = c.prepareStatement(
                    "UPDATE n_cube SET create_dt = ?, status_cd = ? " +
                    "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = 'HEAD'"))
            {
                statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                statement.setString(2, ReleaseStatus.RELEASE.name());
                statement.setString(3, appId.getApp());
                statement.setString(4, appId.getVersion());
                statement.setString(5, ReleaseStatus.SNAPSHOT.name());
                statement.setString(6, appId.getTenant());
                statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to release head cubes for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

        // Step 3: Create new SNAPSHOT cubes from the HEAD RELEASE cubes (next version higher, started for development)
        try
        {
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT n_cube_id, n.n_cube_nm, cube_value_bin, app_cd, notes_bin, version_no_cd, status_cd, branch_id, create_dt, create_hid, test_data_bin, n.revision_number FROM n_cube n, " +
                            "( " +
                            "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                            "  FROM n_cube " +
                            "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? " +
                            "  GROUP BY n_cube_nm " +
                            ") m " +
                            "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.revision_number >= 0 AND " +
                            "n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?"))
            {
                stmt.setString(1, appId.getApp());
                stmt.setString(2, appId.getVersion());
                stmt.setString(3, ReleaseStatus.RELEASE.name());
                stmt.setString(4, appId.getTenant());
                stmt.setString(5, appId.getBranch());
                stmt.setString(6, appId.getApp());
                stmt.setString(7, appId.getVersion());
                stmt.setString(8, ReleaseStatus.RELEASE.name());
                stmt.setString(9, appId.getTenant());
                stmt.setString(10, ApplicationID.HEAD);
                try (ResultSet rs = stmt.executeQuery())
                {
                    int count = 0;
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, create_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd, branch_id, revision_number) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        while (rs.next())
                        {
                            count++;
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));
                            insert.setBytes(3, rs.getBytes("cube_value_bin"));
                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(5, rs.getString("create_hid"));
                            insert.setString(6, newSnapVer);
                            insert.setString(7, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(8, appId.getApp());
                            insert.setBytes(9, rs.getBytes(TEST_DATA_BIN));
                            insert.setBytes(10, rs.getBytes(NOTES_BIN));
                            insert.setString(11, appId.getTenant());
                            insert.setString(12, ApplicationID.HEAD);
                            insert.setLong(13, 0);      // New SNAPSHOT revision numbers start at 0
                            insert.addBatch();
                        }

                        if (count > 0)
                        {
                            insert.executeBatch();
                        }
                    }
                    return count;
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create SNAPSHOT cubes for app: " + appId + ", new version: " + newSnapVer + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int changeVersionValue(Connection c, ApplicationID appId, String newVersion)
    {
        if (doCubesExist(c, appId.createNewSnapshotId(newVersion)))
        {
            throw new IllegalStateException("Cannot change version value to " + newVersion + " because this version already exists.  Choose a different version number, app: " + appId);
        }

        try
        {
            try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?"))
            {
                ps.setString(1, newVersion);
                ps.setString(2, appId.getApp());
                ps.setString(3, appId.getVersion());
                ps.setString(4, ReleaseStatus.SNAPSHOT.name());
                ps.setString(5, appId.getTenant());
                ps.setString(6, appId.getBranch());

                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + appId.getVersion() + ", therefore no versions updated, app: " + appId);
                }
                return count;
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to change SNAPSHOT version from " + appId.getVersion() + " to " + newVersion + " for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getTestData(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT test_data_bin FROM n_cube " +
                        "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                        "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] testData = rs.getBytes("test_data_bin");
                    return testData == null ? "" : new String(testData, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Unable to fetch test data, cube: " + cubeName + ", app: " + appId + " does not exist.");
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch test data for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }



    public boolean updateTestData(Connection c, ApplicationID appId, String cubeName, String testData)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);

        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " is deleted in app: " + appId);
        }

        try (PreparedStatement stmt = c.prepareStatement(
                "UPDATE n_cube SET test_data_bin=? " +
                        "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?"))
        {
            stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, ReleaseStatus.SNAPSHOT.name());
            stmt.setString(6, appId.getTenant());
            stmt.setString(7, appId.getBranch());
            stmt.setLong(8, maxRev);
            return stmt.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to update test data for NCube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean renameCube(Connection c, ApplicationID appId, NCube cube, String newName)
    {
        //  Save in case exception happens and we have to reset proper name on the cube.
        String oldName = cube.getName();

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE n_cube SET n_cube_nm = ?, cube_value_bin = ? " +
                "WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT.name() + "' AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?"))
        {
            //  We have to set the new  name on the cube toFormatJson with the proper name on it.
            cube.name = newName;

            ps.setString(1, newName);
            ps.setBytes(2, cube.toFormattedJson().getBytes("UTF-8"));   // Need to update cube name in JSON in bin column
            ps.setString(3, appId.getApp());
            ps.setString(4, appId.getVersion());
            ps.setString(5, oldName);
            ps.setString(6, appId.getTenant());
            ps.setString(7, appId.getBranch());

            int count = ps.executeUpdate();
            if (count < 1)
            {
                throw new IllegalArgumentException("Could not rename cube, no cube found to rename, for app:" + appId + ", original name: " + oldName + ", new name: " + newName);
            }

            return true;
        }
        catch (RuntimeException e)
        {
            cube.name = oldName;
            throw e;
        }
        catch (Exception e)
        {
            cube.name = oldName;
            String s = "Unable to rename cube: " + oldName + ", app: " + appId + ", new name: " + newName + " due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());
            ps.setString(4, appId.getBranch());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for existing cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doReleaseCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = '" + ApplicationID.HEAD + "'";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, ReleaseStatus.RELEASE.name());
            ps.setString(4, appId.getTenant());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for release cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doesCubeExist(Connection c, ApplicationID appId, String name)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for cube:  " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public Object[] getAppNames(Connection connection, ApplicationID appId)
    {
        String sql = "SELECT DISTINCT app_cd FROM n_cube WHERE tenant_cd = RPAD(?, 10, ' ') and branch_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            List<String> records = new ArrayList<>();

            stmt.setString(1, appId.getTenant());
            stmt.setString(2, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }
            Collections.sort(records);
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all app names from database for tenant: " + appId.getTenant() + ", branch: " + appId.getBranch();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getAppVersions(Connection connection, ApplicationID appId)
    {
        final String sql = "SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') and branch_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, appId.getStatus());
            stmt.setString(3, appId.getTenant());
            stmt.setString(4, appId.getBranch());

            List<String> records = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }

            Collections.sort(records);  // May need to enhance to ensure 2.19.1 comes after 2.2.1
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all versions for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Set<String> getBranches(Connection connection, String tenant)
    {
        final String sql = "SELECT DISTINCT branch_id FROM n_cube WHERE tenant_cd = RPAD(?, 10, ' ')";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, tenant);

            Set<String> branches = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    branches.add(rs.getString(1));
                }
            }

            return branches;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all branches for tenant: " + tenant + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private static String convertPattern(String pattern)
    {
        if (StringUtilities.isEmpty(pattern))
        {
            pattern = "%";
        }
        else
        {
            pattern = pattern.replace('*', '%');
            pattern = pattern.replace('?', '_');
        }
        return pattern;
    }


    public Map commitBranch(Connection c, ApplicationID appId, Collection<NCubeInfoDto> dtos, String username)
    {
        Map<String, String> changes = new LinkedHashMap<>();
        ApplicationID headAppId = appId.asHead();

        for (NCubeInfoDto dto : dtos)
        {
            Long revision = Long.parseLong(dto.revision);
            copyBranchCubeToHead(c, appId, headAppId, dto.name, username, revision);
            replaceHeadSha1(c, appId, dto.name, dto.sha1, revision);
            changes.put(dto.name, ChangeType.fromCode(dto.changeType));
        }
        return changes;
    }

    public int rollbackBranch(Connection c, ApplicationID appId, Object[] infoDtos)
    {
        int count = 0;
        for (Object dto : infoDtos)
        {
            NCubeInfoDto info = (NCubeInfoDto)dto;
            if (rollbackCube(c, appId, info.name)) {
                count++;
            }
        }
        return count;
    }

    public Object[] updateBranch(Connection c, ApplicationID appId)
    {
        // TODO: Persister needs to implement this
        // TODO: When a user selects updateBranch, the following steps happen:
        // TODO: 1. All cubes in the main branch are checked against the cubes in their branch.  If a cube name
        // TODO: matches one in their branch, and they have not modified it and the SHA1's still match, move on
        // TODO: to the next.
        // TODO: 2. If a cube name matches a cube name in their branch and they have not modified, but the main
        // TODO: branch has changed, then you can safely delete their cube (mark it deleted -or- replace it (newer
        // TODO: version) and copy over the cube from the HEAD branch.
        // TODO: 3. If a cube name exists in the main branch that does not exist in their branch, then the cube either
        // TODO: needs to be added (or possibly restored).
        // TODO: 4. If they have any cubes that remain, that do not match the head, and they are not changed, then those
        // TODO: cubes need to be deleted.
        // TODO: 5. If the cube name matches a cube name in their branch, but they have changed it, then skip it (Do
        // TODO: not update it).  Return a list of these (we will show this list to the user letting them know they
        // TODO: have potential conflicts.

        return new Object[0];
    }
}
