package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Random operations on 36 CompactSet configurations -- unordered, insertion, sorted and reverse; case-sensitive and
 * case-insensitive; compactSize 2, 3, 5 and 50; the legacy subclasses -- each checked after every step against a
 * HashSet, LinkedHashSet, TreeSet, case-insensitive TreeSet or CaseInsensitiveSet: size, contents and order with each
 * element's spelling, contains, equals both ways, hashCode, toArray. The operations include iterator removal,
 * removeIf, the bulk operations, and the bulk operations given the set itself. The CompactSet counterpart of
 * CompactMapDifferentialTest; it found removeAll(itself) leaving elements behind or throwing.
 */
class CompactSetDifferentialTest {

    private static final String[] WORDS = {"a", "A", "b", "B", "id", "ID", "Id", "key", "KEY", "c", "d", "e", "f", "g",
            "h", "x", "y", "z"};

    private static final class Config {
        final String name;
        final Supplier<Set<Object>> make;
        final Supplier<Set<Object>> reference;
        final boolean ordered;
        final boolean caseInsensitive;

        Config(String name, Supplier<Set<Object>> make, Supplier<Set<Object>> reference, boolean ordered,
               boolean caseInsensitive) {
            this.name = name;
            this.make = make;
            this.reference = reference;
            this.ordered = ordered;
            this.caseInsensitive = caseInsensitive;
        }
    }

