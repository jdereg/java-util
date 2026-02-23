package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.cedarsoftware.io.JsonIo;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.ARRAY_RESIZE;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.ARRAY_SET_ELEMENT;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.LIST_RESIZE;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.LIST_SET_ELEMENT;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.MAP_PUT;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.MAP_REMOVE;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.OBJECT_ORPHAN;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.SET_ADD;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.SET_REMOVE;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.fromName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for GraphComparator
 *
 * @author John DeRegnaucourt
 */
public class GraphComparatorTest
{
    private static final int SET_TYPE_HASH = 1;
    private static final int SET_TYPE_TREE = 2;
    private static final int SET_TYPE_LINKED = 3;
    public interface HasId
    {
        Object getId();
    }

    private static class Person implements HasId
    {
        long id;
        String first;
        String last;
        Pet favoritePet;
        Pet[] pets;

        public Object getId()
        {
            return id;
        }
    }

    private class Document implements HasId
    {
        long id;
        Person party1;
        Person party2;
        Person party3;

        public Object getId()
        {
            return id;
        }
    }

    private static class Pet implements HasId
    {
        long id;
        String name;
        String type;
        int age;
        String[] nickNames;

        private Pet(long id, String name, String type, int age, String[] nickNames)
        {
            this.id = id;
            this.name = name == null ? null : new String(name);
            this.type = type == null ? null : new String(type);
            this.age = age;
            this.nickNames = nickNames;
        }

        public Object getId()
        {
            return id;
        }
    }

    private static class Employee implements HasId
    {
        long id;
        String first;
        String last;
        Collection<Address> addresses;
        Address mainAddress;

