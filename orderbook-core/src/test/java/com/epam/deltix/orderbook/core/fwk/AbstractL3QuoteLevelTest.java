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

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.epam.deltix.timebase.messages.universal.PackageType.PERIODICAL_SNAPSHOT;
import static com.epam.deltix.timebase.messages.universal.PackageType.VENDOR_SNAPSHOT;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Andrii_Ostapenko1
 */
public abstract class AbstractL3QuoteLevelTest {

    public static final long COINBASE = AlphanumericUtils.toAlphanumericUInt64("COINBASE");
    public static final long BINANCE = AlphanumericUtils.toAlphanumericUInt64("BINANCE");

    public static final String DEFAULT_SYMBOL = "BTC";
    public static final String LTC_SYMBOL = "LTC";
    public static final long DEFAULT_EXCHANGE_ID = COINBASE;

    static Stream<Arguments> quoteProvider() {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        final List<Arguments> asks = new ArrayList<>(maxDepth);
        for (int level = 0; level < maxDepth; level++) {
            asks.add(arguments(maxDepth,
                    "id" + level,
                    QuoteSide.ASK,
                    bbo + level,
                    size + level,
                    bbo));
        }
        final List<Arguments> bids = new ArrayList<>(maxDepth);
        for (int level = 0; level < maxDepth; level++) {
            bids.add(arguments(maxDepth,
                    "id" + maxDepth + level,
                    QuoteSide.BID,
                    bbo - level,
                    size + level,
                    bbo));
        }
        return Stream.concat(asks.stream(), bids.stream());
    }

    public static Stream<Arguments> sideProvider() {
        return Stream.of(
                Arguments.of(QuoteSide.ASK),
                Arguments.of(QuoteSide.BID)
        );
    }

    public abstract OrderBook<OrderBookQuote> getBook();

    public abstract void createBook(OrderBookOptions otherOpt);

    @Test
    public void symbol_NotEmpty() {
        Assertions.assertTrue(getBook().getSymbol().hasValue());
        Assertions.assertEquals(DEFAULT_SYMBOL, getBook().getSymbol().get());
    }

    // Assertion

    @ParameterizedTest
    @EnumSource(value = QuoteSide.class)
    public void marketSide_getTotalQuantity_zero(final QuoteSide side) {
        assertTotalQuantity(side, Decimal64Utils.ZERO);
    }

    /**
     * Validates that the quote at the specified position in the market side matches the expected attributes.
     *
     * This method is crucial for ensuring data integrity and correctness within an order book in a trading system.
     * It fetches a quote by its side (either ASK or BID) and position index, then compares the quote's price, size,
     * and ID with the expected values. If any of the attributes do not match the expected values, this method
     * should trigger an assertion failure, indicating a discrepancy in the order book or a potential issue with
     * quote management.
     *
     * <p>Usage of this method is typically confined to testing scenarios, where verifying the correctness of the
     * order book's state is essential.</p>
     *
     * @param side          The market side (ASK or BID) of the quote to validate.
     * @param pos           The position index of the quote within its side of the market.
     * @param expectedPrice The expected price value of the quote for validation.
     * @param expectedSize  The expected size value of the quote for validation.
     * @param expectedId    The expected unique identifier of the quote for validation.
     *
     * @throws AssertionError if any of the quote's attributes (price, size, or ID) do not match the expected values.
     */
    public void assertEqualPosition(final QuoteSide side,
                                    final short pos,
                                    final long expectedPrice,
                                    final long expectedSize,
                                    final CharSequence expectedId) {
        final OrderBookQuote quote = getQuoteByPosition(side, pos);
        assertId(quote, expectedId);
        assertSize(quote, expectedSize);
        assertPrice(quote, expectedPrice);
    }

    public void assertBookSize(final QuoteSide side, final int expectedSize) {
        final OrderBook<OrderBookQuote> book = getBook();
        final Iterator<OrderBookQuote> itr = book.getMarketSide(side).iterator();
        int actualSize = 0;
        while (itr.hasNext()) {
            itr.next();
            actualSize++;
        }
        Assertions.assertEquals(expectedSize, actualSize, "Number of " + side + "s");
    }

