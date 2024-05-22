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
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.orderbook.core.options.*;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
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

    protected final ObjectPool<Quote> pool;

    protected final MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> exchanges;

    //Parameters
    protected final DisconnectMode disconnectMode;
    protected final ValidationOptions validationOptions;
    private final OrderBookOptions options;

    AbstractL2MultiExchangeProcessor(final OrderBookOptions options, final ObjectPool<Quote> pool) {
        this.options = options;
        this.validationOptions = options.getInvalidQuoteMode().orElse(Defaults.VALIDATION_OPTIONS);
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);

        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int depth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        final int exchanges = options.getInitialExchangesPoolSize().orElse(Defaults.INITIAL_EXCHANGES_POOL_SIZE);
        this.pool = pool;
        this.exchanges = new MutableExchangeListImpl<>(exchanges);
        this.asks = L2MarketSide.factory(exchanges * depth, Defaults.MAX_DEPTH, QuoteSide.ASK);
        this.bids = L2MarketSide.factory(exchanges * depth, Defaults.MAX_DEPTH, QuoteSide.BID);
    }

    @Override
    public boolean isWaitingForSnapshot() {
        if (exchanges.isEmpty()) {
            return true; // No data from exchanges, so we are in "waiting" state
        }

        for (final MutableExchange exchange : exchanges) {
            if (exchange.isWaitingForSnapshot()) {
                return true; // At least one of source exchanges awaits snapshot
            }
        }
        return false;
    }

    @Override
    public boolean isSnapshotAllowed(final PackageHeaderInfo msg) {
        throw new UnsupportedOperationException("Unsupported for multi exchange processor!");
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
    public boolean processSecurityFeedStatus(final SecurityFeedStatusMessage msg) {
        if (msg.getStatus() == FeedStatus.NOT_AVAILABLE) {
            if (disconnectMode == DisconnectMode.CLEAR_EXCHANGE) {
                @Alphanumeric final long exchangeId = msg.getExchangeId();
                final Option<MutableExchange<Quote, L2Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

                if (!holder.hasValue()) {
                    return false;
                }
                final L2Processor<Quote> exchange = holder.get().getProcessor();

                unmapQuote(exchange);
                return exchange.processSecurityFeedStatus(msg);
            }
        }
        return false;
    }

    @Override
    public boolean processBookResetEntry(final PackageHeaderInfo pck, final BookResetEntryInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L2Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

        if (!holder.hasValue()) {
            return false;
        }
        final L2Processor<Quote> exchange = holder.get().getProcessor();

        unmapQuote(exchange);
        return exchange.processBookResetEntry(pck, msg);
    }

    @Override
    public boolean processL2Snapshot(final PackageHeaderInfo msg) {
        final ObjectList<BaseEntryInfo> entries = msg.getEntries();

        // we assume that all entries in the message are from the same exchange
        @Alphanumeric final long exchangeId = entries.get(0).getExchangeId();

        final Option<MutableExchange<Quote, L2Processor<Quote>>> holder = getOrCreateExchange(exchangeId);

        if (!holder.hasValue()) {
            return false;
        }

        final L2Processor<Quote> exchange = holder.get().getProcessor();
        if (exchange.isSnapshotAllowed(msg)) {
            unmapQuote(exchange);
            if (exchange.processL2Snapshot(msg)) {
                mapQuote(exchange, QuoteSide.BID);
                mapQuote(exchange, QuoteSide.ASK);
                return true;
            }
        }
        return false;
    }

    @Override
    public Quote processL2EntryNew(final PackageHeaderInfo pck, final L2EntryNewInfo msg) {
        assert pck.getPackageType() == PackageType.INCREMENTAL_UPDATE;
        final QuoteSide side = msg.getSide();
        final int level = msg.getLevel();
        @Alphanumeric final long exchangeId = msg.getExchangeId();

        final Option<MutableExchange<Quote, L2Processor<Quote>>> holder = getOrCreateExchange(exchangeId);
        // Duplicate
        if (!holder.hasValue() || holder.get().getProcessor().isWaitingForSnapshot()) {
            return null;
        }

        final L2Processor<Quote> exchange = holder.get().getProcessor();

        final L2MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        if (marketSide.isInvalidInsert(level, msg.getPrice(), msg.getSize(), exchangeId)) {
            if (validationOptions.isQuoteInsert()) {
                unmapQuote(exchange);
                exchange.processL2EntryNew(pck, msg);
            }
            return null;
        }

        //Remove worst quote
        //...maybe we should remove
        if (marketSide.isFull()) {
            removeQuote(marketSide.getWorstQuote(), side);
        }

        // We process quote as new by single exchange and then insert it to the aggregated  book
        final Quote quote = exchange.processL2EntryNew(pck, msg);
        if (quote == null) {
            return null;
        }
        final Quote insertQuote = insertQuote(quote, side);
        return insertQuote;
    }

    @Override
    public boolean processL2EntryUpdate(final PackageHeaderInfo pck, final L2EntryUpdateInfo msg) {
        assert pck.getPackageType() == PackageType.INCREMENTAL_UPDATE;
        final int level = msg.getLevel();
        final QuoteSide side = msg.getSide();
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final BookUpdateAction action = msg.getAction();

        final Option<MutableExchange<Quote, L2Processor<Quote>>> exchange = getExchanges().getById(exchangeId);

        if (!exchange.hasValue() || exchange.get().getProcessor().isEmpty() ||
                exchange.get().getProcessor().isWaitingForSnapshot()) {
            return false;
        }

        final L2MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

        if (marketSide.isInvalidUpdate(action, level, msg.getPrice(), msg.getSize(), exchangeId)) {
            if (validationOptions.isQuoteUpdate()) {
                unmapQuote(exchangeId);
                exchange.get().getProcessor().processL2EntryUpdate(pck, msg);
            }
            return false;
        }

        final BookUpdateAction bookUpdateAction = msg.getAction();

        if (bookUpdateAction == BookUpdateAction.DELETE) {
            final Quote quote = marketSide.getQuote(level);
            removeQuote(quote, side);
        } else if (bookUpdateAction == BookUpdateAction.UPDATE) {
            final Quote quote = marketSide.getQuote(level);
            updateQuote(quote, side, msg);
        }
        return exchange.get().getProcessor().processL2EntryUpdate(pck, msg);
    }

    protected abstract void updateQuote(final Quote previous,
                                        final QuoteSide side,
                                        final L2EntryUpdateInfo update);

    private void mapQuote(final L2Processor<Quote> exchange, final QuoteSide side) {
        final L2MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        for (int i = 0; i < marketSide.depth(); i++) {
            final Quote insert = marketSide.getQuote(i);
            insertQuote(insert, side);
        }
    }

    private Quote insertQuote(final Quote insert, final QuoteSide side) {
        return insertQuote(insert, getMarketSide(side));
    }

    protected abstract Quote insertQuote(final Quote insert, final L2MarketSide<Quote> marketSide);

    protected void removeAll(final L2Processor<Quote> exchange, final QuoteSide side) {
        final MarketSide<Quote> marketSide = exchange.getMarketSide(side);
        for (int i = 0; i < marketSide.depth(); i++) {
            final Quote remove = marketSide.getQuote(i);
            removeQuote(remove, getMarketSide(side));
        }
    }

    private void removeQuote(final Quote remove, final QuoteSide side) {
        final L2MarketSide<Quote> marketSide = getMarketSide(side);
        removeQuote(remove, marketSide);
    }

    protected abstract boolean removeQuote(Quote remove, L2MarketSide<Quote> marketSide);

    protected L2Processor<Quote> unmapQuote(final long exchangeId) {
        final L2Processor<Quote> exchange = getOrCreateExchange(exchangeId).get().getProcessor();
        return unmapQuote(exchange);
    }

    protected abstract L2Processor<Quote> unmapQuote(L2Processor<Quote> exchange);

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    private Option<MutableExchange<Quote, L2Processor<Quote>>> getOrCreateExchange(@Alphanumeric final long exchangeId) {
        if (!AlphanumericUtils.isValidAlphanumeric(exchangeId) || TypeConstants.EXCHANGE_NULL == exchangeId) {
            //TODO LOG warning
            return Option.empty();
        }
        final MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> exchanges = this.getExchanges();
        Option<MutableExchange<Quote, L2Processor<Quote>>> holder = exchanges.getById(exchangeId);
        if (!holder.hasValue()) {
            final L2Processor<Quote> processor = new L2SingleExchangeQuoteProcessor<>(options, pool, exchangeId);
            exchanges.add(new MutableExchangeImpl<>(exchangeId, processor));
            holder = exchanges.getById(exchangeId);
        }
        return holder;
    }
}
