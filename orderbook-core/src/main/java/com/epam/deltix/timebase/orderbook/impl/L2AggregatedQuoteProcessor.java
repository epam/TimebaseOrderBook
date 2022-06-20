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
package com.epam.deltix.timebase.orderbook.impl;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.L2EntryUpdateInfo;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.timebase.orderbook.options.GapMode;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;

import static com.epam.deltix.dfp.Decimal64Utils.*;


/**
 * Implementation aggregated order book for L2 quote level.
 *
 * @author Andrii_Ostapenko1
 */
class L2AggregatedQuoteProcessor<Quote extends MutableOrderBookQuote> extends AbstractL2MultiExchangeProcessor<Quote> {

    public L2AggregatedQuoteProcessor(final int initialExchangeCount,
                                      final int initialDepth,
                                      final int maxDepth,
                                      final ObjectPool<Quote> pool,
                                      final GapMode gapMode,
                                      final UpdateMode updateMode) {
        super(initialExchangeCount, initialDepth, maxDepth, pool, gapMode, updateMode);
    }

    @Override
    public String getDescription() {
        return "L2/Aggregation of multiple exchanges";
    }

    @Override
    public void updateQuote(final Quote previous,
                            final QuoteSide side,
                            final L2EntryUpdateInfo update) {
        final L2MarketSide<Quote> marketSide = getMarketSide(side);
        final short level = marketSide.binarySearchLevelByPrice(previous);
        if (level != L2MarketSide.NOT_FOUND) {
            final Quote quote = marketSide.getQuote(level);
            @Decimal final long size = add(subtract(quote.getSize(), previous.getSize()), update.getSize());
            quote.setSize(size);

            final long numberOfOrders = (quote.getNumberOfOrders() - previous.getNumberOfOrders()) + update.getNumberOfOrders();
            quote.setNumberOfOrders(numberOfOrders);
        }
    }

    @Override
    public boolean removeQuote(final Quote remove,
                               final L2MarketSide<Quote> marketSide) {
        final short level = marketSide.binarySearchLevelByPrice(remove);
        if (level != L2MarketSide.NOT_FOUND) {
            final Quote quote = marketSide.getQuote(level);

            final long numberOfOrders = quote.getNumberOfOrders() - remove.getNumberOfOrders();
            quote.setNumberOfOrders(numberOfOrders);
            @Decimal final long size = subtract(quote.getSize(), remove.getSize());

            if (isLessOrEqual(size, ZERO)) {
                marketSide.remove(level);
                pool.release(quote);
            } else {
                quote.setSize(size);
            }
            return true;
        }
        return false;
    }

    @Override
    public Quote insertQuote(final Quote insert, final L2MarketSide<Quote> marketSide) {
        final short level = marketSide.binarySearchNextLevelByPrice(insert);
        Quote quote;
        if (level != marketSide.depth()) {
            quote = marketSide.getQuote(level);
            if (quote.compareTo(insert) == 0) {
                @Decimal final long size = add(insert.getSize(), quote.getSize());
                quote.setSize(size);

                final long numberOfOrders = (insert.getNumberOfOrders() + quote.getNumberOfOrders());
                quote.setNumberOfOrders(numberOfOrders);
                return quote;
            }
        }

        quote = pool.borrow();
        quote.copyFrom(insert);
        quote.setExchangeId(TypeConstants.INT64_NULL);
        marketSide.add(level, quote);
        return quote;
    }

    @Override
    public void clear() {
        for (int i = 0; i < asks.depth(); i++) {
            final Quote release = asks.getQuote(i);
            pool.release(release);
        }
        asks.clear();
        for (int i = 0; i < bids.depth(); i++) {
            final Quote release = bids.getQuote(i);
            pool.release(release);
        }
        bids.clear();
        for (MutableExchange<Quote, L2Processor<Quote>> exchange : exchanges) {
            exchange.getProcessor().clear();
        }
    }

    @Override
    public L2Processor<Quote> clearExchange(final L2Processor<Quote> exchange) {
        if (exchange.isEmpty()) {
            return exchange;
        }
        removeAll(exchange, QuoteSide.ASK);
        removeAll(exchange, QuoteSide.BID);
        exchange.clear();
        return exchange;
    }
}
