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
package com.epam.deltix.timebase.orderbook.impl;

import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.timebase.orderbook.api.MarketSide;
import com.epam.deltix.timebase.orderbook.options.Defaults;
import com.epam.deltix.timebase.orderbook.options.GapMode;
import com.epam.deltix.timebase.orderbook.options.Option;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 * Main class for L2 quote level order book.
 *
 * @author Andrii_Ostapenko1
 */
abstract class AbstractL2MultiExchangeProcessor<Quote extends MutableOrderBookQuote> implements L2Processor<Quote> {

    protected final L2MarketSide<Quote> bids;
    protected final L2MarketSide<Quote> asks;

    protected final MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> exchanges;

    //Parameters
    protected final GapMode gapMode;
    protected final UpdateMode updateMode;
    protected final ObjectPool<Quote> pool;
    protected final short initialDepth;
    protected final int maxDepth;
    /**
     * This parameter using for handle book reset entry.
     *
     * @see QuoteProcessor#isWaitingForSnapshot()
     */
    private boolean isWaitingForSnapshot = false;

    AbstractL2MultiExchangeProcessor(final int initialExchangeCount,
                                     final int initialDepth,
                                     final int maxDepth,
                                     final ObjectPool<Quote> pool,
                                     final GapMode gapMode,
                                     final UpdateMode updateMode) {
        this.initialDepth = (short) initialDepth;
        this.maxDepth = maxDepth;
        this.pool = pool;
        this.gapMode = gapMode;
        this.updateMode = updateMode;
        this.exchanges = new MutableExchangeListImpl<>(initialExchangeCount);
        this.asks = L2MarketSide.factory(initialExchangeCount * initialDepth, Defaults.MAX_DEPTH, QuoteSide.ASK);
        this.bids = L2MarketSide.factory(initialExchangeCount * initialDepth, Defaults.MAX_DEPTH, QuoteSide.BID);
    }

    @Override
    public MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> getExchanges() {
        return exchanges;
    }

