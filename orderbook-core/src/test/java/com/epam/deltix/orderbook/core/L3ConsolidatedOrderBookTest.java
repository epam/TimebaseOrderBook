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

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookFactory;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.fwk.AbstractL3QuoteLevelTest;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.InsertType;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;


/**
 * @author Andrii_Ostapenko1
 */
public class L3ConsolidatedOrderBookTest extends AbstractL3QuoteLevelTest {

    public BindOrderBookOptionsBuilder opt = new OrderBookOptionsBuilder()
            .symbol(DEFAULT_SYMBOL)
            .orderBookType(OrderBookType.CONSOLIDATED)
            .quoteLevels(DataModelType.LEVEL_THREE)
            .initialDepth(10)
            .initialExchangesPoolSize(1)
            .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT);

    private OrderBook<OrderBookQuote> book = OrderBookFactory.create(opt.build());

    @Override
    public OrderBook<OrderBookQuote> getBook() {
        return book;
    }

    @Override
    public void createBook(final OrderBookOptions otherOpt) {
        opt.parent(otherOpt);
        book = OrderBookFactory.create(opt.build());
    }

    @Test
    public void incrementalUpdate_WrongExchange_L3Quote() {
        final QuoteSide side = QuoteSide.ASK;
        final int maxDepth = 10;
        final long bbo = -256;
        final long size = 10;

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + maxDepth, InsertType.ADD_BACK, null, bbo + 10, size);
        simulateCancel(BINANCE, "id" + maxDepth);
        assertBookSize(side, maxDepth + 1);
        assertExchangeBookSize(BINANCE, side, 0);
    }

    @Test
    public void incrementalUpdate_MissingExchange_L3Quote() {
        final QuoteSide side = QuoteSide.ASK;
        final int maxDepth = 10;
        final long bbo = -256;
        final long size = 10;

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        simulateInsert(DEFAULT_SYMBOL, BINANCE, side, "id" + maxDepth, InsertType.ADD_BACK, null, bbo + 10, size);
        simulateCancel(COINBASE, "id" + maxDepth);
        assertBookSize(side, maxDepth + 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_L3Quote(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        int totalDepth = maxDepth * 2;

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);
        assertBookSize(side, totalDepth);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + maxDepth, InsertType.ADD_BACK, null, price, size);
        assertExchangeBookSize(COINBASE, side, maxDepth + 1);
        assertBookSize(side, ++totalDepth); // We expect that the book size will be increased by 1

        simulateInsert(DEFAULT_SYMBOL, BINANCE, side, "id" + maxDepth, InsertType.ADD_BACK, null, price, size);
        assertExchangeBookSize(BINANCE, side, maxDepth + 1);
        assertBookSize(side, ++totalDepth); // We expect that the book size will be increased by 1

        final long pos = side == QuoteSide.BID ? bbo - price : price - bbo;
        assertEqualPosition(side, (short) (2 * pos), Decimal64Utils.fromLong(price), size, quoteId);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_invalidInsert_L3Quote(final int maxDepth,
                                                        final CharSequence quoteId,
                                                        final QuoteSide side,
                                                        final long price,
                                                        final long size,
                                                        final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);
        assertBookSize(side, 2 * maxDepth);
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size);
        assertExchangeBookSize(COINBASE, side, 0);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Cancel_L3Quote(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        simulateCancel(COINBASE, quoteId);
        assertExchangeBookSize(COINBASE, side, maxDepth - 1);

        simulateCancel(BINANCE, quoteId);
        assertExchangeBookSize(BINANCE, side, maxDepth - 1);

        assertBookSize(side, 2 * maxDepth - 2);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_CancelTwice_L3Quote(final int maxDepth,
                                                      final CharSequence quoteId,
                                                      final QuoteSide side,
                                                      final long price,
                                                      final long size,
                                                      final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, 2 * maxDepth - 1);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Modify_L3Quote(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        final int totalDepth = maxDepth * 2;

        simulateModify(COINBASE, quoteId, side, 1, price);
        assertExchangeBookSize(BINANCE, side, maxDepth);
        assertBookSize(side, totalDepth);
        assertEqualPosition(side, getQuotePositionById(side, quoteId), Decimal64Utils.fromLong(price), 1, quoteId);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Replace_L3Quote(final int maxDepth,
                                                  final CharSequence quoteId,
                                                  final QuoteSide side,
                                                  final long price,
                                                  final long size,
                                                  final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        final int totalDepth = maxDepth * 2;

        final long pos = side == QuoteSide.BID ? bbo - price : price - bbo;
        assertEqualPosition(side, (short) (2 * pos), Decimal64Utils.fromLong(price), size, quoteId);
        Assertions.assertEquals(getQuoteByPosition(side, (int) (2 * pos)).getExchangeId(), COINBASE);
        simulateReplace(COINBASE, quoteId, side, price, size);
        assertEqualPosition(side, (short) (2 * pos + 1), Decimal64Utils.fromLong(price), size, quoteId);
        Assertions.assertEquals(getQuoteByPosition(side, (int) (2 * pos + 1)).getExchangeId(), COINBASE); // lost priority

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        final QuoteSide newSide = side == QuoteSide.ASK ? QuoteSide.BID : QuoteSide.ASK;
        simulateReplace(BINANCE, quoteId, newSide, bbo, size);
        assertExchangeBookSize(BINANCE, side, maxDepth - 1);
        assertBookSize(newSide, totalDepth + 1);
        assertEqualPosition(newSide, (short) 2, Decimal64Utils.fromLong(bbo), size, quoteId); // pos is now 3rd, 2 were there since snapshot
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_invalidReplace_L3Quote(final int maxDepth,
                                                         final CharSequence quoteId,
                                                         final QuoteSide side,
                                                         final long price,
                                                         final long size,
                                                         final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);

        final int totalDepth = maxDepth * 2;

        final QuoteSide newSide = side == QuoteSide.ASK ? QuoteSide.BID : QuoteSide.ASK;
        simulateReplace(BINANCE, quoteId, newSide, bbo, size);
        assertExchangeBookSize(BINANCE, side, maxDepth - 1);
        assertBookSize(newSide, totalDepth + 1);
        assertEqualPosition(newSide, (short) 2, Decimal64Utils.fromLong(bbo), size, quoteId);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_invalidModify(final int maxDepth,
                                                final CharSequence quoteId,
                                                final QuoteSide side,
                                                final long price,
                                                final long size,
                                                final long bbo) {

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);
        assertBookSize(side, 2 * maxDepth);
        simulateModify(COINBASE, quoteId, side, size + 10, price);
        assertBookSize(side, maxDepth);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, 2 * maxDepth);
        simulateModify(BINANCE, quoteId, side, size, price - 1);
        assertBookSize(side, maxDepth);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, BINANCE, maxDepth, bbo, size);
        assertBookSize(side, 2 * maxDepth);
        simulateModify(COINBASE, quoteId, side, size - 100, price);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_L3Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(packageType, BINANCE, maxDepth, bbo, size);

        assertBookSize(QuoteSide.BID, maxDepth * 2);
        assertBookSize(QuoteSide.ASK, maxDepth * 2);

        assertIteratorBookQuotes(QuoteSide.ASK, 2 * maxDepth, bbo, size, 2);
        assertIteratorBookQuotes(QuoteSide.BID, 2 * maxDepth, bbo, size, 2);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void resetEntry_L3Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        simulateQuoteSnapshot(packageType, BINANCE, maxDepth, bbo, size);

        simulateResetEntry(COINBASE, packageType);
        assertIteratorBookQuotes(QuoteSide.ASK, maxDepth, bbo, size);
        assertIteratorBookQuotes(QuoteSide.BID, maxDepth, bbo, size);

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
}
