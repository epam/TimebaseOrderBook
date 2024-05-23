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
package com.epam.deltix.orderbook.core.impl;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.annotations.Alphanumeric;
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

    private final EventHandler eventHandler;

    //Parameters
    private final ValidationOptions validationOptions;
    private final DisconnectMode disconnectMode;

    public L2SingleExchangeQuoteProcessor(final OrderBookOptions options, final ObjectPool<Quote> pool) {
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);
        this.validationOptions = options.getInvalidQuoteMode().orElse(Defaults.VALIDATION_OPTIONS);
        this.eventHandler = new EventHandlerImpl(options);

        this.pool = pool;
        this.exchanges = new MutableExchangeListImpl<>();

        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int initialDepth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        this.asks = L2MarketSide.factory(initialDepth, maxDepth, ASK);
        this.bids = L2MarketSide.factory(initialDepth, maxDepth, BID);
    }

    public L2SingleExchangeQuoteProcessor(final OrderBookOptions options,
                                          final ObjectPool<Quote> pool,
                                          @Alphanumeric final long exchangeId) {
        this(options, pool);
        getOrCreateExchange(exchangeId);
    }

    @Override
    public String getDescription() {
        return "L2/Single exchange";
    }

    @Override
    public Quote processL2EntryNew(final PackageHeaderInfo pck, final L2EntryNewInfo msg) {
        final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
        if (!exchange.hasValue()) {
            // TODO Log warning!!
            // TODO move to another palace
            return null;
        }

        if (exchange.get().getProcessor().isWaitingForSnapshot()) {
            return null;
        }

        final QuoteSide side = msg.getSide();
        final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);
        final int level = msg.getLevel();

        if (marketSide.isInvalidInsert(level, msg.getPrice(), msg.getSize(), exchangeId)) {
            if (validationOptions.isQuoteInsert()) {
                clear();
                eventHandler.onBroken();
            }
            return null;
        }

        final Quote quote;
        if (level == marketSide.depth()) { // Add new worst quote
            quote = pool.borrow();
        } else if (marketSide.isFull()) { // Check side is Full and remove Worst quote
            quote = marketSide.removeWorstQuote();
        } else {
            quote = pool.borrow();
        }
        quote.copyFrom(pck, msg);
        marketSide.add(level, quote);
        return quote;
    }

    @Override
    public boolean processL2EntryUpdate(final PackageHeaderInfo pck, final L2EntryUpdateInfo msg) {
        final QuoteSide side = msg.getSide();
        final int level = msg.getLevel();
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final BookUpdateAction action = msg.getAction();

        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getExchanges().getById(exchangeId);
        if (!exchange.hasValue()) {
            // TODO move to another palace
            return false;
        }

        if (exchange.get().getProcessor().isWaitingForSnapshot()) {
            return false;
        }

        final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);
        if (marketSide.isInvalidUpdate(action, level, msg.getPrice(), msg.getSize(), exchangeId)) {
            if (validationOptions.isQuoteUpdate()) {
                clear();
                eventHandler.onBroken();
                return false;
            }
            return true; // skip invalid update
        }

        if (action == BookUpdateAction.DELETE) {
            final Quote remove = marketSide.remove(level);
            pool.release(remove);
        } else if (action == BookUpdateAction.UPDATE) {
            final Quote quote = marketSide.getQuote(level);
            quote.copyFrom(pck, msg);
        }
        return true;
    }

    @Override
    public boolean processL2Snapshot(final PackageHeaderInfo pck) {
        if (!isSnapshotAllowed(pck)) {
            return false;
        }

        final ObjectList<BaseEntryInfo> entries = pck.getEntries();

        final int prevAsksDepth = asks.depth();
        final int prevBidsDepth = bids.depth();

        int askCnt = 0;
        int bidCnt = 0;
        for (int i = 0; i < entries.size(); i++) {
            final BaseEntryInfo e = entries.get(i);
            if (e instanceof L2EntryNewInterface) {
                final L2EntryNewInterface entry = (L2EntryNewInterface) e;

//              //We expect that all entries are sorted by side and level
//              if (entry == null || entry.getSide() == null ||
//                      (entry.getSide() == ASK && askCnt != entry.getLevel()) ||
//                      (entry.getSide() == BID && bidCnt != entry.getLevel())) {
//                  clear();
//                  eventHandler.onBroken();
//                  return false;
//              }

                final int level = entry.getLevel();
                final QuoteSide side = entry.getSide();
                @Alphanumeric final long exchangeId = entry.getExchangeId();

                // We expect that exchangeId is valid and all entries have the same exchangeId
                final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
                if (!exchange.hasValue()) {
                    clear();
                    eventHandler.onBroken();
                    return false;
                }

                final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

                // Both side have the same max depth
                final int maxDepth = marketSide.getMaxDepth();
                if ((side == ASK && askCnt == maxDepth) || (side == BID && bidCnt == maxDepth)) {
                    continue;
                }

                if (marketSide.hasLevel(level)) {
                    final Quote quote = marketSide.getQuote(level);
                    quote.copyFrom(pck, entry);
                } else {
                    final Quote quote = pool.borrow();
                    quote.copyFrom(pck, entry);
                    marketSide.add(level, quote);
                }

                if (side == ASK) {
                    askCnt++;
                } else {
                    bidCnt++;
                }

                if (askCnt == maxDepth && bidCnt == maxDepth) {
                    break;
                }
            }
        }

        //Remove all worst quotes after snapshot.
        //We're doing this because we don't release quotes during snapshot processing.
        for (int i = askCnt; i < prevAsksDepth; i++) {
            final Quote quote = asks.removeWorstQuote();
            pool.release(quote);
        }

        for (int i = bidCnt; i < prevBidsDepth; i++) {
            final Quote quote = bids.removeWorstQuote();
            pool.release(quote);
        }

        //Validate state after snapshot
        //We believe that snapshot is valid, but...
        if (!asks.validateState() || !bids.validateState()) {
            clear();
            eventHandler.onBroken();
            return false;
        }

        eventHandler.onSnapshot();
        return true;
    }

    @Override
    public boolean processBookResetEntry(final PackageHeaderInfo pck, final BookResetEntryInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);

        if (exchange.hasValue()) {
            clear();
            eventHandler.onReset();
            return true;
        } else {
            // TODO LOG warning
            return false;
        }
    }

    @Override
    public boolean processSecurityFeedStatus(final SecurityFeedStatusMessage msg) {
        if (msg.getStatus() == FeedStatus.NOT_AVAILABLE) {
            if (disconnectMode == DisconnectMode.CLEAR_EXCHANGE) {
                @Alphanumeric final long exchangeId = msg.getExchangeId();
                final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
                if (exchange.hasValue()) {
                    clear();
                    eventHandler.onDisconnect();
                    return true;
                }
//                else {
//                    //TODO LOG warning
//                }
            }
        }
        return false;
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
        releaseAndClean(asks);
        releaseAndClean(bids);
    }

    @Override
    public boolean isEmpty() {
        return asks.isEmpty() && bids.isEmpty();
    }

    /**
     * Check if snapshot is available for processing.
     *
     * @param msg - snapshot message
     * @return true if snapshot is available for processing
     */
    @Override
    public boolean isSnapshotAllowed(final PackageHeaderInfo msg) {
        final PackageType type = msg.getPackageType();
        return eventHandler.isSnapshotAllowed(type);
    }

    @Override
    public boolean isWaitingForSnapshot() {
        return eventHandler.isWaitingForSnapshot();
    }

    private void releaseAndClean(final L2MarketSide<Quote> side) {
        if (side.isEmpty()) {
            return;
        }
        for (int i = 0; i < side.depth(); i++) {
            final Quote quote = side.getQuote(i);
            pool.release(quote);
        }
        side.clear();
    }

