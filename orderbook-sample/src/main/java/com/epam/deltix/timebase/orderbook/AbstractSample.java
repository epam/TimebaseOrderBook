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
package com.epam.deltix.timebase.orderbook;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 * @author Andrii_Ostapenko1
 */
public abstract class AbstractSample {

    public static PackageHeader createL2VendorUpdate(final double marketDepth,
                                                     final long exchangeId,
                                                     final CharSequence symbol) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.VENDOR_SNAPSHOT);

        for (int level = 0; level < marketDepth; ++level) {
            final L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(marketDepth + level));
            entryNew.setSize(Decimal64Utils.fromDouble(10));
            entryNew.setLevel((short) level);
            entryNew.setSide(QuoteSide.ASK);
            entryNew.setExchangeId(exchangeId);
            packageHeader.getEntries().add(entryNew);
        }

        for (int level = 0; level < marketDepth; ++level) {
            final L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(marketDepth - level));
            entryNew.setSize(Decimal64Utils.fromDouble(10));
            entryNew.setLevel((short) level);
            entryNew.setSide(QuoteSide.BID);
            entryNew.setExchangeId(exchangeId);
            packageHeader.getEntries().add(entryNew);
        }
        return packageHeader;
    }

    public static PackageHeader createL1VendorUpdate(final double bbo,
                                                     final long exchangeId,
                                                     final CharSequence symbol) {
        final PackageHeader packageHeader = new PackageHeader();
        packageHeader.setEntries(new ObjectArrayList<>());
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.VENDOR_SNAPSHOT);

        L1EntryInterface entryNew = new L1Entry();
        entryNew.setPrice(Decimal64Utils.fromDouble(bbo));
        entryNew.setSize(Decimal64Utils.fromDouble(10));

        entryNew.setSide(QuoteSide.ASK);
        entryNew.setExchangeId(exchangeId);
        packageHeader.getEntries().add(entryNew);

        entryNew = new L1Entry();
        entryNew.setPrice(Decimal64Utils.fromDouble(bbo));
        entryNew.setSize(Decimal64Utils.fromDouble(10));
        entryNew.setSide(QuoteSide.BID);
        entryNew.setExchangeId(exchangeId);
        packageHeader.getEntries().add(entryNew);
        return packageHeader;
    }
}
