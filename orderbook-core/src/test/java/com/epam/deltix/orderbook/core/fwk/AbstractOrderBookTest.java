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
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.Exchange;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.options.Option;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import java.util.Iterator;

/**
 * BookSimulator is a collection of utility methods that support asserting
 * conditions in tests and simulate book actions.
 *
 * @author Andrii_Ostapenko1
 */
public abstract class AbstractOrderBookTest {

    public static final long COINBASE = AlphanumericUtils.toAlphanumericUInt64("COINBASE");
    public static final long BINANCE = AlphanumericUtils.toAlphanumericUInt64("BINANCE");

    public static final short BEST_LEVEL = 0;

    public static final String DEFAULT_SYMBOL = "BTC";
    public static final String LTC_SYMBOL = "LTC";
    public static final long DEFAULT_EXCHANGE_ID = COINBASE;

    public abstract OrderBook<OrderBookQuote> getBook();

    public abstract void createBook(OrderBookOptions otherOpt);

    @Test
    public void symbol_NotEmpty() {
        Assertions.assertTrue(getBook().getSymbol().hasValue());
        Assertions.assertEquals(DEFAULT_SYMBOL, getBook().getSymbol().get());
    }

    @ParameterizedTest
    @EnumSource(value = QuoteSide.class)
    public void marketSide_getTotalQuantity_zero(final QuoteSide side) {
        assertTotalQuantity(side, Decimal64Utils.ZERO);
    }

    // Assertion

    public void assertionBookSideSize(final int expectedSize,
                                      final QuoteSide side,
                                      final OrderBook<OrderBookQuote> book) {
        final Iterator<OrderBookQuote> itr = book.getMarketSide(side).iterator();
        int actualSize = 0;
        while (itr.hasNext()) {
            itr.next();
            actualSize++;
        }
        Assertions.assertEquals(expectedSize, actualSize, "Number of " + side + "s");
    }

    public void assertEqualLevel(final QuoteSide side,
                                 final short level,
                                 final long expectedPrice,
                                 final long expectedSize,
                                 long expectedNumberOfOrders) {
        assertPrice(side, level, expectedPrice);
        assertSize(side, level, expectedSize);
        assertNumberOfOrders(side, level, expectedNumberOfOrders);
    }

    public void assertNotEqualLevel(final QuoteSide side,
                                    final short level,
                                    @Decimal final long expectedPrice,
                                    @Decimal final long expectedSize,
                                    long expectedNumOfOrders) {
        assertNotEqualPrice(side, level, expectedPrice);
        assertNotEqualSize(side, level, expectedSize);
        assertNotEqualNumberOfOrders(side, level, expectedNumOfOrders);
    }

    public void assertSize(final QuoteSide side,
                           final short priceLevel,
                           final long expectedSize) {
        final OrderBookQuote quote = getQuoteByLevel(side, priceLevel, getBook());
        Assertions.assertTrue(Decimal64Utils.isEqual(Decimal64Utils.fromDouble(expectedSize), quote.getSize()),
                "Invalid Size!" +
                        " Expected :" + Decimal64Utils.toString(expectedSize) +
                        " Actual :" + Decimal64Utils.toString(quote.getSize()));
    }

    public void assertTotalQuantity(final QuoteSide side,
                                    @Decimal final long expectedTotalQuantity) {

        @Decimal final long totalQuantity = getBook().getMarketSide(side).getTotalQuantity();
        Assertions.assertTrue(Decimal64Utils.isEqual(expectedTotalQuantity, totalQuantity),
                "Invalid total quantity!" +
                        " Expected :" + Decimal64Utils.toString(expectedTotalQuantity) +
                        " Actual :" + Decimal64Utils.toString(totalQuantity));
    }

    public void assertPrice(final QuoteSide side,
                            final int level,
                            final double expectedPrice) {
        final OrderBookQuote quote = getQuoteByLevel(side, level, getBook());
        Assertions.assertTrue(Decimal64Utils.isEqual(Decimal64Utils.fromDouble(expectedPrice), quote.getPrice()),
                "Invalid Price!" +
                        " Expected :" + expectedPrice +
                        " Actual :" + quote.getPrice());
    }

