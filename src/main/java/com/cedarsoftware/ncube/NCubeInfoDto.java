package com.cedarsoftware.ncube;

import java.util.Date;

/**
 * Class used to carry the NCube meta-information
 * to the client.
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
public class NCubeInfoDto
{
	public String id;
	public Date createDate;
	public Date updateDate;
	public String createHid;
	public String updateHid;
	public String name;
	public String version;
	public String status;
	public String notes;
	public String app;
	public Date sysEffDate;
	public Date sysEndDate;
	public Date bizEffDate;
	public Date bizExpDate;
	// public String cubeJsonData;  // Loaded separately
	// public String testJsonData;  // Loaded separately
}
