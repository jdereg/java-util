package com.cedarsoftware.util;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the other GraphComparator tests leave out, found by measuring their line and branch coverage: the error
 * paths of applyDelta and the Java delta processor, a Collection that is neither a List nor a Set, a field that
 * changes type, an identity change, Delta and DeltaError themselves, and a compare/apply/compare round trip over a
 * graph that has every kind of container in it.
 */
class GraphComparatorGapsTest
{
    private interface HasId
    {
        long getId();
    }

    private static GraphComparator.ID idFetcher()
    {
        return obj -> obj instanceof HasId ? (Object) ((HasId) obj).getId() : null;
    }

    private static class Node implements HasId
    {
        long id;
        String label;
        Node next;
        Object any;
        List<Node> list = new ArrayList<>();
        Map<String, Node> map = new LinkedHashMap<>();
        Set<Node> set = new LinkedHashSet<>();
        Node[] array = new Node[0];
        int[] nums = new int[0];

        Node(long id, String label)
        {
            this.id = id;
            this.label = label;
        }

        public long getId()
        {
            return id;
        }
    }

    private static List<GraphComparator.Delta> compare(Object source, Object target)
    {
        return GraphComparator.compare(source, target, idFetcher());
    }

    private static GraphComparator.Delta only(List<GraphComparator.Delta> deltas)
    {
        assertEquals(1, deltas.size(), deltas.toString());
        return deltas.get(0);
    }

    private static GraphComparator.Delta delta(GraphComparator.Delta.Command cmd, Object id, String field, Object key,
                                               Object target)
    {
        GraphComparator.Delta delta = new GraphComparator.Delta(id, field, "ptr", null, target, key);
        delta.setCmd(cmd);
        return delta;
    }

    private static List<GraphComparator.DeltaError> apply(Node source, GraphComparator.Delta... deltas)
    {
        return GraphComparator.applyDelta(source, Arrays.asList(deltas), idFetcher(),
                GraphComparator.getJavaDeltaProcessor());
    }

    // ---------------------------------------------------------------------------------------------- compare()

    @Test
    void theSameInstanceHasNoDeltas()
    {
        Node node = new Node(1, "a");
        node.next = new Node(2, "b");
        assertTrue(compare(node, node).isEmpty());
    }

    @Test
    void aCollectionThatIsNeitherAListNorASetIsRefused()
    {
        Node source = new Node(1, "a");
        Node target = new Node(1, "a");
        source.any = new ArrayDeque<>(Arrays.asList("x"));
        target.any = new ArrayDeque<>(Arrays.asList("y"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> compare(source, target));
        assertTrue(e.getMessage().contains("custom Collection that does not extend List or Set"), e.getMessage());
        assertTrue(e.getMessage().contains("ArrayDeque"), e.getMessage());
    }

    @Test
    void aFieldThatChangesTypeIsReportedAsATypeChange()
    {
        Node source = new Node(1, "a");
        Node target = new Node(1, "a");
        source.any = "text";
        target.any = 5;

        GraphComparator.Delta delta = only(compare(source, target));
        assertSame(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED, delta.getCmd());
        assertEquals("any", delta.getPath());
        assertEquals(5, delta.getTargetValue());
    }

    @Test
    void aFieldThatPointsToAnotherIdentityIsAssignedAndTheOldOneIsOrphaned()
    {
        Node source = new Node(1, "a");
        Node target = new Node(1, "a");
        source.next = new Node(2, "old");
        target.next = new Node(3, "new");

        List<GraphComparator.Delta> deltas = compare(source, target);
        assertEquals(2, deltas.size(), deltas.toString());
        GraphComparator.Delta assign = deltas.get(0);
        assertSame(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, assign.getCmd());
        assertEquals("next", assign.getPath());
        assertSame(target.next, assign.getTargetValue());
        GraphComparator.Delta orphan = deltas.get(1);
        assertSame(GraphComparator.Delta.Command.OBJECT_ORPHAN, orphan.getCmd());
        assertEquals(2L, orphan.getId());
    }

    @Test
    void anObjectHeldTwiceIsReportedOnceAtTheFirstRouteTheComparisonTakesToIt()
    {
        // the same Node under two fields, renamed in the target. One delta fixes it wherever it is held, and applying
        // two would be applying the same assignment twice, so there is one; its path is one of the two routes.
        Node source = new Node(1, "a");
        Node shared = new Node(2, "shared");
        source.next = shared;
        source.list.add(shared);
        Node target = new Node(1, "a");
        Node sharedTarget = new Node(2, "renamed");
        target.next = sharedTarget;
        target.list.add(sharedTarget);

        GraphComparator.Delta delta = only(compare(source, target));
        assertTrue("next.label".equals(delta.getPath()) || "list[0].label".equals(delta.getPath()), delta.getPath());
        assertEquals("renamed", delta.getTargetValue());
    }

    @Test
    void aCycleTerminatesAndIsReportedOnceAtOneRouteToTheObject()
    {
        Node source = new Node(1, "a");
        source.next = source;
        Node target = new Node(1, "b");
        target.next = target;

        GraphComparator.Delta delta = only(compare(source, target));
        // the object is its own next, so "label" and "next.label" (and "next.next.label"...) are all routes to it
        assertTrue(delta.getPath().matches("(next\\.)*label"), delta.getPath());
        assertEquals("b", delta.getTargetValue());
    }

    // ---------------------------------------------------------------------------------------------- round trip

    private static Node graph(boolean changed)
    {
        Node root = new Node(1, changed ? "root2" : "root");
        root.next = new Node(2, "next");
        root.next.next = changed ? null : new Node(3, "tail");
        root.any = changed ? "now text" : "text";
        root.list.add(new Node(10, "l0"));
        root.list.add(new Node(11, changed ? "l1-renamed" : "l1"));
        if (changed)
        {
            root.list.add(new Node(12, "l2-added"));
        }
        root.map.put("keep", new Node(20, changed ? "kept-renamed" : "kept"));
        if (!changed)
        {
            root.map.put("gone", new Node(21, "gone"));
        }
        else
        {
            root.map.put("fresh", new Node(22, "fresh"));
        }
        root.set.add(new Node(30, "s0"));
        if (changed)
        {
            root.set.add(new Node(31, "s1-added"));
        }
        root.array = changed ? new Node[]{new Node(40, "a0"), new Node(41, "a1-added")}
                : new Node[]{new Node(40, "a0")};
        root.nums = changed ? new int[]{1, 2, 9} : new int[]{1, 2};
        return root;
    }

    @Test
    void applyingTheDeltasMakesTheSourceTheTarget()
    {
        Node source = graph(false);
        Node target = graph(true);

        List<GraphComparator.Delta> deltas = compare(source, target);
        assertFalse(deltas.isEmpty());
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher(),
                GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty(), errors.toString());

        List<GraphComparator.Delta> after = compare(source, target);
        assertTrue(after.isEmpty(), after.toString());
        assertEquals("root2", source.label);
        assertEquals(3, source.list.size());
        assertEquals(3, source.nums.length);
        assertEquals(2, source.array.length);
        assertTrue(source.map.containsKey("fresh") && !source.map.containsKey("gone"));
    }