    public void assertNumberOfOrders(final QuoteSide side, final int level, final long expectedNumberOfOrders) {
        final OrderBookQuote quote = getQuoteByLevel(side, level, getBook());
        Assertions.assertEquals(expectedNumberOfOrders, quote.getNumberOfOrders(),
                () -> "Invalid number of orders!" +
                        " Expected :" + expectedNumberOfOrders +
                        " Actual :" + quote.getNumberOfOrders());

    }


    public void assertNotEqualSize(final QuoteSide side,
                                   final short priceLevel,
                                   @Decimal final long expectedSize) {
        final OrderBookQuote quote = getQuoteByLevel(side, priceLevel, getBook());
        Assertions.assertFalse(Decimal64Utils.isEqual(expectedSize, quote.getSize()),
                "Invalid Size!" +
                        " Expected :" + Decimal64Utils.toString(expectedSize) +
                        " Actual :" + Decimal64Utils.toString(quote.getSize()));
    }

    public void assertNotEqualPrice(final QuoteSide side,
                                    final short level,
                                    @Decimal final long expectedPrice) {
        if (getBook().getMarketSide(side).hasLevel(level)) {
            final OrderBookQuote quote = getQuoteByLevel(side, level, getBook());
            Assertions.assertFalse(Decimal64Utils.isEqual(expectedPrice, quote.getPrice()),
                    "Invalid Price!" +
                            " Expected :" + Decimal64Utils.toString(expectedPrice) +
                            " Actual :" + Decimal64Utils.toString(quote.getPrice()));
        }
    }

    public void assertNotEqualNumberOfOrders(final QuoteSide side, final short level, final long expectedNumberOfOrders) {
        final OrderBookQuote quote = getQuoteByLevel(side, level, getBook());
        Assertions.assertNotEquals(expectedNumberOfOrders, quote.getNumberOfOrders(),
                () -> "Invalid number of orders!" +
                        " Expected :" + expectedNumberOfOrders +
                        " Actual :" + quote.getNumberOfOrders());

    }


    public void assertBookSize(final QuoteSide side, final int count) {
        assertionBookSideSize(count, side, getBook());
    }

    public void assertBookSizeBySides(final int count) {
        assertionBookSideSize(count, QuoteSide.ASK, getBook());
        assertionBookSideSize(count, QuoteSide.BID, getBook());
    }

    public void assertExchangeBookSizeIsEmpty(final long exchangeId, final QuoteSide side) {
        assertExchangeBookSize(exchangeId, side, 0);
    }

    public void assertExchangeBookSizeIsEmpty(final QuoteSide side) {
        assertExchangeBookSize(DEFAULT_EXCHANGE_ID, side, 0);
    }

    public void assertExchangeBookSize(final long exchangeId, final QuoteSide side, final int expectedSize) {
        final Option<? extends Exchange<OrderBookQuote>> exchange = getBook().getExchanges().getById(exchangeId);
        Assertions.assertTrue(exchange.hasValue());
        Assertions.assertEquals(expectedSize, exchange.get().getMarketSide(side).depth());
        int size = 0;
        final Iterator<OrderBookQuote> iterator = exchange.get().getMarketSide(side).iterator();
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        Assertions.assertEquals(expectedSize, size);
    }

    public void assertIteratorBookQuotes(final int expectedQuoteCounts,
                                         final int bbo,
                                         final int expectedSize,
                                         final int expectedNumberOfOrders) {
        assertIteratorBookQuotes(1, expectedQuoteCounts, bbo, expectedSize, expectedNumberOfOrders);
    }

