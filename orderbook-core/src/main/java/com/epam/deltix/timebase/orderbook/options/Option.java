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
package com.epam.deltix.timebase.orderbook.options;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Property class
 *
 * @param <T> stored value.
 */
public class Option<T> {

    /**
     * Common instance for {@code empty()}.
     */
    private static final Option<?> EMPTY = new Option<>();

    private final T data;

    /**
     * Prevents instantiation
     */
    private Option() {
        this.data = null;
    }

    private Option(final T data) {
        Objects.requireNonNull(data, "Option can't be null");
        this.data = data;
    }

    /**
     * Create empty parameter
     *
     * @param <T> type
     * @return empty option
     */
    @SuppressWarnings("unchecked")
    public static <T> Option<T> empty() {
        return (Option<T>) EMPTY;
    }

    /**
     * Wrap the existing data in option
     *
     * @param data value to wrap
     * @param <T> type
     * @return option with data
     */
    public static <T> Option<T> wrap(final T data) {
        return new Option<>(data);
    }

    public T orElse(final T elseData) {
        if (data == null) {
            return elseData;
        }
        return data;
    }

    public Option<T> orAnother(final Option<T> another) {
        if (data == null) {
            return another;
        }
        return this;
    }

    public boolean hasValue() {
        return Objects.nonNull(data);
    }

    public T get() {
        Objects.requireNonNull(data, "Parameter is null!");
        return data;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Option.class.getSimpleName() + "[", "]")
                .add("data=" + (data != null ? data : "[]"))
                .toString();
    }

}
