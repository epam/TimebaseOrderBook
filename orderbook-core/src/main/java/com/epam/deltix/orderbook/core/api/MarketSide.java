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

import com.epam.deltix.dfp.Decimal;
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
     * @return Best quote from side or null if quote not found
     */
    Quote getBestQuote();

    /**
     * Get worst quote.
     *
     * @return Worst quote from side or null if quote not found
     */
    Quote getWorstQuote();

    /**
     * Get quote by level. WARNING: this method can be slow for some implementations (for example, in L3 order book). Use iterator instead.
     *
     * @param level - level to use
     * @return quote or null if quote not found
     */
    Quote getQuote(int level);

    /**
     * Return current depth of market.
     *
     * @return depth of market
     */
    int depth();

    /**
     * The total number of positions (orders size) being bought/sold.
     * <p>
     * Return Decimal64Utils#ZERO if market side is empty.
     *
     * @return total trade quantity
     * @see com.epam.deltix.dfp.Decimal
     * @see com.epam.deltix.dfp.Decimal64Utils#ZERO
     * @see com.epam.deltix.timebase.messages.universal.BasePriceEntry#setSize(long)
     */
    @Decimal
    long getTotalQuantity();

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
    boolean hasLevel(int level);

    /**
     * Get quote by quoteId.
     * Unsupported operation for L2, always returns null.
     *
     * @param quoteId - Quote Id
     * @return quote or null if quote not found
     */
    Quote getQuote(CharSequence quoteId);

    /**
     * Unsupported operation for L2, always returns false.
     *
     * @param quoteId - Quote Id
     * @return true if this market side contains given quote ID
     */
    boolean hasQuote(CharSequence quoteId);

    @Override
    default Iterator<Quote> iterator() {
        return iterator(0);
    }

    @Override
    default Iterator<Quote> iterator(final int fromLevel) {
        return iterator(fromLevel, depth());
    }

    @Override
    default void forEach(final Predicate<Quote> action) {
        forEach(0, depth(), action);
    }

    @Override
    default void forEach(final int level, final Predicate<Quote> action) {
        forEach(0, level, action);
    }

    @Override
    default void forEach(final int fromLevel, final int toLevel, final Predicate<Quote> action) {
        Objects.requireNonNull(action);
        for (int i = fromLevel; i < toLevel; i++) {
            if (!action.test(getQuote(i))) {
                return;
            }
        }
    }

    @Override
    default <Cookie> void forEach(final BiPredicate<Quote, Cookie> action, final Cookie cookie) {
        forEach(0, depth(), action, cookie);
    }

    @Override
    default <Cookie> void forEach(final int fromLevel, final BiPredicate<Quote, Cookie> action, final Cookie cookie) {
        forEach(fromLevel, depth(), action, cookie);
    }

    @Override
    default <Cookie> void forEach(final int fromLevel,
                                  final int toLevel,
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