    public void assertIteratorBookQuotes(final int exchangeCount,
                                         final int expectedQuoteCounts,
                                         final int bbo,
                                         final int expectedSize,
                                         final int expectedNumberOfOrders) {
        QuoteSide side = QuoteSide.ASK;
        Iterator<OrderBookQuote> iterator = getBook().getMarketSide(side).iterator();
        int iterations = 0;
        int price = bbo;
        while (iterator.hasNext()) {
            for (int i = 0; i < exchangeCount; i++) {
                final OrderBookQuote quote = iterator.next();
                Assertions.assertTrue(Decimal64Utils.equals(quote.getPrice(), Decimal64Utils.fromInt(price)));
                Assertions.assertTrue(Decimal64Utils.equals(Decimal64Utils.fromInt(expectedSize), quote.getSize()));
                Assertions.assertTrue(Decimal64Utils.equals(expectedNumberOfOrders, quote.getNumberOfOrders()));
                iterations++;
            }
            price++;
        }

        side = QuoteSide.BID;
        price = bbo;
        iterator = getBook().getMarketSide(side).iterator();
        while (iterator.hasNext()) {
            for (int i = 0; i < exchangeCount; i++) {
                final OrderBookQuote quote = iterator.next();
                Assertions.assertTrue(Decimal64Utils.equals(quote.getPrice(), Decimal64Utils.fromInt(price)));
                Assertions.assertTrue(Decimal64Utils.equals(Decimal64Utils.fromInt(expectedSize), quote.getSize()));
                Assertions.assertTrue(Decimal64Utils.equals(expectedNumberOfOrders, quote.getNumberOfOrders()));
                iterations++;
            }
            price--;
        }
        Assertions.assertEquals(expectedQuoteCounts, iterations);
    }

    // Simulator

    public boolean simulateL1Insert(final QuoteSide side,
                                    final long price,
                                    final long size,
                                    final long numOfOrders) {
        return simulateL1Insert(DEFAULT_SYMBOL, DEFAULT_EXCHANGE_ID, side, price, size, numOfOrders);
    }

    public boolean simulateL1Insert(final String symbol,
                                    final long exchangeId,
                                    final QuoteSide side,
                                    final long price,
                                    final long size,
                                    final long numOfOrders) {
        return L1EntryNewBuilder.simulateL1EntryNew(
                L1EntryNewBuilder.builder()
                        .setSide(side)
                        .setPrice(Decimal64Utils.fromDouble(price))
                        .setSize(Decimal64Utils.fromDouble(size))
                        .setExchangeId(exchangeId)
                        .setNumberOfOrders(numOfOrders)
                        .build(),
                symbol, getBook());
    }


    public void simulateL2Insert(final QuoteSide side,
                                 final short level,
                                 @Decimal final long price,
                                 @Decimal final long size,
                                 final long numberOfOrders) {
        L2EntryNewBuilder.simulateL2EntryNew(
                L2EntryNewBuilder.builder()
                        .setSide(side)
                        .setPrice(price)
                        .setSize(size)
                        .setNumberOfOrders(numberOfOrders)
                        .setLevel(level)
                        .build(),
                DEFAULT_SYMBOL, getBook());
    }

    public boolean simulateL2Insert(final long exchangeId,
                                    final QuoteSide side,
                                    final int level,
                                    final double price,
                                    final double size,
                                    final long numberOfOrders) {
        return simulateL2Insert(DEFAULT_SYMBOL, exchangeId, side, level, price, size, numberOfOrders);
    }

    public boolean simulateL2Insert(final String symbol,
                                    final long exchangeId,
                                    final QuoteSide side,
                                    final int level,
                                    final double price,
                                    final double size,
                                    final long numberOfOrders) {
        return L2EntryNewBuilder.simulateL2EntryNew(
                L2EntryNewBuilder.builder()
                        .setSide(side)
                        .setPrice(Decimal64Utils.fromDouble(price))
                        .setSize(Decimal64Utils.fromDouble(size))
                        .setNumberOfOrders(numberOfOrders)
                        .setExchangeId(exchangeId)
                        .setLevel((short) level)
                        .build(),
                symbol, getBook());
    }

    public void simulateL2Delete(final QuoteSide side,
                                 final short level,
                                 final long price,
                                 final long size,
                                 final long numberOfOrders) {
        simulateBookAction(DEFAULT_EXCHANGE_ID, BookUpdateAction.DELETE, side, level, price, size, numberOfOrders);
    }

