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
interface CompactL2Processor<Quote> extends QuoteProcessor<Quote> {

    @Override
    default DataModelType getQuoteLevels() {
        return DataModelType.LEVEL_TWO;
    }

    @Override
    CompactL2MarketSide<Quote> getMarketSide(QuoteSide side);

    @Override
    MutableExchangeList<MutableExchange<Quote, CompactL2Processor<Quote>>> getExchanges();

    /**
     * This type reports incremental Level2-new: insert  one line in Order Book either on ask or bid side.
     *
     * @param pck
     * @param msg - Level2-new entry
     * @return insert quote
     */
    Quote processL2EntryNew(PackageHeaderInfo pck, L2EntryNewInfo msg);

    /**
     * This type reports incremental Level2-update: update or delete one line in Order Book either on ask or bid side.
     *
     * @param pck
     * @param msg - Level2-update
     * @return true if quote was updated, false if quote was not found
     */
    boolean processL2EntryUpdate(PackageHeaderInfo pck, L2EntryUpdateInfo msg);

    boolean processL2Snapshot(PackageHeaderInfo msg);

    boolean isWaitingForSnapshot();

    boolean isSnapshotAllowed(PackageHeaderInfo msg);

    default boolean processIncrementalUpdate(PackageHeaderInfo pck, final BaseEntryInfo entryInfo) {
        if (entryInfo instanceof L2EntryNew) {
            final L2EntryNew entry = (L2EntryNew) entryInfo;
            return processL2EntryNew(pck, entry) != null;
        } else if (entryInfo instanceof L2EntryUpdate) {
            final L2EntryUpdate entry = (L2EntryUpdate) entryInfo;
            return processL2EntryUpdate(pck, entry);
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
            if (entry instanceof L2EntryNewInfo) {
                return processL2Snapshot(msg);
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
