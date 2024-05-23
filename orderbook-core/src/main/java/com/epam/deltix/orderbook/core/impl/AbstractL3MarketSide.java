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
import com.epam.deltix.orderbook.core.api.EntryValidationCode;
import com.epam.deltix.orderbook.core.impl.collections.rbt.RBTree;
import com.epam.deltix.timebase.messages.universal.InsertType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.collections.CharSeqToObjMap;

import java.util.*;

import static com.epam.deltix.dfp.Decimal64Utils.*;
import static com.epam.deltix.orderbook.core.api.EntryValidationCode.*;

/**
 * @author Andrii_Ostapenko1
 */
abstract class AbstractL3MarketSide<Quote extends MutableOrderBookQuote> implements L3MarketSide<Quote> {

    protected final RBTree<Quote, Quote> data;
    private final CharSeqToObjMap<CharSequence, Quote> quoteHashMap;
    private final ReusableIterator<Quote> itr;
    // This parameter is used to limit maximum elements and to understand whether the side is full or not.
    private final int maxDepth;
    private long virtualClock;

    AbstractL3MarketSide(final int initialCapacity, final int maxDepth) {
        this.maxDepth = maxDepth;
        this.data = new RBTree<>(initialCapacity, new QuoteComparator());
        this.itr = new ReusableIterator<>();
        this.quoteHashMap = new CharSeqToObjMap<>();
        virtualClock = 0;
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public int depth() {
        return data.size();
    }

    @Override
    public long getTotalQuantity() {
        @Decimal long result = ZERO;
        for (final Quote quote : this) {
            result = Decimal64Utils.add(result, quote.getSize());
        }
        return result;
    }

    /**
     * Clears the market side in linear time
     */
    @Override
    public void clear() {
        data.clear();
        quoteHashMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Quote getQuote(final int level) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Quote getQuote(final CharSequence quoteId) {
        return quoteHashMap.get(quoteId, null);
    }

    @Override
    public boolean add(final Quote insert) {
        if (quoteHashMap.put(insert.getQuoteId(), insert)) {
            insert.setSequenceNumber(virtualClock++);
            data.put(insert, insert);
            return true;
        }
        return false;
    }

    @Override
    public Quote remove(final CharSequence quoteId) {
        final Quote result = quoteHashMap.remove(quoteId, null);
        if (result != null) {
            data.remove(result);
        }
        return result;
    }

    @Override
    public Quote remove(final Quote quote) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFull() {
        return depth() == maxDepth;
    }

    @Override
    public Quote getBestQuote() {
        if (isEmpty()) {
            return null;
        }
        return data.firstKey();
    }

    @Override
    public boolean hasLevel(final int level) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasQuote(final CharSequence quoteId) {
        return quoteHashMap.containsKey(quoteId);
    }

    @Override
    public Quote getWorstQuote() {
        if (isEmpty()) {
            return null;
        }
        return data.lastKey();
    }

    /**
     * @return error code, or null if everything is valid
     */
    @Override
    public EntryValidationCode isInvalidInsert(final InsertType type,
                                               final CharSequence quoteId,
                                               final @Decimal long price,
                                               final @Decimal long size,
                                               final QuoteSide side) {
        if (type != InsertType.ADD_BACK) {
            return UNSUPPORTED_INSERT_TYPE;
        }

        if (side == null) {
            return UNSPECIFIED_SIDE;
        }

        if (quoteId == null || quoteId.length() == 0) {
            return MISSING_QUOTE_ID;
        }

        if (isNaN(price)) {
            return MISSING_PRICE;
        }

        if (isLessOrEqual(size, ZERO)) {
            return BAD_SIZE;
        }

        return null; // all good
    }

    /**
     * @return error code, or null if everything is valid
     */
    @Override
    public EntryValidationCode isInvalidUpdate(final Quote quote,
                                               final CharSequence quoteId,
                                               final @Decimal long price,
                                               final @Decimal long size,
                                               final QuoteSide side) {
        if (side == null) {
            return UNSPECIFIED_SIDE;
        }

        if (quoteId == null || quoteId.length() == 0) {
            return MISSING_QUOTE_ID;
        }

        if (quote == null) {
            return UNKNOWN_QUOTE_ID;
        }

        if (isNotEqual(quote.getPrice(), price)) {
            return MODIFY_CHANGE_PRICE;
        }

        if (isLessOrEqual(size, ZERO)) {
            return BAD_SIZE;
        }

        if (Decimal64Utils.isLess(quote.getSize(), size)) {
            return MODIFY_INCREASE_SIZE;
        }

        return null; // all good
    }

    @Override
    public void buildFromSorted(final ArrayList<Quote> quotes) {
        data.buildFromSorted(quotes);
        final int len = quotes.size();
        for (int i = 0; i < len; i++) {
            final Quote quote = quotes.get(i);
            quoteHashMap.put(quote.getQuoteId(), quote);
        }
        virtualClock = data.size();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Quote quote : this) {
            builder.append(quote).append("\n");
        }
        return builder.toString();
    }

    @Override
    public Iterator<Quote> iterator(final int fromLevel, final int toLevel) {
        if (fromLevel != 0) {
            throw new UnsupportedOperationException();
        }
        itr.iterateBy(data);
        return itr;
    }

    /**
     * An adapter to safely externalize the value iterator.
     */
    static final class ReusableIterator<Quote> implements Iterator<Quote> {

        private Iterator<Map.Entry<Quote, Quote>> iterator;

        private void iterateBy(final RBTree<Quote, Quote> tm) {
            Objects.requireNonNull(tm);
            iterator = tm.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Quote next() {
            return iterator.next().getValue();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    static class ASKS<Quote extends MutableOrderBookQuote> extends AbstractL3MarketSide<Quote> {

        ASKS(final int initialDepth, final int maxDepth) {
            super(initialDepth, maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.ASK;
        }

    }

    static class BIDS<Quote extends MutableOrderBookQuote> extends AbstractL3MarketSide<Quote> {

        BIDS(final int initialDepth, final int maxDepth) {
            super(initialDepth, maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.BID;
        }

    }

    class QuoteComparator implements Comparator<Quote> {

        @Override
        public int compare(final Quote o1, final Quote o2) {
            final int priceComp = Decimal64Utils.compareTo(o1.getPrice(), o2.getPrice());
            if (priceComp == 0) {
                return Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
            }
            if (getSide() == QuoteSide.ASK) {
                return priceComp;
            } else {
                return -priceComp;
            }
        }
    }

}