//    private void checkAndFillGap(final L2EntryNewInfo msg) {
//        final int depth = msg.getLevel();
//        final L2MarketSide<Quote> marketSide = getMarketSide(msg.getSide());
//        final int gaps = depth - marketSide.depth();
//
//        // If we have a gap between the last existing level and currently inserted level (empty levels between them),
//        // then let's fill these empty levels with values from the current event.
//        if (gaps > 0) {
//            Quote quote;
//            final int maxDepth = marketSide.getMaxDepth();
//            for (int i = 0; i < gaps && marketSide.depth() < maxDepth; i++) {
//                quote = pool.borrow();
//                quote.copyFrom(pck, msg);
//                marketSide.addWorstQuote(quote);
//            }
//        }
//    }

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     * You can create only one exchange.
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    private Option<MutableExchange<Quote, L2Processor<Quote>>> getOrCreateExchange(@Alphanumeric final long exchangeId) {
        if (!AlphanumericUtils.isValidAlphanumeric(exchangeId) || TypeConstants.EXCHANGE_NULL == exchangeId) {
            //TODO LOG warning
            return Option.empty();
        }

        if (!exchanges.isEmpty()) {
            return exchanges.getById(exchangeId);
        }
        final MutableExchange<Quote, L2Processor<Quote>> exchange = new MutableExchangeImpl<>(exchangeId, this);
        exchanges.add(exchange);
        return exchanges.getById(exchangeId);
    }

}

