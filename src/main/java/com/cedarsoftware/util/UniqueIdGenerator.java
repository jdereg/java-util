package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    
    private static int count = -1;
    private static final int clusterId;
    private static final Map<Long, Long> lastIds = new LinkedHashMap<Long, Long>()
    {
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest)
        {
            return size() > 10000;
        }
    };
    
    static
    {
        SecureRandom random = new SecureRandom();
        clusterId = Math.abs(random.nextInt()) % 100;
    }
    
    public static long getUniqueId()
    {
        synchronized (UniqueIdGenerator.class)
        {
            long newId = getUniqueIdAttempt();

            while (lastIds.containsKey(newId))
            {
                newId = getUniqueIdAttempt();
            }
            lastIds.put(newId, null);
            return newId;
        }
    }
    
    private static long getUniqueIdAttempt()
    {
        // shift time by 4 digits (so that IP and size can be last 4 digits)
        count++;
        if (count >= 1000)
        {
            count = 0;
        }
        return Long.parseLong(String.format("%013d%03d%02d", System.currentTimeMillis(), count, clusterId));
    }
}
