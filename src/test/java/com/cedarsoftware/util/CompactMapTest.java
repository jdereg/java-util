package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static com.cedarsoftware.util.CompactMap.CASE_SENSITIVE;
import static com.cedarsoftware.util.CompactMap.COMPACT_SIZE;
import static com.cedarsoftware.util.CompactMap.MAP_TYPE;
import static com.cedarsoftware.util.CompactMap.ORDERING;
import static com.cedarsoftware.util.CompactMap.SORTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CompactMapTest
{
    @Test
    public void testSizeAndEmpty()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.size() == 0;
        assert map.isEmpty();
        assert map.put("value", 10.0d) == null;
        assert map.size() == 1;
        assert !map.isEmpty();

        assert map.put("alpha", "beta") == null;
        assert map.size() == 2;
        assert !map.isEmpty();

        assert map.remove("alpha").equals("beta");
        assert map.size() == 1;
        assert !map.isEmpty();

        assert 10.0d == (Double) map.remove("value");
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testSizeAndEmptyHardOrder()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.size() == 0;
        assert map.isEmpty();
        assert map.put("value", 10.0) == null;
        assert map.size() == 1;
        assert !map.isEmpty();

        assert map.put("alpha", "beta") == null;
        assert map.size() == 2;
        assert !map.isEmpty();

        // Remove out of order (singleKey item is removed leaving one entry that is NOT the same as single key ("value")
        assert 10.0 == (Double) map.remove("value");
        assert map.size() == 1;
        assert !map.isEmpty();

        assert map.remove("alpha") == "beta";
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testContainsKey()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }

            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert !map.containsKey("foo");

        assert map.put("foo", "bar") == null;
        assert map.containsKey("foo");
        assert !map.containsKey("bar");
        assert !map.containsKey("value");   // not the single key

        assert map.put("value", "baz") == null;
        assert map.containsKey("foo");
        assert map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.size() == 2;

        assert map.remove("foo") == "bar";
        assert !map.containsKey("foo");
        assert map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.size() == 1;

        assert map.remove("value") == "baz";
        assert !map.containsKey("foo");
        assert !map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.isEmpty();
    }

    @Test
    public void testContainsKeyHardOrder()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert !map.containsKey("foo");

        assert map.put("foo", "bar") == null;
        assert map.containsKey("foo");
        assert !map.containsKey("bar");
        assert !map.containsKey("value");   // not the single key

        assert map.put("value", "baz") == null;
        assert map.containsKey("foo");
        assert map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.size() == 2;

        assert map.remove("value") == "baz";
        assert map.containsKey("foo");
        assert !map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.size() == 1;

        assert map.remove("foo") == "bar";
        assert !map.containsKey("foo");
        assert !map.containsKey("value");
        assert !map.containsKey("bar");
        assert map.isEmpty();
    }

    @Test
    public void testContainsValue()
    {
        testContainsValueHelper("value");
        testContainsValueHelper("bingo");
    }

    private void testContainsValueHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert !map.containsValue("6");
        assert !map.containsValue(null);
        assert map.put("value", "6") == null;
        assert map.containsValue("6");
        assert map.put("foo", "bar") == null;
        assert map.containsValue("bar");
        assert !map.containsValue(null);

        assert map.remove("foo") == "bar";
        assert !map.containsValue("bar");
        assert map.containsValue("6");

        assert map.remove("value") == "6";
        assert !map.containsValue("6");
        assert map.isEmpty();

        map.put("key1", "foo");
        map.put("key2", "bar");
        map.put("key3", "baz");
        map.put("key4", "qux");
        assert map.containsValue("foo");
        assert map.containsValue("bar");
        assert map.containsValue("baz");
        assert map.containsValue("qux");
        assert !map.containsValue("quux");
    }

    @Test
    public void testContainsValueHardOrder()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert !map.containsValue("6");
        assert !map.containsValue(null);
        assert map.put("value", "6") == null;
        assert map.containsValue("6");
        assert map.put("foo", "bar") == null;
        assert map.containsValue("bar");
        assert !map.containsValue(null);

        assert map.remove("value") == "6";
        assert !map.containsValue("6");
        assert map.containsValue("bar");

        assert map.remove("foo") == "bar";
        assert !map.containsValue("bar");
        assert map.isEmpty();
    }

    @Test
    public void testGet()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.get("foo") == null;

        assert map.put("foo", "bar") == null;
        assert map.get("foo") == "bar";
        assert map.get("bar") == null;
        assert map.get("value") == null;

        assert map.put("value", "baz") == null;
        assert map.get("foo") == "bar";
        assert map.get("value") == "baz";
        assert map.get("bar") == null;
        assert map.size() == 2;

        assert map.remove("foo") == "bar";
        assert map.get("foo") == null;
        assert map.get("value") == "baz";
        assert map.get("bar") == null;
        assert map.size() == 1;

        assert map.remove("value") == "baz";
        assert map.get("foo") == null;
        assert map.get("value") == null;
        assert map.get("bar") == null;
        assert map.isEmpty();
    }

    @Test
    public void testGetHardOrder()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.get("foo") == null;

        assert map.put("foo", "bar") == null;
        assert map.get("foo") == "bar";
        assert map.get("bar") == null;
        assert map.get("value") == null;

        assert map.put("value", "baz") == null;
        assert map.get("foo") == "bar";
        assert map.get("value") == "baz";
        assert map.get("bar") == null;
        assert map.size() == 2;

        assert map.remove("value") == "baz";
        assert map.get("foo") == "bar";
        assert map.get("value") == null;
        assert map.get("bar") == null;
        assert map.size() == 1;

        assert map.remove("foo") == "bar";
        assert map.get("foo") == null;
        assert map.get("value") == null;
        assert map.get("bar") == null;
        assert map.isEmpty();
    }

    @Test
    public void testPutWithOverride()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("value", "foo") == null;
        assert map.get("value") == "foo";
        assert map.put("value", "bar") == "foo";
        assert map.get("value") == "bar";
        assert map.size() == 1;
    }

    @Test
    public void testPutWithManyEntries()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("foo", "alpha") == null;
        assert map.put("bar", "bravo") == null;
        assert map.put("baz", "charlie") == null;
        assert map.put("qux", "delta") == null;
        assert map.size() == 4;

        assert map.remove("qux") == "delta";
        assert map.size() == 3;
        assert !map.containsKey("qux");

        assert map.remove("baz") == "charlie";
        assert map.size() == 2;
        assert !map.containsKey("baz");

        assert map.remove("bar") == "bravo";
        assert map.size() == 1;
        assert !map.containsKey("bar");

        assert map.remove("foo") == "alpha";
        assert !map.containsKey("foo");
        assert map.isEmpty();
    }

    @Test
    public void testPutWithManyEntriesHardOrder()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("bar", "bravo") == null;
        assert map.put("baz", "charlie") == null;
        assert map.put("qux", "delta") == null;
        assert map.put("foo", "alpha") == null;
        assert map.size() == 4;

        assert map.remove("qux") == "delta";
        assert map.size() == 3;
        assert !map.containsKey("qux");

        assert map.remove("baz") == "charlie";
        assert map.size() == 2;
        assert !map.containsKey("baz");

        assert map.remove("bar") == "bravo";
        assert map.size() == 1;
        assert !map.containsKey("bar");

        assert map.remove("foo") == "alpha";
        assert !map.containsKey("foo");
        assert map.isEmpty();
    }

    @Test
    public void testPutWithManyEntriesHardOrder2()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("bar", "bravo") == null;
        assert map.put("baz", "charlie") == null;
        assert map.put("qux", "delta") == null;
        assert map.put("foo", "alpha") == null;
        assert map.size() == 4;

        assert map.remove("foo") == "alpha";
        assert map.size() == 3;
        assert !map.containsKey("foo");

        assert map.remove("qux") == "delta";
        assert map.size() == 2;
        assert !map.containsKey("qux");

        assert map.remove("baz") == "charlie";
        assert map.size() == 1;
        assert !map.containsKey("baz");

        assert map.remove("bar") == "bravo";
        assert !map.containsKey("bar");
        assert map.isEmpty();
    }

    @Test
    public void testWeirdPuts()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("foo", null) == null;
        assert map.size() == 1;
        assert map.get("foo") == null;
        assert map.containsValue(null);
        assert map.put("foo", "bar") == null;
        assert map.size() == 1;
        assert map.containsValue("bar");
        assert map.put("foo", null) == "bar";
        assert map.size() == 1;
        assert map.containsValue(null);
    }

    @Test
    public void testWeirdPuts1()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("bar", null) == null;
        assert map.size() == 1;
        assert map.get("bar") == null;
        assert map.containsValue(null);
        assert map.put("bar", "foo") == null;
        assert map.size() == 1;
        assert map.containsValue("foo");
        assert map.put("bar", null) == "foo";
        assert map.size() == 1;
        assert map.containsValue(null);
    }

    @Test
    public void testRemove()
    {
        testRemoveHelper("value");
        testRemoveHelper("bingo");
    }

    private void testRemoveHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        // Ensure remove on empty map does nothing.
        assert map.remove("value") == null;
        assert map.remove("foo") == null;

        assert map.put("value", "6.0") == null;
        assert map.remove("foo") == null;

        assert map.remove("value") == "6.0";
        assert map.size() == 0;
        assert map.isEmpty();

        assert map.put("value", "6.0") == null;
        assert map.put("foo", "bar") == null;
        assert map.remove("xxx") == null;

        assert map.remove("value") == "6.0";
        assert map.remove("foo") == "bar";
        assert map.isEmpty();

        assert map.put("value", "6.0") == null;
        assert map.put("foo", "bar") == null;
        assert map.put("baz", "qux") == null;
        assert map.remove("xxx") == null;
        assert map.remove("value") == "6.0";
        assert map.remove("foo") == "bar";
        assert map.remove("baz") == "qux";
        assert map.isEmpty();

        map.put("value", "foo");
        map.put("key2", "bar");
        map.put("key3", "baz");
        map.put("key4", "qux");
        assert map.size() == 4;
        assert map.remove("spunky") == null;
        assert map.size() == 4;
    }

    @Test
    public void testPutAll()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        Map<String, Object> source = new TreeMap<>();
        map.putAll(source);
        assert map.isEmpty();

        source = new TreeMap<>();
        source.put("qux", "delta");

        map.putAll(source);
        assert map.size() == 1;
        assert map.containsKey("qux");
        assert map.containsValue("delta");

        source = new TreeMap<>();
        source.put("qux", "delta");
        source.put("baz", "charlie");

        map.putAll(source);
        assert map.size() == 2;
        assert map.containsKey("qux");
        assert map.containsKey("baz");
        assert map.containsValue("delta");
        assert map.containsValue("charlie");

        source = new TreeMap<>();
        source.put("qux", "delta");
        source.put("baz", "charlie");
        source.put("bar", "bravo");

        map.putAll(source);
        assert map.size() == 3;
        assert map.containsKey("qux");
        assert map.containsKey("baz");
        assert map.containsKey("bar");
        assert map.containsValue("bravo");
        assert map.containsValue("delta");
        assert map.containsValue("charlie");
    }

    @Test
    public void testClear()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "value";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("foo", "bar") == null;
        assert map.size() == 1;
        map.clear();
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testKeySetEmpty()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.keySet().size() == 0;
        assert map.keySet().isEmpty();
        assert !map.keySet().remove("not found");
        assert !map.keySet().contains("whoops");
        Iterator<String> i = map.keySet().iterator();
        assert !i.hasNext();

        try
        {
            assert i.next() == null;
            fail();
        }
        catch (NoSuchElementException e)
        {
        }

        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException ignore)
        { }
    }

    @Test
    public void testKeySet1Item()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("key1", "foo") == null;
        assert map.keySet().size() == 1;
        assert map.keySet().contains("key1");

        Iterator<String> i = map.keySet().iterator();
        assert i.hasNext();
        assert i.next() == "key1";
        assert !i.hasNext();
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException ignore)
        { }

        assert map.put("key1", "bar") == "foo";
        i = map.keySet().iterator();
        i.next();
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySet1ItemHardWay()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("key9", "foo") == null;
        assert map.keySet().size() == 1;
        assert map.keySet().contains("key9");

        Iterator<String> i = map.keySet().iterator();
        assert i.hasNext();
        assert i.next() == "key9";
        assert !i.hasNext();
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException ignore)
        {
        }

        assert map.put("key9", "bar") == "foo";
        i = map.keySet().iterator();
        i.next();
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySetMultiItem()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.keySet().size() == 2;
        assert map.keySet().contains("key1");
        assert map.keySet().contains("key2");

        Iterator<String> i = map.keySet().iterator();
        assert i.hasNext();
        assert i.next().equals("key1");
        assert i.hasNext();
        assert i.next().equals("key2");
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException ignore) { }

        assert map.put("key1", "baz") == "foo";
        assert map.put("key2", "qux") == "bar";

        i = map.keySet().iterator();
        assert i.next().equals("key1");
        i.remove();
        assert i.next().equals("key2");
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySetMultiItem2()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap()
            {
                return new LinkedHashMap<>();
            }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.keySet().size() == 2;
        assert map.keySet().contains("key1");
        assert map.keySet().contains("key2");

        Iterator<String> i = map.keySet().iterator();
        assert i.hasNext();
        assert i.next().equals("key1");
        assert i.hasNext();
        assert i.next().equals("key2");
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException e) { }

        assert map.put("key1", "baz") == "foo";
        assert map.put("key2", "qux") == "bar";

        i = map.keySet().iterator();
        assert i.next().equals("key1");
        assert i.next().equals("key2");
        i = map.keySet().iterator();
        i.next();
        i.remove();
        assert map.size() == 1;
        assert map.keySet().contains("key2");
        i.next();
        i.remove();
        assert map.isEmpty();

        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException ignore) { }
    }

    @Test
    public void testKeySetMultiItemReverseRemove()
    {
        testKeySetMultiItemReverseRemoveHelper("key1");
        testKeySetMultiItemReverseRemoveHelper("bingo");
    }

    private void testKeySetMultiItemReverseRemoveHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> keys = map.keySet();
        Iterator<String> i = keys.iterator();
        i.next();
        i.next();
        i.next();
        i.next();
        assert map.get("key4") == "qux";
        i.remove();
        assert !map.containsKey("key4");
        assert map.size() == 3;

        i = keys.iterator();
        i.next();
        i.next();
        i.next();
        assert map.get("key3") == "baz";
        i.remove();
        assert !map.containsKey("key3");
        assert map.size() == 2;

        i = keys.iterator();
        i.next();
        i.next();
        assert map.get("key2") == "bar";
        i.remove();
        assert !map.containsKey("key2");
        assert map.size() == 1;

        i = keys.iterator();
        i.next();
        assert map.get("key1") == "foo";
        i.remove();
        assert !map.containsKey("key1");
        assert map.size() == 0;
    }

    @Test
    public void testKeySetMultiItemForwardRemove()
    {
        testKeySetMultiItemForwardRemoveHelper("key1");
        testKeySetMultiItemForwardRemoveHelper("bingo");
    }

    private void testKeySetMultiItemForwardRemoveHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> keys = map.keySet();
        Iterator<String> i = keys.iterator();

        String key = i.next();
        assert key.equals("key1");
        assert map.get("key1") == "foo";
        i.remove();
        assert !map.containsKey("key1");
        assert map.size() == 3;

        key = i.next();
        assert key.equals("key2");
        assert map.get("key2") == "bar";
        i.remove();
        assert !map.containsKey("key2");
        assert map.size() == 2;

        key = i.next();
        assert key.equals("key3");
        assert map.get("key3") == "baz";
        i.remove();
        assert !map.containsKey("key3");
        assert map.size() == 1;

        key = i.next();
        assert key.equals("key4");
        assert map.get("key4") == "qux";
        i.remove();
        assert !map.containsKey("key4");
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testKeySetToObjectArray()
    {
        testKeySetToObjectArrayHelper("key1");
        testKeySetToObjectArrayHelper("bingo");
    }

    private void testKeySetToObjectArrayHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;

        Set<String> set = map.keySet();
        Object[] keys = set.toArray();
        assert keys.length == 3;
        assert keys[0] == "key1";
        assert keys[1] == "key2";
        assert keys[2] == "key3";

        assert map.remove("key3") == "baz";
        set = map.keySet();
        keys = set.toArray();
        assert keys.length == 2;
        assert keys[0] == "key1";
        assert keys[1] == "key2";
        assert map.size() == 2;

        assert map.remove("key2") == "bar";
        set = map.keySet();
        keys = set.toArray();
        assert keys.length == 1;
        assert keys[0] == "key1";
        assert map.size() == 1;

        assert map.remove("key1") == "foo";
        set = map.keySet();
        keys = set.toArray();
        assert keys.length == 0;
        assert map.size() == 0;
    }

    @Test
    public void testKeySetToTypedObjectArray()
    {
        testKeySetToTypedObjectArrayHelper("key1");
        testKeySetToTypedObjectArrayHelper("bingo");
    }

    private void testKeySetToTypedObjectArrayHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;

        Set<String> set = map.keySet();
        String[] strings = new String[]{};
        String[] keys = set.toArray(strings);
        assert keys != strings;
        assert keys.length == 3;
        assert keys[0].equals("key1");
        assert keys[1].equals("key2");
        assert keys[2].equals("key3");
        
        strings = new String[]{"a", "b"};
        keys = set.toArray(strings);
        assert keys != strings;

        strings = new String[]{"a", "b", "c"};
        keys = set.toArray(strings);
        assert keys == strings;

        strings = new String[]{"a", "b", "c", "d", "e"};
        keys = set.toArray(strings);
        assert keys == strings;
        assert keys.length == strings.length;
        assert keys[3] == null;

        assert map.remove("key3") == "baz";
        set = map.keySet();
        keys = set.toArray(new String[]{});
        assert keys.length == 2;
        assert keys[0].equals("key1");
        assert keys[1].equals("key2");
        assert map.size() == 2;

        assert map.remove("key2") == "bar";
        set = map.keySet();
        keys = set.toArray(new String[]{});
        assert keys.length == 1;
        assert keys[0].equals("key1");
        assert map.size() == 1;

        assert map.remove("key1") == "foo";
        set = map.keySet();
        keys = set.toArray(new String[]{});
        assert keys.length == 0;
        assert map.size() == 0;
    }

    @Test
    public void testAddToKeySet()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        Set<String> set = map.keySet();

        try
        {
            set.add("bingo");
            fail();
        }
        catch (UnsupportedOperationException ignore) { }

        try
        {
            Collection<String> col = new ArrayList<>();
            col.add("hey");
            col.add("jude");
            set.addAll(col);
            fail();
        }
        catch (UnsupportedOperationException ignore) { }
    }

    @Test
    public void testKeySetContainsAll()
    {
        testKeySetContainsAllHelper("key1");
        testKeySetContainsAllHelper("bingo");
    }

    private void testKeySetContainsAllHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> set = map.keySet();
        Collection<String> strings = new ArrayList<>();
        strings.add("key1");
        strings.add("key4");
        assert set.containsAll(strings);
        strings.add("beep");
        assert !set.containsAll(strings);
    }

    @Test
    public void testKeySetRetainAll()
    {
        testKeySetRetainAllHelper("key1");
        testKeySetRetainAllHelper("bingo");
    }

    private void testKeySetRetainAllHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> set = map.keySet();
        Collection<String> strings = new ArrayList<>();
        strings.add("key1");
        strings.add("key4");
        strings.add("beep");
        assert set.retainAll(strings);
        assert set.size() == 2;
        assert map.get("key1") == "foo";
        assert map.get("key4") == "qux";

        strings.clear();
        strings.add("beep");
        strings.add("boop");
        set.retainAll(strings);
        assert set.size() == 0;
    }

    @Test
    public void testKeySetRemoveAll()
    {
        testKeySetRemoveAllHelper("key1");
        testKeySetRemoveAllHelper("bingo");
    }

    private void testKeySetRemoveAllHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> set = map.keySet();
        Collection<String> strings = new ArrayList<>();
        strings.add("key1");
        strings.add("key4");
        strings.add("beep");
        assert set.removeAll(strings);
        assert set.size() == 2;
        assert map.get("key2") == "bar";
        assert map.get("key3") == "baz";

        strings.clear();
        strings.add("beep");
        strings.add("boop");
        set.removeAll(strings);
        assert set.size() == 2;
        assert map.get("key2") == "bar";
        assert map.get("key3") == "baz";

        strings.add("key2");
        strings.add("key3");
        set.removeAll(strings);
        assert map.size() == 0;
    }

    @Test
    public void testKeySetClear()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "field"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        map.keySet().clear();
        assert map.size() == 0;
    }

    @Test
    public void testValues()
    {
        testValuesHelper("key1");
        testValuesHelper("bingo");
    }

    private void testValuesHelper(final String singleKey)
    {
        CompactMap<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Collection col = map.values();
        assert col.size() == 4;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

        Iterator<Object> i = map.values().iterator();
        assert i.hasNext();
        assert i.next() == "foo";
        i.remove();
        assert map.size() == 3;
        assert col.size() == 3;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY;

        assert i.hasNext();
        assert i.next() == "bar";
        i.remove();
        assert map.size() == 2;
        assert col.size() == 2;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY;

        assert i.hasNext();
        assert i.next() == "baz";
        i.remove();
        assert map.size() == 1;
        assert col.size() == 1;

        assert i.hasNext();
        assert i.next() == "qux";
        i.remove();
        assert map.size() == 0;
        assert col.size() == 0;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.EMPTY;
    }

    @Test
    public void testValuesHardWay()
    {
        testValuesHardWayHelper("key1");
        testValuesHardWayHelper("bingo");
    }

    private void testValuesHardWayHelper(final String singleKey)
    {
        CompactMap<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Collection<Object> col = map.values();
        assert col.size() == 4;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

        Iterator<Object> i = map.values().iterator();
        i.next();
        i.next();
        i.next();
        i.next();
        i.remove();
        assert map.size() == 3;
        assert col.size() == 3;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY;

        i = map.values().iterator();
        i.next();
        i.next();
        i.next();
        i.remove();
        assert map.size() == 2;
        assert col.size() == 2;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY;

        i = map.values().iterator();
        i.next();
        i.next();
        i.remove();
        assert map.size() == 1;
        assert col.size() == 1;
        if (singleKey.equals("key1"))
        {
            assert map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT;
        }
        else
        {
            assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
        }

        i = map.values().iterator();
        i.next();
        i.remove();
        assert map.size() == 0;
        assert col.size() == 0;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.EMPTY;
    }

    @Test
    public void testValuesWith1()
    {
        testValuesWith1Helper("key1");
        testValuesWith1Helper("bingo");
    }

    private void testValuesWith1Helper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        Collection<Object> col = map.values();
        assert col.size() == 1;
        Iterator i = col.iterator();
        assert i.hasNext() == true;
        assert i.next() == "foo";
        assert i.hasNext() == false;
        i.remove();
        
        i = map.values().iterator();
        assert i.hasNext() == false;

        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException ignore) { }

        i = map.values().iterator();
        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException ignore) { }

    }

    @Test
    public void testValuesClear()
    {
        Map<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        map.values().clear();
        assert map.size() == 0;
        assert map.values().isEmpty();
        assert map.values().size() == 0;
    }

    @Test
    public void testWithMapOnRHS()
    {
        testWithMapOnRHSHelper("key1");
        testWithMapOnRHSHelper("bingo");
    }

    @SuppressWarnings("unchecked")
    private void testWithMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", "alpha");
        map1.put("b", "bravo");
        map.put("key1", map1);

        Map<String, Object> x = (Map<String, Object>) map.get("key1");
        assert x instanceof HashMap;
        assert x.size() == 2;

        Map<String, Object> map2 = new HashMap<>();
        map2.put("a", "alpha");
        map2.put("b", "bravo");
        map2.put("c", "charlie");
        map.put("key2", map2);

        x = (Map<String, Object>) map.get("key2");
        assert x instanceof HashMap;
        assert x.size() == 3;

        Map<String, Object> map3 = new HashMap<>();
        map3.put("a", "alpha");
        map3.put("b", "bravo");
        map3.put("c", "charlie");
        map3.put("d", "delta");
        map.put("key3", map3);
        assert map.size() == 3;

        x = (Map<String, Object>) map.get("key3");
        assert x instanceof HashMap;
        assert x.size() == 4;

        assert map.remove("key3") instanceof Map;
        x = (Map<String, Object>) map.get("key2");
        assert x.size() == 3;
        assert map.size() == 2;

        assert map.remove("key2") instanceof Map;
        x = (Map<String, Object>) map.get("key1");
        assert x.size() == 2;
        assert map.size() == 1;

        map.remove("key1");
        assert map.size() == 0;
    }

    @Test
    public void testWithObjectArrayOnRHS()
    {
        testWithObjectArrayOnRHSHelper("key1");
        testWithObjectArrayOnRHSHelper("bingo");
    }

    private void testWithObjectArrayOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 2; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        Object[] array1 = new Object[] { "alpha", "bravo"};
        map.put("key1", array1);

        Object[] x = (Object[]) map.get("key1");
        assert x instanceof Object[];
        assert x.length == 2;

        Object[] array2 = new Object[] { "alpha", "bravo", "charlie" };
        map.put("key2", array2);

        x = (Object[]) map.get("key2");
        assert x instanceof Object[];
        assert x.length == 3;

        Object[] array3 = new Object[] { "alpha", "bravo", "charlie", "delta" };
        map.put("key3", array3);
        assert map.size() == 3;

        x = (Object[]) map.get("key3");
        assert x instanceof Object[];
        assert x.length == 4;

        assert map.remove("key3") instanceof Object[];
        x = (Object[]) map.get("key2");
        assert x.length == 3;
        assert map.size() == 2;

        assert map.remove("key2") instanceof Object[];
        x = (Object[]) map.get("key1");
        assert x.length == 2;
        assert map.size() == 1;

        map.remove("key1");
        assert map.size() == 0;
    }

    @Test
    public void testWithObjectArrayOnRHS1()
    {

        CompactMap<String, Object> map = new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 2; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("key1", "bar");
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT;
        map.put("key1", new Object[] { "bar" } );
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
        Arrays.equals((Object[])map.get("key1"), new Object[] { "bar" });
        map.put("key1", new Object[] { "baz" } );
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
        Arrays.equals((Object[])map.get("key1"), new Object[] { "baz" });
        map.put("key1", new HashMap<>() );
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
        assert map.get("key1") instanceof HashMap;
        Map x = (Map) map.get("key1");
        assert x.isEmpty();
        map.put("key1", "toad");
        assert map.size() == 1;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT;
    }

    @Test
    public void testRemove2To1WithNoMapOnRHS()
    {
        testRemove2To1WithNoMapOnRHSHelper("key1");
        testRemove2To1WithNoMapOnRHSHelper("bingo");
    }

    private void testRemove2To1WithNoMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
            protected int compactSize() { return 3; }
        };

        map.put("key1", "foo");
        map.put("key2", "bar");

        map.remove("key2");
        assert map.size() == 1;
        assert map.get("key1") == "foo";
    }

    @Test
    public void testRemove2To1WithMapOnRHS()
    {
        testRemove2To1WithMapOnRHSHelper("key1");
        testRemove2To1WithMapOnRHSHelper("bingo");
    }

    @SuppressWarnings("unchecked")
    private void testRemove2To1WithMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
            protected int compactSize() { return 3; }
        };

        map.put("key1", new TreeMap<>());
        map.put("key2", new ConcurrentSkipListMap<>());

        map.remove("key2");
        assert map.size() == 1;
        Map<String, Object> x = (Map<String, Object>) map.get("key1");
        assert x.size() == 0;
        assert x instanceof TreeMap;
    }

    @Test
    public void testEntrySet()
    {
        testEntrySetHelper("key1", 2);
        testEntrySetHelper("bingo", 2);
        testEntrySetHelper("key1", 3);
        testEntrySetHelper("bingo", 3);
        testEntrySetHelper("key1", 4);
        testEntrySetHelper("bingo", 4);
        testEntrySetHelper("key1", 5);
        testEntrySetHelper("bingo", 5);
    }

    private void testEntrySetHelper(final String singleKey, final int compactSize)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
            protected int compactSize() { return compactSize; }
        };
        
        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;
        assert map.put(null, null) == null;

        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        assert entrySet.size() == 5;

        // test contains() for success
        Map.Entry<String, Object> testEntry1 = new AbstractMap.SimpleEntry<String, Object>("key1", "foo");
        assert entrySet.contains(testEntry1);
        Map.Entry<String, Object> testEntry2 = new AbstractMap.SimpleEntry<String, Object>("key2", "bar");
        assert entrySet.contains(testEntry2);
        Map.Entry<String, Object> testEntry3 = new AbstractMap.SimpleEntry<String, Object>("key3", "baz");
        assert entrySet.contains(testEntry3);
        Map.Entry<String, Object> testEntry4 = new AbstractMap.SimpleEntry<String, Object>("key4", "qux");
        assert entrySet.contains(testEntry4);
        Map.Entry<String, Object> testEntry5 = new AbstractMap.SimpleEntry<>(null, null);
        assert entrySet.contains(testEntry5);

        // test contains() for fails
        assert !entrySet.contains("foo");
        Map.Entry<String, Object> bogus1 = new AbstractMap.SimpleEntry<String, Object>("key1", "fot");
        assert !entrySet.contains(bogus1);
        Map.Entry<String, Object> bogus4 = new AbstractMap.SimpleEntry<String, Object>("key4", "quz");
        assert !entrySet.contains(bogus4);
        Map.Entry<String, Object> bogus6 = new AbstractMap.SimpleEntry<String, Object>("key6", "quz");
        assert !entrySet.contains(bogus6);

        // test remove for fails()
        assert !entrySet.remove("fuzzy");

        Iterator<Map.Entry<String, Object>> i = entrySet.iterator();
        assert i.hasNext();

        entrySet.remove(testEntry5);
        entrySet.remove(testEntry4);
        entrySet.remove(testEntry3);
        entrySet.remove(testEntry2);
        entrySet.remove(testEntry1);

        assert entrySet.size() == 0;
        assert entrySet.isEmpty();
    }

    @Test
    public void testEntrySetIterator()
    {
        testEntrySetIteratorHelper("key1", 2);
        testEntrySetIteratorHelper("bingo", 2);
        testEntrySetIteratorHelper("key1", 3);
        testEntrySetIteratorHelper("bingo", 3);
        testEntrySetIteratorHelper("key1", 4);
        testEntrySetIteratorHelper("bingo", 4);
        testEntrySetIteratorHelper("key1", 5);
        testEntrySetIteratorHelper("bingo", 5);
    }

    private void testEntrySetIteratorHelper(final String singleKey, final int compactSize)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
            protected int compactSize() { return compactSize; }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;
        assert map.put(null, null) == null;

        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        assert entrySet.size() == 5;

        // test contains() for success
        Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();

        assert "key1".equals(iterator.next().getKey());
        iterator.remove();
        assert map.size() == 4;

        assert "key2".equals(iterator.next().getKey());
        iterator.remove();
        assert map.size() == 3;

        assert "key3".equals(iterator.next().getKey());
        iterator.remove();
        assert map.size() == 2;

        assert "key4".equals(iterator.next().getKey());
        iterator.remove();
        assert map.size() == 1;

        assert null == iterator.next().getKey();
        iterator.remove();
        assert map.size() == 0;
    }

    @Test
    public void testEntrySetIteratorHardWay()
    {
        testEntrySetIteratorHardWayHelper("key1", 2);
        testEntrySetIteratorHardWayHelper("bingo", 2);
        testEntrySetIteratorHardWayHelper("key1", 3);
        testEntrySetIteratorHardWayHelper("bingo", 3);
        testEntrySetIteratorHardWayHelper("key1", 4);
        testEntrySetIteratorHardWayHelper("bingo", 4);
        testEntrySetIteratorHardWayHelper("key1", 5);
        testEntrySetIteratorHardWayHelper("bingo", 5);
    }

    private void testEntrySetIteratorHardWayHelper(final String singleKey, final int compactSize)
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return compactSize; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;
        assert map.put(null, null) == null;

        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        assert entrySet.size() == 5;

        // test contains() for success
        Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();
        assert iterator.hasNext();
        iterator.next();
        assert iterator.hasNext();
        iterator.next();
        assert iterator.hasNext();
        iterator.next();
        assert iterator.hasNext();
        iterator.next();
        assert iterator.hasNext();
        iterator.next();
        assert !iterator.hasNext();
        iterator.remove();
        assert !iterator.hasNext();
        assert map.size() == 4;

        iterator = entrySet.iterator();
        assert iterator.hasNext();
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.remove();
        assert map.size() == 3;

        iterator = entrySet.iterator();
        assert iterator.hasNext();
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.remove();
        assert map.size() == 2;

        iterator = entrySet.iterator();
        assert iterator.hasNext();
        iterator.next();
        iterator.next();
        iterator.remove();
        assert map.size() == 1;

        iterator = entrySet.iterator();
        assert iterator.hasNext();
        iterator.next();
        iterator.remove();
        assert map.size() == 0;

        iterator = entrySet.iterator();
        assert !iterator.hasNext();
        try
        {
            iterator.remove();
            fail();
        }
        catch (IllegalStateException ignore) { }

        try
        {
            iterator.next();
        }
        catch (NoSuchElementException ignore) { }
        assert map.size() == 0;
    }

    @Test
    public void testCompactEntry()
    {
        CompactMap<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("foo", "bar") == null;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
    }

    @Test
    public void testEntrySetClear()
    {
        Map<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        map.entrySet().clear();
        assert map.size() == 0;
    }

    @Test
    public void testUsingCompactEntryWhenMapOnRHS()
    {
        CompactMap<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("key1", new TreeMap<>());
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;

        map.put("key1", 75.0d);
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT;
    }

    @Test
    public void testEntryValueOverwrite()
    {
        testEntryValueOverwriteHelper("key1");
        testEntryValueOverwriteHelper("bingo");
    }

    private void testEntryValueOverwriteHelper(final String singleKey)
    {
        CompactMap<String, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("key1", 9);
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            entry.setValue(16);
        }

        assert 16 == (int) map.get("key1");
    }
    
    @Test
    public void testEntryValueOverwriteMultiple()
    {
        testEntryValueOverwriteMultipleHelper("key1");
        testEntryValueOverwriteMultipleHelper("bingo");
    }

    private void testEntryValueOverwriteMultipleHelper(final String singleKey)
    {
        CompactMap<String, Integer> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, Integer> getNewMap() { return new LinkedHashMap<>(); }
        };

        for (int i=1; i <= 10; i++)
        {
            map.put("key" + i, i * 2);
        }

        int i=1;
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, Integer> entry = iterator.next();
            assert entry.getKey().equals("key" + i);
            assert entry.getValue() == i * 2;       // all values are even
            entry.setValue(i * 2 - 1);
            i++;
        }

        i=1;
        iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, Integer> entry = iterator.next();
            assert entry.getKey().equals("key" + i);
            assert entry.getValue() == i * 2 - 1;       // all values are now odd
            i++;
        }
    }

    @Test
    public void testHashCodeAndEquals()
    {
        testHashCodeAndEqualsHelper("key1");
        testHashCodeAndEqualsHelper("bingo");
    }

    private void testHashCodeAndEqualsHelper(final String singleKey)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected int compactSize() { return 3; }
            protected Map<String, String> getNewMap() { return new LinkedHashMap<>(); }
        };

        // intentionally using LinkedHashMap and TreeMap
        Map<String, String> other = new TreeMap<>();
        assert map.hashCode() == other.hashCode();
        assert map.equals(other);

        map.put("key1", "foo");
        other.put("key1", "foo");
        assert map.hashCode() == other.hashCode();
        assert map.equals(other);

        map.put("key2", "bar");
        other.put("key2", "bar");
        assert map.hashCode() == other.hashCode();
        assert map.equals(other);

        map.put("key3", "baz");
        other.put("key3", "baz");
        assert map.hashCode() == other.hashCode();
        assert map.equals(other);

        map.put("key4", "qux");
        other.put("key4", "qux");
        assert map.hashCode() == other.hashCode();
        assert map.equals(other);

        assert !map.equals(null);
        assert !map.equals(Collections.emptyMap());
    }

    @Test
    public void testCaseInsensitiveMap()
    {
        CompactMap<String, Integer> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, Integer> getNewMap() { return new CaseInsensitiveMap<>(); }
            protected int compactSize() { return 3; }
            protected boolean isCaseInsensitive() { return true; }
        };

        map.put("Key1", 0);
        map.put("Key2", 0);
        assert map.containsKey("key1");
        assert map.containsKey("key2");

        map.put("Key1", 0);
        map.put("Key2", 0);
        map.put("Key3", 0);
        assert map.containsKey("key1");
        assert map.containsKey("key2");
        assert map.containsKey("key3");

        map.put("Key1", 0);
        map.put("Key2", 0);
        map.put("Key3", 0);
        map.put("Key4", 0);
        assert map.containsKey("key1");
        assert map.containsKey("key2");
        assert map.containsKey("key3");
        assert map.containsKey("key4");
    }

    @Test
    public void testNullHandling()
    {
        testNullHandlingHelper("key1");
        testNullHandlingHelper("bingo");
    }

    private void testNullHandlingHelper(final String singleKey)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected int compactSize() { return 3; }
            protected Map<String, String> getNewMap() { return new CaseInsensitiveMap<>(); }
        };

        map.put("key1", null);
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey("key1");

        map.remove("key1");
        assert map.size() == 0;
        assert map.isEmpty();

        map.put(null, "foo");
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey(null);
        assert "foo" == map.get(null);
        assert map.remove(null) == "foo";
    }

    @Test
    public void testCaseInsensitive()
    {
        testCaseInsensitiveHelper("key1");
        testCaseInsensitiveHelper("bingo");
    }

    private void testCaseInsensitiveHelper(final String singleKey)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, String> getNewMap() { return new CaseInsensitiveMap<>(); }
            protected boolean isCaseInsensitive() { return true; }
        };

        // Case insensitive
        map.put("KEY1", null);
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey("key1");

        if (singleKey.equals("key1"))
        {
            assert map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT;
        }
        else
        {
            assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
        }

        map.remove("key1");
        assert map.size() == 0;
        assert map.isEmpty();

        map.put(null, "foo");
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey(null);
        assert "foo" == map.get(null);
        assert map.remove(null) == "foo";

        map.put("Key1", "foo");
        map.put("KEY2", "bar");
        map.put("KEY3", "baz");
        map.put("KEY4", "qux");
        assert map.size() == 4;

        assert map.containsKey("KEY1");
        assert map.containsKey("KEY2");
        assert map.containsKey("KEY3");
        assert map.containsKey("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY1").equals("foo");
        assert map.get("KEY2").equals("bar");
        assert map.get("KEY3").equals("baz");
        assert map.get("KEY4").equals("qux");

        map.remove("KEY1");
        assert map.size() == 3;
        assert map.containsKey("KEY2");
        assert map.containsKey("KEY3");
        assert map.containsKey("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY2").equals("bar");
        assert map.get("KEY3").equals("baz");
        assert map.get("KEY4").equals("qux");

        map.remove("KEY2");
        assert map.size() == 2;
        assert map.containsKey("KEY3");
        assert map.containsKey("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY3").equals("baz");
        assert map.get("KEY4").equals("qux");

        map.remove("KEY3");
        assert map.size() == 1;
        assert map.containsKey("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY4").equals("qux");

        map.remove("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);
        assert map.size() == 0;
    }

    @Test
    public void testCaseInsensitiveHardWay()
    {
        testCaseInsensitiveHardwayHelper("key1");
        testCaseInsensitiveHardwayHelper("bingo");
    }

    private void testCaseInsensitiveHardwayHelper(final String singleKey)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, String> getNewMap() { return new CaseInsensitiveMap<>(); }
            protected int compactSize() { return 3; }
            protected boolean isCaseInsensitive() { return true; }
        };

        // Case insensitive
        map.put("Key1", null);
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey("key1");

        map.remove("key1");
        assert map.size() == 0;
        assert map.isEmpty();

        map.put(null, "foo");
        assert map.size() == 1;
        assert !map.isEmpty();
        assert map.containsKey(null);
        assert "foo".equals(map.get(null));
        assert map.remove(null).equals("foo");

        map.put("KEY1", "foo");
        map.put("KEY2", "bar");
        map.put("KEY3", "baz");
        map.put("KEY4", "qux");

        assert map.containsKey("KEY1");
        assert map.containsKey("KEY2");
        assert map.containsKey("KEY3");
        assert map.containsKey("KEY4");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY1").equals("foo");
        assert map.get("KEY2").equals("bar");
        assert map.get("KEY3").equals("baz");
        assert map.get("KEY4").equals("qux");

        map.remove("KEY4");
        assert map.size() == 3;
        assert map.containsKey("KEY1");
        assert map.containsKey("KEY2");
        assert map.containsKey("KEY3");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY1").equals("foo");
        assert map.get("KEY2").equals("bar");
        assert map.get("KEY3").equals("baz");

        map.remove("KEY3");
        assert map.size() == 2;
        assert map.containsKey("KEY1");
        assert map.containsKey("KEY2");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY1").equals("foo");
        assert map.get("KEY2").equals("bar");

        map.remove("KEY2");
        assert map.size() == 1;
        assert map.containsKey("KEY1");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);

        assert map.get("KEY1").equals("foo");

        map.remove("KEY1");
        assert !map.containsKey(17.0d);
        assert !map.containsKey(null);
        assert map.size() == 0;
    }

    @Test
    public void testCaseInsensitiveInteger()
    {
        testCaseInsensitiveIntegerHelper(16);
        testCaseInsensitiveIntegerHelper(99);
    }

    private void testCaseInsensitiveIntegerHelper(final Integer singleKey)
    {
        CompactMap<Integer, String> map= new CompactMap()
        {
            protected Integer getSingleValueKey() { return 16; }
            protected Map<Integer, String> getNewMap() { return new CaseInsensitiveMap<>(); }
            protected int compactSize() { return 3; }
            protected boolean isCaseInsensitive() { return true; }
        };

        map.put(16, "foo");
        assert map.containsKey(16);
        assert map.get(16).equals("foo");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        map.put(32, "bar");
        assert map.containsKey(32);
        assert map.get(32).equals("bar");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        assert map.remove(32).equals("bar");
        assert map.containsKey(16);
        assert map.get(16).equals("foo");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        assert map.remove(16).equals("foo");
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testCaseInsensitiveIntegerHardWay()
    {
        testCaseInsensitiveIntegerHardWayHelper(16);
        testCaseInsensitiveIntegerHardWayHelper(99);
    }

    private void testCaseInsensitiveIntegerHardWayHelper(final Integer singleKey)
    {
        CompactMap<Integer, String> map= new CompactMap()
        {
            protected Integer getSingleValueKey() { return 16; }
            protected Map<Integer, String> getNewMap() { return new CaseInsensitiveMap<>(); }
            protected int compactSize() { return 3; }
            protected boolean isCaseInsensitive() { return true; }
        };

        map.put(16, "foo");
        assert map.containsKey(16);
        assert map.get(16).equals("foo");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        map.put(32, "bar");
        assert map.containsKey(32);
        assert map.get(32).equals("bar");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        assert map.remove(16).equals("foo");
        assert map.containsKey(32);
        assert map.get(32).equals("bar");
        assert map.get("sponge bob") == null;
        assert map.get(null) == null;

        assert map.remove(32).equals("bar");
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testContains()
    {
        testContainsHelper("key1", 2);
        testContainsHelper("bingo", 2);
        testContainsHelper("key1", 3);
        testContainsHelper("bingo", 3);
        testContainsHelper("key1", 4);
        testContainsHelper("bingo", 4);
    }

    public void testContainsHelper(final String singleKey, final int size)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
            protected boolean isCaseInsensitive() { return false; }
            protected int compactSize() { return size; }
        };

        map.put("key1", "foo");
        map.put("key2", "bar");
        map.put("key3", "baz");
        map.put("key4", "qux");

        assert map.keySet().contains("key1");
        assert map.keySet().contains("key2");
        assert map.keySet().contains("key3");
        assert map.keySet().contains("key4");
        assert !map.keySet().contains("foot");
        assert !map.keySet().contains(null);

        assert map.values().contains("foo");
        assert map.values().contains("bar");
        assert map.values().contains("baz");
        assert map.values().contains("qux");
        assert !map.values().contains("foot");
        assert !map.values().contains(null);
        
        assert map.entrySet().contains(new AbstractMap.SimpleEntry<>("key1", "foo"));
        assert map.entrySet().contains(new AbstractMap.SimpleEntry<>("key2", "bar"));
        assert map.entrySet().contains(new AbstractMap.SimpleEntry<>("key3", "baz"));
        assert map.entrySet().contains(new AbstractMap.SimpleEntry<>("key4", "qux"));
        assert !map.entrySet().contains(new AbstractMap.SimpleEntry<>("foot", "shoe"));
        assert !map.entrySet().contains(new AbstractMap.SimpleEntry<>(null, null));
    }

    @Test
    public void testRetainOrder()
    {
        testRetainOrderHelper("key1", 2);
        testRetainOrderHelper("bingo", 2);
        testRetainOrderHelper("key1", 3);
        testRetainOrderHelper("bingo", 3);
        testRetainOrderHelper("key1", 4);
        testRetainOrderHelper("bingo", 4);
    }

    public void testRetainOrderHelper(final String singleKey, final int size)
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(); }
            protected boolean isCaseInsensitive() { return false; }
            protected int compactSize() { return size; }
        };

        Map<String, String> other = new TreeMap<>();
        Map<String, String> hash = new HashMap<>();
        Random random = new SecureRandom();
        for (int i= 0; i < 100; i++)
        {
            String randomKey = StringUtilities.getRandomString(random, 3, 8);
            map.put(randomKey, null);
            other.put(randomKey, null);
            hash.put(randomKey, null);
        }

        Iterator<String> i = map.keySet().iterator();
        Iterator<String> j = other.keySet().iterator();
        Iterator<String> k = hash.keySet().iterator();
        boolean differ = false;

        while (i.hasNext())
        {
            String a = i.next();
            String b = j.next();
            String c = k.next();
            assert a.equals(b);
            if (!a.equals(c))
            {
                differ = true;
            }
        }

        assert differ;
    }

    @Test
    public void testBadNoArgConstructor()
    {
        CompactMap<String, Object> map= new CompactMap();
        assert "key".equals(map.getSingleValueKey());
        assert map.getNewMap() instanceof HashMap;
        
        try
        {
            new CompactMap<String, Object>() { protected int compactSize() { return 1; } };
            fail();
        }
        catch (IllegalStateException ignored) { }
    }

    @Test
    public void testBadConstructor()
    {
        Map<String, String> tree = new TreeMap<>();
        tree.put("foo", "bar");
        tree.put("baz", "qux");
        Map<String, String> map = new CompactMap<>(tree);
        assert map.get("foo").equals("bar");
        assert map.get("baz").equals("qux");
        assert map.size() == 2;
    }

    @Test
    public void testEqualsDifferingInArrayPortion()
    {
        CompactMap<String, String> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
            protected boolean isCaseInsensitive() { return false; }
            protected int compactSize() { return 3; }
        };

        map.put("key1", "foo");
        map.put("key2", "bar");
        map.put("key3", "baz");
        Map<String, String> tree = new TreeMap<>(map);
        assert map.equals(tree);
        tree.put("key3", "qux");
        assert tree.size() == 3;
        assert !map.equals(tree);
        tree.remove("key3");
        tree.put("key4", "baz");
        assert tree.size() == 3;
        assert !map.equals(tree);
        tree.remove("key4");
        tree.put("key3", "baz");
        assert map.equals(tree);
    }

    @Test
    public void testIntegerKeysInDefaultMap()
    {
        CompactMap<Integer, Integer> map= new CompactMap();
        map.put(6, 10);
        Object key = map.getSingleValueKey();
        assert key instanceof String;   // "key" is the default
    }

    @Test
    public void testCaseInsensitiveEntries()
    {
        CompactMap<Object, Object> map = new CompactMap<Object, Object>()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 3; }
        };
        
        map.put("Key1", "foo");
        map.put("Key2", "bar");
        map.put("Key3", "baz");
        map.put("Key4", "qux");
        map.put("Key5", "quux");
        map.put("Key6", "quux");
        map.put(TimeZone.getDefault(), "garply");
        map.put(16, "x");
        map.put(29, "x");
        map.put(100, 200);
        map.put(null, null);
        TestUtil.assertContainsIgnoreCase(map.toString(), "Key1", "foo", "ZoneInfo");

        Map<Object, Object> map2 = new LinkedHashMap<>();
        map2.put("KEy1", "foo");
        map2.put("KEy2", "bar");
        map2.put("KEy3", "baz");
        map2.put(TimeZone.getDefault(), "qux");
        map2.put("Key55", "quux");
        map2.put("Key6", "xuuq");
        map2.put("Key7", "garply");
        map2.put("Key8", "garply");
        map2.put(29, "garply");
        map2.put(100, 200);
        map2.put(null, null);

        List<Boolean> answers = Arrays.asList(new Boolean[] {true, true, true, false, false, false, false, false, false, true, true });
        assert answers.size() == map.size();
        assert map.size() == map2.size();

        Iterator<Map.Entry<Object, Object>> i = map.entrySet().iterator();
        Iterator<Map.Entry<Object, Object>> j = map2.entrySet().iterator();
        Iterator<Boolean> k = answers.iterator();

        while (i.hasNext())
        {
            Map.Entry<Object, Object> entry1 = i.next();
            Map.Entry<Object, Object> entry2 = j.next();
            Boolean answer = k.next();
            assert Objects.equals(answer, entry1.equals(entry2));
        }
    }

    @Test
    public void testCompactLinkedMap()
    {
        // Ensure CompactLinkedMap is minimally exercised.
        CompactMap<String, Integer> linkedMap = new CompactLinkedMap<>();

        for (int i=0; i < linkedMap.compactSize() + 5; i++)
        {
            linkedMap.put("FoO" + i, i);
        }

        assert linkedMap.containsKey("FoO0");
        assert !linkedMap.containsKey("foo0");
        assert linkedMap.containsKey("FoO1");
        assert !linkedMap.containsKey("foo1");
        assert linkedMap.containsKey("FoO" + (linkedMap.compactSize() + 3));
        assert !linkedMap.containsKey("foo" + (linkedMap.compactSize() + 3));

        CompactMap<String, Integer> copy = new CompactLinkedMap<>(linkedMap);
        assert copy.equals(linkedMap);

        assert copy.containsKey("FoO0");
        assert !copy.containsKey("foo0");
        assert copy.containsKey("FoO1");
        assert !copy.containsKey("foo1");
        assert copy.containsKey("FoO" + (copy.compactSize() + 3));
        assert !copy.containsKey("foo" + (copy.compactSize() + 3));
    }

    @Test
    public void testCompactCIHashMap()
    {
        // Ensure CompactLinkedMap is minimally exercised.
        CompactMap<String, Integer> ciHashMap = new CompactCIHashMap<>();

        for (int i=0; i < ciHashMap.compactSize() + 5; i++)
        {
            ciHashMap.put("FoO" + i, i);
        }

        assert ciHashMap.containsKey("FoO0");
        assert ciHashMap.containsKey("foo0");
        assert ciHashMap.containsKey("FoO1");
        assert ciHashMap.containsKey("foo1");
        assert ciHashMap.containsKey("FoO" + (ciHashMap.compactSize() + 3));
        assert ciHashMap.containsKey("foo" + (ciHashMap.compactSize() + 3));

        CompactMap<String, Integer> copy = new CompactCIHashMap<>(ciHashMap);
        assert copy.equals(ciHashMap);

        assert copy.containsKey("FoO0");
        assert copy.containsKey("foo0");
        assert copy.containsKey("FoO1");
        assert copy.containsKey("foo1");
        assert copy.containsKey("FoO" + (copy.compactSize() + 3));
        assert copy.containsKey("foo" + (copy.compactSize() + 3));
    }

    @Test
    public void testCompactCILinkedMap()
    {
        // Ensure CompactLinkedMap is minimally exercised.
        CompactMap<String, Integer> ciLinkedMap = new CompactCILinkedMap<>();

        for (int i=0; i < ciLinkedMap.compactSize() + 5; i++)
        {
            ciLinkedMap.put("FoO" + i, i);
        }

        assert ciLinkedMap.containsKey("FoO0");
        assert ciLinkedMap.containsKey("foo0");
        assert ciLinkedMap.containsKey("FoO1");
        assert ciLinkedMap.containsKey("foo1");
        assert ciLinkedMap.containsKey("FoO" + (ciLinkedMap.compactSize() + 3));
        assert ciLinkedMap.containsKey("foo" + (ciLinkedMap.compactSize() + 3));

        CompactMap<String, Integer> copy = new CompactCILinkedMap<>(ciLinkedMap);
        assert copy.equals(ciLinkedMap);

        assert copy.containsKey("FoO0");
        assert copy.containsKey("foo0");
        assert copy.containsKey("FoO1");
        assert copy.containsKey("foo1");
        assert copy.containsKey("FoO" + (copy.compactSize() + 3));
        assert copy.containsKey("foo" + (copy.compactSize() + 3));
    }

    @Test
    public void testCaseInsensitiveEntries2()
    {
        CompactMap<Object, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 3; }
        };

        map.put("Key1", "foo");
        
        Iterator<Map.Entry<Object, Object>> i = map.entrySet().iterator();
        Map.Entry<Object, Object> entry = i.next();
        assert !entry.equals(TimeZone.getDefault());
    }

    @Test
    public void testIdentityEquals()
    {
        Map<String, Object> compact= new CompactMap();
        compact.put("foo", "bar");
        assert compact.equals(compact);
    }

    @Test
    public void testCI()
    {
        CompactMap<Object, Object> map= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");
        map.put("thREe", "foo");
        assert map.size() == 3;
    }

    @Test
    public void testWrappedTreeMap()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(String.CASE_INSENSITIVE_ORDER); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("z", "zulu");
        m.put("J", "juliet");
        m.put("a", "alpha");
        assert m.size() == 3;
        Iterator i = m.keySet().iterator();
        assert "a" == i.next();
        assert "J" == i.next();
        assert "z" == i.next();
        assert m.containsKey("A");
        assert m.containsKey("j");
        assert m.containsKey("Z");
    }

    @Test
    public void testMultipleSortedKeysetIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(String.CASE_INSENSITIVE_ORDER); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("z", "zulu");
        m.put("J", "juliet");
        m.put("a", "alpha");
        assert m.size() == 3;

        Set<String> keyset = m.keySet();
        Iterator<String> iter1 = keyset.iterator();
        Iterator<String> iter2 = keyset.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "a".equals(iter1.next());
        assert "a".equals(iter2.next());

        assert "J".equals(iter2.next());
        assert "J".equals(iter1.next());

        assert "z".equals(iter1.next());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "z" == iter2.next();
        assert false == iter2.hasNext();
    }

    @Test
    public void testMultipleSortedValueIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(String.CASE_INSENSITIVE_ORDER); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("z", "zulu");
        m.put("J", "juliet");
        m.put("a", "alpha");
        assert m.size() == 3;

        Collection<String> values = m.values();
        Iterator<String> iter1 = values.iterator();
        Iterator<String> iter2 = values.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "alpha".equals(iter1.next());
        assert "alpha".equals(iter2.next());

        assert "juliet".equals(iter2.next());
        assert "juliet".equals(iter1.next());

        assert "zulu".equals(iter1.next());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "zulu".equals(iter2.next());
        assert false == iter2.hasNext();
    }

    @Test
    public void testMultipleSortedEntrySetIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(String.CASE_INSENSITIVE_ORDER); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("z", "zulu");
        m.put("J", "juliet");
        m.put("a", "alpha");
        assert m.size() == 3;

        Set<Map.Entry<String,String>> entrySet = m.entrySet();
        Iterator<Map.Entry<String,String>> iter1 = entrySet.iterator();
        Iterator<Map.Entry<String,String>> iter2 = entrySet.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "a".equals(iter1.next().getKey());
        assert "a".equals(iter2.next().getKey());

        assert "juliet".equals(iter2.next().getValue());
        assert "juliet".equals(iter1.next().getValue());

        assert "z".equals(iter1.next().getKey());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "zulu".equals(iter2.next().getValue());
        assert false == iter2.hasNext();
    }

    @Test
    public void testMultipleNonSortedKeysetIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("a", "alpha");
        m.put("J", "juliet");
        m.put("z", "zulu");
        assert m.size() == 3;

        Set<String> keyset = m.keySet();
        Iterator<String> iter1 = keyset.iterator();
        Iterator<String> iter2 = keyset.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "a".equals(iter1.next());
        assert "a".equals(iter2.next());

        assert "J".equals(iter2.next());
        assert "J".equals(iter1.next());

        assert "z".equals(iter1.next());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "z".equals(iter2.next());
        assert false == iter2.hasNext();
    }

    @Test
    public void testMultipleNonSortedValueIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("a", "alpha");
        m.put("J", "juliet");
        m.put("z", "zulu");
        assert m.size() == 3;

        Collection<String> values = m.values();
        Iterator<String> iter1 = values.iterator();
        Iterator<String> iter2 = values.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "alpha".equals(iter1.next());
        assert "alpha".equals(iter2.next());

        assert "juliet".equals(iter2.next());
        assert "juliet".equals(iter1.next());

        assert "zulu".equals(iter1.next());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "zulu".equals(iter2.next());
        assert false == iter2.hasNext();
    }

    @Test
    public void testMultipleNonSortedEntrySetIterators()
    {
        CompactMap<String, String> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("a", "alpha");
        m.put("J", "juliet");
        m.put("z", "zulu");
        assert m.size() == 3;

        Set<Map.Entry<String,String>> entrySet = m.entrySet();
        Iterator<Map.Entry<String,String>> iter1 = entrySet.iterator();
        Iterator<Map.Entry<String,String>> iter2 = entrySet.iterator();

        assert iter1.hasNext();
        assert iter2.hasNext();

        assert "a".equals(iter1.next().getKey());
        assert "a".equals(iter2.next().getKey());

        assert "juliet".equals(iter2.next().getValue());
        assert "juliet".equals(iter1.next().getValue());

        assert "z".equals(iter1.next().getKey());
        assert false == iter1.hasNext();
        assert true == iter2.hasNext();

        assert "zulu".equals(iter2.next().getValue());
        assert false == iter2.hasNext();
    }

    @Test
    public void testKeySetRemoveAll2()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        
        Set<Object> s = m.keySet();
        Set<Object> items = new HashSet<>();
        items.add("one");
        items.add("five");
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add("dog");
        s.removeAll(items);
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));
    }

    @Test
    public void testEntrySetContainsAll()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        
        Set<Map.Entry<Object, Object>> s = m.entrySet();
        Set<Map.Entry<Object, Object>> items = new HashSet<>();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("thRee", "Four"));
        assertTrue(s.containsAll(items));

        items = new HashSet<>();
        items.add(getEntry("one", "two"));
        items.add(getEntry("thRee", "Four"));
        assertFalse(s.containsAll(items));
    }

    @Test
    public void testEntrySetRemoveAll()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");

        Set<Map.Entry<Object, Object>> s = m.entrySet();
        Set<Map.Entry<Object, Object>> items = new HashSet<>();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("five", "Six"));
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("dog", "Two"));
        assertFalse(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("three", "Four"));
        assertTrue(s.removeAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetRetainAll()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        Set<Map.Entry<Object, Object>> s = m.entrySet();
        Set<Object> items = new HashSet<>();
        items.add(getEntry("three", "Four"));
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add("dog");
        assertTrue(s.retainAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testPutAll2()
    {
        CompactMap<String, Object> stringMap= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        stringMap.put("Five", "Six");
        CompactCILinkedMap<String, Object> newMap = new CompactCILinkedMap<>();
        newMap.put("thREe", "four");
        newMap.put("Seven", "Eight");

        stringMap.putAll(newMap);

        assertEquals(4, stringMap.size());
        assertNotEquals("two", stringMap.get("one"));
        assertEquals("Six", stringMap.get("fIvE"));
        assertEquals("four", stringMap.get("three"));
        assertEquals("Eight", stringMap.get("seven"));

        CompactMap<String, Object> a= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };
        a.putAll(null);     // Ensure NPE not happening
    }

    @Test
    public void testKeySetRetainAll2()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        Set<Object> s = m.keySet();
        Set<Object> items = new HashSet<>();
        items.add("three");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        s = m.keySet();
        items.clear();
        items.add("dog");
        items.add("one");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("one"));
        assertTrue(m.containsKey("one"));
    }

    @Test
    public void testEqualsWithNullOnRHS()
    {
        // Must have 2 entries and <= compactSize() in the 2 maps:
        Map<String, Object> compact= new CompactMap();
        compact.put("foo", null);
        compact.put("bar", null);
        assert compact.hashCode() != 0;
        Map<String, Object> compact2= new CompactMap();
        compact2.put("foo", null);
        compact2.put("bar", null);
        assert compact.equals(compact2);

        compact.put("foo", "");
        assert !compact.equals(compact2);

        compact2.put("foo", "");
        compact.put("foo", null);
        assert compact.hashCode() != 0;
        assert compact2.hashCode() != 0;
        assert !compact.equals(compact2);
    }

    @Test
    public void testToStringOnEmptyMap()
    {
        Map<String, Object> compact= new CompactMap();
        assert compact.toString().equals("{}");
    }

    @Test
    public void testToStringDoesNotRecurseInfinitely()
    {
        Map<Object, Object> compact= new CompactMap();
        compact.put("foo", compact);
        assert compact.toString() != null;
        assert compact.toString().contains("this Map");

        compact.put(compact, "foo");
        assert compact.toString() != null;

        compact.put(compact, compact);
        assert compact.toString() != null;

        assert new HashMap().hashCode() == new CompactMap<>().hashCode();

        compact.clear();
        compact.put("bar", compact);
        assert compact.toString() != null;
        assert compact.toString().contains("this Map");

        compact.put(compact, "bar");
        assert compact.toString() != null;

        compact.put(compact, compact);
        assert compact.toString() != null;
    }

    @Test
    public void testEntrySetKeyInsensitive()
    {
        CompactMap<String, Object> m = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        
        int one = 0;
        int three = 0;
        int five = 0;
        for (Map.Entry<String, Object> entry : m.entrySet())
        {
            if (entry.equals(new AbstractMap.SimpleEntry<>("one", "Two")))
            {
                one++;
            }
            if (entry.equals(new AbstractMap.SimpleEntry<>("thrEe", "Four")))
            {
                three++;
            }
            if (entry.equals(new AbstractMap.SimpleEntry<>("FIVE", "Six")))
            {
                five++;
            }
        }

        assertEquals(1, one);
        assertEquals(1, three);
        assertEquals(1, five);
    }

    @Test
    public void testEntrySetEquals()
    {
        CompactMap<Object, Object> m= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<Object, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        
        Set<Map.Entry<Object, Object>> s = m.entrySet();
        Set<Map.Entry<Object, Object>> s2 = new HashSet<>();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "Six"));
        assertEquals(s, s2);

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "six"));    // lowercase six
        assertNotEquals(s, s2);

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Thre", "Four"));   // missing 'e' on three
        s2.add(getEntry("Five", "Six"));
        assertNotEquals(s, s2);

        Set<Map.Entry<Object, Object>> s3 = new HashSet<>();
        s3.add(getEntry("one", "Two"));
        s3.add(getEntry("three", "Four"));
        s3.add(getEntry("five","Six"));
        assertEquals(s, s3);

        Set<Map.Entry<Object, Object>> s4 = new CaseInsensitiveSet<>();
        s4.add(getEntry("one", "Two"));
        s4.add(getEntry("three", "Four"));
        s4.add(getEntry("five","Six"));
        assertEquals(s, s4);

        CompactMap<String, Object> secondStringMap= new CompactMap()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        secondStringMap.put("One", "Two");
        secondStringMap.put("Three", "Four");
        secondStringMap.put("Five", "Six");
        assertNotEquals("one", s);

        assertEquals(s, secondStringMap.entrySet());
        // case-insensitive
        secondStringMap.put("five", "Six");
        assertEquals(s, secondStringMap.entrySet());
        secondStringMap.put("six", "sixty");
        assertNotEquals(s, secondStringMap.entrySet());
        secondStringMap.remove("five");
        assertNotEquals(s, secondStringMap.entrySet());
        secondStringMap.put("five", null);
        secondStringMap.remove("six");
        assertNotEquals(s, secondStringMap.entrySet());
        m.put("five", null);
        assertEquals(m.entrySet(), secondStringMap.entrySet());
    }

    @Test
    public void testEntrySetHashCode()
    {
        CompactMap<String, Object> m = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };
        m.put("One", "Two");
        m.put("Three", "Four");
        m.put("Five", "Six");
        CompactMap<String, Object> m2 = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };
        m2.put("one", "Two");
        m2.put("three", "Four");
        m2.put("five", "Six");
        assertEquals(m.hashCode(), m2.hashCode());

        Map<String, Object> m3 = new LinkedHashMap<>();
        m3.put("One", "Two");
        m3.put("Three", "Four");
        m3.put("Five", "Six");
        assertNotEquals(m.hashCode(), m3.hashCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEntrySetHashCode2()
    {
        // Case-sensitive
        CompactMap.CompactMapEntry entry = new CompactMap().new CompactMapEntry("One", "Two");
        AbstractMap.SimpleEntry entry2 = new AbstractMap.SimpleEntry("One", "Two");
        assert entry.equals(entry2);
        assert entry.hashCode() == entry2.hashCode();

        // Case-insensitive
        CompactMap<String, Object> m = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "a"; }
            protected Map<String, Object> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
            protected boolean isCaseInsensitive() { return true; }
            protected int compactSize() { return 4; }
        };

        CompactMap.CompactMapEntry entry3 = m.new CompactMapEntry("One", "Two");
        assert entry.equals(entry3);
        assert entry.hashCode() != entry3.hashCode();

        entry3 = m.new CompactMapEntry("one", "Two");
        assert m.isCaseInsensitive();
        assert entry3.equals(entry);
        assert entry3.hashCode() != entry.hashCode();
    }

    @Test
    public void testUnmodifiability()
    {
        CompactMap<String, Object> m = new CompactCIHashMap<>();
        m.put("foo", "bar");
        m.put("baz", "qux");
        Map<String, Object> noModMap = Collections.unmodifiableMap(m);
        assert noModMap.containsKey("FOO");
        assert noModMap.containsKey("BAZ");

        try
        {
            noModMap.put("Foo", 9);
            fail();
        }
        catch(UnsupportedOperationException e) { }
    }

    @Test
    public void testCompactCIHashMap2()
    {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();

        for (int i=0; i < map.compactSize() + 10; i++)
        {
            map.put("" + i, i);
        }
        assert map.containsKey("0");
        assert map.containsKey("" + (map.compactSize() + 1));
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;    // ensure switch over
    }

    /**
     * Test to demonstrate that if sortCompactArray is flawed and sorts keys without rearranging values,
     * key-value pairs become mismatched.
     */
    @Test
    void testSortCompactArrayMismatchesKeysAndValues() throws Exception {
        // Create a CompactMap with a specific singleValueKey and compactSize
        Map<String, Object> options = new HashMap<>();

        options.put(COMPACT_SIZE, 40);
        options.put(CASE_SENSITIVE, true);
        options.put(MAP_TYPE, TreeMap.class);
        options.put(ORDERING, SORTED);
        CompactMap<String, Integer> compactMap = CompactMap.newMap(options);

        // Insert multiple entries
        compactMap.put("banana", 2);
        compactMap.put("apple", 1);
        compactMap.put("cherry", 3);
        compactMap.put("zed", 4);

        // Verify initial entries
        assertEquals(2, compactMap.get("banana"), "Initial value for 'banana' should be 2.");
        assertEquals(1, compactMap.get("apple"), "Initial value for 'apple' should be 1.");
        assertEquals(3, compactMap.get("cherry"), "Initial value for 'cherry' should be 3.");
        assertEquals(4, compactMap.get("zed"), "Initial value for 'zed' should be 4.");
    }
    
    @EnabledIf("com.cedarsoftware.util.TestUtil#isReleaseMode")
    @Test
    public void testPerformance()
    {
        int maxSize = 1000;
        final int[] compactSize = new int[1];
        int lower = 5;
        int upper = 140;
        long totals[] = new long[upper - lower + 1];

        for (int x = 0; x < 2000; x++)
        {
            for (int i = lower; i < upper; i++)
            {
                compactSize[0] = i;
                CompactMap<String, Integer> map= new CompactMap()
                {
                    protected String getSingleValueKey()
                    {
                        return "key1";
                    }

                    protected Map<String, Integer> getNewMap()
                    {
                        return new HashMap<>();
                    }

                    protected boolean isCaseInsensitive()
                    {
                        return false;
                    }

                    protected int compactSize()
                    {
                        return compactSize[0];
                    }
                };

                long start = System.nanoTime();
                // ===== Timed
                for (int j = 0; j < maxSize; j++)
                {
                    map.put("" + j, j);
                }

                for (int j = 0; j < maxSize; j++)
                {
                    map.get("" + j);
                }

                Iterator iter = map.keySet().iterator();
                while (iter.hasNext())
                {
                    iter.next();
                    iter.remove();
                }
                // ===== End Timed
                long end = System.nanoTime();
                totals[i - lower] += end - start;
            }

            Map<String, Integer> map = new HashMap<>();
            long start = System.nanoTime();
            // ===== Timed
            for (int i = 0; i < maxSize; i++)
            {
                map.put("" + i, i);
            }

            for (int i = 0; i < maxSize; i++)
            {
                map.get("" + i);
            }

            Iterator iter = map.keySet().iterator();
            while (iter.hasNext())
            {
                iter.next();
                iter.remove();
            }
            // ===== End Timed
            long end = System.nanoTime();
            totals[totals.length - 1] += end - start;
        }
        for (int i = lower; i < upper; i++)
        {
            System.out.println("CompacMap.compactSize: " + i + " = " + totals[i - lower] / 1000000.0d);
        }
        System.out.println("HashMap = " + totals[totals.length - 1] / 1000000.0d);
    }

    private Map.Entry<Object, Object> getEntry(final Object key, final Object value)
    {
        return new Map.Entry<Object, Object>()
        {
            Object myValue = value;

            public Object getKey()
            {
                return key;
            }

            public Object getValue()
            {
                return value;
            }

            public Object setValue(Object value)
            {
                Object save = myValue;
                myValue = value;
                return save;
            }
        };
    }
}
