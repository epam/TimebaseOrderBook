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
package com.epam.deltix.timebase.orderbook.api;


import com.epam.deltix.timebase.messages.universal.QuoteSide;

/**
 * Represents the order book entries for a specific stock exchange.
 *
 * @author Andrii_Ostapenko1
 */
public interface Exchange<Quote> {

    /**
     * Get exchangeId for this exchange.
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding.
     *
     * @return exchangeId for this exchange.
     */
    long getExchangeId();

    /**
     * Get market side.
     *
     * @param side - side of quote (ASk or BID) to use.
     * @return market side. never {@code null}
     * @see MarketSide
     */
    MarketSide<Quote> getMarketSide(QuoteSide side);

}
