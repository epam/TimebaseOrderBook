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
package com.epam.deltix.orderbook.core;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookFactory;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.fwk.AbstractL1QuoteLevelTest;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Andrii_Ostapenko1
 */
public class L1SingleExchangeOrderBookTest extends AbstractL1QuoteLevelTest {

    public BindOrderBookOptionsBuilder opt = new OrderBookOptionsBuilder()
            .symbol(DEFAULT_SYMBOL)
            .orderBookType(OrderBookType.SINGLE_EXCHANGE)
            .quoteLevels(DataModelType.LEVEL_ONE)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT);

    public OrderBook<OrderBookQuote> book = OrderBookFactory.create(opt.build());

    static Stream<Arguments> quoteProvider() {
        final int maxDepth = 1;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final List<Arguments> asks = new ArrayList<>(maxDepth);
        asks.add(arguments(bbo,
                QuoteSide.ASK,
                Decimal64Utils.fromDouble(bbo - 0.5),
                Decimal64Utils.fromDouble(size),
                numberOfOrders));
        final List<Arguments> bids = new ArrayList<>(maxDepth);
        bids.add(arguments(bbo,
                QuoteSide.BID,
                Decimal64Utils.fromDouble(bbo + 0.5),
                Decimal64Utils.fromDouble(size),
                numberOfOrders));
        return Stream.concat(asks.stream(), bids.stream());
    }

    @Override
    public OrderBook<OrderBookQuote> getBook() {
        return book;
    }

    @Override
    public void createBook(final OrderBookOptions otherOpt) {
        opt.parent(otherOpt);
        book = OrderBookFactory.create(opt.build());
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should add new quote in order book")
    public void incrementalUpdate_Insert_L1Quote(final int bbo,
                                                 final QuoteSide side,
                                                 @Decimal final long price,
                                                 @Decimal final long size,
                                                 final long numOfOrders) {
        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, bbo, size, numOfOrders);
        simulateL1Insert(side, price, size, numOfOrders);

        assertBookSize(side, 1);
        assertEqualLevel(side, BEST_LEVEL, price, size, numOfOrders);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L1Quote(final PackageType packageType) {
        final int maxDepth = 1;
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        simulateL1QuoteSnapshot(packageType, COINBASE, bbo, size, numOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void resetEntry_L1Quote(final PackageType packageType) {
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        simulateL1QuoteSnapshot(packageType, COINBASE, bbo, size, numOfOrders);
        simulateResetEntry(packageType, COINBASE);

        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);
        Assertions.assertTrue(book.isEmpty());
    }

}
