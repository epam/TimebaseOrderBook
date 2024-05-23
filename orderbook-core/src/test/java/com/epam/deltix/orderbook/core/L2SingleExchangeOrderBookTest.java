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
import com.epam.deltix.orderbook.core.fwk.AbstractL2QuoteLevelTest;
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
public class L2SingleExchangeOrderBookTest extends AbstractL2QuoteLevelTest {

    public BindOrderBookOptionsBuilder opt = new OrderBookOptionsBuilder()
            .symbol(DEFAULT_SYMBOL)
            .orderBookType(OrderBookType.SINGLE_EXCHANGE)
            .quoteLevels(DataModelType.LEVEL_TWO)
            .initialDepth(10)
            .initialExchangesPoolSize(1)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT);

    private OrderBook<OrderBookQuote> book = OrderBookFactory.create(opt.build());

    static Stream<Arguments> quoteProvider() {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final List<Arguments> asks = new ArrayList<>(maxDepth);
        for (int level = 0; level < maxDepth; level++) {
            asks.add(arguments(maxDepth,
                    bbo,
                    QuoteSide.ASK,
                    (short) level,
                    bbo + level,
                    size + level,
                    numberOfOrders,
                    true));
        }
        final List<Arguments> bids = new ArrayList<>(maxDepth);
        for (int level = 0; level < maxDepth; level++) {
            bids.add(arguments(maxDepth,
                    bbo,
                    QuoteSide.BID,
                    (short) level,
                    bbo - level,
                    size + level,
                    numberOfOrders,
                    false));
        }
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
    public void incrementalUpdate_Insert_L2Quote(final int maxExchangeDepth,
                                                 final int bbo,
                                                 final QuoteSide side,
                                                 final short priceLevel,
                                                 final long price,
                                                 final long size,
                                                 final long numOfOrders,
                                                 final boolean addStatistics) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders, addStatistics);
        simulateL2Insert(COINBASE, side, priceLevel, price, size, numOfOrders);

        final long expectedDepth = maxExchangeDepth + 1;
        assertBookSize(side, (int) expectedDepth);
        assertEqualLevel(side, priceLevel, price, size, numOfOrders);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should delete quote in order book")
    public void incrementalUpdate_Delete_L2Quote(final int maxExchangeDepth,
                                                 final int bbo,
                                                 final QuoteSide side,
                                                 final short priceLevel,
                                                 final long price,
                                                 final long size,
                                                 final long numOfOrders,
                                                 final boolean addStatistics) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders, addStatistics);
        simulateL2Delete(side, priceLevel, price, size, numOfOrders);

        assertBookSize(side, maxExchangeDepth - 1);
        assertNotEqualPrice(side, priceLevel, price);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should update quote in order book")
    public void incrementalUpdate_Update_L2Quote(final int maxExchangeDepth,
                                                 final int bbo,
                                                 final QuoteSide side,
                                                 final short priceLevel,
                                                 final long price,
                                                 final long size,
                                                 final long numberOfOrders,
                                                 final boolean addStatistics) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);

        @Decimal final long updateSize = Decimal64Utils.add(size, Decimal64Utils.TWO);
        final long updateNumberOfOrders = numberOfOrders + 1;

        simulateL2Update(side, priceLevel, price, updateSize, updateNumberOfOrders);

        assertEqualLevel(side, priceLevel, price, updateSize, updateNumberOfOrders);
        assertBookSize(side, maxExchangeDepth);
        assertSize(side, priceLevel, updateSize);
        assertNumberOfOrders(side, priceLevel, updateNumberOfOrders);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L2Quote(final PackageType packageType) {
        int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, true);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        maxDepth = maxDepth - 4;
        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, false);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        maxDepth = maxDepth + 4;
        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, true);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        for (int i = 1; i < 10; i++) {
            maxDepth = i;
            simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, false);
            assertBookSize(QuoteSide.BID, maxDepth);
            assertBookSize(QuoteSide.ASK, maxDepth);
        }

        for (int i = 9; i >= 1; i--) {
            maxDepth = i;
            simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, true);
            assertBookSize(QuoteSide.BID, maxDepth);
            assertBookSize(QuoteSide.ASK, maxDepth);
        }

    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void resetEntry_L2Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders, true);
        simulateResetEntry(COINBASE, packageType);

        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);
        Assertions.assertTrue(book.isEmpty());
    }

}
