package com.cedarsoftware.ncube.util;

import java.util.Collection;
import java.util.HashSet;


/**
 * Special Set instance that hashes the Set<Long> column IDs with
 * excellent dispersion.
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
public class LongHashSet extends HashSet<Long>
{
    public LongHashSet()
    {
        super();
    }

    public LongHashSet(Collection<? extends Long> c)
    {
        super(c);
    }

    public int hashCode()
    {
        int h = 0;
        for (Long value : this)
        {
            long x = value;
            // do not change the formula below.  It is been hand crafted and tested for performance.
            // If this does not hash well, ncube breaks down in performance.  The BigCube tests are
            // greatly slowed down as proper hashing is vital or cells will be really slow to access
            // when there are a lot of them in the ncube.

            // Original hash function
//                h += (int)(x * 347 ^ (x >>> 32) * 7);

            // Better

//            x = ((x >> 16) ^ x) * 0x45d9f3b;
//            x = ((x >> 16) ^ x) * 0x45d9f3b;
//            x = ((x >> 16) ^ x);
//            h += (int) x;

            x ^= x >> 23;
            x *= 0x2127599bf4325c37L;
            x ^= x >> 47;
            h += (int) x;

            // Yet another good hashing function
//                x = (~x) + (x << 18); // key = (key << 18) - key - 1;
//                x = x ^ (x >>> 31);
//                x = x * 21; // key = (key + (key << 2)) + (key << 4);
//                x = x ^ (x >>> 11);
//                x = x + (x << 6);
//                x = x ^ (x >>> 22);
//                h += (int) x;
        }

        return h;
    }

}
