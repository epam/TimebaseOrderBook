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

import com.epam.deltix.timebase.messages.universal.QuoteSide;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Represents orders list sorted by price.
 *
 * @param <Quote> type of quote
 * @author Andrii_Ostapenko1
 * @see QuoteSide
 */
public interface MarketSide<Quote> extends IterableMarketSide<Quote> {

    /**
     * Get side of quote(ASK or BID).
     *
     * @return - side of quote.
     * @see QuoteSide
     */
    QuoteSide getSide();

    /**
     * Get best quote.
     *
     * @return Best quote from side.
     */
    Quote getBestQuote();

    /**
     * Get quote by level.
     *
     * @param level - level to use
     * @return quote.
     */
    Quote getQuote(int level);

    /**
     * Return current depth of market.
     *
     * @return depth of market
     */
    int depth();

    /**
     * Returns true if this market side contains no elements.
     *
     * @return true if this market side contains no elements
     */
    boolean isEmpty();

    /**
     * Returns true if this market side contains given quote level.
     *
     * @param level - quote level to use.
     * @return true if this market side contains given quote level
     */
    boolean hasLevel(short level);

    @Override
    default Iterator<Quote> iterator() {
        return iterator((short) 0);
    }

    @Override
    default Iterator<Quote> iterator(final short fromLevel) {
        return iterator(fromLevel, (short) depth());
    }

    @Override
    default void forEach(final Predicate<Quote> action) {
        forEach((short) 0, (short) depth(), action);
    }

    @Override
    default void forEach(final short level, final Predicate<Quote> action) {
        forEach((short) 0, level, action);
    }

    @Override
    default void forEach(final short fromLevel, final short toLevel, final Predicate<Quote> action) {
        Objects.requireNonNull(action);
        for (int i = fromLevel; i < toLevel; i++) {
            if (!action.test(getQuote(i))) {
                return;
            }
        }
    }

    @Override
    default <Cookie> void forEach(final BiPredicate<Quote, Cookie> action, final Cookie cookie) {
        forEach((short) 0, (short) depth(), action, cookie);
    }

    @Override
    default <Cookie> void forEach(final short fromLevel, final BiPredicate<Quote, Cookie> action, final Cookie cookie) {
        forEach(fromLevel, (short) depth(), action, cookie);
    }

    @Override
    default <Cookie> void forEach(final short fromLevel,
                                  final short toLevel,
                                  final BiPredicate<Quote, Cookie> action,
                                  final Cookie cookie) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(cookie);
        for (int i = fromLevel; i < toLevel; i++) {
            if (!action.test(getQuote(i), cookie)) {
                return;
            }
        }
    }
}
