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

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.*;
import com.epam.deltix.orderbook.core.fwk.AbstractL3QuoteLevelTest;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static com.epam.deltix.timebase.messages.universal.PackageType.INCREMENTAL_UPDATE;
import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.ASK;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.BID;
import static org.mockito.Mockito.*;

/**
 * @author Andrii_Ostapenko1
 */
public class L3SingleExchangeOrderBookTest extends AbstractL3QuoteLevelTest {

    public BindOrderBookOptionsBuilder opt = new OrderBookOptionsBuilder()
            .symbol(DEFAULT_SYMBOL)
            .orderBookType(OrderBookType.SINGLE_EXCHANGE)
            .quoteLevels(DataModelType.LEVEL_THREE)
            .initialDepth(10)
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

    private PackageHeader buildPackageHeaderSnapshot() {
        final PackageHeader message = new PackageHeader();
        message.setPackageType(VENDOR_SNAPSHOT);
        message.setEntries(new ObjectArrayList<>());
        message.setSymbol(DEFAULT_SYMBOL);
//         message.setInstrumentType(InstrumentType.FX);
        message.setTimeStampMs(System.currentTimeMillis());
        return message;
    }

    private PackageHeader buildPackageHeaderIncrement() {
        final PackageHeader message = new PackageHeader();
        message.setPackageType(INCREMENTAL_UPDATE);
        message.setEntries(new ObjectArrayList<>());
        message.setSymbol(DEFAULT_SYMBOL);
//        message.setInstrumentType(InstrumentType.FX);
        message.setTimeStampMs(System.currentTimeMillis());
        return message;
    }

    public void insert(final PackageHeader msg,
                       final QuoteSide side,
                       final int quantity,
                       final String price,
                       final String quoteId) {
        final L3EntryNew entry = new L3EntryNew();
        entry.setInsertType(InsertType.ADD_BACK);
        entry.setSide(side);
        entry.setSize(Decimal64Utils.fromInt(quantity));
        entry.setPrice(Decimal64Utils.parse(price));
        entry.setQuoteId(quoteId);
        entry.setExchangeId(COINBASE);
        entry.setParticipantId(null);
        msg.getEntries().add(entry);
    }

    public void update(final PackageHeader msg,
                       final QuoteSide side,
                       final int quantity,
                       final String price,
                       final String quoteId,
                       final QuoteUpdateAction action) {
        final L3EntryUpdate entry = new L3EntryUpdate();
        entry.setAction(action);
        entry.setSide(side);
        entry.setSize(Decimal64Utils.fromInt(quantity));
        entry.setPrice(Decimal64Utils.parse(price));
        entry.setQuoteId(quoteId);
        entry.setExchangeId(COINBASE);
        entry.setParticipantId(null);
        msg.getEntries().add(entry);
    }

