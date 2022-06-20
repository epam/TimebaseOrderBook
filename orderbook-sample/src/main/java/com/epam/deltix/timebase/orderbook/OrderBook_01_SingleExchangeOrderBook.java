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
package com.epam.deltix.timebase.orderbook;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookFactory;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptions;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import com.epam.deltix.timebase.orderbook.options.OrderBookType;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * This sample demonstrates usage of simple order book API
 *
 * @author Andrii_Ostapenko1
 */
public class OrderBook_01_SingleExchangeOrderBook extends AbstractSample {

    public static void main(String[] args) {
        final String symbol = "BTC/USD";
        final long exchangeId = AlphanumericUtils.toAlphanumericUInt64("COINBASE");
        final int marketDepth = 10;

        // Step 1: create order book
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .symbol(symbol)
                .orderBookType(OrderBookType.SINGLE_EXCHANGE)
                .quoteLevels(DataModelType.LEVEL_TWO)
                .initialDepth(marketDepth)
                .initialExchangesPoolSize(1)
                .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
                .build();

        final OrderBook<OrderBookQuote> orderBook = OrderBookFactory.create(opt);

        System.out.println("Hello! I'm " + orderBook.getDescription() + " for stock symbol: " + orderBook.getSymbol().get() + "!");

        // Step 2: feed it with updates
        orderBook.update(createL2VendorUpdate(marketDepth, exchangeId, symbol));

        // Step 4: inspect order book state by iterator
        System.out.println(System.lineSeparator());
        Iterator<OrderBookQuote> iterator = orderBook.getMarketSide(QuoteSide.ASK).iterator();
        while (iterator.hasNext()) {
            final OrderBookQuote quote = iterator.next();
            System.out.println(quote);
        }


        // Step 4.1: inspect order book state by iterator
        System.out.println(System.lineSeparator());
        iterator = orderBook
                .getExchanges()
                .getById(exchangeId).get()
                .getMarketSide(QuoteSide.ASK)
                .iterator();
        while (iterator.hasNext()) {
            final OrderBookQuote quote = iterator.next();
            System.out.println(quote);
        }

        // Step 4.2: inspect order book state by iterator
        System.out.println(System.lineSeparator());
        iterator = orderBook
                .getExchanges()
                .getById(exchangeId).get()
                .getMarketSide(QuoteSide.ASK)
                .iterator((short) 5);
        while (iterator.hasNext()) {
            final OrderBookQuote quote = iterator.next();
            System.out.println(quote);
        }

        // Step 4.3: inspect order book state by iterator
        System.out.println(System.lineSeparator());
        System.out.println(System.lineSeparator());
        iterator = orderBook
                .getExchanges()
                .getById(exchangeId).get()
                .getMarketSide(QuoteSide.ASK)
                .iterator((short) 3, (short) 5);
        while (iterator.hasNext()) {
            final OrderBookQuote quote = iterator.next();
            System.out.println(quote);
        }

        // Step 5: inspect order book state by for each
        System.out.println(System.lineSeparator());
        orderBook.getMarketSide(QuoteSide.ASK).forEach((Predicate<OrderBookQuote>) OrderBook_01_SingleExchangeOrderBook::printOrderBookLevel);

        System.out.println(System.lineSeparator());
        for (OrderBookQuote quote : orderBook.getMarketSide(QuoteSide.ASK)) {
            System.out.println(quote);
        }

        // Step 5.1: inspect order book state by for each with cookie
        System.out.println(System.lineSeparator());
        final PriceAccumulator priceAccumulator = new PriceAccumulator();
        orderBook.getMarketSide(QuoteSide.ASK).forEach(OrderBook_01_SingleExchangeOrderBook::quoteViewAccumulatorAction, priceAccumulator);

    }

    private static boolean printOrderBookLevel(final OrderBookQuote orderBookQuote) {
        System.out.println(orderBookQuote);
        return true; // continue iteration
    }

    private static boolean quoteViewAccumulatorAction(final OrderBookQuote orderBookQuote,
                                                      final PriceAccumulator accumulator) {
        System.out.println(orderBookQuote);
        accumulator.apply(orderBookQuote.getPrice());
        return true; // continue iteration
    }

    private static final class PriceAccumulator {
        @Decimal
        private long price;

        public void apply(@Decimal long add) {
            price = Decimal64Utils.add(price, add);
        }

        public long getPrice() {
            return price;
        }
    }
}
