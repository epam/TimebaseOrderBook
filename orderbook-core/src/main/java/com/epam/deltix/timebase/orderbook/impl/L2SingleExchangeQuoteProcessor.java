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
import com.epam.deltix.timebase.orderbook.options.GapMode;
import com.epam.deltix.timebase.orderbook.options.Option;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import com.epam.deltix.util.collections.generated.ObjectList;

import static com.epam.deltix.timebase.messages.universal.QuoteSide.ASK;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.BID;

/**
 * @author Andrii_Ostapenko1
 */
public class L2SingleExchangeQuoteProcessor<Quote extends MutableOrderBookQuote> implements L2Processor<Quote> {

    protected final ObjectPool<Quote> pool;

    protected final L2MarketSide<Quote> bids;
    protected final L2MarketSide<Quote> asks;

    private final MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> exchanges;

    //Parameters
    private final GapMode gapMode;
    private final UpdateMode updateMode;

    /**
     * This parameter using for handle book reset entry.
     *
     * @see QuoteProcessor#isWaitingForSnapshot()
     */
    private boolean isWaitingForSnapshot = false;

    public L2SingleExchangeQuoteProcessor(final int initialDepth,
                                          final int maxDepth,
                                          final ObjectPool<Quote> pool,
                                          final GapMode gapMode,
                                          final UpdateMode updateMode) {
        this.asks = L2MarketSide.factory(initialDepth, maxDepth, ASK);
        this.bids = L2MarketSide.factory(initialDepth, maxDepth, BID);
        this.pool = pool;
        this.gapMode = gapMode;
        this.updateMode = updateMode;
        this.exchanges = new MutableExchangeListImpl<>();
    }

    public L2SingleExchangeQuoteProcessor(final long exchangeId,
                                          final int initialDepth,
                                          final int maxDepth,
                                          final ObjectPool<Quote> pool,
                                          final GapMode gapMode,
                                          final UpdateMode updateMode) {
        this(initialDepth, maxDepth, pool, gapMode, updateMode);
        getOrCreateExchange(exchangeId);
    }

    @Override
    public String getDescription() {
        return "L2/Single exchange";
    }

    @Override
    public Quote processL2EntryNewInfo(final L2EntryNewInfo l2EntryNewInfo) {
        final long exchangeId = l2EntryNewInfo.getExchangeId();
        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
        if (!exchange.hasValue()) {
            // TODO Log warning!!
            return null;// TODO move to another palace
        }

        if (exchange.get().getProcessor().isEmpty()) {
            switch (updateMode) {
                case WAITING_FOR_SNAPSHOT:
                    return null; // Todo ADD null check!!
                case NON_WAITING_FOR_SNAPSHOT:
                    break;
            }
        }

        final QuoteSide side = l2EntryNewInfo.getSide();
        final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);
        final short level = l2EntryNewInfo.getLevel();

        if (marketSide.isGap(level)) {
            switch (gapMode) {
                case FILL_GAP:
                    checkAndFillGap(l2EntryNewInfo);
                    return marketSide.getWorstQuote();
                case SKIP_AND_DROP:
                    clear();
                case SKIP:
                default:
                    return null;
            }
        }

        final Quote quote;
        if (marketSide.isFull()) { // Check side is Full
            quote = marketSide.removeWorstQuote();
            // Attention we remove worst quote bat not remove quote from multi exchange
        } else {
            quote = pool.borrow();
        }
        mapNewL2ToQuote(l2EntryNewInfo, quote);
        marketSide.add(level, quote);

//        System.out.printf("Insert: Quote %s to exchange by level %s \n", quote, level);// Add logger

