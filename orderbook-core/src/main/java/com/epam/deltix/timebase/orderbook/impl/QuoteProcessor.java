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

import com.epam.deltix.timebase.messages.MarketMessageInfo;
import com.epam.deltix.timebase.messages.universal.BaseEntryInfo;
import com.epam.deltix.timebase.messages.universal.BookResetEntryInfo;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.options.Option;

/**
 * @author Andrii_Ostapenko1
 */
interface QuoteProcessor<Quote> extends OrderBook<Quote> {

    @Override
    default boolean update(final MarketMessageInfo ignore) {
        throw new UnsupportedOperationException("Unsupported for processor: " + getDescription());
    }

    @Override
    default Option<String> getSymbol() {
        throw new UnsupportedOperationException("Unsupported for processor: " + getDescription());
    }

    /**
     * Process incremental update market data entry.
     *
     * @param pck - Package header container
     * @return true if process is success
     */
    boolean process(final BaseEntryInfo pck);

    /**
     * Process only snapshot(VENDOR,PERIODICAL) market data entry.
     *
     * @param marketMessageInfo - Package header container
     * @return true if process is success
     */
    boolean processSnapshot(final PackageHeaderInfo marketMessageInfo);

    /**
     * Waiting snapshot don't apply incremental updates before it.
     * <p>
     * This method using for handle book reset entry.
     *
     * @return true if waiting for snapshot and return false if no waiting for snapshot
     * @see ResetEntryProcessor#processBookResetEntry(BookResetEntryInfo)
     */
    boolean isWaitingForSnapshot();
}
