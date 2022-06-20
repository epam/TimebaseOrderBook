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
package com.epam.deltix.timebase.orderbook.fwk;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 * @author Andrii_Ostapenko1
 */
public class L2EntryUpdateBuilder {

    private final L2EntryUpdate entry;

    public L2EntryUpdateBuilder(final L2EntryUpdate entry) {
        this.entry = entry;
    }

    public static L2EntryUpdateBuilder builder() {
        return new L2EntryUpdateBuilder(new L2EntryUpdate());
    }

    public static boolean simulateL2EntryUpdate(final L2EntryUpdate quote,
                                                final String symbol,
                                                final OrderBook<OrderBookQuote> book) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.getEntries().add(quote);
        return book.update(packageHeader);
    }

    public L2EntryUpdateBuilder setLevel(final short level) {
        entry.setLevel(level);
        return this;
    }

    public L2EntryUpdateBuilder setSide(final QuoteSide side) {
        entry.setSide(side);
        return this;

    }

    public L2EntryUpdateBuilder setPrice(@Decimal final long price) {
        entry.setPrice(price);
        return this;
    }

    public L2EntryUpdateBuilder setSize(@Decimal final long size) {
        entry.setSize(size);
        return this;
    }

    public L2EntryUpdateBuilder setExchangeId(final long exchangeId) {
        entry.setExchangeId(exchangeId);
        return this;
    }

    public L2EntryUpdateBuilder setNumberOfOrders(final long numberOfOrders) {
        entry.setNumberOfOrders(numberOfOrders);
        return this;
    }


    public L2EntryUpdateBuilder setAction(final BookUpdateAction action) {
        entry.setAction(action);
        return this;
    }

    public L2EntryUpdate build() {
        return entry;
    }

}
