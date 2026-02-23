package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cases for varargs constructor support in ClassUtilities.
 * Ensures that varargs constructors are properly handled by packing
 * trailing arguments into arrays as needed.
 */
class ClassUtilitiesVarargsTest {
    
    // Test class with only varargs constructor
    static class VarargsOnly {
        private final String[] values;
        
        public VarargsOnly(String... values) {
            this.values = values;
        }
        
        public String[] getValues() {
            return values;
        }
    }
    
    // Test class with fixed param + varargs
    static class MixedVarargs {
        private final int count;
        private final String[] items;
        
        public MixedVarargs(int count, String... items) {
            this.count = count;
            this.items = items;
        }
        
        public int getCount() {
            return count;
        }
        
        public String[] getItems() {
            return items;
        }
    }
    
    // Test class with multiple fixed params + varargs
    static class ComplexVarargs {
        private final String prefix;
        private final int multiplier;
        private final Integer[] numbers;
        
        public ComplexVarargs(String prefix, int multiplier, Integer... numbers) {
            this.prefix = prefix;
            this.multiplier = multiplier;
            this.numbers = numbers;
        }
        
        public String getPrefix() {
            return prefix;
        }
        
        public int getMultiplier() {
            return multiplier;
        }
        
        public Integer[] getNumbers() {
            return numbers;
        }
    }

    static class NamedVarargsWithOptionalTail {
        private final String prefix;
        private final String[] values;

        public NamedVarargsWithOptionalTail(String prefix, String... values) {
            this.prefix = prefix;
            this.values = values;
        }

        public String getPrefix() {
            return prefix;
        }

        public String[] getValues() {
            return values;
        }
    }

    static class PrimitiveNamedVarargsWithOptionalTail {
        private final int fixed;
        private final int[] values;

        public PrimitiveNamedVarargsWithOptionalTail(int fixed, int... values) {
            this.fixed = fixed;
            this.values = values;
        }

        public int getFixed() {
            return fixed;
        }

        public int[] getValues() {
            return values;
        }
    }
    
    @Test
    @DisplayName("Varargs-only constructor with no arguments")
    void testVarargsOnlyNoArgs() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = new ArrayList<>();
        
