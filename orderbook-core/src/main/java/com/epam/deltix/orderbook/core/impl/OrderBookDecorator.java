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

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.orderbook.core.api.Exchange;
import com.epam.deltix.orderbook.core.api.ExchangeList;
import com.epam.deltix.orderbook.core.api.MarketSide;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.options.Option;
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.Objects;

/**
 * Base class for {@link OrderBook}
 *
 * @author Andrii_Ostapenko
 */
class OrderBookDecorator<Quote, Processor extends QuoteProcessor<Quote>> implements OrderBook<Quote> {
    private final Processor processor;
    private final Option<String> symbol;

    OrderBookDecorator(final Option<String> symbol,
                       final Processor processor) {
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(processor);
        this.processor = processor;
        this.symbol = symbol.orAnother(Option.empty());
    }

    public static boolean isMarketDatePackage(final MessageInfo msg) {
        return msg instanceof PackageHeaderInfo;
    }

    public static boolean isSecurityFeedStatusMessage(final MessageInfo msg) {
        return msg instanceof SecurityFeedStatusMessage;
    }

    public static boolean isSnapshot(final PackageType type) {
        return type == PackageType.VENDOR_SNAPSHOT || type == PackageType.PERIODICAL_SNAPSHOT;
    }

    public static boolean isIncrementalUpdate(final PackageType type) {
        return type == PackageType.INCREMENTAL_UPDATE;
    }

    @Override
    public boolean update(final MessageInfo msg) {
        if (Objects.isNull(msg)) {
            return false;
        }
        if (symbol.hasValue()) {
            if (!CharSequenceUtils.equals(symbol.get(), msg.getSymbol())) {
                //TODO Add logger
                return false;
            }
        }
        if (isMarketDatePackage(msg)) {
            return updateOrderBook((PackageHeaderInfo) msg);
        }
        if (isSecurityFeedStatusMessage(msg)) {
            return updateOrderBook((SecurityFeedStatusMessage) msg);
        }

        return false;
    }

    @Override
    public String getDescription() {
        return processor.getDescription();
    }

    @Override
    public Option<String> getSymbol() {
        return symbol;
    }

    @Override
    public DataModelType getQuoteLevels() {
        return processor.getQuoteLevels();
    }

    @Override
    public MarketSide<Quote> getMarketSide(final QuoteSide side) {
        return processor.getMarketSide(side);
    }

    @Override
    public ExchangeList<? extends Exchange<Quote>> getExchanges() {
        return processor.getExchanges();
    }

    @Override
    public void clear() {
        this.processor.clear();
    }

    @Override
    public boolean isEmpty() {
        return processor.isEmpty();
    }

    @Override
    public boolean isWaitingForSnapshot() {
        return processor.isWaitingForSnapshot();
    }

    private boolean updateOrderBook(final PackageHeaderInfo msg) {
        try {
            if (!isValid(msg)) {
                // TODO add logger
                return false;
            } else if (isIncrementalUpdate(msg.getPackageType())) {
                final ObjectList<BaseEntryInfo> entries = msg.getEntries();
                boolean isProcess = true;
                for (int i = 0; i < entries.size(); i++) {
                    final BaseEntryInfo pck = entries.get(i);
                    if (!processor.processIncrementalUpdate(msg, pck)) {
                        isProcess = false;
                    }
                }
                return isProcess;
            } else if (isSnapshot(msg.getPackageType())) {
                return processor.processSnapshot(msg);
            }
        } catch (final Throwable e) {
            throw new Error("Error processing market data entries:: " + msg.getEntries() +
                    " Book state: ASK: size: " + getMarketSide(QuoteSide.ASK).depth() +
                    " BID: size: " + getMarketSide(QuoteSide.BID).depth(), e);
        }
        return false;
    }

    /**
     * Simple validation of package header.
     *
     * @param msg - package header
     * @return true if package header is valid
     */
    private boolean isValid(final PackageHeaderInfo msg) {
        return msg.hasPackageType() &&
                msg.hasEntries() &&
                msg.getEntries().size() > 0;
    }

    private boolean updateOrderBook(final SecurityFeedStatusMessage msg) {
        try {
            processor.processSecurityFeedStatus(msg);
        } catch (final Throwable e) {
            throw new Error("Error processing market status", e);
        }
        return false;
    }

}
