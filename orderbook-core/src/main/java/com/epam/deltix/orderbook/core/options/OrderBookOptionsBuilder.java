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
import com.epam.deltix.orderbook.core.impl.QuotePoolFactory;
import com.epam.deltix.timebase.messages.universal.DataModelType;

public class OrderBookOptionsBuilder implements OrderBookOptions, BindOrderBookOptionsBuilder {

    private Option<DataModelType> quoteLevels = Option.empty();
    private Option<Boolean> shouldStoreQuoteTimestamps = Option.empty();
    private Option<OrderBookType> bookType = Option.empty();
    private Option<UpdateMode> updateMode = Option.empty();
    private Option<PeriodicalSnapshotMode> periodicalSnapshotMode = Option.empty();
    private Option<String> symbol = Option.empty();
    private Option<Integer> initialDepth = Option.empty();
    private Option<Integer> maxDepth = Option.empty();
    private Option<ValidationOptions> unreachableDepthMode = Option.empty();
    private Option<Integer> initialExchangesPoolSize = Option.empty();
    private Option<OrderBookOptions> otherOptions = Option.empty();
    private Option<DisconnectMode> disconnectMode = Option.empty();
    private Option<ResetMode> resetMode = Option.empty();
    private Option<ErrorListener> errorListener = Option.empty();
    private Option<Integer> initialSharedQuotePoolSize = Option.empty();
    private Option<ObjectPool<? extends OrderBookQuote>> sharedObjectPool = Option.empty();
    private Option<Boolean> isCompactVersion = Option.empty();

    @Override
    public BindOrderBookOptionsBuilder parent(final OrderBookOptions other) {
        this.otherOptions = Option.wrap(other);
        return this;
    }

    @Override
    public OrderBookOptions build() {
        if (!sharedObjectPool.hasValue() && initialSharedQuotePoolSize.hasValue()) {
            this.sharedObjectPool = Option.wrap(QuotePoolFactory.create(this, initialSharedQuotePoolSize.get()));
        }
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
    public BindOrderBookOptionsBuilder periodicalSnapshotMode(final PeriodicalSnapshotMode mode) {
        this.periodicalSnapshotMode = Option.wrap(mode);
        return this;
    }

    @Override
    public Option<PeriodicalSnapshotMode> getPeriodicalSnapshotMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getPeriodicalSnapshotMode().orAnother(periodicalSnapshotMode);
        } else {
            return periodicalSnapshotMode;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder quoteLevels(final DataModelType type) {
        this.quoteLevels = Option.wrap(type);
        return this;
    }

    @Override
    public BindOrderBookOptionsBuilder shouldStoreQuoteTimestamps(final boolean value) {
        this.shouldStoreQuoteTimestamps = Option.wrap(value);
        return this;
    }

    @Override
    public Option<Boolean> shouldStoreQuoteTimestamps() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().shouldStoreQuoteTimestamps().orAnother(shouldStoreQuoteTimestamps);
        } else {
            return shouldStoreQuoteTimestamps;
        }
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
    public BindOrderBookOptionsBuilder validationOptions(final ValidationOptions mode) {
        this.unreachableDepthMode = Option.wrap(mode);
        return this;
    }

    @Override
    public Option<ValidationOptions> getInvalidQuoteMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getInvalidQuoteMode().orAnother(unreachableDepthMode);
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
    public BindOrderBookOptionsBuilder disconnectMode(final DisconnectMode disconnectMode) {
        this.disconnectMode = Option.wrap(disconnectMode);
        return this;
    }

    @Override
    public Option<DisconnectMode> getDisconnectMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getDisconnectMode().orAnother(disconnectMode);
        } else {
            return disconnectMode;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder errorListener(final ErrorListener errorListener) {
        this.errorListener = Option.wrap(errorListener);
        return this;
    }

    @Override
    public Option<ErrorListener> getErrorListener() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getErrorListener().orAnother(errorListener);
        } else {
            return errorListener;
        }
    }

    @Override
    public Option<Integer> getInitialSharedQuotePoolSize() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getInitialSharedQuotePoolSize().orAnother(initialSharedQuotePoolSize);
        } else {
            return initialSharedQuotePoolSize;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder sharedQuotePool(int initialSize) {
        this.initialSharedQuotePoolSize = Option.wrap(initialSize);
        return this;
    }

    @Override
    public BindOrderBookOptionsBuilder sharedQuotePool(final ObjectPool<? extends OrderBookQuote> sharedObjectPool) {
        this.sharedObjectPool = Option.wrap(sharedObjectPool);
        return this;
    }

    @Override
    public Option<ObjectPool<? extends OrderBookQuote>> getSharedObjectPool() {
        if (sharedObjectPool.hasValue()) {
            return otherOptions.get().getSharedObjectPool().orAnother(sharedObjectPool);
        } else {
            return sharedObjectPool;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder isCompactVersion(final boolean value) {
        this.isCompactVersion = Option.wrap(value);
        return this;
    }

    @Override
    public Option<Boolean> isCompactVersion() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().isCompactVersion().orAnother(isCompactVersion);
        } else {
            return isCompactVersion;
        }
    }

    @Override
    public BindOrderBookOptionsBuilder resetMode(final ResetMode resetMode) {
        this.resetMode = Option.wrap(resetMode);
        return this;
    }

    @Override
    public Option<ResetMode> getResetMode() {
        if (otherOptions.hasValue()) {
            return otherOptions.get().getResetMode().orAnother(resetMode);
        } else {
            return resetMode;
        }
    }

}
