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


import com.epam.deltix.orderbook.core.api.ExchangeList;
import com.epam.deltix.orderbook.core.options.Defaults;
import com.epam.deltix.orderbook.core.options.DisconnectMode;
import com.epam.deltix.orderbook.core.options.Option;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 * @author Andrii_Ostapenko1
 */
class L1SingleExchangeQuoteProcessor<Quote extends MutableOrderBookQuote> implements L1Processor<Quote> {

    private final ObjectPool<Quote> pool;

    protected final L1MarketSide<Quote> bids;
    protected final L1MarketSide<Quote> asks;
    private final MutableExchangeList<MutableExchange<Quote, L1Processor<Quote>>> exchanges;

    private final EventHandler eventHandler;

    // Parameters
    private final DisconnectMode disconnectMode;

    L1SingleExchangeQuoteProcessor(final OrderBookOptions options,
                                   final ObjectPool<Quote> pool) {
        this.pool = pool;
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);
        this.eventHandler = new EventHandlerImpl(options);

        this.asks = L1MarketSide.factory(QuoteSide.ASK);
        this.bids = L1MarketSide.factory(QuoteSide.BID);
        this.exchanges = new MutableExchangeListImpl<>();
    }

    @Override
    public String getDescription() {
        return "L1/Single exchange";
    }

    @Override
    public L1MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return side == QuoteSide.BID ? bids : asks;
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

    @Override
    public Quote processL1EntryNew(final PackageHeaderInfo pck, final L1EntryInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L1Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
        if (!exchange.hasValue()) {
            // TODO add null check
            return null;
        }

        if (exchange.get().getProcessor().isWaitingForSnapshot()) {
            return null;
        }

        final QuoteSide side = msg.getSide();
        final L1MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

        final Quote quote;
        if (marketSide.isEmpty()) {
            quote = pool.borrow();
            marketSide.insert(quote);
        } else {
            quote = marketSide.getBestQuote();
        }
        quote.copyFrom(pck, msg);
        return quote;
    }

    @Override
    // TODO add validation for exchange id
    public boolean processL1Snapshot(final PackageHeaderInfo pck) {
        if (!eventHandler.isSnapshotAllowed(pck.getPackageType())) {
            return false;
        }

        // We expect that all entries are sorted by exchange id
        final ObjectList<BaseEntryInfo> entries = pck.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            final BaseEntryInfo entryInfo = entries.get(i);
            final L1EntryInfo entry = (L1EntryInfo) entryInfo;
            final QuoteSide side = entry.getSide();
            @Alphanumeric final long exchangeId = entry.getExchangeId();

            final Option<MutableExchange<Quote, L1Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
            if (!exchange.hasValue()) {
                // TODO Log error and throw exception or add package validation
                return false;
            }

            final L1MarketSide<Quote> marketSide = exchange.get().getProcessor().getMarketSide(side);

            final Quote quote;
            if (marketSide.isEmpty()) {
                quote = pool.borrow();
                marketSide.insert(quote);
            } else {
                quote = marketSide.getBestQuote();
            }
            quote.copyFrom(pck, entry);
        }

        eventHandler.onSnapshot();
        return true;
    }

    @Override
    public boolean isWaitingForSnapshot() {
        return eventHandler.isWaitingForSnapshot();
    }

    @Override
    public boolean processBookResetEntry(final PackageHeaderInfo pck, final BookResetEntryInfo msg) {
        @Alphanumeric final long exchangeId = msg.getExchangeId();
        final Option<MutableExchange<Quote, L1Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);

        if (exchange.hasValue()) {
            clear();
            eventHandler.onReset();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean processSecurityFeedStatus(final SecurityFeedStatusMessage msg) {
        if (msg.getStatus() == FeedStatus.NOT_AVAILABLE) {
            if (disconnectMode == DisconnectMode.CLEAR_EXCHANGE) {
                @Alphanumeric final long exchangeId = msg.getExchangeId();
                final Option<MutableExchange<Quote, L1Processor<Quote>>> exchange = getOrCreateExchange(exchangeId);
                if (exchange.hasValue()) {
                    clear();
                    eventHandler.onDisconnect();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ExchangeList<MutableExchange<Quote, L1Processor<Quote>>> getExchanges() {
        return exchanges;
    }

    private void releaseAndClean(final L1MarketSide<Quote> side) {
        for (int i = 0; i < side.depth(); i++) {
            final Quote quote = side.getQuote(i);
            pool.release(quote);
        }
        side.clear();
    }

    /**
     * Get stock exchange holder by id(create new if it does not exist).
     * You can create only one exchange.
     *
     * @param exchangeId - id of exchange.
     * @return exchange book by id.
     */
    private Option<MutableExchange<Quote, L1Processor<Quote>>> getOrCreateExchange(@Alphanumeric final long exchangeId) {
        if (!exchanges.isEmpty()) {
            return exchanges.getById(exchangeId);
        }
        final MutableExchange<Quote, L1Processor<Quote>> exchange = new MutableExchangeImpl<>(exchangeId, this);
        exchanges.add(exchange);
        return exchanges.getById(exchangeId);
    }

}
