package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Differential test: random operations on every kind of CompactMap -- unordered, insertion, sorted and reverse,
 * case-sensitive and case-insensitive, compactSize 2, 3, 5 and 50 (so that the storage states change constantly),
 * and the legacy subclasses -- each checked after every step against a reference map: a LinkedHashMap, a TreeMap with
 * the same comparator, or a CaseInsensitiveMap. Checked: contents with each key's exact spelling, values, order where
 * the configuration promises one, size, equals both ways, hashCode, and that the storage state matches the size.
 * The operations cover put/remove/get, putAll, removal through each view's iterator, setValue (and removal) through
 * the entry iterator, the views' bulk operations, the Map default methods, and copies. This harness found the
 * putAll / iterator state bugs fixed in 4.113.0; it guards any change meant to keep the answers.
 */
class CompactMapDifferentialTest {

    static final Object[] KEYS = {"a", "A", "b", "B", "id", "ID", "Id", "name", "Name", "NAME", "k0", "k1", "k2", "k3",
            "k4", "k5", "k6", "k7", "k8", "k9", 0, 1, 2, 3, 1L, null};
    interface Config { String name(); CompactMap<Object, Object> make(); Map<Object, Object> reference(); boolean ordered(); boolean ci(); int compactSize(); }

    static Config cfg(String name, Supplier<CompactMap<Object, Object>> make, Supplier<Map<Object, Object>> ref, boolean ordered, boolean ci, int cs) {
        return new Config() {
            public String name() { return name; }
            public CompactMap<Object, Object> make() { return make.get(); }
            public Map<Object, Object> reference() { return ref.get(); }
            public boolean ordered() { return ordered; }
            public boolean ci() { return ci; }
            public int compactSize() { return cs; }
        };
    }

