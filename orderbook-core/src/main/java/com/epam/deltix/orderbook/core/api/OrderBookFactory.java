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
import com.epam.deltix.orderbook.core.impl.L3OrderBookFactory;
import com.epam.deltix.orderbook.core.options.Defaults;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.orderbook.core.options.OrderBookOptionsBuilder;
import com.epam.deltix.orderbook.core.options.OrderBookType;
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
     * @param <Quote> type of quote
     * @param options to use.
     * @return a new OrderBook instance of given type.
     * @throws IllegalArgumentException - if some options does not supported.
     * @see OrderBook
     * @see OrderBookQuote
     */
    public static <Quote extends OrderBookQuote> OrderBook<Quote> create(final OrderBookOptions options) {
        if (Objects.isNull(options)) {
            throw new IllegalArgumentException("Options not allowed to be null.");
        }

        final DataModelType quoteLevels = options.getQuoteLevels().orElse(Defaults.QUOTE_LEVELS);
        final OrderBookType orderBookType = options.getBookType().orElse(Defaults.ORDER_BOOK_TYPE);
        final OrderBook<Quote> book;
        switch (quoteLevels) {
            case LEVEL_ONE:
                if (orderBookType == OrderBookType.SINGLE_EXCHANGE) {
                    book = L1OrderBookFactory.newSingleExchangeBook(options);
                } else {
                    throw new IllegalArgumentException("Unsupported book type: " + orderBookType + " for quote levels: " + quoteLevels);
                }
                break;
            case LEVEL_TWO:
                switch (orderBookType) {
                    case SINGLE_EXCHANGE:
                        book = L2OrderBookFactory.newSingleExchangeBook(options);
                        break;
                    case AGGREGATED:
                        book = L2OrderBookFactory.newAggregatedBook(options);
                        break;
                    case CONSOLIDATED:
                        book = L2OrderBookFactory.newConsolidatedBook(options);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported book type: " + orderBookType + " for quote levels: " + quoteLevels);
                }
                break;
            case LEVEL_THREE:
                switch (orderBookType) {
                    case SINGLE_EXCHANGE:
                        book = L3OrderBookFactory.newSingleExchangeBook(options);
                        break;
                    case CONSOLIDATED:
                        book = L3OrderBookFactory.newConsolidatedBook(options);
                        break;
                    case AGGREGATED:
                    default:
                        throw new IllegalArgumentException("Unsupported book type: " + orderBookType + " for quote levels: " + quoteLevels);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported quote levels: " + quoteLevels);
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