    public void assertExchangeBookSize(final long exchangeId,
                                       final QuoteSide side,
                                       final int expectedSize) {
        final OrderBook<OrderBookQuote> book = getBook();
        final Iterator<OrderBookQuote> itr = book.getExchanges().getById(exchangeId).get().getMarketSide(side).iterator();
        int actualSize = 0;
        while (itr.hasNext()) {
            itr.next();
            actualSize++;
        }
        Assertions.assertEquals(expectedSize, actualSize, "Number of " + side + "s");
    }

    public void assertTotalQuantity(final QuoteSide side,
                                    @Decimal final long expectedTotalQuantity) {

        @Decimal final long totalQuantity = getBook().getMarketSide(side).getTotalQuantity();
        Assertions.assertTrue(Decimal64Utils.isEqual(expectedTotalQuantity, totalQuantity),
                "Invalid total quantity!" +
                        " Expected :" + Decimal64Utils.toString(expectedTotalQuantity) +
                        " Actual :" + Decimal64Utils.toString(totalQuantity));
    }

    public void assertId(final OrderBookQuote quote,
                         final CharSequence expectedId) {
        Assertions.assertTrue(CharSequenceUtils.equals(expectedId, quote.getQuoteId()),
                "Invalid Id!" +
                        " Expected :" + expectedId +
                        " Actual :" + quote.getQuoteId());
    }

    public void assertSize(final OrderBookQuote quote,
                           final long expectedSize) {
        Assertions.assertTrue(Decimal64Utils.isEqual(Decimal64Utils.fromDouble(expectedSize), quote.getSize()),
                "Invalid Size!" +
                        " Expected :" + Decimal64Utils.toString(expectedSize) +
                        " Actual :" + Decimal64Utils.toString(quote.getSize()));
    }

    public void assertPrice(final OrderBookQuote quote,
                            final long expectedPrice) {
        Assertions.assertTrue(Decimal64Utils.isEqual(expectedPrice, quote.getPrice()),
                "Invalid Price!" +
                        " Expected :" + expectedPrice +
                        " Actual :" + quote.getPrice());
    }

    public void assertIteratorBookQuotes(final QuoteSide side,
                                         final int expectedQuoteCounts,
                                         final int bbo,
                                         final int expectedSize) {
        assertIteratorBookQuotes(side, expectedQuoteCounts, bbo, expectedSize, 1);
    }

    // Simulator

    public void assertIteratorBookQuotes(final QuoteSide side,
                                         final int expectedQuoteCounts,
                                         final int bbo,
                                         final int expectedSize,
                                         final int numberOfExchanges) {
        final Iterator<OrderBookQuote> iterator = getBook().getMarketSide(side).iterator();
        int iterations = 0;
        int price = bbo;
        while (iterator.hasNext()) {
            for (int i = 0; i < numberOfExchanges; ++i) {
                final OrderBookQuote quote = iterator.next();
                Assertions.assertTrue(Decimal64Utils.equals(quote.getPrice(), Decimal64Utils.fromInt(price)));
                Assertions.assertTrue(Decimal64Utils.equals(Decimal64Utils.fromInt(expectedSize), quote.getSize()));
                iterations++;
            }
            price += side == QuoteSide.ASK ? 1 : -1;
        }
        Assertions.assertEquals(expectedQuoteCounts, iterations);
    }

    public boolean simulateInsert(final String symbol,
                                  final long exchangeId,
                                  final QuoteSide side,
                                  final CharSequence quoteId,
                                  final InsertType insertType,
                                  final CharSequence insertBeforeQuoteId,
                                  final double price,
                                  final double size) {
        return L3EntryNewBuilder.simulateL3EntryNew(
                L3EntryNewBuilder.builder()
                        .setSide(side)
                        .setPrice(Decimal64Utils.fromDouble(price))
                        .setSize(Decimal64Utils.fromDouble(size))
                        .setExchangeId(exchangeId)
                        .setQuoteId(quoteId)
                        .setInsertType(insertType)
                        .setInsertBeforeQuoteId(insertBeforeQuoteId) // won't matter unless insertType is ADD_BEFORE
                        .build(),
                symbol, getBook());
    }

    public void simulateCancelAllQuoteBySide(final QuoteSide side) {
        final MarketSide<OrderBookQuote> marketSide = getBook().getMarketSide(side);
        while (marketSide.getBestQuote() != null) {
            final OrderBookQuote deleteQuote = getBook().getMarketSide(side).getBestQuote();
            simulateCancel(DEFAULT_EXCHANGE_ID, deleteQuote.getQuoteId());
        }
    }

