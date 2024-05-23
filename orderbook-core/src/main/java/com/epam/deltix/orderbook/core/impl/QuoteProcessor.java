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


import com.epam.deltix.orderbook.core.api.OrderBook;
import com.epam.deltix.orderbook.core.options.Option;
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.BaseEntryInfo;
import com.epam.deltix.timebase.messages.universal.BookResetEntryInfo;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;

/**
 * @author Andrii_Ostapenko1
 */
interface QuoteProcessor<Quote> extends OrderBook<Quote> {

    @Override
    default boolean update(final MessageInfo ignore) {
        throw new UnsupportedOperationException("Unsupported for processor: " + getDescription());
    }

    @Override
    default Option<String> getSymbol() {
        throw new UnsupportedOperationException("Unsupported for processor: " + getDescription());
    }

    /**
     * Process incremental update market data entry.
     *
     * @param pck       - package header container
     * @param entryInfo - base entry container
     * @return true if process is success
     */
    boolean processIncrementalUpdate(PackageHeaderInfo pck, BaseEntryInfo entryInfo);

    /**
     * Process only snapshot(VENDOR,PERIODICAL) market data entry.
     *
     * @param marketMessageInfo - Package header container
     * @return true if process is success
     */
    boolean processSnapshot(PackageHeaderInfo marketMessageInfo);

    /**
     * Process security feed status msg.
     * <p>
     * This msg is used to notify you about connecting to the exchange.
     *
     * @param msg - Status feed container
     * @return true if process is success
     * @see SecurityFeedStatusMessage
     */
    boolean processSecurityFeedStatus(SecurityFeedStatusMessage msg);

    /**
     * Book Reset is a Special type of entry that communicates that market data provider wants you to clear all entries
     * in accumulated order book. Once you receive BookResetEntry you need to wait for the next Snapshot to
     * rebuild order book (incremental update messages that may appear before the snapshot are invalid and should be ignored).
     *
     * @param resetEntry - Book reset entry
     * @param pck - Package header
     * @return true if process is success
     */
    boolean processBookResetEntry(PackageHeaderInfo pck, BookResetEntryInfo resetEntry);
}
