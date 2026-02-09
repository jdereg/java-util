package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CollectionConversions and CollectionHandling bugs.
 *
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
class CollectionConversionsTest {

    // ---- Bug #1: sizeOrDefault doesn't handle arrays, ArrayBlockingQueue fails for >16 elements ----

    @Test
    void arrayToCollection_arrayBlockingQueue_moreThan16Elements() {
        // Array with 20 elements - exceeds the hardcoded default of 16
        Integer[] source = new Integer[20];
        for (int i = 0; i < 20; i++) {
            source[i] = i;
        }

        @SuppressWarnings("unchecked")
        ArrayBlockingQueue<Object> result = (ArrayBlockingQueue<Object>)
                CollectionConversions.arrayToCollection(source, ArrayBlockingQueue.class);

        assertEquals(20, result.size());
        assertTrue(result.contains(0));
        assertTrue(result.contains(19));
    }

    @Test
    void arrayToCollection_arrayBlockingQueue_exactlyCapacity() {
        // Exactly 16 elements - should work even with the old default
        Integer[] source = new Integer[16];
        for (int i = 0; i < 16; i++) {
            source[i] = i;
        }

        @SuppressWarnings("unchecked")
        ArrayBlockingQueue<Object> result = (ArrayBlockingQueue<Object>)
                CollectionConversions.arrayToCollection(source, ArrayBlockingQueue.class);

        assertEquals(16, result.size());
    }

    @Test
    void arrayToCollection_arrayBlockingQueue_largeArray() {
        // Large array - 100 elements
        String[] source = new String[100];
        for (int i = 0; i < 100; i++) {
            source[i] = "item" + i;
        }

        @SuppressWarnings("unchecked")
        ArrayBlockingQueue<Object> result = (ArrayBlockingQueue<Object>)
                CollectionConversions.arrayToCollection(source, ArrayBlockingQueue.class);

        assertEquals(100, result.size());
    }

    @Test
    void collectionToCollection_arrayBlockingQueue_largeSource() {
        // Collection source with >16 elements - sizeOrDefault handles Collection correctly,
        // but verify ArrayBlockingQueue works end-to-end
        List<Integer> source = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            source.add(i);
        }

        Object result = CollectionConversions.collectionToCollection(source, ArrayBlockingQueue.class);
        assertInstanceOf(ArrayBlockingQueue.class, result);
        assertEquals(25, ((Collection<?>) result).size());
    }

    // ---- Bug #2: SynchronousQueue cannot hold elements ----

    @Test
    void arrayToCollection_synchronousQueue_throwsDescriptiveError() {
        String[] source = {"a", "b"};

        assertThrows(IllegalArgumentException.class, () ->
                CollectionConversions.arrayToCollection(source, SynchronousQueue.class));
    }

    // ---- Bug #3: DelayQueue requires Delayed elements ----

    @Test
    void arrayToCollection_delayQueue_throwsDescriptiveError() {
        String[] source = {"a", "b"};

        assertThrows(IllegalArgumentException.class, () ->
                CollectionConversions.arrayToCollection(source, DelayQueue.class));
    }
}