        VarargsOnly instance = (VarargsOnly) ClassUtilities.newInstance(converter, VarargsOnly.class, args);
        assertNotNull(instance);
        assertNotNull(instance.getValues());
        assertEquals(0, instance.getValues().length, "Empty varargs should create empty array");
    }
    
    @Test
    @DisplayName("Varargs-only constructor with single argument")
    void testVarargsOnlySingleArg() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = Arrays.asList("hello");
        
        VarargsOnly instance = (VarargsOnly) ClassUtilities.newInstance(converter, VarargsOnly.class, args);
        assertNotNull(instance);
        assertArrayEquals(new String[]{"hello"}, instance.getValues());
    }
    
    @Test
    @DisplayName("Varargs-only constructor with multiple arguments")
    void testVarargsOnlyMultipleArgs() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = Arrays.asList("one", "two", "three");
        
        VarargsOnly instance = (VarargsOnly) ClassUtilities.newInstance(converter, VarargsOnly.class, args);
        assertNotNull(instance);
        assertArrayEquals(new String[]{"one", "two", "three"}, instance.getValues());
    }
    
    @Test
    @DisplayName("Mixed constructor with fixed param only")
    void testMixedVarargsFixedOnly() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = Arrays.asList(5);
        
        MixedVarargs instance = (MixedVarargs) ClassUtilities.newInstance(converter, MixedVarargs.class, args);
        assertNotNull(instance);
        assertEquals(5, instance.getCount());
        assertNotNull(instance.getItems());
        assertEquals(0, instance.getItems().length, "Varargs should be empty array when not provided");
    }
    
    @Test
    @DisplayName("Mixed constructor with fixed and varargs")
    void testMixedVarargsWithBoth() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = Arrays.asList(3, "a", "b", "c");
        
        MixedVarargs instance = (MixedVarargs) ClassUtilities.newInstance(converter, MixedVarargs.class, args);
        assertNotNull(instance);
        assertEquals(3, instance.getCount());
        assertArrayEquals(new String[]{"a", "b", "c"}, instance.getItems());
    }
    
    @Test
    @DisplayName("Complex varargs with type conversion")
    void testComplexVarargsWithConversion() {
        Converter converter = new Converter(new DefaultConverterOptions());
        // Pass strings that need to be converted to Integer
        List<Object> args = Arrays.asList("test", 2, "10", "20", "30");
        
        ComplexVarargs instance = (ComplexVarargs) ClassUtilities.newInstance(converter, ComplexVarargs.class, args);
        assertNotNull(instance);
        assertEquals("test", instance.getPrefix());
        assertEquals(2, instance.getMultiplier());
        assertArrayEquals(new Integer[]{10, 20, 30}, instance.getNumbers());
    }
    
    @Test
    @DisplayName("Varargs with array argument directly")
    void testVarargsWithArrayArgument() {
        Converter converter = new Converter(new DefaultConverterOptions());
        // Pass an array directly as the varargs argument
        String[] array = {"x", "y", "z"};
        List<Object> args = Arrays.asList((Object) array);
        
        VarargsOnly instance = (VarargsOnly) ClassUtilities.newInstance(converter, VarargsOnly.class, args);
        assertNotNull(instance);
        assertArrayEquals(array, instance.getValues());
    }
    
    @Test
    @DisplayName("Mixed varargs with array argument for varargs part")
    void testMixedVarargsWithArrayArgument() {
        Converter converter = new Converter(new DefaultConverterOptions());
        // Pass fixed param and array for varargs
        String[] items = {"item1", "item2"};
        List<Object> args = Arrays.asList(7, items);
        
        MixedVarargs instance = (MixedVarargs) ClassUtilities.newInstance(converter, MixedVarargs.class, args);
        assertNotNull(instance);
        assertEquals(7, instance.getCount());
        assertArrayEquals(items, instance.getItems());
    }
    
    @Test
    @DisplayName("Varargs with null values")
    void testVarargsWithNulls() {
        Converter converter = new Converter(new DefaultConverterOptions());
        List<Object> args = Arrays.asList("first", null, "third");
        
        VarargsOnly instance = (VarargsOnly) ClassUtilities.newInstance(converter, VarargsOnly.class, args);
        assertNotNull(instance);
        assertEquals(3, instance.getValues().length);
        assertEquals("first", instance.getValues()[0]);
        assertNull(instance.getValues()[1]);
        assertEquals("third", instance.getValues()[2]);
    }

    @Test
    @DisplayName("Named map missing object varargs key should produce empty varargs array")
    void testNamedMapMissingObjectVarargsKey() {
        Converter converter = new Converter(new DefaultConverterOptions());
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("prefix", "head");

        NamedVarargsWithOptionalTail instance =
                (NamedVarargsWithOptionalTail) ClassUtilities.newInstance(converter, NamedVarargsWithOptionalTail.class, args);

        assertNotNull(instance);
        assertEquals("head", instance.getPrefix());
        assertNotNull(instance.getValues());
        assertEquals(0, instance.getValues().length);
    }

    @Test
    @DisplayName("Named map missing primitive varargs key should produce empty varargs array")
    void testNamedMapMissingPrimitiveVarargsKey() {
        Converter converter = new Converter(new DefaultConverterOptions());
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("fixed", 7);

        PrimitiveNamedVarargsWithOptionalTail instance =
                (PrimitiveNamedVarargsWithOptionalTail) ClassUtilities.newInstance(
                        converter, PrimitiveNamedVarargsWithOptionalTail.class, args);

        assertNotNull(instance);
        assertEquals(7, instance.getFixed());
        assertNotNull(instance.getValues());
        assertArrayEquals(new int[0], instance.getValues());
    }
}
