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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.epam.deltix.dfp.Decimal64Utils.*;
import static com.epam.deltix.timebase.messages.TypeConstants.EXCHANGE_NULL;

/**
 * @author Andrii_Ostapenko1
 */
abstract class AbstractL2MarketSide<Quote extends MutableOrderBookQuote> implements L2MarketSide<Quote> {

    protected final List<Quote> data;
    private final ReusableIterator<Quote> itr;
    // This parameter is used to limit maximum elements and to understand whether the side is full or not.
    private final int maxDepth;

    AbstractL2MarketSide(final int initialCapacity, final int maxDepth) {
        this.maxDepth = maxDepth;
        this.data = new ArrayList<>(initialCapacity);
        this.itr = new ReusableIterator<>();
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public int depth() {
        return data.size();
    }

    //TODO Add configuration parameter for type of calculating total quantity
    @Override
    public long getTotalQuantity() {
        @Decimal long result = ZERO;
        for (int i = 0; i < data.size(); i++) {
            result = Decimal64Utils.add(result, data.get(i).getSize());
        }
        return result;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Quote getQuote(final int level) {
        if (!hasLevel(level)) {
            return null;
        }
        return data.get(level);
    }

    @Override
    public void add(final int level, final Quote insert) {
        data.add(level, insert);
    }

    @Override
    public void addWorstQuote(final Quote insert) {
        data.add(insert);
    }

    @Override
    public Quote remove(final int level) {
        if (!hasLevel(level)) {
            return null;
        }
        return data.remove(level);
    }

    @Override
    public int binarySearch(final Quote find) {
        int low = 0;
        int high = data.size() - 1;

        Quote quote;
// TODO Refactor
//        while (low <= high) {
//            int mid = (low + high) >>> 1;
//            quote = data.get(mid);
//            int cmp = (getSide() == QuoteSide.ASK
//                    ? 1
//                    : -1) * find.compareTo(quote);    // isOffer ? midVal - price : price - midVal;
//
//            if (cmp < 0) {
//                low = mid + 1;
//            } else if (cmp > 0) {
//                high = mid - 1;
//            } else {
//                return (short) mid;                                                    // price found
//            }
//        }
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            quote = data.get(mid);

            final int cmp = quote.compareTo(find);

            if (cmp < 0) {
                if (getSide() == QuoteSide.BID) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            } else if (cmp > 0) {
                if (getSide() == QuoteSide.BID) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            } else {
                return mid;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public int binarySearchNextLevelByPrice(final Quote find) {
        int low = 0;
        int high = data.size() - 1;

        Quote quote;
// TODO Refactor
//        while (low <= high) {
//            int mid = (low + high) >>> 1;
//            quote = data.get(mid);
//            int cmp = (getSide() == QuoteSide.ASK
//                    ? 1
//                    : -1) * find.compareTo(quote);    // isOffer ? midVal - price : price - midVal;
//
//            if (cmp < 0) {
//                low = mid + 1;
//            } else if (cmp > 0) {
//                high = mid - 1;
//            } else {
//                return (short) mid;                                                    // price found
//            }
//        }
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            quote = data.get(mid);

            final int cmp = quote.compareTo(find);

            if (cmp < 0) {
                if (getSide() == QuoteSide.BID) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            } else if (cmp > 0) {
                if (getSide() == QuoteSide.BID) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            } else {
                return mid;
            }
        }
        return low;
    }

    @Override
    public boolean hasLevel(final int level) {
        if (level >= 0 && data.size() > level) {
            return Objects.nonNull(data.get(level));
        }
        return false;
    }

    @Override
    public Quote removeWorstQuote() {
        return data.remove(data.size() - 1);
    }

    @Override
    public boolean isFull() {
        return depth() >= maxDepth;
    }

    //TODO add doc !!
    @Override
    public boolean isGap(final int level) {
        return !hasLevel(level) && level > depth();
    }

    @Override
    public boolean isUnreachableLeve(int level) {
        return level < 0 || level >= getMaxDepth();
    }

    @Override
    public Quote getBestQuote() {
        if (isEmpty()) {
            return null;
        }
        return data.get(0);
    }

    @Override
    public Quote getWorstQuote() {
        if (!isEmpty()) {
            return data.get(data.size() - 1);
        }
        return null;
    }

    @Override
    public boolean isInvalidInsert(final int level, final @Decimal long price, final @Decimal long size, final @Alphanumeric long exchangeId) {
        //TODO need to defined default type for internal decimal
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
            return isNotEqual(getQuote(level).getPrice(), price) || isLess(size, ZERO);
        }
        return false;
    }

    /**
     * Checking the insertion of the quotation price.
     * @param level - quote level to use
     * @param price - price to be checked
     * @return <tt>true</tt> if this price is sorted.
     */
    @Override
    public boolean checkOrderPrice(final int level, final @Decimal long price) {

        @Decimal final long previousPrice = hasLevel(level - 1) ? getQuote(level - 1).getPrice() : NULL;
        @Decimal final long nextPrice = hasLevel(level) ? getQuote(level).getPrice() : NULL;

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
        for (int i = 0; i < depth(); i++) {
            final Quote quote = getQuote(i);
            if (isInvalidInsert(i, quote.getPrice(), quote.getSize(), quote.getExchangeId())) {
                //TODO add log
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

        private int size;

        private MarketSide<Quote> marketSide;

        private void iterateBy(final MarketSide<Quote> marketSide, final int cursor, final int size) {
            Objects.requireNonNull(marketSide);
            this.marketSide = marketSide;
            this.cursor = cursor;
            if (size > marketSide.depth() || size < 0) {
                this.size = marketSide.depth();
            } else {
                this.size = size;
            }
            if (cursor > size || cursor < 0) {
                this.cursor = size;
            }
        }

        @Override
        public boolean hasNext() {
            return cursor != size;
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

    static class ASK<Quote extends MutableOrderBookQuote> extends AbstractL2MarketSide<Quote> {

        ASK(final int initialCapacity, final int maxDepth) {
            super(initialCapacity, maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.ASK;
        }

    }

    static class BID<Quote extends MutableOrderBookQuote> extends AbstractL2MarketSide<Quote> {

        BID(final int initialDepth, final int maxDepth) {
            super(initialDepth, maxDepth);
        }

        @Override
        public QuoteSide getSide() {
            return QuoteSide.BID;
        }

    }
}