    public void simulateL2DeleteAllQuoteBySide(final QuoteSide side) {
        final MarketSide<OrderBookQuote> marketSide = getBook().getMarketSide(side);
        while (marketSide.getBestQuote() != null) {
            final OrderBookQuote deleteQuote = getBook().getMarketSide(side).getBestQuote();
            simulateL2Delete(side, (short) 0, deleteQuote.getPrice(), deleteQuote.getSize(), deleteQuote.getNumberOfOrders());
        }
    }

    public boolean simulateL2Delete(final long exchangeId,
                                    final QuoteSide side,
                                    final int level,
                                    @Decimal final long price,
                                    @Decimal final long size,
                                    final long numberOfOrders) {
        return simulateBookAction(exchangeId, BookUpdateAction.DELETE, side, level, price, size, numberOfOrders);
    }

    public void simulateL2Delete(final long numberOfDeletedQuotes,
                                 final long exchangeId,
                                 final QuoteSide side,
                                 @Decimal final long price,
                                 @Decimal final long size,
                                 final long numberOfOrders) {
        for (int level = 0; level < numberOfDeletedQuotes; level++) {
            simulateL2Delete(exchangeId, side, (short) level, price, size, numberOfOrders);
        }
    }

    public void simulateL2Update(final QuoteSide side,
                                 final int level,
                                 final long price,
                                 final long size,
                                 final long numberOfOrders) {
        simulateBookAction(DEFAULT_EXCHANGE_ID, BookUpdateAction.UPDATE, side, level, price, size, numberOfOrders);
    }

    public void simulateL2Update(final long exchangeId,
                                 final QuoteSide side,
                                 final int level,
                                 @Decimal final long price,
                                 @Decimal final long size,
                                 final long numberOfOrders) {
        simulateBookAction(exchangeId, BookUpdateAction.UPDATE, side, level, price, size, numberOfOrders);
    }

    public boolean simulateResetEntry(final long exchangeId, final PackageType packageType) {
        return getBook().update(createBookResetEntry(packageType, exchangeId));
    }

    public boolean simulateSecurityFeedStatus(final long exchangeId, final FeedStatus status) {
        return getBook().update(createSecurityFeedStatus(exchangeId, status));
    }


    public boolean simulateBookAction(final long exchangeId,
                                      final BookUpdateAction action,
                                      final QuoteSide side,
                                      final int level,
                                      final long price,
                                      final long size,
                                      final long numberOfOrders) {
        return L2EntryUpdateBuilder.simulateL2EntryUpdate(
                L2EntryUpdateBuilder.builder()
                        .setSide(side)
                        .setPrice(Decimal64Utils.fromDouble(price))
                        .setSize(Decimal64Utils.fromDouble(size))
                        .setNumberOfOrders(numberOfOrders)
                        .setLevel((short) level)
                        .setAction(action)
                        .setExchangeId(exchangeId)
                        .build(),
                DEFAULT_SYMBOL,
                getBook());
    }

    // Book Helper

    public OrderBookQuote getQuoteByLevel(final QuoteSide side,
                                          final int priceLevel,
                                          final OrderBook<OrderBookQuote> book) {
        final Iterator<OrderBookQuote> itr = book.getMarketSide(side).iterator();
        int i = 0;
        while (itr.hasNext()) {
            final OrderBookQuote quote = itr.next();
            if (priceLevel == i) {
                return quote;
            }
            i++;
        }
        throw new AssertionFailedError();
    }

    public boolean simulateL2QuoteSnapshot(final PackageType packageType,
                                           final long exchangeId,
                                           final int depth,
                                           final double bbo,
                                           final long size,
                                           final long numberOfOrders) {
        return simulateL2QuoteSnapshot(packageType, exchangeId, depth, bbo, size, numberOfOrders, true);
    }

