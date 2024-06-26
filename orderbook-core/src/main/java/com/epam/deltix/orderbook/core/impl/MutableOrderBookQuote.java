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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.timebase.messages.universal.BasePriceEntryInfo;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;
import com.epam.deltix.util.annotations.Alphanumeric;


/**
 * ReadWrite interface for Quote(the smallest element in order book).
 *
 * @author Andrii_Ostapenko1
 */
interface MutableOrderBookQuote extends OrderBookQuote, Comparable<MutableOrderBookQuote> {

    /**
     * Ask, Bid or Trade price.
     *
     * @param price - Price
     */
    void setPrice(@Decimal long price);

    /**
     * Ask, Bid or Trade quantity.
     *
     * @param size - Size
     */
    void setSize(@Decimal long size);

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     * see #getExchange()
     *
     * @param exchangeId - Exchange Code
     */
    void setExchangeId(@Alphanumeric long exchangeId);

    /**
     * Numbers of orders.
     *
     * @param numberOfOrders - Number Of Orders
     */
    void setNumberOfOrders(long numberOfOrders);


    default void setTimestamp(long timestamp) {
        //do nothing
    }

    default void setOriginalTimestamp(long timestamp) {
        //do nothing
    }

    default long getSequenceNumber() {
        // return nothing
        return Long.MIN_VALUE;
    }

    default void setSequenceNumber(long timestamp) {
        // do nothing
    }

    /**
     * Method copies state to a given instance
     *
     * @param src class instance that should be used as a copy source
     */
    void copyFrom(MutableOrderBookQuote src);

    /**
     * Method copies state to a given instance
     *
     * @param pck
     * @param src - BasePriceEntryInfo
     */
    void copyFrom(PackageHeaderInfo pck, BasePriceEntryInfo src);

    /**
     * Resets all instance properties to their default values
     */
    void release();

}
