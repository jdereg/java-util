package com.cedarsoftware.util.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Single-flight loads by key, for a cache's computeIfAbsent: one thread runs the load for a key while any other
 * thread asking for that key meanwhile waits for its result (or its exception), and loads of different keys run in
 * parallel. Nothing is kept once a load ends. Internal to java-util.
 * <p>
 * A load that asks for its own key again waits for itself: it throws IllegalStateException, as
 * ConcurrentHashMap.computeIfAbsent does. Loads that wait on each other's keys in a cycle, on different threads,
 * deadlock, as they would in ConcurrentHashMap.
 *
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
public final class KeyedLoads {

    /** A cache's load of a key no other thread is loading: run the function, write the result, return the value. */
    @FunctionalInterface
    public interface Load<K, V> {
        V apply(K key, Function<? super K, ? extends V> function);
    }

    private static final Object NULL_KEY = new Object();

    private final ConcurrentHashMap<Object, Flight> flights = new ConcurrentHashMap<>();

    /**
     * One load in progress. The loading thread finishes it with volatile writes alone; its monitor is taken only
     * when another thread has waited for it.
     */
    private static final class Flight {
        final Thread loader = Thread.currentThread();
        private Object value;
        private Throwable failure;
        private volatile boolean done;
        private volatile boolean waited;

        void finish(Object result, Throwable thrown) {
            value = result;
            failure = thrown;
            done = true;                    // publishes value and failure
            if (waited) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }

        /** The load's value, waiting for it, not interruptibly (as a ConcurrentHashMap compute waits). */
        Object await() {
            boolean interrupted = false;
            synchronized (this) {
                waited = true;
                while (!done) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            return value;
        }
    }

    /**
     * {@code load} applied to the key and function by this thread, unless another thread is loading the key: then
     * that load's value, or its exception, after waiting for it.
     */
    @SuppressWarnings("unchecked")
    public <K, V> V load(K key, Function<? super K, ? extends V> function, Load<K, V> load) {
        Object k = key == null ? NULL_KEY : key;
        Flight mine = new Flight();
        Flight running = flights.putIfAbsent(k, mine);
        if (running != null) {
            if (running.loader == mine.loader) {
                throw new IllegalStateException("Recursive load: the load of a key asked for that key again");
            }
            return (V) running.await();
        }
        try {
            V value = load.apply(key, function);
            mine.finish(value, null);
            return value;
        } catch (RuntimeException | Error e) {
            mine.finish(null, e);
            throw e;
        } finally {
            flights.remove(k, mine);
        }
    }
}
