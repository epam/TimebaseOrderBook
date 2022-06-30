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
package com.epam.deltix.timebase.orderbook.api;

import com.epam.deltix.timebase.orderbook.options.Option;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents stock exchange list.
 * <p>
 * A {@link ExchangeList} with a fully {@link Iterable} set of entries.
 *
 * @author Andrii_Ostapenko1
 */
public interface ExchangeList<StockExchange> extends Iterable<StockExchange> {

    /**
     * Get exchange holder by id.
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     *
     * @param exchangeId -  id of exchange to use.
     * @return an {@code Optional} containing the exchange holder; never {@code null} but
     * potentially empty
     */
    Option<StockExchange> getById(long exchangeId);

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    int size();

    /**
     * Returns true if this list contains no stock exchange.
     *
     * @return true if this list contains no elements
     */
    boolean isEmpty();

    /**
     * Creates a new sequential or parallel {@code Stream} from a
     * {@code Spliterator}.
     * <p>
     * Note: This method allocates memory!
     *
     * @param parallel if {@code true} then the returned stream is a parallel
     *                 stream; if {@code false} the returned stream is a sequential
     *                 stream.
     * @return a new sequential or parallel {@code Stream}
     * @see Stream
     * @see java.util.Spliterator
     */
    default Stream<StockExchange> stream(final boolean parallel) {
        return StreamSupport.stream(spliterator(), parallel);
    }

    /**
     * Creates a new sequential  {@code Stream} from a
     * {@code Spliterator}.
     * <p>
     * Note: This method allocates memory!
     *
     * @return a new sequential {@code Stream}
     * @see Stream
     * @see java.util.Spliterator
     */
    default Stream<StockExchange> stream() {
        return stream(false);
    }

    default void forEach(final Predicate<StockExchange> action) {
        for (final StockExchange e : this) {
            if (!action.test(e)) {
                return;
            }
        }
    }

    default <Cookie> void forEach(final BiPredicate<StockExchange, Cookie> action,
                                  final Cookie cookie) {
        for (final StockExchange e : this) {
            if (!action.test(e, cookie)) {
                return;
            }
        }
    }

}