        public Object getId()
        {
            return id;
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            Employee employee = (Employee) o;

            if (id != employee.id)
            {
                return false;
            }

            if (first != null ? !first.equals(employee.first) : employee.first != null)
            {
                return false;
            }
            if (last != null ? !last.equals(employee.last) : employee.last != null)
            {
                return false;
            }
            if (mainAddress != null ? !mainAddress.equals(employee.mainAddress) : employee.mainAddress != null)
            {
                return false;
            }
            if (addresses == null || employee.addresses == null)
            {
                return addresses == employee.addresses;
            }

            if (addresses.size() != employee.addresses.size())
            {
                return false;
            }

            for (Address left : addresses)
            {
                Iterator j = employee.addresses.iterator();
                boolean found = false;
                while (j.hasNext())
                {
                    if (left.equals(j.next()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    return false;
                }
            }

            return true;
        }

        public int hashCode()
        {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (first != null ? first.hashCode() : 0);
            result = 31 * result + (last != null ? last.hashCode() : 0);
            result = 31 * result + (addresses != null ? addresses.hashCode() : 0);
            result = 31 * result + (mainAddress != null ? mainAddress.hashCode() : 0);
            return result;
        }
    }

    private static class Address implements HasId
    {
        long id;
        String street;
        String state;
        String city;
        int zip;
        Collection<Object> junk;

        public Object getId()
        {
            return id;
        }

        public Collection<Object> getJunk()
        {
            return junk;
        }

        public void setJunk(Collection<Object> col)
        {
            junk = col;
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            Address address = (Address) o;

            if (id != address.id)
            {
                return false;
            }
            if (zip != address.zip)
            {
                return false;
            }
            if (city != null ? !city.equals(address.city) : address.city != null)
            {
                return false;
            }

            if (state != null ? !state.equals(address.state) : address.state != null)
            {
                return false;
            }
            if (street != null ? !street.equals(address.street) : address.street != null)
            {
                return false;
            }
            if (junk == null || address.junk == null)
            {
                return junk == address.junk;
            }

            return junk.equals(address.junk);
        }

        public int hashCode()
        {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (street != null ? street.hashCode() : 0);
            result = 31 * result + (state != null ? state.hashCode() : 0);
            result = 31 * result + (city != null ? city.hashCode() : 0);
            result = 31 * result + zip;
            result = 31 * result + (junk != null ? junk.hashCode() : 0);
            return result;
        }
    }

    private static class Dictionary implements HasId
    {
        long id;
        String name;
        Map<Object, Object> contents;

        public Object getId()
        {
            return id;
        }
    }

    private static class ObjectArray implements HasId
    {
        long id;
        Object[] array;

        public Object getId()
        {
            return id;
        }
    }

    private static class SetContainer implements HasId
    {
        long id;
        Set<Object> set;

        public Object getId()
        {
            return id;
        }
    }

    private static class ListContainer implements HasId
    {
        long id;
        List<Object> list;

        public Object getId()
        {
            return id;
        }
    }

    private static class Dude implements HasId
    {
        private long id;
        private UnidentifiedObject dude;

        public Object getId()
        {
            return id;
        }
    }

    private static class UnidentifiedObject
    {
        private final String name;
        private final int age;
        private final List<Pet> pets = new ArrayList<>();

        private UnidentifiedObject(String name, int age)
        {
            this.name = name;
            this.age = age;
        }

        public void addPet(Pet p)
        {
            pets.add(p);
        }
    }

    @Test
    public void testAlpha()
    {
        // TODO: Need to find faster way to get last IP address (problem for unique id generator, not GraphComparator)
        UniqueIdGenerator.getUniqueId();
    }

    @Test
    public void testSimpleObjectDifference() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        Person p2 = persons[1];
        p2.first = "Jack";
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("first".equals(delta.getFieldName()));
        assertNull(delta.getOptionalKey());
        assertTrue("John".equals(delta.getSourceValue()));
        assertTrue("Jack".equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == id);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testNullingField() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        Pet savePet = persons[0].favoritePet;
        persons[1].favoritePet = null;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("favoritePet".equals(delta.getFieldName()));
        assertNull(delta.getOptionalKey());
        assertTrue(savePet == delta.getSourceValue());
        assertTrue(null == delta.getTargetValue());
        assertTrue((Long) delta.getId() == id);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // An element within an array having a primitive field differences
    // on elements within the array.
    @Test
    public void testArrayItemDifferences() throws Exception
    {
        Person[] persons = createTwoPersons();
        Person p2 = persons[1];
        p2.pets[0].name = "Edward";
        p2.pets[1].age = 2;
        long edId = persons[0].pets[0].id;
        long bellaId = persons[0].pets[1].id;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertEquals(2, deltas.size());
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("name".equals(delta.getFieldName()));
        assertNull(delta.getOptionalKey());
        assertTrue("Eddie".equals(delta.getSourceValue()));
        assertTrue("Edward".equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == edId);

        delta = deltas.get(1);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("age".equals(delta.getFieldName()));
        assertNull(delta.getOptionalKey());
        assertTrue(1 == (Integer) delta.getSourceValue());
        assertTrue(2 == (Integer) delta.getTargetValue());
        assertTrue((Long) delta.getId() == bellaId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New array is shorter than original
    @Test
    public void testShortenArray() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        long bellaId = persons[0].pets[1].id;
        Person p2 = persons[1];
        p2.pets = new Pet[1];
        p2.pets[0] = persons[0].pets[0];
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(persons[0].pets.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets.equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == id);
        assertTrue(1 == (Integer) delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());
        assertTrue((Long) delta.getId() == bellaId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New array has no elements (but not null)
    @Test
    public void testShortenArrayToZeroLength() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        long bellaId = persons[0].pets[1].id;
        Person p2 = persons[1];
        p2.pets = new Pet[0];
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(persons[0].pets.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets.equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == id);
        assertTrue(0 == (Integer) delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());
        assertTrue((Long) delta.getId() == bellaId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New array has no elements (but not null)
    @Test
    public void testShortenPrimitiveArrayToZeroLength() throws Exception
    {
        Person[] persons = createTwoPersons();
        long petId = persons[0].pets[0].id;
        persons[1].pets[0].nickNames = new String[]{};
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        // No orphan command in Delta list because this is an array of primitives (only 1 delta, not 2 like above)
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(persons[0].pets[0].nickNames.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets[0].nickNames.equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == petId);
        assertTrue(0 == (Integer) delta.getOptionalKey());

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New array is longer than original
    @Test
    public void testLengthenArray() throws Exception
    {
        Person[] persons = createTwoPersons();
        long pid = persons[0].id;
        Person p2 = persons[1];
        Pet[] pets = new Pet[3];
        System.arraycopy(p2.pets, 0, pets, 0, 2);
        long id = UniqueIdGenerator.getUniqueId();
        pets[2] = new Pet(id, "Andy", "feline", 3, new String[]{"andrew", "candy", "dandy", "dumbo"});
        p2.pets = pets;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(persons[0].pets.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets.equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == pid);
        assertTrue(3 == (Integer) delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(2 == (Integer) delta.getOptionalKey());
        assertTrue(null == delta.getSourceValue());
        assertTrue(pets[2].equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == pid);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testNullOutArrayElements() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        long bellaId = persons[0].pets[1].id;
        Person p2 = persons[1];
        p2.pets[0] = null;
        p2.pets[1] = null;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 3);
        GraphComparator.Delta delta = deltas.get(1);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(0 == (Integer) delta.getOptionalKey());
        assertTrue(persons[0].pets[0].equals(delta.getSourceValue()));
        assertTrue(null == delta.getTargetValue());
        assertTrue((Long) delta.getId() == id);

        delta = deltas.get(0);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(1 == (Integer) delta.getOptionalKey());
        assertTrue(persons[0].pets[1].equals(delta.getSourceValue()));
        assertTrue(null == delta.getTargetValue());
        assertTrue((Long) delta.getId() == id);

        // Note: Only one orphan (Bella) because Eddie is pointed to by favoritePet field.
        delta = deltas.get(2);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());
        assertTrue((Long) delta.getId() == bellaId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New array is shorter than original array, plus element 0 is what was in element 1
    @Test
    public void testArrayLengthDifferenceAndMove() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        Person p2 = persons[1];
        p2.pets = new Pet[1];
        p2.pets[0] = persons[0].pets[1];
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(persons[0].pets.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets.equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == id);
        assertTrue(1 == (Integer) delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(0 == (Integer) delta.getOptionalKey());
        assertTrue(p2.pets[0].equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == id);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    // New element set into an array
    @Test
    public void testNewArrayElement() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        long edId = persons[0].pets[0].id;
        Person p2 = persons[1];
        p2.pets[0] = new Pet(UniqueIdGenerator.getUniqueId(), "Andy", "feline", 3, new String[]{"fat cat"});
        p2.favoritePet = p2.pets[0];
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());
        assertTrue(deltas.size() == 3);

        boolean arraySetElementFound = false;
        boolean objectAssignFieldFound = false;
        boolean objectOrphanFound = false;


        for (GraphComparator.Delta delta : deltas) {
            if (ARRAY_SET_ELEMENT == delta.getCmd()) {
                assertTrue("pets".equals(delta.getFieldName()));
                assertTrue(0 == (Integer)delta.getOptionalKey());
                assertTrue(persons[1].pets[0].equals(delta.getTargetValue()));
                assertTrue(id == (Long) delta.getId());
                arraySetElementFound = true;
            } else if (OBJECT_ASSIGN_FIELD == delta.getCmd()) {
                assertTrue("favoritePet".equals(delta.getFieldName()));
                assertTrue(delta.getOptionalKey() == null);
                assertTrue(persons[1].pets[0].equals(delta.getTargetValue()));
                assertTrue(id == (Long) delta.getId());
                objectAssignFieldFound = true;
            } else if (OBJECT_ORPHAN == delta.getCmd()) {
                assertTrue(edId == (Long) delta.getId());
                objectOrphanFound = true;
            }
        }


        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
        assertTrue(persons[0].pets[0] == persons[0].favoritePet);   // Ensure same instance is used in array and favoritePet field
    }

    @Test
    public void testPrimitiveArrayElementDifferences() throws Exception
    {
        Person[] persons = createTwoPersons();
        long edId = persons[0].pets[0].id;
        Person p2 = persons[1];
        p2.pets[0].nickNames[0] = null;
        p2.pets[0].nickNames[1] = "bobo";
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());
        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(0 == (Integer) delta.getOptionalKey());
        assertTrue(persons[0].pets[0].nickNames[0].equals(delta.getSourceValue()));
        assertTrue(null == delta.getTargetValue());
        assertTrue((Long) delta.getId() == edId);

        delta = deltas.get(1);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(1 == (Integer) delta.getOptionalKey());
        assertTrue(persons[0].pets[0].nickNames[1].equals(delta.getSourceValue()));
        assertTrue("bobo".equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == edId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testLengthenPrimitiveArray() throws Exception
    {
        Person[] persons = createTwoPersons();
        long bellaId = persons[0].pets[1].id;
        Person p2 = persons[1];
        final int len = p2.pets[1].nickNames.length;
        String[] nickNames = new String[len + 1];
        System.arraycopy(p2.pets[1].nickNames, 0, nickNames, 0, len);
        nickNames[len] = "Scissor hands";
        p2.pets[1].nickNames = nickNames;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());
        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_RESIZE == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(4 == (Integer) delta.getOptionalKey());
        assertTrue(persons[0].pets[1].nickNames.equals(delta.getSourceValue()));
        assertTrue(nickNames == delta.getTargetValue());
        assertTrue((Long) delta.getId() == bellaId);

        delta = deltas.get(1);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(3 == (Integer) delta.getOptionalKey());
        assertTrue(null == delta.getSourceValue());
        assertTrue("Scissor hands".equals(delta.getTargetValue()));
        assertTrue((Long) delta.getId() == bellaId);

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testNullObjectArrayField() throws Exception
    {
        Person[] persons = createTwoPersons();
        long id = persons[0].id;
        long bellaId = persons[0].pets[1].id;
        Person p2 = persons[1];
        p2.pets = null;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("pets".equals(delta.getFieldName()));
        assertTrue(persons[0].pets.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets == delta.getTargetValue());
        assertTrue((Long) delta.getId() == id);
        assertNull(delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());
        assertTrue((Long) delta.getId() == bellaId);

        // Eddie not orphaned because favoritePet field still points to him

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testNullPrimitiveArrayField() throws Exception
    {
        Person[] persons = createTwoPersons();
        persons[1].pets[0].nickNames = null;
        long id = persons[1].pets[0].id;
        assertFalse(DeepEquals.deepEquals(persons[0], persons[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], persons[1], getIdFetcher());

        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("nickNames".equals(delta.getFieldName()));
        assertTrue(persons[0].pets[0].nickNames.equals(delta.getSourceValue()));
        assertTrue(persons[1].pets[0].nickNames == delta.getTargetValue());
        assertTrue((Long) delta.getId() == id);
        assertNull(delta.getOptionalKey());

        GraphComparator.applyDelta(persons[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(persons[0], persons[1]));
    }

    @Test
    public void testObjectArrayWithPrimitives() throws Exception
    {
        ObjectArray source = new ObjectArray();
        source.id = UniqueIdGenerator.getUniqueId();
        source.array = new Object[]{'a', 'b', 'c', 'd'};

        ObjectArray target = (ObjectArray) clone(source);
        target.array[3] = 5;

        assertFalse(DeepEquals.deepEquals(source, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertEquals("array", delta.getFieldName());
        assertEquals(3, delta.getOptionalKey());
        assertEquals('d', delta.getSourceValue());
        assertEquals(5, delta.getTargetValue());

        GraphComparator.applyDelta(source, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(source, target));
    }

    @Test
    public void testObjectArrayWithArraysAsElements() throws Exception
    {
        ObjectArray source = new ObjectArray();
        source.id = UniqueIdGenerator.getUniqueId();
        source.array = new Object[]{new String[]{"1a", "1b", "1c"}, new String[]{"2a", "2b", "2c"}};

        ObjectArray target = (ObjectArray) clone(source);
        String[] strings = (String[]) target.array[1];
        strings[2] = "2C";

        assertFalse(DeepEquals.deepEquals(source, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertEquals("array", delta.getFieldName());
        assertEquals(1, delta.getOptionalKey());
        assertTrue(((String[]) delta.getSourceValue())[2] == "2c");
        assertTrue(((String[]) delta.getTargetValue())[2] == "2C");

        GraphComparator.applyDelta(source, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(source, target));
    }

    @Test
    public void testArraySetElementOutOfBounds() throws Exception
    {
        ObjectArray src = new ObjectArray();
        src.array = new Object[3];
        src.array[0] = "one";
        src.array[1] = 2;
        src.array[2] = 3L;

        ObjectArray target = new ObjectArray();
        target.array = new Object[3];
        target.array[0] = "one";
        target.array[1] = 2;
        target.array[2] = null;

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(ARRAY_SET_ELEMENT == delta.getCmd());
        assertEquals("array", delta.getFieldName());
        assertEquals(2, delta.getOptionalKey());

        delta.setOptionalKey(20);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getError().contains("ARRAY_SET_ELEMENT"));
        assertTrue(error.getError().contains("failed"));
    }

    @Test
    public void testSetRemoveNonPrimitive() throws Exception
    {
        Employee[] employees = createTwoEmployees(SET_TYPE_LINKED);
        long id = employees[0].id;
        Iterator i = employees[1].addresses.iterator();
        employees[1].addresses.remove(i.next());
        assertFalse(DeepEquals.deepEquals(employees[0], employees[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(employees[0], employees[1], getIdFetcher());

        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(SET_REMOVE == delta.getCmd());
        assertTrue("addresses".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertNull(delta.getTargetValue());
        assertNull(delta.getOptionalKey());
        assertTrue(employees[0].addresses.iterator().next().equals(delta.getSourceValue()));

        GraphComparator.applyDelta(employees[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(employees[0], employees[1]));
    }

    @Test
    public void testSetAddNonPrimitive() throws Exception
    {
        Employee[] employees = createTwoEmployees(SET_TYPE_HASH);
        long id = employees[0].id;
        Address addr = new Address();
        addr.zip = 90210;
        addr.state = "CA";
        addr.id = UniqueIdGenerator.getUniqueId();
        addr.city = "Beverly Hills";
        addr.street = "1000 Rodeo Drive";
        employees[1].addresses.add(addr);
        assertFalse(DeepEquals.deepEquals(employees[0], employees[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(employees[0], employees[1], getIdFetcher());

        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(SET_ADD == delta.getCmd());
        assertTrue("addresses".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertTrue(addr.equals(delta.getTargetValue()));
        assertNull(delta.getSourceValue());
        assertNull(delta.getOptionalKey());

        GraphComparator.applyDelta(employees[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(employees[0], employees[1]));
    }

    @Test
    public void testSetAddRemovePrimitive() throws Exception
    {
        Employee[] employees = createTwoEmployees(SET_TYPE_LINKED);
        Iterator i = employees[0].addresses.iterator();
        Address address = (Address) i.next();
        long id = (Long) address.getId();
        address.setJunk(new HashSet<>());
        address.getJunk().add("lat/lon");
        Date now = new Date();
        address.getJunk().add(now);
        i = employees[1].addresses.iterator();
        address = (Address) i.next();
        address.setJunk(new HashSet<>());
        address.getJunk().add(now);
        address.getJunk().add(19);

        assertFalse(DeepEquals.deepEquals(employees[0], employees[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(employees[0], employees[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(SET_REMOVE == delta.getCmd());
        assertTrue("junk".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertNull(delta.getTargetValue());
        assertNull(delta.getOptionalKey());
        assertTrue("lat/lon".equals(delta.getSourceValue()));

        delta = deltas.get(1);
        assertTrue(SET_ADD == delta.getCmd());
        assertTrue("junk".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertNull(delta.getSourceValue());
        assertNull(delta.getOptionalKey());
        assertTrue(19 == (Integer) delta.getTargetValue());

        GraphComparator.applyDelta(employees[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(employees[0], employees[1]));
    }

    @Test
    public void testNullSetField() throws Exception
    {
        Employee[] employees = createTwoEmployees(SET_TYPE_HASH);
        long id = employees[0].id;
        employees[1].addresses = null;

        assertFalse(DeepEquals.deepEquals(employees[0], employees[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(employees[0], employees[1], getIdFetcher());

        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("addresses".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertNull(delta.getTargetValue());
        assertNull(delta.getOptionalKey());
        assertTrue(employees[0].addresses.equals(delta.getSourceValue()));

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        GraphComparator.applyDelta(employees[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(employees[0], employees[1]));
    }

    @Test
    public void testMapPut() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        long id = dictionaries[0].id;
        dictionaries[1].contents.put("Entry2", "Foo");
        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_PUT == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertEquals(delta.getTargetValue(), "Foo");
        assertEquals(delta.getOptionalKey(), "Entry2");
        assertNull(delta.getSourceValue());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapPutForReplace() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        long id = dictionaries[0].id;
        dictionaries[0].contents.put("Entry2", "Bar");
        dictionaries[1].contents.put("Entry2", "Foo");
        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_PUT == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertEquals(delta.getTargetValue(), "Foo");
        assertEquals(delta.getOptionalKey(), "Entry2");
        assertEquals(delta.getSourceValue(), "Bar");

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapRemove() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        long id = dictionaries[0].id;
        dictionaries[1].contents.remove("Eddie");
        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_REMOVE == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertTrue(delta.getId().equals(id));
        assertTrue(delta.getSourceValue() instanceof Pet);
        assertEquals(delta.getOptionalKey(), "Eddie");
        assertNull(delta.getTargetValue());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapRemoveUntilEmpty() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        long id = dictionaries[0].id;
        dictionaries[1].contents.clear();
        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 5);

        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_REMOVE == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertNull(delta.getTargetValue());

        delta = deltas.get(1);
        assertTrue(MAP_REMOVE == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertNull(delta.getTargetValue());

        delta = deltas.get(2);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        delta = deltas.get(3);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        delta = deltas.get(4);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapFieldAssignToNull() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        dictionaries[1].contents = null;
        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 4);

        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertTrue("contents".equals(delta.getFieldName()));
        assertNull(delta.getTargetValue());

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        delta = deltas.get(2);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        delta = deltas.get(3);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapValueChange() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        Person p = (Person) dictionaries[0].contents.get("DeRegnaucourt");
        dictionaries[1].contents.put("Eddie", p.pets[1]);

        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_PUT == delta.getCmd());
        assertEquals("contents", delta.getFieldName());
        assertEquals("Eddie", delta.getOptionalKey());
        assertTrue(delta.getTargetValue() instanceof Pet);

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapValueChangeToNull() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        dictionaries[1].contents.put("Eddie", null);

        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_PUT == delta.getCmd());
        assertEquals("contents", delta.getFieldName());
        assertEquals("Eddie", delta.getOptionalKey());
        assertNull(delta.getTargetValue());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    @Test
    public void testMapValueChangeToPrimitive() throws Exception
    {
        Dictionary[] dictionaries = createTwoDictionaries();
        dictionaries[1].contents.put("Eddie", Boolean.TRUE);

        assertFalse(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(dictionaries[0], dictionaries[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(MAP_PUT == delta.getCmd());
        assertEquals("contents", delta.getFieldName());
        assertEquals("Eddie", delta.getOptionalKey());
        assertTrue((Boolean) delta.getTargetValue());

        GraphComparator.applyDelta(dictionaries[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(dictionaries[0], dictionaries[1]));
    }

    // An element within a List having a primitive field differences
    // on elements within the List.
    @Test
    public void testListItemDifferences() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add("one");
        target.list.add(2L);
        target.list.add(3L);

        assertTrue(DeepEquals.deepEquals(src, target));
    }

    // New array is shorter than original
    @Test
    public void testShortenList() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add("one");
        target.list.add(2);

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_RESIZE == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(2, delta.getOptionalKey());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    // New List has no elements (but not null)
    @Test
    public void testShortenListToZeroLength() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_RESIZE == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(0, delta.getOptionalKey());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    // New List is longer than original
    @Test
    public void testLengthenList() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add("one");
        target.list.add(2);
        target.list.add(3L);
        target.list.add(Boolean.TRUE);

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_RESIZE == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(4, delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(3, delta.getOptionalKey());
        assertNull(delta.getSourceValue());
        assertEquals(true, delta.getTargetValue());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testNullOutListElements() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add(null);
        target.list.add(null);

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 3);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_RESIZE == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(2, delta.getOptionalKey());

        delta = deltas.get(2);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(0, delta.getOptionalKey());
        assertNotNull(delta.getSourceValue());
        assertNull(delta.getTargetValue());

        delta = deltas.get(1);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(1, delta.getOptionalKey());
        assertNotNull(delta.getSourceValue());
        assertNull(delta.getTargetValue());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testNullListField() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = null;

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertNull(delta.getOptionalKey());
        assertNotNull(delta.getSourceValue());
        assertNull(delta.getTargetValue());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testChangeListElementField() throws Exception
    {
        Person[] persons = createTwoPersons();
        Pet dog1 = persons[0].pets[0];
        Pet dog2 = persons[0].pets[1];
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add(dog1);
        src.list.add(dog2);

        ListContainer target = (ListContainer) clone(src);
        Pet dog2copy = (Pet) target.list.get(1);
        dog2copy.age = 7;

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertEquals("age", delta.getFieldName());
        assertNull(delta.getOptionalKey());
        assertEquals(1, delta.getSourceValue());
        assertEquals(7, delta.getTargetValue());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testReplaceListElementObject() throws Exception
    {
        Pet dog1 = getPet("Eddie");
        Pet dog2 = getPet("Bella");
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add(dog1);
        src.list.add(dog2);

        ListContainer target = (ListContainer) clone(src);
        Pet fido = new Pet(UniqueIdGenerator.getUniqueId(), "Fido", "canine", 3, new String[]{"Buddy", "Captain D-Bag", "Sam"});
        target.list.set(1, fido);

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 2);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(1, delta.getOptionalKey());
        assertEquals(dog2, delta.getSourceValue());
        assertEquals(fido, delta.getTargetValue());

        delta = deltas.get(1);
        assertTrue(OBJECT_ORPHAN == delta.getCmd());
        assertEquals(dog2.id, delta.getId());

        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testBadResizeValue()
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_RESIZE == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(0, delta.getOptionalKey());

        delta.setOptionalKey(-1);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getError().contains("LIST_RESIZE"));
        assertTrue(error.getError().contains("failed"));
    }

    @Test
    public void testDiffListTypes() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new LinkedList<>();
        target.list.add("one");
        target.list.add(2);
        target.list.add(3L);

        assertTrue(DeepEquals.deepEquals(src, target));

        // Prove that it ignored List type and only considered the contents
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.isEmpty());
    }

    @Test
    public void testDiffCollectionTypes() throws Exception
    {
        Employee emps[] = createTwoEmployees(SET_TYPE_LINKED);
        Employee empTarget = emps[1];
        empTarget.addresses = new ArrayList<>();
        empTarget.addresses.addAll(emps[0].addresses);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(emps[0], empTarget, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(delta.getCmd(), OBJECT_FIELD_TYPE_CHANGED);
        assertEquals(delta.getFieldName(), "addresses");

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(emps[0], deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getError().contains("OBJECT_FIELD_TYPE_CHANGED"));
        assertTrue(error.getError().contains("failed"));
    }

    @Test
    public void testListSetElementOutOfBounds() throws Exception
    {
        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");
        src.list.add(2);
        src.list.add(3L);

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add("one");
        target.list.add(2);
        target.list.add(null);

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(2, delta.getOptionalKey());

        delta.setOptionalKey(20);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getError().contains("LIST_SET_ELEMENT"));
        assertTrue(error.getError().contains("failed"));
    }

    @Test
    public void testDeltaSetterGetter()
    {
        GraphComparator.Delta delta = new GraphComparator.Delta(0, "foo", null, null, null, null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);
        assertEquals(OBJECT_ASSIGN_FIELD, delta.getCmd());
        delta.setFieldName("field");
        assertEquals("field", delta.getFieldName());
        delta.setId(9);
        assertEquals(9, delta.getId());
        delta.setOptionalKey(6);
        assertEquals(6, delta.getOptionalKey());
        delta.setSourceValue('a');
        assertEquals('a', delta.getSourceValue());
        delta.setTargetValue(Boolean.TRUE);
        assertEquals(true, delta.getTargetValue());
        assertNotNull(delta.toString());
    }

    @Test
    public void testDeltaCommandBadEnums() throws Exception
    {
        try
        {
            GraphComparator.Delta.Command cmd = fromName(null);
            fail("Should have thrown exception for null enum");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try
        {
            GraphComparator.Delta.Command cmd = fromName("jonas");
            fail("Should have thrown exception for unknown enum");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testApplyDeltaWithCommandParams() throws Exception
    {
//        SetContainer srcSet = new SetContainer();
//        srcSet.set = new HashSet<>();
//        srcSet.set.add("one");
//
//        SetContainer targetSet = new SetContainer();
//        targetSet.set = new HashSet<>();
//        targetSet.set.add("once");
//
//        assertFalse(DeepEquals.deepEquals(srcSet, targetSet));

        ListContainer src = new ListContainer();
        src.list = new ArrayList<>();
        src.list.add("one");

        ListContainer target = new ListContainer();
        target.list = new ArrayList<>();
        target.list.add("once");

        assertFalse(DeepEquals.deepEquals(src, target));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(LIST_SET_ELEMENT == delta.getCmd());
        assertEquals("list", delta.getFieldName());
        assertEquals(0, delta.getOptionalKey());
        Object id = delta.getId();
        delta.setId(19);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getError().contains("LIST_SET_ELEMENT"));
        assertTrue(error.getError().contains("failed"));

        delta.setId(id);
        String name = delta.getFieldName();
        delta.setFieldName(null);
        errors = GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 1);
        error = errors.get(0);
        assertTrue(error.getError().contains("LIST_SET_ELEMENT"));
        assertTrue(error.getError().contains("failed"));


        delta.setFieldName(name);
        GraphComparator.applyDelta(src, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(src, target));
    }

    @Test
    public void testNullSource() throws Exception
    {
        Person[] persons = createTwoPersons();
        persons[1].first = "Dracula";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, persons[1], getIdFetcher());
        assertTrue(deltas.size() == 1);
        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertEquals(GraphComparator.ROOT, delta.getFieldName());
        assertNull(delta.getSourceValue());
        assertEquals(persons[1], delta.getTargetValue());
        assertNull(delta.getOptionalKey());
    }

    @Test
    public void testNullTarget() throws Exception
    {
        Person[] persons = createTwoPersons();

        List<GraphComparator.Delta> deltas = GraphComparator.compare(persons[0], null, getIdFetcher());

        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(OBJECT_ASSIGN_FIELD == delta.getCmd());
        assertEquals(GraphComparator.ROOT, delta.getFieldName());
        assertEquals(delta.getSourceValue(), persons[0]);
        assertNull(delta.getTargetValue());
        assertNull(delta.getOptionalKey());

        delta = deltas.get(1);
        assertTrue(delta.getCmd() == OBJECT_ORPHAN);
        assertNull(delta.getOptionalKey());
        assertNull(delta.getFieldName());
        assertNotNull(delta.getId());

        delta = deltas.get(2);
        assertTrue(delta.getCmd() == OBJECT_ORPHAN);
        assertNull(delta.getOptionalKey());
        assertNull(delta.getFieldName());
        assertNotNull(delta.getId());

        delta = deltas.get(3);
        assertTrue(delta.getCmd() == OBJECT_ORPHAN);
        assertNull(delta.getOptionalKey());
        assertNull(delta.getFieldName());
        assertNotNull(delta.getId());
    }

    @Test
    public void testNullIdIsTreatedAsUnidentifiedObject() throws Exception
    {
        NullableId source = new NullableId();
        source.id = null;
        source.name = "alpha";

        NullableId target = new NullableId();
        target.id = null;
        target.name = "alpha";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, getIdFetcher());
        assertTrue(deltas.isEmpty());
    }

    @Test
    public void testRootArray() throws Exception
    {
        Pet eddie = getPet("Eddie");
        Pet bella = getPet("Bella");
        Pet andy = getPet("Andy");
        Object[] srcPets = new Object[]{eddie, bella};
        Object[] targetPets = new Object[]{eddie, andy};

        assertFalse(DeepEquals.deepEquals(srcPets, targetPets));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(srcPets, targetPets, getIdFetcher());
        assertEquals(deltas.size(), 2);

        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(delta.getCmd() == ARRAY_SET_ELEMENT);
        assertEquals(delta.getOptionalKey(), 1);
        assertEquals(delta.getFieldName(), GraphComparator.ROOT);
        assertTrue(DeepEquals.deepEquals(delta.getTargetValue(), andy));

        delta = deltas.get(1);
        assertTrue(delta.getCmd() == OBJECT_ORPHAN);
        assertNull(delta.getOptionalKey());
        assertNull(delta.getFieldName());
        assertEquals(delta.getId(), bella.id);
    }

    @Test
    public void testUnidentifiedObject() throws Exception
    {
        Dude sourceDude = getDude("Dan", 48);
        Dude targetDude = (Dude) clone(sourceDude);
        assertTrue(DeepEquals.deepEquals(sourceDude, targetDude));
        targetDude.dude.pets.get(0).name = "bunny";
        assertFalse(DeepEquals.deepEquals(sourceDude, targetDude));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(sourceDude, targetDude, getIdFetcher());
        assertEquals(deltas.size(), 1);

        GraphComparator.Delta delta = deltas.get(0);
        assertTrue(delta.getCmd() == OBJECT_ASSIGN_FIELD);
        assertNull(delta.getOptionalKey());
        assertEquals(delta.getFieldName(), "dude");

        GraphComparator.applyDelta(sourceDude, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(DeepEquals.deepEquals(sourceDude, targetDude));
    }

    @Test
    public void testCompareApplyHandlesShadowedFields()
    {
        ShadowChild source = new ShadowChild();
        source.id = UniqueIdGenerator.getUniqueId();
        source.value = 10;
        ((ShadowParent) source).value = 20;

        ShadowChild target = new ShadowChild();
        target.id = source.id;
        target.value = 11;
        ((ShadowParent) target).value = 21;

        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, getIdFetcher());
        assertEquals(2, deltas.size());

        Set<String> fieldNames = new HashSet<>();
        for (GraphComparator.Delta delta : deltas)
        {
            assertEquals(OBJECT_ASSIGN_FIELD, delta.getCmd());
            fieldNames.add(delta.getFieldName());
        }
        assertTrue(fieldNames.contains("value"));
        assertTrue(fieldNames.stream().anyMatch(name -> name.endsWith(".value") && !"value".equals(name)));

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());
        assertTrue(DeepEquals.deepEquals(source, target));
    }

    @Test
    public void testDeltaCommand() throws Exception
    {
        GraphComparator.Delta.Command cmd = MAP_PUT;
        assertEquals(cmd.getName(), "map.put");
        GraphComparator.Delta.Command remove = cmd.fromName("map.remove");
        assertTrue(remove == MAP_REMOVE);
    }

    @Test
    public void testDeltaCommandFromNameUsesLocaleRoot()
    {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));
        try
        {
            assertEquals(LIST_RESIZE, fromName("LIST.RESIZE"));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testApplyDeltaFailFast() throws Exception
    {
        Pet eddie = getPet("Eddie");
        Pet bella = getPet("Bella");
        Pet andy = getPet("Andy");
        Object[] srcPets = new Object[]{eddie, bella};
        Object[] targetPets = new Object[]{eddie, andy};

        assertFalse(DeepEquals.deepEquals(srcPets, targetPets));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(srcPets, targetPets, getIdFetcher());
        assertEquals(deltas.size(), 2);

        GraphComparator.Delta delta = deltas.get(0);
        delta.setId(33);
        delta.setCmd(ARRAY_RESIZE);
        delta = deltas.get(1);
        delta.setCmd(LIST_SET_ELEMENT);
        delta.setFieldName("xyz");

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(srcPets, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.size() == 2);
        errors = GraphComparator.applyDelta(srcPets, deltas, getIdFetcher(), GraphComparator.getJavaDeltaProcessor(), true);
        assertTrue(errors.size() == 1);
    }

    @Test
    public void testSkip()
    {
        Person p1 = new Person();
        p1.id = 10;
        p1.first = "George";
        p1.last = "Washington";

        Person p2 = new Person();
        p2.id = 20;
        p2.first = "John";
        p2.last = "Adams";

        Person p3 = new Person();
        p3.id = 30;
        p3.first = "Thomas";
        p3.last = "Jefferson";

        Person p4 = new Person();
        p4.id = 40;
        p4.first = "James";
        p4.last = "Madison";

        Document doc1 = new Document();
        doc1.id = 1;
        doc1.party1 = p1;
        doc1.party2 = p2;
        doc1.party3 = p1;

        Document doc2 = new Document();
        doc2.id = 1;
        doc2.party1 = p4;
        doc2.party2 = p2;
        doc2.party3 = p4;

        List<GraphComparator.Delta> deltas;
        deltas = GraphComparator.compare(doc1, doc2, getIdFetcher());
    }

    /**
     * Initial case
     * A->B->X
     * A->C->X
     * A->D->X
     * Y (isolated)
     * Ending case
     * A->B->Y
     * A->C->X
     * A->D->Y
     * <p/>
     * Should have two deltas:
     * 1. B->X goes to B->Y
     * 2. D->X goes to D->Y
     */
    @Test
    public void testTwoPointersToSameInstance() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        Doc A = new Doc("A");
        A.childB = B;
        A.childC = C;
        A.childD = D;


        Doc Acopy = (Doc) clone(A);
        Acopy.childB.child = Y;
        Acopy.childC.child = X;
        Acopy.childD.child = Y;
        List<GraphComparator.Delta> deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(deltas.size(), 2);

        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(delta.getCmd(), OBJECT_ASSIGN_FIELD);
        assertTrue(delta.getTargetValue() instanceof Node);
        Node node = (Node) delta.getTargetValue();
        assertEquals(node.name, "Y");
        delta = deltas.get(1);
        assertEquals(delta.getCmd(), OBJECT_ASSIGN_FIELD);
        assertTrue(delta.getTargetValue() instanceof Node);
        node = (Node) delta.getTargetValue();
        assertEquals(node.name, "Y");
    }

    @Test
    public void testCycle() throws Exception
    {
        Node A = new Node("A");
        Node B = new Node("B");
        Node C = new Node("C");
        A.child = B;
        B.child = C;
        C.child = A;

        Node Acopy = (Node) clone(A);

        // Equal with cycle
        List deltas = new ArrayList<>();
        GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(0, deltas.size());
    }

    @Test
    public void testTwoPointersToSameInstanceArray() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        Object[] A = new Object[3];
        A[0] = B;
        A[1] = C;
        A[2] = D;

        Object[] Acopy = (Object[]) clone(A);

        B = (Node) Acopy[0];
        D = (Node) Acopy[2];
        B.child = Y;
        D.child = Y;
        List deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(2, deltas.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoPointersToSameInstanceOrderedCollection() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        List<Object> A = new ArrayList<>();
        A.add(B);
        A.add(C);
        A.add(D);

        List<Object> Acopy = (List<Object>) clone(A);

        B = (Node) Acopy.get(0);
        D = (Node) Acopy.get(2);
        B.child = Y;
        D.child = Y;
        List<GraphComparator.Delta> deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(2, deltas.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoPointersToSameInstanceUnorderedCollection() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        Set<Object> A = new LinkedHashSet<>();
        A.add(B);
        A.add(C);
        A.add(D);

        Set<Object> Acopy = (Set<Object>) clone(A);

        Iterator i = Acopy.iterator();
        B = (Node) i.next();
        i.next();   // skip C
        D = (Node) i.next();
        B.child = Y;
        D.child = Y;

        List<GraphComparator.Delta> deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(2, deltas.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoPointersToSameInstanceUnorderedMap() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        Map<String, Object> A = new HashMap<>();
        A.put("childB", B);
        A.put("childC", C);
        A.put("childD", D);

        Map<String, Object> Acopy = (Map<String, Object>) clone(A);

        B = (Node) Acopy.get("childB");
        D = (Node) Acopy.get("childD");
        B.child = Y;
        D.child = Y;

        List<GraphComparator.Delta> deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(2, deltas.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoPointersToSameInstanceOrderedMap() throws Exception
    {
        Node X = new Node("X");
        Node Y = new Node("Y");

        Node B = new Node("B", X);
        Node C = new Node("C", X);
        Node D = new Node("D", X);

        Map<String, Object> A = new TreeMap<>();
        A.put("childB", B);
        A.put("childC", C);
        A.put("childD", D);

        Map<String, Object> Acopy = (Map<String, Object>) clone(A);

        B = (Node) Acopy.get("childB");
        D = (Node) Acopy.get("childD");
        B.child = Y;
        D.child = Y;

        List<GraphComparator.Delta> deltas = GraphComparator.compare(A, Acopy, getIdFetcher());
        assertEquals(2, deltas.size());
    }

    @Test
    public void testSortedAndUnsortedMap()
    {
        Map<String, String> map1 = new LinkedHashMap<>();
        Map<String, String> map2 = new TreeMap<>();
        map1.put("C", "charlie");
        map1.put("A", "alpha");
        map1.put("B", "beta");
        map2.put("C", "charlie");
        map2.put("B", "beta");
        map2.put("A", "alpha");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(map1, map2, null);
        assertEquals(0, deltas.size());

        map1 = new TreeMap<>(Comparator.naturalOrder());
        map1.put("a", "b");
        map1.put("c", "d");
        map2 = new TreeMap<>(Comparator.reverseOrder());
        map2.put("a", "b");
        map2.put("c", "d");
        deltas = GraphComparator.compare(map1, map2, null);
        assertEquals(0, deltas.size());
    }

    @Test
    public void testSortedAndUnsortedSet()
    {
        SortedSet<String> set1 = new TreeSet<>();
        Set<String> set2 = new HashSet<>();
        List<GraphComparator.Delta> deltas =  GraphComparator.compare(set1, set2, null);
        assertEquals(0, deltas.size());

        set1 = new TreeSet<>();
        set1.add("a");
        set1.add("b");
        set1.add("c");
        set1.add("d");
        set1.add("e");

        set2 = new LinkedHashSet<>();
        set2.add("e");
        set2.add("d");
        set2.add("c");
        set2.add("b");
        set2.add("a");
        deltas =  GraphComparator.compare(set1, set2, null);
        assertEquals(0, deltas.size());
    }

    // ----------------------------------------------------------
    // Helper classes (not tests)
    // ----------------------------------------------------------
    static class Node implements HasId
    {
        String name;
        Node child;

        Node(String name)
        {
            this.name = name;
        }

        Node(String name, Node child)
        {
            this.name = name;
            this.child = child;
        }

        public Object getId()
        {
            return name;
        }
    }

    static class Doc implements HasId
    {
        String namex;
        Node childB;
        Node childC;
        Node childD;

        Doc(String name)
        {
            this.namex = name;
        }

        public Object getId()
        {
            return namex;
        }
    }

    static class ShadowParent implements HasId
    {
        long id;
        int value;

        public Object getId()
        {
            return id;
        }
    }

    static class ShadowChild extends ShadowParent
    {
        int value;
    }

    static class NullableId implements HasId
    {
        Long id;
        String name;

        public Object getId()
        {
            return id;
        }
    }

    private Dictionary[] createTwoDictionaries() throws Exception
    {
        Person[] persons = createTwoPersons();
        Dictionary dictionary = new Dictionary();
        dictionary.id = UniqueIdGenerator.getUniqueId();
        dictionary.name = "Websters";
        dictionary.contents = new HashMap<>();
        dictionary.contents.put(persons[0].last, persons[0]);
        dictionary.contents.put(persons[0].pets[0].name, persons[0].pets[0]);

        Dictionary dict = (Dictionary) clone(dictionary);

        return new Dictionary[]{dictionary, dict};
    }

    private Person[] createTwoPersons() throws Exception
    {
        Pet dog1 = getPet("eddie");
        Pet dog2 = getPet("bella");
        Person p1 = new Person();
        p1.id = UniqueIdGenerator.getUniqueId();
        p1.first = "John";
        p1.last = "DeRegnaucourt";
        p1.favoritePet = dog1;
        p1.pets = new Pet[2];
        p1.pets[0] = dog1;
        p1.pets[1] = dog2;

        Person p2 = (Person) clone(p1);

        return new Person[]{p1, p2};
    }

    private Pet getPet(String name)
    {
        if ("andy".equalsIgnoreCase(name))
        {
            return new Pet(UniqueIdGenerator.getUniqueId(), "Andy", "feline", 3, new String[]{"andrew", "candy", "dandy", "dumbo"});
        }
        else if ("eddie".equalsIgnoreCase(name))
        {
            return new Pet(UniqueIdGenerator.getUniqueId(), "Eddie", "Terrier", 4, new String[]{"edward", "edwardo"});
        }
        else if ("bella".equalsIgnoreCase(name))
        {
            return new Pet(UniqueIdGenerator.getUniqueId(), "Bella", "Chihuahua", 1, new String[]{"bellaboo", "bella weena", "rotten dog"});
        }
        return null;
    }

    private Employee[] createTwoEmployees(int setType) throws Exception
    {
        Address addr1 = new Address();
        addr1.id = UniqueIdGenerator.getUniqueId();
        addr1.street = "210 Ballard Drive";
        addr1.city = "Springboro";
        addr1.state = "OH";
        addr1.zip = 45066;

        Address addr2 = new Address();
        addr2.id = UniqueIdGenerator.getUniqueId();
        addr2.street = "10101 Pickfair Drive";
        addr2.city = "Austin";
        addr2.state = "TX";
        addr2.zip = 78750;

        Employee emp1 = new Employee();
        emp1.id = UniqueIdGenerator.getUniqueId();
        emp1.first = "John";
        emp1.last = "DeRegnaucourt";
        if (setType == SET_TYPE_HASH)
        {
            emp1.addresses = new HashSet<>();
        }
        else if (setType == SET_TYPE_TREE)
        {
            emp1.addresses = new TreeSet<>();
        }
        else if (setType == SET_TYPE_LINKED)
        {
            emp1.addresses = new LinkedHashSet<>();
        }
        else
        {
            throw new RuntimeException("unknown set type: " + setType);
        }

        emp1.addresses.add(addr1);
        emp1.addresses.add(addr2);
        emp1.mainAddress = addr1;

        Employee emp2 = (Employee) clone(emp1);

        return new Employee[]{emp1, emp2};
    }

    private Dude getDude(String name, int age)
    {
        Dude dude = new Dude();
        dude.id = UniqueIdGenerator.getUniqueId();
        dude.dude = new UnidentifiedObject(name, age);
        dude.dude.addPet(getPet("bella"));
        dude.dude.addPet(getPet("eddie"));
        return dude;
    }

    private Object clone(Object source) {
        return JsonIo.deepCopy(source, null, null);
    }

    private GraphComparator.ID getIdFetcher()
    {
        return objectToId -> {
            if (objectToId instanceof HasId)
            {
                HasId obj = (HasId) objectToId;
                return obj.getId();
            }
            else if (objectToId instanceof Collection || objectToId instanceof Map)
            {
                return null;
            }
            throw new RuntimeException("Object does not support getId(): " + (objectToId != null ? objectToId.getClass().getName() : "null"));
        };
    }
}
