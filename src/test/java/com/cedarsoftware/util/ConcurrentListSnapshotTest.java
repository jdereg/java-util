package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Serializing while another thread changes the list writes a list it really held. Default serialization
     * copied the bucket map, head, tail and size without the lock: the review's probe deserialized nulls never
     * added, out-of-order elements, or threw NullPointerException.
     */
    @Test
    @SuppressWarnings("unchecked")
    void serializingWhileAnotherThreadWritesCapturesAListItHeld() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < 2000; i++) {
            list.add(i);
        }
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = new Thread(() -> {             // a queue: append the next number, drop the oldest
            int next = 2000;
            while (!stop.get()) {
                list.add(next++);
                list.pollFirst();
            }
        });
        writer.start();
        int bad = 0;
        int copies = 0;
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(400);
        try {
            while (System.nanoTime() < end) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                    out.writeObject(list);
                }
                List<Integer> copy;
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    copy = (List<Integer>) in.readObject();
                }
                copies++;
                List<Integer> elements = new ArrayList<>(copy);
                for (int i = 0; i < elements.size(); i++) {
                    if (elements.get(i) == null || i > 0 && elements.get(i) != elements.get(i - 1) + 1) {
                        bad++;
                        break;
                    }
                }
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        assertTrue(copies > 0);
        assertEquals(0, bad, "of " + copies + " copies, " + bad + " were not a run of consecutive numbers");
    }
}
