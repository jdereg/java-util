package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;

/**
 * Generate a unique ID that fits within a long value.  The ID will be unique for the given JVM, and it makes a
 * solid attempt to ensure uniqueness in a clustered environment.  An environment variable <b>JAVA_UTIL_CLUSTERID</b>
 * can be set to a value 0-99 to mark this JVM uniquely in the cluster.  If this environment variable is not set,
 * then a SecureRandom value from 0-99 is chosen for the machine cluster id.<br>
 * <br>
 * There is an API [getUniqueId()] to get a unique ID that will work through the year 5138.  This API will generate
 * unique IDs at a rate of up to 1 million per second.  There is another API [getUniqueId19()] that will work through
 * the year 2286, however this API will generate unique IDs at a rate up to 10 million per second.  The trade-off is
 * the faster API will generate positive IDs only good for about 286 years [after 2000].<br>
 * <br>
 * The IDs are guaranteed to be strictly increasing.  There is an API you can call (getDate()) that will return the
 * date and time (to the millisecond) that they ID was created.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * Roger Judd (@HonorKnight on GitHub) for adding code to ensure increasing order.
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@SuppressWarnings("unchecked")
public class UniqueIdGenerator
{
    public static final String JAVA_UTIL_CLUSTERID = "JAVA_UTIL_CLUSTERID";

    private UniqueIdGenerator()
    {
    }

    private static final Object lock = new Object();
    private static final Object lock19 = new Object();
    private static int count = 0;
    private static int count2 = 0;
    private static long previousTimeMilliseconds = 0;
    private static long previousTimeMilliseconds2 = 0;
    private static final int serverId;
    private static final Map<Long, Long> lastIds = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 1000;
        }
    };
    private static final Map<Long, Long> lastIdsFull = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 10000;
        }
    };

    static
    {
        int id = getServerId(JAVA_UTIL_CLUSTERID);
        String setVia = "environment variable: " + JAVA_UTIL_CLUSTERID;
        if (id == -1)
        {
            String envName = SystemUtilities.getExternalVariable(JAVA_UTIL_CLUSTERID);
            if (StringUtilities.hasContent(envName))
            {
                String envValue = SystemUtilities.getExternalVariable(envName);
                id = getServerId(envValue);
                setVia = "environment variable: " + envName;
            }
            if (id == -1)
            {   // Try Cloud Foundry instance index
                id = getServerId("CF_INSTANCE_INDEX");
                setVia = "environment variable: CF_INSTANCE_INDEX";
                if (id == -1)
                {
                    // use random number if all else fails
                    SecureRandom random = new SecureRandom();
                    id = abs(random.nextInt()) % 100;
                    setVia = "new SecureRandom()";
                }
            }
        }
        System.out.println("java-util using server id=" + id + " for last two digits of generated unique IDs. Set using " + setVia);
        serverId = id;
    }

    private static int getServerId(String externalVarName)
    {
        String id = SystemUtilities.getExternalVariable(externalVarName);
        try
        {
            if (StringUtilities.isEmpty(id))
            {
                return -1;
            }
            return abs(parseInt(id)) % 100;
        }
        catch (NumberFormatException e)
        {
            System.err.println("Unable to get unique server id or index from environment variable/system property key-value: " + externalVarName + "=" + id);
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * ID format will be 1234567890123.999.99 (no dots - only there for clarity - the number is a long).  There are
     * 13 digits for time - good until 2286, and then it will be 14 digits (good until 5138) for time - milliseconds
     * since Jan 1, 1970.  This is followed by a count that is 000 through 999.  This is followed by a random 2 digit
     * number. This number is chosen when the JVM is started and then stays fixed until next restart.  This is to
     * ensure cluster uniqueness.<br>
     * <br>
     * There is the possibility two machines could choose the same random number at start. Even still, collisions would
     * be highly unlikely because for a collision to occur, a number would have to be chosen at the same millisecond
     * <b>with</b> the count at the same position.<br>
     * <br>
     * This API is slower than the 19 digit API.  Grabbing a bunch of IDs in a tight loop for example, could cause
     * delays while it waits for the millisecond to tick over.  This API can return 1,000 unique IDs per millisecond
     * max.<br>
     * <br>
     * The IDs returned are guaranteed to be strictly increasing.
     *
     * @return long unique ID
     */
    public static long getUniqueId()
    {
        synchronized (lock)
        {
            long id = getUniqueIdAttempt();
            while (lastIds.containsKey(id))
            {
                id = getUniqueIdAttempt();
            }
            lastIds.put(id, null);
            return id;
        }
    }

    private static long getUniqueIdAttempt()
    {
        count++;
        if (count >= 1000)
        {
            count = 0;
        }

        long currentTimeMilliseconds = currentTimeMillis();

        if (currentTimeMilliseconds > previousTimeMilliseconds)
        {
            count = 0;
            previousTimeMilliseconds = currentTimeMilliseconds;
        }

        return currentTimeMilliseconds * 100000 + count * 100L + serverId;
    }

    /**
     * ID format will be 1234567890123.9999.99 (no dots - only there for clarity - the number is a long).  There are
     * 13 digits for time - milliseconds since Jan 1, 1970. This is followed by a count that is 0000 through 9999.
     * This is followed by a random 2 digit number. This number is chosen when the JVM is started and then stays fixed
     * until next restart.  This is to ensure cluster uniqueness.<br>
     * <br>
     * There is the possibility two machines could choose the same random number at start. Even still, collisions would
     * be highly unlikely because for a collision to occur, a number would have to be chosen at the same millisecond
     * <b>with</b> the count at the same position.<br>
     * <br>
     * The returned ID will be 19 digits and this API will work through 2286.  After then, it would likely return
     * negative numbers (still unique).<br>
     * <br>
     * This API is faster than the 18 digit API.  This API can return 10,000 unique IDs per millisecond max.<br>
     * <br>
     * The IDs returned are guaranteed to be strictly increasing.
     *
     * @return long unique ID
     */
    public static long getUniqueId19()
    {
        synchronized (lock19)
        {
            long id = getFullUniqueId19();
            while (lastIdsFull.containsKey(id))
            {
                id = getFullUniqueId19();
            }
            lastIdsFull.put(id, null);
            return id;
        }
    }

    // Use up to 19 digits (much faster)
    private static long getFullUniqueId19()
    {
        count2++;
        if (count2 >= 10000)
        {
            count2 = 0;
        }

        long currentTimeMilliseconds = currentTimeMillis();

        if (currentTimeMilliseconds > previousTimeMilliseconds2)
        {
            count2 = 0;
            previousTimeMilliseconds2 = currentTimeMilliseconds;
        }

        return currentTimeMilliseconds * 1000000 + count2 * 100L + serverId;
    }

    /**
     * Find out when the ID was generated.
     *
     * @param uniqueId long unique ID that was generated from the .getUniqueId() API
     * @return Date when the ID was generated, with the time portion accurate to the millisecond. The time
     * is measured in milliseconds, between the time the id was generated and midnight, January 1, 1970 UTC.
     */
    public static Date getDate(long uniqueId)
    {
        return new Date(uniqueId / 100000);
    }

    /**
     * Find out when the ID was generated. "19" version.
     *
     * @param uniqueId long unique ID that was generated from the .getUniqueId19() API
     * @return Date when the ID was generated, with the time portion accurate to the millisecond. The time
     * is measured in milliseconds, between the time the id was generated and midnight, January 1, 1970 UTC.
     */
    public static Date getDate19(long uniqueId)
    {
        return new Date(uniqueId / 1000000);
    }
}
