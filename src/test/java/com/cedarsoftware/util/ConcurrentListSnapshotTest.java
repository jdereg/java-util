package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ConcurrentList promises point-in-time snapshots to readers: iteration does, and so must streams and
 * serialization.
 */
class ConcurrentListSnapshotTest {

    /**
     * A stream sees the list as it was when the stream was made, like iterator(). On JDK 9+ the List default read
     * the live list by index, so a list changing mid-stream skipped elements or threw IndexOutOfBoundsException.
     */
    @Test
    void aStreamIsASnapshot() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < 20; i++) {
            list.add(i);
        }
        List<Integer> expected = new ArrayList<>(list);
        boolean[] first = {true};
        List<Integer> streamed = list.stream().peek(x -> {
            if (first[0]) {
                first[0] = false;
                for (int i = 0; i < 10; i++) {
                    list.pollFirst();               // another thread draining the list
                }
            }
        }).collect(Collectors.toList());
        assertEquals(expected, streamed);
        assertEquals(10, list.size());
    }
}
