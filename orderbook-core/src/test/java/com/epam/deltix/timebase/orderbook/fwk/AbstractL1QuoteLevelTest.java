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
import com.epam.deltix.timebase.orderbook.options.OrderBookOptions;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

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
    public void incrementalUpdate_Insert_L1Quote_invalidSymbol(final int bbo,
                                                               final QuoteSide side,
                                                               @Decimal final long price,
                                                               @Decimal final long size,
                                                               final long numberOfOrders) {
        Assertions.assertFalse(simulateL1Insert(LTC_SYMBOL, BINANCE, side, price, size, numberOfOrders));
        simulateL1QuoteSnapshot(PackageType.VENDOR_SNAPSHOT, COINBASE, bbo, size, numberOfOrders);
        Assertions.assertFalse(simulateL1Insert(LTC_SYMBOL, BINANCE, side, price, size, numberOfOrders));
    }

}
