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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;


/**
 * @author Andrii_Ostapenko1
 */
public class L3EntryUpdateBuilder {

    private final L3EntryUpdate entry;

    public L3EntryUpdateBuilder(final L3EntryUpdate entry) {
        this.entry = entry;
    }

    public static L3EntryUpdateBuilder builder() {
        return new L3EntryUpdateBuilder(new L3EntryUpdate());
    }

    public static boolean simulateL3EntryUpdate(final L3EntryUpdate quote,
                                                final String symbol,
                                                final OrderBook<OrderBookQuote> book) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.getEntries().add(quote);
        return book.update(packageHeader);
    }

    public L3EntryUpdateBuilder setSide(final QuoteSide side) {
        entry.setSide(side);
        return this;
    }

    public L3EntryUpdateBuilder setPrice(@Decimal final long price) {
        entry.setPrice(price);
        return this;
    }

    public L3EntryUpdateBuilder setSize(@Decimal final long size) {
        entry.setSize(size);
        return this;
    }

    public L3EntryUpdateBuilder setExchangeId(final long exchangeId) {
        entry.setExchangeId(exchangeId);
        return this;
    }


    public L3EntryUpdateBuilder setAction(final QuoteUpdateAction action) {
        entry.setAction(action);
        return this;
    }

    public L3EntryUpdateBuilder setQuoteId(final CharSequence quoteId) {
        entry.setQuoteId(quoteId);
        return this;
    }

    public L3EntryUpdate build() {
        return entry;
    }

}
