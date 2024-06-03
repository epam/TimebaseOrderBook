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


import com.epam.deltix.orderbook.core.api.ErrorListener;
import com.epam.deltix.orderbook.core.api.OrderBookQuote;
import com.epam.deltix.orderbook.core.impl.ObjectPool;
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
     * What do we do with periodical snapshots?
     *
     * @param mode to use
     * @return builder
     * @see Defaults#PERIODICAL_SNAPSHOT_MODE
     * @see com.epam.deltix.timebase.messages.universal.PackageType#PERIODICAL_SNAPSHOT
     */
     BindOrderBookOptionsBuilder periodicalSnapshotMode(PeriodicalSnapshotMode mode);

    /**
     * Quote levels to use.
     *
     * @param type to use
     * @return builder
     * @see Defaults#QUOTE_LEVELS
     */
    BindOrderBookOptionsBuilder quoteLevels(DataModelType type);

    /**
     * Should store timestamps of quote updates?
     * If you enable this option, additional memory will be allocated for timestamps in each quote.
     *
     * <p>
     * When this option is enabled, you may use {@link OrderBookQuote#getOriginalTimestamp()} to get original quote timestamp
     * and {@link OrderBookQuote#getTimestamp()} to get timestamp of order book update. For checking if quote has timestamped
     * you may use {@link OrderBookQuote#hasOriginalTimestamp()} and {@link OrderBookQuote#hasTimestamp()}.
     * <p>
     * By default, this option is disabled.
     *
     * @param value flag
     * @return builder
     * @see com.epam.deltix.orderbook.core.api.OrderBookQuoteTimestamp
     */
    BindOrderBookOptionsBuilder shouldStoreQuoteTimestamps(boolean value);

    /**
     * Order book type to use.
     *
     * @param type to use
     * @return builder
     * @see Defaults#ORDER_BOOK_TYPE
     */
    BindOrderBookOptionsBuilder orderBookType(OrderBookType type);

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
     */
    BindOrderBookOptionsBuilder maxDepth(int value);

    /**
     * What do we do with invalid packets?
     * Supported for L2/L1 quote level
     *
     * @param mode to use.
     * @return builder
     * @see Defaults#VALIDATION_OPTIONS
     * @see ValidationOptions
     */
    BindOrderBookOptionsBuilder validationOptions(ValidationOptions mode);

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

    /**
     * How order book will react on disconnect market data event
     *
     * @param mode to use
     * @see Defaults#DISCONNECT_MODE
     * @return builder
     */
    BindOrderBookOptionsBuilder disconnectMode(DisconnectMode mode);

    /**
     * How order book will processing increment update after reset market data event
     *
     * @param mode to use
     * @see Defaults#RESET_MODE
     * @return builder
     */
    BindOrderBookOptionsBuilder resetMode(ResetMode mode);

    /**
     * Custom error logging
     *
     * @param errorListener custom error listener
     * @return builder
     */
    BindOrderBookOptionsBuilder errorListener(ErrorListener errorListener);

    //TODO add javadoc
    BindOrderBookOptionsBuilder sharedQuotePool(int initialSize);

    //TODO This method may not make sense

    /**
     * When defined allows sharing pool of OrderBookQuote objects between multiple order books
     * @param sharedObjectPool shared object pool to use
     * @return builder
     */
    BindOrderBookOptionsBuilder sharedQuotePool(ObjectPool<? extends OrderBookQuote> sharedObjectPool);

    /**
     * Use compact version of order book?
     * If you enable this option, order book will only store prices and sizes in one array (and therefore should be faster)
     * <p>
     * By default, this option is disabled.
     *
     * @param value flag
     * @return builder
     */
    BindOrderBookOptionsBuilder isCompactVersion(boolean value);
}
