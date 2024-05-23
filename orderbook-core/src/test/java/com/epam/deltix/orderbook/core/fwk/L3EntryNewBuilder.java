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
public class L3EntryNewBuilder {


    private final L3EntryNew entry;

    public L3EntryNewBuilder(final L3EntryNew entry) {
        this.entry = entry;
    }

    public static boolean simulateL3EntryNew(final L3EntryNew quote,
                                             final String symbol,
                                             final OrderBook<OrderBookQuote> book) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.getEntries().add(quote);
        return book.update(packageHeader);
    }

    public static L3EntryNewBuilder builder() {
        return new L3EntryNewBuilder(new L3EntryNew());
    }


    public L3EntryNewBuilder setSide(final QuoteSide side) {
        entry.setSide(side);
        return this;

    }

    public L3EntryNewBuilder setInsertType(final InsertType insertType) {
        entry.setInsertType(insertType);
        return this;
    }

    // won't be used, since insertType is ADD_BACK only
    public L3EntryNewBuilder setInsertBeforeQuoteId(final CharSequence quoteId) {
        entry.setInsertBeforeQuoteId(quoteId);
        return this;
    }

    public L3EntryNewBuilder setPrice(@Decimal final long price) {
        entry.setPrice(price);
        return this;
    }

    public L3EntryNewBuilder setSize(@Decimal final long size) {
        entry.setSize(size);
        return this;
    }

    public L3EntryNewBuilder setQuoteId(final CharSequence quoteId) {
        entry.setQuoteId(quoteId);
        return this;
    }

    public L3EntryNewBuilder setExchangeId(final long exchangeId) {
        entry.setExchangeId(exchangeId);
        return this;
    }

    public L3EntryNew build() {
        return entry;
    }
}
