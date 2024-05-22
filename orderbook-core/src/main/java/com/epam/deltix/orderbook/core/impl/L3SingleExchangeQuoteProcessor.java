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


import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.orderbook.core.api.EntryValidationCode;
import com.epam.deltix.orderbook.core.api.ErrorListener;
import com.epam.deltix.orderbook.core.options.Defaults;
import com.epam.deltix.orderbook.core.options.DisconnectMode;
import com.epam.deltix.orderbook.core.options.OrderBookOptions;
import com.epam.deltix.orderbook.core.options.ValidationOptions;
import com.epam.deltix.timebase.messages.MarketMessageInfo;
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.ArrayList;

import static com.epam.deltix.timebase.messages.universal.QuoteSide.ASK;
import static com.epam.deltix.timebase.messages.universal.QuoteSide.BID;

/**
 * @author Andrii_Ostapenko1
 */
public class L3SingleExchangeQuoteProcessor<Quote extends MutableOrderBookQuote> implements L3Processor<Quote> {

    protected final ObjectPool<Quote> pool;
    private final L3MarketSide<Quote> bids;
    private final L3MarketSide<Quote> asks;

    private final EventHandler eventHandler;

    @Alphanumeric
    private long exchangeId = TypeConstants.ALPHANUMERIC_NULL;

    private MutableExchangeList<MutableExchange<Quote, L3Processor<Quote>>> exchanges;

    //Parameters
    private final ValidationOptions validationOptions;
    private final DisconnectMode disconnectMode;
    private final ErrorListener errorListener;


    private final ArrayList<Quote> asksList;
    private final ArrayList<Quote> bidsList;

    public L3SingleExchangeQuoteProcessor(final OrderBookOptions options, final ObjectPool<Quote> pool) {
        this.disconnectMode = options.getDisconnectMode().orElse(Defaults.DISCONNECT_MODE);
        this.validationOptions = options.getInvalidQuoteMode().orElse(Defaults.VALIDATION_OPTIONS);
        this.eventHandler = new EventHandlerImpl(options);
        this.errorListener = options.getErrorListener().orElse(Defaults.DEFAULT_ERROR_LISTENER);

        this.pool = pool;

        final int maxDepth = options.getMaxDepth().orElse(Defaults.MAX_DEPTH);
        final int initialDepth = options.getInitialDepth().orElse(Math.min(Defaults.INITIAL_DEPTH, maxDepth));
        this.asks = L3MarketSide.factory(initialDepth, maxDepth, ASK);
        this.bids = L3MarketSide.factory(initialDepth, maxDepth, BID);
        this.asksList = new ArrayList<>(initialDepth);
        this.bidsList = new ArrayList<>(initialDepth);
    }

    @Override
    public String getDescription() {
        return "L3/Single exchange";
    }

    public void failInsert(final PackageHeaderInfo pck, final EntryValidationCode errorCode) {
        if (validationOptions.isQuoteInsert()) {
            clear();
            errorListener.onError(pck, errorCode);
            eventHandler.onBroken();
        }
    }

    @Override
    public Quote processL3EntryNew(final PackageHeaderInfo pck, final L3EntryNewInfo msg) {
        if (!validExchange(pck, msg.getExchangeId())) {
            return null;
        }

        if (isWaitingForSnapshot()) {
            return null;
        }

        final QuoteSide side = msg.getSide();
        final L3MarketSide<Quote> marketSide = getMarketSide(side);
        final EntryValidationCode errorCode = marketSide.isInvalidInsert(msg.getInsertType(), msg.getQuoteId(), msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            failInsert(pck, errorCode);
            return null;
        }

        final Quote quote;
        if (marketSide.isFull()) { // CAREFUL! In this case we can't guarantee uniqueness of quoteIds
            final Quote worstQuote = marketSide.getWorstQuote();
            if (side == ASK && Decimal64Utils.isGreater(worstQuote.getPrice(), msg.getPrice()) ||
                    side == BID && Decimal64Utils.isGreater(msg.getPrice(), worstQuote.getPrice())) {
                quote = marketSide.remove(worstQuote.getQuoteId());
            } else {
                return null;
            }
        } else {
            quote = pool.borrow();
        }

        quote.copyFrom(pck, msg);
        if (!marketSide.add(quote)) {
            pool.release(quote);
            failInsert(pck, EntryValidationCode.DUPLICATE_QUOTE_ID);
            return null;
        }
        return quote;
    }

