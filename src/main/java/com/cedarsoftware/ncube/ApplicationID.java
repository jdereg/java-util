package com.cedarsoftware.ncube;

import com.cedarsoftware.util.StringUtilities;

import java.util.regex.Matcher;

/**
 * This class binds together Tenant, App, and version.  These fields together
 * completely identify the application (and version) that a given n-cube belongs
 * to.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class ApplicationID
{
    public static final String DEFAULT_TENANT = "NONE";
    public static final String DEFAULT_APP = "DEFAULT_APP";
    public static final String DEFAULT_VERSION = "999.99.9";
    public static final String DEFAULT_STATUS = ReleaseStatus.SNAPSHOT.name();
    public static final String HEAD = "HEAD";
    public static final String TEST_BRANCH = "TEST";

    public static final transient ApplicationID testAppId = new ApplicationID(DEFAULT_TENANT, DEFAULT_APP, DEFAULT_VERSION, DEFAULT_STATUS, TEST_BRANCH);

    private final String tenant;
    private final String app;
    private final String version;
    private final String status;
    private final String branch;

    // For serialization support only
    private ApplicationID()
    {
        tenant = DEFAULT_TENANT;
        app = DEFAULT_APP;
        version = DEFAULT_VERSION;
        status = ReleaseStatus.SNAPSHOT.name();
        branch = HEAD;
    }

    /**
     * This constructor is used only by Test Code
     */
    @Deprecated
    public ApplicationID(String tenant, String app, String version, String status)
    {
        throw new RuntimeException("Use the 5 argument constructor - add branch as the last argument");
    }

    public ApplicationID(String tenant, String app, String version, String status, String branch)
    {
        this.tenant = tenant;
        this.app = app;
        this.version = version;
        this.status = status;
        this.branch = branch;
        validate();
    }

    public String getTenant()
    {
        return tenant;
    }

    public String getApp()
    {
        return app;
    }

    public String getVersion()
    {
        return version;
    }

    public String getStatus()
    {
        return status;
    }

    public String getBranch()
    {
        return branch;
    }

    public String cacheKey()
    {
        return cacheKey("");
    }

    public String cacheKey(String name)
    {
        if (StringUtilities.isEmpty(name))
        {
            return (tenant + " / " + app + " / " + version + " / " + branch + " /").toLowerCase();
        }
        return (tenant + " / " + app + " / " + version + " / " + branch + " / " + name).toLowerCase();
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ApplicationID))
        {
            return false;
        }

        ApplicationID that = (ApplicationID) o;

        return StringUtilities.equalsIgnoreCase(tenant, that.tenant) &&
                StringUtilities.equalsIgnoreCase(app, that.app) &&
                StringUtilities.equalsIgnoreCase(status, that.status) &&
                StringUtilities.equals(version, that.version) &&
                StringUtilities.equalsIgnoreCase(branch, that.branch);

    }

    public int hashCode()
    {
        int result = tenant.toLowerCase().hashCode();
        result = 31 * result + app.toLowerCase().hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + status.toUpperCase().hashCode();
        result = 31 * result + branch.toLowerCase().hashCode();
        return result;
    }

    public String toString()
    {
        return cacheKey();
    }

    public boolean isSnapshot()
    {
        return ReleaseStatus.SNAPSHOT.name().equals(status);
    }

    public boolean isRelease()
    {
        return ReleaseStatus.RELEASE.name().equals(status);
    }

    /**
     * Creates a new SNAPSHOT version of this application id.
     * @param ver new version.
     * @return a new ApplicationId that is a snapshot of the new version passed in.
     */
    public ApplicationID createNewSnapshotId(String ver)
    {
        //  In the Change Version the status was always SNAPSHOT when creating a new version.
        //  That is why we hardcode this to snapshot here.
        return new ApplicationID(tenant, app, ver, ReleaseStatus.SNAPSHOT.name(), branch);
    }

    public void validate()
    {
        validateTenant(tenant);
        validateApp(app);
        validateVersion(version);
        validateStatus(status);
        validateBranch(branch);
    }

    static void validateTenant(String tenant)
    {
        if (StringUtilities.hasContent(tenant))
        {
            Matcher m = Regexes.validTenantName.matcher(tenant);
            if (m.find() && tenant.length() <= 10)
            {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid tenant string: '" + tenant + "'. Tenant must contain only A-Z, a-z, or 0-9 and dash (-). From 1 to 10 characters max.");
    }

    static void validateApp(String appName)
    {
        if (StringUtilities.isEmpty(appName))
        {
            throw new IllegalArgumentException("App cannot be null or empty");
        }
    }

    static void validateStatus(String status)
    {
        if (status == null)
        {
            throw new IllegalArgumentException("status name cannot be null");
        }
        ReleaseStatus.valueOf(status);
    }

    static void validateBranch(String branch)
    {
        if (StringUtilities.isEmpty(branch))
        {
            throw new IllegalArgumentException("n-cube branch cannot be null or empty");
        }
        Matcher m = Regexes.validBranch.matcher(branch);
        if (m.find() && branch.length() <= 80)
        {
            return;
        }
        throw new IllegalArgumentException("Invalid branch: '" + branch + "'. n-cube branch must contain only A-Z, a-z, or 0-9 dash(-), underscore (_), and dot (.) From 1 to 80 characters.");
    }

    void validateBranchIsNotHead() {
        if (branch.equals(HEAD)) {
            throw new IllegalArgumentException("Branch cannot be 'HEAD'");
        }
    }

    void validateStatusIsNotRelease() {
        if (isRelease()) {
            throw new IllegalArgumentException("Status cannot be 'RELEASE'");
        }
    }

    public static void validateVersion(String version)
    {
        if (StringUtilities.isEmpty(version))
        {
            throw new IllegalArgumentException("n-cube version cannot be null or empty");
        }

        Matcher m = Regexes.validVersion.matcher(version);
        if (m.find())
        {
            return;
        }
        throw new IllegalArgumentException("Invalid version: '" + version + "'. n-cube version must follow the form n.n.n where n is a number 0 or greater. The numbers stand for major.minor.revision");
    }

    //TODO: We need to allow DEFAULT_BRANCH (HEAD) to be overridden below with an environment variable (or -Dsystem.property)
    public static ApplicationID getBootVersion(String tenant, String app)
    {
        return new ApplicationID(tenant, app, "0.0.0", ReleaseStatus.SNAPSHOT.name(), HEAD);
    }
}
