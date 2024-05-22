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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.timebase.messages.universal.BookUpdateAction;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.annotations.Alphanumeric;

import java.util.Objects;

/**
 * @author Andrii_Ostapenko1
 */
interface L2MarketSide<Quote> extends MarketSide<Quote> {

    int NOT_FOUND = -1;

    static <Quote extends MutableOrderBookQuote> L2MarketSide<Quote> factory(final int initialDepth,
                                                                             final int maxDepth,
                                                                             final QuoteSide side) {
        Objects.requireNonNull(side);
        switch (side) {
            case BID:
                return new AbstractL2MarketSide.BID<>(initialDepth, maxDepth);
            case ASK:
                return new AbstractL2MarketSide.ASK<>(initialDepth, maxDepth);
            default:
                throw new IllegalStateException("Unexpected value: " + side);
        }
    }

    @Override
    default Quote getQuote(final CharSequence quoteId) {
        // Not supported for L2
        return null;
    }

    @Override
    default boolean hasQuote(final CharSequence quoteId) {
        // Not supported for L2
        return false;
    }

    /**
     * Inserts the specified quote at the specified level. Shifts the quotes right (adds one to their indices).
     *
     * @param level  level  to be inserted
     * @param insert quote to be inserted
     */
    void add(int level, Quote insert);

    /**
     * Inserts the specified quote at the end. Shifts the quotes right (adds one to their indices).
     *
     * @param insert quote to be inserted
     */
    void addWorstQuote(Quote insert);

    int getMaxDepth();

    int binarySearch(Quote find);

    int binarySearchNextLevelByPrice(Quote find);

    /**
     * Remove worst quote.
     *
     * @return removed quote
     */
    Quote removeWorstQuote();

    /**
     * Remove quote by level.
     * Shifts any subsequent elements to the left.
     *
     * @param level - the level of the quote to be removed
     * @return removed quote
     */
    Quote remove(int level);

    /**
     * Returns <tt>true</tt> if this market side if full.
     *
     * @return <tt>true</tt> if this market side if full.
     */
    boolean isFull();

    /**
     * Verifies ability insert or update quote by market side.
     *
     * @param level - quote level to use
     * @return <tt>true</tt> if this quote level is unexpected
     */
    boolean isGap(int level);


    boolean isUnreachableLeve(int level);

    /**
     * Checks if the specified price is sorted.
     *
     * @param level - quote level to use
     * @param price - price to be checked
     * @return <tt>true</tt> if this price is sorted.
     */
    boolean checkOrderPrice(int level, @Decimal long price);

    void clear();

    /**
     * Validates state of market side.
     *
     * @return - <tt>true</tt> if state is valid
     */
    boolean validateState();

    boolean isInvalidInsert(int level,
                            @Decimal long price,
                            @Decimal long size,
                            @Alphanumeric long exchangeId);

    boolean isInvalidUpdate(BookUpdateAction action,
                            int level,
                            @Decimal long price,
                            @Decimal long size,
                            @Alphanumeric long exchangeId);
}