    public void simulateCancel(final long exchangeId, // no need to provide other fields, and they won't be checked
                               final CharSequence quoteId) {
        simulateBookAction(exchangeId, QuoteUpdateAction.CANCEL, quoteId, QuoteSide.ASK, 1, 1);
    }

    public void simulateModify(final long exchangeId,
                               final CharSequence quoteId,
                               final QuoteSide side,
                               @Decimal final long size,
                               @Decimal final long price) {
        simulateBookAction(exchangeId, QuoteUpdateAction.MODIFY, quoteId, side, price, size);
    }

    public void simulateReplace(final long exchangeId,
                                final CharSequence quoteId,
                                final QuoteSide side,
                                @Decimal final long price,
                                @Decimal final long size) {
        simulateBookAction(exchangeId, QuoteUpdateAction.REPLACE, quoteId, side, price, size);
    }

    public boolean simulateBookAction(final long exchangeId,
                                      final QuoteUpdateAction action,
                                      final CharSequence quoteId,
                                      final QuoteSide side,
                                      final long price,
                                      final long size) {
        return L3EntryUpdateBuilder.simulateL3EntryUpdate(
                L3EntryUpdateBuilder.builder()
                        .setSide(side)
                        .setPrice(Decimal64Utils.fromDouble(price))
                        .setSize(Decimal64Utils.fromDouble(size))
                        .setAction(action)
                        .setExchangeId(exchangeId)
                        .setQuoteId(quoteId)
                        .build(),
                DEFAULT_SYMBOL,
                getBook());
    }

    public PackageHeader createBookResetEntry(final PackageType packageType, final long exchangeId) {
        final PackageHeader packageHeader = new PackageHeader();
        final ObjectArrayList<BaseEntryInfo> baseEntryInfo = new ObjectArrayList<>();

        final BookResetEntry resetEntry = new BookResetEntry();
        resetEntry.setExchangeId(exchangeId);
        resetEntry.setModelType(getBook().getQuoteLevels());
        baseEntryInfo.add(resetEntry);

        packageHeader.setEntries(baseEntryInfo);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        return packageHeader;
    }

    public SecurityFeedStatusMessage createSecurityFeedStatus(final long exchangeId, final FeedStatus status) {
        final SecurityFeedStatusMessage message = new SecurityFeedStatusMessage();
        message.setSymbol(DEFAULT_SYMBOL);
        message.setExchangeId(exchangeId);
        message.setStatus(status);
        return message;
    }

    public boolean simulateResetEntry(final long exchangeId, final PackageType packageType) {
        return getBook().update(createBookResetEntry(packageType, exchangeId));
    }

    //  Book Helper

    public boolean simulateSecurityFeedStatus(final long exchangeId, final FeedStatus status) {
//        return getBook().update(createSecurityFeedStatus(exchangeId, status));
        return true;
    }

    public OrderBookQuote getQuoteByPosition(final QuoteSide side,
                                             final int pos) {
        final OrderBook<OrderBookQuote> book = getBook();
        int i = 0;
        for (final OrderBookQuote quote : book.getMarketSide(side)) {
            if (i++ == pos) {
                return quote;
            }
        }
        throw new AssertionFailedError();
    }

    public short getQuotePositionById(final QuoteSide side,
                                      final CharSequence quoteId) {
        final OrderBook<OrderBookQuote> book = getBook();
        short i = 0;
        for (final OrderBookQuote quote : book.getMarketSide(side)) {
            if (CharSequenceUtils.equals(quote.getQuoteId(), quoteId)) {
                return i;
            }
            i++;
        }
        throw new AssertionFailedError();
    }

    public boolean simulateQuoteSnapshot(final PackageType packageType,
                                         final long exchangeId,
                                         final int depth,
                                         final double bbo,
                                         final long size) {

        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setOriginalTimestamp(System.currentTimeMillis());
        packageHeader.setTimeStampMs(System.currentTimeMillis());

        final ObjectArrayList<BaseEntryInfo> baseEntryInfo = new ObjectArrayList<>();

        packageHeader.setEntries(baseEntryInfo);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        for (int j = 0; j < depth; ++j) {
            final L3EntryNew entryNew = new L3EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(bbo + j));
            entryNew.setSize(Decimal64Utils.isNormal(size) ? size : Decimal64Utils.fromLong(size));
            entryNew.setSide(QuoteSide.ASK);
            entryNew.setExchangeId(exchangeId);
            entryNew.setQuoteId("id" + j);
            entryNew.setInsertType(InsertType.ADD_BACK);
            baseEntryInfo.add(entryNew);
        }

