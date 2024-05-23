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
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.timebase.messages.universal.BookUpdateAction;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.annotations.Alphanumeric;

import java.util.Iterator;
import java.util.Objects;

import static com.epam.deltix.dfp.Decimal64Utils.*;
import static com.epam.deltix.timebase.messages.TypeConstants.EXCHANGE_NULL;

/**
 * @author Andrii_Ostapenko1
 */
abstract class CompactAbstractL2MarketSide<Quote extends MutableOrderBookQuote> implements CompactL2MarketSide<Quote> {

    protected final long[] data;
    private final Quote holder = (Quote) new MutableOrderBookQuoteImpl();
    private final ReusableIterator<Quote> itr;
    // This parameter is used to limit maximum elements and to understand whether the side is full or not.
    private final int maxDepth;
    private int depth;

    CompactAbstractL2MarketSide(final int maxDepth) {
        this.maxDepth = maxDepth;
        this.data = new long[maxDepth << 1];
        depth = 0;
        this.itr = new ReusableIterator<>();
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public long getTotalQuantity() {
        @Decimal long result = ZERO;
        final int len = depth << 1;
        for (int i = 1; i < len; i += 2) {
            result = Decimal64Utils.add(result, data[i]);
        }
        return result;
    }

    @Override
    public void clear() {
        depth = 0;
    }

    @Override
    public boolean isEmpty() {
        return depth == 0;
    }

    @Override
    public Quote getQuote(final int level) {
        if (!hasLevel(level)) {
            return null;
        }
        final int idx = level << 1;
        holder.setPrice(data[idx]);
        holder.setSize(data[idx + 1]);
        return holder;
    }

    @Override
    public void add(final int level, final long price, final long size) {
        final int idx = level << 1;
        if (level < depth) {
            System.arraycopy(data, idx, data, idx + 2, (depth << 1) - idx);
        }
        data[idx] = price;
        data[idx + 1] = size;
        ++depth;
    }

    public void set(final int level, final long price, final long size) {
        if (level == depth) {
            ++depth;
        }
        final int idx = level << 1;
        data[idx] = price;
        data[idx + 1] = size;
    }

    @Override
    public void remove(final int level) {
        if (hasLevel(level)) {
            final int idx = level << 1;
            if (level < depth) {
                System.arraycopy(data, idx + 2, data, idx, (depth << 1) - idx - 2);
            }
            --depth;
        }
    }

    @Override
    public boolean hasLevel(final int level) {
        return depth > level && level >= 0;
    }

    @Override
    public void removeWorstQuote() {
        --depth;
    }

    @Override
    public boolean isFull() {
        return depth == maxDepth;
    }

    @Override
    public boolean isGap(final int level) {
        return !hasLevel(level) && level > depth;
    }

    @Override
    public boolean isUnreachableLeve(int level) {
        return level < 0 || level >= maxDepth;
    }

    @Override
    public Quote getBestQuote() {
        return getQuote(0);
    }

    @Override
    public Quote getWorstQuote() {
        return getQuote(depth - 1);
    }

    @Override
    public boolean isInvalidInsert(final int level,
                                   final @Decimal long price,
                                   final @Decimal long size,
                                   final @Alphanumeric long exchangeId) {
        if (level < 0 || isEqual(price, NULL) || isLessOrEqual(size, ZERO) || exchangeId == EXCHANGE_NULL) {
            return true;
        }
        if (isUnreachableLeve(level)) {
            return true;
        }
        if (isGap(level)) {
            return true;
        }
        return !checkOrderPrice(level, price);
    }

    @Override
    public boolean isInvalidUpdate(final BookUpdateAction action,
                                   final int level,
                                   final @Decimal long price,
                                   final @Decimal long size,
                                   final @Alphanumeric long exchangeId) {
        if (!hasLevel(level)) {
            return true;
        }
        if (action != BookUpdateAction.DELETE) {
            return isNotEqual(data[level << 1], price) || isLess(size, ZERO);
        }
        return false;
    }

    @Override
    public boolean checkOrderPrice(final int level, final @Decimal long price) {

        @Decimal final long previousPrice = hasLevel(level - 1) ? data[(level - 1) << 1] : NULL;
        @Decimal final long nextPrice = hasLevel(level) ? data[level << 1] : NULL;

        boolean badState = false;
        if (getSide() == QuoteSide.ASK) {
            if (isNotEqual(previousPrice, NULL) && isGreater(previousPrice, price)) {
                badState = true;
            }
            if (isNotEqual(nextPrice, NULL) && isLess(nextPrice, price)) {
                badState = true;
            }
        } else {
            if (isNotEqual(previousPrice, NULL) && isLess(previousPrice, price)) {
                badState = true;
            }
            if (isNotEqual(nextPrice, NULL) && isGreater(nextPrice, price)) {
                badState = true;
            }
        }
        return !badState;
    }

    @Override
    public boolean validateState() {
        if (isEmpty()) {
            return true;
        }
        final int len = depth << 1;
        for (int i = 0; i < len; i += 2) {
            if (isInvalidInsert(i >> 1, data[i], data[i + 1], EXCHANGE_NULL ^ 1)) {
                return false;
            }
        }
        return true;
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
    public Iterator<Quote> iterator(final int fromLevel, final int toLevel) {
        itr.iterateBy(this, fromLevel, toLevel);
        return itr;
    }

    /**
     * An adapter to safely externalize the value iterator.
     */
    static final class ReusableIterator<Quote> implements Iterator<Quote> {

        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor;

        private int end;

        private MarketSide<Quote> marketSide;

        private void iterateBy(final MarketSide<Quote> marketSide, final int cursor, final int end) {
            Objects.requireNonNull(marketSide);
            this.marketSide = marketSide;
            this.cursor = cursor;
            if (end > marketSide.depth() || end < 0) {
                this.end = marketSide.depth();
            } else {
                this.end = end;
            }
            if (cursor > end || cursor < 0) {
                this.cursor = end;
            }
        }

        @Override
        public boolean hasNext() {
            return cursor != end;
        }

        @Override
        public Quote next() {
            return marketSide.getQuote(cursor++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    static class ASK<Quote extends MutableOrderBookQuote> extends CompactAbstractL2MarketSide<Quote> {

        ASK(final int maxDepth) {
            super(maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.ASK;
        }

    }

    static class BID<Quote extends MutableOrderBookQuote> extends CompactAbstractL2MarketSide<Quote> {

        BID(final int maxDepth) {
            super(maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.BID;
        }

    }
}
