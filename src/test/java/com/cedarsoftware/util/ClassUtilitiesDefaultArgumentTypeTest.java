package com.cedarsoftware.util;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default newInstance() supplies for a parameter it has no argument for is of the parameter's type, or null. A
 * default was taken from the first interface mapping the type implements, whatever that mapping made: an
 * ArrayBlockingQueue parameter got a LinkedBlockingQueue, a GregorianCalendar or a Path an ArrayList, a Writer a
 * ByteArrayInputStream. The constructor then refused the defaults, and every parameter became null.
 */
class ClassUtilitiesDefaultArgumentTypeTest {

    public static class Q {
        final ArrayBlockingQueue<?> queue;
        final List<?> list;

        public Q(ArrayBlockingQueue<?> queue, List<?> list) {
            this.queue = queue;
            this.list = list;
        }
    }

    private static Object defaultFor(Class<?> type) throws Exception {
        Method method = ClassUtilities.class.getDeclaredMethod("getArgForType", Converter.class, Class.class);
        method.setAccessible(true);
        return method.invoke(null, new Converter(new DefaultConverterOptions()), type);
    }

    @Test
    void aParameterWithoutAUsableDefaultDoesNotCostTheOthersTheirs() {
        Q q = (Q) ClassUtilities.newInstance(Q.class, Collections.emptyList());
        assertNull(q.queue);
        assertNotNull(q.list, "the List parameter still gets its empty list");
        assertTrue(q.list.isEmpty());
    }

    @Test
    void everyDefaultIsOfItsParametersTypeOrNull() throws Exception {
        Class<?>[] types = {ArrayBlockingQueue.class, PriorityBlockingQueue.class, AbstractQueue.class,
                GregorianCalendar.class, Calendar.class, Path.class, Writer.class, OutputStream.class,
                AbstractSequentialList.class, AbstractList.class, AbstractSet.class, AbstractMap.class,
                List.class, Set.class, Map.class, NavigableMap.class, Deque.class, Queue.class, BlockingQueue.class,
                Collection.class, Iterable.class, Iterator.class, CharSequence.class, AutoCloseable.class,
                Cloneable.class};
        for (Class<?> type : types) {
            Object value = defaultFor(type);
            assertTrue(value == null || type.isInstance(value),
                    () -> type.getSimpleName() + " got a " + value.getClass().getName());
        }
    }

    @Test
    void theMappedTypesKeepTheirDefaults() throws Exception {
        assertEquals(java.util.ArrayList.class, defaultFor(List.class).getClass());
        assertEquals(java.util.ArrayList.class, defaultFor(AbstractList.class).getClass());
        assertEquals(java.util.LinkedHashMap.class, defaultFor(Map.class).getClass());
        assertEquals(java.util.LinkedHashMap.class, defaultFor(AbstractMap.class).getClass());
        assertEquals(java.util.concurrent.LinkedBlockingQueue.class, defaultFor(BlockingQueue.class).getClass());
        assertEquals(StringBuilder.class, defaultFor(CharSequence.class).getClass());
    }
}
