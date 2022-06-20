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

import com.epam.deltix.timebase.messages.universal.BookResetEntryInfo;

/**
 * Processor for reset entry universal market data format that included in package (Package Header).
 *
 * @author Andrii_Ostapenko
 */
interface ResetEntryProcessor {

    /**
     * Book Reset is a Special type of entry that communicates that market data provider wants you to clear all entries
     * in accumulated order book. Once you receive BookResetEntry you need to wait for the next Snapshot to
     * rebuild order book (incremental update messages that may appear before the snapshot are invalid and should be ignored).
     *
     * @param bookResetEntryInfo - Book reset entry
     */
    void processBookResetEntry(final BookResetEntryInfo bookResetEntryInfo);

}