        return quote;
    }

    @Override
    public void processL2EntryUpdateInfo(final L2EntryUpdateInfo l2EntryUpdateInfo) {
        final long exchangeId = l2EntryUpdateInfo.getExchangeId();
        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
        if (!exchange.hasValue()) {
            return;// TODO move to another palace
        }

        final QuoteSide side = l2EntryUpdateInfo.getSide();
        final short level = l2EntryUpdateInfo.getLevel();

        final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

        if (!marketSide.hasLevel(level)) {
            return;
        }

        final BookUpdateAction bookUpdateAction = l2EntryUpdateInfo.getAction();
        if (bookUpdateAction == BookUpdateAction.DELETE) {
            final Quote remove = marketSide.remove(level);
            pool.release(remove);
//            System.out.printf("Remove: Quote %s from exchange by level %s\n", remove, level); // TODO add logger
        } else if (bookUpdateAction == BookUpdateAction.UPDATE) {
            final Quote quote = marketSide.getQuote(level);
            mapUpdateL2ToQuote(l2EntryUpdateInfo, quote);
//            System.out.printf("Update: Quote %s from exchange by level %s\n", quote, level); // TODO add logger
        }
    }

    @Override
    // TODO add validation for exchange id
    public void processL2VendorSnapshot(final PackageHeaderInfo marketMessageInfo) {
        final ObjectList<BaseEntryInfo> entries = marketMessageInfo.getEntries();
        if (entries.size() < asks.depth() + bids.depth()) {
            clear();
        }

        int askCnt = 0;
        int bidCnt = 0;
        for (int i = 0; i < entries.size(); i++) {
            final BaseEntryInfo pck = entries.get(i);
            final L2EntryNewInfo l2EntryNewInfo = (L2EntryNew) pck;
            final short level = l2EntryNewInfo.getLevel();
            final QuoteSide side = l2EntryNewInfo.getSide();
            final long exchangeId = l2EntryNewInfo.getExchangeId();

            final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
            if (!exchange.hasValue()) {
                // TODO LOG warning
                continue;
            }

            final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

            final short maxDepth = marketSide.getMaxDepth();// Both side have the same max depth
            if ((side == ASK && askCnt == maxDepth) || (side == BID && bidCnt == maxDepth)) {
                continue;
            }

            if (marketSide.hasLevel(level)) {
                final Quote quote = marketSide.getQuote(level);
                mapNewL2ToQuote(l2EntryNewInfo, quote);
            } else {
                final Quote quote = pool.borrow();
                mapNewL2ToQuote(l2EntryNewInfo, quote);
                marketSide.add(level, quote);
            }

            if (side == ASK) {
                askCnt++;
            } else {
                bidCnt++;
            }

            if (askCnt == maxDepth && bidCnt == maxDepth) {
                return;
            }
        }
        asks.trim();
        bids.trim();
        notWaitingForSnapshot();
    }

    @Override
    public void processBookResetEntry(final BookResetEntryInfo bookResetEntryInfo) {
        clear();
        waitingForSnapshot();
    }

    @Override
    public MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> getExchanges() {
        return exchanges;
    }

    @Override
    public L2MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return side == BID ? bids : asks;
    }

    @Override
    public void clear() {
        for (int i = 0; i < asks.depth(); i++) {
            final Quote release = asks.getQuote(i);
            pool.release(release);
        }
        asks.clear();
        for (int i = 0; i < bids.depth(); i++) {
            final Quote release = bids.getQuote(i);
            pool.release(release);
        }
        bids.clear();
    }

    @Override
    public boolean isEmpty() {
        return asks.isEmpty() && bids.isEmpty();
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

    private void checkAndFillGap(final L2EntryNewInfo l2) {
        final short depth = l2.getLevel();
        final L2MarketSide<Quote> marketSide = getMarketSide(l2.getSide());
        final int gaps = depth - marketSide.depth();

        // If we have a gap between the last existing level and currently inserted level (empty levels between them),
        // then let's fill these empty levels with values from the current event.
        if (gaps > 0) {
            Quote quote;
            final short maxDepth = marketSide.getMaxDepth();
            for (int i = 0; i < gaps && marketSide.depth() < maxDepth; i++) {
                quote = pool.borrow();
                mapNewL2ToQuote(l2, quote);
                marketSide.addLast(quote);
            }
            marketSide.trim();
        }
    }

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     * You can create only one exchange.
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    private Option<MutableExchange<Quote, L2Processor<Quote>>> getOrCreateExchange(final long exchangeId) {
        if (!exchanges.isEmpty()) {
            return exchanges.getById(exchangeId);
        }
        final MutableExchangeImpl<Quote, L2Processor<Quote>> exchange = new MutableExchangeImpl<>(exchangeId, this);
        exchanges.add(exchange);
        return exchanges.getById(exchangeId);
    }

    /**
     * Update quote with L2EntryUpdate.
     *
     * @param quote             - order book quote entry
     * @param l2EntryUpdateInfo - L2EntryUpdate
     */
    protected void mapUpdateL2ToQuote(final L2EntryUpdateInfo l2EntryUpdateInfo, final Quote quote) {
        quote.setSize(l2EntryUpdateInfo.getSize());
        quote.setNumberOfOrders(l2EntryUpdateInfo.getNumberOfOrders());
    }

    /**
     * Update quote with L2EntryNew.
     *
     * @param l2EntryNewInfo - L2EntryNew
     * @param quote          - quote
     */
    protected void mapNewL2ToQuote(final L2EntryNewInfo l2EntryNewInfo, final Quote quote) {
        if (quote.getSize() != l2EntryNewInfo.getSize()) {
            quote.setSize(l2EntryNewInfo.getSize());
        }
        if (quote.getPrice() != l2EntryNewInfo.getPrice()) {
            quote.setPrice(l2EntryNewInfo.getPrice());
        }
        if (quote.getExchangeId() != l2EntryNewInfo.getExchangeId()) {
            quote.setExchangeId(l2EntryNewInfo.getExchangeId());
        }
        if (quote.getNumberOfOrders() != l2EntryNewInfo.getNumberOfOrders()) {
            quote.setNumberOfOrders(l2EntryNewInfo.getNumberOfOrders());
        }
    }

}

