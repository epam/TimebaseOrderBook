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
package com.epam.deltix.orderbook.core.api;

import com.epam.deltix.orderbook.core.impl.L1OrderBookFactory;
import com.epam.deltix.orderbook.core.impl.L2OrderBookFactory;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.universal.DataModelType;

import java.util.Objects;

/**
 * Factory that can be used to create  {@link OrderBook}.
 *
 * <p>This is the entry point for order book API.</p>
 *
 * @author Andrii_Ostapenko1
 * @see OrderBook
 */
public class OrderBookFactory {

    /**
     * Prevents instantiation
     */
    protected OrderBookFactory() {
    }

    /**
     * Factory method for create order book with the given options.
     *
     * <p>
     * Note: FlyWeight pattern in use. We don't keep any references on your classes (opt) after method returns execution.
     *
     * @param <Quote> type of quote
     * @param opt     to use.
     * @return a new OrderBook instance of given type.
     * @throws NullPointerException          - if opt is null.
     * @throws UnsupportedOperationException - if some options does not supported.
     * @see OrderBook
     * @see OrderBookQuote
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> create(final OrderBookOptions opt) {
        Objects.requireNonNull(opt);
        final Option<String> symbol = opt.getSymbol();
        final DataModelType quoteLevels = opt.getQuoteLevels().orElse(Defaults.QUOTE_LEVELS);
        final OrderBookType orderBookType = opt.getBookType().orElse(Defaults.ORDER_BOOK_TYPE);
        OrderBook<Quote> book = null;
        final UpdateMode updateMode = opt.getUpdateMode().orElse(Defaults.UPDATE_MODE);
        switch (quoteLevels) {
            case LEVEL_ONE:
                if (orderBookType == OrderBookType.SINGLE_EXCHANGE) {
                    book = L1OrderBookFactory.newSingleExchangeBook(symbol, updateMode);
                } else {
                    throw new UnsupportedOperationException("Unsupported book mode: " + orderBookType + " for quote levels: " + quoteLevels);
                }
                break;
            case LEVEL_TWO:
                final GapMode gapMode = opt.getGapMode().orElse(Defaults.GAP_MODE);
                final int initialDepth = opt.getInitialDepth().orElse(Defaults.INITIAL_DEPTH);
                final int maxDepth = opt.getMaxDepth().orElse(Defaults.MAX_DEPTH);
                final Integer exchangePoolSize = opt.getInitialExchangesPoolSize().orElse(Defaults.INITIAL_EXCHANGES_POOL_SIZE);
                switch (orderBookType) {
                    case SINGLE_EXCHANGE:
                        book = L2OrderBookFactory.newSingleExchangeBook(symbol, initialDepth, maxDepth, gapMode, updateMode);
                        break;
                    case AGGREGATED:
                        book = L2OrderBookFactory.newAggregatedBook(symbol, exchangePoolSize, initialDepth, maxDepth, gapMode, updateMode);
                        break;
                    case CONSOLIDATED:
                        book = L2OrderBookFactory.newConsolidatedBook(symbol, exchangePoolSize, initialDepth, maxDepth, gapMode, updateMode);
                        break;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported quote levels: " + quoteLevels);
        }
        return book;
    }

    /**
     * Factory method for create the order book with default options.
     *
     * @param <Quote> type of quote
     * @return a new OrderBook instance of given type.
     * @see Defaults
     * @see OrderBook
     * @see OrderBookQuote
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> create() {
        return create(new OrderBookOptionsBuilder().build());
    }

}
