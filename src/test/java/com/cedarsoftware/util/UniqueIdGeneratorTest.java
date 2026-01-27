package com.cedarsoftware.util;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.cedarsoftware.util.LoggingConfig;

import static com.cedarsoftware.util.UniqueIdGenerator.getDate;
import static com.cedarsoftware.util.UniqueIdGenerator.getDate19;
import static com.cedarsoftware.util.UniqueIdGenerator.getInstant;
import static com.cedarsoftware.util.UniqueIdGenerator.getInstant19;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId19;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class UniqueIdGeneratorTest
{
    private static final Logger LOG = Logger.getLogger(UniqueIdGeneratorTest.class.getName());
    static {
        LoggingConfig.init();
    }

    private static final int bucketSize = 200000;

    @Test
    void testIdLengths()
    {
        long id18 = getUniqueId();
        long id19 = getUniqueId19();

        assert String.valueOf(id18).length() == 18;
        assert String.valueOf(id19).length() == 19;
    }

    @Test
    void testIDtoDate()
    {
        long id = getUniqueId();
        Date date = getDate(id);
        assert abs(date.getTime() - currentTimeMillis()) < 2;

        id = getUniqueId19();
        date = getDate19(id);
        assert abs(date.getTime() - currentTimeMillis()) < 2;
    }

    @Test
    void testIDtoInstant()
    {
        long id = getUniqueId();
        long currentTime = currentTimeMillis();
        Instant instant = getInstant(id);
        assert abs(instant.toEpochMilli() - currentTime) <= 2;

        id = getUniqueId19();
        instant = getInstant19(id);
        currentTime = currentTimeMillis();
        assert abs(instant.toEpochMilli() - currentTime) <= 2;
    }

    @Test
    void testGetDateWithNegativeId()
    {
        assertThrows(IllegalArgumentException.class, () -> getDate(-1L));
        assertThrows(IllegalArgumentException.class, () -> getDate(Long.MIN_VALUE));
    }

    @Test
    void testGetDate19WithNegativeId()
    {
        assertThrows(IllegalArgumentException.class, () -> getDate19(-1L));
        assertThrows(IllegalArgumentException.class, () -> getDate19(Long.MIN_VALUE));
    }

    @Test
    void testGetInstantWithNegativeId()
    {
        assertThrows(IllegalArgumentException.class, () -> getInstant(-1L));
        assertThrows(IllegalArgumentException.class, () -> getInstant(Long.MIN_VALUE));
    }

    @Test
    void testGetInstant19WithNegativeId()
    {
        assertThrows(IllegalArgumentException.class, () -> getInstant19(-1L));
        assertThrows(IllegalArgumentException.class, () -> getInstant19(Long.MIN_VALUE));
    }

    @Test
    void testUniqueIdGeneration()
    {
        int testSize = 100000;
        Long[] keep = new Long[testSize];
        Long[] keep19 = new Long[testSize];

        for (int i=0; i < testSize; i++)
        {
            keep[i] = getUniqueId();
            keep19[i] = getUniqueId19();
        }

        Set<Long> unique = new HashSet<>(testSize);
        Set<Long> unique19 = new HashSet<>(testSize);
        for (int i=0; i < testSize; i++)
        {
            unique.add(keep[i]);
            unique19.add(keep19[i]);
        }
        assertEquals(unique.size(), testSize);
        assertEquals(unique19.size(), testSize);
        
        assertMonotonicallyIncreasing(keep);
        assertMonotonicallyIncreasing(keep19);
    }
    
    /**
     * Asserts that the provided array of Longs is monotonically increasing (non-decreasing).
     * Assumes all elements in the array are non-null.
     *
     * @param ids the array of Longs to check
     */
    private void assertMonotonicallyIncreasing(Long[] ids) {
        for (int i = 1; i < ids.length; i++) {
            assertTrue(ids[i] >= ids[i - 1],
                    String.format("Array is not monotonically increasing at index %d: %d < %d",
                            i, ids[i], ids[i - 1]));
        }
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void speedTest()
    {
        long start = System.currentTimeMillis();
        int count = 0;
        while (System.currentTimeMillis() < start + 1000) {
            UniqueIdGenerator.getUniqueId19();
            count++;
        }
        LOG.info("count = " + count);
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testConcurrency()
    {
        final CountDownLatch startLatch = new CountDownLatch(1);
        int numTests = 4;
        final CountDownLatch finishedLatch = new CountDownLatch(numTests);

        // 18 digit ID buckets
        final Set<Long> bucket1 = new LinkedHashSet<>();
        final Set<Long> bucket2 = new LinkedHashSet<>();
        final Set<Long> bucket3 = new LinkedHashSet<>();
        final Set<Long> bucket4 = new LinkedHashSet<>();

        // 19 digit ID buckets
        final Set<Long> bucketA = new LinkedHashSet<>();
        final Set<Long> bucketB = new LinkedHashSet<>();
        final Set<Long> bucketC = new LinkedHashSet<>();
        final Set<Long> bucketD = new LinkedHashSet<>();

        Runnable test1 = () -> {
            await(startLatch);
            fillBucket(bucket1);
            fillBucket19(bucketA);
            finishedLatch.countDown();
        };

        Runnable test2 = () -> {
            await(startLatch);
            fillBucket(bucket2);
            fillBucket19(bucketB);
            finishedLatch.countDown();
        };

        Runnable test3 = () -> {
            await(startLatch);
            fillBucket(bucket3);
            fillBucket19(bucketC);
            finishedLatch.countDown();
        };

        Runnable test4 = () -> {
            await(startLatch);
            fillBucket(bucket4);
            fillBucket19(bucketD);
            finishedLatch.countDown();
        };

        long start = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(numTests);
        executor.execute(test1);
        executor.execute(test2);
        executor.execute(test3);
        executor.execute(test4);

        startLatch.countDown();  // trigger all threads to begin
        await(finishedLatch);   // wait for all threads to finish
        
        long end = System.nanoTime();
        LOG.info("(end - start) / 1000000.0 = " + (end - start) / 1000000.0);

        assertMonotonicallyIncreasing(bucket1.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucket2.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucket3.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucket4.toArray(new Long[]{}));

        assertMonotonicallyIncreasing(bucketA.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucketB.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucketC.toArray(new Long[]{}));
        assertMonotonicallyIncreasing(bucketD.toArray(new Long[]{}));

        // Assert that there are no duplicates between any buckets
        // Compare:
        //     1->2, 1->3, 1->4
        //     2->3, 2->4
        //     3->4
        // That covers all combinations.  Each bucket has 3 comparisons (can be on either side of the comparison).
        Set<Long> copy = new HashSet<>(bucket1);
        assert bucket1.size() == bucketSize;
        bucket1.retainAll(bucket2);
        assert bucket1.isEmpty();
        bucket1.addAll(copy);
        
        assert bucket1.size() == bucketSize;
        bucket1.retainAll(bucket3);
        assert bucket1.isEmpty();
        bucket1.addAll(copy);

        assert bucket1.size() == bucketSize;
        bucket1.retainAll(bucket4);
        assert bucket1.isEmpty();
        bucket1.addAll(copy);

        // Assert that there are no duplicates between bucket2 and any of the other buckets (bucket1/bucket2 has already been checked).
        copy = new HashSet<>(bucket2);
        assert bucket2.size() == bucketSize;
        bucket2.retainAll(bucket3);
        assert bucket2.isEmpty();
        bucket2.addAll(copy);

        assert bucket2.size() == bucketSize;
        bucket2.retainAll(bucket4);
        assert bucket2.isEmpty();
        bucket2.addAll(copy);

        // Assert that there are no duplicates between bucket3 and any of the other buckets (bucket3 has already been compared to 1 & 2)
        copy = new HashSet<>(bucket3);
        assert bucket3.size() == bucketSize;
        bucket3.retainAll(bucket4);
        assert bucket3.isEmpty();
        bucket3.addAll(copy);

        // Assert that there are no duplicates between bucketA and any of the other buckets (19 digit buckets).
        copy = new HashSet<>(bucketA);
        assert bucketA.size() == bucketSize;
        bucketA.retainAll(bucketB);
        assert bucketA.isEmpty();
        bucketA.addAll(copy);

        assert bucketA.size() == bucketSize;
        bucketA.retainAll(bucketC);
        assert bucketA.isEmpty();
        bucketA.addAll(copy);

        assert bucketA.size() == bucketSize;
        bucketA.retainAll(bucketD);
        assert bucketA.isEmpty();
        bucketA.addAll(copy);

        // Assert that there are no duplicates between bucket2 and any of the other buckets (bucketA/bucketB has already been checked).
        copy = new HashSet<>(bucketB);
        assert bucketB.size() == bucketSize;
        bucketB.retainAll(bucketC);
        assert bucketB.isEmpty();
        bucketB.addAll(copy);

        assert bucketB.size() == bucketSize;
        bucketB.retainAll(bucketD);
        assert bucketB.isEmpty();
        bucketB.addAll(copy);

        // Assert that there are no duplicates between bucket3 and any of the other buckets (bucketC has already been compared to A & B)
        copy = new HashSet<>(bucketC);
        assert bucketC.size() == bucketSize;
        bucketC.retainAll(bucketD);
        assert bucketC.isEmpty();
        bucketC.addAll(copy);
        
        executor.shutdown();
    }

    private void await(CountDownLatch latch)
    {
        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void fillBucket(Set<Long> bucket)
    {
        for (int i=0; i < bucketSize; i++)
        {
            bucket.add(getUniqueId());
        }
    }

    private void fillBucket19(Set<Long> bucket)
    {
        for (int i=0; i < bucketSize; i++)
        {
            bucket.add(getUniqueId19());
        }
    }
}