    @Test
    public void simple() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder().build();
        createBook(opt);
        final PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "99.99", "B2");
        insert(packageHeader, BID, 1, "78.99898", "B1");
        insert(packageHeader, ASK, 1, "100.001", "A1");
        book.update(packageHeader);

        Assertions.assertFalse(book.isEmpty());

        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        Assertions.assertEquals(2, bids.depth());
        Assertions.assertTrue(bids.iterator().hasNext(), "iterator is not empty");

        final OrderBookQuote bestBid = bids.iterator().next();
        Assertions.assertTrue(CharSequenceUtils.equals("B2", bestBid.getQuoteId()));
    }

    @Test
    @DisplayName("incremental insert: insert of new best quote")
    public void simple_incremental_insert() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder().build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "78.99898", "B1");
        book.update(packageHeader);

        final int initialDepth = bids.depth() + asks.depth();

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        insert(packageHeader, BID, 1, "99.99", "B2");
        book.update(packageHeader);

        Assertions.assertEquals(initialDepth + 1, bids.depth());
        Assertions.assertTrue(CharSequenceUtils.equals("B2", bids.getBestQuote().getQuoteId()));
    }

    @Test
    @DisplayName("incremental update: replace quote of book with depth 1")
    public void simple_incremental_update() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder().maxDepth(1).build();
        createBook(opt);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "99.99", "B2");
        insert(packageHeader, ASK, 1, "78.99898", "A1");
        book.update(packageHeader);

        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);
        Assertions.assertTrue(CharSequenceUtils.equals("A1", asks.getBestQuote().getQuoteId()));

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, ASK, 1, "78.998", "B2", QuoteUpdateAction.REPLACE);
        book.update(packageHeader);

        Assertions.assertEquals(1, asks.depth());
        Assertions.assertTrue(CharSequenceUtils.equals("B2", asks.getBestQuote().getQuoteId()));
    }

    @Test
    @DisplayName("Examples from")
    public void example1() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder().build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        Assertions.assertTrue(book.isEmpty());

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, ASK, 1, "10.15", "id0");
        insert(packageHeader, ASK, 1, "10.15", "id1");
        insert(packageHeader, ASK, 1, "10.15", "id2");
        insert(packageHeader, ASK, 1, "10.2", "id3");
        insert(packageHeader, ASK, 1, "10.2", "id4");
        book.update(packageHeader);

        Assertions.assertEquals(packageHeader.getEntries().size(), asks.depth());
        Assertions.assertEquals(0, bids.depth());

        packageHeader = buildPackageHeaderIncrement();
        insert(packageHeader, ASK, 1, "10.15", "id6");
        book.update(packageHeader);

        final int curDepth = bids.depth() + asks.depth();
        Assertions.assertEquals(6, curDepth);
        System.out.println(Decimal64Utils.toDouble(Decimal64Utils.parse("10.15")));
        assertEqualPosition(ASK, (short) 3, Decimal64Utils.parse("10.15"), 1, "id6");
    }

    @Test
    @DisplayName("Examples from")
    public void examples5_10() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder().build();
        createBook(opt);

        Assertions.assertTrue(book.isEmpty());

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 100, "10.15", "id0");
        insert(packageHeader, ASK, 20, "10.2", "id1");
        insert(packageHeader, BID, 20, "10.15", "id2");
        insert(packageHeader, ASK, 40, "10.2", "id3");
        insert(packageHeader, BID, 30, "10.15", "id4");
        insert(packageHeader, BID, 40, "10.1", "id5");
        insert(packageHeader, ASK, 50, "10.25", "id6");
        insert(packageHeader, BID, 2, "10.1", "id7");
        insert(packageHeader, ASK, 100, "10.25", "id8");
        insert(packageHeader, BID, 20, "10.05", "id9");
        insert(packageHeader, ASK, 50, "10.35", "id10");
        insert(packageHeader, BID, 20, "10.00", "id11");
        insert(packageHeader, ASK, 50, "10.35", "id12");
        insert(packageHeader, ASK, 20, "10.35", "id13");
        insert(packageHeader, BID, 90, "9.95", "id14");
        insert(packageHeader, ASK, 20, "10.4", "id15");
        insert(packageHeader, BID, 90, "9.95", "id16");

        Assertions.assertTrue(book.update(packageHeader));

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, ASK, 40, "10.25", "id6", QuoteUpdateAction.MODIFY);
        Assertions.assertTrue(book.update(packageHeader));
        assertEqualPosition(ASK, (short) 2, Decimal64Utils.parse("10.25"), 40, "id6");

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, ASK, 30, "10.25", "id6", QuoteUpdateAction.REPLACE);
        Assertions.assertTrue(book.update(packageHeader));
        assertEqualPosition(ASK, (short) 2, Decimal64Utils.parse("10.25"), 100, "id8");
        assertEqualPosition(ASK, (short) 3, Decimal64Utils.parse("10.25"), 30, "id6");

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, ASK, 40, "10.25", "id5", QuoteUpdateAction.CANCEL);
        Assertions.assertTrue(book.update(packageHeader));
        assertEqualPosition(BID, (short) 3, Decimal64Utils.parse("10.1"), 2, "id7");

