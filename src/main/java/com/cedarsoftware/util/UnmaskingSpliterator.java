package com.cedarsoftware.util;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A spliterator over a null-safe collection's backing view that hands out each element unmasked (the null sentinel
 * as null) and otherwise traverses as the backing spliterator does: weakly consistent and never sized, so a stream
 * over the view does not fail when the collection changes meanwhile. The Collection default was an iterator
 * spliterator with a fixed size, which threw IllegalStateException when the source grew or shrank mid-stream.
 * Not NONNULL; when the backing spliterator is SORTED, it reports the view's own comparator.
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
final class UnmaskingSpliterator<S, T> implements Spliterator<T> {
    private final Spliterator<S> delegate;
    private final Function<? super S, ? extends T> unmask;
    private final Comparator<? super T> comparator;

    /**
     * @param delegate   the backing view's spliterator
     * @param unmask     turns a backing element into the element the view hands out
     * @param comparator the view's comparator (null for natural order), reported when the delegate is SORTED
     */
    UnmaskingSpliterator(Spliterator<S> delegate, Function<? super S, ? extends T> unmask,
                         Comparator<? super T> comparator) {
        this.delegate = delegate;
        this.unmask = unmask;
        this.comparator = comparator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return delegate.tryAdvance(s -> action.accept(unmask.apply(s)));
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        delegate.forEachRemaining(s -> action.accept(unmask.apply(s)));
    }

    @Override
    public Spliterator<T> trySplit() {
        Spliterator<S> split = delegate.trySplit();
        return split == null ? null : new UnmaskingSpliterator<>(split, unmask, comparator);
    }

    @Override
    public long estimateSize() {
        return delegate.estimateSize();
    }

    @Override
    public int characteristics() {
        return delegate.characteristics() & ~Spliterator.NONNULL;
    }

    @Override
    public Comparator<? super T> getComparator() {
        if (!hasCharacteristics(Spliterator.SORTED)) {
            throw new IllegalStateException();
        }
        return comparator;
    }
}