    // ---------------------------------------------------------------------------------------------- applyDelta

    @Test
    void aDeltaForAnObjectThatIsNotThereIsAnError()
    {
        List<GraphComparator.DeltaError> errors = apply(new Node(1, "a"),
                delta(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, 99L, "label", null, "x"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("source object not found, obj id: 99"), errors.get(0).getError());
    }

    @Test
    void aDeltaForAFieldThatIsNotThereIsAnError()
    {
        List<GraphComparator.DeltaError> errors = apply(new Node(1, "a"),
                delta(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, 1L, "nope", null, "x"));
        assertTrue(errors.get(0).getError().contains("field name missing: nope"), errors.get(0).getError());
    }

    @Test
    void aDeltaWithNoCommandIsAnError()
    {
        List<GraphComparator.DeltaError> errors = apply(new Node(1, "a"),
                delta(null, 1L, "label", null, "x"));
        assertEquals(1, errors.size());
    }

    @Test
    void anErrorKeepsThePathOfTheDeltaThatFailed()
    {
        GraphComparator.Delta bad = delta(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, 99L, "label", null, "x");
        bad.setPath("customer.label");
        List<GraphComparator.DeltaError> errors = apply(new Node(1, "a"), bad);
        assertEquals("customer.label", errors.get(0).getPath());
        assertTrue(errors.get(0).toString().contains("customer.label"), errors.get(0).toString());
    }

    @Test
    void anErrorMessageIncludesTheCauseOfTheFailure()
    {
        Node source = new Node(1, "a");
        // an int field assigned a String: the reflective set fails, and the message says why
        List<GraphComparator.DeltaError> errors = apply(source,
                delta(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, 1L, "id", null, "not a long"));
        assertTrue(errors.get(0).getError().contains("unable to set object field: id"), errors.get(0).getError());
        assertTrue(errors.get(0).getError().contains("caused by"), errors.get(0).getError());
    }

    @Test
    void aResizeAcceptsAnyWholeNumberTypeThatFitsAndRefusesTheRest()
    {
        for (Object size : new Object[]{3, 3L, (short) 3, (byte) 3, BigInteger.valueOf(3)})
        {
            Node source = new Node(1, "a");
            List<GraphComparator.DeltaError> errors = apply(source,
                    delta(GraphComparator.Delta.Command.ARRAY_RESIZE, 1L, "nums", size, null));
            assertTrue(errors.isEmpty(), size.getClass() + ": " + errors);
            assertEquals(3, source.nums.length, size.getClass().toString());
        }
        for (Object size : new Object[]{-1, 1L << 40, BigInteger.ONE.shiftLeft(40), "3", 3.0d, null})
        {
            Node source = new Node(1, "a");
            List<GraphComparator.DeltaError> errors = apply(source,
                    delta(GraphComparator.Delta.Command.ARRAY_RESIZE, 1L, "nums", size, null));
            assertEquals(1, errors.size(), String.valueOf(size));
            assertTrue(errors.get(0).getError().contains("the optionalKey must be a integer value 0 or greater"),
                    errors.get(0).getError());
            assertEquals(0, source.nums.length, "a refused resize changes nothing");
        }
    }

    @Test
    void anArrayCommandOnAFieldThatIsNotAnArrayIsAnError()
    {
        for (GraphComparator.Delta.Command cmd : new GraphComparator.Delta.Command[]{
                GraphComparator.Delta.Command.ARRAY_RESIZE, GraphComparator.Delta.Command.ARRAY_SET_ELEMENT})
        {
            List<GraphComparator.DeltaError> errors = apply(new Node(1, "a"), delta(cmd, 1L, "label", 0, "x"));
            assertTrue(errors.get(0).getError().contains("is not an Array [] type"), errors.get(0).getError());
        }
    }

    @Test
    void aCommandOnAFieldThatHoldsNullIsAnError()
    {
        Node source = new Node(1, "a");
        source.list = null;
        List<GraphComparator.DeltaError> errors = apply(source,
                delta(GraphComparator.Delta.Command.LIST_SET_ELEMENT, 1L, "list", 0, "x"));
        assertTrue(errors.get(0).getError().contains("null value at field: list"), errors.get(0).getError());
    }

    @Test
    void aCommandOnAFieldOfTheWrongTypeIsAnError()
    {
        // the field is declared Object and holds a Map; a list command on it must be refused
        Node source = new Node(1, "a");
        source.any = new HashMap<>();
        List<GraphComparator.DeltaError> errors = apply(source,
                delta(GraphComparator.Delta.Command.LIST_SET_ELEMENT, 1L, "any", 0, "x"));
        assertTrue(errors.get(0).getError().contains("is not of type"), errors.get(0).getError());
    }

    @Test
    void anErrorMessageShowsAnArrayKeyElementByElement()
    {
        Node source = new Node(1, "a");
        source.list = null;
        List<GraphComparator.DeltaError> errors = apply(source,
                delta(GraphComparator.Delta.Command.LIST_SET_ELEMENT, 1L, "list", new Object[]{1, null, "x"}, "x"));
        assertTrue(errors.get(0).getError().contains("optionalKey: [1,null,x]"), errors.get(0).getError());
        errors = apply(source, delta(GraphComparator.Delta.Command.LIST_SET_ELEMENT, 1L, "list", null, "x"));
        assertTrue(errors.get(0).getError().contains("optionalKey: null"), errors.get(0).getError());
    }

    @Test
    void aTypeChangeIsReportedAsAnErrorWhenAppliedAndChangesNothing()
    {
        // the Java delta processor leaves a change of type to the caller: it applies nothing and says so
        Node source = new Node(1, "a");
        Node target = new Node(1, "a");
        source.any = "text";
        target.any = 5;
        List<GraphComparator.Delta> deltas = compare(source, target);
        assertSame(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED, only(deltas).getCmd());

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher(),
                GraphComparator.getJavaDeltaProcessor());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("OBJECT_FIELD_TYPE_CHANGED failed, field: any, obj id: 1"),
                errors.get(0).getError());
        assertEquals("any", errors.get(0).getPath());
        assertEquals("text", source.any);
    }

    @Test
    void anOrphanCommandChangesNothingAndIsNotAnError()
    {
        Node source = new Node(1, "a");
        GraphComparator.Delta orphan = delta(GraphComparator.Delta.Command.OBJECT_ORPHAN, 1L, null, null, null);
        assertTrue(apply(source, orphan).isEmpty());
        assertEquals("a", source.label);
    }

    // ---------------------------------------------------------------------------------------------- Delta

    @Test
    void aDeltaIsEqualToAnotherAtTheSameSourcePointer()
    {
        GraphComparator.Delta a = new GraphComparator.Delta(1L, "f", "ptr", "x", "y", null);
        GraphComparator.Delta b = new GraphComparator.Delta(2L, "g", "ptr", "p", "q", 7);
        GraphComparator.Delta c = new GraphComparator.Delta(1L, "f", "other", "x", "y", null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "ptr");
        assertEquals(a, a);
    }

    @Test
    void thePathCanBeSetAndAnUnsetPathIsEmpty()
    {
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "f", "ptr", null, null, null);
        assertEquals("", delta.getPath());
        delta.setPath("a.b[2]");
        assertEquals("a.b[2]", delta.getPath());
        delta.setPath(null);
        assertEquals("", delta.getPath(), "a path read back as null (an older serialized Delta) is the empty path");
    }
}