    public boolean simulateL2QuoteSnapshot(final PackageType packageType,
                                           final long exchangeId,
                                           final int depth,
                                           final double bbo,
                                           final long size,
                                           final long numberOfOrders,
                                           final boolean addStatistics) {

        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setOriginalTimestamp(System.currentTimeMillis());
        packageHeader.setTimeStampMs(System.currentTimeMillis());

        final ObjectArrayList<BaseEntryInfo> baseEntryInfos = new ObjectArrayList<>();

        packageHeader.setEntries(baseEntryInfos);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        // do it in the middle - kind of messy way to see if this breaks OB processor
        if (addStatistics) {
            final StatisticsEntry openInterest = new StatisticsEntry();
            openInterest.setType(StatisticsType.OPEN_INTEREST);
            openInterest.setValue(Decimal64Utils.parse("12345.678"));
            openInterest.setExchangeId(exchangeId);
            baseEntryInfos.add(openInterest);
        }

        for (int j = 0; j < depth; ++j) {
            final L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(getExpectedQuotePrice(QuoteSide.ASK, bbo, j)));
            entryNew.setSize(Decimal64Utils.isNormal(size) ? size : Decimal64Utils.fromLong(size));
            entryNew.setLevel((short) j);
            entryNew.setSide(QuoteSide.ASK);
            entryNew.setExchangeId(exchangeId);
            entryNew.setNumberOfOrders(numberOfOrders);
            baseEntryInfos.add(entryNew);
        }

        // do it in the middle - kind of messy way to see if this breaks OB processor
        if (addStatistics) {
            final StatisticsEntry tradeVolume = new StatisticsEntry();
            tradeVolume.setType(StatisticsType.TRADE_VOLUME);
            tradeVolume.setValue(Decimal64Utils.parse("12345.678"));
            tradeVolume.setExchangeId(exchangeId);
            baseEntryInfos.add(tradeVolume);
        }

