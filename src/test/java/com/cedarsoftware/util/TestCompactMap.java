package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.fail;

/**
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */

public class TestCompactMap
{
    @Test
    public void testSizeAndEmpty()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
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

        assert map.size() == 0;
        assert map.isEmpty();
        assert map.put("value", 10.0d) == null;
        assert map.size() == 1;
        assert !map.isEmpty();

        assert map.put("alpha", "beta") == null;
        assert map.size() == 2;
        assert !map.isEmpty();

        assert map.remove("alpha") == "beta";
        assert map.size() == 1;
        assert !map.isEmpty();

        assert 10.0d == (Double) map.remove("value");
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testSizeAndEmptyHardOrder()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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
    }

    @Test
    public void testContainsValueHardOrder()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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
        Map<String, Object> map = new CompactMap<String, Object>()
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

        assert map.put("value", "foo") == null;
        assert map.get("value") == "foo";
        assert map.put("value", "bar") == "foo";
        assert map.get("value") == "bar";
        assert map.size() == 1;
    }

    @Test
    public void testPutWithManyEntries()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }

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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }

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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }

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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }

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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "foo";
            }

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
        Map<String, Object> map = new CompactMap<String, Object>()
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
    }

    @Test
    public void testPutAll()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
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

        Map source = new TreeMap();
        map.putAll(source);
        assert map.isEmpty();

        source = new TreeMap();
        source.put("qux", "delta");

        map.putAll(source);
        assert map.size() == 1;
        assert map.containsKey("qux");
        assert map.containsValue("delta");

        source = new TreeMap();
        source.put("qux", "delta");
        source.put("baz", "charlie");

        map.putAll(source);
        assert map.size() == 2;
        assert map.containsKey("qux");
        assert map.containsKey("baz");
        assert map.containsValue("delta");
        assert map.containsValue("charlie");

        source = new TreeMap();
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
        Map<String, Object> map = new CompactMap<String, Object>()
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

        assert map.put("foo", "bar") == null;
        assert map.size() == 1;
        map.clear();
        assert map.size() == 0;
        assert map.isEmpty();
    }

    @Test
    public void testKeySetEmpty()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }

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
        catch (IllegalStateException e)
        {
        }
    }

    @Test
    public void testKeySet1Item()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }

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
        catch (NoSuchElementException e)
        {
        }

        assert map.put("key1", "bar") == "foo";
        i = map.keySet().iterator();
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySet1ItemHardWay()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }

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
        catch (NoSuchElementException e)
        {
        }

        assert map.put("key9", "bar") == "foo";
        i = map.keySet().iterator();
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySetMultiItem()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }

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
        assert i.next() == "key1";
        assert i.hasNext();
        assert i.next() == "key2";
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException e)
        {
        }

        assert map.put("key1", "baz") == "foo";
        assert map.put("key2", "qux") == "bar";

        i = map.keySet().iterator();
        assert i.next() == "key1";
        i.remove();
        assert i.next() == "key2";
        i.remove();
        assert map.isEmpty();
    }

    @Test
    public void testKeySetMultiItem2()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey()
            {
                return "key1";
            }

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
        assert i.next() == "key1";
        assert i.hasNext();
        assert i.next() == "key2";
        try
        {
            i.next();
            fail();
        }
        catch (NoSuchElementException e)
        {
        }

        assert map.put("key1", "baz") == "foo";
        assert map.put("key2", "qux") == "bar";

        i = map.keySet().iterator();
        assert i.next() == "key1";
        assert i.next() == "key2";
        i.remove();
        assert map.size() == 1;
        assert map.keySet().contains("key1");
        i.remove();
        assert map.isEmpty();

        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException e)
        {
        }
    }

    @Test
    public void testKeySetMultiItemReverseRemove()
    {
        testKeySetMultiItemReverseRemoveHelper("key1");
        testKeySetMultiItemReverseRemoveHelper("bingo");
    }

    private void testKeySetMultiItemReverseRemoveHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Set<String> keys = map.keySet();
        Iterator<String> i = keys.iterator();

        String key = i.next();
        assert key == "key1";
        assert map.get("key1") == "foo";
        i.remove();
        assert !map.containsKey("key1");
        assert map.size() == 3;

        key = i.next();
        assert key == "key2";
        assert map.get("key2") == "bar";
        i.remove();
        assert !map.containsKey("key2");
        assert map.size() == 2;

        key = i.next();
        assert key == "key3";
        assert map.get("key3") == "baz";
        i.remove();
        assert !map.containsKey("key3");
        assert map.size() == 1;

        key = i.next();
        assert key == "key4";
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        assert keys[0] == "key1";
        assert keys[1] == "key2";
        assert keys[2] == "key3";
        
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
        assert keys[0] == "key1";
        assert keys[1] == "key2";
        assert map.size() == 2;

        assert map.remove("key2") == "bar";
        set = map.keySet();
        keys = set.toArray(new String[]{});
        assert keys.length == 1;
        assert keys[0] == "key1";
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        Set<String> set = map.keySet();

        try
        {
            set.add("bingo");
            fail();
        }
        catch (UnsupportedOperationException e) { }

        try
        {
            Collection<String> col = new ArrayList<>();
            col.add("hey");
            col.add("jude");
            set.addAll(col);
            fail();
        }
        catch (UnsupportedOperationException e) { }
    }

    @Test
    public void testKeySetContainsAll()
    {
        testKeySetContainsAllHelper("key1");
        testKeySetContainsAllHelper("bingo");
    }

    private void testKeySetContainsAllHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "field"; }
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
        CompactMap<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Collection col = map.values();
        assert col.size() == 4;

        Iterator<Object> i = map.values().iterator();
        assert i.hasNext();
        assert i.next() == "foo";
        i.remove();
        assert map.size() == 3;
        assert col.size() == 3;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

        assert i.hasNext();
        assert i.next() == "bar";
        i.remove();
        assert map.size() == 2;
        assert col.size() == 2;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

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
        CompactMap<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("key1", "foo") == null;
        assert map.put("key2", "bar") == null;
        assert map.put("key3", "baz") == null;
        assert map.put("key4", "qux") == null;

        Collection col = map.values();
        assert col.size() == 4;

        Iterator<Object> i = map.values().iterator();
        i.next();
        i.next();
        i.next();
        i.next();
        i.remove();
        assert map.size() == 3;
        assert col.size() == 3;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

        i = map.values().iterator();
        i.next();
        i.next();
        i.next();
        i.remove();
        assert map.size() == 2;
        assert col.size() == 2;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.MAP;

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
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
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
        catch (NoSuchElementException e) { }

        i = map.values().iterator();
        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException e) { }

    }

    @Test
    public void testValuesClear()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
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

    private void testWithMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        Map map1 = new HashMap();
        map1.put("a", "alpha");
        map1.put("b", "bravo");
        map.put("key1", map1);

        Map x = (Map) map.get("key1");
        assert x instanceof HashMap;
        assert x.size() == 2;

        Map map2 = new HashMap();
        map2.put("a", "alpha");
        map2.put("b", "bravo");
        map2.put("c", "charlie");
        map.put("key2", map2);

        x = (Map) map.get("key2");
        assert x instanceof HashMap;
        assert x.size() == 3;

        Map map3 = new HashMap();
        map3.put("a", "alpha");
        map3.put("b", "bravo");
        map3.put("c", "charlie");
        map3.put("d", "delta");
        map.put("key3", map3);
        assert map.size() == 3;

        x = (Map) map.get("key3");
        assert x instanceof HashMap;
        assert x.size() == 4;

        assert map.remove("key3") instanceof Map;
        x = (Map) map.get("key2");
        assert x.size() == 3;
        assert map.size() == 2;

        assert map.remove("key2") instanceof Map;
        x = (Map) map.get("key1");
        assert x.size() == 2;
        assert map.size() == 1;

        map.remove("key1");
        assert map.size() == 0;
    }

    @Test
    public void testRemove2To1WithNoMapOnRHS()
    {
        testRemove2To1WithNoMapOnRHSHelper("key1");
        testRemove2To1WithNoMapOnRHSHelper("bingo");
    }

    private void testRemove2To1WithNoMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
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

    private void testRemove2To1WithMapOnRHSHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("key1", new TreeMap());
        map.put("key2", new ConcurrentSkipListMap());

        map.remove("key2");
        assert map.size() == 1;
        Map x = (Map) map.get("key1");
        assert x.size() == 0;
        assert x instanceof TreeMap;
    }

    @Test
    public void testEntrySet()
    {
        testEntrySetHelper("key1");
        testEntrySetHelper("bingo");
    }

    private void testEntrySetHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        testEntrySetIteratorHelper("key1");
        testEntrySetIteratorHelper("bingo");
    }

    private void testEntrySetIteratorHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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

        iterator.next();
        iterator.remove();
        assert map.size() == 4;

        iterator.next();
        iterator.remove();
        assert map.size() == 3;

        iterator.next();
        iterator.remove();
        assert map.size() == 2;

        iterator.next();
        iterator.remove();
        assert map.size() == 1;

        iterator.next();
        iterator.remove();
        assert map.size() == 0;
    }

    @Test
    public void testEntrySetIteratorHardWay()
    {
        testEntrySetIteratorHardWayHelper("key1");
        testEntrySetIteratorHardWayHelper("bingo");
    }

    private void testEntrySetIteratorHardWayHelper(final String singleKey)
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.remove();
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
        catch (IllegalStateException e) { }

        try
        {
            iterator.next();
        }
        catch (NoSuchElementException e) { }
        assert map.size() == 0;
    }

    @Test
    public void testCompactEntry()
    {
        CompactMap<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        assert map.put("foo", "bar") == null;
        assert map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY;
    }

    @Test
    public void testEntrySetClear()
    {
        Map<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
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
        CompactMap<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, Object> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("key1", new TreeMap());
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
        CompactMap<String, Object> map = new CompactMap<String, Object>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
        CompactMap<String, Integer> map = new CompactMap<String, Integer>()
        {
            protected String getSingleValueKey() { return singleKey; }
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
    public void testMinus()
    {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>()
        {
            protected String getSingleValueKey() { return "key1"; }
            protected Map<String, Integer> getNewMap() { return new LinkedHashMap<>(); }
        };

        try
        {
            map.minus(null);
            fail();
        }
        catch (UnsupportedOperationException e) {  }

        try
        {
            map.plus(null);
            fail();
        }
        catch (UnsupportedOperationException e) { }
    }

    @Test
    public void testHashCodeAndEquals()
    {
        testHashCodeAndEqualsHelper("key1");
        testHashCodeAndEqualsHelper("bingo");
    }

    private void testHashCodeAndEqualsHelper(final String singleKey)
    {
        CompactMap<String, String> map = new CompactMap<String, String>()
        {
            protected String getSingleValueKey() { return "key1"; }
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
}
