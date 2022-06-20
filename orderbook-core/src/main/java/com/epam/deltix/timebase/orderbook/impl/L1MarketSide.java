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
package com.epam.deltix.timebase.orderbook.impl;

import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.timebase.orderbook.api.MarketSide;

import java.util.Objects;

/**
 * @author Andrii_Ostapenko1
 */
interface L1MarketSide<Quote> extends MarketSide<Quote> {

    static <Quote extends MutableOrderBookQuote> L1MarketSide<Quote> factory(final QuoteSide side) {
        Objects.requireNonNull(side);
        switch (side) {
            case BID:
                return new AbstractL1MarketSide.BID<>();
            case ASK:
                return new AbstractL1MarketSide.ASK<>();
            default:
                throw new IllegalStateException("Unexpected value: " + side);
        }
    }

    void insert(Quote insert);

    void clear();
}
