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
 * An enumeration of possible values for configuring the unreachable(level more than max deep limit) level in stock quotes.
 * <p>
 * Note: UnreachableDepthMode depends on max deep limit
 * Supported for L2
 *
 * @author Andrii_Ostapenko1
 * @see BindOrderBookOptionsBuilder#maxDepth(int)
 */
// TODO ADD UNIT TEST!!
public enum UnreachableDepthMode {

    /**
     * The operator ignores all unreachable levels in stock quotes.
     * <p>
     * If we have a level more than available level, then let's skip quote.
     */
    SKIP,

    /**
     * The operator drop all quotes for stock exchange.
     * <p>
     * If we have a level more than available level,
     * then let's skip quote and drop all quote for stock exchange.
     */
    SKIP_AND_DROP,

}
