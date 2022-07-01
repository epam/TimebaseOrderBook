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

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.timebase.messages.universal.QuoteSide;

import java.util.Iterator;
import java.util.Objects;

/**
 * @author Andrii_Ostapenko1
 */
abstract class AbstractL1MarketSide<Quote extends OrderBookQuote> implements L1MarketSide<Quote> {

    private final ReusableIterator<Quote> itr;
    private Quote quote;

    AbstractL1MarketSide() {
        this.itr = new ReusableIterator<>();
    }

    @Override
    public void insert(final Quote insert) {
        this.quote = insert;
    }

    @Override
    public void clear() {
        this.quote = null;
    }

    @Override
    public Quote getQuote(final int level) {
        return !isEmpty() && level == 0 ? getBestQuote() : null;
    }

    @Override
    public long getTotalQuantity() {
        if (Objects.nonNull(quote)) {
            return quote.getSize();
        }
        return Decimal64Utils.ZERO;
    }

    @Override
    public int depth() {
        return quote == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return Objects.isNull(quote);
    }

    @Override
    public boolean hasLevel(final short level) {
        return !isEmpty() && level == 0;
    }

    @Override
    public Quote getBestQuote() {
        return quote;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Quote quote : this) {
            builder.append(quote).append(" ");
        }
        return builder.toString();
    }

    @Override
    public Iterator<Quote> iterator(final short fromLevel, final short toLevel) {
        itr.iterateBy(quote);
        return itr;
    }

    /**
     * An adapter to safely externalize the value iterator.
     */
    static final class ReusableIterator<Quote extends OrderBookQuote> implements Iterator<Quote> {

        private Quote quote;

        private void iterateBy(final Quote quote) {
            this.quote = quote;
        }

        @Override
        public boolean hasNext() {
            return Objects.nonNull(quote);
        }

        @Override
        public Quote next() {
            final Quote next = quote;
            quote = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    static final class ASK<Quote extends MutableOrderBookQuote> extends AbstractL1MarketSide<Quote> {

        @Override
        public QuoteSide getSide() {
            return QuoteSide.ASK;
        }
    }

    static final class BID<Quote extends MutableOrderBookQuote> extends AbstractL1MarketSide<Quote> {

        @Override
        public QuoteSide getSide() {
            return QuoteSide.BID;
        }
    }

}