        for (int j = 0; j < depth; ++j) {
            final L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(getExpectedQuotePrice(QuoteSide.BID, bbo, j)));
            entryNew.setSize(Decimal64Utils.isNormal(size) ? size : Decimal64Utils.fromLong(size));
            entryNew.setLevel((short) j);
            entryNew.setSide(QuoteSide.BID);
            entryNew.setExchangeId(exchangeId);
            entryNew.setNumberOfOrders(numberOfOrders);
            baseEntryInfos.add(entryNew);
        }

        // do it at the end - kind of messy way to see if this breaks OB processor
        if (addStatistics) {
            final StatisticsEntry settlementPrice = new StatisticsEntry();
            settlementPrice.setType(StatisticsType.SETTLEMENT_PRICE);
            settlementPrice.setValue(Decimal64Utils.parse("12345.678"));
            settlementPrice.setExchangeId(exchangeId);
            baseEntryInfos.add(settlementPrice);
        }

        return getBook().update(packageHeader);
    }

    protected double getExpectedQuotePrice(final QuoteSide side,
                                           final double bbo,
                                           final int level) {
        if (side == QuoteSide.BID) {
            return bbo - level;
        } else {
            return bbo + level;
        }
    }

    public PackageHeader simulateL2QuoteSnapshotBySide(final PackageType packageType,
                                                       final long exchangeId,
                                                       final int orderBookDepth,
                                                       final QuoteSide side,
                                                       final long bestBidAndAsk,
                                                       final long size,
                                                       final long numberOfOrders,
                                                       final boolean addStatistics) {

        final PackageHeader packageHeader = new PackageHeader();
        final ObjectArrayList<BaseEntryInfo> baseEntryInfos = new ObjectArrayList<>();

        packageHeader.setEntries(baseEntryInfos);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        // do it in the middle - kind of messy way to see if this breaks OB processor
        if (addStatistics) {
            final StatisticsEntry openInterest = new StatisticsEntry();
            openInterest.setType(StatisticsType.OPEN_INTEREST);
            openInterest.setValue(Decimal64Utils.parse("12345.678"));
            openInterest.setExchangeId(exchangeId);
            baseEntryInfos.add(openInterest);
        }

        if (side.equals(QuoteSide.ASK)) {
            for (int j = 0; j < orderBookDepth; ++j) {
                final L2EntryNew entryNew = new L2EntryNew();
                entryNew.setPrice(Decimal64Utils.fromDouble(bestBidAndAsk + j));
                entryNew.setSize(Decimal64Utils.fromDouble(size));
                entryNew.setLevel((short) j);
                entryNew.setSide(QuoteSide.ASK);
                entryNew.setExchangeId(exchangeId);
                entryNew.setNumberOfOrders(numberOfOrders);
                baseEntryInfos.add(entryNew);
            }
        } else if (side.equals(QuoteSide.BID)) {
            for (int j = 0; j < orderBookDepth; ++j) {
                final L2EntryNew entryNew = new L2EntryNew();
                entryNew.setPrice(Decimal64Utils.fromDouble(bestBidAndAsk - j));
                entryNew.setSize(Decimal64Utils.fromDouble(size));
                entryNew.setLevel((short) j);
                entryNew.setSide(QuoteSide.BID);
                entryNew.setExchangeId(exchangeId);
                entryNew.setNumberOfOrders(numberOfOrders);
                baseEntryInfos.add(entryNew);
            }
        }

        if (addStatistics) {
            final StatisticsEntry tradeVolume = new StatisticsEntry();
            tradeVolume.setType(StatisticsType.TRADE_VOLUME);
            tradeVolume.setValue(Decimal64Utils.parse("12345.678"));
            tradeVolume.setExchangeId(exchangeId);
            baseEntryInfos.add(tradeVolume);
        }

        getBook().update(packageHeader);
        return packageHeader;
    }

    public boolean simulateL1QuoteSnapshot(final PackageType packageType,
                                           final long exchangeId,
                                           final long bestBidAndAsk,
                                           final long size,
                                           final long numberOfOrders) {

        final PackageHeader packageHeader = new PackageHeader();
        final long now = System.currentTimeMillis();
        packageHeader.setTimeStampMs(now);
        packageHeader.setOriginalTimestamp(now);

        final ObjectArrayList<BaseEntryInfo> baseEntryInfos = new ObjectArrayList<>();

        packageHeader.setEntries(baseEntryInfos);
        packageHeader.setSymbol(DEFAULT_SYMBOL);
        packageHeader.setPackageType(packageType);

        L1EntryInterface entryNew = new L1Entry();
        entryNew.setPrice(Decimal64Utils.fromDouble(bestBidAndAsk));
        entryNew.setSize(Decimal64Utils.fromDouble(size));

        entryNew.setSide(QuoteSide.ASK);
        entryNew.setExchangeId(exchangeId);
        entryNew.setNumberOfOrders(numberOfOrders);
        baseEntryInfos.add(entryNew);

        entryNew = new L1Entry();
        entryNew.setPrice(Decimal64Utils.fromDouble(bestBidAndAsk));
        entryNew.setSize(Decimal64Utils.fromDouble(size));
        entryNew.setSide(QuoteSide.BID);
        entryNew.setExchangeId(exchangeId);
        entryNew.setNumberOfOrders(numberOfOrders);
        baseEntryInfos.add(entryNew);

        return getBook().update(packageHeader);
    }

    public boolean simulateL1QuoteSnapshot(final PackageType packageType,
                                           final long bestBidAndAsk,
                                           final long size,
                                           final long numberOfOrders) {

        return this.simulateL1QuoteSnapshot(packageType, DEFAULT_EXCHANGE_ID, bestBidAndAsk, size, numberOfOrders);
    }

    public PackageHeader createBookResetEntry(final PackageType packageType, final long exchangeId) {
        final PackageHeader packageHeader = new PackageHeader();
        final ObjectArrayList<BaseEntryInfo> baseEntryInfos = new ObjectArrayList<>();

        final BookResetEntry resetEntry = new BookResetEntry();
        resetEntry.setExchangeId(exchangeId);
        resetEntry.setModelType(getBook().getQuoteLevels());
        baseEntryInfos.add(resetEntry);

        packageHeader.setEntries(baseEntryInfos);
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

}
