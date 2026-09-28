package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * newInstance() fills a varargs parameter with the elements of an array or Collection it is given, and a value that
 * will not convert becomes the default in its own slot.
 * <ul>
 *   <li>By name: a failed conversion advanced the index twice, so the default landed one slot too far and the next
 *       write fell off the end of the array; the named match was abandoned for the positional one, which produced
 *       garbage ({@code ["1","abc","3"]} for {@code int...} gave {@code [0]}). And an array value -- how a JSON array
 *       arrives -- was taken as a single element.</li>
 *   <li>By position: a lone array that is not the parameter's own type ({@code Object[]} for {@code String...}, or an
 *       {@code Integer[]} for {@code int...}) was packed as one element, which converted to null or 0.</li>
 * </ul>
 */
class ClassUtilitiesVarargsElementsTest {

    public static class Nums {
        final String name;
        final int[] nums;

        public Nums(String name, int... nums) {
            this.name = name;
            this.nums = nums;
        }
    }

    public static class Tags {
        final String name;
        final String[] tags;

        public Tags(String name, String... tags) {
            this.name = name;
            this.tags = tags;
        }
    }

    public static class Ints {
        final int[] values;

        public Ints(int... values) {
            this.values = values;
        }
    }

    public static class Items {
        final Object[] items;

        public Items(Object... items) {
            this.items = items;
        }
    }

    @Test
    void byNameAValueThatWillNotConvertBecomesTheDefaultInItsOwnSlot() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("name", "n");
        args.put("nums", Arrays.asList("1", "abc", "3"));
        Nums nums = (Nums) ClassUtilities.newInstance(Nums.class, args);
        assertEquals("n", nums.name);
        assertArrayEquals(new int[]{1, 0, 3}, nums.nums);

        args.put("nums", Arrays.asList("1", "2", "abc", "4"));
        assertArrayEquals(new int[]{1, 2, 0, 4}, ((Nums) ClassUtilities.newInstance(Nums.class, args)).nums);
    }

    @Test
    void byNameAnArrayValueGivesItsElements() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("name", "n");
        args.put("tags", new Object[]{"a", "b"});
        assertArrayEquals(new String[]{"a", "b"}, ((Tags) ClassUtilities.newInstance(Tags.class, args)).tags);

        args.put("tags", Arrays.asList("a", "b"));
        assertArrayEquals(new String[]{"a", "b"}, ((Tags) ClassUtilities.newInstance(Tags.class, args)).tags);

        Map<String, Object> numbers = new LinkedHashMap<>();
        numbers.put("name", "n");
        numbers.put("nums", new Object[]{"4", 5L});
        assertArrayEquals(new int[]{4, 5}, ((Nums) ClassUtilities.newInstance(Nums.class, numbers)).nums);
    }

    @Test
    void byPositionALoneArrayGivesItsElements() {
        Tags tags = (Tags) ClassUtilities.newInstance(Tags.class, Arrays.asList("n", new Object[]{"a", "b"}));
        assertEquals("n", tags.name);
        assertArrayEquals(new String[]{"a", "b"}, tags.tags);

        Ints ints = (Ints) ClassUtilities.newInstance(Ints.class, Collections.singletonList(new Integer[]{1, 2}));
        assertArrayEquals(new int[]{1, 2}, ints.values);
    }

    @Test
    void aLoneValueThatIsItselfAnElementStaysOne() {
        List<String> list = Arrays.asList("a", "b");
        Items items = (Items) ClassUtilities.newInstance(Items.class, Collections.singletonList(list));
        assertEquals(1, items.items.length, "a List is an Object: one element, as Java would pass it");
        assertEquals(list, items.items[0]);

        Object[] own = {"x", "y"};
        Items passed = (Items) ClassUtilities.newInstance(Items.class, Collections.singletonList(own));
        assertArrayEquals(own, passed.items, "the parameter's own array type is the varargs array itself");
    }
}
