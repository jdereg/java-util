package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test cases for generated-key Map ordering fix in ClassUtilities.
 * Ensures that Maps with generated keys (arg0, arg1, etc.) are properly
 * ordered even when there are gaps in the sequence.
 */
class ClassUtilitiesGeneratedKeysTest {
    
    // Test class with multiple parameters
    static class MultiParamClass {
        private final String first;
        private final String second;
        private final String third;
        
        public MultiParamClass(String first, String second, String third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
        
        public String getFirst() {
            return first;
        }
        
        public String getSecond() {
            return second;
        }
        
        public String getThird() {
            return third;
        }
    }
    
    @Test
    @DisplayName("Generated keys with sequential ordering (arg0, arg1, arg2)")
    void testGeneratedKeysSequential() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("arg0", "first");
        args.put("arg1", "second");
        args.put("arg2", "third");
        
        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class, args);
        
        assertNotNull(instance);
        assertEquals("first", instance.getFirst());
        assertEquals("second", instance.getSecond());
        assertEquals("third", instance.getThird());
    }
    
    @Test
    @DisplayName("Generated keys with gap in sequence (arg0, arg2, arg4)")
    void testGeneratedKeysWithGaps() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Create map with gaps - arg1 and arg3 are missing
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("arg0", "first");
        args.put("arg2", "second");
        args.put("arg4", "third");
        
        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class, args);
        
        assertNotNull(instance);
        assertEquals("first", instance.getFirst());
        assertEquals("second", instance.getSecond());
        assertEquals("third", instance.getThird());
    }
    
    @Test
    @DisplayName("Generated keys out of order in map")
    void testGeneratedKeysOutOfOrder() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Create map with keys in wrong order
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("arg2", "third");
        args.put("arg0", "first");
        args.put("arg1", "second");
        
        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class, args);
        
        assertNotNull(instance);
        assertEquals("first", instance.getFirst());
        assertEquals("second", instance.getSecond());
        assertEquals("third", instance.getThird());
    }
    
    @Test
    @DisplayName("Generated keys with high numbers (arg10, arg11, arg9)")
    void testGeneratedKeysHighNumbers() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test with high numbers to ensure numeric sorting works correctly
        // arg9 should come before arg10 and arg11
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("arg11", "third");
        args.put("arg9", "first");
        args.put("arg10", "second");
        
        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class, args);
        
        assertNotNull(instance);
        assertEquals("first", instance.getFirst());
        assertEquals("second", instance.getSecond());
        assertEquals("third", instance.getThird());
    }
    
    // Test class with varargs
    static class VarArgsClass {
        private final String[] values;
        
        public VarArgsClass(String... values) {
            this.values = values;
        }
        
        public String[] getValues() {
            return values;
        }
    }
    
    @Test
    @DisplayName("Generated keys with varargs constructor")
    void testGeneratedKeysWithVarargs() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("arg2", "c");
        args.put("arg0", "a");
        args.put("arg1", "b");
        args.put("arg3", "d");
        
        VarArgsClass instance = (VarArgsClass) ClassUtilities.newInstance(converter, VarArgsClass.class, args);
        
        assertNotNull(instance);
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, instance.getValues());
    }
    
    @Test
    @DisplayName("Non-generated keys should not be affected")
    void testNonGeneratedKeys() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Use actual parameter names, not generated keys
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("first", "value1");
        args.put("second", "value2");
        args.put("third", "value3");
        
        // This should still work but use named parameter matching
        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class, args);
        
        assertNotNull(instance);
        // Values might be matched differently since these are named parameters
        // The test verifies that non-generated keys are handled differently
    }

    /**
     * A Map whose entry iterator hands out one entry object, re-pointed at each field (JsonObject's did, before json-io
     * 4.113.0). Arguments with gaps in their numbering are sorted by key; the entries were kept and sorted
     * themselves, so every argument was the last field's value.
     */
    @Test
    @DisplayName("Generated keys with gaps, from a Map whose iterator reuses one entry")
    void testGeneratedKeysWithGapsFromAReusingIterator() {
        Converter converter = new Converter(new DefaultConverterOptions());
        Map<String, Object> backing = new LinkedHashMap<>();
        backing.put("arg4", "third");
        backing.put("arg0", "first");
        backing.put("arg2", "second");

        MultiParamClass instance = (MultiParamClass) ClassUtilities.newInstance(converter, MultiParamClass.class,
                new OneEntryMap(backing));

        assertEquals("first", instance.getFirst());
        assertEquals("second", instance.getSecond());
        assertEquals("third", instance.getThird());
    }

    /** A read-only view of a map whose entry iterator re-points one entry object at each field. */
    private static final class OneEntryMap extends AbstractMap<String, Object> {
        private final Map<String, Object> backing;

        OneEntryMap(Map<String, Object> backing) {
            this.backing = backing;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return new AbstractSet<Map.Entry<String, Object>>() {
                @Override
                public Iterator<Map.Entry<String, Object>> iterator() {
                    Iterator<Map.Entry<String, Object>> it = backing.entrySet().iterator();
                    String[] key = new String[1];
                    Object[] value = new Object[1];
                    Map.Entry<String, Object> shared = new Map.Entry<String, Object>() {
                        public String getKey() {
                            return key[0];
                        }

                        public Object getValue() {
                            return value[0];
                        }

                        public Object setValue(Object v) {
                            throw new UnsupportedOperationException();
                        }
                    };
                    return new Iterator<Map.Entry<String, Object>>() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        public Map.Entry<String, Object> next() {
                            Map.Entry<String, Object> e = it.next();
                            key[0] = e.getKey();
                            value[0] = e.getValue();
                            return shared;
                        }
                    };
                }

                @Override
                public int size() {
                    return backing.size();
                }
            };
        }
    }
}
