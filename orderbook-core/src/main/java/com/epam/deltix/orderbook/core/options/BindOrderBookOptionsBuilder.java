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
package com.epam.deltix.orderbook.core.options;

import com.epam.deltix.timebase.messages.universal.DataModelType;

/**
 * @author Andrii_Ostapenko1
 */
public interface BindOrderBookOptionsBuilder {

    /**
     * Override the defaults from the given option.
     * You may use this only once.
     *
     * @param other options to base on
     * @return builder
     */
    BindOrderBookOptionsBuilder parent(OrderBookOptions other);

    /**
     * Produce the final Options
     *
     * @return options object.
     */
    OrderBookOptions build();

    /**
     * Stock symbol to use.
     * <p>
     * This stock symbol is used to check all input packets before processing.
     * If you are sure that your market data contains data for only one stock symbol, you may not set this option
     * and skip validation for symbol per each package during update order book.
     *
     * @param symbol to use
     * @return builder
     */
    BindOrderBookOptionsBuilder symbol(String symbol);

    /**
     * What do we do with incremental update if we have empty order book?
     *
     * @param mode to use
     * @return builder
     * @see Defaults#UPDATE_MODE
     * @see com.epam.deltix.timebase.messages.universal.PackageType#INCREMENTAL_UPDATE
     */
    BindOrderBookOptionsBuilder updateMode(UpdateMode mode);

    /**
     * Quote levels to use.
     *
     * @param type to use
     * @return builder
     * @see Defaults#QUOTE_LEVELS
     */
    BindOrderBookOptionsBuilder quoteLevels(DataModelType type);

    /**
     * Order book type to use.
     *
     * @param type to use
     * @return builder
     * @see Defaults#ORDER_BOOK_TYPE
     */
    BindOrderBookOptionsBuilder orderBookType(OrderBookType type);

    /**
     * What do we do if we have a gap between the last existing level and the current inserted level (empty levels in between)?.
     * <p>
     * Supported for L2 quote level.
     *
     * @param mode to use
     * @return builder
     * @see Defaults#GAP_MODE
     * @see GapMode
     */
    BindOrderBookOptionsBuilder gapMode(GapMode mode);

    /**
     * How large initial depth of market should be?
     * Supported for all  order book types.
     *
     * @param value initial max depth.
     * @return builder
     * @see Defaults#INITIAL_DEPTH
     * @see OrderBookType
     */
    BindOrderBookOptionsBuilder initialDepth(int value);

    /**
     * How large maximum (limit) depth of market should be?
     * Supported for L2 quote level
     *
     * @param value initial max depth.
     * @return builder
     * @see Defaults#MAX_DEPTH
     * @see OrderBookType
     */
    BindOrderBookOptionsBuilder maxDepth(int value);

    /**
     * How large initial pool size for stock exchanges should be?
     * Supported for AGGREGATED and CONSOLIDATED order book type.
     *
     * @param value pool size to use
     * @return builder
     * @see Defaults#INITIAL_EXCHANGES_POOL_SIZE
     * @see OrderBookType#CONSOLIDATED
     * @see OrderBookType#AGGREGATED
     */
    BindOrderBookOptionsBuilder initialExchangesPoolSize(int value);

}
