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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Andrii_Ostapenko1
 */
public class L2AggregatedOrderBookTest extends AbstractL2QuoteLevelTest {

    public BindOrderBookOptionsBuilder opt = new OrderBookOptionsBuilder()
            .symbol(DEFAULT_SYMBOL)
            .orderBookType(OrderBookType.AGGREGATED)
            .quoteLevels(DataModelType.LEVEL_TWO)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
            .initialDepth(500)
            .initialExchangesPoolSize(1);

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
                    size,
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
                    size,
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
    public void incrementUpdate_Insert_L2Quote(final int maxExchangeDepth,
                                               final int bbo,
                                               final QuoteSide side,
                                               final short priceLevel,
                                               @Decimal final long price,
                                               @Decimal final long size,
                                               final long numberOfOrders,
                                               final boolean addStatistics) {
        int expectedDepth = maxExchangeDepth;

        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);
        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, BINANCE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);

        double expectedPrice = price;
        if (side == QuoteSide.ASK) {
            expectedPrice = expectedPrice - 0.1;
        } else {
            expectedPrice = expectedPrice + 0.1;
        }

        expectedDepth++;

        simulateL2Insert(COINBASE, side, priceLevel, expectedPrice, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, expectedDepth);
        assertBookSize(side, expectedDepth);

        simulateL2Insert(BINANCE, side, priceLevel, expectedPrice, size, numberOfOrders);
        assertExchangeBookSize(BINANCE, side, expectedDepth);
        assertBookSize(side, expectedDepth);

        assertPrice(side, priceLevel, expectedPrice);
        assertSize(side, priceLevel, size * 2);
        assertNumberOfOrders(side, priceLevel, numberOfOrders * 2);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Delete_L2Quote(final int maxExchangeDepth,
                                                 final int bbo,
                                                 final QuoteSide side,
                                                 final short priceLevel,
                                                 @Decimal final long price,
                                                 @Decimal final long size,
                                                 final long numberOfOrders,
                                                 final boolean addStatistics) {
        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);
        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, BINANCE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);

        simulateL2Delete(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth - 1);

        simulateL2Delete(BINANCE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSize(BINANCE, side, maxExchangeDepth - 1);

        assertBookSize(side, maxExchangeDepth - 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_Update_L2Quote(final int maxExchangeDepth,
                                               final int bbo,
                                               final QuoteSide side,
                                               final short priceLevel,
                                               final long price,
                                               final long size,
                                               final long numberOfOrders,
                                               final boolean addStatistics) {
        final int expectedDepth = maxExchangeDepth;
        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);
        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, BINANCE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);


        @Decimal final long updateSize = Decimal64Utils.add(size, Decimal64Utils.TWO);
        final long updateNumberOfOrders = numberOfOrders + 1;

        simulateL2Update(BINANCE, side, priceLevel, price, updateSize, updateNumberOfOrders);
        assertBookSize(side, expectedDepth);

        simulateL2Update(COINBASE, side, priceLevel, price, updateSize, updateNumberOfOrders);
        assertBookSize(side, expectedDepth);

        //TODO add strategy to handle updates with different prices in the same level
//        assertPrice(side, priceLevel, price);
        assertSize(side, priceLevel, updateSize * 2);
        assertNumberOfOrders(side, priceLevel, updateNumberOfOrders * 2);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L2Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders, false);
        simulateL2QuoteSnapshot(packageType, BINANCE, maxDepth, bbo, size, numberOfOrders, true);

        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        assertExchangeBookSize(COINBASE, QuoteSide.ASK, maxDepth);
        assertExchangeBookSize(COINBASE, QuoteSide.BID, maxDepth);

        assertExchangeBookSize(BINANCE, QuoteSide.ASK, maxDepth);
        assertExchangeBookSize(BINANCE, QuoteSide.BID, maxDepth);

        final int expectedSize = size * 2;
        final int expectedNumberOfOrders = numberOfOrders * 2;

        final int expectedQuoteCounts = maxDepth * 2;

        assertIteratorBookQuotes(expectedQuoteCounts, bbo, expectedSize, expectedNumberOfOrders);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void resetEntry_L2Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numOfOrders, true);
        simulateL2QuoteSnapshot(packageType, BINANCE, maxDepth, bbo, size, numOfOrders, false);

        simulateResetEntry(COINBASE, packageType);
        assertIteratorBookQuotes(maxDepth * 2, bbo, size, numOfOrders);

        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        assertExchangeBookSize(COINBASE, QuoteSide.ASK, 0);
        assertExchangeBookSize(COINBASE, QuoteSide.BID, 0);

        simulateResetEntry(BINANCE, packageType);

        assertExchangeBookSize(BINANCE, QuoteSide.ASK, 0);
        assertExchangeBookSize(BINANCE, QuoteSide.BID, 0);

        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);

        Assertions.assertTrue(book.isEmpty());
    }


    @Test
    public void isWaitingForSnapshotTest() {
        Assertions.assertTrue(book.isWaitingForSnapshot()); // initially we definitely wait for snapshot

        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, 3, 25, 5, 1, true);
        Assertions.assertFalse(book.isWaitingForSnapshot());

        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, BINANCE, 3, 25, 5, 1, false);
        Assertions.assertFalse(book.isWaitingForSnapshot());

        simulateResetEntry(BINANCE, PackageType.VENDOR_SNAPSHOT);
        Assertions.assertFalse(book.isWaitingForSnapshot());
    }
}
