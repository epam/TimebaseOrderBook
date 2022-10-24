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

public class OrderBookOptionsBuilder implements OrderBookOptions, BindOrderBookOptionsBuilder {

    private Option<DataModelType> quoteLevels = Option.empty();
    private Option<OrderBookType> bookType = Option.empty();
    private Option<UpdateMode> updateMode = Option.empty();
    private Option<GapMode> gapMode = Option.empty();
    private Option<String> symbol = Option.empty();
    private Option<Integer> initialDepth = Option.empty();

    private Option<Integer> maxDepth = Option.empty();

    private Option<UnreachableDepthMode> unreachableDepthMode = Option.empty();
    private Option<Integer> initialExchangesPoolSize = Option.empty();
    private Option<OrderBookOptions> otherOptions = Option.empty();

    @Override
    public BindOrderBookOptionsBuilder parent(final OrderBookOptions other) {
        this.otherOptions = Option.wrap(other);
        return this;
    }

    @Override
    public OrderBookOptions build() {
        return this;
    }

    @Override
    public BindOrderBookOptionsBuilder updateMode(final UpdateMode mode) {
        this.updateMode = Option.wrap(mode);
        return this;
    }

    @Override
    public Option<UpdateMode> getUpdateMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getUpdateMode().orAnother(updateMode);
        } else {
            return updateMode;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder quoteLevels(final DataModelType type) {
        this.quoteLevels = Option.wrap(type);
        return this;
    }

    @Override
    public Option<DataModelType> getQuoteLevels() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getQuoteLevels().orAnother(quoteLevels);
        } else {
            return quoteLevels;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder orderBookType(final OrderBookType type) {
        this.bookType = Option.wrap(type);
        return this;
    }

    @Override
    public Option<OrderBookType> getBookType() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getBookType().orAnother(bookType);
        } else {
            return bookType;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder initialDepth(final int value) {
        this.initialDepth = Option.wrap(value);
        return this;
    }

    @Override
    public Option<Integer> getInitialDepth() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getInitialDepth().orAnother(initialDepth);
        } else {
            return initialDepth;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder maxDepth(int value) {
        this.maxDepth = Option.wrap(value);
        return this;
    }

    @Override
    public Option<Integer> getMaxDepth() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getMaxDepth().orAnother(maxDepth);
        } else {
            return maxDepth;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder unreachableDepthMode(final UnreachableDepthMode mode) {
        this.unreachableDepthMode = Option.wrap(mode);
        return this;
    }

    @Override
    public Option<UnreachableDepthMode> getUnreachableDepthMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getUnreachableDepthMode().orAnother(unreachableDepthMode);
        } else {
            return unreachableDepthMode;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder initialExchangesPoolSize(final int value) {
        this.initialExchangesPoolSize = Option.wrap(value);
        return this;
    }

    @Override
    public Option<Integer> getInitialExchangesPoolSize() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getInitialExchangesPoolSize().orAnother(initialExchangesPoolSize);
        } else {
            return initialExchangesPoolSize;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder symbol(final String symbol) {
        this.symbol = Option.wrap(symbol);
        return this;
    }

    @Override
    public Option<String> getSymbol() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getSymbol().orAnother(symbol);
        } else {
            return symbol;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder gapMode(final GapMode mode) {
        this.gapMode = Option.wrap(mode);
        return this;
    }

    @Override
    public Option<GapMode> getGapMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getGapMode().orAnother(gapMode);
        } else {
            return gapMode;
        }
    }
}
