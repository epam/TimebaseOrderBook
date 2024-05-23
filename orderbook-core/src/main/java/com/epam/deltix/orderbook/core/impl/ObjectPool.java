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
package com.epam.deltix.orderbook.core.impl;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Never-shrinking version of ObjectPool. Not thread safe
 */
public final class ObjectPool<T> {

    private final Supplier<T> factory;
    private final Consumer<T> releaseCallback;
    private Object[] array;
    private int size;

    //TODO add javadoc
    public ObjectPool(final int initialSize, final Supplier<T> factory) {
        this(initialSize, factory, null);
    }

    public ObjectPool(final int initialSize, final Supplier<T> factory, final Consumer<T> releaseCallback) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Illegal size: " + initialSize);
        }
        this.releaseCallback = releaseCallback;

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
        if (size > 0) {
            final int last = --size;
            final Object item = array[last];
            array[last] = null; // clear reference to borrowed item
            assert item != null;
            return (T) item;
        } else {
            return factory.get();
        }
    }

    public void release(final T item) {
        if (item != null) {
            if (size == array.length) {
                array = Arrays.copyOf(array, size << 1);
            }

            array[size++] = item;

            if (releaseCallback != null) {
                releaseCallback.accept(item);
            }
        }
    }

    public int getTotalSize() {
        return size;
    }

    /**
     * Clear all entries and release all cached objects to Java Garbage Collector
     */
    private void clear() {
        Arrays.fill(array, 0, size, null);
        size = 0;
    }

}