    static List<Config> configs() {
        List<Config> list = new ArrayList<>();
        for (int cs : new int[]{2, 3, 5, 50}) {
            list.add(cfg("unordered cs" + cs, () -> CompactMap.builder().compactSize(cs).build(), LinkedHashMap::new, false, false, cs));
            list.add(cfg("insertion cs" + cs, () -> CompactMap.builder().compactSize(cs).insertionOrder().build(), LinkedHashMap::new, true, false, cs));
            list.add(cfg("sorted cs" + cs, () -> CompactMap.builder().compactSize(cs).sortedOrder().build(),
                    () -> new TreeMap<>(CompactMap.CompactMapComparator.get(false, false)), true, false, cs));
            list.add(cfg("reverse cs" + cs, () -> CompactMap.builder().compactSize(cs).reverseOrder().build(),
                    () -> new TreeMap<>(CompactMap.CompactMapComparator.get(false, true)), true, false, cs));
            list.add(cfg("CI unordered cs" + cs, () -> CompactMap.builder().compactSize(cs).caseSensitive(false).build(),
                    () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()), false, true, cs));
            list.add(cfg("CI insertion cs" + cs, () -> CompactMap.builder().compactSize(cs).caseSensitive(false).insertionOrder().build(),
                    () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()), true, true, cs));
            list.add(cfg("CI sorted cs" + cs, () -> CompactMap.builder().compactSize(cs).caseSensitive(false).sortedOrder().build(),
                    () -> new TreeMap<>(CompactMap.CompactMapComparator.get(true, false)), true, true, cs));
            list.add(cfg("CI reverse cs" + cs, () -> CompactMap.builder().compactSize(cs).caseSensitive(false).reverseOrder().build(),
                    () -> new TreeMap<>(CompactMap.CompactMapComparator.get(true, true)), true, true, cs));
        }
        list.add(cfg("legacy CompactMap", () -> new CompactMap<>(), LinkedHashMap::new, false, false, 50));
        list.add(cfg("legacy CompactLinkedMap", () -> new CompactLinkedMap<>(), LinkedHashMap::new, true, false, 50));
        list.add(cfg("legacy CompactCIHashMap", () -> new CompactCIHashMap<>(),
                () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()), false, true, 50));
        list.add(cfg("legacy CompactCILinkedMap", () -> new CompactCILinkedMap<>(),
                () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()), true, true, 50));
        return list;
    }

    static Object key(Random r) { return KEYS[r.nextInt(KEYS.length)]; }
    static Object value(Random r) { return r.nextInt(8) == 0 ? null : r.nextInt(20); }

    static String step(Random r, CompactMap<Object, Object> m, Map<Object, Object> ref, Config c) {
        int pick = r.nextInt(100);
        if (pick < 30) {
            Object k = key(r), v = value(r);
            Object a = m.put(k, v), b = ref.put(k, v);
            if (!Objects.equals(a, b)) throw new IllegalStateException("put(" + k + ") returned " + a + " vs " + b);
            return "put(" + k + "," + v + ")";
        } else if (pick < 45) {
            Object k = key(r);
            Object a = m.remove(k), b = ref.remove(k);
            if (!Objects.equals(a, b)) throw new IllegalStateException("remove(" + k + ") returned " + a + " vs " + b);
            return "remove(" + k + ")";
        } else if (pick < 52) {
            Object k = key(r);
            if (!Objects.equals(m.get(k), ref.get(k)) || m.containsKey(k) != ref.containsKey(k)) {
                throw new IllegalStateException("get/containsKey(" + k + ") " + m.get(k) + "/" + m.containsKey(k) + " vs " + ref.get(k) + "/" + ref.containsKey(k));
            }
            Object v = value(r);
            if (m.containsValue(v) != ref.containsValue(v)) throw new IllegalStateException("containsValue(" + v + ")");
            return "get(" + k + ")";
        } else if (pick < 57) {
            Map<Object, Object> src = new LinkedHashMap<>();
            int n = r.nextInt(6);
            for (int i = 0; i < n; i++) src.put(key(r), value(r));
            m.putAll(src); ref.putAll(src);
            return "putAll(" + src + ")";
        } else if (pick < 62) {   // iterator removal through one of the three views
            int view = r.nextInt(3);
            int every = 1 + r.nextInt(3);
            Iterator<?> it = view == 0 ? m.keySet().iterator() : view == 1 ? m.values().iterator() : m.entrySet().iterator();
            List<Object> removedKeys = new ArrayList<>();
            int i = 0;
            List<Object> keysInOrder = new ArrayList<>(m.keySet());
            while (it.hasNext()) {
                it.next();
                if (i % every == 0) { it.remove(); removedKeys.add(keysInOrder.get(i)); }
                i++;
            }
            for (Object k : removedKeys) ref.remove(k);
            return "iterRemove(view" + view + ",every" + every + ")";
        } else if (pick < 67) {   // setValue through the entry iterator, then possibly remove
            Iterator<Map.Entry<Object, Object>> it = m.entrySet().iterator();
            StringBuilder sb = new StringBuilder("setValue[");
            while (it.hasNext()) {
                Map.Entry<Object, Object> e = it.next();
                if (r.nextBoolean()) {
                    Object k = e.getKey();          // read before any removal: a TreeMap reuses entry objects
                    Object v = value(r);
                    Object old = e.setValue(v);
                    Object refOld = ref.put(k, v);
                    if (!Objects.equals(old, refOld)) throw new IllegalStateException("setValue returned " + old + " vs " + refOld);
                    sb.append(k).append('=').append(v).append(' ');
                    if (r.nextInt(3) == 0) { it.remove(); ref.remove(k); sb.append("(removed) "); }
                }
            }
            return sb.append(']').toString();
        } else if (pick < 72) {   // view bulk operations
            List<Object> some = new ArrayList<>();
            int n = r.nextInt(5);
            for (int i = 0; i < n; i++) some.add(key(r));
            // The reference applies these key by key: a CaseInsensitiveMap's views inherit the JDK's AbstractSet
            // bulk methods, which use the argument's (case-sensitive) contains() depending on the sizes.
            if (r.nextBoolean()) {
                int before = ref.size();
                boolean a = m.keySet().removeAll(some);
                for (Object k : some) ref.remove(k);
                boolean b = ref.size() != before;
                if (a != b) throw new IllegalStateException("keySet.removeAll returned " + a + " vs " + b);
                return "keySet.removeAll(" + some + ")";
            } else {
                Map<Object, Object> keep = c.ci() ? new CaseInsensitiveMap<>() : new HashMap<>();
                for (Object k : some) keep.put(k, Boolean.TRUE);
                int before = ref.size();
                boolean a = m.keySet().retainAll(some);
                ref.keySet().removeIf(k -> !keep.containsKey(k));
                boolean b = ref.size() != before;
                if (a != b) throw new IllegalStateException("keySet.retainAll returned " + a + " vs " + b);
                return "keySet.retainAll(" + some + ")";
            }
        } else if (pick < 76) {   // entrySet remove / retainAll with entries built here
            Object k = key(r), v = value(r);
            Map.Entry<Object, Object> e = new AbstractMap.SimpleEntry<>(k, v);
            if (r.nextBoolean()) {
                boolean a = m.entrySet().remove(e), b = ref.entrySet().remove(e);
                if (a != b) throw new IllegalStateException("entrySet.remove(" + e + ") returned " + a + " vs " + b);
                return "entrySet.remove(" + e + ")";
            }
            List<Map.Entry<Object, Object>> keep = new ArrayList<>();
            for (Map.Entry<Object, Object> x : ref.entrySet()) if (r.nextBoolean()) keep.add(new AbstractMap.SimpleEntry<>(x.getKey(), x.getValue()));
            boolean a = m.entrySet().retainAll(keep), b = ref.entrySet().retainAll(keep);
            if (a != b) throw new IllegalStateException("entrySet.retainAll returned " + a + " vs " + b);
            return "entrySet.retainAll(" + keep + ")";
        } else if (pick < 90) {   // Map default methods
            Object k = key(r), v = value(r);
            switch (r.nextInt(8)) {
                case 0: { Object a = m.putIfAbsent(k, v), b = ref.putIfAbsent(k, v); same("putIfAbsent", a, b); return "putIfAbsent(" + k + "," + v + ")"; }
                case 1: { Object a = m.merge(k, 1, (x, y) -> x instanceof Integer ? (Integer) x + 1 : null), b = ref.merge(k, 1, (x, y) -> x instanceof Integer ? (Integer) x + 1 : null); same("merge", a, b); return "merge(" + k + ")"; }
                case 2: { Object a = m.computeIfAbsent(k, x -> v), b = ref.computeIfAbsent(k, x -> v); same("computeIfAbsent", a, b); return "computeIfAbsent(" + k + "," + v + ")"; }
                case 3: { Object a = m.computeIfPresent(k, (x, y) -> v), b = ref.computeIfPresent(k, (x, y) -> v); same("computeIfPresent", a, b); return "computeIfPresent(" + k + "," + v + ")"; }
                case 4: { Object a = m.compute(k, (x, y) -> v), b = ref.compute(k, (x, y) -> v); same("compute", a, b); return "compute(" + k + "," + v + ")"; }
                case 5: { Object a = m.replace(k, v), b = ref.replace(k, v); same("replace", a, b); return "replace(" + k + "," + v + ")"; }
                case 6: { boolean a = m.remove(k, v), b = ref.remove(k, v); same("remove(k,v)", a, b); return "remove(" + k + "," + v + ")"; }
                default: { m.replaceAll((x, y) -> y == null ? null : (Integer) y + 1); ref.replaceAll((x, y) -> y == null ? null : (Integer) y + 1); return "replaceAll(+1)"; }
            }
        } else if (pick < 95) {
            Object k = key(r);
            same("getOrDefault", m.getOrDefault(k, "dflt"), ref.getOrDefault(k, "dflt"));
            return "getOrDefault(" + k + ")";
        } else if (pick < 97) {
            CompactMap<Object, Object> copy = c.make();
            copy.putAll(m);
            if (!copy.equals(m) || !m.equals(copy) || copy.hashCode() != m.hashCode()) throw new IllegalStateException("copy differs: " + copy + " vs " + m);
            return "copyCheck";
        } else if (pick < 98) {
            m.clear(); ref.clear();
            return "clear";
        } else {
            for (Map.Entry<Object, Object> e : m.entrySet()) { /* walk */ }
            return "walk";
        }
    }

    static void same(String what, Object a, Object b) {
        if (!Objects.equals(a, b)) throw new IllegalStateException(what + " returned " + a + " vs " + b);
    }

    static String check(CompactMap<Object, Object> m, Map<Object, Object> ref, Config c) {
        if (m.size() != ref.size()) return "size: " + m.size() + " vs " + ref.size() + " m=" + m + " ref=" + ref;
        List<Object> mk = new ArrayList<>(m.keySet()), rk = new ArrayList<>(ref.keySet());
        if (c.ordered()) {
            if (!mk.equals(rk)) {
                return (sameIgnoringSpelling(mk, rk) ? "key spelling: " : "order: ") + mk + " vs " + rk;
            }
        } else {
            if (!new HashSet<>(mk).equals(new HashSet<>(rk))) {
                return (sameIgnoringSpelling(sorted(mk), sorted(rk)) ? "key spelling: " : "contents: ") + m + " vs " + ref;
            }
        }
        for (Object k : rk) {
            if (!Objects.equals(m.get(k), ref.get(k))) return "value: key " + k + " " + m.get(k) + " vs " + ref.get(k);
        }
        if (!m.equals(ref)) return "equals(ref): false for m=" + m + " ref=" + ref;
        if (!ref.equals(m) && !c.ci()) return "ref.equals(m): false for m=" + m + " ref=" + ref;
        if (!c.ci() && m.hashCode() != ref.hashCode()) return "hashCode: " + m.hashCode() + " vs " + ref.hashCode();
        CompactMap.LogicalValueType t = m.getLogicalValueType();
        int n = m.size();
        boolean stateOk = n == 0 ? t == CompactMap.LogicalValueType.EMPTY
                : n == 1 ? (t == CompactMap.LogicalValueType.OBJECT || t == CompactMap.LogicalValueType.ENTRY)
                : n <= c.compactSize() ? t == CompactMap.LogicalValueType.ARRAY : t == CompactMap.LogicalValueType.MAP;
        if (!stateOk) return "state: " + t + " at size " + n + " (compactSize " + c.compactSize() + ")";
        return null;
    }

    static List<Object> sorted(List<Object> keys) {
        List<Object> copy = new ArrayList<>(keys);
        copy.sort(CompactMap.CompactMapComparator.get(true, false));
        return copy;
    }

    static boolean sameIgnoringSpelling(List<Object> a, List<Object> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Object x = a.get(i), y = b.get(i);
            if (x instanceof String && y instanceof String) { if (!((String) x).equalsIgnoreCase((String) y)) return false; }
            else if (!Objects.equals(x, y)) return false;
        }
        return true;
    }

    @Test
    void everyConfigurationAgreesWithItsReferenceMap() {
        for (Config c : configs()) {
            for (long seed = 1; seed <= 15; seed++) {
                Random r = new Random(seed * 31 + c.name().hashCode());
                CompactMap<Object, Object> m = c.make();
                Map<Object, Object> ref = c.reference();
                List<String> trace = new ArrayList<>();
                for (int i = 0; i < 250; i++) {
                    String op;
                    try {
                        op = step(r, m, ref, c);
                    } catch (RuntimeException e) {
                        fail(c.name() + " seed " + seed + ": " + e + "\n  after " + tail(trace));
                        return;
                    }
                    trace.add(op);
                    String problem = check(m, ref, c);
                    if (problem != null) {
                        fail(c.name() + " seed " + seed + ": " + problem + "\n  after " + tail(trace));
                    }
                }
            }
        }
    }

    private static String tail(List<String> trace) {
        return String.join("; ", trace.subList(Math.max(0, trace.size() - 20), trace.size()));
    }
}
