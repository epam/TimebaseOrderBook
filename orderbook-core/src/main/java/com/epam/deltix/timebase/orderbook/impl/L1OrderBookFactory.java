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
import com.epam.deltix.timebase.orderbook.options.Option;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;

/**
 * A factory that implements order book for Level1.
 *
 * <p>
 * Not thread safe!
 *
 * @author Andrii_Ostapenko1
 */
@SuppressWarnings("unchecked")
public class L1OrderBookFactory {

    /**
     * Creates OrderBook for single exchange market feed.
     *
     * @param <Quote>    - type of quote
     * @param symbol     - type of symbol
     * @param updateMode - modes of order book work. Waiting first snapshot don't apply incremental updates before it or no.
     * @return order book
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> newSingleExchangeBook(final Option<String> symbol, final UpdateMode updateMode) {
        final int initialSize = 2;
        final ObjectPool<MutableOrderBookQuote> pool = new ObjectPool<>(initialSize, MutableOrderBookQuoteImpl::new);
        final QuoteProcessor<MutableOrderBookQuote> processor = new L1SingleExchangeQuoteProcessor<>(pool, updateMode);
        return (OrderBook<Quote>) new OrderBookDecorator<>(symbol, processor);
    }

}
