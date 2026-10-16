package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GraphComparator issue #56 (the path of a difference, not only the name of its last field) and issue #46
 * (an element that is new in the target graph is reported as a list.setElement, not as an orphan).
 */
class GraphComparatorDeltaPathTest
{
    private interface HasId
    {
        long getId();
    }

    private static GraphComparator.ID idFetcher()
    {
        return obj -> obj instanceof HasId ? (Object) ((HasId) obj).getId() : null;
    }

    private static class Item implements HasId
    {
        long id;
        String name;
        String note;

        Item(long id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public long getId()
        {
            return id;
        }
    }

    private static class Customer implements HasId
    {
        long id;
        String name;
        Item favorite;

        Customer(long id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public long getId()
        {
            return id;
        }
    }

    private static class Order implements HasId
    {
        long id;
        String name;
        Customer customer;
        List<Item> items = new ArrayList<>();
        Item[] spares = new Item[0];
        Map<String, Item> byCode = new HashMap<>();
        Set<Item> bag = new LinkedHashSet<>();

        Order(long id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public long getId()
        {
            return id;
        }
    }

    private static Order order(String suffix)
    {
        Order o = new Order(1, "order" + suffix);
        o.customer = new Customer(10, "customer" + suffix);
        o.customer.favorite = new Item(20, "favorite" + suffix);
        o.items.add(new Item(30, "first" + suffix));
        o.items.add(new Item(31, "second" + suffix));
        o.spares = new Item[]{new Item(40, "spare" + suffix)};
        o.byCode.put("A", new Item(50, "mapped" + suffix));
        o.bag.add(new Item(60, "bagged" + suffix));
        return o;
    }

    private static GraphComparator.Delta find(List<GraphComparator.Delta> deltas, String path)
    {
        for (GraphComparator.Delta delta : deltas)
        {
            if (path.equals(delta.getPath()))
            {
                return delta;
            }
        }
        throw new AssertionError("no delta at path " + path + " in " + paths(deltas));
    }

    private static List<String> paths(List<GraphComparator.Delta> deltas)
    {
        List<String> paths = new ArrayList<>();
        for (GraphComparator.Delta delta : deltas)
        {
            paths.add(delta.getPath());
        }
        return paths;
    }

    @Test
    void sameLeafNameInDifferentBranchesHasDifferentPaths()
    {
        Order source = order("");
        Order target = order("");
        target.name = "renamed";
        target.customer.name = "renamedCustomer";
        target.customer.favorite.name = "renamedFavorite";
        target.items.get(1).name = "renamedSecond";
        target.spares[0].name = "renamedSpare";
        target.byCode.get("A").name = "renamedMapped";
        target.bag.iterator().next().name = "renamedBagged";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, idFetcher());

        // all seven differ in a field called "name"; the path says which
        for (GraphComparator.Delta delta : deltas)
        {
            assertEquals("name", delta.getFieldName(), delta.toString());
        }
        assertEquals(new LinkedHashSet<>(Arrays.asList("name", "customer.name", "customer.favorite.name",
                "items[1].name", "spares[0].name", "byCode[A].name", "bag[60].name")),
                new LinkedHashSet<>(paths(deltas)));
        assertSame(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, find(deltas, "customer.favorite.name").getCmd());
        assertTrue(find(deltas, "items[1].name").toString().contains("path='items[1].name'"));
    }

    @Test
    void collectionCommandsCarryThePathOfTheirCollection()
    {
        Order source = order("");
        Order target = order("");
        target.items.add(new Item(32, "third"));
        target.byCode.put("B", new Item(51, "put"));
        target.byCode.remove("A");
        target.bag.add(new Item(61, "added"));
        target.spares = new Item[]{target.spares[0], new Item(41, "spare2")};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, idFetcher());

        assertSame(GraphComparator.Delta.Command.LIST_RESIZE, find(deltas, "items").getCmd());
        assertSame(GraphComparator.Delta.Command.LIST_SET_ELEMENT, find(deltas, "items[2]").getCmd());
        assertSame(GraphComparator.Delta.Command.ARRAY_RESIZE, find(deltas, "spares").getCmd());
        assertSame(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT, find(deltas, "spares[1]").getCmd());
        assertSame(GraphComparator.Delta.Command.MAP_PUT, find(deltas, "byCode[B]").getCmd());
        assertSame(GraphComparator.Delta.Command.MAP_REMOVE, find(deltas, "byCode[A]").getCmd());
        assertSame(GraphComparator.Delta.Command.SET_ADD, find(deltas, "bag").getCmd());
    }

    @Test
    void theRootHasTheEmptyPathAndAnOrphanHasNone()
    {
        Order source = order("");
        Order target = order("");
        target.customer = null;     // the customer (and its favorite) leave the graph

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, idFetcher());

        assertSame(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, find(deltas, "customer").getCmd());
        int orphans = 0;
        for (GraphComparator.Delta delta : deltas)
        {
            if (delta.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN)
            {
                orphans++;
                assertEquals("", delta.getPath());
            }
        }
        assertEquals(2, orphans);
        assertEquals("", new GraphComparator.Delta(1L, "f", "ptr", null, null, null).getPath());
    }

    @Test
    void anElementNewInTheTargetIsAListSetElementCarryingIt()
    {
        // Issue #46. Ten items; the target is the source with one item taken out of the middle.
        Order withAll = new Order(1, "o");
        for (int i = 0; i < 10; i++)
        {
            withAll.items.add(new Item(100 + i, "item" + i));
        }
        Order withoutOne = new Order(1, "o");
        for (Item item : withAll.items)
        {
            if (item.id != 105)
            {
                withoutOne.items.add(new Item(item.id, item.name));
            }
        }

        // Taking the item out: it is an orphan.
        List<GraphComparator.Delta> removed = GraphComparator.compare(withAll, withoutOne, idFetcher());
        GraphComparator.Delta orphan = null;
        for (GraphComparator.Delta delta : removed)
        {
            if (delta.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN)
            {
                orphan = delta;
            }
        }
        assertNotNull(orphan);
        assertEquals(105L, orphan.getId());

        // Putting it back: no orphan (nothing left the graph). The item is the targetValue of a list.setElement at
        // its position, after a list.resize; the items after it are set-elements too, one position each.
        List<GraphComparator.Delta> added = GraphComparator.compare(withoutOne, withAll, idFetcher());
        GraphComparator.Delta resize = find(added, "items");
        assertSame(GraphComparator.Delta.Command.LIST_RESIZE, resize.getCmd());
        assertEquals(10, resize.getOptionalKey());
        GraphComparator.Delta atFive = find(added, "items[5]");
        assertSame(GraphComparator.Delta.Command.LIST_SET_ELEMENT, atFive.getCmd());
        assertEquals(105L, ((Item) atFive.getTargetValue()).id);
        for (GraphComparator.Delta delta : added)
        {
            assertTrue(delta.getCmd() != GraphComparator.Delta.Command.OBJECT_ORPHAN, delta.toString());
        }

        // And applying them turns the source into the target.
        GraphComparator.applyDelta(withoutOne, added, idFetcher(), GraphComparator.getJavaDeltaProcessor());
        List<Long> ids = new ArrayList<>();
        for (Item item : withoutOne.items)
        {
            ids.add(item.id);
        }
        assertEquals(Arrays.asList(100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L), ids);
    }
}
