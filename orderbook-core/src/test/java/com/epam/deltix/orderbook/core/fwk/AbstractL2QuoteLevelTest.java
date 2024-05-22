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
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.epam.deltix.timebase.messages.universal.PackageType.PERIODICAL_SNAPSHOT;
import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;




/**
 * @author Andrii_Ostapenko1
 * @created 17/01/2022 - 10:12 PM
 */
public abstract class AbstractL2QuoteLevelTest extends AbstractOrderBookTest {

    public static Stream<Arguments> packageTypeAndSideProvider() {
        return Stream.of(
                Arguments.of(VENDOR_SNAPSHOT, QuoteSide.ASK),
                Arguments.of(PERIODICAL_SNAPSHOT, QuoteSide.ASK),
                Arguments.of(VENDOR_SNAPSHOT, QuoteSide.BID),
                Arguments.of(PERIODICAL_SNAPSHOT, QuoteSide.BID)
        );
    }

    public static Stream<Arguments> sideProvider() {
        return Stream.of(
                Arguments.of(QuoteSide.ASK),
                Arguments.of(QuoteSide.BID)
        );
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void shouldStoreQuoteTimestamp_L1Quote(final int maxExchangeDepth,
                                                  final int bbo,
                                                  final QuoteSide side,
                                                  final int priceLevel,
                                                  @Decimal final long price,
                                                  @Decimal final long size,
                                                  final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .shouldStoreQuoteTimestamps(true)
                .build();
        createBook(opt);
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
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
    public void shouldNotStoreQuoteTimestamp_L1Quote(final int maxExchangeDepth,
                                                     final int bbo,
                                                     final QuoteSide side,
                                                     final int priceLevel,
                                                     @Decimal final long price,
                                                     @Decimal final long size,
                                                     final long numberOfOrders,
                                                     final boolean addStatistics) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .shouldStoreQuoteTimestamps(false)
                .build();
        createBook(opt);
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);
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
    public void incrementalUpdate_Insert_L1Quote_invalidPackage(final int maxExchangeDepth,
                                                                final int bbo,
                                                                final QuoteSide side,
                                                                final int priceLevel,
                                                                @Decimal final long price,
                                                                @Decimal final long size,
                                                                final long numberOfOrders) {
        Assertions.assertFalse(simulateL1Insert(side, price, size, numberOfOrders));
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        Assertions.assertFalse(simulateL1Insert(side, price, size, numberOfOrders));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_L2Quote_invalidSymbol(final int maxExchangeDepth,
                                                               final int bbo,
                                                               final QuoteSide side,
                                                               final int priceLevel,
                                                               @Decimal final long price,
                                                               @Decimal final long size,
                                                               final long numberOfOrders) {
        Assertions.assertFalse(simulateL2Insert(LTC_SYMBOL, COINBASE, side, priceLevel, price, size, numberOfOrders));
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        Assertions.assertFalse(simulateL2Insert(LTC_SYMBOL, COINBASE, side, priceLevel, price, size, numberOfOrders));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Delete_base_L2Quote(final int maxExchangeDepth,
                                                      final int bbo,
                                                      final QuoteSide side,
                                                      final int priceLevel,
                                                      @Decimal final long price,
                                                      @Decimal final long size,
                                                      final long numberOfOrders) {
        simulateL2Delete(BINANCE, side, priceLevel, price, size, numberOfOrders);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_Insert_base_L2Quote(final int maxExchangeDepth,
                                                    final int bbo,
                                                    final QuoteSide side,
                                                    final int priceLevel,
                                                    @Decimal final long price,
                                                    @Decimal final long size,
                                                    final long numberOfOrders) {
        simulateL2Insert(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_Update_base_L2Quote(final int maxExchangeDepth,
                                                    final int bbo,
                                                    final QuoteSide side,
                                                    final int priceLevel,
                                                    @Decimal final long price,
                                                    @Decimal final long size,
                                                    final long numberOfOrders) {
        simulateL2Update(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void l2Quote_bbo_quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getWorstQuote());

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getWorstQuote());

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);

        final int expectedWorstBid = bbo - maxDepth + 1;
        final int expectedWorstAsk = bbo + maxDepth - 1;

        Assertions.assertEquals(expectedWorstBid, Decimal64Utils.toInt(getBook().getMarketSide(QuoteSide.BID).getWorstQuote().getPrice()));
        Assertions.assertEquals(bbo, Decimal64Utils.toInt(getBook().getMarketSide(QuoteSide.BID).getBestQuote().getPrice()));
        Assertions.assertEquals(bbo, Decimal64Utils.toInt(getBook().getMarketSide(QuoteSide.ASK).getBestQuote().getPrice()));
        Assertions.assertEquals(expectedWorstAsk, Decimal64Utils.toInt(getBook().getMarketSide(QuoteSide.ASK).getWorstQuote().getPrice()));

        getBook().clear();

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getWorstQuote());

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getWorstQuote());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT"})
    public void snapshot_L2Quote_base_clear(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        getBook().clear();
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L2Quote_base_totalQuantity(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        @Decimal final long expectedTotalQuantity = Decimal64Utils.fromInt(size * maxDepth);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertTotalQuantity(QuoteSide.BID, expectedTotalQuantity);
        assertTotalQuantity(QuoteSide.ASK, expectedTotalQuantity);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void incrementalUpdate_L2Quote_addLevelMoreThenSnapshotDepth(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 250;
        final int size = 5;
        final int numberOfOrders = 25;

        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);

        //TODO refactor (make more simple)
        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        for (int i = 0; i < maxDepth; i++) {
            simulateL2Insert(COINBASE,
                    QuoteSide.ASK,
                    maxDepth + i,
                    bbo + maxDepth + i,
                    size, numberOfOrders);
            assertBookSize(QuoteSide.ASK, maxDepth + i + 1);
        }
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, 2 * maxDepth);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
        assertPrice(QuoteSide.BID, 0, getExpectedQuotePrice(QuoteSide.BID, bbo, 0));
        assertPrice(QuoteSide.BID, 1, getExpectedQuotePrice(QuoteSide.BID, bbo, 1));
        assertPrice(QuoteSide.BID, 2, getExpectedQuotePrice(QuoteSide.BID, bbo, 2));
        assertPrice(QuoteSide.ASK, 0, getExpectedQuotePrice(QuoteSide.ASK, bbo, 0));
        assertPrice(QuoteSide.ASK, 1, getExpectedQuotePrice(QuoteSide.ASK, bbo, 1));
        assertPrice(QuoteSide.ASK, 2, getExpectedQuotePrice(QuoteSide.ASK, bbo, 2));

        for (int i = 0; i < maxDepth; i++) {
            simulateL2Insert(COINBASE,
                    QuoteSide.BID,
                    maxDepth + i,
                    bbo - maxDepth - i,
                    size, numberOfOrders);
            assertBookSize(QuoteSide.BID, maxDepth + i + 1);
        }
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_RandomDelete_InsertLast_L2Quote(final int maxExchangeDepth,
                                                                final int bbo,
                                                                final QuoteSide side,
                                                                final int priceLevel,
                                                                final long price,
                                                                final long size,
                                                                final long numberOfOrders) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        simulateL2Delete(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth - 1);
        if (side == QuoteSide.ASK) {
            simulateL2Insert(COINBASE, side, maxExchangeDepth - 1, bbo + maxExchangeDepth + 1, size, numberOfOrders);
        } else {
            simulateL2Insert(COINBASE, side, maxExchangeDepth - 1, bbo - maxExchangeDepth + 1, size, numberOfOrders);
        }
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    @ParameterizedTest
    @MethodSource("sideProvider")
    public void snapshot_L2Quote_snapshotAfterDelete(
            final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;
        final int numberOfDeletedQuotes = 2;

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size, numberOfOrders);
        simulateL2Delete(numberOfDeletedQuotes, COINBASE, side, 0, 0, 0);
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSize(side, maxDepth);
    }

    @Test
    public void snapshot_L2Quote_base_isEmpty() {
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);
        Assertions.assertTrue(getBook().isEmpty());
    }

    //TODO Refactor this test (make more simple)
    @ParameterizedTest
    @MethodSource("packageTypeAndSideProvider")
    public void incrementUpdate_Update_Skip_Gaps_Quote(final PackageType packageType,
                                                       final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .validationOptions(ValidationOptions.builder()
                        .validateQuoteInsert()
                        .skipInvalidQuoteUpdate()
                        .build())
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        simulateL2Update(COINBASE, side, 0, Decimal64Utils.fromInt(0), Decimal64Utils.fromInt(size), numberOfOrders);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_unreachableDepthModeSkip_insert_L2Quote(final int maxExchangeDepth,
                                                                          final int bbo,
                                                                          final QuoteSide side,
                                                                          final int priceLevel,
                                                                          final long price,
                                                                          final long size,
                                                                          final long numOfOrders) {

        final int maxDepth = 2;
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .maxDepth(maxDepth)
                .validationOptions(ValidationOptions.builder()
                        .skipInvalidQuoteInsert()
                        .skipInvalidQuoteUpdate()
                        .build())
                .build();

        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        if (side == QuoteSide.ASK) {
            simulateL2Insert(COINBASE, side, priceLevel, bbo + 1, size, numOfOrders);
        } else {
            simulateL2Insert(COINBASE, side, priceLevel, bbo - 1, size, numOfOrders);
        }
        assertBookSizeBySides(maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_gapModeSkip_delete_L2Quote(final int maxExchangeDepth,
                                                             final int bbo,
                                                             final QuoteSide side,
                                                             final int priceLevel,
                                                             @Decimal final long price,
                                                             @Decimal final long size,
                                                             final long numOfOrders) {

        int maxDepth = 2;
        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(maxDepth)
                .validationOptions(ValidationOptions.builder()
                        .validateQuoteInsert()
                        .skipInvalidQuoteUpdate()
                        .build())
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Delete(COINBASE, side, priceLevel, price, size, numOfOrders);
        simulateL2Delete(COINBASE, side, priceLevel, price, size, numOfOrders);

        if (priceLevel == 0) {
            maxDepth = 0;
        } else if (priceLevel < maxDepth) {
            maxDepth = maxDepth - 1;
        }
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT"}
    )
    // TODO: 11/28/2022 refactor.. Add optimization during update snapshot with same depth
    public void snapshot_byOneSide(final PackageType packageType) {
        final int maxDepth = 4;
        final int processDepth = 4;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

//      Filling two side 4:4
        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSizeBySides(processDepth);
//      Cleaning one side 0:4
        simulateL2DeleteAllQuoteBySide(QuoteSide.BID);
        assertExchangeBookSizeIsEmpty(QuoteSide.BID);
        assertBookSize(QuoteSide.ASK, maxDepth);
//      Filling opposite side 4:0
        simulateL2QuoteSnapshotBySide(packageType, COINBASE, maxDepth, QuoteSide.BID, bbo, size, numberOfOrders, true);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertExchangeBookSizeIsEmpty(QuoteSide.ASK);
//      Filling two side 2:2
        simulateL2QuoteSnapshot(packageType, COINBASE, 2, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, 2);
        assertBookSize(QuoteSide.ASK, 2);
//      Filling two side 4:4
        simulateL2QuoteSnapshot(packageType, COINBASE, 4, bbo, size, numberOfOrders);
        assertBookSize(QuoteSide.BID, 4);
        assertBookSize(QuoteSide.ASK, 4);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_unreachableDepthMode_SkipAndDrop_insert_L2Quote(final int maxExchangeDepth,
                                                                                  final int bbo,
                                                                                  final QuoteSide side,
                                                                                  final int priceLevel,
                                                                                  @Decimal final long price,
                                                                                  @Decimal final long size,
                                                                                  final long numOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .maxDepth(priceLevel)
                .validationOptions(ValidationOptions.builder()
                        .validateQuoteInsert()
                        .validateQuoteUpdate()
                        .build())
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Insert(COINBASE, side, priceLevel + 2, price, size, numOfOrders);
        assertBookSizeBySides(0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void securityStatusMessage_L2Quote(final int maxExchangeDepth,
                                              final int bbo,
                                              final QuoteSide side,
                                              final int priceLevel,
                                              @Decimal final long price,
                                              @Decimal final long size,
                                              final long numberOfOrders,
                                              final boolean addStatistics) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);

        simulateSecurityFeedStatus(COINBASE, FeedStatus.NOT_AVAILABLE);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL2Insert(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure insert is ignored (we are waiting for snapshot)

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders, addStatistics);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L2Quote_WaitingSnapshot(final int maxExchangeDepth,
                                                   final int bbo,
                                                   final QuoteSide side,
                                                   final int priceLevel,
                                                   @Decimal final long price,
                                                   @Decimal final long size,
                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL2Insert(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure insert is ignored (we are waiting for snapshot)

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L2Quote_PeriodicalSnapshotMode_ONLY_ONE(final int maxExchangeDepth,
                                                                   final int bbo,
                                                                   final QuoteSide side,
                                                                   final int priceLevel,
                                                                   @Decimal final long price,
                                                                   @Decimal final long size,
                                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.ONLY_ONE)
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);

        double updatePrice = price;
        if (side == QuoteSide.ASK) {
            updatePrice = updatePrice - 0.5;
        } else {
            updatePrice = updatePrice + 0.5;
        }

        simulateL2Insert(COINBASE, side, priceLevel, updatePrice, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth + 1); // make sure insert is processed

        Assertions.assertFalse(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth + 1); // make sure book is ignore periodical snapshot after insert

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        Assertions.assertFalse(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
    }


    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L2Quote_PeriodicalSnapshotMode_SKIP_ALL(final int maxExchangeDepth,
                                                                   final int bbo,
                                                                   final QuoteSide side,
                                                                   final int priceLevel,
                                                                   @Decimal final long price,
                                                                   @Decimal final long size,
                                                                   final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.SKIP_ALL)
                .build();
        createBook(opt);

        Assertions.assertFalse(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        Assertions.assertTrue(simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);

        Assertions.assertFalse(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertFalse(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L2Quote_PeriodicalSnapshotMode_PROCESS_ALL(final int maxExchangeDepth,
                                                                      final int bbo,
                                                                      final QuoteSide side,
                                                                      final int priceLevel,
                                                                      @Decimal final long price,
                                                                      @Decimal final long size,
                                                                      final long numberOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.PROCESS_ALL)
                .build();
        createBook(opt);

        Assertions.assertTrue(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        Assertions.assertTrue(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);

        Assertions.assertTrue(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        Assertions.assertTrue(simulateL2QuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders));
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    @Test
    public void resetEntry() {
        simulateResetEntry(DEFAULT_EXCHANGE_ID, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(QuoteSide.BID);
        assertExchangeBookSizeIsEmpty(QuoteSide.ASK);
        Assertions.assertFalse(getBook().getExchanges().isEmpty());
    }

    @Test
    public void resetEntry_Invalid() {
        Assertions.assertFalse(simulateResetEntry(TypeConstants.EXCHANGE_NULL, VENDOR_SNAPSHOT));
        Assertions.assertTrue(getBook().isEmpty());
        Assertions.assertTrue(getBook().getExchanges().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_L2Quote_NoWaitingSnapshot(final int maxExchangeDepth,
                                                     final int bbo,
                                                     final QuoteSide side,
                                                     final int priceLevel,
                                                     @Decimal final long price,
                                                     @Decimal final long size,
                                                     final long numberOfOrders) {
        OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL2Insert(COINBASE, side, BEST_LEVEL, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, 1); // make sure insert is not ignored

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);


        opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertExchangeBookSizeIsEmpty(COINBASE, side); // make sure book is clean

        simulateL2Insert(COINBASE, side, BEST_LEVEL, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, 1); // make sure insert is not ignored

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    //TODO add a mode for unconsolidated date

//    @ParameterizedTest
//    @MethodSource("quoteProvider")
//    public void incrementalUpdate_Insert_Duplicate_L2Quote(final int depth,
//                                                           final int bbo,
//                                                           final QuoteSide side,
//                                                           final int priceLevel,
//                                                           @Decimal final long price,
//                                                           @Decimal final long size,
//                                                           final long numberOfOrders) {
//        simulateL2QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, depth, bbo, size, numberOfOrders);
//
//        simulateL2Insert(COINBASE, side, priceLevel, price, size, numberOfOrders);
//        assertExchangeBookSize(COINBASE, side, depth + 1);
//
//        simulateL2Insert(COINBASE, side, priceLevel, price, size, numberOfOrders);
//        assertExchangeBookSize(COINBASE, side, depth + 1);
//    }

}
