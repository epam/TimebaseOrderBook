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

import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.timebase.orderbook.options.GapMode;
import com.epam.deltix.timebase.orderbook.options.Option;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;

/**
 * A factory that implements order book for Level2.
 * Level2 is Market By Level (order book that aggregates same-priced bids and offers)
 * <p>
 * Not thread safe!
 *
 * @author Andrii_Ostapenko1
 */
@SuppressWarnings("unchecked")
public class L2OrderBookFactory {

    /**
     * Creates OrderBook for single exchange market feed of given initial depth
     *
     * @param symbol       - type of symbol
     * @param initialDepth - initial book depth
     * @param maxDepth     - max order book depth
     * @param gapMode      - skipped levels mode
     * @param updateMode   - modes of order book update.
     * @param <Quote>      - type of quote
     * @return instance of OrderBook for single exchange
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newSingleExchangeBook(final Option<String> symbol,
                                                                                        final int initialDepth,
                                                                                        final int maxDepth,
                                                                                        final GapMode gapMode,
                                                                                        final UpdateMode updateMode) {
        final ObjectPool<MutableOrderBookQuote> pool = new ObjectPool<>(initialDepth, MutableOrderBookQuoteImpl::new);
        final QuoteProcessor<MutableOrderBookQuote> processor =
                new L2SingleExchangeQuoteProcessor<>(initialDepth, maxDepth, pool, gapMode, updateMode);
        return (OrderBook<Quote>) new OrderBookDecorator<>(symbol, processor);
    }

    /**
     * Creates OrderBook for market feed from multiple exchanges of given maximum depth.
     * Consolidated book preserve information about quote's exchange.
     *
     * @param symbol               - type of symbol\
     * @param initialExchangeCount - initial pool size for stock exchanges
     * @param initialDepth         - initial book depth
     * @param maxDepth             - max order book depth
     * @param gapMode              - skipped levels mode
     * @param updateMode           - modes of order book update.
     * @param <Quote>              - type of quote
     * @return instance of Order Book with multiple exchanges
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newConsolidatedBook(final Option<String> symbol,
                                                                                      final int initialExchangeCount,
                                                                                      final int initialDepth,
                                                                                      final int maxDepth,
                                                                                      final GapMode gapMode,
                                                                                      final UpdateMode updateMode) {
        final ObjectPool<MutableOrderBookQuote> pool = new ObjectPool<>(initialExchangeCount * initialDepth, MutableOrderBookQuoteImpl::new);
        final QuoteProcessor<MutableOrderBookQuote> processor =
                new L2ConsolidatedQuoteProcessor<>(initialExchangeCount, initialDepth, maxDepth, pool, gapMode, updateMode);
        return (OrderBook<Quote>) new OrderBookDecorator<>(symbol, processor);
    }

    /**
     * Creates OrderBook for market feed from multiple exchanges of given maximum depth.
     * Aggregated order book groups quotes from multiple exchanges by price.
     *
     * @param symbol               - type of symbol\
     * @param initialExchangeCount - initial pool size for stock exchanges
     * @param initialDepth         - initial book depth
     * @param maxDepth             - max order book depth
     * @param gapMode              - skipped levels mode
     * @param updateMode           - modes of order book update.
     * @param <Quote>              - type of quote
     * @return instance of Order Book with multiple exchanges
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newAggregatedBook(final Option<String> symbol,
                                                                                    final int initialExchangeCount,
                                                                                    final int initialDepth,
                                                                                    final int maxDepth,
                                                                                    final GapMode gapMode,
                                                                                    final UpdateMode updateMode) {
        final ObjectPool<MutableOrderBookQuote> pool = new ObjectPool<>(initialExchangeCount * initialDepth * 4, MutableOrderBookQuoteImpl::new);
        final QuoteProcessor<MutableOrderBookQuote> processor =
                new L2AggregatedQuoteProcessor<>(initialExchangeCount, initialDepth, maxDepth, pool, gapMode, updateMode);
        return (OrderBook<Quote>) new OrderBookDecorator<>(symbol, processor);
    }

}
