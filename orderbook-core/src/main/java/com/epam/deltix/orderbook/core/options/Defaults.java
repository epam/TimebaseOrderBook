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
import com.epam.deltix.timebase.messages.universal.DataModelType;

/**
 * Order book global defaults: these are used when no other values are available.
 */
public final class Defaults {

    /**
     * Utility class.
     */
    private Defaults() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Default {@link UpdateMode}.
     */
    public static final UpdateMode UPDATE_MODE = UpdateMode.WAITING_FOR_SNAPSHOT;

    /**
     * Default {@link PeriodicalSnapshotMode}.
     */
    public static final PeriodicalSnapshotMode PERIODICAL_SNAPSHOT_MODE = PeriodicalSnapshotMode.PROCESS_ALL;

    /**
     * Default {@link DataModelType}.
     */
    public static final DataModelType QUOTE_LEVELS = DataModelType.LEVEL_ONE;

    /**
     * Should Order book store timestamps of quote updates?
     */
    public static final boolean SHOULD_STORE_QUOTE_TIMESTAMPS = false;

    /**
     * Default {@link OrderBookType}.
     */
    public static final OrderBookType ORDER_BOOK_TYPE = OrderBookType.SINGLE_EXCHANGE;

    /**
     * The initial depth of market.
     */
    public static final Integer INITIAL_DEPTH = 256;

    /**
     * The maximum depth of market.
     */
    public static final Integer MAX_DEPTH = 32767;

    /**
     * Default {@link ValidationOptions}.
     */
    public static final ValidationOptions VALIDATION_OPTIONS = ValidationOptions.ALL_ENABLED;

    /**
     * Default {@link ResetMode}.
     */
    public static final ResetMode RESET_MODE = ResetMode.NON_WAITING_FOR_SNAPSHOT;

    /**
     * Initial pool size for stock exchanges.
     */
    public static final Integer INITIAL_EXCHANGES_POOL_SIZE = 1;

    /**
     * Default {@link DisconnectMode}.
     */
    public static final DisconnectMode DISCONNECT_MODE = DisconnectMode.CLEAR_EXCHANGE;

    public static final ErrorListener DEFAULT_ERROR_LISTENER = (message, errorCode)
            -> System.err.println("Error parsing message for " + message.getSymbol() + " at " + message.getTimeStampMs() + ": " + errorCode);


}
