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
 * An enumeration of possible values for configuring the update mode.
 * <p>
 * Modes of order book update. Waiting snapshot don't apply incremental updates before it or no.
 */
public enum UpdateMode {

    /**
     * Waiting snapshot before processing incremental update.
     */
    WAITING_FOR_SNAPSHOT,

    /**
     * Process incremental update without waiting for the first snapshot.
     */
    // TODO ADD TEST for reset entry
    NON_WAITING_FOR_SNAPSHOT
}
