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
package com.epam.deltix.orderbook.core.api;

import com.epam.deltix.dfp.Decimal;

/**
 * Represents order book entry.
 *
 * @author Andrii_Ostapenko1
 */
public interface OrderBookQuote extends OrderBookQuoteTimestamp {

    /**
     * Ask, Bid or Trade price.
     *
     * @return Price
     */
    @Decimal
    long getPrice();

    /**
     * Ask, Bid or Trade quantity.
     *
     * @return Size
     */
    @Decimal
    long getSize();

    /**
     * Numbers of orders.
     *
     * @return Number Of Orders
     */
    long getNumberOfOrders();

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     *
     * @return Exchange Code
     */
    long getExchangeId();

    /**
     * Ask, Bid or Trade price.
     *
     * @return true if Price is not null
     */
    boolean hasPrice();

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     *
     * @return true if Exchange Code is not null
     */
    boolean hasExchangeId();

    /**
     * Numbers of orders.
     *
     * @return true if Number Of Orders is not null
     */
    boolean hasNumberOfOrders();

    /**
     * Ask, Bid or Trade quantity.
     *
     * @return true if Size is not null
     */
    boolean hasSize();

    /**
     * Quote ID. In Forex market, for example, quote ID can be referenced in
     * TradeOrders (to identify market maker's quote/rate we want to deal with).
     * Each market maker usually keeps this ID unique per session per day. This
     * is a alpha-numeric text field that can reach 64 characters or more,
     * <p>
     * Supported for L3 quote level
     *
     * @return Quote ID or null if not found
     */
    CharSequence getQuoteId();

    /**
     * Quote ID. In Forex market, for example, quote ID can be referenced in
     * TradeOrders (to identify market maker's quote/rate we want to deal with).
     * Each market maker usually keeps this ID unique per session per day. This
     * is a alpha-numeric text field that can reach 64 characters or more,
     * <p>
     * Supported for L3 quote level
     *
     * @return true if Quote ID is not null
     */
    boolean hasQuoteId();

    /**
     * Id of participant (or broker ID).
     * Supported for L3 quote level
     *
     * @return Participant or null if not found
     */
    CharSequence getParticipantId();

    /**
     * Id of participant (or broker ID).
     * Supported for L3 quote level
     *
     * @return true if Participant is not null
     */
    boolean hasParticipantId();

}
