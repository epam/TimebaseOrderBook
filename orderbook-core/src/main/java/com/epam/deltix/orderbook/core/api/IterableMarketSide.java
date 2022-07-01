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
package com.epam.deltix.orderbook.core.api;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * A {@link MarketSide} with a fully {@link Iterable} set of entries.
 * Implementations of this interface <strong>must</strong> be able
 * to iterate over all contained quotes.
 *
 * @author Andrii_Ostapenko1
 */
public interface IterableMarketSide<Quote> extends Iterable<Quote> {

    /**
     * Returns an iterator over the elements in this market side in proper sequence.
     *
     * @return an iterator over the elements in this market side in proper sequence
     */
    Iterator<Quote> iterator();

    /**
     * Returns an iterator over the elements in this market side in proper sequence.
     *
     * @param fromLevel - Starting price level index to use
     * @return an iterator over the elements in this order book in proper sequence
     */
    Iterator<Quote> iterator(short fromLevel);

    /**
     * Returns an iterator over the elements in this market side in proper sequence.
     *
     * @param fromLevel - Starting price level index to use
     * @param toLevel   - End price level index to use
     * @return an iterator over the elements in this order book in proper sequence
     */
    Iterator<Quote> iterator(short fromLevel, short toLevel);

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
    default Stream<Quote> stream(final boolean parallel) {
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
    default Stream<Quote> stream() {
        return stream(false);
    }

    void forEach(Predicate<Quote> action);

    void forEach(short fromLevel, Predicate<Quote> action);

    void forEach(short fromLevel, short toLevel, Predicate<Quote> action);

    <Cookie> void forEach(BiPredicate<Quote, Cookie> action, Cookie cookie);

    <Cookie> void forEach(short fromLevel, BiPredicate<Quote, Cookie> action, Cookie cookie);

    <Cookie> void forEach(short fromLevel, short toLevel, BiPredicate<Quote, Cookie> action, Cookie cookie);
}