//        System.out.println("Example 8:");
//        packageHeader = increment()
//        update(packageHeader, BID, 30, "10.12", "id4", QuoteUpdateAction.MODIFY)
//                .build();
//        Assertions.assertFalse(book.update(packageHeader));

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 30, "10.12", "id4", QuoteUpdateAction.REPLACE);
        Assertions.assertTrue(book.update(packageHeader));
        assertEqualPosition(BID, (short) 2, Decimal64Utils.parse("10.12"), 30, "id4");
        assertEqualPosition(BID, (short) 3, Decimal64Utils.parse("10.1"), 2, "id7");

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 80, "10.25", "id8", QuoteUpdateAction.REPLACE);
        Assertions.assertTrue(book.update(packageHeader));
        assertEqualPosition(BID, (short) 0, Decimal64Utils.parse("10.25"), 80, "id8");

    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should add new quote in order book")
    public void snapshot_of_size_1(final int maxDepth,
                                   final CharSequence quoteId,
                                   final QuoteSide side,
                                   final long price,
                                   final long size,
                                   final long bbo) {
        final PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, side, (int) size, Long.toString(price), quoteId.toString());
        getBook().update(packageHeader);
        assertBookSize(side, 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should add new quote in order book")
    public void incrementalUpdate_Insert(final int maxDepth,
                                         final CharSequence quoteId,
                                         final QuoteSide side,
                                         final long price,
                                         final long size,
                                         final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + maxDepth, InsertType.ADD_BACK, quoteId, price, size);
        assertBookSize(side, maxDepth + 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should not add new quote in order book")
    public void incrementalUpdate_invalidInsert(final int maxDepth,
                                                final CharSequence quoteId,
                                                final QuoteSide side,
                                                final long price,
                                                final long size,
                                                final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should cancel quote in order book")
    public void incrementalUpdate_Cancel(final int maxDepth,
                                         final CharSequence quoteId,
                                         final QuoteSide side,
                                         final long price,
                                         final long size,
                                         final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, price, size);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, maxDepth - 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should fail to cancel the same quote twice")
    public void incrementalUpdate_CancelTwice(final int maxDepth,
                                              final CharSequence quoteId,
                                              final QuoteSide side,
                                              final long price,
                                              final long size,
                                              final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, maxDepth - 1);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should replace quote and lose its priority")
    public void incrementalUpdate_Replace(final int maxDepth,
                                          final CharSequence quoteId,
                                          final QuoteSide side,
                                          final long price,
                                          final long size,
                                          final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, maxDepth + 1); // now there are two quotes with same price
        final short oldIdx = getQuotePositionById(side, quoteId);
        simulateReplace(COINBASE, quoteId, side, price, size);
        final short newIdx = getQuotePositionById(side, quoteId);
        Assertions.assertNotEquals(oldIdx, newIdx);
        assertEqualPosition(side, oldIdx, Decimal64Utils.fromLong(price), size, "id" + 2 * maxDepth); // new quote took its place
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should modify quote in order book and preserve its priority")
    public void incrementalUpdate_Modify(final int maxDepth,
                                         final CharSequence quoteId,
                                         final QuoteSide side,
                                         final long price,
                                         final long size,
                                         final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);

        simulateModify(COINBASE, quoteId, side, size - 1, price);
        assertBookSize(side, maxDepth);
        assertEqualPosition(side, getQuotePositionById(side, quoteId), Decimal64Utils.fromLong(price), size - 1, quoteId);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should not modify quote in order book")
    public void incrementalUpdate_invalidModify(final int maxDepth,
                                                final CharSequence quoteId,
                                                final QuoteSide side,
                                                final long price,
                                                final long size,
                                                final long bbo) {

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateModify(COINBASE, quoteId, side, size + 10, price);
        assertBookSize(side, 0);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateModify(COINBASE, quoteId, side, size, price - 1);
        assertBookSize(side, 0);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateModify(COINBASE, quoteId, side, size - 100, price);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, mode = EnumSource.Mode.INCLUDE, names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot(final PackageType packageType) {
        int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(BID, maxDepth);
        assertBookSize(ASK, maxDepth);

        maxDepth = maxDepth - 4;
        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(BID, maxDepth);
        assertBookSize(ASK, maxDepth);

        maxDepth = maxDepth + 4;
        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(BID, maxDepth);
        assertBookSize(ASK, maxDepth);

        for (int i = 1; i < 10; i++) {
            maxDepth = i;
            simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
            assertBookSize(BID, maxDepth);
            assertBookSize(ASK, maxDepth);
        }

        for (int i = 9; i > 0; i--) {
            maxDepth = i;
            simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
            assertBookSize(BID, maxDepth);
            assertBookSize(ASK, maxDepth);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, mode = EnumSource.Mode.INCLUDE, names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void resetEntry(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        simulateResetEntry(COINBASE, packageType);

        assertBookSize(BID, 0);
        assertBookSize(ASK, 0);
        Assertions.assertTrue(book.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = QuoteSide.class, mode = EnumSource.Mode.INCLUDE)
    public void iterateSide(final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertIteratorBookQuotes(side, maxDepth, bbo, size);
    }


    @Test
    @DisplayName("incremental insert: cancel of missing quote of new best quote")
    public void simple_incremental_cancel() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 1, "99.99", "B3", QuoteUpdateAction.CANCEL);
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.UNKNOWN_QUOTE_ID);

    }

    // Mockito Tests
    @Test
    @DisplayName("incremental insert: double insert of the same quoteId")
    public void simple_incremental_mock_insert() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        insert(packageHeader, BID, 1, "99.99", "B1");
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.DUPLICATE_QUOTE_ID);
    }

    @Test
    @DisplayName("incremental insert: insert of the quote with negative size")
    public void simple_incremental_insert_without_price() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 1, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        insert(packageHeader, BID, -123, "-123.3", "B3");
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.BAD_SIZE);
    }

    @Test
    @DisplayName("incremental modify: does not exist")
    public void simple_incremental_modify() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 2, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 1, "78.99898", "B3", QuoteUpdateAction.MODIFY);
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.UNKNOWN_QUOTE_ID);
    }

    @Test
    @DisplayName("incremental modify: price changed")
    public void simple_incremental_modify_price() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 2, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 2, "78.99892", "B1", QuoteUpdateAction.MODIFY);
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.MODIFY_CHANGE_PRICE);
    }

    @Test
    @DisplayName("incremental modify: size increased")
    public void simple_incremental_modify_size() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 2, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 2, "78.99898", "B1", QuoteUpdateAction.MODIFY);
        book.update(packageHeader);

        verify(errorListener, times(0)).onError(packageHeader, EntryValidationCode.MODIFY_INCREASE_SIZE);

        packageHeader = buildPackageHeaderIncrement();
        update(packageHeader, BID, 4, "78.99898", "B1", QuoteUpdateAction.MODIFY);
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.MODIFY_INCREASE_SIZE);
    }

    @Test
    @DisplayName("incremental insert: exchanges differ")
    public void simple_incremental_wrong_exchangeId() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 2, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        final L3EntryUpdate entry = new L3EntryUpdate();
        entry.setAction(QuoteUpdateAction.MODIFY);
        entry.setSide(BID);
        entry.setSize(Decimal64Utils.fromInt(1));
        entry.setPrice(Decimal64Utils.parse("78.99892"));
        entry.setQuoteId("B1");
        entry.setExchangeId(BINANCE);
        entry.setParticipantId(null);
        packageHeader.getEntries().add(entry);
        book.update(packageHeader);

        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.EXCHANGE_ID_MISMATCH);
    }

    @Test
    @DisplayName("incremental insert: using action other than ADD_BACK")
    public void simple_incremental_add_not_back() {
        final ErrorListener errorListener = mock(ErrorListener.class);
        final OrderBookOptions opt = new OrderBookOptionsBuilder().errorListener(errorListener).build();
        createBook(opt);
        final MarketSide<OrderBookQuote> bids = book.getMarketSide(BID);
        final MarketSide<OrderBookQuote> asks = book.getMarketSide(ASK);

        PackageHeader packageHeader = buildPackageHeaderSnapshot();
        insert(packageHeader, BID, 2, "78.99898", "B1");
        book.update(packageHeader);

        Assertions.assertEquals(1, bids.depth());
        Assertions.assertTrue(asks.isEmpty());

        packageHeader = buildPackageHeaderIncrement();
        final L3EntryNew entry = new L3EntryNew();
        entry.setSide(BID);
        entry.setSize(Decimal64Utils.fromInt(1));
        entry.setPrice(Decimal64Utils.parse("78.99892"));
        entry.setQuoteId("B2");
        entry.setExchangeId(COINBASE);
        entry.setParticipantId(null);
        entry.setInsertType(InsertType.ADD_BEFORE);
        entry.setInsertBeforeQuoteId("B1");
        packageHeader.getEntries().add(entry);
        book.update(packageHeader);
        verify(errorListener, times(1)).onError(packageHeader, EntryValidationCode.UNSUPPORTED_INSERT_TYPE);
    }

}
