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
interface CompactL2MarketSide<Quote> extends MarketSide<Quote> {

    static <Quote extends MutableOrderBookQuote> CompactL2MarketSide<Quote> factory(final int maxDepth,
                                                                                    final QuoteSide side) {
        Objects.requireNonNull(side);
        switch (side) {
            case BID:
                return new CompactAbstractL2MarketSide.BID<>(maxDepth);
            case ASK:
                return new CompactAbstractL2MarketSide.ASK<>(maxDepth);
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
     * Inserts the specified quote at the specified level and shifts the quotes right.
     *
     * @param level - the level at which quote needs to be inserted.
     * @param price - the price of the quote.
     * @param size - the size of the quote.
     */
    void add(int level, long price, long size);

    /**
     * Returns the maximum depth of this market side.
     *
     * @return the maximum depth.
     */
    int getMaxDepth();

    /**
     * Removes the worst quote from this market side.
     */
    void removeWorstQuote();

    /**
     * Remove quote by level.
     * Shifts any subsequent elements to the left.
     *
     * @param level - the level of the quote to be removed
     */
    void remove(int level);

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
     * Validates the state of this market side.
     *
     * @return true if the market side state is valid, false otherwise.
     */
    boolean validateState();

    /**
     * Sets a quote at given level with provided price and size.
     *
     * @param level - level at which the quote should be set.
     * @param price - price of the quote.
     * @param size - size of the quote.
     */
    void set(int level, long price, long size);

    /**
     * Checks if inserting a quote at given level with provided price, size and exchangeId would be invalid.
     *
     * @param level - level to check
     * @param price - price to check
     * @param size - size to check
     * @param exchangeId - exchangeId to check
     * @return true if the insert operation would be invalid, false otherwise.
     */
    boolean isInvalidInsert(int level,
                            @Decimal long price,
                            @Decimal long size,
                            @Alphanumeric long exchangeId);

    /**
     * Checks if updating a quote with given action, level, price, size and exchangeId would be invalid.
     *
     * @param action - action to check
     * @param level - level to check
     * @param price - price to check
     * @param size - size to check
     * @param exchangeId - exchangeId to check
     * @return true if the update operation would be invalid, false otherwise.
     */
    boolean isInvalidUpdate(BookUpdateAction action,
                            int level,
                            @Decimal long price,
                            @Decimal long size,
                            @Alphanumeric long exchangeId);
}
