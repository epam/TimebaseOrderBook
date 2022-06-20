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

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.timebase.messages.MarketMessageInfo;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.timebase.orderbook.api.Exchange;
import com.epam.deltix.timebase.orderbook.api.ExchangeList;
import com.epam.deltix.timebase.orderbook.api.MarketSide;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.options.Option;
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

    public OrderBookDecorator(final Option<String> symbol,
                              final Processor processor) {
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(processor);
        this.processor = processor;
        this.symbol = symbol.orAnother(Option.empty());
    }

    @Override
    public boolean update(final MarketMessageInfo message) {
        if (Objects.isNull(message)) {
            return false;
        }
        if (symbol.hasValue()) {
            if (!CharSequenceUtils.equals(symbol.get(), message.getSymbol())) {
                return false;
            }
        }
        if (isMarketDatePackage(message)) {
            return updateOrderBook((PackageHeaderInfo) message);
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

    // TODO add package validation!
    private boolean updateOrderBook(final PackageHeaderInfo marketMessageInfo) {
        try {
            if (!marketMessageInfo.hasEntries()) {
                return false;
            } else if (isIncrementalUpdate(marketMessageInfo.getPackageType())) {
                final ObjectList<BaseEntryInfo> entries = marketMessageInfo.getEntries();
                boolean isProcess = true;
                for (int i = 0; i < entries.size(); i++) {
                    final BaseEntryInfo pck = entries.get(i);
                    if (!processor.process(pck)) {
                        isProcess = false;
                    }
                }
                return isProcess;
            } else if (isSnapshot(marketMessageInfo.getPackageType())) {
                return processor.processSnapshot(marketMessageInfo);
            }
        } catch (final Throwable e) {
            throw new Error("Internal Error process entries: " + marketMessageInfo.getEntries()
                    + " Book state: ASK: size: " + getMarketSide(QuoteSide.ASK).depth() + " " + getMarketSide(QuoteSide.ASK) +
                    " BID: size: " + getMarketSide(QuoteSide.BID).depth() + " " + getMarketSide(QuoteSide.BID), e);
        }
        return false;
    }

    private boolean isMarketDatePackage(final MarketMessageInfo marketMessageInfo) {
        return marketMessageInfo instanceof PackageHeaderInfo;
    }

    private boolean isSnapshot(final PackageType packageType) {
        return packageType == PackageType.VENDOR_SNAPSHOT || packageType == PackageType.PERIODICAL_SNAPSHOT;
    }

    private boolean isIncrementalUpdate(final PackageType packageType) {
        return packageType == PackageType.INCREMENTAL_UPDATE;
    }

}
