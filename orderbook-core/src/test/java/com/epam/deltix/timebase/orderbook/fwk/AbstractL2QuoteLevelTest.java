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
package com.epam.deltix.timebase.orderbook.fwk;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.timebase.orderbook.options.GapMode;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptions;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;

/**
 * @author Andrii_Ostapenko1
 * @created 17/01/2022 - 10:12 PM
 */
public abstract class AbstractL2QuoteLevelTest extends AbstractOrderBookTest {

    public static Stream<Arguments> packageTypeAndSideProvider() {
        return Stream.of(
                Arguments.of(VENDOR_SNAPSHOT, QuoteSide.ASK),
                Arguments.of(PackageType.PERIODICAL_SNAPSHOT, QuoteSide.ASK),
                Arguments.of(VENDOR_SNAPSHOT, QuoteSide.BID),
                Arguments.of(PackageType.PERIODICAL_SNAPSHOT, QuoteSide.BID)
        );
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_L1Quote_invalidPackage(final int maxExchangeDepth,
                                                                final int bbo,
                                                                final QuoteSide side,
                                                                final short priceLevel,
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
                                                               final short priceLevel,
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
                                                      final short priceLevel,
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
                                                    final short priceLevel,
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
                                                    final short priceLevel,
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

        @Decimal long expectedTotalQuantity = Decimal64Utils.fromInt(size * maxDepth);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertTotalQuantity(QuoteSide.BID, expectedTotalQuantity);
        assertTotalQuantity(QuoteSide.ASK, expectedTotalQuantity);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_RandomDelete_InsertLast_L2Quote(final int maxExchangeDepth,
                                                                final int bbo,
                                                                final QuoteSide side,
                                                                final short priceLevel,
                                                                @Decimal final long price,
                                                                @Decimal final long size,
                                                                final long numberOfOrders) {
        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
        simulateL2Delete(COINBASE, side, priceLevel, price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth - 1);
        simulateL2Insert(COINBASE, side, (short) (maxExchangeDepth - 1), price, size, numberOfOrders);
        assertExchangeBookSize(COINBASE, side, maxExchangeDepth);
    }

    @ParameterizedTest
    @MethodSource("packageTypeAndSideProvider")
    public void snapshot_L2Quote_snapshotAfterDelete(final PackageType packageType,
                                                     final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;
        final int numberOfDeletedQuotes = 2;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        simulateL2Delete(numberOfDeletedQuotes, COINBASE, side, (short) 0, 0, 0);
        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
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
    public void incrementUpdate_Insert_Skip_Gaps_Quote(final PackageType packageType,
                                                       final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final int numberOfDeletedQuotes = 4;
        final int expectedDepth = maxDepth - numberOfDeletedQuotes;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        simulateL2Delete(numberOfDeletedQuotes, COINBASE, side, 0, 0, 0);

        for (int i = expectedDepth + 1; i < maxDepth + 1; i++) {
            simulateL2Insert(COINBASE, side, (short) 8, Decimal64Utils.fromInt(80), Decimal64Utils.fromInt(size), numberOfOrders);
            assertBookSize(side, expectedDepth);
        }
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

        final int numberOfDeletedQuotes = 2;
        final int expectedDepth = maxDepth - numberOfDeletedQuotes;

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        simulateL2Delete(numberOfDeletedQuotes, COINBASE, side, 0, 0, 0);

        for (int i = expectedDepth; i < maxDepth + 1; i++) {
            simulateL2Update(COINBASE, side, (short) 8, Decimal64Utils.fromInt(80), Decimal64Utils.fromInt(size), numberOfOrders);
            assertBookSize(side, expectedDepth);
        }
    }


    // GAP MODE

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_maxDepth_gapModeSkip_L2Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int processDepth = 5;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(processDepth).gapMode(GapMode.SKIP).build();
        createBook(opt);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSizeBySides(processDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_gapModeSkip_dynamic_insert_L2Quote(final int maxExchangeDepth,
                                                                     final int bbo,
                                                                     final QuoteSide side,
                                                                     final short priceLevel,
                                                                     @Decimal final long price,
                                                                     @Decimal final long size,
                                                                     final long numOfOrders) {

        int maxDepth = priceLevel + 1;
        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(maxDepth).gapMode(GapMode.SKIP).build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Insert(COINBASE, side, priceLevel, price, size, numOfOrders);
        assertBookSizeBySides(maxDepth);
        assertEqualLevel(side, priceLevel, price, size, numOfOrders);
    }


    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_gapModeSkip_insert_L2Quote(final int maxExchangeDepth,
                                                             final int bbo,
                                                             final QuoteSide side,
                                                             final short priceLevel,
                                                             @Decimal final long price,
                                                             @Decimal final long size,
                                                             final long numOfOrders) {

        int maxDepth = 2;
        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(maxDepth).gapMode(GapMode.SKIP).build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Insert(COINBASE, side, priceLevel, price, size, numOfOrders);
        assertBookSizeBySides(maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_gapModeSkip_delete_L2Quote(final int maxExchangeDepth,
                                                             final int bbo,
                                                             final QuoteSide side,
                                                             final short priceLevel,
                                                             @Decimal final long price,
                                                             @Decimal final long size,
                                                             final long numOfOrders) {

        int maxDepth = 2;
        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(maxDepth).gapMode(GapMode.SKIP).build();
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
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_maxDepth_gapModeSkipAndDrop_L2Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int processDepth = 5;
        final int bbo = 25;
        final int size = 5;
        final int numberOfOrders = 25;

        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(processDepth).gapMode(GapMode.SKIP_AND_DROP).build();
        createBook(opt);

        simulateL2QuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size, numberOfOrders);
        assertBookSizeBySides(processDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_maxDepth_gapModeSkipAndDrop_insert_L2Quote(final int maxExchangeDepth,
                                                                    final int bbo,
                                                                    final QuoteSide side,
                                                                    final short priceLevel,
                                                                    @Decimal final long price,
                                                                    @Decimal final long size,
                                                                    final long numOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .maxDepth(priceLevel)
                .gapMode(GapMode.SKIP_AND_DROP)
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Insert(COINBASE, side, (short) (priceLevel + 2), price, size, numOfOrders);
        assertBookSizeBySides(0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void snapshot_gapModeSkipAndDrop_insert_L2Quote(final int maxExchangeDepth,
                                                           final int bbo,
                                                           final QuoteSide side,
                                                           final short priceLevel,
                                                           @Decimal final long price,
                                                           @Decimal final long size,
                                                           final long numOfOrders) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .gapMode(GapMode.SKIP_AND_DROP)
                .build();
        createBook(opt);

        simulateL2QuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxExchangeDepth, bbo, size, numOfOrders);
        simulateL2Insert(COINBASE, side, (short) (maxExchangeDepth + priceLevel), price, size, numOfOrders);
        assertBookSizeBySides(0);
    }

}
