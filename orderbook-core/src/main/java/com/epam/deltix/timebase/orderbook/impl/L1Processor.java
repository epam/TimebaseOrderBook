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

import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 * Processor for L1 quote in universal market data format that included in package (Package Header).
 *
 * @author Andrii_Ostapenko
 */
interface L1Processor<Quote> extends QuoteProcessor<Quote>, ResetEntryProcessor {

    @Override
    default DataModelType getQuoteLevels() {
        return DataModelType.LEVEL_ONE;
    }

    @Override
    L1MarketSide<Quote> getMarketSide(final QuoteSide side);

    /**
     * This type reports Level1-new: insert or update one line in Order Book either on ask or bid side.
     *
     * @param l1EntryNewInfo - Level1-new entry to use
     * @return insert quote
     */
    Quote processL1EntryNewInfo(final L1EntryInfo l1EntryNewInfo);

    void processL1VendorSnapshot(final PackageHeaderInfo marketMessageInfo);

    default boolean process(final BaseEntryInfo pck) {
        if (pck instanceof L1EntryInfo) {
            final L1EntryInfo l1EntryNewInfo = (L1EntryInfo) pck;
            processL1EntryNewInfo(l1EntryNewInfo);
            return true;
        }
        return false;
    }

    default boolean processSnapshot(final PackageHeaderInfo marketMessageInfo) {
        final ObjectList<BaseEntryInfo> entries = marketMessageInfo.getEntries();
        final BaseEntryInfo baseEntryInfo = entries.get(0);
        if (baseEntryInfo instanceof L1EntryInfo) {
            processL1VendorSnapshot(marketMessageInfo);
            return true;
        } else if (baseEntryInfo instanceof BookResetEntryInfo) {
            final BookResetEntryInfo resetEntryInfo = (BookResetEntryInfo) baseEntryInfo;
            if (resetEntryInfo.getModelType() == getQuoteLevels()) {
                processBookResetEntry(resetEntryInfo);
                return true;
            }
        }
        return false;
    }

}
