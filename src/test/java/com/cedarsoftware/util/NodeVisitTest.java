package com.cedarsoftware.util;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link Traverser.NodeVisit} basic getters.
 */
public class NodeVisitTest {

    @Test
    void testGetNode() {
        Object node = new Object();
        Traverser.NodeVisit visit = new Traverser.NodeVisit(node, Collections.emptyMap());
        assertSame(node, visit.getNode());
    }

    @Test
    void testGetNodeClass() {
        String node = "test";
        Traverser.NodeVisit visit = new Traverser.NodeVisit(node, Collections.emptyMap());
        assertEquals(String.class, visit.getNodeClass());
    }
}
