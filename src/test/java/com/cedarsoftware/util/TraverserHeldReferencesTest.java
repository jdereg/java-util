package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Traverser follows the references its Javadoc promises. A JDK class is a leaf, so an object held in an Optional, an
 * AtomicReference, an AtomicReferenceArray or a JDK Map.Entry outside a map was never reached; those holders are now
 * followed through their public methods, the same on every JDK. The compiler-made references were skipped too: an
 * inner object's outer object (this$0) and an anonymous or local class's captured variables (val$...).
 */
class TraverserHeldReferencesTest {

    static class Customer {
        String name = "c";
    }

    static class Order {
        Optional<Customer> viaOptional = Optional.of(new Customer());
        AtomicReference<Customer> viaAtomic = new AtomicReference<>(new Customer());
        AtomicReferenceArray<Customer> viaAtomicArray = new AtomicReferenceArray<>(new Customer[] {new Customer()});
        Map.Entry<String, Customer> viaEntry = new AbstractMap.SimpleEntry<>("k", new Customer());
        Map.Entry<Customer, String> viaImmutableKey = new AbstractMap.SimpleImmutableEntry<>(new Customer(), "v");
        List<Map.Entry<String, Customer>> entries = new ArrayList<>(Collections.singletonList(
                new AbstractMap.SimpleEntry<>("k", new Customer())));
        Optional<Customer> empty = Optional.empty();
    }

    /** A user subclass of AtomicReference: get() is final, so following it runs no user code. */
    static class Slot extends AtomicReference<Customer> {
        String label = "slot";

        Slot(Customer customer) {
            super(customer);
        }
    }

    class Inner {
        int x;
    }

    Customer held = new Customer();

    private static Set<Object> reached(Object root) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Traverser.traverse(root, visit -> seen.add(visit.getNode()), null);
        return seen;
    }

    @Test
    void objectsInJdkHoldersAreReached() {
        Order order = new Order();
        Set<Object> seen = reached(order);
        assertTrue(seen.contains(order.viaOptional.get()), "Optional");
        assertTrue(seen.contains(order.viaAtomic.get()), "AtomicReference");
        assertTrue(seen.contains(order.viaAtomicArray.get(0)), "AtomicReferenceArray");
        assertTrue(seen.contains(order.viaEntry.getValue()), "SimpleEntry value");
        assertTrue(seen.contains(order.viaImmutableKey.getKey()), "SimpleImmutableEntry key");
        assertTrue(seen.contains(order.entries.get(0).getValue()), "an entry in a List");

        Slot slot = new Slot(new Customer());
        assertTrue(reached(slot).contains(slot.get()), "AtomicReference subclass");
    }

    /** The holders are followed whatever else is around them, and a Customer is visited once. */
    @Test
    void eachObjectIsVisitedOnce() {
        Customer shared = new Customer();
        List<Object> visits = new ArrayList<>();
        Object[] root = {Optional.of(shared), new AtomicReference<>(shared), shared};
        Traverser.traverse(root, visit -> visits.add(visit.getNode()), null);
        assertTrue(visits.stream().filter(v -> v == shared).count() == 1, visits.toString());
    }

    @Test
    void anInnerObjectReachesItsOuterObject() {
        TraverserHeldReferencesTest outer = new TraverserHeldReferencesTest();
        Inner inner = outer.new Inner();
        Set<Object> seen = reached(inner);
        assertTrue(seen.contains(outer));
        assertTrue(seen.contains(outer.held));
    }

    @Test
    void anAnonymousOrLocalClassReachesWhatItCaptured() {
        Customer captured = new Customer();
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
                captured.name = "run";
            }
        };
        assertTrue(reached(anonymous).contains(captured), "anonymous class");

        Customer alsoCaptured = new Customer();
        class Local {
            String name() {
                return alsoCaptured.name;
            }
        }
        assertTrue(reached(new Local()).contains(alsoCaptured), "local class");
    }

    /** classesToSkip stops the walk at an outer object, as at any other. */
    @Test
    void classesToSkipStopsAtTheOuterObject() {
        TraverserHeldReferencesTest outer = new TraverserHeldReferencesTest();
        Inner inner = outer.new Inner();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Class<?>> skip = new java.util.HashSet<>(Arrays.asList(TraverserHeldReferencesTest.class));
        Traverser.traverse(inner, visit -> seen.add(visit.getNode()), skip);
        assertFalse(seen.contains(outer));
        assertFalse(seen.contains(outer.held));
    }
}
