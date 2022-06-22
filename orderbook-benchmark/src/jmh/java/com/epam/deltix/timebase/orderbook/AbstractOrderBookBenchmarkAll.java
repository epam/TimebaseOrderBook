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
import org.openjdk.jol.info.GraphLayout;

import java.util.ArrayList;
import java.util.Random;

public abstract class AbstractOrderBookBenchmarkAll {

    final ArrayList<L2EntryUpdate> updateOffers = new ArrayList<>();
    final ArrayList<L2EntryUpdate> updateBids = new ArrayList<>();

    final PackageHeader packageHeader = new PackageHeader();

    final Random random = new Random();

    void initUpdateMarketData(final int maxDepth,
                              final int numberOfExchange) {
        for (int exchangeId = 0; exchangeId < numberOfExchange; exchangeId++) {
            for (int level = 0; level < maxDepth; ++level) {
                L2EntryUpdate entryUpdate = new L2EntryUpdate();
                entryUpdate.setPrice(Decimal64Utils.fromDouble(maxDepth + level));
                entryUpdate.setSize(Decimal64Utils.fromDouble(10));
                entryUpdate.setLevel((short) level);
                entryUpdate.setSide(QuoteSide.ASK);
                entryUpdate.setExchangeId(exchangeId);
                entryUpdate.setAction(BookUpdateAction.UPDATE);
                updateOffers.add(entryUpdate);
            }

            for (int level = 0; level < maxDepth; ++level) {
                L2EntryUpdate entryUpdate = new L2EntryUpdate();
                entryUpdate.setPrice(Decimal64Utils.fromDouble(maxDepth - level));
                entryUpdate.setSize(Decimal64Utils.fromDouble(10));
                entryUpdate.setLevel((short) level);
                entryUpdate.setSide(QuoteSide.BID);
                entryUpdate.setExchangeId(exchangeId);
                entryUpdate.setAction(BookUpdateAction.UPDATE);
                updateBids.add(entryUpdate);
            }
        }
    }

    PackageHeader createVendorUpdate(final int maxDepth,
                                     final int exchangeId,
                                     final CharSequence symbol) {

        if (packageHeader.hasEntries()) {
            packageHeader.getEntries().clear();
        } else {
            packageHeader.setEntries(new ObjectArrayList<>());
        }

        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.VENDOR_SNAPSHOT);

        for (int level = 0; level < maxDepth; ++level) {
            L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(maxDepth + level));
            entryNew.setSize(Decimal64Utils.fromDouble(random.nextInt(1000)));
            entryNew.setLevel((short) level);
            entryNew.setSide(QuoteSide.ASK);
            entryNew.setExchangeId(exchangeId);
            packageHeader.getEntries().add(entryNew);
        }

        for (int level = 0; level < maxDepth; ++level) {
            L2EntryNew entryNew = new L2EntryNew();
            entryNew.setPrice(Decimal64Utils.fromDouble(maxDepth - level));
            entryNew.setSize(Decimal64Utils.fromDouble(random.nextInt(1000)));
            entryNew.setLevel((short) level);
            entryNew.setSide(QuoteSide.BID);
            entryNew.setExchangeId(exchangeId);
            packageHeader.getEntries().add(entryNew);
        }
        return packageHeader;
    }

    void generateUpdateMarketDate(final int maxDepth,
                                  final int numberOfExchange,
                                  final CharSequence symbol) {
        final int level = random.nextInt(maxDepth * numberOfExchange);
        packageHeader.getEntries().clear();
        packageHeader.setSymbol(symbol);
        packageHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        packageHeader.getEntries().add(random.nextInt(2) == 0 ? updateOffers.get(level) : updateBids.get(level));
    }

    void totalSize(Object object) {
        System.out.println(object.getClass().getName() + " " + GraphLayout.parseInstance(object).totalSize() + " byte(s)");
    }

    void footPrint(Object object) {
        System.out.println(object.getClass().getName() + " " + GraphLayout.parseInstance(object).toFootprint());
    }

}
