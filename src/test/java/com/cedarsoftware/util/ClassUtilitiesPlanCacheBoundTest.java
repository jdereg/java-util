package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * newInstance() keeps a bounded number of constructor plans per class. They are keyed by the classes of the supplied
 * values, which the caller chooses -- the values of a JSON object pick them -- so a class sent every combination held
 * a plan for each, without limit. Past the bound the plans start over, and every instance is still the one a fresh
 * match builds.
 */
class ClassUtilitiesPlanCacheBoundTest {

    public static class Four {
        final Object a;
        final Object b;
        final Object c;
        final Object d;

        public Four(Object a, Object b, Object c, Object d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        /** Per parameter: which supplied value it received, else null or the type of the default made for it. */
        List<String> received(List<Object> supplied) {
            List<String> out = new ArrayList<>(4);
            for (Object field : Arrays.asList(a, b, c, d)) {
                int index = -1;
                for (int i = 0; i < supplied.size(); i++) {
                    if (field != null && supplied.get(i) == field) {
                        index = i;
                    }
                }
                out.add(index >= 0 ? "value " + index : field == null ? "null" : "new " + field.getClass().getName());
            }
            return out;
        }
    }

    private static final Object[] KINDS = {null, "s", 1L, 1.5d, Boolean.TRUE, new LinkedHashMap<>(), new Object[0]};

    private static int plansFor(Class<?> c) throws Exception {
        Field field = ClassUtilities.class.getDeclaredField("constructorPlans");
        field.setAccessible(true);
        return ((Map<?, ?>) ((ClassValue<?>) field.get(null)).get(c)).size();
    }

    private static List<Object> shape(int s) {
        List<Object> args = new ArrayList<>(4);
        for (int k = 0; k < 4; k++) {
            args.add(KINDS[s % KINDS.length]);
            s /= KINDS.length;
        }
        return args;
    }

    @Test
    void plansPerClassAreBoundedAndEveryInstanceIsTheOneAFreshMatchBuilds() throws Exception {
        int shapes = (int) Math.pow(KINDS.length, 4);          // 2,401
        List<List<String>> first = new ArrayList<>(shapes);
        int most = 0;
        for (int s = 0; s < shapes; s++) {
            List<Object> args = shape(s);
            first.add(((Four) ClassUtilities.newInstance(Four.class, args)).received(args));
            most = Math.max(most, plansFor(Four.class));
        }
        assertTrue(most <= 256, "plans held for one class: " + most);

        for (int round = 0; round < 2; round++) {           // replayed while cached, planned again once dropped
            for (int s = 0; s < shapes; s++) {
                List<Object> args = shape(s);
                List<String> again = ((Four) ClassUtilities.newInstance(Four.class, args)).received(args);
                assertEquals(first.get(s), again, "shape " + s + ", round " + round);
            }
        }
        assertTrue(plansFor(Four.class) <= 256);
    }
}
