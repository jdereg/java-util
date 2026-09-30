package com.cedarsoftware.util;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.ARRAY_RESIZE;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.ARRAY_SET_ELEMENT;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * GraphComparator's Java delta processor makes the fields it is handed accessible, through ReflectionUtils' checked
 * path. applyDelta hands it fields from ReflectionUtils, already accessible, but the processor is public, and a
 * caller may hand it a field of its own: a private one straight from getDeclaredField() must still be written.
 */
class GraphComparatorProcessorFieldAccessTest {

    static class Target {
        private String name = "a";
        private int[] values = {1, 2, 3};
    }

    @SuppressWarnings("deprecation")
    private static Field raw(String name) throws Exception {
        Field field = Target.class.getDeclaredField(name);     // a new Field object, never made accessible
        assertFalse(field.isAccessible());
        return field;
    }

    @Test
    void theJavaDeltaProcessorWritesAPrivateFieldItIsHandedDirectly() throws Exception {
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        Target target = new Target();

        GraphComparator.Delta assign = new GraphComparator.Delta(1, "name", "name", "a", "b", null);
        assign.setCmd(OBJECT_ASSIGN_FIELD);
        processor.processObjectAssignField(target, raw("name"), assign);
        assertEquals("b", target.name);

        GraphComparator.Delta set = new GraphComparator.Delta(1, "values", "values", 2, 9, 1);
        set.setCmd(ARRAY_SET_ELEMENT);
        processor.processArraySetElement(target, raw("values"), set);
        assertArrayEquals(new int[]{1, 9, 3}, target.values);

        GraphComparator.Delta resize = new GraphComparator.Delta(1, "values", "values", null, null, 2);
        resize.setCmd(ARRAY_RESIZE);
        processor.processArrayResize(target, raw("values"), resize);
        assertArrayEquals(new int[]{1, 9}, target.values);
    }
}
