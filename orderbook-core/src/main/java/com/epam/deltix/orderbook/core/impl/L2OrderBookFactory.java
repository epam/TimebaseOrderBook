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


import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.options.Defaults;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;

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
     * Prevents instantiation
     */
    protected L2OrderBookFactory() {
    }

    /**
     * Creates OrderBook for single exchange market feed of given initial depth
     *
     * @param options -  options to use
     * @param <Quote> - type of quote
     * @return instance of OrderBook for single exchange
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newSingleExchangeBook(final OrderBookOptions options) {
        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int depth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        final boolean isCompact = options.isCompactVersion().orElse(false);

        final QuoteProcessor<? extends MutableOrderBookQuote> processor;
        if (isCompact) {
            processor = new CompactL2SingleExchangeQuoteProcessor<>(options);
        } else {
            final ObjectPool<? extends MutableOrderBookQuote> pool =
                    (ObjectPool<? extends MutableOrderBookQuote>) options.getSharedObjectPool().orElse(QuotePoolFactory.create(options, depth));
            processor = new L2SingleExchangeQuoteProcessor<>(options, pool);
        }

        return (OrderBook<Quote>) new OrderBookDecorator<>(options.getSymbol(), processor);
    }

    /**
     * Creates OrderBook for market feed from multiple exchanges of given maximum depth.
     * Consolidated book preserve information about quote's exchange.
     *
     * @param options -  options to use
     * @param <Quote> - type of quote
     * @return instance of Order Book with multiple exchanges
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newConsolidatedBook(final OrderBookOptions options) {
        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int depth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        final int exchanges = options.getInitialExchangesPoolSize().orElse(Defaults.INITIAL_EXCHANGES_POOL_SIZE);

        final ObjectPool<? extends MutableOrderBookQuote> pool = (ObjectPool<? extends MutableOrderBookQuote>) options.getSharedObjectPool()
                        .orElse(QuotePoolFactory.create(options, exchanges * depth));

        final QuoteProcessor<? extends MutableOrderBookQuote> processor = new L2ConsolidatedQuoteProcessor<>(options, pool);
        return (OrderBook<Quote>) new OrderBookDecorator<>(options.getSymbol(), processor);
    }

    /**
     * Creates OrderBook for market feed from multiple exchanges of given maximum depth.
     * Aggregated order book groups quotes from multiple exchanges by price.
     *
     * @param options -  options to use
     * @param <Quote> - type of quote
     * @return instance of Order Book with multiple exchanges
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newAggregatedBook(final OrderBookOptions options) {
        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int depth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        final int exchanges = options.getInitialExchangesPoolSize().orElse(Defaults.INITIAL_EXCHANGES_POOL_SIZE);

        final ObjectPool<? extends MutableOrderBookQuote> pool = (ObjectPool<? extends MutableOrderBookQuote>) options.getSharedObjectPool()
                        .orElse(QuotePoolFactory.create(options, exchanges * depth * 4));

        final QuoteProcessor<? extends MutableOrderBookQuote> processor = new L2AggregatedQuoteProcessor<>(options, pool);
        return (OrderBook<Quote>) new OrderBookDecorator<>(options.getSymbol(), processor);
    }
}
