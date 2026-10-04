package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * An object with no conversion of its own converts to a Map of its fields: values as values, objects as Maps.
 */
class ConverterObjectToMapTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    enum Shade { RED, GREEN }

    static class Address {
        String city;

        Address(String city) {
            this.city = city;
        }
    }

    static class Person {
        String name = "Ann";
        Address home;
        Address work;
        BigDecimal salary = new BigDecimal("1234.50");
        Date born = new Date(0);
        LocalDate start = LocalDate.of(2024, 1, 15);
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Shade favorite = Shade.GREEN;
        char initial = 'A';
        int[] numbers = {1, 2};
    }

    static class Node {
        String name;
        Node parent;
        List<Node> children = new ArrayList<>();

        Node(String name) {
            this.name = name;
        }
    }

    static class Base {
        String id = "base";
    }

    static class Derived extends Base {
        String id = "derived";
    }

    /**
     * A field holding a value (BigDecimal, Date, LocalDate, UUID, an enum) keeps it. They were walked as objects:
     * {} on JDK 17+, where their fields cannot be read, and their internals ({scale=2}, {mostSigBits=...}) on JDK 8.
     */
    @Test
    void valuesStayValues() {
        Map<?, ?> map = converter.convert(new Person(), Map.class);
        assertEquals("Ann", map.get("name"));
        assertEquals(new BigDecimal("1234.50"), map.get("salary"));
        assertEquals(new Date(0), map.get("born"));
        assertEquals(LocalDate.of(2024, 1, 15), map.get("start"));
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), map.get("id"));
        assertEquals(Shade.GREEN, map.get("favorite"));
        assertEquals("A", map.get("initial"));
        assertEquals(Arrays.asList(1, 2), map.get("numbers"));
    }

    /**
     * An object reached twice converts once, and both places hold that Map. The second field to reach it got null:
     * a home and work Address that were the same object left work = null.
     */
    @Test
    void anObjectReachedTwiceIsInBothPlaces() {
        Person person = new Person();
        Address shared = new Address("Cincinnati");
        person.home = shared;
        person.work = shared;
        Map<?, ?> map = converter.convert(person, Map.class);
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("city", "Cincinnati");
        assertEquals(city, map.get("home"));
        assertSame(map.get("home"), map.get("work"));
    }

    /**
     * A cycle (an object that is its own ancestor) ends in null, through a collection as through a field. A cycle
     * through a List overflowed the stack: each element was converted afresh, with no record of its ancestors.
     */
    @Test
    void aCycleEndsInNull() {
        Node root = new Node("root");
        Node kid = new Node("kid");
        kid.parent = root;
        root.children.add(kid);
        Map<?, ?> map = converter.convert(root, Map.class);
        assertEquals("root", map.get("name"));
        List<?> children = (List<?>) map.get("children");
        Map<?, ?> kidMap = (Map<?, ?>) children.get(0);
        assertEquals("kid", kidMap.get("name"));
        assertNull(kidMap.get("parent"), "the cycle back to root");
        assertEquals(new ArrayList<>(), kidMap.get("children"));
    }

    /** A subclass field hides the superclass field of the same name, as in Java. The superclass's value won. */
    @Test
    void theSubclassFieldWins() {
        Map<?, ?> map = converter.convert(new Derived(), Map.class);
        assertEquals("derived", map.get("id"));
    }

    static class Holder {
        List<List<Address>> nested = new ArrayList<>();
        Map<String, Address> byName = new LinkedHashMap<>();
        Holder next;
    }

    /**
     * Containers inside containers convert element by element: a List of Lists of objects, a Map of objects. An
     * element that was itself a container was walked as an object (an ArrayList's internals, or {} on JDK 17+).
     */
    @Test
    void containersInsideContainersConvert() {
        Holder holder = new Holder();
        holder.nested.add(new ArrayList<>(Arrays.asList(new Address("Dayton"))));
        holder.byName.put("home", new Address("Akron"));
        Map<?, ?> map = converter.convert(holder, Map.class);
        Map<String, Object> dayton = new LinkedHashMap<>();
        dayton.put("city", "Dayton");
        assertEquals(Arrays.asList(Arrays.asList(dayton)), map.get("nested"));
        Map<String, Object> akron = new LinkedHashMap<>();
        akron.put("city", "Akron");
        Map<String, Object> byName = new LinkedHashMap<>();
        byName.put("home", akron);
        assertEquals(byName, map.get("byName"));
    }

    /** A chain 100,000 objects deep converts without overflowing the stack. */
    @Test
    void aDeepChainDoesNotOverflowTheStack() {
        Holder head = new Holder();
        Holder tail = head;
        for (int i = 0; i < 100_000; i++) {
            tail.next = new Holder();
            tail = tail.next;
        }
        Map<?, ?> map = converter.convert(head, Map.class);
        int depth = 0;
        for (Map<?, ?> m = map; m != null; m = (Map<?, ?>) m.get("next")) {
            depth++;
        }
        assertEquals(100_001, depth);
    }
}
