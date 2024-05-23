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

/**
 * An enumeration of possible values for setting the processing periodic snapshots mode.
 * <p>
 * Modes for processing periodic snapshots. What do we do with periodic snapshots?
 *
 * @see deltix.timebase.api.messages.universal.PackageType#PERIODICAL_SNAPSHOT
 */
public enum PeriodicalSnapshotMode {

    /**
     * Skip all periodic snapshots.
     */
    SKIP_ALL,

    /**
     * Processing of all periodic snapshots.
     */
    PROCESS_ALL,

    /**
     * Processing a periodic snapshot only once when the order book is waiting for a snapshot.
     *
     * @see deltix.orderbook.core.options.UpdateMode#WAITING_FOR_SNAPSHOT
     */
    ONLY_ONE,
}
