package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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

/**
 * Graph Utility algorithms, such as Asymmetric Graph Difference.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright [2010] John DeRegnaucourt
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class GraphComparator
{
    public static final String ROOT = "-root-";

    public interface ID
    {
        Object getId(Object objectToId);
    }

    public static class Delta
    {
        private String srcPtr;
        private Object id;
        private String fieldName;
        private Object srcValue;
        private Object targetValue;
        private Object optionalKey;
        private Command cmd;

        public Delta(Object id, String fieldName, String srcPtr, Object srcValue, Object targetValue, Object optKey)
        {
            this.id = id;
            this.fieldName = fieldName;
            this.srcPtr = srcPtr;
            this.srcValue = srcValue;
            this.targetValue = targetValue;
            optionalKey = optKey;
        }

        public Object getId()
        {
            return id;
        }

        public void setId(Object id)
        {
            this.id = id;
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public void setFieldName(String fieldName)
        {
            this.fieldName = fieldName;
        }

        public Object getSourceValue()
        {
            return srcValue;
        }

        public void setSourceValue(Object srcValue)
        {
            this.srcValue = srcValue;
        }

        public Object getTargetValue()
        {
            return targetValue;
        }

        public void setTargetValue(Object targetValue)
        {
            this.targetValue = targetValue;
        }

        public Object getOptionalKey()
        {
            return optionalKey;
        }

        public void setOptionalKey(Object optionalKey)
        {
            this.optionalKey = optionalKey;
        }

        public Command getCmd()
        {
            return cmd;
        }

        public void setCmd(Command cmd)
        {
            this.cmd = cmd;
        }

        public String toString()
        {
            return "Delta {" +
                    "id=" + id +
                    ", fieldName='" + fieldName + '\'' +
                    ", srcPtr=" + srcPtr +
                    ", srcValue=" + srcValue +
                    ", targetValue=" + targetValue +
                    ", optionalKey=" + optionalKey +
                    ", cmd='" + cmd + '\'' +
                    '}';
        }

        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (other == null || getClass() != other.getClass())
            {
                return false;
            }

            Delta delta = (Delta) other;
            return srcPtr.equals(delta.srcPtr);
        }

        public int hashCode()
        {
            return srcPtr.hashCode();
        }

        /**
         * These are all possible Delta.Commands that are generated when performing
         * the graph comparison.
         */
        public enum Command
        {
            ARRAY_SET_ELEMENT("array.setElement"),
            ARRAY_RESIZE("array.resize"),
            OBJECT_ASSIGN_FIELD("object.assignField"),
            OBJECT_ORPHAN("object.orphan"),
            OBJECT_FIELD_TYPE_CHANGED("object.fieldTypeChanged"),
            SET_ADD("set.add"),
            SET_REMOVE("set.remove"),
            MAP_PUT("map.put"),
            MAP_REMOVE("map.remove"),
            LIST_RESIZE("list.resize"),
            LIST_SET_ELEMENT("list.setElement");

            private String name;
            Command(final String name)
            {
                this.name = name.intern();
            }

            public String getName()
            {
                return name;
            }

            public static Command fromName(String name)
            {
                if (name == null || "".equals(name.trim()))
                {
                    throw new IllegalArgumentException("Name is required for Command.forName()");
                }

                name = name.toLowerCase();
                for (Command t : Command.values())
                {
                    if (t.getName().equals(name))
                    {
                        return t;
                    }
                }

                throw new IllegalArgumentException("Unknown Command enum: " + name);
            }
        }
    }

    public static class DeltaError extends Delta
    {
        public String error;

        public DeltaError(String error, Delta delta)
        {
            super(delta.getId(), delta.fieldName, delta.srcPtr, delta.srcValue, delta.targetValue, delta.optionalKey);
            this.error = error;
        }

        public String getError()
        {
            return error;
        }
    }

    public interface DeltaProcessor
    {
        void processArraySetElement(Object srcValue, Field field, Delta delta);
        void processArrayResize(Object srcValue, Field field, Delta delta);
        void processObjectAssignField(Object srcValue, Field field, Delta delta);
        void processObjectOrphan(Object srcValue, Field field, Delta delta);
        void processObjectTypeChanged(Object srcValue, Field field, Delta delta);
        void processSetAdd(Object srcValue, Field field, Delta delta);
        void processSetRemove(Object srcValue, Field field, Delta delta);
        void processMapPut(Object srcValue, Field field, Delta delta);
        void processMapRemove(Object srcValue, Field field, Delta delta);
        void processListResize(Object srcValue, Field field, Delta delta);
        void processListSetElement(Object srcValue, Field field, Delta delta);

        class Helper
        {
            private static Object getFieldValueAs(Object source, Field field, Class type, Delta delta)
            {
                Object fieldValue;
                try
                {
                    fieldValue = field.get(source);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(delta.cmd + " failed, unable to access field: " + field.getName() +
                            ", obj id: " + delta.id + ", optionalKey: " + getStringValue(delta.optionalKey), e);
                }

                if (fieldValue == null)
                {
                    throw new RuntimeException(delta.cmd + " failed, null value at field: " + field.getName() + ", obj id: " +
                            delta.id + ", optionalKey: " + getStringValue(delta.optionalKey));
                }

                if (!type.isAssignableFrom(fieldValue.getClass()))
                {
                    throw new ClassCastException(delta.cmd + " failed, field: " + field.getName() + " is not of type: " +
                            type.getName() + ", obj id: " + delta.id + ", optionalKey: " + getStringValue(delta.optionalKey));
                }
                return fieldValue;
            }

            private static int getResizeValue(Delta delta)
            {
                boolean rightType = delta.optionalKey instanceof Integer ||
                        delta.optionalKey instanceof Long ||
                        delta.optionalKey instanceof Short ||
                        delta.optionalKey instanceof Byte ||
                        delta.optionalKey instanceof BigInteger;

                if (rightType && ((Number)delta.optionalKey).intValue() >= 0)
                {
                    return ((Number)delta.optionalKey).intValue();
                }
                else
                {
                    throw new IllegalArgumentException(delta.cmd + " failed, the optionalKey must be a integer value 0 or greater, field: " + delta.fieldName +
                            ", obj id: " + delta.id + ", optionalKey: " + getStringValue(delta.optionalKey));
                }
            }

            private static String getStringValue(Object foo)
            {
                if (foo == null)
                {
                    return "null";
                }
                else if (foo.getClass().isArray())
                {
                    StringBuilder s = new StringBuilder();
                    s.append('[');
                    int len = Array.getLength(foo);
                    for (int i=0; i < len; i++)
                    {
                        Object element = Array.get(foo, i);
                        s.append(element == null ? "null" : element.toString());
                        if (i < len - 1)
                        {
                            s.append(',');
                        }
                    }
                    s.append(']');
                    return s.toString();
                }
                return foo.toString();
            }
        }
    }

    /**
     * Perform the asymmetric graph delta.  This will compare two disparate graphs
     * and generate the necessary 'commands' to convert the source graph into the
     * target graph.  All nodes (cities) in the graph must be uniquely identifiable.
     * An ID interface must be passed in, where the supplied implementation of this, usually
     * done as an anonymous inner function, implements the ID.getId() method.  The
     * compare() function uses this interface to get the unique ID from the graph nodes.
     *
     * @return Collection of Delta records.  Each delta record records a difference
     * between graph A - B (asymmetric difference). It contains the information required to
     * morph B into A.  For example, if Graph B represents a stored object model in the
     * database, and Graph A is an inbound change of that graph, the deltas can be applied
     * to B such that the persistent storage will now be A.
     */
    public static List<Delta> compare(Object source, Object target, final ID idFetcher)
    {
        Set<Delta> deltas = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        LinkedList<Delta> stack = new LinkedList<>();
        stack.push(new Delta(0L, ROOT, ROOT, source, target, null));

        while (!stack.isEmpty())
        {
            Delta delta = stack.pop();
            String path = delta.srcPtr;

            if (!stack.isEmpty())
            {
                path += "." + System.identityHashCode(stack.peek().srcValue);
            }

            // for debugging
//            System.out.println("path = " + path);

            if (visited.contains(path))
            {   // handle cyclic graphs correctly.
                // srcPtr is taken into account (see Delta.equals()), which means
                // that an instance alone is not enough to skip, the pointer to it
                // must also be identical (before skipping it).
                continue;
            }
            final Object srcValue = delta.srcValue;
            final Object targetValue = delta.targetValue;

            visited.add(path);

            if (srcValue == targetValue)
            {   // Same instance is always equal to itself.
                continue;
            }

            if (srcValue == null || targetValue == null)
            {   // If either one is null, they are not equal (both can't be null, due to above comparison).
                delta.setCmd(OBJECT_ASSIGN_FIELD);
                deltas.add(delta);
                continue;
            }

            if (!srcValue.getClass().equals(targetValue.getClass()))
            {   // Must be same class when not a Map, Set, List.  This allows comparison to
                // ignore an ArrayList versus a LinkedList (only the contents will be checked).
                if (!((srcValue instanceof Map && targetValue instanceof Map) ||
                        (srcValue instanceof Set && targetValue instanceof Set) ||
                        (srcValue instanceof List && targetValue instanceof List)))
                {
                    delta.setCmd(OBJECT_FIELD_TYPE_CHANGED);
                    deltas.add(delta);
                    continue;
                }
            }

            if (isLogicalPrimitive(srcValue.getClass()))
            {
                if (!srcValue.equals(targetValue))
                {
                    delta.setCmd(OBJECT_ASSIGN_FIELD);
                    deltas.add(delta);
                }
                continue;
            }

            // Special handle [] types because they require CopyElement / Resize commands unique to Arrays.
            if (srcValue.getClass().isArray())
            {
                compareArrays(delta, deltas, stack, idFetcher);
                continue;
            }

            // Special handle Sets because they require Add/Remove commands unique to Sets
            if (srcValue instanceof Set)
            {
                compareSets(delta, deltas, stack, idFetcher);
                continue;
            }

            // Special handle Maps because they required Put/Remove commands unique to Maps
            if (srcValue instanceof Map)
            {
                compareMaps(delta, deltas, stack, idFetcher);
                continue;
            }

            // Special handle List because they require CopyElement / Resize commands unique to List
            if (srcValue instanceof List)
            {
                compareLists(delta, deltas, stack, idFetcher);
                continue;
            }

            if (srcValue instanceof Collection)
            {
                throw new RuntimeException("Detected custom Collection that does not extend List or Set: " +
                        srcValue.getClass().getName() + ". GraphUtils.compare() needs to be updated to support it, obj id: " + delta.id + ", field: " + delta.fieldName);
            }

            if (isIdObject(srcValue, idFetcher) && isIdObject(targetValue, idFetcher))
            {
                final Object srcId = idFetcher.getId(srcValue);
                final Object targetId = idFetcher.getId(targetValue);

                if (!srcId.equals(targetId))
                {   // Field references different object, need to create a command that assigns the new object to the field.
                    // This maintains 'Graph Shape'
                    delta.setCmd(OBJECT_ASSIGN_FIELD);
                    deltas.add(delta);
                    continue;
                }

                final Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(srcValue.getClass());
                String sysId = "(" + System.identityHashCode(srcValue) + ").";

                for (Field field : fields)
                {
                    try
                    {
                        String srcPtr = sysId + field.getName();
                        stack.push(new Delta(srcId, field.getName(), srcPtr, field.get(srcValue), field.get(targetValue), null));
                    }
                    catch (Exception ignored) { }
                }
            }
            else
            {   // Non-ID object, need to check for 'deep' equivalency (best we can do).  This works, but the change could
                // be at a lower level in the graph (overly safe).  However, without an ID, there is no way to point to the
                // lower level difference object.
                if (!DeepEquals.deepEquals(srcValue, targetValue))
                {
                    delta.setCmd(OBJECT_ASSIGN_FIELD);
                    deltas.add(delta);
                }
            }
        }

        // source objects by ID
        final Set potentialOrphans = new HashSet();
        Traverser.traverse(source, new Traverser.Visitor()
        {
            public void process(Object o)
            {
                if (isIdObject(o, idFetcher))
                {
                    potentialOrphans.add(idFetcher.getId(o));
                }
            }
        });

        // Remove all target objects from potential orphan map, leaving remaining objects
        // that are no longer referenced in the potentialOrphans map.
        Traverser.traverse(target, new Traverser.Visitor()
        {
            public void process(Object o)
            {
                if (isIdObject(o, idFetcher))
                {
                    potentialOrphans.remove(idFetcher.getId(o));
                }
            }
        });

        List forReturn = new ArrayList(deltas);
        // Generate DeltaCommands for orphaned objects
        for (Object id : potentialOrphans)
        {
            Delta orphanDelta = new Delta(id, null, "", null, null, null);
            orphanDelta.setCmd(OBJECT_ORPHAN);
            forReturn.add(orphanDelta);
        }

        return forReturn;
    }

    /**
     * @return boolean true if the passed in object is a 'Logical' primitive.  Logical primitive is defined
     * as all primitives plus primitive wrappers, String, Date, Calendar, Number, or Character
     */
    private static boolean isLogicalPrimitive(Class c)
    {
        return c.isPrimitive() ||
                String.class == c ||
                Date.class.isAssignableFrom(c) ||
                Number.class.isAssignableFrom(c) ||
                Boolean.class.isAssignableFrom(c) ||
                Calendar.class.isAssignableFrom(c) ||
                TimeZone.class.isAssignableFrom(c) ||
                Character.class == c;
    }

    private static boolean isIdObject(Object o, ID idFetcher)
    {
        if (o == null)
        {
            return false;
        }
        Class c = o.getClass();
        if (isLogicalPrimitive(c) ||
                c.isArray() ||
                Collection.class.isAssignableFrom(c) ||
                Map.class.isAssignableFrom(c) ||
                Object.class == c)
        {
            return false;
        }

        try
        {
            idFetcher.getId(o);
            return true;
        }
        catch (Exception ignored)
        {
            return false;
        }

    }
    /**
     * Deeply compare two Arrays []. Both arrays must be of the same type, same length, and all
     * elements within the arrays must be deeply equal in order to return true.  The appropriate
     * 'resize' or 'setElement' commands will be generated.
     */
    private static void compareArrays(Delta delta, Collection deltas, LinkedList stack, ID idFetcher)
    {
        int srcLen = Array.getLength(delta.srcValue);
        int targetLen = Array.getLength(delta.targetValue);

        if (srcLen != targetLen)
        {
            delta.setCmd(ARRAY_RESIZE);
            delta.setOptionalKey(targetLen);
            deltas.add(delta);
        }

        final String sysId = "(" + System.identityHashCode(delta.srcValue) + ')';
        final Class compType = delta.targetValue.getClass().getComponentType();

        if (isLogicalPrimitive(compType))
        {
            for (int i=0; i < targetLen; i++)
            {
                final Object targetValue = Array.get(delta.targetValue, i);
                String srcPtr = sysId + '[' + i + ']';

                if (i < srcLen)
                {   // Do positional check
                    final Object srcValue = Array.get(delta.srcValue, i);

                    if (srcValue == null && targetValue != null ||
                            srcValue != null && targetValue == null ||
                            !srcValue.equals(targetValue))
                    {
                        copyArrayElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                    }
                }
                else
                {   // Target array is larger, issue set-element-commands for each additional element
                    copyArrayElement(delta, deltas, srcPtr, null, targetValue, i);
                }
            }
        }
        else
        {   // Only map IDs in array when the array type is non-primitive
            for (int i = targetLen - 1; i >= 0; i--)
            {
                final Object targetValue = Array.get(delta.targetValue, i);
                String srcPtr = sysId + '[' + i + ']';

                if (i < srcLen)
                {   // Do positional check
                    final Object srcValue = Array.get(delta.srcValue, i);

                    if (targetValue == null || srcValue == null)
                    {
                        if (srcValue != targetValue)
                        {   // element was nulled out, create a command to copy it (no need to recurse [add to stack] because null has no depth)
                            copyArrayElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                        }
                    }
                    else if (isIdObject(srcValue, idFetcher) && isIdObject(targetValue, idFetcher))
                    {
                        Object srcId = idFetcher.getId(srcValue);
                        Object targetId = idFetcher.getId(targetValue);

                        if (targetId.equals(srcId))
                        {   // No need to copy, same object in same array position, but it's fields could have changed, so add the object to
                            // the stack for further graph delta comparison.
                            stack.push(new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetValue, i));
                        }
                        else
                        {   // IDs do not match?  issue a set-element-command
                            copyArrayElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                        }
                    }
                    else if (!DeepEquals.deepEquals(srcValue, targetValue))
                    {
                        copyArrayElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                    }
                }
                else
                {   // Target is larger than source - elements have been added, issue a set-element-command for each new position one at the end
                    copyArrayElement(delta, deltas, srcPtr, null, targetValue, i);
                }
            }
        }
    }

    private static void copyArrayElement(Delta delta, Collection deltas, String srcPtr, Object srcValue, Object targetValue, int index)
    {
        Delta copyDelta = new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetValue, index);
        copyDelta.setCmd(ARRAY_SET_ELEMENT);
        deltas.add(copyDelta);
    }

    /**
     * Deeply compare two Sets and generate the appropriate 'add' or 'remove' commands
     * to rectify their differences.
     */
    private static void compareSets(Delta delta, Collection deltas, LinkedList stack, ID idFetcher)
    {
        Set srcSet = (Set) delta.srcValue;
        Set targetSet = (Set) delta.targetValue;

        // Create ID to Object map for target Set
        Map targetIdToValue = new HashMap();
        for (Object targetValue : targetSet)
        {
            if (targetValue != null && isIdObject(targetValue, idFetcher))
            {   // Only map non-null target array elements
                targetIdToValue.put(idFetcher.getId(targetValue), targetValue);
            }
        }

        Map srcIdToValue = new HashMap();
        String sysId = "(" + System.identityHashCode(srcSet) + ").remove(";
        for (Object srcValue : srcSet)
        {
            String srcPtr = sysId + System.identityHashCode(srcValue) + ')';
            if (isIdObject(srcValue, idFetcher))
            {   // Only map non-null source array elements
                Object srcId = idFetcher.getId(srcValue);
                srcIdToValue.put(srcId, srcValue);

                if (targetIdToValue.containsKey(srcId))
                {   // Queue item for deep, field level check as the object is still there (it's fields could have changed).
                    stack.push(new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetIdToValue.get(srcId), null));
                }
                else
                {
                    Delta removeDelta = new Delta(delta.id, delta.fieldName, srcPtr, srcValue, null, null);
                    removeDelta.setCmd(SET_REMOVE);
                    deltas.add(removeDelta);
                }
            }
            else
            {
                if (!targetSet.contains(srcValue))
                {
                    Delta removeDelta = new Delta(delta.id, delta.fieldName, srcPtr, srcValue, null, null);
                    removeDelta.setCmd(SET_REMOVE);
                    deltas.add(removeDelta);
                }
            }
        }

        sysId = "(" + System.identityHashCode(targetSet) + ").add(";
        for (Object targetValue : targetSet)
        {
            String srcPtr = sysId + System.identityHashCode(targetValue) + ')';
            if (isIdObject(targetValue, idFetcher))
            {
                Object targetId = idFetcher.getId(targetValue);
                if (!srcIdToValue.containsKey(targetId))
                {
                    Delta addDelta = new Delta(delta.id, delta.fieldName, srcPtr, null, targetValue, null);
                    addDelta.setCmd(SET_ADD);
                    deltas.add(addDelta);
                }
            }
            else
            {
                if (!srcSet.contains(targetValue))
                {
                    Delta addDelta = new Delta(delta.id, delta.fieldName, srcPtr, null, targetValue, null);
                    addDelta.setCmd(SET_ADD);
                    deltas.add(addDelta);
                }
            }
        }

        // TODO: If LinkedHashSet, may need to issue commands to reorder...
    }

    /**
     * Deeply compare two Maps and generate the appropriate 'put' or 'remove' commands
     * to rectify their differences.
     */
    private static void compareMaps(Delta delta, Collection deltas, LinkedList stack, ID idFetcher)
    {
        Map<Object, Object> srcMap = (Map<Object, Object>) delta.srcValue;
        Map<Object, Object> targetMap = (Map<Object, Object>) delta.targetValue;

        // Walk source Map keys and see if they exist in target map.  If not, that entry needs to be removed.
        // If the key exists in both, then the value must tested for equivalence.  If !equal, then a PUT command
        // is created to re-associate target value to key.
        final String sysId = "(" + System.identityHashCode(srcMap) + ')';
        for (Map.Entry entry : srcMap.entrySet())
        {
            Object srcKey = entry.getKey();
            Object srcValue = entry.getValue();
            String srcPtr = sysId + "['" + System.identityHashCode(srcKey) + "']";

            if (targetMap.containsKey(srcKey))
            {
                Object targetValue = targetMap.get(srcKey);
                if (srcValue == null || targetValue == null)
                {   // Null value in either source or target
                    if (srcValue != targetValue)
                    {   // Value differed, must create PUT command to overwrite source value associated to key
                        addMapPutDelta(delta, deltas, srcPtr, targetValue, srcKey);
                    }
                }
                else if (isIdObject(srcValue, idFetcher) && isIdObject(targetValue, idFetcher))
                {   // Both source and destination have same object (by id) as the value, add delta to stack (field-by-field check for item).
                    if (idFetcher.getId(srcValue).equals(idFetcher.getId(targetValue)))
                    {
                        stack.push(new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetValue, null));
                    }
                    else
                    {   // Different ID associated to same key, must create PUT command to overwrite source value associated to key
                        addMapPutDelta(delta, deltas, srcPtr, targetValue, srcKey);
                    }
                }
                else if (!DeepEquals.deepEquals(srcValue, targetValue))
                {   // Non-null, non-ID value associated to key, and the two values are not equal.  Create PUT command to overwrite.
                    addMapPutDelta(delta, deltas, srcPtr, targetValue, srcKey);
                }
            }
            else
            {   // target does not have this Key in it's map, therefore create REMOVE command to remove it from source map.
                Delta removeDelta = new Delta(delta.id, delta.fieldName, srcPtr, srcValue, null, srcKey);
                removeDelta.setCmd(MAP_REMOVE);
                deltas.add(removeDelta);
            }
        }

        for (Map.Entry entry : targetMap.entrySet())
        {
            Object targetKey = entry.getKey();
            String srcPtr = sysId + "['" + System.identityHashCode(targetKey) + "']";

            if (!srcMap.containsKey(targetKey))
            {   // Add Delta command map.put
                Delta putDelta = new Delta(delta.id, delta.fieldName, srcPtr, null, entry.getValue(), targetKey);
                putDelta.setCmd(MAP_PUT);
                deltas.add(putDelta);
            }
        }
        // TODO: If LinkedHashMap, may need to issue commands to reorder...
    }

    private static void addMapPutDelta(Delta delta, Collection deltas, String srcPtr, Object targetValue, Object key)
    {
        Delta putDelta = new Delta(delta.id, delta.fieldName, srcPtr, null, targetValue, key);
        putDelta.setCmd(MAP_PUT);
        deltas.add(putDelta);
    }

    /**
     * Deeply compare two Lists and generate the appropriate 'resize' or 'set' commands
     * to rectify their differences.
     */
    private static void compareLists(Delta delta, Collection deltas, LinkedList stack, ID idFetcher)
    {
        List srcList = (List) delta.srcValue;
        List targetList = (List) delta.targetValue;
        int srcLen = srcList.size();
        int targetLen = targetList.size();

        if (srcLen != targetLen)
        {
            delta.setCmd(LIST_RESIZE);
            delta.setOptionalKey(targetLen);
            deltas.add(delta);
        }

        final String sysId = "(" + System.identityHashCode(srcList) + ')';
        for (int i = targetLen - 1; i >= 0; i--)
        {
            final Object targetValue = targetList.get(i);
            String srcPtr = sysId + '{' + i + '}';

            if (i < srcLen)
            {   // Do positional check
                final Object srcValue = srcList.get(i);

                if (targetValue == null || srcValue == null)
                {
                    if (srcValue != targetValue)
                    {   // element was nulled out, create a command to copy it (no need to recurse [add to stack] because null has no depth)
                        copyListElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                    }
                }
                else if (isIdObject(srcValue, idFetcher) && isIdObject(targetValue, idFetcher))
                {
                    Object srcId = idFetcher.getId(srcValue);
                    Object targetId = idFetcher.getId(targetValue);

                    if (targetId.equals(srcId))
                    {   // No need to copy, same object in same List position, but it's fields could have changed, so add the object to
                        // the stack for further graph delta comparison.
                        stack.push(new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetValue, i));
                    }
                    else
                    {   // IDs do not match?  issue a set-element-command
                        copyListElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                    }
                }
                else if (!DeepEquals.deepEquals(srcValue, targetValue))
                {
                    copyListElement(delta, deltas, srcPtr, srcValue, targetValue, i);
                }
            }
            else
            {   // Target is larger than source - elements have been added, issue a set-element-command for each new position one at the end
                copyListElement(delta, deltas, srcPtr, null, targetValue, i);
            }
        }
    }

    private static void copyListElement(Delta delta, Collection deltas, String srcPtr, Object srcValue, Object targetValue, int index)
    {
        Delta copyDelta = new Delta(delta.id, delta.fieldName, srcPtr, srcValue, targetValue, index);
        copyDelta.setCmd(LIST_SET_ELEMENT);
        deltas.add(copyDelta);
    }

    /**
     * Apply the Delta commands to the source object graph, making
     * the requested changes to the source graph.  The source of the
     * commands is typically generated from the output of the 'compare()'
     * API, where this source graph was compared to another target
     * graph, and the delta commands were generated from that comparison.
     *
     * @param source Source object graph
     * @param commands List of Delta commands.  These commands carry the
     * information required to identify the nodes to be modified, as well
     * as the values to modify them to (including commands to resize arrays,
     * set values into arrays, set fields to specific values, put new entries
     * into Maps, etc.
     * @return List<DeltaError> which contains the String error message
     * describing why the Delta could not be applied, and a reference to the
     * Delta that was attempted to be applied.
     */
    public static List<DeltaError> applyDelta(Object source, List<Delta> commands, final ID idFetcher, DeltaProcessor deltaProcessor, boolean ... failFast)
    {
        // Index all objects in source graph
        final Map srcMap = new HashMap();
        Traverser.traverse(source, new Traverser.Visitor()
        {
            public void process(Object o)
            {
                if (isIdObject(o, idFetcher))
                {
                    srcMap.put(idFetcher.getId(o), o);
                }
            }
        });

        List<DeltaError> errors = new ArrayList<>();
        boolean failQuick = failFast != null && failFast.length == 1 && failFast[0];

        for (Delta delta : commands)
        {
            if (failQuick && errors.size() == 1)
            {
                return errors;
            }

            Object srcValue = srcMap.get(delta.id);
            if (srcValue == null)
            {
                errors.add(new DeltaError(delta.cmd + " failed, source object not found, obj id: " + delta.id, delta));
                continue;
            }

            Map<String, Field> fields = ReflectionUtils.getDeepDeclaredFieldMap(srcValue.getClass());
            Field field = fields.get(delta.fieldName);
            if (field == null && OBJECT_ORPHAN != delta.cmd)
            {
                errors.add(new DeltaError(delta.cmd + " failed, field name missing: " + delta.fieldName + ", obj id: " + delta.id, delta));
                continue;
            }

//            if (LOG.isDebugEnabled())
//            {
//                LOG.debug(delta.toString());
//            }

            try
            {
                switch (delta.cmd)
                {
                    case ARRAY_SET_ELEMENT:
                        deltaProcessor.processArraySetElement(srcValue, field, delta);
                        break;

                    case ARRAY_RESIZE:
                        deltaProcessor.processArrayResize(srcValue, field, delta);
                        break;

                    case OBJECT_ASSIGN_FIELD:
                        deltaProcessor.processObjectAssignField(srcValue, field, delta);
                        break;

                    case OBJECT_ORPHAN:
                        deltaProcessor.processObjectOrphan(srcValue, field, delta);
                        break;

                    case OBJECT_FIELD_TYPE_CHANGED:
                        deltaProcessor.processObjectTypeChanged(srcValue, field, delta);
                        break;

                    case SET_ADD:
                        deltaProcessor.processSetAdd(srcValue, field, delta);
                        break;

                    case SET_REMOVE:
                        deltaProcessor.processSetRemove(srcValue, field, delta);
                        break;

                    case MAP_PUT:
                        deltaProcessor.processMapPut(srcValue, field, delta);
                        break;

                    case MAP_REMOVE:
                        deltaProcessor.processMapRemove(srcValue, field, delta);
                        break;

                    case LIST_RESIZE:
                        deltaProcessor.processListResize(srcValue, field, delta);
                        break;

                    case LIST_SET_ELEMENT:
                        deltaProcessor.processListSetElement(srcValue, field, delta);
                        break;

                    default:
                        errors.add(new DeltaError("Unknown command: " + delta.cmd, delta));
                        break;
                }
            }
            catch(Exception e)
            {
                StringBuilder str = new StringBuilder();
                Throwable t = e;
                do
                {
                    str.append(t.getMessage());
                    t = t.getCause();
                    if (t != null)
                    {
                        str.append(", caused by: ");
                    }
                } while (t != null);
                errors.add(new DeltaError(str.toString(), delta));
            }
        }

        return errors;
    }

    /**
     * @return DeltaProcessor that handles updating Java objects
     * with Delta commands.  The typical use is to update the
     * source graph objects with Delta commands to bring it to
     * match the target graph.
     */
    public static DeltaProcessor getJavaDeltaProcessor()
    {
        return new JavaDeltaProcessor();
    }

    private static class JavaDeltaProcessor implements DeltaProcessor
    {
        public void processArraySetElement(Object source, Field field, Delta delta)
        {
            if (!field.getType().isArray())
            {
                throw new RuntimeException(delta.cmd + " failed, field: " + field.getName() + " is not an Array [] type, obj id: " +
                        delta.id + ", position: " + Helper.getStringValue(delta.optionalKey));
            }

            Object sourceArray = Helper.getFieldValueAs(source, field, field.getType(), delta);
            int pos = Helper.getResizeValue(delta);
            int srcArrayLen = Array.getLength(sourceArray);

            if (pos >= srcArrayLen)
            {   // pos < 0 already checked in getResizeValue()
                throw new ArrayIndexOutOfBoundsException(delta.cmd + " failed, index out of bounds: " + pos +
                        ", array size: " + srcArrayLen + ", field: " + field.getName() + ", obj id: " + delta.id);
            }

            Array.set(sourceArray, pos, delta.targetValue);
        }

        public void processArrayResize(Object source, Field field, Delta delta)
        {
            if (!field.getType().isArray())
            {
                throw new RuntimeException(delta.cmd + " failed, field: " + field.getName() + " is not an Array [] type, obj id: " +
                        delta.id + ", new size: " + Helper.getStringValue(delta.optionalKey));
            }

            int newSize = Helper.getResizeValue(delta);
            Object sourceArray = Helper.getFieldValueAs(source, field, field.getType(), delta);
            int maxKeepLen = Math.min(newSize, Array.getLength(sourceArray));
            Object newArray = Array.newInstance(field.getType().getComponentType(), newSize);
            System.arraycopy(sourceArray, 0, newArray, 0, maxKeepLen);

            try
            {
                field.set(source, newArray);
            }
            catch (Exception e)
            {
                throw new RuntimeException(delta.cmd + " failed, could not reassign array to field: " + field.getName() + " with value: " +
                        Helper.getStringValue(delta.targetValue) + ", obj id: " + delta.id + ", optionalKey: " + delta.optionalKey, e);
            }
        }

        public void processObjectAssignField(Object source, Field field, Delta delta)
        {
            try
            {
                field.set(source, delta.targetValue);
            }
            catch (Exception e)
            {
                throw new RuntimeException(delta.cmd + " failed, unable to set object field: " + field.getName() +
                        " with value: " + Helper.getStringValue(delta.targetValue) + ", obj id: " + delta.id, e);
            }
        }

        public void processObjectOrphan(Object srcValue, Field field, Delta delta)
        {
            // Do nothing
        }

        public void processObjectTypeChanged(Object srcValue, Field field, Delta delta)
        {
            throw new RuntimeException(delta.cmd + " failed, field: " + field.getName() + ", obj id: " + delta.id);
        }

        public void processSetAdd(Object source, Field field, Delta delta)
        {
            Set set = (Set) Helper.getFieldValueAs(source, field, Set.class, delta);
            set.add(delta.getTargetValue());
        }

        public void processSetRemove(Object source, Field field, Delta delta)
        {
            Set set = (Set) Helper.getFieldValueAs(source, field, Set.class, delta);
            set.remove(delta.getSourceValue());
        }

        public void processMapPut(Object source, Field field, Delta delta)
        {
            Map map = (Map) Helper.getFieldValueAs(source, field, Map.class, delta);
            map.put(delta.optionalKey, delta.getTargetValue());
        }

        public void processMapRemove(Object source, Field field, Delta delta)
        {
            Map map = (Map) Helper.getFieldValueAs(source, field, Map.class, delta);
            map.remove(delta.optionalKey);
        }

        public void processListResize(Object source, Field field, Delta delta)
        {
            List list = (List) Helper.getFieldValueAs(source, field, List.class, delta);
            int newSize = Helper.getResizeValue(delta);
            int deltaLen = newSize - list.size();

            if (deltaLen > 0)
            {   // grow list
                for (int i=0; i < deltaLen; i++)
                {   // Pad list out with nulls
                    list.add(null);
                }
            }
            else if (deltaLen < 0)
            {   // shrink list
                deltaLen = -deltaLen;
                for (int i=0; i < deltaLen; i++)
                {
                    list.remove(list.size() - 1);
                }
            }
        }

        public void processListSetElement(Object source, Field field, Delta delta)
        {
            List list = (List) Helper.getFieldValueAs(source, field, List.class, delta);
            int pos = Helper.getResizeValue(delta);
            int listLen = list.size();

            if (pos >= listLen)
            {   // pos < 0 already checked in getResizeValue()
                throw new IndexOutOfBoundsException(delta.cmd + " failed, index out of bounds: " +
                        pos + ", list size: " + list.size() + ", field: " + field.getName() + ", obj id: " + delta.id);
            }

            list.set(pos, delta.targetValue);
        }
    }
}