    private static Set<Object> caseInsensitiveTree(boolean reverse) {
        Comparator<Object> order = (a, b) -> String.CASE_INSENSITIVE_ORDER.compare((String) a, (String) b);
        return new TreeSet<>(reverse ? order.reversed() : order);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Config> configs() {
        List<Config> list = new ArrayList<>();
        for (int size : new int[]{2, 3, 5, 50}) {
            list.add(new Config("unordered/cs/" + size, () -> (Set) CompactSet.builder().compactSize(size).build(),
                    HashSet::new, false, false));
            list.add(new Config("insertion/cs/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).insertionOrder().build(),
                    LinkedHashSet::new, true, false));
            list.add(new Config("sorted/cs/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).sortedOrder().build(),
                    TreeSet::new, true, false));
            list.add(new Config("reverse/cs/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).reverseOrder().build(),
                    () -> new TreeSet<>(Collections.reverseOrder()), true, false));
            list.add(new Config("unordered/ci/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).caseSensitive(false).build(),
                    CaseInsensitiveSet::new, false, true));
            list.add(new Config("insertion/ci/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).caseSensitive(false).insertionOrder().build(),
                    CaseInsensitiveSet::new, true, true));
            list.add(new Config("sorted/ci/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).caseSensitive(false).sortedOrder().build(),
                    () -> caseInsensitiveTree(false), true, true));
            list.add(new Config("reverse/ci/" + size,
                    () -> (Set) CompactSet.builder().compactSize(size).caseSensitive(false).reverseOrder().build(),
                    () -> caseInsensitiveTree(true), true, true));
        }
        list.add(new Config("CompactLinkedSet", () -> (Set) new CompactLinkedSet<>(), LinkedHashSet::new, true, false));
        list.add(new Config("CompactCIHashSet", () -> (Set) new CompactCIHashSet<>(), CaseInsensitiveSet::new, false,
                true));
        list.add(new Config("CompactCILinkedSet", () -> (Set) new CompactCILinkedSet<>(), CaseInsensitiveSet::new, true,
                true));
        list.add(new Config("CompactSet()", () -> (Set) new CompactSet<>(), HashSet::new, false, false));
        return list;
    }

    @Test
    void everyConfigurationAnswersAsItsReferenceSet() {
        List<String> failures = new ArrayList<>();
        long steps = 0;
        for (Config config : configs()) {
            for (long seed = 1; seed <= 10; seed++) {
                Random random = new Random(seed * 131 + config.name.hashCode());
                String failure = run(config, config.make.get(), config.reference.get(), random, 200);
                steps += 200;
                if (failure != null && failures.size() < 10) {
                    failures.add(config.name + " seed " + seed + ": " + failure);
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.toString());
        assertTrue(steps >= 70_000, "steps " + steps);
    }

    private static String run(Config config, Set<Object> set, Set<Object> reference, Random random, int operations) {
        List<String> log = new ArrayList<>();
        for (int i = 0; i < operations; i++) {
            String operation;
            try {
                operation = step(config, set, reference, random);
            } catch (RuntimeException e) {
                return "step " + i + " threw " + e + " after " + recent(log);
            }
            log.add(operation);
            String problem = check(config, set, reference);
            if (problem != null) {
                return "step " + i + ": " + problem + " after " + recent(log);
            }
        }
        return null;
    }

    private static List<String> recent(List<String> log) {
        return log.subList(Math.max(0, log.size() - 5), log.size());
    }

    private static List<Object> bag(Random random, int most) {
        List<Object> bag = new ArrayList<>();
        for (int k = random.nextInt(most); k > 0; k--) {
            bag.add(WORDS[random.nextInt(WORDS.length)]);
        }
        return bag;
    }

    private static void same(String what, boolean actual, boolean expected) {
        if (actual != expected) {
            throw new IllegalStateException(what + " returned " + actual + ", expected " + expected);
        }
    }

    private static String step(Config config, Set<Object> set, Set<Object> reference, Random random) {
        String word = WORDS[random.nextInt(WORDS.length)];
        switch (random.nextInt(14)) {
            case 0:
            case 1:
            case 2:
                same("add " + word, set.add(word), reference.add(word));
                return "add " + word;
            case 3:
            case 4:
                same("remove " + word, set.remove(word), reference.remove(word));
                return "remove " + word;
            case 5: {
                Iterator<Object> it = set.iterator();
                if (!it.hasNext()) {
                    return "iterator remove (empty)";
                }
                Object first = it.next();
                it.remove();
                reference.remove(first);
                return "iterator remove " + first;
            }
            case 6: {
                List<Object> bag = bag(random, 4);
                same("addAll " + bag, set.addAll(bag), reference.addAll(bag));
                return "addAll " + bag;
            }
            case 7: {
                List<Object> bag = bag(random, 4);
                set.removeAll(bag);
                for (Object o : bag) {
                    reference.remove(o);
                }
                return "removeAll " + bag;
            }
            case 8: {
                List<Object> bag = bag(random, 6);
                set.retainAll(bag);
                Set<Object> keep = config.reference.get();
                keep.addAll(bag);
                reference.removeIf(e -> !keep.contains(e));
                return "retainAll " + bag;
            }
            case 9:
                set.removeAll(set);
                reference.clear();
                return "removeAll(itself)";
            case 10:
                same("retainAll(itself)", set.retainAll(set), false);
                return "retainAll(itself)";
            case 11:
                same("addAll(itself)", set.addAll(set), false);
                return "addAll(itself)";
            case 12: {
                List<Object> bag = Arrays.asList(word, WORDS[random.nextInt(WORDS.length)]);
                same("containsAll " + bag, set.containsAll(bag), reference.containsAll(bag));
                return "containsAll " + bag;
            }
            default: {
                String target = word;
                boolean caseInsensitive = config.caseInsensitive;
                java.util.function.Predicate<Object> matches =
                        e -> caseInsensitive ? ((String) e).equalsIgnoreCase(target) : e.equals(target);
                same("removeIf " + word, set.removeIf(matches), reference.removeIf(matches));
                return "removeIf " + word;
            }
        }
    }

    private static String check(Config config, Set<Object> set, Set<Object> reference) {
        if (set.size() != reference.size()) {
            return "size " + set.size() + " vs " + reference.size();
        }
        if (config.ordered) {
            if (!new ArrayList<>(set).equals(new ArrayList<>(reference))) {
                return "order or spelling " + set + " vs " + reference;
            }
        } else if (!new HashSet<>(set).equals(new HashSet<>(reference))) {
            return "contents " + set + " vs " + reference;
        }
        for (String word : WORDS) {
            if (set.contains(word) != reference.contains(word)) {
                return "contains(" + word + ") " + set.contains(word) + " vs " + reference.contains(word);
            }
        }
        if (!set.equals(reference)) {
            return "equals(reference) is false: " + set + " vs " + reference;
        }
        if (!config.caseInsensitive) {
            if (!reference.equals(set)) {
                return "reference.equals(set) is false";
            }
            if (set.hashCode() != reference.hashCode()) {
                return "hashCode " + set.hashCode() + " vs " + reference.hashCode();
            }
        } else if (reference instanceof CaseInsensitiveSet && set.hashCode() != reference.hashCode()) {
            return "hashCode " + set.hashCode() + " vs CaseInsensitiveSet " + reference.hashCode();
        }
        if (!Arrays.asList(set.toArray()).equals(new ArrayList<>(set))) {
            return "toArray differs from iteration";
        }
        return null;
    }
}
