package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.jupiter.api.Assertions.*;

public class GraphComparatorJavaDeltaProcessorTest {

    private static class DataHolder {
        long id;
        String[] arrayField;
        List<String> listField;
        Set<String> setField;
        Map<String, String> mapField;
        String strField;
    }

    private GraphComparator.DeltaProcessor getProcessor() {
        return GraphComparator.getJavaDeltaProcessor();
    }

    private Field getField(String name) throws Exception {
        Field f = DataHolder.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Test
    public void testProcessArrayResize() throws Exception {
        DataHolder d = new DataHolder();
        d.arrayField = new String[] {"a", "b"};

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "arrayField", "", d.arrayField, null, 3);
        delta.setCmd(ARRAY_RESIZE);

        getProcessor().processArrayResize(d, getField("arrayField"), delta);

        assertEquals(3, d.arrayField.length);
        assertEquals("a", d.arrayField[0]);
        assertEquals("b", d.arrayField[1]);
        assertNull(d.arrayField[2]);
    }

    @Test
    public void testProcessArraySetElement() throws Exception {
        DataHolder d = new DataHolder();
        d.arrayField = new String[] {"a", "b", "c"};

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "arrayField", "", d.arrayField[1], "z", 1);
        delta.setCmd(ARRAY_SET_ELEMENT);

        getProcessor().processArraySetElement(d, getField("arrayField"), delta);

        assertArrayEquals(new String[]{"a", "z", "c"}, d.arrayField);
    }

    @Test
    public void testProcessListResize() throws Exception {
        DataHolder d = new DataHolder();
        d.listField = new ArrayList<>(Arrays.asList("a", "b"));

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "listField", "", d.listField, null, 3);
        delta.setCmd(LIST_RESIZE);

        getProcessor().processListResize(d, getField("listField"), delta);

        assertEquals(3, d.listField.size());
        assertEquals(Arrays.asList("a", "b", null), d.listField);
    }

    @Test
    public void testProcessListSetElement() throws Exception {
        DataHolder d = new DataHolder();
        d.listField = new ArrayList<>(Arrays.asList("a", "b", "c"));

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "listField", "", "b", "x", 1);
        delta.setCmd(LIST_SET_ELEMENT);

        getProcessor().processListSetElement(d, getField("listField"), delta);

        assertEquals(Arrays.asList("a", "x", "c"), d.listField);
    }

    @Test
    public void testProcessMapPut() throws Exception {
        DataHolder d = new DataHolder();
        d.mapField = new HashMap<>();
        d.mapField.put("k1", "v1");

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "mapField", "", null, "v2", "k2");
        delta.setCmd(MAP_PUT);

        getProcessor().processMapPut(d, getField("mapField"), delta);

        assertEquals(2, d.mapField.size());
        assertEquals("v2", d.mapField.get("k2"));
    }

    @Test
    public void testProcessMapRemove() throws Exception {
        DataHolder d = new DataHolder();
        d.mapField = new HashMap<>();
        d.mapField.put("k1", "v1");
        d.mapField.put("k2", "v2");

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "mapField", "", "v2", null, "k2");
        delta.setCmd(MAP_REMOVE);

        getProcessor().processMapRemove(d, getField("mapField"), delta);

        assertEquals(1, d.mapField.size());
        assertFalse(d.mapField.containsKey("k2"));
    }

    @Test
    public void testProcessObjectAssignField() throws Exception {
        DataHolder d = new DataHolder();
        d.strField = "old";

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "strField", "", "old", "new", null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);

        getProcessor().processObjectAssignField(d, getField("strField"), delta);

        assertEquals("new", d.strField);
    }

    @Test
    public void testProcessObjectOrphan() throws Exception {
        DataHolder d = new DataHolder();
        d.strField = "stay";

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "strField", "", null, null, null);
        delta.setCmd(OBJECT_ORPHAN);

        getProcessor().processObjectOrphan(d, getField("strField"), delta);

        assertEquals("stay", d.strField);
    }

    @Test
    public void testProcessObjectTypeChanged() throws Exception {
        DataHolder d = new DataHolder();
        d.listField = new ArrayList<>();

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "listField", "", null, null, null);
        delta.setCmd(OBJECT_FIELD_TYPE_CHANGED);

        assertThrows(RuntimeException.class, () -> getProcessor().processObjectTypeChanged(d, getField("listField"), delta));
    }

    @Test
    public void testProcessSetAdd() throws Exception {
        DataHolder d = new DataHolder();
        d.setField = new HashSet<>();

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "setField", "", null, "x", null);
        delta.setCmd(SET_ADD);

        getProcessor().processSetAdd(d, getField("setField"), delta);

        assertTrue(d.setField.contains("x"));
    }

    @Test
    public void testProcessSetRemove() throws Exception {
        DataHolder d = new DataHolder();
        d.setField = new HashSet<>(Arrays.asList("a", "b"));

        GraphComparator.Delta delta = new GraphComparator.Delta(d.id, "setField", "", "a", null, null);
        delta.setCmd(SET_REMOVE);

        getProcessor().processSetRemove(d, getField("setField"), delta);

        assertFalse(d.setField.contains("a"));
        assertEquals(1, d.setField.size());
    }
}