    public boolean failUpdate(final MarketMessageInfo message, final EntryValidationCode errorCode) {
        if (validationOptions.isQuoteUpdate()) {
            clear();
            errorListener.onError(message, errorCode);
            eventHandler.onBroken();
            return false;
        }
        return true; // skip invalid update
    }

    public boolean handleReplace(final PackageHeaderInfo pck, final L3EntryUpdateInfo msg) {
        final QuoteSide side = msg.getSide();
        final CharSequence quoteId = msg.getQuoteId();
        final L3MarketSide<Quote> newSide = getMarketSide(side);

        final EntryValidationCode errorCode = newSide.isInvalidInsert(InsertType.ADD_BACK, quoteId, msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            return failUpdate(pck, errorCode);
        }

        final Quote quote = newSide.remove(quoteId);
        if (quote != null) { // replace didn't change side
            quote.copyFrom(pck, msg);
            newSide.add(quote);
            return true;
        }

        final L3MarketSide<Quote> prevSide = getMarketSide(side == ASK ? BID : ASK);
        final Quote removed = prevSide.remove(quoteId);
        if (removed != null) { // replace changed side
            Quote newQuote = removed;
            if (newSide.isFull()) {
                pool.release(removed);
                final Quote worstQuote = newSide.getWorstQuote();
                if (side == ASK && Decimal64Utils.isGreater(worstQuote.getPrice(), msg.getPrice()) ||
                        side == BID && Decimal64Utils.isGreater(msg.getPrice(), worstQuote.getPrice())) {
                    newQuote = newSide.remove(worstQuote.getQuoteId());
                } else {
                    return true;
                }
            }
            newQuote.copyFrom(pck, msg);
            newSide.add(newQuote);
            return true;
        }

        return failUpdate(pck, EntryValidationCode.UNKNOWN_QUOTE_ID);
    }

    public boolean handleCancel(final PackageHeaderInfo pck, final L3EntryUpdateInfo msg) {
        final CharSequence quoteId = msg.getQuoteId();
        final QuoteSide side = msg.getSide() == ASK ? ASK : BID;

        Quote removed = getMarketSide(side).remove(quoteId);
        if (removed == null) {
            // setting it as ASK would suffice when side is set correctly or not set at all (null)
            removed = getMarketSide(side == ASK ? BID : ASK).remove(quoteId);
        }

        if (removed == null) {
            return failUpdate(pck, EntryValidationCode.UNKNOWN_QUOTE_ID);
        }
        pool.release(removed);
        return true;
    }

    public boolean handleModify(final PackageHeaderInfo pck, final L3EntryUpdateInfo msg) {
        final QuoteSide side = msg.getSide(); // probably we should validate that side != null immediately?
        final CharSequence quoteId = msg.getQuoteId();
        final L3MarketSide<Quote> marketSide = getMarketSide(side);
        final Quote quote = marketSide.getQuote(quoteId);

        final EntryValidationCode errorCode = marketSide.isInvalidUpdate(quote, msg.getQuoteId(), msg.getPrice(), msg.getSize(), side);
        if (errorCode != null) {
            return failUpdate(pck, errorCode);
        }

        quote.copyFrom(pck, msg);
        return true;
    }

    @Override
    public boolean processL3EntryUpdate(final PackageHeaderInfo pck, final L3EntryUpdateInfo msg) {
        if (!validExchange(pck, msg.getExchangeId())) {
            return false;
        }

        if (isWaitingForSnapshot()) {
            return false;
        }

        final QuoteUpdateAction action = msg.getAction();
        if (action == QuoteUpdateAction.CANCEL) {
            return handleCancel(pck, msg);
        }
        if (action == QuoteUpdateAction.REPLACE) {
            return handleReplace(pck, msg);
        }
        if (action == QuoteUpdateAction.MODIFY) {
            return handleModify(pck, msg);
        }
        return failUpdate(pck, EntryValidationCode.UNSUPPORTED_UPDATE_ACTION);
    }

