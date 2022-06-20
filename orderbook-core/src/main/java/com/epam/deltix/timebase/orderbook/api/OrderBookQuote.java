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

import com.epam.deltix.dfp.Decimal;

/**
 * Represents order book entry.
 *
 * @author Andrii_Ostapenko1
 */
public interface OrderBookQuote {

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

}
