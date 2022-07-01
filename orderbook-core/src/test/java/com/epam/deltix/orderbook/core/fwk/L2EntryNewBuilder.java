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
import com.epam.deltix.timebase.messages.universal.L2EntryNew;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 * @author Andrii_Ostapenko1
 */
public class L2EntryNewBuilder {


    private final L2EntryNew entry;

    public L2EntryNewBuilder(final L2EntryNew entry) {
        this.entry = entry;
    }

    public static boolean simulateL2EntryNew(final L2EntryNew quote,
                                             final String symbol,
                                             final OrderBook<OrderBookQuote> book) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.getEntries().add(quote);
        return book.update(packageHeader);
    }

    public static L2EntryNewBuilder builder() {
        return new L2EntryNewBuilder(new L2EntryNew());
    }


    public L2EntryNewBuilder setLevel(final short level) {
        entry.setLevel(level);
        return this;
    }

    public L2EntryNewBuilder setSide(final QuoteSide side) {
        entry.setSide(side);
        return this;

    }

    public L2EntryNewBuilder setPrice(@Decimal final long price) {
        entry.setPrice(price);
        return this;
    }

    public L2EntryNewBuilder setSize(@Decimal final long size) {
        entry.setSize(size);
        return this;
    }


    public L2EntryNewBuilder setNumberOfOrders(@Decimal final long numberOfOrders) {
        entry.setNumberOfOrders(numberOfOrders);
        return this;
    }

    public L2EntryNewBuilder setExchangeId(final long exchangeId) {
        entry.setExchangeId(exchangeId);
        return this;
    }

    public L2EntryNew build() {
        return entry;
    }
}