        for (int j = 0; j < depth; ++j) {
            final L3EntryNew entryNew = new L3EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(bbo - j));
            entryNew.setSize(Decimal64Utils.isNormal(size) ? size : Decimal64Utils.fromLong(size));
            entryNew.setSide(QuoteSide.BID);
            entryNew.setExchangeId(exchangeId);
            entryNew.setQuoteId("id" + depth + j);
            entryNew.setInsertType(InsertType.ADD_BACK);
            baseEntryInfo.add(entryNew);
        }
        return getBook().update(packageHeader);
    }

    public PackageHeader simulateQuoteSnapshotBySide(final PackageType packageType,
                                                     final long exchangeId,
                                                     final int orderBookDepth,
                                                     final QuoteSide side,
                                                     final long bestBidAndAsk,
                                                     final long size) {

        final PackageHeader packageHeader = new PackageHeader();
        final ObjectArrayList<BaseEntryInfo> baseEntryInfo = new ObjectArrayList<>();

        packageHeader.setEntries(baseEntryInfo);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        for (int j = 0; j < orderBookDepth; ++j) {
            final L3EntryNew entryNew = new L3EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(bestBidAndAsk + (side == QuoteSide.ASK ? j : -j)));
            entryNew.setSize(Decimal64Utils.fromDouble(size));
            entryNew.setSide(side);
            entryNew.setExchangeId(exchangeId);
            entryNew.setQuoteId("id" + j);
            entryNew.setInsertType(InsertType.ADD_BACK);
            baseEntryInfo.add(entryNew);
        }

        getBook().update(packageHeader);
        return packageHeader;
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void shouldStoreQuoteTimestamp(final int maxDepth,
                                          final CharSequence quoteId,
                                          final QuoteSide side,
                                          final long price,
                                          final long size,
                                          final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .shouldStoreQuoteTimestamps(true)
                .build();
        createBook(opt);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        getBook().getMarketSide(side)
                .forEach(q -> {
                    Assertions.assertTrue(q.hasOriginalTimestamp());
                    Assertions.assertNotEquals(OrderBookQuote.TIMESTAMP_UNKNOWN, q.getTimestamp());
                });
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void shouldNotStoreQuoteTimestamp(final int maxDepth,
                                             final CharSequence quoteId,
                                             final QuoteSide side,
                                             final long price,
                                             final long size,
                                             final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .build();
        createBook(opt);
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        getBook().getMarketSide(side)
                .forEach(q -> {
                    Assertions.assertFalse(q.hasTimestamp());
                    Assertions.assertEquals(OrderBookQuote.TIMESTAMP_UNKNOWN, q.getTimestamp());
                });
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_Quote_invalidSymbol(final int maxDepth,
                                                             final CharSequence quoteId,
                                                             final QuoteSide side,
                                                             final long price,
                                                             final long size,
                                                             final long bbo) {
        Assertions.assertFalse(simulateInsert(LTC_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size));
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        Assertions.assertFalse(simulateInsert(LTC_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should not cancel quote, because of different exchange")
    public void incrementalUpdate_invalidCancel_L3Quote(final int maxDepth,
                                                        final CharSequence quoteId,
                                                        final QuoteSide side,
                                                        final long price,
                                                        final long size,
                                                        final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateCancel(BINANCE, quoteId);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Should cancel quote")
    public void incrementalUpdate_Cancel_L3Quote(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, maxDepth - 1);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Trying to update the book waiting for snapshot")
    public void incrementalUpdate_waitingSnapshot_Insert_L3Quote(final int maxDepth,
                                                                 final CharSequence quoteId,
                                                                 final QuoteSide side,
                                                                 final long price,
                                                                 final long size,
                                                                 final long bbo) {
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size);
        assertBookSize(side, 0);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    @DisplayName("Trying to update the book not waiting for snapshot")
    public void incrementalUpdate_Insert_L3Quote(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .updateMode(UpdateMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);
        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, quoteId, price, size);
        assertBookSize(side, 1);
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
        simulateModify(COINBASE, quoteId, side, size - 1, price);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void bbo_L3Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.BID).getWorstQuote());

        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getBestQuote());
        Assertions.assertNull(getBook().getMarketSide(QuoteSide.ASK).getWorstQuote());

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);

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
    public void snapshot_clear(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);

        getBook().clear();
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void snapshot_totalQuantity_L3Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        @Decimal final long expectedTotalQuantity = Decimal64Utils.fromInt(size * maxDepth);

        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertTotalQuantity(QuoteSide.BID, expectedTotalQuantity);
        assertTotalQuantity(QuoteSide.ASK, expectedTotalQuantity);
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT", "PERIODICAL_SNAPSHOT"})
    public void incrementalUpdate_addAboveSnapshotDepth_L3Quote(final PackageType packageType) {
        final int maxDepth = 10;
        final int bbo = 250;
        final int size = 5;

        int id = maxDepth;
        for (final QuoteSide side : new QuoteSide[]{QuoteSide.ASK, QuoteSide.BID}) {
            simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
            for (int i = 0; i < maxDepth; i++) {
                simulateInsert(DEFAULT_SYMBOL,
                        COINBASE,
                        side,
                        "id" + id++,
                        InsertType.ADD_BACK,
                        null,
                        bbo + maxDepth + i,
                        size);
                assertBookSize(side, maxDepth + i + 1);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementUpdate_Cancel_Insert_L3Quote(final int maxDepth,
                                                      final CharSequence quoteId,
                                                      final QuoteSide side,
                                                      final long price,
                                                      final long size,
                                                      final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
        simulateCancel(COINBASE, quoteId);
        assertBookSize(side, maxDepth - 1);
        simulateInsert(DEFAULT_SYMBOL,
                COINBASE,
                side,
                quoteId,
                InsertType.ADD_BACK,
                null,
                bbo + (side == QuoteSide.ASK ? maxDepth : -maxDepth),
                size);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("sideProvider")
    public void snapshot_Quote_snapshotAfterDelete(final QuoteSide side) {
        final int maxDepth = 10;
        final int bbo = 25;
        final int size = 5;

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        simulateCancel(COINBASE, "id6");
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
    }

    @Test
    public void book_isEmpty() {
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);
        Assertions.assertTrue(getBook().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"VENDOR_SNAPSHOT"}
    )
    public void snapshot_byOneSide(final PackageType packageType) {
        final int maxDepth = 4;
        final int bbo = 25;
        final int size = 5;

//      Filling two side 4:4
        simulateQuoteSnapshot(packageType, COINBASE, maxDepth, bbo, size);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, maxDepth);
//      Cleaning one side 0:4
        simulateCancelAllQuoteBySide(QuoteSide.BID);
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, maxDepth);
//      Filling opposite side 4:0
        simulateQuoteSnapshotBySide(packageType, COINBASE, maxDepth, QuoteSide.BID, bbo, size);
        assertBookSize(QuoteSide.BID, maxDepth);
        assertBookSize(QuoteSide.ASK, 0);
//      Filling two side 2:2
        simulateQuoteSnapshot(packageType, COINBASE, 2, bbo, size);
        assertBookSize(QuoteSide.BID, 2);
        assertBookSize(QuoteSide.ASK, 2);
//      Filling two side 4:4
        simulateQuoteSnapshot(packageType, COINBASE, 4, bbo, size);
        assertBookSize(QuoteSide.BID, 4);
        assertBookSize(QuoteSide.ASK, 4);
    }

//    @ParameterizedTest
//    @MethodSource("quoteProvider")
//    @Skip
//    public void securityStatusMessage_Quote(final int maxDepth,
//                                            final CharSequence quoteId,
//                                            final QuoteSide side,
//                                            final long price,
//                                            final long size,
//                                            final long bbo) {
//        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
//
//        simulateSecurityFeedStatus(COINBASE, FeedStatus.NOT_AVAILABLE);
//        assertBookSize(side, 0); // make sure book is clean
//
//        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, null , price, size);
//        assertBookSize(side, 0); // make sure insert is ignored (we are waiting for snapshot)
//
//        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
//        assertBookSize(side, maxDepth);
//    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_Quote_WaitingSnapshot(final int maxDepth,
                                                 final CharSequence quoteId,
                                                 final QuoteSide side,
                                                 final long price,
                                                 final long size,
                                                 final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertBookSize(side, 0); // make sure book is clean

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, 0); // make sure insert is ignored (we are waiting for snapshot)

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_Quote_PeriodicalSnapshotMode_ONLY_ONE(final int maxDepth,
                                                                 final CharSequence quoteId,
                                                                 final QuoteSide side,
                                                                 final long price,
                                                                 final long size,
                                                                 final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.ONLY_ONE)
                .build();
        createBook(opt);

        simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        assertBookSize(side, 0); // make sure book is clean

        simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, maxDepth + 1); // make sure insert is processed

        Assertions.assertFalse(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth + 1); // make sure book is ignore periodical snapshot after insert

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        Assertions.assertFalse(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_Quote_PeriodicalSnapshotMode_SKIP_ALL(final int maxDepth,
                                                                 final CharSequence quoteId,
                                                                 final QuoteSide side,
                                                                 final long price,
                                                                 final long size,
                                                                 final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.SKIP_ALL)
                .build();
        createBook(opt);

        Assertions.assertFalse(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        Assertions.assertTrue(simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);

        Assertions.assertFalse(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertFalse(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, 0); // make sure book is clean
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_Quote_PeriodicalSnapshotMode_PROCESS_ALL(final int maxDepth,
                                                                    final CharSequence quoteId,
                                                                    final QuoteSide side,
                                                                    final long price,
                                                                    final long size,
                                                                    final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .periodicalSnapshotMode(PeriodicalSnapshotMode.PROCESS_ALL)
                .build();
        createBook(opt);

        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);
        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);

        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);
        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);

        simulateResetEntry(COINBASE, PERIODICAL_SNAPSHOT);
        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);
        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, maxDepth, bbo, size));
        assertBookSize(side, maxDepth);
    }

    @Test
    public void resetEntry() {
        simulateResetEntry(DEFAULT_EXCHANGE_ID, VENDOR_SNAPSHOT);
        assertBookSize(QuoteSide.BID, 0);
        assertBookSize(QuoteSide.ASK, 0);
        Assertions.assertTrue(getBook().isEmpty());
    }

    @Test
    public void resetEntry_Invalid() {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);
        Assertions.assertTrue(simulateQuoteSnapshot(PERIODICAL_SNAPSHOT, COINBASE, 25, 25, 25));
        Assertions.assertFalse(simulateResetEntry(TypeConstants.EXCHANGE_NULL, VENDOR_SNAPSHOT));
        Assertions.assertFalse(getBook().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void resetEntry_Quote_NoWaitingSnapshot(final int maxDepth,
                                                   final CharSequence quoteId,
                                                   final QuoteSide side,
                                                   final long price,
                                                   final long size,
                                                   final long bbo) {
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .resetMode(ResetMode.NON_WAITING_FOR_SNAPSHOT)
                .build();
        createBook(opt);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertBookSize(side, 0); // make sure book is clean

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, 1); // make sure insert is not ignored

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);

        createBook(opt);

        simulateResetEntry(COINBASE, VENDOR_SNAPSHOT);
        assertBookSize(side, 0); // make sure book is clean

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, quoteId, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, 1); // make sure insert is not ignored

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);
        assertBookSize(side, maxDepth);
    }

    @ParameterizedTest
    @MethodSource("quoteProvider")
    public void incrementalUpdate_Insert_Duplicate_Quote(final int maxDepth,
                                                         final CharSequence quoteId,
                                                         final QuoteSide side,
                                                         final long price,
                                                         final long size,
                                                         final long bbo) {
        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, maxDepth + 1);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, 0); // clears on invalidUpdate

        final ValidationOptions validationOptions = ValidationOptions.builder()
                .skipInvalidQuoteInsert()
                .build();
        final OrderBookOptions opt = new OrderBookOptionsBuilder()
                .validationOptions(validationOptions)
                .build();
        createBook(opt);

        simulateQuoteSnapshot(VENDOR_SNAPSHOT, COINBASE, maxDepth, bbo, size);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, maxDepth + 1);

        simulateInsert(DEFAULT_SYMBOL, COINBASE, side, "id" + 2 * maxDepth, InsertType.ADD_BACK, null, price, size);
        assertBookSize(side, maxDepth + 1); // skips invalidUpdate
    }

}

