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

import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 * Processor for L2 universal market data format that included in package (Package Header).
 *
 * @author Andrii_Ostapenko
 */
interface L2Processor<Quote> extends QuoteProcessor<Quote>, ResetEntryProcessor {

    @Override
    default DataModelType getQuoteLevels() {
        return DataModelType.LEVEL_TWO;
    }

    @Override
    L2MarketSide<Quote> getMarketSide(final QuoteSide side);

    @Override
    MutableExchangeList<MutableExchange<Quote, L2Processor<Quote>>> getExchanges();

    /**
     * This type reports incremental Level2-new: insert  one line in Order Book either on ask or bid side.
     *
     * @param l2EntryNewInfo - Level2-new entry
     * @return insert quote
     */
    Quote processL2EntryNewInfo(final L2EntryNewInfo l2EntryNewInfo);

    /**
     * This type reports incremental Level2-update: update or delete one line in Order Book either on ask or bid side.
     *
     * @param l2EntryUpdateInfo - Level2-update
     */
    void processL2EntryUpdateInfo(final L2EntryUpdateInfo l2EntryUpdateInfo);

    void processL2VendorSnapshot(final PackageHeaderInfo marketMessageInfo);

    default boolean process(final BaseEntryInfo pck) {
        if (pck instanceof L2EntryNew) {
            final L2EntryNew l2EntryNewInfo = (L2EntryNew) pck;
            processL2EntryNewInfo(l2EntryNewInfo);
            return true;
        } else if (pck instanceof L2EntryUpdate) {
            final L2EntryUpdate l2EntryUpdateInfo = (L2EntryUpdate) pck;
            processL2EntryUpdateInfo(l2EntryUpdateInfo);
            return true;
        }
        return false;
    }

    default boolean processSnapshot(final PackageHeaderInfo marketMessageInfo) {
        final ObjectList<BaseEntryInfo> entries = marketMessageInfo.getEntries();
        final BaseEntryInfo baseEntryInfo = entries.get(0);
        if (baseEntryInfo instanceof L2EntryNewInfo) {
            processL2VendorSnapshot(marketMessageInfo);
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
