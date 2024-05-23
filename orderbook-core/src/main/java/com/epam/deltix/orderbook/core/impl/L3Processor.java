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
 * Processor for L3 universal market data format that included in package (Package Header).
 *
 * @author Andrii_Ostapenko
 */
interface L3Processor<Quote> extends QuoteProcessor<Quote> {

    @Override
    default DataModelType getQuoteLevels() {
        return DataModelType.LEVEL_THREE;
    }

    @Override
    L3MarketSide<Quote> getMarketSide(QuoteSide side);

    /**
     * This type reports incremental Level3-new: insert  one line in Order Book either on ask or bid side.
     *
     * @param pck
     * @param msg - Level3-new entry
     * @return insert quote
     */
    Quote processL3EntryNew(PackageHeaderInfo pck, L3EntryNewInfo msg);

    /**
     * This type reports incremental Level3-update: update or delete one line in Order Book either on ask or bid side.
     *
     * @param pck
     * @param msg - Level3-update
     * @return true if quote was updated, false if quote was not found
     */
    boolean processL3EntryUpdate(PackageHeaderInfo pck, L3EntryUpdateInfo msg);

    boolean processL3Snapshot(PackageHeaderInfo msg);

    boolean isSnapshotAllowed(PackageHeaderInfo msg);

    default boolean processIncrementalUpdate(final PackageHeaderInfo pck, final BaseEntryInfo entryInfo) {
        if (entryInfo instanceof L3EntryNew) {
            final L3EntryNew entry = (L3EntryNew) entryInfo;
            return processL3EntryNew(pck, entry) != null;
        } else if (entryInfo instanceof L3EntryUpdate) {
            final L3EntryUpdate entry = (L3EntryUpdate) entryInfo;
            return processL3EntryUpdate(pck, entry);
        } else if (entryInfo instanceof StatisticsEntry) {
            return true;
        }
        return false;
    }

    default boolean processSnapshot(final PackageHeaderInfo msg) {
        final ObjectList<BaseEntryInfo> entries = msg.getEntries();
        final int n = entries.size();
        // skip statistic entries try to establish if we are dealing with order book reset or normal snapshot
        for (int i = 0; i < n; i++) {
            final BaseEntryInfo entry = entries.get(i);
            if (entry instanceof L3EntryNewInfo) {
                return processL3Snapshot(msg);
            } else if (entry instanceof BookResetEntryInfo) {
                final BookResetEntryInfo resetEntry = (BookResetEntryInfo) entry;
                if (resetEntry.getModelType() == getQuoteLevels()) {
                    return processBookResetEntry(msg, resetEntry);
                }
            }
        }
        return false;
    }

}
