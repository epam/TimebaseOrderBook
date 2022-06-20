/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.timebase.orderbook.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Never-shrinking version of ObjectPool. Not thread safe
 */
public final class ObjectPool<T extends MutableOrderBookQuote> {

    private final Supplier<T> factory;

    private Object[] array;
    private int size;

    public ObjectPool(final int initialSize, final Supplier<T> factory) {
        final Object[] array = new Object[(initialSize == 0) ? 1 : initialSize];

        for (int i = 0; i < initialSize; i++) {
            final T item = factory.get();
            assert item != null;
            array[i] = item;
        }

        this.size = initialSize;
        this.factory = factory;
        this.array = array;
    }

    @SuppressWarnings("unchecked")
    public T borrow() {
        final Object item;
        if (size > 0) {
            item = array[--size];
        } else {
            item = factory.get();
        }
        assert item != null;
        return (T) item;
    }

    public void release(final T item) {
        if (item != null) {
            if (size == array.length) {
                array = Arrays.copyOf(array, size << 1);
            }

            array[size++] = item;
            item.release();// TODO ???
        }
    }

    /**
     * Returns all given items into pool and clears input list (!)
     */
    public void releaseAndClean(final List<T> items) {
        final int cnt = items.size();
        final int capacity = size + cnt;

        if (capacity > array.length) {
            array = Arrays.copyOf(array, capacity + (capacity >> 3));
        }

        for (int i = 0; i < cnt; i++) {
            final T item = items.get(i);

            if (item != null) {
                array[size++] = item;
            }
        }

        items.clear();
    }

    public int getTotalSize() {
        return size;
    }

    /**
     * Clear all entries and release all cached objects to Java Garbage Collector
     */
    public void clear() {
        Arrays.fill(array, 0, size, null);
        size = 0;
    }

}