    @Override
    public L2MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return side == QuoteSide.BID ? bids : asks;
    }

    @Override
    public boolean isEmpty() {
        return asks.isEmpty() && bids.isEmpty();
    }

    @Override
    public void processBookResetEntry(final BookResetEntryInfo bookResetEntryInfo) {
        clearExchange(bookResetEntryInfo.getExchangeId());
        waitingForSnapshot();
    }

    @Override
    public void processL2VendorSnapshot(final PackageHeaderInfo marketMessageInfo) {
        final ObjectList<BaseEntryInfo> entries = marketMessageInfo.getEntries();
        @Alphanumeric final long exchangeId = entries.get(0).getExchangeId();

        final L2Processor<Quote> exchange = clearExchange(exchangeId);

        exchange.processL2VendorSnapshot(marketMessageInfo);

        insertAll(exchange, QuoteSide.BID);
        insertAll(exchange, QuoteSide.ASK);
        notWaitingForSnapshot();
    }

    @Override
    public Quote processL2EntryNewInfo(final L2EntryNewInfo l2EntryNewInfo) {
        final QuoteSide side = l2EntryNewInfo.getSide();
        final long exchangeId = l2EntryNewInfo.getExchangeId();
        final short level = l2EntryNewInfo.getLevel();
        final L2Processor<Quote> exchange = getOrCreateExchange(exchangeId);

        // Duplicate
        if (exchange.isEmpty()) {
            switch (updateMode) {
                case WAITING_FOR_SNAPSHOT:
                    return null; // Todo ADD null check!!
                case NON_WAITING_FOR_SNAPSHOT:
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported mode: " + updateMode);
            }
        }

        final L2MarketSide<Quote> marketSide = exchange.getMarketSide(side);

        // TODO: 6/30/2022 need to refactor return value
        // Duplicate
        if (marketSide.isGap(level)) {
            switch (gapMode) {
                case FILL_GAP:// We fill gaps at the exchange level.
                    break;
                case SKIP_AND_DROP:
                    clearExchange(exchange);
                    return null;
                case SKIP:
                    return null;
                default:
                    throw new UnsupportedOperationException("Unsupported mode: " + gapMode);
            }
        }

        if (marketSide.isFull()) { //Remove worst quote
            removeQuote(marketSide.getWorstQuote(), side);
        }

        final Quote quote = exchange.processL2EntryNewInfo(l2EntryNewInfo);
        final Quote insertQuote = insertQuote(quote, side);

        return insertQuote;
    }

    @Override
    public void processL2EntryUpdateInfo(final L2EntryUpdateInfo l2EntryUpdateInfo) {
        final long exchangeId = l2EntryUpdateInfo.getExchangeId();
        final L2Processor<Quote> exchange = getOrCreateExchange(exchangeId);

        if (exchange.isEmpty()) {
            return;
        }

        final BookUpdateAction bookUpdateAction = l2EntryUpdateInfo.getAction();

        final short level = l2EntryUpdateInfo.getLevel();
        final QuoteSide side = l2EntryUpdateInfo.getSide();

        final L2MarketSide<Quote> marketSide = exchange.getMarketSide(side);

        // TODO  check if overlay WHY
//        if (depth < currentSize) {
//            final T item = items[depth];
//
//            if ((item != null) && (item.getPrice() != event.getPrice())) {    // check if overlay
//                delete(depth);
//                insert(depth, event);
//
//                break;
//            } else {
//                update(depth, event);
//            }
//        } else {
//            insert(depth, event);
//        }
        if (!marketSide.hasLevel(level)) {
            return; // Stop processing if exchange don't know about quote level
        }

        if (bookUpdateAction == BookUpdateAction.DELETE) {
            final Quote remove = marketSide.getQuote(level);
            removeQuote(remove, side);
        } else if (bookUpdateAction == BookUpdateAction.UPDATE) {
            final Quote quote = marketSide.getQuote(level);
            updateQuote(quote, side, l2EntryUpdateInfo);
        }
        exchange.processL2EntryUpdateInfo(l2EntryUpdateInfo);
    }


    @Override
    public boolean isWaitingForSnapshot() {
        return isWaitingForSnapshot;
    }

    private void waitingForSnapshot() {
        if (!isWaitingForSnapshot()) {
            isWaitingForSnapshot = true;
        }
    }

    private void notWaitingForSnapshot() {
        if (isWaitingForSnapshot()) {
            isWaitingForSnapshot = false;
        }
    }

    abstract void updateQuote(final Quote previous,
                              final QuoteSide side,
                              final L2EntryUpdateInfo update);

    public void insertAll(final L2Processor<Quote> exchange, final QuoteSide side) {
        final L2MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        for (int i = 0; i < marketSide.depth(); i++) {
            final Quote insert = marketSide.getQuote(i);
            insertQuote(insert, side);
        }
    }

    public Quote insertQuote(final Quote insert, final QuoteSide side) {
        return insertQuote(insert, getMarketSide(side));
    }

    abstract Quote insertQuote(final Quote insert, final L2MarketSide<Quote> marketSide);

    public void removeAll(final L2Processor<Quote> exchange, final QuoteSide side) {
        final MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        for (int i = 0; i < marketSide.depth(); i++) {
            final Quote remove = marketSide.getQuote(i);
            removeQuote(remove, getMarketSide(side));
        }
    }

    public void removeQuote(final Quote remove, final QuoteSide side) {
        final L2MarketSide<Quote> marketSide = getMarketSide(side);
        removeQuote(remove, marketSide);
    }

    abstract boolean removeQuote(final Quote remove, final L2MarketSide<Quote> marketSide);

    abstract L2Processor<Quote> clearExchange(final L2Processor<Quote> exchange);

    public L2Processor<Quote> clearExchange(final long exchangeId) {
        final L2Processor<Quote> exchange = getOrCreateExchange(exchangeId);
        return clearExchange(exchange);
    }

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    public L2Processor<Quote> getOrCreateExchange(final long exchangeId) {
        final MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> exchanges = this.getExchanges();
        Option<MutableExchange<Quote, L2Processor<Quote>>> exchangeHolder = exchanges.getById(exchangeId);
        if (!exchangeHolder.hasValue()) {
            final L2SingleExchangeQuoteProcessor<Quote> processor =
                    new L2SingleExchangeQuoteProcessor<>(exchangeId, initialDepth, maxDepth, pool, gapMode, updateMode);
            exchanges.add(new MutableExchangeImpl<>(exchangeId, processor));
            exchangeHolder = exchanges.getById(exchangeId);
        }
        return exchangeHolder.get().getProcessor();
    }
}
