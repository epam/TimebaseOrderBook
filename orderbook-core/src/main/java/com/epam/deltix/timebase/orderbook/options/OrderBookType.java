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
package com.epam.deltix.timebase.orderbook.options;

/**
 * An enumeration of possible values for configuring the order book type.
 * <p>
 * Order book can work with single stock exchange and make aggregation by multiple stock exchange.
 *
 * @author Andrii_Ostapenko1
 */
public enum OrderBookType {

    /**
     * Order book that process market data only from single exchange.
     * Supported for L1/L2
     */
    SINGLE_EXCHANGE,

    /**
     * Aggregated view of multiple exchanges, you can see combined size of each price level.
     * Supported for L2
     */
    AGGREGATED,

    /**
     * Consolidated view on the market from multiple exchanges, you can see individual exchange sizes
     * Supported for L2
     */
    CONSOLIDATED;
}
