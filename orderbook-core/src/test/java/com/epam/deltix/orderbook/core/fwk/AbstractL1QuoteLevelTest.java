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
package com.epam.deltix.orderbook.core.fwk;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;

import com.epam.deltix.orderbook.core.api.OrderBookQuoteTimestamp;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static com.epam.deltix.timebase.messages.universal.PackageType.PERIODICAL_SNAPSHOT;
import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;


/**
 * @author Andrii_Ostapenko1
 * @created 17/01/2022 - 10:12 PM
 */
public abstract class AbstractL1QuoteLevelTest extends AbstractOrderBookTest {

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_Insert_base_L1Quote(final int bbo,
                                                    final QuoteSide side,
                                                    @Decimal final long price,
                                                    @Decimal final long size,
                                                    final long numberOfOrders) {
        simulateL1Insert(side, price, size, numberOfOrders);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L1Quote_base_clear(final PackageType packageType) {
        final int maxDepth = 1;
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        simulateL1QuoteSnapshot(packageType, COINBASE, bbo, size, numOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        getBook().clear();
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);

        simulateL1QuoteSnapshot(packageType, COINBASE, bbo, size, numOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L1Quote_base_totalQuantity(final PackageType packageType) {
        final int maxDepth = 1;
        final int bbo = 25;
        final int size = 5;
        final int numOfOrders = 25;

        @Decimal final long expectedTotalQuantity = Decimal64Utils.fromInt(size * maxDepth);
        simulateL1QuoteSnapshot(packageType, COINBASE, bbo, size, numOfOrders);

        assertTotalQuantity(QuoteSide.BID, expectedTotalQuantity);
        assertTotalQuantity(QuoteSide.ASK, expectedTotalQuantity);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_L1Quote_base_isEmpty(final int bbo,
                                              final QuoteSide side,
                                              @Decimal final long price,
                                              @Decimal final long size,
                                              final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .updateMode(UpdateMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);
        assertBookSize(side, 0);
        simulateL1Insert(side, price, size, numberOfOrders);
        assertBookSize(side, 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void shouldStoreQuoteTimestamp_L1Quote(final int bbo,
                                                  final QuoteSide side,
                                                  @Decimal final long price,
                                                  @Decimal final long size,
                                                  final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .shouldStoreQuoteTimestamps(true)
                .build();
        createBook(opt);
        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        getBook().getMarketSide(side)
                .forEach(q -> {
                    Assertions.assertTrue(q.hasOriginalTimestamp());
                    Assertions.assertTrue(q.getOriginalTimestamp() != OrderBookQuoteTimestamp.TIMESTAMP_UNKNOWN);
                    Assertions.assertTrue(q.hasTimestamp());
                    Assertions.assertTrue(q.getTimestamp() != OrderBookQuoteTimestamp.TIMESTAMP_UNKNOWN);
                });
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void shouldNotStoreQuoteTimestamp_L1Quote(final int bbo,
                                                  final QuoteSide side,
                                                  @Decimal final long price,
                                                  @Decimal final long size,
                                                  final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .shouldStoreQuoteTimestamps(false)
                .build();
        createBook(opt);
        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        getBook().getMarketSide(side)
                .forEach(q -> {
                    Assertions.assertFalse(q.hasOriginalTimestamp());
                    Assertions.assertEquals(OrderBookQuoteTimestamp.TIMESTAMP_UNKNOWN, q.getOriginalTimestamp());
                    Assertions.assertFalse(q.hasTimestamp());
                    Assertions.assertEquals(OrderBookQuoteTimestamp.TIMESTAMP_UNKNOWN, q.getTimestamp());
                });
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_L1Quote_invalidSymbol(final int bbo,
                                                               final QuoteSide side,
                                                               @Decimal final long price,
                                                               @Decimal final long size,
                                                               final long numberOfOrders) {
        Assertions.assertFalse(simulateL1Insert(LTC_SYMBOL, BINANCE, side, price, size, numberOfOrders));
        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        Assertions.assertFalse(simulateL1Insert(LTC_SYMBOL, BINANCE, side, price, size, numberOfOrders));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void securityStatusMessage_L2Quote(final int bbo,
                                              final QuoteSide side,
                                              @Decimal final long price,
                                              @Decimal final long size,
                                              final long numberOfOrders) {
        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);

        simulateSecurityFeedStatus(COINBASE, FeedStatus.NOT_AVAILABLE);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL1Insert(DEFAULT_SYMBOL, COINBASE, side, price, size, numberOfOrders);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure insert is ignored (we are waiting for snapshot)

        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        assertBookSize(side, 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L1Quote_NoWaitingSnapshot(final int bbo,
                                                     final QuoteSide side,
                                                     @Decimal final long price,
                                                     @Decimal final long size,
                                                     final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL1Insert(DEFAULT_SYMBOL, COINBASE, side, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, 1); // make sure insert is not ignored

        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        assertBookSize(side, 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L1Quote_WaitingSnapshot(final int bbo,
                                                   final QuoteSide side,
                                                   @Decimal final long price,
                                                   @Decimal final long size,
                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL1Insert(DEFAULT_SYMBOL, COINBASE, side, price, size, numberOfOrders);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure insert is ignored

        simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        assertBookSize(side, 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L1Quote_PeriodicalSnapshotMode_ONLY_ONE(final int bbo,
                                                                   final QuoteSide side,
                                                                   @Decimal final long price,
                                                                   @Decimal final long size,
                                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.ONLY_ONE)
                .build();
        createBook(opt);

        Assertions.assertTrue(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        assertBookSize(side, 1);
        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        Assertions.assertTrue(simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        simulateL1Insert(DEFAULT_SYMBOL, COINBASE, side, price, size, numberOfOrders);
        Assertions.assertTrue(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L1Quote_PeriodicalSnapshotMode_PROCESS_ALL(final int bbo,
                                                                      final QuoteSide side,
                                                                      @Decimal final long price,
                                                                      @Decimal final long size,
                                                                      final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.PROCESS_ALL)
                .build();
        createBook(opt);

        Assertions.assertTrue(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        Assertions.assertTrue(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        Assertions.assertTrue(simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        Assertions.assertTrue(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L1Quote_PeriodicalSnapshotMode_SKIP_ALL(final int bbo,
                                                                   final QuoteSide side,
                                                                   @Decimal final long price,
                                                                   @Decimal final long size,
                                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.SKIP_ALL)
                .build();
        createBook(opt);

        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        Assertions.assertTrue(simulateL1QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        simulateL1Insert(DEFAULT_SYMBOL, COINBASE, side, price, size, numberOfOrders);
        Assertions.assertFalse(simulateL1QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, bbo, size, numberOfOrders));
    }

}