    @Override
    public boolean processL3Snapshot(final PackageHeaderInfo pck) {
        if (!isSnapshotAllowed(pck)) {
            return false;
        }
        clear();
        final ObjectList<BaseEntryInfo> entries = pck.getEntries();

        asksList.clear();
        bidsList.clear();
        final int len = entries.size();
        for (int i = 0; i < len; i++) {
            final BaseEntryInfo e = entries.get(i);
            if (e instanceof L3EntryNewInterface) {
                final L3EntryNewInterface entry = (L3EntryNewInterface) e;
                if (!validExchange(pck, entry.getExchangeId())) {
                    // We expect that exchangeId is valid and all entries have the same exchangeId
                    continue;
                }

                final QuoteSide side = entry.getSide();
                final L3MarketSide<Quote> marketSide = getMarketSide(side);

                // Both sides have the same max depth
                final int maxDepth = marketSide.getMaxDepth();
                if ((side == ASK && asksList.size() == maxDepth) || (side == BID && bidsList.size() == maxDepth)) {
                    continue;
                }

                // We don't check for duplicate quoteIds, we can do it with hashMap if necessary
                final EntryValidationCode errorCode =
                        marketSide.isInvalidInsert(entry.getInsertType(), entry.getQuoteId(), entry.getPrice(), entry.getSize(), side);
                if (errorCode != null) {
                    if (validationOptions.isQuoteInsert()) {
                        errorListener.onError(pck, errorCode);
                        eventHandler.onBroken();
                    }
                    return false;
                }

                final Quote quote = pool.borrow();
                quote.copyFrom(pck, entry);

                if (side == ASK) {
                    quote.setSequenceNumber(asksList.size());
                    asksList.add(quote);
                } else {
                    quote.setSequenceNumber(bidsList.size());
                    bidsList.add(quote);
                }

                if (asksList.size() == maxDepth && bidsList.size() == maxDepth) {
                    break;
                }
            }
        }
        // Bid and ask entries in each snapshot package should be sorted
        // from best to worst price, same-priced entries should be in FIFO order
        getMarketSide(ASK).buildFromSorted(asksList);
        getMarketSide(BID).buildFromSorted(bidsList);

        eventHandler.onSnapshot();
        return true;
    }

    @Override
    public boolean processBookResetEntry(final PackageHeaderInfo pck, final BookResetEntryInfo msg) {
        if (validExchange(pck, msg.getExchangeId())) {
            clear();
            eventHandler.onReset();
            return true;
        }
        return false;
    }

    @Override
    public boolean processSecurityFeedStatus(final SecurityFeedStatusMessage msg) {
        if (msg.getStatus() == FeedStatus.NOT_AVAILABLE) {
            if (disconnectMode == DisconnectMode.CLEAR_EXCHANGE) {
                if (validExchange(msg, msg.getExchangeId())) {
                    clear();
                    eventHandler.onDisconnect();
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public MutableExchangeList<MutableExchange<Quote, L3Processor<Quote>>> getExchanges() {
        if (exchanges == null && exchangeId != TypeConstants.ALPHANUMERIC_NULL) {
            exchanges = new MutableExchangeListImpl<>();
            exchanges.add(new MutableExchangeImpl<>(exchangeId, this));
        }
        return exchanges;
    }

    @Override
    public L3MarketSide<Quote> getMarketSide(final QuoteSide side) {
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

    private void releaseAndClean(final L3MarketSide<Quote> side) {
        if (side.isEmpty()) {
            return;
        }
        for (final Quote quote : side) {
            pool.release(quote);
        }
        side.clear();
    }

    private boolean validExchange(final MessageInfo pck, @Alphanumeric final long exchangeId) {
        if (TypeConstants.ALPHANUMERIC_NULL == exchangeId) {
            errorListener.onError(pck, EntryValidationCode.MISSING_EXCHANGE_ID);
            return false;
        }

        if (TypeConstants.ALPHANUMERIC_NULL == this.exchangeId) {
            this.exchangeId = exchangeId;
        } else {
            if (this.exchangeId != exchangeId) {
                errorListener.onError(pck, EntryValidationCode.EXCHANGE_ID_MISMATCH);
                return false;
            }
        }
        return true;
    }

}

