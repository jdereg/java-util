package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Collection that contains itself, inside a typed array key (a List[], say), is a cycle as it is inside an
 * Object[] or a List. The typed-array path hashed each element with its own hashCode() before checking whether it was
 * a container, and a self-containing List's hashCode() never returns: put() threw StackOverflowError.
 */
class MultiKeyMapSelfContainingElementTest {

    @Test
    @SuppressWarnings("unchecked")
    void aSelfContainingListInATypedArrayIsACycle() {
        List<Object> self = new ArrayList<>();
        self.add("x");
        self.add(self);
        LinkedList<Object> other = new LinkedList<>();
        other.add(other);

        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new List[]{self}, "list[]");
        map.put(new Collection[]{null, other}, "collection[]");
        map.put(new ArrayList[]{(ArrayList<Object>) (Object) self, null}, "arraylist[]");
        assertEquals(3, map.size());
        assertEquals("list[]", map.get(new List[]{self}));
        assertEquals("list[]", map.get(new Object[]{self}));
        assertEquals("collection[]", map.get(new Object[]{null, other}));
        assertEquals("arraylist[]", map.get(new List[]{self, null}));
    }
}
