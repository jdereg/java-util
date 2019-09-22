package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.*;

/**
 * Generate a unique ID that fits within a long value quickly, will never create a duplicate value,
 * even if called insanely fast, and it incorporates part of the IP address so that machines in
 * a cluster will not create duplicates.  It guarantees no duplicates because it keeps
 * the last 100 generated, and compares those against the value generated, if it matches, it
 * will continue generating until it does not match.  It will generate 100 per millisecond without
 * matching.  Once the requests for more than 100 unique IDs per millisecond is exceeded, the
 * caller will be slowed down, because it will be retrying.  Keep in mind, 100 per millisecond is
 * 10 microseconds continuously without interruption.
 *
 * @author John DeRegnaucourt (john@cedarsoftware.com)
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
public class UniqueIdGenerator
{
    private UniqueIdGenerator () {}

    private static final Object lock = new Object();
    private static final Object lock19 = new Object();
    private static int count = 0;
    private static int count2 = 0;
    private static final int clusterId;
    private static final Map<Long, Long> lastIds = new LinkedHashMap<Long, Long>()
    {
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest)
        {
            return size() > 1000;
        }
    };
    private static final Map<Long, Long> lastIdsFull = new LinkedHashMap<Long, Long>()
    {
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest)
        {
            return size() > 10000;
        }
    };

    static
    {
        String id = SystemUtilities.getExternalVariable("JAVA_UTIL_CLUSTERID");
        if (StringUtilities.isEmpty(id))
        {
            SecureRandom random = new SecureRandom();
            clusterId = Math.abs(random.nextInt()) % 100;
        }
        else
        {
            try
            {
                clusterId = Math.abs(Integer.parseInt(id)) % 100;
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Environment / System variable JAVA_UTIL_CLUSTERID must be 0-99");
            }
        }
    }

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

    /**
     * ID format will be 1234567890123.999.99 (no dots - only there for clarity - the number is a long).  There are
     * 13 digits for time - until 2286, and then it will be 14 digits for time - milliseconds since Jan 1, 1970.
     * This is followed by a count that is 000 through 999.  This is followed by a random 2 digit number. This number
     * is chosen when the JVM is started and then stays fixed until next restart.  This is to ensure cluster uniqueness.
     *
     * There is the possibility two machines could choose the same random number at start. Even still, collisions would
     * be highly unlikely because for a collision to occur, a number would have to be chosen at the same millisecond,
     * with the count at the same position.
     *
     * The returned ID will be 18 digits through 2286, and then it will be 19 digits through 5138.
     * 
     * This API is slower than the 19 digit API.  Grabbing a bunch of IDs super quick causes delays while it waits
     * for the millisecond to tick over.  This API can return 1,000 unique IDs per millisecond max.
     * @return long unique ID
     */
    private static long getUniqueIdAttempt()
    {
        count++;
        if (count >= 1000)
        {
            count = 0;
        }

        return System.currentTimeMillis() * 100000 + count * 100 + clusterId;
    }

    /**
     * ID format will be 1234567890123.9999.99 (no dots - only there for clarity - the number is a long).  There are
     * 13 digits for time - milliseconds since Jan 1, 1970. This is followed by a count that is 0000 through 9999.
     * This is followed by a random 2 digit number. This number is chosen when the JVM is started and then stays fixed
     * until next restart.  This is to ensure cluster uniqueness.
     *
     * There is the possibility two machines could choose the same random number at start. Even still, collisions would
     * be highly unlikely because for a collision to occur, a number would have to be chosen at the same millisecond,
     * with the count at the same position.
     *
     * The returned ID will be 19 digits and this API will work through 2286.  After then, it would likely return
     * negative numbers (still unique).
     *
     * This API is faster than the 18 digit API.  This API can return 10,000 unique IDs per millisecond max.
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

        return System.currentTimeMillis() * 1000000 + count2 * 100 + clusterId;
    }
}
