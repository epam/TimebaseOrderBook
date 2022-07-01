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
 * An enumeration of possible values for configuring the gaps in stock quotes.
 * <p>
 * Note: GapMode working only with INCREMENTAL_UPDATE
 *
 * @author Andrii_Ostapenko1
 * @see com.epam.deltix.timebase.messages.universal.PackageType
 */
// TODO ADD UNIT TEST!!
public enum GapMode {

    /**
     * The operator ignores all gaps in stock quotes.
     * <p>
     * If we have a gap between the last existing level and currently inserted level (empty levels between them),
     * then let's skip quote.
     */
    SKIP,

    /**
     * The operator drop all quotes for stock exchange.
     * <p>
     * If we have a gap between the last existing level and currently inserted level (empty levels between them),
     * then let's skip quote and drop all quote for stock exchange.
     */
    SKIP_AND_DROP,

    /**
     * The operator fill gaps in stock quotes.
     * <p>
     * If we have a gap between the last existing level and currently inserted level (empty levels between them),
     * then let's fill these empty levels with values from the current event.
     */
    FILL_GAP

}
