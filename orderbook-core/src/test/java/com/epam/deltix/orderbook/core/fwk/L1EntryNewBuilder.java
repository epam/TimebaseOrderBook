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
public class L1EntryNewBuilder {


    private final L1EntryInterface entry;

    public L1EntryNewBuilder(final L1EntryInterface entry) {
        this.entry = entry;
    }

    public static boolean simulateL1EntryNew(final L1EntryInterface quote,
                                             final String symbol,
                                             final OrderBook<OrderBookQuote> book) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.getEntries().add(quote);
        return book.update(packageHeader);
    }

    public static L1EntryNewBuilder builder() {
        return new L1EntryNewBuilder(new L1Entry());
    }

    public L1EntryNewBuilder setSide(final QuoteSide side) {
        entry.setSide(side);
        return this;

    }

    public L1EntryNewBuilder setPrice(@Decimal final long price) {
        entry.setPrice(price);
        return this;
    }

    public L1EntryNewBuilder setSize(@Decimal final long size) {
        entry.setSize(size);
        return this;
    }


    public L1EntryNewBuilder setNumberOfOrders(@Decimal final long numberOfOrders) {
        entry.setNumberOfOrders(numberOfOrders);
        return this;
    }

    public L1EntryNewBuilder setExchangeId(final long exchangeId) {
        entry.setExchangeId(exchangeId);
        return this;
    }

    public L1EntryInterface build() {
        return entry;
    }
}
