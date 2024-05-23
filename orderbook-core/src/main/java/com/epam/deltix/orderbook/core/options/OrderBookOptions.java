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
 * Order book options.
 *
 * @author Andrii_Ostapenko1
 */
public interface OrderBookOptions {

    /**
     * Startup mode.
     *
     * @return snapshot mode.
     */
    Option<UpdateMode> getUpdateMode();

    /**
     * Periodical snapshot mode.
     *
     * @return periodical snapshot mode.
     */
    Option<PeriodicalSnapshotMode> getPeriodicalSnapshotMode();

    /**
     * Stock symbol.
     *
     * @return stock symbol.
     */
    Option<String> getSymbol();

    /**
     * Quote levels.
     *
     * @return quote levels.
     */
    Option<DataModelType> getQuoteLevels();

    /**
     * Should store quote timestamps.
     *
     * @return flag.
     */
    Option<Boolean> shouldStoreQuoteTimestamps();

    /**
     * Order book type.
     *
     * @return order book mode.
     */
    Option<OrderBookType> getBookType();

    /**
     * Initial depth of market.
     *
     * @return initial max depth of market.
     */
    Option<Integer> getInitialDepth();

    /**
     * Max depth of market (Limit).
     *
     * @return max depth of market.
     */
    Option<Integer> getMaxDepth();

    /**
     * Invalid quote mode.
     *
     * @return unreachableDepth mode.
     */
    Option<ValidationOptions> getInvalidQuoteMode();

    /**
     * Initial pool size for stock exchanges.
     *
     * @return pool size.
     */
    Option<Integer> getInitialExchangesPoolSize();

    /**
     * How order book will react on disconnect market data event.
     *
     * @return disconnectBehaviour.
     */
    Option<DisconnectMode> getDisconnectMode();

    /**
     * How order book will processing increment update after reset market data event
     *
     * @return resetMode
     */
    Option<ResetMode> getResetMode();

    //TODO add javadoc
    Option<ErrorListener> getErrorListener();

    //TODO add javadoc
    Option<Integer> getInitialSharedQuotePoolSize();

    //TODO add javadoc
    Option<ObjectPool<? extends OrderBookQuote>> getSharedObjectPool();

    /**
     * Whether compact version of L2 order book is used
     *
     * @return flag
     */
    Option<Boolean> isCompactVersion();
}
